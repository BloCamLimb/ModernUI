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
import icyllis.modernui.util.ArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.concurrent.GuardedBy;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ResourcesLoader {

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private PackAssets[] mPackAssets;

    @GuardedBy("mLock")
    private ResourcesProvider[] mPreviousProviders;

    @GuardedBy("mLock")
    private ResourcesProvider[] mProviders;

    @GuardedBy("mLock")
    private final ArrayMap<WeakReference<Object>, UpdateCallbacks> mChangeCallbacks = new ArrayMap<>();

    private final Consumer<UpdateCallbacks> mInvokeCallbacks = c -> c.onLoaderUpdated(this);

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public interface UpdateCallbacks {

        /**
         * Invoked when a {@link ResourcesLoader} has a {@link ResourcesProvider} added, removed,
         * or reordered.
         *
         * @param loader the loader that was updated
         */
        void onLoaderUpdated(@NonNull ResourcesLoader loader);
    }

    public ResourcesLoader() {
    }

    /**
     * Retrieves the list of providers loaded into this instance. Providers are listed in increasing
     * precedence order. A provider will override the values of providers listed before itself.
     */
    @SuppressWarnings("Java9CollectionFactory")
    @Unmodifiable
    @NonNull
    public List<ResourcesProvider> getProviders() {
        synchronized (mLock) {
            if (mProviders == null) {
                return Collections.emptyList();
            }
            // we know the array is unmodifiable, so wrap it and return an unmodifiable view
            // to avoid an array copy (List.of)
            return Collections.unmodifiableList(Arrays.asList(mProviders));
        }
    }

    /**
     * Appends a provider to the end of the provider list. If the provider is already present in the
     * loader list, the list will not be modified.
     *
     * @param resourcesProvider the provider to add
     */
    public void addProvider(@NonNull ResourcesProvider resourcesProvider) {
        synchronized (mLock) {
            if (mProviders == null) {
                mProviders = new ResourcesProvider[]{resourcesProvider};
            } else {
                for (ResourcesProvider provider : mProviders) {
                    if (provider == resourcesProvider) {
                        return;
                    }
                }
                int end = mProviders.length;
                mProviders = Arrays.copyOf(mProviders, end + 1);
                mProviders[end] = resourcesProvider;
            }
            notifyProvidersChangedLocked();
        }
    }

    /**
     * Removes a provider from the provider list. If the provider is not present in the provider
     * list, the list will not be modified.
     *
     * @param resourcesProvider the provider to remove
     */
    public void removeProvider(@NonNull ResourcesProvider resourcesProvider) {
        synchronized (mLock) {
            if (mProviders == null) {
                return;
            }
            var providers = mProviders;
            int length = providers.length;
            for (int i = 0; i < length; i++) {
                if (providers[i] == resourcesProvider) {
                    if (length == 1) {
                        mProviders = null;
                    } else {
                        mProviders = new ResourcesProvider[length - 1];
                        System.arraycopy(providers, 0, mProviders, 0, i);
                        System.arraycopy(providers, i + 1, mProviders, i, length - i - 1);
                    }
                    notifyProvidersChangedLocked();
                    return;
                }
            }
        }
    }

    /**
     * Sets the list of providers.
     *
     * @param resourcesProviders the new providers
     */
    public void setProviders(@NonNull List<ResourcesProvider> resourcesProviders) {
        synchronized (mLock) {
            mProviders = resourcesProviders.toArray(new ResourcesProvider[0]);
            notifyProvidersChangedLocked();
        }
    }

    /**
     * Removes all {@link ResourcesProvider ResourcesProvider(s)}.
     */
    public void clearProviders() {
        synchronized (mLock) {
            mProviders = null;
            notifyProvidersChangedLocked();
        }
    }

    /**
     * Retrieves the list of {@link PackAssets} used by the providers.
     *
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    @NonNull
    public List<PackAssets> getApkAssets() {
        synchronized (mLock) {
            if (mPackAssets == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(mPackAssets);
        }
    }

    /**
     * Registers a callback to be invoked when {@link ResourcesProvider ResourcesProvider(s)}
     * change.
     * @param instance the instance tied to the callback
     * @param callbacks the callback to invoke
     *
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public void registerOnProvidersChangedCallback(@NonNull Object instance,
                                                   @NonNull UpdateCallbacks callbacks) {
        synchronized (mLock) {
            mChangeCallbacks.put(new WeakReference<>(instance), callbacks);
        }
    }

    /**
     * Removes a previously registered callback.
     * @param instance the instance tied to the callback
     *
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public void unregisterOnProvidersChangedCallback(@NonNull Object instance) {
        synchronized (mLock) {
            for (int i = 0, n = mChangeCallbacks.size(); i < n; i++) {
                final WeakReference<Object> key = mChangeCallbacks.keyAt(i);
                if (key.refersTo(instance)) {
                    mChangeCallbacks.removeAt(i);
                    return;
                }
            }
        }
    }

    /**
     * Invokes registered callbacks when the list of {@link ResourcesProvider} instances this loader
     * uses changes.
     */
    private void notifyProvidersChangedLocked() {
        if (Arrays.equals(mPreviousProviders, mProviders)) {
            return;
        }

        if (mProviders == null || mProviders.length == 0) {
            mPackAssets = null;
        } else {
            mPackAssets = new PackAssets[mProviders.length];
            for (int i = 0, n = mProviders.length; i < n; i++) {
                mProviders[i].incUsageCount();
                mPackAssets[i] = mProviders[i].getPackAssets();
            }
        }

        // Decrement the ref count after incrementing the new provider ref count so providers
        // present before and after this method do not drop to zero references.
        if (mPreviousProviders != null) {
            for (ResourcesProvider provider : mPreviousProviders) {
                provider.decUsageCount();
            }
        }

        mPreviousProviders = mProviders;

        final ObjectOpenHashSet<UpdateCallbacks> uniqueCallbacks = new ObjectOpenHashSet<>();

        for (int i = mChangeCallbacks.size() - 1; i >= 0; i--) {
            final WeakReference<Object> key = mChangeCallbacks.keyAt(i);
            if (key.refersTo(null)) {
                mChangeCallbacks.removeAt(i);
            } else {
                uniqueCallbacks.add(mChangeCallbacks.valueAt(i));
            }
        }

        uniqueCallbacks.forEach(mInvokeCallbacks);
    }
}
