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
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.Size;
import icyllis.modernui.core.Core;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.concurrent.GuardedBy;

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
@SuppressWarnings("MagicConstant")
public class Paint {

    /**
     * Set {@code Style} to fill, stroke, or both fill and stroke geometry.
     * <p>
     * The stroke and fill share all paint attributes; for instance, they are drawn with the same color.
     * Use {@link #STROKE_AND_FILL} to avoid hitting the same pixels twice with a stroke draw and
     * a fill draw. The default is {@link #FILL}.
     */
    public enum Style {
        /**
         * Geometry and text drawn with this style will be filled, ignoring all
         * stroke-related settings in the paint.
         */
        FILL            (icyllis.arc3d.sketch.Paint.FILL),
        /**
         * Geometry and text drawn with this style will be stroked, respecting
         * the stroke-related fields on the paint.
         */
        STROKE          (icyllis.arc3d.sketch.Paint.STROKE),
        /**
         * Geometry and text drawn with this style will be both filled and
         * stroked at the same time, respecting the stroke-related fields on
         * the paint. This mode can give unexpected results if the geometry
         * is oriented counter-clockwise. This restriction does not apply to
         * either FILL or STROKE.
         */
        FILL_AND_STROKE (icyllis.arc3d.sketch.Paint.STROKE_AND_FILL);

        final int nativeInt;

        Style(int nativeInt) {
            this.nativeInt = nativeInt;
        }
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
    public static final int FILL_AND_STROKE = 0x02;

    private static final int STYLE_SHIFT = 0;
    private static final int STYLE_MASK = 0x03;

    /**
     * The {@code Cap} specifies the treatment for the beginning and ending of
     * stroked lines and paths. The default is {@link #ROUND}.
     */
    public enum Cap {
        /**
         * The stroke ends with the path, and does not project beyond it.
         */
        BUTT    (icyllis.arc3d.sketch.Paint.CAP_BUTT),
        /**
         * The stroke projects out as a semicircle, with the center at the
         * end of the path.
         */
        ROUND   (icyllis.arc3d.sketch.Paint.CAP_ROUND),
        /**
         * The stroke projects out as a square, with the center at the end
         * of the path.
         */
        SQUARE  (icyllis.arc3d.sketch.Paint.CAP_SQUARE);

        /**
         * @hidden
         */
        @ApiStatus.Internal
        public final int nativeInt;

        Cap(int nativeInt) {
            this.nativeInt = nativeInt;
        }
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

    private static final int CAP_SHIFT = 2;
    private static final int CAP_MASK = 0x0C;

    /**
     * The {@code Join} specifies the treatment where lines and curve segments
     * join on a stroked path. The default is {@link #ROUND}.
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
    public enum Join {
        /**
         * The outer edges of a join meet at a sharp angle
         */
        MITER   (icyllis.arc3d.sketch.Paint.JOIN_MITER),
        /**
         * The outer edges of a join meet in a circular arc.
         */
        ROUND   (icyllis.arc3d.sketch.Paint.JOIN_ROUND),
        /**
         * The outer edges of a join meet with a straight line
         */
        BEVEL   (icyllis.arc3d.sketch.Paint.JOIN_BEVEL);

        final int nativeInt;

        Join(int nativeInt) {
            this.nativeInt = nativeInt;
        }
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

    private static final int JOIN_SHIFT = 4;
    private static final int JOIN_MASK = 0x30;

    /**
     * The {@code Align} specifies the treatment where the stroke is placed in relation
     * to the object edge, this only applies to closed contours. The default is
     * {@link #CENTER}.
     */
    @ApiStatus.Experimental
    public enum Align {
        /**
         * The stroke is aligned to center.
         */
        CENTER   (icyllis.arc3d.sketch.Paint.ALIGN_CENTER),
        /**
         * The stroke is aligned to inside.
         */
        INSIDE   (icyllis.arc3d.sketch.Paint.ALIGN_INSIDE),
        /**
         * The stroke is aligned to outside.
         */
        OUTSIDE  (icyllis.arc3d.sketch.Paint.ALIGN_OUTSIDE);

        final int nativeInt;

        Align(int nativeInt) {
            this.nativeInt = nativeInt;
        }
    }

    /**
     * The stroke is aligned to center.
     */
    @ApiStatus.Experimental
    public static final int ALIGN_CENTER = 0x00;

    /**
     * The stroke is aligned to inside.
     */
    @ApiStatus.Experimental
    public static final int ALIGN_INSIDE = 0x40;

    /**
     * The stroke is aligned to outside.
     */
    @ApiStatus.Experimental
    public static final int ALIGN_OUTSIDE = 0x80;

    private static final int ALIGN_SHIFT = 6;
    private static final int ALIGN_MASK = 0xC0;

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static final int TEXT_ANTI_ALIAS_DEFAULT = 0x0,
            TEXT_ANTI_ALIAS_OFF = 0x4,
            TEXT_ANTI_ALIAS_ON = 0xC,
            TEXT_ANTI_ALIAS_MASK = 0xC;

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static final int LINEAR_TEXT_FLAG = 0x10;

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static final int FILTER_MODE_SHIFT = 5,
            FILTER_MODE_MASK = 0x20;

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static final int MIPMAP_MODE_SHIFT = 6,
            MIPMAP_MODE_MASK = 0xC0;

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static int sDefaultFlags = (ImageShader.FILTER_MODE_LINEAR << FILTER_MODE_SHIFT) |
            TEXT_ANTI_ALIAS_DEFAULT | LINEAR_TEXT_FLAG;

    // the recycle bin, see obtain()
    private static final Paint[] sPool = new Paint[8];
    @GuardedBy("sPool")
    private static int sPoolSize;

    // closed by cleaner
    private final icyllis.arc3d.sketch.Paint mPaint;

    private ColorFilter mColorFilter;

    /*
     * Shared by Paint and TextPaint
     * |-------|-------|-------|-------|
     *                                00 NORMAL (default)
     *                                01 BOLD
     *                                10 ITALIC
     *                                11 BOLD_ITALIC
     *                                11 TEXT_STYLE_MASK
     *                              00   TEXT_ANTI_ALIAS_DEFAULT (default)
     *                              01   TEXT_ANTI_ALIAS_OFF
     *                              11   TEXT_ANTI_ALIAS_ON
     *                              11   TEXT_ANTI_ALIAS_MASK
     *                             1     LINEAR_TEXT_FLAG
     *                            1      FILTER_MODE_MASK
     *                          11       MIPMAP_MODE_MASK
     * |-------|-------|-------|-------|
     */
    /**
     * @hidden
     */
    @ApiStatus.Internal
    protected int mFlags;

    /**
     * Creates a new Paint with defaults.
     *
     * @see #obtain()
     */
    public Paint() {
        mPaint = new icyllis.arc3d.sketch.Paint();
        internalReset();
        Core.registerNativeResource(this, mPaint);
    }

    /**
     * Create a new paint, initialized with the attributes in the specified
     * paint parameter.
     *
     * @param paint Existing paint used to initialize the attributes of the
     *              new paint.
     */
    @SuppressWarnings("IncompleteCopyConstructor")
    public Paint(@Nullable Paint paint) {
        if (paint == null) {
            mPaint = new icyllis.arc3d.sketch.Paint();
            internalReset();
        } else {
            mPaint = new icyllis.arc3d.sketch.Paint(paint.mPaint);
            internalSetFrom(paint);
        }
        Core.registerNativeResource(this, mPaint);
    }

    /**
     * Returns a paint from the shared pool, if any, or creates a new one.<br>
     * The attributes of the paint are guaranteed to be defaults according to this class.
     * A call to {@link #recycle()} is expected (not strictly necessary) after use.
     * <p>
     * For example:
     * <pre>{@code
     * @Override
     * protected void onDraw(Canvas canvas) {
     *     var paint = Paint.obtain();
     *
     *     paint.setColor(mColorA);
     *     canvas.drawRect(mRectA, paint);
     *
     *     paint.setColor(mColorB);
     *     canvas.drawRect(mRectB, paint);
     *
     *     paint.recycle(); // recycle it before method return
     * }
     * }</pre>
     * In most cases, you'll need to cache a paint object in its owner object, such as
     * a View or a Drawable. Thus this method is not really recommended.
     *
     * @return a pooled paint object
     * @see #recycle()
     */
    @NonNull
    public static Paint obtain() {
        synchronized (sPool) {
            if (sPoolSize != 0) {
                int i = --sPoolSize;
                Paint p = sPool[i];
                sPool[i] = null;
                return p;
            }
        }
        return new Paint();
    }

    /**
     * Releases this paint object to the shared pool. You can't touch this paint anymore
     * after recycling.
     *
     * @see #obtain()
     */
    public void recycle() {
        reset();
        synchronized (sPool) {
            if (sPoolSize != sPool.length) {
                sPool[sPoolSize++] = this;
            }
        }
    }

    /**
     * Set all contents of this paint to their initial values.
     */
    public void reset() {
        mPaint.reset();
        internalReset();
    }

    /**
     * Set all contents of this paint from the specified paint.
     *
     * @param paint the paint to set this paint from
     */
    public void set(@Nullable Paint paint) {
        if (paint == null) {
            reset();
        } else if (this != paint) {
            mPaint.set(paint.mPaint);
            internalSetFrom(paint);
        }
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    protected void internalReset() {
        mColorFilter = null;
        mFlags = sDefaultFlags;
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    protected void internalSetFrom(@NonNull Paint paint) {
        mColorFilter = paint.mColorFilter;
        mFlags = paint.mFlags;
    }

    ///// Solid Color

    /**
     * Return the paint's solid color in sRGB. Note that the color is a 32-bit value
     * containing alpha as well as r,g,b. This 32-bit value is not premultiplied,
     * meaning that its alpha can be any value, regardless of the values of r,g,b.
     * <p>
     * This method is obsolete and discouraged. Converting color to a
     * packed 32-bit integer (8-bit per channel) leads to precision loss.
     * Use this only if you specifically need to quantize color components for storage
     * or legacy API compatibility. Otherwise, use {@link #getColor4f()}.
     *
     * @return the paint's color (and alpha).
     */
    @ApiStatus.Obsolete
    public int getColor() {
        return ((int) (mPaint.getAlpha() * 255.0f + 0.5f) << 24) |
                ((int) (mPaint.getRed() * 255.0f + 0.5f) << 16) |
                ((int) (mPaint.getGreen() * 255.0f + 0.5f) << 8) |
                (int) (mPaint.getBlue() * 255.0f + 0.5f);
    }

    /**
     * Set the paint's solid color in sRGB. Note that the color is a 32-bit value
     * containing alpha as well as r,g,b. This 32-bit value is not premultiplied,
     * meaning that its alpha can be any value, regardless of the values of r,g,b.
     * <p>
     * Use {@link #setColor4f} for full-precision floating-point access.
     *
     * @param color the new color (including alpha) to set in the paint.
     */
    public void setColor(@ColorInt int color) {
        mPaint.setColor(color);
    }

    /**
     * Returns alpha and RGB used when stroking and filling. The color is four floating
     * point values, un-premultiplied. The color values are interpreted as being in
     * the sRGB color space.
     * <p>
     * Use {@link #getColor4f(float[])} to avoid creating a new array.
     *
     * @return a new float array that contains r,g,b,a values
     */
    @NonNull
    @Size(4)
    public float[] getColor4f() {
        return new float[]{mPaint.getRed(), mPaint.getGreen(), mPaint.getBlue(), mPaint.getAlpha()};
    }

    /**
     * Returns alpha and RGB used when stroking and filling. The color is four floating
     * point values, un-premultiplied. The color values are interpreted as being in
     * the sRGB color space.
     *
     * @param dst a non-null array of 4 floats that will hold the result of the method
     * @return the passed float array that contains r,g,b,a values
     */
    @NonNull
    @Size(4)
    public float[] getColor4f(@NonNull @Size(4) float[] dst) {
        mPaint.getColor4f(dst);
        return dst;
    }

    /**
     * Returns the raw value of the red component.
     * It's in sRGB space and independent of alpha.
     *
     * @see #getAlphaF()
     * @see #getRedF()
     * @see #getGreenF()
     * @return red value
     * @since 3.13.0
     */
    public float getRedF() {
        return mPaint.getRed();
    }

    /**
     * Returns the raw value of the green component.
     * It's in sRGB space and independent of alpha.
     *
     * @see #getAlphaF()
     * @see #getRedF()
     * @see #getBlueF()
     * @return green value
     * @since 3.13.0
     */
    public float getGreenF() {
        return mPaint.getGreen();
    }

    /**
     * Returns the raw value of the blue component.
     * It's in sRGB space and independent of alpha.
     *
     * @see #getAlphaF()
     * @see #getGreenF()
     * @see #getBlueF()
     * @return blue value
     * @since 3.13.0
     */
    public float getBlueF() {
        return mPaint.getBlue();
    }

    /**
     * Sets alpha and RGB used when stroking and filling. The color is four floating
     * point values, un-premultiplied. The color values are interpreted as being in
     * the sRGB color space. The alpha value will be clamped to 0..1, NaN alpha will
     * become 0.
     * <p>
     * Starting from 3.13.0, this method will no longer clamp R, G, B values to 0..1.
     * The rendering pipeline still applies the gamut and transfer function to
     * out of range values.
     *
     * @param r the new red component of the paint's color.
     * @param g the new green component of the paint's color.
     * @param b the new blue component of the paint's color.
     * @param a the new alpha component (0..1) of the paint's color.
     */
    public void setColor4f(float r, float g, float b, float a) {
        mPaint.setColor4f(r, g, b, a);
    }

    /**
     * Helper to getColor() that just returns the color's alpha value. This is
     * the same as calling getColor() >>> 24. It always returns a value between
     * 0 (completely transparent) and 255 (completely opaque).
     * <p>
     * This method is obsolete and discouraged.
     * Converting alpha to an 8-bit integer leads to precision loss.
     * Use this only if you specifically need to quantize alpha values for storage
     * or legacy API compatibility. Otherwise, use {@link #getAlphaF()}.
     *
     * @return the alpha component of the paint's color.
     */
    @ApiStatus.Obsolete
    public int getAlpha() {
        return (int) (mPaint.getAlpha() * 255.0f + 0.5f);
    }

    /**
     * Helper to setColor(), that only assigns the color's alpha value,
     * leaving its r,g,b values unchanged.
     * <p>
     * Use {@link #setAlphaF(float)} for full-precision floating-point access.
     *
     * @param a the alpha component [0..255] of the paint's color
     */
    public void setAlpha(int a) {
        mPaint.setAlpha(a * (1 / 255.0f));
    }

    /**
     * Retrieves alpha/opacity from the color used when stroking and filling.
     *
     * @see #getRedF()
     * @see #getGreenF()
     * @see #getBlueF()
     * @return alpha ranging from zero, fully transparent, to one, fully opaque
     */
    public float getAlphaF() {
        return mPaint.getAlpha();
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
        mPaint.setAlpha(a);
    }

    /**
     * Sets alpha and RGB used when stroking and filling. The color is four floating
     * point values, un-premultiplied. The color values are interpreted as being in
     * the sRGB color space.
     * <p>
     * This method clamps the R,G,B values to 0..1, use {@link #setColor4f}
     * for unclamped floating-point access.
     *
     * @param r the new red component (0..1) of the paint's color.
     * @param g the new green component (0..1) of the paint's color.
     * @param b the new blue component (0..1) of the paint's color.
     * @param a the new alpha component (0..1) of the paint's color.
     */
    public final void setRGBA(float r, float g, float b, float a) {
        setColor4f(
                MathUtil.pin(r, 0f, 1f),
                MathUtil.pin(g, 0f, 1f),
                MathUtil.pin(b, 0f, 1f),
                a);
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
    public void setRGBA(int r, int g, int b, int a) {
        mPaint.setColor4(r, g, b, a);
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
        mPaint.setColor4(r, g, b, a);
    }

    ///// Basic Flags

    /**
     * Returns true if antialiasing should be used.
     * The default value is true.
     *
     * @return anti-aliasing state
     * @see #setAntiAlias(boolean)
     */
    public boolean isAntiAlias() {
        return mPaint.isAntiAlias();
    }

    /**
     * Sets a hint that indicates if antialiasing should be used. An implementation
     * may use analytic method by computing geometry's coverage, distance-to-edge
     * method by computing signed distance field, or multisampling to do antialiasing.
     * If true, the AA step is calculated in screen space. The default value is true.
     *
     * @param aa setting for anti-aliasing
     */
    public void setAntiAlias(boolean aa) {
        mPaint.setAntiAlias(aa);
    }

    /**
     * Returns true if color error may be distributed to smooth color transition.
     * An implementation may use a bayer matrix or blue noise texture to do dithering.
     * The default value is false.
     *
     * @return dithering state
     * @see #setDither(boolean)
     */
    public boolean isDither() {
        return mPaint.isDither();
    }

    /**
     * Sets a hint that indicates if color error may be distributed to smooth color transition.
     * An implementation may use a bayer matrix or blue noise texture to do dithering.
     * The default value is false.
     *
     * @param dither setting for dithering
     */
    public void setDither(boolean dither) {
        mPaint.setDither(dither);
    }

    /**
     * Returns the paint's style, used for controlling how primitives' geometries
     * are interpreted, except where noted. The default is {@link #FILL}.
     *
     * @return the paint's style setting (fill, stroke or both)
     * @see #setStyle(int)
     */
    @MagicConstant(intValues = {FILL, STROKE, STROKE_AND_FILL, FILL_AND_STROKE})
    public int getStyle() {
        return mPaint.getStyle() << STYLE_SHIFT;
    }

    /**
     * Sets the paint's style, used for controlling how primitives' geometries
     * are interpreted, except where noted. The default is {@link Style#FILL}.
     *
     * @param style the new style to set in the paint
     */
    public void setStyle(@NonNull Style style) {
        mPaint.setStyle(style.nativeInt);
    }

    /**
     * Sets the paint's style, used for controlling how primitives' geometries
     * are interpreted, except where noted. The default is {@link #FILL}.
     *
     * @param style the new style to set in the paint
     */
    public void setStyle(@MagicConstant(intValues = {FILL, STROKE, STROKE_AND_FILL, FILL_AND_STROKE}) int style) {
        mPaint.setStyle((style & STYLE_MASK) >>> STYLE_SHIFT);
    }

    ///// Stroke Parameters

    /**
     * Sets paint's style to STROKE if true, or FILL if false.
     *
     * @param stroke true to stroke shapes, false to fill shapes
     */
    public void setStroke(boolean stroke) {
        setStyle(stroke ? icyllis.arc3d.sketch.Paint.STROKE : icyllis.arc3d.sketch.Paint.FILL);
    }

    /**
     * Returns the paint's cap type, controlling how the start and end of stroked
     * lines and paths are treated, except where noted.
     * The default is {@link #CAP_ROUND}.
     *
     * @return the line cap style for the paint
     * @see #setStrokeCap(int)
     */
    @MagicConstant(intValues = {CAP_BUTT, CAP_ROUND, CAP_SQUARE})
    public int getStrokeCap() {
        return mPaint.getStrokeCap() << CAP_SHIFT;
    }

    /**
     * Sets the paint's cap type, controlling how the start and end of stroked
     * lines and paths are treated, except where noted.
     * The default is {@link Cap#ROUND}.
     *
     * @param cap set the paint's line cap style
     */
    public void setStrokeCap(@NonNull Cap cap) {
        mPaint.setStrokeCap(cap.nativeInt);
    }

    /**
     * Sets the paint's cap type, controlling how the start and end of stroked
     * lines and paths are treated, except where noted.
     * The default is {@link #CAP_ROUND}.
     *
     * @param cap set the paint's line cap style
     */
    public void setStrokeCap(@MagicConstant(intValues = {CAP_BUTT, CAP_ROUND, CAP_SQUARE}) int cap) {
        mPaint.setStrokeCap((cap & CAP_MASK) >>> CAP_SHIFT);
    }

    /**
     * Returns the paint's stroke join type. The default is {@link #JOIN_ROUND}.
     *
     * @return the paint's Join
     * @see #setStrokeJoin(int)
     */
    @MagicConstant(intValues = {JOIN_MITER, JOIN_ROUND, JOIN_BEVEL})
    public int getStrokeJoin() {
        return mPaint.getStrokeJoin() << JOIN_SHIFT;
    }

    /**
     * Sets the paint's stroke join type. The default is {@link Join#ROUND}.
     *
     * @param join set the paint's Join
     */
    public void setStrokeJoin(@NonNull Join join) {
        mPaint.setStrokeJoin(join.nativeInt);
    }

    /**
     * Sets the paint's stroke join type. The default is {@link #JOIN_ROUND}.
     *
     * @param join set the paint's Join
     */
    public void setStrokeJoin(@MagicConstant(intValues = {JOIN_MITER, JOIN_ROUND, JOIN_BEVEL}) int join) {
        mPaint.setStrokeJoin((join & JOIN_MASK) >>> JOIN_SHIFT);
    }

    /**
     * Returns the paint's stroke align type. The default is {@link #ALIGN_CENTER}.
     * Note that this only applies to closed contours, otherwise stroking behaves
     * as {@link #ALIGN_CENTER}.
     *
     * @return the paint's Align
     * @see #setStrokeAlign(int)
     */
    @MagicConstant(intValues = {ALIGN_CENTER, ALIGN_INSIDE, ALIGN_OUTSIDE})
    public int getStrokeAlign() {
        return mPaint.getStrokeAlign() << ALIGN_SHIFT;
    }

    /**
     * Sets the paint's stroke align type. The default is {@link Align#CENTER}.
     * Note that this only applies to closed contours, otherwise stroking behaves
     * as {@link Align#CENTER}.
     *
     * @param align set the paint's Align
     */
    @ApiStatus.Experimental
    public void setStrokeAlign(@NonNull Align align) {
        mPaint.setStrokeAlign(align.nativeInt);
    }

    /**
     * Sets the paint's stroke align type. The default is {@link #ALIGN_CENTER}.
     * Note that this only applies to closed contours, otherwise stroking behaves
     * as {@link #ALIGN_CENTER}.
     *
     * @param align set the paint's Align
     */
    @ApiStatus.Experimental
    public void setStrokeAlign(@MagicConstant(intValues = {ALIGN_CENTER, ALIGN_INSIDE, ALIGN_OUTSIDE}) int align) {
        mPaint.setStrokeAlign((align & ALIGN_MASK) >>> ALIGN_SHIFT);
    }

    /**
     * Returns the thickness of the pen for stroking shapes. The default value is 1.0 px.
     *
     * @return the paint's stroke width; zero for hairline, greater than zero for pen thickness
     * @see #setStrokeWidth(float)
     */
    public float getStrokeWidth() {
        return mPaint.getStrokeWidth();
    }

    /**
     * Sets the thickness of the pen for stroking shapes. The default value is 1.0 px.
     * A stroke width of zero is treated as "hairline" width. Hairlines are always exactly one
     * pixel wide in screen space (their thickness does not change as the canvas is scaled).
     *
     * @param width set the paint's stroke width; zero for hairline, greater than zero for pen thickness
     */
    public void setStrokeWidth(float width) {
        mPaint.setStrokeWidth(width);
    }

    /**
     * Returns the miter limit at which a sharp corner is drawn beveled.
     * The default value is 4.0 px.
     *
     * @return zero and greater miter limit
     * @see #setStrokeMiter(float)
     */
    public float getStrokeMiter() {
        return mPaint.getStrokeMiter();
    }

    /**
     * Sets the miter limit at which a sharp corner is drawn beveled.
     * The default value is 4.0 px.
     *
     * @param miter zero and greater miter limit
     */
    public void setStrokeMiter(float miter) {
        mPaint.setStrokeMiter(miter);
    }

    ///// Effects

    /**
     * Returns true if there are optional colors used when filling a path, such as a gradient.
     * <p>
     * Note: There is no way to get the Shader object. The Shader set by {@link #setShader(Shader)}
     * will be optimized and converted to internal form. However, this method guarantees that the
     * return value is consistent with the nullability of the object passed to setShader method.
     *
     * @return true to use Shader colors, false to use Paint's solid color
     */
    public boolean hasShader() {
        return mPaint.getShader() != null;
    }

    /**
     * Sets optional colors used when drawing geometries and text, such as a gradient. The final alpha
     * will be Shader's alpha modulated by Paint's alpha. Sets to null to use Paint's solid color.
     *
     * @param shader how geometry is filled with color; if null, solid color is used instead
     */
    public void setShader(@Nullable Shader shader) {
        mPaint.setShader(shader != null
                ? icyllis.arc3d.core.RefCnt.create(shader.getNativeShader())
                : null);
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
        if (mColorFilter != colorFilter) {
            mColorFilter = colorFilter;
            mPaint.setColorFilter(colorFilter != null
                    ? colorFilter.getNativeColorFilter()
                    : null);
        }
    }

    /**
     * Get the paint's blend mode. By default, returns {@link BlendMode#SRC_OVER}.
     * A null value means a custom blend.
     *
     * @return the paint's blend mode used to combine source color with destination color
     */
    @Nullable
    public BlendMode getBlendMode() {
        var mode = mPaint.getBlendMode();
        return mode != null ? BlendMode.VALUES[mode.ordinal()] : null;
    }

    /**
     * Set or clear the blend mode. A blend mode defines how source pixels
     * (generated by a drawing command) are composited with the destination pixels
     * (content of the render target).
     * <p>
     * A null blend mode signifies the default {@link BlendMode#SRC_OVER} behavior.
     *
     * @param mode the blend mode to be installed in the paint, may be null
     * @see BlendMode
     */
    public void setBlendMode(@Nullable BlendMode mode) {
        mPaint.setBlendMode(mode != null ? mode.getNativeBlendMode() : null);
    }

    ///// Sampler Parameters

    /**
     * Returns the current filter. The default is {@link ImageShader#FILTER_MODE_LINEAR}.
     * The value is ignored when anisotropic filtering is used.
     *
     * @return the current filter
     * @see #setFilterMode(int)
     */
    @ImageShader.FilterMode
    public final int getFilterMode() {
        return (mFlags & FILTER_MODE_MASK) >>> FILTER_MODE_SHIFT;
    }

    /**
     * Set the interpolation method for sampling texture images.
     * The default is {@link ImageShader#FILTER_MODE_LINEAR}.
     * Calling this method does NOT affect anisotropic filtering.
     *
     * @param filter the paint's filter
     * @see #getFilterMode()
     */
    public final void setFilterMode(@ImageShader.FilterMode int filter) {
        mFlags = (mFlags & ~FILTER_MODE_MASK) | ((filter << FILTER_MODE_SHIFT) & FILTER_MODE_MASK);
    }

    /**
     * Returns the current filter. True will use {@link ImageShader#FILTER_MODE_LINEAR},
     * false will use {@link ImageShader#FILTER_MODE_NEAREST}.
     * The value is ignored when anisotropic filtering is used.
     *
     * @return the current filter, true means bilinear sampling, false means nearest neighbor sampling
     */
    public boolean isFilter() {
        return (mFlags & FILTER_MODE_MASK) != (ImageShader.FILTER_MODE_NEAREST << FILTER_MODE_SHIFT);
    }

    /**
     * Set the interpolation method for sampling textures images.
     * True to use {@link ImageShader#FILTER_MODE_LINEAR}, false to
     * use {@link ImageShader#FILTER_MODE_NEAREST}.
     *
     * @param filter true to use bilinear sampling, false to use nearest neighbor sampling
     */
    public void setFilter(boolean filter) {
        mFlags = (mFlags & ~FILTER_MODE_MASK) |
                (filter
                        ? (ImageShader.FILTER_MODE_LINEAR << FILTER_MODE_SHIFT)
                        : (ImageShader.FILTER_MODE_NEAREST << FILTER_MODE_SHIFT)
                );
    }

    /**
     * Returns the mipmap mode. The value is ignored when anisotropic filtering is used.
     * The default is {@link ImageShader#MIPMAP_MODE_NONE}.
     *
     * @return the mipmap mode
     */
    @ImageShader.MipmapMode
    public final int getMipmapMode() {
        return (mFlags & MIPMAP_MODE_MASK) >>> MIPMAP_MODE_SHIFT;
    }

    /**
     * Set the mipmap mode for sampling texture images. The value is ignored when
     * anisotropic filtering is used. The default is {@link ImageShader#MIPMAP_MODE_NONE}.
     *
     * @param mipmap the mipmap mode
     */
    public final void setMipmapMode(@ImageShader.MipmapMode int mipmap) {
        mFlags = (mFlags & ~MIPMAP_MODE_MASK) | ((mipmap << MIPMAP_MODE_SHIFT) & MIPMAP_MODE_MASK);
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public icyllis.arc3d.sketch.Paint getNativePaint() {
        return mPaint;
    }

    @Override
    public int hashCode() {
        int result = mPaint.hashCode();
        result = 31 * result + mFlags;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Paint paint)) return false;
        return mPaint.equals(paint.mPaint) &&
                mFlags == paint.mFlags;
    }
}
