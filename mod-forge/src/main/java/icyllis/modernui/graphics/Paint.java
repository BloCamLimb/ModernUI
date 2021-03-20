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
     * Set current paint alpha in integer form
     *
     * @param a alpha [0,255]
     */
    public void setAlpha(int a) {
        mColor = (mColor & 0xFFFFFF) | (a << 24);
    }

    public int getColor() {
        return mColor;
    }

    @Nonnull
    public Style getStyle() {
        return Style.values()[mFlags & STYLE_MASK];
    }

    public void setStyle(@Nonnull Style style) {
        mFlags = (mFlags & ~STYLE_MASK) | style.ordinal();
    }

    public float getStrokeWidth() {
        return mStrokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        mStrokeWidth = strokeWidth;
    }

    public float getFeatherRadius() {
        return mFeatherRadius;
    }

    public void setFeatherRadius(float featherRadius) {
        mFeatherRadius = featherRadius;
    }
}
