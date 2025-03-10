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
import icyllis.modernui.R;
import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.annotation.StyleableRes;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.resources.AssetManager.ResolvedBag;
import icyllis.modernui.resources.ResourceTypes.Res_value;
import icyllis.modernui.util.AttributeSet;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.DisplayMetrics;
import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.util.Arrays;
import java.util.function.BiFunction;

import static icyllis.modernui.resources.AssetManager.kMaxIterations;

@ApiStatus.Experimental
@SuppressWarnings("SuspiciousMethodCalls")
public class Resources {

    public static final Marker MARKER = MarkerManager.getMarker("Resources");
    public static final String DEFAULT_NAMESPACE = R.ns;

    private final DisplayMetrics mMetrics = new DisplayMetrics();

    final Pools.SynchronizedPool<TypedArray> mTypedArrayPool = new Pools.SynchronizedPool<>(5);

    String[] mNamespaces = {DEFAULT_NAMESPACE};
    String[] mTypeStrings;
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

    /**
     * This exception is thrown by the resource APIs when a requested resource
     * can not be found.
     */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException() {
        }

        public NotFoundException(String message) {
            super(message);
        }

        public NotFoundException(String message, Exception cause) {
            super(message, cause);
        }
    }

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
        if (cookie == 0 && id >= 0 && id < mGlobalObjects.length) {
            return (CharSequence) mGlobalObjects[id];
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    ColorStateList loadColorStateList(@NonNull TypedValue value,
                                      @Nullable ResourceId id,
                                      @Nullable Theme theme) {
        // Handle inline color definitions.
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return ColorStateList.valueOf(value.data);
        }

        if (value.type == TypedValue.TYPE_FACTORY) {
            Object object = mGlobalObjects[value.data];
            if (object instanceof BiFunction<?, ?, ?>) {
                object = ((BiFunction<Resources, Theme, ?>) object).apply(this, theme);
                if (object == null) {
                    return null;
                }
                if (object instanceof ColorStateList) {
                    return (ColorStateList) object;
                }
            }
            throw new NotFoundException("Resource is not a color: " + value);
        }

        String name = id != null ? id.toString() : "(missing name)";
        throw new NotFoundException(
                "Can't find ColorStateList from drawable " + name);
    }

    @SuppressWarnings("unchecked")
    Drawable loadDrawable(@NonNull TypedValue value,
                          @Nullable ResourceId id,
                          @Nullable Theme theme) {
        try {
            if (value.type == TypedValue.TYPE_FACTORY) {
                Object object = mGlobalObjects[value.data];
                if (object instanceof BiFunction<?, ?, ?>) {
                    object = ((BiFunction<Resources, Theme, ?>) object).apply(this, theme);
                    if (object == null) {
                        return null;
                    }
                    if (object instanceof Drawable) {
                        return (Drawable) object;
                    }
                }
                throw new NotFoundException("Resource is not a drawable: " + value);
            }

            //TODO
            return null;
        } catch (Exception e) {
            String name = id != null ? id.toString() : "(missing name)";

            // The target drawable might fail to load for any number of
            // reasons, but we always want to include the resource name.
            // Since the client already expects this method to throw a
            // NotFoundException, just throw one of those.
            final NotFoundException nfe = new NotFoundException("Drawable " + name, e);
            nfe.setStackTrace(new StackTraceElement[0]);
            throw nfe;
        }
    }

    ResolvedBag getBag(@NonNull ResourceId resId) {
        return getBag(resId.entry());
    }

    @SuppressWarnings("ConstantValue")
    ResolvedBag getBag(@NonNull String style) {
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
                    keys[i * 2 + 0] = DEFAULT_NAMESPACE;
                    keys[i * 2 + 1] = mKeyStrings[data[offset + MAP_NAME]];
                    values[i * ResolvedBag.VALUE_COLUMNS + ResolvedBag.COLUMN_TYPE] = data[offset + MAP_DATA_TYPE];
                    values[i * ResolvedBag.VALUE_COLUMNS + ResolvedBag.COLUMN_DATA] = data[offset + MAP_DATA];
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
            // allocate max possible array
            String[] newKeys = new String[maxCount * 2];
            int[] newValues = new int[maxCount * ResolvedBag.VALUE_COLUMNS];
            int newIndex = 0;
            String[] parentKeys = parentBag.keys;
            int[] parentValues = parentBag.values;
            int childIndex = 0;
            int childOffset = offset;
            int parentIndex = 0;

            // merge two sorted arrays (parent and child)

            while (childIndex < entryCount && parentIndex < parentCount) {
                String childKey = mKeyStrings[data[childOffset + MAP_NAME]];
                String parentKey = parentKeys[parentIndex * 2 + 1];

                int keyCompare = childKey.compareTo(parentKey);

                if (keyCompare <= 0) {
                    // Use the child key if it comes before the parent
                    // or is equal to the parent (overrides).
                    newKeys[newIndex * 2 + 0] = DEFAULT_NAMESPACE;
                    newKeys[newIndex * 2 + 1] = childKey;
                    newValues[newIndex * ResolvedBag.VALUE_COLUMNS + ResolvedBag.COLUMN_TYPE] =
                            data[childOffset + MAP_DATA_TYPE];
                    newValues[newIndex * ResolvedBag.VALUE_COLUMNS + ResolvedBag.COLUMN_DATA] =
                            data[childOffset + MAP_DATA];
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
                newValues[newIndex * ResolvedBag.VALUE_COLUMNS + ResolvedBag.COLUMN_TYPE] =
                        data[childOffset + MAP_DATA_TYPE];
                newValues[newIndex * ResolvedBag.VALUE_COLUMNS + ResolvedBag.COLUMN_DATA] =
                        data[childOffset + MAP_DATA];
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

        // open addressing, namespace -> attribute -> entry
        Object2ObjectOpenHashMap<String, Object2ObjectOpenHashMap<String, Entry>> mEntries;

        /**
         * Place new attribute values into the theme.  The style resource
         * specified by <var>resId</var> will be
         * retrieved from this Theme's resources, its values placed into the
         * Theme object.
         *
         * <p>The semantics of this function depends on the <var>force</var>
         * argument:  If false, only values that are not already defined in
         * the theme will be copied from the system resource; otherwise, if
         * any of the style's attributes are already defined in the theme, the
         * current values in the theme will be overwritten.
         *
         * @param resId The resource ID of a style resource from which to
         *              obtain attribute values.
         * @param force If true, values in the style resource will always be
         *              used in the theme; otherwise, they will only be used
         *              if not already defined in the theme.
         * @throws IllegalArgumentException if the resId is not a style or not found
         */
        public void applyStyle(@NonNull @StyleRes ResourceId resId, boolean force) {
            synchronized (mLock) {
                boolean result = applyStyleInternal(resId, force);
                if (!result) {
                    throw new IllegalArgumentException("Failed to apply style " +
                            resId + " to theme");
                }
                mKey.append(resId, force);
                mKeyCopy = mKey.clone();
            }
        }

        private boolean applyStyleInternal(@NonNull ResourceId resId, boolean force) {
            var bag = getBag(resId);
            if (bag == null) {
                return false;
            }

            int entryCount = bag.getEntryCount();
            if (entryCount == 0) {
                return true;
            }

            boolean initial = mEntries == null;

            Object2ObjectOpenHashMap<String, Object2ObjectOpenHashMap<String, Entry>> newEntries =
                    new Object2ObjectOpenHashMap<>();
            for (int i = 0; i < entryCount; i++) {
                boolean isUndefined = isUndefined(bag.type(i), bag.data(i));
                if (!force && isUndefined) {
                    continue;
                }
                String namespace = bag.namespace(i);
                String attribute = bag.attribute(i);
                Object2ObjectOpenHashMap<String, Entry> group;
                Entry existing;
                if (!initial && (group = mEntries.get(namespace)) != null && (existing = group.get(attribute)) != null) {
                    if (force || isUndefined(existing.type, existing.data)) {
                        existing.set(bag, i);
                    }
                } else if (!isUndefined) {
                    Entry entry = new Entry();
                    entry.set(bag, i);
                    newEntries.computeIfAbsent(namespace, __ -> new Object2ObjectOpenHashMap<>())
                            .put(attribute, entry);
                }
            }

            if (initial) {
                mEntries = newEntries;
            } else {
                for (var it = newEntries.object2ObjectEntrySet().fastIterator(); it.hasNext(); ) {
                    var group = it.next();
                    mEntries.computeIfAbsent(group.getKey(), __ -> new Object2ObjectOpenHashMap<>())
                            .putAll(group.getValue());
                }
            }

            return true;
        }

        /**
         * Return a TypedArray holding the values defined by
         * <var>Theme</var> which are listed in <var>attrs</var>.
         *
         * <p>Be sure to call {@link TypedArray#recycle() TypedArray.recycle()} when you are done
         * with the array.
         *
         * @param attrs The desired attributes to be retrieved. See {@link StyleableRes} for requirements.
         * @return Returns a TypedArray holding an array of the attribute values.
         * Be sure to call {@link TypedArray#recycle() TypedArray.recycle()}
         * when done with it.
         * @see Resources#obtainAttributes
         * @see #obtainStyledAttributes(ResourceId, String[])
         * @see #obtainStyledAttributes(AttributeSet, ResourceId, ResourceId, String[])
         */
        @NonNull
        public TypedArray obtainStyledAttributes(@NonNull @StyleableRes String[] attrs) {
            return obtainStyledAttributes(null, null, null, attrs);
        }

        /**
         * Return a TypedArray holding the values defined by the <var>style</var>
         * resource which are listed in <var>attrs</var>.
         *
         * <p>Be sure to call {@link TypedArray#recycle() TypedArray.recycle()} when you are done
         * with the array.
         *
         * @param style The desired style resource.
         * @param attrs The desired attributes to be retrieved. See
         *              {@link StyleableRes} for requirements.
         * @return Returns a TypedArray holding an array of the attribute values.
         * Be sure to call {@link TypedArray#recycle() TypedArray.recycle()}
         * when done with it.
         * @see Resources#obtainAttributes
         * @see #obtainStyledAttributes(ResourceId, String[])
         * @see #obtainStyledAttributes(AttributeSet, ResourceId, ResourceId, String[])
         */
        @NonNull
        public TypedArray obtainStyledAttributes(@Nullable @StyleRes ResourceId style,
                                                 @NonNull @StyleableRes String[] attrs) {
            return obtainStyledAttributes(null, null, style, attrs);
        }

        /**
         * Return a TypedArray holding the attribute values in
         * <var>set</var>
         * that are listed in <var>attrs</var>.  In addition, if the given
         * AttributeSet specifies a style class (through the "style" attribute),
         * that style will be applied on top of the base attributes it defines.
         *
         * <p>Be sure to call {@link TypedArray#recycle() TypedArray.recycle()} when you are done
         * with the array.
         *
         * <p>When determining the final value of a particular attribute, there
         * are four inputs that come into play:</p>
         *
         * <ol>
         *     <li> Any attribute values in the given AttributeSet.
         *     <li> The style resource specified in the AttributeSet (named
         *     "style").
         *     <li> The default style specified by <var>defStyleAttr</var> and
         *     <var>defStyleRes</var>
         *     <li> The base values in this theme.
         * </ol>
         *
         * <p>Each of these inputs is considered in-order, with the first listed
         * taking precedence over the following ones.  In other words, if in the
         * AttributeSet you have supplied <code>&lt;Button
         * textColor="#ff000000"&gt;</code>, then the button's text will
         * <em>always</em> be black, regardless of what is specified in any of
         * the styles.
         *
         * @param set          The base set of attribute values.  May be null.
         * @param defStyleAttr An attribute in the current theme that contains a
         *                     reference to a style resource that supplies
         *                     defaults values for the TypedArray.  Can be
         *                     null to not look for defaults.
         * @param defStyleRes  A resource identifier of a style resource that
         *                     supplies default values for the TypedArray,
         *                     used only if defStyleAttr is null or can not be found
         *                     in the theme.  Can be null to not look for defaults.
         * @param attrs        The desired attributes to be retrieved. See
         *                     {@link StyleableRes} for requirements.
         * @return Returns a TypedArray holding an array of the attribute values.
         * Be sure to call {@link TypedArray#recycle() TypedArray.recycle()}
         * when done with it.
         * @see Resources#obtainAttributes
         * @see #obtainStyledAttributes(String[])
         * @see #obtainStyledAttributes(ResourceId, String[])
         */
        @NonNull
        public TypedArray obtainStyledAttributes(@Nullable AttributeSet set,
                                                 @Nullable @AttrRes ResourceId defStyleAttr,
                                                 @Nullable @StyleRes ResourceId defStyleRes,
                                                 @NonNull @StyleableRes String[] attrs) {
            assert defStyleAttr == null || defStyleAttr.type().equals("attr");
            assert (attrs.length & 1) == 0;
            final int len = attrs.length >> 1;
            final TypedArray array = TypedArray.obtain(getResources(), len);

            synchronized (mLock) {
                applyStyle(set, defStyleAttr, defStyleRes,
                        attrs, array.mData, array.mIndices,
                        array.mValue,
                        array.mDefStyleAttrFinder);
            }
            array.mDefStyleAttrFinder.clear();
            return array;
        }

        /**
         * Retrieve the value of an attribute in the Theme.  The contents of
         * <var>outValue</var> are ultimately filled in by
         * {@link Resources#getValue}.
         *
         * @param resId       The resource identifier of the desired theme
         *                    attribute.
         * @param outValue    Filled in with the ultimate resource value supplied
         *                    by the attribute.
         * @param resolveRefs If true, resource references will be walked; if
         *                    false, <var>outValue</var> may be a
         *                    TYPE_REFERENCE.  In either case, it will never
         *                    be a TYPE_ATTRIBUTE.
         * @return boolean Returns true if the attribute was found and
         * <var>outValue</var> is valid, else false.
         */
        public boolean resolveAttribute(@NonNull @AttrRes ResourceId resId,
                                        @NonNull TypedValue outValue, boolean resolveRefs) {
            assert resId.type().equals("attr");
            return resolveAttribute(resId.namespace(), resId.entry(), outValue, resolveRefs);
        }

        /**
         * Retrieve the value of an attribute in the Theme.  The contents of
         * <var>outValue</var> are ultimately filled in by
         * {@link Resources#getValue}.
         *
         * @param outValue    Filled in with the ultimate resource value supplied
         *                    by the attribute.
         * @param resolveRefs If true, resource references will be walked; if
         *                    false, <var>outValue</var> may be a
         *                    TYPE_REFERENCE.  In either case, it will never
         *                    be a TYPE_ATTRIBUTE.
         * @return boolean Returns true if the attribute was found and
         * <var>outValue</var> is valid, else false.
         */
        public boolean resolveAttribute(@NonNull String namespace, @NonNull String attribute,
                                        @NonNull TypedValue outValue, boolean resolveRefs) {
            synchronized (mLock) {
                if (!getAttribute(namespace, attribute, outValue)) {
                    return false;
                }

                if (resolveRefs) {
                    //TODO
                }

                // post-process
                switch (outValue.type & Res_value.TYPE_MASK) {
                    case TypedValue.TYPE_STRING -> {
                        if ((outValue.object = getPooledStringForCookie(outValue.cookie, outValue.data)) == null) {
                            return false;
                        }
                    }
                    case TypedValue.TYPE_REFERENCE -> {
                        int typeId = outValue.type >>> Res_value.TYPE_ID_SHIFT;
                        if (typeId != 0) {
                            outValue.object = new ResourceId(
                                    mNamespaces[outValue.data >>> Res_value.NAMESPACE_INDEX_SHIFT],
                                    mTypeStrings[typeId - 1],
                                    mKeyStrings[outValue.data & Res_value.KEY_INDEX_MASK]
                            );
                            // erase higher 8 bits
                            outValue.type = TypedValue.TYPE_REFERENCE;
                        } else {
                            outValue.object = null;
                        }
                    }
                    case TypedValue.TYPE_ATTRIBUTE -> outValue.object = ResourceId.attr(
                            mNamespaces[outValue.data >>> Res_value.NAMESPACE_INDEX_SHIFT],
                            mKeyStrings[outValue.data & Res_value.KEY_INDEX_MASK]
                    );
                    default -> outValue.object = null;
                }

                return true;
            }
        }

        static class Entry {
            int type;
            int data;
            int cookie;
            int typeSpecFlags;

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

        boolean getAttribute(@NonNull String namespace, @NonNull String attribute,
                             @NonNull TypedValue outValue) {
            if (mEntries == null) {
                return false;
            }
            int typeSpecFlags = 0;
            for (int __ = 0; __ < kMaxIterations; __++) {
                Object2ObjectOpenHashMap<String, Entry> group;
                Entry e;
                if ((group = mEntries.get(namespace)) == null || (e = group.get(attribute)) == null) {
                    return false;
                }
                if (isUndefined(e.type, e.data)) {
                    return false;
                }
                typeSpecFlags |= e.typeSpecFlags;
                if (e.type == Res_value.TYPE_ATTRIBUTE) {
                    namespace = mNamespaces[e.data >>> Res_value.NAMESPACE_INDEX_SHIFT];
                    attribute = mKeyStrings[e.data & Res_value.KEY_INDEX_MASK];
                    continue;
                }

                outValue.cookie = e.cookie;
                outValue.flags = typeSpecFlags;
                outValue.type = e.type;
                outValue.data = e.data;
                return true;
            }
            return false;
        }

        boolean resolveAttributeReference(@NonNull TypedValue value) {
            if (value.type != Res_value.TYPE_ATTRIBUTE) {
                //TODO currently we have only style resources, where styles do not need resolve
                return true;
            }

            int flags = value.flags;
            String namespace = mNamespaces[value.data >>> Res_value.NAMESPACE_INDEX_SHIFT];
            String attribute = mKeyStrings[value.data & Res_value.KEY_INDEX_MASK];
            if (!getAttribute(namespace, attribute, value)) {
                return false;
            }

            value.flags |= flags;
            return true;
        }

        void applyStyle(@Nullable AttributeSet set,
                        @Nullable @AttrRes ResourceId defStyleAttr,
                        @Nullable @StyleRes ResourceId defStyleRes,
                        @NonNull String[] attrs,
                        @NonNull int[] outValues, @NonNull int[] outIndices,
                        @NonNull TypedValue value,
                        @NonNull BagAttributeFinder defStyleAttrFinder) {

            AssetManager.ResolvedBag defStyleBag = null;
            if (defStyleAttr != null) {
                if (getAttribute(defStyleAttr.namespace(), defStyleAttr.entry(), value)) {
                    defStyleBag = getBag(mKeyStrings[value.data]);
                }
            }
            if (defStyleBag == null && defStyleRes != null) {
                defStyleBag = getBag(defStyleRes);
            }
            defStyleAttrFinder.reset(defStyleBag);

            int valuesIdx = 0;
            int indicesIdx = 0;

            for (int ii = 0; ii < attrs.length; ii += 2) {
                String curNs = attrs[ii];
                String curAttr = attrs[ii + 1];

                value.reset();

                int defAttrIdx = defStyleAttrFinder.find(curNs, curAttr);
                if (defAttrIdx != -1) {
                    assert defStyleBag != null;
                    value.setTo(defStyleBag, defAttrIdx);
                }

                if (value.type != Res_value.TYPE_NULL) {
                    resolveAttributeReference(value);
                } else if (value.data != Res_value.DATA_NULL_EMPTY) {
                    if (getAttribute(curNs, curAttr, value)) {

                    }
                }

                outValues[valuesIdx + TypedArray.STYLE_TYPE] = value.type;
                outValues[valuesIdx + TypedArray.STYLE_DATA] = value.data;
                outValues[valuesIdx + TypedArray.STYLE_COOKIE] = value.cookie;
                outValues[valuesIdx + TypedArray.STYLE_FLAGS] = value.flags;

                if (value.type != Res_value.TYPE_NULL || value.data == Res_value.DATA_NULL_EMPTY) {
                    outIndices[++indicesIdx] = ii >> 1;
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

        /**
         * @hidden
         */
        @NonNull
        @Contract(pure = true)
        @ApiStatus.Internal
        public String[] getTheme() {
            final ThemeKey key = getKey();
            final int n = key.mForce != null ? key.mForce.length : 0;
            final String[] themes = new String[n * 2];
            for (int i = 0, j = n - 1; i < themes.length; i += 2, --j) {
                themes[i] = key.mResId[j].toString();
                themes[i + 1] = key.mForce[j] ? "forced" : "not forced";
            }
            return themes;
        }

        @Override
        public int hashCode() {
            return getKey().hashCode();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Theme other = (Theme) o;
            return getKey().equals(other.getKey());
        }

        @NonNull
        @Override
        public String toString() {
            return '{' +
                    "Themes=" + Arrays.deepToString(getTheme()) +
                    '}';
        }

        ThemeKey getKey() {
            synchronized (mLock) {
                return mKeyCopy;
            }
        }
    }

    static final class ThemeKey implements Cloneable {
        Object[] mResId;
        boolean[] mForce;

        private transient int mHashCode = 0;

        private int findValue(@NonNull ResourceId resId, boolean force) {
            int count = mForce != null ? mForce.length : 0;
            for (int i = 0; i < count; i++) {
                if (mForce[i] == force && resId.equals(mResId[i])) {
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
            final Object resId = mResId[index];
            final boolean force = mForce[index];

            // we need copy-on-write

            final Object[] newResId = new Object[count];
            System.arraycopy(mResId, 0, newResId, 0, index);
            System.arraycopy(mResId, index + 1, newResId, index, count - index - 1);
            newResId[count - 1] = resId;
            mResId = newResId;

            final boolean[] newForce = new boolean[count];
            System.arraycopy(mForce, 0, newForce, 0, index);
            System.arraycopy(mForce, index + 1, newForce, index, count - index - 1);
            newForce[count - 1] = force;
            mForce = newForce;

            // recompute hashcode

            mHashCode = 0;
            for (int i = 0; i < count; i++) {
                mHashCode = 31 * mHashCode + newResId[i].hashCode();
                mHashCode = 31 * mHashCode + (newForce[i] ? 1 : 0);
            }
        }

        public void append(@NonNull ResourceId resId, boolean force) {
            final int index = findValue(resId, force);
            if (index >= 0) {
                moveToLast(index);
            } else {
                int newCount = mForce != null ? mForce.length + 1 : 1;

                // we need copy-on-write

                final Object[] newResId = mResId == null ? new Object[newCount]
                        : Arrays.copyOf(mResId, newCount);
                newResId[newCount - 1] = resId;
                mResId = newResId;

                final boolean[] newForce = mForce == null ? new boolean[newCount]
                        : Arrays.copyOf(mForce, newCount);
                newForce[newCount - 1] = force;
                mForce = newForce;

                mHashCode = 31 * mHashCode + resId.hashCode();
                mHashCode = 31 * mHashCode + (force ? 1 : 0);
            }
        }

        public void setTo(@NonNull ThemeKey other) {
            // A shallow copy, because we use copy-on-write
            mResId = other.mResId;
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
            return Arrays.equals(mResId, t.mResId);
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
