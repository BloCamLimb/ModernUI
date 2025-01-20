/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2025 BloCamLimb <pocamelards@gmail.com>
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

import java.util.Arrays;

public class NWayCanvas extends NoDrawCanvas {

    protected Canvas[] mList = new Canvas[10];
    protected int mSize = 0;

    public NWayCanvas(int width, int height) {
        super(width, height);
    }

    @Override
    public void close() {
        removeAll();
        super.close();
    }

    public void addCanvas(@RawPtr Canvas canvas) {
        // We are using the nway canvas as a wrapper for the originally added canvas, and the device
        // on the nway may contradict calls for the device on this canvas. So, to add a second
        // canvas, the devices on the first canvas, and the nway base device must be different.
        assert mSize == 0 || mList[0].getRootDevice() != getRootDevice();
        if (canvas != null) {
            if (mSize == mList.length) {
                mList = Arrays.copyOf(mList, mSize + (mSize >> 1));
            }
            mList[mSize++] = canvas;
        }
    }

    public void removeCanvas(@RawPtr Canvas canvas) {
        int index = -1;
        for (int i = 0; i < mSize; i++) {
            if (mList[i] == canvas) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            mList[index] = mList[mSize - 1];
            mList[--mSize] = null;
        }
    }

    public void removeAll() {
        for (int i = 0; i < mSize; i++) {
            mList[i] = null;
        }
        mSize = 0;
    }

    @Override
    protected void willSave() {
        for (int i = 0; i < mSize; i++) {
            mList[i].save();
        }
        super.willSave();
    }

    @Override
    protected void willRestore() {
        for (int i = 0; i < mSize; i++) {
            mList[i].restore();
        }
        super.willRestore();
    }

    @Override
    protected void didTranslate(float dx, float dy, float dz) {
        for (int i = 0; i < mSize; i++) {
            mList[i].translate(dx, dy, dz);
        }
    }

    @Override
    protected void didScale(float sx, float sy, float sz) {
        for (int i = 0; i < mSize; i++) {
            mList[i].scale(sx, sy, sz);
        }
    }

    @Override
    protected void didConcat(Matrix4c matrix) {
        for (int i = 0; i < mSize; i++) {
            mList[i].concat(matrix);
        }
    }

    @Override
    protected void didSetMatrix(Matrix4c matrix) {
        for (int i = 0; i < mSize; i++) {
            mList[i].setMatrix(matrix);
        }
    }

    @Override
    protected void onClipRect(Rect2fc rect, int clipOp, boolean doAA) {
        for (int i = 0; i < mSize; i++) {
            mList[i].clipRect(rect, clipOp, doAA);
        }
        super.onClipRect(rect, clipOp, doAA);
    }

    @Override
    protected void onDrawPaint(Paint paint) {
        for (int i = 0; i < mSize; i++) {
            mList[i].drawPaint(paint);
        }
    }

    @Override
    protected void onDrawPoints(int mode, float[] pts, int offset, int count, Paint paint) {
        for (int i = 0; i < mSize; i++) {
            mList[i].drawPoints(mode, pts, offset, count, paint);
        }
    }

    @Override
    protected void onDrawLine(float x0, float y0, float x1, float y1, int cap, float width, Paint paint) {
        for (int i = 0; i < mSize; i++) {
            mList[i].drawLine(x0, y0, x1, y1, cap, width, paint);
        }
    }

    @Override
    protected void onDrawRect(Rect2fc r, Paint paint) {
        for (int i = 0; i < mSize; i++) {
            mList[i].drawRect(r, paint);
        }
    }

    @Override
    protected void onDrawRRect(RRect rr, Paint paint) {
        for (int i = 0; i < mSize; i++) {
            mList[i].drawRRect(rr, paint);
        }
    }

    @Override
    protected void onDrawCircle(float cx, float cy, float radius, Paint paint) {
        for (int i = 0; i < mSize; i++) {
            mList[i].drawCircle(cx, cy, radius, paint);
        }
    }

    @Override
    protected void onDrawArc(float cx, float cy, float radius, float startAngle, float sweepAngle,
                             int cap, float width, Paint paint) {
        for (int i = 0; i < mSize; i++) {
            mList[i].drawArc(cx, cy, radius, startAngle, sweepAngle, cap, width, paint);
        }
    }

    @Override
    protected void onDrawPie(float cx, float cy, float radius, float startAngle, float sweepAngle, Paint paint) {
        for (int i = 0; i < mSize; i++) {
            mList[i].drawPie(cx, cy, radius, startAngle, sweepAngle, paint);
        }
    }

    @Override
    protected void onDrawChord(float cx, float cy, float radius, float startAngle, float sweepAngle, Paint paint) {
        for (int i = 0; i < mSize; i++) {
            mList[i].drawChord(cx, cy, radius, startAngle, sweepAngle, paint);
        }
    }

    @Override
    protected void onDrawImageRect(@RawPtr Image image, Rect2fc src, Rect2fc dst, SamplingOptions sampling, Paint paint,
                                   int constraint) {
        for (int i = 0; i < mSize; i++) {
            mList[i].drawImageRect(image, src, dst, sampling, paint, constraint);
        }
    }

    @Override
    protected void onDrawTextBlob(TextBlob blob, float originX, float originY, Paint paint) {
        for (int i = 0; i < mSize; i++) {
            mList[i].drawTextBlob(blob, originX, originY, paint);
        }
    }

    @Override
    protected void onDrawGlyphRunList(GlyphRunList glyphRunList, Paint paint) {
        for (int i = 0; i < mSize; i++) {
            mList[i].onDrawGlyphRunList(glyphRunList, paint);
        }
    }

    @Override
    protected void onDrawVertices(Vertices vertices, @SharedPtr Blender blender, Paint paint) {
        for (int i = 0; i < mSize; i++) {
            mList[i].drawVertices(vertices, RefCnt.create(blender), paint);
        }
        RefCnt.move(blender);
    }

    @Override
    protected void onDrawEdgeAAQuad(Rect2fc rect, float[] clip, int edgeFlags, Paint paint) {
        for (int i = 0; i < mSize; i++) {
            mList[i].onDrawEdgeAAQuad(rect, clip, edgeFlags, paint);
        }
    }
}
