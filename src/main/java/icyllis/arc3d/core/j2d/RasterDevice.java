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

package icyllis.arc3d.core.j2d;

import icyllis.arc3d.core.Canvas;
import icyllis.arc3d.core.Image;
import icyllis.arc3d.core.Paint;
import icyllis.arc3d.core.*;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;

public class RasterDevice extends Device {

    private Raster mRaster;
    private Graphics2D mG2D;

    public RasterDevice(Raster raster) {
        super(raster.getInfo());
        mRaster = raster;
        mG2D = Objects.requireNonNull(raster.mBufImg).createGraphics();
    }

    @Override
    public boolean isClipAA() {
        return false;
    }

    @Override
    public boolean isClipEmpty() {
        return false;
    }

    @Override
    public boolean isClipRect() {
        return false;
    }

    @Override
    public boolean isClipWideOpen() {
        return false;
    }

    @Override
    public void pushClipStack() {

    }

    @Override
    public void popClipStack() {

    }

    @Override
    public void getClipBounds(@NonNull Rect2i bounds) {

    }

    @Override
    protected Rect2ic getClipBounds() {
        return null;
    }

    @Override
    public void clipRect(Rect2fc rect, int clipOp, boolean doAA) {

    }

    @Override
    public void drawPaint(icyllis.arc3d.core.Paint paint) {

    }

    @Override
    public void drawPoints(int mode, float[] pts, int offset, int count, Paint paint) {

    }

    @Override
    public void drawLine(float x0, float y0, float x1, float y1, int cap, float width, icyllis.arc3d.core.Paint paint) {

    }

    @Override
    public void drawRect(Rect2fc r, icyllis.arc3d.core.Paint paint) {
        mG2D.fill(new Rectangle2D.Float(
                r.x(), r.y(), r.width(), r.height()
        ));
    }

    @Override
    public void drawRoundRect(RoundRect rr, Paint paint) {
        mG2D.fill(new RoundRectangle2D.Float(

        ));
    }

    @Override
    public void drawCircle(float cx, float cy, float radius, icyllis.arc3d.core.Paint paint) {

    }

    @Override
    public void drawArc(float cx, float cy, float radius, float startAngle, float sweepAngle, int cap, float width, Paint paint) {

    }

    @Override
    public void drawPie(float cx, float cy, float radius, float startAngle, float sweepAngle, Paint paint) {

    }

    @Override
    public void drawChord(float cx, float cy, float radius, float startAngle, float sweepAngle, Paint paint) {

    }

    @Override
    public void drawImageRect(@RawPtr Image image, Rect2fc src, Rect2fc dst,
                              SamplingOptions sampling, Paint paint, int constraint) {

    }

    @Override
    protected void onDrawGlyphRunList(Canvas canvas, GlyphRunList glyphRunList, Paint paint) {

    }

    @Override
    public void drawVertices(Vertices vertices, @SharedPtr Blender blender, Paint paint) {
        RefCnt.move(blender);
    }
}
