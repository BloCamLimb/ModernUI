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
import icyllis.arc3d.core.ColorSpaces;
import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.engine.Engine;
import icyllis.arc3d.granite.GraniteSurface;
import icyllis.arc3d.granite.RecordingContext;
import icyllis.arc3d.sketch.Surface;

public final class GLWindowSurface extends WindowSurface {

    // EGLSurface/HDC is stored inside this
    public long mWindow; // SDL_Window *
    private final GLManager mGLManager;

    public GraniteSurface mBackbufferSurface;
    private RecordingContext mContext;

    public GLWindowSurface(RecordingContext recordingContext, long window,
                           GLManager manager) {
        mContext = recordingContext;
        mWindow = window;
        mGLManager = manager;
    }

    @Override
    public boolean configure(int clientWidth, int clientHeight,
                          int desiredColorMode) {
        if (mBackbufferSurface != null) {
            mBackbufferSurface.unref();
        }
        //TODO use linear space
        mBackbufferSurface = GraniteSurface.wrapGLDefaultFramebuffer(
                mContext,
                ImageInfo.make(clientWidth, clientHeight,
                        ColorInfo.CT_RGBA_8888, ColorInfo.AT_PREMUL,
                        ColorSpaces.SRGB),
                Engine.SurfaceOrigin.kLowerLeft
        );
        return mBackbufferSurface != null;
    }

    @Override
    public GraniteSurface getCurrentSurface() {
        return mBackbufferSurface;
    }

    public long getWindow() {
        return mWindow;
    }
}
