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
 * geometries and images.
 */
public class Paint {

    // the instance on UI thread
    private static final Paint sInstance = new Paint();

    private static final int STYLE_MASK = 0x3;

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
        FILL_AND_STROKE()
    }

    private int mColor;
    private int mFlags;
    private float mStrokeWidth;
    private float mFeatherRadius;

    public Paint() {
        reset();
    }

    public void reset() {
        mColor = 0xFFFFFFFF;
        mStrokeWidth = 1;
        mFeatherRadius = 1;
    }

    /**
     * Take the paint on UI thread. The paint states will be reset before
     * drawing the whole GUI, but may be modified by your drawing method.
     *
     * @return a paint instance
     */
    @Nonnull
    public static Paint take() {
        return sInstance;
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
    }

    /**
     * Set current paint color, keep previous alpha.
     *
     * @param r red component [0, 255]
     * @param g green component [0, 255]
     * @param b blue component [0, 255]
     */
    public void setRGB(int r, int g, int b) {
        mColor = (mColor & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /**
     * Set current paint color in 0xAARRGGBB format.
     *
     * @param color the color to set
     */
    public void setColor(int color) {
        mColor = color;
    }

    /**
     * Set current paint alpha value in integer form.
     *
     * @param a the new alpha value ranged from 0 to 255
     */
    public void setAlpha(int a) {
        mColor = (mColor & 0xFFFFFF) | (a << 24);
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
     * Return the paint's style, used for controlling how primitives' geometries
     * are interpreted (except images, which always assumes Fill).
     *
     * @return the paint's style setting (Fill, Stroke, StrokeAndFill)
     */
    @Nonnull
    public Style getStyle() {
        return Style.values()[mFlags & STYLE_MASK];
    }

    /**
     * Set the paint's style, used for controlling how primitives' geometries
     * are interpreted (except images, which always assumes Fill).
     *
     * @param style the new style to set in the paint
     */
    public void setStyle(@Nonnull Style style) {
        mFlags = (mFlags & ~STYLE_MASK) | style.ordinal();
    }

    /**
     * Return the width for stroking.
     *
     * @return the paint's stroke width, used whenever the paint's style is
     * Stroke or StrokeAndFill.
     */
    public float getStrokeWidth() {
        return mStrokeWidth;
    }

    /**
     * Set the width for stroking.
     *
     * @param width set the paint's stroke width, used whenever the paint's
     *              style is Stroke or StrokeAndFill.
     */
    public void setStrokeWidth(float width) {
        mStrokeWidth = Math.max(0, width);
    }

    /**
     * Get current feather radius.
     * <p>
     * Feather radius is used to smooth the edges of geometry, and the actual
     * effect varies in draw methods. The default value is 1.0 px.
     *
     * @return feather radius
     * @see #setFeatherRadius(float)
     */
    public float getFeatherRadius() {
        return mFeatherRadius;
    }

    /**
     * Set the feather radius in pixels for this paint.
     * <p>
     * Feather radius is used to smooth the edges of geometry, and the actual
     * effect varies in draw methods. The default value is 1.0 px.
     *
     * @param radius the new feather radius to set
     */
    public void setFeatherRadius(float radius) {
        mFeatherRadius = Math.max(0, radius);
    }
}
