/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import icyllis.arc3d.core.*;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.text.Font;
import org.jetbrains.annotations.ApiStatus;

import java.nio.*;

/**
 * Canvas powered by Arc3D Canvas.
 *
 * @hidden
 */
@ApiStatus.Internal
public class ArcCanvas extends Canvas {

    @RawPtr
    private icyllis.arc3d.core.Canvas mCanvas;

    public ArcCanvas(@RawPtr icyllis.arc3d.core.Canvas canvas) {
        mCanvas = canvas;
    }

    @Override
    public int save() {
        return mCanvas.save();
    }

    @Override
    public int saveLayer(float left, float top, float right, float bottom, int alpha) {
        //TODO not supported yet
        return save();
    }

    @Override
    public void restore() {
        mCanvas.restore();
    }

    @Override
    public int getSaveCount() {
        return mCanvas.getSaveCount();
    }

    @Override
    public void restoreToCount(int saveCount) {
        mCanvas.restoreToCount(saveCount);
    }

    @Override
    public void translate(float dx, float dy) {
        mCanvas.translate(dx, dy);
    }

    @Override
    public void translate(float dx, float dy, float dz) {
        mCanvas.translate(dx, dy, dz);
    }

    @Override
    public void scale(float sx, float sy) {
        mCanvas.scale(sx, sy);
    }

    @Override
    public void scale(float sx, float sy, float sz) {
        mCanvas.scale(sx, sy, sz);
    }

    @Override
    public void scale(float sx, float sy, float px, float py) {
        mCanvas.scale(sx, sy, px, py);
    }

    @Override
    public void rotate(float degrees) {
        mCanvas.rotate(degrees);
    }

    @Override
    public void rotate(float degrees, float px, float py) {
        mCanvas.rotate(degrees, px, py);
    }

    @Override
    public void shear(float sx, float sy) {
        mCanvas.shear(sx, sy);
    }

    @Override
    public void shear(float sx, float sy, float px, float py) {
        mCanvas.shear(sx, sy, px, py);
    }

    @Override
    public void concat(@NonNull Matrix matrix) {
        mCanvas.concat(matrix);
    }

    @Override
    public void concat(@NonNull Matrix4 matrix) {
        mCanvas.concat(matrix);
    }

    @NonNull
    @Override
    public Matrix4 getMatrix() {
        Matrix4 mat = new Matrix4();
        mCanvas.getLocalToDevice(mat);
        return mat;
    }

    @Override
    public boolean clipRect(float left, float top, float right, float bottom) {
        mCanvas.clipRect(left, top, right, bottom, ClipOp.CLIP_OP_INTERSECT);
        return !mCanvas.isClipEmpty();
    }

    @Override
    public boolean clipOutRect(float left, float top, float right, float bottom) {
        mCanvas.clipRect(left, top, right, bottom, ClipOp.CLIP_OP_DIFFERENCE);
        return !mCanvas.isClipEmpty();
    }

    @Override
    public boolean quickReject(float left, float top, float right, float bottom) {
        return mCanvas.quickReject(left, top, right, bottom);
    }

    @Override
    public void drawColor(int color, BlendMode mode) {
        mCanvas.drawColor(color, mode.getNativeBlendMode());
    }

    @Override
    public void drawPaint(Paint paint) {
        mCanvas.drawPaint(paint.getNativePaint());
    }

    @Override
    public void drawLine(float x0, float y0, float x1, float y1,
                         @NonNull Paint.Cap cap, float thickness, @NonNull Paint paint) {
        mCanvas.drawLine(x0, y0, x1, y1, cap.nativeInt, thickness, paint.getNativePaint());
    }

    @Override
    public void drawLine(float x0, float y0, float x1, float y1, Paint paint) {
        mCanvas.drawLine(x0, y0, x1, y1, paint.getNativePaint());
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom, Paint paint) {
        mCanvas.drawRect(left, top, right, bottom, paint.getNativePaint());
    }

    @Override
    public void drawRectGradient(float left, float top, float right, float bottom, int colorUL, int colorUR,
                                 int colorLR, int colorLL, Paint paint) {
        //TODO bilinear gradient not supported yet
        mCanvas.drawRect(left, top, right, bottom, paint.getNativePaint());
    }

    @Override
    public void drawRoundRect(float left, float top, float right, float bottom, float radius, int sides, Paint paint) {
        //TODO per-corner radius not supported yet
        mCanvas.drawRoundRect(left, top, right, bottom, radius, paint.getNativePaint());
    }

    @Override
    public void drawRoundRectGradient(float left, float top, float right, float bottom,
                                      int colorUL, int colorUR, int colorLR, int colorLL, float radius, Paint paint) {
        //TODO per-corner radius not supported yet
        mCanvas.drawRoundRect(left, top, right, bottom, radius, paint.getNativePaint());
    }

    @Override
    public void drawCircle(float cx, float cy, float radius, Paint paint) {
        mCanvas.drawCircle(cx, cy, radius, paint.getNativePaint());
    }

    @Override
    public void drawArc(float cx, float cy, float radius, float startAngle, float sweepAngle, Paint paint) {
        mCanvas.drawArc(cx, cy, radius, startAngle, sweepAngle, paint.getNativePaint());
    }

    @Override
    public void drawPie(float cx, float cy, float radius, float startAngle, float sweepAngle, Paint paint) {
        mCanvas.drawPie(cx, cy, radius, startAngle, sweepAngle, paint.getNativePaint());
    }

    @Override
    public void drawBezier(float x0, float y0, float x1, float y1, float x2, float y2, Paint paint) {
        //TODO not supported yet
    }

    @Override
    public void drawImage(Image image, float left, float top, @Nullable Paint paint) {
        if (image == null) {
            return;
        }
        mCanvas.drawImage(image.getNativeImage(), left, top,
                paint != null && paint.getFilterMode() == ImageShader.FILTER_MODE_NEAREST
                        ? SamplingOptions.POINT
                        : SamplingOptions.LINEAR,
                paint != null ? paint.getNativePaint() : null);
    }

    @Override
    public void drawImage(Image image, float srcLeft, float srcTop, float srcRight, float srcBottom,
                          float dstLeft, float dstTop, float dstRight, float dstBottom, @Nullable Paint paint) {
        if (image == null) {
            return;
        }
        mCanvas.drawImageRect(image.getNativeImage(),
                new Rect2f(srcLeft, srcTop, srcRight, srcBottom),
                new Rect2f(dstLeft, dstTop, dstRight, dstBottom),
                paint != null && paint.getFilterMode() == ImageShader.FILTER_MODE_NEAREST
                        ? SamplingOptions.POINT
                        : SamplingOptions.LINEAR,
                paint != null ? paint.getNativePaint() : null,
                icyllis.arc3d.core.Canvas.SRC_RECT_CONSTRAINT_FAST);
    }

    @Override
    public void drawRoundImage(Image image, float left, float top, float radius, Paint paint) {

    }

    @Override
    public void drawGlyphs(@NonNull int[] glyphs, int glyphOffset,
                           @NonNull float[] positions, int positionOffset,
                           int glyphCount, @NonNull Font font,
                           float x, float y, @NonNull Paint paint) {
        icyllis.arc3d.core.Font nativeFont = new icyllis.arc3d.core.Font();
        nativeFont.setTypeface(font.getNativeTypeface());
        nativeFont.setSize(paint.getFontSize());
        nativeFont.setEdging(paint.isTextAntiAlias()
                ? icyllis.arc3d.core.Font.kAntiAlias_Edging
                : icyllis.arc3d.core.Font.kAlias_Edging);
        nativeFont.setLinearMetrics(paint.isLinearText());
        if (nativeFont.getTypeface() == null) {
            return;
        }
        mCanvas.drawGlyphs(glyphs, glyphOffset,
                positions, positionOffset,
                glyphCount, x, y, nativeFont, paint.getNativePaint());
    }

    @Override
    public void drawMesh(@NonNull VertexMode mode, @NonNull FloatBuffer pos,
                         @Nullable IntBuffer color, @Nullable FloatBuffer tex,
                         @Nullable ShortBuffer indices, @Nullable Blender blender,
                         @NonNull Paint paint) {
        Vertices vertices = Vertices.makeCopy(mode.nativeInt, pos, tex, color, indices);
        icyllis.arc3d.core.Paint nativePaint = paint.getNativePaint();
        if (blender == null) {
            blender = nativePaint.getShader() != null
                    ? icyllis.arc3d.core.BlendMode.MODULATE
                    : icyllis.arc3d.core.BlendMode.DST;
        }
        mCanvas.drawVertices(vertices, blender, nativePaint);
    }
}
