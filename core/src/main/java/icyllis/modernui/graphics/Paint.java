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

import javax.annotation.Nonnull;

/**
 * The Paint class holds the style and color information about how to draw
 * geometries and images. Use with Canvas.
 */
public class Paint {

    private static final ThreadLocal<Paint> TLS = ThreadLocal.withInitial(Paint::new);

    private static final int STYLE_MASK = 0x3;

    private static final int MULTI_COLOR = 0x4;

    /**
     * The Style specifies if the primitive being drawn is filled, stroked, or
     * both (in the same color). The default is FILL.
     */
    public enum Style {
        /**
         * Geometry drawn with this style will be filled, ignoring all
         * stroke-related settings in the paint.
         */
        FILL(),

        /**
         * Geometry drawn with this style will be stroked, respecting
         * the stroke-related fields on the paint.
         */
        STROKE(),

        /**
         * Geometry drawn with this style will be both filled and
         * stroked at the same time, respecting the stroke-related fields on
         * the paint. This mode can give unexpected results if the geometry
         * is oriented counter-clockwise. This restriction does not apply to
         * either FILL or STROKE.
         */
        FILL_AND_STROKE();

        private static final Style[] VALUES = values();
    }

    private int mColor;
    private int mFlags;
    private float mStrokeWidth;
    private float mSmoothRadius;

    private final int[] mColors = new int[4];

    /**
     * Creates a new Paint.
     *
     * @see #take()
     */
    public Paint() {
        reset();
    }

    /**
     * Reset the paint to defaults.
     */
    public void reset() {
        mColor = ~0;
        mFlags = 0;
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
     *     paint.reset(); // the drawable may use the paint
     *     paint.setColor(mColorB);
     *     canvas.drawRect(mRectB, paint);
     * }
     * </pre>
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
     *
     * @param r red component [0, 255]
     * @param g green component [0, 255]
     * @param b blue component [0, 255]
     * @param a alpha component [0, 255]
     */
    public void setRGBA(int r, int g, int b, int a) {
        mColor = (a << 24) | (r << 16) | (g << 8) | b;
        mFlags &= ~MULTI_COLOR;
    }

    /**
     * Set current paint color, with unchanged alpha.
     *
     * @param r red component [0, 255]
     * @param g green component [0, 255]
     * @param b blue component [0, 255]
     */
    public void setRGB(int r, int g, int b) {
        mColor = (mColor & 0xFF000000) | (r << 16) | (g << 8) | b;
        mFlags &= ~MULTI_COLOR;
    }

    /**
     * Set current paint color in 0xAARRGGBB format. The default color is white.
     *
     * @param color the color to set
     */
    public void setColor(int color) {
        mColor = color;
        mFlags &= ~MULTI_COLOR;
    }

    /**
     * Set current paint alpha value in integer form.
     *
     * @param a the new alpha value ranged from 0 to 255
     */
    public void setAlpha(int a) {
        mColor = (mColor & 0xFFFFFF) | (a << 24);
        mFlags &= ~MULTI_COLOR;
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
     * Set the colors in ARGB and enable multi color mode. When enabled, a primitive
     * should use the colors sequentially, and {@link #setColor(int)} is ignored. You can
     * use this to make gradient effect or edge fading in one pass, without shaders.
     * <p>
     * A Paint object has an unique private array storing these values, a copy of given array
     * will be used. The colors are used in the order of top left, top right, bottom right
     * and bottom left.
     * <p>
     * If the length of given array is less than 4, then rest color values are undefined.
     * If greater than 4, then rest values are ignored.
     * <p>
     * By default, this mode is disabled. Calling other methods like {@link #setColor(int)}
     * or {@link #setAlpha(int)} disables the mode as well.
     *
     * @param colors a list of sequential colors, maximum count is 4
     * @see #isMultiColor()
     */
    public void setColors(@Nonnull int[] colors) {
        int l = Math.min(colors.length, mColors.length);
        System.arraycopy(colors, 0, mColors, 0, l);
        mFlags |= MULTI_COLOR;
    }

    /**
     * Returns the backing array of the multi colors. Each call will return
     * the same array object. Do not modify the elements of the array.
     *
     * @return the backing array of the multi colors
     * @see #setColors(int[])
     */
    @Nonnull
    public int[] getColors() {
        return mColors;
    }

    /**
     * Returns whether multiple colors is used.
     *
     * @return whether multi color mode is enabled
     * @see #setColors(int[])
     */
    public boolean isMultiColor() {
        return (mFlags & MULTI_COLOR) != 0;
    }

    /**
     * Return the paint's style, used for controlling how primitives' geometries
     * are interpreted (except where noted).
     *
     * @return the paint's style setting (Fill, Stroke, StrokeAndFill)
     */
    @Nonnull
    public Style getStyle() {
        return Style.VALUES[mFlags & STYLE_MASK];
    }

    /**
     * Set the paint's style, used for controlling how primitives' geometries
     * are interpreted (exclude images, they are always filled).
     *
     * @param style the new style to set in the paint
     */
    public void setStyle(@Nonnull Style style) {
        mFlags = (mFlags & ~STYLE_MASK) | style.ordinal();
    }

    /**
     * Return the width for stroking. The default value is 2.0 px.
     *
     * @return the paint's stroke width, used whenever the paint's style is
     * Stroke or StrokeAndFill.
     */
    public float getStrokeWidth() {
        return mStrokeWidth;
    }

    /**
     * Set the width for stroking. The default value is 2.0 px.
     *
     * @param width set the paint's stroke width, used whenever the paint's
     *              style is Stroke or StrokeAndFill.
     */
    public void setStrokeWidth(float width) {
        mStrokeWidth = Math.max(0, width);
    }

    /**
     * Get current smooth radius.
     * <p>
     * Smooth radius is used to smooth and blur the edges of geometry. The default value is 2.0 px.
     *
     * @return feather radius
     * @see #setSmoothRadius(float)
     */
    public float getSmoothRadius() {
        return mSmoothRadius;
    }

    /**
     * Set the smooth radius in pixels for this paint.
     * <p>
     * Smooth radius is used to smooth and blur the edges of geometry. The default value is 2.0 px.
     * This value may be ignored by implementation.
     *
     * @param radius the new feather radius to set
     */
    public void setSmoothRadius(float radius) {
        mSmoothRadius = Math.max(0, radius);
    }
}
