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

/**
 * Builds and provides a reusable {@link GlyphRunList}.
 */
public class GlyphRunBuilder {

    private final GlyphRunList mGlyphRunList = new GlyphRunList();

    private GlyphRunList.GlyphRun[] mStorage = new GlyphRunList.GlyphRun[10];
    private int mGlyphRunCount;

    {
        for (int i = 0; i < mStorage.length; i++) {
            mStorage[i] = new GlyphRunList.GlyphRun();
        }
    }

    private final StrikeDesc mTmpStrikeDesc = new StrikeDesc();

    private Glyph[] mTmpGlyphs = new Glyph[60];

    private Glyph[] ensureGlyphs(int glyphCount) {
        if (mTmpGlyphs.length < glyphCount) {
            mTmpGlyphs = new Glyph[glyphCount];
        }
        return mTmpGlyphs;
    }

    private final Rect2f mTmpBounds = new Rect2f();

    /**
     * Initializes and returns a read-only view.
     */
    public GlyphRunList setGlyphRunList(
            int[] glyphs, int glyphOffset,
            float[] positions, int positionOffset,
            int glyphCount, Font font,
            float originX, float originY,
            Paint paint
    ) {
        // compute exact bounds
        var glyphPtrs = ensureGlyphs(glyphCount);
        //TODO this is not correct, need to canonicalize
        var strike = mTmpStrikeDesc.update(font, paint, Matrix.identity())
                .findOrCreateStrike();
        strike.getMetrics(glyphs, glyphOffset, glyphCount, glyphPtrs);

        var bounds = mTmpBounds;
        bounds.setEmpty();
        for (int i = 0, j = positionOffset; i < glyphCount; i += 1, j += 2) {
            var glyphPtr = glyphPtrs[i];
            if (!glyphPtr.isEmpty()) {
                // offset bounds by position x/y
                float l = glyphPtr.getLeft() + positions[j];
                float t = glyphPtr.getTop() + positions[j + 1];
                float r = glyphPtr.getLeft() + glyphPtr.getWidth() + positions[j];
                float b = glyphPtr.getTop() + glyphPtr.getHeight() + positions[j + 1];
                bounds.join(l, t, r, b);
            }
        }
        mStorage[0].set(glyphs, glyphOffset, positions, positionOffset, glyphCount, font);

        mGlyphRunList.set(mStorage, 1,
                bounds, originX, originY);
        return mGlyphRunList;
    }
}
