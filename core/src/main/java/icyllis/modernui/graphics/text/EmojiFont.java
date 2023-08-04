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
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Rect;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.Locale;

/**
 * Special font for Color Emoji.
 */
public final class EmojiFont implements Font {

    private final String mName;
    private final IntSet mCoverage;
    private final float mBaseSize;
    private final float mBaseAscent; // positive
    private final float mBaseDescent;
    private final float mBaseSpacing;

    private final Object2IntMap<CharSequence> mMap;

    private final CharSequenceBuilder mLookupKey = new CharSequenceBuilder();

    public EmojiFont(String name, IntSet coverage, int size, int ascent, int spacing,
                     int base, Object2IntMap<CharSequence> map) {
        mName = name;
        mCoverage = coverage;
        mBaseSize = (float) size / base;
        mBaseAscent = (float) ascent / base;
        mBaseDescent = (float) (size - ascent) / base;
        mBaseSpacing = (float) spacing / base;
        mMap = map;
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
        final var breaker = BreakIterator.getCharacterInstance(paint.mLocale);
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

            int code;
            synchronized (mLookupKey) {
                code = mMap.getInt(
                        mLookupKey.updateChars(buf, pieceStart, pieceLimit)
                );
            }
            if (code == 0) {
                char vs = buf[pieceLimit - 1];
                if (vs != Emoji.VARIATION_SELECTOR_15) {
                    if (vs == Emoji.VARIATION_SELECTOR_16) {
                        // try w/o
                        synchronized (mLookupKey) {
                            code = mMap.getInt(
                                    mLookupKey.updateChars(buf, pieceStart, pieceLimit - 1)
                            );
                        }
                    } else {
                        // try w/
                        synchronized (mLookupKey) {
                            mLookupKey.updateChars(buf, pieceStart, pieceLimit)
                                    .add((char) Emoji.VARIATION_SELECTOR_16);
                            code = mMap.getInt(mLookupKey);
                        }
                    }
                }
            }
            if (code != 0) {
                if (advances != null) {
                    advances[pieceStart - advanceOffset] = adv;
                }

                if (glyphs != null) {
                    glyphs.add(code);
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
}
