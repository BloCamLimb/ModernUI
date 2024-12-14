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

import icyllis.arc3d.core.Glyph;
import icyllis.arc3d.core.Mask;
import icyllis.arc3d.engine.Engine;
import org.jspecify.annotations.NonNull;

/**
 * This class holds information for a glyph about its pre-rendered image in a
 * GPU texture.
 */
public class BakedGlyph extends DrawAtlas.AtlasLocator {

    public BakedGlyph() {
    }

    public static int chooseMaskFormat(byte mask) {
        // promote B/W to Alpha8
        return switch (mask) {
            case Mask.kBW_Format, Mask.kA8_Format -> Engine.MASK_FORMAT_A8;
            case Mask.kARGB32_Format -> Engine.MASK_FORMAT_ARGB;
            default -> throw new AssertionError();
        };
    }

    public static int chooseMaskFormat(@NonNull Glyph glyph) {
        return chooseMaskFormat(glyph.getMaskFormat());
    }
}
