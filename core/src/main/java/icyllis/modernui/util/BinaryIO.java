/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

import icyllis.modernui.text.TextUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Provides static methods to perform binary I/O.
 *
 * @since 3.7
 */
public final class BinaryIO {

    /**
     * Value types.
     */
    private static final byte VAL_NULL = 0;
    private static final byte
            VAL_BYTE = 1,
            VAL_SHORT = 2,
            VAL_INT = 3,
            VAL_LONG = 4,
            VAL_FLOAT = 5,
            VAL_DOUBLE = 6,
            VAL_BOOLEAN = 7,
            VAL_CHAR = 8;
    private static final byte
            VAL_BYTE_ARRAY = 9,
            VAL_SHORT_ARRAY = 10,
            VAL_INT_ARRAY = 11,
            VAL_LONG_ARRAY = 12,
            VAL_FLOAT_ARRAY = 13,
            VAL_DOUBLE_ARRAY = 14,
            VAL_BOOLEAN_ARRAY = 15,
            VAL_CHAR_ARRAY = 16;
    private static final byte
            VAL_STRING = 17,
            VAL_CHAR_SEQUENCE = 18,
            VAL_UUID = 19;
    private static final byte
            VAL_LIST = 20,
            VAL_DATA_SET = 21,
            VAL_FLATTENABLE = 22,
            VAL_OBJECT_ARRAY = 23,
            VAL_SERIALIZABLE = 24;

    /**
     * Reads a compressed DataSet from a GNU zipped file.
     * <p>
     * The stream should be a FileInputStream or a FileChannel->ChannelInputStream,
     * and will be closed after the method call.
     *
     * @param stream the FileInputStream or FileChannel->ChannelInputStream
     * @return the newly inflated data set
     */
    @Nullable
    public static DataSet inflate(@Nonnull InputStream stream,
                                  @Nullable ClassLoader loader) throws IOException {
        try (var input = new DataInputStream(
                new BufferedInputStream(new GZIPInputStream(stream, 4096), 4096))) {
            return readDataSet(input, loader);
        }
    }

    /**
     * Writes and compresses a DataSet to a GNU zipped file. The file can have no extension.
     * The standard extension is <code>.dat.gz</code> or <code>.gz</code>.
     * <p>
     * The stream should be a FileOutputStream or a FileChannel->ChannelOutputStream,
     * and will be closed after the method call.
     *
     * @param stream the FileOutputStream or FileChannel->ChannelOutputStream
     * @param source the data set to deflate
     */
    public static void deflate(@Nonnull OutputStream stream,
                               @Nonnull DataSet source) throws IOException {
        try (var output = new DataOutputStream(
                new BufferedOutputStream(new GZIPOutputStream(stream, 4096), 4096))) {
            writeDataSet(output, source);
        }
    }

    /**
     * Write a value and its type.
     *
     * @param out the data output
     * @param v   the value to write
     * @throws IOException if an IO error occurs
     */
    public static void writeValue(@Nonnull DataOutput out, @Nullable Object v) throws IOException {
        if (v == null) {
            out.writeByte(VAL_NULL);
        } else if (v instanceof String) {
            out.writeByte(VAL_STRING);
            writeString(out, (String) v);
        } else if (v instanceof Integer) {
            out.writeByte(VAL_INT);
            out.writeInt((Integer) v);
        } else if (v instanceof Long) {
            out.writeByte(VAL_LONG);
            out.writeLong((Long) v);
        } else if (v instanceof Float) {
            out.writeByte(VAL_FLOAT);
            out.writeFloat((Float) v);
        } else if (v instanceof Double) {
            out.writeByte(VAL_DOUBLE);
            out.writeDouble((Double) v);
        } else if (v instanceof Byte) {
            out.writeByte(VAL_BYTE);
            out.writeByte((Byte) v);
        } else if (v instanceof Short) {
            out.writeByte(VAL_SHORT);
            out.writeShort((Short) v);
        } else if (v instanceof Character) {
            out.writeByte(VAL_CHAR);
            out.writeChar((Character) v);
        } else if (v instanceof Boolean) {
            out.writeByte(VAL_BOOLEAN);
            out.writeBoolean((Boolean) v);
        } else if (v instanceof CharSequence) {
            out.writeByte(VAL_CHAR_SEQUENCE);
            TextUtils.write(out, (CharSequence) v);
        } else if (v instanceof UUID value) {
            out.writeByte(VAL_UUID);
            out.writeLong(value.getMostSignificantBits());
            out.writeLong(value.getLeastSignificantBits());
        } else if (v instanceof byte[]) {
            out.writeByte(VAL_BYTE_ARRAY);
            writeByteArray(out, (byte[]) v);
        } else if (v instanceof char[]) {
            out.writeByte(VAL_CHAR_ARRAY);
            writeCharArray(out, (char[]) v);
        } else if (v instanceof List) {
            out.writeByte(VAL_LIST);
            writeList(out, (List<?>) v);
        } else if (v instanceof DataSet) {
            out.writeByte(VAL_DATA_SET);
            writeDataSet(out, (DataSet) v);
        } else if (v instanceof Flattenable value) {
            out.writeByte(VAL_FLATTENABLE);
            writeString(out, value.getClass().getName());
            value.write(out);
        } else if (v instanceof int[]) {
            out.writeByte(VAL_INT_ARRAY);
            writeIntArray(out, (int[]) v);
        } else if (v instanceof long[]) {
            out.writeByte(VAL_LONG_ARRAY);
            writeLongArray(out, (long[]) v);
        } else if (v instanceof short[]) {
            out.writeByte(VAL_SHORT_ARRAY);
            writeShortArray(out, (short[]) v);
        } else if (v instanceof float[]) {
            out.writeByte(VAL_FLOAT_ARRAY);
            writeFloatArray(out, (float[]) v);
        } else if (v instanceof double[]) {
            out.writeByte(VAL_DOUBLE_ARRAY);
            writeDoubleArray(out, (double[]) v);
        } else if (v instanceof boolean[]) {
            out.writeByte(VAL_BOOLEAN_ARRAY);
            writeBooleanArray(out, (boolean[]) v);
        } else {
            Class<?> clazz = v.getClass();
            if (clazz.isArray() && clazz.getComponentType() == Object.class) {
                out.writeByte(VAL_OBJECT_ARRAY);
                writeArray(out, (Object[]) v);
            } else if (v instanceof Serializable value) {
                out.writeByte(VAL_SERIALIZABLE);
                writeString(out, value.getClass().getName());
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeObject(value);
                oos.close();
                writeByteArray(out, os.toByteArray());
            }
            // others are silently ignored
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T readValue(@Nonnull DataInput in, @Nullable ClassLoader loader,
                                  @Nullable Class<T> clazz, @Nullable Class<?> itemType) throws IOException {
        final byte type = in.readByte();
        final Object object = switch (type) {
            case VAL_NULL -> null;
            case VAL_BYTE -> in.readByte();
            case VAL_SHORT -> in.readShort();
            case VAL_INT -> in.readInt();
            case VAL_LONG -> in.readLong();
            case VAL_FLOAT -> in.readFloat();
            case VAL_DOUBLE -> in.readDouble();
            case VAL_BOOLEAN -> in.readBoolean();
            case VAL_CHAR -> in.readChar();
            case VAL_BYTE_ARRAY -> readByteArray(in);
            case VAL_SHORT_ARRAY -> readShortArray(in);
            case VAL_INT_ARRAY -> readIntArray(in);
            case VAL_LONG_ARRAY -> readLongArray(in);
            case VAL_FLOAT_ARRAY -> readFloatArray(in);
            case VAL_DOUBLE_ARRAY -> readDoubleArray(in);
            case VAL_BOOLEAN_ARRAY -> readBooleanArray(in);
            case VAL_CHAR_ARRAY -> readCharArray(in);
            case VAL_STRING -> readString(in);
            case VAL_CHAR_SEQUENCE -> TextUtils.read(in);
            case VAL_UUID -> new UUID(in.readLong(), in.readLong());
            case VAL_LIST -> readList(in, loader, itemType);
            case VAL_DATA_SET -> readDataSet(in, loader);
            case VAL_OBJECT_ARRAY -> {
                if (itemType == null) {
                    itemType = Object.class;
                }
                if (clazz != null) {
                    if (!clazz.isArray()) {
                        throw new IOException("About to read an array but type "
                                + clazz.getCanonicalName()
                                + " required by caller is not an array.");
                    }
                    Class<?> itemArrayType = itemType.arrayType();
                    if (!clazz.isAssignableFrom(itemArrayType)) {
                        throw new IOException("About to read a " + itemArrayType.getCanonicalName()
                                + ", which is not a subtype of type " + clazz.getCanonicalName()
                                + " required by caller.");
                    }
                }
                yield readArray(in, loader, itemType);
            }
            default -> throw new IOException("Unknown value type identifier: " + type);
        };
        if (object != null && clazz != null && !clazz.isInstance(object)) {
            throw new IOException("Deserialized object " + object
                    + " is not an instance of required class " + clazz.getName()
                    + " provided in the parameter");
        }
        return (T) object;
    }

    /**
     * Write a byte array.
     *
     * @param out the data output
     * @param b   the bytes to write
     * @throws IOException if an IO error occurs
     */
    public static void writeByteArray(@Nonnull DataOutput out,
                                      @Nullable byte[] b) throws IOException {
        if (b == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(b.length);
        out.write(b);
    }

    /**
     * Write a byte array.
     *
     * @param out the data output
     * @param b   the bytes to write
     * @throws IOException if an IO error occurs
     */
    public static void writeByteArray(@Nonnull DataOutput out,
                                      @Nullable byte[] b, int off, int len) throws IOException {
        if (b == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(len);
        out.write(b, off, len);
    }

    @Nullable
    public static byte[] readByteArray(@Nonnull DataInput in) throws IOException {
        int n = in.readInt();
        if (n < 0)
            return null;
        byte[] b = new byte[n];
        in.readFully(b, 0, n);
        return b;
    }

    /**
     * Write a short array.
     *
     * @param out   the data output
     * @param value the short array to write
     * @throws IOException if an IO error occurs
     */
    public static void writeShortArray(@Nonnull DataOutput out,
                                       @Nullable short[] value) throws IOException {
        if (value == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(value.length);
        for (short e : value)
            out.writeShort(e);
    }

    @Nullable
    public static short[] readShortArray(@Nonnull DataInput in) throws IOException {
        int n = in.readInt();
        if (n < 0)
            return null;
        short[] value = new short[n];
        for (int i = 0; i < n; i++)
            value[i] = in.readShort();
        return value;
    }

    /**
     * Write an int array.
     *
     * @param out   the data output
     * @param value the int array to write
     * @throws IOException if an IO error occurs
     */
    public static void writeIntArray(@Nonnull DataOutput out,
                                     @Nullable int[] value) throws IOException {
        if (value == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(value.length);
        for (int e : value)
            out.writeInt(e);
    }

    @Nullable
    public static int[] readIntArray(@Nonnull DataInput in) throws IOException {
        int n = in.readInt();
        if (n < 0)
            return null;
        int[] value = new int[n];
        for (int i = 0; i < n; i++)
            value[i] = in.readInt();
        return value;
    }

    /**
     * Write a long array.
     *
     * @param out   the data output
     * @param value the long array to write
     * @throws IOException if an IO error occurs
     */
    public static void writeLongArray(@Nonnull DataOutput out,
                                      @Nullable long[] value) throws IOException {
        if (value == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(value.length);
        for (long e : value)
            out.writeLong(e);
    }

    @Nullable
    public static long[] readLongArray(@Nonnull DataInput in) throws IOException {
        int n = in.readInt();
        if (n < 0)
            return null;
        long[] value = new long[n];
        for (int i = 0; i < n; i++)
            value[i] = in.readLong();
        return value;
    }

    /**
     * Write a float array.
     *
     * @param out   the data output
     * @param value the float array to write
     * @throws IOException if an IO error occurs
     */
    public static void writeFloatArray(@Nonnull DataOutput out,
                                       @Nullable float[] value) throws IOException {
        if (value == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(value.length);
        for (float e : value)
            out.writeFloat(e);
    }

    @Nullable
    public static float[] readFloatArray(@Nonnull DataInput in) throws IOException {
        int n = in.readInt();
        if (n < 0)
            return null;
        float[] value = new float[n];
        for (int i = 0; i < n; i++)
            value[i] = in.readFloat();
        return value;
    }

    /**
     * Write a double array.
     *
     * @param out   the data output
     * @param value the double array to write
     * @throws IOException if an IO error occurs
     */
    public static void writeDoubleArray(@Nonnull DataOutput out,
                                        @Nullable double[] value) throws IOException {
        if (value == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(value.length);
        for (double e : value)
            out.writeDouble(e);
    }

    @Nullable
    public static double[] readDoubleArray(@Nonnull DataInput in) throws IOException {
        int n = in.readInt();
        if (n < 0)
            return null;
        double[] value = new double[n];
        for (int i = 0; i < n; i++)
            value[i] = in.readDouble();
        return value;
    }

    /**
     * Write a boolean array.
     *
     * @param out   the data output
     * @param value the boolean array to write
     * @throws IOException if an IO error occurs
     */
    public static void writeBooleanArray(@Nonnull DataOutput out,
                                         @Nullable boolean[] value) throws IOException {
        if (value == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(value.length);
        for (boolean e : value)
            out.writeBoolean(e);
    }

    @Nullable
    public static boolean[] readBooleanArray(@Nonnull DataInput in) throws IOException {
        int n = in.readInt();
        if (n < 0)
            return null;
        boolean[] value = new boolean[n];
        for (int i = 0; i < n; i++)
            value[i] = in.readBoolean();
        return value;
    }

    /**
     * Write a char array.
     *
     * @param out   the data output
     * @param value the char array to write
     * @throws IOException if an IO error occurs
     */
    public static void writeCharArray(@Nonnull DataOutput out,
                                      @Nullable char[] value) throws IOException {
        if (value == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(value.length);
        for (char e : value)
            out.writeChar(e);
    }

    @Nullable
    public static char[] readCharArray(@Nonnull DataInput in) throws IOException {
        int n = in.readInt();
        if (n < 0)
            return null;
        char[] value = new char[n];
        for (int i = 0; i < n; i++)
            value[i] = in.readChar();
        return value;
    }

    /**
     * Write an object array.
     *
     * @param out the data output
     * @param a   the object array to write
     * @throws IOException if an IO error occurs
     */
    public static void writeArray(@Nonnull DataOutput out,
                                  @Nullable Object[] a) throws IOException {
        if (a == null) {
            out.writeInt(-1);
            return;
        }
        int n = a.length, i = 0;
        out.writeInt(n);
        while (i < n)
            writeValue(out, a[i++]);
    }

    /**
     * Read an object array.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T[] readArray(@Nonnull DataInput in, @Nullable ClassLoader loader,
                                    @Nonnull Class<T> clazz) throws IOException {
        int n = in.readInt();
        if (n < 0)
            return null;
        T[] a = (T[]) (clazz == Object.class ? new Object[n] : Array.newInstance(clazz, n));
        for (int i = 0; i < n; i++) {
            T value = readValue(in, loader, clazz, null);
            a[i] = value;
        }
        return a;
    }

    /**
     * Write a string.
     *
     * @param out the data output
     * @param s   the string to write
     * @throws IOException if an IO error occurs
     */
    public static void writeString(@Nonnull DataOutput out, @Nullable String s) throws IOException {
        writeString16(out, s);
    }

    /**
     * Write a string in UTF-8 format.
     *
     * @param out the data output
     * @param s   the string to write
     * @throws IOException if an IO error occurs
     */
    public static void writeString8(@Nonnull DataOutput out, @Nullable String s) throws IOException {
        if (s == null) {
            out.writeInt(-1);
        } else {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            out.writeInt(bytes.length);
            out.write(bytes);
        }
    }

    /**
     * Write a string in UTF-16 BE format.
     *
     * @param out the data output
     * @param s   the string to write
     * @throws IOException if an IO error occurs
     */
    public static void writeString16(@Nonnull DataOutput out, @Nullable String s) throws IOException {
        if (s == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(s.length());
            out.writeChars(s);
        }
    }

    /**
     * Read a string.
     *
     * @param in the data input
     * @throws IOException if an IO error occurs
     */
    @Nullable
    public static String readString(@Nonnull DataInput in) throws IOException {
        return readString16(in);
    }

    /**
     * Read a string in UTF-8 format.
     *
     * @param in the data input
     * @throws IOException if an IO error occurs
     */
    @Nullable
    public static String readString8(@Nonnull DataInput in) throws IOException {
        int n = in.readInt();
        if (n < 0)
            return null;
        byte[] bytes = new byte[n];
        in.readFully(bytes, 0, n);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Read a string in UTF-16 BE format.
     *
     * @param in the data input
     * @throws IOException if an IO error occurs
     */
    @Nullable
    public static String readString16(@Nonnull DataInput in) throws IOException {
        int n = in.readInt();
        if (n < 0)
            return null;
        char[] value = new char[n];
        for (int i = 0; i < n; i++)
            value[i] = in.readChar();
        return new String(value);
    }

    /**
     * Write a list.
     *
     * @param out the data output
     * @param ls  the list to write
     * @throws IOException if an IO error occurs
     */
    public static void writeList(@Nonnull DataOutput out,
                                 @Nullable List<?> ls) throws IOException {
        if (ls == null) {
            out.writeInt(-1);
            return;
        }
        int n = ls.size(), i = 0;
        out.writeInt(n);
        while (i < n)
            writeValue(out, ls.get(i++));
    }

    /**
     * Read a list as a value.
     *
     * @param in the data input
     * @return the newly created list
     * @throws IOException if an IO error occurs
     */
    @Nullable
    private static <T> List<T> readList(@Nonnull DataInput in, @Nullable ClassLoader loader,
                                        @Nullable Class<? extends T> clazz) throws IOException {
        int n = in.readInt();
        if (n < 0)
            return null;
        List<T> ls = new ArrayList<>(n);
        while (n-- > 0)
            ls.add(readValue(in, loader, clazz, null));
        return ls;
    }

    /**
     * Write a data set.
     *
     * @param out the data output
     * @param ds  the data set to write
     * @throws IOException if an IO error occurs
     */
    public static void writeDataSet(@Nonnull DataOutput out,
                                    @Nullable DataSet ds) throws IOException {
        if (ds == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(ds.size());
        var it = ds.new FastEntryIterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            writeString(out, e.getKey());
            writeValue(out, e.getValue());
        }
    }

    /**
     * Read a data set as a value.
     *
     * @param in the data input
     * @return the newly created data set
     * @throws IOException if an IO error occurs
     */
    @Nullable
    public static DataSet readDataSet(@Nonnull DataInput in,
                                      @Nullable ClassLoader loader) throws IOException {
        int n = in.readInt();
        if (n < 0)
            return null;
        DataSet ds = new DataSet(n);
        while (n-- > 0)
            ds.put(readString(in), readValue(in, loader, null, null));
        return ds;
    }
}
