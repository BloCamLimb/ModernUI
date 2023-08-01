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

import icyllis.arc3d.core.Strike;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Rect;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.Locale;
import java.util.Map;

/**
 * Special font for Color Emoji.
 */
public final class EmojiFont implements Font {

    private final String mFontName;
    private final IntSet mSupportedUnicodes;
    private final int mSpriteSize;
    private final int mAscent; // positive
    private final float mNormalizeFactor;

    private final Map<CharSequence, EmojiEntry> mEmojiMap;

    /**
     * @param id       used as glyph code
     * @param image    image file name
     * @param sequence emoji char sequence
     */
    public record EmojiEntry(int id, String image, String sequence) {
    }

    public EmojiFont(String fontName, IntSet supportedUnicodes, int spriteSize, int ascent, float normalizeFactor,
                     Map<CharSequence, EmojiEntry> emojiMap) {
        mFontName = fontName;
        mSupportedUnicodes = supportedUnicodes;
        mSpriteSize = spriteSize;
        mAscent = ascent;
        mNormalizeFactor = normalizeFactor;
        mEmojiMap = emojiMap;
    }

    @Override
    public int getStyle() {
        return 0;
    }

    @Override
    public String getFullName(@NonNull Locale locale) {
        return mFontName;
    }

    @Override
    public String getFamilyName(@NonNull Locale locale) {
        return mFontName;
    }

    @Override
    public int getMetrics(@NonNull FontPaint paint, @Nullable FontMetricsInt fm) {
        return 0;
    }

    @Override
    public boolean hasGlyph(int ch, int vs) {
        return mSupportedUnicodes.contains(ch);
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


        return 0;
    }

    @Override
    public Strike findOrCreateStrike(FontPaint paint) {
        return null;
    }
}
