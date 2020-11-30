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

import icyllis.modernui.system.mixin.AccessFoodStats;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.FoodStats;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class MsgHandler {

    static void handle(short index, @Nonnull PacketBuffer payload, @Nullable ServerPlayerEntity player) {

    }

    @OnlyIn(Dist.CLIENT)
    static class C {

        static void handle(short index, @Nonnull PacketBuffer payload, @Nullable ClientPlayerEntity player) {
            if (player != null) {
                if (index == 0) {
                    food(payload, player);
                }
            }
        }

        private static void food(@Nonnull PacketBuffer buffer, @Nonnull ClientPlayerEntity player) {
            FoodStats foodStats = player.getFoodStats();
            foodStats.setFoodSaturationLevel(buffer.readFloat());
            ((AccessFoodStats) foodStats).setFoodExhaustionLevel(buffer.readFloat());
        }
    }
}
