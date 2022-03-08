/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.font;

import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.core.ArchCore;
import icyllis.modernui.graphics.font.FontCollection.Run;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The layout of a styled text run, including text shaping results, glyph measurements and
 * their rendering information.
 *
 * @see LayoutCache
 * @see GlyphManager
 * @see GraphemeBreak
 * @since 2.6
 */
public class LayoutPiece {

    // all glyphs used for rendering, invisible glyphs have been removed
    // the order is visually left-to-right. shared pointers
    private TexturedGlyph[] mGlyphs;

    // x1 y1 x2 y2... relative to the same pivot, for rendering mGlyphs
    private float[] mPositions;

    // glyphs to char indices
    //private int[] mCharIndices;

    // the size and order are relative to the text buf (char array)
    // only grapheme cluster bounds have advances, others are zeros
    // eg: [13.57, 0, 14.26, 0, 0]
    private float[] mAdvances;

    // maximum font metrics
    // besides, we use their sign bit for constructor flags
    int mAscent;
    int mDescent;

    // total advance
    private float mAdvance;

    /**
     * Creates the glyph layout of a piece. No reference to the buffer will be held.
     *
     * @param buf     text buffer, cannot be null or empty
     * @param start   start char offset
     * @param end     end char index
     * @param isRtl   whether to layout in right-to-left
     * @param paint   the font paint affecting measurement
     * @param measure whether to calculate individual char advance
     * @param layout  whether to compute full layout for rendering
     * @param hint    pass if you have already a layout piece
     * @see LayoutCache#getOrCreate(char[], int, int, boolean, FontPaint, boolean, boolean)
     */
    LayoutPiece(@Nonnull char[] buf, int start, int end, boolean isRtl, @Nonnull FontPaint paint,
                boolean measure, boolean layout, @Nullable LayoutPiece hint) {
        if (start < 0 || start >= end || buf.length == 0 || end > buf.length) {
            throw new IllegalArgumentException();
        }
        GlyphManager engine = GlyphManager.getInstance();

        boolean receivingLayout = false;
        if (hint != null) {
            if ((hint.mAscent & 0x80000000) != 0) {
                mAdvances = hint.mAdvances;
                assert mAdvances != null;
            }
            if ((hint.mDescent & 0x80000000) != 0) {
                if (ArchCore.isOnRenderThread()) {
                    mGlyphs = hint.mGlyphs;
                    mPositions = hint.mPositions;
                    assert mGlyphs != null;
                } else {
                    ArchCore.postOnRenderThread(() -> {
                        mGlyphs = hint.mGlyphs;
                        mPositions = hint.mPositions;
                        assert mGlyphs != null;
                    });
                    receivingLayout = true;
                }
            }
        }

        // check again if needed currently
        measure = measure && mAdvances == null;
        layout = layout && (mGlyphs == null || !receivingLayout); // checked hint

        if (measure) {
            mAdvances = new float[end - start];
        }
        final FontMetricsInt extent = new FontMetricsInt();

        // async on render thread
        final List<TexturedGlyph> glyphs = new ArrayList<>();
        final FloatList positions = new FloatArrayList();
        //final IntList charIndices = new IntArrayList();

        final List<Run> items = paint.mFontCollection.itemize(buf, start, end);
        for (int runIndex = isRtl ? items.size() - 1 : 0;
             isRtl ? runIndex >= 0 : runIndex < items.size(); ) {
            Run run = items.get(runIndex);

            Font derived = engine.getFontMetrics(run.mFont, paint, extent);
            GlyphVector vector = engine.layoutGlyphVector(derived, buf, run.mStart, run.mEnd, isRtl);

            if (measure) {
                ClusterWork clusterWork = new ClusterWork(derived, buf, isRtl, mAdvances, start);
                GraphemeBreak.forTextRun(buf, paint.mLocale, run.mStart, run.mEnd, clusterWork);
            }

            if (layout) {
                TextureWork textureWork = new TextureWork(vector, glyphs, positions, mAdvance);
                if (ArchCore.isOnRenderThread()) {
                    textureWork.run();
                } else {
                    ArchCore.postOnRenderThread(textureWork);
                }
            }

            mAdvance += vector.getGlyphPosition(vector.getNumGlyphs()).getX();

            if (isRtl) {
                runIndex--;
            } else {
                runIndex++;
            }
        }
        if (layout) {
            if (ArchCore.isOnRenderThread()) {
                mGlyphs = glyphs.toArray(new TexturedGlyph[0]);
                mPositions = positions.toFloatArray();
            } else {
                ArchCore.postOnRenderThread(() -> {
                    mGlyphs = glyphs.toArray(new TexturedGlyph[0]);
                    mPositions = positions.toFloatArray();
                });
            }
        }

        mAscent = extent.ascent;
        if (measure || mAdvances != null) {
            mAscent |= 0x80000000;
        }
        mDescent = extent.descent;
        if (layout || mGlyphs != null || receivingLayout) {
            mDescent |= 0x80000000;
        }
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

        private TextureWork(GlyphVector vector, List<TexturedGlyph> glyphs, FloatList positions, float offsetX) {
            mVector = vector;
            mGlyphs = glyphs;
            mPositions = positions;
            mOffsetX = offsetX;
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
                }
            }
        }
    }

    /**
     * The array is about all visible glyphs for rendering in order from left to right.
     * <p>
     * May null if not compute full layout or not from render thread.
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
     * <p>
     * May null if not compute full layout or not from render thread.
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
    @Deprecated
    public int[] getCharIndices() {
        throw new UnsupportedOperationException();
    }

    /**
     * The array of all chars advance, the length and order are relative to the text buffer.
     * Only grapheme cluster bounds have advances, others are zeros. For example: [13.57, 0, 14.26, 0, 0].
     * The length is constructor <code>end - start</code>.
     * <p>
     * May null if not compute measurement.
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
        extent.extendBy(getAscent(), getDescent());
    }

    /**
     * Gets the font metrics of the maximum extent of this piece.
     *
     * @return ascent to baseline, always positive
     */
    public int getAscent() {
        return mAscent & 0x7FFFFFFF;
    }

    /**
     * Gets the font metrics of the maximum extent of this piece.
     *
     * @return descent to baseline, always positive
     */
    public int getDescent() {
        return mDescent & 0x7FFFFFFF;
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
        int m = 48;
        if (mGlyphs != null) {
            m += 16 + 16 + (mGlyphs.length << 4);
        }
        if (mAdvances != null) {
            m += 16 + (mAdvances.length << 2);
        }
        return m;
    }
}
