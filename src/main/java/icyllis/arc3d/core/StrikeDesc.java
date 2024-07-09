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

/**
 * Descriptor of font strike.
 * <p>
 * Our engine does not implement a FontScalerContext
 */
public class StrikeDesc {

    public static final int
            kFrameAndFill_Flag = 0x01,
            kLinearMetrics_Flag = 0x02;

    private Typeface mTypeface;
    private float mTextSize;
    private float mPostScaleX;
    private float mPostScaleY;
    private float mPostShearX;
    private float mPostShearY;
    private float mFrameWidth;
    private float mMiterLimit;
    private byte mEdging;
    private byte mStroke;
    private short mFlags;
    private transient int mHash;

    private static float round_tx_elem(float x) {
        return Math.round(x * 1024) / 1024.0f;
    }

    public StrikeDesc() {
    }

    public StrikeDesc(StrikeDesc other) {
        mTypeface = other.mTypeface;
        mTextSize = other.mTextSize;
        mPostScaleX = other.mPostScaleX;
        mPostScaleY = other.mPostScaleY;
        mPostShearX = other.mPostShearX;
        mPostShearY = other.mPostShearY;
        mFrameWidth = other.mFrameWidth;
        mMiterLimit = other.mMiterLimit;
        mEdging = other.mEdging;
        mStroke = other.mStroke;
        mFlags = other.mFlags;
        mHash = other.mHash;
    }

    public StrikeDesc(Font font, Paint paint,
                      Matrixc deviceMatrix) {
        update(font, paint, deviceMatrix);
    }

    public StrikeDesc update(Font font, Paint paint,
                             Matrixc deviceMatrix) {
        assert !deviceMatrix.hasPerspective();

        mTypeface = font.getTypeface();
        mTextSize = font.getSize();

        int typeMask = deviceMatrix.getType();
        if ((typeMask & Matrixc.kScale_Mask) != 0) {
            mPostScaleX = round_tx_elem(deviceMatrix.getScaleX());
            mPostScaleY = round_tx_elem(deviceMatrix.getScaleY());
        } else {
            mPostScaleX = mPostScaleY = 1;
        }
        if ((typeMask & Matrixc.kAffine_Mask) != 0) {
            mPostShearX = round_tx_elem(deviceMatrix.getShearX());
            mPostShearY = round_tx_elem(deviceMatrix.getShearY());
        } else {
            mPostShearX = mPostShearY = 0;
        }

        int style = paint.getStyle();
        float strokeWidth = paint.getStrokeWidth();

        int flags = 0;

        // no hairline support
        if (style != Paint.FILL && strokeWidth > 0) {
            mFrameWidth = strokeWidth;
            mMiterLimit = paint.getStrokeMiter();
            mStroke = (byte) ((paint.getStrokeJoin() << 4) | paint.getStrokeCap());

            if (style == Paint.STROKE_AND_FILL) {
                flags |= kFrameAndFill_Flag;
            }
        } else {
            mFrameWidth = -1;
            mMiterLimit = 0;
            mStroke = 0;
        }

        mEdging = font.getEdging();

        if (font.isLinearMetrics()) {
            flags |= kLinearMetrics_Flag;
        }

        mFlags = (short) flags;

        int h = mTypeface.hashCode();
        h = 31 * h + Float.floatToIntBits(mTextSize);
        h = 31 * h + Float.floatToIntBits(mPostScaleX);
        h = 31 * h + Float.floatToIntBits(mPostScaleY);
        h = 31 * h + Float.floatToIntBits(mPostShearX);
        h = 31 * h + Float.floatToIntBits(mPostShearY);
        h = 31 * h + Float.floatToIntBits(mFrameWidth);
        h = 31 * h + Float.floatToIntBits(mMiterLimit);
        h = 31 * h + (int) mEdging;
        h = 31 * h + (int) mStroke;
        h = 31 * h + (int) mFlags;
        mHash = h;

        return this;
    }

    public void getLocalMatrix(Matrix dst) {
        dst.setScale(mTextSize, mTextSize);
    }

    public void getDeviceMatrix(Matrix dst) {
        dst.set(mPostScaleX, mPostShearY, 0,
                mPostShearX, mPostScaleY, 0,
                0, 0, 1);
    }

    public void getGlyphMatrix(Matrix dst) {
        getDeviceMatrix(dst);
        dst.preScale(mTextSize, mTextSize);
    }

    public int getStrokeCap() {
        return mStroke & 0xF;
    }

    public int getStrokeJoin() {
        return mStroke >>> 4;
    }

    public StrikeDesc copy() {
        return new StrikeDesc(this);
    }

    @Override
    public int hashCode() {
        return mHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof StrikeDesc that) {
            return mTextSize == that.mTextSize &&
                    mPostScaleX == that.mPostScaleX &&
                    mPostScaleY == that.mPostScaleY &&
                    mPostShearX == that.mPostShearX &&
                    mPostShearY == that.mPostShearY &&
                    mFrameWidth == that.mFrameWidth &&
                    mMiterLimit == that.mMiterLimit &&
                    mEdging == that.mEdging &&
                    mStroke == that.mStroke &&
                    mFlags == that.mFlags &&
                    mTypeface.equals(that.mTypeface);
        }
        return false;
    }
}
