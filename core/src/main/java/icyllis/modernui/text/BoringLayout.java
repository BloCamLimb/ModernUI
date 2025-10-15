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

import com.ibm.icu.text.Bidi;
import icyllis.modernui.annotation.IntRange;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.text.CharUtils;
import icyllis.modernui.graphics.text.FontMetricsInt;
import icyllis.modernui.graphics.text.LayoutCache;
import icyllis.modernui.graphics.text.ShapedText;
import org.jetbrains.annotations.ApiStatus;

/**
 * A BoringLayout is a simple Layout implementation for text that
 * fits on a single line and is not {@link Spanned}.
 * You will probably never want to make one of these yourself;
 * if you do, be sure to call {@link #isBoring} first to make sure
 * the text meets the criteria.
 * <p>
 * This class is used by widgets to control text layout. You should not need
 * to use this class directly unless you are implementing your own widget
 * or custom display object, in which case
 * you are encouraged to use a Layout.</p>
 */
public class BoringLayout extends Layout {
    //TODO make use of PrecomputedText

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
    @NonNull
    public static BoringLayout make(@NonNull CharSequence source, @NonNull TextPaint paint, int outerWidth,
                                    @NonNull Alignment align, float spacingMult, float spacingAdd,
                                    @NonNull BoringLayout.Metrics metrics, boolean includePad) {
        return make(source, paint, outerWidth, align, TextDirectionHeuristics.FIRSTSTRONG_LTR,
                spacingMult, spacingAdd, metrics, includePad);
    }

    /**
     * Utility function to construct a BoringLayout instance.
     *
     * @param source      the text to render
     * @param paint       the default paint for the layout
     * @param outerWidth  the wrapping width for the text
     * @param align       whether to left, right, or center the text
     * @param textDir     the text direction
     * @param spacingMult not used by BoringLayout, but returned by {@link Layout#getSpacingMultiplier()}
     * @param spacingAdd  not used by BoringLayout, but returned by {@link Layout#getSpacingAdd()}
     * @param metrics     {@link Metrics} instance that contains information about FontMetrics,
     *                    line width, and BiDi algorithm
     * @param includePad  set whether to include extra space beyond font ascent and descent which is
     *                    needed to avoid clipping in some scripts
     */
    @NonNull
    public static BoringLayout make(@NonNull CharSequence source, @NonNull TextPaint paint, int outerWidth,
                                    @NonNull Alignment align, @NonNull TextDirectionHeuristic textDir,
                                    float spacingMult, float spacingAdd, @NonNull BoringLayout.Metrics metrics,
                                    boolean includePad) {
        return new BoringLayout(source, paint, outerWidth, align, textDir, spacingMult, spacingAdd, metrics,
                includePad);
    }

    @NonNull
    public static BoringLayout make(@NonNull CharSequence source, @NonNull TextPaint paint, int outerWidth,
                                    @NonNull Alignment align, float spacingMult, float spacingAdd,
                                    @NonNull BoringLayout.Metrics metrics, boolean includePad,
                                    @Nullable TextUtils.TruncateAt ellipsize, int ellipsizedWidth) {
        return make(source, paint, outerWidth, align, TextDirectionHeuristics.FIRSTSTRONG_LTR,
                spacingMult, spacingAdd, metrics, includePad, ellipsize, ellipsizedWidth);
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
    @NonNull
    public static BoringLayout make(@NonNull CharSequence source, @NonNull TextPaint paint, int outerWidth,
                                    @NonNull Alignment align, @NonNull TextDirectionHeuristic textDir,
                                    float spacingMult, float spacingAdd, @NonNull BoringLayout.Metrics metrics,
                                    boolean includePad, @Nullable TextUtils.TruncateAt ellipsize, int ellipsizedWidth) {
        return new BoringLayout(source, paint, outerWidth, align, textDir, spacingMult, spacingAdd, metrics,
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
    public BoringLayout replaceOrMake(@NonNull CharSequence source, @NonNull TextPaint paint, int outerWidth,
                                      @NonNull Alignment align, @NonNull TextDirectionHeuristic textDir,
                                      float spacingMult, float spacingAdd, @NonNull BoringLayout.Metrics metrics,
                                      boolean includePad) {
        replaceWith(source, paint, outerWidth, align, textDir, spacingMult, spacingAdd);

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
                                      @NonNull Alignment align, @NonNull TextDirectionHeuristic textDir,
                                      @NonNull BoringLayout.Metrics metrics, boolean includePad,
                                      @Nullable TextUtils.TruncateAt ellipsize,
                                      @IntRange(from = 0) int ellipsizedWidth) {
        return replaceOrMake(source, paint, outerWidth, align, textDir, 1.0f, 0.0f, metrics, includePad,
                ellipsize, ellipsizedWidth);
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    @NonNull
    public BoringLayout replaceOrMake(@NonNull CharSequence source,
                                      @NonNull TextPaint paint, @IntRange(from = 0) int outerWidth,
                                      @NonNull Alignment align, @NonNull TextDirectionHeuristic textDir,
                                      float spacingMultiplier, float spacingAmount,
                                      @NonNull BoringLayout.Metrics metrics, boolean includePad,
                                      @Nullable TextUtils.TruncateAt ellipsize,
                                      @IntRange(from = 0) int ellipsizedWidth) {
        boolean trust;

        if (ellipsize == null || ellipsize == TextUtils.TruncateAt.MARQUEE) {
            replaceWith(source, paint, outerWidth, align, textDir, spacingMultiplier, spacingAmount);

            mEllipsizedWidth = outerWidth;
            mEllipsizedStart = 0;
            mEllipsizedCount = 0;
            trust = true;
        } else {
            String replacement = ellipsize(source, paint, metrics, ellipsizedWidth, ellipsize);
            replaceWith(replacement != null ? replacement : source,
                    paint, outerWidth, align, textDir, spacingMultiplier, spacingAmount);

            mEllipsizedWidth = ellipsizedWidth;
            trust = replacement == null;
        }

        init(getText(), paint, align, metrics, includePad, trust);
        return this;
    }

    private ShapedText mShaped;

    /* package */ int mBottom, mDesc;
    private int mTopPadding, mBottomPadding;
    private float mMax;
    private int mEllipsizedWidth, mEllipsizedStart, mEllipsizedCount;
    private int mDir;
    private Directions mDirections;

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
                        TextDirectionHeuristic textDir, float spacingMult, float spacingAdd,
                        BoringLayout.Metrics metrics, boolean includePad) {
        super(source, paint, outerWidth, align, textDir, spacingMult, spacingAdd);

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
                        TextDirectionHeuristic textDir, float spacingMult, float spacingAdd,
                        BoringLayout.Metrics metrics, boolean includePad,
                        TextUtils.TruncateAt ellipsize, int ellipsizedWidth) {
        super(source, paint, outerWidth, align, textDir, spacingMult, spacingAdd);

        boolean trust;

        if (ellipsize == null || ellipsize == TextUtils.TruncateAt.MARQUEE) {
            mEllipsizedWidth = outerWidth;
            mEllipsizedStart = 0;
            mEllipsizedCount = 0;
            trust = true;
        } else {
            String replacement = ellipsize(source, paint, metrics, ellipsizedWidth, ellipsize);
            if (replacement != null) {
                replaceWith(replacement, paint, outerWidth, align, textDir, spacingMult, spacingAdd);
            }

            mEllipsizedWidth = ellipsizedWidth;
            trust = replacement == null;
        }

        init(getText(), paint, align, metrics, includePad, trust);
    }

    // Optimized version provided by Modern UI
    /* package */ String ellipsize(@NonNull CharSequence text, @NonNull TextPaint paint,
                                   @NonNull BoringLayout.Metrics metrics,
                                   float avail, @NonNull TextUtils.TruncateAt where) {
        if (metrics.width <= avail) {

            mEllipsizedStart = 0;
            mEllipsizedCount = 0;

            return null;
        }

        char[] ellipsis = TextUtils.getEllipsisChars(where);

        final int len = text.length();
        final ShapedText layout;
        if (metrics.bidi != null) {
            layout = new ShapedText(metrics.bidi, 0, len, paint.getInternalPaint(),
                    ShapedText.CREATE_CLUSTER_ADVANCES);
        } else {
            layout = new ShapedText(metrics.text, 0, len, 0, len,
                    ShapedText.BIDI_OVERRIDE_LTR, paint.getInternalPaint(),
                    ShapedText.CREATE_CLUSTER_ADVANCES);
        }

        // NB: ellipsis string is considered as Force LTR
        float ellipsisWidth = LayoutCache.getOrCreate(ellipsis, 0, ellipsis.length,
                0, ellipsis.length, false, paint.getInternalPaint(), 0).getAdvance();
        avail -= ellipsisWidth;

        int left = 0;
        int right = len;
        if (avail >= 0) {
            float[] advances = layout.getAdvances();
            if (where == TextUtils.TruncateAt.START) {
                right = len - TextUtils.breakText(advances, len, false, avail);
            } else if (where == TextUtils.TruncateAt.END) {
                left = TextUtils.breakText(advances, len, true, avail);
            } else {
                assert where == TextUtils.TruncateAt.MIDDLE;
                right = len - TextUtils.breakText(advances, len, false, avail / 2);
                for (int i = right; i < len; i++) {
                    avail -= advances[i];
                }
                left = TextUtils.breakText(advances, right, true, avail);
            }
        }

        mEllipsizedStart = left;
        mEllipsizedCount = right - left;

        // We're going to modify the text array, but padded with zero-width spaces to
        // preserve the original length and offsets instead of truncating
        final char[] buf = metrics.text;
        final int removed = right - left;
        final int remaining = len - removed;
        if (remaining > 0 && removed >= ellipsis.length) {
            System.arraycopy(ellipsis, 0, buf, left, ellipsis.length);
            left += ellipsis.length;
        } // else skip the ellipsis
        for (int i = left; i < right; i++) {
            buf[i] = TextUtils.ELLIPSIS_FILLER;
        }
        return new String(buf, 0, len);
    }

    /* package */ void init(CharSequence source, TextPaint paint, Alignment align,
                            BoringLayout.Metrics metrics, boolean includePad, boolean trustWidth) {
        int spacing;

        int len = source.length();
        assert len == metrics.text.length;
        /*
         * NB: The source is synchronized with the text array, and the bidi object also holds a reference to
         * the text array. We assume that the bidi result is not affected by ellipsizer, then there's no need
         * to create bidi again. Text layouts created using either the text array or bidi object will reflect
         * the (ellipsized or not) text to be displayed.
         */
        if (metrics.bidi != null) {
            assert metrics.bidi.getText() == metrics.text;
            assert metrics.bidi.getLength() == len;
            mShaped = new ShapedText(metrics.bidi, 0, len, paint.getInternalPaint(),
                    ShapedText.CREATE_POSITIONED_GLYPHS);
        } else {
            mShaped = new ShapedText(metrics.text, 0, len, 0, len,
                    ShapedText.BIDI_OVERRIDE_LTR, paint.getInternalPaint(),
                    ShapedText.CREATE_POSITIONED_GLYPHS);
        }

        assert paint == getPaint();

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

            assert mMax == (int) Math.ceil(mShaped.getAdvance());
        } else {
            /*
             * If we have ellipsized, we have to actually calculate the
             * width because the width that was passed in was for the
             * full text, not the ellipsized form.
             */
            mMax = (int) Math.ceil(mShaped.getAdvance());
        }

        Bidi bidi = metrics.bidi;
        // Easy case: bidi == null means the text is all LTR and no bidi support is needed.
        if (bidi == null) {
            mDirections = Directions.ALL_LEFT_TO_RIGHT;
        } else if (bidi.getRunCount() == 1) {
            // Easy case: If the original text only contains single directionality run, no need
            // to reorder visually.
            if (bidi.getRunLevel(0) == 1) {
                mDirections = Directions.ALL_RIGHT_TO_LEFT;
            } else if (bidi.getRunLevel(0) == 0) {
                mDirections = Directions.ALL_LEFT_TO_RIGHT;
            } else {
                mDirections = new Directions(new int[]{
                        0, bidi.getRunLevel(0) << Directions.RUN_LEVEL_SHIFT | len});
            }
        } else {
            // Reorder directionality run visually.
            byte[] levels = new byte[bidi.getRunCount()];
            for (int i = 0; i < bidi.getRunCount(); ++i) {
                levels[i] = (byte) bidi.getRunLevel(i);
            }
            int[] visualOrders = Bidi.reorderVisual(levels);

            int[] dirs = new int[bidi.getRunCount() * 2];
            for (int i = 0; i < bidi.getRunCount(); ++i) {
                int vIndex;
                if ((bidi.getBaseLevel() & 0x01) == 1) {
                    // For the historical reasons, if the base directionality is RTL, TextLine
                    // handles text from the right, i.e. the visually reordered run needs to be reversed.
                    // Even though the ShapedText used by BoringLayout is rendered from left to right
                    // (because there is no leading margins or tabs), the base Layout class may use
                    // directions for other purposes, we keep this behavior
                    vIndex = visualOrders[bidi.getRunCount() - i - 1];
                } else {
                    vIndex = visualOrders[i];
                }

                // Special packing of dire
                dirs[i * 2] = bidi.getRunStart(vIndex);
                dirs[i * 2 + 1] = bidi.getRunLevel(vIndex) << Directions.RUN_LEVEL_SHIFT
                        | (bidi.getRunLimit(vIndex) - dirs[i * 2]);
            }

            mDirections = new Directions(dirs);
        }

        if (bidi == null) {
            mDir = Layout.DIR_LEFT_TO_RIGHT;
        } else {
            mDir = (bidi.getParaLevel() & 0x01) == 0
                    ? Layout.DIR_LEFT_TO_RIGHT : Layout.DIR_RIGHT_TO_LEFT;
        }

        /*if (includePad) {
            mTopPadding = metrics.top - metrics.ascent;
            mBottomPadding = metrics.bottom - metrics.descent;
        }*/
    }

    /**
     * Determine and compute metrics if given text can be handled by BoringLayout.
     *
     * @param text    a text to be calculated text layout.
     * @param paint   a paint object used for styling.
     * @param metrics a metrics object to be recycled. If null is passed, this function create new
     *                object.
     * @return layout metric for the given text. If metrics is not null, this method fills values
     * to given metrics object instead of allocating new metrics object. null if given text
     * is unable to be handled by BoringLayout.
     */
    @Nullable
    public static Metrics isBoring(@NonNull CharSequence text, @NonNull TextPaint paint, @Nullable Metrics metrics) {
        return isBoring(text, paint, TextDirectionHeuristics.FIRSTSTRONG_LTR, metrics);
    }

    /**
     * Returns null if not boring; the width, copied chars, bidi, ascent, and descent in the
     * provided Metrics object (or a new one if the provided one was null)
     * if boring.
     *
     * @param text    a text to be calculated text layout.
     * @param paint   a paint object used for styling.
     * @param textDir a text direction.
     * @param metrics the out metrics.
     * @return metrics on success. null if text cannot be rendered by BoringLayout.
     */
    @Nullable
    public static Metrics isBoring(@NonNull CharSequence text, @NonNull TextPaint paint,
                                   @NonNull TextDirectionHeuristic textDir, @Nullable Metrics metrics) {
        return isBoring(text, paint, textDir, false /* useFallbackLineSpacing */, metrics);
    }

    /**
     * Returns null if not boring; the width, copied chars, bidi, ascent, and descent in the
     * provided Metrics object (or a new one if the provided one was null)
     * if boring.
     *
     * @param text                   a text to be calculated text layout.
     * @param paint                  a paint object used for styling.
     * @param textDir                a text direction.
     * @param useFallbackLineSpacing True for adjusting the line spacing based on fallback fonts.
     *                               False for keeping the first font's line height. If some glyphs
     *                               requires larger vertical spaces, by passing true to this
     *                               argument, the layout increase the line height to fit all glyphs.
     * @param metrics                the out metrics.
     * @return metrics on success. null if text cannot be rendered by BoringLayout.
     */
    @Nullable
    public static Metrics isBoring(@NonNull CharSequence text, @NonNull TextPaint paint,
                                   @NonNull TextDirectionHeuristic textDir, boolean useFallbackLineSpacing,
                                   @Nullable Metrics metrics) {
        if (metrics != null) {
            metrics.reset();
        }
        if (text instanceof Spanned) {
            return null; // Spanned text is not boring.
        }
        final int textLength = text.length();

        // In many cases a complete buffer is needed, so create a new one of the exact length
        final char[] buffer = new char[textLength];
        boolean needsBidi = textDir != TextDirectionHeuristics.LTR
                && textDir != TextDirectionHeuristics.FIRSTSTRONG_LTR
                && textDir != TextDirectionHeuristics.ANYRTL_LTR;
        for (int start = 0; start < textLength; ) {
            // Increment 500 at most.
            final int end = Math.min(start + 500, textLength);

            // No need to worry about getting half codepoints, since we consider surrogate code
            // units "interesting" as soon we see one.
            CharUtils.getChars(text, start, end, buffer, start);

            for (int i = start; i < end; i++) {
                final char c = buffer[i];
                if (c == '\n' || c == '\t') {
                    // If the text contains any line feeds or tabs, not boring.
                    return null;
                }
                if (!needsBidi) {
                    needsBidi = TextUtils.couldAffectRtl(c);
                }
            }

            start = end;
        }

        final Metrics fm = metrics != null ? metrics : new Metrics();

        paint.getFontMetricsInt(fm);

        fm.text = buffer;
        if (!needsBidi) {
            fm.bidi = null;

            FontMetricsInt extent = useFallbackLineSpacing ? new FontMetricsInt() : null;
            fm.width = (int) paint.measureTextRun(buffer, 0, textLength, false, extent);
            if (useFallbackLineSpacing) {
                fm.extendBy(extent);
            }
        } else {
            final byte paraLevel;
            if (textDir == TextDirectionHeuristics.LTR) {
                paraLevel = Bidi.LTR;
            } else if (textDir == TextDirectionHeuristics.RTL) {
                paraLevel = Bidi.RTL;
            } else if (textDir == TextDirectionHeuristics.FIRSTSTRONG_LTR) {
                paraLevel = Bidi.LEVEL_DEFAULT_LTR;
            } else if (textDir == TextDirectionHeuristics.FIRSTSTRONG_RTL) {
                paraLevel = Bidi.LEVEL_DEFAULT_RTL;
            } else {
                final boolean isRtl = textDir.isRtl(buffer, 0, textLength);
                paraLevel = isRtl ? Bidi.RTL : Bidi.LTR;
            }
            fm.bidi = new Bidi(textLength, 0);
            fm.bidi.setPara(buffer, paraLevel, null);

            ShapedText layout = new ShapedText(fm.bidi, 0, textLength, paint.getInternalPaint(), 0);
            fm.width = (int) Math.ceil(layout.getAdvance());
            if (useFallbackLineSpacing) {
                fm.extendBy(layout.getAscent(), layout.getDescent());
            }
        }

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
        return mDir;
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
        return mDirections;
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

    @Override
    public void drawText(@NonNull Canvas canvas, int firstLine, int lastLine) {
        // Added by Modern UI
        Alignment align = getAlignment();
        if (align == Alignment.ALIGN_NORMAL) {
            align = (mDir == DIR_LEFT_TO_RIGHT) ?
                    Alignment.ALIGN_LEFT : Alignment.ALIGN_RIGHT;
        } else if (align == Alignment.ALIGN_OPPOSITE) {
            align = (mDir == DIR_LEFT_TO_RIGHT) ?
                    Alignment.ALIGN_RIGHT : Alignment.ALIGN_LEFT;
        }
        final int x;
        if (align == Alignment.ALIGN_LEFT) {
            x = 0;
        } else {
            int max = (int) mMax;
            if (align == Alignment.ALIGN_RIGHT) {
                x = getWidth() - max;
            } else { // Alignment.ALIGN_CENTER
                max = max & ~1;
                x = (getWidth() - max) >> 1;
            }
        }
        canvas.drawShapedText(mShaped, x, mBottom - mDesc, getPaint());
    }

    /**
     * This class holds ascent, descent, text advance (width), and some
     * opaque objects for creating BoringLayout.
     */
    public static class Metrics extends FontMetricsInt {

        public int width;

        char[] text;
        @Nullable
        Bidi bidi;

        @Override
        public void reset() {
            super.reset();
            width = 0;
            text = null;
            bidi = null;
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
            return super.toString() + " width=" + width;
        }
    }
}
