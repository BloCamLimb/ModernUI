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
import icyllis.modernui.core.NativeImage;
import icyllis.modernui.text.TextUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages all glyphs, font atlases, measures glyph metrics and draw them of
 * different sizes and styles, and upload them to generated OpenGL textures.
 *
 * @see GLFontAtlas
 * @see FontCollection
 */
public class GlyphManager {

    public static final Marker MARKER = MarkerManager.getMarker("Glyph");

    /**
     * The width in pixels of a transparent border between individual glyphs in the atlas.
     * This border keeps neighboring glyphs from "bleeding through" when mipmap used.
     */
    public static final int GLYPH_BORDER = 2;

    /**
     * Transparent (alpha zero) black background color for use with BufferedImage.clearRect().
     */
    private static final Color BG_COLOR = new Color(0, 0, 0, 0);

    /**
     * Config values.
     * Bitmap-like fonts, with anti aliasing and high precision OFF.
     * This may require additional reviews on pixel alignment.
     */
    public static volatile boolean sAntiAliasing = true;
    public static volatile boolean sFractionalMetrics = true;

    /**
     * The global instance.
     */
    private static volatile GlyphManager sInstance;

    /**
     * All font atlases, with specified font size.
     */
    private Int2ObjectOpenHashMap<GLFontAtlas> mAtlases;

    /**
     * Font (with size and style) to int key.
     */
    private Object2IntOpenHashMap<Font> mFontTable;

    private final Object2IntFunction<Font> mFontTableMapper = f -> mFontTable.size();

    /**
     * Draw a single glyph onto this image and then loaded from here into an OpenGL texture.
     */
    private BufferedImage mImage;

    /**
     * The Graphics2D associated with glyph image and used for bit blit.
     */
    private Graphics2D mGraphics;

    /**
     * Intermediate data array for use with image.
     */
    private int[] mImageData;

    /**
     * A direct buffer used for loading the pre-rendered glyph images into OpenGL textures.
     */
    private ByteBuffer mImageBuffer;

    private final CopyOnWriteArrayList<Runnable> mAtlasResizeCallbacks = new CopyOnWriteArrayList<>();

    private GlyphManager() {
        // init
        reload();
    }

    @Nonnull
    public static GlyphManager getInstance() {
        if (sInstance == null) {
            synchronized (GlyphManager.class) {
                if (sInstance == null) {
                    sInstance = new GlyphManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * Reload the glyph manager, clear all created textures.
     */
    public void reload() {
        if (mAtlases != null) {
            for (GLFontAtlas atlas : mAtlases.values()) {
                atlas.close();
            }
        }
        mAtlases = new Int2ObjectOpenHashMap<>();
        mFontTable = new Object2IntOpenHashMap<>();
        mFontTable.defaultReturnValue(-1);
        allocateImage(64, 64);
    }

    /**
     * Given a font, perform full text layout/shaping and create a new GlyphVector for a text.
     *
     * @param font  the derived Font used to layout a GlyphVector for the text
     * @param text  the plain text to layout
     * @param start the offset into text at which to start the layout
     * @param limit the (offset + length) at which to stop performing the layout
     * @param isRtl whether the text should layout right-to-left
     * @return the newly laid-out GlyphVector
     */
    @Nonnull
    public GlyphVector layoutGlyphVector(@Nonnull Font font, char[] text, int start, int limit, boolean isRtl) {
        return font.layoutGlyphVector(mGraphics.getFontRenderContext(), text, start, limit,
                isRtl ? Font.LAYOUT_RIGHT_TO_LEFT : Font.LAYOUT_LEFT_TO_RIGHT);
    }

    @Nonnull
    public GlyphVector createGlyphVector(@Nonnull Font font, char[] text) {
        return font.createGlyphVector(mGraphics.getFontRenderContext(), text);
    }

    /**
     * Given a derived font and a glyph code within that font, locate the glyph's pre-rendered image
     * in the glyph atlas and return its cache entry. The entry stores the texture with the
     * pre-rendered glyph image, as well as the position and size of that image within the texture.
     *
     * @param font      the font (with size and style) to which this glyphCode belongs and which
     *                  was used to pre-render the glyph
     * @param glyphCode the font specific glyph code (should be laid-out) to lookup in the atlas
     * @return the cached glyph sprite or null if the glyph has nothing to render
     */
    @Nullable
    @RenderThread
    public GLBakedGlyph lookupGlyph(@Nonnull Font font, int glyphCode) {
        long fontKey = mFontTable.computeIfAbsent(font, mFontTableMapper);
        long key = (fontKey << 32L) | glyphCode;
        GLFontAtlas atlas = mAtlases.computeIfAbsent(font.getSize(), __ -> new GLFontAtlas());
        GLBakedGlyph glyph = atlas.getGlyph(key);
        if (glyph != null && glyph.texture == 0) {
            return cacheGlyph(font, glyphCode, atlas, glyph, key);
        }
        return glyph;
    }

    @RenderThread
    public void debug() {
        String basePath = NativeImage.saveDialogGet(NativeImage.SaveFormat.PNG, "FontAtlas");
        if (basePath != null) {
            // XXX: remove extension name
            basePath = basePath.substring(0, basePath.length() - 4);
        }
        int index = 0;
        for (var atlas : mAtlases.values()) {
            if (basePath != null) {
                atlas.debug(basePath + "_" + index);
                index++;
            } else {
                atlas.debug(null);
            }
        }
    }

    public void dumpInfo(PrintWriter pw) {
        int glyphSize = 0;
        long memorySize = 0;
        for (var atlas : mAtlases.values()) {
            glyphSize += atlas.getGlyphCount();
            memorySize += atlas.getMemorySize();
        }
        pw.print("GlyphManager: ");
        pw.print("Atlases=" + mAtlases.size());
        pw.print(", Glyphs=" + glyphSize);
        pw.println(", MemorySize=" + TextUtils.binaryCompact(memorySize) + " (" + memorySize + " bytes)");
    }

    @Nullable
    @RenderThread
    private GLBakedGlyph cacheGlyph(@Nonnull Font font, int glyphCode,
                                    @Nonnull GLFontAtlas atlas, @Nonnull GLBakedGlyph glyph,
                                    long key) {
        // there's no need to layout glyph vector, we only draw the specific glyphCode
        // which is already laid-out in LayoutEngine
        GlyphVector vector = font.createGlyphVector(mGraphics.getFontRenderContext(), new int[]{glyphCode});

        Rectangle bounds = vector.getPixelBounds(null, 0, 0);

        if (bounds.width == 0 || bounds.height == 0) {
            atlas.setNull(key);
            return null;
        }

        //glyph.advance = vector.getGlyphMetrics(0).getAdvanceX();
        glyph.x = bounds.x;
        glyph.y = bounds.y;
        glyph.width = bounds.width;
        glyph.height = bounds.height;
        int borderedWidth = bounds.width + GLYPH_BORDER * 2;
        int borderedHeight = bounds.height + GLYPH_BORDER * 2;

        while (borderedWidth > mImage.getWidth() || borderedHeight > mImage.getHeight()) {
            allocateImage(mImage.getWidth() << 1, mImage.getHeight() << 1);
        }

        // give it an offset to draw at origin
        mGraphics.drawGlyphVector(vector, GLYPH_BORDER - bounds.x, GLYPH_BORDER - bounds.y);

        // copy raw pixel data from BufferedImage to imageData array with one integer per pixel in 0xAARRGGBB form
        mImage.getRGB(0, 0, borderedWidth, borderedHeight, mImageData, 0, borderedWidth);

        final int size = borderedWidth * borderedHeight;
        for (int i = 0; i < size; i++) {
            // alpha channel for grayscale texture
            mImageBuffer.put((byte) (mImageData[i] >>> 24));
        }
        mImageBuffer.flip();

        boolean resized = atlas.stitch(glyph, MemoryUtil.memAddress(mImageBuffer));
        if (resized) {
            for (var r : mAtlasResizeCallbacks) {
                r.run();
            }
        }

        mGraphics.clearRect(0, 0, mImage.getWidth(), mImage.getHeight());
        mImageBuffer.clear();
        return glyph;
    }

    private void allocateImage(int width, int height) {
        mImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        mGraphics = mImage.createGraphics();

        mImageData = new int[width * height];
        mImageBuffer = BufferUtils.createByteBuffer(mImageData.length); // auto GC

        // set background color for use with clearRect()
        mGraphics.setBackground(BG_COLOR);

        // drawImage() to this buffer will copy all source pixels instead of alpha blending them into the current image
        mGraphics.setComposite(AlphaComposite.Src);

        // this only for shape rendering, so we turn it off
        mGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        if (sAntiAliasing) {
            mGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            mGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
        if (sFractionalMetrics) {
            mGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        } else {
            mGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        }
    }

    public void addAtlasResizeCallback(Runnable callback) {
        mAtlasResizeCallbacks.add(callback);
    }

    public void removeAtlasResizeCallback(Runnable callback) {
        mAtlasResizeCallbacks.remove(callback);
    }

    /**
     * Re-calculate font metrics in pixels, the higher 32 bits are ascent and
     * lower 32 bits are descent.
     */
    @SuppressWarnings("MagicConstant")
    public int getFontMetrics(@Nonnull FontPaint paint, @Nullable FontMetricsInt fm) {
        int ascent = 0, descent = 0, height = 0;
        for (Font f : paint.mFontCollection.getFonts()) {
            FontMetrics metrics = mGraphics.getFontMetrics(f.deriveFont(paint.getFontStyle(), paint.mFontSize));
            ascent = Math.max(ascent, metrics.getAscent()); // positive
            descent = Math.max(descent, metrics.getDescent()); // positive
            height = Math.max(height, metrics.getHeight());
        }
        if (fm != null) {
            fm.ascent = ascent;
            fm.descent = descent;
        }
        return height;
    }

    /**
     * Extend metrics.
     *
     * @see LayoutPiece#LayoutPiece(char[], int, int, boolean, FontPaint, boolean, boolean, LayoutPiece)
     */
    @SuppressWarnings("MagicConstant")
    public Font getFontMetrics(@Nonnull Font font, @Nonnull FontPaint paint, @Nonnull FontMetricsInt fm) {
        font = font.deriveFont(paint.getFontStyle(), paint.mFontSize);
        fm.extendBy(mGraphics.getFontMetrics(font));
        return font;
    }

    /*@SuppressWarnings("MagicConstant")
    public void measure(@Nonnull char[] text, int contextStart, int contextEnd, @Nonnull FontPaint paint, boolean isRtl,
                        @Nonnull BiConsumer<GraphemeMetrics, FontPaint> consumer) {
        final List<FontRun> runs = paint.mTypeface.itemize(text, contextStart, contextEnd);
        float advance = 0;
        final FontMetricsInt fm = new FontMetricsInt();
        for (FontRun run : runs) {
            final Font font = run.getFont().deriveFont(paint.mFontStyle, paint.mFontSize);
            final GlyphVector vector = layoutGlyphVector(font, text, run.getStart(), run.getEnd(), isRtl);
            final int num = vector.getNumGlyphs();
            advance += vector.getGlyphPosition(num).getX();
            fm.extendBy(mGraphics.getFontMetrics(font));
        }
        consumer.accept(new GraphemeMetrics(advance, fm), paint);
    }*/
}
