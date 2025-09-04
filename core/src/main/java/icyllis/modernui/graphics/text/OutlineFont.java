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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.util.Log;
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
                    (mask & 1) != 0
                            ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                            : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    (mask & 2) != 0
                            ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
                            : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            sGraphics[mask] = graphics;
        }
    }

    // legacy code uses ceiling, new code uses rounding
    private static final boolean USE_ROUNDING_FONT_METRICS = true;

    private static final String[] LOGICAL_FONT_NAMES = {
            java.awt.Font.DIALOG,
            java.awt.Font.SANS_SERIF,
            java.awt.Font.SERIF,
            java.awt.Font.MONOSPACED
    };

    private final java.awt.Font mFont;
    private final icyllis.arc3d.sketch.j2d.Typeface_JDK mTypeface;

    private final SparseArray<java.awt.Font> mFonts = new SparseArray<>();

    private final boolean mIsLogicalFont;

    public OutlineFont(java.awt.Font font) {
        mFont = Objects.requireNonNull(font);
        mFont.getFamily(); // cache Font2D field
        mTypeface = new icyllis.arc3d.sketch.j2d.Typeface_JDK(mFont);
        mIsLogicalFont = Arrays.stream(LOGICAL_FONT_NAMES)
                .anyMatch(s -> s.equalsIgnoreCase(font.getName()));
    }

    private static Graphics2D getGraphics(@NonNull FontPaint paint) {
        return sGraphics[(paint.isAntiAlias() ? 1 : 0) | (paint.isLinearMetrics() ? 2 : 0)];
    }

    public static FontRenderContext getFontRenderContext(@NonNull Paint paint) {
        return sGraphics[(paint.isTextAntiAlias() ? 1 : 0) | (paint.isLinearText() ? 2 : 0)]
                .getFontRenderContext();
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
    private java.awt.Font chooseFont(float size) {
        if (size <= 1) {
            return mFont;
        }
        if (size <= 96) {
            int key = (int) (size / FontPaint.FONT_SIZE_GRANULARITY + 0.5f);
            var value = mFonts.get(key);
            if (value != null) {
                return value;
            }
            value = mFont.deriveFont(key * FontPaint.FONT_SIZE_GRANULARITY);
            mFonts.put(key, value);
            return value;
        }
        return mFont.deriveFont(size);
    }

    @NonNull
    public java.awt.Font chooseFont(int size) {
        return chooseFont((float) size);
    }

    @NonNull
    public java.awt.Font chooseFont(@NonNull Paint paint) {
        return chooseFont(FontPaint.getCanonicalFontSize(paint.getTextSize()));
    }

    @Override
    public int getMetrics(@NonNull FontPaint paint, @Nullable FontMetricsInt fm) {
        if (paint.getFontStyle() != getStyle()) {
            throw new IllegalArgumentException();
        }
        var font = chooseFont(paint.getFontSize());
        var g = getGraphics(paint);
        int ascent; // positive
        int descent; // positive
        int leading; // positive
        if (USE_ROUNDING_FONT_METRICS) {
            var metrics = font.getLineMetrics("M", g.getFontRenderContext());
            ascent = Math.round(metrics.getAscent());
            descent = Math.round(metrics.getDescent());
            leading = Math.round(metrics.getLeading());
        } else {
            try {
                var metrics = g.getFontMetrics(font);
                ascent = metrics.getAscent();
                descent = metrics.getDescent();
                leading = metrics.getLeading();
            } catch (HeadlessException e) {
                // this is used in some scenarios, the results of the two methods are the same
                var metrics = font.getLineMetrics("M", g.getFontRenderContext());
                ascent = (int) (0.95f + metrics.getAscent());
                descent = (int) (0.95f + metrics.getDescent());
                leading = (int) (0.95f + metrics.getDescent() + metrics.getLeading()) - descent;
            }
        }
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
        var frc = getGraphics(paint).getFontRenderContext();
        var vector = chooseFont(paint.getFontSize())
                .createGlyphVector(frc, buf);
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
        var frc = getGraphics(paint).getFontRenderContext();
        var vector = face.layoutGlyphVector(
                frc, buf,
                layoutStart, layoutLimit,
                layoutFlags
        );
        int nGlyphs = vector.getNumGlyphs();

        if (advances != null && nGlyphs > 0) {
            final int baseFlags = isRtl
                    ? java.awt.Font.LAYOUT_RIGHT_TO_LEFT
                    : java.awt.Font.LAYOUT_LEFT_TO_RIGHT;
            // this is a bit slow
            int[] clusters = vector.getGlyphCharIndices(0, nGlyphs, null);
            forClusterRange(clusters, layoutStart, layoutLimit,
                    (clusterStart, clusterLimit) -> {
                        if (clusterStart < layoutStart || clusterStart >= clusterLimit
                                || clusterLimit > layoutLimit) {
                            Log.LOGGER.error(FontFamily.MARKER,
                                    "cluster range ({}, {}) out of layout bounds ({}, {})",
                                    clusterStart, clusterLimit, layoutStart, layoutLimit);
                            return;
                        }
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

    private static void forClusterRange(@NonNull int[] clusters, int layoutStart, int layoutLimit,
                                        @NonNull ClusterConsumer consumer) {
        assert clusters.length > 0;
        Arrays.sort(clusters);
        int prev = clusters[0];
        for (int i = 1; i < clusters.length; i++) {
            int v = clusters[i];
            if (v == prev) continue;
            consumer.accept(layoutStart + prev, layoutStart + v);
            prev = v;
        }
        consumer.accept(layoutStart + prev, layoutLimit);
    }

    @FunctionalInterface
    private interface ClusterConsumer {

        void accept(int clusterStart, int clusterEnd);
    }

    @Override
    public icyllis.arc3d.sketch.Typeface getNativeTypeface() {
        return mTypeface;
    }

    @Override
    public String toString() {
        return "OutlineFont{" +
                "mFont=" + mFont +
                ", mTypeface=" + mTypeface +
                ", mIsLogicalFont=" + mIsLogicalFont +
                '}';
    }
}
