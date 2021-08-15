/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.text;

import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.math.MathUtil;
import icyllis.modernui.platform.RenderCore;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The layout of a styled text run, including glyphs' measurements and rendering info.
 *
 * @see LayoutCache
 * @since 2.6
 */
public class LayoutPiece {

    // all glyphs used for rendering, invisible glyphs have been removed
    // the order is visually left-to-right. shared pointers
    private TexturedGlyph[] mGlyphs;

    // x1 y1 x2 y2... relative to the same pivot, for rendering mGlyphs
    private float[] mPositions;

    // glyphs to char indices
    private int[] mCharIndices;

    // the size and order are relative to the text buf (char array)
    // only grapheme cluster bounds have advances, others are zeros
    // eg: [13.57, 0, 14.26, 0, 0]
    private final float[] mAdvances;

    // maximum font metrics
    private final int mAscent;
    private final int mDescent;

    // total advance
    private float mAdvance;

    /**
     * Creates the glyph layout of a piece.
     *
     * @param buf   text buffer
     * @param start start char offset
     * @param end   end char index
     * @param isRtl whether to layout in right-to-left
     * @param paint the font paint affecting the glyph layout
     */
    public LayoutPiece(@Nonnull char[] buf, int start, int end, boolean isRtl, @Nonnull FontPaint paint) {
        GlyphManager engine = GlyphManager.getInstance();
        mAdvances = new float[end - start];
        FontMetricsInt extent = new FontMetricsInt();

        // async on render thread
        final List<TexturedGlyph> glyphs = new ArrayList<>();
        final FloatList positions = new FloatArrayList();
        final IntList charIndices = new IntArrayList();

        final List<FontRun> items = paint.mTypeface.itemize(buf, start, end);
        for (int runIndex = isRtl ? items.size() - 1 : 0;
             isRtl ? runIndex >= 0 : runIndex < items.size(); ) {
            FontRun run = items.get(runIndex);

            Font derived = engine.getFontMetrics(run.mFont, paint, extent);
            GlyphVector vector = engine.layoutGlyphVector(derived, buf, run.mStart, run.mEnd, isRtl);

            ClusterWork clusterWork = new ClusterWork(derived, buf, isRtl, mAdvances, start);
            GraphemeBreak.forTextRun(buf, paint.mLocale, run.mStart, run.mEnd, clusterWork);

            TextureWork textureWork = new TextureWork(vector, glyphs, positions, mAdvance, charIndices, run.mStart);
            RenderCore.recordRenderCall(textureWork);

            mAdvance += vector.getGlyphPosition(vector.getNumGlyphs()).getX();

            if (isRtl) {
                runIndex--;
            } else {
                runIndex++;
            }
        }
        // flatten
        RenderCore.recordRenderCall(() -> {
            mGlyphs = glyphs.toArray(new TexturedGlyph[0]);
            mPositions = positions.toFloatArray();
            mCharIndices = charIndices.toIntArray();
        });

        mAscent = extent.mAscent;
        mDescent = extent.mDescent;
    }

    private static class ClusterWork implements GraphemeBreak.RunConsumer {

        private final Font mFont;
        private final char[] mBuf;
        private final boolean mIsRtl;
        private final float[] mAdvances;
        private final int mContextStart;

        public ClusterWork(Font font, char[] buf, boolean isRtl, float[] advances, int contextStart) {
            mFont = font;
            mBuf = buf;
            mIsRtl = isRtl;
            mAdvances = advances;
            mContextStart = contextStart;
        }

        @Override
        public void onRun(int start, int end) {
            GlyphVector vector = GlyphManager.getInstance().layoutGlyphVector(mFont, mBuf, start, end, mIsRtl);
            mAdvances[start - mContextStart] = (float) vector.getGlyphPosition(vector.getNumGlyphs()).getX();
        }
    }

    private static class TextureWork implements Runnable {

        private final GlyphVector mVector;
        private final List<TexturedGlyph> mGlyphs;
        private final FloatList mPositions;
        private final float mOffsetX;
        private final IntList mIndices;
        private final int mStart;

        private TextureWork(GlyphVector vector, List<TexturedGlyph> glyphs, FloatList positions,
                            float offsetX, IntList indices, int start) {
            mVector = vector;
            mGlyphs = glyphs;
            mPositions = positions;
            mOffsetX = offsetX;
            mIndices = indices;
            mStart = start;
        }

        @Override
        public void run() {
            GlyphManager engine = GlyphManager.getInstance();
            for (int i = 0, e = mVector.getNumGlyphs(); i < e; i++) {
                TexturedGlyph glyph = engine.lookupGlyph(mVector.getFont(), mVector.getGlyphCode(i));
                // ignore invisible glyphs
                if (glyph != null) {
                    mGlyphs.add(glyph);
                    Point2D point = mVector.getGlyphPosition(i);
                    mPositions.add((float) point.getX() + mOffsetX);
                    mPositions.add((float) point.getY());
                    mIndices.add(mVector.getGlyphCharIndex(i) + mStart);
                }
            }
        }
    }

    /**
     * The array is about all visible glyphs for rendering in order from left to right.
     *
     * @return glyphs
     */
    @RenderThread
    public TexturedGlyph[] getGlyphs() {
        return mGlyphs;
    }

    /**
     * This array holds the repeat of x offset, y offset of glyph positions.
     * The length is twice as long as the glyph array.
     *
     * @return glyph positions
     */
    @RenderThread
    public float[] getPositions() {
        return mPositions;
    }

    /**
     * This array maps glyph to start char index of original text buffer with constructor char offset.
     * For RTL text is in descending order, since glyphs array is always left to right.
     * <p>
     * This can be used to render the sub-range of this piece, if target index doesn't break grapheme cluster.
     * The length is equal to that of the glyph array.
     *
     * @return char indices
     */
    @RenderThread
    public int[] getCharIndices() {
        return mCharIndices;
    }

    /**
     * The array of all chars advance, the length and order are relative to the text buffer.
     * Only grapheme cluster bounds have advances, others are zeros. For example: [13.57, 0, 14.26, 0, 0].
     * The length is constructor <code>end - start</code>.
     *
     * @return advances
     */
    public float[] getAdvances() {
        return mAdvances;
    }

    /**
     * Expands the font metrics of the maximum extent of this piece.
     *
     * @param extent to expand from
     */
    public void getExtent(@Nonnull FontMetricsInt extent) {
        extent.extendBy(mAscent, mDescent);
    }

    /**
     * Returns the total advance of this piece.
     *
     * @return advance
     */
    public float getAdvance() {
        return mAdvance;
    }

    public int getMemoryUsage() {
        return MathUtil.roundUp(12 + 16 + 8 + 16 + 8 + 16 + 8 + 16 + 8 + 4 + 4 + 4 +
                (mGlyphs == null ? 0 : mGlyphs.length * (8 + 4 + 4 + 4)) +
                (mAdvances.length << 2), 8);
    }
}
