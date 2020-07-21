/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.font.process;

import icyllis.modernui.font.glyph.TexturedGlyph;

import javax.annotation.Nonnull;

/**
 * Temporary resulted glyph
 */
public class ProcessingGlyph implements Comparable<ProcessingGlyph> {

    public static final byte STATIC_TEXT = 0;

    public static final byte DYNAMIC_DIGIT = 1;

    public static final byte RANDOM_DIGIT = 2;

    /**
     * First assignment is stripIndex, and it will be adjusted to stringIndex later
     */
    public int stringIndex;

    /**
     * For type {@link #DYNAMIC_DIGIT} or {@link #RANDOM_DIGIT}
     */
    public final TexturedGlyph[] glyphs;

    /**
     * For type {@link #STATIC_TEXT}
     */
    public final TexturedGlyph glyph;

    /**
     * Either {@link #STATIC_TEXT}, {@link #DYNAMIC_DIGIT} or {@link #RANDOM_DIGIT}
     */
    public final byte type;

    public ProcessingGlyph(int stripIndex, TexturedGlyph glyph, byte type) {
        this.stringIndex = stripIndex;
        glyphs = null;
        this.glyph = glyph;
        this.type = type;
    }

    public ProcessingGlyph(int stripIndex, TexturedGlyph[] glyphs, byte type) {
        this.stringIndex = stripIndex;
        this.glyphs = glyphs;
        glyph = null;
        this.type = type;
    }

    @Override
    public int compareTo(@Nonnull ProcessingGlyph o) {
        return Integer.compare(stringIndex, o.stringIndex);
    }
}
