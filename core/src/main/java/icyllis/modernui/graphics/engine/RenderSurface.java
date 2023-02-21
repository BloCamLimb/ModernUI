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

package icyllis.modernui.graphics.engine;

import icyllis.modernui.graphics.RefCnt;
import icyllis.modernui.graphics.SharedPtr;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Manages a wrapped {@link RenderTarget} on the client (no texture access).
 */
public final class RenderSurface extends RefCnt implements Surface {

    @SharedPtr
    private RenderTarget mRenderTarget;

    RenderSurface(@SharedPtr RenderTarget target) {
        assert (target != null && target.getTexture() == null);
        mRenderTarget = target;
    }

    @Override
    protected void dispose() {
        mRenderTarget = RefCnt.move(mRenderTarget);
    }

    @Override
    public int getWidth() {
        return mRenderTarget.getWidth();
    }

    @Override
    public int getHeight() {
        return mRenderTarget.getHeight();
    }

    @Override
    public int getSampleCount() {
        return mRenderTarget.getSampleCount();
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        return mRenderTarget.getBackendFormat();
    }

    @Override
    public int getSurfaceFlags() {
        return mRenderTarget.getSurfaceFlags();
    }

    @Override
    public RenderTarget getRenderTarget() {
        return Objects.requireNonNull(mRenderTarget, "Disposed");
    }
}
