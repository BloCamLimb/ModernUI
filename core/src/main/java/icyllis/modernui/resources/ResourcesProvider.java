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
import icyllis.modernui.core.Core;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.concurrent.GuardedBy;
import java.lang.ref.Cleaner;

public class ResourcesProvider implements AutoCloseable {

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private boolean mOpen;

    @GuardedBy("mLock")
    private int mUsageCount;

    private PackAssets mPackAssets;
    private final Cleaner.Cleanable mCleanup;

    ResourcesProvider(@NonNull PackAssets packAssets) {
        mOpen = true;
        mUsageCount = 0;
        mPackAssets = packAssets;
        mCleanup = Core.registerNativeResource(this, packAssets);
    }

    void incUsageCount() {
        synchronized (mLock) {
            if (!mOpen) {
                throw new IllegalStateException("Operation failed: resources provider is closed");
            }
            mUsageCount++;
        }
    }

    void decUsageCount() {
        synchronized (mLock) {
            mUsageCount--;
        }
    }

    /**
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public PackAssets getPackAssets() {
        return mPackAssets;
    }

    /**
     * Frees internal data structures.
     * <p>
     * Closed providers can no longer be added to {@link ResourcesLoader ResourcesLoader(s)}.
     * This method can only be called when this provider is not being used by any ResourceLoader.
     * <p>
     * When this object becomes phantom-reachable, the system will automatically
     * do this cleanup operation.
     *
     * @throws IllegalStateException if provider is currently used by a ResourcesLoader
     */
    @Override
    public void close() {
        synchronized (mLock) {
            if (!mOpen) {
                return;
            }
            if (mUsageCount != 0) {
                throw new IllegalStateException("Failed to close provider used by " + mUsageCount
                        + " ResourcesLoader instances");
            }
            mOpen = false;
        }
        mPackAssets = null;
        mCleanup.clean();
    }
}
