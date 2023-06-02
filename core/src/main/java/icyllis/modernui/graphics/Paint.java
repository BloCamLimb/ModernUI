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

import icyllis.modernui.annotation.*;
import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.concurrent.GuardedBy;
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

    private static final int ANTI_ALIAS_MASK = 0x100;
    private static final int DITHER_MASK = 0x200;

    // the recycle bin, see obtain()
    private static final Paint[] sPool = new Paint[4];
    @GuardedBy("sPool")
    private static int sPoolSize;

    // local bits
    private static final int PRIVATE_MASK = 0x7FFFFFFF;
    // this bit is guarded by 'sPool'
    private static final int IN_POOL_MASK = 0x80000000;

    private static final int DEFAULT_FLAGS = FILL |
            CAP_ROUND | JOIN_ROUND | ALIGN_CENTER | ANTI_ALIAS_MASK;

    // color components using non-premultiplied alpha
    private float mR; // may out of range
    private float mG; // may out of range
    private float mB; // may out of range
    private float mA; // 0..1

    private float mWidth;       // stroke-width
    private float mMiterLimit;  // stroke-miterlimit
    private float mSmoothWidth;

    // optional objects
    private Shader mShader;
    private Blender mBlender;
    //private MaskFilter mMaskFilter;
    //private ColorFilter mColorFilter;
    private ImageFilter mImageFilter;

    /*
     * bitfield
     * |--------|--------|
     *                 00  FILL             (def)
     *                 01  STROKE
     *                 10  STROKE_AND_FILL
     *               00    CAP_BUTT
     *               01    CAP_ROUND        (def)
     *               10    CAP_SQUARE
     *             00      JOIN_MITER
     *             01      JOIN_ROUND       (def)
     *             10      JOIN_BEVEL
     *           00        ALIGN_CENTER     (def)
     *           01        ALIGN_INSIDE
     *           10        ALIGN_OUTSIDE
     * |--------|--------|
     *         1           ANTI_ALIAS       (def)
     *        1            DITHER
     * |--------|--------|
     *
     * the highest bit is locked by 'sPool'
     */
    private int mFlags;

    /**
     * Creates a new Paint with defaults.
     *
     * @see #obtain()
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
     * Returns a paint from the shared pool, if any, or creates a new one.<br>
     * The attributes of the paint are guaranteed to be defaults mentioned in this class.
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
        Paint result = null;
        synchronized (sPool) {
            if (sPoolSize > 0) {
                final int i = --sPoolSize;
                result = sPool[i];
                sPool[i] = null;
            }
        }
        if (result == null)
            return new Paint();
        result.mFlags &= ~IN_POOL_MASK;
        result.reset();
        return result;
    }

    /**
     * Releases this paint object to the shared pool. You can't touch this paint anymore
     * after recycling.
     *
     * @throws IllegalStateException If the instance is already in the pool.
     * @see #obtain()
     */
    public final void recycle() {
        assert (mFlags & IN_POOL_MASK) == 0;
        synchronized (sPool) {
            if (sPoolSize < sPool.length) {
                if ((mFlags & IN_POOL_MASK) != 0) {
                    throw new IllegalStateException("Already in the pool!");
                }
                sPool[sPoolSize++] = this;
                mFlags |= IN_POOL_MASK;
            }
        }
    }

    /**
     * Set all contents of this paint to their initial values.
     */
    public final void reset() {
        assert (mFlags & IN_POOL_MASK) == 0;
        mR = 1;
        mG = 1;
        mB = 1;
        mA = 1; // opaque white
        mWidth = 2;
        mMiterLimit = 4;
        mSmoothWidth = 0;
        mShader = null;
        mBlender = null;
        //mMaskFilter = null;
        //mColorFilter = null;
        mImageFilter = null;
        mFlags = DEFAULT_FLAGS;
    }

    /**
     * Set all contents of this paint from the specified paint.
     *
     * @param paint the paint to set this paint from
     */
    public final void set(Paint paint) {
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
            //mMaskFilter = paint.mMaskFilter;
            //mColorFilter = paint.mColorFilter;
            mImageFilter = paint.mImageFilter;
            mFlags = paint.mFlags;
        }
    }

    /**
     * Return the paint's solid color in sRGB. Note that the color is a 32-bit value
     * containing alpha as well as r,g,b. This 32-bit value is not premultiplied,
     * meaning that its alpha can be any value, regardless of the values of r,g,b.
     *
     * @return the paint's color (and alpha).
     */
    @ColorInt
    public final int getColor() {
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
    public final void setColor(@ColorInt int color) {
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
    public final float getAlphaf() {
        return mA;
    }

    /**
     * Helper to getColor() that just returns the color's alpha value. This is
     * the same as calling getColor() >>> 24. It always returns a value between
     * 0 (completely transparent) and 255 (completely opaque).
     *
     * @return the alpha component of the paint's color.
     */
    public final int getAlpha() {
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
    public final void setAlphaf(float a) {
        mA = MathUtil.clamp(a, 0.0f, 1.0f);
    }

    /**
     * Helper to setColor(), that only assigns the color's alpha value,
     * leaving its r,g,b values unchanged.
     *
     * @param a the alpha component [0..255] of the paint's color
     */
    public final void setAlpha(int a) {
        mA = MathUtil.clamp(a / 255.0f, 0.0f, 1.0f);
    }

    /**
     * Returns the value of the red component.
     *
     * @see #alpha()
     * @see #green()
     * @see #blue()
     */
    public final float red() {
        return mR;
    }

    /**
     * Returns the value of the green component.
     *
     * @see #alpha()
     * @see #red()
     * @see #blue()
     */
    public final float green() {
        return mG;
    }

    /**
     * Returns the value of the blue component.
     *
     * @see #alpha()
     * @see #red()
     * @see #green()
     */
    public final float blue() {
        return mB;
    }

    /**
     * Returns the value of the alpha component.
     *
     * @see #alpha()
     * @see #red()
     * @see #green()
     */
    public final float alpha() {
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
    public final void setARGB(int a, int r, int g, int b) {
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
    public final void setARGB(float a, float r, float g, float b) {
        mR = MathUtil.clamp(r, 0.0f, 1.0f);
        mG = MathUtil.clamp(g, 0.0f, 1.0f);
        mB = MathUtil.clamp(b, 0.0f, 1.0f);
        mA = MathUtil.clamp(a, 0.0f, 1.0f);
    }

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
    public final int getStyle() {
        return mFlags & STYLE_MASK;
    }

    /**
     * Sets the paint's style, used for controlling how primitives' geometries
     * are interpreted, except where noted. The default is {@link #FILL}.
     *
     * @param style the new style to set in the paint
     */
    public final void setStyle(@Style int style) {
        mFlags = (mFlags & ~STYLE_MASK) | (style & STYLE_MASK);
    }

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
    public final int getStrokeCap() {
        return mFlags & CAP_MASK;
    }

    /**
     * Sets the paint's cap type, controlling how the start and end of stroked
     * lines and paths are treated, except where noted.
     * The default is {@link #CAP_ROUND}.
     *
     * @param cap set the paint's line cap style
     */
    public final void setStrokeCap(@Cap int cap) {
        mFlags = (mFlags & ~CAP_MASK) | (cap & CAP_MASK);
    }

    /**
     * Returns the paint's stroke join type. The default is {@link #JOIN_ROUND}.
     *
     * @return the paint's Join
     * @see #setStrokeJoin(int)
     */
    @Join
    public final int getStrokeJoin() {
        return mFlags & JOIN_MASK;
    }

    /**
     * Sets the paint's stroke join type. The default is {@link #JOIN_ROUND}.
     *
     * @param join set the paint's Join
     */
    public final void setStrokeJoin(@Join int join) {
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
    public final float getStrokeWidth() {
        return mWidth;
    }

    /**
     * Sets the thickness of the pen for stroking shapes. The default value is 2.0 px.
     * A stroke width of zero is treated as "hairline" width. Hairlines are always exactly one
     * pixel wide in screen space (their thickness does not change as the canvas is scaled).
     *
     * @param width set the paint's stroke width; zero for hairline, greater than zero for pen thickness
     */
    public final void setStrokeWidth(float width) {
        mWidth = Math.max(width, 0);
    }

    /**
     * Returns the miter limit at which a sharp corner is drawn beveled.
     * The default value is 4.0 px.
     *
     * @return zero and greater miter limit
     * @see #setStrokeMiter(float)
     */
    public final float getStrokeMiter() {
        return mMiterLimit;
    }

    /**
     * Sets the miter limit at which a sharp corner is drawn beveled.
     * The default value is 4.0 px.
     *
     * @param miter zero and greater miter limit
     */
    public final void setStrokeMiter(float miter) {
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

    /**
     * Returns optional colors used when filling a path, such as a gradient.
     *
     * @return Shader if previously set, null otherwise
     */
    @Nullable
    public final Shader getShader() {
        return mShader;
    }

    /**
     * Sets optional colors used when filling a path, such as a gradient.
     *
     * @param shader how geometry is filled with color; if null, solid color is used instead
     */
    public final void setShader(@Nullable Shader shader) {
        mShader = shader;
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
    public final void setBlendMode(@Nullable BlendMode mode) {
        if (mode == null || mode == BlendMode.SRC_OVER) {
            mBlender = null;
        } else {
            mBlender = mode;
        }
    }

    /**
     * Get the paint's blend mode. By default, returns {@link BlendMode#SRC_OVER}.
     * A null value means a custom blend.
     *
     * @return the paint's blend mode used to combine source color with destination color
     */
    @Nullable
    public final BlendMode getBlendMode() {
        return mBlender != null ? mBlender.asBlendMode() : BlendMode.SRC_OVER;
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
     * Sets the current blender.
     * <p>
     * A null blender signifies the default {@link BlendMode#SRC_OVER} behavior.
     * <p>
     * For convenience, you can call {@link #setBlendMode(BlendMode)} if the blend effect
     * can be expressed as one of those values.
     *
     * @see #getBlender()
     * @see #setBlendMode(BlendMode)
     */
    public final void setBlender(@Nullable Blender blender) {
        mBlender = blender;
    }

    /**
     * Returns the user-supplied blend function, if one has been set.
     * <p>
     * A null blender signifies the default {@link BlendMode#SRC_OVER} behavior.
     *
     * @return the blender assigned to this paint, otherwise null
     * @see #setBlender(Blender)
     * @see #getBlendMode()
     */
    @Nullable
    public final Blender getBlender() {
        return mBlender;
    }

    /**
     * Returns ImageFilter if set, or null.
     *
     * @return ImageFilter if previously set, null otherwise
     */
    @Nullable
    public final ImageFilter getImageFilter() {
        return mImageFilter;
    }

    /**
     * Sets ImageFilter, pass null to clear ImageFilter, and remove ImageFilter effect
     * on drawing.
     *
     * @param imageFilter how Image is sampled when transformed
     */
    public final void setImageFilter(@Nullable ImageFilter imageFilter) {
        mImageFilter = imageFilter;
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
        var mode = getBlendMode();
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
        /*result = 31 * result + Objects.hashCode(mMaskFilter);
        result = 31 * result + Objects.hashCode(mColorFilter);*/
        result = 31 * result + Objects.hashCode(mImageFilter);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Paint paint)) return false;
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
                /*Objects.equals(mMaskFilter, paint.mMaskFilter) &&
                Objects.equals(mColorFilter, paint.mColorFilter) &&*/
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
        /*s.append(", mMaskFilter=");
        s.append(mMaskFilter);
        s.append(", mColorFilter=");
        s.append(mColorFilter);*/
        s.append(", mImageFilter=");
        s.append(mImageFilter);
        s.append('}');
        return s.toString();
    }

    public static int getAlphaDirect(@Nullable Paint paint) {
        return paint != null ? paint.getAlpha() : 0xFF;
    }

    @Nullable
    public static BlendMode getBlendModeDirect(@Nullable Paint paint) {
        return paint != null ? paint.getBlendMode() : BlendMode.SRC_OVER;
    }

    public static boolean isBlendedShader(@Nullable Shader shader) {
        return shader != null && !shader.isOpaque();
    }

    public static boolean isBlendedImageFilter(@Nullable ImageFilter filter) {
        return filter != null;
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
