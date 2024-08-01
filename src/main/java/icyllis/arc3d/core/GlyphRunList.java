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

package icyllis.arc3d.core;

public class GlyphRunList {

    public static class GlyphRun {

        public int[] mGlyphs;
        public int mGlyphOffset;

        public float[] mPositions;
        public int mPositionOffset;

        public int mGlyphCount;

        public Font mFont;

        public GlyphRun() {
        }

        public void set(int[] glyphs, int glyphOffset,
                        float[] positions, int positionOffset,
                        int glyphCount, Font font) {
            mGlyphs = glyphs;
            mGlyphOffset = glyphOffset;
            mPositions = positions;
            mPositionOffset = positionOffset;
            mGlyphCount = glyphCount;
            mFont = font;
        }

        public int runSize() {
            return mGlyphCount;
        }

        public Font font() {
            return mFont;
        }
    }

    /**
     * Raw pointer view.
     */
    public GlyphRun[] mGlyphRuns;
    public int mGlyphRunCount;
    public final Rect2f mSourceBounds = new Rect2f();
    public float mOriginX;
    public float mOriginY;

    public GlyphRunList() {
    }

    public void set(GlyphRun[] glyphRuns, int glyphRunCount,
                    Rect2fc bounds, float originX, float originY) {
        mGlyphRuns = glyphRuns;
        mGlyphRunCount = glyphRunCount;
        mSourceBounds.set(bounds);
        mOriginX = originX;
        mOriginY = originY;
    }

    public int maxGlyphRunSize() {
        int size = 0;
        for (int i = 0; i < mGlyphRunCount; i++) {
            size = Math.max(mGlyphRuns[i].runSize(), size);
        }
        return size;
    }
}
