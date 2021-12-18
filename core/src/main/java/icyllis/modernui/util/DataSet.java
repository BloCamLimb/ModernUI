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
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A DataSet (sometimes known as DataStore) encapsulates a mapping from String keys
 * to values of various primitive types. The base class can safely be persisted to
 * and restored from local disk or network. A DataSet can be used as a node of other
 * DataSets to construct a tree structure.
 *
 * @see DataSetIO
 */
@ParametersAreNonnullByDefault
public class DataSet {

    public static final Marker MARKER = MarkerManager.getMarker("DataSet");

    final Object2ObjectMap<String, Object> mMap;

    /**
     * Create a new DataSet with a load factor of 0.8f.
     */
    public DataSet() {
        mMap = new Object2ObjectOpenHashMap<>(12, 0.8f);
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
        final String s = getString(key);
        return s == null ? defValue : s;
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
        final UUID u = getUUID(key);
        return u == null ? defValue : u;
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key.
     * <p>
     * Note that if the list elements are primitive types, you should use the methods
     * which are specified by primitive types.
     *
     * @param key the key whose associated value is to be returned
     * @return the List value to which the specified key is mapped, or null
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key) {
        final Object o = mMap.get(key);
        try {
            return (List<T>) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "List", e);
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
     * @return the List value to which the specified key is mapped
     */
    @Nonnull
    public <T> List<T> computeList(String key) {
        List<T> l = getList(key);
        if (l == null) {
            l = new ArrayList<>();
            putList(key, l);
        }
        return l;
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
        DataSet s = getDataSet(key);
        if (s == null) {
            s = new DataSet();
            putDataSet(key, s);
        }
        return s;
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
        ByteList l = getByteList(key);
        if (l != null) {
            return l;
        }
        l = new ByteArrayList();
        mMap.put(key, l);
        return l;
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
        ShortList l = getShortList(key);
        if (l != null) {
            return l;
        }
        l = new ShortArrayList();
        mMap.put(key, l);
        return l;
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
        IntList l = getIntList(key);
        if (l != null) {
            return l;
        }
        l = new IntArrayList();
        mMap.put(key, l);
        return l;
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
        LongList l = getLongList(key);
        if (l != null) {
            return l;
        }
        l = new LongArrayList();
        mMap.put(key, l);
        return l;
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
        FloatList l = getFloatList(key);
        if (l != null) {
            return l;
        }
        l = new FloatArrayList();
        mMap.put(key, l);
        return l;
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
        DoubleList l = getDoubleList(key);
        if (l != null) {
            return l;
        }
        l = new DoubleArrayList();
        mMap.put(key, l);
        return l;
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
        if (value instanceof ByteArrayList) {
            mMap.put(key, value);
        } else {
            throw new IllegalArgumentException("Not subclasses of ByteArrayList");
        }
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
        if (value instanceof ShortArrayList) {
            mMap.put(key, value);
        } else {
            throw new IllegalArgumentException("Not subclasses of ShortArrayList");
        }
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
        if (value instanceof IntArrayList) {
            mMap.put(key, value);
        } else {
            throw new IllegalArgumentException("Not subclasses of IntArrayList");
        }
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
        if (value instanceof LongArrayList) {
            mMap.put(key, value);
        } else {
            throw new IllegalArgumentException("Not subclasses of LongArrayList");
        }
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
        if (value instanceof FloatArrayList) {
            mMap.put(key, value);
        } else {
            throw new IllegalArgumentException("Not subclasses of FloatArrayList");
        }
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
        if (value instanceof DoubleArrayList) {
            mMap.put(key, value);
        } else {
            throw new IllegalArgumentException("Not subclasses of DoubleArrayList");
        }
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

    @Override
    public String toString() {
        final ObjectIterator<Object2ObjectMap.Entry<String, Object>> it = Object2ObjectMaps.fastIterator(mMap);
        if (!it.hasNext())
            return "{}";
        final StringBuilder s = new StringBuilder();
        s.append('{');
        for (; ; ) {
            Object2ObjectMap.Entry<String, Object> e = it.next();
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
}
