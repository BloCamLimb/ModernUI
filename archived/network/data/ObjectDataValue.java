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

import javax.annotation.Nullable;
import java.util.Objects;

public class ObjectDataValue<T> extends DataValue {

    private T value;

    private final IDataAdaptor<T> adaptor;

    ObjectDataValue(Runnable dirty, @Nullable String key, IDataAdaptor<T> adaptor) {
        super(dirty, key);
        this.adaptor = adaptor;
    }

    @Override
    public void write(CompoundNBT compoundNBT) {
        Objects.requireNonNull(key, "Accessing NBT value but key is unspecified");
        adaptor.write(compoundNBT, key, value);
    }

    @Override
    public void read(CompoundNBT compoundNBT) {
        Objects.requireNonNull(key, "Accessing NBT value but key is unspecified");
        value = adaptor.read(compoundNBT, key);
    }

    @Override
    public void write(PacketBuffer buffer) {
        adaptor.write(buffer, value);
    }

    @Override
    public void read(PacketBuffer buffer) {
        value = adaptor.read(buffer);
    }

    public void setValue(T v) {
        if (!value.equals(v)) {
            value = v;
            markDirty();
        }
    }

    public T getValue() {
        return value;
    }

    public void markDirty() {
        dirty.run();
    }
}
