/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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
import icyllis.modernui.graphics.text.CharUtils;
import icyllis.modernui.graphics.text.ShapedText;
import icyllis.modernui.graphics.text.TextRunShaper;

import java.util.Objects;

/**
 * Provides conversion from a text into glyph array.
 * <p>
 * Text shaping is a preprocess for drawing text into canvas with glyphs. The glyph is a most
 * primitive unit of the text drawing, consist of glyph identifier in the font file and its position
 * and style. You can draw the shape result to Canvas by calling
 * {@link icyllis.modernui.graphics.Canvas#drawShapedText}.
 * <p>
 * For most of the use cases, {@link #shapeText} will provide text shaping
 * functionalities needed.
 *
 * @since 3.8
 */
public class TextShaper {

    /**
     * A consumer interface for accepting text shape result.
     */
    public interface GlyphsConsumer {
        /**
         * Accept text shape result.
         * <p>
         * The implementation must <em>not</em> keep reference of paint since it will be mutated
         * for the subsequent styles. Also, for saving heap size, keep only necessary members in
         * the {@link TextPaint} instead of copying {@link TextPaint} object.
         *
         * @param start   The start index of the shaped text.
         * @param count   The length of the shaped text.
         * @param glyphs  The shape result.
         * @param paint   The paint to be used for drawing.
         * @param offsetX The additional X offset (relative to the left) of this style run.
         * @param offsetY The additional Y offset (relative to the top) of this style run.
         */
        void accept(
                @IntRange(from = 0) int start,
                @IntRange(from = 0) int count,
                @NonNull ShapedText glyphs,
                @NonNull TextPaint paint,
                float offsetX,
                float offsetY);
    }

    /**
     * Shape multi-styled text.
     * <p>
     * In the LTR context, the shape result will go from left to right, thus you may want to draw
     * glyphs from left most position of the canvas. In the RTL context, the shape result will go
     * from right to left, thus you may want to draw glyphs from right most position of the canvas.
     *
     * @param text     a styled text.
     * @param start    a start index of shaping target in the text.
     * @param count    a length of shaping target in the text.
     * @param dir      a text direction.
     * @param paint    a paint
     * @param consumer a consumer of the shape result.
     */
    public static void shapeText(
            @NonNull CharSequence text, @IntRange(from = 0) int start,
            @IntRange(from = 0) int count, @NonNull TextDirectionHeuristic dir,
            @NonNull TextPaint paint, @NonNull GlyphsConsumer consumer) {
        Objects.requireNonNull(text);
        if (!(text instanceof Spanned)) {
            consumer.accept(start, count,
                    shapeText(text, start, count, dir, paint),
                    paint, 0, 0);
            return;
        }
        MeasuredParagraph mp = MeasuredParagraph.buildForBidi(
                text, start, start + count, dir, null);
        TextLine tl = TextLine.obtain();
        try {
            // runs are in logical order
            tl.set(paint, text, start, start + count,
                    mp.getParagraphDir(),
                    mp.getDirections(0, count),
                    false /* tabstop is not supported */,
                    null,
                    -1, -1 // ellipsis is not supported.
            );
            tl.shape(consumer);
        } finally {
            tl.recycle();
            mp.recycle();
        }
    }

    /**
     * Shape non-styled text.
     * <p>
     * This function shapes the text of the given range under the context of given context range.
     * Some script, e.g. Arabic or Devanagari, changes letter shape based on its location or
     * surrounding characters.
     *
     * @param text  a text buffer to be shaped
     * @param start a start index of shaping target in the buffer.
     * @param count a length of shaping target in the buffer.
     * @param dir   a text direction.
     * @param paint a paint used for shaping text.
     * @return a shape result.
     */
    @NonNull
    public static ShapedText shapeText(
            @NonNull char[] text, @IntRange(from = 0) int start,
            @IntRange(from = 0) int count,
            @NonNull TextDirectionHeuristic dir, @NonNull TextPaint paint) {
        Objects.requireNonNull(dir);
        Objects.checkFromIndexSize(start, count, text.length);
        // similar to MeasuredParagraph.buildForBidi()
        final int bidiFlags;
        if ((dir == TextDirectionHeuristics.LTR
                || dir == TextDirectionHeuristics.FIRSTSTRONG_LTR
                || dir == TextDirectionHeuristics.ANYRTL_LTR)
                && !TextUtils.requiresBidi(text, start, start + count)) {
            bidiFlags = ShapedText.BIDI_OVERRIDE_LTR;
        } else if (dir == TextDirectionHeuristics.LTR) {
            bidiFlags = ShapedText.BIDI_LTR;
        } else if (dir == TextDirectionHeuristics.RTL) {
            bidiFlags = ShapedText.BIDI_RTL;
        } else if (dir == TextDirectionHeuristics.FIRSTSTRONG_LTR) {
            bidiFlags = ShapedText.BIDI_DEFAULT_LTR;
        } else if (dir == TextDirectionHeuristics.FIRSTSTRONG_RTL) {
            bidiFlags = ShapedText.BIDI_DEFAULT_RTL;
        } else {
            final boolean isRtl = dir.isRtl(text, start, start + count);
            bidiFlags = isRtl ? ShapedText.BIDI_RTL : ShapedText.BIDI_LTR;
        }
        if (bidiFlags == ShapedText.BIDI_OVERRIDE_LTR ||
                (start == 0 && count == text.length)) {
            return new ShapedText(text, start, start + count,
                    start, start + count, bidiFlags, paint.getInternalPaint());
        } else {
            // make a copy for bidi analysis
            char[] para = new char[count];
            System.arraycopy(text, start, para, 0, count);
            return new ShapedText(para, 0, count,
                    0, count, bidiFlags, paint.getInternalPaint());
        }
    }

    /**
     * Shape non-styled text.
     * <p>
     * This function shapes the text of the given range under the context of given context range.
     * Some script, e.g. Arabic or Devanagari, changes letter shape based on its location or
     * surrounding characters.
     *
     * @param text  a text buffer to be shaped. Any styled spans stored in this text are ignored.
     * @param start a start index of shaping target in the buffer.
     * @param count a length of shaping target in the buffer.
     * @param dir   a text direction.
     * @param paint a paint used for shaping text.
     * @return a shape result
     */
    @NonNull
    public static ShapedText shapeText(
            @NonNull CharSequence text, @IntRange(from = 0) int start,
            @IntRange(from = 0) int count,
            @NonNull TextDirectionHeuristic dir, @NonNull TextPaint paint) {
        Objects.checkFromIndexSize(start, count, text.length());
        // for these three cases, a new array may not be necessary
        boolean mayTemp = (dir == TextDirectionHeuristics.LTR
                || dir == TextDirectionHeuristics.FIRSTSTRONG_LTR
                || dir == TextDirectionHeuristics.ANYRTL_LTR);
        char[] buf;
        if (mayTemp) {
            buf = CharUtils.obtain(count);
        } else {
            buf = new char[count];
        }
        try {
            CharUtils.getChars(text, start, start + count, buf, 0);
            return shapeText(buf, 0, count, dir, paint);
        } finally {
            if (mayTemp) {
                CharUtils.recycle(buf);
            }
        }
    }

    /**
     * Shape non-styled text.
     * <p>
     * This function shapes the text of the given range under the context of given context range.
     * Some script, e.g. Arabic or Devanagari, changes letter shape based on its location or
     * surrounding characters.
     *
     * @param text         a text buffer to be shaped
     * @param start        a start index of shaping target in the buffer.
     * @param count        a length of shaping target in the buffer.
     * @param contextStart a start index of context used for shaping in the buffer.
     * @param contextCount a length of context used for shaping in the buffer.
     * @param isRtl        true if this text is shaped for RTL direction, false otherwise.
     * @param paint        a paint used for shaping text.
     * @return a shape result.
     */
    @NonNull
    public static ShapedText shapeTextRun(
            @NonNull char[] text, @IntRange(from = 0) int start,
            @IntRange(from = 0) int count, int contextStart, int contextCount,
            boolean isRtl, @NonNull TextPaint paint) {
        return TextRunShaper.shapeTextRun(text, start, count, contextStart, contextCount, isRtl,
                paint.getInternalPaint());
    }

    /**
     * Shape non-styled text.
     * <p>
     * This function shapes the text of the given range under the context of given context range.
     * Some script, e.g. Arabic or Devanagari, changes letter shape based on its location or
     * surrounding characters.
     *
     * @param text         a text buffer to be shaped. Any styled spans stored in this text are ignored.
     * @param start        a start index of shaping target in the buffer.
     * @param count        a length of shaping target in the buffer.
     * @param contextStart a start index of context used for shaping in the buffer.
     * @param contextCount a length of context used for shaping in the buffer.
     * @param isRtl        true if this text is shaped for RTL direction, false otherwise.
     * @param paint        a paint used for shaping text.
     * @return a shape result
     */
    @NonNull
    public static ShapedText shapeTextRun(
            @NonNull CharSequence text, @IntRange(from = 0) int start,
            @IntRange(from = 0) int count, int contextStart, int contextCount,
            boolean isRtl, @NonNull TextPaint paint) {
        return TextRunShaper.shapeTextRun(text, start, count, contextStart, contextCount, isRtl,
                paint.getInternalPaint());
    }

    private TextShaper() {
    }
}
