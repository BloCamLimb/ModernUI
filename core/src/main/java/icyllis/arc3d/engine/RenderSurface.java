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

package icyllis.arc3d.engine;

import icyllis.modernui.annotation.SharedPtr;
import icyllis.modernui.core.RefCnt;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Manages a wrapped {@link RenderTarget} on the client (no texture access).
 * For example, the OpenGL default framebuffer (framebuffer id = 0).
 */
public final class RenderSurface extends RefCnt implements RenderTarget {

    @SharedPtr
    private FramebufferSet mFramebufferSet;

    RenderSurface(@SharedPtr FramebufferSet fs) {
        assert (fs != null && fs.getColorBuffer() == null);
        mFramebufferSet = fs;
    }

    @Override
    protected void deallocate() {
        mFramebufferSet = RefCnt.move(mFramebufferSet);
    }

    @Override
    public int getWidth() {
        return mFramebufferSet.getWidth();
    }

    @Override
    public int getHeight() {
        return mFramebufferSet.getHeight();
    }

    @Override
    public int getSampleCount() {
        return mFramebufferSet.getSampleCount();
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        return mFramebufferSet.getBackendFormat();
    }

    @Override
    public int getSurfaceFlags() {
        return mFramebufferSet.getSurfaceFlags();
    }

    @Override
    public FramebufferSet getFramebufferSet() {
        return Objects.requireNonNull(mFramebufferSet, "Disposed");
    }
}
