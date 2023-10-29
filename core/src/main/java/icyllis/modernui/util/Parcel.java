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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.text.TextUtils;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A Parcel is a container for a sequence of bytes, or an abstract view of NIO
 * ByteBuffer, Netty ByteBuf, that performs non-blocking binary I/O on various
 * data objects.
 * <p>
 * Parcel provides methods for converting arbitrary objects to and from binaries.
 * This is mainly used for in-memory communication between activities, network
 * communication between clients, and inter-process communication. It may not be
 * ideal for persistent storage.
 *
 * @see Parcelable
 * @since 3.9
 */
//TODO review
public abstract class Parcel {

    /**
     * Value types, version 3.7, do not change.
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
            VAL_UUID = 19,
            VAL_INSTANT = 20;
    private static final byte
            VAL_DATA_SET = 64,
            VAL_PARCELABLE = 65,
            VAL_CHAR_SEQUENCE = 66,
            VAL_LIST = 68,
            VAL_OBJECT_ARRAY = 118,
            VAL_SERIALIZABLE = 127;

    // Modern UI: ConcurrentHashMap for better performance
    private static final ConcurrentHashMap<ClassLoader, ConcurrentHashMap<String, Parcelable.Creator<?>>>
            gCreators = new ConcurrentHashMap<>();

    public Parcel() {
    }

    /**
     * Reads a compressed DataSet from a GZIP file.
     * <p>
     * The stream should be a FileInputStream or a ChannelInputStream over FileChannel,
     * and will be closed after the method call.
     *
     * @param stream a FileInputStream or a ChannelInputStream over FileChannel
     * @return the data set
     */
    @NonNull
    public static DataSet inflate(@NonNull InputStream stream,
                                  @Nullable ClassLoader loader) throws IOException {
        try (var in = new InputStreamParcel(new GZIPInputStream(
                new BufferedInputStream(stream, 4096)))) {
            var res = in.readDataSet(loader);
            if (res == null) {
                throw new IOException("Insufficient data");
            }
            return res;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException ioe) {
                throw ioe;
            }
            throw e;
        }
    }

    /**
     * Writes and compresses a DataSet to a GZIP file.
     * <p>
     * The stream should be a FileOutputStream or a ChannelOutputStream over FileChannel,
     * and will be closed after the method call.
     *
     * @param stream a FileOutputStream or a ChannelOutputStream over FileChannel
     * @param source the data set
     */
    public static void deflate(@NonNull OutputStream stream,
                               @NonNull DataSet source) throws IOException {
        try (var out = new OutputStreamParcel(new GZIPOutputStream(
                new BufferedOutputStream(stream, 4096)))) {
            out.writeDataSet(source);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException ioe) {
                throw ioe;
            }
            throw e;
        }
    }

    public void writeBytes(byte[] src) {
        writeBytes(src, 0, src.length);
    }

    public abstract void writeBytes(byte[] src, int off, int len);

    public void writeBoolean(boolean b) {
        writeByte(b ? 1 : 0);
    }

    public abstract void writeByte(int v);

    public abstract void writeShort(int v);

    public abstract void writeInt(int v);

    public abstract void writeLong(long v);

    public abstract void writeChar(int v);

    public abstract void writeFloat(float v);

    public abstract void writeDouble(double v);

    public void readBytes(byte[] dst) {
        readBytes(dst, 0, dst.length);
    }

    public abstract void readBytes(byte[] dst, int off, int len);

    public boolean readBoolean() {
        return readByte() != 0;
    }

    public abstract byte readByte();

    public abstract short readShort();

    public abstract int readInt();

    public abstract long readLong();

    public abstract char readChar();

    public abstract float readFloat();

    public abstract double readDouble();

    /**
     * Write a value and its type.
     *
     * @param v the value to write
     */
    public void writeValue(@Nullable Object v) {
        if (v == null) {
            writeByte(VAL_NULL);
        } else if (v instanceof String) {
            writeByte(VAL_STRING);
            writeString((String) v);
        } else if (v instanceof Integer) {
            writeByte(VAL_INT);
            writeInt((Integer) v);
        } else if (v instanceof Long) {
            writeByte(VAL_LONG);
            writeLong((Long) v);
        } else if (v instanceof Float) {
            writeByte(VAL_FLOAT);
            writeFloat((Float) v);
        } else if (v instanceof Double) {
            writeByte(VAL_DOUBLE);
            writeDouble((Double) v);
        } else if (v instanceof Byte) {
            writeByte(VAL_BYTE);
            writeByte((Byte) v);
        } else if (v instanceof Short) {
            writeByte(VAL_SHORT);
            writeShort((Short) v);
        } else if (v instanceof Character) {
            writeByte(VAL_CHAR);
            writeChar((Character) v);
        } else if (v instanceof Boolean) {
            writeByte(VAL_BOOLEAN);
            writeBoolean((Boolean) v);
        } else if (v instanceof UUID value) {
            writeByte(VAL_UUID);
            writeLong(value.getMostSignificantBits());
            writeLong(value.getLeastSignificantBits());
        } else if (v instanceof Instant value) {
            writeByte(VAL_INSTANT);
            writeLong(value.getEpochSecond());
            writeInt(value.getNano());
        } else if (v instanceof int[]) {
            writeByte(VAL_INT_ARRAY);
            writeIntArray((int[]) v);
        } else if (v instanceof byte[]) {
            writeByte(VAL_BYTE_ARRAY);
            writeByteArray((byte[]) v);
        } else if (v instanceof char[]) {
            writeByte(VAL_CHAR_ARRAY);
            writeCharArray((char[]) v);
        } else if (v instanceof DataSet) {
            writeByte(VAL_DATA_SET);
            writeDataSet((DataSet) v);
        } else if (v instanceof Parcelable value) {
            writeByte(VAL_PARCELABLE);
            writeString(value.getClass().getName());
            value.writeToParcel(this, 0);
        } else if (v instanceof CharSequence) {
            writeByte(VAL_CHAR_SEQUENCE);
            TextUtils.writeToParcel((CharSequence) v, this, 0);
        } else if (v instanceof List) {
            writeByte(VAL_LIST);
            writeList((List<?>) v);
        } else if (v instanceof long[]) {
            writeByte(VAL_LONG_ARRAY);
            writeLongArray((long[]) v);
        } else if (v instanceof short[]) {
            writeByte(VAL_SHORT_ARRAY);
            writeShortArray((short[]) v);
        } else if (v instanceof float[]) {
            writeByte(VAL_FLOAT_ARRAY);
            writeFloatArray((float[]) v);
        } else if (v instanceof double[]) {
            writeByte(VAL_DOUBLE_ARRAY);
            writeDoubleArray((double[]) v);
        } else if (v instanceof boolean[]) {
            writeByte(VAL_BOOLEAN_ARRAY);
            writeBooleanArray((boolean[]) v);
        } else {
            Class<?> clazz = v.getClass();
            if (clazz.isArray() && clazz.getComponentType() == Object.class) {
                // pure Object[]
                writeByte(VAL_OBJECT_ARRAY);
                writeArray((Object[]) v);
            } else if (v instanceof Serializable value) {
                writeByte(VAL_SERIALIZABLE);
                String name = value.getClass().getName();
                writeString(name);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(value);
                    oos.close();
                    writeByteArray(baos.toByteArray());
                } catch (IOException ioe) {
                    throw new BadParcelableException("Parcelable encountered "
                            + "IOException writing serializable object (name = "
                            + name + ")", ioe);
                }
            }
            // others are silently ignored
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T readValue(@Nullable ClassLoader loader,
                           @Nullable Class<T> clazz, @Nullable Class<?> elemType) {
        final byte type = readByte();
        final Object object = switch (type) {
            case VAL_NULL -> null;
            case VAL_BYTE -> readByte();
            case VAL_SHORT -> readShort();
            case VAL_INT -> readInt();
            case VAL_LONG -> readLong();
            case VAL_FLOAT -> readFloat();
            case VAL_DOUBLE -> readDouble();
            case VAL_BOOLEAN -> readBoolean();
            case VAL_CHAR -> readChar();
            case VAL_BYTE_ARRAY -> readByteArray();
            case VAL_SHORT_ARRAY -> readShortArray();
            case VAL_INT_ARRAY -> readIntArray();
            case VAL_LONG_ARRAY -> readLongArray();
            case VAL_FLOAT_ARRAY -> readFloatArray();
            case VAL_DOUBLE_ARRAY -> readDoubleArray();
            case VAL_BOOLEAN_ARRAY -> readBooleanArray();
            case VAL_CHAR_ARRAY -> readCharArray();
            case VAL_STRING -> readString();
            case VAL_UUID -> new UUID(readLong(), readLong());
            case VAL_INSTANT -> Instant.ofEpochSecond(readLong(), readInt());
            case VAL_DATA_SET -> readDataSet(loader);
            case VAL_PARCELABLE -> readParcelable0(loader, clazz);
            case VAL_CHAR_SEQUENCE -> TextUtils.createFromParcel(this);
            case VAL_LIST -> readList(loader, elemType);
            case VAL_OBJECT_ARRAY -> {
                if (elemType == null) {
                    elemType = Object.class;
                }
                if (clazz != null) {
                    if (!clazz.isArray()) {
                        throw new BadParcelableException("About to read an array but type "
                                + clazz.getCanonicalName()
                                + " required by caller is not an array.");
                    }
                    Class<?> itemArrayType = elemType.arrayType();
                    if (!clazz.isAssignableFrom(itemArrayType)) {
                        throw new BadParcelableException("About to read a " + itemArrayType.getCanonicalName()
                                + ", which is not a subtype of type " + clazz.getCanonicalName()
                                + " required by caller.");
                    }
                }
                yield readArray(loader, elemType);
            }
            default -> throw new BadParcelableException("Unknown value type identifier: " + type);
        };
        if (object != null && clazz != null && !clazz.isInstance(object)) {
            throw new BadParcelableException("Deserialized object " + object
                    + " is not an instance of required class " + clazz.getName()
                    + " provided in the parameter");
        }
        return (T) object;
    }

    /**
     * Flatten the name of the class of the Parcelable and its contents
     * into the parcel.
     *
     * @param p               The Parcelable object to be written.
     * @param parcelableFlags Contextual flags as per
     *                        {@link Parcelable#writeToParcel(Parcel, int) Parcelable.writeToParcel()}.
     */
    public final void writeParcelable(@Nullable Parcelable p, int parcelableFlags) {
        if (p == null) {
            writeString(null);
            return;
        }
        writeParcelableCreator(p);
        p.writeToParcel(this, parcelableFlags);
    }

    /**
     * Flatten the name of the class of the Parcelable into this Parcel.
     *
     * @param p The Parcelable object to be written.
     * @see #readParcelableCreator
     */
    public final void writeParcelableCreator(@NonNull Parcelable p) {
        String name = p.getClass().getName();
        writeString(name);
    }

    @Nullable
    public <T> T readParcelable(@Nullable ClassLoader loader,
                                @NonNull Class<T> clazz) {
        return readParcelable0(loader, Objects.requireNonNull(clazz));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T readParcelable0(@Nullable ClassLoader loader,
                                 @Nullable Class<T> clazz) {
        Parcelable.Creator<?> creator = readParcelableCreator0(loader, clazz);
        if (creator == null) {
            return null;
        }
        if (creator instanceof Parcelable.ClassLoaderCreator<?>) {
            return (T) ((Parcelable.ClassLoaderCreator<?>) creator).createFromParcel(this, loader);
        }
        return (T) creator.createFromParcel(this);
    }

    @Nullable
    public <T> Parcelable.Creator<T> readParcelableCreator(
            @Nullable ClassLoader loader,
            @NonNull Class<T> clazz) {
        return readParcelableCreator0(loader, Objects.requireNonNull(clazz));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T> Parcelable.Creator<T> readParcelableCreator0(
            @Nullable ClassLoader loader,
            @Nullable Class<T> clazz) {
        final var name = readString();
        if (name == null) {
            return null;
        }
        final var map = gCreators.computeIfAbsent(loader, __ -> new ConcurrentHashMap<>());
        Parcelable.Creator<?> creator = map.get(name);
        if (creator != null) {
            if (clazz != null) {
                var target = creator.getClass().getEnclosingClass();
                if (!clazz.isAssignableFrom(target)) {
                    throw new BadParcelableException("Parcelable creator " + name + " is not "
                            + "a subclass of required class " + clazz.getName()
                            + " provided in the parameter");
                }
            }
            return (Parcelable.Creator<T>) creator;
        }

        try {
            var target = (loader == null ? Parcel.class.getClassLoader() : loader)
                    .loadClass(name);
            if (!Parcelable.class.isAssignableFrom(target)) {
                throw new BadParcelableException("Parcelable protocol requires subclassing "
                        + "from Parcelable on class " + name);
            }
            if (clazz != null) {
                if (!clazz.isAssignableFrom(target)) {
                    throw new BadParcelableException("Parcelable creator " + name + " is not "
                            + "a subclass of required class " + clazz.getName()
                            + " provided in the parameter");
                }
            }
            var f = target.getField("CREATOR");
            if ((f.getModifiers() & Modifier.STATIC) == 0) {
                throw new BadParcelableException("Parcelable protocol requires "
                        + "the CREATOR object to be static on class " + name);
            }
            if (!Parcelable.Creator.class.isAssignableFrom(f.getType())) {
                throw new BadParcelableException("Parcelable protocol requires a "
                        + "Parcelable.Creator object called "
                        + "CREATOR on class " + name);
            }
            creator = (Parcelable.Creator<?>) f.get(null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Parcelable protocol requires a "
                    + "Parcelable.Creator object called "
                    + "CREATOR on class " + name, e);
        } catch (ClassNotFoundException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        if (creator == null) {
            throw new BadParcelableException("Parcelable protocol requires a "
                    + "non-null Parcelable.Creator object called "
                    + "CREATOR on class " + name);
        }

        // Modern UI: just like Android, there's always a race
        map.put(name, creator);

        return (Parcelable.Creator<T>) creator;
    }

    /**
     * Write a byte array.
     *
     * @param b the bytes to write
     */
    public void writeByteArray(@Nullable byte[] b) {
        if (b == null) {
            writeInt(-1);
            return;
        }
        writeInt(b.length);
        writeBytes(b);
    }

    /**
     * Write a byte array.
     *
     * @param b the bytes to write
     */
    public void writeByteArray(@Nullable byte[] b, int off, int len) {
        if (b == null) {
            writeInt(-1);
            return;
        }
        writeInt(len);
        writeBytes(b, off, len);
    }

    @Nullable
    public byte[] readByteArray() {
        int n = readInt();
        if (n < 0)
            return null;
        byte[] b = new byte[n];
        readBytes(b, 0, n);
        return b;
    }

    /**
     * Write a short array.
     *
     * @param value the short array to write
     */
    public void writeShortArray(@Nullable short[] value) {
        if (value == null) {
            writeInt(-1);
            return;
        }
        writeInt(value.length);
        for (short e : value)
            writeShort(e);
    }

    @Nullable
    public short[] readShortArray() {
        int n = readInt();
        if (n < 0)
            return null;
        short[] value = new short[n];
        for (int i = 0; i < n; i++)
            value[i] = readShort();
        return value;
    }

    /**
     * Write an int array.
     *
     * @param value the int array to write
     */
    public void writeIntArray(@Nullable int[] value) {
        if (value == null) {
            writeInt(-1);
            return;
        }
        writeInt(value.length);
        for (int e : value)
            writeInt(e);
    }

    @Nullable
    public int[] readIntArray() {
        int n = readInt();
        if (n < 0)
            return null;
        int[] value = new int[n];
        for (int i = 0; i < n; i++)
            value[i] = readInt();
        return value;
    }

    /**
     * Write a long array.
     *
     * @param value the long array to write
     */
    public void writeLongArray(@Nullable long[] value) {
        if (value == null) {
            writeInt(-1);
            return;
        }
        writeInt(value.length);
        for (long e : value)
            writeLong(e);
    }

    @Nullable
    public long[] readLongArray() {
        int n = readInt();
        if (n < 0)
            return null;
        long[] value = new long[n];
        for (int i = 0; i < n; i++)
            value[i] = readLong();
        return value;
    }

    /**
     * Write a float array.
     *
     * @param value the float array to write
     */
    public void writeFloatArray(@Nullable float[] value) {
        if (value == null) {
            writeInt(-1);
            return;
        }
        writeInt(value.length);
        for (float e : value)
            writeFloat(e);
    }

    @Nullable
    public float[] readFloatArray() {
        int n = readInt();
        if (n < 0)
            return null;
        float[] value = new float[n];
        for (int i = 0; i < n; i++)
            value[i] = readFloat();
        return value;
    }

    /**
     * Write a double array.
     *
     * @param value the double array to write
     */
    public void writeDoubleArray(@Nullable double[] value) {
        if (value == null) {
            writeInt(-1);
            return;
        }
        writeInt(value.length);
        for (double e : value)
            writeDouble(e);
    }

    @Nullable
    public double[] readDoubleArray() {
        int n = readInt();
        if (n < 0)
            return null;
        double[] value = new double[n];
        for (int i = 0; i < n; i++)
            value[i] = readDouble();
        return value;
    }

    /**
     * Write a boolean array.
     *
     * @param value the boolean array to write
     */
    public void writeBooleanArray(@Nullable boolean[] value) {
        if (value == null) {
            writeInt(-1);
            return;
        }
        writeInt(value.length);
        for (boolean e : value)
            writeBoolean(e);
    }

    @Nullable
    public boolean[] readBooleanArray() {
        int n = readInt();
        if (n < 0)
            return null;
        boolean[] value = new boolean[n];
        for (int i = 0; i < n; i++)
            value[i] = readBoolean();
        return value;
    }

    /**
     * Write a char array.
     *
     * @param value the char array to write
     */
    public void writeCharArray(@Nullable char[] value) {
        if (value == null) {
            writeInt(-1);
            return;
        }
        writeInt(value.length);
        for (char e : value)
            writeChar(e);
    }

    @Nullable
    public char[] readCharArray() {
        int n = readInt();
        if (n < 0)
            return null;
        char[] value = new char[n];
        for (int i = 0; i < n; i++)
            value[i] = readChar();
        return value;
    }

    /**
     * Write an object array.
     *
     * @param a the object array to write
     */
    public void writeArray(@Nullable Object[] a) {
        if (a == null) {
            writeInt(-1);
            return;
        }
        writeInt(a.length);
        for (var e : a) {
            writeValue(e);
        }
    }

    /**
     * Read an object array.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T[] readArray(@Nullable ClassLoader loader,
                             @NonNull Class<T> clazz) {
        int n = readInt();
        if (n < 0)
            return null;
        T[] a = (T[]) (clazz == Object.class ? new Object[n] : Array.newInstance(clazz, n));
        for (int i = 0; i < n; i++) {
            T value = readValue(loader, clazz, null);
            a[i] = value;
        }
        return a;
    }

    /**
     * Write a string.
     *
     * @param s the string to write
     */
    public void writeString(@Nullable String s) {
        writeString16(s);
    }

    /**
     * Write a string in UTF-8 format.
     *
     * @param s the string to write
     */
    public void writeString8(@Nullable String s) {
        if (s == null) {
            writeInt(-1);
        } else {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            writeInt(bytes.length);
            writeBytes(bytes);
        }
    }

    /**
     * Write a string in UTF-16 BE format.
     *
     * @param s the string to write
     */
    public void writeString16(@Nullable String s) {
        if (s == null) {
            writeInt(-1);
        } else {
            int len = s.length();
            writeInt(len);
            for (int i = 0; i < len; i++) {
                writeChar(s.charAt(i));
            }
        }
    }

    /**
     * Read a string.
     */
    @Nullable
    public String readString() {
        return readString16();
    }

    /**
     * Read a string in UTF-8 format.
     */
    @Nullable
    public String readString8() {
        int n = readInt();
        if (n < 0)
            return null;
        byte[] bytes = new byte[n];
        readBytes(bytes, 0, n);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Read a string in UTF-16 BE format.
     */
    @Nullable
    public String readString16() {
        int n = readInt();
        if (n < 0)
            return null;
        char[] value = new char[n];
        for (int i = 0; i < n; i++)
            value[i] = readChar();
        return new String(value);
    }

    /**
     * Write a list.
     *
     * @param list the list to write
     */
    public void writeList(@Nullable List<?> list) {
        if (list == null) {
            writeInt(-1);
            return;
        }
        writeInt(list.size());
        for (var e : list) {
            writeValue(e);
        }
    }

    /**
     * Read a list as a value.
     *
     * @return the newly created list
     */
    @Nullable
    private <T> List<T> readList(@Nullable ClassLoader loader,
                                 @Nullable Class<? extends T> clazz) {
        int n = readInt();
        if (n < 0) {
            return null;
        }
        var res = new ArrayList<T>(n);
        while (n-- != 0) {
            res.add(readValue(loader, clazz, null));
        }
        return res;
    }

    /**
     * Write a data set.
     *
     * @param source the data set to write
     */
    public void writeDataSet(@Nullable DataSet source) {
        if (source == null) {
            writeInt(-1);
            return;
        }
        writeInt(source.size());
        var it = source.new FastEntryIterator();
        while (it.hasNext()) {
            var e = it.next();
            writeString(e.getKey());
            writeValue(e.getValue());
        }
    }

    /**
     * Read a data set as a value.
     *
     * @param loader the class loader for {@link Parcelable} classes
     * @return the newly created data set
     */
    @Nullable
    public DataSet readDataSet(@Nullable ClassLoader loader) {
        int n = readInt();
        if (n < 0) {
            return null;
        }
        var res = new DataSet(n);
        while (n-- != 0) {
            res.put(readString(), readValue(loader, null, null));
        }
        return res;
    }
}
