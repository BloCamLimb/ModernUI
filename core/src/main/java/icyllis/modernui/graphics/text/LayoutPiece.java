/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.graphics.text;

import icyllis.arc3d.core.MathUtil;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Rect;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.function.Function;

/**
 * The layout of a styled text run, including text shaping results, glyph metrics and
 * their positions.
 *
 * @see LayoutCache
 * @since 2.6
 */
@Immutable
public final class LayoutPiece {

    // all laid-out glyphs, the order is visually left-to-right
    private final int[] mGlyphs;

    // x0 y0 x1 y1... for positioning glyphs
    private final float[] mPositions;

    private final byte[] mFontIndices;
    private final Font[] mFonts;

    // the size and order are relative to the text buf (char array)
    // only grapheme cluster bounds have advances, others are zeros
    // eg: [13.57, 0, 14.26, 0, 0]
    private final float[] mAdvances;

    // total font metrics
    private final int mAscent;
    private final int mDescent;

    // total advance
    private final float mAdvance;

    private final int mBoundsX;
    private final int mBoundsY;
    private final int mBoundsWidth;
    private final int mBoundsHeight;

    private final int mNumChars;

    final int mComputeFlags;

    /**
     * Creates the glyph layout of a piece. No reference to the buffer will be held.
     *
     * @param buf      text buffer, cannot be null or empty
     * @param start    start char offset
     * @param limit    end char index
     * @param isRtl    whether to layout in right-to-left
     * @param paint    the font paint affecting measurement
     * @param hint     existing layout piece
     * @param newFlags additional flags to compute
     */
    LayoutPiece(@NonNull char[] buf, int contextStart, int contextLimit, int start, int limit,
                boolean isRtl, @NonNull FontPaint paint, @Nullable LayoutPiece hint, int newFlags) {
        final boolean computeAdvances =
                (newFlags & LayoutCache.COMPUTE_CLUSTER_ADVANCES) != 0;
        final boolean computeBounds =
                (newFlags & LayoutCache.COMPUTE_GLYPHS_PIXEL_BOUNDS) != 0;

        int count = limit - start;
        if (computeAdvances) {
            assert hint == null || hint.mAdvances == null;
            mAdvances = new float[count];
        } else if (hint != null) {
            mAdvances = hint.mAdvances;
        } else {
            mAdvances = null;
        }
        final FontMetricsInt extent = new FontMetricsInt();
        final Rect bounds = new Rect();

        final ByteArrayList fontIndices;
        final IntArrayList glyphs;
        final FloatArrayList positions;
        if (hint == null) {
            // reserve memory, glyph count is <= char count
            fontIndices = new ByteArrayList(count);
            glyphs = new IntArrayList(count);
            positions = new FloatArrayList(count * 2);
        } else {
            fontIndices = null;
            glyphs = null;
            positions = null;
        }

        final ArrayList<Font> fontVec;
        final HashMap<Font, Byte> fontMap;
        final Function<Font, Byte> nextID;
        if (hint == null) {
            fontVec = new ArrayList<>();
            fontMap = new HashMap<>();
            nextID = font -> {
                fontVec.add(font);
                return (byte) fontMap.size();
            };
        } else {
            fontVec = null;
            fontMap = null;
            nextID = null;
        }

        int style = paint.getFontStyle();
        int size = paint.getFontSize();
        float advance = 0;

        final var items = paint.mFont.itemize(buf, start, limit);
        int runIndex, runLimit, runDir;
        if (isRtl) {
            runIndex = items.size() - 1;
            runLimit = -1;
            runDir = -1;
        } else {
            runIndex = 0;
            runLimit = items.size();
            runDir = 1;
        }
        // this is visually left-to-right
        for (; runIndex != runLimit; runIndex += runDir) {
            var run = items.get(runIndex);

            byte fontIdx = -1;
            int oldGlyphSize = 0;
            var font = run.family().getClosestMatch(style);
            if (hint == null) {
                fontIdx = fontMap.computeIfAbsent(font, nextID);
                font.getMetrics(paint, extent);

                oldGlyphSize = glyphs.size();
            }

            float adv = font.doComplexLayout(
                    buf, contextStart, contextLimit,
                    run.start(), run.limit(),
                    isRtl, paint,
                    glyphs, positions,
                    computeAdvances ? mAdvances : null,
                    start,
                    computeBounds ? bounds : null,
                    advance, 0
            );

            if (hint == null) {
                int newGlyphSize = glyphs.size();
                fontIndices.size(newGlyphSize);
                Arrays.fill(fontIndices.elements(),
                        oldGlyphSize, newGlyphSize, fontIdx);
            }

            advance += adv;
        }
        if (hint != null) {
            mGlyphs = hint.mGlyphs;
            mPositions = hint.mPositions;
            mFontIndices = hint.mFontIndices;
            mFonts = hint.mFonts;
            mAscent = hint.mAscent;
            mDescent = hint.mDescent;
            mAdvance = hint.mAdvance;
            assert mAdvance == advance;

            mComputeFlags = hint.mComputeFlags | newFlags;
        } else {
            mGlyphs = glyphs.toIntArray();
            mPositions = positions.toFloatArray();
            if (fontVec.size() > 1) {
                mFontIndices = fontIndices.toByteArray();
            } else {
                mFontIndices = null;
            }
            mFonts = fontVec.toArray(new Font[0]);

            mAscent = extent.ascent;
            mDescent = extent.descent;
            mAdvance = advance;

            mComputeFlags = newFlags;
        }

        if (computeBounds || hint == null) {
            mBoundsX = bounds.x();
            mBoundsY = bounds.y();
            mBoundsWidth = bounds.width();
            mBoundsHeight = bounds.height();
        } else {
            mBoundsX = hint.mBoundsX;
            mBoundsY = hint.mBoundsY;
            mBoundsWidth = hint.mBoundsWidth;
            mBoundsHeight = hint.mBoundsHeight;
        }

        mNumChars = count;
        assert (hint == null || hint.mNumChars == count);

        assert mGlyphs.length * 2 == mPositions.length;
        assert mFontIndices == null || mFontIndices.length == mGlyphs.length;
    }

    /**
     * Returns the number of glyphs.
     */
    public int getGlyphCount() {
        return mGlyphs.length;
    }

    /**
     * The array is about all laid-out glyphs for in order visually from left to right.
     *
     * @return glyphs
     */
    public int[] getGlyphs() {
        return mGlyphs;
    }

    /**
     * This array holds the repeat of x offset, y offset of glyph positions.
     * The length is twice as long as the glyph array.
     *
     * @return glyph positions
     */
    public float[] getPositions() {
        return mPositions;
    }

    /**
     * Returns which font should be used for the i-th glyph.
     *
     * @param i the index
     * @return the font
     */
    public Font getFont(int i) {
        if (mFontIndices != null) {
            return mFonts[mFontIndices[i]];
        }
        return mFonts[0];
    }

    /**
     * Returns the number of characters (i.e. constructor <code>limit - start</code> in code units).
     */
    public int getCharCount() {
        return mNumChars;
    }

    /**
     * The array of all chars advance, the length and order are relative to
     * the text buffer, if computed. Only grapheme cluster bounds have advances,
     * others are zeros. For example: [13.57, 0, 14.26, 0, 0]. The length is
     * constructor <code>end - start</code>.
     *
     * @return advances, or null
     * @see LayoutCache#COMPUTE_CLUSTER_ADVANCES
     */
    public float[] getAdvances() {
        return mAdvances;
    }

    /**
     * Expands the font metrics of the maximum extent of this piece.
     *
     * @param extent to expand from
     */
    public void getExtent(@NonNull FontMetricsInt extent) {
        extent.extendBy(mAscent, mDescent);
    }

    /**
     * Gets the font metrics of the maximum extent of this piece.
     *
     * @return ascent to baseline, always negative
     */
    public int getAscent() {
        return mAscent;
    }

    /**
     * Gets the font metrics of the maximum extent of this piece.
     *
     * @return descent to baseline, always positive
     */
    public int getDescent() {
        return mDescent;
    }

    /**
     * Returns the total advance of this piece.
     *
     * @return advance
     */
    public float getAdvance() {
        return mAdvance;
    }

    /**
     * Returns the floor left value of the pixel bounds of all glyph images,
     * relative to (0, 0), if computed.
     *
     * @return the bounds X, or 0
     * @see LayoutCache#COMPUTE_GLYPHS_PIXEL_BOUNDS
     */
    public int getBoundsX() {
        return mBoundsX;
    }

    /**
     * Returns the floor top value of the pixel bounds of all glyph images,
     * relative to (0, 0), if computed.
     *
     * @return the bounds Y, or 0
     * @see LayoutCache#COMPUTE_GLYPHS_PIXEL_BOUNDS
     */
    public int getBoundsY() {
        return mBoundsY;
    }

    /**
     * Returns the width value of the pixel bounds of all glyph images,
     * if computed.
     *
     * @return the bounds width, or 0
     * @see LayoutCache#COMPUTE_GLYPHS_PIXEL_BOUNDS
     */
    public int getBoundsWidth() {
        return mBoundsWidth;
    }

    /**
     * Returns the height value of the pixel bounds of all glyph images,
     * if computed.
     *
     * @return the bounds height, or 0
     * @see LayoutCache#COMPUTE_GLYPHS_PIXEL_BOUNDS
     */
    public int getBoundsHeight() {
        return mBoundsHeight;
    }

    /**
     * Returns which flags were computed.
     *
     * @see LayoutCache#COMPUTE_CLUSTER_ADVANCES
     * @see LayoutCache#COMPUTE_GLYPHS_PIXEL_BOUNDS
     */
    public int getComputeFlags() {
        return mComputeFlags;
    }

    public int getMemoryUsage() {
        int m = 64;
        m += 16 + MathUtil.align8(mGlyphs.length << 2);
        m += 16 + MathUtil.align8(mPositions.length << 2);
        if (mFontIndices != null) {
            m += 16 + MathUtil.align8(mFontIndices.length);
        }
        m += 16 + MathUtil.align8(mFonts.length << 2);
        if (mAdvances != null) {
            m += 16 + MathUtil.align8(mAdvances.length << 2);
        }
        return m;
    }

    @Override
    public String toString() {
        return "LayoutPiece{" +
                "mGlyphs=" + Arrays.toString(mGlyphs) +
                ", mPositions=" + Arrays.toString(mPositions) +
                ", mFonts=" + Arrays.toString(mFonts) +
                ", mFontIndices=" + Arrays.toString(mFontIndices) +
                ", mAdvances=" + Arrays.toString(mAdvances) +
                ", mAscent=" + mAscent +
                ", mDescent=" + mDescent +
                ", mAdvance=" + mAdvance +
                ", mBoundsX=" + mBoundsX +
                ", mBoundsY=" + mBoundsY +
                ", mBoundsWidth=" + mBoundsWidth +
                ", mBoundsHeight=" + mBoundsHeight +
                ", mComputeFlags=0x" + Integer.toHexString(mComputeFlags) +
                '}';
    }
}
