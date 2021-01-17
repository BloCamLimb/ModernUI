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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class LongDataValue extends DataValue {

    private long value;

    LongDataValue(Runnable dirty, @Nullable String key) {
        super(dirty, key);
    }

    @Override
    public void write(@Nonnull CompoundNBT compoundNBT) {
        Objects.requireNonNull(key, "Accessing NBT value but key is unspecified");
        compoundNBT.putLong(key, value);
    }

    @Override
    public void read(@Nonnull CompoundNBT compoundNBT) {
        Objects.requireNonNull(key, "Accessing NBT value but key is unspecified");
        value = compoundNBT.getLong(key);
    }

    @Override
    public void write(@Nonnull PacketBuffer buffer) {
        buffer.writeVarLong(value);
    }

    @Override
    public void read(@Nonnull PacketBuffer buffer) {
        value = buffer.readVarLong();
    }

    public void setValue(long v) {
        if (value != v) {
            value = v;
            dirty.run();
        }
    }

    public long getValue() {
        return value;
    }
}
