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

import icyllis.arc3d.core.RefCnt;
import icyllis.modernui.annotation.*;
import icyllis.modernui.core.Core;
import icyllis.modernui.graphics.text.FontPaint;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.concurrent.GuardedBy;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
    public enum Style {
        /**
         * Geometry and text drawn with this style will be filled, ignoring all
         * stroke-related settings in the paint.
         */
        FILL            (icyllis.arc3d.core.Paint.FILL),
        /**
         * Geometry and text drawn with this style will be stroked, respecting
         * the stroke-related fields on the paint.
         */
        STROKE          (icyllis.arc3d.core.Paint.STROKE),
        /**
         * Geometry and text drawn with this style will be both filled and
         * stroked at the same time, respecting the stroke-related fields on
         * the paint. This mode can give unexpected results if the geometry
         * is oriented counter-clockwise. This restriction does not apply to
         * either FILL or STROKE.
         */
        FILL_AND_STROKE (icyllis.arc3d.core.Paint.FILL_AND_STROKE);

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
        BUTT    (icyllis.arc3d.core.Paint.CAP_BUTT),
        /**
         * The stroke projects out as a semicircle, with the center at the
         * end of the path.
         */
        ROUND   (icyllis.arc3d.core.Paint.CAP_ROUND),
        /**
         * The stroke projects out as a square, with the center at the end
         * of the path.
         */
        SQUARE  (icyllis.arc3d.core.Paint.CAP_SQUARE);

        final int nativeInt;

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
        MITER   (icyllis.arc3d.core.Paint.JOIN_MITER),
        /**
         * The outer edges of a join meet in a circular arc.
         */
        ROUND   (icyllis.arc3d.core.Paint.JOIN_ROUND),
        /**
         * The outer edges of a join meet with a straight line
         */
        BEVEL   (icyllis.arc3d.core.Paint.JOIN_BEVEL);

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
        CENTER   (icyllis.arc3d.core.Paint.ALIGN_CENTER),
        /**
         * The stroke is aligned to inside.
         */
        INSIDE   (icyllis.arc3d.core.Paint.ALIGN_INSIDE),
        /**
         * The stroke is aligned to outside.
         */
        OUTSIDE  (icyllis.arc3d.core.Paint.ALIGN_OUTSIDE);

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

    @ApiStatus.Internal
    @MagicConstant(intValues = {
            NORMAL,
            BOLD,
            ITALIC,
            BOLD_ITALIC
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextStyle {
    }

    /**
     * Font style constant to request the plain/regular/normal style
     */
    public static final int NORMAL = FontPaint.NORMAL;
    /**
     * Font style constant to request the bold style
     */
    public static final int BOLD = FontPaint.BOLD;
    /**
     * Font style constant to request the italic style
     */
    public static final int ITALIC = FontPaint.ITALIC;
    /**
     * Font style constant to request the bold and italic style
     */
    public static final int BOLD_ITALIC = FontPaint.BOLD_ITALIC;

    static final int TEXT_STYLE_MASK = NORMAL | BOLD | ITALIC;

    static final int TEXT_ANTI_ALIAS_DEFAULT = 0x0;
    static final int TEXT_ANTI_ALIAS_OFF = 0x4;
    static final int TEXT_ANTI_ALIAS_ON = 0x8;
    static final int TEXT_ANTI_ALIAS_MASK = 0xC;

    static final int LINEAR_TEXT_FLAG = 0x10;

    static final int FILTER_MODE_SHIFT = 5;
    static final int FILTER_MODE_MASK = 0x20;

    static final int MIPMAP_MODE_SHIFT = 6;
    static final int MIPMAP_MODE_MASK = 0xC0;

    static final int DEFAULT_FLAGS = NORMAL | (ImageShader.FILTER_MODE_LINEAR << FILTER_MODE_SHIFT) |
            TEXT_ANTI_ALIAS_DEFAULT | LINEAR_TEXT_FLAG;

    // the recycle bin, see obtain()
    private static final Paint[] sPool = new Paint[8];
    @GuardedBy("sPool")
    private static int sPoolSize;

    // closed by cleaner
    private final icyllis.arc3d.core.Paint mPaint;

    private Shader          mShader;
    private ColorFilter     mColorFilter;

    // style + rendering hints (+ text decoration)
    protected int mFlags;
    private float mFontSize;

    /**
     * Creates a new Paint with defaults.
     *
     * @see #obtain()
     */
    public Paint() {
        mPaint = new icyllis.arc3d.core.Paint();
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
            mPaint = new icyllis.arc3d.core.Paint();
            internalReset();
        } else {
            mPaint = new icyllis.arc3d.core.Paint(paint.mPaint);
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

    private void internalReset() {
        mShader = null;
        mColorFilter = null;
        mFlags = DEFAULT_FLAGS;
        mFontSize = 16;
    }

    private void internalSetFrom(@NonNull Paint paint) {
        mShader = paint.mShader;
        mColorFilter = paint.mColorFilter;
        mFlags = paint.mFlags;
        mFontSize = paint.mFontSize;
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
        return mPaint.getColor();
    }

    /**
     * Set the paint's solid color in sRGB. Note that the color is a 32-bit value
     * containing alpha as well as r,g,b. This 32-bit value is not premultiplied,
     * meaning that its alpha can be any value, regardless of the values of r,g,b.
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
     *
     * @return a new float array that contains r,g,b,a values
     */
    @NonNull
    @Size(4)
    public float[] getColor4f() {
        return new float[]{mPaint.r(), mPaint.g(), mPaint.b(), mPaint.a()};
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
     * Sets alpha and RGB used when stroking and filling. The color is four floating
     * point values, un-premultiplied. The color values are interpreted as being in
     * the sRGB color space.
     *
     * @param r the new red component (0..1) of the paint's color.
     * @param g the new green component (0..1) of the paint's color.
     * @param b the new blue component (0..1) of the paint's color.
     * @param a the new alpha component (0..1) of the paint's color.
     */
    public void setColor4f(float r, float g, float b, float a) {
        mPaint.setColor4f(r, g, b, a);
    }

    /**
     * Helper to getColor() that just returns the color's alpha value. This is
     * the same as calling getColor() >>> 24. It always returns a value between
     * 0 (completely transparent) and 255 (completely opaque).
     *
     * @return the alpha component of the paint's color.
     */
    public int getAlpha() {
        return mPaint.getAlpha();
    }

    /**
     * Helper to setColor(), that only assigns the color's alpha value,
     * leaving its r,g,b values unchanged.
     *
     * @param a the alpha component [0..255] of the paint's color
     */
    public void setAlpha(int a) {
        mPaint.setAlpha(a);
    }

    /**
     * Retrieves alpha/opacity from the color used when stroking and filling.
     *
     * @return alpha ranging from zero, fully transparent, to one, fully opaque
     */
    public float getAlphaF() {
        return mPaint.getAlphaF();
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
        mPaint.setAlphaF(a);
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
    public final void setRGBA(float r, float g, float b, float a) {
        setColor4f(r, g, b, a);
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
        mPaint.setRGBA(r, g, b, a);
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
        mPaint.setARGB(a, r, g, b);
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
        setStyle(stroke ? icyllis.arc3d.core.Paint.STROKE : icyllis.arc3d.core.Paint.FILL);
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
        if (mShader != shader) {
            mShader = shader;
            mPaint.setShader(shader != null
                    ? RefCnt.create(shader.getNativeShader())
                    : null);
        }
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
                    ? RefCnt.create(colorFilter.getNativeColorFilter())
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

    /**
     * Get desired text's style, combination of NORMAL, BOLD and ITALIC.
     *
     * @return the desired style of the font
     */
    @TextStyle
    public int getTextStyle() {
        return mFlags & TEXT_STYLE_MASK;
    }

    /**
     * Set desired text's style, combination of NORMAL, BOLD and ITALIC.
     * If the font family does not support this style natively, our engine
     * will use a simulation algorithm, also known as fake bold and fake italic.
     * The default value is NORMAL.
     *
     * @param textStyle the desired style of the font
     */
    public void setTextStyle(@TextStyle int textStyle) {
        mFlags = (mFlags & ~TEXT_STYLE_MASK) | (textStyle & TEXT_STYLE_MASK);
    }

    /**
     * Return the paint's text size in pixel units.
     * <p>
     * The default value is 16.
     *
     * @return the paint's text size in pixel units.
     * @see #setTextSize(float)
     */
    public float getTextSize() {
        return mFontSize;
    }

    /**
     * Set the paint's text size in pixel units. For example, a text size
     * of 16 (1em) means the letter 'M' is 16 pixels high in device space.
     * Very large or small sizes will impact rendering performance, and the
     * rendering system might not render text at these sizes. For now, text
     * sizes will clamp to 1 and 2184. You can have even larger glyphs through
     * matrix transformation, and our engine will attempt to use SDF text rendering.
     * This method has no effect if size is not greater than or equal to zero.
     * <p>
     * The default value is 16.
     *
     * @param textSize set the paint's text size in pixel units.
     */
    public void setTextSize(float textSize) {
        if (textSize >= 0) {
            mFontSize = textSize;
        }
    }

    public boolean isTextAntiAlias() {
        return switch (mFlags & TEXT_ANTI_ALIAS_MASK) {
            case TEXT_ANTI_ALIAS_ON -> true;
            case TEXT_ANTI_ALIAS_OFF -> false;
            default -> isAntiAlias();
        };
    }

    public void setTextAntiAlias(boolean textAA) {
        mFlags = (mFlags & ~TEXT_ANTI_ALIAS_MASK) |
                (textAA ? TEXT_ANTI_ALIAS_ON : TEXT_ANTI_ALIAS_OFF);
    }

    /**
     * @return whether to enable linear text
     */
    public boolean isLinearText() {
        return (mFlags & LINEAR_TEXT_FLAG) != 0;
    }

    /**
     * Paint flag that enables smooth linear scaling of text.
     *
     * <p>Enabling this flag does not actually scale text, but rather adjusts
     * text draw operations to deal gracefully with smooth adjustment of scale.
     * When this flag is enabled, font hinting is disabled to prevent shape
     * deformation between scale factors, and glyph caching is disabled due to
     * the large number of glyph images that will be generated.</p>
     * <p>
     * The default value is false.
     *
     * @param linearText whether to enable linear text
     */
    public void setLinearText(boolean linearText) {
        if (linearText) {
            mFlags |= LINEAR_TEXT_FLAG;
        } else {
            mFlags &= ~LINEAR_TEXT_FLAG;
        }
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
    public icyllis.arc3d.core.Paint getNativePaint() {
        return mPaint;
    }

    /**
     * Populates font attributes to native font object, excluding the typeface.
     *
     * @hidden
     */
    @ApiStatus.Internal
    public void getNativeFont(@NonNull icyllis.arc3d.core.Font nativeFont) {
        nativeFont.setSize(FontPaint.getCanonicalFontSize(getTextSize()));
        nativeFont.setEdging(isTextAntiAlias()
                ? icyllis.arc3d.core.Font.kAntiAlias_Edging
                : icyllis.arc3d.core.Font.kAlias_Edging);
        nativeFont.setLinearMetrics(isLinearText());
    }

    @Override
    public int hashCode() {
        int result = mPaint.hashCode();
        result = 31 * result + mFlags;
        result = 31 * result + Float.floatToIntBits(mFontSize);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Paint paint)) return false;
        return mPaint.equals(paint.mPaint) &&
                mFlags == paint.mFlags &&
                mFontSize == paint.mFontSize;
    }
}
