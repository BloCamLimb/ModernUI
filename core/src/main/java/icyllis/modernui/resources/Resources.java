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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static icyllis.modernui.resources.AssetManager.kMaxIterations;

public class Resources {

    public static final Marker MARKER = MarkerFactory.getMarker("Resources");

    private static final Object sLock = new Object();

    @GuardedBy("sLock")
    private static Resources sSystem;

    private ResourcesImpl mResourcesImpl;

    final Pools.SynchronizedPool<TypedArray> mTypedArrayPool = new Pools.SynchronizedPool<>(5);





    private static final VarHandle TMP_VALUE;

    static {
        try {
            TMP_VALUE = MethodHandles.lookup().findVarHandle(Resources.class, "mTmpValue", TypedValue.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("FieldMayBeFinal")
    private volatile TypedValue mTmpValue = new TypedValue();

    /**
     * WeakReferences to Themes that were constructed from this Resources object.
     * We keep track of these in case our underlying implementation is changed, in which case
     * the Themes must also get updated ThemeImpls.
     */
    @GuardedBy("itself")
    private final ArrayList<WeakReference<Theme>> mThemeRefs = new ArrayList<>();

    /**
     * Returns a default theme for the framework.
     *
     * @hidden
     * @param curTheme The current theme, or null if not specified.
     * @return A theme resource identifier
     */
    @ApiStatus.Internal
    public static ResourceId selectDefaultTheme(ResourceId curTheme) {
        if (curTheme != null) {
            return curTheme;
        }
        return R.style.Theme_Material3_Light;
    }

    /**
     * This exception is thrown by the resource APIs when a requested resource
     * can not be found.
     */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException() {
        }

        public NotFoundException(@Nullable String message) {
            super(message);
        }

        public NotFoundException(@Nullable String message, @Nullable Exception cause) {
            super(message, cause);
        }
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public interface UpdateCallbacks extends ResourcesLoader.UpdateCallbacks {
        /**
         * Invoked when a {@link Resources} instance has a {@link ResourcesLoader} added, removed,
         * or reordered.
         *
         * @param resources the instance being updated
         * @param newLoaders the new set of loaders for the instance
         */
        void onLoadersChanged(@NonNull Resources resources,
                              @NonNull List<ResourcesLoader> newLoaders);
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public Resources(@NonNull AssetManager assetManager, @Nullable DisplayMetrics metrics,
                     @Nullable Configuration configuration) {
        this(null);
        mResourcesImpl = new ResourcesImpl(assetManager, metrics, configuration);
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public Resources(@Nullable ClassLoader classLoader) {

    }

    private Resources() {
        mResourcesImpl = new ResourcesImpl(
                AssetManager.getSystem(),
                null, null
        );
    }

    @NonNull
    public static Resources getSystem() {
        synchronized (sLock) {
            Resources ret = sSystem;
            if (ret == null) {
                ret = new Resources();
                sSystem = ret;
            }
            return ret;
        }
    }

    /**
     * @hide
     * @hidden
     */
    // called from UI thread only
    @ApiStatus.Internal
    public void setImpl(ResourcesImpl impl) {
        if (impl == mResourcesImpl) {
            return;
        }

        // Modern UI changed:
        // the assignment operation is moved inside the lock, just in case newTheme() cannot
        // reference the latest Impl
        synchronized (mThemeRefs) {
            // ensure all themes are updated to the new ResourcesImpl
            mResourcesImpl = impl;
            var iter = mThemeRefs.iterator();
            while (iter.hasNext()) {
                var theme = iter.next().get();
                if (theme == null)
                    iter.remove();
                else
                    theme.rebase(impl);
            }
        }
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public ResourcesImpl getImpl() {
        return mResourcesImpl;
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public void updateConfiguration(Configuration config, DisplayMetrics metrics) {
        mResourcesImpl.updateConfiguration(config, metrics);
    }

    /**
     * Generate a new Theme object for this set of Resources.  It initially
     * starts out empty.
     * <p>
     * This method can be called from any thread.
     *
     * @return the newly created Theme
     */
    @NonNull
    public final Theme newTheme() {
        synchronized (mThemeRefs) {
            // put constructor inside lock to avoid race between newTheme and setImpl
            var theme = new Theme(this, mResourcesImpl);
            // AOSP's nextPowerOf2() is wrongly implemented, just don't use that and always
            // clean up the whole list
            mThemeRefs.removeIf(r -> r.refersTo(null));
            mThemeRefs.add(new WeakReference<>(theme));
            return theme;
        }
    }

    /**
     * Returns the current display metrics that are in effect for this resources
     * object. The returned object should be treated as read-only.
     */
    @UnmodifiableView
    public DisplayMetrics getDisplayMetrics() {
        return mResourcesImpl.getDisplayMetrics();
    }

    /**
     * Return the current configuration that is in effect for this resource
     * object. The returned object should be treated as read-only.
     */
    @UnmodifiableView
    public Configuration getConfiguration() {
        return mResourcesImpl.getConfiguration();
    }

    @ApiStatus.Experimental
    public void getValue(@NonNull ResourceId id, @NonNull TypedValue outValue,
                         boolean resolveRefs) throws NotFoundException {
        mResourcesImpl.getValue(id, outValue, resolveRefs);
    }

    @NonNull
    public ColorStateList getColorStateList(@NonNull ResourceId id,
                                            @Nullable Theme theme)
            throws NotFoundException {
        TypedValue tmp = (TypedValue) TMP_VALUE.getAndSetAcquire(this, null);
        try {
            TypedValue val = tmp != null ? tmp : new TypedValue();
            return getColorStateList(id, val, theme);
        } finally {
            if (tmp != null) {
                TMP_VALUE.setRelease(this, tmp);
            }
        }
    }

    @NonNull
    public ColorStateList getColorStateList(@NonNull ResourceId id,
                                            @NonNull TypedValue value,
                                            @Nullable Theme theme)
            throws NotFoundException {
        ResourcesImpl impl = mResourcesImpl;
        impl.getValue(id, value, true);
        return impl.loadColorStateList(this, value, id, theme);
    }

    @NonNull
    public ColorStateList loadColorStateList(@NonNull TypedValue value,
                                             @Nullable ResourceId id,
                                             @Nullable Theme theme)
            throws NotFoundException {
        return mResourcesImpl.loadColorStateList(this, value, id, theme);
    }

    @NonNull
    public Drawable loadDrawable(@NonNull TypedValue value,
                                 @Nullable ResourceId id,
                                 @Nullable Theme theme)
            throws NotFoundException {
        return mResourcesImpl.loadDrawable(this, value, id, theme);
    }

    @NonNull
    public Asset getRawResource(@NonNull ResourceId id)
            throws NotFoundException {
        TypedValue tmp = (TypedValue) TMP_VALUE.getAndSetAcquire(this, null);
        try {
            TypedValue val = tmp != null ? tmp : new TypedValue();
            return getRawResource(id, val);
        } finally {
            if (tmp != null) {
                TMP_VALUE.setRelease(this, tmp);
            }
        }
    }

    @NonNull
    public Asset getRawResource(@NonNull ResourceId id, @NonNull TypedValue value)
            throws NotFoundException {
        ResourcesImpl impl = mResourcesImpl;
        impl.getValue(id, value, true);
        return impl.loadRawResource(value, id);
    }

    @NonNull
    public Asset loadRawResource(@NonNull TypedValue value, @Nullable ResourceId id)
            throws NotFoundException {
        return mResourcesImpl.loadRawResource(value, id);
    }

    /**
     * Performs a sequential search across selected asset packs for a specific path.
     * Returns the asset reference on the first match, or {@code null} otherwise.
     * Enables access to the "assets" directory.
     * <p>
     * A valid path must:
     * <ul>
     * <li>use only forward slashes as separators, not backslashes</li>
     * <li>contain only lowercase letters a to z, digits 0 to 9, dash (-), dot (.), underscore (_) in each segment</li>
     * <li>not start with or end with a slash</li>
     * <li>not use single dot (.) or dotdot (..) as a path segment</li>
     * <li>not contain consecutive slashes (//)</li>
     * </ul>
     *
     * @param path the path of the asset to search
     * @return a reference to the asset, or null if not found
     */
    @Nullable
    public Asset getAsset(@NonNull String path) {
        return mResourcesImpl.mAssetManager.getNonAsset("assets/" + path);
    }





    @ThreadSafe
    public static final class Theme {

        private final Object mLock = new Object();

        private final Resources mResources;
        @GuardedBy("mLock")
        private ResourcesImpl mResourcesImpl;

        @GuardedBy("mLock")
        private final Resources.ThemeKey mKey = new Resources.ThemeKey();
        // a snapshot of key
        @GuardedBy("mLock")
        private Resources.ThemeKey mKeyCopy = mKey.clone();

        // open addressing, namespace -> attribute -> entry
        @GuardedBy("mLock")
        private Object2ObjectOpenHashMap<String, Object2ObjectOpenHashMap<String, Entry>> mEntries;

        Theme(@NonNull Resources resources, ResourcesImpl resourcesImpl) {
            mResourcesImpl = resourcesImpl;
            mResources = resources;
        }

        void rebase(ResourcesImpl resourcesImpl) {
            synchronized (mLock) {
                mResourcesImpl = resourcesImpl;
                //TODO invalidate mEntries
            }
        }

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

        @GuardedBy("mLock")
        private boolean applyStyleInternal(@NonNull ResourceId resId, boolean force) {
            var bag = mResourcesImpl.mAssetManager.getBag(resId);
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
                    newEntries.computeIfAbsent(namespace, Theme::newHashMap)
                            .put(attribute, entry);
                }
            }

            if (initial) {
                mEntries = newEntries;
            } else {
                for (var it = newEntries.object2ObjectEntrySet().fastIterator(); it.hasNext(); ) {
                    var group = it.next();
                    mEntries.computeIfAbsent(group.getKey(), Theme::newHashMap)
                            .putAll(group.getValue());
                }
            }

            return true;
        }

        private static <K, V, X> Object2ObjectOpenHashMap<K, V> newHashMap(X __) {
            return new Object2ObjectOpenHashMap<>();
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
         * Return a TypedArray holding the values defined by the <var>resId</var>
         * resource which are listed in <var>attrs</var>.
         *
         * <p>Be sure to call {@link TypedArray#recycle() TypedArray.recycle()} when you are done
         * with the array.
         *
         * @param resId The desired style resource.
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
        public TypedArray obtainStyledAttributes(@Nullable @StyleRes ResourceId resId,
                                                 @NonNull @StyleableRes String[] attrs) {
            return obtainStyledAttributes(null, null, resId, attrs);
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
            array.mTheme = this;
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

                int cookie = outValue.cookie;
                assert cookie >= 0;

                if (resolveRefs) {
                    //TODO
                }

                return mResourcesImpl.postProcess(outValue);
            }
        }

        static final class Entry implements Cloneable {
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

            @Override
            public Entry clone() {
                try {
                    return (Entry) super.clone();
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        static boolean isUndefined(int type, int data) {
            return type == Res_value.TYPE_NULL && data != Res_value.DATA_NULL_EMPTY;
        }

        @GuardedBy("mLock")
        private boolean getAttribute(
                             @NonNull String namespace, @NonNull String attribute,
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
                    LoadedResources loadedResources = mResourcesImpl.mAssetManager.getLoadedResources(e.cookie);
                    if (loadedResources != null) {
                        namespace = loadedResources.lookupPackageName(e.data >>> Res_value.PACKAGE_ID_SHIFT);
                        attribute = loadedResources.getKeyStringPool().getStringAt(e.data & Res_value.KEY_INDEX_MASK);
                        if (namespace != null && attribute != null) {
                            continue;
                        }
                    }
                    return false;
                }

                outValue.cookie = e.cookie;
                outValue.flags = typeSpecFlags;
                outValue.type = e.type;
                outValue.data = e.data;
                return true;
            }
            return false;
        }

        private boolean resolveAttributeReference(@NonNull TypedValue value) {
            if (value.type != Res_value.TYPE_ATTRIBUTE) {
                //TODO currently we have only style resources, where styles do not need resolve
                return true;
            }

            LoadedResources loadedResources = mResourcesImpl.mAssetManager.getLoadedResources(value.cookie);
            if (loadedResources == null) {
                return false;
            }
            String namespace = loadedResources.lookupPackageName(value.data >>> Res_value.PACKAGE_ID_SHIFT);
            String attribute = loadedResources.getKeyStringPool().getStringAt(value.data & Res_value.KEY_INDEX_MASK);
            if (namespace == null || attribute == null) {
                return false;
            }

            int flags = value.flags;
            if (!getAttribute(namespace, attribute, value)) {
                return false;
            }

            value.flags |= flags;
            return true;
        }

        @GuardedBy("mLock")
        private void applyStyle(
                        @Nullable AttributeSet set,
                        @Nullable @AttrRes ResourceId defStyleAttr,
                        @Nullable @StyleRes ResourceId defStyleRes,
                        @NonNull String[] attrs,
                        @NonNull int[] outValues, @NonNull int[] outIndices,
                        @NonNull TypedValue value,
                        @NonNull BagAttributeFinder defStyleAttrFinder) {

            AssetManager.ResolvedBag defStyleBag = null;
            if (defStyleAttr != null) {
                if (getAttribute(defStyleAttr.namespace(), defStyleAttr.entry(), value)) {
                    LoadedResources loadedResources = mResourcesImpl.mAssetManager.getLoadedResources(value.cookie);
                    if (loadedResources != null) {
                        ResourceId styleId = loadedResources.lookupResourceId(null,
                                value.data, (value.type & Res_value.DATA_TYPE_ID_MASK) >>> Res_value.DATA_TYPE_ID_SHIFT);
                        if (styleId != null) {
                            defStyleBag = mResourcesImpl.mAssetManager.getBag(styleId);
                        }
                    }
                }
            }
            if (defStyleBag == null && defStyleRes != null) {
                defStyleBag = mResourcesImpl.mAssetManager.getBag(defStyleRes);
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
            return mResources;
        }

        public void setTo(@NonNull Theme other) {
            if (this == other) {
                return;
            }
            synchronized (mLock) {
                synchronized (other.mLock) {
                    //TODO we also need to lock underlying Resources & AssetManager
                    // and retrieve attribute values if they are different.
                    // atm we just do deep copy.
                    if (other.mEntries == null) {
                        mEntries = null;
                    } else {
                        if (mEntries == null) {
                            mEntries = new Object2ObjectOpenHashMap<>();
                        }
                        for (var oit = other.mEntries.object2ObjectEntrySet().fastIterator(); oit.hasNext(); ) {
                            var oe = oit.next();
                            Object2ObjectOpenHashMap<String, Entry> inner =
                                    new Object2ObjectOpenHashMap<>(oe.getValue().size());
                            for (var iit = oe.getValue().object2ObjectEntrySet().fastIterator(); iit.hasNext(); ) {
                                var ie = iit.next();
                                inner.put(ie.getKey(), ie.getValue().clone());
                            }
                            mEntries.put(oe.getKey(), inner);
                        }
                    }

                    mKey.setTo(other.getKey());
                    mKeyCopy = mKey.clone();
                }
            }
        }

        public void clear() {
            synchronized (mLock) {
                mEntries = null;
                mKey.clear();
                mKeyCopy = mKey.clone();
            }
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

        public void clear() {
            mResId = null;
            mForce = null;
            mHashCode = 0;
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
