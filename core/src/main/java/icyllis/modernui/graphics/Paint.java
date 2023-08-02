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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
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
public class Paint extends icyllis.arc3d.core.Paint {

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

    public static final int FONT_STYLE_MASK = NORMAL | BOLD | ITALIC;

    static final int TEXT_ANTI_ALIAS_DEFAULT = 0x0;
    static final int TEXT_ANTI_ALIAS_OFF = 0x8;
    static final int TEXT_ANTI_ALIAS_ON = 0xC;
    static final int TEXT_ANTI_ALIAS_MASK = 0xC;

    public static final int LINEAR_TEXT_FLAG = 0x10;

    // the recycle bin, see obtain()
    private static final Paint[] sPool = new Paint[8];
    @GuardedBy("sPool")
    private static int sPoolSize;

    // style + rendering hints (+ text decoration)
    protected int mFontFlags;
    protected float mFontSize;

    /**
     * Creates a new Paint with defaults.
     *
     * @see #obtain()
     */
    public Paint() {
        super();
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
    @Override
    public void reset() {
        super.reset();
        mFontFlags = NORMAL;
        mFontSize = 16;
    }

    /**
     * Set all contents of this paint from the specified paint.
     *
     * @param paint the paint to set this paint from
     */
    public void set(Paint paint) {
        super.set(paint);
        if (paint != null) {
            mFontFlags = paint.mFontFlags;
            mFontSize = paint.mFontSize;
        }
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
        setBlendMode(mode != null ? mode.ordinal() : -1);
    }

    /**
     * Get the paint's blend mode. By default, returns {@link icyllis.arc3d.core.BlendMode#SRC_OVER}.
     * A null value means a custom blend.
     *
     * @return the paint's blend mode used to combine source color with destination color
     */
    @Nullable
    public final BlendMode getBlendMode() {
        var mode = getBlendModeDirect(this);
        return mode == -1 ? null : BlendMode.VALUES[mode];
    }

    /**
     * Get desired text's style, combination of NORMAL, BOLD and ITALIC.
     *
     * @return the desired style of the font
     */
    @TextStyle
    public final int getTextStyle() {
        return getFontStyle();
    }

    public int getFontStyle() {
        return mFontFlags & FONT_STYLE_MASK;
    }

    /**
     * Set desired text's style, combination of NORMAL, BOLD and ITALIC.
     * If the font family does not support this style natively, our engine
     * will use a simulation algorithm, also known as fake bold and fake italic.
     * The default value is NORMAL.
     *
     * @param textStyle the desired style of the font
     */
    public final void setTextStyle(@TextStyle int textStyle) {
        setFontStyle(textStyle);
    }

    public void setFontStyle(int fontStyle) {
        if ((fontStyle & ~FONT_STYLE_MASK) == 0) {
            mFontFlags |= fontStyle;
        } else {
            mFontFlags &= ~FONT_STYLE_MASK;
        }
    }

    /**
     * Return the paint's text size in pixel units. For example, a text size
     * of 16 means the letter 'M' is 16 pixels high in device space.
     * For performance reasons, this value is always rounded to an integer.
     * <p>
     * The default value is 16.
     *
     * @return the paint's text size in pixel units.
     */
    public float getTextSize() {
        return mFontSize;
    }

    public int getFontSize() {
        return (int) (mFontSize + 0.5);
    }

    /**
     * Set the paint's text size in pixel units. For example, a text size
     * of 16 (1em) means the letter 'M' is 16 pixels high in device space.
     * For performance reasons, this value is always rounded to an integer,
     * and clamps to 8 and 96. You can have even larger glyphs through matrix
     * transformation, and our engine will attempt to use SDF text rendering.
     * <p>
     * Note: the point size is measured at 72 dpi, while Windows has 96 dpi.
     * This indicates that the font size 12 in MS Word is equal to the font
     * size 16 here (12 * 4/3 == 16).
     * <p>
     * The default value is 16.
     *
     * @param textSize set the paint's text size in pixel units.
     */
    public void setTextSize(float textSize) {
        if (textSize > 0) {
            mFontSize = textSize;
        }
    }

    public void setFontSize(int fontSize) {
        if (fontSize > 0) {
            mFontSize = fontSize;
        }
    }

    public boolean isTextAntiAlias() {
        return switch (mFontFlags & TEXT_ANTI_ALIAS_MASK) {
            case TEXT_ANTI_ALIAS_ON -> true;
            case TEXT_ANTI_ALIAS_OFF -> false;
            default -> isAntiAlias();
        };
    }

    public void setTextAntiAlias(boolean textAntiAlias) {
        mFontFlags = (mFontFlags & ~TEXT_ANTI_ALIAS_MASK) |
                (textAntiAlias ? TEXT_ANTI_ALIAS_ON : TEXT_ANTI_ALIAS_OFF);
    }

    /**
     * @return whether to enable linear text
     */
    public boolean isLinearText() {
        return (mFontFlags & LINEAR_TEXT_FLAG) != 0;
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
            mFontFlags &= ~LINEAR_TEXT_FLAG;
        } else {
            mFontFlags |= LINEAR_TEXT_FLAG;
        }
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + mFontFlags;
        result = 31 * result + Float.hashCode(mFontSize);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Paint paint)) return false;
        return super.equals(paint) &&
                mFontFlags == paint.mFontFlags &&
                mFontSize == paint.mFontSize;
    }
}
