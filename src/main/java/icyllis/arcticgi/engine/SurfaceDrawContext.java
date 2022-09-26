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
        super(context, readView, writeView, ImageInfo.makeColorInfo(colorType, ImageInfo.ALPHA_TYPE_PREMUL));
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
