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
 * This class collects stroke params from paint and constructs new paths
 * by stroking geometries.
 */
public class StrokeRec {

    public static final int
            kHairline_Style = 0,
            kFill_Style = 1,
            kStroke_Style = 2,
            kStrokeAndFill_Style = 3;

    private float mWidth;
    private float mMiterLimit;
    private float mResScale;
    private byte mCap, mJoin, mAlign;
    private boolean mStrokeAndFill;

    /**
     * A fill style.
     */
    public StrokeRec() {
        mWidth = -1;
        mMiterLimit = 4;
        mResScale = 1;
        mCap = Paint.CAP_BUTT;
        mJoin = Paint.JOIN_ROUND;
        mAlign = Paint.ALIGN_CENTER;
        mStrokeAndFill = false;
    }

    /**
     * Create from paint, assuming resScale = 1.
     */
    public StrokeRec(Paint paint) {
        this(paint, 1);
    }

    /**
     * Create from paint.
     */
    public StrokeRec(Paint paint, float resScale) {
        init(paint, paint.getStyle(), paint.getStrokeWidth(), resScale);
    }

    /**
     * Create from paint with overrides, assuming resScale = 1.
     */
    public StrokeRec(Paint paint, @Paint.Style int style, float width) {
        init(paint, style, width, 1);
    }

    /**
     * Create from paint with overrides.
     */
    public StrokeRec(Paint paint, @Paint.Style int style, float width, float resScale) {
        init(paint, style, width, resScale);
    }

    public void init(Paint paint, @Paint.Style int style, float width, float resScale) {
        assert width >= 0;
        mResScale = resScale;

        switch (style) {
            case Paint.FILL -> {
                mWidth = -1;
                mStrokeAndFill = false;
            }
            case Paint.STROKE -> {
                mWidth = width;
                mStrokeAndFill = false;
            }
            case Paint.STROKE_AND_FILL -> {
                if (width == 0) {
                    mWidth = -1;
                    mStrokeAndFill = false;
                } else {
                    mWidth = width;
                    mStrokeAndFill = true;
                }
            }
        }

        mMiterLimit = paint.getStrokeMiter();
        mCap = (byte) paint.getStrokeCap();
        mJoin = (byte) paint.getStrokeJoin();
        mAlign = (byte) paint.getStrokeAlign();
    }

    public int getStyle() {
        if (mWidth < 0) {
            return kFill_Style;
        } else if (mWidth == 0) {
            return kHairline_Style;
        } else {
            return mStrokeAndFill ? kStrokeAndFill_Style : kStroke_Style;
        }
    }

    public boolean isFillStyle() {
        return mWidth < 0;
    }

    public boolean isHairlineStyle() {
        return mWidth == 0;
    }

    /**
     * Returns true if this represents any thick stroking, i.e. applyToPath()
     * will return true.
     */
    public boolean isStrokeStyle() {
        return mWidth > 0;
    }

    public void setFillStyle() {
        mWidth = -1;
        mStrokeAndFill = false;
    }

    public void setHairlineStyle() {
        mWidth = 0;
        mStrokeAndFill = false;
    }

    /**
     * Specify the strokewidth, and optionally if you want stroke + fill.
     * Note, if width==0, then this request is taken to mean:<br>
     * strokeAndFill==true  new style will be Fill<br>
     * strokeAndFill==false  new style will be Hairline
     */
    public void setStrokeStyle(float width, boolean strokeAndFill) {
        assert width >= 0;
        if (strokeAndFill && width == 0) {
            // hairline+fill == fill
            setFillStyle();
        } else {
            mWidth = width;
            mStrokeAndFill = strokeAndFill;
        }
    }

    public float getWidth() {
        return mWidth;
    }

    public void setWidth(float width) {
        mWidth = width;
    }

    public int getCap() {
        return mCap;
    }

    public int getJoin() {
        return mJoin;
    }

    public int getAlign() {
        return mAlign;
    }

    public float getMiterLimit() {
        return mMiterLimit;
    }

    public void setCap(@Paint.Cap int cap) {
        mCap = (byte) cap;
    }

    public void setJoin(@Paint.Join int join) {
        mJoin = (byte) join;
    }

    public void setAlign(@Paint.Align int align) {
        mAlign = (byte) align;
    }

    public void setMiterLimit(float miterLimit) {
        assert miterLimit >= 0;
        mMiterLimit = miterLimit;
    }

    public void setStrokeParams(@Paint.Cap int cap, @Paint.Join int join,
                                @Paint.Align int align, float miterLimit) {
        assert miterLimit >= 0;
        mMiterLimit = miterLimit;
        mCap = (byte) cap;
        mJoin = (byte) join;
        mAlign = (byte) align;
    }

    /**
     * ResScale is the "intended" resolution for the output.<br>
     * Default is 1.0.<br>
     * Larger values (res > 1) indicate that the result should be more precise, since it will
     * be zoomed up, and small errors will be magnified.<br>
     * Smaller values (0 < res < 1) indicate that the result can be less precise, since it will
     * be zoomed down, and small errors may be invisible.
     */
    public float getResScale() {
        return mResScale;
    }

    public void setResScale(float resScale) {
        assert resScale > 0 && Float.isFinite(resScale);
        mResScale = resScale;
    }

    /**
     * Apply these stroke parameters to the src path, emitting the result
     * to dst.
     * <p>
     * If there was no change (i.e. style == hairline or fill) this returns
     * false and dst is unchanged. Otherwise returns true and the result is
     * emitted to dst.
     * <p>
     * src and dst must NOT come from the same object.
     */
    public boolean applyToPath(PathIterable src, PathConsumer dst) {
        if (mWidth <= 0) { // hairline or fill
            return false;
        }

        var stroker = new PathStroker();
        stroker.init(dst, mWidth * 0.5f, mCap, mJoin, mMiterLimit, mResScale);
        src.forEach(stroker);

        if (mStrokeAndFill) {
            //TODO handle direction
            src.forEach(dst);
        }

        return true;
    }

    /**
     * Apply these stroke parameters to a paint.
     */
    public void applyToPaint(Paint paint) {
        if (mWidth < 0) { // fill
            paint.setStyle(Paint.FILL);
            return;
        }

        paint.setStyle(mStrokeAndFill ? Paint.STROKE_AND_FILL : Paint.STROKE);
        paint.setStrokeWidth(mWidth);
        paint.setStrokeMiter(mMiterLimit);
        paint.setStrokeCap(mCap);
        paint.setStrokeJoin(mJoin);
        paint.setStrokeAlign(mAlign);
    }

    /**
     * Gives a conservative value for the outset that should be applied to a
     * geometries bounds to account for any inflation due to applying this
     * stroke to the geometry.
     */
    public float getInflationRadius() {
        return getInflationRadius(mWidth, mCap, mJoin, mAlign, mMiterLimit);
    }

    /**
     * Compare if two Strokes have an equal effect on a path.
     * Equal Strokes produce equal paths. Equality of produced
     * paths does not take the ResScale parameter into account.
     */
    public boolean hasSameEffect(StrokeRec other) {
        if (mWidth <= 0) { // hairline or fill
            return getStyle() == other.getStyle();
        }
        return mWidth == other.mWidth &&
                (mJoin != Paint.JOIN_MITER || mMiterLimit == other.mMiterLimit) &&
                mCap == other.mCap &&
                mJoin == other.mJoin &&
                mAlign == other.mAlign &&
                mStrokeAndFill == other.mStrokeAndFill;
    }

    public static float getInflationRadius(float strokeWidth,
                                           int cap, int join, int align,
                                           float miterLimit) {
        if (strokeWidth < 0) { // fill
            return 0;
        } else if (strokeWidth == 0) { // hairline
            //TODO this is not correct, should map into device space
            return 1;
        }

        float multiplier = 1;
        if (join == Paint.JOIN_MITER) {
            multiplier = Math.max(multiplier, miterLimit);
        }
        if (align == Paint.ALIGN_CENTER) {
            if (cap == Paint.CAP_SQUARE) {
                multiplier = Math.max(multiplier, MathUtil.SQRT2);
            }
            multiplier *= 0.5f;
        }
        return strokeWidth * multiplier;
    }
}
