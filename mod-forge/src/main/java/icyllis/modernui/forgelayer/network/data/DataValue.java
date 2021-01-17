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

package icyllis.modernui.forgelayer.network.data;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;

public abstract class DataValue {

    final Runnable dirty;

    /**
     * For NBT storage
     */
    @Nullable
    final String key;

    DataValue(Runnable dirty, @Nullable String key) {
        this.dirty = dirty;
        this.key = key;
    }

    public abstract void write(CompoundNBT compoundNBT);

    public abstract void read(CompoundNBT compoundNBT);

    public abstract void write(PacketBuffer buffer);

    public abstract void read(PacketBuffer buffer);
}
