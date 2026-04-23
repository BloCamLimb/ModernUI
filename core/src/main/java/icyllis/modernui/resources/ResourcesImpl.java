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
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiFunction;

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

    @Nullable
    CharSequence getPooledStringForCookie(int cookie, int id) {
        LoadedResources loadedResources = mAssetManager.getLoadedResources(cookie);
        if (loadedResources == null) {
            return null;
        }
        return loadedResources.getGlobalStringPool().getSequenceAt(id);
    }

    @Nullable
    ResourceId getReferenceIdForCookie(int cookie, int typeId, int data) {
        assert typeId > 0;
        LoadedResources loadedResources = mAssetManager.getLoadedResources(cookie);
        if (loadedResources == null) {
            return null;
        }
        return loadedResources.lookupResourceId(null, data, typeId);
    }

    @Nullable
    ResourceId getAttributeIdForCookie(int cookie, int data) {
        LoadedResources loadedResources = mAssetManager.getLoadedResources(cookie);
        if (loadedResources == null) {
            return null;
        }
        String namespace = loadedResources.lookupPackageName(data >>> ResourceTypes.Res_value.PACKAGE_ID_SHIFT);
        String attribute =
                loadedResources.getKeyStringPool().getStringAt(data & ResourceTypes.Res_value.KEY_INDEX_MASK);
        if (namespace != null && attribute != null) {
            return ResourceId.attr(namespace, attribute);
        }
        return null;
    }

    // do post-processing before exposing it to public API users,
    // freeze the 'object' field
    boolean postProcess(@NonNull TypedValue outValue) {
        switch (outValue.type & ResourceTypes.Res_value.DATA_TYPE_MASK) {
            case TypedValue.TYPE_STRING -> {
                if ((outValue.object = getPooledStringForCookie(outValue.cookie, outValue.data)) == null) {
                    return false;
                }
            }
            case TypedValue.TYPE_REFERENCE -> {
                int typeId =
                        (outValue.type & ResourceTypes.Res_value.DATA_TYPE_ID_MASK) >>> ResourceTypes.Res_value.DATA_TYPE_ID_SHIFT;
                if (typeId != 0) {
                    // erase higher 8 bits
                    outValue.type = TypedValue.TYPE_REFERENCE;
                    if ((outValue.object = getReferenceIdForCookie(outValue.cookie, typeId, outValue.data)) == null) {
                        return false;
                    }
                } else {
                    outValue.object = null;
                }
            }
            case TypedValue.TYPE_ATTRIBUTE -> {
                if ((outValue.object = getAttributeIdForCookie(outValue.cookie, outValue.data)) == null) {
                    return false;
                }
            }
            default -> outValue.object = null;
        }
        return true;
    }

    public void getValue(@NonNull ResourceId id, @NonNull TypedValue outValue,
                         boolean resolveRefs) throws Resources.NotFoundException {
        boolean found = mAssetManager.getResource(id, outValue);
        if (found) {
            found = postProcess(outValue);
        }
        if (!found) {
            throw new Resources.NotFoundException("Resource " + id);
        }
    }

    @SuppressWarnings("unchecked")
    @NonNull
    ColorStateList loadColorStateList(@NonNull Resources wrapper,
                                      @NonNull TypedValue value,
                                      @Nullable ResourceId id,
                                      @Nullable Resources.Theme theme)
            throws Resources.NotFoundException {
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
            throw new Resources.NotFoundException("Resource is not a color: " + value);
        }

        String name = id != null ? id.toString() : "(missing name)";
        throw new Resources.NotFoundException(
                "Can't find ColorStateList from drawable " + name);
    }

    @SuppressWarnings("unchecked")
    @NonNull
    Drawable loadDrawable(@NonNull Resources wrapper,
                          @NonNull TypedValue value,
                          @Nullable ResourceId id,
                          @Nullable Resources.Theme theme)
            throws Resources.NotFoundException {
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
                throw new Resources.NotFoundException("Resource is not a drawable: " + value);
            }

            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return new ColorDrawable(value.data);
            }

            //TODO load plain XML
            throw new Resources.NotFoundException("Resource is not a drawable: " + value);
        } catch (Exception e) {
            String name = id != null ? id.toString() : "(missing name)";

            // The target drawable might fail to load for any number of
            // reasons, but we always want to include the resource name.
            // Since the client already expects this method to throw a
            // NotFoundException, just throw one of those.
            final Resources.NotFoundException nfe = new Resources.NotFoundException("Drawable " + name, e);
            nfe.setStackTrace(new StackTraceElement[0]);
            throw nfe;
        }
    }

    @NonNull
    Asset loadRawResource(@NonNull TypedValue value, @Nullable ResourceId id)
            throws Resources.NotFoundException {
        var path = value.getString();
        if (path == null) {
            String msg = id != null
                    ? "Resource ID " + id + " is not raw resource"
                    : "Resource value " + value + " is not a string or cannot be resolved";
            throw new Resources.NotFoundException(msg);
        }
        Asset asset = mAssetManager.getNonAsset(path.toString(), value.cookie);
        if (asset == null) {
            String msg = "File " + path;
            if (id != null) {
                msg = msg + " from resource ID " + id;
            }
            throw new Resources.NotFoundException(msg);
        }
        return asset;
    }
}
