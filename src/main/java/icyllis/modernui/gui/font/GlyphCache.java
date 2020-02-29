/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package icyllis.modernui.gui.font;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.renderer.GLAllocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

class GlyphCache {

    /**
     * The width in pixels of every texture used for caching pre-rendered glyph images. Used by GlyphCache when calculating
     * floating point 0.0-1.0 texture coordinates. Must be a power of two for mip-mapping to work.
     */
    private static final int TEXTURE_WIDTH = 256;

    /**
     * The height in pixels of every texture used for caching pre-rendered glyph images. Used by GlyphCache when calculating
     * floating point 0.0-1.0 texture coordinates. Must be a power of two for mip-mapping to work.
     */
    private static final int TEXTURE_HEIGHT = 256;

    /**
     * Initial width in pixels of the stringImage buffer used to extract individual glyph images.
     */
    private static final int STRING_WIDTH = 256;

    /**
     * Initial height in pixels of the stringImage buffer used to extract individual glyph images.
     */
    private static final int STRING_HEIGHT = 64;

    /**
     * The width in pixels of a transparent border between individual glyphs in the cache texture. This border keeps neighboring
     * glyphs from "bleeding through" when the scaled GUI resolution is not pixel aligned and sometimes results in off-by-one
     * sampling of the glyph cache textures.
     */
    private static final int GLYPH_BORDER = 1;

    /**
     * Transparent (alpha zero) white background color for use with BufferedImage.clearRect().
     */
    private static Color BACK_COLOR = new Color(255, 255, 255, 0);

    /**
     * The point size at which every OpenType font is rendered.
     */
    private float fontSize = 18;

    /**
     * If true, then enble anti-aliasing when rendering the font glyph
     */
    private boolean antiAliasEnabled = false;


    /**
     * Temporary image for rendering a string to and then extracting the glyph images from.
     */
    private BufferedImage stringImage;

    /**
     * The Graphics2D associated with stringImage and used for string drawing to extract the individual glyph shapes.
     */
    private Graphics2D stringGraphics;


    /**
     * All font glyphs are packed inside this image and are then loaded from here into an OpenGL texture.
     */
    private BufferedImage glyphCacheImage = new BufferedImage(TEXTURE_WIDTH, TEXTURE_HEIGHT, BufferedImage.TYPE_INT_ARGB);

    /**
     * The Graphics2D associated with glyphCacheImage and used for bit blitting between stringImage.
     */
    private Graphics2D glyphCacheGraphics = glyphCacheImage.createGraphics();

    /**
     * Needed for all text layout operations that create GlyphVectors (maps point size to pixel size).
     */
    private FontRenderContext fontRenderContext = glyphCacheGraphics.getFontRenderContext();


    /**
     * Intermediate data array for use with textureImage.getRgb().
     */
    private int[] imageData = new int[TEXTURE_WIDTH * TEXTURE_HEIGHT];

    /**
     * A big-endian direct int buffer used with glTexSubImage2D() and glTexImage2D(). Used for loading the pre-rendered glyph
     * images from the glyphCacheImage BufferedImage into OpenGL textures. This buffer uses big-endian byte ordering to ensure
     * that the integers holding packed RGBA colors are stored into memory in a predictable order.
     */
    private IntBuffer imageBuffer = ByteBuffer.allocateDirect(4 * TEXTURE_WIDTH * TEXTURE_HEIGHT).order(ByteOrder.BIG_ENDIAN).asIntBuffer();

    /**
     * A single integer direct buffer with native byte ordering used for returning values from glGenTextures().
     */
    private IntBuffer singleIntBuffer = GLAllocation.createDirectByteBuffer(4).asIntBuffer();

    /**
     * List of all available physical fonts on the system. Used by lookupFont() to find alternate fonts.
     */
    private List<Font> allFonts = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts());

    /**
     * A list of all fonts that have been returned so far by lookupFont(), and that will always be searched first for a usable font before
     * searching through allFonts[]. This list will only have plain variation of a font at a dummy point size, unlike fontCache which could
     * have multiple entries for the various styles (i.e. bold, italic, etc.) of a font. This list starts with Java's "SansSerif" logical
     * font.
     */
    private List<Font> usedFonts = Lists.newArrayList();


    /**
     * ID of current OpenGL cache texture being used by cacheGlyphs() to store pre-rendered glyph images.
     */
    private int textureName;

    /**
     * A cache of all fonts that have at least one glyph pre-rendered in a texture. Each font maps to an integer (monotonically
     * increasing) which forms the upper 32 bits of the key into the glyphCache map. This font cache can include different styles
     * of the same font family like bold or italic.
     */
    private LinkedHashMap<Font, Integer> fontCache = Maps.newLinkedHashMap();

    /**
     * A cache of pre-rendered glyphs mapping each glyph by its glyphcode to the position of its pre-rendered image within
     * the cache texture. The key is a 64 bit number such that the lower 32 bits are the glyphcode and the upper 32 are the
     * index of the font in the fontCache. This makes for a single globally unique number to identify any glyph from any font.
     */
    private LinkedHashMap<Long, Entry> glyphCache = Maps.newLinkedHashMap();


    /**
     * The X coordinate of the upper=left corner in glyphCacheImage where the next glyph image should be stored. Glyphs are
     * always added left-to-right on the curren tline until it fills up, at which point they continue filling the texture on
     * the next line.
     */
    private int cachePosX = GLYPH_BORDER;

    /**
     * The Y coordinate of the upper-left corner in glyphCacheImage where the next glyph image should be stored. Glyphs are
     * stored left-to-right in horizontal lines, and top-to-bottom until the entire texture fills up. At that point, a new
     * texture is allocated to keep storing additional glyphs, and the original texture remains allocated for the lifetime of
     * the application.
     */
    private int cachePosY = GLYPH_BORDER;

    /**
     * The height in pixels of the current line of glyphs getting written into the texture. This value determines by how much
     * cachePosY will get incremented when the current horizontal line in the texture fills up.
     */
    private int cacheLineHeight = 0;

    /**
     * This class holds information for a glyph about its pre-rendered image in an OpenGL texture. The texture coordinates in
     * this class are normalized in the standard 0.0 - 1.0 OpenGL range.
     */
    static class Entry {
        /**
         * The OpenGL texture ID that contains this glyph image.
         */
        int textureName;

        /**
         * The width in pixels of the glyph image.
         */
        int width;

        /**
         * The height in pixels of the glyph image.
         */
        int height;

        /**
         * The horizontal texture coordinate of the upper-left corner.
         */
        float u1;

        /**
         * The vertical texture coordinate of the upper-left corner.
         */
        float v1;

        /**
         * The horizontal texture coordinate of the lower-right corner.
         */
        float u2;

        /**
         * The vertical texture coordinate of the lower-right corner.
         */
        float v2;
    }

    /**
     * A single instance of GlyphCache is allocated for internal use by the StringCache class.
     */
    GlyphCache() {
        /* Set background color for use with clearRect() */
        glyphCacheGraphics.setBackground(BACK_COLOR);

        /* The drawImage() to this buffer will copy all source pixels instead of alpha blending them into the current image */
        glyphCacheGraphics.setComposite(AlphaComposite.Src);

        allocateGlyphCacheTexture();
        allocateStringImage(STRING_WIDTH, STRING_HEIGHT);

        /* Use Java's logical font as the default initial font if user does not override it in some configuration file */
        java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().preferLocaleFonts();
        //usedFonts.add(new Font(Font.SANS_SERIF, Font.PLAIN, 72)); //size 1 > 72
    }

    /**
     * Change the default font used to pre-render glyph images. If this method is called at runtime, the existing glyph images will remain cached
     * in their respective textures and will remain accessible to StringCache. This method is normally called by StringCache.setDefaultFont() since
     * the StringCache must also invalidate itself so it can re-layout and re-cache the glyphs for all strings using the new font.
     *
     * @param size the new point size
     */
    void setDefaultFont(float size, boolean antiAlias) {
        usedFonts.clear();
        //usedFonts.add(new Font(name, Font.PLAIN, 72)); //size 1 > 72
        try {
            Font f = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/assets/modernui/font/unix.otf"));
            usedFonts.add(f);
            //ModernUI.LOGGER.info(ModernUI.MARKER, "{} has been loaded", f.getName());
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }

        fontSize = size;
        antiAliasEnabled = antiAlias;
        setRenderingHints();
    }

    public void clear() {
        glyphCache.clear();
    }

    /**
     * Given a single OpenType font, perform full text layout and create a new GlyphVector for a string.
     *
     * @param font        the Font used to layout a GlyphVector for the string
     * @param text        the string to layout
     * @param start       the offset into text at which to start the layout
     * @param limit       the (offset + length) at which to stop performing the layout
     * @param layoutFlags either Font.LAYOUT_RIGHT_TO_LEFT or Font.LAYOUT_LEFT_TO_RIGHT
     * @return the newly created GlyphVector
     */
    GlyphVector layoutGlyphVector(Font font, char[] text, int start, int limit, int layoutFlags) {
        /* Ensure this font is already in fontCache so it can be referenced by cacheGlyphs() later on */
        if (!fontCache.containsKey(font)) {
            fontCache.put(font, fontCache.size());
        }
        return font.layoutGlyphVector(fontRenderContext, text, start, limit, layoutFlags);
    }

    /**
     * Find the first font in the system able to render at least one character from a given string. The function always tries searching first
     * in the fontCache (based on the request style). Failing that, it searches the usedFonts list followed by the allFonts[] array.
     *
     * @param text  the string to check against the font
     * @param start the offset into text at which to start checking characters for being supported by a font
     * @param limit the (offset + length) at which to stop checking characters
     * @param style combination of the Font.PLAIN, Font.BOLD, and Font.ITALIC to request a particular font style
     * @return an OpenType font capable of displaying at least the first character at the start position in text
     */
    Font lookupFont(char[] text, int start, int limit, int style) {
        /* Try using an already known base font; the first font in usedFonts list is the one set with setDefaultFont() */
        Iterator<Font> iterator = usedFonts.iterator();
        while (iterator.hasNext()) {
            /* Only use the font if it can layout at least the first character of the requested string range */
            Font font = iterator.next();
            if (font.canDisplayUpTo(text, start, limit) != start) {
                /* Return a font instance of the proper point size and style; usedFonts has only 1pt sized plain style fonts */
                return font.deriveFont(style, fontSize);
            }
        }

        /* If still not found, try searching through all fonts installed on the system for the first that can layout this string */
        iterator = allFonts.iterator();
        while (iterator.hasNext()) {
            /* Only use the font if it can layout at least the first character of the requested string range */
            Font font = iterator.next();
            if (font.canDisplayUpTo(text, start, limit) != start) {
                /* If found, add this font to the usedFonts list so it can be looked up faster next time */
                ModernUI.LOGGER.debug("{} has been loaded", font.getName());
                usedFonts.add(font);

                /* Return a font instance of the proper point size and style; allFonts has only 1pt sized plain style fonts */
                return font.deriveFont(style, fontSize);
            }
        }

        /* If no supported fonts found, use the default one (first in usedFonts) so it can draw its unknown character glyphs */
        Font font = usedFonts.get(0);

        /* Return a font instance of the proper point size and style; usedFonts only 1pt sized plain style fonts */
        return font.deriveFont(style, fontSize);
    }

    /**
     * Given an OpenType font and a glyph code within that font, locate the glyph's pre-rendered image in the glyph cache and return its
     * cache entry,. The entry stores the texture ID with the pre-rendered glyph image, as well as the position and size of that image
     * within the texture. This function assumes that any glyph lookup requests passed to it have been already cached by an earlier call
     * to cacheGlyphs().
     *
     * @param font      the font to which this glyphCode belongs and which was used to pre-render the glyph image in cacheGlyphs()
     * @param glyphCode the font specific flyph code to lookup in the cache
     * @return the cache entry for this font/glyphCode pair
     */
    Entry lookupGlyph(Font font, int glyphCode) {
        long fontKey = (long) fontCache.get(font) << 32;
        return glyphCache.get(fontKey | glyphCode);
    }

    /**
     * Given an OpenType font and a string, make sure that every glyph used by that string is pre-rendered into an OpenGL texture and cached
     * in the glyphCache map for later retrieval by lookupGlyph()
     *
     * @param font        the font used to create a GlyphVector for the string and to actually draw the individual glyphs
     * @param text        the string from which to cache glyph images
     * @param start       the offset into text at which to start caching glyphs
     * @param limit       the (offset + length) at which to stop caching glyphs
     * @param layoutFlags either Font.LAYOUT_RIGHT_TO_LEFT or Font.LAYOUT_LEFT_TO_RIGHT; needed for weak bidirectional characters like
     *                    parenthesis which are mapped to two different glyph codes depending on the surrounding text direction
     */
    //todo May need a blank border of pixels around everything for mip-map/tri-linear filtering with Optifine
    void cacheGlyphs(Font font, char[] text, int start, int limit, int layoutFlags) {
        /* Create new GlyphVector so glyphs can be moved around (kerning workaround; see below) without affecting caller */
        GlyphVector vector = layoutGlyphVector(font, text, start, limit, layoutFlags);

        /* Pixel aligned bounding box for the entire vector; only set if the vector has to be drawn to cache a glyph image */
        Rectangle vectorBounds = null;

        /* This forms the upper 32 bits of the fontCache key to make every font/glyph code point unique */
        long fontKey = (long) fontCache.get(font) << 32;

        int numGlyphs = vector.getNumGlyphs(); /* Length of the GlyphVector */
        Rectangle dirty = null;                /* Total area within texture that needs to be updated with glTexSubImage2D() */
        boolean vectorRendered = false;        /* True if entire GlyphVector was rendered into stringImage */

        for (int index = 0; index < numGlyphs; index++) {
            /* If this glyph code is already in glyphCache, then there is no reason to pre-render it again */
            int glyphCode = vector.getGlyphCode(index);
            if (glyphCache.containsKey(fontKey | glyphCode)) {
                continue;
            }

            /*
             * The only way to get glyph shapes with font hinting is to draw the entire glyph vector into a
             * temporary BufferedImage, and then bit blit the individual glyphs based on their bounding boxes
             * returned by the glyph vector. Although it is possible to call font.createGlyphVector() with an
             * array of glyphcodes (and therefore render only a few glyphs at a time), this produces corrupted
             * Davengari glyphs under Windows 7. The vectorRendered flag will draw the string at most one time.
             */
            if (!vectorRendered) {
                vectorRendered = true;

                /*
                 * Kerning can make it impossible to cleanly separate adjacent glyphs. To work around this,
                 * each glyph is manually advanced by 2 pixels to the right of its neighbor before rendering
                 * the entire string. The getGlyphPixelBounds() later on will return the new adjusted bounds
                 * for the glyph.
                 */
                for (int i = 0; i < numGlyphs; i++) {
                    Point2D pos = vector.getGlyphPosition(i);
                    pos.setLocation(pos.getX() + 2 * i, pos.getY());
                    vector.setGlyphPosition(i, pos);
                }

                /*
                 * Compute the exact area that the rendered string will take up in the image buffer. Note that
                 * the string will actually be drawn at a positive (x,y) offset from (0,0) to leave enough room
                 * for the ascent above the baseline and to correct for a few glyphs that appear to have negative
                 * horizontal bearing (e.g. U+0423 Cyrillic uppercase letter U on Windows 7).
                 */
                vectorBounds = vector.getPixelBounds(fontRenderContext, 0, 0);

                /* Enlage the stringImage if it is too small to store the entire rendered string */
                if (vectorBounds.width > stringImage.getWidth() || vectorBounds.height > stringImage.getHeight()) {
                    int width = Math.max(vectorBounds.width, stringImage.getWidth());
                    int height = Math.max(vectorBounds.height, stringImage.getHeight());
                    allocateStringImage(width, height);
                }

                /* Erase the upper-left corner where the string will get drawn*/
                stringGraphics.clearRect(0, 0, vectorBounds.width, vectorBounds.height);

                /* Draw string with opaque white color and baseline adjustment so the upper-left corner of the image is at (0,0) */
                stringGraphics.drawGlyphVector(vector, -vectorBounds.x, -vectorBounds.y);
            }

            /*
             * Get the glyph's pixel-aligned bounding box. The JavaDoc claims that the "The outline returned
             * by this method is positioned around the origin of each individual glyph." However, the actual
             * bounds are all relative to the start of the entire GlyphVector, which is actually more useful
             * for extracting the glyph's image from the rendered string.
             */
            Rectangle rect = vector.getGlyphPixelBounds(index, null, -vectorBounds.x, -vectorBounds.y);

            /* If the current line in cache image is full, then advance to the next line */
            if (cachePosX + rect.width + GLYPH_BORDER > TEXTURE_WIDTH) {
                cachePosX = GLYPH_BORDER;
                cachePosY += cacheLineHeight + GLYPH_BORDER;
                cacheLineHeight = 0;
            }

            /*
             * If the entire image is full, update the current OpenGL texture with everything changed so far in the image
             * (i.e. the dirty rectangle), allocate a new cache texture, and then continue storing glyph images to the
             * upper-left corner of the new texture.
             */
            if (cachePosY + rect.height + GLYPH_BORDER > TEXTURE_HEIGHT) {
                updateTexture(dirty);
                dirty = null;

                /* Note that allocateAndSetupTexture() will leave the GL texture already bound */
                allocateGlyphCacheTexture();
                cachePosY = cachePosX = GLYPH_BORDER;
                cacheLineHeight = 0;
            }

            /* The tallest glyph on this line determines the total vertical advance in the texture */
            if (rect.height > cacheLineHeight) {
                cacheLineHeight = rect.height;
            }

            /*
             * Blit the individual glyph from it's position in the temporary string buffer to its (cachePosX,
             * cachePosY) position in the texture. NOTE: We don't have to erase the area in the texture image
             * first because the composite method in the Graphics object is always set to AlphaComposite.Src.
             */
            glyphCacheGraphics.drawImage(stringImage,
                    cachePosX, cachePosY, cachePosX + rect.width, cachePosY + rect.height,
                    rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, null);

            /*
             * Store this glyph's position in texture and its origin offset. Note that "rect" will not be modified after
             * this point, and getGlyphPixelBounds() always returns a new Rectangle.
             */
            rect.setLocation(cachePosX, cachePosY);

            /*
             * Create new cache entry to record both the texture used by the glyph and its position within that texture.
             * Texture coordinates are normalized to 0.0-1.0 by dividing with TEXTURE_WIDTH and TEXTURE_HEIGHT.
             */
            Entry entry = new Entry();
            entry.textureName = textureName;
            entry.width = rect.width;
            entry.height = rect.height;
            entry.u1 = ((float) rect.x) / TEXTURE_WIDTH;
            entry.v1 = ((float) rect.y) / TEXTURE_HEIGHT;
            entry.u2 = ((float) (rect.x + rect.width)) / TEXTURE_WIDTH;
            entry.v2 = ((float) (rect.y + rect.height)) / TEXTURE_HEIGHT;

            /*
             * The lower 32 bits of the glyphCache key are the glyph codepoint. The upper 64 bits are the font number
             * stored in the fontCache. This creates a unique numerical id for every font/glyph combination.
             */
            glyphCache.put(fontKey | glyphCode, entry);

            /*
             * Track the overall modified region in the texture by performing a union of this glyph's texture position
             * with the update region created so far. Reusing "rect" here makes it easier to extend the dirty rectangle
             * region than using the add(x, y) method to extend by a single point. Also note that creating the first
             * dirty rectangle here avoids having to deal with the special rules for empty/non-existent rectangles.
             */
            if (dirty == null) {
                dirty = new Rectangle(cachePosX, cachePosY, rect.width, rect.height);
            } else {
                dirty.add(rect);
            }

            /* Advance cachePosX so the next glyph can be stored immediately to the right of this one */
            cachePosX += rect.width + GLYPH_BORDER;
        }

        /* Update OpenGL texture if any part of the glyphCacheImage has changed */
        updateTexture(dirty);
    }

    /**
     * Update a portion of the current glyph cache texture using the contents of the glyphCacheImage with glTexSubImage2D().
     *
     * @param dirty The rectangular region in glyphCacheImage that has changed and needs to be copied into the texture
     *              bleedover when interpolation is active or add a small "fudge factor" to the UV coordinates like already n FontRenderer
     */
    //todo Add mip-mapping support here
    //todo Test with bilinear texture interpolation and possibly add a 1 pixel transparent border around each glyph to avoid
    private void updateTexture(Rectangle dirty) {
        /* Only update OpenGL texture if changes were made to the texture */
        if (dirty != null) {
            /* Load imageBuffer with pixel data ready for transfer to OpenGL texture */
            updateImageBuffer(dirty.x, dirty.y, dirty.width, dirty.height);

            GlStateManager.bindTexture(textureName);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, dirty.x, dirty.y, dirty.width, dirty.height,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, imageBuffer);
        }
    }

    /**
     * Allocte and initialize a new BufferedImage and Graphics2D context for rendering strings into. May need to be called
     * at runtime to re-allocate a bigger BufferedImage if cacheGlyphs() is called with a very long string.
     */
    private void allocateStringImage(int width, int height) {
        stringImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        stringGraphics = stringImage.createGraphics();
        setRenderingHints();

        /* Set background color for use with clearRect() */
        stringGraphics.setBackground(BACK_COLOR);

        /*
         * Full white (1.0, 1.0, 1.0, 1.0) can be modulated by vertex color to produce a full gamut of text colors, although with
         * a GL_ALPHA8 texture, only the alpha component of the color will actually get loaded into the texture.
         */
        stringGraphics.setPaint(Color.WHITE);
    }

    /**
     * Set rendering hints on stringGraphics object. Uses current antiAliasEnabled settings and is therefore called both from
     * allocateStringImage() when expanding the size of the BufferedImage and from setDefaultFont() when changing current
     * configuration.
     */
    private void setRenderingHints() {
        stringGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                antiAliasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        stringGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                antiAliasEnabled ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        stringGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    }

    /**
     * Allocate a new OpenGL texture for caching pre-rendered glyph images. The new texture is initialized to fully transparent
     * white so the individual glyphs images within can have a transparent border between them. The new texture remains bound
     * after returning from the function.
     */
    //todo use GL_ALPHA4 if anti-alias is turned off for even smaller textures
    private void allocateGlyphCacheTexture() {
        /* Initialize the background to all white but fully transparent. */
        glyphCacheGraphics.clearRect(0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        /* Allocate new OpenGL texure */
        singleIntBuffer.clear();
        GL11.glGenTextures(singleIntBuffer);
        textureName = singleIntBuffer.get(0);

        /* Load imageBuffer with pixel data ready for transfer to OpenGL texture */
        updateImageBuffer(0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        /*
         * Initialize texture with the now cleared BufferedImage. Using a texture with GL_ALPHA8 internal format may result in
         * faster rendering since the GPU has to only fetch 1 byte per texel instead of 4 with a regular RGBA texture.
         */
        GlStateManager.bindTexture(textureName);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_ALPHA8, TEXTURE_WIDTH, TEXTURE_HEIGHT, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, imageBuffer);

        /* Explicitely disable mipmap support becuase updateTexture() will only update the base level 0 */
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    /**
     * Copy pixel data from a region in glyphCacheImage into imageBuffer and prepare it for use with glText(Sub)Image2D(). This
     * function takes care of converting the ARGB format used with BufferedImage into the RGBA format used by OpenGL.
     *
     * @param x      the horizontal coordinate of the region's upper-left corner
     * @param y      the vertical coordinate of the region's upper-left corner
     * @param width  the width of the pixel region that will be copied into the buffer
     * @param height the height of the pixel region that will be copied into the buffer
     */
    private void updateImageBuffer(int x, int y, int width, int height) {
        /* Copy raw pixel data from BufferedImage to imageData array with one integer per pixel in 0xAARRGGBB form */
        glyphCacheImage.getRGB(x, y, width, height, imageData, 0, width);

        /* Swizzle each color integer from Java's ARGB format to OpenGL's RGBA */
        for (int i = 0; i < width * height; i++) {
            int color = imageData[i];
            imageData[i] = (color << 8) | (color >>> 24);
        }

        /* Copy int array to direct buffer; big-endian order ensures a 0xRR, 0xGG, 0xBB, 0xAA byte layout */
        imageBuffer.clear();
        imageBuffer.put(imageData);
        imageBuffer.flip();
    }

    static class Glyph implements Comparable<Glyph> {

        /** The index into the original string (i.e. with color codes) for the character that generated this glyph. */
        int stringIndex;

        /** Texture ID and position/size of the glyph's pre-rendered image within the cache texture. */
        Entry texture;

        /** Glyph's horizontal position (in pixels) relative to the entire string's baseline */
        int x;

        /** Glyph's vertical position (in pixels) relative to the entire string's baseline */
        int y;

        /** Glyph's horizontal advance (in pixels) used for strikethrough and underline effects */
        float advance;

        /**
         * Allows arrays of Glyph objects to be sorted. Performs numeric comparison on stringIndex.
         *
         * @param o the other Glyph object being compared with this one
         * @return either -1, 0, or 1 if this < other, this == other, or this > other
         */
        @Override
        public int compareTo(Glyph o) {
            return Integer.compare(stringIndex, o.stringIndex);
        }
    }
}
