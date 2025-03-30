/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2025 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.sketch.*;
import icyllis.arc3d.core.*;
import icyllis.arc3d.sketch.Device;
import icyllis.arc3d.sketch.Image;
import icyllis.arc3d.sketch.effects.ColorFilter;
import icyllis.arc3d.sketch.shaders.ImageShader;
import icyllis.arc3d.sketch.shaders.Shader;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.geom.BlurredBox;
import icyllis.arc3d.granite.geom.BoundsManager;
import icyllis.arc3d.granite.geom.EdgeAAQuad;
import icyllis.arc3d.granite.geom.HybridBoundsManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * The device that is backed by GPU.
 */
public final class GraniteDevice extends Device {

    // raw pointer
    private RecordingContext mRC;
    // unique ref
    private SurfaceDrawContext mSDC;

    private final ClipStack mClipStack;
    private final ObjectArrayList<ClipStack.Element> mElementsForMask = new ObjectArrayList<>();

    // The max depth value sent to the DrawContext, incremented so each draw has a unique value.
    private int mCurrentDepth = DrawOrder.CLEAR_DEPTH;

    // Tracks accumulated intersections for ordering dependent use of the color and depth attachment
    // (i.e. depth-based clipping, and transparent blending)
    private final BoundsManager mColorDepthBoundsManager;

    private final Paint mSubRunPaint = new Paint();
    private final TextBlobCache.FeatureKey mBlobKey = new TextBlobCache.FeatureKey();

    private GraniteDevice(RecordingContext rc, SurfaceDrawContext sdc) {
        super(sdc.getImageInfo());
        mRC = rc;
        mSDC = sdc;
        mClipStack = new ClipStack(this);
        // These default tuning numbers for the HybridBoundsManager were chosen from looking at performance
        // and accuracy curves produced by the BoundsManagerBench for random draw bounding boxes. This
        // config will use brute force for the first 64 draw calls to the Device and then switch to a grid
        // that is dynamically sized to produce cells that are 16x16, up to a grid that's 32x32 cells.
        // This seemed like a sweet spot balancing accuracy for low-draw count surfaces and overhead for
        // high-draw count and high-resolution surfaces. With the 32x32 grid limit, cell size will increase
        // above 16px when the surface dimension goes above 512px.
        // TODO: These could be exposed as context options or surface options, and we may want to have
        // different strategies in place for a base device vs. a layer's device.
        mColorDepthBoundsManager = new HybridBoundsManager(
                getWidth(), getHeight(),
                16, 64, 32
        );
        //mColorDepthBoundsManager = new SimpleBoundsManager();
    }

    @Nullable
    @SharedPtr
    public static GraniteDevice make(@RawPtr RecordingContext rc,
                                     @NonNull ImageInfo deviceInfo,
                                     int surfaceFlags,
                                     int origin,
                                     byte initialLoadOp,
                                     String label,
                                     boolean trackDevice) {
        if (rc == null) {
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

        ImageDesc desc = rc.getCaps().getDefaultColorImageDesc(
                Engine.ImageType.k2D,
                deviceInfo.colorType(),
                backingWidth,
                backingHeight,
                1,
                surfaceFlags | ISurface.FLAG_SAMPLED_IMAGE | ISurface.FLAG_RENDERABLE);
        if (desc == null) {
            return null;
        }
        short readSwizzle = rc.getCaps().getReadSwizzle(
                desc, deviceInfo.colorType());
        @SharedPtr
        ImageViewProxy targetView = ImageViewProxy.make(rc, desc,
                origin, readSwizzle,
                (surfaceFlags & ISurface.FLAG_BUDGETED) != 0, label);
        if (targetView == null) {
            return null;
        }

        return make(rc, targetView, deviceInfo, initialLoadOp, trackDevice);
    }

    @Nullable
    @SharedPtr
    public static GraniteDevice make(@RawPtr RecordingContext rc,
                                     @SharedPtr ImageViewProxy targetView,
                                     ImageInfo deviceInfo,
                                     byte initialLoadOp,
                                     boolean trackDevice) {
        if (rc == null) {
            return null;
        }
        SurfaceDrawContext sdc = SurfaceDrawContext.make(rc,
                targetView, deviceInfo);
        if (sdc == null) {
            return null;
        }
        if (initialLoadOp == Engine.LoadOp.kClear) {
            sdc.clear(null);
        } else if (initialLoadOp == Engine.LoadOp.kDiscard) {
            sdc.discard();
        }

        @SharedPtr
        GraniteDevice device = new GraniteDevice(rc, sdc);
        if (trackDevice) {
            rc.trackDevice(RefCnt.create(device));
        }
        return device;
    }

    @Override
    protected void deallocate() {
        super.deallocate();
        mSDC.close();
        mSDC = null;
        assert mRC == null;
    }

    public void setImmutable() {
        if (mRC != null) {
            // Push any pending work to the RC now. setImmutable() is only called by the
            // destructor of a client-owned Surface, or explicitly in layer/filtering workflows. In
            // both cases this is restricted to the RC's thread. This is in contrast to deallocate(),
            // which might be called from another thread if it was linked to an Image used in multiple
            // recorders.
            flushPendingWork();
            mRC.untrackDevice(this);
            // Discarding the RC ensures that there are no further operations that can be recorded
            // and is relied on by Image::notifyInUse() to detect when it can unlink from a Device.
            discardRC();
        }
    }

    public void discardRC() {
        mRC = null;
    }

    @NonNull
    @Override
    public RecordingContext getRecordingContext() {
        assert mRC != null;
        return mRC;
    }

    /**
     * @return raw ptr to the read view
     */
    @RawPtr
    public ImageViewProxy getReadView() {
        return mSDC.getReadView();
    }

    @Nullable
    @SharedPtr
    public GraniteImage makeImageCopy(@NonNull Rect2ic subset,
                                      boolean budgeted,
                                      boolean mipmapped,
                                      boolean approxFit) {
        assert mRC.isOwnerThread();
        flushPendingWork();

        var srcInfo = getImageInfo();
        @RawPtr
        var srcView = mSDC.getReadView();
        String label = srcView.getLabel();
        if (label == null || label.isEmpty()) {
            label = "CopyDeviceTexture";
        } else {
            label += "_DeviceCopy";
        }

        return GraniteImage.copy(
                mRC, srcView, srcInfo, subset,
                budgeted, mipmapped, approxFit, label
        );
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
        mClipStack.clipRect(getLocalToDevice33(), rect, clipOp);
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
    public void getClipBounds(@NonNull Rect2i bounds) {
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
        if (isClipWideOpen() && !paint_depends_on_dst(paint)) {
            float[] color = new float[4];
            if (PaintParams.getSolidColor(paint, getImageInfo(), color)) {
                // do fullscreen clear
                mSDC.clear(color);
                return;
            } else {
                // This paint does not depend on the destination and covers the entire surface, so
                // discard everything previously recorded and proceed with the draw.
                mSDC.discard();
            }
        }

        // An empty shape with an inverse fill completely floods the clip
        drawGeometry(getLocalToDevice33(), null, (__, dest) -> dest.setEmpty(),
                true, paint, mRC.getRendererProvider().getNonAABoundsFill(), null);
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
                    drawEllipse(pts[i], pts[i + 1], radius, radius, paint);
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
        drawGeometry(getLocalToDevice33(), shape, SimpleShape::getBounds, false, paint,
                mRC.getRendererProvider().getSimpleBox(false), null);
    }

    @Override
    public void drawRect(Rect2fc r, Paint paint) {
        if (paint.getStyle() == Paint.FILL) {
            if (paint.isAntiAlias()) {
                drawGeometry(getLocalToDevice33(),
                        new EdgeAAQuad(r, EdgeAAQuad.kAll),
                        EdgeAAQuad::getBounds, false, paint,
                        mRC.getRendererProvider().getPerEdgeAAQuad(), null);
            } else {
                drawGeometry(getLocalToDevice33(),
                        new Rect2f(r), Rect2f::store, false, paint,
                        mRC.getRendererProvider().getNonAABoundsFill(), null);
            }
        } else {
            var join = paint.getStrokeJoin();
            boolean complex = join == Paint.JOIN_BEVEL ||
                    (join == Paint.JOIN_MITER && paint.getStrokeMiter() < MathUtil.SQRT2);
            GeometryRenderer renderer = complex
                    ? mRC.getRendererProvider().getComplexBox()
                    : mRC.getRendererProvider().getSimpleBox(false);
            drawGeometry(getLocalToDevice33(), new SimpleShape(r), SimpleShape::getBounds, false, paint,
                    renderer, null);
        }
    }

    @Override
    public void drawRRect(RRect rr, Paint paint) {
        //TODO stroking an ellipse requires new renderer
        boolean complex = switch (rr.getType()) {
            case RRect.kOval_Type, RRect.kSimple_Type -> rr.getSimpleRadiusX() != rr.getSimpleRadiusY();
            case RRect.kNineSlice_Type, RRect.kComplex_Type -> true;
            default -> {
                // empty and rect are handled by Canvas
                assert false;
                yield false;
            }
        };
        GeometryRenderer renderer = complex
                ? mRC.getRendererProvider().getComplexBox()
                : mRC.getRendererProvider().getSimpleBox(false);
        drawGeometry(getLocalToDevice33(), new SimpleShape(rr), SimpleShape::getBounds, false, paint,
                renderer, null);
    }

    @Override
    public void drawEllipse(float cx, float cy, float rx, float ry, Paint paint) {
        var shape = new SimpleShape();
        shape.setEllipse(cx, cy, rx, ry);
        GeometryRenderer renderer = rx != ry
                ? mRC.getRendererProvider().getComplexBox()
                : mRC.getRendererProvider().getSimpleBox(false);
        drawGeometry(getLocalToDevice33(), shape, SimpleShape::getBounds, false, paint,
                renderer, null);
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
        drawGeometry(getLocalToDevice33(), shape, ArcShape::getBounds, false, paint,
                mRC.getRendererProvider().getArc(shape.mType), null);
    }

    @Override
    public void drawPie(float cx, float cy, float radius, float startAngle,
                        float sweepAngle, Paint paint) {
        var shape = new ArcShape(cx, cy, radius, startAngle, sweepAngle, 0);
        shape.mType = ArcShape.kPie_Type;
        drawGeometry(getLocalToDevice33(), shape, ArcShape::getBounds, false, paint,
                mRC.getRendererProvider().getArc(shape.mType), null);
    }

    @Override
    public void drawChord(float cx, float cy, float radius, float startAngle,
                          float sweepAngle, Paint paint) {
        var shape = new ArcShape(cx, cy, radius, startAngle, sweepAngle, 0);
        shape.mType = ArcShape.kChord_Type;
        drawGeometry(getLocalToDevice33(), shape, ArcShape::getBounds, false, paint,
                mRC.getRendererProvider().getArc(shape.mType), null);
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

        if (glyphRunList.mOriginalTextBlob != null) {
            // use cache if it comes from TextBlob
            var blobCache = mRC.getTextBlobCache();
            mBlobKey.update(glyphRunList, paint, positionMatrix);
            var entry = blobCache.find(glyphRunList.mOriginalTextBlob, mBlobKey);
            if (entry == null || !entry.canReuse(paint, positionMatrix,
                    glyphRunList.getSourceBounds().centerX(),
                    glyphRunList.getSourceBounds().centerY())) {
                if (entry != null) {
                    // We have to remake the blob because changes may invalidate our masks.
                    blobCache.remove(entry);
                }
                entry = BakedTextBlob.make(
                        glyphRunList,
                        paint,
                        positionMatrix,
                        StrikeCache.getGlobalStrikeCache()
                );
                entry = blobCache.insert(glyphRunList.mOriginalTextBlob, mBlobKey,
                        entry);
            }
            entry.draw(canvas, glyphRunList.mOriginX, glyphRunList.mOriginY, paint, this);
        } else {
            SubRunContainer container = SubRunContainer.make(
                    glyphRunList,
                    positionMatrix,
                    paint,
                    StrikeCache.getGlobalStrikeCache()
            );
            container.draw(canvas, glyphRunList.mOriginX, glyphRunList.mOriginY, paint, this);
        }
    }

    @Override
    public void drawVertices(Vertices vertices, Blender blender, Paint paint) {
        drawGeometry(getLocalToDevice33(), vertices, Vertices::getBounds, false, paint,
                mRC.getRendererProvider().getVertices(
                        vertices.getVertexMode(), vertices.hasColors(), vertices.hasTexCoords()),
                blender); // move
    }

    @Override
    public void drawEdgeAAQuad(Rect2fc r, float[] clip, int flags, Paint paint) {
        EdgeAAQuad quad = clip != null ? new EdgeAAQuad(clip, flags) : new EdgeAAQuad(r, flags);
        drawGeometry(getLocalToDevice33(),
                quad,
                EdgeAAQuad::getBounds, false, paint,
                mRC.getRendererProvider().getPerEdgeAAQuad(), null);
    }

    @Override
    public boolean drawBlurredRRect(RRect rr, Paint paint, float blurRadius, float noiseAlpha) {
        if (!Float.isFinite(blurRadius)) {
            return true;
        }
        //TODO compute device blur radius
        if (blurRadius < 0.1f) {
            drawRRect(rr, paint);
            return true;
        }
        if (!(noiseAlpha >= 0f)) {
            noiseAlpha = 0f;
        }
        float minDim = Math.min(rr.width(), rr.height());
        BlurredBox shape = new BlurredBox(rr);
        // we found that multiplying the radius by 1.25 is closest to a Gaussian blur with a sigma of radius/3
        blurRadius *= 1.25f;
        float radius = rr.getSimpleRadiusX();
        // the closer to a rectangle, the larger the corner radius needs to be
        float t = Math.min(radius / Math.min(minDim, blurRadius), 1.0f);
        radius += MathUtil.lerp(0.36f, 0.09f, t) * blurRadius;
        shape.mRadius = radius;
        shape.mBlurRadius = blurRadius;
        shape.mNoiseAlpha = noiseAlpha;
        drawGeometry(getLocalToDevice33(), shape, BlurredBox::getBounds, false, paint,
                mRC.getRendererProvider().getSimpleBox(true), null);
        return true;
    }

    public void drawAtlasSubRun(SubRunContainer.AtlasSubRun subRun,
                                float originX, float originY,
                                Paint paint) {
        int maskFormat = subRun.getMaskFormat();
        if (!mRC.getAtlasProvider().getGlyphAtlasManager().initAtlas(
                maskFormat
        )) {
            return;
        }

        int subRunEnd = subRun.getGlyphCount();
        Paint subRunPaint = mSubRunPaint;
        boolean flushed = false;
        for (int subRunCursor = 0; subRunCursor < subRunEnd; ) {
            int glyphsPrepared = subRun.prepareGlyphs(subRunCursor, subRunEnd,
                    mRC);
            if (glyphsPrepared < 0) {
                // There was a problem allocating the glyph in the atlas.
                break;
            }
            if (glyphsPrepared > 0) {
                // subRunToDevice is our "localToDevice",
                // as sub run's coordinates are returned in sub run's space
                Matrix subRunToLocal = new Matrix();
                Matrix subRunToDevice = new Matrix();
                int filter = subRun.getMatrixAndFilter(
                        getLocalToDevice33(),
                        originX, originY,
                        subRunToLocal,
                        subRunToDevice);
                SubRunData subRunData = new SubRunData(subRun,
                        subRunToLocal, filter,
                        subRunCursor, glyphsPrepared);

                subRunPaint.set(paint);
                if (subRun.getMaskFormat() == Engine.MASK_FORMAT_ARGB) {
                    subRunPaint.setShader(null);
                }
                subRunPaint.setStyle(Paint.FILL);

                drawGeometry(subRunToDevice, subRunData, SubRunData::getBounds, false, paint,
                        mRC.getRendererProvider().getRasterText(maskFormat),
                        BlendMode.DST_IN);
            } else if (flushed) {
                // Treat as an error.
                break;
            }
            subRunCursor += glyphsPrepared;

            if (subRunCursor < subRunEnd) {
                // Flush if not all the glyphs are handled because the atlas is out of space.
                // We flush every Device because the glyphs that are being flushed/referenced are not
                // necessarily specific to this Device. This addresses both multiple Surfaces within
                // a Recorder, and nested layers.
                mRC.flushTrackedDevices();
                flushed = true;
            }
        }
        subRunPaint.reset();
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

    private static boolean paint_depends_on_dst(Paint paint) {
        return paint_depends_on_dst(paint.a(),
                paint.getShader(),
                paint.getColorFilter(),
                paint.getBlender(),
                null);
    }

    public <GEO> void drawGeometry(Matrixc localToDevice,
                                   GEO geometry,
                                   BiConsumer<GEO, Rect2f> boundsFn,
                                   boolean inverseFill,
                                   Paint paint,
                                   GeometryRenderer renderer,
                                   Blender primitiveBlender) {
        Draw draw = new Draw(localToDevice, geometry);
        draw.mInverseFill = inverseFill;
        draw.mRenderer = renderer;

        if (paint.getStyle() != Paint.FILL) {
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
            return;
        }

        // A primitive blender should be ignored if there is no primitive color to blend against.
        // Additionally, if a renderer emits a primitive color, then a null primitive blender should
        // be interpreted as SrcOver blending mode.
        if (!renderer.emitsPrimitiveColor()) {
            primitiveBlender = null;
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
                draw, mElementsForMask, mColorDepthBoundsManager, drawDepth);

        // A draw's order always depends on the clips that must be drawn before it
        int paintOrder = clipOrder + 1;
        // If a draw is not opaque, it must be drawn after the most recent draw it intersects with in
        // order to blend correctly.
        if (renderer.emitsCoverage() || paint_depends_on_dst(draw.mPaintParams)) {
            int prevDraw = mColorDepthBoundsManager.getMostRecentDraw(draw.mDrawBounds);
            paintOrder = Math.max(paintOrder, prevDraw + 1);
        }

        //TODO stencil set

        draw.mDrawOrder = DrawOrder.makeFromDepthAndPaintersOrder(
                drawDepth, paintOrder
        );

        mSDC.recordDraw(draw);

        // Post-draw book keeping (bounds manager, depth tracking, etc.)
        mColorDepthBoundsManager.recordDraw(draw.mDrawBounds, paintOrder);
        mCurrentDepth = drawDepth;
    }

    public void drawClipShape(Draw draw) {
        if (draw.mInverseFill || !(draw.mGeometry instanceof Rect2f)) {
            //TODO not implement tessellation yet
            return;
        }
        // difference clip => non-inverse-fill, draw rect
        draw.mRenderer = mRC.getRendererProvider().getNonAABoundsFill();

        assert mSDC.numPendingSteps() + draw.mRenderer.numSteps() < DrawPass.MAX_RENDER_STEPS;

        assert !draw.mRenderer.emitsCoverage();

        mSDC.recordDraw(draw);

        int depth = draw.getDepth();
        if (depth > mCurrentDepth) {
            mCurrentDepth = depth;
        }
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
     * {@link GraniteDevice}'s {@link RecordingContext}.
     */
    public void flushPendingWork() {
        assert mRC.isOwnerThread();
        // Push any pending uploads from the atlas provider that pending draws reference.
        mRC.getAtlasProvider().recordUploads(mSDC);

        // Clip shapes are depth-only draws, but aren't recorded in the DrawContext until a flush in
        // order to determine the Z values for each element.
        mClipStack.recordDeferredClipDraws();

        // Flush all pending items to the internal task list and reset Device tracking state
        mSDC.flush(mRC);

        mColorDepthBoundsManager.clear();
        mCurrentDepth = DrawOrder.CLEAR_DEPTH;

        // Any cleanup in the AtlasProvider
        mRC.getAtlasProvider().compact();

        DrawTask drawTask = mSDC.snapDrawTask(mRC);

        if (drawTask != null) {
            mRC.addTask(drawTask);
        }
    }
}
