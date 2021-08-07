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

import icyllis.modernui.math.MathUtil;
import icyllis.modernui.platform.RenderCore;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;

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

    // the size and order are relative to the text buf (char array)
    // only grapheme cluster bounds have advances, others are zeros
    // eg: [13.57, 0, 14.26, 0, 0]
    private final float[] mAdvances;

    // maximum font metrics
    private final int mAscent;
    private final int mDescent;

    // total advance
    private float mAdvance;

    public LayoutPiece(@Nonnull char[] buf, int start, int end, boolean isRtl, @Nonnull FontPaint paint) {
        GlyphManager engine = GlyphManager.getInstance();
        mAdvances = new float[end - start];
        FontMetricsInt extent = new FontMetricsInt();

        // deferred
        List<TexturedGlyph> glyphs = new ArrayList<>();
        FloatList positions = new FloatArrayList();

        List<FontRun> items = paint.mTypeface.itemize(buf, start, end);
        for (int runIndex = isRtl ? items.size() - 1 : 0;
             isRtl ? runIndex >= 0 : runIndex < items.size(); ) {
            FontRun run = items.get(runIndex);

            Font derived = engine.getFontMetrics(run.mFont, paint, extent);
            GlyphVector vector = engine.layoutGlyphVector(derived, buf, run.mStart, run.mEnd, isRtl);

            ClusterWork clusterWork = new ClusterWork(derived, buf, isRtl, mAdvances, start);
            GraphemeBreak.forTextRun(buf, paint.mLocale, run.mStart, run.mEnd, clusterWork);

            TextureWork textureWork = new TextureWork(vector, glyphs, positions, mAdvance);
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

    public TexturedGlyph[] getGlyphs() {
        return mGlyphs;
    }

    public float[] getPositions() {
        return mPositions;
    }

    public float[] getAdvances() {
        return mAdvances;
    }

    public void getExtent(@Nonnull FontMetricsInt extent) {
        extent.extendBy(mAscent, mDescent);
    }

    public float getAdvance() {
        return mAdvance;
    }

    public int getMemoryUsage() {
        return MathUtil.roundUp(12 + 16 + 16 + 16 + 4 + 4 + 4 +
                (mGlyphs == null ? 0 : mGlyphs.length << 4) +
                (mAdvances.length << 2), 8);
    }
}
