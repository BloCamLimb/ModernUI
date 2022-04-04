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

import icyllis.modernui.annotation.ColorInt;
import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The Paint class holds the style and color information about how to draw
 * geometries and images.
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

    private static final int STYLE_MASK = 0x3;
    private static final int GRADIENT_MASK = 0x4;
    private static final int BLEND_MODE_SHIFT = 4;
    private static final int BLEND_MODE_MASK = 0xFF << BLEND_MODE_SHIFT;

    private static final int DEFAULT_FLAGS = FILL | (BlendMode.toValue(BlendMode.SRC_OVER) << BLEND_MODE_SHIFT);

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
        mColor = ~0;
        mFlags = DEFAULT_FLAGS;
        mStrokeWidth = 2;
        mSmoothRadius = 2;
    }

    /**
     * <b>WARNING: This method may be abused.</b>
     * <p>
     * Returns a thread-local paint, the paint will be reset before return.
     * This method is designed for temporary operations that avoid creating
     * new objects, for a drawing operation that is not after this method or
     * {@link #reset()}, the state of the paint may be modified by system.
     * <p>
     * For example:
     * <pre>{@code
     * @Override
     * protected void onDraw(Canvas canvas) {
     *     var paint = Paint.take();
     *     paint.setColor(mColorA);
     *     canvas.drawRect(mRectA, paint);
     *
     *     doSomeActions(); // call any other methods except with Canvas
     *
     *     paint.reset(); // not sure if they will use the paint, so reset it
     *     paint.setColor(mColorB);
     *     canvas.drawRect(mRectB, paint);
     * }
     * }</pre>
     * The API implementation requires that any method in {@link Canvas} must NOT
     * modify the state of the Paint instance obtained by this method.
     *
     * @return a thread-local paint
     */
    @Nonnull
    public static Paint take() {
        Paint paint = TLS.get();
        paint.reset();
        return paint;
    }

    /**
     * Set the paint's color. Note that the color is an int containing alpha
     * as well as r,g,b. This 32bit value is not premultiplied, meaning that
     * its alpha can be any value, regardless of the values of r,g,b.
     * See the Color class for more details.
     *
     * @param color The new color (including alpha) to set in the paint.
     */
    public void setColor(@ColorInt int color) {
        mColor = color;
        mFlags &= ~GRADIENT_MASK;
    }

    /**
     * Return the paint's color in sRGB. Note that the color is a 32bit value
     * containing alpha as well as r,g,b. This 32bit value is not premultiplied,
     * meaning that its alpha can be any value, regardless of the values of
     * r,g,b. See the Color class for more details.
     *
     * @return the paint's color (and alpha).
     */
    @ColorInt
    public int getColor() {
        return mColor;
    }

    /**
     * Helper to setColor(), that only assigns the color's alpha value,
     * leaving its r,g,b values unchanged. Results are undefined if the alpha
     * value is outside the range [0..255]
     *
     * @param a set the alpha component [0..255] of the paint's color.
     */
    public void setAlpha(int a) {
        mColor = (mColor << 8 >>> 8) | (a << 24);
        mFlags &= ~GRADIENT_MASK;
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
     * Helper to setColor(), that only assigns the color's r,g,b values,
     * leaving its alpha value unchanged.
     *
     * @param r The new red component (0..255) of the paint's color.
     * @param g The new green component (0..255) of the paint's color.
     * @param b The new blue component (0..255) of the paint's color.
     */
    public void setRGB(int r, int g, int b) {
        mColor = (mColor >>> 24 << 24) | (r << 16) | (g << 8) | b;
        mFlags &= ~GRADIENT_MASK;
    }

    /**
     * Helper to setColor(), that takes r,g,b,a and constructs the color int
     *
     * @param r The new red component (0..255) of the paint's color.
     * @param g The new green component (0..255) of the paint's color.
     * @param b The new blue component (0..255) of the paint's color.
     * @param a The new alpha component (0..255) of the paint's color.
     */
    public void setRGBA(int r, int g, int b, int a) {
        setColor(Color.argb(a, r, g, b));
    }

    /**
     * Helper to setColor(), that takes a,r,g,b and constructs the color int
     *
     * @param a The new alpha component (0..255) of the paint's color.
     * @param r The new red component (0..255) of the paint's color.
     * @param g The new green component (0..255) of the paint's color.
     * @param b The new blue component (0..255) of the paint's color.
     */
    public void setARGB(int a, int r, int g, int b) {
        setColor(Color.argb(a, r, g, b));
    }

    /**
     * Set the colors in ARGB and enable gradient mode. When enabled, a primitive should
     * use the colors sequentially (in Z shape), and {@link #setColor(int)} is ignored.
     * You can use this to make gradient effect or edge fading effect in one pass,
     * without post-processing shaders.
     * <p>
     * A Paint object has a backing array storing these values, then a copy of the parameter
     * array will be used. The colors are used in the order of top left, top right, bottom left
     * and bottom right, like letter "Z".
     * <p>
     * If the length of the given array is less than 4, then rest color values will use the
     * last color in the given array. If greater than 4, then rest values are ignored.
     * <p>
     * By default, this mode is disabled. Calling other methods like {@link #setColor(int)}
     * or {@link #setAlpha(int)} will disable the mode.
     *
     * @param colors an array of sequential colors
     * @see #setColors(int, int, int, int)
     * @see #isGradient()
     */
    public void setColors(@ColorInt int[] colors) {
        if (colors == null) {
            return;
        }
        int len = colors.length;
        if (len == 0) {
            return;
        }
        if (mColors == null) {
            mColors = new int[4];
        }
        if (len < 4) {
            System.arraycopy(colors, 0, mColors, 0, len);
            for (int i = len; i < 4; i++) {
                mColors[i] = colors[len - 1];
            }
        } else {
            System.arraycopy(colors, 0, mColors, 0, 4);
        }
        mFlags |= GRADIENT_MASK;
    }

    /**
     * Set the colors in ARGB and enable gradient mode. When enabled, a primitive should
     * use the colors sequentially (in Z shape), and {@link #setColor(int)} is ignored.
     * You can use this to make gradient effect or edge fading effect in one pass,
     * without post-processing shaders.
     *
     * @param tl the top-left color
     * @param tr the top-right color
     * @param bl the bottom-left color
     * @param br the bottom-right color
     * @see #setColors(int[])
     * @see #isGradient()
     */
    public void setColors(@ColorInt int tl, @ColorInt int tr, @ColorInt int bl, @ColorInt int br) {
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
     * <p>
     * When a round cap is installed, the half of the stroke width will be used as
     * the stroke radius by analytic geometry.
     *
     * @return the paint's stroke width, used whenever the paint's style is {@link Paint#STROKE}
     */
    public float getStrokeWidth() {
        return mStrokeWidth;
    }

    /**
     * Set the width for stroking. The default value is 2.0 px.
     * <p>
     * When a round cap is installed, the half of the stroke width will be used as
     * the stroke radius by analytic geometry.
     *
     * @param width set the paint's stroke width, used whenever the paint's
     *              style is {@link Paint#STROKE}
     */
    public void setStrokeWidth(float width) {
        mStrokeWidth = Math.max(0, width);
    }

    /**
     * Return the current smooth radius. The default value is 2.0 px.
     * <p>
     * Smooth radius is used to smooth or blur the edge of primitives by analytic geometry.
     * It looks like anti-aliasing, but it's not limited to one pixel.
     *
     * @return the paint's smooth radius
     * @see #setSmoothRadius(float)
     */
    public float getSmoothRadius() {
        return mSmoothRadius;
    }

    /**
     * Set the smooth radius in pixels for this paint. The default value is 2.0 px.
     * <p>
     * Smooth radius is used to smooth or blur the edge of primitives by analytic geometry.
     * It looks like anti-aliasing, but it's not limited to one pixel.
     *
     * @param radius the paint's smooth radius
     */
    public void setSmoothRadius(float radius) {
        mSmoothRadius = Math.max(0, radius);
    }

    /**
     * Set or clear the blend mode. A blend mode defines how source pixels
     * (generated by a drawing command) are composited with the destination pixels
     * (content of the render target).
     * <p>
     * Pass null to clear any previous blend mode.
     *
     * @param blendMode the blend mode to be installed in the paint, may be null
     * @see BlendMode
     */
    public void setBlendMode(@Nullable BlendMode blendMode) {
        int value = BlendMode.toValue(blendMode == null ? BlendMode.SRC_OVER : blendMode) << BLEND_MODE_SHIFT;
        mFlags = (mFlags & ~BLEND_MODE_MASK) | value;
    }

    /**
     * Get the paint's blend mode. By default, returns {@link BlendMode#SRC_OVER}.
     *
     * @return the paint's blend mode used to combine source color with destination color
     */
    @Nonnull
    public BlendMode getBlendMode() {
        return BlendMode.fromValue((mFlags & BLEND_MODE_MASK) >> BLEND_MODE_SHIFT);
    }

    /**
     * Returns true if the paint prevents all drawing; otherwise, the paint may or
     * may not allow drawing.
     * <p>
     * Returns true if, for example, blend mode combined with alpha computes a
     * new alpha of zero.
     *
     * @return true if the paint prevents all drawing
     */
    public boolean nothingToDraw() {
        switch (getBlendMode()) {
            case SRC_OVER, SRC_ATOP, DST_OUT, DST_OVER, PLUS -> {
                return getAlpha() == 0;
            }
            case DST -> {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Paint{");
        if (isGradient()) {
            s.append("colors=[#");
            s.append(Integer.toHexString(mColors[0]));
            s.append(", #");
            s.append(Integer.toHexString(mColors[1]));
            s.append(", #");
            s.append(Integer.toHexString(mColors[2]));
            s.append(", #");
            s.append(Integer.toHexString(mColors[3]));
            s.append(']');
        } else {
            s.append("color=#");
            s.append(Integer.toHexString(mColor));
        }
        int style = getStyle();
        s.append(", style=");
        if (style == FILL) {
            s.append("FILL");
        } else if (style == STROKE) {
            s.append("STROKE");
        } else {
            s.append("FILL|STROKE");
        }
        s.append(", blendMode=");
        s.append(getBlendMode());
        s.append(", strokeWidth=");
        s.append(mStrokeWidth);
        s.append(", smoothRadius=");
        s.append(mSmoothRadius);
        s.append('}');
        return s.toString();
    }

    public static int getAlphaDirect(@Nullable Paint paint) {
        return paint != null ? paint.getAlpha() : 0xFF;
    }

    @Nonnull
    public static BlendMode getBlendModeDirect(@Nullable Paint paint) {
        return paint != null ? paint.getBlendMode() : BlendMode.SRC_OVER;
    }

    public static boolean isOpaquePaint(@Nullable Paint paint) {
        if (paint == null) {
            return true;
        }
        if (paint.getAlpha() != 0xFF) {
            return false;
        }
        BlendMode mode = paint.getBlendMode();
        return mode == BlendMode.SRC_OVER || mode == BlendMode.SRC;
    }
}
