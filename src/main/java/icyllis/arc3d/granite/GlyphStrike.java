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

import icyllis.arc3d.sketch.StrikeDesc;
import icyllis.arc3d.sketch.Strike;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jspecify.annotations.NonNull;

/**
 * The GPU {@link GlyphStrike} holds GPU {@link BakedGlyph Glyphs} for a CPU {@link Strike}.
 */
public final class GlyphStrike {

    private final StrikeDesc mStrikeDesc;
    private final Int2ObjectOpenHashMap<BakedGlyph> mGlyphs = new Int2ObjectOpenHashMap<>();

    /**
     * <var>desc</var> must be immutable, no copy will be made.
     */
    // Use GlyphStrikeCache to obtain an instance
    public GlyphStrike(@NonNull StrikeDesc desc) {
        assert desc.isImmutable();
        mStrikeDesc = desc;
    }

    /**
     * Find or create Glyph and returns a pointer to it.
     */
    public @NonNull BakedGlyph getGlyph(int glyphID) {
        return mGlyphs.computeIfAbsent(glyphID, __ -> new BakedGlyph());
    }

    // immutable
    public @NonNull StrikeDesc getStrikeDesc() {
        return mStrikeDesc;
    }
}
