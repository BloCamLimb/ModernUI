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

package icyllis.modernui.core.forge;

import icyllis.modernui.ModernUI;
import icyllis.modernui.core.mixin.AccessFoodData;
import icyllis.modernui.view.UIManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

@ApiStatus.Internal
public final class NetMessages {

    static NetworkHandler network;

    private NetMessages() {
    }

    // handle c2s messages
    static void handle(short index, @Nonnull FriendlyByteBuf payload, @Nonnull ServerPlayer player) {

    }

    // returns a safe supplier of a s2c handler on client
    @Nonnull
    static NetworkHandler.IClientMsgHandler handle() {
        return C::handle; // this supplier won't be called on dedicated server, so it's in the C class
    }

    @Nonnull
    public static NetworkHandler.Broadcaster food(float foodSaturationLevel, float foodExhaustionLevel) {
        FriendlyByteBuf buf = network.targetAt(0);
        buf.writeFloat(foodSaturationLevel);
        buf.writeFloat(foodExhaustionLevel);
        return network.prepare(buf);
    }

    @Nonnull
    static NetworkHandler.Broadcaster menu(int containerId, int menuId, Consumer<FriendlyByteBuf> writer) {
        FriendlyByteBuf buf = network.targetAt(1);
        buf.writeVarInt(containerId);
        buf.writeVarInt(menuId);
        if (writer != null) {
            writer.accept(buf);
        }
        return network.prepare(buf);
    }

    // this class doesn't allow to load on dedicated server
    @OnlyIn(Dist.CLIENT)
    public static final class C {

        private C() {
        }

        private static void handle(short index, @Nonnull FriendlyByteBuf payload, @Nonnull LocalPlayer player) {
            switch (index) {
                case 0:
                    food(payload, player);
                    break;
                case 1:
                    menu(payload, player);
                    break;
            }
        }

        private static void food(@Nonnull FriendlyByteBuf buffer, @Nonnull LocalPlayer player) {
            FoodData foodData = player.getFoodData();
            foodData.setSaturation(buffer.readFloat());
            ((AccessFoodData) foodData).setExhaustionLevel(buffer.readFloat());
        }

        private static void menu(@Nonnull FriendlyByteBuf buffer, @Nonnull LocalPlayer player) {
            final int containerId = buffer.readVarInt();
            final int menuId = buffer.readVarInt();
            final MenuType<?> type = Registry.MENU.byId(menuId);
            boolean success = false;
            if (type == null) {
                ModernUI.LOGGER.warn(UIManager.MARKER, "Trying to open invalid screen for menu id: {}", menuId);
            } else {
                final AbstractContainerMenu menu = type.create(containerId, player.inventory, buffer);
                if (menu == null) {
                    ModernUI.LOGGER.error(UIManager.MARKER, "No container menu created from menu type: {}", Registry.MENU.getKey(type));
                } else {
                    success = UIManager.getInstance().openGUI(player, menu);
                }
            }
            if (!success) player.closeContainer(); // close server container
        }
    }
}
