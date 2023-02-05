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

import icyllis.modernui.ModernUI;
import icyllis.modernui.text.TextUtils;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A Data Set (sometimes known as Data Store) encapsulates mappings from int and string
 * keys to primitive values (also includes {@link String} and {@link UUID} values).
 * The specified data types can safely be persisted to and restored from local storage
 * and network in binary form. Unsupported IO data types can also be put into the
 * data set for in-memory operations, and they will be silently ignored during IO.
 * <p>
 * A Data Set object can be put into other data sets (exclude itself) to
 * construct a tree structure. Additionally, an array structure (exposed in {@link List})
 * is also supported, where elements are backed by {@link ArrayList}. All object keys
 * can not be null.
 * <p>
 * Common IO interfaces are {@link DataInput} and {@link DataOutput}, where
 * {@link String} are coded in Java modified UTF-8 format. When the target is local
 * storage, the data will be gzip compressed. You can check the source code to find
 * the detailed format specifications.
 * <p>
 * For Netty network IO, calls {@link #readDataSet(DataInput, ClassLoader)} passing a
 * ByteBufInputStream and {@link #writeDataSet(DataOutput, DataSet)} passing a ByteBufOutputStream.
 * <p>
 * For local IO, calls {@link #inflate(InputStream, ClassLoader)} and
 * {@link #deflate(OutputStream, DataSet)} passing FileInputStream and FileOutputStream.
 * <p>
 * Format conversion between common data-interchange formats such as JSON and Minecraft NBT
 * can be easily done. The default implementations are not provided here.
 */
//TODO not finished yet
@SuppressWarnings({"unchecked", "unused"})
public final class DataSet implements Map<String, Object> {

    public static final Marker MARKER = MarkerManager.getMarker("DataSet");

    /**
     * Value types.
     */
    private static final byte VAL_NULL = 0;
    private static final byte
            VAL_BOOLEAN = 1,
            VAL_BYTE = 2,
            VAL_CHAR = 3,
            VAL_SHORT = 4,
            VAL_INT = 5,
            VAL_LONG = 6,
            VAL_FLOAT = 7,
            VAL_DOUBLE = 8;
    private static final byte
            VAL_BOOLEAN_ARRAY = 9,
            VAL_BYTE_ARRAY = 10,
            VAL_CHAR_ARRAY = 11,
            VAL_SHORT_ARRAY = 12,
            VAL_INT_ARRAY = 13,
            VAL_LONG_ARRAY = 14,
            VAL_FLOAT_ARRAY = 15,
            VAL_DOUBLE_ARRAY = 16;
    private static final byte
            VAL_STRING = 17,
            VAL_CHAR_SEQUENCE = 18,
            VAL_UUID = 19;
    private static final byte
            VAL_LIST = 20,
            VAL_DATA_SET = 21,
            VAL_DATA_SERIALIZABLE = 22,
            VAL_OBJECT_ARRAY = 23,
            VAL_SERIALIZABLE = 24;

    /**
     * The default initial size of a hash table.
     */
    private static final int DEFAULT_INITIAL_SIZE = 16;
    /**
     * The default load factor of a hash table.
     */
    private static final float DEFAULT_LOAD_FACTOR = .75f;

    /**
     * The array of keys.
     */
    private String[] mKey;
    /**
     * The array of values.
     */
    private Object[] mValue;

    /**
     * The index of the head entry in iteration order.
     * It is valid if {@link #mSize} is nonzero; otherwise, it contains -1.
     */
    private int mHead = -1;
    /**
     * The index of the tail entry in iteration order.
     * It is valid if {@link #mSize} is nonzero; otherwise, it contains -1.
     */
    private int mTail = -1;
    /**
     * For each entry, the next and the prev entry in iteration order, packed as
     * {@code ((prev & 0xFFFFFFFFL) << 32) | (next & 0xFFFFFFFFL)}.
     * The head entry contains predecessor -1, and the tail entry contains successor -1.
     */
    private long[] mLink;

    /**
     * Number of entries in the set.
     */
    private int mSize;

    /**
     * Threshold after which we rehash. It must be the table size times load factor.
     */
    private int mThreshold;

    /**
     * Cached set of entries.
     */
    private Set<Entry<String, Object>> mEntries;
    /**
     * Cached set of keys.
     */
    private Set<String> mKeys;
    /**
     * Cached collection of values.
     */
    private Collection<Object> mValues;

    /**
     * Creates a new, empty DataSet.
     */
    public DataSet() {
        mKey = new String[DEFAULT_INITIAL_SIZE];
        mValue = new Object[DEFAULT_INITIAL_SIZE];
        mLink = new long[DEFAULT_INITIAL_SIZE];
        mThreshold = (int) (DEFAULT_INITIAL_SIZE * DEFAULT_LOAD_FACTOR);
    }

    DataSet(int n) {
        n = (int) Math.ceil(n / DEFAULT_LOAD_FACTOR);
        if (n < DEFAULT_INITIAL_SIZE || n > (1 << (Integer.SIZE - 2)))
            throw new IllegalStateException();
        n = 1 << -Integer.numberOfLeadingZeros(n - 1); // ceilPow2
        mKey = new String[n];
        mValue = new Object[n];
        mLink = new long[n];
        mThreshold = (int) (n * DEFAULT_LOAD_FACTOR);
    }

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
    public static DataSet inflate(InputStream stream, ClassLoader loader) throws IOException {
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
    public static void deflate(OutputStream stream, DataSet source) throws IOException {
        try (var output = new DataOutputStream(
                new BufferedOutputStream(new GZIPOutputStream(stream, 4096), 4096))) {
            writeDataSet(output, source);
        }
    }

    static int hash(Object key) {
        final int h = key.hashCode() * 0x9e3779b1;
        return h ^ (h >>> 16);
    }

    // Query Operations

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    @Override
    public int size() {
        return mSize;
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * @return {@code true} if this map contains no key-value mappings
     */
    @Override
    public boolean isEmpty() {
        return mSize == 0;
    }

    private int find(String key) {
        Objects.requireNonNull(key);
        String k;
        final String[] keys = mKey;
        final int mask = keys.length - 1;
        int pos;
        // The starting point.
        if ((k = keys[pos = hash(key) & mask]) == null)
            return -pos - 1;
        if (key.equals(k))
            return pos;
        // There's always an unused entry.
        while (true) {
            if ((k = keys[pos = (pos + 1) & mask]) == null)
                return -pos - 1;
            if (key.equals(k))
                return pos;
        }
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     * More formally, returns {@code true} if and only if this map contains a mapping
     * for a key {@code k} such that {@code Objects.equals(key, k)}. (There can be
     * at most one such mapping.)
     *
     * @param key the key whose presence in this data set is to be tested
     * @return {@code true} if this data set contains a mapping for the specified key
     * @throws NullPointerException if the specified key is null
     */
    @Override
    public boolean containsKey(Object key) {
        Objects.requireNonNull(key);
        String k;
        final String[] keys = mKey;
        final int mask = keys.length - 1;
        int pos;
        // The starting point.
        if ((k = keys[pos = hash(key) & mask]) == null)
            return false;
        if (k == key || key.equals(k))
            return true;
        // There's always an unused entry.
        while (true) {
            if ((k = keys[pos = (pos + 1) & mask]) == null)
                return false;
            if (k == key || key.equals(k))
                return true;
        }
    }

    /**
     * Returns {@code true} if this map contains one or more keys to the specified value.
     * More formally, returns {@code true} if and only if this map contains at least
     * one mapping to a value {@code v} such that {@code Objects.equals(value, v)}.
     * This operation requires time linear in the map size.
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if this map contains one or more keys to the specified value
     */
    // @formatter:off
    @Override
    public boolean containsValue(Object value) {
        final Object[] values = mValue;
        final String[] keys = mKey;
        for (int i = keys.length; i-- > 0;)
            if (keys[i] != null && Objects.equals(value, values[i]))
                return true;
        return false;
    }
    // @formatter:on

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code Objects.equals(key, k)},
     * then this method returns {@code v}; otherwise it returns {@code null}.
     * (There can be at most one such mapping.)
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     * {@code null} if this map contains no mapping for the key
     * @throws NullPointerException if the specified key is null
     */
    @Override
    public Object get(Object key) {
        Objects.requireNonNull(key);
        String k;
        final String[] keys = mKey;
        final int mask = keys.length - 1;
        int pos;
        // The starting point.
        if ((k = keys[pos = hash(key) & mask]) == null)
            return null;
        if (k == key || key.equals(k))
            return mValue[pos];
        // There's always an unused entry.
        while (true) {
            if ((k = keys[pos = (pos + 1) & mask]) == null)
                return null;
            if (k == key || key.equals(k))
                return mValue[pos];
        }
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        final Object o = get(key);
        if (o == null)
            return defaultValue;
        return o;
    }

    @Nullable
    @Override
    public Object putIfAbsent(String key, Object value) {
        final int pos = find(key);
        if (pos >= 0)
            return mValue[pos];
        insert(-pos - 1, key, value);
        return null;
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     * {@code null} if this map contains no mapping for the key
     */
    public <T> T getValue(String key) {
        Object o = get(key);
        if (o == null)
            return null;
        try {
            return (T) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "<T>", e);
            return null;
        }
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     * {@code null} if this map contains no mapping for the key
     */
    public <T> T getValue(String key, Class<T> clazz) {
        Object o = get(key);
        if (o == null)
            return null;
        try {
            return (T) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, clazz.getName(), e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or false if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key the key whose associated value is to be returned
     * @return the boolean value to which the specified key is mapped
     */
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key          the key whose associated value is to be returned
     * @param defaultValue the value to return if key does not exist
     * @return the boolean value to which the specified key is mapped
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object o = get(key);
        if (o == null)
            return defaultValue;
        try {
            return (boolean) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Boolean", defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or (byte) 0 if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key the key whose associated value is to be returned
     * @return the byte value to which the specified key is mapped
     */
    public byte getByte(String key) {
        return getByte(key, (byte) 0);
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key          the key whose associated value is to be returned
     * @param defaultValue the value to return if key does not exist
     * @return the byte value to which the specified key is mapped
     */
    public byte getByte(String key, byte defaultValue) {
        Object o = get(key);
        if (o == null)
            return defaultValue;
        try {
            return (byte) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Byte", defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or '0' if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key the key whose associated value is to be returned
     * @return the char value to which the specified key is mapped
     */
    public char getChar(String key) {
        return getChar(key, '0');
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key          the key whose associated value is to be returned
     * @param defaultValue the value to return if key does not exist
     * @return the char value to which the specified key is mapped
     */
    public char getChar(String key, char defaultValue) {
        Object o = get(key);
        if (o == null)
            return defaultValue;
        try {
            return (char) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Character", defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or (short) 0 if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key the key whose associated value is to be returned
     * @return the short value to which the specified key is mapped
     */
    public short getShort(String key) {
        return getShort(key, (short) 0);
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key          the key whose associated value is to be returned
     * @param defaultValue the value to return if key does not exist
     * @return the short value to which the specified key is mapped
     */
    public short getShort(String key, short defaultValue) {
        Object o = get(key);
        if (o == null)
            return defaultValue;
        try {
            return (short) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Short", defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or 0 if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key the key whose associated value is to be returned
     * @return the int value to which the specified key is mapped
     */
    public int getInt(String key) {
        return getInt(key, 0);
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key          the key whose associated value is to be returned
     * @param defaultValue the value to return if key does not exist
     * @return the int value to which the specified key is mapped
     */
    public int getInt(String key, int defaultValue) {
        Object o = get(key);
        if (o == null)
            return defaultValue;
        try {
            return (int) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Integer", defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or 0L if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key the key whose associated value is to be returned
     * @return the long value to which the specified key is mapped
     */
    public long getLong(String key) {
        return getLong(key, 0L);
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key          the key whose associated value is to be returned
     * @param defaultValue the value to return if key does not exist
     * @return the long value to which the specified key is mapped
     */
    public long getLong(String key, long defaultValue) {
        Object o = get(key);
        if (o == null)
            return defaultValue;
        try {
            return (long) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Long", defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or 0.0f if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key the key whose associated value is to be returned
     * @return the float value to which the specified key is mapped
     */
    public float getFloat(String key) {
        return getFloat(key, 0.0f);
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key          the key whose associated value is to be returned
     * @param defaultValue the value to return if key does not exist
     * @return the float value to which the specified key is mapped
     */
    public float getFloat(String key, float defaultValue) {
        Object o = get(key);
        if (o == null)
            return defaultValue;
        try {
            return (float) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Float", defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or 0.0 if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key the key whose associated value is to be returned
     * @return the double value to which the specified key is mapped
     */
    public double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key          the key whose associated value is to be returned
     * @param defaultValue the value to return if key does not exist
     * @return the double value to which the specified key is mapped
     */
    public double getDouble(String key, double defaultValue) {
        Object o = get(key);
        if (o == null)
            return defaultValue;
        try {
            return (double) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Double", defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the byte[] value to which the specified key is mapped, or null
     */
    public byte[] getByteArray(String key) {
        Object o = get(key);
        if (o == null)
            return null;
        try {
            return (byte[]) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "byte[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the short[] value to which the specified key is mapped, or null
     */
    public short[] getShortArray(String key) {
        Object o = get(key);
        if (o == null)
            return null;
        try {
            return (short[]) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "short[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the int[] value to which the specified key is mapped, or null
     */
    public int[] getIntArray(String key) {
        Object o = get(key);
        if (o == null)
            return null;
        try {
            return (int[]) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "int[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the long[] value to which the specified key is mapped, or null
     */
    public long[] getLongArray(String key) {
        Object o = get(key);
        if (o == null)
            return null;
        try {
            return (long[]) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "long[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the float[] value to which the specified key is mapped, or null
     */
    public float[] getFloatArray(String key) {
        Object o = get(key);
        if (o == null)
            return null;
        try {
            return (float[]) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "float[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the double[] value to which the specified key is mapped, or null
     */
    public double[] getDoubleArray(String key) {
        Object o = get(key);
        if (o == null)
            return null;
        try {
            return (double[]) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "double[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the String value to which the specified key is mapped, or null
     */
    public String getString(String key) {
        final Object o = get(key);
        if (o == null)
            return null;
        try {
            return (String) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "String", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     *
     * @param key          the key whose associated value is to be returned
     * @param defaultValue the value to return if key does not exist or if a null
     *                     value is associated with the given key.
     * @return the String value associated with the given key, or defaultValue
     * if no valid String object is currently mapped to that key.
     */
    @Nonnull
    public String getString(String key, String defaultValue) {
        final Object o = get(key);
        if (o == null)
            return defaultValue;
        try {
            return (String) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "String", defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the UUID value to which the specified key is mapped, or null
     */
    public UUID getUUID(String key) {
        final Object o = get(key);
        if (o == null)
            return null;
        try {
            return (UUID) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "UUID", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     *
     * @param key          the key whose associated value is to be returned
     * @param defaultValue the value to return if key does not exist or if a null
     *                     value is associated with the given key.
     * @return the UUID value associated with the given key, or defaultValue
     * if no valid UUID object is currently mapped to that key.
     */
    @Nonnull
    public UUID getUUID(String key, UUID defaultValue) {
        final Object o = get(key);
        if (o == null)
            return defaultValue;
        try {
            return (UUID) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "UUID", defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     * <p>
     * Note that if the list elements are primitive types, you should use the methods
     * which are specified by primitive types.
     *
     * @param key the key whose associated value is to be returned
     * @param <T> the element type
     * @return the List value to which the specified key is mapped, or null
     */
    public <T> List<T> getList(String key) {
        final Object o = get(key);
        if (o == null)
            return null;
        try {
            return (List<T>) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "List<T>", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the DataSet value to which the specified key is mapped, or null
     */
    public DataSet getDataSet(String key) {
        final Object o = get(key);
        if (o == null)
            return null;
        try {
            return (DataSet) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "DataSet", e);
            return null;
        }
    }

    // Modification Operations

    private void insert(int pos, String key, Object value) {
        mKey[pos] = key;
        mValue[pos] = value;
        if (mSize == 0) {
            mHead = mTail = pos;
            mLink[pos] = -1L;
        } else {
            mLink[mTail] ^= ((mLink[mTail] ^ (pos & 0xFFFFFFFFL)) & 0xFFFFFFFFL);
            mLink[pos] = ((mTail & 0xFFFFFFFFL) << 32) | 0xFFFFFFFFL;
            mTail = pos;
        }
        if (mSize++ >= mThreshold) {
            int cap = mKey.length;
            if (cap > (1 << 30))
                throw new IllegalStateException("hashtable is too large");
            rehash(cap << 1);
        }
    }

    /**
     * Rehashes the map.
     *
     * @param cap the new size
     */
    private void rehash(final int cap) {
        final String[] key = mKey;
        final Object[] value = mValue;
        final int mask = cap - 1; // Note that this is used by the hashing macro
        final String[] newKey = new String[cap];
        final Object[] newValue = new Object[cap];
        int i = mHead, prev = -1, newPrev = -1, t, pos;
        final long[] link = mLink;
        final long[] newLink = new long[cap];
        mHead = -1;
        for (int j = mSize; j-- != 0; ) {
            if (key[i] == null)
                pos = cap;
            else {
                pos = hash(key[i]) & mask;
                while (!(newKey[pos] == null))
                    pos = (pos + 1) & mask;
            }
            newKey[pos] = key[i];
            newValue[pos] = value[i];
            if (prev != -1) {
                newLink[newPrev] ^= ((newLink[newPrev] ^ (pos & 0xFFFFFFFFL)) & 0xFFFFFFFFL);
                newLink[pos] ^= ((newLink[pos] ^ ((newPrev & 0xFFFFFFFFL) << 32)) & 0xFFFFFFFF00000000L);
                newPrev = pos;
            } else {
                newPrev = mHead = pos;
                // Special case of SET(newLink[pos], -1, -1);
                newLink[pos] = -1L;
            }
            t = i;
            i = (int) link[i];
            prev = t;
        }
        mLink = newLink;
        mTail = newPrev;
        if (newPrev != -1)
            // Special case of SET_NEXT(newLink[newPrev], -1);
            newLink[newPrev] |= 0xFFFFFFFFL;
        mThreshold = (int) (cap * DEFAULT_LOAD_FACTOR);
        mKey = newKey;
        mValue = newValue;
    }

    /**
     * Associates the specified value with the specified key in this map
     * (optional operation).  If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     * <p>
     * Note that the value must be a supported type by DataSets.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     * {@code null} if there was no mapping for {@code key}.
     */
    @Nullable
    @Override
    public Object put(String key, Object value) {
        if (Objects.requireNonNull(value) == this) {
            throw new IllegalArgumentException("closed loop");
        }
        final int pos = find(key);
        if (pos < 0) {
            insert(-pos - 1, key, value);
            return null;
        }
        final Object oldValue = mValue[pos];
        mValue[pos] = value;
        return oldValue;
    }

    /**
     * Inserts a byte value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the byte value to be associated with the specified key
     */
    public void putByte(String key, byte value) {
        put(key, value);
    }

    /**
     * Inserts a short value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the short value to be associated with the specified key
     */
    public void putShort(String key, short value) {
        put(key, value);
    }

    /**
     * Inserts an int value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the int value to be associated with the specified key
     */
    public void putInt(String key, int value) {
        put(key, value);
    }

    /**
     * Inserts a long value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the long value to be associated with the specified key
     */
    public void putLong(String key, long value) {
        put(key, value);
    }

    /**
     * Inserts a float value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the float value to be associated with the specified key
     */
    public void putFloat(String key, float value) {
        put(key, value);
    }

    /**
     * Inserts a double value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the double value to be associated with the specified key
     */
    public void putDouble(String key, double value) {
        put(key, value);
    }

    /**
     * Inserts a boolean value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the boolean value to be associated with the specified key
     */
    public void putBoolean(String key, boolean value) {
        put(key, value);
    }

    /**
     * Inserts a String value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the String value to be associated with the specified key
     */
    public void putString(String key, String value) {
        put(key, value);
    }

    /**
     * Inserts a UUID value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the UUID value to be associated with the specified key
     */
    public void putUUID(String key, UUID value) {
        put(key, value);
    }

    /**
     * Inserts a List value into the mapping, replacing any existing value for the given key.
     * <p>
     * The list value type must be the same and supported by DataSets. Significantly,
     * the list can be nested list or a list of nodes.
     * <p>
     * Note that if the list elements are primitive types, you should use the methods
     * which are specified by primitive types.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the List value to be associated with the specified key
     */
    public void putList(String key, List<?> value) {
        put(key, value);
    }

    /**
     * Inserts a DataSet value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the DataSet value to be associated with the specified key
     */
    public void putDataSet(String key, DataSet value) {
        put(key, value);
    }

    /**
     * Inserts a byte[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the byte[] value to be associated with the specified key
     */
    public void putByteArray(String key, byte[] value) {
        put(key, value);
    }

    /**
     * Inserts a short[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the short[] value to be associated with the specified key
     */
    public void putShortArray(String key, short[] value) {
        put(key, value);
    }

    /**
     * Inserts an int[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the int[] value to be associated with the specified key
     */
    public void putIntArray(String key, int[] value) {
        put(key, value);
    }

    /**
     * Inserts a long[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the long[] value to be associated with the specified key
     */
    public void putLongArray(String key, long[] value) {
        put(key, value);
    }

    /**
     * Inserts a float[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the float[] value to be associated with the specified key
     */
    public void putFloatArray(String key, float[] value) {
        put(key, value);
    }

    /**
     * Inserts a double[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the double[] value to be associated with the specified key
     */
    public void putDoubleArray(String key, double[] value) {
        put(key, value);
    }

    /**
     * Modifies the {@link #mLink} vector so that the given entry is removed. This method will complete
     * in constant time.
     *
     * @param i the index of an entry.
     */
    private void updateLinks(int i) {
        if (mSize == 0) {
            mHead = mTail = -1;
            return;
        }
        if (mHead == i) {
            mHead = (int) mLink[i];
            if (mHead >= 0) {
                mLink[mHead] |= 0xFFFFFFFF_00000000L;
            }
            return;
        }
        if (mTail == i) {
            mTail = (int) (mLink[i] >>> 32);
            if (mTail >= 0) {
                mLink[mTail] |= 0x00000000_FFFFFFFFL;
            }
            return;
        }
        final long link = mLink[i];
        final int prev = (int) (link >>> 32);
        final int next = (int) link;
        mLink[prev] ^= ((mLink[prev] ^ (link & 0x00000000_FFFFFFFFL)) & 0x00000000_FFFFFFFFL);
        mLink[next] ^= ((mLink[next] ^ (link & 0xFFFFFFFF_00000000L)) & 0xFFFFFFFF_00000000L);
    }

    /**
     * Modifies the {@link #mLink} vector for a shift from x to y.
     * <p>
     * This method will complete in constant time.
     *
     * @param x the source position.
     * @param y the destination position.
     */
    private void updateLinks(int x, int y) {
        if (mSize == 1) {
            mHead = mTail = y;
            mLink[y] = -1L;
        } else if (mHead == x) {
            mHead = y;
            int next = (int) mLink[x];
            mLink[next] ^= ((mLink[next] ^ ((y & 0xFFFFFFFFL) << 32)) & 0xFFFFFFFF_00000000L);
            mLink[y] = mLink[x];
        } else if (mTail == x) {
            mTail = y;
            int prev = (int) (mLink[x] >>> 32);
            mLink[prev] ^= ((mLink[prev] ^ (y & 0xFFFFFFFFL)) & 0x00000000_FFFFFFFFL);
            mLink[y] = mLink[x];
        } else {
            final long link = mLink[x];
            final int prev = (int) (link >>> 32);
            final int next = (int) link;
            mLink[prev] ^= ((mLink[prev] ^ (y & 0xFFFFFFFFL)) & 0x00000000_FFFFFFFFL);
            mLink[next] ^= ((mLink[next] ^ ((y & 0xFFFFFFFFL) << 32)) & 0xFFFFFFFF_00000000L);
            mLink[y] = link;
        }
    }

    private Object removeEntry(int pos) {
        final Object value = mValue[pos];
        mValue[pos] = null;
        mSize--;
        updateLinks(pos);
        shiftKeys(pos);
        if (mSize < mThreshold / 4 && mKey.length > DEFAULT_INITIAL_SIZE)
            rehash(mKey.length / 2);
        return value;
    }

    /**
     * Shifts left entries with the specified hash code, starting at the specified position, and empties
     * the resulting free entry.
     *
     * @param pos a starting position.
     */
    // @formatter:off
    private void shiftKeys(int pos) {
        // Shift entries with the same hash.
        int prev, i;
        String k;
        final String[] key = mKey;
        final int mask = key.length - 1;
        for (;;) {
            pos = ((prev = pos) + 1) & mask;
            for (;;) {
                if ((k = key[pos]) == null) {
                    key[prev] = null;
                    mValue[prev] = null;
                    return;
                }
                i = hash(k) & mask;
                if (prev <= pos ? prev >= i || i > pos : prev >= i && i > pos)
                    break;
                pos = (pos + 1) & mask;
            }
            key[prev] = k;
            mValue[prev] = mValue[pos];
            updateLinks(pos, prev);
        }
    }
    // @formatter:on

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * <p>Returns the value to which this map previously associated the key,
     * or {@code null} if the map contained no mapping for the key.
     *
     * <p>The map will not contain a mapping for the specified key once the
     * call returns.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with {@code key}, or
     * {@code null} if there was no mapping for {@code key}.
     */
    @Override
    public Object remove(Object key) {
        Objects.requireNonNull(key);
        String k;
        final String[] keys = mKey;
        final int mask = keys.length - 1;
        int pos;
        // The starting point.
        if ((k = keys[pos = hash(key) & mask]) == null)
            return null;
        if (k == key || key.equals(k))
            return removeEntry(pos);
        // There's always an unused entry.
        while (true) {
            if ((k = keys[pos = (pos + 1) & mask]) == null)
                return null;
            if (k == key || key.equals(k))
                return removeEntry(pos);
        }
    }

    /**
     * Removes the mapping associated with the first key in iteration order.
     *
     * @return the value previously associated with the first key in iteration order.
     * @throws NoSuchElementException is this map is empty.
     */
    public Object removeFirst() {
        if (mSize == 0)
            throw new NoSuchElementException();
        final int pos = mHead;
        // Abbreviated version of updateLinks(pos)
        mHead = (int) mLink[pos];
        if (mHead >= 0) {
            mLink[mHead] |= 0xFFFFFFFF_00000000L;
        }
        mSize--;
        final Object v = mValue[pos];
        shiftKeys(pos);
        if (mSize < mThreshold / 4 && mKey.length > DEFAULT_INITIAL_SIZE)
            rehash(mKey.length / 2);
        return v;
    }

    /**
     * Removes the mapping associated with the last key in iteration order.
     *
     * @return the value previously associated with the last key in iteration order.
     * @throws NoSuchElementException is this map is empty.
     */
    public Object removeLast() {
        if (mSize == 0)
            throw new NoSuchElementException();
        final int pos = mTail;
        // Abbreviated version of updateLinks(pos)
        mTail = (int) (mLink[pos] >>> 32);
        if (mTail >= 0) {
            mLink[mTail] |= 0x00000000_FFFFFFFFL;
        }
        mSize--;
        final Object v = mValue[pos];
        shiftKeys(pos);
        if (mSize < mThreshold / 4 && mKey.length > DEFAULT_INITIAL_SIZE)
            rehash(mKey.length / 2);
        return v;
    }

    // Bulk Operations

    @Override
    public void putAll(@Nonnull Map<? extends String, ?> map) {
        int capacity = (int) Math.min(1 << 30,
                1L << -Long.numberOfLeadingZeros((long) Math.ceil((mSize + map.size()) / DEFAULT_LOAD_FACTOR) - 1));
        if (capacity > mKey.length)
            rehash(capacity);
        for (var e : map.entrySet())
            put(e.getKey(), e.getValue());
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    @Override
    public void clear() {
        if (mSize == 0)
            return;
        mSize = 0;
        Arrays.fill(mKey, null);
        Arrays.fill(mValue, null);
        mHead = mTail = -1;
    }

    // Views

    /**
     * @return a set view of the keys contained in this map
     */
    @Nonnull
    @Override
    public Set<String> keySet() {
        if (mKeys == null)
            mKeys = new KeySet();
        return mKeys;
    }

    @Nonnull
    @Override
    public Collection<Object> values() {
        if (mValues == null)
            mValues = new Values();
        return mValues;
    }

    @Nonnull
    @Override
    public Set<Entry<String, Object>> entrySet() {
        if (mEntries == null)
            mEntries = new MapEntrySet();
        return mEntries;
    }

    /**
     * A list iterator over a linked map.
     *
     * <p>
     * This class provides a list iterator over a linked hash map. The constructor runs in constant
     * time.
     */
    private abstract class MapIterator<ACTION> {

        /**
         * The entry that will be returned by the next call to {@link java.util.ListIterator#previous()} (or
         * {@code null} if no previous entry exists).
         */
        int prev = -1;
        /**
         * The entry that will be returned by the next call to {@link java.util.ListIterator#next()} (or
         * {@code null} if no next entry exists).
         */
        int next = -1;
        /**
         * The last entry that was returned (or -1 if we did not iterate or used
         * {@link java.util.Iterator#remove()}).
         */
        int curr = -1;
        /**
         * The current index (in the sense of a {@link java.util.ListIterator}). Note that this value is not
         * meaningful when this iterator has been created using the nonempty constructor.
         */
        int index = -1;

        MapIterator() {
            next = mTail;
            index = 0;
        }

        MapIterator(String key) {
            Objects.requireNonNull(key);
            if (Objects.equals(key, mKey[mTail])) {
                prev = mTail;
                index = mSize;
                return;
            }
            String k;
            final String[] keys = mKey;
            final int mask = keys.length - 1;
            // The starting point.
            int pos = hash(key) & mask;
            // There's always an unused entry.
            while ((k = keys[pos]) != null) {
                if (key.equals(k)) {
                    // Note: no valid index known.
                    next = (int) mLink[pos];
                    prev = pos;
                    return;
                }
                pos = (pos + 1) & mask;
            }
            throw new NoSuchElementException("The key " + key + " does not belong to this map.");
        }

        abstract void accept(ACTION action, int index);

        public boolean hasNext() {
            return next != -1;
        }

        public boolean hasPrevious() {
            return prev != -1;
        }

        private void forward0() {
            if (index >= 0)
                return;
            if (prev == -1) {
                index = 0;
                return;
            }
            if (next == -1) {
                index = mSize;
                return;
            }
            int pos = mTail;
            index = 1;
            while (pos != prev) {
                pos = (int) mLink[pos];
                index++;
            }
        }

        public int nextIndex() {
            forward0();
            return index;
        }

        public int previousIndex() {
            forward0();
            return index - 1;
        }

        public int nextEntry() {
            if (!hasNext())
                throw new NoSuchElementException();
            curr = next;
            next = (int) mLink[curr];
            prev = curr;
            if (index >= 0)
                index++;
            return curr;
        }

        public int previousEntry() {
            if (!hasPrevious())
                throw new NoSuchElementException();
            curr = prev;
            prev = (int) (mLink[curr] >>> 32);
            next = curr;
            if (index >= 0)
                index--;
            return curr;
        }

        public void forEachRemaining(ACTION action) {
            while (hasNext()) {
                curr = next;
                next = (int) mLink[curr];
                prev = curr;
                if (index >= 0)
                    index++;
                accept(action, curr);
            }
        }

        // @formatter:off
        public void remove() {
            forward0();
            if (curr == -1)
                throw new IllegalStateException();
            if (curr == prev) {
				/* If the last operation was a next(), we are removing an entry that preceeds
						   the current index, and thus we must decrement it. */
                index--;
                prev = (int) (mLink[curr] >>> 32);
            } else
                next = (int) mLink[curr];
            mSize--;
			/* Now we manually fix the pointers. Because of our knowledge of next
				   and prev, this is going to be faster than calling fixPointers(). */
            if (prev == -1)
                mHead = next;
            else
                mLink[prev] ^= ((mLink[prev] ^ (next & 0xFFFFFFFFL)) & 0xFFFFFFFFL);
            if (next == -1)
                mTail = prev;
            else
                mLink[next] ^= ((mLink[next] ^ ((prev & 0xFFFFFFFFL) << 32)) & 0xFFFFFFFF00000000L);
            int last, slot, pos = curr;
            curr = -1;
            String k;
            final String[] keys = mKey;
            final int mask = keys.length - 1;
            // We have to horribly duplicate the shiftKeys() code because we need to update next/prev.
            for (;;) {
                pos = ((last = pos) + 1) & mask;
                for (;;) {
                    if (((k = keys[pos]) == null)) {
                        keys[last] = (null);
                        mValue[last] = null;
                        return;
                    }
                    slot = hash(k) & mask;
                    if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos)
                        break;
                    pos = (pos + 1) & mask;
                }
                keys[last] = k;
                mValue[last] = mValue[pos];
                if (next == pos)
                    next = last;
                if (prev == pos)
                    prev = last;
                updateLinks(pos, last);
            }
        }
        // @formatter:on

        public int skip(final int n) {
            int i = n;
            while (i-- != 0 && hasNext()) nextEntry();
            return n - i - 1;
        }

        public int back(final int n) {
            int i = n;
            while (i-- != 0 && hasPrevious()) previousEntry();
            return n - i - 1;
        }
    }

    /**
     * An iterator on keys.
     *
     * <p>
     * We simply override the
     * {@link java.util.ListIterator#next()}/{@link java.util.ListIterator#previous()} methods (and
     * possibly their type-specific counterparts) so that they return keys instead of entries.
     */
    private final class KeyIterator extends MapIterator<Consumer<? super String>> implements ListIterator<String> {

        public KeyIterator() {
            super();
        }

        public KeyIterator(String k) {
            super(k);
        }

        // forEachRemaining inherited from MapIterator superclass.
        // Despite the superclass declared with generics, the way Java inherits and generates bridge methods
        // avoids the boxing/unboxing
        @Override
        void accept(Consumer<? super String> action, int index) {
            action.accept(mKey[index]);
        }

        @Override
        public String previous() {
            return mKey[previousEntry()];
        }

        @Override
        public void set(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String next() {
            return mKey[nextEntry()];
        }
    }

    /**
     * An iterator on values.
     *
     * <p>
     * We simply override the
     * {@link java.util.ListIterator#next()}/{@link java.util.ListIterator#previous()} methods (and
     * possibly their type-specific counterparts) so that they return values instead of entries.
     */
    private final class ValueIterator extends MapIterator<Consumer<? super Object>> implements ListIterator<Object> {

        public ValueIterator() {
            super();
        }

        // forEachRemaining inherited from MapIterator superclass.
        // Despite the superclass declared with generics, the way Java inherits and generates bridge methods
        // avoids the boxing/unboxing
        @Override
        void accept(final Consumer<? super Object> action, final int index) {
            action.accept(mValue[index]);
        }

        @Override
        public Object previous() {
            return mValue[previousEntry()];
        }

        @Override
        public void set(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object next() {
            return mValue[nextEntry()];
        }
    }

    private final class KeySet extends AbstractSet<String> {

        @Nonnull
        @Override
        public Iterator<String> iterator() {
            return new KeyIterator();
        }

        @Override
        public int size() {
            return mSize;
        }

        @Override
        public void forEach(Consumer<? super String> action) {
            for (int i = mSize, curr, next = mHead; i-- != 0; ) {
                curr = next;
                next = (int) mLink[curr];
                action.accept(mKey[curr]);
            }
        }

        @Override
        public boolean contains(Object o) {
            return containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            return DataSet.this.remove(o) != null;
        }

        @Override
        public void clear() {
            DataSet.this.clear();
        }
    }

    private final class Values extends AbstractCollection<Object> {

        @Nonnull
        @Override
        public Iterator<Object> iterator() {
            return new ValueIterator();
        }

        @Override
        public int size() {
            return mSize;
        }

        @Override
        public void forEach(Consumer<? super Object> action) {
            for (int i = mSize, curr, next = mHead; i-- != 0; ) {
                curr = next;
                next = (int) mLink[curr];
                action.accept(mValue[curr]);
            }
        }

        @Override
        public boolean contains(Object o) {
            return containsValue(o);
        }

        @Override
        public void clear() {
            DataSet.this.clear();
        }
    }

    /**
     * The entry class for a hash map does not record key and value, but rather the position in the hash
     * table of the corresponding entry. This is necessary so that calls to
     * {@link Map.Entry#setValue(Object)} are reflected in the map
     */
    private final class MapEntry implements Map.Entry<String, Object> {

        // The table index this entry refers to, or -1 if this entry has been deleted.
        int mIndex;

        MapEntry() {
        }

        MapEntry(int index) {
            mIndex = index;
        }

        @Override
        public String getKey() {
            return mKey[mIndex];
        }

        @Override
        public Object getValue() {
            return mValue[mIndex];
        }

        @Override
        public Object setValue(Object newValue) {
            Object oldValue = mValue[mIndex];
            mValue[mIndex] = newValue;
            return oldValue;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(mKey[mIndex]) ^ Objects.hashCode(mValue[mIndex]);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Map.Entry<?, ?> e
                    && Objects.equals(mKey[mIndex], e.getKey())
                    && Objects.equals(mValue[mIndex], e.getValue());
        }

        @Nonnull
        @Override
        public String toString() {
            return mKey[mIndex] + "=" + mValue[mIndex];
        }
    }

    private final class EntryIterator extends MapIterator<Consumer<? super Map.Entry<String, Object>>>
            implements ListIterator<Map.Entry<String, Object>> {

        private MapEntry mEntry;

        public EntryIterator() {
        }

        public EntryIterator(String from) {
            super(from);
        }

        // forEachRemaining inherited from MapIterator superclass.
        @Override
        void accept(Consumer<? super Map.Entry<String, Object>> action, int index) {
            action.accept(new MapEntry(index));
        }

        @Override
        public MapEntry next() {
            return mEntry = new MapEntry(nextEntry());
        }

        @Override
        public MapEntry previous() {
            return mEntry = new MapEntry(previousEntry());
        }

        @Override
        public void remove() {
            super.remove();
            mEntry.mIndex = -1; // You cannot use a deleted entry.
        }

        @Override
        public void set(Entry<String, Object> e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(Entry<String, Object> e) {
            throw new UnsupportedOperationException();
        }
    }

    private final class FastEntryIterator extends MapIterator<Consumer<? super Entry<String, Object>>>
            implements ListIterator<Entry<String, Object>> {

        private final MapEntry mEntry = new MapEntry();

        public FastEntryIterator() {
        }

        public FastEntryIterator(String from) {
            super(from);
        }

        // forEachRemaining inherited from MapIterator superclass.
        @Override
        void accept(Consumer<? super Entry<String, Object>> action, int index) {
            mEntry.mIndex = index;
            action.accept(mEntry);
        }

        @Override
        public MapEntry next() {
            mEntry.mIndex = nextEntry();
            return mEntry;
        }

        @Override
        public MapEntry previous() {
            mEntry.mIndex = previousEntry();
            return mEntry;
        }

        @Override
        public void set(Entry<String, Object> e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(Entry<String, Object> e) {
            throw new UnsupportedOperationException();
        }
    }

    private final class MapEntrySet extends AbstractSet<Entry<String, Object>> {

        @Nonnull
        @Override
        public Iterator<Entry<String, Object>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return mSize;
        }

        @Override
        public void clear() {
            DataSet.this.clear();
        }
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int j = mSize, i = 0; j-->0;) {
            while (mKey[i] == null)
                i++;
            h += mKey[i].hashCode() ^ Objects.hashCode(mValue[i]);
            i++;
        }
        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Map<?, ?> m))
            return false;
        if (m.size() != size())
            return false;
        try {
            var it = new FastEntryIterator();
            while (it.hasNext()) {
                MapEntry e = it.next();
                String key = e.getKey();
                Object value = e.getValue();
                if (value == null) {
                    if (!(m.get(key) == null && m.containsKey(key)))
                        return false;
                } else {
                    if (!value.equals(m.get(key)))
                        return false;
                }
            }
        } catch (ClassCastException | NullPointerException e) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        if (isEmpty())
            return "{}";
        var s = new StringBuilder();
        s.append('{');
        var it = new FastEntryIterator();
        for (;;) {
            s.append(it.next());
            if (!it.hasNext())
                return s.append('}').toString();
            s.append(',').append(' ');
        }
    }

    static void typeWarning(int key, Object value, String className,
                            ClassCastException e) {
        typeWarning(key, value, className, "<null>", e);
    }

    // Log a message if the value was non-null but not of the expected type
    static void typeWarning(int key, Object value, String className,
                            Object defaultValue, ClassCastException e) {
        ModernUI.LOGGER.warn(MARKER, "Key {} expected {} but value was a {}. The default value {} was returned.",
                key, className, value.getClass().getName(), defaultValue);
        ModernUI.LOGGER.warn(MARKER, "Attempt to cast generated internal exception", e);
    }

    static void typeWarning(String key, Object value, String className,
                            ClassCastException e) {
        typeWarning(key, value, className, "<null>", e);
    }

    // Log a message if the value was non-null but not of the expected type
    static void typeWarning(String key, Object value, String className,
                            Object defaultValue, ClassCastException e) {
        ModernUI.LOGGER.warn(MARKER, "Key {} expected {} but value was a {}. The default value {} was returned.",
                key, className, value.getClass().getName(), defaultValue);
        ModernUI.LOGGER.warn(MARKER, "Attempt to cast generated internal exception", e);
    }

    private static byte getValueType(Object v) {
        if (v instanceof String)
            return VAL_STRING;
        else if (v instanceof Integer)
            return VAL_INT;
        else {
            Class<?> clazz = v.getClass();
            if (clazz.isArray() && clazz.getComponentType() == Object.class)
                return VAL_OBJECT_ARRAY;
            else if (v instanceof Serializable)
                return VAL_SERIALIZABLE;
            else return -1; // silently ignored
        }
    }

    /**
     * Write a value and its type.
     *
     * @param out  the data output
     * @param v the value to write
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
        } else if (v instanceof DataSerializable value) {
            out.writeByte(VAL_DATA_SERIALIZABLE);
            writeString(out, value.getClass().getName());
            value.writeData(out);
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
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(stream);
                oos.writeObject(value);
                oos.close();
                writeByteArray(out, stream.toByteArray());
            }
            // others are silently ignored
        }
    }

    /**
     * Write a string in UTF-16 BE format.
     *
     * @param out the data output
     * @param s   the string to write
     * @throws IOException if an IO error occurs
     */
    public static void writeString(@Nonnull DataOutput out, @Nullable String s) throws IOException {
        if (s == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(s.length());
        out.writeChars(s);
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

    /**
     * Write an object array.
     *
     * @param out   the data output
     * @param value the object array to write
     * @throws IOException if an IO error occurs
     */
    public static void writeArray(@Nonnull DataOutput out,
                                  @Nullable Object[] value) throws IOException {
        if (value == null) {
            out.writeInt(-1);
            return;
        }
        int n = value.length, i = 0;
        out.writeInt(n);
        while (i < n)
            writeValue(out, value[i++]);
    }

    /**
     * Write a list.
     *
     * @param out   the data output
     * @param value the list to write
     * @throws IOException if an IO error occurs
     */
    public static void writeList(@Nonnull DataOutput out,
                                 @Nullable List<?> value) throws IOException {
        if (value == null) {
            out.writeInt(-1);
            return;
        }
        int n = value.size(), i = 0;
        out.writeInt(n);
        while (i < n)
            writeValue(out, value.get(i++));
    }

    /**
     * Write a data set.
     *
     * @param out   the data output
     * @param value the data set to write
     * @throws IOException if an IO error occurs
     */
    public static void writeDataSet(@Nonnull DataOutput out,
                                    @Nullable DataSet value) throws IOException {
        if (value == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(value.size());
        var it = value.new FastEntryIterator();
        while (it.hasNext()) {
            MapEntry e = it.next();
            writeString(out, e.getKey());
            writeValue(out, e.getValue());
        }
    }

    @Nullable
    public static <T> T readValue(@Nonnull DataInput in, @Nullable ClassLoader loader,
                                  @Nullable Class<T> clazz, @Nullable Class<?> itemType) throws IOException {
        final byte type = in.readByte();
        final Object object = switch (type) {
            case VAL_NULL -> null;
            case VAL_BOOLEAN -> in.readBoolean();
            case VAL_BYTE -> in.readByte();
            case VAL_CHAR -> in.readChar();
            case VAL_SHORT -> in.readShort();
            case VAL_INT -> in.readInt();
            case VAL_LONG -> in.readLong();
            case VAL_FLOAT -> in.readFloat();
            case VAL_DOUBLE -> in.readDouble();
            case VAL_BOOLEAN_ARRAY -> readBooleanArray(in);
            case VAL_STRING -> readString(in);
            case VAL_CHAR_SEQUENCE -> TextUtils.read(in);
            case VAL_UUID -> new UUID(in.readLong(), in.readLong());
            case VAL_LIST -> readList(in, loader, itemType);
            case VAL_DATA_SET -> readDataSet(in, loader);
            default -> throw new IOException("Unknown value type identifier: " + type);
        };
        if (object != null && clazz != null && !clazz.isInstance(object)) {
            throw new IOException("Deserialized object " + object
                    + " is not an instance of required class " + clazz.getName()
                    + " provided in the parameter");
        }
        return (T) object;
    }

    @Nullable
    public static String readString(@Nonnull DataInput in) throws IOException {
        int n = in.readInt();
        if (n < 0)
            return null;
        char[] value = new char[n];
        for (int i = 0; i < n; i++)
            value[i] = in.readChar();
        return new String(value);
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
        List<T> result = new ArrayList<>(n);
        while (n-- > 0)
            result.add(readValue(in, loader, clazz, null));
        return result;
    }

    /**
     * Read a data set as a value.
     *
     * @param in the data input
     * @return the newly created data set
     * @throws IOException if an IO error occurs
     */
    @Nullable
    public static DataSet readDataSet(@Nonnull DataInput in, @Nullable ClassLoader loader) throws IOException {
        int n = in.readInt();
        if (n < 0)
            return null;
        DataSet result = n > (DEFAULT_INITIAL_SIZE * DEFAULT_LOAD_FACTOR)
                ? new DataSet(n)
                : new DataSet();
        while (n-- > 0)
            result.put(readString(in), readValue(in, loader, null, null));
        return result;
    }
}
