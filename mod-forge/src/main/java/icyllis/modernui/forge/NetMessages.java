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

import icyllis.modernui.mcimpl.mixin.AccessFoodData;
import icyllis.modernui.forge.network.NetworkHandler;
import icyllis.modernui.view.UIManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
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

    // handle c2s messages
    static void handle(short index, @Nonnull FriendlyByteBuf payload, @Nullable ServerPlayer player) {

    }

    // return a safe supplier of a s2c handler on client
    @Nonnull
    static NetworkHandler.IClientMsgHandler handle() {
        return C::handle;
    }

    // return a safe supplier of a s2c handler on dedicated server
    @Nullable
    static NetworkHandler.IClientMsgHandler ignore() {
        return null;
    }

    @Nonnull
    public static NetworkHandler food(float foodSaturationLevel, float foodExhaustionLevel) {
        FriendlyByteBuf buffer = network.allocBuf(0);
        buffer.writeFloat(foodSaturationLevel);
        buffer.writeFloat(foodExhaustionLevel);
        return network;
    }

    static NetworkHandler menu(int containerId, int menuId, Consumer<FriendlyByteBuf> writer) {
        FriendlyByteBuf buffer = network.allocBuf(1);
        buffer.writeVarInt(containerId);
        buffer.writeVarInt(menuId);
        if (writer != null) {
            writer.accept(buffer);
        }
        return network;
    }

    // this class doesn't exist on dedicated server
    @OnlyIn(Dist.CLIENT)
    public static final class C {

        private C() {
        }

        private static void handle(short index, @Nonnull FriendlyByteBuf payload, @Nullable LocalPlayer player) {
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

        private static void food(@Nonnull FriendlyByteBuf buffer, @Nonnull LocalPlayer player) {
            FoodData foodData = player.getFoodData();
            foodData.setSaturation(buffer.readFloat());
            ((AccessFoodData) foodData).setExhaustionLevel(buffer.readFloat());
        }

        private static void menu(@Nonnull FriendlyByteBuf buffer, @Nonnull LocalPlayer player) {
            UIManager.getInstance().openGUI(player, buffer.readVarInt(), buffer.readVarInt(), buffer);
        }
    }
}
