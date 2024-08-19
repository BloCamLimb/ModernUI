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

import com.ibm.icu.text.BreakIterator;
import icyllis.arc3d.core.Strike;
import icyllis.arc3d.core.Typeface;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Rect;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;
import java.util.Locale;

/**
 * Special font for Color Emoji.
 */
//TODO the layout code is not correct
public final class EmojiFont implements Font {

    private final String mName;
    private final IntSet mCoverage;
    private final float mBaseSize;
    private final float mBaseAscent; // positive
    private final float mBaseDescent;
    private final float mBaseSpacing;

    // emoji char sequence to glyph ID, 1-based
    private final Object2IntMap<CharSequence> mMap;

    // glyph ID to res name, /assets/modernui/emoji/[res name], 0-based
    private final List<String> mFiles;

    private final CharSequenceBuilder mLookupKey = new CharSequenceBuilder();

    public EmojiFont(String name, IntSet coverage, int size, int ascent, int spacing,
                     int base, Object2IntMap<CharSequence> map, List<String> files) {
        mName = name;
        mCoverage = coverage;
        mBaseSize = (float) size / base;
        mBaseAscent = (float) ascent / base;
        mBaseDescent = (float) (size - ascent) / base;
        mBaseSpacing = (float) spacing / base;
        mMap = map;
        mFiles = files;
    }

    public String getFileName(int glyphId) {
        // 1-based to 0-based
        return mFiles.get(glyphId - 1);
    }

    @Override
    public int getStyle() {
        return FontPaint.NORMAL;
    }

    @Override
    public String getFullName(@NonNull Locale locale) {
        return mName;
    }

    @Override
    public String getFamilyName(@NonNull Locale locale) {
        return mName;
    }

    @Override
    public int getMetrics(@NonNull FontPaint paint, @Nullable FontMetricsInt fm) {
        int size = paint.getFontSize();
        int ascent = (int) (0.95 + mBaseAscent * size);
        int descent = (int) (0.95 + mBaseDescent * size);
        if (fm != null) {
            fm.extendBy(-ascent, descent);
        }
        return ascent + descent;
    }

    @Override
    public boolean hasGlyph(int ch, int vs) {
        return mCoverage.contains(ch);
    }

    @Override
    public int calcGlyphScore(char[] buf, int start, int limit) {
        final var breaker = BreakIterator.getCharacterInstance(Locale.ROOT);
        final var iterator = new CharArrayIterator(buf, start, limit);

        breaker.setText(iterator);

        int prevPos = start;
        int currPos;
        while ((currPos = breaker.following(prevPos)) != BreakIterator.DONE) {
            int glyphId = find(buf, prevPos, currPos);
            if (glyphId == 0) {
                return prevPos;
            }
            prevPos = currPos;
        }

        return prevPos;
    }

    private int find(char[] buf, int start, int limit) {
        int glyphId;
        synchronized (mLookupKey) {
            glyphId = mMap.getInt(
                    mLookupKey.updateChars(buf, start, limit)
            );
        }
        if (glyphId == 0) {
            char vs = buf[limit - 1];
            if (vs == Emoji.VARIATION_SELECTOR_16) {
                // try w/o
                synchronized (mLookupKey) {
                    glyphId = mMap.getInt(
                            mLookupKey.updateChars(buf, start, limit - 1)
                    );
                }
            }
        }
        return glyphId;
    }

    @Override
    public float doSimpleLayout(char[] buf, int start, int limit,
                                FontPaint paint, IntArrayList glyphs,
                                FloatArrayList positions, float x, float y) {
        // no support
        return 0;
    }

    @Override
    public float doComplexLayout(char[] buf,
                                 int contextStart, int contextLimit,
                                 int layoutStart, int layoutLimit,
                                 boolean isRtl, FontPaint paint,
                                 IntArrayList glyphs, FloatArrayList positions,
                                 float[] advances, int advanceOffset,
                                 Rect bounds, float x, float y) {
        // Measure grapheme cluster in visual order
        final var breaker = BreakIterator.getCharacterInstance(Locale.ROOT);
        // We simply ignore the context range
        final var iterator = new CharArrayIterator(buf, layoutStart, layoutLimit);
        breaker.setText(iterator);

        boolean hint = (paint.getRenderFlags() & FontPaint.RENDER_FLAG_LINEAR_METRICS) == 0;
        float sz = paint.getFontSize();
        float add = mBaseSpacing * sz;
        if (hint) {
            add = Math.max(1, (int) (0.95 + add));
        }
        sz = mBaseSize * sz;
        if (hint) {
            sz = (int) (0.95 + sz);
        }
        float adv = sz + add * 2;

        if (hint) {
            x = (int) x;
            y = (int) y;
        }

        int prevPos;
        int currPos;
        if (isRtl) {
            prevPos = layoutLimit;
        } else {
            prevPos = layoutStart;
        }

        float currAdvance = 0;

        while ((currPos = isRtl
                ? breaker.preceding(prevPos)
                : breaker.following(prevPos)) != BreakIterator.DONE) {
            int pieceStart = Math.min(prevPos, currPos);
            int pieceLimit = Math.max(prevPos, currPos);

            int glyphId = find(buf, pieceStart, pieceLimit);
            if (glyphId != 0) {
                if (advances != null) {
                    advances[pieceStart - advanceOffset] = adv;
                }

                if (glyphs != null) {
                    glyphs.add(glyphId);
                }
                if (positions != null) {
                    positions.add(x + currAdvance + add);
                    positions.add(y);
                }

                currAdvance += adv;
            }

            prevPos = currPos;
        }

        if (bounds != null) {
            int s = paint.getFontSize();
            bounds.union(
                    (int) x,
                    (int) (y - mBaseAscent * s - 0.05),
                    (int) (x + currAdvance + 0.95),
                    (int) (y + mBaseDescent * s + 0.95)
            );
        }

        return currAdvance;
    }

    @Override
    public Strike findOrCreateStrike(FontPaint paint) {
        return null;
    }

    @Override
    public Typeface getNativeTypeface() {
        //TODO wait for Arc3D SVG font rendering
        return null;
    }
}
