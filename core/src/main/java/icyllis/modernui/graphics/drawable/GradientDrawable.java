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

package icyllis.modernui.graphics.drawable;

import icyllis.modernui.annotation.*;
import icyllis.modernui.graphics.*;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.util.ColorStateList;
import org.intellij.lang.annotations.MagicConstant;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A {@link Drawable} with a color gradient for buttons, backgrounds, etc.
 */
public class GradientDrawable extends ShapeDrawable {

    /**
     * Gradient is linear (default.)
     *
     * @see icyllis.modernui.graphics.LinearGradient
     */
    public static final int LINEAR_GRADIENT = 0;

    /**
     * @see icyllis.modernui.graphics.RadialGradient
     */
    public static final int RADIAL_GRADIENT = 1;

    /**
     * @see icyllis.modernui.graphics.AngularGradient
     */
    public static final int ANGULAR_GRADIENT = 2;

    /**
     * @hidden
     */
    @MagicConstant(intValues = {LINEAR_GRADIENT, RADIAL_GRADIENT, ANGULAR_GRADIENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface GradientType {
    }

    /**
     * Radius is in pixels.
     */
    public static final int RADIUS_TYPE_PIXELS = 0;

    /**
     * Radius is a fraction of the base size.
     */
    public static final int RADIUS_TYPE_FRACTION = 1;

    /**
     * Radius is a fraction of the bounds size.
     */
    public static final int RADIUS_TYPE_FRACTION_PARENT = 2;

    /**
     * @hidden
     */
    @MagicConstant(intValues = {RADIUS_TYPE_PIXELS, RADIUS_TYPE_FRACTION, RADIUS_TYPE_FRACTION_PARENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RadiusType {
    }

    /**
     * Controls how the gradient is oriented relative to the drawable's bounds
     */
    public enum Orientation {
        /**
         * draw the gradient from the top to the bottom
         */
        TOP_BOTTOM,
        /**
         * draw the gradient from the top-right to the bottom-left
         */
        TR_BL,
        /**
         * draw the gradient from the right to the left
         */
        RIGHT_LEFT,
        /**
         * draw the gradient from the bottom-right to the top-left
         */
        BR_TL,
        /**
         * draw the gradient from the bottom to the top
         */
        BOTTOM_TOP,
        /**
         * draw the gradient from the bottom-left to the top-right
         */
        BL_TR,
        /**
         * draw the gradient from the left to the right
         */
        LEFT_RIGHT,
        /**
         * draw the gradient from the top-left to the bottom-right
         */
        TL_BR,
    }

    /**
     * Default orientation for GradientDrawable
     */
    private static final Orientation DEFAULT_ORIENTATION = Orientation.TOP_BOTTOM;

    private GradientState mGradientState;

    private float mGradientRadius;

    public GradientDrawable() {
        this(new GradientState(DEFAULT_ORIENTATION, null), null);
    }

    /**
     * Create a new gradient drawable given an orientation and an array
     * of colors for the gradient.
     */
    public GradientDrawable(@NonNull Orientation orientation, @Nullable @ColorInt int[] colors) {
        this(new GradientState(orientation, colors), null);
    }

    @Override
    void updateGradient() {
        final GradientState st = mGradientState;

        int[] gradientColors = null;
        if (st.mGradientColors != null) {
            gradientColors = new int[st.mGradientColors.length];
            for (int i = 0; i < gradientColors.length; i++) {
                if (st.mGradientColors[i] != null) {
                    gradientColors[i] = st.mGradientColors[i].getDefaultColor();
                }
            }
        }
        if (gradientColors != null) {
            final RectF r = mRect;
            final float x0, x1, y0, y1;

            if (st.mGradient == LINEAR_GRADIENT) {
                final float level = st.mUseLevel ? getLevel() / (float) MAX_LEVEL : 1.0f;
                switch (st.mOrientation) {
                    case TOP_BOTTOM:
                        x0 = r.left;            y0 = r.top;
                        x1 = x0;                y1 = level * r.bottom;
                        break;
                    case TR_BL:
                        x0 = r.right;           y0 = r.top;
                        x1 = level * r.left;    y1 = level * r.bottom;
                        break;
                    case RIGHT_LEFT:
                        x0 = r.right;           y0 = r.top;
                        x1 = level * r.left;    y1 = y0;
                        break;
                    case BR_TL:
                        x0 = r.right;           y0 = r.bottom;
                        x1 = level * r.left;    y1 = level * r.top;
                        break;
                    case BOTTOM_TOP:
                        x0 = r.left;            y0 = r.bottom;
                        x1 = x0;                y1 = level * r.top;
                        break;
                    case BL_TR:
                        x0 = r.left;            y0 = r.bottom;
                        x1 = level * r.right;   y1 = level * r.top;
                        break;
                    case LEFT_RIGHT:
                        x0 = r.left;            y0 = r.top;
                        x1 = level * r.right;   y1 = y0;
                        break;
                    default:/* TL_BR */
                        x0 = r.left;            y0 = r.top;
                        x1 = level * r.right;   y1 = level * r.bottom;
                        break;
                }

                mFillPaint.setShader(new LinearGradient(x0, y0, x1, y1,
                        gradientColors, st.mPositions, Shader.TileMode.CLAMP, null));
            } else if (st.mGradient == RADIAL_GRADIENT) {
                x0 = r.left + r.width() * st.mCenterX;
                y0 = r.top + r.height() * st.mCenterY;

                float radius = st.mGradientRadius;
                if (st.mGradientRadiusType == RADIUS_TYPE_FRACTION) {
                    // Fall back to parent width or height if intrinsic
                    // size is not specified.
                    final float width = st.mWidth >= 0 ? st.mWidth : r.width();
                    final float height = st.mHeight >= 0 ? st.mHeight : r.height();
                    radius *= Math.min(width, height);
                } else if (st.mGradientRadiusType == RADIUS_TYPE_FRACTION_PARENT) {
                    radius *= Math.min(r.width(), r.height());
                }

                if (st.mUseLevel) {
                    radius *= getLevel() / (float) MAX_LEVEL;
                }

                mGradientRadius = radius;

                if (!(radius > 0)) {
                    // We can't have a shader with non-positive radius, so
                    // let's have a very, very small radius.
                    radius = 0.001f;
                }

                mFillPaint.setShader(new RadialGradient(x0, y0, radius,
                        gradientColors, st.mPositions, Shader.TileMode.CLAMP, null));
            } else if (st.mGradient == ANGULAR_GRADIENT) {
                x0 = r.left + r.width() * st.mCenterX;
                y0 = r.top + r.height() * st.mCenterY;

                float sweep = st.mUseLevel ? (360.0f * getLevel() / MAX_LEVEL) : 360.0f;
                Matrix matrix = new Matrix();
                matrix.setRotate(-90 * MathUtil.DEG_TO_RAD, x0, y0);

                mFillPaint.setShader(new AngularGradient(x0, y0, 0, sweep,
                        gradientColors, st.mPositions, Shader.TileMode.CLAMP, matrix));
            }

            // If we don't have a solid color, the alpha channel must be
            // maxed out so that alpha modulation works correctly.
            if (st.mSolidColors == null) {
                mFillPaint.setColor4f(1f, 1f, 1f, 1f);
            }
        } else {
            // added by Modern UI: clear shader
            mFillPaint.setShader(null);
        }
    }

    /**
     * Sets the type of gradient used by this drawable.
     * <p>
     * <strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.
     *
     * @param gradient The type of the gradient: {@link #LINEAR_GRADIENT},
     *                 {@link #RADIAL_GRADIENT} or {@link #ANGULAR_GRADIENT}
     *
     * @see #mutate()
     * @see #getGradientType()
     */
    public void setGradientType(@GradientType int gradient) {
        mGradientState.setGradientType(gradient);
        mShapeIsDirty = true;
        invalidateSelf();
    }

    /**
     * Returns the type of gradient used by this drawable, one of
     * {@link #LINEAR_GRADIENT}, {@link #RADIAL_GRADIENT}, or
     * {@link #ANGULAR_GRADIENT}.
     *
     * @return the type of gradient used by this drawable
     * @see #setGradientType(int)
     */
    @GradientType
    public int getGradientType() {
        return mGradientState.mGradient;
    }

    /**
     * Sets the position of the center of the gradient as a fraction of the
     * width and height.
     * <p>
     * The default value is (0.5, 0.5).
     * <p>
     * <strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.
     *
     * @param x the X-position of the center of the gradient
     * @param y the Y-position of the center of the gradient
     *
     * @see #mutate()
     * @see #setGradientType(int)
     * @see #getGradientCenterX()
     * @see #getGradientCenterY()
     */
    public void setGradientCenter(float x, float y) {
        mGradientState.setGradientCenter(x, y);
        mShapeIsDirty = true;
        invalidateSelf();
    }

    /**
     * Returns the X-position of the center of the gradient as a fraction of
     * the width.
     *
     * @return the X-position of the center of the gradient
     * @see #setGradientCenter(float, float)
     */
    public float getGradientCenterX() {
        return mGradientState.mCenterX;
    }

    /**
     * Returns the Y-position of the center of this gradient as a fraction of
     * the height.
     *
     * @return the Y-position of the center of the gradient
     * @see #setGradientCenter(float, float)
     */
    public float getGradientCenterY() {
        return mGradientState.mCenterY;
    }

    /**
     * Sets the radius of the gradient. The radius is honored only when the
     * gradient type is set to {@link #RADIAL_GRADIENT}.
     * <p>
     * <strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.
     *
     * @param gradientRadius the radius of the gradient
     * @param type the unit of the radius
     *
     * @see #mutate()
     * @see #setGradientType(int)
     * @see #getGradientRadius()
     */
    public void setGradientRadius(float gradientRadius, @RadiusType int type) {
        mGradientState.setGradientRadius(gradientRadius, type);
        mShapeIsDirty = true;
        invalidateSelf();
    }

    /**
     * Returns the radius of the gradient in pixels. The radius is valid only
     * when the gradient type is set to {@link #RADIAL_GRADIENT}.
     *
     * @return the radius of the gradient in pixels
     * @see #setGradientRadius(float, int)
     */
    public @Px float getGradientRadius() {
        if (mGradientState.mGradient != RADIAL_GRADIENT) {
            return 0;
        }
        if (mGradientState.mGradientRadiusType == RADIUS_TYPE_PIXELS) {
            return mGradientState.mGradientRadius;
        }

        updateRectIsEmpty();
        return mGradientRadius;
    }

    /**
     * Sets whether this drawable's {@code level} property will be used to
     * scale the gradient. If a gradient is not used, this property has no
     * effect.
     * <p>
     * Scaling behavior varies based on gradient type:
     * <ul>
     *     <li>{@link #LINEAR_GRADIENT} adjusts the ending position along the
     *         gradient's axis of orientation (see {@link #getOrientation()})
     *     <li>{@link #RADIAL_GRADIENT} adjusts the outer radius
     *     <li>{@link #ANGULAR_GRADIENT} adjusts the ending angle
     * </ul>
     * <p>
     * The default value for this property is {@code false}.
     * <p>
     * <strong>Note</strong>: Changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.
     *
     * @param useLevel {@code true} if the gradient should be scaled based on
     *                 level, {@code false} otherwise
     *
     * @see #mutate()
     * @see #setLevel(int)
     * @see #getLevel()
     * @see #getUseLevel()
     */
    public void setUseLevel(boolean useLevel) {
        mGradientState.mUseLevel = useLevel;
        mShapeIsDirty = true;
        invalidateSelf();
    }

    /**
     * Returns whether this drawable's {@code level} property will be used to
     * scale the gradient.
     *
     * @return {@code true} if the gradient should be scaled based on level,
     *         {@code false} otherwise
     * @see #setUseLevel(boolean)
     */
    public boolean getUseLevel() {
        return mGradientState.mUseLevel;
    }

    /**
     * Returns the orientation of the gradient defined in this drawable.
     *
     * @return the orientation of the gradient defined in this drawable
     * @see #setOrientation(Orientation)
     */
    @NonNull
    public Orientation getOrientation() {
        return mGradientState.mOrientation;
    }

    /**
     * Sets the orientation of the gradient defined in this drawable.
     * <p>
     * <strong>Note</strong>: changing orientation will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing the orientation.
     *
     * @param orientation the desired orientation (angle) of the gradient
     *
     * @see #mutate()
     * @see #getOrientation()
     */
    public void setOrientation(@NonNull Orientation orientation) {
        mGradientState.mOrientation = orientation;
        mShapeIsDirty = true;
        invalidateSelf();
    }

    /**
     * Sets the colors used to draw the gradient.
     * <p>
     * Each color is specified as an ARGB integer and the array must contain at
     * least 2 colors.
     * <p>
     * <strong>Note</strong>: changing colors will affect all instances of a
     * drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing the colors.
     *
     * @param colors an array containing 2 or more ARGB colors
     * @see #mutate()
     * @see #setColor(int)
     */
    public void setColors(@Nullable @ColorInt int[] colors) {
        setColors(colors, null);
    }

    /**
     * Sets the colors and offsets used to draw the gradient.
     * <p>
     * Each color is specified as an ARGB integer and the array must contain at
     * least 2 colors.
     * <p>
     * <strong>Note</strong>: changing colors will affect all instances of a
     * drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing the colors.
     *
     * @param colors an array containing 2 or more ARGB colors
     * @param offsets optional array of floating point parameters representing the positions
     *                of the colors. Null evenly disperses the colors
     * @see #mutate()
     * @see #setColors(int[])
     */
    public void setColors(@Nullable @ColorInt int[] colors, @Nullable float[] offsets) {
        mGradientState.setGradientColors(colors);
        mGradientState.mPositions = offsets;
        mShapeIsDirty = true;
        invalidateSelf();
    }

    /**
     * Returns the colors used to draw the gradient, or {@code null} if the
     * gradient is drawn using a single color or no colors.
     *
     * @return the colors used to draw the gradient, or {@code null}
     * @see #setColors(int[] colors)
     */
    @Nullable
    public int[] getColors() {
        if (mGradientState.mGradientColors == null) {
            return null;
        } else {
            int[] colors = new int[mGradientState.mGradientColors.length];
            for (int i = 0; i < mGradientState.mGradientColors.length; i++) {
                if (mGradientState.mGradientColors[i] != null) {
                    colors[i] = mGradientState.mGradientColors[i].getDefaultColor();
                }
            }
            return colors;
        }
    }

    /**
     * Sets a hint that indicates if color error may be distributed to smooth color transition.
     * The default value is false.
     */
    public void setDither(boolean dither) {
        if (dither != mGradientState.mDither) {
            mGradientState.mDither = dither;
            invalidateSelf();
        }
    }

    /**
     * Returns true if color error may be distributed to smooth color transition.
     * The default value is false.
     */
    public boolean isDither() {
        return mGradientState.mDither;
    }

    @NonNull
    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mGradientState = new GradientState(mGradientState, null);
            mShapeState = mGradientState;
            updateLocalState(null);
            updateLocalState();
            mMutated = true;
        }
        return this;
    }

    static final class GradientState extends ShapeState {

        @GradientType
        public int mGradient = LINEAR_GRADIENT;

        public int mAngle = 0;
        public Orientation mOrientation;

        public ColorStateList[] mGradientColors; // no support for state-based color
        public float[] mPositions;

        float mCenterX = 0.5f;
        float mCenterY = 0.5f;
        float mGradientRadius = 0.5f;
        @RadiusType
        int mGradientRadiusType = RADIUS_TYPE_FRACTION;
        boolean mUseLevel = false;

        public GradientState(@NonNull Orientation orientation, @Nullable int[] gradientColors) {
            mOrientation = orientation;
            setGradientColors(gradientColors);
        }

        public GradientState(@NonNull GradientState orig, @Nullable Resources res) {
            super(orig, res);
            mGradient = orig.mGradient;
            mAngle = orig.mAngle;
            mOrientation = orig.mOrientation;
            if (orig.mGradientColors != null) {
                mGradientColors = orig.mGradientColors.clone();
            }
            if (orig.mPositions != null) {
                mPositions = orig.mPositions.clone();
            }
            mCenterX = orig.mCenterX;
            mCenterY = orig.mCenterY;
            mGradientRadius = orig.mGradientRadius;
            mGradientRadiusType = orig.mGradientRadiusType;
            mUseLevel = orig.mUseLevel;
        }

        public void setGradientType(@GradientType int gradient) {
            mGradient = gradient;
        }

        public void setGradientCenter(float x, float y) {
            mCenterX = x;
            mCenterY = y;
        }

        public void setGradientColors(@Nullable int[] colors) {
            if (colors == null) {
                mGradientColors = null;
            } else {
                // allocate new CSL array only if the size of the current array is different
                // from the size of the given parameter
                if (mGradientColors == null || mGradientColors.length != colors.length) {
                    mGradientColors = new ColorStateList[colors.length];
                }
                for (int i = 0; i < colors.length; i++) {
                    mGradientColors[i] = ColorStateList.valueOf(colors[i]);
                }
            }
            mSolidColors = null;
            computeOpacity();
        }

        @Override
        public void setSolidColors(@Nullable ColorStateList colors) {
            mGradientColors = null;
            super.setSolidColors(colors);
        }

        @Override
        protected void computeOpacity() {
            mOpaqueOverShape = false;

            if (mGradientColors != null) {
                for (int i = 0; i < mGradientColors.length; i++) {
                    if (mGradientColors[i] != null
                            && (mGradientColors[i].getDefaultColor() >>> 24) != 0xff) {
                        return;
                    }
                }
            }

            // An unfilled shape is not opaque over bounds or shape
            if (mGradientColors == null && mSolidColors == null) {
                return;
            }

            // Colors are opaque, so opaqueOverShape=true,
            mOpaqueOverShape = true;
        }

        public void setGradientRadius(float gradientRadius, @RadiusType int type) {
            mGradientRadius = gradientRadius;
            mGradientRadiusType = type;
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            return new GradientDrawable(this, null);
        }

        @NonNull
        @Override
        public Drawable newDrawable(@Nullable Resources res) {
            return new GradientDrawable(this, res);
        }
    }

    /**
     * Creates a new themed GradientDrawable based on the specified constant state.
     * <p>
     * The resulting drawable is guaranteed to have a new constant state.
     *
     * @param state Constant state from which the drawable inherits
     */
    private GradientDrawable(@NonNull GradientState state, @Nullable Resources res) {
        super(state, res);

        mGradientState = state;

        updateLocalState();
    }

    private void updateLocalState() {
        final GradientState state = mGradientState;

        if (state.mSolidColors == null && state.mGradientColors != null) {
            // Make sure the fill alpha is maxed out.
            mFillPaint.setColor4f(1f, 1f, 1f, 1f);
        }
    }
}
