/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.system;

import icyllis.modernui.network.NetworkHandler;
import icyllis.modernui.system.mixin.AccessFoodData;
import icyllis.modernui.view.UIManager;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.FoodStats;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * For internal use
 */
public final class NetMessages {

    static NetworkHandler network;

    private NetMessages() {
    }

    static void handle(short index, @Nonnull PacketBuffer payload, @Nullable ServerPlayerEntity player) {

    }

    @Nonnull
    static NetworkHandler.IClientMsgHandler handle() {
        return C::handle;
    }

    @Nullable
    static NetworkHandler.IClientMsgHandler ignore() {
        return null;
    }

    @Nonnull
    public static NetworkHandler food(float foodSaturationLevel, float foodExhaustionLevel) {
        PacketBuffer buffer = network.allocBuf(0);
        buffer.writeFloat(foodSaturationLevel);
        buffer.writeFloat(foodExhaustionLevel);
        return network;
    }

    static NetworkHandler menu(int containerId, int menuId, Consumer<PacketBuffer> writer) {
        PacketBuffer buffer = network.allocBuf(1);
        buffer.writeVarInt(containerId);
        buffer.writeVarInt(menuId);
        if (writer != null) {
            writer.accept(buffer);
        }
        return network;
    }

    // on logical client
    @OnlyIn(Dist.CLIENT)
    public static final class C {

        private C() {
        }

        private static void handle(short index, @Nonnull PacketBuffer payload, @Nullable ClientPlayerEntity player) {
            if (player != null) {
                switch (index) {
                    case 0:
                        food(payload, player);
                        break;
                    case 1:
                        menu(payload, player);
                        break;
                }
            }
        }

        private static void food(@Nonnull PacketBuffer buffer, @Nonnull ClientPlayerEntity player) {
            FoodStats foodData = player.getFoodStats();
            foodData.setFoodSaturationLevel(buffer.readFloat());
            ((AccessFoodData) foodData).setFoodExhaustionLevel(buffer.readFloat());
        }

        private static void menu(@Nonnull PacketBuffer buffer, @Nonnull ClientPlayerEntity player) {
            UIManager.getInstance().openGUI(player, buffer.readVarInt(), buffer.readVarInt(), buffer);
        }
    }
}
