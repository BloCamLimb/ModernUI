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

public class NoDrawCanvas extends Canvas {

    public NoDrawCanvas(int width, int height) {
        super(width, height);
    }

    @Override
    protected void onDrawPaint(Paint paint) {
    }

    @Override
    protected void onDrawPoints(int mode, float[] pts, int offset, int count, Paint paint) {
    }

    @Override
    protected void onDrawLine(float x0, float y0, float x1, float y1, int cap, float width, Paint paint) {
    }

    @Override
    protected void onDrawRect(Rect2fc r, Paint paint) {
    }

    @Override
    protected void onDrawRRect(RRect rr, Paint paint) {
    }

    @Override
    protected void onDrawCircle(float cx, float cy, float radius, Paint paint) {
    }

    @Override
    protected void onDrawArc(float cx, float cy, float radius, float startAngle, float sweepAngle,
                             int cap, float width, Paint paint) {
    }

    @Override
    protected void onDrawPie(float cx, float cy, float radius, float startAngle, float sweepAngle, Paint paint) {
    }

    @Override
    protected void onDrawChord(float cx, float cy, float radius, float startAngle, float sweepAngle, Paint paint) {
    }

    @Override
    protected void onDrawImageRect(@RawPtr Image image, Rect2fc src, Rect2fc dst, SamplingOptions sampling, Paint paint,
                                   int constraint) {
    }

    @Override
    protected void onDrawTextBlob(TextBlob blob, float originX, float originY, Paint paint) {
    }

    @Override
    protected void onDrawGlyphRunList(GlyphRunList glyphRunList, Paint paint) {
    }

    @Override
    protected void onDrawVertices(Vertices vertices, @SharedPtr Blender blender, Paint paint) {
        RefCnt.move(blender);
    }

    @Override
    protected void onDrawEdgeAAQuad(Rect2fc rect, float[] clip, int edgeFlags, Paint paint) {
    }
}
