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

package icyllis.modernui.resources;

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.util.DisplayMetrics;
import icyllis.modernui.util.Pools;
import icyllis.modernui.util.TypedValue;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;

import icyllis.modernui.resources.AssetManager.ResolvedBag;
import icyllis.modernui.resources.AssetManager.BagAttributeFinder;
import icyllis.modernui.resources.ResourceTypes.Res_value;

import java.util.Arrays;

@ApiStatus.Experimental
public class Resources {

    public static final Marker MARKER = MarkerManager.getMarker("Resources");
    public static final String DEFAULT_NAMESPACE = ModernUI.ID;

    private final DisplayMetrics mMetrics = new DisplayMetrics();

    final Pools.SynchronizedPool<TypedArray> mTypedArrayPool = new Pools.SynchronizedPool<>(5);

    String[] mKeyStrings;
    Object[] mGlobalObjects;

    String[] mStyleKeys;
    int[] mStyleOffsets;

    static final int MAP_ENTRY_HEADER_COLUMNS = 2;
    static final int MAP_COLUMNS = 3;

    static final int MAP_ENTRY_PARENT = 0;
    static final int MAP_ENTRY_COUNT = 1;
    static final int MAP_ENTRY_ENTRIES = 2;

    static final int MAP_NAME = 0;
    static final int MAP_DATA_TYPE = 1;
    static final int MAP_DATA = 2;

    /*
        this is defined as:
    struct map_entry {
        int32_t parent; // index of keyStrings
        int32_t count;
        struct map {
            uint32_t name;
            uint32_t dataType;
            uint32_t data;
        } entries[count];
    } data[0];
     */
    int[] mData;

    Object2ObjectOpenHashMap<String, ResolvedBag> mCachedBags = new Object2ObjectOpenHashMap<>();

    public Resources() {
        mMetrics.setToDefaults();
    }

    @ApiStatus.Internal
    public void updateMetrics(DisplayMetrics metrics) {
        if (metrics != null) {
            mMetrics.setTo(metrics);
        }
    }

    public final Theme newTheme() {
        return new Theme();
    }

    public DisplayMetrics getDisplayMetrics() {
        return mMetrics;
    }

    public void getValue(@NonNull CharSequence namespace, @NonNull CharSequence typeName,
                         @NonNull CharSequence entryName, @NonNull TypedValue outValue, boolean resolveRefs) {

    }

    @Nullable
    CharSequence getPooledStringForCookie(int cookie, int id) {
        if (cookie == 0 && id >= 0) {
            return (CharSequence) mGlobalObjects[id];
        }
        return null;
    }

    @SuppressWarnings("ConstantValue")
    ResolvedBag getBag(String style) {
        var cached = mCachedBags.get(style);
        if (cached != null) {
            return cached;
        }

        int entryIndex = Arrays.binarySearch(mStyleKeys, style);
        if (entryIndex < 0) {
            return null;
        }

        int offset = mStyleOffsets[entryIndex];
        int[] data = mData;

        int parentId = data[offset + MAP_ENTRY_PARENT];
        int entryCount = data[offset + MAP_ENTRY_COUNT];
        offset += MAP_ENTRY_HEADER_COLUMNS;
        if (parentId == -1) {
            // no parent
            ResolvedBag bag = new ResolvedBag();
            assert MAP_COLUMNS == ResolvedBag.VALUE_COLUMNS;
            if (entryCount > 0) {
                // TODO assert entries already sorted
                String[] keys = new String[entryCount * 2];
                int[] values = new int[entryCount * 3];
                for (int i = 0; i < entryCount; i++) {
                    keys[i*2+0] = DEFAULT_NAMESPACE;
                    keys[i*2+1] = mKeyStrings[data[offset + MAP_NAME]];
                    values[i*ResolvedBag.VALUE_COLUMNS+ResolvedBag.COLUMN_TYPE] = data[offset+MAP_DATA_TYPE];
                    values[i*ResolvedBag.VALUE_COLUMNS+ResolvedBag.COLUMN_DATA] = data[offset+MAP_DATA];
                    offset += MAP_COLUMNS;
                }
                bag.keys = keys;
                bag.values = values;
            }
            mCachedBags.put(style, bag);
            return bag;
        }

        var parentBag = getBag(mKeyStrings[parentId]);
        if (parentBag == null) {
            ModernUI.LOGGER.error(MARKER, "Failed to find parent '{}' of bag '{}'",
                    mKeyStrings[parentId], style);
            return null;
        }

        ResolvedBag newBag = new ResolvedBag();
        int parentCount = parentBag.getEntryCount();
        int maxCount = parentCount + entryCount;
        if (maxCount > 0) {
            String[] newKeys = new String[maxCount * 2];
            int[] newValues = new int[maxCount * ResolvedBag.VALUE_COLUMNS];
            int newIndex = 0;
            String[] parentKeys = parentBag.keys;
            int[] parentValues = parentBag.values;
            int childIndex = 0;
            int childOffset = offset;
            int parentIndex = 0;

            while (childIndex < entryCount && parentIndex < parentCount) {
                String childKey = mKeyStrings[data[childOffset + MAP_NAME]];
                String parentKey = parentKeys[parentIndex * 2 + 1];

                int keyCompare = childKey.compareTo(parentKey);

                if (keyCompare <= 0) {
                    // Use the child key if it comes before the parent
                    // or is equal to the parent (overrides).
                    newKeys[newIndex * 2 + 0] = DEFAULT_NAMESPACE;
                    newKeys[newIndex * 2 + 1] = childKey;
                    newValues[newIndex*ResolvedBag.VALUE_COLUMNS+ResolvedBag.COLUMN_TYPE] = data[childOffset + MAP_DATA_TYPE];
                    newValues[newIndex*ResolvedBag.VALUE_COLUMNS+ResolvedBag.COLUMN_DATA] = data[childOffset + MAP_DATA];
                    childIndex++;
                    childOffset += MAP_COLUMNS;
                } else {
                    newKeys[newIndex * 2 + 0] = DEFAULT_NAMESPACE;
                    newKeys[newIndex * 2 + 1] = parentKey;
                    System.arraycopy(parentValues, parentIndex * ResolvedBag.VALUE_COLUMNS,
                            newValues, newIndex * ResolvedBag.VALUE_COLUMNS, ResolvedBag.VALUE_COLUMNS);
                }

                // assert already sorted
                assert newIndex == 0 ||
                        newKeys[newIndex * 2 + 1].compareTo(newKeys[(newIndex - 1) * 2 + 1]) >= 0;

                if (keyCompare >= 0) {
                    parentIndex++;
                }
                newIndex++;
            }

            while (childIndex < entryCount) {
                String childKey = mKeyStrings[data[childOffset + MAP_NAME]];
                newKeys[newIndex * 2 + 0] = DEFAULT_NAMESPACE;
                newKeys[newIndex * 2 + 1] = childKey;
                newValues[newIndex*ResolvedBag.VALUE_COLUMNS+ResolvedBag.COLUMN_TYPE] = data[childOffset + MAP_DATA_TYPE];
                newValues[newIndex*ResolvedBag.VALUE_COLUMNS+ResolvedBag.COLUMN_DATA] = data[childOffset + MAP_DATA];
                childIndex++;
                childOffset += MAP_COLUMNS;
                newIndex++;
            }

            if (parentIndex < parentCount) {
                int numToCopy = parentCount - parentIndex;
                System.arraycopy(parentValues, parentIndex * ResolvedBag.VALUE_COLUMNS,
                        newValues, newIndex * ResolvedBag.VALUE_COLUMNS, numToCopy * ResolvedBag.VALUE_COLUMNS);
                for (int i = 0; i < numToCopy; i++) {
                    String parentKey = parentKeys[parentIndex * 2 + 1];
                    newKeys[newIndex * 2 + 0] = DEFAULT_NAMESPACE;
                    newKeys[newIndex * 2 + 1] = parentKey;
                    parentIndex++;
                    newIndex++;
                }
            }

            assert newIndex <= maxCount;
            if (newIndex < maxCount) {
                // trim to size
                newKeys = Arrays.copyOf(newKeys, newIndex * 2);
                newValues = Arrays.copyOf(newValues, newIndex * ResolvedBag.VALUE_COLUMNS);
            }

            newBag.keys = newKeys;
            newBag.values = newValues;
        }
        mCachedBags.put(style, newBag);
        return newBag;
    }

    public final class Theme {

        private final Object mLock = new Object();

        private final Resources.ThemeKey mKey = new Resources.ThemeKey();
        // a snapshot of key
        private Resources.ThemeKey mKeyCopy = mKey.clone();

        // open addressing
        Object2ObjectOpenHashMap<String, Entry> mEntries;

        /**
         * Place new attribute values into the theme.  The style resource
         * specified by <var>namespace</var>:<var>style</var> will be
         * retrieved from this Theme's resources, its values placed into the
         * Theme object.
         *
         * <p>The semantics of this function depends on the <var>force</var>
         * argument:  If false, only values that are not already defined in
         * the theme will be copied from the system resource; otherwise, if
         * any of the style's attributes are already defined in the theme, the
         * current values in the theme will be overwritten.
         *
         * @param namespace The namespace of a style resource from which to
         *                  obtain attribute values.
         * @param style     The name of a style resource from which to
         *                  obtain attribute values.
         * @param force     If true, values in the style resource will always be
         *                  used in the theme; otherwise, they will only be used
         *                  if not already defined in the theme.
         */
        public void applyStyle(@NonNull String namespace, @NonNull String style, boolean force) {
            synchronized (mLock) {
                boolean result = applyStyleInternal(namespace, style, force);
                if (!result) {
                    throw new IllegalArgumentException("Failed to apply style " +
                            ResourceId.toString(namespace, "style", style) + " to theme");
                }
                mKey.append(namespace, style, force);
                mKeyCopy = mKey.clone();
            }
        }

        private boolean applyStyleInternal(String namespace, String style, boolean force) {
            var bag = getBag(style);
            if (bag == null) {
                return false;
            }

            int entryCount = bag.getEntryCount();
            if (entryCount == 0) {
                return true;
            }

            boolean initial = mEntries == null;

            Object2ObjectOpenHashMap<String, Entry> newEntries = new Object2ObjectOpenHashMap<>(entryCount);
            for (int i = 0; i < entryCount; i++) {
                boolean isUndefined = isUndefined(bag.type(i), bag.data(i));
                if (!force && isUndefined) {
                    continue;
                }
                String key = bag.attribute(i);
                Entry existing;
                if (!initial && (existing = mEntries.get(key)) != null) {
                    if (force || isUndefined(existing.type, existing.data)) {
                        existing.set(bag, i);
                    }
                } else if (!isUndefined) {
                    Entry entry = new Entry();
                    entry.set(bag, i);
                    newEntries.put(key, entry);
                }
            }

            if (initial) {
                mEntries = newEntries;
            } else {
                mEntries.putAll(newEntries);
            }

            return true;
        }

        @NonNull
        public TypedArray obtainStyledAttributes(@Nullable @StyleRes String style,
                                                 @NonNull String[] attrs) {
            assert (attrs.length & 1) == 0;
            final int len = attrs.length >> 1;
            final TypedArray array = TypedArray.obtain(getResources(), len);

            synchronized (mLock) {
                applyStyle(style, attrs,
                        array.mData, array.mIndices);
            }
            return array;
        }

        static class Entry {
            int cookie;
            int typeSpecFlags;
            int type;
            int data;

            void set(@NonNull ResolvedBag bag, int index) {
                int offset = index * ResolvedBag.VALUE_COLUMNS;
                type = bag.values[offset + ResolvedBag.COLUMN_TYPE];
                data = bag.values[offset + ResolvedBag.COLUMN_DATA];
                cookie = bag.values[offset + ResolvedBag.COLUMN_COOKIE];
                typeSpecFlags = bag.typeSpecFlags;
            }
        }

        static boolean isUndefined(int type, int data) {
            return type == Res_value.TYPE_NULL && data != Res_value.DATA_NULL_EMPTY;
        }

        @Nullable
        AssetManager.SelectedValue getAttribute(String entryName) {
            if (mEntries == null) {
                return null;
            }
            int typeSpecFlags = 0;
            for (int i = 0; i < 20; i++) {
                Entry e = mEntries.get(entryName);
                if (e == null) {
                    return null;
                }
                if (isUndefined(e.type, e.data)) {
                    return null;
                }
                typeSpecFlags |= e.typeSpecFlags;
                if (e.type == Res_value.TYPE_ATTRIBUTE) {
                    entryName = mKeyStrings[e.data];
                    continue;
                }

                var value = new AssetManager.SelectedValue();
                value.cookie = e.cookie;
                value.flags = typeSpecFlags;
                value.type = e.type;
                value.data = e.data;
                return value;
            }
            return null;
        }

        boolean resolveAttributeReference(AssetManager.SelectedValue value) {
            if (value.type != Res_value.TYPE_ATTRIBUTE) {
                //TODO currently we have only style resources, where styles do not need resolve
                return true;
            }

            var result = getAttribute(mKeyStrings[value.data]);
            if (result == null) {
                return false;
            }

            value.cookie = result.cookie;
            value.type = result.type;
            value.data = result.data;
            value.flags |= result.flags;
            return true;
        }

        void applyStyle(@Nullable @StyleRes String style,
                        @NonNull String[] attrs,
                        @NonNull int[] outValues, @NonNull int[] outIndices) {

            ResolvedBag xmlStyleBag = null;
            BagAttributeFinder xmlStyleAttrFinder = null;
            if (style != null && !style.isEmpty()) {
                xmlStyleBag = getBag(style);
                xmlStyleAttrFinder = new BagAttributeFinder(xmlStyleBag);
            }

            AssetManager.SelectedValue value = new AssetManager.SelectedValue();

            int valuesIdx = 0;
            int indicesIdx = 0;

            for (int ii = 0; ii < attrs.length; ii += 2) {
                String curNs = attrs[ii];
                String curName = attrs[ii+1];

                value.reset();

                if (xmlStyleAttrFinder != null) {
                    int xmlAttrIdx = xmlStyleAttrFinder.find(curNs, curName);
                    if (xmlAttrIdx != -1) {
                        value.set(xmlStyleBag, xmlAttrIdx);
                    }
                }

                if (value.type != Res_value.TYPE_NULL) {
                    resolveAttributeReference(value);
                } else if (value.data != Res_value.DATA_NULL_EMPTY) {
                    var attrValue = getAttribute(curName);
                    if (attrValue != null) {
                        value.set(attrValue);
                    }
                }

                outValues[valuesIdx + TypedArray.STYLE_TYPE] = value.type;
                outValues[valuesIdx + TypedArray.STYLE_DATA] = value.data;
                outValues[valuesIdx + TypedArray.STYLE_ASSET_COOKIE] = value.cookie;
                outValues[valuesIdx + TypedArray.STYLE_CHANGING_CONFIGURATIONS] = value.flags;

                if (value.type != Res_value.TYPE_NULL || value.data == Res_value.DATA_NULL_EMPTY) {
                    outIndices[++indicesIdx] = ii>>1;
                }
                valuesIdx += TypedArray.STYLE_NUM_ENTRIES;
            }

            outIndices[0] = indicesIdx;
        }

        /**
         * Returns the resources to which this theme belongs.
         *
         * @return Resources to which this theme belongs.
         */
        public Resources getResources() {
            return Resources.this;
        }

        ThemeKey getKey() {
            synchronized (mLock) {
                return mKeyCopy;
            }
        }
    }

    static final class ThemeKey implements Cloneable {
        String[] mNamespace;
        String[] mThemeName;
        boolean[] mForce;

        private transient int mHashCode = 0;

        private int findValue(String namespace, String themeName, boolean force) {
            int count = mForce != null ? mForce.length : 0;
            for (int i = 0; i < count; i++) {
                if (mForce[i] == force &&
                        themeName.equals(mThemeName[i]) &&
                        namespace.equals(mNamespace[i])) {
                    return i;
                }
            }
            return -1;
        }

        private void moveToLast(int index) {
            int count = mForce.length;
            if (index < 0 || index >= count - 1) {
                return;
            }
            final String namespace = mNamespace[index];
            final String themeName = mThemeName[index];
            final boolean force = mForce[index];

            // we need copy-on-write

            final String[] newNamespace = new String[count];
            System.arraycopy(mNamespace, 0, newNamespace, 0, index);
            System.arraycopy(mNamespace, index + 1, newNamespace, index, count - index - 1);
            newNamespace[count - 1] = namespace;
            mNamespace = newNamespace;

            final String[] newThemeName = new String[count];
            System.arraycopy(mThemeName, 0, newThemeName, 0, index);
            System.arraycopy(mThemeName, index + 1, newThemeName, index, count - index - 1);
            newThemeName[count - 1] = themeName;
            mThemeName = newThemeName;

            final boolean[] newForce = new boolean[count];
            System.arraycopy(mForce, 0, newForce, 0, index);
            System.arraycopy(mForce, index + 1, newForce, index, count - index - 1);
            newForce[count - 1] = force;
            mForce = newForce;

            // recompute hashcode

            mHashCode = 0;
            for (int i = 0; i < count; i++) {
                mHashCode = 31 * mHashCode + newNamespace[i].hashCode();
                mHashCode = 31 * mHashCode + newThemeName[i].hashCode();
                mHashCode = 31 * mHashCode + (newForce[i] ? 1 : 0);
            }
        }

        public void append(String namespace, String themeName, boolean force) {
            final int index = findValue(namespace, themeName, force);
            if (index >= 0) {
                moveToLast(index);
            } else {
                int count = mForce != null ? mForce.length + 1 : 1;

                // we need copy-on-write

                final String[] newNamespace = mNamespace == null ? new String[count]
                        : Arrays.copyOf(mNamespace, count);
                newNamespace[count - 1] = namespace;
                mNamespace = newNamespace;

                final String[] newThemeName = mThemeName == null ? new String[count]
                        : Arrays.copyOf(mThemeName, count);
                newThemeName[count - 1] = themeName;
                mThemeName = newThemeName;

                final boolean[] newForce = mForce == null ? new boolean[count]
                        : Arrays.copyOf(mForce, count);
                newForce[count - 1] = force;
                mForce = newForce;

                mHashCode = 31 * mHashCode + namespace.hashCode();
                mHashCode = 31 * mHashCode + themeName.hashCode();
                mHashCode = 31 * mHashCode + (force ? 1 : 0);
            }
        }

        public void setTo(@NonNull ThemeKey other) {
            // A shallow copy, because we use copy-on-write
            mNamespace = other.mNamespace;
            mThemeName = other.mThemeName;
            mForce = other.mForce;
            mHashCode = other.mHashCode;
        }

        @Override
        public int hashCode() {
            return mHashCode;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ThemeKey t = (ThemeKey) o;

            if (!Arrays.equals(mForce, t.mForce)) {
                return false;
            }
            if (!Arrays.equals(mThemeName, t.mThemeName)) {
                return false;
            }
            return Arrays.equals(mNamespace, t.mNamespace);
        }

        // A shallow copy, because we use copy-on-write
        @Override
        public ThemeKey clone() {
            try {
                return (ThemeKey) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
