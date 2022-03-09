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

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Internal use.
 */
public final class NetworkMessages {

    private static final int S2C_OPEN_MENU = 0;

    static NetworkHandler sNetwork;

    private NetworkMessages() {
    }

    // a safe supplier
    @Nonnull
    static NetworkHandler.ClientListener msg() {
        // this supplier won't be called on dedicated server, so it's in the inner class
        return C::msg;
    }

    /*@Deprecated
    @Nonnull
    public static PacketDispatcher syncFood(float foodSaturationLevel, float foodExhaustionLevel) {
        FriendlyByteBuf buf = NetworkHandler.buffer(0);
        buf.writeFloat(foodSaturationLevel);
        buf.writeFloat(foodExhaustionLevel);
        return sNetwork.dispatcher(buf);
    }*/

    static void openMenu(int containerId, int menuId, @Nullable Consumer<FriendlyByteBuf> writer, ServerPlayer p) {
        FriendlyByteBuf buf = NetworkHandler.buffer(S2C_OPEN_MENU);
        buf.writeVarInt(containerId);
        buf.writeVarInt(menuId);
        if (writer != null) {
            writer.accept(buf);
        }
        sNetwork.sendToPlayer(buf, p);
    }

    // this class doesn't load on dedicated server
    @OnlyIn(Dist.CLIENT)
    private static final class C {

        private C() {
        }

        private static void msg(short index, @Nonnull FriendlyByteBuf payload, @Nonnull Supplier<LocalPlayer> player) {
            /*case 0:
                    syncFood(payload, player);
                    break;*/
            if (index == S2C_OPEN_MENU) {
                openMenu(payload, player);
            }
        }

        /*@Deprecated
        private static void syncFood(@Nonnull FriendlyByteBuf buffer, @Nonnull LocalPlayer player) {
            FoodData foodData = player.getFoodData();
            foodData.setSaturation(buffer.readFloat());
            ((AccessFoodData) foodData).setExhaustionLevel(buffer.readFloat());
        }*/

        @SuppressWarnings("deprecation")
        private static void openMenu(@Nonnull FriendlyByteBuf payload, @Nonnull Supplier<LocalPlayer> player) {
            final int containerId = payload.readVarInt();
            // No barrier, SAFE
            final MenuType<?> type = Registry.MENU.byIdOrThrow(payload.readVarInt());
            final ResourceLocation key = Registry.MENU.getKey(type);
            assert key != null;
            payload.retain();
            Minecraft.getInstance().execute(() -> {
                final LocalPlayer p = player.get();
                if (p != null) {
                    UIManager.getInstance().start(p, type.create(containerId, p.getInventory(), payload), key);
                }
                payload.release();
            });
        }
    }
}
