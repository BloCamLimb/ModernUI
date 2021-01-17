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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Used to store NBT on server and sync data to client in a fast way.
 *
 * @since 2.1
 */
public class UniDataManager {

    private static final Map<Class<?>, IDataAdaptor<?>> adaptorMap = new HashMap<>();

    static {
        adaptorMap.put(String.class, new IDataAdaptor.StringAdaptor());
        adaptorMap.put(UUID.class, new IDataAdaptor.UUIDAdaptor());
    }

    private List<List<DataValue>> groups = new ArrayList<>();

    private List<MutableInt> dirty = new ArrayList<>();

    public void writeAllNBT(CompoundNBT compoundNBT) {
        groups.forEach(l -> l.forEach(d -> d.write(compoundNBT)));
    }

    public void writeNBT(CompoundNBT compoundNBT, int groupIndex) {
        groups.get(groupIndex).forEach(d -> d.write(compoundNBT));
    }

    public void readAllNBT(CompoundNBT compoundNBT) {
        groups.forEach(l -> l.forEach(d -> d.read(compoundNBT)));
    }

    public void readNBT(CompoundNBT compoundNBT, int groupIndex) {
        groups.get(groupIndex).forEach(d -> d.read(compoundNBT));
    }

    public void writeAllData(PacketBuffer buffer, boolean forceWrite) {
        for (int i = 0; i < groups.size(); i++) {
            writeData(buffer, i, forceWrite);
        }
    }

    public void writeData(@Nonnull PacketBuffer buffer, int groupIndex, boolean forceWrite) {
        MutableInt l = this.dirty.get(groupIndex);
        int dirty = forceWrite ? -1 : l.intValue();
        l.setValue(0);
        buffer.writeVarInt(dirty);
        if (dirty != 0) {
            List<DataValue> list = groups.get(groupIndex);
            for (int i = 0; i < list.size(); i++) {
                if ((dirty & (1 << i)) != 0) {
                    list.get(i).write(buffer);
                }
            }
        }
    }

    public void readAllData(@Nonnull PacketBuffer buffer) {
        for (int i = 0; i < groups.size(); i++) {
            readData(buffer, i);
        }
    }

    public void readData(@Nonnull PacketBuffer buffer, int groupIndex) {
        int dirty = buffer.readVarInt();
        if (dirty != 0) {
            List<DataValue> list = groups.get(groupIndex);
            for (int i = 0; i < list.size(); i++) {
                if ((dirty & (1 << i)) != 0) {
                    list.get(i).read(buffer);
                }
            }
        }
    }

    public ByteDataValue createByte(int groupIndex, @Nullable String key) {
        List<DataValue> list = checkGroupIndex(groupIndex);
        int mask = 1 << list.size();
        MutableInt l = dirty.get(groupIndex);
        ByteDataValue dataValue;
        list.add(dataValue = new ByteDataValue(() -> l.setValue(l.intValue() | mask), key));
        return dataValue;
    }

    public ShortDataValue createShort(int groupIndex, @Nullable String key) {
        List<DataValue> list = checkGroupIndex(groupIndex);
        int mask = 1 << list.size();
        MutableInt l = dirty.get(groupIndex);
        ShortDataValue dataValue;
        list.add(dataValue = new ShortDataValue(() -> l.setValue(l.intValue() | mask), key));
        return dataValue;
    }

    public IntDataValue createInt(int groupIndex, @Nullable String key) {
        List<DataValue> list = checkGroupIndex(groupIndex);
        int mask = 1 << list.size();
        MutableInt l = dirty.get(groupIndex);
        IntDataValue dataValue;
        list.add(dataValue = new IntDataValue(() -> l.setValue(l.intValue() | mask), key));
        return dataValue;
    }

    public LongDataValue createLong(int groupIndex, @Nullable String key) {
        List<DataValue> list = checkGroupIndex(groupIndex);
        int mask = 1 << list.size();
        MutableInt l = dirty.get(groupIndex);
        LongDataValue dataValue;
        list.add(dataValue = new LongDataValue(() -> l.setValue(l.intValue() | mask), key));
        return dataValue;
    }

    public FloatDataValue createFloat(int groupIndex, @Nullable String key) {
        List<DataValue> list = checkGroupIndex(groupIndex);
        int mask = 1 << list.size();
        MutableInt l = dirty.get(groupIndex);
        FloatDataValue dataValue;
        list.add(dataValue = new FloatDataValue(() -> l.setValue(l.intValue() | mask), key));
        return dataValue;
    }

    public DoubleDataValue createDouble(int groupIndex, @Nullable String key) {
        List<DataValue> list = checkGroupIndex(groupIndex);
        int mask = 1 << list.size();
        MutableInt l = dirty.get(groupIndex);
        DoubleDataValue dataValue;
        list.add(dataValue = new DoubleDataValue(() -> l.setValue(l.intValue() | mask), key));
        return dataValue;
    }

    @SuppressWarnings("unchecked")
    public <T> ObjectDataValue<T> createObject(Class<T> type, int groupIndex, @Nullable String key) {
        IDataAdaptor<T> adaptor = (IDataAdaptor<T>) adaptorMap.get(type);
        Objects.requireNonNull(adaptor);
        List<DataValue> list = checkGroupIndex(groupIndex);
        int mask = 1 << list.size();
        MutableInt l = dirty.get(groupIndex);
        ObjectDataValue<T> dataValue;
        list.add(dataValue = new ObjectDataValue<>(() -> l.setValue(l.intValue() | mask), key, adaptor));
        return dataValue;
    }

    @Nonnull
    private List<DataValue> checkGroupIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException();
        }
        final int size = groups.size();
        if (size > index) {
            throw new IllegalStateException();
        } else if (size == index) {
            groups.add(new ObjectArrayList<>());
            dirty.add(new MutableInt());
        }
        List<DataValue> list = groups.get(index);
        if (list.size() == 32) {
            throw new IllegalStateException();
        }
        return list;
    }

    public static <T> void registerAdaptor(Class<T> type, IDataAdaptor<T> adaptor) {
        IDataAdaptor<?> p = adaptorMap.put(type, adaptor);
        if (p != null) {
            throw new IllegalStateException("Duplicated adaptor with type " + type);
        }
    }
}
