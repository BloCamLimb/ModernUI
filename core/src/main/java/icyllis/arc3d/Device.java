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

package icyllis.arc3d;

import icyllis.arc3d.engine.*;
import icyllis.modernui.graphics.ImageInfo;
import icyllis.modernui.graphics.Paint;

/**
 * Engine implementation for canvas devices.
 */
public final class Device extends BaseDevice {

    private ClipStack mClipStack;

    private Device(SurfaceDrawContext context, ImageInfo info, boolean clear) {
        super(info);
    }

    @SharedPtr
    private static Device make(SurfaceDrawContext sdc,
                               int alphaType,
                               boolean clear) {
        if (sdc == null) {
            return null;
        }
        if (alphaType != ImageInfo.AT_PREMUL && alphaType != ImageInfo.AT_OPAQUE) {
            return null;
        }
        RecordingContext rContext = sdc.getContext();
        if (rContext.isDiscarded()) {
            return null;
        }
        int colorType = Engine.colorTypeToPublic(sdc.getColorType());
        if (rContext.isSurfaceCompatible(colorType)) {
            ImageInfo info = new ImageInfo(sdc.getWidth(), sdc.getHeight(), colorType, alphaType, null);
            return new Device(sdc, info, clear);
        }
        return null;
    }

    @SharedPtr
    public static Device make(RecordingContext rContext,
                              int colorType,
                              int alphaType,
                              int width, int height,
                              int sampleCount,
                              int surfaceFlags,
                              int origin,
                              boolean clear) {
        if (rContext == null) {
            return null;
        }
        SurfaceDrawContext sdc = SurfaceDrawContext.make(rContext,
                colorType, width, height, sampleCount, surfaceFlags, origin);
        return make(sdc, alphaType, clear);
    }

    @Override
    public boolean clipIsAA() {
        return false;
    }

    @Override
    public boolean clipIsWideOpen() {
        return false;
    }

    @Override
    protected int getClipType() {
        return 0;
    }

    @Override
    protected Rect2i getClipBounds() {
        return null;
    }

    @Override
    protected void drawPaint(Paint paint) {

    }
}
