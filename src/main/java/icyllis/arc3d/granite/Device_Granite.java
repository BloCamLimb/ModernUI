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
import icyllis.arc3d.granite.geom.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nullable;

/**
 * The device that is backed by GPU.
 */
public final class Device_Granite extends icyllis.arc3d.core.Device {

    private final RecordingContext mContext;
    private SurfaceDrawContext mSDC;
    private ClipStack mClipStack;

    private final ObjectArrayList<ClipStack.Element> mElementsForMask = new ObjectArrayList<>();

    private int mCurrentDepth;

    private final BoundsManager mBoundsManager;

    private Device_Granite(RecordingContext context, SurfaceDrawContext sdc) {
        super(sdc.getImageInfo());
        mContext = context;
        mSDC = sdc;
        mClipStack = new ClipStack(this);
        /*mBoundsManager = GridBoundsManager.makeRes(
                width(), height(),
                16, 32
        );*/
        //mBoundsManager = new SimpleBoundsManager();
        mBoundsManager = new FullBoundsManager();
    }

    @Nullable
    @SharedPtr
    public static Device_Granite make(RecordingContext rContext,
                                      ImageInfo deviceInfo,
                                      int surfaceFlags,
                                      int origin,
                                      byte initialLoadOp,
                                      String label) {
        if (rContext == null) {
            return null;
        }

        if ((surfaceFlags & ISurface.FLAG_MIPMAPPED) != 0 &&
                (surfaceFlags & ISurface.FLAG_APPROX_FIT) != 0) {
            // mipmapping requires full size
            return null;
        }

        int backingWidth = deviceInfo.width();
        int backingHeight = deviceInfo.height();
        if ((surfaceFlags & ISurface.FLAG_APPROX_FIT) != 0) {
            backingWidth = ISurface.getApproxSize(backingWidth);
            backingHeight = ISurface.getApproxSize(backingHeight);
        }

        ImageDesc desc = rContext.getCaps().getDefaultColorImageDesc(
                Engine.ImageType.k2D,
                deviceInfo.colorType(),
                backingWidth,
                backingHeight,
                1,
                surfaceFlags | ISurface.FLAG_RENDERABLE);
        if (desc == null) {
            return null;
        }
        short readSwizzle = rContext.getCaps().getReadSwizzle(
                desc, deviceInfo.colorType());
        @SharedPtr
        ImageViewProxy targetView = ImageViewProxy.make(rContext, desc,
                origin, readSwizzle,
                (surfaceFlags & ISurface.FLAG_BUDGETED) != 0, label);
        if (targetView == null) {
            return null;
        }

        return make(rContext, targetView, deviceInfo, initialLoadOp);
    }

    @SharedPtr
    public static Device_Granite make(RecordingContext context,
                                      @SharedPtr ImageViewProxy targetView,
                                      ImageInfo deviceInfo,
                                      byte initialLoadOp) {
        if (context == null) {
            return null;
        }
        SurfaceDrawContext sdc = SurfaceDrawContext.make(context,
                targetView, deviceInfo);
        if (sdc == null) {
            return null;
        }
        if (initialLoadOp == Engine.LoadOp.kClear) {
            sdc.clear(null);
        } else if (initialLoadOp == Engine.LoadOp.kDiscard) {
            sdc.discard();
        }

        return new Device_Granite(context, sdc);
    }

    @Override
    protected void deallocate() {
        super.deallocate();
        mSDC.close();
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
    public boolean isClipAA() {
        return false;
    }

    @Override
    public boolean isClipEmpty() {
        return mClipStack.currentClipState() == ClipStack.STATE_EMPTY;
    }

    @Override
    public boolean isClipRect() {
        var state = mClipStack.currentClipState();
        return state == ClipStack.STATE_DEVICE_RECT || state == ClipStack.STATE_WIDE_OPEN;
    }

    @Override
    public boolean isClipWideOpen() {
        return mClipStack.currentClipState() == ClipStack.STATE_WIDE_OPEN;
    }

    private final Rect2f mTmpClipBounds = new Rect2f();
    private final Rect2i mTmpClipBoundsI = new Rect2i();

    @Override
    protected Rect2ic getClipBounds() {
        mClipStack.getConservativeBounds(mTmpClipBounds);
        mTmpClipBounds.roundOut(mTmpClipBoundsI);
        return mTmpClipBoundsI;
    }

    private final Rect2f mTmpOpBounds = new Rect2f();

    @Override
    public void drawPaint(Paint paint) {
        float[] color = new float[4];
        if (PaintParams.getSolidColor(paint, mInfo, color)) {
            mSDC.clear(color);
        }
    }

    @Override
    public void drawPoints(float[] pts, int offset, int count, Paint paint) {

    }

    @Override
    public void drawLine(float x0, float y0, float x1, float y1,
                         @Paint.Cap int cap, float width, Paint paint) {
        Draw draw = new Draw();
        draw.mTransform = getLocalToDevice();
        var shape = new SimpleShape();
        shape.setLine(x0, y0, x1, y1, cap, width);
        draw.mGeometry = shape;
        shape.getBounds(mTmpOpBounds);
        drawGeometry(draw, mTmpOpBounds, paint,
                mContext.getRendererProvider().getSimpleBox(paint.isAntiAlias()));
    }

    @Override
    public void drawRect(Rect2fc r, Paint paint) {
        Draw draw = new Draw();
        draw.mTransform = getLocalToDevice();
        var shape = new SimpleShape();
        shape.setRect(r);
        draw.mGeometry = shape;
        mTmpOpBounds.set(r);
        drawGeometry(draw, mTmpOpBounds, paint,
                mContext.getRendererProvider().getSimpleBox(paint.isAntiAlias()));
    }

    @Override
    public void drawCircle(float cx, float cy, float radius, Paint paint) {
        Draw draw = new Draw();
        draw.mTransform = getLocalToDevice();
        var shape = new SimpleShape();
        shape.setEllipse(cx - radius, cy - radius, cx + radius, cy + radius);
        draw.mGeometry = shape;
        shape.getBounds(mTmpOpBounds);
        drawGeometry(draw, mTmpOpBounds, paint,
                mContext.getRendererProvider().getSimpleBox(paint.isAntiAlias()));
    }

    @Override
    public void drawRoundRect(RoundRect rr, Paint paint) {
        Draw draw = new Draw();
        draw.mTransform = getLocalToDevice();
        draw.mGeometry = new SimpleShape(rr);
        rr.getRect(mTmpOpBounds);
        drawGeometry(draw, mTmpOpBounds, paint,
                mContext.getRendererProvider().getSimpleBox(paint.isAntiAlias()));
    }

    //TODO
    public void drawArc(ArcShape arc, int cap, Paint paint) {
        Draw draw = new Draw();
        draw.mTransform = getLocalToDevice();
        draw.mGeometry = arc;
        mTmpOpBounds.set(arc.mCenterX - arc.mRadius, arc.mCenterY - arc.mRadius,
                arc.mCenterX + arc.mRadius, arc.mCenterY + arc.mRadius);
        drawGeometry(draw, mTmpOpBounds, paint,
                mContext.getRendererProvider().getArc(cap));
    }

    public void drawAtlasSubRun(SubRunContainer.AtlasSubRun subRun,
                                float originX, float originY,
                                Paint paint) {
        int maskFormat = subRun.getMaskFormat();
        if (!mContext.getAtlasProvider().getGlyphAtlasManager().initAtlas(
                maskFormat
        )) {
            return;
        }

        int subRunEnd = subRun.getGlyphCount();
        boolean flushed = false;
        for (int subRunCursor = 0; subRunCursor < subRunEnd;) {
            int glyphsPrepared = subRun.prepareGlyphs(subRunCursor, subRunEnd,
                    mContext);
            if (glyphsPrepared < 0) {
                // There was a problem allocating the glyph in the atlas.
                return;
            }
            if (glyphsPrepared > 0) {
                SubRunData subRunData = new SubRunData(subRun,
                        getLocalToDevice(), originX, originY,
                        subRunCursor, glyphsPrepared);
                // subRunToDevice is our "localToDevice",
                // as sub run's coordinates are returned in sub run's space
                Matrix4 subRunToDevice = new Matrix4(getLocalToDevice());
                subRunToDevice.preConcat2D(subRunData.getSubRunToLocal());

                Draw draw = new Draw();
                draw.mTransform = subRunToDevice;
                draw.mGeometry = subRunData;
                var bounds = new Rect2f(subRunData.getBounds());
                drawGeometry(draw, bounds, paint,
                        mContext.getRendererProvider().getRasterText(maskFormat));
                //TODO

            } else if (flushed) {
                // Treat as an error.
                return;
            }
            subRunCursor += glyphsPrepared;

            if (subRunCursor < subRunEnd) {
                // Flush if not all the glyphs are handled because the atlas is out of space.
                // We flush every Device because the glyphs that are being flushed/referenced are not
                // necessarily specific to this Device. This addresses both multiple SkSurfaces within
                // a Recorder, and nested layers.
                //TODO
                flushed = true;
            }
        }
    }

    private static boolean blender_depends_on_dst(Blender blender,
                                                  boolean srcIsTransparent) {
        BlendMode bm = blender != null ? blender.asBlendMode() : BlendMode.SRC_OVER;
        if (bm == null) {
            // custom blender
            return true;
        }
        if (bm == BlendMode.SRC || bm == BlendMode.CLEAR) {
            // src and clear blending never depends on dst
            return false;
        }
        if (bm == BlendMode.SRC_OVER) {
            // src-over depends on dst if src is transparent (a != 1)
            return srcIsTransparent;
        }
        return true;
    }

    public void drawGeometry(Draw draw,
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
            draw.mStrokeCap = (byte) paint.getStrokeCap();
            draw.mStrokeAlign = (byte) paint.getStrokeAlign();
        }

        final boolean outsetBoundsForAA = true;
        mElementsForMask.clear();
        boolean clippedOut = mClipStack.prepareForDraw(draw, opBounds, outsetBoundsForAA, mElementsForMask);
        if (clippedOut) {
            return;
        }

        boolean needsFlush = needsFlushBeforeDraw(1);
        if (needsFlush) {
            flushPendingWork();
        }

        // Update the clip stack after issuing a flush (if it was needed). A draw will be recorded after
        // this point.
        int drawDepth = mCurrentDepth + 1;
        int clipOrder = mClipStack.updateForDraw(draw, mElementsForMask, mBoundsManager, drawDepth);

        draw.mPaintParams = new PaintParams(paint, null);
        { //TODO simplify this branch
            int prevDraw = mBoundsManager.getMostRecentDraw(draw.mDrawBounds);
            int nextOrder = prevDraw + 1;
            clipOrder = Math.max(clipOrder, nextOrder);
        }

        draw.mDrawOrder = DrawOrder.makeFromDepthAndPaintersOrder(
                drawDepth, clipOrder
        );

        mSDC.recordDraw(draw);

        mBoundsManager.recordDraw(draw.mDrawBounds, clipOrder);
        mCurrentDepth = drawDepth;
    }

    public void drawClipShape(Draw draw, boolean inverseFill) {
        //TODO
    }

    private boolean needsFlushBeforeDraw(int numNewRenderSteps) {
        return (DrawPass.MAX_RENDER_STEPS - mSDC.pendingNumSteps()) < numNewRenderSteps;
    }

    /**
     * Ensures clip elements are drawn that will clip previous draw calls, snaps all pending work
     * from the {@link SurfaceDrawContext} as a {@link RenderPassTask} and records it in the
     * {@link Device_Granite}'s {@link RecordingContext}.
     */
    public void flushPendingWork() {
        mContext.getAtlasProvider().recordUploads(mSDC);

        // Clip shapes are depth-only draws, but aren't recorded in the DrawContext until a flush in
        // order to determine the Z values for each element.
        mClipStack.recordDeferredClipDraws();

        // Flush all pending items to the internal task list and reset Device tracking state
        mSDC.flush(mContext);

        mBoundsManager.clear();
        mCurrentDepth = DrawOrder.MIN_VALUE;

        DrawTask drawTask = mSDC.snapDrawTask(mContext);

        if (drawTask != null) {
            mContext.addTask(drawTask);
        }
    }
}
