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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.RawPtr;

import javax.annotation.Nullable;

/**
 * Manages all baked glyphs and their texture atlases.
 */
public class GlyphAtlasManager {

    private final DrawAtlas[] mAtlases = new DrawAtlas[Engine.MASK_FORMAT_COUNT];

    @Nullable
    @RawPtr
    public ImageViewProxy getCurrentTexture(int maskFormat) {
        var atlas = getAtlas(maskFormat);
        if (atlas != null) {
            return atlas.getTexture();
        }
        return null;
    }

    public boolean hasGlyph(int maskFormat, AtlasLocator glyph) {
        return false;
    }

    private DrawAtlas getAtlas(int maskFormat) {
        return mAtlases[maskFormat];
    }
}
