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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Descriptor of font strike.
 * <p>
 * A {@code StrikeDesc} is immutable unless it is a {@link Mutable} subclass.
 */
@NullMarked
public sealed class StrikeDesc {

    public static final int
            kFrameAndFill_Flag = 0x01,
            kSubpixelPositioning_Flag = 0x02,
            kLinearMetrics_Flag = 0x04;

    private Typeface mTypeface;

    private float mTextSize;

    private float mPostScaleX;
    private float mPostScaleY;
    private float mPostShearX;
    private float mPostShearY;

    private float mFrameWidth;
    private float mMiterLimit;

    private byte mMaskFormat;
    private byte mStrokeJoin;
    short mFlags;

    @Nullable
    private PathEffect mPathEffect;

    private transient int mHash;

    StrikeDesc() {
    }

    StrikeDesc(StrikeDesc other) {
        mTypeface = other.mTypeface;
        mPathEffect = other.mPathEffect;
        mTextSize = other.mTextSize;
        mPostScaleX = other.mPostScaleX;
        mPostScaleY = other.mPostScaleY;
        mPostShearX = other.mPostShearX;
        mPostShearY = other.mPostShearY;
        mFrameWidth = other.mFrameWidth;
        mMiterLimit = other.mMiterLimit;
        mMaskFormat = other.mMaskFormat;
        mStrokeJoin = other.mStrokeJoin;
        mFlags = other.mFlags;
        mHash = other.mHash;
    }

    public static StrikeDesc makeMask(Font font, @Nullable Paint paint,
                                      Matrixc deviceMatrix) {
        return new StrikeDesc().update(font, paint, deviceMatrix);
    }

    /**
     * Return the scalar with only limited fractional precision. Used to consolidate matrices
     * that vary only slightly when we create our key into the font cache, since the font scaler
     * typically returns the same looking results for tiny changes in the matrix.
     */
    public static float round_mat_elem(float x) {
        return Math.round(x * 1024) / 1024.0f;
    }

    private StrikeDesc update(Font font, @Nullable Paint paint,
                              Matrixc deviceMatrix) {
        if (deviceMatrix.hasPerspective()) {
            throw new IllegalArgumentException();
        }

        mTypeface = font.getTypeface();
        mTextSize = font.getSize();

        int typeMask = deviceMatrix.getType();
        if ((typeMask & Matrixc.kScale_Mask) != 0) {
            mPostScaleX = round_mat_elem(deviceMatrix.getScaleX());
            mPostScaleY = round_mat_elem(deviceMatrix.getScaleY());
        } else {
            mPostScaleX = mPostScaleY = 1;
        }
        if ((typeMask & Matrixc.kAffine_Mask) != 0) {
            mPostShearX = round_mat_elem(deviceMatrix.getShearX());
            mPostShearY = round_mat_elem(deviceMatrix.getShearY());
        } else {
            mPostShearX = mPostShearY = 0;
        }

        int style = paint != null ? paint.getStyle() : Paint.FILL;
        float strokeWidth = paint != null ? paint.getStrokeWidth() : 0;

        short flags = 0;

        if (style != Paint.FILL && strokeWidth >= 0) {
            mFrameWidth = strokeWidth;
            if (paint.getStrokeJoin() == Paint.JOIN_MITER) {
                mMiterLimit = paint.getStrokeMiter();
            } else {
                mMiterLimit = 0;
            }
            mStrokeJoin = (byte) paint.getStrokeJoin();

            if (style == Paint.STROKE_AND_FILL) {
                flags |= kFrameAndFill_Flag;
            }
        } else {
            mFrameWidth = -1;
            mMiterLimit = 0;
            mStrokeJoin = 0;
        }

        mMaskFormat = switch (font.getEdging()) {
            case Font.kAlias_Edging -> Mask.kBW_Format;
            case Font.kAntiAlias_Edging -> Mask.kA8_Format;
            default -> {
                assert false;
                yield Mask.kA8_Format;
            }
        };

        if (font.isSubpixel()) {
            flags |= kSubpixelPositioning_Flag;
        }
        if (font.isLinearMetrics()) {
            flags |= kLinearMetrics_Flag;
        }

        mFlags = flags;

        mPathEffect = paint != null ? paint.getPathEffect() : null;

        computeHashCode();
        return this;
    }

    // Create a strike spec for mask style cache entries.
    private StrikeDesc updateForMask(Font font, Paint paint,
                                     Matrixc deviceMatrix) {
        return update(font, paint, deviceMatrix);
    }

    public void getLocalMatrix(Matrix dst) {
        dst.setScale(mTextSize, mTextSize);
    }

    public void getDeviceMatrix(Matrix dst) {
        dst.set(mPostScaleX, mPostShearY, 0,
                mPostShearX, mPostScaleY, 0,
                0, 0, 1);
    }

    public void getTotalMatrix(Matrix dst) {
        getDeviceMatrix(dst);
        dst.preScale(mTextSize, mTextSize);
    }

    public float getTextSize() {
        return mTextSize;
    }

    public float getPostScaleX() {
        return mPostScaleX;
    }

    public float getPostScaleY() {
        return mPostScaleY;
    }

    public float getPostShearX() {
        return mPostShearX;
    }

    public float getPostShearY() {
        return mPostShearY;
    }

    public float getFrameWidth() {
        return mFrameWidth;
    }

    public float getMiterLimit() {
        return mMiterLimit;
    }

    // Mask::Format
    public byte getMaskFormat() {
        return mMaskFormat;
    }

    @Paint.Join
    public int getStrokeJoin() {
        return mStrokeJoin;
    }

    public int getFlags() {
        return mFlags & 0xFFFF;
    }

    public @Nullable PathEffect getPathEffect() {
        return mPathEffect;
    }

    public Strike findOrCreateStrike() {
        return StrikeCache.getGlobalStrikeCache().findOrCreateStrike(this);
    }

    public Strike findOrCreateStrike(StrikeCache cache) {
        return cache.findOrCreateStrike(this);
    }

    public ScalerContext createScalerContext() {
        return mTypeface.createScalerContext(this);
    }

    void computeHashCode() {
        int h = mTypeface.hashCode();
        h = 31 * h + Float.floatToIntBits(mTextSize);
        h = 31 * h + Float.floatToIntBits(mPostScaleX);
        h = 31 * h + Float.floatToIntBits(mPostScaleY);
        h = 31 * h + Float.floatToIntBits(mPostShearX);
        h = 31 * h + Float.floatToIntBits(mPostShearY);
        h = 31 * h + Float.floatToIntBits(mFrameWidth);
        h = 31 * h + Float.floatToIntBits(mMiterLimit);
        h = 31 * h + (int) mMaskFormat;
        h = 31 * h + (int) mStrokeJoin;
        h = 31 * h + (int) mFlags;
        h = 31 * h + Objects.hashCode(mPathEffect);
        mHash = h;
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
                    mMaskFormat == that.mMaskFormat &&
                    mStrokeJoin == that.mStrokeJoin &&
                    mFlags == that.mFlags &&
                    mTypeface.equals(that.mTypeface) &&
                    Objects.equals(mPathEffect, that.mPathEffect);
        }
        return false;
    }

    public StrikeDesc immutable() {
        return getClass() == StrikeDesc.class ? this : new StrikeDesc(this);
    }

    public boolean isImmutable() {
        return getClass() == StrikeDesc.class;
    }

    public long getMemorySize() {
        return 64;
    }

    /**
     * A reusable strike desc for lookup.
     */
    public static final class Mutable extends StrikeDesc {

        public Mutable() {
        }

        public Mutable(StrikeDesc other) {
            super(other);
        }

        public StrikeDesc update(Font font, @Nullable Paint paint,
                                 Matrixc deviceMatrix) {
            return super.update(font, paint, deviceMatrix);
        }

        public StrikeDesc updateForMask(Font font, Paint paint,
                                        Matrixc deviceMatrix) {
            return super.updateForMask(font, paint, deviceMatrix);
        }

        public void setFlags(int flags) {
            mFlags = (short) flags;
            computeHashCode();
        }
    }
}
