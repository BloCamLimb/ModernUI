/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * {@code Paint} controls options applied when drawing. {@code Paint} collects
 * all options outside the {@code Canvas} clip and {@code Canvas} matrix.
 * such as style and color information, applied to geometries and images.
 * <p>
 * {@code Paint} collects effects and filters that describe single-pass and
 * multiple-pass algorithms that alter the drawing geometry, color, and
 * transparency. For instance, {@code Paint} does not directly implement
 * dashing or blur, but contains the objects that do so.
 */
@SuppressWarnings({"MagicConstant", "unused"})
public class Paint {

    /**
     * Set {@code Style} to fill, stroke, or both fill and stroke geometry.
     * <p>
     * The stroke and fill share all paint attributes; for instance, they are drawn with the same color.
     * Use {@link #STROKE_AND_FILL} to avoid hitting the same pixels twice with a stroke draw and
     * a fill draw. The default is {@link #FILL}.
     */
    @MagicConstant(intValues = {FILL, STROKE, STROKE_AND_FILL, FILL_AND_STROKE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Style {
    }

    /**
     * Geometry drawn with this style will be filled, ignoring all
     * stroke-related settings in the paint.
     */
    public static final int FILL = 0x00;

    /**
     * Geometry drawn with this style will be stroked, respecting
     * the stroke-related fields on the paint.
     */
    public static final int STROKE = 0x01;

    /**
     * Geometry (path) drawn with this style will be both filled and
     * stroked at the same time, respecting the stroke-related fields on
     * the paint. This shares all paint attributes; for instance, they
     * are drawn with the same color. Use this to avoid hitting the same
     * pixels twice with a stroke draw and a fill draw.
     */
    public static final int STROKE_AND_FILL = 0x02;
    public static final int FILL_AND_STROKE = 0x02; // alias

    private static final int STYLE_MASK = 0x03;

    /**
     * The {@code Cap} specifies the treatment for the beginning and ending of
     * stroked lines and paths. The default is {@link #CAP_ROUND}.
     */
    @MagicConstant(intValues = {CAP_BUTT, CAP_ROUND, CAP_SQUARE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Cap {
    }

    /**
     * The stroke ends with the path, and does not project beyond it.
     */
    public static final int CAP_BUTT = 0x00;

    /**
     * The stroke projects out as a semicircle, with the center at the
     * end of the path.
     */
    public static final int CAP_ROUND = 0x04;

    /**
     * The stroke projects out as a square, with the center at the end
     * of the path.
     */
    public static final int CAP_SQUARE = 0x08;

    private static final int CAP_MASK = 0x0C;

    /**
     * The {@code Join} specifies the treatment where lines and curve segments
     * join on a stroked path. The default is {@link #JOIN_ROUND}.
     * <p>
     * {@code Join} affects the four corners of a stroked rectangle, and the connected segments
     * in a stroked path.
     * <p>
     * Choose miter join to draw sharp corners. Choose round join to draw a circle with a
     * radius equal to the stroke width on top of the corner. Choose bevel join to minimally
     * connect the thick strokes.
     * <p>
     * The fill path constructed to describe the stroked path respects the join setting but may
     * not contain the actual join. For instance, a fill path constructed with round joins does
     * not necessarily include circles at each connected segment.
     */
    @MagicConstant(intValues = {JOIN_MITER, JOIN_ROUND, JOIN_BEVEL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Join {
    }

    /**
     * The outer edges of a join meet at a sharp angle
     */
    public static final int JOIN_MITER = 0x00;

    /**
     * The outer edges of a join meet in a circular arc.
     */
    public static final int JOIN_ROUND = 0x10;

    /**
     * The outer edges of a join meet with a straight line
     */
    public static final int JOIN_BEVEL = 0x20;

    private static final int JOIN_MASK = 0x30;

    /**
     * The {@code Align} specifies the treatment where the stroke is placed in relation
     * to the object edge. The default is {@link #ALIGN_CENTER}.
     */
    @MagicConstant(intValues = {ALIGN_CENTER, ALIGN_INSIDE, ALIGN_OUTSIDE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Align {
    }

    /**
     * The stroke is aligned to center.
     */
    public static final int ALIGN_CENTER = 0x00;

    /**
     * The stroke is aligned to inside.
     */
    public static final int ALIGN_INSIDE = 0x40;

    /**
     * The stroke is aligned to outside.
     */
    public static final int ALIGN_OUTSIDE = 0x80;

    private static final int ALIGN_MASK = 0xC0;

    /**
     * The {@code FilterMode} specifies the sampling method on transformed texture images.
     * The default is {@link #FILTER_MODE_LINEAR}.
     */
    @MagicConstant(intValues = {FILTER_MODE_NEAREST, FILTER_MODE_LINEAR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FilterMode {
    }

    /**
     * Single sample point (nearest neighbor).
     */
    public static final int FILTER_MODE_NEAREST = 0x000;

    /**
     * Interpolate between 2x2 sample points (bilinear interpolation).
     */
    public static final int FILTER_MODE_LINEAR = 0x100;

    private static final int FILTER_MODE_MASK = 0x100;

    /**
     * The {@code MipmapMode} specifies the interpolation method for MIP image levels when
     * down-sampling texture images. The default is {@link #MIPMAP_MODE_LINEAR}.
     */
    @MagicConstant(intValues = {MIPMAP_MODE_NONE, MIPMAP_MODE_NEAREST, MIPMAP_MODE_LINEAR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MipmapMode {
    }

    /**
     * Ignore mipmap levels, sample from the "base".
     */
    public static final int MIPMAP_MODE_NONE = 0x000;

    /**
     * Sample from the nearest level.
     */
    public static final int MIPMAP_MODE_NEAREST = 0x200;

    /**
     * Interpolate between the two nearest levels.
     */
    public static final int MIPMAP_MODE_LINEAR = 0x400;

    private static final int MIPMAP_MODE_MASK = 0x600;

    private static final int MAX_ANISOTROPY_SHIFT = 11;
    private static final int MAX_ANISOTROPY_MASK = 0x1F << MAX_ANISOTROPY_SHIFT;

    private static final int ANTI_ALIAS_MASK = 0x10000;
    private static final int DITHER_MASK = 0x20000;

    private static final int DEFAULT_FLAGS = FILL |
            CAP_ROUND | JOIN_ROUND | ALIGN_CENTER |
            FILTER_MODE_LINEAR | MIPMAP_MODE_LINEAR | ANTI_ALIAS_MASK;

    // color components using non-premultiplied alpha
    private float mR; // 0..1
    private float mG; // 0..1
    private float mB; // 0..1
    private float mA; // 0..1

    private float mWidth;       // stroke-width
    private float mMiterLimit;  // stroke-miterlimit
    private float mSmoothWidth;

    // optional objects
    private Shader mShader;
    private Blender mBlender;
    private MaskFilter mMaskFilter;
    private ColorFilter mColorFilter;
    private ImageFilter mImageFilter;

    /*
     * bitfield
     * 24       16       8        0
     * |--------|--------|--------|
     *                          00  FILL             (def)
     *                          01  STROKE
     *                          10  STROKE_AND_FILL
     *                        00    CAP_BUTT
     *                        01    CAP_ROUND        (def)
     *                        10    CAP_SQUARE
     *                      00      JOIN_MITER
     *                      01      JOIN_ROUND       (def)
     *                      10      JOIN_BEVEL
     *                    00        ALIGN_CENTER     (def)
     *                    01        ALIGN_INSIDE
     *                    10        ALIGN_OUTSIDE
     * |--------|--------|--------|
     *                  0           FILTER_NEAREST
     *                  1           FILTER_LINEAR       (def)
     *                00            MIPMAP_MODE_NONE
     *                01            MIPMAP_MODE_NEAREST
     *                11            MIPMAP_MODE_LINEAR  (def)
     *           11111              MAX_ANISOTROPY_MASK (range: 0-16, def: 0)
     * |--------|--------|--------|
     *         1                    ANTI_ALIAS       (def)
     *        1                     DITHER
     * |--------|--------|--------|
     */
    private int mFlags;

    /**
     * Creates a new Paint with defaults.
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
    public Paint(@Nullable Paint paint) {
        set(paint);
    }

    /**
     * Set all contents of this paint to their initial values.
     */
    public void reset() {
        mR = 1;
        mG = 1;
        mB = 1;
        mA = 1; // opaque white
        mWidth = 2;
        mMiterLimit = 4;
        mSmoothWidth = 0;
        mShader = null;
        mBlender = null;
        mMaskFilter = null;
        mColorFilter = null;
        mImageFilter = null;
        mFlags = DEFAULT_FLAGS;
    }

    /**
     * Set all contents of this paint from the specified paint.
     *
     * @param paint the paint to set this paint from
     */
    public void set(Paint paint) {
        if (paint == null) {
            reset();
        } else {
            mR = paint.mR;
            mG = paint.mG;
            mB = paint.mB;
            mA = paint.mA;
            mWidth = paint.mWidth;
            mMiterLimit = paint.mMiterLimit;
            mSmoothWidth = paint.mSmoothWidth;
            mShader = paint.mShader;
            mBlender = paint.mBlender;
            mMaskFilter = paint.mMaskFilter;
            mColorFilter = paint.mColorFilter;
            mImageFilter = paint.mImageFilter;
            mFlags = paint.mFlags;
        }
    }

    ///// Solid Color

    /**
     * Return the paint's solid color in sRGB. Note that the color is a 32-bit value
     * containing alpha as well as r,g,b. This 32-bit value is not premultiplied,
     * meaning that its alpha can be any value, regardless of the values of r,g,b.
     *
     * @return the paint's color (and alpha).
     */
    public int getColor() {
        return ((int) (mA * 255.0f + 0.5f) << 24) |
                ((int) (mR * 255.0f + 0.5f) << 16) |
                ((int) (mG * 255.0f + 0.5f) << 8) |
                (int) (mB * 255.0f + 0.5f);
    }

    /**
     * Set the paint's solid color in sRGB. Note that the color is a 32-bit value
     * containing alpha as well as r,g,b. This 32-bit value is not premultiplied,
     * meaning that its alpha can be any value, regardless of the values of r,g,b.
     *
     * @param color the new color (including alpha) to set in the paint.
     */
    public void setColor(int color) {
        mR = ((color >> 16) & 0xff) / 255.0f;
        mG = ((color >> 8) & 0xff) / 255.0f;
        mB = (color & 0xff) / 255.0f;
        mA = (color >>> 24) / 255.0f;
    }

    /**
     * Retrieves alpha/opacity from the color used when stroking and filling.
     *
     * @return alpha ranging from zero, fully transparent, to one, fully opaque
     */
    public float getAlphaF() {
        return mA;
    }

    /**
     * Helper to getColor() that just returns the color's alpha value. This is
     * the same as calling getColor() >>> 24. It always returns a value between
     * 0 (completely transparent) and 255 (completely opaque).
     *
     * @return the alpha component of the paint's color.
     */
    public int getAlpha() {
        return (int) (mA * 255.0f + 0.5f);
    }

    /**
     * Replaces alpha, leaving RGB unchanged.
     * <code>a</code> is a value from 0.0 to 1.0.
     * <code>a</code> set to 0.0 makes color fully transparent;
     * <code>a</code> set to 1.0 makes color fully opaque.
     *
     * @param a the alpha component [0..1] of the paint's color
     */
    public void setAlphaF(float a) {
        mA = MathUtil.clamp(a, 0.0f, 1.0f);
    }

    /**
     * Helper to setColor(), that only assigns the color's alpha value,
     * leaving its r,g,b values unchanged.
     *
     * @param a the alpha component [0..255] of the paint's color
     */
    public void setAlpha(int a) {
        mA = MathUtil.clamp(a / 255.0f, 0.0f, 1.0f);
    }

    /**
     * Returns the value of the red component.
     *
     * @see #a()
     * @see #g()
     * @see #b()
     */
    public final float r() {
        return mR;
    }

    /**
     * Returns the value of the green component.
     *
     * @see #a()
     * @see #r()
     * @see #b()
     */
    public final float g() {
        return mG;
    }

    /**
     * Returns the value of the blue component.
     *
     * @see #a()
     * @see #r()
     * @see #g()
     */
    public final float b() {
        return mB;
    }

    /**
     * Returns the value of the alpha component.
     *
     * @see #a()
     * @see #r()
     * @see #g()
     */
    public final float a() {
        return mA;
    }

    /**
     * Helper to setColor(), that only assigns the color's <code>r,g,b</code> values,
     * leaving its alpha value unchanged.
     *
     * @param r the new red component (0..255) of the paint's color.
     * @param g the new green component (0..255) of the paint's color.
     * @param b the new blue component (0..255) of the paint's color.
     */
    public final void setRGB(int r, int g, int b) {
        mR = MathUtil.clamp(r / 255.0f, 0.0f, 1.0f);
        mG = MathUtil.clamp(g / 255.0f, 0.0f, 1.0f);
        mB = MathUtil.clamp(b / 255.0f, 0.0f, 1.0f);
    }

    /**
     * Helper to setColor(), that only assigns the color's <code>r,g,b</code> values,
     * leaving its alpha value unchanged.
     *
     * @param r the new red component (0..1) of the paint's color.
     * @param g the new green component (0..1) of the paint's color.
     * @param b the new blue component (0..1) of the paint's color.
     */
    public final void setRGB(float r, float g, float b) {
        mR = MathUtil.clamp(r, 0.0f, 1.0f);
        mG = MathUtil.clamp(g, 0.0f, 1.0f);
        mB = MathUtil.clamp(b, 0.0f, 1.0f);
    }

    /**
     * Helper to setColor(), that takes <code>r,g,b,a</code> and constructs the color int.
     *
     * @param r the new red component (0..255) of the paint's color.
     * @param g the new green component (0..255) of the paint's color.
     * @param b the new blue component (0..255) of the paint's color.
     * @param a the new alpha component (0..255) of the paint's color.
     */
    public final void setRGBA(int r, int g, int b, int a) {
        mR = MathUtil.clamp(r / 255.0f, 0.0f, 1.0f);
        mG = MathUtil.clamp(g / 255.0f, 0.0f, 1.0f);
        mB = MathUtil.clamp(b / 255.0f, 0.0f, 1.0f);
        mA = MathUtil.clamp(a / 255.0f, 0.0f, 1.0f);
    }

    /**
     * Helper to setColor(), that takes floating point <code>r,g,b,a</code> values.
     *
     * @param r the new red component (0..1) of the paint's color.
     * @param g the new green component (0..1) of the paint's color.
     * @param b the new blue component (0..1) of the paint's color.
     * @param a the new alpha component (0..1) of the paint's color.
     */
    public final void setRGBA(float r, float g, float b, float a) {
        mR = MathUtil.clamp(r, 0.0f, 1.0f);
        mG = MathUtil.clamp(g, 0.0f, 1.0f);
        mB = MathUtil.clamp(b, 0.0f, 1.0f);
        mA = MathUtil.clamp(a, 0.0f, 1.0f);
    }

    /**
     * Helper to setColor(), that takes <code>a,r,g,b</code> and constructs the color int.
     *
     * @param a the new alpha component (0..255) of the paint's color.
     * @param r the new red component (0..255) of the paint's color.
     * @param g the new green component (0..255) of the paint's color.
     * @param b the new blue component (0..255) of the paint's color.
     */
    public void setARGB(int a, int r, int g, int b) {
        mR = MathUtil.clamp(r / 255.0f, 0.0f, 1.0f);
        mG = MathUtil.clamp(g / 255.0f, 0.0f, 1.0f);
        mB = MathUtil.clamp(b / 255.0f, 0.0f, 1.0f);
        mA = MathUtil.clamp(a / 255.0f, 0.0f, 1.0f);
    }

    /**
     * Helper to setColor(), that takes floating point <code>a,r,g,b</code> values.
     *
     * @param r the new red component (0..1) of the paint's color.
     * @param g the new green component (0..1) of the paint's color.
     * @param b the new blue component (0..1) of the paint's color.
     * @param a the new alpha component (0..1) of the paint's color.
     */
    public void setARGB(float a, float r, float g, float b) {
        mR = MathUtil.clamp(r, 0.0f, 1.0f);
        mG = MathUtil.clamp(g, 0.0f, 1.0f);
        mB = MathUtil.clamp(b, 0.0f, 1.0f);
        mA = MathUtil.clamp(a, 0.0f, 1.0f);
    }

    ///// Basic Flags

    /**
     * Returns true if distance-to-edge anti-aliasing should be used.
     * If true, the AA step is calculated in screen space.
     * The default value is true.
     *
     * @return anti-aliasing state
     * @see #setAntiAlias(boolean)
     */
    public final boolean isAntiAlias() {
        return (mFlags & ANTI_ALIAS_MASK) != 0;
    }

    /**
     * Sets a hint that indicates if distance-to-edge anti-aliasing should be used.
     * If true, the AA step is calculated in screen space.
     * The default value is true.
     *
     * @param aa setting for anti-aliasing
     * @see #setSmoothWidth(float)
     */
    public final void setAntiAlias(boolean aa) {
        if (aa) {
            mFlags |= ANTI_ALIAS_MASK;
        } else {
            mFlags &= ~ANTI_ALIAS_MASK;
        }
    }

    /**
     * Returns true if color error may be distributed to smooth color transition.
     * An implementation may use a look-up table to do dithering. The default value is false.
     *
     * @return dithering state
     * @see #setDither(boolean)
     */
    public final boolean isDither() {
        return (mFlags & DITHER_MASK) != 0;
    }

    /**
     * Sets a hint that indicates if color error may be distributed to smooth color transition.
     * An implementation may use a look-up table to do dithering. The default value is false.
     *
     * @param dither setting for dithering
     */
    public final void setDither(boolean dither) {
        if (dither) {
            mFlags |= DITHER_MASK;
        } else {
            mFlags &= ~DITHER_MASK;
        }
    }

    /**
     * Returns the paint's style, used for controlling how primitives' geometries
     * are interpreted, except where noted. The default is {@link #FILL}.
     *
     * @return the paint's style setting (fill, stroke or both)
     * @see #setStyle(int)
     */
    @Style
    public int getStyle() {
        return mFlags & STYLE_MASK;
    }

    /**
     * Sets the paint's style, used for controlling how primitives' geometries
     * are interpreted, except where noted. The default is {@link #FILL}.
     *
     * @param style the new style to set in the paint
     */
    public void setStyle(@Style int style) {
        mFlags = (mFlags & ~STYLE_MASK) | (style & STYLE_MASK);
    }

    ///// Stroke Parameters

    /**
     * Sets paint's style to STROKE if true, or FILL if false.
     *
     * @param stroke true to stroke shapes, false to fill shapes
     */
    public final void setStroke(boolean stroke) {
        mFlags = (mFlags & ~STYLE_MASK) | (stroke ? STROKE : FILL);
    }

    /**
     * Returns the paint's cap type, controlling how the start and end of stroked
     * lines and paths are treated, except where noted.
     * The default is {@link #CAP_ROUND}.
     *
     * @return the line cap style for the paint
     * @see #setStrokeCap(int)
     */
    @Cap
    public int getStrokeCap() {
        return mFlags & CAP_MASK;
    }

    /**
     * Sets the paint's cap type, controlling how the start and end of stroked
     * lines and paths are treated, except where noted.
     * The default is {@link #CAP_ROUND}.
     *
     * @param cap set the paint's line cap style
     */
    public void setStrokeCap(@Cap int cap) {
        mFlags = (mFlags & ~CAP_MASK) | (cap & CAP_MASK);
    }

    /**
     * Returns the paint's stroke join type. The default is {@link #JOIN_ROUND}.
     *
     * @return the paint's Join
     * @see #setStrokeJoin(int)
     */
    @Join
    public int getStrokeJoin() {
        return mFlags & JOIN_MASK;
    }

    /**
     * Sets the paint's stroke join type. The default is {@link #JOIN_ROUND}.
     *
     * @param join set the paint's Join
     */
    public void setStrokeJoin(@Join int join) {
        mFlags = (mFlags & ~JOIN_MASK) | (join & JOIN_MASK);
    }

    /**
     * Returns the paint's stroke align type. The default is {@link #ALIGN_CENTER}.
     *
     * @return the paint's Align
     * @see #setStrokeAlign(int)
     */
    @Align
    public final int getStrokeAlign() {
        return mFlags & ALIGN_MASK;
    }

    /**
     * Sets the paint's stroke align type. The default is {@link #ALIGN_CENTER}.
     *
     * @param align set the paint's Align
     */
    public final void setStrokeAlign(@Align int align) {
        mFlags = (mFlags & ~ALIGN_MASK) | (align & ALIGN_MASK);
    }

    /**
     * Returns the thickness of the pen for stroking shapes. The default value is 2.0 px.
     *
     * @return the paint's stroke width; zero for hairline, greater than zero for pen thickness
     * @see #setStrokeWidth(float)
     */
    public float getStrokeWidth() {
        return mWidth;
    }

    /**
     * Sets the thickness of the pen for stroking shapes. The default value is 2.0 px.
     * A stroke width of zero is treated as "hairline" width. Hairlines are always exactly one
     * pixel wide in screen space (their thickness does not change as the canvas is scaled).
     *
     * @param width set the paint's stroke width; zero for hairline, greater than zero for pen thickness
     */
    public void setStrokeWidth(float width) {
        mWidth = Math.max(width, 0);
    }

    /**
     * Returns the miter limit at which a sharp corner is drawn beveled.
     * The default value is 4.0 px.
     *
     * @return zero and greater miter limit
     * @see #setStrokeMiter(float)
     */
    public float getStrokeMiter() {
        return mMiterLimit;
    }

    /**
     * Sets the miter limit at which a sharp corner is drawn beveled.
     * The default value is 4.0 px.
     *
     * @param miter zero and greater miter limit
     */
    public void setStrokeMiter(float miter) {
        mMiterLimit = Math.max(miter, 0);
    }

    /**
     * Returns the current smooth width. The default value is 0.0 px.
     *
     * @return the paint's smooth width, zero or greater
     * @see #setSmoothWidth(float)
     */
    public final float getSmoothWidth() {
        return mSmoothWidth;
    }

    /**
     * Sets the smooth width in pixels for this paint. The default value is 0.0 px.
     * <p>
     * A non-zero smooth width applies to Hermite interpolation of geometries' edges
     * in local space. Otherwise, if anti-aliasing is requested, the width will be
     * resulted from the L2-norm of derivatives in screen space. Thus, the
     * anti-aliasing will be ignored when smooth width is greater than zero.
     *
     * @param smooth the paint's smooth width, zero or greater
     * @see #setAntiAlias(boolean)
     */
    public final void setSmoothWidth(float smooth) {
        mSmoothWidth = Math.max(smooth, 0);
    }

    ///// Sampler Parameters

    /**
     * Returns the bilinear interpolation for sampling texture images. True means
     * {@link #FILTER_MODE_LINEAR}, while false means {@link #FILTER_MODE_NEAREST}.
     * The value is ignored when anisotropic filtering is used. The default value
     * is true.
     *
     * @return whether to use bilinear filter on images, or nearest neighbor
     * @see #setFilter(boolean)
     */
    public final boolean isFilter() {
        return (mFlags & FILTER_MODE_MASK) != 0;
    }

    /**
     * Set the bilinear interpolation for sampling texture images. True means
     * {@link #FILTER_MODE_LINEAR}, while false means {@link #FILTER_MODE_NEAREST}.
     * Calling this method disables anisotropic filtering. The default value
     * is true.
     *
     * @param filter whether to use bilinear filter on images, or nearest neighbor
     * @see #isFilter()
     */
    public final void setFilter(boolean filter) {
        if (filter) {
            mFlags = (mFlags | FILTER_MODE_LINEAR) & ~MAX_ANISOTROPY_MASK;
        } else {
            mFlags &= ~(FILTER_MODE_MASK | MAX_ANISOTROPY_MASK);
        }
    }

    /**
     * Returns the current filter. The default is {@link #FILTER_MODE_LINEAR}.
     * The value is ignored when anisotropic filtering is used.
     *
     * @return the current filter
     * @see #setFilterMode(int)
     */
    @FilterMode
    public final int getFilterMode() {
        return mFlags & FILTER_MODE_MASK;
    }

    /**
     * Set the interpolation method for sampling texture images.
     * The default is {@link #FILTER_MODE_LINEAR}.
     * Calling this method does NOT affect anisotropic filtering.
     *
     * @param filter the paint's filter
     * @see #getFilterMode()
     */
    public final void setFilterMode(@FilterMode int filter) {
        mFlags = (mFlags & ~FILTER_MODE_MASK) | (filter & FILTER_MODE_MASK);
    }

    /**
     * Returns the mipmap mode. The value is ignored when anisotropic filtering is used.
     * The default is {@link #MIPMAP_MODE_LINEAR}.
     *
     * @return the mipmap mode
     */
    @MipmapMode
    public final int getMipmapMode() {
        return mFlags & MIPMAP_MODE_MASK;
    }

    /**
     * Set the mipmap mode for sampling texture images. The value is ignored when
     * anisotropic filtering is used. The default is {@link #MIPMAP_MODE_LINEAR}.
     *
     * @param mipmap the mipmap mode
     */
    public final void setMipmapMode(@MipmapMode int mipmap) {
        mFlags = (mFlags & ~MIPMAP_MODE_MASK) | (mipmap & MIPMAP_MODE_MASK);
    }

    /**
     * Returns whether the anisotropic filtering is enabled.
     * If enabled, {@link #getFilterMode()} and {@link #getMipmapMode()} are ignored.
     *
     * @return true if anisotropic filtering will be used for sampling images.
     */
    public final boolean isAnisotropy() {
        return (mFlags & MAX_ANISOTROPY_MASK) != 0;
    }

    /**
     * Returns the maximum level of anisotropic filtering. The default value is 0 (OFF).
     *
     * @return the max anisotropy
     */
    public final int getMaxAnisotropy() {
        return (mFlags & MAX_ANISOTROPY_MASK) >> MAX_ANISOTROPY_SHIFT;
    }

    /**
     * Set the maximum level of anisotropic filtering. A value greater than 0 means
     * anisotropic filtering is used, and {@link #getFilterMode()} and {@link #getMipmapMode()}
     * are ignored. If the device does not support anisotropic filtering, this method will
     * be downgraded to bilinear filtering. The default value is 0 (OFF).
     *
     * @param maxAnisotropy the max anisotropy level ranged from 0 to 16
     */
    public final void setMaxAnisotropy(int maxAnisotropy) {
        mFlags = (mFlags & ~MAX_ANISOTROPY_MASK) |
                (MathUtil.clamp(maxAnisotropy, 0, 16) << MAX_ANISOTROPY_SHIFT);
    }

    ///// Effects

    /**
     * Returns optional colors used when filling a path, such as a gradient.
     *
     * @return Shader if previously set, null otherwise
     */
    @Nullable
    public Shader getShader() {
        return mShader;
    }

    /**
     * Sets optional colors used when filling a path, such as a gradient.
     *
     * @param shader how geometry is filled with color; if null, solid color is used instead
     */
    public void setShader(@Nullable Shader shader) {
        mShader = shader;
    }

    /**
     * Returns ColorFilter if set, or null.
     *
     * @return ColorFilter if previously set, null otherwise
     */
    @Nullable
    public ColorFilter getColorFilter() {
        return mColorFilter;
    }

    /**
     * Sets ColorFilter to filter, pass null to clear ColorFilter.
     *
     * @param colorFilter ColorFilter to apply to subsequent draw
     */
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mColorFilter = colorFilter;
    }

    /**
     * Returns true if BlendMode is {@link BlendMode#SRC_OVER}, the default.
     *
     * @return true if BlendMode is {@link BlendMode#SRC_OVER}
     */
    public final boolean isSrcOver() {
        return mBlender == null || mBlender.asBlendMode() == BlendMode.SRC_OVER;
    }

    /**
     * Returns the user-supplied blend function, if one has been set.
     * <p>
     * A null blender signifies the default {@link BlendMode#SRC_OVER} behavior.
     *
     * @return the blender assigned to this paint, otherwise null
     * @see #setBlender(Blender)
     */
    @Nullable
    public final Blender getBlender() {
        return mBlender;
    }

    /**
     * Sets the current blender.
     * <p>
     * A null blender signifies the default {@link BlendMode#SRC_OVER} behavior.
     * <p>
     * For convenience, you can pass {@link BlendMode} if the blend effect can be
     * expressed as one of those values. A blend mode defines how source pixels
     * (generated by a drawing command) are composited with the destination pixels
     * (content of the render target).
     *
     * @param blender the blender to be installed in the paint, may be null
     * @see #getBlender()
     */
    public final void setBlender(@Nullable Blender blender) {
        mBlender = blender;
    }

    /**
     * Returns MaskFilter if set, or null.
     *
     * @return MaskFilter if previously set, null otherwise
     */
    @Nullable
    public MaskFilter getMaskFilter() {
        return mMaskFilter;
    }

    /**
     * Sets MaskFilter, pass null to clear MaskFilter and leave MaskFilter effect on
     * mask alpha unaltered.
     *
     * @param maskFilter modifies clipping mask generated from drawn geometry
     */
    public void setMaskFilter(@Nullable MaskFilter maskFilter) {
        mMaskFilter = maskFilter;
    }

    /**
     * Returns ImageFilter if set, or null.
     *
     * @return ImageFilter if previously set, null otherwise
     */
    @Nullable
    public ImageFilter getImageFilter() {
        return mImageFilter;
    }

    /**
     * Sets ImageFilter, pass null to clear ImageFilter, and remove ImageFilter effect
     * on drawing.
     *
     * @param imageFilter how Image is sampled when transformed
     */
    public void setImageFilter(@Nullable ImageFilter imageFilter) {
        mImageFilter = imageFilter;
    }

    ///// Utility

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
        var mode = getBlendModeDirect(this);
        if (mode != null) {
            switch (mode) {
                case SRC_OVER, SRC_ATOP, DST_OUT, DST_OVER, PLUS -> {
                    if (getAlpha() == 0) {
                        return !isBlendedImageFilter(mImageFilter);
                    }
                }
                case DST -> {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if Paint does not include elements requiring extensive computation
     * to compute BaseDevice bounds of drawn geometry.
     *
     * @return true if Paint allows for fast computation of bounds
     */
    @ApiStatus.Internal
    public final boolean canComputeFastBounds() {
        return mImageFilter == null || mImageFilter.canComputeFastBounds();
    }

    /**
     * Only call this if canComputeFastBounds() returned true. This takes a
     * raw rectangle (the raw bounds of a shape), and adjusts it for stylistic
     * effects in the paint (e.g. stroking). If needed, it uses the storage
     * parameter. It returns the adjusted bounds that can then be used
     * for {@link Canvas#quickReject(Rect2f)} tests.
     * <p>
     * This method ensures that orig will not be modified, and the result
     * will always be stored into the storage rect.
     *
     * @param orig    geometry modified by Paint when drawn
     * @param storage fast computed bounds of geometry
     */
    @ApiStatus.Internal
    public final void computeFastBounds(Rect2f orig, Rect2f storage) {
        int style = getStyle();
        // ultra fast-case: filling with no effects that affect geometry
        if (style == FILL) {
            if (mMaskFilter == null && mImageFilter == null) {
                storage.set(orig);
                return;
            }
        }

        storage.set(orig);

        int align = getStrokeAlign();
        if (style != FILL && align != ALIGN_INSIDE && mWidth > 0) {
            // since we're stroked, outset the rect by the radius (and join type, caps)
            float multiplier = 1;
            if (getStrokeJoin() == JOIN_MITER) {
                multiplier = Math.max(multiplier, mMiterLimit);
            }
            if (getStrokeCap() == CAP_SQUARE) {
                multiplier = Math.max(multiplier, MathUtil.SQRT2);
            }
            // width or radius
            float stroke = mWidth * multiplier;
            if (align == ALIGN_CENTER) {
                stroke *= 0.5f;
            } // ALIGN_OUTSIDE
            storage.inset(-stroke, -stroke);
        }

        if (mMaskFilter != null) {
            mMaskFilter.computeFastBounds(storage, storage);
        }

        if (mImageFilter != null) {
            mImageFilter.computeFastBounds(storage, storage);
        }
    }

    @Override
    public int hashCode() {
        int result = mFlags;
        result = 31 * result + Float.hashCode(mR);
        result = 31 * result + Float.hashCode(mG);
        result = 31 * result + Float.hashCode(mB);
        result = 31 * result + Float.hashCode(mA);
        result = 31 * result + Float.hashCode(mWidth);
        result = 31 * result + Float.hashCode(mMiterLimit);
        result = 31 * result + Float.hashCode(mSmoothWidth);
        result = 31 * result + Objects.hashCode(mShader);
        result = 31 * result + Objects.hashCode(mBlender);
        result = 31 * result + Objects.hashCode(mMaskFilter);
        result = 31 * result + Objects.hashCode(mColorFilter);
        result = 31 * result + Objects.hashCode(mImageFilter);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Paint paint)) return false;
        return equals(paint);
    }

    protected final boolean equals(Paint paint) {
        return mFlags == paint.mFlags &&
                mR == paint.mR &&
                mG == paint.mG &&
                mB == paint.mB &&
                mA == paint.mA &&
                mWidth == paint.mWidth &&
                mMiterLimit == paint.mMiterLimit &&
                mSmoothWidth == paint.mSmoothWidth &&
                Objects.equals(mShader, paint.mShader) &&
                Objects.equals(mBlender, paint.mBlender) &&
                Objects.equals(mMaskFilter, paint.mMaskFilter) &&
                Objects.equals(mColorFilter, paint.mColorFilter) &&
                Objects.equals(mImageFilter, paint.mImageFilter);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Paint{");
        s.append("mColor4f=(");
        s.append(mR);
        s.append(", ");
        s.append(mG);
        s.append(", ");
        s.append(mB);
        s.append(", ");
        s.append(mA);
        int style = getStyle();
        s.append("), mStyle=");
        if (style == FILL) {
            s.append("FILL");
        } else if (style == STROKE) {
            s.append("STROKE");
        } else {
            s.append("FILL|STROKE");
        }
        int cap = getStrokeCap();
        s.append(", mCap=");
        if (cap == CAP_ROUND) {
            s.append("ROUND");
        } else if (cap == CAP_SQUARE) {
            s.append("SQUARE");
        } else {
            s.append("BUTT");
        }
        int join = getStrokeJoin();
        s.append(", mJoin=");
        if (join == JOIN_ROUND) {
            s.append("ROUND");
        } else if (join == JOIN_BEVEL) {
            s.append("BEVEL");
        } else {
            s.append("MITER");
        }
        int align = getStrokeAlign();
        s.append(", mAlign=");
        if (align == ALIGN_CENTER) {
            s.append("CENTER");
        } else if (align == ALIGN_INSIDE) {
            s.append("INSIDE");
        } else {
            s.append("OUTSIDE");
        }
        s.append(", mAntiAlias=");
        s.append(isAntiAlias());
        s.append(", mDither=");
        s.append(isDither());
        s.append(", mStrokeWidth=");
        s.append(mWidth);
        s.append(", mStrokeMiter=");
        s.append(mMiterLimit);
        s.append(", mSmoothWidth=");
        s.append(mSmoothWidth);
        s.append(", mShader=");
        s.append(mShader);
        s.append(", mBlender=");
        s.append(mBlender);
        s.append(", mMaskFilter=");
        s.append(mMaskFilter);
        s.append(", mColorFilter=");
        s.append(mColorFilter);
        s.append(", mImageFilter=");
        s.append(mImageFilter);
        s.append('}');
        return s.toString();
    }

    public static int getAlphaDirect(@Nullable Paint paint) {
        return paint != null ? paint.getAlpha() : 0xFF;
    }

    public static BlendMode getBlendModeDirect(@Nullable Paint paint) {
        if (paint != null) {
            var blender = paint.getBlender();
            if (blender != null) {
                return blender.asBlendMode();
            }
        }
        return BlendMode.SRC_OVER;
    }

    public static boolean isBlendedShader(@Nullable Shader shader) {
        return shader != null && !shader.isOpaque();
    }

    public static boolean isBlendedColorFilter(@Nullable ColorFilter filter) {
        return filter != null && !filter.isAlphaUnchanged();
    }

    public static boolean isBlendedImageFilter(@Nullable ImageFilter filter) {
        //TODO: check if we should allow image filters to broadcast that they don't affect alpha
        // just like color filters
        return filter != null;
    }

    public static boolean isOpaquePaint(@Nullable Paint paint) {
        if (paint == null) {
            return true;
        }
        if (paint.getAlpha() != 0xFF ||
                isBlendedShader(paint.mShader) ||
                isBlendedColorFilter(paint.mColorFilter)) {
            return false;
        }
        // Only let simple srcOver / src blending modes declare opaque, since behavior is clear.
        var mode = getBlendModeDirect(paint);
        return mode == BlendMode.SRC_OVER || mode == BlendMode.SRC;
    }
}
