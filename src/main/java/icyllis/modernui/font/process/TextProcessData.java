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

import icyllis.modernui.font.node.IGlyphRenderInfo;

import java.util.ArrayList;
import java.util.List;

public class TextProcessData {

    /**
     * Array of temporary formatting info
     */
    public final List<FormattingStyle> codes = new ArrayList<>();

    /**
     * List of all processing glyphs
     */
    public final List<ProcessingGlyph> allList = new ArrayList<>();

    /**
     * List of processing glyphs with same layout direction
     */
    public final List<ProcessingGlyph> layoutList = new ArrayList<>();

    /**
     * Used in layoutFont
     */
    public final List<ProcessingGlyph> minimalList = new ArrayList<>();

    /**
     * Indicates current style index in {@link #codes} for layout processing
     */
    public int codeIndex;

    /**
     * The total advance (horizontal width) of the processing text
     */
    public float advance;

    /**
     * Needed in RTL layout
     */
    public float layoutRight;

    public void mergeGlyphs(float adjust) {
        if (adjust != 0) {
            layoutList.forEach(e -> e.offsetX += adjust);
        }
        allList.addAll(layoutList);
        layoutList.clear();
    }

    public void mergeLayout(float adjust) {
        if (adjust != 0) {
            minimalList.forEach(e -> e.offsetX += adjust);
        }
        layoutList.addAll(minimalList);
        minimalList.clear();
    }

    public IGlyphRenderInfo[] wrapGlyphs() {
        return allList.stream().map(ProcessingGlyph::toGlyph).toArray(IGlyphRenderInfo[]::new);
    }

    public float wrapAdvance() {
        float r = advance;
        allList.clear();
        codes.clear();
        codeIndex = 0;
        advance = 0;
        return r;
    }

}
