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

import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.platform.RenderCore;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class GlyphManager {

    /**
     * Transparent (alpha zero) black background color for use with BufferedImage.clearRect().
     */
    private static final Color BG_COLOR = new Color(0, 0, 0, 0);

    /**
     * The global instance.
     */
    private static volatile GlyphManager sInstance;

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

    /**
     * All font atlases, with specified font family, size and style.
     */
    private Map<Font, GlyphAtlas> mAtlases;

    /**
     * The lock used with getGlyph()
     */
    private final Object mQueryLock = new Object();

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
        mAtlases = new HashMap<>();
        allocateImage(64, 64);
    }

    /**
     * Given a font, perform full text layout/shaping and create a new GlyphVector for a text.
     *
     * @param font  the derived Font used to layout a GlyphVector for the text
     * @param text  the text to layout
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

    /**
     * Given a derived font and a glyph code within that font, locate the glyph's pre-rendered image
     * in the glyph atlas and return its cache entry,. The entry stores the texture with the
     * pre-rendered glyph image, as well as the position and size of that image within the texture.
     *
     * @param font      the font (with size and style) to which this glyphCode belongs and which
     *                  was used to pre-render the glyph,
     * @param glyphCode the font specific glyph code (should be laid-out) to lookup in the atlas
     * @return the cached glyph info
     */
    @Nonnull
    public GlyphInfo getGlyph(@Nonnull Font font, int glyphCode) {
        GlyphAtlas atlas;
        synchronized (mQueryLock) {
            atlas = mAtlases.computeIfAbsent(font, f -> new GlyphAtlas());
        }
        GlyphInfo glyph = atlas.getGlyph(glyphCode);
        if (glyph.width == GlyphInfo.CREATED) {
            RenderCore.recordRenderCall(
                    () -> cacheGlyph(font, glyphCode, atlas, glyph));
            glyph.width = GlyphInfo.UPLOADING;
        }
        return glyph;
    }

    public void export() {
        mAtlases.values().forEach(GlyphAtlas::export);
    }

    @RenderThread
    private void cacheGlyph(@Nonnull Font font, int glyphCode, @Nonnull GlyphAtlas atlas, @Nonnull GlyphInfo glyph) {
        // there's no need to layout glyph vector, we only draw the specific glyphCode
        // which is already laid-out in LayoutEngine
        GlyphVector vector = font.createGlyphVector(mGraphics.getFontRenderContext(), new int[]{glyphCode});

        Rectangle bounds = vector.getGlyphPixelBounds(0, null, 0, 0);
        glyph.advance = vector.getGlyphMetrics(0).getAdvanceX();
        glyph.offsetX = bounds.x;
        glyph.offsetY = bounds.y;
        glyph.width = bounds.width;
        glyph.height = bounds.height;

        if (bounds.width > mImage.getWidth() || bounds.height > mImage.getHeight()) {
            allocateImage(mImage.getWidth() << 1, mImage.getHeight() << 1);
        }

        // give it an offset to draw at origin
        mGraphics.drawGlyphVector(vector, -bounds.x, -bounds.y);

        // copy raw pixel data from BufferedImage to imageData array with one integer per pixel in 0xAARRGGBB form
        mImage.getRGB(0, 0, bounds.width, bounds.height, mImageData, 0, bounds.width);

        final int size = bounds.width * bounds.height;
        for (int i = 0; i < size; i++) {
            // alpha channel for grayscale texture
            mImageBuffer.put((byte) (mImageData[i] >>> 24));
        }
        mImageBuffer.flip();

        atlas.stitch(glyph, MemoryUtil.memAddress(mImageBuffer));

        mGraphics.clearRect(0, 0, mImage.getWidth(), mImage.getHeight());
        mImageBuffer.clear();
    }

    private void allocateImage(int width, int height) {
        mImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        mGraphics = mImage.createGraphics();

        mImageData = new int[width * height];
        mImageBuffer = BufferUtils.createByteBuffer(width * height);

        // set background color for use with clearRect()
        mGraphics.setBackground(BG_COLOR);

        // drawImage() to this buffer will copy all source pixels instead of alpha blending them into the current image
        mGraphics.setComposite(AlphaComposite.Src);

        // this only for shape rendering, so we turn it off
        mGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // enable text antialias and highly precise rendering
        mGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        mGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }
}
