/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.text;

import icyllis.modernui.annotation.NonNull;

import java.util.Objects;

/**
 * Provides conversion from a text into glyph array.
 * <p>
 * Text shaping is a preprocess for drawing text into canvas with glyphs. The glyph is a most
 * primitive unit of the text drawing, consist of glyph identifier in the font file and its position
 * and style. You can draw the shape result to Canvas by calling
 * {@link icyllis.modernui.graphics.Canvas#drawShapedText}.
 * <p>
 * For most of the use cases, {@link icyllis.modernui.text.TextShaper} will provide text shaping
 * functionalities needed. {@link TextRunShaper} is a lower level API that is used by
 * {@link icyllis.modernui.text.TextShaper}, it assumes that you have already performed BiDi analysis.
 *
 * @since 3.12.1
 */
// Moved from text to graphics.text
public class TextRunShaper {

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
            @NonNull char[] text, int start, int count,
            int contextStart, int contextCount,
            boolean isRtl, @NonNull FontPaint paint) {
        int bidiFlags = isRtl ? ShapedText.BIDI_OVERRIDE_RTL : ShapedText.BIDI_OVERRIDE_LTR;
        return new ShapedText(text, contextStart, contextStart + contextCount,
                start, start + count, bidiFlags, paint);
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
            @NonNull CharSequence text, int start, int count,
            int contextStart, int contextCount,
            boolean isRtl, @NonNull FontPaint paint) {
        Objects.checkFromIndexSize(contextStart, contextCount, text.length());
        char[] buf = CharUtils.obtain(contextCount);
        try {
            CharUtils.getChars(text, contextStart, contextStart + contextCount, buf, 0);
            return shapeTextRun(buf, start - contextStart, count,
                    0, contextCount, isRtl, paint);
        } finally {
            CharUtils.recycle(buf);
        }
    }

    private TextRunShaper() {
    }
}
