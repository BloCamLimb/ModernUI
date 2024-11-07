/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.effects.ColorFilter;
import icyllis.arc3d.core.shaders.Shader;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * {@code Paint} controls options applied when drawing. {@code Paint} collects
 * all options outside the {@code Canvas} clip and {@code Canvas} matrix,
 * such as style and color information, applied to geometries and images.
 * <p>
 * {@code Paint} collects effects and filters that describe single-pass and
 * multiple-pass algorithms that alter the drawing geometry, color, and
 * transparency. For instance, {@code Paint} does not directly implement
 * dashing or blur, but contains the objects that do so.
 * <p>
 * A {@code Paint} object must be closed or reset if it has {@link Shader},
 * {@link ColorFilter}, or {@link Blender} installed.
 */
@SuppressWarnings({"MagicConstant", "unused"})
public class Paint implements AutoCloseable {

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
    public static final int FILL = 0;

    /**
     * Geometry drawn with this style will be stroked, respecting
     * the stroke-related fields on the paint.
     */
    public static final int STROKE = 1;

    /**
     * Geometry (path) drawn with this style will be both filled and
     * stroked at the same time, respecting the stroke-related fields on
     * the paint. This shares all paint attributes; for instance, they
     * are drawn with the same color. Use this to avoid hitting the same
     * pixels twice with a stroke draw and a fill draw.
     */
    public static final int STROKE_AND_FILL = 2;
    public static final int FILL_AND_STROKE = 2; // alias

    private static final int STYLE_SHIFT = 0;
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
    public static final int CAP_BUTT = 0;

    /**
     * The stroke projects out as a semicircle, with the center at the
     * end of the path.
     */
    public static final int CAP_ROUND = 1;

    /**
     * The stroke projects out as a square, with the center at the end
     * of the path.
     */
    public static final int CAP_SQUARE = 2;

    /**
     * The number of cap types.
     */
    @ApiStatus.Internal
    public static final int CAP_COUNT = 3;

    private static final int CAP_SHIFT = 2;
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
    public static final int JOIN_MITER = 0;

    /**
     * The outer edges of a join meet in a circular arc.
     */
    public static final int JOIN_ROUND = 1;

    /**
     * The outer edges of a join meet with a straight line
     */
    public static final int JOIN_BEVEL = 2;

    /**
     * The number of join types.
     */
    @ApiStatus.Internal
    public static final int JOIN_COUNT = 3;

    private static final int JOIN_SHIFT = 4;
    private static final int JOIN_MASK = 0x30;

    /**
     * The {@code Align} specifies the treatment where the stroke is placed in relation
     * to the object edge, this only applies to closed contours. The default is
     * {@link #ALIGN_CENTER}.
     */
    @MagicConstant(intValues = {ALIGN_CENTER, ALIGN_INSIDE, ALIGN_OUTSIDE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Align {
    }

    /**
     * The stroke is aligned to center.
     */
    public static final int ALIGN_CENTER = 0;

    /**
     * The stroke is aligned to inside.
     */
    public static final int ALIGN_INSIDE = 1;

    /**
     * The stroke is aligned to outside.
     */
    public static final int ALIGN_OUTSIDE = 2;

    /**
     * The number of align types.
     */
    @ApiStatus.Internal
    public static final int ALIGN_COUNT = 3;

    private static final int ALIGN_SHIFT = 6;
    private static final int ALIGN_MASK = 0xC0;

    private static final int ANTI_ALIAS_MASK = 0x10000;
    private static final int DITHER_MASK = 0x20000;

    private static final int DEFAULT_FLAGS = (FILL << STYLE_SHIFT) |
            (CAP_ROUND << CAP_SHIFT) | (JOIN_ROUND << JOIN_SHIFT) |
            (ALIGN_CENTER << ALIGN_SHIFT) | ANTI_ALIAS_MASK;

    // color components using non-premultiplied alpha
    private float mR; // 0..1
    private float mG; // 0..1
    private float mB; // 0..1
    private float mA; // 0..1

    private float mWidth;       // stroke-width
    private float mMiterLimit;  // stroke-miterlimit

    // optional objects
    private PathEffect mPathEffect;
    @SharedPtr
    private Shader mShader;
    @SharedPtr
    private ColorFilter mColorFilter;
    @SharedPtr
    private Blender mBlender;

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
        mR = 1.0f;
        mG = 1.0f;
        mB = 1.0f;
        mA = 1.0f; // opaque white
        mWidth = 1.0f;
        mMiterLimit = 4.0f;
        mPathEffect = null;
        mShader = RefCnt.move(mShader);
        mColorFilter = RefCnt.move(mColorFilter);
        mBlender = RefCnt.move(mBlender);
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
            mPathEffect = paint.mPathEffect;
            mShader = RefCnt.create(mShader, paint.mShader);
            mColorFilter = RefCnt.create(mColorFilter, paint.mColorFilter);
            mBlender = RefCnt.create(mBlender, paint.mBlender);
            mFlags = paint.mFlags;
        }
    }

    @Override
    public void close() {
        mShader = RefCnt.move(mShader);
        mColorFilter = RefCnt.move(mColorFilter);
        mBlender = RefCnt.move(mBlender);
    }

    ///// Solid Color

    /**
     * Return the paint's solid color in sRGB. Note that the color is a 32-bit value
     * containing alpha as well as r,g,b. This 32-bit value is not premultiplied,
     * meaning that its alpha can be any value, regardless of the values of r,g,b.
     *
     * @return the paint's color (and alpha).
     */
    @ColorInt
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
    public void setColor(@ColorInt int color) {
        mA = (color >>> 24) * (1 / 255.0f);
        mR = ((color >> 16) & 0xff) * (1 / 255.0f);
        mG = ((color >> 8) & 0xff) * (1 / 255.0f);
        mB = (color & 0xff) * (1 / 255.0f);
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
     * Returns the color used when stroking and filling.
     * Color is stored in <var>dst</var> array in sRGB space, un-premultiplied form.
     *
     * @param dst an array that receives R,G,B,A color components
     */
    public final void getColor4f(@Nonnull @Size(4) float[] dst) {
        dst[0] = mR;
        dst[1] = mG;
        dst[2] = mB;
        dst[3] = mA;
    }

    /**
     * Sets alpha and RGB used when stroking and filling. The color is four floating
     * point values, un-premultiplied. The color values are interpreted as being in
     * the sRGB color space.
     *
     * @param r the new red component (0..1) of the paint's color.
     * @param g the new green component (0..1) of the paint's color.
     * @param b the new blue component (0..1) of the paint's color.
     * @param a the new alpha component (0..1) of the paint's color.
     */
    public final void setColor4f(float r, float g, float b, float a) {
        mR = MathUtil.pin(r, 0.0f, 1.0f);
        mG = MathUtil.pin(g, 0.0f, 1.0f);
        mB = MathUtil.pin(b, 0.0f, 1.0f);
        mA = MathUtil.pin(a, 0.0f, 1.0f);
    }

    /**
     * Sets alpha and RGB used when stroking and filling. The color is four floating
     * point values, un-premultiplied. The color values are interpreted as being in
     * the colorSpace. If colorSpace is null, then color is assumed to be in the
     * sRGB color space.
     *
     * @param r          the new red component of the paint's color.
     * @param g          the new green component of the paint's color.
     * @param b          the new blue component of the paint's color.
     * @param a          the new alpha component (0..1) of the paint's color.
     * @param colorSpace ColorSpace describing the encoding of color
     */
    public final void setColor4f(float r, float g, float b, float a,
                                 @Nullable ColorSpace colorSpace) {
        if (colorSpace != null && !colorSpace.isSrgb()) {
            var c = ColorSpace.connect(colorSpace).transform(
                    r, g, b
            );
            setColor4f(c[0], c[1], c[2], a);
        } else {
            setColor4f(r, g, b, a);
        }
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
        mA = MathUtil.pin(a, 0.0f, 1.0f);
    }

    /**
     * Helper to setColor(), that only assigns the color's alpha value,
     * leaving its r,g,b values unchanged.
     *
     * @param a the alpha component [0..255] of the paint's color
     */
    public void setAlpha(int a) {
        mA = MathUtil.pin(a * (1 / 255.0f), 0.0f, 1.0f);
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
        mR = MathUtil.pin(r * (1 / 255.0f), 0.0f, 1.0f);
        mG = MathUtil.pin(g * (1 / 255.0f), 0.0f, 1.0f);
        mB = MathUtil.pin(b * (1 / 255.0f), 0.0f, 1.0f);
    }

    /**
     * Sets color used when drawing solid fills. The color components range from 0 to 255.
     * The color is un-premultiplied; alpha sets the transparency independent of RGB.
     *
     * @param r amount of red, from no red (0) to full red (255)
     * @param g amount of green, from no green (0) to full green (255)
     * @param b amount of blue, from no blue (0) to full blue (255)
     * @param a amount of alpha, from fully transparent (0) to fully opaque (255)
     */
    public final void setRGBA(int r, int g, int b, int a) {
        mR = MathUtil.pin(r * (1 / 255.0f), 0.0f, 1.0f);
        mG = MathUtil.pin(g * (1 / 255.0f), 0.0f, 1.0f);
        mB = MathUtil.pin(b * (1 / 255.0f), 0.0f, 1.0f);
        mA = MathUtil.pin(a * (1 / 255.0f), 0.0f, 1.0f);
    }

    /**
     * Sets color used when drawing solid fills. The color components range from 0 to 255.
     * The color is un-premultiplied; alpha sets the transparency independent of RGB.
     *
     * @param a amount of alpha, from fully transparent (0) to fully opaque (255)
     * @param r amount of red, from no red (0) to full red (255)
     * @param g amount of green, from no green (0) to full green (255)
     * @param b amount of blue, from no blue (0) to full blue (255)
     */
    public void setARGB(int a, int r, int g, int b) {
        mA = MathUtil.pin(a * (1 / 255.0f), 0.0f, 1.0f);
        mR = MathUtil.pin(r * (1 / 255.0f), 0.0f, 1.0f);
        mG = MathUtil.pin(g * (1 / 255.0f), 0.0f, 1.0f);
        mB = MathUtil.pin(b * (1 / 255.0f), 0.0f, 1.0f);
    }

    ///// Basic Flags

    /**
     * Returns true if antialiasing should be used.
     * The default value is true.
     *
     * @return anti-aliasing state
     * @see #setAntiAlias(boolean)
     */
    public final boolean isAntiAlias() {
        return (mFlags & ANTI_ALIAS_MASK) != 0;
    }

    /**
     * Sets a hint that indicates if antialiasing should be used. An implementation
     * may use analytic method by computing geometry's coverage, distance-to-edge
     * method by computing signed distance field, or multisampling to do antialiasing.
     * If true, the AA step is calculated in screen space. The default value is true.
     *
     * @param aa setting for anti-aliasing
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
     * An implementation may use a bayer matrix or blue noise texture to do dithering.
     * The default value is false.
     *
     * @return dithering state
     * @see #setDither(boolean)
     */
    public final boolean isDither() {
        return (mFlags & DITHER_MASK) != 0;
    }

    /**
     * Sets a hint that indicates if color error may be distributed to smooth color transition.
     * An implementation may use a bayer matrix or blue noise texture to do dithering.
     * The default value is false.
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
        return (mFlags & STYLE_MASK) >>> STYLE_SHIFT;
    }

    /**
     * Sets the paint's style, used for controlling how primitives' geometries
     * are interpreted, except where noted. The default is {@link #FILL}.
     *
     * @param style the new style to set in the paint
     */
    public void setStyle(@Style int style) {
        mFlags = (mFlags & ~STYLE_MASK) | ((style << STYLE_SHIFT) & STYLE_MASK);
    }

    ///// Stroke Parameters

    /**
     * Sets paint's style to STROKE if true, or FILL if false.
     *
     * @param stroke true to stroke shapes, false to fill shapes
     */
    public final void setStroke(boolean stroke) {
        mFlags = (mFlags & ~STYLE_MASK) | (stroke ? (STROKE << STYLE_SHIFT) : (FILL << STYLE_SHIFT));
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
        return (mFlags & CAP_MASK) >>> CAP_SHIFT;
    }

    /**
     * Sets the paint's cap type, controlling how the start and end of stroked
     * lines and paths are treated, except where noted.
     * The default is {@link #CAP_ROUND}.
     *
     * @param cap set the paint's line cap style
     */
    public void setStrokeCap(@Cap int cap) {
        mFlags = (mFlags & ~CAP_MASK) | ((cap << CAP_SHIFT) & CAP_MASK);
    }

    /**
     * Returns the paint's stroke join type. The default is {@link #JOIN_ROUND}.
     *
     * @return the paint's Join
     * @see #setStrokeJoin(int)
     */
    @Join
    public int getStrokeJoin() {
        return (mFlags & JOIN_MASK) >>> JOIN_SHIFT;
    }

    /**
     * Sets the paint's stroke join type. The default is {@link #JOIN_ROUND}.
     *
     * @param join set the paint's Join
     */
    public void setStrokeJoin(@Join int join) {
        mFlags = (mFlags & ~JOIN_MASK) | ((join << JOIN_SHIFT) & JOIN_MASK);
    }

    /**
     * Returns the paint's stroke align type. The default is {@link #ALIGN_CENTER}.
     * Note that this only applies to closed contours, otherwise stroking behaves
     * as {@link #ALIGN_CENTER}.
     *
     * @return the paint's Align
     * @see #setStrokeAlign(int)
     */
    @Align
    public final int getStrokeAlign() {
        return (mFlags & ALIGN_MASK) >>> ALIGN_SHIFT;
    }

    /**
     * Sets the paint's stroke align type. The default is {@link #ALIGN_CENTER}.
     * Note that this only applies to closed contours, otherwise stroking behaves
     * as {@link #ALIGN_CENTER}.
     *
     * @param align set the paint's Align
     */
    public final void setStrokeAlign(@Align int align) {
        mFlags = (mFlags & ~ALIGN_MASK) | ((align << ALIGN_SHIFT) & ALIGN_MASK);
    }

    /**
     * Returns the thickness of the pen for stroking shapes. The default value is 1.0 px.
     *
     * @return the paint's stroke width; zero for hairline, greater than zero for pen thickness
     * @see #setStrokeWidth(float)
     */
    public float getStrokeWidth() {
        return mWidth;
    }

    /**
     * Sets the thickness of the pen for stroking shapes. The default value is 1.0 px.
     * A stroke width of zero is treated as "hairline" width. Hairlines are always exactly one
     * pixel wide in screen space (their thickness does not change as the canvas is scaled).
     *
     * @param width set the paint's stroke width; zero for hairline, greater than zero for pen thickness
     */
    public void setStrokeWidth(float width) {
        if (width >= 0) {
            // do not use Math.max(), also capture NaN
            mWidth = width;
        }
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
        if (miter >= 0) {
            // do not use Math.max(), also capture NaN
            mMiterLimit = miter;
        }
    }

    ///// Effects

    /**
     * Returns optional colors used when filling a path, such as a gradient.
     *
     * @return Shader if previously set, null otherwise
     */
    @Nullable
    @RawPtr
    public Shader getShader() {
        return mShader;
    }

    /**
     * Returns optional colors used when filling a path, such as a gradient.
     *
     * @return Shader if previously set, null otherwise
     */
    @Nullable
    @SharedPtr
    public Shader refShader() {
        return RefCnt.create(mShader);
    }

    /**
     * Sets optional colors used when filling a path, such as a gradient.
     *
     * @param shader how geometry is filled with color; if null, solid color is used instead
     */
    public void setShader(@Nullable @SharedPtr Shader shader) {
        mShader = RefCnt.move(mShader, shader);
    }

    /**
     * Returns ColorFilter if set, or null.
     *
     * @return ColorFilter if previously set, null otherwise
     */
    @Nullable
    @RawPtr
    public ColorFilter getColorFilter() {
        return mColorFilter;
    }

    /**
     * Returns ColorFilter if set, or null.
     *
     * @return ColorFilter if previously set, null otherwise
     */
    @Nullable
    @SharedPtr
    public ColorFilter refColorFilter() {
        return RefCnt.create(mColorFilter);
    }

    /**
     * Sets ColorFilter to filter, pass null to clear ColorFilter.
     *
     * @param colorFilter ColorFilter to apply to subsequent draw
     */
    public void setColorFilter(@Nullable @SharedPtr ColorFilter colorFilter) {
        mColorFilter = RefCnt.move(mColorFilter, colorFilter);
    }

    /**
     * If the current blender can be represented as a BlendMode enum, this returns that
     * enum object. If it cannot, then this returns null.
     */
    @Nullable
    public BlendMode getBlendMode() {
        return mBlender != null ? mBlender.asBlendMode() : BlendMode.SRC_OVER;
    }

    /**
     * Returns true if BlendMode claims to be equivalent to {@link BlendMode#SRC_OVER}, the default.
     *
     * @return true if BlendMode is {@link BlendMode#SRC_OVER}
     */
    public final boolean isSrcOver() {
        return mBlender == null || mBlender.asBlendMode() == BlendMode.SRC_OVER;
    }

    /**
     * Helper method for calling setBlender().
     */
    public void setBlendMode(@Nullable BlendMode mode) {
        setBlender(mode == BlendMode.SRC_OVER ? null : mode);
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
    @RawPtr
    public final Blender getBlender() {
        return mBlender;
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
    @SharedPtr
    public Blender refBlender() {
        return RefCnt.create(mBlender);
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
    public final void setBlender(@Nullable @SharedPtr Blender blender) {
        mBlender = RefCnt.move(mBlender, blender);
    }

    /**
     * Returns PathEffect if set, or null.
     *
     * @return PathEffect if previously set, null otherwise
     */
    public final PathEffect getPathEffect() {
        return mPathEffect;
    }

    /**
     * Sets PathEffect to pathEffect. Pass null to leave the path geometry unaltered.
     *
     * @param pathEffect replace Path with a modification when drawn
     */
    public final void setPathEffect(@Nullable PathEffect pathEffect) {
        mPathEffect = pathEffect;
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
        var mode = getBlendMode();
        if (mode != null) {
            final boolean checkAlpha;
            switch (mode) {
                case SRC_OVER,
                        DST_OVER,
                        DST_OUT,
                        SRC_ATOP,
                        XOR,
                        PLUS,
                        PLUS_CLAMPED,
                        MINUS,
                        MINUS_CLAMPED -> checkAlpha = true;
                case DST -> {
                    return true;
                }
                // All advanced blend modes are SrcOver-like
                default -> checkAlpha = mode.isAdvanced();
            }
            if (checkAlpha && getAlphaF() == 0.0f) {
                return !isBlendedColorFilter(mColorFilter);
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
    public final boolean canComputeFastBounds(@RawPtr @Nullable ImageFilter imageFilter) {
        //TODO PathEffect
        return imageFilter == null || imageFilter.canComputeFastBounds();
    }

    /**
     * Only call this if canComputeFastBounds() returned true. This takes a
     * raw rectangle (the raw bounds of a shape), and adjusts it for stylistic
     * effects in the paint (e.g. stroking). If needed, it uses the storage
     * parameter. It returns the adjusted bounds that can then be used
     * for {@link Canvas#quickReject(Rect2fc)} tests.
     * <p>
     * This method ensures that orig will not be modified, and the result
     * will always be stored into the storage rect.
     *
     * @param orig    geometry modified by Paint when drawn
     * @param storage fast computed bounds of geometry
     */
    @ApiStatus.Internal
    public final void computeFastBounds(@RawPtr @Nullable ImageFilter imageFilter,
                                        Rect2fc orig, Rect2f storage) {
        int style = getStyle();
        // ultra fast-case: filling with no effects that affect geometry
        if (style == FILL) {
            if (imageFilter == null) {
                storage.set(orig);
                return;
            }
        }

        storage.set(orig);

        if (style != FILL) {
            float stroke = StrokeRec.getInflationRadius(mWidth,
                    getStrokeCap(), getStrokeJoin(), getStrokeAlign(), mMiterLimit);
            storage.outset(stroke, stroke);
        }

        if (imageFilter != null) {
            imageFilter.computeFastBounds(storage, storage);
        }
    }

    @Override
    public int hashCode() {
        int result = mFlags;
        // there is no negative zero
        result = 31 * result + Float.floatToIntBits(mR);
        result = 31 * result + Float.floatToIntBits(mG);
        result = 31 * result + Float.floatToIntBits(mB);
        result = 31 * result + Float.floatToIntBits(mA);
        result = 31 * result + Float.floatToIntBits(mWidth);
        result = 31 * result + Float.floatToIntBits(mMiterLimit);
        result = 31 * result + Objects.hashCode(mPathEffect);
        result = 31 * result + Objects.hashCode(mShader);
        result = 31 * result + Objects.hashCode(mColorFilter);
        result = 31 * result + Objects.hashCode(mBlender);
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
                // there is no negative zero
                mR == paint.mR &&
                mG == paint.mG &&
                mB == paint.mB &&
                mA == paint.mA &&
                mWidth == paint.mWidth &&
                mMiterLimit == paint.mMiterLimit &&
                Objects.equals(mPathEffect, paint.mPathEffect) &&
                Objects.equals(mShader, paint.mShader) &&
                Objects.equals(mColorFilter, paint.mColorFilter) &&
                Objects.equals(mBlender, paint.mBlender);
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
            s.append("STROKE_AND_FILL");
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
        s.append(", mWidth=");
        s.append(mWidth);
        s.append(", mMiterLimit=");
        s.append(mMiterLimit);
        s.append(", mPathEffect=");
        s.append(mPathEffect);
        s.append(", mShader=");
        s.append(mShader);
        s.append(", mColorFilter=");
        s.append(mColorFilter);
        s.append(", mBlender=");
        s.append(mBlender);
        s.append('}');
        return s.toString();
    }

    @ApiStatus.Internal
    public static int getAlphaDirect(@Nullable Paint paint) {
        return paint != null ? paint.getAlpha() : 0xFF;
    }

    @ApiStatus.Internal
    public static BlendMode getBlendModeDirect(@Nullable Paint paint) {
        if (paint != null) {
            return paint.getBlendMode();
        }
        return BlendMode.SRC_OVER;
    }

    @ApiStatus.Internal
    public static boolean isBlendedShader(@Nullable Shader shader) {
        return shader != null && !shader.isOpaque();
    }

    @ApiStatus.Internal
    public static boolean isBlendedColorFilter(@Nullable ColorFilter filter) {
        return filter != null && !filter.isAlphaUnchanged();
    }

    @ApiStatus.Internal
    public static boolean isBlendedImageFilter(@Nullable ImageFilter filter) {
        //TODO: check if we should allow image filters to broadcast that they don't affect alpha
        // just like color filters
        return filter != null;
    }

    @ApiStatus.Internal
    public static boolean isOpaquePaint(@Nullable Paint paint) {
        if (paint == null) {
            return true;
        }
        if (paint.getAlphaF() != 1.0f ||
                isBlendedShader(paint.mShader) ||
                isBlendedColorFilter(paint.mColorFilter)) {
            return false;
        }
        // Only let simple srcOver / src blending modes declare opaque, since behavior is clear.
        var mode = getBlendModeDirect(paint);
        return mode == BlendMode.SRC_OVER || mode == BlendMode.SRC;
    }
}
