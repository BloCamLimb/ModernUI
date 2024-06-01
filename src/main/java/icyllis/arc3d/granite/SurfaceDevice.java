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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.geom.BoundsManager;
import icyllis.arc3d.granite.geom.SDFRoundRectStep;

import java.util.ArrayList;

/**
 * The device that is backed by GPU.
 */
public final class SurfaceDevice extends icyllis.arc3d.core.Device {

    private SurfaceDrawContext mSDC;
    private ClipStack mClipStack;

    private final ArrayList<ClipStack.Element> mElementsForMask = new ArrayList<>();

    private int mCurrentDepth;

    private final BoundsManager mBoundsManager = new BoundsManager();

    private GeometryRenderer mSimpleRoundRectRenderer = new GeometryRenderer(
            "SimpleRRectStep", new SDFRoundRectStep()
    );

    private SurfaceDevice(SurfaceDrawContext context, ImageInfo info, boolean clear) {
        super(info);
        mSDC = context;
        mClipStack = new ClipStack(this);
    }

    @SharedPtr
    private static SurfaceDevice make(SurfaceDrawContext sdc,
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
            return new SurfaceDevice(sdc, info, clear);
        }
        return null;
    }

    @SharedPtr
    public static SurfaceDevice make(RecordingContext rContext,
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
    public static SurfaceDevice make(RecordingContext rContext,
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
    protected void onSave() {
        super.onSave();
        mClipStack.save();
    }

    @Override
    protected void onRestore() {
        super.onRestore();
        mClipStack.restore();
    }

    public ClipStack getClipStack() {
        return mClipStack;
    }

    @Override
    public void clipRect(Rect2f rect, int clipOp, boolean doAA) {
        mClipStack.clipRect(getLocalToDevice(), rect, clipOp);
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

    private final Rect2f mTmpOpBounds = new Rect2f();

    public void drawRoundRect(RoundRect r, Paint paint) {
        DrawOp draw = new DrawOp();
        draw.mTransform = getLocalToDevice().clone();
        draw.mGeometry = r;

        mTmpOpBounds.set(r.mLeft, r.mTop, r.mRight, r.mBottom);
        drawGeometry(draw, mTmpOpBounds, paint, mSimpleRoundRectRenderer);
    }

    public void drawGeometry(DrawOp draw,
                             Rect2f opBounds,
                             Paint paint,
                             GeometryRenderer renderer) {
        if (renderer == null) {
            return;
        }
        draw.mRenderer = renderer;

        if (paint.getStyle() == Paint.FILL) {
            draw.mStrokeRadius = -1;
        } else {
            draw.mStrokeRadius = paint.getStrokeWidth() * 0.5f;
            switch (paint.getStrokeJoin()) {
                case Paint.JOIN_ROUND -> draw.mJoinLimit = -1;
                case Paint.JOIN_BEVEL -> draw.mJoinLimit = 0;
                case Paint.JOIN_MITER -> draw.mJoinLimit = paint.getStrokeMiter();
            }
            draw.mStrokeCap = paint.getStrokeCap();
        }

        final boolean outsetBoundsForAA = true;

        boolean clippedOut = mClipStack.prepareForDraw(draw, opBounds, outsetBoundsForAA, mElementsForMask);
        if (clippedOut) {
            return;
        }

        int drawDepth = mCurrentDepth + 1;
        int clipOrder = mClipStack.updateForDraw(draw, mElementsForMask, mBoundsManager, drawDepth);

        long drawOrder = DrawOrder.makeFromDepthAndPaintersOrder(
                drawDepth, clipOrder
        );
        draw.mDrawOrder = drawOrder;

        mSDC.recordDraw(draw);

        mBoundsManager.recordDraw(draw.mDrawBounds, clipOrder);
        mCurrentDepth = drawDepth;
    }

    public void drawClipShape(DrawOp draw, boolean inverseFill) {
        //TODO
    }
}
