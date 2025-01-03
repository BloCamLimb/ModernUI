/*
 * Modern UI.
 * Copyright (C) 2019-2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.drawable;

import icyllis.modernui.annotation.*;
import icyllis.modernui.graphics.*;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.util.ColorStateList;
import org.intellij.lang.annotations.MagicConstant;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A {@link Drawable} for drawing shapes.
 * <p>
 * We have five shapes that are:
 * <ul>
 *     <li>Rectangle</li>
 *     <li>Circle</li>
 *     <li>Ring</li>
 *     <li>HLine</li>
 *     <li>VLine</li>
 * </ul>
 * In details: rectangle, circle and ring can be filled, stroked, or both,
 * with different colors.<br>
 * Rectangle can be rounded; circle and ring can use {@link #getLevel() level},
 * that is, they become pie and arc, which can be stroked as well.<br>
 */
public class ShapeDrawable extends Drawable {

    /**
     * Shape is a rectangle, possibly with rounded corners.
     */
    public static final int RECTANGLE = 0;

    /**
     * Shape is a circle or pie.
     */
    public static final int CIRCLE = 1;

    /**
     * Shape is a ring or arc.
     */
    public static final int RING = 2;

    /**
     * Shape is a horizontal line.
     */
    public static final int HLINE = 3;

    /**
     * Shape is a vertical line.
     */
    public static final int VLINE = 4;

    /**
     * Blends two alpha using modulate. This method has errors but is fast.
     *
     * @param srcAlpha 0..255 no validation
     * @param dstAlpha 0..255 no validation
     * @return result alpha
     */
    public static int modulateAlpha(int srcAlpha, int dstAlpha) {
        int multiplier = dstAlpha + (dstAlpha >> 7);
        return srcAlpha * multiplier >> 8;
    }

    /**
     * @hidden
     */
    @MagicConstant(intValues = {RECTANGLE, CIRCLE, RING, HLINE, VLINE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Shape {
    }

    private static final float DEFAULT_INNER_RADIUS_RATIO = 3.0f;
    private static final float DEFAULT_THICKNESS_RATIO = 9.0f;

    ShapeState mShapeState;

    private Rect mPadding;

    final Paint mFillPaint = new Paint();
    private Paint mStrokePaint;   // optional, set by the caller
    private ColorFilter mColorFilter;   // optional, set by the caller
    private BlendModeColorFilter mBlendModeColorFilter;
    boolean mShapeIsDirty;

    final RectF mRect = new RectF();

    private int mAlpha = 0xFF;  // modified by the caller
    boolean mMutated;

    public ShapeDrawable() {
        this(new ShapeState(), null);
    }

    /**
     * This checks mShapeIsDirty, and if it is true, recomputes the drawing
     * rectangle (mRect).
     *
     * @return true if the resulting rectangle is empty
     */
    final boolean updateRectIsEmpty() {
        if (mShapeIsDirty) {
            mShapeIsDirty = false;

            mRect.set(getBounds());

            if (mStrokePaint != null && mStrokePaint.getColor() != Color.TRANSPARENT) {
                // the stroke align is center, inset half stroke width to fit in bounds
                float inset = mStrokePaint.getStrokeWidth() * 0.5f;
                mRect.inset(inset, inset);
            }

            updateGradient();
        }
        if (mShapeState.mStrokeWidth > 0) {
            // fixed by Modern UI: do not skip stroke-only draw
            return getBounds().isEmpty();
        } else {
            return mRect.isEmpty();
        }
    }

    // Used by GradientDrawable subclass
    void updateGradient() {
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (updateRectIsEmpty()) {
            return;
        }

        // remember the alpha values, in case we temporarily overwrite them
        // when we modulate them with mAlpha
        final int prevFillAlpha = mFillPaint.getAlpha();
        final int prevStrokeAlpha = mStrokePaint != null ? mStrokePaint.getAlpha() : 0;
        // compute the modulate alpha values
        final int currFillAlpha = modulateAlpha(prevFillAlpha, mAlpha);
        final int currStrokeAlpha = modulateAlpha(prevStrokeAlpha, mAlpha);

        final ShapeState st = mShapeState;
        final ColorFilter colorFilter = mColorFilter != null ? mColorFilter : mBlendModeColorFilter;

        //TODO currently we don't have layer support, so fill+stroke with alpha<255 or non-null color filter
        // results in rendering artifacts. once we have custom effect support, we will write a dedicated shader
        // that fills and strokes with different colors simultaneously

        mFillPaint.setAlpha(currFillAlpha);
        mFillPaint.setDither(st.mDither);
        mFillPaint.setColorFilter(colorFilter);
        if (colorFilter != null && st.mSolidColors == null) {
            // Modern UI convention is white alpha
            mFillPaint.setRGBA(255, 255, 255, mAlpha);
        }
        // fixed by Modern UI: also check BlendMode/Shader/ColorFilter to determine there's fill or stroke
        final boolean haveFill = !mFillPaint.getNativePaint().nothingToDraw();
        final boolean haveStroke;
        final boolean restoreStroke;
        if (mStrokePaint != null &&
                mStrokePaint.getStrokeWidth() > 0 &&
                mStrokePaint.getColor() != Color.TRANSPARENT) {
            mStrokePaint.setAlpha(currStrokeAlpha);
            // stroke won't use dithering
            mStrokePaint.setColorFilter(colorFilter);
            haveStroke = !mStrokePaint.getNativePaint().nothingToDraw();
            restoreStroke = true;
        } else {
            haveStroke = false;
            restoreStroke = false;
        }

        if (haveFill | haveStroke) {
            final RectF r = mRect;
            switch (st.mShape) {
                case RECTANGLE -> {
                    if (st.mRadius > 0.0f) {
                        float rad = Math.min(st.mRadius,
                                Math.min(r.width(), r.height()) * 0.5f);
                        if (haveFill) {
                            canvas.drawRoundRect(r, rad, mFillPaint);
                        }
                        if (haveStroke) {
                            canvas.drawRoundRect(r, rad, mStrokePaint);
                        }
                    } else {
                        if (haveFill) {
                            canvas.drawRect(r, mFillPaint);
                        }
                        if (haveStroke) {
                            canvas.drawRect(r, mStrokePaint);
                        }
                    }
                }
                case CIRCLE -> {
                    float cx = r.centerX();
                    float cy = r.centerY();
                    float radius = Math.min(r.width(), r.height()) * 0.5f;
                    if (st.mUseLevelForShape && getLevel() < MAX_LEVEL) {
                        float sweep = 360.0f * getLevel() / MAX_LEVEL;
                        if (haveFill) {
                            canvas.drawPie(cx, cy, radius, -90, sweep, mFillPaint);
                        }
                        if (haveStroke) {
                            canvas.drawPie(cx, cy, radius, -90, sweep, mStrokePaint);
                        }
                    } else {
                        if (haveFill) {
                            canvas.drawCircle(cx, cy, radius, mFillPaint);
                        }
                        if (haveStroke) {
                            canvas.drawCircle(cx, cy, radius, mStrokePaint);
                        }
                    }
                }
                case RING -> {
                    float cx = r.centerX();
                    float cy = r.centerY();
                    float thickness = st.mThickness != -1 ?
                            st.mThickness : Math.min(r.width(), r.height()) / st.mThicknessRatio;
                    float innerRadius = st.mInnerRadius != -1 ?
                            st.mInnerRadius : Math.min(r.width(), r.height()) / st.mInnerRadiusRatio;
                    float radius = innerRadius + thickness * 0.5f;
                    float sweep = st.mUseLevelForShape ? (360.0f * getLevel() / MAX_LEVEL) : 360f;
                    Paint.Cap cap = st.mRadius > 0 ? Paint.Cap.ROUND : Paint.Cap.BUTT;
                    if (haveFill) {
                        canvas.drawArc(cx, cy, radius, -90, sweep,
                                cap, thickness, mFillPaint);
                    }
                    if (haveStroke) {
                        canvas.drawArc(cx, cy, radius, -90, sweep,
                                cap, thickness, mStrokePaint);
                    }
                }
                case HLINE -> {
                    float y = r.centerY();
                    Paint.Cap cap = st.mRadius > 0 ? Paint.Cap.ROUND : Paint.Cap.BUTT;
                    if (haveStroke) {
                        mStrokePaint.setStrokeCap(cap);
                        canvas.drawLine(r.left, y, r.right, y, mStrokePaint);
                    } else {
                        // Modern UI added, both are the same
                        canvas.drawLine(r.left, y, r.right, y, cap, r.height(), mFillPaint);
                    }
                }
                case VLINE -> {
                    float x = r.centerX();
                    Paint.Cap cap = st.mRadius > 0 ? Paint.Cap.ROUND : Paint.Cap.BUTT;
                    if (haveStroke) {
                        mStrokePaint.setStrokeCap(cap);
                        canvas.drawLine(x, r.top, x, r.bottom, mStrokePaint);
                    } else {
                        // Modern UI added, both are the same
                        canvas.drawLine(x, r.top, x, r.bottom, cap, r.width(), mFillPaint);
                    }
                }
            }
        }

        mFillPaint.setAlpha(prevFillAlpha);
        mFillPaint.setColorFilter(null);
        if (restoreStroke) {
            mStrokePaint.setAlpha(prevStrokeAlpha);
            mStrokePaint.setColorFilter(null);
        }
    }

    /**
     * <p>Sets the type of shape used to draw the gradient.</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param shape The desired shape for this drawable: {@link #RECTANGLE},
     *              {@link #CIRCLE}, {@link #RING}, {@link #HLINE} or {@link #VLINE}
     * @see #mutate()
     */
    public void setShape(@Shape int shape) {
        mShapeState.setShape(shape);
        invalidateSelf();
    }

    /**
     * Returns the type of shape used by this drawable, one of {@link #RECTANGLE},
     * {@link #CIRCLE}, {@link #RING}, {@link #HLINE} or {@link #VLINE}.
     *
     * @return the type of shape used by this drawable
     * @see #setShape(int)
     */
    @Shape
    public int getShape() {
        return mShapeState.mShape;
    }

    /**
     * <p>Sets whether to draw circles and rings based on level. If {@link #getLevel()}
     * is less than {@link #MAX_LEVEL}, a circle shape becomes a pie, and a ring shape
     * becomes an open arc. {@link #CIRCLE} and {@link #RING} will start at 0 o'clock
     * direction and sweep clockwise. The default is true.</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param useLevelForShape Whether to use level for shape
     * @see #getUseLevelForShape()
     */
    public void setUseLevelForShape(boolean useLevelForShape) {
        mShapeState.mUseLevelForShape = useLevelForShape;
        invalidateSelf();
    }

    /**
     * @see #setUseLevelForShape(boolean)
     */
    public boolean getUseLevelForShape() {
        return mShapeState.mUseLevelForShape;
    }

    /**
     * Configure the padding of the shape, there is no padding by default.
     *
     * @param left   Left padding of the shape
     * @param top    Top padding of the shape
     * @param right  Right padding of the shape
     * @param bottom Bottom padding of the shape
     */
    public void setPadding(@Px int left, @Px int top, @Px int right, @Px int bottom) {
        if (mShapeState.mPadding == null) {
            mShapeState.mPadding = new Rect();
        }

        mShapeState.mPadding.set(left, top, right, bottom);
        mPadding = mShapeState.mPadding;
        invalidateSelf();
    }

    @Override
    public boolean getPadding(@NonNull Rect padding) {
        if (mPadding != null) {
            padding.set(mPadding);
            return true;
        } else {
            return super.getPadding(padding);
        }
    }

    /**
     * <p>Sets the size of the shape drawn by this drawable.</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param width  The width of the shape used by this drawable
     * @param height The height of the shape used by this drawable
     * @see #mutate()
     */
    public void setSize(int width, int height) {
        mShapeState.setSize(width, height);
        invalidateSelf();
    }

    /**
     * Specifies the radius for the corners of the gradient. If this is > 0,
     * then the drawable is drawn in a round-rectangle, rather than a
     * rectangle. This property is honored only when the shape is of type
     * {@link #RECTANGLE}. Specifically, if this is > 0, the line ends are
     * rounded when the shape is of type {@link #HLINE}, {@link #VLINE} or
     * {@link #RING}.
     * <p>
     * <strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.
     *
     * @param radius The radius in pixels of the corners of the rectangle shape
     * @see #mutate()
     * @see #setShape(int)
     */
    public void setCornerRadius(float radius) {
        mShapeState.setCornerRadius(radius);
        invalidateSelf();
    }

    /**
     * Returns the radius for the corners of the gradient, that was previously set with
     * {@link #setCornerRadius(float)}.
     *
     * @return the radius in pixels of the corners of the rectangle shape, or 0
     * @see #setCornerRadius
     */
    public float getCornerRadius() {
        return mShapeState.mRadius;
    }

    /**
     * Changes this drawable to use a single color instead of a gradient.
     * <p>
     * <strong>Note</strong>: changing color will affect all instances of a
     * drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing the color.
     *
     * @param color The color used to fill the shape
     * @see #mutate()
     * @see #getColor
     */
    public void setColor(@ColorInt int color) {
        mShapeState.setSolidColors(ColorStateList.valueOf(color));
        mFillPaint.setColor(color);
        invalidateSelf();
    }

    /**
     * Changes this drawable to use a single color state list instead of a
     * gradient. Calling this method with a null argument will clear the color
     * and is equivalent to calling {@link #setColor(int)} with the argument
     * {@link Color#TRANSPARENT}.
     * <p>
     * <strong>Note</strong>: changing color will affect all instances of a
     * drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing the color.</p>
     *
     * @param colorStateList The color state list used to fill the shape
     * @see #mutate()
     * @see #getColor
     */
    public void setColor(@Nullable ColorStateList colorStateList) {
        if (colorStateList == null) {
            setColor(Color.TRANSPARENT);
        } else {
            final int[] stateSet = getState();
            final int color = colorStateList.getColorForState(stateSet, Color.TRANSPARENT);
            mShapeState.setSolidColors(colorStateList);
            mFillPaint.setColor(color);
            invalidateSelf();
        }
    }

    /**
     * Returns the color state list used to fill the shape, or {@code null} if
     * the shape is filled with a gradient or has no fill color.
     *
     * @return the color state list used to fill this gradient, or {@code null}
     * @see #setColor(int)
     * @see #setColor(ColorStateList)
     */
    @Nullable
    public ColorStateList getColor() {
        return mShapeState.mSolidColors;
    }

    /**
     * <p>Set the stroke width and color for the drawable. If width is zero,
     * then no stroke is drawn.</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param width The width in pixels of the stroke
     * @param color The color of the stroke
     * @see #mutate()
     */
    public void setStroke(int width, @ColorInt int color) {
        mShapeState.setStroke(width, ColorStateList.valueOf(color));
        setStrokeInternal(width, color);
    }

    /**
     * <p>Set the stroke width and color state list for the drawable. If width
     * is zero, then no stroke is drawn.</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param width          The width in pixels of the stroke
     * @param colorStateList The color state list of the stroke
     * @see #mutate()
     */
    public void setStroke(int width, @Nullable ColorStateList colorStateList) {
        mShapeState.setStroke(width, colorStateList);
        final int color;
        if (colorStateList == null) {
            color = Color.TRANSPARENT;
        } else {
            final int[] stateSet = getState();
            color = colorStateList.getColorForState(stateSet, Color.TRANSPARENT);
        }
        setStrokeInternal(width, color);
    }

    private void setStrokeInternal(int width, int color) {
        if (mStrokePaint == null) {
            mStrokePaint = new Paint();
            mStrokePaint.setStyle(Paint.STROKE);
        }
        mStrokePaint.setStrokeWidth(width);
        mStrokePaint.setColor(color);
        mShapeIsDirty = true;
        invalidateSelf();
    }

    /**
     * Inner radius of the ring expressed as a ratio of the ring's width.
     * This value is used only when {@link #getInnerRadius()} is -1.
     *
     * @see #getInnerRadiusRatio()
     */
    public void setInnerRadiusRatio(
            @FloatRange(from = 0.0f, fromInclusive = false) float innerRadiusRatio) {
        if (!(innerRadiusRatio > 0)) {
            throw new IllegalArgumentException("Ratio must be greater than zero");
        }
        mShapeState.mInnerRadiusRatio = innerRadiusRatio;
        invalidateSelf();
    }

    /**
     * Return the inner radius of the ring expressed as a ratio of the ring's width.
     * This value is used only when {@link #getInnerRadius()} is -1.
     *
     * @see #setInnerRadiusRatio(float)
     */
    public float getInnerRadiusRatio() {
        return mShapeState.mInnerRadiusRatio;
    }

    /**
     * Configure the inner radius of the ring, or -1 to use {@link #getInnerRadiusRatio()}.
     * The default value is -1.
     *
     * @see #getInnerRadius()
     */
    public void setInnerRadius(@Px int innerRadius) {
        mShapeState.mInnerRadius = innerRadius;
        invalidateSelf();
    }

    /**
     * Return the inner radius of the ring, or -1 to use {@link #getInnerRadiusRatio()}.
     * The default value is -1.
     *
     * @see #setInnerRadius(int)
     */
    @Px
    public int getInnerRadius() {
        return mShapeState.mInnerRadius;
    }

    /**
     * Configure the thickness of the ring expressed as a ratio of the ring's width.
     * This value is used only when {@link #getThickness()} is -1.
     *
     * @see #getThicknessRatio()
     */
    public void setThicknessRatio(
            @FloatRange(from = 0.0f, fromInclusive = false) float thicknessRatio) {
        if (!(thicknessRatio > 0)) {
            throw new IllegalArgumentException("Ratio must be greater than zero");
        }
        mShapeState.mThicknessRatio = thicknessRatio;
        invalidateSelf();
    }

    /**
     * Return the thickness ratio of the ring expressed as a ratio of the ring's width.
     * This value is used only when {@link #getThickness()} is -1.
     *
     * @see #setThicknessRatio(float)
     */
    public float getThicknessRatio() {
        return mShapeState.mThicknessRatio;
    }

    /**
     * Configure the thickness of the ring, or -1 to use {@link #getThicknessRatio()}.
     * The default value is -1.
     */
    public void setThickness(@Px int thickness) {
        mShapeState.mThickness = thickness;
        invalidateSelf();
    }

    /**
     * Return the thickness of the ring, or -1 to use {@link #getThicknessRatio()}.
     * The default value is -1.
     *
     * @see #setThickness(int)
     */
    @Px
    public int getThickness() {
        return mShapeState.mThickness;
    }

    @Override
    protected boolean onStateChange(@NonNull int[] stateSet) {
        boolean invalidateSelf = false;

        final ShapeState s = mShapeState;
        final ColorStateList solidColors = s.mSolidColors;
        if (solidColors != null) {
            final int newColor = solidColors.getColorForState(stateSet, Color.TRANSPARENT);
            final int oldColor = mFillPaint.getColor();
            if (oldColor != newColor) {
                mFillPaint.setColor(newColor);
                invalidateSelf = true;
            }
        }

        final Paint strokePaint = mStrokePaint;
        if (strokePaint != null) {
            final ColorStateList strokeColors = s.mStrokeColors;
            if (strokeColors != null) {
                final int newColor = strokeColors.getColorForState(stateSet, Color.TRANSPARENT);
                final int oldColor = strokePaint.getColor();
                if (oldColor != newColor) {
                    strokePaint.setColor(newColor);
                    mShapeIsDirty = true;
                    invalidateSelf = true;
                }
            }
        }

        if (s.mTint != null && s.mBlendMode != null) {
            mBlendModeColorFilter = updateBlendModeFilter(mBlendModeColorFilter,
                    s.mTint, s.mBlendMode);
            invalidateSelf = true;
        }

        if (invalidateSelf) {
            invalidateSelf();
            return true;
        }

        return false;
    }

    @Override
    public boolean isStateful() {
        final ShapeState s = mShapeState;
        return super.isStateful()
                || (s.mSolidColors != null && s.mSolidColors.isStateful())
                || (s.mStrokeColors != null && s.mStrokeColors.isStateful())
                || (s.mTint != null && s.mTint.isStateful());
    }

    @Override
    public boolean hasFocusStateSpecified() {
        final ShapeState s = mShapeState;
        return (s.mSolidColors != null && s.mSolidColors.hasFocusStateSpecified())
                || (s.mStrokeColors != null && s.mStrokeColors.hasFocusStateSpecified())
                || (s.mTint != null && s.mTint.hasFocusStateSpecified());
    }

    @Override
    public void setAlpha(int alpha) {
        if (alpha != mAlpha) {
            mAlpha = alpha;
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return mAlpha;
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        if (colorFilter != mColorFilter) {
            mColorFilter = colorFilter;
            invalidateSelf();
        }
    }

    @Nullable
    @Override
    public ColorFilter getColorFilter() {
        return mColorFilter;
    }

    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        mShapeState.mTint = tint;
        mBlendModeColorFilter =
                updateBlendModeFilter(mBlendModeColorFilter, tint, mShapeState.mBlendMode);
        invalidateSelf();
    }

    @Override
    public void setTintBlendMode(@NonNull BlendMode blendMode) {
        mShapeState.mBlendMode = blendMode;
        mBlendModeColorFilter =
                updateBlendModeFilter(mBlendModeColorFilter, mShapeState.mTint, blendMode);
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);
        mShapeIsDirty = true;
    }

    @Override
    protected boolean onLevelChange(int level) {
        super.onLevelChange(level);
        mShapeIsDirty = true;
        invalidateSelf();
        return true;
    }

    @Override
    public int getIntrinsicWidth() {
        return mShapeState.mWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mShapeState.mHeight;
    }

    @Override
    public ConstantState getConstantState() {
        return mShapeState;
    }

    @NonNull
    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mShapeState = new ShapeState(mShapeState, null);
            updateLocalState(null);
            mMutated = true;
        }
        return this;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        mMutated = false;
    }

    static class ShapeState extends ConstantState {

        @Shape
        public int mShape = RECTANGLE;

        public ColorStateList mSolidColors;
        public ColorStateList mStrokeColors;

        public int mStrokeWidth = -1; // if >= 0 use stroking.

        public float mRadius = 0.0f;

        public Rect mPadding = null;

        public int mWidth = -1;
        public int mHeight = -1;
        public float mInnerRadiusRatio = DEFAULT_INNER_RADIUS_RATIO;
        public float mThicknessRatio = DEFAULT_THICKNESS_RATIO;
        public int mInnerRadius = -1;
        public int mThickness = -1;

        public boolean mDither = false;

        boolean mUseLevelForShape = true;

        ColorStateList mTint = null;
        BlendMode mBlendMode = DEFAULT_BLEND_MODE;

        public ShapeState() {
        }

        public ShapeState(@NonNull ShapeState orig, @Nullable Resources res) {
            mShape = orig.mShape;
            mSolidColors = orig.mSolidColors;
            mStrokeColors = orig.mStrokeColors;
            mStrokeWidth = orig.mStrokeWidth;
            mRadius = orig.mRadius;
            if (orig.mPadding != null) {
                mPadding = new Rect(orig.mPadding);
            }
            mWidth = orig.mWidth;
            mHeight = orig.mHeight;
            mInnerRadiusRatio = orig.mInnerRadiusRatio;
            mThicknessRatio = orig.mThicknessRatio;
            mInnerRadius = orig.mInnerRadius;
            mThickness = orig.mThickness;
            mDither = orig.mDither;
            mUseLevelForShape = orig.mUseLevelForShape;
            mTint = orig.mTint;
            mBlendMode = orig.mBlendMode;
        }

        public void setShape(@Shape int shape) {
            mShape = shape;
        }

        public void setSolidColors(@Nullable ColorStateList colors) {
            mSolidColors = colors;
        }

        public void setStroke(int width, @Nullable ColorStateList colors) {
            mStrokeWidth = width;
            mStrokeColors = colors;
        }

        public void setCornerRadius(float radius) {
            if (radius >= 0) {
                mRadius = radius;
            } else {
                mRadius = 0;
            }
        }

        public void setSize(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            return new ShapeDrawable(this, null);
        }

        @NonNull
        @Override
        public Drawable newDrawable(@Nullable Resources res) {
            return new ShapeDrawable(this, res);
        }
    }

    ShapeDrawable(@NonNull ShapeState state, @Nullable Resources res) {
        mShapeState = state;

        updateLocalState(res);
    }

    void updateLocalState(@Nullable Resources res) {
        final ShapeState state = mShapeState;

        if (state.mSolidColors != null) {
            final int[] currentState = getState();
            final int stateColor = state.mSolidColors.getColorForState(currentState, 0);
            mFillPaint.setColor(stateColor);
        } else {
            mFillPaint.setColor(Color.TRANSPARENT);
        }

        mPadding = state.mPadding;

        if (state.mStrokeWidth >= 0) {
            mStrokePaint = new Paint();
            mStrokePaint.setStyle(Paint.STROKE);
            mStrokePaint.setStrokeWidth(state.mStrokeWidth);

            if (state.mStrokeColors != null) {
                final int[] currentState = getState();
                final int strokeStateColor = state.mStrokeColors.getColorForState(
                        currentState, Color.TRANSPARENT);
                mStrokePaint.setColor(strokeStateColor);
            }
        }

        mBlendModeColorFilter = updateBlendModeFilter(mBlendModeColorFilter,
                state.mTint, state.mBlendMode);
        mShapeIsDirty = true;
    }
}
