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

import icyllis.arcticgi.core.ImageInfo;
import icyllis.arcticgi.core.SharedPtr;

public class SurfaceDrawContext extends SurfaceFillContext {

    public SurfaceDrawContext(RecordingContext context,
                              SurfaceProxyView readView,
                              SurfaceProxyView writeView,
                              int colorType) {
        super(context, readView, writeView, ImageInfo.makeColorInfo(colorType, ImageInfo.ALPHA_PREMULTIPLIED));
    }

    public static SurfaceDrawContext make(
            RecordingContext context,
            BackendFormat format,
            int width, int height,
            int sampleCount,
            boolean mipmapped,
            boolean backingFit,
            boolean budgeted,
            boolean isProtected,
            int surfaceFlags,
            int origin,
            short readSwizzle,
            short writeSwizzle) {
        if (context.isDropped()) {
            return null;
        }

        @SharedPtr
        TextureProxy proxy = context.getProxyProvider().createRenderTextureProxy(
                format,
                width,
                height,
                sampleCount,
                mipmapped,
                backingFit,
                budgeted,
                surfaceFlags,
                true
        );
        if (proxy == null) {
            return null;
        }

        // two views, inc one more ref
        proxy.ref();
        SurfaceProxyView readView = new SurfaceProxyView(proxy, origin, readSwizzle);
        SurfaceProxyView writeView = new SurfaceProxyView(proxy, origin, writeSwizzle);

        return new SurfaceDrawContext(context, readView, writeView, ImageInfo.COLOR_UNKNOWN);
    }
}
