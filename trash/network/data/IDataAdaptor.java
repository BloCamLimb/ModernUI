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
import java.util.UUID;

/**
 * Write and read non-primitive data value
 *
 * @param <T> value type
 */
public interface IDataAdaptor<T> {

    void write(CompoundNBT compoundNBT, String key, T value);

    T read(CompoundNBT compoundNBT, String key);

    void write(PacketBuffer buffer, T value);

    T read(PacketBuffer buffer);

    class StringAdaptor implements IDataAdaptor<String> {

        @Override
        public void write(@Nonnull CompoundNBT compoundNBT, String key, String value) {
            compoundNBT.putString(key, value);
        }

        @Override
        public String read(@Nonnull CompoundNBT compoundNBT, String key) {
            return compoundNBT.getString(key);
        }

        @Override
        public void write(@Nonnull PacketBuffer buffer, String value) {
            buffer.writeString(value, Short.MAX_VALUE);
        }

        @Override
        public String read(@Nonnull PacketBuffer buffer) {
            return buffer.readString(Short.MAX_VALUE);
        }
    }

    class UUIDAdaptor implements IDataAdaptor<UUID> {

        @Override
        public void write(@Nonnull CompoundNBT compoundNBT, String key, UUID value) {
            compoundNBT.putUniqueId(key, value);
        }

        @Override
        public UUID read(@Nonnull CompoundNBT compoundNBT, String key) {
            return compoundNBT.getUniqueId(key);
        }

        @Override
        public void write(@Nonnull PacketBuffer buffer, UUID value) {
            buffer.writeUniqueId(value);
        }

        @Override
        public UUID read(@Nonnull PacketBuffer buffer) {
            return buffer.readUniqueId();
        }
    }
}
