/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine.graphene;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.geom.SDFRoundRectStep;

/**
 * The device that is backed by GPU.
 */
public final class Device_Gpu extends icyllis.arc3d.core.Device {

    private SurfaceDrawContext mSDC;
    private ClipStack mClipStack;

    private GeometryRenderer mSimpleRoundRectRenderer = new GeometryRenderer(
            "SimpleRRectStep", new SDFRoundRectStep(false)
    );

    private Device_Gpu(SurfaceDrawContext context, ImageInfo info, boolean clear) {
        super(info);
        mSDC = context;
    }

    @SharedPtr
    private static Device_Gpu make(SurfaceDrawContext sdc,
                                   int alphaType,
                                   boolean clear) {
        if (sdc == null) {
            return null;
        }
        if (alphaType != ColorInfo.AT_PREMUL && alphaType != ColorInfo.AT_OPAQUE) {
            return null;
        }
        RecordingContext rContext = sdc.getContext();
        if (rContext.isDiscarded()) {
            return null;
        }
        int colorType = Engine.colorTypeToPublic(sdc.getColorType());
        //TODO F
        if (true/*rContext.isSurfaceCompatible(colorType)*/) {
            ImageInfo info = new ImageInfo(sdc.getWidth(), sdc.getHeight(), colorType, alphaType, null);
            return new Device_Gpu(sdc, info, clear);
        }
        return null;
    }

    @SharedPtr
    public static Device_Gpu make(RecordingContext rContext,
                                  int colorType,
                                  int alphaType,
                                  ColorSpace colorSpace,
                                  int width, int height,
                                  int sampleCount,
                                  int surfaceFlags,
                                  int origin,
                                  boolean clear) {
        if (rContext == null) {
            return null;
        }
        SurfaceDrawContext sdc = SurfaceDrawContext.make(rContext,
                colorType, colorSpace, width, height, sampleCount, surfaceFlags, origin);
        return make(sdc, alphaType, clear);
    }

    @SharedPtr
    public static Device_Gpu make(RecordingContext rContext,
                                  int colorType,
                                  ColorSpace colorSpace,
                                  SurfaceProxy proxy,
                                  int origin,
                                  boolean clear) {
        if (rContext == null) {
            return null;
        }
        /*SurfaceDrawContext sdc = SurfaceDrawContext.make(rContext,
                colorType, colorSpace, proxy, origin);*/
        //FIXME
        //return make(sdc, ColorInfo.AT_PREMUL, clear);
        return null;
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
    protected Rect2ic getClipBounds() {
        return null;
    }

    @Override
    public void drawPaint(Paint paint) {

    }

    @Override
    public void drawRect(Rect2f r, Paint paint) {

    }

    public void drawRoundRect(RoundRect r, Paint paint) {
        mSDC.recordDraw(mSimpleRoundRectRenderer,
                getLocalToDevice(),
                r,
                null,
                0);
    }
}
