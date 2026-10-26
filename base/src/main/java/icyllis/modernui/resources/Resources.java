/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.resources;

import icyllis.arc3d.fastutil.objects.Object2ObjectOpenHashMap;
import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.GuardedBy;
import icyllis.modernui.annotation.Immutable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.annotation.ThreadSafe;
import icyllis.modernui.graphics.BitmapFactory;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.drawable.ColorDrawable;
import icyllis.modernui.graphics.drawable.ColorStateListDrawable;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.ImageDrawable;
import icyllis.modernui.system.LocaleList;
import icyllis.modernui.util.AttributeSet;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.DisplayMetrics;
import icyllis.modernui.util.Log;
import icyllis.modernui.util.Pools;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.WeakReference;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

@ThreadSafe
public class Resources {

    public static final Marker MARKER = MarkerFactory.getMarker("Resources");



    private final DisplayMetrics mMetrics = new DisplayMetrics();

    private final AssetManager mAssetManager;

    private final DrawableCache mDrawableCache = new DrawableCache();

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



    @GuardedBy("itself")
    private final Object2ObjectOpenHashMap<ThemeKey, WeakReference<Theme>> mThemes = new Object2ObjectOpenHashMap<>();





    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public Resources(@NonNull AssetManager assetManager, @NonNull DisplayMetrics metrics) {
        mAssetManager = assetManager;
        mMetrics.setTo(metrics);
    }

    /**
     * @hide
     * @hidden
     */
    // should be called only from UI thread
    @ApiStatus.Internal
    public void updateDisplaySize(int widthPixels, int heightPixels) {
        mMetrics.widthPixels = widthPixels;
        mMetrics.heightPixels = heightPixels;
    }

    /**
     * Generate a Theme object for this set of Resources.
     * <p>
     * Since Theme is immutable, calling this method multiple times with equal
     * ThemeKey will result in the same Theme object.
     * <p>
     * This method can be called from any thread.
     *
     * @param key the key used to create the theme
     * @return the Theme for the given key
     */
    @NonNull
    public final Theme newTheme(@NonNull ThemeKey key) {
        synchronized (mThemes) {
            WeakReference<Theme> existingRef = mThemes.get(key);
            if (existingRef != null) {
                Theme existing = existingRef.get();
                if (existing != null) {
                    return existing;
                }
            }
            var theme = new Theme(this, key);
            // AOSP's nextPowerOf2() is wrongly implemented, just don't use that and always
            // clean up the whole list
            mThemes.values().removeIf(r -> r.refersTo(null));
            mThemes.put(key, new WeakReference<>(theme));
            return theme;
        }
    }

    /**
     * Returns the display metrics that can affect resources returned by this object.
     * <p>
     * For a given {@link Resources} instance, this method always returns the same
     * {@link DisplayMetrics} object whose fields are immutable except for
     * {@link DisplayMetrics#widthPixels} and {@link DisplayMetrics#heightPixels}.
     * The returned object must be treated as read-only.
     */
    @NonNull
    @UnmodifiableView
    public final DisplayMetrics getDisplayMetrics() {
        return mMetrics;
    }

    /**
     * Return the locale list that can affect resources returned by this object.
     * <p>
     * The underlying implementation will use {@link com.ibm.icu.util.LocaleMatcher} to
     * find the best resource entry for a given resource id, if the resource is changing
     * with Locale.
     * <p>
     * For a given {@link Resources} instance, this method always returns the same
     * {@link LocaleList} object
     */
    @NonNull
    public final LocaleList getLocales() {
        return mAssetManager.getLocales();
    }

    /**
     * Returns the underlying AssetManager of this Resources object.
     * <p>
     * Each Resources instance has a deterministic AssetManager, but different
     * Resources instances can return the same AssetManager.
     *
     * @hidden
     */
    @ApiStatus.Internal
    @NonNull
    public final AssetManager getAssetManager() {
        return mAssetManager;
    }

    @CheckReturnValue
    @ApiStatus.Experimental
    public boolean getValue(@NonNull ResourceId id, @NonNull TypedValue outValue,
                            boolean resolveRefs) {
        boolean found = mAssetManager.getResource(id, mMetrics.densityDpi, outValue);
        if (resolveRefs) {
            //TODO
        }
        if (found) {
            found = mAssetManager.postProcess(outValue);
        }
        return found;
    }

    @Nullable
    public ColorStateList getColorStateList(@NonNull ResourceId id,
                                            @Nullable Theme theme) {
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

    @Nullable
    public ColorStateList getColorStateList(@NonNull ResourceId id,
                                            @NonNull TypedValue value,
                                            @Nullable Theme theme) {
        if (getValue(id, value, true)) {
            return loadColorStateList(value, id, theme);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public ColorStateList loadColorStateList(@NonNull TypedValue value,
                                             @Nullable ResourceId id,
                                             @Nullable Theme theme) {
        // Handle inline color definitions.
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return ColorStateList.valueOf(value.data);
        }

        if (value.type == TypedValue.TYPE_FACTORY) {
            Object object = value.object;
            if (object != null) {
                object = ((BiFunction<Resources, Theme, ?>) object).apply(this, theme);
                if (object instanceof ColorStateList) {
                    return (ColorStateList) object;
                }
            }
        }
        //TODO load plain XML

        Log.LOGGER.error(MARKER, "{} from resource ID {} is not a color",
                value, id, new UnsupportedOperationException());
        return null;
    }

    @Nullable
    public Drawable getDrawable(@NonNull ResourceId id,
                                @Nullable Theme theme) {
        TypedValue tmp = (TypedValue) TMP_VALUE.getAndSetAcquire(this, null);
        try {
            TypedValue val = tmp != null ? tmp : new TypedValue();
            return getDrawable(id, val, theme);
        } finally {
            if (tmp != null) {
                TMP_VALUE.setRelease(this, tmp);
            }
        }
    }

    @Nullable
    public Drawable getDrawable(@NonNull ResourceId id,
                                @NonNull TypedValue value,
                                @Nullable Theme theme) {
        if (getValue(id, value, true)) {
            return loadDrawable(value, id, theme);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public Drawable loadDrawable(@NonNull TypedValue value,
                                 @Nullable ResourceId id,
                                 @Nullable Theme theme) {
        try {
            if (value.type == TypedValue.TYPE_FACTORY) {
                Object object = value.object;
                if (object != null) {
                    object = ((BiFunction<Resources, Resources.Theme, ?>) object).apply(this, theme);
                    if (object instanceof Drawable) {
                        return (Drawable) object;
                    }
                    if (object instanceof ColorStateList) {
                        return new ColorStateListDrawable((ColorStateList) object);
                    }
                }
            }

            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return new ColorDrawable(value.data);
            }

            if (value.type == TypedValue.TYPE_STRING) {
                long key = (((long) value.cookie) << 32) | value.data;
                long cacheGeneration = mDrawableCache.getGeneration();

                Drawable drawable = mDrawableCache.getInstance(key, this, theme);
                if (drawable != null) {
                    return drawable;
                }

                drawable = loadDrawableForCookie(this, value, id);
                if (drawable == null) {
                    return null;
                }

                Drawable.ConstantState cs = drawable.getConstantState();
                if (cs != null) {
                    mDrawableCache.put(key, theme, cs, cacheGeneration,
                            false);
                }

                return drawable;
            }


            Log.LOGGER.error(MARKER, "{} from resource ID {} is not a drawable",
                    value, id, new UnsupportedOperationException());
            return null;
        } catch (Exception e) {
            Log.LOGGER.error(MARKER, "Failed to load drawable. {} from resource ID {}",
                    value, id, e);
            return null;
        }
    }

    @Nullable
    public Image getImage(@NonNull ResourceId id,
                          boolean needNewInstance) {
        TypedValue tmp = (TypedValue) TMP_VALUE.getAndSetAcquire(this, null);
        try {
            TypedValue val = tmp != null ? tmp : new TypedValue();
            return getImage(id, val, needNewInstance ? Image::clone : UnaryOperator.identity());
        } finally {
            if (tmp != null) {
                TMP_VALUE.setRelease(this, tmp);
            }
        }
    }

    @Nullable
    public Image getImage(@NonNull ResourceId id,
                          @NonNull TypedValue value,
                          @NonNull UnaryOperator<Image> deriveOp) {
        if (getValue(id, value, true)) {
            return loadImage(value, id, deriveOp);
        }
        return null;
    }

    @Nullable
    public Image loadImage(@NonNull TypedValue value,
                           @Nullable ResourceId id,
                           @NonNull UnaryOperator<Image> deriveOp) {
        AssetPack pack = mAssetManager.getPack(value.cookie);
        if (pack == null) {
            return null;
        }
        int key = value.data;

        Image image = pack.getImageCache().getInstance(key, deriveOp);
        if (image != null) {
            return image;
        }

        image = loadImageForCookie(value, id);
        if (image == null) {
            return null;
        }

        return pack.getImageCache().putAndGet(key, image,
                deriveOp);
    }

    @Nullable
    private Image loadImageForCookie(@NonNull TypedValue value, @Nullable ResourceId id) {
        Asset asset = loadRawResource(value, id);
        if (asset == null) {
            return null;
        }
        try (var bitmap = asset.isCompressed()
                ? BitmapFactory.decodeStream(asset.openStream())
                : BitmapFactory.decodeChannel(asset.openChannel())) {
            var newImage = Image.createTextureFromBitmap(bitmap);
            if (newImage == null) {
                //TODO create CPU image once supported by Arc3D, so this never fail
                Log.LOGGER.error(MARKER, "Failed to create GPU image, resource ID {}, value {}",
                        id, value);
            }
            return newImage;
        } catch (Exception e) {
            Log.LOGGER.error(MARKER, "Failed to decode image. File {} at cookie {} from resource ID {}",
                    value.getString(), value.cookie, id, e);
            return null;
        }
    }

    @Nullable
    public Asset getRawResource(@NonNull ResourceId id) {
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

    @Nullable
    public Asset getRawResource(@NonNull ResourceId id, @NonNull TypedValue value) {
        if (getValue(id, value, true)) {
            return loadRawResource(value, id);
        }
        return null;
    }

    @Nullable
    public Asset loadRawResource(@NonNull TypedValue value, @Nullable ResourceId id) {
        var string = value.getString();
        if (string == null) {
            Log.LOGGER.error(MARKER, "{} from resource ID {} is not a path",
                    value, id, new UnsupportedOperationException());
            return null;
        }
        var path = string.toString();
        Asset asset = mAssetManager.getNonAsset(path, value.cookie);
        if (asset == null) {
            Log.LOGGER.error(MARKER, "File {} at cookie {} from resource ID {} cannot be found",
                    path, value.cookie, id, new NoSuchFileException(path));
            return null;
        }
        return asset;
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
        return mAssetManager.getAsset(path);
    }

    @Nullable
    private Drawable loadDrawableForCookie(@NonNull Resources wrapper,
                                           @NonNull TypedValue value,
                                           @Nullable ResourceId id) {
        var string = value.getString();
        if (string == null) {
            Log.LOGGER.error(MARKER, "{} from resource ID {} is not a path",
                    value, id, new UnsupportedOperationException());
            return null;
        }
        var path = string.toString();

        if (path.endsWith(".xml")) {
            //TODO load plain XML
            return null;
        }

        Image image = loadImage(value, id, UnaryOperator.identity());
        if (image == null) {
            return null;
        }

        return new ImageDrawable(wrapper, image);
    }







    @Immutable
    public static final class Theme {
        //TODO move this class to upper level in 4.0

        private final Resources mResources;
        private final AssetManager mAssetManager;

        private final ThemeKey mKey;

        // attribute name -> entry
        // we use j.u.HashMap instead of open addressing for best lookup performance,
        // since lookup miss rate is very high
        private final HashMap<String, Entry> mEntries;

        static final class Entry {
            final int type;
            final int data;
            final int cookie;
            final int typeSpecFlags;

            Entry(@NonNull ResolvedBag bag, int index) {
                int offset = index * ResolvedBag.VALUE_COLUMNS;
                type = bag.values[offset + ResolvedBag.COLUMN_TYPE];
                data = bag.values[offset + ResolvedBag.COLUMN_DATA];
                cookie = bag.values[offset + ResolvedBag.COLUMN_COOKIE];
                typeSpecFlags = bag.typeSpecFlags;
            }
        }

        static boolean isUndefined(int type, int data) {
            return type == TypedValue.TYPE_NULL && data == TypedValue.DATA_NULL_UNDEFINED;
        }

        private Theme(@NonNull Resources resources, @NonNull ThemeKey key) {
            mResources = resources;
            mAssetManager = resources.getAssetManager();

            HashMap<String, Entry> entries = new HashMap<>();
            int count = key.count();
            for (int i = 0; i < count; i++) {
                ResourceId resId = key.resId(i);
                ResolvedBag bag = mAssetManager.getBag(resId);
                if (bag == null) {
                    continue;
                }
                boolean force = key.isForce(i);

                int entryCount = bag.getEntryCount();
                for (int j = 0; j < entryCount; j++) {
                    boolean isUndefined = isUndefined(bag.type(j), bag.data(j));
                    if (!force && isUndefined) {
                        continue;
                    }
                    String attr = bag.keys[j];
                    if (isUndefined) {
                        entries.remove(attr);
                    } else {
                        var entry = new Entry(bag, j);
                        if (force) {
                            entries.put(attr, entry);
                        } else {
                            entries.putIfAbsent(attr, entry);
                        }
                    }
                }
            }

            mEntries = entries;
            mKey = key;
        }

        /**
         * Return a TypedArray holding the values defined by
         * <var>Theme</var> which are listed in <var>attrs</var>.
         *
         * <p>Be sure to call {@link TypedArray#recycle() TypedArray.recycle()} when you are done
         * with the array.
         *
         * @param attrs The names of desired attributes to be retrieved.
         * @return Returns a TypedArray holding an array of the attribute values.
         * Be sure to call {@link TypedArray#recycle() TypedArray.recycle()}
         * when done with it.
         * @see #obtainStyledAttributes(ResourceId, AttributeSlots)
         * @see #obtainStyledAttributes(AttributeSet, String, ResourceId, AttributeSlots)
         */
        @NonNull
        public TypedArray obtainStyledAttributes(String @NonNull[] attrs) {
            //TODO no need run into generic path
            return obtainStyledAttributes(null, (String) null, null, attrs);
        }

        @Deprecated(forRemoval = true)
        @NonNull
        public TypedArray obtainStyledAttributes(@Nullable @StyleRes ResourceId resId,
                                                 String @NonNull[] attrs) {
            return obtainStyledAttributes(null, (String) null, resId,
                    new AttributeSlots(null, attrs));
        }

        /**
         * Return a TypedArray holding the values defined by the <var>resId</var>
         * resource which are listed in <var>slots</var>.
         *
         * <p>Be sure to call {@link TypedArray#recycle() TypedArray.recycle()} when you are done
         * with the array.
         *
         * @param resId The desired style resource.
         * @param slots The names of desired attributes to be retrieved.  It
         *              should be pre-constructed and reused.
         * @return Returns a TypedArray holding an array of the attribute values.
         * Be sure to call {@link TypedArray#recycle() TypedArray.recycle()}
         * when done with it.
         * @see #obtainStyledAttributes(ResourceId, AttributeSlots)
         * @see #obtainStyledAttributes(AttributeSet, String, ResourceId, AttributeSlots)
         */
        @NonNull
        public TypedArray obtainStyledAttributes(@Nullable @StyleRes ResourceId resId,
                @NonNull AttributeSlots slots) {
            return obtainStyledAttributes(null, (String) null, resId, slots);
        }

        /**
         * @deprecated namespace and type are not used in attribute name anymore, this will be removed in 4.0
         */
        @Deprecated(forRemoval = true)
        @NonNull
        public TypedArray obtainStyledAttributes(@Nullable AttributeSet set,
                                                 @Nullable @AttrRes ResourceId defStyleAttr,
                                                 @Nullable @StyleRes ResourceId defStyleRes,
                String @NonNull[] attrs) {
            assert defStyleAttr == null || defStyleAttr.type().equals("attr");
            return obtainStyledAttributes(set,
                    defStyleAttr != null ? defStyleAttr.entry() : null, defStyleRes,
                    attrs);
        }

        @Deprecated(forRemoval = true)
        @NonNull
        public TypedArray obtainStyledAttributes(@Nullable AttributeSet set,
                                                 @Nullable String defStyleAttr,
                                                 @Nullable @StyleRes ResourceId defStyleRes,
                                                 String @NonNull[] attrs) {
            return obtainStyledAttributes(set, defStyleAttr, defStyleRes,
                    new AttributeSlots(null, attrs));
        }

        /**
         * Return a TypedArray holding the attribute values in
         * <var>set</var>
         * that are listed in <var>slots</var>.  In addition, if the given
         * AttributeSet specifies a style class (through the "style" attribute),
         * that style will be applied on top of the base attributes it defines.
         *
         * <p>Be sure to call {@link TypedArray#recycle() TypedArray.recycle()} when you are done
         * with the array.
         *
         * <p>When determining the final value of a particular attribute, there
         * are three inputs that come into play:</p>
         *
         * <ol>
         *     <li> Any attribute values in the given AttributeSet.
         *     <li> The style resource specified in the AttributeSet (named
         *     "style"), or the default style specified by <var>defStyleAttr</var> and
         *     <var>defStyleRes</var>.
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
         * @param slots        The names of desired attributes to be retrieved.  It
         *                     should be pre-constructed and reused.
         * @return Returns a TypedArray holding an array of the attribute values.
         * Be sure to call {@link TypedArray#recycle() TypedArray.recycle()}
         * when done with it.
         * @see #obtainStyledAttributes(String[])
         * @see #obtainStyledAttributes(ResourceId, String[])
         */
        @NonNull
        public TypedArray obtainStyledAttributes(@Nullable AttributeSet set,
                                                 @Nullable String defStyleAttr,
                                                 @Nullable @StyleRes ResourceId defStyleRes,
                                                 @NonNull AttributeSlots slots) {
            final TypedArray array = TypedArray.obtain(getResources(),
                    slots.size());

            resolveAttributes(set, defStyleAttr, defStyleRes,
                    slots, array.mData, array.mIndices,
                    array.mValue);
            array.mTheme = this;
            return array;
        }

        /**
         * @deprecated namespace and type are not used in attribute name anymore, this will be removed in 4.0
         */
        @Deprecated
        public boolean resolveAttribute(@NonNull @AttrRes ResourceId resId,
                                        @NonNull TypedValue outValue, boolean resolveRefs) {
            assert resId.type().equals("attr");
            return resolveAttribute(resId.entry(), outValue, resolveRefs);
        }

        /**
         * @deprecated namespace is not used in attribute name anymore, this will be removed in 4.0
         */
        @Deprecated
        public boolean resolveAttribute(@NonNull String namespace, @NonNull String name,
                                        @NonNull TypedValue outValue, boolean resolveRefs) {
            var ignored = namespace;
            return resolveAttribute(name, outValue, resolveRefs);
        }

        /**
         * Retrieve the value of an attribute in the Theme.  The contents of
         * <var>outValue</var> are ultimately filled in by
         * {@link Resources#getValue}.
         *
         * @param name        The name of the desired theme attribute.
         * @param outValue    Filled in with the ultimate resource value supplied
         *                    by the attribute.
         * @param resolveRefs If true, resource references will be walked; if
         *                    false, <var>outValue</var> may be a
         *                    TYPE_REFERENCE.  In either case, it will never
         *                    be a TYPE_ATTRIBUTE.
         * @return boolean Returns true if the attribute was found and
         * <var>outValue</var> is valid, else false.
         */
        public boolean resolveAttribute(@NonNull String name,
                                        @NonNull TypedValue outValue, boolean resolveRefs) {
            if (!getAttribute(name, outValue)) {
                return false;
            }

            int cookie = outValue.cookie;
            assert cookie >= 0;

            if (resolveRefs) {
                //TODO
            }

            return mAssetManager.postProcess(outValue);
        }

        private boolean getAttribute(
                             @NonNull String name,
                             @NonNull TypedValue outValue) {
            int typeSpecFlags = 0;
            for (int __ = 0; __ < AssetManager.kMaxIterations; __++) {
                Entry e;
                if ((e = mEntries.get(name)) == null) {
                    return false;
                }
                if (isUndefined(e.type, e.data)) {
                    return false;
                }
                typeSpecFlags |= e.typeSpecFlags;
                //TODO for recursive attributes, resolve them in advance in constructor
                if (e.type == TypedValue.TYPE_ATTRIBUTE) {
                    ResourceMap resources = mAssetManager.getResources(e.cookie);
                    if (resources != null) {
                        name = resources.getKeyStringPool()
                                .getStringAt(e.data);
                        if (name != null) {
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
            if (value.type != TypedValue.TYPE_ATTRIBUTE) {
                //TODO currently we have only style resources, where styles do not need resolve
                return true;
            }

            ResourceMap resources = mAssetManager.getResources(value.cookie);
            if (resources == null) {
                return false;
            }
            String attr = resources.getKeyStringPool()
                    .getStringAt(value.data);
            if (attr == null) {
                return false;
            }

            int flags = value.flags;
            if (!getAttribute(attr, value)) {
                return false;
            }

            value.flags |= flags;
            return true;
        }

        private void resolveAttributes(
                @Nullable AttributeSet set,
                @Nullable String defStyleAttr,
                @Nullable @StyleRes ResourceId defStyleRes,
                @NonNull AttributeSlots slots,
                int @NonNull [] outValues, int @NonNull [] outIndices,
                @NonNull TypedValue value) {

            ResolvedBag styleBag = null;
            if (defStyleAttr != null &&
                    resolveAttribute(defStyleAttr, value, true) &&
                    value.type == TypedValue.TYPE_REFERENCE) {
                var styleId = value.getReference();
                if (styleId != null) {
                    styleBag = mAssetManager.getBag(styleId);
                }
            }
            if (styleBag == null && defStyleRes != null) {
                styleBag = mAssetManager.getBag(defStyleRes);
            }

            if (styleBag != null) {
                String[] haystack = styleBag.keys;
                for (int i = 0; i < haystack.length; i++) {
                    String curAttr = haystack[i];
                    int ii = slots.indexOf(curAttr);
                    if (ii < 0) {
                        continue;
                    }
                    value.setTo(styleBag, i);

                    if (value.type == TypedValue.TYPE_NULL) {
                        continue;
                    }

                    resolveAttributeReference(value);
                    int valuesIdx = ii * TypedArray.STYLE_NUM_ENTRIES;
                    outValues[valuesIdx + TypedArray.STYLE_TYPE] = value.type;
                    outValues[valuesIdx + TypedArray.STYLE_DATA] = value.data;
                    outValues[valuesIdx + TypedArray.STYLE_COOKIE] = value.cookie;
                    outValues[valuesIdx + TypedArray.STYLE_FLAGS] = value.flags;
                }
            }

            int indicesIdx = 0;
            int valuesIdx = 0;
            String[] keys = slots.getKeys();
            for (int ii = 0; ii < keys.length; ii++) {

                if (outValues[valuesIdx + TypedArray.STYLE_TYPE] == TypedValue.TYPE_NULL &&
                        outValues[valuesIdx + TypedArray.STYLE_DATA] != TypedValue.DATA_NULL_EMPTY) {
                    if (getAttribute(keys[ii], value)) {
                        outValues[valuesIdx + TypedArray.STYLE_TYPE] = value.type;
                        outValues[valuesIdx + TypedArray.STYLE_DATA] = value.data;
                        outValues[valuesIdx + TypedArray.STYLE_COOKIE] = value.cookie;
                        outValues[valuesIdx + TypedArray.STYLE_FLAGS] = value.flags;
                    }
                }

                if (outValues[valuesIdx + TypedArray.STYLE_TYPE] != TypedValue.TYPE_NULL ||
                        outValues[valuesIdx + TypedArray.STYLE_DATA] == TypedValue.DATA_NULL_EMPTY) {
                    outIndices[++indicesIdx] = ii;
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

        public ThemeKey getKey() {
            return mKey;
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
                    "resources=" + getResources() +
                    ", key=" + getKey() +
                    '}';
        }
    }
}
