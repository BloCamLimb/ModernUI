/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.forge;

import icyllis.modernui.ModernUI;
import icyllis.modernui.mixin.AccessFoodData;
import icyllis.modernui.screen.UIManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Internal use
 */
public final class NetworkMessages {

    static NetworkHandler sNetwork;

    private NetworkMessages() {
    }

    // handle C2S messages
    static void handle(short index, @Nonnull FriendlyByteBuf payload, @Nonnull ServerPlayer player) {
    }

    // returns a safe supplier of a S2C handler on client
    @Nonnull
    static NetworkHandler.ClientListener handle() {
        return C::handle; // this supplier won't be called on dedicated server, so it's in the C class
    }

    @Deprecated
    @Nonnull
    public static PacketDispatcher syncFood(float foodSaturationLevel, float foodExhaustionLevel) {
        FriendlyByteBuf buf = NetworkHandler.buffer(0);
        buf.writeFloat(foodSaturationLevel);
        buf.writeFloat(foodExhaustionLevel);
        return sNetwork.getDispatcher(buf);
    }

    @Nonnull
    static PacketDispatcher openMenu(int containerId, int menuId, @Nullable Consumer<FriendlyByteBuf> writer) {
        FriendlyByteBuf buf = NetworkHandler.buffer(1);
        buf.writeVarInt(containerId);
        buf.writeVarInt(menuId);
        if (writer != null) {
            writer.accept(buf);
        }
        return sNetwork.getDispatcher(buf);
    }

    // this class doesn't allow to load on dedicated server
    @OnlyIn(Dist.CLIENT)
    public static final class C {

        private C() {
        }

        private static void handle(short index, @Nonnull FriendlyByteBuf payload, @Nonnull LocalPlayer player) {
            switch (index) {
                /*case 0:
                    syncFood(payload, player);
                    break;*/
                case 1:
                    openMenu(payload, player);
                    break;
            }
        }

        @Deprecated
        private static void syncFood(@Nonnull FriendlyByteBuf buffer, @Nonnull LocalPlayer player) {
            FoodData foodData = player.getFoodData();
            foodData.setSaturation(buffer.readFloat());
            ((AccessFoodData) foodData).setExhaustionLevel(buffer.readFloat());
        }

        @SuppressWarnings("deprecation")
        private static void openMenu(@Nonnull FriendlyByteBuf buffer, @Nonnull LocalPlayer player) {
            final int containerId = buffer.readVarInt();
            final int menuId = buffer.readVarInt();
            final MenuType<?> type = Registry.MENU.byId(menuId);
            boolean success = false;
            if (type == null) {
                ModernUI.LOGGER.warn(UIManager.MARKER, "Trying to open invalid screen for menu id: {}", menuId);
            } else {
                final AbstractContainerMenu menu = type.create(containerId, player.inventory, buffer);
                ResourceLocation key = Registry.MENU.getKey(type);
                if (menu == null) {
                    ModernUI.LOGGER.error(UIManager.MARKER, "No container menu created from menu type: {}", key);
                } else if (key != null) {
                    success = UIManager.getInstance().openMenu(player, menu, key.getNamespace());
                }
            }
            if (!success) {
                player.closeContainer(); // close server menu
            }
        }
    }
}
