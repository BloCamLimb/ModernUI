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

import icyllis.arc3d.SharedPtr;
import icyllis.modernui.graphics.ImageInfo;

public class SurfaceDrawContext extends SurfaceFillContext {

    public SurfaceDrawContext(RecordingContext context,
                              SurfaceProxyView readView,
                              SurfaceProxyView writeView,
                              int colorType) {
        super(context, readView, writeView,
                ImageInfo.makeColorInfo(colorType, ImageInfo.AT_PREMUL));
    }

    public static SurfaceDrawContext make(
            RecordingContext context,
            int colorType,
            int width, int height,
            int sampleCount,
            int surfaceFlags,
            int origin) {
        if (context == null || context.isDiscarded()) {
            return null;
        }

        BackendFormat format = context.getCaps().getDefaultBackendFormat(colorType, true);
        if (format == null) {
            return null;
        }

        @SharedPtr
        TextureProxy proxy = context.getProxyProvider().createRenderTextureProxy(
                format,
                width,
                height,
                sampleCount,
                surfaceFlags
        );
        if (proxy == null) {
            return null;
        }

        short readSwizzle = context.getCaps().getReadSwizzle(format, colorType);
        short writeSwizzle = context.getCaps().getWriteSwizzle(format, colorType);

        // two views, inc one more ref
        proxy.ref();
        SurfaceProxyView readView = new SurfaceProxyView(proxy, origin, readSwizzle);
        SurfaceProxyView writeView = new SurfaceProxyView(proxy, origin, writeSwizzle);

        return new SurfaceDrawContext(context, readView, writeView, colorType);
    }
}
