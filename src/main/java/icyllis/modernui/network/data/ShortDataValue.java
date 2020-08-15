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

package icyllis.modernui.network.data;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class ShortDataValue extends DataValue {

    private short value;

    ShortDataValue(Runnable dirty, @Nullable String key) {
        super(dirty, key);
    }

    @Override
    public void write(@Nonnull CompoundNBT compoundNBT) {
        Objects.requireNonNull(key, "Accessing NBT value but key is unspecified");
        compoundNBT.putShort(key, value);
    }

    @Override
    public void read(@Nonnull CompoundNBT compoundNBT) {
        Objects.requireNonNull(key, "Accessing NBT value but key is unspecified");
        value = compoundNBT.getShort(key);
    }

    @Override
    public void write(@Nonnull PacketBuffer buffer) {
        buffer.writeShort(value);
    }

    @Override
    public void read(@Nonnull PacketBuffer buffer) {
        value = buffer.readShort();
    }

    public void setValue(short v) {
        if (value != v) {
            value = v;
            dirty.run();
        }
    }

    public short getValue() {
        return value;
    }
}
