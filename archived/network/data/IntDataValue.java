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

package icyllis.modernui.forge.network.data;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class IntDataValue extends DataValue {

    private int value;

    IntDataValue(Runnable dirty, @Nullable String key) {
        super(dirty, key);
    }

    @Override
    public void write(@Nonnull CompoundNBT compoundNBT) {
        Objects.requireNonNull(key, "Accessing NBT value but key is unspecified");
        compoundNBT.putInt(key, value);
    }

    @Override
    public void read(@Nonnull CompoundNBT compoundNBT) {
        Objects.requireNonNull(key, "Accessing NBT value but key is unspecified");
        value = compoundNBT.getInt(key);
    }

    @Override
    public void write(@Nonnull PacketBuffer buffer) {
        buffer.writeVarInt(value);
    }

    @Override
    public void read(@Nonnull PacketBuffer buffer) {
        value = buffer.readVarInt();
    }

    public void setValue(int v) {
        if (value != v) {
            value = v;
            dirty.run();
        }
    }

    public int getValue() {
        return value;
    }
}
