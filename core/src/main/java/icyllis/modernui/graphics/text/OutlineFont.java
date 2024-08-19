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
import icyllis.arc3d.core.Typeface;
import icyllis.arc3d.core.j2d.Typeface_JDK;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.util.SparseArray;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.util.*;

public final class OutlineFont implements Font {

    static final Graphics2D[] sGraphics = new Graphics2D[4];

    static {
        var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        for (int mask = 0; mask < 4; mask++) {
            var graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    (mask & FontPaint.RENDER_FLAG_ANTI_ALIAS) != 0
                            ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                            : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    (mask & FontPaint.RENDER_FLAG_LINEAR_METRICS) != 0
                            ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
                            : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            sGraphics[mask] = graphics;
        }
    }

    private static final String[] LOGICAL_FONT_NAMES = {
            java.awt.Font.DIALOG,
            java.awt.Font.SANS_SERIF,
            java.awt.Font.SERIF,
            java.awt.Font.MONOSPACED
    };

    private final java.awt.Font mFont;
    private final Typeface_JDK mTypeface;

    private final SparseArray<java.awt.Font> mFonts = new SparseArray<>();

    private final boolean mIsLogicalFont;

    public OutlineFont(java.awt.Font font) {
        mFont = Objects.requireNonNull(font);
        mTypeface = new Typeface_JDK(mFont);
        mIsLogicalFont = Arrays.stream(LOGICAL_FONT_NAMES)
                .anyMatch(s -> s.equalsIgnoreCase(font.getName()));
    }

    public static FontRenderContext getFontRenderContext(int renderFlags) {
        return sGraphics[renderFlags].getFontRenderContext();
    }

    @Override
    public int getStyle() {
        return mFont.getStyle();
    }

    @Override
    public String getFullName(@NonNull Locale locale) {
        return mFont.getFontName(locale);
    }

    @Override
    public String getFamilyName(@NonNull Locale locale) {
        return mFont.getFamily(locale);
    }

    @NonNull
    public java.awt.Font chooseFont(int size) {
        if (size <= 1) {
            return mFont;
        }
        if (size <= 96) {
            var value = mFonts.get(size);
            if (value != null) {
                return value;
            }
            value = mFont.deriveFont((float) size);
            mFonts.put(size, value);
            return value;
        }
        return mFont.deriveFont((float) size);
    }

    @Override
    public int getMetrics(@NonNull FontPaint paint, @Nullable FontMetricsInt fm) {
        if (paint.getFontStyle() != getStyle()) {
            throw new IllegalArgumentException();
        }
        var font = chooseFont(paint.getFontSize());
        var metrics = sGraphics[paint.getRenderFlags()]
                .getFontMetrics(font);
        int ascent = metrics.getAscent(); // positive
        int descent = metrics.getDescent(); // positive
        int leading = metrics.getLeading(); // positive
        if (fm != null) {
            fm.extendBy(-ascent, descent, leading);
        }
        return ascent + descent + leading;
    }

    @Override
    public boolean hasGlyph(int ch, int vs) {
        return mFont.canDisplay(ch);
    }

    @Override
    public int calcGlyphScore(char[] buf, int start, int limit) {
        int offset = mFont.canDisplayUpTo(buf, start, limit);
        // Logical fonts have lower priority
        return (offset == -1 ? limit : offset) + (mIsLogicalFont ? -1 : 0);
    }

    @Override
    public float doSimpleLayout(char[] buf, int start, int limit,
                                FontPaint paint, IntArrayList glyphs,
                                FloatArrayList positions, float x, float y) {
        int style = paint.getFontStyle();
        if (style != getStyle()) {
            throw new IllegalArgumentException();
        }
        if (start != 0 || limit != buf.length) {
            buf = Arrays.copyOfRange(buf, start, limit);
        }
        var vector = chooseFont(paint.getFontSize())
                .createGlyphVector(
                        getFontRenderContext(paint.getRenderFlags()),
                        buf
                );
        int nGlyphs = vector.getNumGlyphs();

        if (glyphs != null || positions != null) {
            // this is visually left-to-right
            for (int i = 0; i < nGlyphs; i++) {
                if (glyphs != null) {
                    glyphs.add(vector.getGlyphCode(i));
                }
                if (positions != null) {
                    var point = vector.getGlyphPosition(i);
                    positions.add((float) point.getX() + x);
                    positions.add((float) point.getY() + y);
                }
            }
        }
        return (float) vector.getGlyphPosition(nGlyphs).getX();
    }

    @Override
    public float doComplexLayout(char[] buf,
                                 int contextStart, int contextLimit,
                                 int layoutStart, int layoutLimit,
                                 boolean isRtl, FontPaint paint,
                                 IntArrayList glyphs, FloatArrayList positions,
                                 float[] advances, int advanceOffset,
                                 Rect bounds, float x, float y) {
        int style = paint.getFontStyle();
        if (style != getStyle()) {
            throw new IllegalArgumentException();
        }
        int layoutFlags = isRtl
                ? java.awt.Font.LAYOUT_RIGHT_TO_LEFT
                : java.awt.Font.LAYOUT_LEFT_TO_RIGHT;
        if (layoutStart == contextStart) {
            layoutFlags |= java.awt.Font.LAYOUT_NO_START_CONTEXT;
        }
        if (layoutLimit == contextLimit) {
            layoutFlags |= java.awt.Font.LAYOUT_NO_LIMIT_CONTEXT;
        }
        var face = chooseFont(paint.getFontSize());
        var frc = getFontRenderContext(paint.getRenderFlags());
        var vector = face.layoutGlyphVector(
                frc, buf,
                layoutStart, layoutLimit,
                layoutFlags
        );
        int nGlyphs = vector.getNumGlyphs();

        if (advances != null) {
            final int baseFlags = isRtl
                    ? java.awt.Font.LAYOUT_RIGHT_TO_LEFT
                    : java.awt.Font.LAYOUT_LEFT_TO_RIGHT;
            // this is a bit slow
            GraphemeBreak.forTextRun(buf, paint.mLocale, layoutStart, layoutLimit,
                    (clusterStart, clusterLimit) -> {
                        int flags = baseFlags;
                        if (clusterStart == contextStart) {
                            flags |= java.awt.Font.LAYOUT_NO_START_CONTEXT;
                        }
                        if (clusterLimit == contextLimit) {
                            flags |= java.awt.Font.LAYOUT_NO_LIMIT_CONTEXT;
                        }
                        var vec = face.layoutGlyphVector(
                                frc, buf,
                                clusterStart, clusterLimit,
                                flags
                        );
                        advances[clusterStart - advanceOffset] =
                                (float) vec.getGlyphPosition(vec.getNumGlyphs()).getX();
                    });
        }

        if (glyphs != null || positions != null) {
            // this is visually left-to-right
            for (int i = 0; i < nGlyphs; i++) {
                if (glyphs != null) {
                    glyphs.add(vector.getGlyphCode(i));
                }
                if (positions != null) {
                    var point = vector.getGlyphPosition(i);
                    positions.add((float) point.getX() + x);
                    positions.add((float) point.getY() + y);
                }
            }
        }

        if (bounds != null) {
            // this is a bit slow
            var r = vector.getPixelBounds(null, x, y);
            bounds.union(r.x, r.y, r.x + r.width, r.y + r.height);
        }

        return (float) vector.getGlyphPosition(nGlyphs).getX();
    }

    @Override
    public Strike findOrCreateStrike(FontPaint paint) {
        return null;
    }

    @Override
    public Typeface getNativeTypeface() {
        return mTypeface;
    }
}
