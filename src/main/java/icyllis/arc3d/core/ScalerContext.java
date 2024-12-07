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

import icyllis.arc3d.core.j2d.DrawBase;
import org.checkerframework.checker.nullness.qual.NonNull;
import sun.misc.Unsafe;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * The ScalerContext controls the rasterization with a specified typeface
 * and rasterization options.
 * <p>
 * This class is not thread safe, even it is stateless.
 */
public abstract class ScalerContext {

    protected final StrikeDesc mDesc;

    private final Typeface mTypeface;

    // if this is set, we draw the image from a path, rather than
    // calling generateImage.
    private final boolean mGenerateImageFromPath;

    public ScalerContext(Typeface typeface,
                         StrikeDesc desc) {
        mDesc = new StrikeDesc(desc);
        // Allow the typeface to adjust the rec.
        typeface.onFilterStrikeDesc(mDesc);

        mTypeface = typeface;
        mGenerateImageFromPath = mDesc.getFrameWidth() >= 0;
    }

    public final Typeface getTypeface() {
        return mTypeface;
    }

    // Mask::Format
    public final byte getMaskFormat() {
        return mDesc.getMaskFormat();
    }

    public final boolean isLinearMetrics() {
        return (mDesc.getFlags() & StrikeDesc.kLinearMetrics_Flag) != 0;
    }

    private static void saturate_glyph_bounds(
            Glyph glyph, float left, float top, float right, float bottom) {
        // round out to get grid-fitted bounds
        int l = (int) Math.floor(left);
        int t = (int) Math.floor(top);
        int r = (int) Math.ceil(right);
        int b = (int) Math.ceil(bottom);
        // saturate cast
        glyph.mLeft = (short) MathUtil.clamp(l, Short.MIN_VALUE, Short.MAX_VALUE);
        glyph.mTop = (short) MathUtil.clamp(t, Short.MIN_VALUE, Short.MAX_VALUE);
        glyph.mWidth = (short) MathUtil.clamp(r - l, 0, 0xFFFF);
        glyph.mHeight = (short) MathUtil.clamp(b - t, 0, 0xFFFF);
    }

    @NonNull
    public final Glyph makeGlyph(int glyphID) {
        Glyph glyph = new Glyph(glyphID);
        // subclass may return a different value
        glyph.mMaskFormat = getMaskFormat();
        GlyphMetrics metrics = generateMetrics(glyph);
        assert !metrics.mNeverRequestPath || !metrics.mComputeFromPath;

        glyph.mMaskFormat = metrics.mMaskFormat;

        if (metrics.mComputeFromPath || (mGenerateImageFromPath && !metrics.mNeverRequestPath)) {
            internalGetPath(glyph);
            Path devPath = glyph.getPath();
            if (devPath != null) {
                // other formats can not be produced from paths.
                if (glyph.mMaskFormat != Mask.kBW_Format &&
                        glyph.mMaskFormat != Mask.kA8_Format) {
                    glyph.mMaskFormat = Mask.kA8_Format;
                }

                var bounds = devPath.getBounds();
                saturate_glyph_bounds(glyph,
                        bounds.left(), bounds.top(),
                        bounds.right(), bounds.bottom());
            }
        } else {
            saturate_glyph_bounds(glyph,
                    metrics.mLeft, metrics.mTop,
                    metrics.mRight, metrics.mBottom);
            if (metrics.mNeverRequestPath) {
                glyph.setPath((Path) null);
            }
        }

        // if either dimension is empty, zap the image bounds of the glyph
        if (glyph.mWidth == 0 || glyph.mHeight == 0) {
            glyph.mLeft = glyph.mTop = glyph.mWidth = glyph.mHeight = 0;
        }

        return glyph;
    }

    public final void getImage(Glyph glyph) {
        if (!mGenerateImageFromPath) {
            generateImage(glyph, glyph.getImageBase(), glyph.getImageAddress());
        } else {
            assert glyph.setPathHasBeenCalled();
            Path devPath = glyph.getPath();
            if (devPath == null) {
                generateImage(glyph, glyph.getImageBase(), glyph.getImageAddress());
            } else {
                assert glyph.getMaskFormat() != Mask.kARGB32_Format;
                generateImageFromPath(glyph, devPath);
            }
        }
    }

    private static void generateImageFromPath(
            Glyph glyph, Path path) {
        assert glyph.getMaskFormat() == Mask.kBW_Format ||
                glyph.getMaskFormat() == Mask.kA8_Format;

        Matrix matrix = Matrix.makeTranslate(-glyph.getLeft(), -glyph.getTop());

        Paint paint = new Paint();
        paint.setStyle(Paint.FILL);
        paint.setAntiAlias(glyph.getMaskFormat() != Mask.kBW_Format);
        paint.setBlendMode(BlendMode.SRC);

        BufferedImage bufferedImage = new BufferedImage(glyph.getWidth(), glyph.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);

        DrawBase draw = new DrawBase();
        draw.mG2D = bufferedImage.createGraphics();
        draw.mCTM = matrix;

        draw.drawPath(path, paint);

        var data = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();

        switch (glyph.getMaskFormat()) {
            case Mask.kBW_Format -> {
                PixelUtils.packA8ToBW(
                        data,
                        Unsafe.ARRAY_BYTE_BASE_OFFSET,
                        glyph.getWidth(),
                        glyph.getImageBase(),
                        glyph.getImageAddress(),
                        glyph.getRowBytes(),
                        glyph.getWidth(),
                        glyph.getHeight()
                );
            }
            case Mask.kA8_Format -> {
                PixelUtils.copyImage(
                        data,
                        Unsafe.ARRAY_BYTE_BASE_OFFSET,
                        glyph.getWidth(),
                        glyph.getImageBase(),
                        glyph.getImageAddress(),
                        glyph.getRowBytes(),
                        glyph.getRowBytes(),
                        glyph.getHeight()
                );
            }
        }

        paint.close();
    }

    public final void getPath(@NonNull Glyph glyph) {
        internalGetPath(glyph);
    }

    private void internalGetPath(@NonNull Glyph glyph) {
        if (glyph.setPathHasBeenCalled()) {
            return;
        }

        Path path = new Path();
        if (!generatePath(glyph, path)) {
            glyph.setPath((Path) null);
            return;
        }

        if (mDesc.getFrameWidth() >= 0 || mDesc.getPathEffect() != null) {
            // need the path in user-space, with only the point-size applied
            // so that our stroking and effects will operate the same way they
            // would if the user had extracted the path themself, and then
            // called drawPath
            Matrix matrix = new Matrix();
            Matrix inverse = new Matrix();

            mDesc.getDeviceMatrix(matrix);
            if (!matrix.invert(inverse)) {
                glyph.setPath(new Path());
                return;
            }
            Path localPath = new Path();
            path.transform(inverse, localPath);
            // now localPath is only affected by the paint settings, and not the canvas matrix

            StrokeRec strokeRec = new StrokeRec();

            if (mDesc.getFrameWidth() >= 0) {
                strokeRec.setStrokeStyle(mDesc.getFrameWidth(),
                        (mDesc.getFlags() & StrikeDesc.kFrameAndFill_Flag) != 0);
                // glyphs are always closed contours, so cap type is ignored,
                // so we just pass something.
                strokeRec.setStrokeParams(Paint.CAP_BUTT,
                        mDesc.getStrokeJoin(),
                        Paint.ALIGN_CENTER,
                        mDesc.getMiterLimit());
            }

            if (mDesc.getPathEffect() != null) {
                //TODO
            }

            path.reset();
            if (strokeRec.applyToPath(localPath, path)) {
                // set to stroke path
                localPath.set(path);
            }

            // transform into device space
            localPath.transform(matrix, path);
            localPath.recycle();
        }
        path.trimToSize();
        glyph.setPath(path);
        path.recycle();
    }

    public static class GlyphMetrics {
        // CBox (box of all control points, approximate) or
        // BBox (exact bonding box, compute extrema) of the glyph
        public float mLeft;
        public float mTop;
        public float mRight;
        public float mBottom;

        public byte mMaskFormat;

        public boolean mNeverRequestPath;
        public boolean mComputeFromPath;

        public GlyphMetrics(byte maskFormat) {
            mMaskFormat = maskFormat;
        }
    }

    protected abstract GlyphMetrics generateMetrics(Glyph glyph);

    /**
     * Generates the contents of glyph.fImage.
     * When called, glyph.fImage will be pointing to a pre-allocated,
     * uninitialized region of memory of size glyph.imageSize().
     * This method may not change glyph.fMaskFormat.
     * <p>
     * Because glyph.imageSize() will determine the size of fImage,
     * generateMetrics will be called before generateImage.
     */
    protected abstract void generateImage(Glyph glyph, Object imageBase, long imageAddress);

    /**
     * Sets the passed path to the glyph outline.
     * If this cannot be done the path is set to empty;
     * Does not apply subpixel positioning to the path.
     *
     * @return false if this glyph does not have any path.
     */
    protected abstract boolean generatePath(Glyph glyph, Path dst);
}
