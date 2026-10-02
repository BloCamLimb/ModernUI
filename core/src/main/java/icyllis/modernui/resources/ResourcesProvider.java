/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
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

import icyllis.modernui.annotation.NonNull;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.concurrent.GuardedBy;
import java.io.Closeable;
import java.io.IOException;

public class ResourcesProvider implements Closeable {

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private boolean mOpen;

    @GuardedBy("mLock")
    private int mUsageCount;

    private AssetPack mAssetPack;

    ResourcesProvider(@NonNull AssetPack assetPack) {
        mOpen = true;
        mUsageCount = 0;
        mAssetPack = assetPack;
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
    public AssetPack getAssetPack() {
        return mAssetPack;
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
    public void close() throws IOException {
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
        mAssetPack.close();
        mAssetPack = null;
    }
}
