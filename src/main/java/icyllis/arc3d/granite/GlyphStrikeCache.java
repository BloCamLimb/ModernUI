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
import org.jspecify.annotations.NonNull;

import java.util.HashMap;

/**
 * GlyphStrikeCache manages strikes which are indexed by a Strike. These strikes can then be
 * used to generate individual Glyph Masks.
 */
public class GlyphStrikeCache {

    private final HashMap<StrikeDesc, GlyphStrike> mCache = new HashMap<>();

    /**
     * <var>desc</var> must be immutable, no copy will be made.
     */
    public @NonNull GlyphStrike findOrCreateStrike(@NonNull StrikeDesc desc) {
        assert desc.isImmutable();
        return mCache.computeIfAbsent(desc, GlyphStrike::new);
    }

    public void clear() {
        mCache.clear();
    }
}
