/*
 * Modern UI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.BitmapFactory;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.drawable.ColorDrawable;
import icyllis.modernui.graphics.drawable.ColorStateListDrawable;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.ImageDrawable;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.DisplayMetrics;
import icyllis.modernui.util.Log;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.CheckReturnValue;

import java.nio.file.NoSuchFileException;
import java.util.function.BiFunction;

import static icyllis.modernui.resources.Resources.MARKER;

/**
 * The implementation of Resource access. This class contains the AssetManager and all caches
 * associated with it.
 * <p>
 * {@link Resources} is just a thing wrapper around this class. When a configuration change
 * occurs, clients can retain the same {@link Resources} reference because the underlying
 * {@link ResourcesImpl} object will be updated or re-redirected.
 * <p>
 * A single {@link Resources} instance can reference different {@link ResourcesImpl} objects
 * over its lifetime, while multiple {@link Resources} instances can share the same
 * {@link ResourcesImpl} object. A {@link ResourcesImpl} maintains a 1-to-1 mapping with a unique
 * {@link AssetManager}. Both {@link AssetManager} and {@link ResourcesImpl} caches can be invalidated
 * due to {@link Configuration} changes. The {@link PackAssets} list within an {@link AssetManager}
 * is immutable once the instance is created.
 * <p>
 * Upon a {@link Configuration} change, the system may switch the {@link ResourcesImpl} reference
 * within a {@link Resources} instance, rather than just updating existing {@link ResourcesImpl}
 * objects. This allows the old {@link ResourcesImpl} to remain referenced by other {@link Resources}
 * instances or to be retained in the cache pool. Each {@link Resources} handle acts as a proxy
 * that can be dynamically redirected to a different backing {@link ResourcesImpl} to match its
 * specific environment.
 *
 * @hide
 * @hidden
 */
@ApiStatus.Internal
public final class ResourcesImpl {

    private final Object mAccessLock = new Object();

    private final DisplayMetrics mMetrics = new DisplayMetrics();

    private final Configuration mConfiguration = new Configuration();

    final AssetManager mAssetManager;

    // cache of GPU textures or CPU bitmaps
    private final ImageCache mImageCache = new ImageCache();

    private final DrawableCache mDrawableCache = new DrawableCache();

    public ResourcesImpl(@NonNull AssetManager assetManager, @Nullable DisplayMetrics metrics,
                         @Nullable Configuration configuration) {
        mAssetManager = assetManager;
        mMetrics.setToDefaults();
        mConfiguration.setToDefaults();
        updateConfiguration(configuration, metrics);
    }

    public void updateConfiguration(Configuration configuration, DisplayMetrics metrics) {
        synchronized (mAccessLock) {
            if (metrics != null) {
                mMetrics.setTo(metrics);
            }

            mImageCache.onConfigurationChange(0);
            mDrawableCache.onConfigurationChange(0);
        }
    }

    DisplayMetrics getDisplayMetrics() {
        return mMetrics;
    }

    Configuration getConfiguration() {
        return mConfiguration;
    }

    public void clearAllCaches() {
        synchronized (mAccessLock) {
            mImageCache.clear();
            mDrawableCache.clear();
        }
    }

    @CheckReturnValue
    public boolean getValue(@NonNull ResourceId id, @NonNull TypedValue outValue,
                            boolean resolveRefs) {
        boolean found = mAssetManager.getResource(id, outValue);
        if (resolveRefs) {
            //TODO
        }
        if (found) {
            found = mAssetManager.postProcess(outValue);
        }
        return found;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    ColorStateList loadColorStateList(@NonNull Resources wrapper,
                                      @NonNull TypedValue value,
                                      @Nullable ResourceId id,
                                      @Nullable Resources.Theme theme) {
        // Handle inline color definitions.
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return ColorStateList.valueOf(value.data);
        }

        if (value.type == TypedValue.TYPE_FACTORY) {
            LoadedResources loadedResources = mAssetManager.getLoadedResources(value.cookie);
            if (loadedResources != null) {
                Object object = loadedResources.lookupFactory(value.data);
                if (object != null) {
                    object = ((BiFunction<Resources, Resources.Theme, ?>) object).apply(wrapper, theme);
                    if (object instanceof ColorStateList) {
                        return (ColorStateList) object;
                    }
                }
            }
        }
        //TODO load plain XML

        Log.LOGGER.error(MARKER, "{} from resource ID {} is not a color",
                value, id, new UnsupportedOperationException());
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    Drawable loadDrawable(@NonNull Resources wrapper,
                          @NonNull TypedValue value,
                          @Nullable ResourceId id,
                          @Nullable Resources.Theme theme) {
        try {
            if (value.type == TypedValue.TYPE_FACTORY) {
                LoadedResources loadedResources = mAssetManager.getLoadedResources(value.cookie);
                if (loadedResources != null) {
                    Object object = loadedResources.lookupFactory(value.data);
                    if (object != null) {
                        object = ((BiFunction<Resources, Resources.Theme, ?>) object).apply(wrapper, theme);
                        if (object instanceof Drawable) {
                            return (Drawable) object;
                        }
                        if (object instanceof ColorStateList) {
                            return new ColorStateListDrawable((ColorStateList) object);
                        }
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

                Drawable drawable = mDrawableCache.getInstance(key, wrapper, theme);
                if (drawable != null) {
                    return drawable;
                }

                drawable = loadDrawableForCookie(wrapper, value, id);
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

        Image image = loadImage(value, id, false);
        if (image == null) {
            return null;
        }

        return new ImageDrawable(wrapper, image);
    }

    @Nullable
    Image loadImage(@NonNull TypedValue value, @Nullable ResourceId id,
                    boolean needNewInstance) {
        long key = (((long) value.cookie) << 32) | value.data;
        long cacheGeneration = mImageCache.getGeneration();

        Image image = mImageCache.getInstance(key, needNewInstance);
        if (image != null) {
            return image;
        }

        image = loadImageForCookie(value, id);
        if (image == null) {
            return null;
        }

        return mImageCache.putAndGet(key, image,
                cacheGeneration, needNewInstance);
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
    Asset loadRawResource(@NonNull TypedValue value, @Nullable ResourceId id) {
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
}
