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
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.*;
import java.util.*;
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
 * is also supported, array objects are backed by {@link ArrayList} and primitive-specified
 * array lists such as {@link IntArrayList}. All object keys and values can not be null.
 * <p>
 * The default constructor seeks the balance between memory usage and IO performance
 * (data insertion and deletion, inflation and deflation, and network serialization).
 * You can use custom maps to create a data set according to your actual needs.
 * <p>
 * Common IO interfaces are {@link DataInput} and {@link DataOutput}, where
 * {@link String} are coded in Java modified UTF-8 format. When the target is local
 * storage, the data will be gzip compressed. You can check the source code to find
 * the detailed format specifications.
 * <p>
 * For Netty network IO, calls {@link #readDataSet(DataInput)} passing a ByteBufInputStream
 * and {@link #writeDataSet(DataSet, DataOutput)} passing a ByteBufOutputStream.
 * <p>
 * For local IO, calls {@link #inflate(InputStream)} and {@link #deflate(DataSet, OutputStream)}
 * passing FileInputStream and FileOutputStream.
 * <p>
 * Format conversion between common data-interchange formats such as JSON and Minecraft NBT
 * can be easily done. The default implementations are not provided here.
 */
@SuppressWarnings("unused")
@NotThreadSafe
@ParametersAreNonnullByDefault
public class DataSet {

    public static final Marker MARKER = MarkerManager.getMarker("DataSet");

    private static final byte VAL_NULL = 0x00;
    private static final byte VAL_BYTE = 0x01;
    private static final byte VAL_SHORT = 0x02;
    private static final byte VAL_INT = 0x03;
    private static final byte VAL_LONG = 0x04;
    private static final byte VAL_FLOAT = 0x05;
    private static final byte VAL_DOUBLE = 0x06;
    private static final byte VAL_STRING = 0x07;
    private static final byte VAL_UUID = 0x08;
    private static final byte VAL_LIST = 0x09;
    private static final byte VAL_DATA_SET = 0x0A;

    protected final Int2ObjectMap<Object> mIntMap;
    protected final Map<String, Object> mStringMap;

    /**
     * Create a new DataSet using an {@link Int2ObjectOpenHashMap} and
     * {@link Object2ObjectOpenHashMap} with a load factor of 0.8f.
     */
    public DataSet() {
        // Note: for string keys, Java HashMap is faster, but it takes more memory
        // for int keys, fast-util map is always faster
        mIntMap = new Int2ObjectOpenHashMap<>(12, 0.8f);
        mStringMap = new Object2ObjectOpenHashMap<>(12, 0.8f);
    }

    /**
     * Create a new DataSet using the given maps.
     *
     * @param intMap    the backing map using int keys
     * @param stringMap the backing map using string keys
     */
    public DataSet(Int2ObjectMap<Object> intMap, Map<String, Object> stringMap) {
        mIntMap = intMap;
        mStringMap = stringMap;
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
        return mIntMap.size() + mStringMap.size();
    }

    /**
     * Returns {@code true} if this data set contains no key-value mappings.
     *
     * @return {@code true} if this data set contains no key-value mappings
     */
    public boolean isEmpty() {
        return mIntMap.isEmpty() && mStringMap.isEmpty();
    }

    /**
     * Returns {@code true} if this data set contains a mapping for the specified key.
     *
     * @param key the key whose presence in this data set is to be tested
     * @return {@code true} if this data set contains a mapping for the specified key
     */
    public boolean contains(int key) {
        return mIntMap.containsKey(key);
    }

    /**
     * Returns {@code true} if this data set contains a mapping for the specified key.
     *
     * @param key the key whose presence in this data set is to be tested
     * @return {@code true} if this data set contains a mapping for the specified key
     */
    public boolean contains(String key) {
        return mStringMap.containsKey(key);
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this data set contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     * {@code null} if this map contains no mapping for the key
     */
    public Object get(int key) {
        return mIntMap.get(key);
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
        return mStringMap.get(key);
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
    public <T> T getValue(int key) {
        Object o = mIntMap.get(key);
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
        Object o = mStringMap.get(key);
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
    public byte getByte(int key) {
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
    public byte getByte(int key, byte defValue) {
        Object o = mIntMap.get(key);
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
    public short getShort(int key) {
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
    public short getShort(int key, short defValue) {
        Object o = mIntMap.get(key);
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
    public int getInt(int key) {
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
    public int getInt(int key, int defValue) {
        Object o = mIntMap.get(key);
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
    public long getLong(int key) {
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
    public long getLong(int key, long defValue) {
        Object o = mIntMap.get(key);
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
    public float getFloat(int key) {
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
    public float getFloat(int key, float defValue) {
        Object o = mIntMap.get(key);
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
    public double getDouble(int key) {
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
    public double getDouble(int key, double defValue) {
        Object o = mIntMap.get(key);
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
    public boolean getBoolean(int key) {
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
    public boolean getBoolean(int key, boolean defValue) {
        Object o = mIntMap.get(key);
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
        Object o = mStringMap.get(key);
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
        Object o = mStringMap.get(key);
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
        Object o = mStringMap.get(key);
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
        Object o = mStringMap.get(key);
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
        Object o = mStringMap.get(key);
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
        Object o = mStringMap.get(key);
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
        Object o = mStringMap.get(key);
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
     * If the length is variable, use {@link #getByteList(int)} instead.
     *
     * @param key the key whose associated value is to be returned
     * @return the byte[] value to which the specified key is mapped, or null
     */
    public byte[] getByteArray(int key) {
        Object o = mIntMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return ((ByteArrayList) o).elements();
        } catch (ClassCastException e) {
            typeWarning(key, o, "byte[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     * If the length is variable, use {@link #getShortList(int)} instead.
     *
     * @param key the key whose associated value is to be returned
     * @return the short[] value to which the specified key is mapped, or null
     */
    public short[] getShortArray(int key) {
        Object o = mIntMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return ((ShortArrayList) o).elements();
        } catch (ClassCastException e) {
            typeWarning(key, o, "short[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     * If the length is variable, use {@link #getIntList(int)} instead.
     *
     * @param key the key whose associated value is to be returned
     * @return the int[] value to which the specified key is mapped, or null
     */
    public int[] getIntArray(int key) {
        Object o = mIntMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return ((IntArrayList) o).elements();
        } catch (ClassCastException e) {
            typeWarning(key, o, "int[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     * If the length is variable, use {@link #getLongList(int)} instead.
     *
     * @param key the key whose associated value is to be returned
     * @return the long[] value to which the specified key is mapped, or null
     */
    public long[] getLongArray(int key) {
        Object o = mIntMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return ((LongArrayList) o).elements();
        } catch (ClassCastException e) {
            typeWarning(key, o, "long[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     * If the length is variable, use {@link #getFloatList(int)} instead.
     *
     * @param key the key whose associated value is to be returned
     * @return the float[] value to which the specified key is mapped, or null
     */
    public float[] getFloatArray(int key) {
        Object o = mIntMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return ((FloatArrayList) o).elements();
        } catch (ClassCastException e) {
            typeWarning(key, o, "float[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     * If the length is variable, use {@link #getDoubleList(int)} instead.
     *
     * @param key the key whose associated value is to be returned
     * @return the double[] value to which the specified key is mapped, or null
     */
    public double[] getDoubleArray(int key) {
        Object o = mIntMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return ((DoubleArrayList) o).elements();
        } catch (ClassCastException e) {
            typeWarning(key, o, "double[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     * If the length is variable, use {@link #getByteList(int)} instead.
     *
     * @param key the key whose associated value is to be returned
     * @return the byte[] value to which the specified key is mapped, or null
     */
    public byte[] getByteArray(String key) {
        Object o = mStringMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return ((ByteArrayList) o).elements();
        } catch (ClassCastException e) {
            typeWarning(key, o, "byte[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     * If the length is variable, use {@link #getShortList(int)} instead.
     *
     * @param key the key whose associated value is to be returned
     * @return the short[] value to which the specified key is mapped, or null
     */
    public short[] getShortArray(String key) {
        Object o = mStringMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return ((ShortArrayList) o).elements();
        } catch (ClassCastException e) {
            typeWarning(key, o, "short[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     * If the length is variable, use {@link #getIntList(int)} instead.
     *
     * @param key the key whose associated value is to be returned
     * @return the int[] value to which the specified key is mapped, or null
     */
    public int[] getIntArray(String key) {
        Object o = mStringMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return ((IntArrayList) o).elements();
        } catch (ClassCastException e) {
            typeWarning(key, o, "int[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     * If the length is variable, use {@link #getLongList(int)} instead.
     *
     * @param key the key whose associated value is to be returned
     * @return the long[] value to which the specified key is mapped, or null
     */
    public long[] getLongArray(String key) {
        Object o = mStringMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return ((LongArrayList) o).elements();
        } catch (ClassCastException e) {
            typeWarning(key, o, "long[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     * If the length is variable, use {@link #getFloatList(int)} instead.
     *
     * @param key the key whose associated value is to be returned
     * @return the float[] value to which the specified key is mapped, or null
     */
    public float[] getFloatArray(String key) {
        Object o = mStringMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return ((FloatArrayList) o).elements();
        } catch (ClassCastException e) {
            typeWarning(key, o, "float[]", e);
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     * If the length is variable, use {@link #getDoubleList(int)} instead.
     *
     * @param key the key whose associated value is to be returned
     * @return the double[] value to which the specified key is mapped, or null
     */
    public double[] getDoubleArray(String key) {
        Object o = mStringMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return ((DoubleArrayList) o).elements();
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
    public String getString(int key) {
        final Object o = mIntMap.get(key);
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
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the String value to which the specified key is mapped, or null
     */
    public String getString(String key) {
        final Object o = mStringMap.get(key);
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
    public String getString(int key, String defValue) {
        final Object o = mIntMap.get(key);
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
        final Object o = mStringMap.get(key);
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
    public UUID getUUID(int key) {
        final Object o = mIntMap.get(key);
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
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the UUID value to which the specified key is mapped, or null
     */
    public UUID getUUID(String key) {
        final Object o = mStringMap.get(key);
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
    public UUID getUUID(int key, UUID defValue) {
        final Object o = mIntMap.get(key);
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
        final Object o = mStringMap.get(key);
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
    public <T> List<T> getList(int key) {
        final Object o = mIntMap.get(key);
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
    public <T> List<T> acquireList(int key) {
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
    public DataSet getDataSet(int key) {
        final Object o = mIntMap.get(key);
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
    public DataSet acquireDataSet(int key) {
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
        final Object o = mStringMap.get(key);
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
    public <T> List<T> acquireList(String key) {
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
        final Object o = mStringMap.get(key);
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
    public DataSet acquireDataSet(String key) {
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
    public ByteList getByteList(int key) {
        final Object o = mIntMap.get(key);
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
    public ByteList acquireByteList(int key) {
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
    public ShortList getShortList(int key) {
        final Object o = mIntMap.get(key);
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
    public ShortList acquireShortList(int key) {
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
    public IntList getIntList(int key) {
        final Object o = mIntMap.get(key);
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
    public IntList acquireIntList(int key) {
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
    public LongList getLongList(int key) {
        final Object o = mIntMap.get(key);
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
    public LongList acquireLongList(int key) {
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
    public FloatList getFloatList(int key) {
        final Object o = mIntMap.get(key);
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
    public FloatList acquireFloatList(int key) {
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
    public DoubleList getDoubleList(int key) {
        final Object o = mIntMap.get(key);
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
    public DoubleList acquireDoubleList(int key) {
        DoubleList list = getDoubleList(key);
        if (list == null) {
            list = new DoubleArrayList();
            putDoubleList(key, list);
        }
        return list;
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     *
     * @param key the key whose associated value is to be returned
     * @return the ByteList value to which the specified key is mapped, or null
     */
    public ByteList getByteList(String key) {
        final Object o = mStringMap.get(key);
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
    public ByteList acquireByteList(String key) {
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
        final Object o = mStringMap.get(key);
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
    public ShortList acquireShortList(String key) {
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
        final Object o = mStringMap.get(key);
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
    public IntList acquireIntList(String key) {
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
        final Object o = mStringMap.get(key);
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
    public LongList acquireLongList(String key) {
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
        final Object o = mStringMap.get(key);
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
    public FloatList acquireFloatList(String key) {
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
        final Object o = mStringMap.get(key);
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
    public DoubleList acquireDoubleList(String key) {
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
    public Object put(int key, Object value) {
        if (value == this) {
            throw new IllegalArgumentException("You can't put yourself");
        }
        return mIntMap.put(key, value);
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
        return mStringMap.put(key, value);
    }

    /**
     * Inserts a byte value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the byte value to be associated with the specified key
     */
    public void putByte(int key, byte value) {
        mIntMap.put(key, Byte.valueOf(value));
    }

    /**
     * Inserts a short value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the short value to be associated with the specified key
     */
    public void putShort(int key, short value) {
        mIntMap.put(key, Short.valueOf(value));
    }

    /**
     * Inserts an int value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the int value to be associated with the specified key
     */
    public void putInt(int key, int value) {
        mIntMap.put(key, Integer.valueOf(value));
    }

    /**
     * Inserts a long value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the long value to be associated with the specified key
     */
    public void putLong(int key, long value) {
        mIntMap.put(key, Long.valueOf(value));
    }

    /**
     * Inserts a float value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the float value to be associated with the specified key
     */
    public void putFloat(int key, float value) {
        mIntMap.put(key, Float.valueOf(value));
    }

    /**
     * Inserts a double value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the double value to be associated with the specified key
     */
    public void putDouble(int key, double value) {
        mIntMap.put(key, Double.valueOf(value));
    }

    /**
     * Inserts a boolean value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the boolean value to be associated with the specified key
     */
    public void putBoolean(int key, boolean value) {
        mIntMap.put(key, Byte.valueOf((byte) (value ? 1 : 0)));
    }

    /**
     * Inserts a String value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the String value to be associated with the specified key
     */
    public void putString(int key, String value) {
        mIntMap.put(key, value);
    }

    /**
     * Inserts a UUID value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the UUID value to be associated with the specified key
     */
    public void putUUID(int key, UUID value) {
        mIntMap.put(key, value);
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
    public void putList(int key, List<?> value) {
        mIntMap.put(key, value);
    }

    /**
     * Inserts a DataSet value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the DataSet value to be associated with the specified key
     */
    public void putDataSet(int key, DataSet value) {
        if (value == this) {
            throw new IllegalArgumentException("You can't put yourself");
        }
        mIntMap.put(key, value);
    }

    /**
     * Inserts a byte[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the byte[] value to be associated with the specified key
     */
    public void putByteArray(int key, byte[] value) {
        mIntMap.put(key, ByteArrayList.wrap(value));
    }

    /**
     * Inserts a ByteList value into the mapping, replacing any existing value for the given key.
     * <p>
     * Note that the list must be subclasses of {@link ByteArrayList}.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the ByteList value to be associated with the specified key
     */
    public void putByteList(int key, ByteList value) {
        mIntMap.put(key, value);
    }

    /**
     * Inserts a short[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the short[] value to be associated with the specified key
     */
    public void putShortArray(int key, short[] value) {
        mIntMap.put(key, ShortArrayList.wrap(value));
    }

    /**
     * Inserts a ShortList value into the mapping, replacing any existing value for the given key.
     * <p>
     * Note that the list must be subclasses of {@link ShortArrayList}.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the ShortList value to be associated with the specified key
     */
    public void putShortList(int key, ShortList value) {
        mIntMap.put(key, value);
    }

    /**
     * Inserts an int[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the int[] value to be associated with the specified key
     */
    public void putIntArray(int key, int[] value) {
        mIntMap.put(key, IntArrayList.wrap(value));
    }

    /**
     * Inserts a IntList value into the mapping, replacing any existing value for the given key.
     * <p>
     * Note that the list must be subclasses of {@link IntArrayList}.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the IntList value to be associated with the specified key
     */
    public void putIntList(int key, IntList value) {
        mIntMap.put(key, value);
    }

    /**
     * Inserts a long[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the long[] value to be associated with the specified key
     */
    public void putLongArray(int key, long[] value) {
        mIntMap.put(key, LongArrayList.wrap(value));
    }

    /**
     * Inserts a LongList value into the mapping, replacing any existing value for the given key.
     * <p>
     * Note that the list must be subclasses of {@link LongArrayList}.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the LongList value to be associated with the specified key
     */
    public void putLongList(int key, LongList value) {
        mIntMap.put(key, value);
    }

    /**
     * Inserts a float[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the float[] value to be associated with the specified key
     */
    public void putFloatArray(int key, float[] value) {
        mIntMap.put(key, FloatArrayList.wrap(value));
    }

    /**
     * Inserts a FloatList value into the mapping, replacing any existing value for the given key.
     * <p>
     * Note that the list must be subclasses of {@link FloatArrayList}.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the FloatList value to be associated with the specified key
     */
    public void putFloatList(int key, FloatList value) {
        mIntMap.put(key, value);
    }

    /**
     * Inserts a double[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the double[] value to be associated with the specified key
     */
    public void putDoubleArray(int key, double[] value) {
        mIntMap.put(key, DoubleArrayList.wrap(value));
    }

    /**
     * Inserts a DoubleList value into the mapping, replacing any existing value for the given key.
     * <p>
     * Note that the list must be subclasses of {@link DoubleArrayList}.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the DoubleList value to be associated with the specified key
     */
    public void putDoubleList(int key, DoubleList value) {
        mIntMap.put(key, value);
    }

    /**
     * Inserts a byte value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the byte value to be associated with the specified key
     */
    public void putByte(String key, byte value) {
        mStringMap.put(key, value);
    }

    /**
     * Inserts a short value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the short value to be associated with the specified key
     */
    public void putShort(String key, short value) {
        mStringMap.put(key, value);
    }

    /**
     * Inserts an int value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the int value to be associated with the specified key
     */
    public void putInt(String key, int value) {
        mStringMap.put(key, value);
    }

    /**
     * Inserts a long value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the long value to be associated with the specified key
     */
    public void putLong(String key, long value) {
        mStringMap.put(key, value);
    }

    /**
     * Inserts a float value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the float value to be associated with the specified key
     */
    public void putFloat(String key, float value) {
        mStringMap.put(key, value);
    }

    /**
     * Inserts a double value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the double value to be associated with the specified key
     */
    public void putDouble(String key, double value) {
        mStringMap.put(key, value);
    }

    /**
     * Inserts a boolean value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the boolean value to be associated with the specified key
     */
    public void putBoolean(String key, boolean value) {
        mStringMap.put(key, (byte) (value ? 1 : 0));
    }

    /**
     * Inserts a String value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the String value to be associated with the specified key
     */
    public void putString(String key, String value) {
        mStringMap.put(key, value);
    }

    /**
     * Inserts a UUID value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the UUID value to be associated with the specified key
     */
    public void putUUID(String key, UUID value) {
        mStringMap.put(key, value);
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
        mStringMap.put(key, value);
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
        mStringMap.put(key, value);
    }

    /**
     * Inserts a byte[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the byte[] value to be associated with the specified key
     */
    public void putByteArray(String key, byte[] value) {
        mStringMap.put(key, ByteArrayList.wrap(value));
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
        mStringMap.put(key, value);
    }

    /**
     * Inserts a short[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the short[] value to be associated with the specified key
     */
    public void putShortArray(String key, short[] value) {
        mStringMap.put(key, ShortArrayList.wrap(value));
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
        mStringMap.put(key, value);
    }

    /**
     * Inserts an int[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the int[] value to be associated with the specified key
     */
    public void putIntArray(String key, int[] value) {
        mStringMap.put(key, IntArrayList.wrap(value));
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
        mStringMap.put(key, value);
    }

    /**
     * Inserts a long[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the long[] value to be associated with the specified key
     */
    public void putLongArray(String key, long[] value) {
        mStringMap.put(key, LongArrayList.wrap(value));
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
        mStringMap.put(key, value);
    }

    /**
     * Inserts a float[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the float[] value to be associated with the specified key
     */
    public void putFloatArray(String key, float[] value) {
        mStringMap.put(key, FloatArrayList.wrap(value));
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
        mStringMap.put(key, value);
    }

    /**
     * Inserts a double[] value into the mapping, replacing any existing value for the given key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the double[] value to be associated with the specified key
     */
    public void putDoubleArray(String key, double[] value) {
        mStringMap.put(key, DoubleArrayList.wrap(value));
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
        mStringMap.put(key, value);
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
    public Object remove(int key) {
        return mIntMap.remove(key);
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
        return mStringMap.remove(key);
    }

    /**
     * @return a set view of the keys contained in this map
     */
    @Nonnull
    public IntSet intKeys() {
        return mIntMap.keySet();
    }

    /**
     * @return a set view of the keys contained in this map
     */
    @Nonnull
    public Set<String> stringKeys() {
        return mStringMap.keySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataSet dataSet = (DataSet) o;

        if (!mIntMap.equals(dataSet.mIntMap)) return false;
        return mStringMap.equals(dataSet.mStringMap);
    }

    @Override
    public int hashCode() {
        int result = mIntMap.hashCode();
        result = 31 * result + mStringMap.hashCode();
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }
        final StringBuilder s = new StringBuilder();
        s.append('{');
        if (!mIntMap.isEmpty()) {
            final ObjectIterator<Int2ObjectMap.Entry<Object>> it = Int2ObjectMaps.fastIterator(mIntMap);
            while (it.hasNext()) {
                Int2ObjectMap.Entry<Object> e = it.next();
                s.append(e.getIntKey());
                s.append('=');
                s.append(e.getValue());
                s.append(',').append(' ');
            }
        }
        if (!mStringMap.isEmpty()) {
            final Set<Map.Entry<String, Object>> entries = mStringMap.entrySet();
            final Iterator<Map.Entry<String, Object>> it = entries instanceof Object2ObjectMap.FastEntrySet ?
                    ((Object2ObjectMap.FastEntrySet) entries).fastIterator() : entries.iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> e = it.next();
                s.append(e.getKey());
                s.append('=');
                s.append(e.getValue());
                s.append(',').append(' ');
            }
        }
        s.delete(s.length() - 2, s.length());
        return s.append('}').toString();
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

    /**
     * Write a list as a value.
     *
     * @param list   the list to write
     * @param output the data output
     * @throws IOException if an IO error occurs
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
            // short path for Object arrays, but do not break primitive-specified arrays
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
            }
        }
    }

    /**
     * Write a data set as a value.
     *
     * @param set    the set to write
     * @param output the data output
     * @throws IOException if an IO error occurs
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void writeDataSet(DataSet set, DataOutput output) throws IOException {
        if (!set.mIntMap.isEmpty()) {
            for (final ObjectIterator<Int2ObjectMap.Entry<Object>> it = Int2ObjectMaps.fastIterator(set.mIntMap);
                 it.hasNext(); ) {
                final Int2ObjectMap.Entry<Object> entry = it.next();
                final Object v = entry.getValue();
                if (v instanceof Byte) {
                    output.writeByte(VAL_BYTE);
                    output.writeInt(entry.getIntKey());
                    output.writeByte((byte) v);
                } else if (v instanceof Short) {
                    output.writeByte(VAL_SHORT);
                    output.writeInt(entry.getIntKey());
                    output.writeShort((short) v);
                } else if (v instanceof Integer) {
                    output.writeByte(VAL_INT);
                    output.writeInt(entry.getIntKey());
                    output.writeInt((int) v);
                } else if (v instanceof Long) {
                    output.writeByte(VAL_LONG);
                    output.writeInt(entry.getIntKey());
                    output.writeLong((long) v);
                } else if (v instanceof Float) {
                    output.writeByte(VAL_FLOAT);
                    output.writeInt(entry.getIntKey());
                    output.writeFloat((float) v);
                } else if (v instanceof Double) {
                    output.writeByte(VAL_DOUBLE);
                    output.writeInt(entry.getIntKey());
                    output.writeDouble((double) v);
                } else if (v instanceof String) {
                    output.writeByte(VAL_STRING);
                    output.writeInt(entry.getIntKey());
                    output.writeUTF((String) v);
                } else if (v instanceof UUID u) {
                    output.writeByte(VAL_UUID);
                    output.writeInt(entry.getIntKey());
                    output.writeLong(u.getMostSignificantBits());
                    output.writeLong(u.getLeastSignificantBits());
                } else if (v instanceof List) {
                    output.writeByte(VAL_LIST);
                    output.writeInt(entry.getIntKey());
                    writeList((List<?>) v, output);
                } else if (v instanceof DataSet) {
                    output.writeByte(VAL_DATA_SET);
                    output.writeInt(entry.getIntKey());
                    writeDataSet((DataSet) v, output);
                }
            }
        }
        output.writeByte(VAL_NULL);
        if (!set.mStringMap.isEmpty()) {
            final Set<Map.Entry<String, Object>> entries = set.mStringMap.entrySet();
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
     * @throws IOException if an IO error occurs
     */
    @Nonnull
    private static List<?> readList(DataInput input) throws IOException {
        final byte id = input.readByte();
        if (id == VAL_NULL) {
            // short path for Object arrays, but do not break primitive-specified arrays
            return new ArrayList<>();
        }
        // here, the size may be zero for non-primitive types
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
     * @throws IOException if an IO error occurs
     */
    @Nonnull
    public static DataSet readDataSet(DataInput input) throws IOException {
        final DataSet set = new DataSet();
        final Int2ObjectMap<Object> map = set.mIntMap;
        byte id;
        while ((id = input.readByte()) != VAL_NULL) {
            final int key = input.readInt();
            switch (id) {
                case VAL_BYTE -> map.put(key, Byte.valueOf(input.readByte()));
                case VAL_SHORT -> map.put(key, Short.valueOf(input.readShort()));
                case VAL_INT -> map.put(key, Integer.valueOf(input.readInt()));
                case VAL_LONG -> map.put(key, Long.valueOf(input.readLong()));
                case VAL_FLOAT -> map.put(key, Float.valueOf(input.readFloat()));
                case VAL_DOUBLE -> map.put(key, Double.valueOf(input.readDouble()));
                case VAL_STRING -> map.put(key, input.readUTF());
                case VAL_UUID -> map.put(key, new UUID(input.readLong(), input.readLong()));
                case VAL_LIST -> map.put(key, readList(input));
                case VAL_DATA_SET -> map.put(key, readDataSet(input));
                default -> throw new IOException("Unknown value type identifier: " + id);
            }
        }
        final Map<String, Object> stringMap = set.mStringMap;
        while ((id = input.readByte()) != VAL_NULL) {
            final String key = input.readUTF();
            switch (id) {
                case VAL_BYTE -> stringMap.put(key, input.readByte());
                case VAL_SHORT -> stringMap.put(key, input.readShort());
                case VAL_INT -> stringMap.put(key, input.readInt());
                case VAL_LONG -> stringMap.put(key, input.readLong());
                case VAL_FLOAT -> stringMap.put(key, input.readFloat());
                case VAL_DOUBLE -> stringMap.put(key, input.readDouble());
                case VAL_STRING -> stringMap.put(key, input.readUTF());
                case VAL_UUID -> stringMap.put(key, new UUID(input.readLong(), input.readLong()));
                case VAL_LIST -> stringMap.put(key, readList(input));
                case VAL_DATA_SET -> stringMap.put(key, readDataSet(input));
                default -> throw new IOException("Unknown value type identifier: " + id);
            }
        }
        return set;
    }
}
