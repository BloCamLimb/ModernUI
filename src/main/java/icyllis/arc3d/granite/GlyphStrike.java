/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.granite;

import icyllis.arc3d.core.StrikeDesc;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nonnull;

/**
 * The GPU GlyphStrike holds GPU {@link BakedGlyph Glyphs} for a Strike.
 */
public class GlyphStrike {

    private final StrikeDesc mStrikeDesc;
    private final Int2ObjectOpenHashMap<BakedGlyph> mGlyphs = new Int2ObjectOpenHashMap<>();

    /**
     * <var>desc</var> must be immutable, no copy will be made.
     */
    public GlyphStrike(StrikeDesc desc) {
        mStrikeDesc = desc;
    }

    /**
     * Find or create Glyph and returns a pointer to it.
     */
    @Nonnull
    public BakedGlyph getGlyph(int glyphID) {
        return mGlyphs.computeIfAbsent(glyphID, __ -> new BakedGlyph());
    }

    // read only!!
    public StrikeDesc getStrikeDesc() {
        return mStrikeDesc;
    }
}
