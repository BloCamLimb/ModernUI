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

package icyllis.modernui.network;

import icyllis.modernui.system.mixin.AccessorFoodStats;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.FoodStats;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class S2CMsgHandler {

    static final IConsumer[] CONSUMERS = new IConsumer[]{S2CMsgHandler::food};

    private static void food(PacketBuffer buffer, @Nullable ClientPlayerEntity player) {
        if (player != null) {
            FoodStats foodStats = player.getFoodStats();
            foodStats.setFoodSaturationLevel(buffer.readFloat());
            ((AccessorFoodStats) foodStats).setFoodExhaustionLevel(buffer.readFloat());
        }
    }

    @FunctionalInterface
    interface IConsumer {

        void handle(PacketBuffer buffer, @Nullable ClientPlayerEntity player);
    }
}
