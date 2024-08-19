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
import java.lang.ref.Reference;
import java.util.Arrays;

/**
 * TextBlob combines multiple text runs into an immutable container. Each text
 * run consists of glyphs, positions, and Paint. Only parts of Paint related to
 * fonts and text rendering are used by run.
 */
public final class TextBlob {

    /**
     * Returns a text blob built from a single run of text with positions.
     * This is equivalent to using TextBlobBuilder and calling allocRunPos().
     * Returns null if <var>glyphCount</var> is zero.
     *
     * @param glyphs     the array of glyphIDs to draw
     * @param positions  where to draw each glyph relative to origin
     * @param glyphCount number of glyphs to draw
     * @param font       typeface, text size and so, used to describe the text
     * @return new text blob or null
     */
    @Nullable
    public static TextBlob make(@Nonnull int[] glyphs, int glyphOffset,
                                @Nonnull float[] positions, int positionOffset,
                                int glyphCount, @Nonnull Font font,
                                @Nullable Rect2fc bounds) {
        if (glyphCount <= 0) {
            return null;
        }
        return makeNoCopy(
                Arrays.copyOfRange(glyphs, glyphOffset, glyphOffset + glyphCount),
                Arrays.copyOfRange(positions, positionOffset, positionOffset + glyphCount * 2),
                new Font(font), bounds
        );
    }

    /**
     * Helper class for constructing TextBlob.
     */
    public static final class Builder {

        /**
         * RunBuffer supplies storage for glyphs and positions within a run.
         * <p>
         * A run is a sequence of glyphs sharing font metrics and positioning.
         * Each run may position its glyphs by providing (x,y) pairs, one per glyph.
         */
        public final class RunBuffer {

            public RunBuffer addGlyph(int glyph) {
                mGlyphs[mGlyphOffset++] = glyph;
                return this;
            }

            public RunBuffer addPosition(float x, float y) {
                mPositions[mPositionOffset++] = x;
                mPositions[mPositionOffset++] = y;
                return this;
            }

            public RunBuffer addGlyphs(@Nonnull int[] glyphs, int offset, int count) {
                System.arraycopy(glyphs, offset, mGlyphs, mGlyphOffset, count);
                mGlyphOffset += count;
                return this;
            }

            public RunBuffer addPositions(@Nonnull float[] positions, int offset, int count) {
                System.arraycopy(positions, offset, mPositions, mPositionOffset, count * 2);
                mPositionOffset += count * 2;
                return this;
            }
        }

        // concatenated glyph buffer and position buffer
        private int[] mGlyphs;
        private int mGlyphOffset;
        private float[] mPositions;
        private int mPositionOffset;

        // font and glyph count for each glyph run
        private Font[] mFonts;
        private int[] mCounts;
        private int mRunCount;

        private Rect2f mBounds = new Rect2f();
        private boolean mDeferredBounds = false;

        private final RunBuffer mCurrentRunBuffer = new RunBuffer();

        /**
         * Constructs empty TextBlobBuilder. By default, TextBlobBuilder has no runs.
         */
        public Builder() {
        }

        /**
         * Returns run with storage for glyphs and positions. Caller must write
         * <var>count</var> glyphs and <var>count</var> positions to {@link RunBuffer}
         * before next call to TextBlobBuilder.
         * <p>
         * Glyphs share metrics in font.
         * <p>
         * Glyphs are positioned using positions written by caller to RunBuffer, using
         * two float values for each position.
         * <p>
         * <var>bounds</var> defines an optional bounding box, used to skip drawing
         * when TextBlob bounds does not intersect Canvas bounds. If bounds is null,
         * TextBlob bounds is computed from positions, and glyphs metrics.
         *
         * @param font   Font used for this run
         * @param count  number of glyphs
         * @param bounds optional run bounding box
         * @return writable glyph buffer and position buffer
         */
        @Nonnull
        public RunBuffer allocRunPos(@Nonnull Font font, int count,
                                     @Nullable Rect2fc bounds) {
            if (count <= 0) {
                return mCurrentRunBuffer;
            }

            // The length of the position array is the largest among the four arrays,
            // only check if position can overflow.
            if (count > Integer.MAX_VALUE / 2 ||
                    (mPositions != null && (long) mPositions.length + (count * 2L) > Integer.MAX_VALUE)) {
                throw new IllegalStateException();
            }

            if (!mergeRun(font, count)) {
                updateDeferredBounds();

                if (mGlyphs != null) {
                    mGlyphOffset = mGlyphs.length;
                    mPositionOffset = mPositions.length;
                }

                reserve(count, true);

                mFonts[mRunCount] = new Font(font);
                mCounts[mRunCount] = count;
                mRunCount++;
            }

            if (!mDeferredBounds) {
                if (bounds != null) {
                    mBounds.join(bounds);
                } else {
                    mDeferredBounds = true;
                }
            }

            return mCurrentRunBuffer;
        }

        /**
         * Returns TextBlob built from runs of glyphs added by builder. Returned
         * TextBlob is immutable; its contents may not be altered.
         * Returns null if no runs of glyphs were added by builder.
         * <p>
         * Resets TextBlobBuilder to its initial empty state, allowing it to be
         * reused to build a new set of runs.
         *
         * @return TextBlob or null
         */
        @Nullable
        public TextBlob build() {
            if (mRunCount == 0) {
                assert mGlyphs == null;
                assert mPositions == null;
                assert mFonts == null;
                assert mCounts == null;
                assert mBounds.isEmpty();
                return null;
            }

            updateDeferredBounds();

            assert validate();

            TextBlob blob = new TextBlob(
                    mRunCount, mFonts, mCounts,
                    mGlyphs, mPositions, mBounds
            );
            mGlyphs = null;
            mPositions = null;
            mFonts = null;
            mCounts = null;
            mRunCount = 0;
            mBounds = new Rect2f();
            return blob;
        }

        private boolean mergeRun(@Nonnull Font font, int count) {
            assert count > 0;
            if (mRunCount == 0) {
                return false;
            }

            int lastRun = mRunCount - 1;
            Font lastRunFont = mFonts[lastRun];
            int lastRunCount = mCounts[lastRun];

            assert lastRunCount > 0;

            if (!lastRunFont.equals(font)) {
                return false;
            }

            // We have checked for overflow
            assert (lastRunCount + count > lastRunCount);

            mGlyphOffset = mGlyphs.length;
            mPositionOffset = mPositions.length;

            reserve(count, false);

            mCounts[lastRun] += count;

            return true;
        }

        private void reserve(int count, boolean addRun) {
            assert count > 0;
            if (mGlyphs == null) {
                assert mPositions == null;
                mGlyphs = new int[count];
                mPositions = new float[count * 2];
            } else {
                assert mPositions != null;
                // reserve_exact
                mGlyphs = Arrays.copyOf(mGlyphs, mGlyphs.length + count);
                mPositions = Arrays.copyOf(mPositions, mPositions.length + count * 2);
            }

            if (addRun) {
                if (mFonts == null) {
                    assert mCounts == null;
                    // initial size is 1 for single run
                    mFonts = new Font[1];
                    mCounts = new int[1];
                } else if (mRunCount == mFonts.length) {
                    assert mCounts != null;
                    // grow 1/8, min 4, max 1024
                    int cap = mFonts.length + MathUtil.clamp(mFonts.length >> 3, 4, 1024);
                    assert cap > 0;
                    mFonts = Arrays.copyOf(mFonts, cap);
                    mCounts = Arrays.copyOf(mCounts, cap);
                }
            }
        }

        private void updateDeferredBounds() {
            assert !mDeferredBounds || mRunCount > 0;

            if (!mDeferredBounds) {
                return;
            }

            int lastRun = mRunCount - 1;
            Font lastRunFont = mFonts[lastRun];
            int lastRunCount = mCounts[lastRun];

            computeTightBounds(
                    mGlyphs, mGlyphOffset - lastRunCount,
                    mPositions, mPositionOffset - lastRunCount * 2,
                    lastRunCount, lastRunFont,
                    mBounds
            );
            mDeferredBounds = false;
        }

        private boolean validate() {
            int count = 0;
            for (int i = 0; i < mRunCount; i++) {
                assert mFonts[i] != null;
                assert mCounts[i] > 0;
                count += mCounts[i];
            }
            assert mGlyphs.length == count;
            assert mPositions.length == count * 2;
            return true;
        }
    }

    /**
     * @hidden
     */
    // privilege method
    @ApiStatus.Internal
    public static TextBlob makeNoCopy(@Nonnull int[] glyphs, @Nonnull float[] positions,
                                      @Nonnull Font font, @Nullable Rect2fc bounds) {
        assert glyphs.length > 0 && positions.length == glyphs.length * 2;
        final var finalBounds = new Rect2f();
        if (bounds != null) {
            bounds.store(finalBounds);
        } else {
            computeTightBounds(
                    glyphs, 0,
                    positions, 0,
                    glyphs.length, font,
                    finalBounds
            );
        }
        return new TextBlob(1,
                new Font[]{font}, new int[]{glyphs.length},
                glyphs, positions, finalBounds);
    }

    static void computeTightBounds(int[] glyphs, int glyphOffset,
                                   float[] positions, int positionOffset,
                                   int glyphCount, Font font,
                                   Rect2f bounds) {
        // compute exact bounds
        var glyphPtrs = new Glyph[glyphCount];
        //TODO this is not correct, need to canonicalize
        var strike = new StrikeDesc().update(font, null, Matrix.identity())
                .findOrCreateStrike();
        strike.getMetrics(glyphs, glyphOffset, glyphCount, glyphPtrs);

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
    }

    // actual number of glyph runs
    private final int mRunCount;
    // font and glyph count for each glyph run
    private final Font[] mFonts;
    private final int[] mCounts;
    // concatenated glyph buffer and position buffer
    private final int[] mGlyphs;
    private final float[] mPositions;
    private final Rect2f mBounds;

    TextBlob(int runCount,
             Font[] fonts, int[] counts,
             int[] glyphs, float[] positions,
             Rect2f bounds) {
        mRunCount = runCount;
        mFonts = fonts;
        mCounts = counts;
        mGlyphs = glyphs;
        mPositions = positions;
        mBounds = bounds;
    }

    /**
     * Returns bounding box in pixels. Returned bounds may be larger than the bounds
     * of all glyphs in runs when drawing to Canvas.
     *
     * @return bounding box, may be conservative
     */
    @Nonnull
    public Rect2fc getBounds() {
        return mBounds;
    }

    public void getBounds(@Nonnull Rect2f bounds) {
        mBounds.store(bounds);
    }

    // used internally

    @ApiStatus.Internal
    public int getRunCount() {
        return mRunCount;
    }

    @ApiStatus.Internal
    public Font[] getFonts() {
        return mFonts;
    }

    @ApiStatus.Internal
    public int[] getCounts() {
        return mCounts;
    }

    @ApiStatus.Internal
    public int[] getGlyphs() {
        return mGlyphs;
    }

    @ApiStatus.Internal
    public float[] getPositions() {
        return mPositions;
    }

    // hashCode() keeps the default System.identityHashCode()

    /**
     * Special implementation of equals. In addition to strong reference equality,
     * it can also be compared with Reference.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Reference r) {
            return r.refersTo(this);
        }
        return false;
    }
}
