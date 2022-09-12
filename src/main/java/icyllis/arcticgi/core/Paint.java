/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.core;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * A Paint collects all options outside the Canvas clip and Canvas matrix,
 * such as style and color information, applied when drawing geometries and
 * images.
 * <p>
 * A Paint also collects effects and filters that describe single-pass and
 * multiple-pass algorithms that alter the drawing geometry, color, and
 * transparency. For instance, Paint does not directly implement dashing or
 * blur, but contains the objects that do so.
 * <p>
 * Note that multisampling anti-aliasing (MSAA) is always enabled. A Paint
 * object takes up approximately 64 bytes in the heap memory.
 */
@SuppressWarnings("unused")
public class Paint {

    /**
     * The Style specifies if the primitive being drawn is filled, stroked, or
     * both (in the same color). The default is FILL.
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
     * Geometry (path) drawn with this style will be both filled and stroked
     * at the same time, respecting the stroke-related fields on the paint.
     * This share all paint attributes; for instance, they are drawn
     * with the same color. Use this to avoid hitting the same pixels twice
     * with a stroke draw and a fill draw.
     */
    public static final int STROKE_AND_FILL = 0x02;
    public static final int FILL_AND_STROKE = 0x02; // alias

    private static final int STYLE_MASK = 0x03;

    /**
     * The <code>Cap</code> specifies the treatment for the beginning and ending of
     * stroked lines and paths. The default is ROUND.
     */
    @MagicConstant(intValues = {CAP_ROUND, CAP_BUTT, CAP_SQUARE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Cap {
    }

    /**
     * The stroke projects out as a semicircle, with the center at the
     * end of the path.
     */
    public static final int CAP_ROUND = 0x00;

    /**
     * The stroke ends with the path, and does not project beyond it.
     */
    public static final int CAP_BUTT = 0x04;

    /**
     * The stroke projects out as a square, with the center at the end
     * of the path.
     */
    public static final int CAP_SQUARE = 0x08;

    private static final int CAP_MASK = 0x0C;

    /**
     * The <code>Join</code> specifies the treatment where lines and curve segments
     * join on a stroked path. The default is ROUND.
     * <p>
     * Join affects the four corners of a stroked rectangle, and the connected segments in a
     * stroked path.
     * <p>
     * Choose miter join to draw sharp corners. Choose round join to draw a circle with a
     * radius equal to the stroke width on top of the corner. Choose bevel join to minimally
     * connect the thick strokes.
     * <p>
     * The fill path constructed to describe the stroked path respects the join setting but may
     * not contain the actual join. For instance, a fill path constructed with round joins does
     * not necessarily include circles at each connected segment.
     */
    @MagicConstant(intValues = {JOIN_ROUND, JOIN_BEVEL, JOIN_MITER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Join {
    }

    /**
     * The outer edges of a join meet in a circular arc.
     */
    public static final int JOIN_ROUND = 0x00;

    /**
     * The outer edges of a join meet with a straight line.
     */
    public static final int JOIN_BEVEL = 0x10;

    /**
     * The outer edges of a join meet at a sharp angle.
     */
    public static final int JOIN_MITER = 0x20;

    private static final int JOIN_MASK = 0x30;

    /**
     * The <code>Align</code> specifies the treatment where the stroke is placed in relation
     * to the object edge. The default is CENTER.
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

    private static final int BLEND_MODE_SHIFT = 8;
    private static final int BLEND_MODE_MASK = 0xFF << BLEND_MODE_SHIFT;

    private static final int DEFAULT_FLAGS = FILL | CAP_ROUND | JOIN_ROUND | ALIGN_CENTER |
            BlendMode.toValue(BlendMode.SRC_OVER) << BLEND_MODE_SHIFT;

    // the recycle bin, see take()
    private static final Paint[] sBag = new Paint[4];
    private static int sBagSize;

    private float mR;
    private float mG;
    private float mB;
    private float mA;

    private float mWidth;
    private float mMiter;
    private float mFeather;

    private Shader mShader;
    private MaskFilter mMaskFilter;
    private ColorFilter mColorFilter;
    private ImageFilter mImageFilter;

    // bitfield
    private int mFlags;

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
     * @param paint an existing paint used to initialize the attributes of the
     *              new paint
     */
    public Paint(@Nonnull Paint paint) {
        set(paint);
    }

    /**
     * Returns a reset paint from the shared pool.
     * <p>
     * This method is motivated by the restriction on stack-allocated objects in Java.
     * A {@link #drop()} is expected (not strictly necessary) before a pop operation.
     * <p>
     * For example:
     * <pre>{@code
     * @Override
     * protected void onDraw(Canvas canvas) {
     *     var paint = Paint.take();
     *     paint.setColor(mColorA);
     *     canvas.drawRect(mRectA, paint);
     *     // ...
     *     paint.setColor(mColorB);
     *     canvas.drawRect(mRectB, paint);
     *     paint.drop(); // recycle it before method return
     * }
     * }</pre>
     *
     * @return a shared paint object
     * @see #drop()
     */
    @Nonnull
    public static Paint take() {
        Paint paint;
        synchronized (sBag) {
            if (sBagSize == 0)
                return new Paint();
            paint = sBag[--sBagSize];
            // still hold strong reference
        }
        paint.reset();
        return paint;
    }

    /**
     * Release this paint object to the shared pool. Then you may never touch this paint anymore.
     *
     * @see #take()
     */
    public final void drop() {
        synchronized (sBag) {
            if (sBagSize == sBag.length)
                return;
            sBag[sBagSize++] = this;
        }
    }

    /**
     * Set all attributes of this paint to their initial values.
     */
    public final void reset() {
        mR = 0;
        mG = 0;
        mB = 0;
        mA = 1;
        mWidth = 4;
        mMiter = 4;
        mFeather = 2;
        mShader = null;
        mMaskFilter = null;
        mColorFilter = null;
        mImageFilter = null;
        mFlags = DEFAULT_FLAGS;
    }

    /**
     * Set all attributes of this paint from the specified paint.
     *
     * @param paint the paint to set this paint from
     */
    public final void set(Paint paint) {
        mR = paint.mR;
        mG = paint.mG;
        mB = paint.mB;
        mA = paint.mA;
        mWidth = paint.mWidth;
        mMiter = paint.mMiter;
        mFeather = paint.mFeather;
        mShader = paint.mShader;
        mMaskFilter = paint.mMaskFilter;
        mColorFilter = paint.mColorFilter;
        mImageFilter = paint.mImageFilter;
        mFlags = paint.mFlags;
    }

    /**
     * Set the paint's solid color. Note that the color is an int containing alpha
     * as well as r,g,b. This 32bit value is not premultiplied, meaning that
     * its alpha can be any value, regardless of the values of r,g,b.
     * See the Color class for more details.
     *
     * @param color the new straight color (including alpha) to set in the paint.
     */
    public final void setColor(@ColorInt int color) {
        mR = ((color >> 16) & 0xff) / 255.0f;
        mG = ((color >> 8) & 0xff) / 255.0f;
        mB = (color & 0xff) / 255.0f;
        mA = (color >>> 24) / 255.0f;
    }

    /**
     * Sets alpha and RGB used when stroking and filling. The color is four floating
     * point values, un-premultiplied, in the sRGB color space.
     *
     * @param color un-premultiplied RGBA
     */
    public final void setColor(Color color) {
        mR = color.mR;
        mG = color.mG;
        mB = color.mB;
        mA = color.mA;
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
    public final int getColor() {
        return ((int) (mA * 255.0f + 0.5f) << 24) |
                ((int) (mR * 255.0f + 0.5f) << 16) |
                ((int) (mG * 255.0f + 0.5f) << 8) |
                (int) (mB * 255.0f + 0.5f);
    }

    /**
     * Retrieves alpha and RGB, un-premultiplied, as four floating point values. RGB are
     * extended sRGB values (sRGB gamut, and encoded with the sRGB transfer function).
     *
     * @param color un-premultiplied RGBA
     */
    public final void getColor(Color color) {
        color.set(mR, mG, mB, mA);
    }

    /**
     * Helper to setColor(), that only assigns the color's alpha value,
     * leaving its r,g,b values unchanged. Results are undefined if the alpha
     * value is outside the range [0..255].
     *
     * @param a set the alpha component [0..255] of the paint's color.
     */
    public final void setAlpha(int a) {
        mA = a / 255.0f;
    }

    /**
     * Replaces alpha, leaving RGB unchanged. <code>a</code> is a value from 0.0 to 1.0,
     * results are undefined if the <code>a</code> is outside the range [0..1].
     * <code>a</code> set to zero makes color fully transparent;
     * <code>a</code> set to 1.0 makes color fully opaque.
     *
     * @param a alpha component [0..1] of color
     */
    public final void setAlpha(float a) {
        mA = a;
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
     * Retrieves alpha/opacity from the color used when stroking and filling.
     *
     * @return alpha ranging from zero, fully transparent, to one, fully opaque
     */
    public final float alpha() {
        return mA;
    }

    /**
     * Returns the value of the red component in the range \([0..1]\).
     *
     * @see #alpha()
     * @see #green()
     * @see #blue()
     */
    public final float red() {
        return mR;
    }

    /**
     * Returns the value of the green component in the range \([0..1]\).
     *
     * @see #alpha()
     * @see #red()
     * @see #blue()
     */
    public final float green() {
        return mG;
    }

    /**
     * Returns the value of the blue component in the range \([0..1]\).
     *
     * @see #alpha()
     * @see #red()
     * @see #green()
     */
    public final float blue() {
        return mB;
    }

    /**
     * Helper to setColor(), that only assigns the color's <code>r,g,b</code> values,
     * leaving its alpha value unchanged. Results are undefined if the any component
     * value is outside the range [0..255].
     *
     * @param r the new red component (0..255) of the paint's color.
     * @param g the new green component (0..255) of the paint's color.
     * @param b the new blue component (0..255) of the paint's color.
     */
    public final void setRGB(int r, int g, int b) {
        mR = r / 255.0f;
        mG = g / 255.0f;
        mB = b / 255.0f;
    }

    /**
     * Helper to setColor(), that only assigns the color's <code>r,g,b</code> values,
     * leaving its alpha value unchanged. Results are undefined if the any component
     * value is outside the range [0..1].
     *
     * @param r the new red component (0..1) of the paint's color.
     * @param g the new green component (0..1) of the paint's color.
     * @param b the new blue component (0..1) of the paint's color.
     */
    public final void setRGB(float r, float g, float b) {
        mR = r;
        mG = g;
        mB = b;
    }

    /**
     * Helper to setColor(), that takes <code>r,g,b,a</code> and constructs the color int.
     * Results are undefined if the any component value is outside the range [0..255].
     *
     * @param r the new red component (0..255) of the paint's color.
     * @param g the new green component (0..255) of the paint's color.
     * @param b the new blue component (0..255) of the paint's color.
     * @param a the new alpha component (0..255) of the paint's color.
     */
    public final void setRGBA(int r, int g, int b, int a) {
        mR = r / 255.0f;
        mG = g / 255.0f;
        mB = b / 255.0f;
        mA = a / 255.0f;
    }

    /**
     * Helper to setColor(), that takes floating point <code>r,g,b,a</code> values.
     * Results are undefined if the any component value is outside the range [0..1].
     *
     * @param r the new red component (0..1) of the paint's color.
     * @param g the new green component (0..1) of the paint's color.
     * @param b the new blue component (0..1) of the paint's color.
     * @param a the new alpha component (0..1) of the paint's color.
     */
    public final void setRGBA(float r, float g, float b, float a) {
        mR = r;
        mG = g;
        mB = b;
        mA = a;
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
        mR = r / 255.0f;
        mG = g / 255.0f;
        mB = b / 255.0f;
        mA = a / 255.0f;
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
        mR = r;
        mG = g;
        mB = b;
        mA = a;
    }

    /**
     * Return the paint's style, used for controlling how primitives' geometries
     * are interpreted, except where noted.
     *
     * @return the paint's style setting (fill, stroke or both)
     */
    public final int getStyle() {
        return mFlags & STYLE_MASK;
    }

    /**
     * Set the paint's style, used for controlling how primitives' geometries
     * are interpreted, except where noted.
     *
     * @param style the new style to set in the paint
     */
    public final void setStyle(@Style int style) {
        mFlags = (mFlags & ~STYLE_MASK) | (style & STYLE_MASK);
    }

    /**
     * Set paint's style to STROKE if true, or FILL if false.
     *
     * @param stroke true to stroke shapes, false to fill shapes
     */
    public final void setStroke(boolean stroke) {
        mFlags = (mFlags & ~STYLE_MASK) | (stroke ? STROKE : FILL);
    }

    /**
     * Return the paint's Cap, controlling how the start and end of stroked
     * lines and paths are treated, except where noted.
     *
     * @return the line cap style for the paint
     */
    public final int getStrokeCap() {
        return mFlags & CAP_MASK;
    }

    /**
     * Set the paint's Cap, controlling how the start and end of stroked
     * lines and paths are treated, except where noted.
     *
     * @param cap set the paint's line cap style
     */
    public final void setStrokeCap(@Cap int cap) {
        mFlags = (mFlags & ~CAP_MASK) | (cap & CAP_MASK);
    }

    /**
     * Return the paint's stroke join type.
     *
     * @return the paint's Join
     */
    public final int getStrokeJoin() {
        return mFlags & JOIN_MASK;
    }

    /**
     * Set the paint's Join.
     *
     * @param join set the paint's Join
     */
    public final void setStrokeJoin(@Join int join) {
        mFlags = (mFlags & ~JOIN_MASK) | (join & JOIN_MASK);
    }

    /**
     * Return the paint's stroke align type.
     *
     * @return the paint's Align
     */
    public final int getStrokeAlign() {
        return mFlags & ALIGN_MASK;
    }

    /**
     * Set the paint's Align.
     *
     * @param align set the paint's Align
     */
    public final void setStrokeAlign(@Align int align) {
        mFlags = (mFlags & ~ALIGN_MASK) | (align & ALIGN_MASK);
    }

    /**
     * Return the brush width/thickness/weight/size for stroking. The default value is 4.0 px.
     * The half of the stroke width will be used as the stroke radius by analytic geometry.
     * <p>
     * A value of 0 will draw nothing.
     *
     * @return the paint's stroke width
     */
    public final float getStrokeWidth() {
        return mWidth;
    }

    /**
     * Set the brush width/thickness/weight/size for stroking. The default value is 4.0 px.
     * The half of the stroke width will be used as the stroke radius by analytic geometry.
     * <p>
     * Negative values are treated as 0, and a value of 0 will draw nothing.
     *
     * @param width set the paint's stroke width
     */
    public final void setStrokeWidth(float width) {
        mWidth = Math.max(width, 0);
    }

    /**
     * Returns the miter limit at which a sharp corner is drawn beveled.
     * The default value is 4.0 px.
     *
     * @return zero and greater miter limit
     */
    public final float getStrokeMiter() {
        return mMiter;
    }

    /**
     * Sets the miter limit at which a sharp corner is drawn beveled.
     * The default value is 4.0 px.
     * <p>
     * Valid values are zero and greater, negative values are treated as 0.
     *
     * @param miter zero and greater miter limit
     */
    public final void setStrokeMiter(float miter) {
        mMiter = Math.max(miter, 0);
    }

    /**
     * Return the current feather radius. The default value is 2.0 px.
     * <p>
     * Feather effect is used to smooth or blur the edge of primitives by analytic geometry.
     * You can also think of it as the softness (100% minus hardness) of the brush. Where
     * setting feather to the half of the stroke width is seen as hardness=0%. For example,
     * the default stroke width is 4.0 px, feather radius is 2.0 px and line cap is ROUND,
     * this will be equal to Photoshop's soft round brush whose hardness=0%. This will not
     * affect the anti-aliasing. Note that there is no limit to the feather radius.
     *
     * @return the paint's feather radius, zero or greater
     * @see #setFeather(float)
     */
    public final float getFeather() {
        return mFeather;
    }

    /**
     * Set the feather radius in pixels for this paint. The default value is 2.0 px.
     * <p>
     * Feather effect is used to smooth or blur the edge of primitives by analytic geometry.
     * You can also think of it as the softness (100% minus hardness) of the brush. Where
     * setting feather to the half of the stroke width is seen as hardness=0%. For example,
     * the default stroke width is 4.0 px, feather radius is 2.0 px and line cap is ROUND,
     * this will be equal to Photoshop's soft round brush whose hardness=0%. This will not
     * affect the anti-aliasing. Note that there is no limit to the feather radius.
     * <p>
     * Negative values are treated as 0.
     *
     * @param feather the paint's feather radius, zero or greater
     */
    public final void setFeather(float feather) {
        mFeather = Math.max(feather, 0);
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
     * Returns ColorFilter if set, or null.
     *
     * @return ColorFilter if previously set, null otherwise
     */
    @Nullable
    public final ColorFilter getColorFilter() {
        return mColorFilter;
    }

    /**
     * Sets ColorFilter to filter, pass null to clear ColorFilter.
     *
     * @param colorFilter ColorFilter to apply to subsequent draw
     */
    public final void setColorFilter(@Nullable ColorFilter colorFilter) {
        mColorFilter = colorFilter;
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
    public final void setBlendMode(@Nullable BlendMode blendMode) {
        if (blendMode != null) {
            int value = BlendMode.toValue(blendMode) << BLEND_MODE_SHIFT;
            mFlags = (mFlags & ~BLEND_MODE_MASK) | value;
        } else {
            mFlags = (mFlags & ~BLEND_MODE_MASK) | (DEFAULT_FLAGS | BLEND_MODE_MASK);
        }
    }

    /**
     * Get the paint's blend mode. By default, returns {@link BlendMode#SRC_OVER}.
     *
     * @return the paint's blend mode used to combine source color with destination color
     */
    @Nonnull
    public final BlendMode getBlendMode() {
        return BlendMode.fromValue((mFlags & BLEND_MODE_MASK) >> BLEND_MODE_SHIFT);
    }

    /**
     * Returns true if BlendMode is SRC_OVER, the default.
     *
     * @return true if BlendMode is SRC_OVER
     */
    public final boolean isSrcOver() {
        return (mFlags & BLEND_MODE_MASK) == DEFAULT_FLAGS;
    }

    /**
     * Returns MaskFilter if set, or null.
     *
     * @return MaskFilter if previously set, null otherwise
     */
    @Nullable
    public final MaskFilter getMaskFilter() {
        return mMaskFilter;
    }

    /**
     * Sets MaskFilter, pass null to clear MaskFilter and leave MaskFilter effect on
     * mask alpha unaltered.
     *
     * @param maskFilter modifies clipping mask generated from drawn geometry
     */
    public final void setMaskFilter(@Nullable MaskFilter maskFilter) {
        mMaskFilter = maskFilter;
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
    public final boolean nothingToDraw() {
        switch (getBlendMode()) {
            case SRC_OVER, SRC_ATOP, DST_OUT, DST_OVER, PLUS, MINUS -> {
                if (mShader == null && getAlpha() == 0) {
                    return !isBlendedColorFilter(mColorFilter) && !isBlendedImageFilter(mImageFilter);
                }
            }
            case DST -> {
                return true;
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
                multiplier = Math.max(multiplier, mMiter);
            }
            if (getStrokeCap() == CAP_SQUARE) {
                multiplier = Math.max(multiplier, MathUtil.SQRT_OF_TWO);
            }
            // width or radius
            float stroke = mWidth * multiplier;
            if (align == ALIGN_CENTER) {
                stroke *= 0.5;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Paint paint = (Paint) o;
        if (mFlags != paint.mFlags) return false;
        if (Float.floatToIntBits(paint.mR) != Float.floatToIntBits(mR)) return false;
        if (Float.floatToIntBits(paint.mG) != Float.floatToIntBits(mG)) return false;
        if (Float.floatToIntBits(paint.mB) != Float.floatToIntBits(mB)) return false;
        if (Float.floatToIntBits(paint.mA) != Float.floatToIntBits(mA)) return false;
        if (Float.floatToIntBits(paint.mWidth) != Float.floatToIntBits(mWidth)) return false;
        if (Float.floatToIntBits(paint.mMiter) != Float.floatToIntBits(mMiter)) return false;
        if (Float.floatToIntBits(paint.mFeather) != Float.floatToIntBits(mFeather)) return false;
        if (!Objects.equals(mShader, paint.mShader)) return false;
        if (!Objects.equals(mMaskFilter, paint.mMaskFilter)) return false;
        if (!Objects.equals(mColorFilter, paint.mColorFilter)) return false;
        return Objects.equals(mImageFilter, paint.mImageFilter);
    }

    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(mR);
        result = 31 * result + Float.floatToIntBits(mG);
        result = 31 * result + Float.floatToIntBits(mB);
        result = 31 * result + Float.floatToIntBits(mA);
        result = 31 * result + Float.floatToIntBits(mWidth);
        result = 31 * result + Float.floatToIntBits(mMiter);
        result = 31 * result + Float.floatToIntBits(mFeather);
        result = 31 * result + (mShader != null ? mShader.hashCode() : 0);
        result = 31 * result + (mMaskFilter != null ? mMaskFilter.hashCode() : 0);
        result = 31 * result + (mColorFilter != null ? mColorFilter.hashCode() : 0);
        result = 31 * result + (mImageFilter != null ? mImageFilter.hashCode() : 0);
        result = 31 * result + mFlags;
        return result;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Paint{");
        s.append("mColor=(");
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
        } else if (cap == CAP_BUTT) {
            s.append("BUTT");
        } else {
            s.append("SQUARE");
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
        s.append(", mStrokeWidth=");
        s.append(mWidth);
        s.append(", mStrokeMiter=");
        s.append(mMiter);
        s.append(", mFeather=");
        s.append(mFeather);
        s.append(", mShader=");
        s.append(mShader);
        s.append(", mColorFilter=");
        s.append(mColorFilter);
        s.append(", mBlendMode=");
        s.append(getBlendMode());
        s.append(", mMaskFilter=");
        s.append(mMaskFilter);
        s.append(", mImageFilter=");
        s.append(mImageFilter);
        s.append('}');
        return s.toString();
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
        BlendMode mode = paint.getBlendMode();
        return mode == BlendMode.SRC_OVER || mode == BlendMode.SRC;
    }

    public static int getAlphaDirect(@Nullable Paint paint) {
        return paint != null ? paint.getAlpha() : 0xFF;
    }

    @Nonnull
    public static BlendMode getBlendModeDirect(@Nullable Paint paint) {
        return paint != null ? paint.getBlendMode() : BlendMode.SRC_OVER;
    }
}
