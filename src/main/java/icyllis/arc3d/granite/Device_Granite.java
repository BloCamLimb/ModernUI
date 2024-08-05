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

import icyllis.arc3d.core.Image;
import icyllis.arc3d.core.*;
import icyllis.arc3d.core.effects.ColorFilter;
import icyllis.arc3d.core.shaders.ImageShader;
import icyllis.arc3d.core.shaders.Shader;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.geom.BoundsManager;
import icyllis.arc3d.granite.geom.FullBoundsManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiConsumer;

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
    public void pushClipStack() {
        mClipStack.save();
    }

    @Override
    public void popClipStack() {
        mClipStack.restore();
    }

    public ClipStack getClipStack() {
        return mClipStack;
    }

    @Override
    public void clipRect(Rect2fc rect, int clipOp, boolean doAA) {
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
    public void getClipBounds(@Nonnull Rect2i bounds) {
        mClipStack.getConservativeBounds(mTmpClipBounds);
        mTmpClipBounds.roundOut(bounds);
    }

    @Override
    protected Rect2ic getClipBounds() {
        mClipStack.getConservativeBounds(mTmpClipBounds);
        mTmpClipBounds.roundOut(mTmpClipBoundsI);
        return mTmpClipBoundsI;
    }

    @Override
    public void drawPaint(Paint paint) {
        float[] color = new float[4];
        if (PaintParams.getSolidColor(paint, mInfo, color)) {
            mSDC.clear(color);
        }
    }

    @Override
    public void drawPoints(int mode, float[] pts, int offset, int count, Paint paint) {
        // draw points by filling shape
        var oldStyle = paint.getStyle();
        paint.setStyle(Paint.FILL);
        var cap = paint.getStrokeCap();
        if (mode == Canvas.POINT_MODE_POINTS) {
            float radius = paint.getStrokeWidth() * 0.5f;
            if (cap == Paint.CAP_ROUND) {
                for (int i = offset, e = offset + count * 2; i < e; i += 2) {
                    drawCircle(pts[i], pts[i + 1], radius, paint);
                }
            } else {
                Rect2f rect = new Rect2f(-radius, -radius, radius, radius);
                for (int i = offset, e = offset + count * 2; i < e; i += 2) {
                    rect.offsetTo(pts[i], pts[i + 1]);
                    drawRect(rect, paint);
                }
            }
        } else {
            float width = paint.getStrokeWidth();
            int inc = mode == Canvas.POINT_MODE_LINES ? 4 : 2;
            for (int i = offset, e = offset + (count - 1) * 2; i < e; i += inc) {
                drawLine(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], cap, width, paint);
            }
        }
        paint.setStyle(oldStyle);
    }

    @Override
    public void drawLine(float x0, float y0, float x1, float y1,
                         @Paint.Cap int cap, float width, Paint paint) {
        var shape = new SimpleShape();
        shape.setLine(x0, y0, x1, y1, cap, width);
        drawGeometry(getLocalToDevice(), shape, SimpleShape::getBounds, paint,
                mContext.getRendererProvider().getSimpleBox(paint.isAntiAlias()), null);
    }

    @Override
    public void drawRect(Rect2fc r, Paint paint) {
        drawGeometry(getLocalToDevice(), new SimpleShape(r), SimpleShape::getBounds, paint,
                mContext.getRendererProvider().getSimpleBox(paint.isAntiAlias()), null);
    }

    @Override
    public void drawRoundRect(RoundRect rr, Paint paint) {
        drawGeometry(getLocalToDevice(), new SimpleShape(rr), SimpleShape::getBounds, paint,
                mContext.getRendererProvider().getSimpleBox(paint.isAntiAlias()), null);
    }

    @Override
    public void drawCircle(float cx, float cy, float radius, Paint paint) {
        var shape = new SimpleShape();
        shape.setEllipseXY(cx, cy, radius, radius);
        drawGeometry(getLocalToDevice(), shape, SimpleShape::getBounds, paint,
                mContext.getRendererProvider().getSimpleBox(paint.isAntiAlias()), null);
    }

    @Override
    public void drawArc(float cx, float cy, float radius, float startAngle,
                        float sweepAngle, int cap, float width, Paint paint) {
        var shape = new ArcShape(cx, cy, radius, startAngle, sweepAngle, width * 0.5f);
        shape.mType = switch (cap) {
            case Paint.CAP_BUTT -> ArcShape.kArc_Type;
            case Paint.CAP_ROUND -> ArcShape.kArcRound_Type;
            case Paint.CAP_SQUARE -> ArcShape.kArcSquare_Type;
            default -> throw new AssertionError();
        };
        drawGeometry(getLocalToDevice(), shape, ArcShape::getBounds, paint,
                mContext.getRendererProvider().getArc(shape.mType), null);
    }

    @Override
    public void drawPie(float cx, float cy, float radius, float startAngle,
                        float sweepAngle, Paint paint) {
        var shape = new ArcShape(cx, cy, radius, startAngle, sweepAngle, 0);
        shape.mType = ArcShape.kPie_Type;
        drawGeometry(getLocalToDevice(), shape, ArcShape::getBounds, paint,
                mContext.getRendererProvider().getArc(shape.mType), null);
    }

    @Override
    public void drawChord(float cx, float cy, float radius, float startAngle,
                          float sweepAngle, Paint paint) {
        var shape = new ArcShape(cx, cy, radius, startAngle, sweepAngle, 0);
        shape.mType = ArcShape.kChord_Type;
        drawGeometry(getLocalToDevice(), shape, ArcShape::getBounds, paint,
                mContext.getRendererProvider().getArc(shape.mType), null);
    }

    @Override
    public void drawImageRect(@RawPtr Image image, Rect2fc src, Rect2fc dst,
                              SamplingOptions sampling, Paint paint, int constraint) {
        Paint modifiedPaint = new Paint(paint);
        Rect2f modifiedDst = ImageShader.preparePaintForDrawImageRect(
                image, sampling, src, dst,
                constraint == Canvas.SRC_RECT_CONSTRAINT_STRICT,
                modifiedPaint
        );
        if (!modifiedDst.isEmpty()) {
            //TODO use edge AA quad
            drawRect(modifiedDst, modifiedPaint);
        }
        modifiedPaint.close();
    }

    @Override
    protected void onDrawGlyphRunList(Canvas canvas,
                                      GlyphRunList glyphRunList,
                                      Paint paint) {
        Matrix positionMatrix = new Matrix(getLocalToDevice33());
        positionMatrix.preTranslate(glyphRunList.mOriginX, glyphRunList.mOriginY);
        SubRunContainer container = SubRunContainer.make(
                glyphRunList,
                positionMatrix,
                paint,
                StrikeCache.getGlobalStrikeCache()
        );
        container.draw(canvas, glyphRunList.mOriginX, glyphRunList.mOriginY, paint, this);
    }

    @Override
    public void drawVertices(Vertices vertices, @SharedPtr Blender blender, Paint paint) {
        drawGeometry(getLocalToDevice(), vertices, Vertices::getBounds, paint,
                mContext.getRendererProvider().getVertices(
                        vertices.getVertexMode(), vertices.hasColors(), vertices.hasTexCoords()),
                blender); // move
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
        Paint subRunPaint = new Paint();
        boolean flushed = false;
        for (int subRunCursor = 0; subRunCursor < subRunEnd; ) {
            int glyphsPrepared = subRun.prepareGlyphs(subRunCursor, subRunEnd,
                    mContext);
            if (glyphsPrepared < 0) {
                // There was a problem allocating the glyph in the atlas.
                break;
            }
            if (glyphsPrepared > 0) {
                SubRunData subRunData = new SubRunData(subRun,
                        getLocalToDevice(), originX, originY,
                        subRunCursor, glyphsPrepared);
                // subRunToDevice is our "localToDevice",
                // as sub run's coordinates are returned in sub run's space
                Matrix4 subRunToDevice = new Matrix4(getLocalToDevice());
                subRunToDevice.preConcat2D(subRunData.getSubRunToLocal());

                subRunPaint.set(paint);
                if (subRun.getMaskFormat() == Engine.MASK_FORMAT_ARGB) {
                    subRunPaint.setShader(null);
                }
                subRunPaint.setStyle(Paint.FILL);

                drawGeometry(subRunToDevice, subRunData, SubRunData::getBounds, paint,
                        mContext.getRendererProvider().getRasterText(maskFormat),
                        BlendMode.DST_IN);
            } else if (flushed) {
                // Treat as an error.
                break;
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
        subRunPaint.close();
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

    private static boolean paint_depends_on_dst(float a,
                                                Shader shader,
                                                ColorFilter colorFilter,
                                                Blender finalBlender,
                                                Blender primitiveBlender) {
        boolean srcIsTransparent = a != 1.0f || (shader != null && !shader.isOpaque()) ||
                (colorFilter != null && !colorFilter.isAlphaUnchanged());

        if (primitiveBlender != null && blender_depends_on_dst(primitiveBlender, srcIsTransparent)) {
            return true;
        }

        return blender_depends_on_dst(finalBlender, srcIsTransparent);
    }

    private static boolean paint_depends_on_dst(PaintParams paintParams) {
        return paint_depends_on_dst(paintParams.a(),
                paintParams.getShader(),
                paintParams.getColorFilter(),
                paintParams.getFinalBlender(),
                paintParams.getPrimitiveBlender());
    }

    public <GEO> void drawGeometry(Matrix4c localToDevice,
                                   GEO geometry,
                                   BiConsumer<GEO, Rect2f> boundsFn,
                                   Paint paint,
                                   GeometryRenderer renderer,
                                   @SharedPtr Blender primitiveBlender) {
        Draw draw = new Draw();
        draw.mTransform = localToDevice;
        draw.mGeometry = geometry;
        draw.mRenderer = renderer;

        if (paint.getStyle() == Paint.FILL) {
            draw.mHalfWidth = -1;
        } else {
            draw.mHalfWidth = paint.getStrokeWidth() * 0.5f;
            switch (paint.getStrokeJoin()) {
                case Paint.JOIN_ROUND -> draw.mJoinLimit = -1;
                case Paint.JOIN_BEVEL -> draw.mJoinLimit = 0;
                case Paint.JOIN_MITER -> draw.mJoinLimit = paint.getStrokeMiter();
            }
            draw.mStrokeCap = (byte) paint.getStrokeCap();
            draw.mStrokeAlign = (byte) paint.getStrokeAlign();
        }

        // Calculate the clipped bounds of the draw and determine the clip elements that affect the
        // draw without updating the clip stack.
        final boolean outsetBoundsForAA = renderer.outsetBoundsForAA();
        mElementsForMask.clear();
        boolean clippedOut = mClipStack.prepareForDraw(draw, geometry, boundsFn,
                outsetBoundsForAA, mElementsForMask);
        if (clippedOut) {
            RefCnt.move(primitiveBlender);
            return;
        }

        // A primitive blender should be ignored if there is no primitive color to blend against.
        // Additionally, if a renderer emits a primitive color, then a null primitive blender should
        // be interpreted as SrcOver blending mode.
        if (!renderer.emitsPrimitiveColor()) {
            primitiveBlender = RefCnt.move(primitiveBlender);
        } else if (primitiveBlender == null) {
            primitiveBlender = BlendMode.SRC_OVER;
        }

        draw.mPaintParams = new PaintParams(paint, primitiveBlender); // move

        final int numNewRenderSteps = renderer.numSteps();

        // Decide if we have any reason to flush pending work. We want to flush before updating the clip
        // state or making any permanent changes to a path atlas, since otherwise clip operations and/or
        // atlas entries for the current draw will be flushed.
        final boolean needsFlush = needsFlushBeforeDraw(numNewRenderSteps);
        if (needsFlush) {
            flushPendingWork();
        }

        // Update the clip stack after issuing a flush (if it was needed). A draw will be recorded after
        // this point.
        int drawDepth = mCurrentDepth + 1;
        int clipOrder = mClipStack.updateForDraw(
                draw, mElementsForMask, mBoundsManager, drawDepth);

        // A draw's order always depends on the clips that must be drawn before it
        int paintOrder = clipOrder + 1;
        // If a draw is not opaque, it must be drawn after the most recent draw it intersects with in
        // order to blend correctly.
        if (renderer.emitsCoverage() || paint_depends_on_dst(draw.mPaintParams)) {
            int prevDraw = mBoundsManager.getMostRecentDraw(draw.mDrawBounds);
            paintOrder = Math.max(paintOrder, prevDraw + 1);
        }

        //TODO stencil set

        draw.mDrawOrder = DrawOrder.makeFromDepthAndPaintersOrder(
                drawDepth, paintOrder
        );

        mSDC.recordDraw(draw);

        // Post-draw book keeping (bounds manager, depth tracking, etc.)
        mBoundsManager.recordDraw(draw.mDrawBounds, paintOrder);
        mCurrentDepth = drawDepth;
    }

    public void drawClipShape(Draw draw, boolean inverseFill) {
        //TODO
    }

    private boolean needsFlushBeforeDraw(int numNewRenderSteps) {
        // Must also account for the elements in the clip stack that might need to be recorded.
        numNewRenderSteps += mClipStack.maxDeferredClipDraws() * GeometryRenderer.MAX_RENDER_STEPS;
        // Need flush if we don't have room to record into the current list.
        return (DrawPass.MAX_RENDER_STEPS - mSDC.numPendingSteps()) < numNewRenderSteps;
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
