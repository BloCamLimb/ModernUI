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
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A DataSet (sometimes known as DataStore) encapsulates a mapping from String keys
 * to primitive values. The specified data types can safely be persisted to and
 * restored from local storage and network in bytes. Though other data types can also
 * be put into the dataset for in-memory operations, they will be silently ignored
 * during IO. A dataset object can be put into other datasets (exclude self) to
 * construct a tree structure. Neither key nor value can be null.
 * <p>
 * The default constructor seeks the balance between memory usage and IO performance
 * (data insertion and deletion, inflation and deflation, and network serialization).
 * You can use a custom map to create a dataset according to your actual needs.
 * <p>
 * Common I/O interfaces are {@link DataInput} and {@link DataOutput}, where
 * {@link String} are coded in Java modified UTF-8 format. When the target is local
 * storage, the data will be gzip compressed. You can check the source code to find
 * the detailed format specifications.
 * <p>
 * This is NOT a thread-safe object.
 */
@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
public class DataSet {

    public static final Marker MARKER = MarkerManager.getMarker("DataSet");

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

    protected final Map<String, Object> mMap;

    /**
     * Create a new DataSet using an <code>Object2ObjectOpenHashMap</code> with a load factor of 0.8f.
     */
    public DataSet() {
        mMap = new Object2ObjectOpenHashMap<>(12, 0.8f);
    }

    /**
     * Create a new DataSet using the given map.
     *
     * @param map the backing map
     */
    public DataSet(Map<String, Object> map) {
        mMap = map;
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
    @Nonnull
    public static DataSet inflate(InputStream stream) throws IOException {
        try (var input = new DataInputStream(new BufferedInputStream(new GZIPInputStream(stream, 4096), 4096))) {
            return readDataSet(input);
        }
    }

    /**
     * Writes and compresses a DataSet to a GNU zipped file. The file can have no extension.
     * The standard extension is <code>.dat.gz</code> or <code>.gz</code>.
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
     * Returns the number of key-value mappings in this data set.
     *
     * @return the number of key-value mappings in this data set
     */
    public int size() {
        return mMap.size();
    }

    /**
     * Returns {@code true} if this data set contains no key-value mappings.
     *
     * @return {@code true} if this data set contains no key-value mappings
     */
    public boolean isEmpty() {
        return mMap.isEmpty();
    }

    /**
     * Returns {@code true} if this data set contains a mapping for the specified key.
     *
     * @param key the key whose presence in this data set is to be tested
     * @return {@code true} if this data set contains a mapping for the specified key
     */
    public boolean contains(String key) {
        return mMap.containsKey(key);
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this data set contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     * {@code null} if this map contains no mapping for the key
     */
    public Object get(String key) {
        return mMap.get(key);
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this data set contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @param <T> the value type
     * @return the value to which the specified key is mapped, or
     * {@code null} if this map contains no mapping for the key
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (T) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "<T>", e);
            return null;
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
     * Returns the value associated with the given key, or defValue if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key      the key whose associated value is to be returned
     * @param defValue the value to return if key does not exist
     * @return the byte value to which the specified key is mapped
     */
    public byte getByte(String key, byte defValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defValue;
        }
        try {
            return ((Number) o).byteValue();
        } catch (ClassCastException e) {
            typeWarning(key, o, "Number", defValue, e);
            return defValue;
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
     * Returns the value associated with the given key, or defValue if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key      the key whose associated value is to be returned
     * @param defValue the value to return if key does not exist
     * @return the short value to which the specified key is mapped
     */
    public short getShort(String key, short defValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defValue;
        }
        try {
            return ((Number) o).shortValue();
        } catch (ClassCastException e) {
            typeWarning(key, o, "Number", defValue, e);
            return defValue;
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
     * Returns the value associated with the given key, or defValue if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key      the key whose associated value is to be returned
     * @param defValue the value to return if key does not exist
     * @return the int value to which the specified key is mapped
     */
    public int getInt(String key, int defValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defValue;
        }
        try {
            return ((Number) o).intValue();
        } catch (ClassCastException e) {
            typeWarning(key, o, "Number", defValue, e);
            return defValue;
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
     * Returns the value associated with the given key, or defValue if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key      the key whose associated value is to be returned
     * @param defValue the value to return if key does not exist
     * @return the long value to which the specified key is mapped
     */
    public long getLong(String key, long defValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defValue;
        }
        try {
            return ((Number) o).longValue();
        } catch (ClassCastException e) {
            typeWarning(key, o, "Number", defValue, e);
            return defValue;
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
     * Returns the value associated with the given key, or defValue if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key      the key whose associated value is to be returned
     * @param defValue the value to return if key does not exist
     * @return the float value to which the specified key is mapped
     */
    public float getFloat(String key, float defValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defValue;
        }
        try {
            return ((Number) o).floatValue();
        } catch (ClassCastException e) {
            typeWarning(key, o, "Number", defValue, e);
            return defValue;
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
     * Returns the value associated with the given key, or defValue if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key      the key whose associated value is to be returned
     * @param defValue the value to return if key does not exist
     * @return the double value to which the specified key is mapped
     */
    public double getDouble(String key, double defValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defValue;
        }
        try {
            return ((Number) o).doubleValue();
        } catch (ClassCastException e) {
            typeWarning(key, o, "Number", defValue, e);
            return defValue;
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
     * Returns the value associated with the given key, or defValue if
     * no mapping of the desired type exists for the given key.
     * Significantly, any numbers found with the key can be returned.
     *
     * @param key      the key whose associated value is to be returned
     * @param defValue the value to return if key does not exist
     * @return the boolean value to which the specified key is mapped
     */
    public boolean getBoolean(String key, boolean defValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defValue;
        }
        try {
            return ((Number) o).byteValue() != 0;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Number", defValue, e);
            return defValue;
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
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
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
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
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
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
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
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
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
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
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
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
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
        final Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (String) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "String", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or defValue if
     * no mapping of the desired type exists for the given key.
     *
     * @param key      the key whose associated value is to be returned
     * @param defValue the value to return if key does not exist or if a null
     *                 value is associated with the given key.
     * @return the String value associated with the given key, or defValue
     * if no valid String object is currently mapped to that key.
     */
    @Nonnull
    public String getString(String key, String defValue) {
        final Object o = mMap.get(key);
        if (o == null) {
            return defValue;
        }
        try {
            return (String) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "String", defValue, e);
            return defValue;
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
        final Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (UUID) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "UUID", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or defValue if
     * no mapping of the desired type exists for the given key.
     *
     * @param key      the key whose associated value is to be returned
     * @param defValue the value to return if key does not exist or if a null
     *                 value is associated with the given key.
     * @return the UUID value associated with the given key, or defValue
     * if no valid UUID object is currently mapped to that key.
     */
    @Nonnull
    public UUID getUUID(String key, UUID defValue) {
        final Object o = mMap.get(key);
        if (o == null) {
            return defValue;
        }
        try {
            return (UUID) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "UUID", defValue, e);
            return defValue;
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
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key) {
        final Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (List<T>) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "List<T>", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key. If
     * no mapping of the desired type exists for the given key,
     * attempts to compute its value using the default construction
     * function and inserts it into this map.
     * <p>
     * Note that if the list elements are primitive types, you should use the methods
     * which are specified by primitive types.
     *
     * @param key the key whose associated value is to be returned
     * @param <T> the element type
     * @return the List value to which the specified key is mapped
     */
    @Nonnull
    public <T> List<T> computeList(String key) {
        List<T> list = getList(key);
        if (list == null) {
            list = new ArrayList<>();
            putList(key, list);
        }
        return list;
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the DataSet value to which the specified key is mapped, or null
     */
    public DataSet getDataSet(String key) {
        final Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (DataSet) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "DataSet", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key. If
     * no mapping of the desired type exists for the given key,
     * attempts to compute its value using the default construction
     * function and inserts it into this map.
     *
     * @param key the key whose associated value is to be returned
     * @return the DataSet value to which the specified key is mapped
     */
    @Nonnull
    public DataSet computeDataSet(String key) {
        DataSet set = getDataSet(key);
        if (set == null) {
            set = new DataSet();
            putDataSet(key, set);
        }
        return set;
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the ByteList value to which the specified key is mapped, or null
     */
    public ByteList getByteList(String key) {
        final Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (ByteList) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "ByteList", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key. If
     * no mapping of the desired type exists for the given key,
     * attempts to compute its value using the default construction
     * function and inserts it into this map.
     *
     * @param key the key whose associated value is to be returned
     * @return the ByteList value to which the specified key is mapped, or null
     */
    @Nonnull
    public ByteList computeByteList(String key) {
        ByteList list = getByteList(key);
        if (list == null) {
            list = new ByteArrayList();
            putByteList(key, list);
        }
        return list;
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the ShortList value to which the specified key is mapped, or null
     */
    public ShortList getShortList(String key) {
        final Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (ShortList) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "ShortList", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key. If
     * no mapping of the desired type exists for the given key,
     * attempts to compute its value using the default construction
     * function and inserts it into this map.
     *
     * @param key the key whose associated value is to be returned
     * @return the ShortList value to which the specified key is mapped, or null
     */
    @Nonnull
    public ShortList computeShortList(String key) {
        ShortList list = getShortList(key);
        if (list == null) {
            list = new ShortArrayList();
            putShortList(key, list);
        }
        return list;
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the IntList value to which the specified key is mapped, or null
     */
    public IntList getIntList(String key) {
        final Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (IntList) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "IntList", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key. If
     * no mapping of the desired type exists for the given key,
     * attempts to compute its value using the default construction
     * function and inserts it into this map.
     *
     * @param key the key whose associated value is to be returned
     * @return the IntList value to which the specified key is mapped, or null
     */
    @Nonnull
    public IntList computeIntList(String key) {
        IntList list = getIntList(key);
        if (list == null) {
            list = new IntArrayList();
            putIntList(key, list);
        }
        return list;
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the LongList value to which the specified key is mapped, or null
     */
    public LongList getLongList(String key) {
        final Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (LongList) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "LongList", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key. If
     * no mapping of the desired type exists for the given key,
     * attempts to compute its value using the default construction
     * function and inserts it into this map.
     *
     * @param key the key whose associated value is to be returned
     * @return the LongList value to which the specified key is mapped, or null
     */
    @Nonnull
    public LongList computeLongList(String key) {
        LongList list = getLongList(key);
        if (list == null) {
            list = new LongArrayList();
            putLongList(key, list);
        }
        return list;
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the FloatList value to which the specified key is mapped, or null
     */
    public FloatList getFloatList(String key) {
        final Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (FloatList) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "FloatList", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key. If
     * no mapping of the desired type exists for the given key,
     * attempts to compute its value using the default construction
     * function and inserts it into this map.
     *
     * @param key the key whose associated value is to be returned
     * @return the FloatList value to which the specified key is mapped, or null
     */
    @Nonnull
    public FloatList computeFloatList(String key) {
        FloatList list = getFloatList(key);
        if (list == null) {
            list = new FloatArrayList();
            putFloatList(key, list);
        }
        return list;
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the DoubleList value to which the specified key is mapped, or null
     */
    public DoubleList getDoubleList(String key) {
        final Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (DoubleList) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "DoubleList", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key. If
     * no mapping of the desired type exists for the given key,
     * attempts to compute its value using the default construction
     * function and inserts it into this map.
     *
     * @param key the key whose associated value is to be returned
     * @return the DoubleList value to which the specified key is mapped, or null
     */
    @Nonnull
    public DoubleList computeDoubleList(String key) {
        DoubleList list = getDoubleList(key);
        if (list == null) {
            list = new DoubleArrayList();
            putDoubleList(key, list);
        }
        return list;
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
    public Object put(String key, Object value) {
        if (value == this) {
            throw new IllegalArgumentException("You can't put yourself");
        }
        return mMap.put(key, value);
    }

    /**
     * Inserts a byte value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the byte value to be associated with the specified key
     */
    public void putByte(String key, byte value) {
        mMap.put(key, value);
    }

    /**
     * Inserts a short value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the short value to be associated with the specified key
     */
    public void putShort(String key, short value) {
        mMap.put(key, value);
    }

    /**
     * Inserts an int value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the int value to be associated with the specified key
     */
    public void putInt(String key, int value) {
        mMap.put(key, value);
    }

    /**
     * Inserts a long value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the long value to be associated with the specified key
     */
    public void putLong(String key, long value) {
        mMap.put(key, value);
    }

    /**
     * Inserts a float value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the float value to be associated with the specified key
     */
    public void putFloat(String key, float value) {
        mMap.put(key, value);
    }

    /**
     * Inserts a double value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the double value to be associated with the specified key
     */
    public void putDouble(String key, double value) {
        mMap.put(key, value);
    }

    /**
     * Inserts a boolean value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the boolean value to be associated with the specified key
     */
    public void putBoolean(String key, boolean value) {
        mMap.put(key, (byte) (value ? 1 : 0));
    }

    /**
     * Inserts a String value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the String value to be associated with the specified key
     */
    public void putString(String key, String value) {
        mMap.put(key, value);
    }

    /**
     * Inserts a UUID value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the UUID value to be associated with the specified key
     */
    public void putUUID(String key, UUID value) {
        mMap.put(key, value);
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
        mMap.put(key, value);
    }

    /**
     * Inserts a DataSet value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the DataSet value to be associated with the specified key
     */
    public void putDataSet(String key, DataSet value) {
        if (value == this) {
            throw new IllegalArgumentException("You can't put yourself");
        }
        mMap.put(key, value);
    }

    /**
     * Inserts a byte[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the byte[] value to be associated with the specified key
     */
    public void putByteArray(String key, byte[] value) {
        mMap.put(key, value);
    }

    /**
     * Inserts a byte[] value into the mapping, replacing any existing value for the given key.
     * A new array created from the ByteList will be used.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the byte[] source value to be associated with the specified key
     */
    public void putByteArray(String key, ByteList value) {
        putByteArray(key, value.toByteArray());
    }

    /**
     * Inserts a ByteList value into the mapping, replacing any existing value for the given key.
     * <p>
     * Note that the list must be subclasses of {@link ByteArrayList}.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the ByteList value to be associated with the specified key
     */
    public void putByteList(String key, ByteList value) {
        mMap.put(key, value);
    }

    /**
     * Inserts a short[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the short[] value to be associated with the specified key
     */
    public void putShortArray(String key, short[] value) {
        mMap.put(key, value);
    }

    /**
     * Inserts a short[] value into the mapping, replacing any existing value for the given key.
     * A new array created from the ShortList will be used.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the short[] source value to be associated with the specified key
     */
    public void putShortArray(String key, ShortList value) {
        putShortArray(key, value.toShortArray());
    }

    /**
     * Inserts a ShortList value into the mapping, replacing any existing value for the given key.
     * <p>
     * Note that the list must be subclasses of {@link ShortArrayList}.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the ShortList value to be associated with the specified key
     */
    public void putShortList(String key, ShortList value) {
        mMap.put(key, value);
    }

    /**
     * Inserts an int[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the int[] value to be associated with the specified key
     */
    public void putIntArray(String key, int[] value) {
        mMap.put(key, value);
    }

    /**
     * Inserts an int[] value into the mapping, replacing any existing value for the given key.
     * A new array created from the IntList will be used.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the int[] source value to be associated with the specified key
     */
    public void putIntArray(String key, IntList value) {
        putIntArray(key, value.toIntArray());
    }

    /**
     * Inserts a IntList value into the mapping, replacing any existing value for the given key.
     * <p>
     * Note that the list must be subclasses of {@link IntArrayList}.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the IntList value to be associated with the specified key
     */
    public void putIntList(String key, IntList value) {
        mMap.put(key, value);
    }

    /**
     * Inserts a long[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the long[] value to be associated with the specified key
     */
    public void putLongArray(String key, long[] value) {
        mMap.put(key, value);
    }

    /**
     * Inserts a long[] value into the mapping, replacing any existing value for the given key.
     * A new array created from the LongList will be used.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the long[] source value to be associated with the specified key
     */
    public void putLongArray(String key, LongList value) {
        putLongArray(key, value.toLongArray());
    }

    /**
     * Inserts a LongList value into the mapping, replacing any existing value for the given key.
     * <p>
     * Note that the list must be subclasses of {@link LongArrayList}.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the LongList value to be associated with the specified key
     */
    public void putLongList(String key, LongList value) {
        mMap.put(key, value);
    }

    /**
     * Inserts a float[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the float[] value to be associated with the specified key
     */
    public void putFloatArray(String key, float[] value) {
        mMap.put(key, value);
    }

    /**
     * Inserts a float[] value into the mapping, replacing any existing value for the given key.
     * A new array created from the FloatList will be used.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the float[] source value to be associated with the specified key
     */
    public void putFloatArray(String key, FloatList value) {
        putFloatArray(key, value.toFloatArray());
    }

    /**
     * Inserts a FloatList value into the mapping, replacing any existing value for the given key.
     * <p>
     * Note that the list must be subclasses of {@link FloatArrayList}.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the FloatList value to be associated with the specified key
     */
    public void putFloatList(String key, FloatList value) {
        mMap.put(key, value);
    }

    /**
     * Inserts a double[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the double[] value to be associated with the specified key
     */
    public void putDoubleArray(String key, double[] value) {
        mMap.put(key, value);
    }

    /**
     * Inserts a double[] value into the mapping, replacing any existing value for the given key.
     * A new array created from the DoubleList will be used.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the double[] source value to be associated with the specified key
     */
    public void putDoubleArray(String key, DoubleList value) {
        putDoubleArray(key, value.toDoubleArray());
    }

    /**
     * Inserts a DoubleList value into the mapping, replacing any existing value for the given key.
     * <p>
     * Note that the list must be subclasses of {@link DoubleArrayList}.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the DoubleList value to be associated with the specified key
     */
    public void putDoubleList(String key, DoubleList value) {
        mMap.put(key, value);
    }

    /**
     * Removes the mapping for a key from this map if it is present
     * (optional operation).
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
    public Object remove(String key) {
        return mMap.remove(key);
    }

    /**
     * @return a set view of the keys contained in this map
     */
    @Nonnull
    public Set<String> keys() {
        return mMap.keySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return mMap.equals(((DataSet) o).mMap);
    }

    @Override
    public int hashCode() {
        return mMap.hashCode();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public String toString() {
        final Set<Map.Entry<String, Object>> entries = mMap.entrySet();
        final Iterator<Map.Entry<String, Object>> it = entries instanceof Object2ObjectMap.FastEntrySet ?
                ((Object2ObjectMap.FastEntrySet) entries).fastIterator() : entries.iterator();
        if (!it.hasNext())
            return "{}";
        final StringBuilder s = new StringBuilder();
        s.append('{');
        for (; ; ) {
            Map.Entry<String, Object> e = it.next();
            s.append(e.getKey());
            s.append('=');
            s.append(e.getValue());
            if (!it.hasNext())
                return s.append('}').toString();
            s.append(',').append(' ');
        }
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
            if (e instanceof String) {
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
            } else if (e instanceof List) {
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
            } else if (e instanceof byte[]) {
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
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void writeDataSet(DataSet set, DataOutput output) throws IOException {
        final Set<Map.Entry<String, Object>> entries = set.mMap.entrySet();
        for (final Iterator<Map.Entry<String, Object>> it = entries instanceof Object2ObjectMap.FastEntrySet ?
                ((Object2ObjectMap.FastEntrySet) entries).fastIterator() : entries.iterator(); it.hasNext(); ) {
            final Map.Entry<String, Object> entry = it.next();
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
            } else if (v instanceof String) {
                output.writeByte(VAL_STRING);
                output.writeUTF(entry.getKey());
                output.writeUTF((String) v);
            } else if (v instanceof UUID u) {
                output.writeByte(VAL_UUID);
                output.writeUTF(entry.getKey());
                output.writeLong(u.getMostSignificantBits());
                output.writeLong(u.getLeastSignificantBits());
            } else if (v instanceof List) {
                output.writeByte(VAL_LIST);
                output.writeUTF(entry.getKey());
                writeList((List<?>) v, output);
            } else if (v instanceof DataSet) {
                output.writeByte(VAL_DATA_SET);
                output.writeUTF(entry.getKey());
                writeDataSet((DataSet) v, output);
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
        final int size = input.readInt();
        switch (id) {
            case VAL_BYTE -> {
                ByteArrayList list = new ByteArrayList(size);
                for (int i = 0; i < size; i++) {
                    list.add(input.readByte());
                }
                return list;
            }
            case VAL_SHORT -> {
                ShortArrayList list = new ShortArrayList(size);
                for (int i = 0; i < size; i++) {
                    list.add(input.readShort());
                }
                return list;
            }
            case VAL_INT -> {
                IntArrayList list = new IntArrayList(size);
                for (int i = 0; i < size; i++) {
                    list.add(input.readInt());
                }
                return list;
            }
            case VAL_LONG -> {
                LongArrayList list = new LongArrayList(size);
                for (int i = 0; i < size; i++) {
                    list.add(input.readLong());
                }
                return list;
            }
            case VAL_FLOAT -> {
                FloatArrayList list = new FloatArrayList(size);
                for (int i = 0; i < size; i++) {
                    list.add(input.readFloat());
                }
                return list;
            }
            case VAL_DOUBLE -> {
                DoubleArrayList list = new DoubleArrayList(size);
                for (int i = 0; i < size; i++) {
                    list.add(input.readDouble());
                }
                return list;
            }
            case VAL_BYTE_ARRAY -> {
                ArrayList<byte[]> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    int l = input.readInt();
                    byte[] val = new byte[l];
                    input.readFully(val, 0, l);
                    list.add(val);
                }
                return list;
            }
            case VAL_SHORT_ARRAY -> {
                ArrayList<short[]> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
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
                ArrayList<int[]> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
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
                ArrayList<long[]> list = new ArrayList<>(size);
                for (long i = 0; i < size; i++) {
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
                ArrayList<float[]> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
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
                ArrayList<double[]> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
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
                ArrayList<String> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(input.readUTF());
                }
                return list;
            }
            case VAL_UUID -> {
                ArrayList<UUID> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(new UUID(input.readLong(), input.readLong()));
                }
                return list;
            }
            case VAL_LIST -> {
                ArrayList<List<?>> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(readList(input));
                }
                return list;
            }
            case VAL_DATA_SET -> {
                ArrayList<DataSet> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(readDataSet(input));
                }
                return list;
            }
            default -> throw new IOException("Unknown element type identifier: " + id);
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
                default -> throw new IOException("Unknown value type identifier: " + id);
            }
        }
        return set;
    }
}
