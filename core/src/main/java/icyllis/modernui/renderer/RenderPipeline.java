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

import icyllis.arc3d.core.ColorSpace;
import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.Engine;
import icyllis.arc3d.granite.GraniteSurface;
import icyllis.arc3d.sketch.NullSurface;
import icyllis.arc3d.sketch.Surface;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Core;

public abstract class RenderPipeline {

    // Render thread
    @Nullable
    public abstract WindowSurface createWindowSurface(long window,
                                                      int format,
                                                      ColorSpace colorSpace);

    // UI thread
    @Nullable
    @SharedPtr
    public Surface createSurface(ImageInfo info) {
        @SharedPtr
        Surface surf = null;

        var gpuContext = Core.peekUiRecordingContext();
        if (gpuContext != null) {
            surf = GraniteSurface.makeRenderTarget(
                    gpuContext,
                    info,
                    false,
                    Engine.SurfaceOrigin.kUpperLeft,
                    null
            );
        }

        if (surf != null) {
            return surf;
        }

        surf = NullSurface.make(info.width(), info.height());

        return surf;
    }
}
