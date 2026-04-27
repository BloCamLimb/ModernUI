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
import icyllis.modernui.graphics.drawable.ColorDrawable;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.DisplayMetrics;
import icyllis.modernui.util.Log;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.CheckReturnValue;

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
        }
    }

    DisplayMetrics getDisplayMetrics() {
        return mMetrics;
    }

    Configuration getConfiguration() {
        return mConfiguration;
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
            Log.LOGGER.error(MARKER, "Resource is not a color: {}", value);
            return null;
        }
        //TODO load plain XML

        String name = id != null ? id.toString() : "(missing name)";
        Log.LOGGER.error(MARKER, "Can't find ColorStateList from drawable {}", name);
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
                    }
                }
                Log.LOGGER.error(MARKER, "Resource is not a drawable: {}", value);
                return null;
            }

            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return new ColorDrawable(value.data);
            }

            //TODO load plain XML
            Log.LOGGER.error(MARKER, "Resource is not a drawable: {}", value);
            return null;
        } catch (Exception e) {
            Log.LOGGER.error(MARKER, "Drawable cannot load: {}", id != null ? id.toString() : value, e);
            return null;
        }
    }

    @Nullable
    Asset loadRawResource(@NonNull TypedValue value, @Nullable ResourceId id) {
        var path = value.getString();
        if (path == null) {
            String msg = id != null
                    ? "Resource ID " + id + " is not raw resource"
                    : "Resource value " + value + " is not a string or cannot be resolved";
            Log.LOGGER.error(MARKER, msg);
            return null;
        }
        Asset asset = mAssetManager.getNonAsset(path.toString(), value.cookie);
        if (asset == null) {
            String msg = "File " + path;
            if (id != null) {
                msg = msg + " from resource ID " + id;
            }
            Log.LOGGER.error(MARKER, "{} cannot be found", msg);
            return null;
        }
        return asset;
    }
}
