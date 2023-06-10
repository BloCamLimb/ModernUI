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

package icyllis.modernui.graphics;

import icyllis.arc3d.engine.RenderTarget;

/**
 * Surface is responsible for managing the pixels that a canvas draws into.
 * The pixels will be allocated on the GPU (a {@link RenderTarget} surface).
 * Surface takes care of allocating a {@link Canvas} that will draw into the surface.
 * Call {@link #getCanvas()} to use that canvas (it is owned by the surface).
 * Surface always has non-zero dimensions. If there is a request for a new surface,
 * and either of the requested dimensions are zero, then null will be returned.
 */
public class Surface extends RefCnt {

    @SharedPtr
    private BaseDevice mDevice;

    public Surface(@SharedPtr BaseDevice device) {
        mDevice = device;
    }

    @Override
    protected void deallocate() {
        mDevice.unref();
        mDevice = null;
    }
}
