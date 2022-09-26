/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.engine;

import javax.annotation.Nullable;

/**
 * Lazy-callback or wrapped a render target (no texture access).
 */
//TODO
public final class RenderSurfaceProxy extends SurfaceProxy {

    private RenderTarget mRenderTarget;

    RenderSurfaceProxy(BackendFormat format, int width, int height, int surfaceFlags) {
        super(format, width, height, surfaceFlags);
    }

    @Override
    protected void dispose() {
    }

    @Nullable
    @Override
    public RenderTarget peekRenderTarget() {
        return mRenderTarget;
    }
}
