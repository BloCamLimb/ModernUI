/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.util;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The I/O methods of {@link DataSet DataSets}, performing encoding and decoding.
 * Common I/O interfaces are {@link DataInput} and {@link DataOutput},
 * where Strings are coded in Java modified UTF-8 format.
 */
@ParametersAreNonnullByDefault
public final class DataSetIO {

    private static final byte VAL_NULL = 0;
    private static final byte VAL_BYTE = 1;
    private static final byte VAL_SHORT = 2;
    private static final byte VAL_INT = 3;
    private static final byte VAL_LONG = 4;
    private static final byte VAL_FLOAT = 5;
    private static final byte VAL_DOUBLE = 6;
    private static final byte VAL_BYTE_ARRAY = 7;
    private static final byte VAL_SHORT_ARRAY = 8;
    private static final byte VAL_INT_ARRAY = 9;
    private static final byte VAL_LONG_ARRAY = 10;
    private static final byte VAL_FLOAT_ARRAY = 11;
    private static final byte VAL_DOUBLE_ARRAY = 12;
    private static final byte VAL_STRING = 13;
    private static final byte VAL_UUID = 14;
    private static final byte VAL_LIST = 15;
    private static final byte VAL_DATA_SET = 16;

    /**
     * Reads a compressed DataSet from a GNU zipped file.
     * <p>
     * The stream should be a FileInputStream or a FileChannel->ChannelInputStream,
     * and will be closed after the method call.
     *
     * @param stream the FileInputStream or FileChannel->ChannelInputStream
     * @return the newly inflated data set
     */
    @Nonnull
    public static DataSet inflate(InputStream stream) throws IOException {
        try (var input = new DataInputStream(new BufferedInputStream(new GZIPInputStream(stream, 4096), 4096))) {
            return readDataSet(input);
        }
    }

    /**
     * Writes and compresses a DataSet to a GNU zipped file.
     * <p>
     * The stream should be a FileOutputStream or a FileChannel->ChannelOutputStream,
     * and will be closed after the method call.
     *
     * @param source the data set to deflate
     * @param stream the FileOutputStream or FileChannel->ChannelOutputStream
     */
    public static void deflate(DataSet source, OutputStream stream) throws IOException {
        try (var output = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(stream, 4096), 4096))) {
            writeDataSet(source, output);
        }
    }

    /**
     * Write a list as a value.
     *
     * @param list   the list to write
     * @param output the data output
     * @throws IOException if an I/O error occurs
     */
    @SuppressWarnings("unchecked")
    private static void writeList(List<?> list, DataOutput output) throws IOException {
        final int size = list.size();
        if (list instanceof ByteArrayList) {
            output.writeByte(VAL_BYTE);
            output.writeInt(size);
            if (size > 0) {
                output.write(((ByteArrayList) list).elements(), 0, size);
            }
        } else if (list instanceof ShortArrayList) {
            output.writeByte(VAL_SHORT);
            output.writeInt(size);
            if (size > 0) {
                short[] data = ((ShortArrayList) list).elements();
                for (int i = 0; i < size; i++) {
                    output.writeShort(data[i]);
                }
            }
        } else if (list instanceof IntArrayList) {
            output.writeByte(VAL_INT);
            output.writeInt(size);
            if (size > 0) {
                int[] data = ((IntArrayList) list).elements();
                for (int i = 0; i < size; i++) {
                    output.writeInt(data[i]);
                }
            }
        } else if (list instanceof LongArrayList) {
            output.writeByte(VAL_LONG);
            output.writeInt(size);
            if (size > 0) {
                long[] data = ((LongArrayList) list).elements();
                for (int i = 0; i < size; i++) {
                    output.writeLong(data[i]);
                }
            }
        } else if (list instanceof FloatArrayList) {
            output.writeByte(VAL_FLOAT);
            output.writeInt(size);
            if (size > 0) {
                float[] data = ((FloatArrayList) list).elements();
                for (int i = 0; i < size; i++) {
                    output.writeFloat(data[i]);
                }
            }
        } else if (list instanceof DoubleArrayList) {
            output.writeByte(VAL_DOUBLE);
            output.writeInt(size);
            if (size > 0) {
                double[] data = ((DoubleArrayList) list).elements();
                for (int i = 0; i < size; i++) {
                    output.writeDouble(data[i]);
                }
            }
        } else if (size == 0) {
            output.writeByte(VAL_NULL);
        } else {
            final Object e = list.get(0);
            if (e instanceof byte[]) {
                output.writeByte(VAL_BYTE_ARRAY);
                output.writeInt(size);
                for (byte[] a : (List<byte[]>) list) {
                    output.writeInt(a.length);
                    output.write(a);
                }
            } else if (e instanceof short[]) {
                output.writeByte(VAL_SHORT_ARRAY);
                output.writeInt(size);
                for (short[] a : (List<short[]>) list) {
                    output.writeInt(a.length);
                    for (short s : a) {
                        output.writeShort(s);
                    }
                }
            } else if (e instanceof int[]) {
                output.writeByte(VAL_INT_ARRAY);
                output.writeInt(size);
                for (int[] a : (List<int[]>) list) {
                    output.writeInt(a.length);
                    for (int i : a) {
                        output.writeInt(i);
                    }
                }
            } else if (e instanceof long[]) {
                output.writeByte(VAL_LONG_ARRAY);
                output.writeInt(size);
                for (long[] a : (List<long[]>) list) {
                    output.writeInt(a.length);
                    for (long l : a) {
                        output.writeLong(l);
                    }
                }
            } else if (e instanceof float[]) {
                output.writeByte(VAL_FLOAT_ARRAY);
                output.writeInt(size);
                for (float[] a : (List<float[]>) list) {
                    output.writeInt(a.length);
                    for (float f : a) {
                        output.writeFloat(f);
                    }
                }
            } else if (e instanceof double[]) {
                output.writeByte(VAL_DOUBLE_ARRAY);
                output.writeInt(size);
                for (double[] a : (List<double[]>) list) {
                    output.writeInt(a.length);
                    for (double d : a) {
                        output.writeDouble(d);
                    }
                }
            } else if (e instanceof String) {
                output.writeByte(VAL_STRING);
                output.writeInt(size);
                for (String s : (List<String>) list) {
                    output.writeUTF(s);
                }
            } else if (e instanceof UUID) {
                output.writeByte(VAL_UUID);
                output.writeInt(size);
                for (UUID u : (List<UUID>) list) {
                    output.writeLong(u.getMostSignificantBits());
                    output.writeLong(u.getLeastSignificantBits());
                }
            } else if (e instanceof List<?>) {
                output.writeByte(VAL_LIST);
                output.writeInt(size);
                for (List<?> li : (List<List<?>>) list) {
                    writeList(li, output);
                }
            } else if (e instanceof DataSet) {
                output.writeByte(VAL_DATA_SET);
                output.writeInt(size);
                for (DataSet set : (List<DataSet>) list) {
                    writeDataSet(set, output);
                }
            } else {
                throw new IllegalStateException("Unknown element class " + e.getClass());
            }
        }
    }

    /**
     * Write a data set as a value.
     *
     * @param set    the set to write
     * @param output the data output
     * @throws IOException if an I/O error occurs
     */
    public static void writeDataSet(DataSet set, DataOutput output) throws IOException {
        for (var entry : set.mMap.entrySet()) {
            final Object v = entry.getValue();
            if (v instanceof Byte) {
                output.writeByte(VAL_BYTE);
                output.writeUTF(entry.getKey());
                output.writeByte((byte) v);
            } else if (v instanceof Short) {
                output.writeByte(VAL_SHORT);
                output.writeUTF(entry.getKey());
                output.writeShort((short) v);
            } else if (v instanceof Integer) {
                output.writeByte(VAL_INT);
                output.writeUTF(entry.getKey());
                output.writeInt((int) v);
            } else if (v instanceof Long) {
                output.writeByte(VAL_LONG);
                output.writeUTF(entry.getKey());
                output.writeLong((long) v);
            } else if (v instanceof Float) {
                output.writeByte(VAL_FLOAT);
                output.writeUTF(entry.getKey());
                output.writeFloat((float) v);
            } else if (v instanceof Double) {
                output.writeByte(VAL_DOUBLE);
                output.writeUTF(entry.getKey());
                output.writeDouble((double) v);
            } else if (v instanceof byte[] a) {
                output.writeByte(VAL_BYTE_ARRAY);
                output.writeUTF(entry.getKey());
                output.writeInt(a.length);
                output.write(a);
            } else if (v instanceof short[] a) {
                output.writeByte(VAL_SHORT_ARRAY);
                output.writeUTF(entry.getKey());
                output.writeInt(a.length);
                for (short s : a) {
                    output.writeShort(s);
                }
            } else if (v instanceof int[] a) {
                output.writeByte(VAL_INT_ARRAY);
                output.writeUTF(entry.getKey());
                output.writeInt(a.length);
                for (int i : a) {
                    output.writeInt(i);
                }
            } else if (v instanceof long[] a) {
                output.writeByte(VAL_LONG_ARRAY);
                output.writeUTF(entry.getKey());
                output.writeInt(a.length);
                for (long l : a) {
                    output.writeLong(l);
                }
            } else if (v instanceof float[] a) {
                output.writeByte(VAL_FLOAT_ARRAY);
                output.writeUTF(entry.getKey());
                output.writeInt(a.length);
                for (float f : a) {
                    output.writeFloat(f);
                }
            } else if (v instanceof double[] a) {
                output.writeByte(VAL_DOUBLE_ARRAY);
                output.writeUTF(entry.getKey());
                output.writeInt(a.length);
                for (double d : a) {
                    output.writeDouble(d);
                }
            } else if (v instanceof String) {
                output.writeByte(VAL_STRING);
                output.writeUTF(entry.getKey());
                output.writeUTF((String) v);
            } else if (v instanceof UUID u) {
                output.writeByte(VAL_UUID);
                output.writeUTF(entry.getKey());
                output.writeLong(u.getMostSignificantBits());
                output.writeLong(u.getLeastSignificantBits());
            } else if (v instanceof List<?>) {
                output.writeByte(VAL_LIST);
                output.writeUTF(entry.getKey());
                writeList((List<?>) v, output);
            } else if (v instanceof DataSet) {
                output.writeByte(VAL_DATA_SET);
                output.writeUTF(entry.getKey());
                writeDataSet((DataSet) v, output);
            } else {
                throw new IllegalStateException("Unknown value type " + v.getClass());
            }
        }
        output.writeByte(VAL_NULL);
    }

    /**
     * Read a list as a value.
     *
     * @param input the data input
     * @return the newly created list
     * @throws IOException if an I/O error occurs
     */
    @Nonnull
    private static List<?> readList(DataInput input) throws IOException {
        final byte id = input.readByte();
        if (id == VAL_NULL) {
            return new ArrayList<>();
        }
        final int len = input.readInt();
        switch (id) {
            case VAL_BYTE -> {
                ByteArrayList list = new ByteArrayList(len);
                for (int i = 0; i < len; i++) {
                    list.add(input.readByte());
                }
                return list;
            }
            case VAL_SHORT -> {
                ShortArrayList list = new ShortArrayList(len);
                for (int i = 0; i < len; i++) {
                    list.add(input.readShort());
                }
                return list;
            }
            case VAL_INT -> {
                IntArrayList list = new IntArrayList(len);
                for (int i = 0; i < len; i++) {
                    list.add(input.readInt());
                }
                return list;
            }
            case VAL_LONG -> {
                LongArrayList list = new LongArrayList(len);
                for (int i = 0; i < len; i++) {
                    list.add(input.readLong());
                }
                return list;
            }
            case VAL_FLOAT -> {
                FloatArrayList list = new FloatArrayList(len);
                for (int i = 0; i < len; i++) {
                    list.add(input.readFloat());
                }
                return list;
            }
            case VAL_DOUBLE -> {
                DoubleArrayList list = new DoubleArrayList(len);
                for (int i = 0; i < len; i++) {
                    list.add(input.readDouble());
                }
                return list;
            }
            case VAL_BYTE_ARRAY -> {
                ArrayList<byte[]> list = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    int l = input.readInt();
                    byte[] val = new byte[l];
                    input.readFully(val, 0, l);
                    list.add(val);
                }
                return list;
            }
            case VAL_SHORT_ARRAY -> {
                ArrayList<short[]> list = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    int l = input.readInt();
                    short[] val = new short[l];
                    for (int j = 0; j < l; j++) {
                        val[j] = input.readShort();
                    }
                    list.add(val);
                }
                return list;
            }
            case VAL_INT_ARRAY -> {
                ArrayList<int[]> list = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    int l = input.readInt();
                    int[] val = new int[l];
                    for (int j = 0; j < l; j++) {
                        val[j] = input.readInt();
                    }
                    list.add(val);
                }
                return list;
            }
            case VAL_LONG_ARRAY -> {
                ArrayList<long[]> list = new ArrayList<>(len);
                for (long i = 0; i < len; i++) {
                    int l = input.readInt();
                    long[] val = new long[l];
                    for (int j = 0; j < l; j++) {
                        val[j] = input.readLong();
                    }
                    list.add(val);
                }
                return list;
            }
            case VAL_FLOAT_ARRAY -> {
                ArrayList<float[]> list = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    int l = input.readInt();
                    float[] val = new float[l];
                    for (int j = 0; j < l; j++) {
                        val[j] = input.readFloat();
                    }
                    list.add(val);
                }
                return list;
            }
            case VAL_DOUBLE_ARRAY -> {
                ArrayList<double[]> list = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    int l = input.readInt();
                    double[] val = new double[l];
                    for (int j = 0; j < l; j++) {
                        val[j] = input.readDouble();
                    }
                    list.add(val);
                }
                return list;
            }
            case VAL_STRING -> {
                ArrayList<String> list = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    list.add(input.readUTF());
                }
                return list;
            }
            case VAL_UUID -> {
                ArrayList<UUID> list = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    list.add(new UUID(input.readLong(), input.readLong()));
                }
                return list;
            }
            case VAL_LIST -> {
                ArrayList<List<?>> list = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    list.add(readList(input));
                }
                return list;
            }
            case VAL_DATA_SET -> {
                ArrayList<DataSet> list = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    list.add(readDataSet(input));
                }
                return list;
            }
            default -> throw new IOException("Unknown element class identifier " + id);
        }
    }

    /**
     * Read a data set as a value.
     *
     * @param input the data input
     * @return the newly created data set
     * @throws IOException if an I/O error occurs
     */
    @Nonnull
    public static DataSet readDataSet(DataInput input) throws IOException {
        final DataSet set = new DataSet();
        final Map<String, Object> map = set.mMap;
        byte id;
        while ((id = input.readByte()) != VAL_NULL) {
            final String key = input.readUTF();
            switch (id) {
                case VAL_BYTE -> map.put(key, input.readByte());
                case VAL_SHORT -> map.put(key, input.readShort());
                case VAL_INT -> map.put(key, input.readInt());
                case VAL_LONG -> map.put(key, input.readLong());
                case VAL_FLOAT -> map.put(key, input.readFloat());
                case VAL_DOUBLE -> map.put(key, input.readDouble());
                case VAL_BYTE_ARRAY -> {
                    int len = input.readInt();
                    byte[] val = new byte[len];
                    input.readFully(val, 0, len);
                    map.put(key, val);
                }
                case VAL_SHORT_ARRAY -> {
                    int len = input.readInt();
                    short[] val = new short[len];
                    for (int i = 0; i < len; i++) {
                        val[i] = input.readShort();
                    }
                    map.put(key, val);
                }
                case VAL_INT_ARRAY -> {
                    int len = input.readInt();
                    int[] val = new int[len];
                    for (int i = 0; i < len; i++) {
                        val[i] = input.readInt();
                    }
                    map.put(key, val);
                }
                case VAL_LONG_ARRAY -> {
                    int len = input.readInt();
                    long[] val = new long[len];
                    for (int i = 0; i < len; i++) {
                        val[i] = input.readLong();
                    }
                    map.put(key, val);
                }
                case VAL_FLOAT_ARRAY -> {
                    int len = input.readInt();
                    float[] val = new float[len];
                    for (int i = 0; i < len; i++) {
                        val[i] = input.readFloat();
                    }
                    map.put(key, val);
                }
                case VAL_DOUBLE_ARRAY -> {
                    int len = input.readInt();
                    double[] val = new double[len];
                    for (int i = 0; i < len; i++) {
                        val[i] = input.readDouble();
                    }
                    map.put(key, val);
                }
                case VAL_STRING -> map.put(key, input.readUTF());
                case VAL_UUID -> map.put(key, new UUID(input.readLong(), input.readLong()));
                case VAL_LIST -> map.put(key, readList(input));
                case VAL_DATA_SET -> map.put(key, readDataSet(input));
                default -> throw new IOException("Unknown value type identifier " + id);
            }
        }
        return set;
    }
}
