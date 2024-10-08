/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.core;

import icyllis.arc3d.engine.RecordingContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class PaintFilterCanvas extends NWayCanvas {

    private final Paint mFilterPaint = new Paint();

    /**
     * The new PaintFilterCanvas is configured for forwarding to the
     * specified canvas.  Also copies the target canvas matrix and clip bounds.
     */
    public PaintFilterCanvas(@RawPtr @Nonnull Canvas canvas) {
        super(canvas.getImageInfo().width(),
                canvas.getImageInfo().height());
        // Transfer matrix & clip state before adding the target canvas.
        Rect2i clipBounds = new Rect2i();
        canvas.getDeviceClipBounds(clipBounds);
        clipRect(clipBounds);
        Matrix4 localToDevice = new Matrix4();
        canvas.getLocalToDevice(localToDevice);
        setMatrix(localToDevice);

        addCanvas(canvas);
    }

    /**
     * Called with the paint that will be used to draw the specified type.
     * The implementation may modify the paint as they wish.
     * <p>
     * The result bool is used to determine whether the draw op is to be
     * executed (true) or skipped (false).
     * <p>
     * Note: The base implementation calls onFilter() for top-level/explicit paints only.
     * To also filter encapsulated paints (e.g. Picture, TextBlob), clients may need to
     * override the relevant methods (i.e. drawPicture, drawTextBlob).
     */
    public abstract boolean onFilter(Paint paint);

    @RawPtr
    private Canvas getProxy() {
        assert mSize == 1;
        return mList[0];
    }

    @Override
    public int getBaseLayerWidth() {
        return getProxy().getBaseLayerWidth();
    }

    @Override
    public int getBaseLayerHeight() {
        return getProxy().getBaseLayerHeight();
    }

    @Nullable
    @Override
    public RecordingContext getRecordingContext() {
        return getProxy().getRecordingContext();
    }

    @Override
    protected void onDrawPaint(Paint paint) {
        mFilterPaint.set(paint);
        if (onFilter(mFilterPaint)) {
            super.onDrawPaint(mFilterPaint);
        }
        mFilterPaint.close();
    }

    @Override
    protected void onDrawPoints(int mode, float[] pts, int offset, int count, Paint paint) {
        mFilterPaint.set(paint);
        if (onFilter(mFilterPaint)) {
            super.onDrawPoints(mode, pts, offset, count, mFilterPaint);
        }
        mFilterPaint.close();
    }

    @Override
    protected void onDrawLine(float x0, float y0, float x1, float y1, int cap, float width, Paint paint) {
        mFilterPaint.set(paint);
        if (onFilter(mFilterPaint)) {
            super.onDrawLine(x0, y0, x1, y1, cap, width, mFilterPaint);
        }
        mFilterPaint.close();
    }

    @Override
    protected void onDrawRect(Rect2fc r, Paint paint) {
        mFilterPaint.set(paint);
        if (onFilter(mFilterPaint)) {
            super.onDrawRect(r, mFilterPaint);
        }
        mFilterPaint.close();
    }

    @Override
    protected void onDrawRoundRect(RoundRect rr, Paint paint) {
        mFilterPaint.set(paint);
        if (onFilter(mFilterPaint)) {
            super.onDrawRoundRect(rr, mFilterPaint);
        }
        mFilterPaint.close();
    }

    @Override
    protected void onDrawCircle(float cx, float cy, float radius, Paint paint) {
        mFilterPaint.set(paint);
        if (onFilter(mFilterPaint)) {
            super.onDrawCircle(cx, cy, radius, mFilterPaint);
        }
        mFilterPaint.close();
    }

    @Override
    protected void onDrawArc(float cx, float cy, float radius, float startAngle, float sweepAngle,
                             int cap, float width, Paint paint) {
        mFilterPaint.set(paint);
        if (onFilter(mFilterPaint)) {
            super.onDrawArc(cx, cy, radius, startAngle, sweepAngle, cap, width, mFilterPaint);
        }
        mFilterPaint.close();
    }

    @Override
    protected void onDrawPie(float cx, float cy, float radius, float startAngle, float sweepAngle, Paint paint) {
        mFilterPaint.set(paint);
        if (onFilter(mFilterPaint)) {
            super.onDrawPie(cx, cy, radius, startAngle, sweepAngle, mFilterPaint);
        }
        mFilterPaint.close();
    }

    @Override
    protected void onDrawChord(float cx, float cy, float radius, float startAngle, float sweepAngle, Paint paint) {
        mFilterPaint.set(paint);
        if (onFilter(mFilterPaint)) {
            super.onDrawChord(cx, cy, radius, startAngle, sweepAngle, mFilterPaint);
        }
        mFilterPaint.close();
    }

    @Override
    protected void onDrawImageRect(@RawPtr Image image, Rect2fc src, Rect2fc dst, SamplingOptions sampling, Paint paint,
                                   int constraint) {
        mFilterPaint.set(paint);
        if (onFilter(mFilterPaint)) {
            super.onDrawImageRect(image, src, dst, sampling, mFilterPaint, constraint);
        }
        mFilterPaint.close();
    }

    @Override
    protected void onDrawTextBlob(TextBlob blob, float originX, float originY, Paint paint) {
        mFilterPaint.set(paint);
        if (onFilter(mFilterPaint)) {
            super.onDrawTextBlob(blob, originX, originY, mFilterPaint);
        }
        mFilterPaint.close();
    }

    @Override
    protected void onDrawGlyphRunList(GlyphRunList glyphRunList, Paint paint) {
        mFilterPaint.set(paint);
        if (onFilter(mFilterPaint)) {
            super.onDrawGlyphRunList(glyphRunList, mFilterPaint);
        }
        mFilterPaint.close();
    }

    @Override
    protected void onDrawVertices(Vertices vertices, @SharedPtr Blender blender, Paint paint) {
        mFilterPaint.set(paint);
        if (onFilter(mFilterPaint)) {
            super.onDrawVertices(vertices, blender, mFilterPaint);
        } else {
            RefCnt.move(blender);
        }
        mFilterPaint.close();
    }

    @Nullable
    @Override
    protected Surface onNewSurface(ImageInfo info) {
        return getProxy().makeSurface(info);
    }

    @Nonnull
    @Override
    protected ImageInfo onGetImageInfo() {
        return getProxy().getImageInfo();
    }
}
