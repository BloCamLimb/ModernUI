/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.text;

import icyllis.modernui.annotation.IntRange;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.text.CharUtils;
import icyllis.modernui.graphics.text.FontMetricsInt;
import icyllis.modernui.graphics.text.ShapedText;
import icyllis.modernui.graphics.text.TextRunShaper;
import icyllis.modernui.text.style.ParagraphStyle;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * A BoringLayout is a very simple Layout implementation for text that
 * fits on a single line and is all left-to-right characters.
 * You will probably never want to make one of these yourself;
 * if you do, be sure to call {@link #isBoring} first to make sure
 * the text meets the criteria.
 * <p>
 * This class is used by widgets to control text layout. You should not need
 * to use this class directly unless you are implementing your own widget
 * or custom display object, in which case
 * you are encouraged to use a Layout.</p>
 */
public class BoringLayout extends Layout implements TextUtils.EllipsizeCallback {

    /**
     * Utility function to construct a BoringLayout instance.
     *
     * @param source     the text to render
     * @param paint      the default paint for the layout
     * @param outerWidth the wrapping width for the text
     * @param align      whether to left, right, or center the text
     * @param metrics    {@code #Metrics} instance that contains information about FontMetrics and
     *                   line width
     * @param includePad set whether to include extra space beyond font ascent and descent which is
     *                   needed to avoid clipping in some scripts
     */
    public static BoringLayout make(CharSequence source, TextPaint paint, int outerWidth,
                                    Alignment align, float spacingMult, float spacingAdd, BoringLayout.Metrics metrics,
                                    boolean includePad) {
        return new BoringLayout(source, paint, outerWidth, align, spacingMult, spacingAdd, metrics,
                includePad);
    }

    /**
     * Utility function to construct a BoringLayout instance.
     *
     * @param source          the text to render
     * @param paint           the default paint for the layout
     * @param outerWidth      the wrapping width for the text
     * @param align           whether to left, right, or center the text
     * @param metrics         {@code #Metrics} instance that contains information about FontMetrics and
     *                        line width
     * @param includePad      set whether to include extra space beyond font ascent and descent which is
     *                        needed to avoid clipping in some scripts
     * @param ellipsize       whether to ellipsize the text if width of the text is longer than the
     *                        requested width
     * @param ellipsizedWidth the width to which this Layout is ellipsizing. If {@code ellipsize} is
     *                        {@code null}, or is {@link TextUtils.TruncateAt#MARQUEE} this value is
     *                        not used, {@code outerWidth} is used instead
     */
    public static BoringLayout make(CharSequence source, TextPaint paint, int outerWidth,
                                    Alignment align, float spacingMult, float spacingAdd, BoringLayout.Metrics metrics,
                                    boolean includePad, TextUtils.TruncateAt ellipsize, int ellipsizedWidth) {
        return new BoringLayout(source, paint, outerWidth, align, spacingMult, spacingAdd, metrics,
                includePad, ellipsize, ellipsizedWidth);
    }

    /**
     * Returns a BoringLayout for the specified text, potentially reusing
     * this one if it is already suitable.  The caller must make sure that
     * no one is still using this Layout.
     *
     * @param source     the text to render
     * @param paint      the default paint for the layout
     * @param outerWidth the wrapping width for the text
     * @param align      whether to left, right, or center the text
     * @param metrics    {@code #Metrics} instance that contains information about FontMetrics and
     *                   line width
     * @param includePad set whether to include extra space beyond font ascent and descent which is
     *                   needed to avoid clipping in some scripts
     */
    public BoringLayout replaceOrMake(CharSequence source, TextPaint paint, int outerWidth,
                                      Alignment align, float spacingMult, float spacingAdd,
                                      BoringLayout.Metrics metrics,
                                      boolean includePad) {
        replaceWith(source, paint, outerWidth, align, spacingMult, spacingAdd);

        mEllipsizedWidth = outerWidth;
        mEllipsizedStart = 0;
        mEllipsizedCount = 0;

        init(source, paint, align, metrics, includePad, true);
        return this;
    }

    /**
     * Returns a BoringLayout for the specified text, potentially reusing
     * this one if it is already suitable.  The caller must make sure that
     * no one is still using this Layout.
     *
     * The spacing multiplier and additional amount spacing are not used by BoringLayout.
     * {@link Layout#getSpacingMultiplier()} will return 1.0 and {@link Layout#getSpacingAdd()} will
     * return 0.0.
     *
     * @param source the text to render
     * @param paint the default paint for the layout
     * @param outerWidth the wrapping width for the text
     * @param align whether to left, right, or center the text
     * @param metrics {@code #Metrics} instance that contains information about FontMetrics and
     *                line width
     * @param includePad set whether to include extra space beyond font ascent and descent which is
     *                   needed to avoid clipping in some scripts
     * @param ellipsize whether to ellipsize the text if width of the text is longer than the
     *                  requested width. null if ellipsis not applied.
     * @param ellipsizedWidth the width to which this Layout is ellipsizing. If {@code ellipsize} is
     *                        {@code null}, or is {@link TextUtils.TruncateAt#MARQUEE} this value is
     *                        not used, {@code outerWidth} is used instead
     */
    @NonNull
    public BoringLayout replaceOrMake(@NonNull CharSequence source,
                                      @NonNull TextPaint paint, @IntRange(from = 0) int outerWidth,
                                      @NonNull Alignment align,
                                      @NonNull BoringLayout.Metrics metrics, boolean includePad,
                                      @Nullable TextUtils.TruncateAt ellipsize,
                                      @IntRange(from = 0) int ellipsizedWidth) {
        return replaceOrMake(source, paint, outerWidth, align, 1.0f, 0.0f, metrics, includePad,
                ellipsize, ellipsizedWidth);
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    @NonNull
    public BoringLayout replaceOrMake(@NonNull CharSequence source,
                                      @NonNull TextPaint paint, @IntRange(from = 0) int outerWidth,
                                      @NonNull Alignment align, float spacingMultiplier, float spacingAmount,
                                      @NonNull BoringLayout.Metrics metrics, boolean includePad,
                                      @Nullable TextUtils.TruncateAt ellipsize,
                                      @IntRange(from = 0) int ellipsizedWidth) {
        boolean trust;

        if (ellipsize == null || ellipsize == TextUtils.TruncateAt.MARQUEE) {
            replaceWith(source, paint, outerWidth, align, 1f, 0f);

            mEllipsizedWidth = outerWidth;
            mEllipsizedStart = 0;
            mEllipsizedCount = 0;
            trust = true;
        } else {
            replaceWith(TextUtils.ellipsize(source, paint, ellipsizedWidth, ellipsize, true, this),
                    paint, outerWidth, align, spacingMultiplier, spacingAmount);

            mEllipsizedWidth = ellipsizedWidth;
            trust = false;
        }

        init(getText(), paint, align, metrics, includePad, trust);
        return this;
    }

    private ShapedText mDirect;
    private TextPaint mPaint;

    /* package */ int mBottom, mDesc;   // for Direct
    private int mTopPadding, mBottomPadding;
    private float mMax;
    private int mEllipsizedWidth, mEllipsizedStart, mEllipsizedCount;

    /**
     * @param source     the text to render
     * @param paint      the default paint for the layout
     * @param outerWidth the wrapping width for the text
     * @param align      whether to left, right, or center the text
     * @param metrics    {@code #Metrics} instance that contains information about FontMetrics and
     *                   line width
     * @param includePad set whether to include extra space beyond font ascent and descent which is
     *                   needed to avoid clipping in some scripts
     */
    public BoringLayout(CharSequence source, TextPaint paint, int outerWidth, Alignment align,
                        float spacingMult, float spacingAdd, BoringLayout.Metrics metrics, boolean includePad) {
        super(source, paint, outerWidth, align, spacingMult, spacingAdd);

        mEllipsizedWidth = outerWidth;
        mEllipsizedStart = 0;
        mEllipsizedCount = 0;

        init(source, paint, align, metrics, includePad, true);
    }

    /**
     * @param source          the text to render
     * @param paint           the default paint for the layout
     * @param outerWidth      the wrapping width for the text
     * @param align           whether to left, right, or center the text
     * @param metrics         {@code #Metrics} instance that contains information about FontMetrics and
     *                        line width
     * @param includePad      set whether to include extra space beyond font ascent and descent which is
     *                        needed to avoid clipping in some scripts
     * @param ellipsize       whether to ellipsize the text if width of the text is longer than the
     *                        requested {@code outerwidth}
     * @param ellipsizedWidth the width to which this Layout is ellipsizing. If {@code ellipsize} is
     *                        {@code null}, or is {@link TextUtils.TruncateAt#MARQUEE} this value is
     *                        not used, {@code outerwidth} is used instead
     */
    public BoringLayout(CharSequence source, TextPaint paint, int outerWidth, Alignment align,
                        float spacingMult, float spacingAdd, BoringLayout.Metrics metrics, boolean includePad,
                        TextUtils.TruncateAt ellipsize, int ellipsizedWidth) {
        super(source, paint, outerWidth, align, spacingMult, spacingAdd);

        boolean trust;

        if (ellipsize == null || ellipsize == TextUtils.TruncateAt.MARQUEE) {
            mEllipsizedWidth = outerWidth;
            mEllipsizedStart = 0;
            mEllipsizedCount = 0;
            trust = true;
        } else {
            replaceWith(TextUtils.ellipsize(source, paint, ellipsizedWidth, ellipsize, true, this),
                    paint, outerWidth, align, spacingMult, spacingAdd);

            mEllipsizedWidth = ellipsizedWidth;
            trust = false;
        }

        init(getText(), paint, align, metrics, includePad, trust);
    }

    /* package */ void init(CharSequence source, TextPaint paint, Alignment align,
                            BoringLayout.Metrics metrics, boolean includePad, boolean trustWidth) {
        int spacing;

        if (source instanceof String) {
            String direct = source.toString();
            int len = direct.length();
            mDirect = TextRunShaper.shapeTextRun(
                    direct,
                    0, len,
                    0, len,
                    false,
                    paint.getInternalPaint()
            );
        } else {
            mDirect = null;
        }

        mPaint = paint;

        if (false/*includePad*/) {
            /*spacing = metrics.bottom - metrics.top;
            mDesc = metrics.bottom;*/
        } else {
            spacing = metrics.descent - metrics.ascent;
            mDesc = metrics.descent;
        }

        mBottom = spacing;

        if (trustWidth) {
            mMax = metrics.width;
        } else {
            /*
             * If we have ellipsized, we have to actually calculate the
             * width because the width that was passed in was for the
             * full text, not the ellipsized form.
             */
            TextLine line = TextLine.obtain();
            line.set(paint, source, 0, source.length(), Layout.DIR_LEFT_TO_RIGHT,
                    Directions.ALL_LEFT_TO_RIGHT, false, null,
                    mEllipsizedStart, mEllipsizedStart + mEllipsizedCount);
            mMax = (int) Math.ceil(line.metrics(null));
            line.recycle();
        }

        /*if (includePad) {
            mTopPadding = metrics.top - metrics.ascent;
            mBottomPadding = metrics.bottom - metrics.descent;
        }*/
    }

    /**
     * Determine and compute metrics if given text can be handled by BoringLayout.
     *
     * @param text  a text
     * @param paint a paint
     * @return layout metric for the given text. null if given text is unable to be handled by
     * BoringLayout.
     */
    public static Metrics isBoring(CharSequence text, TextPaint paint) {
        return isBoring(text, paint, TextDirectionHeuristics.FIRSTSTRONG_LTR, null);
    }

    /**
     * Determine and compute metrics if given text can be handled by BoringLayout.
     *
     * @param text    a text
     * @param paint   a paint
     * @param metrics a metrics object to be recycled. If null is passed, this function creat new
     *                object.
     * @return layout metric for the given text. If metrics is not null, this method fills values
     * to given metrics object instead of allocating new metrics object. null if given text
     * is unable to be handled by BoringLayout.
     */
    public static Metrics isBoring(CharSequence text, TextPaint paint, Metrics metrics) {
        return isBoring(text, paint, TextDirectionHeuristics.FIRSTSTRONG_LTR, metrics);
    }

    /**
     * Returns true if the text contains any RTL characters, bidi format characters, or surrogate
     * code units (SMP characters).
     */
    private static boolean hasAnyInterestingChars(CharSequence text, int textLength) {
        final int MAX_BUF_LEN = 500;
        final char[] buffer = CharUtils.obtain(MAX_BUF_LEN);
        try {
            for (int start = 0; start < textLength; start += MAX_BUF_LEN) {
                final int end = Math.min(start + MAX_BUF_LEN, textLength);

                // No need to worry about getting half codepoints, since we consider surrogate code
                // units "interesting" as soon we see one.
                CharUtils.getChars(text, start, end, buffer, 0);

                final int len = end - start;
                for (int i = 0; i < len; i++) {
                    final char c = buffer[i];
                    if (c == '\n' || c == '\t' || TextUtils.couldAffectRtl(c)) {
                        return true;
                    }
                }
            }
            return false;
        } finally {
            CharUtils.recycle(buffer);
        }
    }

    /**
     * Returns null if not boring; the width, ascent, and descent in the
     * provided Metrics object (or a new one if the provided one was null)
     * if boring.
     *
     * @hidden
     */
    @Nullable
    public static Metrics isBoring(@Nonnull CharSequence text, TextPaint paint,
                                   TextDirectionHeuristic textDir, Metrics metrics) {
        final int textLength = text.length();
        if (hasAnyInterestingChars(text, textLength)) {
            return null;  // There are some interesting characters. Not boring.
        }
        if (textDir != null
                // isRtl() will never return true for these three cases if there's no interesting chars
                && textDir != TextDirectionHeuristics.LTR
                && textDir != TextDirectionHeuristics.FIRSTSTRONG_LTR
                && textDir != TextDirectionHeuristics.ANYRTL_LTR
                && textDir.isRtl(text, 0, textLength)) {
            return null;  // The heuristic considers the paragraph direction RTL. Not boring.
        }
        if (text instanceof Spanned sp) {
            List<?> styles = sp.getSpans(0, textLength, ParagraphStyle.class);
            if (!styles.isEmpty()) {
                return null;  // There are some ParagraphStyle spans. Not boring.
            }
        }

        Metrics fm = metrics;
        if (fm == null) {
            fm = new Metrics();
        } else {
            fm.reset();
        }

        TextLine line = TextLine.obtain();
        line.set(paint, text, 0, textLength, Layout.DIR_LEFT_TO_RIGHT,
                Directions.ALL_LEFT_TO_RIGHT, false, null,
                0 /* ellipsisStart, 0 since text has not been ellipsized at this point */,
                0 /* ellipsisEnd, 0 since text has not been ellipsized at this point */);
        fm.width = (int) Math.ceil(line.metrics(fm));
        line.recycle();

        return fm;
    }

    @Override
    public int getHeight() {
        return mBottom;
    }

    @Override
    public int getLineCount() {
        return 1;
    }

    @Override
    public int getLineTop(int line) {
        if (line == 0)
            return 0;
        else
            return mBottom;
    }

    @Override
    public int getLineDescent(int line) {
        return mDesc;
    }

    @Override
    public int getLineStart(int line) {
        if (line == 0)
            return 0;
        else
            return getText().length();
    }

    @Override
    public int getParagraphDirection(int line) {
        return DIR_LEFT_TO_RIGHT;
    }

    @Override
    public boolean getLineContainsTab(int line) {
        return false;
    }

    @Override
    public float getLineMax(int line) {
        return mMax;
    }

    @Override
    public float getLineWidth(int line) {
        return (line == 0 ? mMax : 0);
    }

    @Override
    public final Directions getLineDirections(int line) {
        return Directions.ALL_LEFT_TO_RIGHT;
    }

    @Override
    public int getTopPadding() {
        return mTopPadding;
    }

    @Override
    public int getBottomPadding() {
        return mBottomPadding;
    }

    @Override
    public int getEllipsisCount(int line) {
        return mEllipsizedCount;
    }

    @Override
    public int getEllipsisStart(int line) {
        return mEllipsizedStart;
    }

    @Override
    public int getEllipsizedWidth() {
        return mEllipsizedWidth;
    }

    // Override draw so it will be faster.
    @Override
    public void drawText(@NonNull Canvas canvas, int firstLine, int lastLine) {
        if (mDirect != null) {
            // Added by Modern UI
            Alignment align = getAlignment();
            final int x;
            if (align == Alignment.ALIGN_NORMAL || align == Alignment.ALIGN_LEFT) {
                x = 0;
            } else {
                int max = (int) mMax;
                if (align == Alignment.ALIGN_OPPOSITE || align == Alignment.ALIGN_RIGHT) {
                    x = getWidth() - max;
                } else { // Alignment.ALIGN_CENTER
                    max = max & ~1;
                    x = (getWidth() - max) >> 1;
                }
            }
            canvas.drawShapedText(mDirect, x, mBottom - mDesc, mPaint);
        } else {
            super.drawText(canvas, firstLine, lastLine);
        }
    }

    /**
     * Callback for the ellipsizer to report what region it ellipsized.
     */
    @Override
    public void ellipsized(int start, int end) {
        mEllipsizedStart = start;
        mEllipsizedCount = end - start;
    }

    public static class Metrics extends FontMetricsInt {

        public int width;

        @Override
        public void reset() {
            super.reset();
            width = 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            Metrics metrics = (Metrics) o;

            return width == metrics.width;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + width;
            return result;
        }

        @Override
        public String toString() {
            return super.toString() + ", width=" + width;
        }
    }
}
