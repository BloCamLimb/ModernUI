/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.sketch;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/**
 * GlyphRun provides raw buffer views and represents a text run.
 */
@ApiStatus.Internal
public class GlyphRun {

    /**
     * Raw pointer view.
     */
    public int[] mGlyphs;
    public int mGlyphOffset;

    /**
     * Raw pointer view.
     */
    public float[] mPositions;
    public int mPositionOffset;

    public int mGlyphCount;

    private Font mFont;

    public GlyphRun() {
    }

    public void set(int @NonNull[] glyphs, int glyphOffset,
            float @NonNull[] positions, int positionOffset,
                    int glyphCount, @NonNull Font font) {
        mGlyphs = glyphs;
        mGlyphOffset = glyphOffset;
        mPositions = positions;
        mPositionOffset = positionOffset;
        mGlyphCount = glyphCount;
        mFont = font;
    }

    /**
     * Read-only view.
     */
    public Font font() {
        return mFont;
    }

    /**
     * Release heavy buffers.
     */
    public void clear() {
        mGlyphs = null;
        mPositions = null;
        mFont = null;
    }
}
