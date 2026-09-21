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

package icyllis.modernui.renderer;

import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.core.ColorSpace;
import icyllis.arc3d.sketch.Surface;
import org.jetbrains.annotations.ApiStatus;

/**
 * Represents the surface and its swapchain of a platform window,
 * depending on the video driver and current graphics API.
 * <p>
 * This class is used by multiple threads.
 *
 * @hidden
 */
@ApiStatus.Internal
public abstract class WindowSurface {

    public boolean serverNeedsSwapchainRecreation() {
        return false;
    }

    /**
     * Requires external synchronization.
     */
    public long createSwapchain(int clientWidth, int clientHeight,
                                @ColorInfo.ColorType int desiredColorType,
                                ColorSpace desiredColorSpace) {
        return 0;
    }

    public void waitAcquireNextImage() {
    }

    public int acquireNextImage(long swapchain) {
        return 0;
    }

    /**
     * Requires external synchronization.
     */
    public abstract Surface getCurrentSurface();
}
