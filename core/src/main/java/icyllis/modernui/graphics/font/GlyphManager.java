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

import icyllis.arc3d.engine.Engine;
import icyllis.modernui.annotation.*;
import icyllis.modernui.graphics.Bitmap;
import icyllis.modernui.graphics.text.FontCollection;
import icyllis.modernui.text.TextUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.ToIntFunction;

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
     * <p>
     * Additional notes: two pixels because we may use SDF to stroke.
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
    public static volatile boolean sFractionalMetrics = false;

    /**
     * The global instance.
     */
    private static volatile GlyphManager sInstance;

    private GLFontAtlas mA8Atlas;

    /**
     * Font (with size and style) to int key.
     */
    private final Object2IntOpenHashMap<Font> mFontTable = new Object2IntOpenHashMap<>();

    private final ArrayList<Font> mReverseFontTable = new ArrayList<>();
    private final ToIntFunction<Font> mFontTableMapper = f -> {
        mReverseFontTable.add(f);
        return mFontTable.size() + 1;
    };

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

    private final CopyOnWriteArrayList<Runnable> mAtlasInvalidationCallbacks = new CopyOnWriteArrayList<>();

    private GlyphManager() {
        // init
        reload();
    }

    @NonNull
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
        if (mA8Atlas != null) {
            mA8Atlas.close();
        }
        mA8Atlas = null;
        mFontTable.clear();
        mFontTable.trim();
        mReverseFontTable.clear();
        mReverseFontTable.trimToSize();
        allocateImage(64, 64);
    }

    /**
     * Compute a glyph key used to retrieve GPU baked glyph, the key is valid
     * until next {@link #reload()}.
     *
     * @param glyphCode the font specific glyph code
     * @return a key
     */
    public long computeGlyphKey(@NonNull Font font, int glyphCode) {
        long fontKey = mFontTable.computeIfAbsent(font, mFontTableMapper);
        return (fontKey << 32) | glyphCode;
    }

    public Font getFontFromKey(long key) {
        return mReverseFontTable.get((int) (key >> 32) - 1);
    }

    public static int getGlyphCodeFromKey(long key) {
        return (int) key;
    }

    @Nullable
    public GLBakedGlyph lookupGlyph(long key) {
        if (mA8Atlas == null) {
            mA8Atlas = new GLFontAtlas(Engine.MASK_FORMAT_A8);
        }
        GLBakedGlyph glyph = mA8Atlas.getGlyph(key);
        if (glyph != null && glyph.texture == 0) {
            Font font = getFontFromKey(key);
            int glyphCode = getGlyphCodeFromKey(key);
            return cacheGlyph(font, glyphCode, mA8Atlas, glyph, key);
        }
        return glyph;
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
    public GLBakedGlyph lookupGlyph(@NonNull Font font, int glyphCode) {
        long key = computeGlyphKey(font, glyphCode);
        if (mA8Atlas == null) {
            mA8Atlas = new GLFontAtlas(Engine.MASK_FORMAT_A8);
        }
        GLBakedGlyph glyph = mA8Atlas.getGlyph(key);
        if (glyph != null && glyph.texture == 0) {
            return cacheGlyph(font, glyphCode, mA8Atlas, glyph, key);
        }
        return glyph;
    }

    @RenderThread
    public void debug() {
        String basePath = Bitmap.saveDialogGet(Bitmap.SaveFormat.PNG, null, "FontAtlas");
        if (basePath != null) {
            // XXX: remove extension name
            basePath = basePath.substring(0, basePath.length() - 4);
        }
        if (mA8Atlas != null) {
            if (basePath != null) {
                mA8Atlas.debug(basePath + ".png");
            } else {
                mA8Atlas.debug(null);
            }
        }
    }

    public void dumpInfo(PrintWriter pw) {
        int glyphCount = 0;
        long memorySize = 0;
        if (mA8Atlas != null) {
            glyphCount += mA8Atlas.getGlyphCount();
            memorySize += mA8Atlas.getMemorySize();
        }
        pw.print("GlyphManager: ");
        pw.print("Atlases=" + (mA8Atlas != null ? 1 : 0));
        pw.print(", Glyphs=" + glyphCount);
        pw.println(", GPUMemorySize=" + TextUtils.binaryCompact(memorySize) + " (" + memorySize + " bytes)");
    }

    @Nullable
    @RenderThread
    private GLBakedGlyph cacheGlyph(@NonNull Font font, int glyphCode,
                                    @NonNull GLFontAtlas atlas, @NonNull GLBakedGlyph glyph,
                                    long key) {
        // there's no need to layout glyph vector, we only draw the specific glyphCode
        // which is already laid-out in LayoutEngine
        GlyphVector vector = font.createGlyphVector(mGraphics.getFontRenderContext(), new int[]{glyphCode});

        Rectangle bounds = vector.getPixelBounds(null, 0, 0);

        if (bounds.width == 0 || bounds.height == 0) {
            atlas.setNoPixels(key);
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

        boolean invalidated = atlas.stitch(glyph, MemoryUtil.memAddress(mImageBuffer));
        if (invalidated) {
            mAtlasInvalidationCallbacks.forEach(Runnable::run);
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

    /**
     * Called when the atlas resized or fully reset, which means
     * texture ID changed or previous {@link GLBakedGlyph}s become invalid.
     */
    public void addAtlasInvalidationCallback(Runnable callback) {
        mAtlasInvalidationCallbacks.add(Objects.requireNonNull(callback));
    }

    public void removeAtlasInvalidationCallback(Runnable callback) {
        mAtlasInvalidationCallbacks.remove(Objects.requireNonNull(callback));
    }

    /*@SuppressWarnings("MagicConstant")
    public void measure(@NonNull char[] text, int contextStart, int contextEnd, @NonNull FontPaint paint, boolean isRtl,
                        @NonNull BiConsumer<GraphemeMetrics, FontPaint> consumer) {
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
