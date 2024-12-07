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

import org.jetbrains.annotations.ApiStatus;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;

/**
 * Builds and provides a reusable {@link GlyphRunList}.
 */
@ApiStatus.Internal
public class GlyphRunBuilder {

    private final GlyphRunList mGlyphRunList = new GlyphRunList();

    private GlyphRun[] mGlyphRuns = new GlyphRun[10];
    private int mGlyphRunCount;

    // for bounds computation
    private final Rect2f mTmpBounds = new Rect2f();
    private final StrikeDesc mTmpStrikeDesc = new StrikeDesc();
    private Glyph[] mTmpGlyphs = new Glyph[60];

    private Glyph[] ensureGlyphs(int glyphCount) {
        if (mTmpGlyphs.length < glyphCount) {
            mTmpGlyphs = new Glyph[glyphCount];
        }
        return mTmpGlyphs;
    }

    /**
     * Initializes and returns a read-only view.
     */
    public GlyphRunList setGlyphRunList(
            int @NonNull[] glyphs, int glyphOffset,
            float @NonNull[] positions, int positionOffset,
            int glyphCount, @NonNull Font font,
            @NonNull Paint paint,
            float originX, float originY
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
        addGlyphRun(glyphs, glyphOffset, positions, positionOffset, glyphCount, font);
        return setGlyphRunList(null, bounds, originX, originY);
    }

    /**
     * Initializes and returns a read-only view.
     */
    public GlyphRunList blobToGlyphRunList(@NonNull TextBlob blob,
                                           float originX, float originY) {
        final Font[] fonts = blob.getFonts();
        final int[] counts = blob.getCounts();
        final int[] glyphs = blob.getGlyphs();
        final float[] positions = blob.getPositions();
        final int runCount = blob.getRunCount();
        int glyphOffset = 0;
        int positionOffset = 0;
        for (int i = 0; i < runCount; i++) {
            final int glyphCount = counts[i];
            assert glyphCount > 0;
            final Font font = fonts[i];
            addGlyphRun(glyphs, glyphOffset,
                    positions, positionOffset,
                    glyphCount, font);
            glyphOffset += glyphCount;
            positionOffset += glyphCount * 2;
        }
        return setGlyphRunList(blob, blob.getBounds(), originX, originY);
    }

    public void clear() {
        mGlyphRunCount = 0;
        mGlyphRunList.clear();
    }

    private void addGlyphRun(int @NonNull[] glyphs, int glyphOffset,
            float @NonNull[] positions, int positionOffset,
                             int glyphCount, @NonNull Font font) {
        if (glyphCount <= 0) {
            return;
        }
        if (mGlyphRunCount == mGlyphRuns.length) {
            // text blob can hold up to (int.max / 2) runs
            int cap = mGlyphRuns.length;
            cap = cap + (cap >> 1);
            cap = Math.min(cap, Integer.MAX_VALUE / 2);
            mGlyphRuns = Arrays.copyOf(mGlyphRuns, cap);
        }
        if (mGlyphRuns[mGlyphRunCount] == null) {
            mGlyphRuns[mGlyphRunCount] = new GlyphRun();
        }
        mGlyphRuns[mGlyphRunCount++].set(
                glyphs, glyphOffset, positions, positionOffset, glyphCount, font
        );
    }

    private GlyphRunList setGlyphRunList(@Nullable TextBlob blob, Rect2fc bounds,
                                         float originX, float originY) {
        mGlyphRunList.set(mGlyphRuns, mGlyphRunCount, blob, bounds, originX, originY);
        return mGlyphRunList;
    }
}
