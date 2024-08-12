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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * GlyphRunList provides a buffer view to GlyphRuns and additional drawing info.
 */
@ApiStatus.Internal
public class GlyphRunList {

    /**
     * Raw pointer view.
     */
    public GlyphRun[] mGlyphRuns;
    public int mGlyphRunCount;

    /**
     * Raw pointer, nullable.
     */
    public TextBlob mOriginalTextBlob;

    private final Rect2f mSourceBounds = new Rect2f();
    public float mOriginX;
    public float mOriginY;

    public GlyphRunList() {
    }

    public void set(GlyphRun[] glyphRuns, int glyphRunCount,
                    @Nullable TextBlob blob,
                    Rect2fc bounds, float originX, float originY) {
        mGlyphRuns = glyphRuns;
        mGlyphRunCount = glyphRunCount;
        mOriginalTextBlob = blob;
        mSourceBounds.set(bounds);
        mOriginX = originX;
        mOriginY = originY;
    }

    @Nonnull
    public Rect2fc getSourceBounds() {
        return mSourceBounds;
    }

    public void getSourceBoundsWithOrigin(@Nonnull Rect2f bounds) {
        mSourceBounds.store(bounds);
        bounds.offset(mOriginX, mOriginY);
    }

    public int maxGlyphRunSize() {
        int size = 0;
        for (int i = 0; i < mGlyphRunCount; i++) {
            size = Math.max(mGlyphRuns[i].mGlyphCount, size);
        }
        return size;
    }

    /**
     * Release heavy buffers.
     */
    public void clear() {
        if (mGlyphRuns != null) {
            for (int i = 0; i < mGlyphRunCount; i++) {
                mGlyphRuns[i].clear();
            }
            mGlyphRuns = null;
        }
        mOriginalTextBlob = null;
    }
}
