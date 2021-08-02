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

import icyllis.modernui.platform.RenderCore;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.util.ArrayList;
import java.util.List;

/**
 * The layout of a styled text run, including glyphs' measurements and rendering info.
 *
 * @see LayoutCache
 * @since 2.6
 */
public class LayoutPiece {

    // all glyphs used for rendering, null elements that have nothing to render
    // the order is visually left-to-right
    private TexturedGlyph[] mGlyphs;

    // x1 y1 x2 y2... relative to the same pivot, for rendering mGlyphs
    private final float[] mPositions;

    // the size and order are relative to the text buf (char array)
    // only grapheme cluster bounds have advances, others are zeros
    // eg: [13.57, 0, 14.26, 0, 0]
    private final float[] mAdvances;

    // maximum font metrics
    private final FontMetricsInt mExtent = new FontMetricsInt();

    // total advance
    private float mAdvance;

    public LayoutPiece(@Nonnull char[] buf, int start, int limit, boolean isRtl, @Nonnull FontPaint paint) {
        GlyphManager engine = GlyphManager.getInstance();
        FloatList positions = new FloatArrayList();
        mAdvances = new float[limit - start];

        // deferred
        List<TexturedGlyph> glyphs = new ArrayList<>();

        List<FontRun> items = paint.mTypeface.itemize(buf, start, limit);
        for (int runIndex = isRtl ? items.size() - 1 : 0;
             isRtl ? runIndex >= 0 : runIndex < items.size(); ) {
            FontRun run = items.get(runIndex);

            Font derived = engine.getFontMetrics(run.mFont, paint, mExtent);
            GlyphVector vector = engine.layoutGlyphVector(derived, buf, run.mStart, run.mEnd, isRtl);

            int num = vector.getNumGlyphs();
            float[] pos = vector.getGlyphPositions(0, num, null);
            for (int i = 0; i < pos.length; i += 2) {
                pos[i] += mAdvance;
            }
            positions.addElements(positions.size(), pos);

            ClusterWork clusterWork = new ClusterWork(derived, buf, isRtl, mAdvances, start);
            GraphemeBreak.forTextRun(buf, paint.mLocale, run.mStart, run.mEnd, clusterWork);

            mAdvance += vector.getGlyphPosition(num).getX();

            TextureWork textureWork = new TextureWork(vector, glyphs);
            RenderCore.recordRenderCall(textureWork);

            if (isRtl) {
                runIndex--;
            } else {
                runIndex++;
            }
        }
        mPositions = positions.toFloatArray();

        RenderCore.recordRenderCall(() -> mGlyphs = glyphs.toArray(new TexturedGlyph[0]));
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

        private TextureWork(GlyphVector vector, List<TexturedGlyph> glyphs) {
            mVector = vector;
            mGlyphs = glyphs;
        }

        @Override
        public void run() {
            GlyphManager engine = GlyphManager.getInstance();
            for (int i = 0, e = mVector.getNumGlyphs(); i < e; i++) {
                mGlyphs.add(engine.lookupGlyph(mVector.getFont(), mVector.getGlyphCode(i)));
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

    public FontMetricsInt getExtent() {
        return mExtent;
    }

    public float getAdvance() {
        return mAdvance;
    }
}
