/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;

/**
 * The Paint class holds the style and color information about how to draw
 * geometries and images. Use with Canvas.
 */
public class Paint {

    private static final ThreadLocal<Paint> TLS = ThreadLocal.withInitial(Paint::new);

    /**
     * Geometry drawn with this style will be filled, ignoring all
     * stroke-related settings in the paint.
     */
    public static final int FILL = 0x0;

    /**
     * Geometry drawn with this style will be stroked, respecting
     * the stroke-related fields on the paint.
     */
    public static final int STROKE = 0x1;

    private static final int STYLE_MASK = FILL | STROKE;
    private static final int GRADIENT_MASK = 0x2;

    private int mColor;
    private int mFlags;
    private float mStrokeWidth;
    private float mSmoothRadius;

    private int[] mColors;

    /**
     * Creates a new Paint with defaults.
     *
     * @see #take()
     */
    public Paint() {
        reset();
    }

    /**
     * Create a new paint, initialized with the attributes in the specified
     * paint parameter.
     *
     * @param paint Existing paint used to initialize the attributes of the
     *              new paint.
     */
    public Paint(@Nonnull Paint paint) {
        mColor = paint.mColor;
        mFlags = paint.mFlags;
        mStrokeWidth = paint.mStrokeWidth;
        mSmoothRadius = paint.mSmoothRadius;
        if (paint.mColors != null) {
            setColors(paint.mColors);
        }
    }

    /**
     * Set the paint to defaults.
     */
    public void reset() {
        mColor = 0xFFFFFFFF;
        mFlags = FILL;
        mStrokeWidth = 2;
        mSmoothRadius = 2;
    }

    /**
     * Returns the thread-local paint, the paint will be reset before return.
     * This method is designed for temporary operations, for a drawing operation
     * that is not after this method or {@link #reset()}, the state of the paint
     * may be modified.
     * <p>
     * For example:
     * <pre>
     * &#64;Override
     * protected void onDraw(Canvas canvas) {
     *     var paint = Paint.take();
     *     paint.setColor(mColorA);
     *     canvas.drawRect(mRectA, paint);
     *     mDrawable.draw(canvas);
     *     // the drawable may use the paint, so reset it
     *     paint.reset();
     *     paint.setColor(mColorB);
     *     canvas.drawRect(mRectB, paint);
     * }
     * </pre>
     * The API implementation requires that any method in {@link Canvas} must NOT
     * modify the state of Paint.
     *
     * @return the thread-local paint
     */
    @Nonnull
    public static Paint take() {
        Paint paint = TLS.get();
        paint.reset();
        return paint;
    }

    /**
     * Set current paint color with alpha.
     * Disable gradient colors if enabled.
     *
     * @param r red component [0, 255]
     * @param g green component [0, 255]
     * @param b blue component [0, 255]
     * @param a alpha component [0, 255]
     */
    public void setRGBA(int r, int g, int b, int a) {
        mColor = (a << 24) | (r << 16) | (g << 8) | b;
        mFlags &= ~GRADIENT_MASK;
    }

    /**
     * Set current paint color, with unchanged alpha.
     * Disable gradient colors if enabled.
     *
     * @param r red component [0, 255]
     * @param g green component [0, 255]
     * @param b blue component [0, 255]
     */
    public void setRGB(int r, int g, int b) {
        mColor = (mColor & 0xFF000000) | (r << 16) | (g << 8) | b;
        mFlags &= ~GRADIENT_MASK;
    }

    /**
     * Set current paint color in 0xAARRGGBB format. The default color is white.
     * Disable gradient colors if enabled.
     *
     * @param color the color to set
     */
    public void setColor(int color) {
        mColor = color;
        mFlags &= ~GRADIENT_MASK;
    }

    /**
     * Set current paint alpha value in integer form.
     * If gradient colors are used, set alpha for all colors.
     *
     * @param a the new alpha value ranged from 0 to 255
     */
    public void setAlpha(int a) {
        if (isGradient()) {
            a <<= 24;
            for (int i = 0; i < 4; i++) {
                mColors[i] = (mColors[i] & 0xFFFFFF) | a;
            }
        } else {
            mColor = (mColor & 0xFFFFFF) | (a << 24);
        }
    }

    /**
     * Return the paint's color in ARGB. Note that the color is a 32bit value
     * containing alpha as well as r,g,b. This 32bit value is not premultiplied,
     * meaning that its alpha can be any value, regardless of the values of
     * r,g,b.
     *
     * @return the paint's color (and alpha).
     */
    public int getColor() {
        return mColor;
    }

    /**
     * Helper to getColor() that just returns the color's alpha value. This is
     * the same as calling getColor() >>> 24. It always returns a value between
     * 0 (completely transparent) and 255 (completely opaque).
     *
     * @return the alpha component of the paint's color.
     */
    public int getAlpha() {
        return mColor >>> 24;
    }

    /**
     * Set the colors in ARGB and enable gradient mode. When enabled, a primitive
     * should use the colors sequentially, and {@link #setColor(int)} is ignored. You can
     * use this to make gradient effect or edge fading in one pass, without shaders.
     * <p>
     * A Paint object has a unique private array storing these values, a copy of given array
     * will be used. The colors are used in the order of top left, top right, bottom left
     * and bottom right, like "Z".
     * <p>
     * If the length of given array is less than 4, then rest color values are undefined.
     * If greater than 4, then rest values are ignored.
     * <p>
     * By default, this mode is disabled. Calling other methods like {@link #setColor(int)}
     * except {@link #setAlpha(int)} disables the mode as well.
     *
     * @param colors a list of sequential colors, maximum count is 4
     * @see #setColors(int, int, int, int)
     * @see #isGradient()
     */
    public void setColors(@Nonnull int[] colors) {
        if (mColors == null) {
            mColors = new int[4];
        }
        System.arraycopy(colors, 0, mColors, 0, Math.min(colors.length, 4));
        mFlags |= GRADIENT_MASK;
    }

    /**
     * Set the colors in ARGB and enable gradient mode. When enabled, a primitive
     * should use the colors sequentially, and {@link #setColor(int)} is ignored. You can
     * use this to make gradient effect or edge fading in one pass, without shaders.
     *
     * @param tl the top-left color
     * @param tr the top-right color
     * @param bl the bottom-left color
     * @param br the bottom-right color
     * @see #setColors(int[])
     * @see #isGradient()
     */
    public void setColors(int tl, int tr, int bl, int br) {
        if (mColors == null) {
            mColors = new int[4];
        }
        mColors[0] = tl;
        mColors[1] = tr;
        mColors[2] = bl;
        mColors[3] = br;
        mFlags |= GRADIENT_MASK;
    }

    /**
     * Returns the backing array of the gradient colors. Each call will return
     * the same array object. Do not modify the elements of the array.
     *
     * @return the backing array of the gradient colors, may be null
     * @see #setColors(int[])
     */
    public int[] getColors() {
        return mColors;
    }

    /**
     * Returns whether gradient colors are used.
     *
     * @return whether gradient mode is enabled
     * @see #setColors(int[])
     */
    public boolean isGradient() {
        return (mFlags & GRADIENT_MASK) != 0;
    }

    /**
     * Return the paint's style, used for controlling how primitives' geometries
     * are interpreted, except where noted.
     *
     * @return the paint's style setting (Fill, Stroke)
     */
    public int getStyle() {
        return mFlags & STYLE_MASK;
    }

    /**
     * Set the paint's style, used for controlling how primitives' geometries
     * are interpreted, except where noted.
     *
     * @param style the new style to set in the paint
     */
    public void setStyle(@MagicConstant(intValues = {FILL, STROKE}) int style) {
        mFlags = (mFlags & ~STYLE_MASK) | style;
    }

    /**
     * Return the width for stroking. The default value is 2.0 px.
     *
     * @return the paint's stroke width, used whenever the paint's style is {@link Paint#STROKE}
     */
    public float getStrokeWidth() {
        return mStrokeWidth;
    }

    /**
     * Set the width for stroking. The default value is 2.0 px.
     *
     * @param width set the paint's stroke width, used whenever the paint's
     *              style is {@link Paint#STROKE}
     */
    public void setStrokeWidth(float width) {
        mStrokeWidth = Math.max(0, width);
    }

    /**
     * Return the current smooth radius.
     * <p>
     * Smooth radius is used to smooth and blur the edges of geometry by analytic geometry.
     * The default value is 2.0 px.
     *
     * @return feather radius
     * @see #setSmoothRadius(float)
     */
    public float getSmoothRadius() {
        return mSmoothRadius;
    }

    /**
     * Set the smooth radius in pixels for this paint. This method is not a substitute for
     * anti-aliasing and is rarely used.
     * <p>
     * Smooth radius is used to smooth and blur the edges of geometry by analytic geometry.
     * The default value is 2.0 px. This value may be ignored by some primitives.
     *
     * @param radius the new feather radius to set
     */
    public void setSmoothRadius(float radius) {
        mSmoothRadius = Math.max(0, radius);
    }
}
