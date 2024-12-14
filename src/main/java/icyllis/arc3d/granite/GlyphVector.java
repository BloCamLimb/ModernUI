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

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.RecordingContext;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;

/**
 * GlyphVector provides a way to delay the lookup of GPU {@link BakedGlyph Glyphs}
 * until the code is running on the GPU in single threaded mode. The GlyphVector is created
 * in a multi-threaded environment, but the {@link GlyphStrike} is only single threaded
 * (and must be single threaded because of the atlas).
 */
public class GlyphVector
    // this class extends BulkUseUpdater and just wants it to be treated as a value type
        extends DrawAtlas.PlotBulkUseUpdater {

    // the strikeDesc and packedGlyphIDs
    private final StrikeDesc mStrikeDesc;
    private final int[] mGlyphs;
    private BakedGlyph[] mBakedGlyphs;

    private long mAtlasGeneration = DrawAtlas.AtlasGenerationCounter.INVALID_GENERATION;

    /**
     * All params are read-only, copy will be made.
     */
    public GlyphVector(@NonNull StrikeDesc strikeDesc, int @NonNull [] glyphs, int start, int end) {
        mStrikeDesc = strikeDesc.immutable();
        mGlyphs = Arrays.copyOfRange(glyphs, start, end);
    }

    // This call is not thread safe. It should only be called from a known single-threaded env.
    public BakedGlyph[] getGlyphs() {
        return mBakedGlyphs;
    }

    public int getGlyphCount() {
        return mGlyphs.length;
    }

    /**
     * Update atlas for glyphs in the given range if needed, returns the number
     * of glyphs that are updated (may less than end-start if atlas is full).
     * If an error occurred, returns the bitwise NOT (a negative value).
     */
    // This call is not thread safe. It should only be called from a known single-threaded env.
    public int prepareGlyphs(int start, int end,
                             int maskFormat, RecordingContext context) {
        assert 0 <= start && start <= end && end <= mGlyphs.length;
        // convert CPU glyphs to GPU glyphs, must be run in single-threaded mode
        if (mBakedGlyphs == null) {
            var glyphStrike = context.getGlyphStrikeCache().findOrCreateStrike(
                    mStrikeDesc
            );
            var bakedGlyphs = new BakedGlyph[mGlyphs.length];
            for (int i = 0, e = mGlyphs.length; i < e; ++i) {
                bakedGlyphs[i] = glyphStrike.getGlyph(mGlyphs[i]);
            }
            mBakedGlyphs = bakedGlyphs;
        }

        var atlasManager = context.getAtlasProvider().getGlyphAtlasManager();
        var tokenTracker = context.getAtlasTokenTracker();

        long currentGeneration = atlasManager.getAtlasGeneration(maskFormat);

        if (mAtlasGeneration != currentGeneration) {
            // Calculate the texture coordinates for the vertexes during first use (mAtlasGeneration
            // is set to INVALID_GENERATION) or the atlas has changed in subsequent calls...
            super.clear();

            Strike strike = mStrikeDesc.findOrCreateStrike();

            boolean success = true;
            int glyphsUpdated = 0;
            for (int i = start; i < end; ++i) {
                var bakedGlyph = mBakedGlyphs[i];

                if (!atlasManager.hasGlyph(maskFormat, bakedGlyph)) {
                    // do CPU rasterization if needed
                    Glyph glyph;
                    strike.lock();
                    try {
                        glyph = strike.getGlyph(mGlyphs[i]);
                        strike.prepareForImage(glyph);
                    } finally {
                        strike.unlock();
                    }
                    var res = atlasManager.addGlyphToAtlas(
                            glyph, bakedGlyph
                    );
                    if (res != DrawAtlas.RESULT_SUCCESS) {
                        assert res == DrawAtlas.RESULT_FAILURE ||
                                res == DrawAtlas.RESULT_TRY_AGAIN;
                        // try again meaning the atlas is full, it's not an error
                        success = res != DrawAtlas.RESULT_FAILURE;
                        break;
                    }
                }

                atlasManager.addGlyphAndSetLastUseToken(this,
                        bakedGlyph,
                        maskFormat,
                        tokenTracker.nextFlushToken());
                ++glyphsUpdated;
            }

            // Update atlas generation if there are no more glyphs to put in the atlas.
            if (success && start + glyphsUpdated == mGlyphs.length) {
                // Cannot use 'currentGeneration' above.
                // Need to get the freshest value of the atlas' generation because
                // DrawAtlas.addRect may have changed it.
                mAtlasGeneration = atlasManager.getAtlasGeneration(maskFormat);
            }

            return success ? glyphsUpdated : ~glyphsUpdated;
        } else {
            // The atlas hasn't changed, so our texture coordinates are still valid.
            if (end == mGlyphs.length) {
                // The atlas hasn't changed and the texture coordinates are all still valid. Update
                // all the plots used to the new use token.
                atlasManager.setLastUseTokenBulk(maskFormat,
                        this,
                        tokenTracker.nextFlushToken());
            }
            return end - start;
        }
    }

    @Override
    public long getMemorySize() {
        long size = super.getMemorySize();
        size += 8 + mStrikeDesc.getMemorySize();
        size += 32 + MathUtil.align8((long) mGlyphs.length * 12) + 16;
        size += 8;
        return size;
    }
}
