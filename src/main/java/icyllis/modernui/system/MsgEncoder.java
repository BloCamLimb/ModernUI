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
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * Internal Use Only
 */
public final class MsgEncoder {

    static NetworkHandler network;

    @Nonnull
    public static NetworkHandler food(float foodSaturationLevel, float foodExhaustionLevel) {
        PacketBuffer buffer = network.allocBuffer(0);
        buffer.writeFloat(foodSaturationLevel);
        buffer.writeFloat(foodExhaustionLevel);
        return network;
    }

    @OnlyIn(Dist.CLIENT)
    public static class C {

    }
}
