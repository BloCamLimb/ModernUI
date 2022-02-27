/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.forge;

import icyllis.modernui.util.DataSet;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contains {@link DataSet} and {@link CompoundTag} utilities.
 */
@SuppressWarnings("unchecked")
public final class DsNbtUtils {

    private DsNbtUtils() {
    }

    /**
     * Write the data set to the given byte buf.
     *
     * @param source the source data set
     * @param buf    the target byte buf
     * @return the byte buf as a convenience
     */
    @Nonnull
    public static FriendlyByteBuf writeDataSet(@Nonnull DataSet source, @Nonnull FriendlyByteBuf buf) {
        try {
            DataSet.writeDataSet(source, new ByteBufOutputStream(buf));
        } catch (IOException e) {
            throw new EncoderException(e);
        }
        return buf;
    }

    /**
     * Read a data set from the given byte buf.
     *
     * @param buf the source byte buf
     * @return the decoded data set
     */
    @Nonnull
    public static DataSet readDataSet(@Nonnull FriendlyByteBuf buf) {
        try {
            return DataSet.readDataSet(new ByteBufInputStream(buf));
        } catch (IOException e) {
            throw new DecoderException(e);
        }
    }

    /**
     * Write only the string mapping of the given data set to the target compound tag.
     * ByteList, IntList and LongList will be converted to their ArrayTags. If one of
     * these lists nested in the parent list, then it will be silently ignored.
     *
     * @param source the source data set
     * @param tag    the target compound tag
     * @return the compound tag as a convenience
     */
    @Nonnull
    public static CompoundTag writeDataSet(@Nonnull DataSet source, @Nonnull CompoundTag tag) {
        Iterator<Map.Entry<String, Object>> it = source.stringEntryIterator();
        if (it != null) {
            while (it.hasNext()) {
                final Map.Entry<String, Object> entry = it.next();
                final Object v = entry.getValue();
                if (v instanceof Byte) {
                    tag.putByte(entry.getKey(), (byte) v);
                } else if (v instanceof Short) {
                    tag.putShort(entry.getKey(), (short) v);
                } else if (v instanceof Integer) {
                    tag.putInt(entry.getKey(), (int) v);
                } else if (v instanceof Long) {
                    tag.putLong(entry.getKey(), (long) v);
                } else if (v instanceof Float) {
                    tag.putFloat(entry.getKey(), (float) v);
                } else if (v instanceof Double) {
                    tag.putDouble(entry.getKey(), (double) v);
                } else if (v instanceof String) {
                    tag.putString(entry.getKey(), (String) v);
                } else if (v instanceof UUID) {
                    tag.putUUID(entry.getKey(), (UUID) v);
                } else if (v instanceof ByteArrayList) {
                    tag.putByteArray(entry.getKey(), ((ByteArrayList) v).toByteArray());
                } else if (v instanceof IntArrayList) {
                    tag.putIntArray(entry.getKey(), ((IntArrayList) v).toIntArray());
                } else if (v instanceof LongArrayList) {
                    tag.putLongArray(entry.getKey(), ((LongArrayList) v).toLongArray());
                } else if (v instanceof List) {
                    tag.put(entry.getKey(), writeList((List<?>) v, new ListTag()));
                } else if (v instanceof DataSet) {
                    tag.put(entry.getKey(), writeDataSet((DataSet) v, new CompoundTag()));
                }
            }
        }
        return tag;
    }

    @Nonnull
    private static ListTag writeList(@Nonnull List<?> list, @Nonnull ListTag tag) {
        if (list.isEmpty()) {
            return tag;
        }
        if (list instanceof ShortArrayList) {
            for (short v : (ShortArrayList) list) {
                tag.add(ShortTag.valueOf(v));
            }
        } else if (list instanceof FloatArrayList) {
            for (float v : (FloatArrayList) list) {
                tag.add(FloatTag.valueOf(v));
            }
        } else if (list instanceof DoubleArrayList) {
            for (double v : (DoubleArrayList) list) {
                tag.add(DoubleTag.valueOf(v));
            }
        } else {
            final Object e = list.get(0);
            if (e instanceof String) {
                for (String s : (List<String>) list) {
                    tag.add(StringTag.valueOf(s));
                }
            } else if (e instanceof UUID) {
                for (UUID u : (List<UUID>) list) {
                    tag.add(NbtUtils.createUUID(u));
                }
            } else if (e instanceof List) {
                for (List<?> li : (List<List<?>>) list) {
                    tag.add(writeList(li, new ListTag()));
                }
            } else if (e instanceof DataSet) {
                for (DataSet set : (List<DataSet>) list) {
                    tag.add(writeDataSet(set, new CompoundTag()));
                }
            }
        }
        return tag;
    }
}
