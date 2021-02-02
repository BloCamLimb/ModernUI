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

package icyllis.modernui.font.glyph;

import com.mojang.blaze3d.platform.GlStateManager;
import icyllis.modernui.ModernUI;
import icyllis.modernui.font.pipeline.TextRenderNode;
import icyllis.modernui.font.pipeline.TextRenderType;
import icyllis.modernui.font.process.VanillaTextKey;
import icyllis.modernui.mcimpl.ModernUIMod;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Draw using glyphs of different sizes and fonts, and store them in auto generated OpenGL textures
 * <p>
 * RAM usage: &lt; 10MB
 * GPU memory usage: &lt; 22MB
 */
@SuppressWarnings("unused")
public class GlyphManager {

    public static final Marker MARKER = MarkerManager.getMarker("Font");

    /**
     * Config values.
     */
    public static String sPreferredFont;
    public static boolean sAntiAliasing;
    public static boolean sHighPrecision;
    public static boolean sEnableMipmap;
    public static int sMipmapLevel;
    /**
     * The resolution level of font, higher levels would better work with high resolution monitors.
     * Reference: 1 (Standard, 1.5K Fullscreen), 2 (High, 2K~3K Fullscreen), 3 (Ultra, 4K Fullscreen)
     * This should match your GUI scale. Scale -> Level: [1,2] -> 1; [3,4] -> 2; [5,) -> 3
     */
    public static int sResolutionLevel;

    /**
     * Determined in mod loading stage
     */
    private static boolean sOldJava;

    /**
     * The width in pixels of every texture used for caching pre-rendered glyph images. Used by GlyphCache when calculating
     * floating point 0.0-1.0 texture coordinates. Must be a power of two for mip-mapping to work.
     */
    @Deprecated
    private static final int TEXTURE_WIDTH = 256;
    /**
     * The height in pixels of every texture used for caching pre-rendered glyph images. Used by GlyphCache when calculating
     * floating point 0.0-1.0 texture coordinates. Must be a power of two for mip-mapping to work.
     */
    @Deprecated
    private static final int TEXTURE_HEIGHT = 256;

    public static final int TEXTURE_SIZE = 1024;

    /**
     * Initial width in pixels of the stringImage buffer used to extract individual glyph images.
     */
    @Deprecated
    private static final int STRING_WIDTH = 256;
    /**
     * Initial height in pixels of the stringImage buffer used to extract individual glyph images.
     */
    @Deprecated
    private static final int STRING_HEIGHT = 64;

    /**
     * For bilinear or trilinear mipmap textures, similar to {@link #GLYPH_SPACING}, but must be smaller than it
     */
    private static final int GLYPH_BORDER = 2;

    /**
     * The width in pixels of a transparent border between individual glyphs in the cache texture. This border keeps neighboring
     * glyphs from "bleeding through" when the scaled GUI resolution is not pixel aligned and sometimes results in off-by-one
     * sampling of the glyph cache textures.
     */
    private static final int GLYPH_SPACING = GLYPH_BORDER + 1;

    /**
     * For drawing, due to {@link #GLYPH_BORDER}, we need an offset for drawing glyphs
     *
     * @see TextRenderNode#drawText(com.mojang.blaze3d.vertex.BufferBuilder, String, float, float, int, int, int, int)
     */
    public static final float GLYPH_OFFSET = GLYPH_BORDER / 2.0f;

    /**
     * Transparent (alpha zero) black background color for use with BufferedImage.clearRect().
     */
    private static final Color BG_COLOR = new Color(0, 0, 0, 0);


    /**
     * Temporary image for rendering a string to and then extracting the glyph images from.
     */
    @Deprecated
    private BufferedImage tempStringImage;

    /**
     * The Graphics2D associated with stringImage and used for string drawing to extract the individual glyph shapes.
     */
    @Deprecated
    private Graphics2D tempStringGraphics;


    /**
     * All font glyphs are packed inside this image and are then loaded from here into an OpenGL texture.
     */
    private final BufferedImage glyphTextureImage = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);

    /**
     * The Graphics2D associated with glyphTextureImage and used for bit blit between stringImage.
     */
    private final Graphics2D glyphTextureGraphics = glyphTextureImage.createGraphics();


    /**
     * Intermediate data array for use with textureImage.getRgb().
     */
    private final int[] imageData = new int[((1 << 6) * (1 << 6)) << 1];

    /**
     * A direct buffer used with glTexSubImage2D(). Used for loading the pre-rendered glyph
     * images from the glyphCacheImage BufferedImage into OpenGL textures.
     */
    private final ByteBuffer uploadBuffer = BufferUtils.createByteBuffer(imageData.length);

    /**
     * A single integer direct buffer with native byte ordering used for returning values from glGenTextures().
     */
    private final IntBuffer textureGenBuffer = BufferUtils.createIntBuffer(1);

    /**
     * List of all available physical fonts on the system. Used by lookupFont() to find alternate fonts.
     */
    private final List<Font> allFonts;

    /**
     * A list of all fonts that have been returned so far by lookupFont(), and that will always be searched first for a usable font before
     * searching through allFonts[]. This list will only have plain variation of a font at a dummy point size, unlike fontCache which could
     * have multiple entries for the various styles (i.e. bold, italic, etc.) of a font.
     */
    private final List<Font> selectedFonts = new ObjectArrayList<>();


    /**
     * ID of current OpenGL cache texture being used by cacheGlyphs() to store pre-rendered glyph images.
     */
    private int textureName;

    /**
     * A cache of all fonts that have at least one glyph pre-rendered in a texture. Each font maps to an integer (monotonically
     * increasing) which forms the upper 32 bits of the key into the glyphCache map. This font cache can include different styles
     * of the same font family like bold or italic and different size.
     */
    private final Object2IntMap<Font> fontKeyMap = new Object2IntArrayMap<>(8);

    /**
     * A cache of pre-rendered glyphs mapping each glyph by its glyphcode to the position of its pre-rendered image within
     * the cache texture. The key is a 64 bit number such that the lower 32 bits are the glyphcode and the upper 32 are the
     * index of the font in the fontCache. This makes for a single globally unique number to identify any glyph from any font.
     */
    private final Long2ObjectMap<TexturedGlyph> glyphCache = new Long2ObjectArrayMap<>(4096);

    /**
     * Font ID {@link #fontKeyMap} to an array of length 10 represent 0-9 digits (in that order)
     * These glyph advance are equal for fast rendering. For example {@link VanillaTextKey#hashCode()} did.
     */
    private final Int2ObjectMap<TexturedGlyph[]> digitsMap = new Int2ObjectArrayMap<>(4);

    private final Int2ObjectMap<TexturedGlyph> emojiMap = new Int2ObjectArrayMap<>(32);

    /**
     * Emoji texture atlas
     */
    private int emojiTexture;


    /**
     * The X coordinate of the upper=left corner in glyphCacheImage where the next glyph image should be stored. Glyphs are
     * always added left-to-right on the current line until it fills up, at which point they continue filling the texture on
     * the next line.
     */
    private int currPosX = GLYPH_SPACING;

    /**
     * The Y coordinate of the upper-left corner in glyphCacheImage where the next glyph image should be stored. Glyphs are
     * stored left-to-right in horizontal lines, and top-to-bottom until the entire texture fills up. At that point, a new
     * texture is allocated to keep storing additional glyphs, and the original texture remains allocated for the lifetime of
     * the application.
     */
    private int currPosY = GLYPH_SPACING;

    /**
     * The height in pixels of the current line of glyphs getting written into the texture. This value determines by how much
     * cachePosY will get incremented when the current horizontal line in the texture fills up.
     */
    private int currLineHeight = 0;

    /**
     * A single instance of GlyphManager is allocated for internal use.
     */
    public GlyphManager() {
        checkJava();
        /* Set background color for use with clearRect() */
        glyphTextureGraphics.setBackground(BG_COLOR);

        /* The drawImage() to this buffer will copy all source pixels instead of alpha blending them into the current image */
        glyphTextureGraphics.setComposite(AlphaComposite.Src);

        allocateGlyphTexture();
        //allocateStringImage(STRING_WIDTH, STRING_HEIGHT);

        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        /* Use Java's logical font as the default initial font if user does not override it in some configuration file */
        environment.preferLocaleFonts();

        allFonts = Arrays.asList(environment.getAllFonts());

        loadPreferredFonts();

        setRenderingHints();
    }

    private void checkJava() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion == null) {
            ModernUI.LOGGER.fatal(ModernUI.MARKER, "Java version is missing");
        } else if (javaVersion.startsWith("1.8")) {
            try {
                int update = Integer.parseInt(javaVersion.split("_")[1].split("-")[0]);
                if (update < 201) {
                    sOldJava = true;
                    ModernUIMod.getMod().warnSetup("warning.modernui.old_java", "11.0.9", javaVersion);
                }
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                ModernUI.LOGGER.warn(ModernUI.MARKER, "Failed to check java version: {}", javaVersion, e);
            }
        }
    }

    /**
     * Reload fonts, clear all cached data
     */
    public void reload() {
        currPosX = GLYPH_SPACING;
        currPosY = GLYPH_SPACING;
        currLineHeight = 0;
        fontKeyMap.clear();
        glyphCache.clear();
        digitsMap.clear();
        emojiMap.clear();
        textureName = 0;
        emojiTexture = 0;
        TextRenderType.clearTextures();
        selectedFonts.clear();
        allocateGlyphTexture();
        loadPreferredFonts();
        setRenderingHints();
        ModernUI.LOGGER.debug(MARKER, "Font engine reloaded");
    }

    private void loadPreferredFonts() {
        if (!sPreferredFont.isEmpty()) {
            String typeface = sPreferredFont;
            if (typeface.endsWith(".ttf") || typeface.endsWith(".otf")) {
                if (typeface.contains(":/") || typeface.contains(":\\")) {
                    if (!sOldJava) {
                        try {
                            Font f = Font.createFont(Font.TRUETYPE_FONT, new File(
                                    typeface.replaceAll("\\\\", "/")));
                            selectedFonts.add(f);
                            ModernUI.LOGGER.debug(MARKER, "Preferred font {} was loaded", f.getName());
                        } catch (FontFormatException | IOException e) {
                            ModernUI.LOGGER.warn(MARKER, "Preferred font {} failed to load", typeface, e);
                        }
                    } else {
                        ModernUI.LOGGER.warn(MARKER, "Cannot load external font {} since Java is too old", typeface);
                    }
                } else if (typeface.contains(":")) {
                    if (!sOldJava) {
                        try (Resource resource = Minecraft.getInstance().getResourceManager()
                                .getResource(new ResourceLocation(typeface))) {
                            Font f = Font.createFont(Font.TRUETYPE_FONT, resource.getInputStream());
                            selectedFonts.add(f);
                            ModernUI.LOGGER.debug(MARKER, "Preferred font {} was loaded", f.getName());
                        } catch (FontFormatException | IOException e) {
                            ModernUI.LOGGER.warn(MARKER, "Preferred font {} failed to load", typeface, e);
                        }
                    } else {
                        ModernUI.LOGGER.warn(MARKER, "Cannot load resource pack font {} since Java is too old", typeface);
                    }
                } else {
                    ModernUI.LOGGER.warn(MARKER, "Preferred font {} is invalid", typeface);
                }
            } else {
                Optional<Font> font = allFonts.stream().filter(f -> f.getFamily(Locale.ROOT).equals(typeface)).findFirst();
                if (font.isPresent()) {
                    selectedFonts.add(new Font(typeface, Font.PLAIN, 12));
                    ModernUI.LOGGER.debug(MARKER, "Preferred font {} was loaded", typeface);
                } else {
                    ModernUI.LOGGER.warn(MARKER, "Preferred font {} cannot found or invalid", typeface);
                }
            }
        }

        if (!sOldJava) {
            try (InputStream stream = getClass().getResourceAsStream("/assets/modernui/font/biliw.otf")) {
                Font f = Font.createFont(Font.TRUETYPE_FONT, stream);
                selectedFonts.add(f);
            } catch (FontFormatException | IOException e) {
                ModernUI.LOGGER.warn(MARKER, "Built-in font failed to load", e);
            } catch (NullPointerException e) {
                ModernUI.LOGGER.warn(MARKER, "Built-in font was missing", e);
            }
        }
        // May be Arial, depends on the OS
        selectedFonts.add(new Font(Font.SANS_SERIF, Font.PLAIN, 72)); // size 1 > 72
    }

    /**
     * Given a single OpenType font, perform full text layout and create a new GlyphVector for a string.
     *
     * @param font        the Font used to layout a GlyphVector for the string
     * @param text        the string to layout
     * @param start       the offset into text at which to start the layout
     * @param limit       the (offset + length) at which to stop performing the layout
     * @param layoutFlags either {@link Font#LAYOUT_RIGHT_TO_LEFT} or {@link Font#LAYOUT_LEFT_TO_RIGHT}
     * @return the newly laid-out GlyphVector
     */
    public GlyphVector layoutGlyphVector(@Nonnull Font font, char[] text, int start, int limit, int layoutFlags) {
        return font.layoutGlyphVector(glyphTextureGraphics.getFontRenderContext(), text, start, limit, layoutFlags);
    }

    /**
     * Find the first font in the system able to render the given codePoint. The function always tries searching first
     * in the selected fonts followed by the allFonts.
     *
     * @param codePoint the codePoint to check against the font
     * @return the font to use in selection list (without fontStyle and fontSize),
     * @see #deriveFont(Font, int, int)
     */
    @Nonnull
    public Font lookupFont(int codePoint) {
        for (Font font : selectedFonts) {
            /* Only use the font if it can layout at least the first character of the requested string range */
            if (font.canDisplay(codePoint)) {
                return font;
            }
        }

        /* If still not found, try searching through all fonts installed on the system for the first that can layout this string */
        for (Font font : allFonts) {
            /* Only use the font if it can layout at least the first character of the requested string range */
            if (font.canDisplay(codePoint)) {
                /* If found, add this font to the selectedFonts list so it can be looked up faster next time */
                selectedFonts.add(font);
                ModernUI.LOGGER.debug(MARKER, "Extra font {} was loaded", font.getName());
                return font;
            }
        }

        /* If no supported fonts found, use the default one (first in selectedFonts) so it can draw its unknown character glyphs */
        return selectedFonts.get(0);
    }

    public TexturedGlyph lookupEmoji(int codePoint) {
        return emojiMap.computeIfAbsent(codePoint, l -> {
            if (emojiTexture == 0) {
                ResourceLocation resourceLocation = new ResourceLocation(ModernUI.ID, "textures/gui/emoji.png");
                AbstractTexture texture = new SimpleTexture(resourceLocation);
                Minecraft.getInstance().getTextureManager().register(resourceLocation, texture);
                emojiTexture = texture.getId();
            }
            return new TexturedGlyph(emojiTexture, 12, 0, -8, 12, 12, 0, 0, 0.046875f, 0.046875f);
        });
    }

    /**
     * Derive a font with given style and size
     *
     * @param font      font without fontStyle and fontSize
     * @param fontStyle font style
     * @param fontSize  font size
     * @return derived font with style and size
     */
    @Nonnull
    public Font deriveFont(@Nonnull Font font, int fontStyle, int fontSize) {
        fontSize *= sResolutionLevel;
        font = font.deriveFont(fontStyle, fontSize);
        /* Ensure this font is already in fontKeyMap so it can be referenced by lookupGlyph() later on */
        fontKeyMap.putIfAbsent(font, fontKeyMap.size());
        return font;
    }

    public float getResolutionFactor() {
        // based on a gui scale of 2
        return sResolutionLevel * 2.0f;
    }

    /**
     * Find the first font in the system able to render the given codePoint. The function always tries searching first
     * in the fontCache (based on the request style and size).
     * Failing that, it searches the selectedFonts list followed by the allFonts[] array.
     *
     * @param codePoint the codePoint to check against the font
     * @param fontStyle combination of the Font.PLAIN, Font.BOLD, and Font.ITALIC to request a particular font style
     * @param fontSize  the font size required for the text
     */
    @Nonnull
    @Deprecated
    private Font lookupFont(int codePoint, int fontStyle, int fontSize) {
        //int nextOffset;
        // Try using an already known base font;
        // the first font in selectedFonts list is the one set with highest priority
        for (Font font : selectedFonts) {
            /* Only use the font if it can layout at least the first character of the requested string range */
            if (font.canDisplay(codePoint)) {
                /* Return a font instance of the proper point size and style; selectedFonts has only 1pt sized plain style fonts */
                return font.deriveFont(fontStyle, fontSize);
            }
        }

        /* If still not found, try searching through all fonts installed on the system for the first that can layout this string */
        for (Font font : allFonts) {
            /* Only use the font if it can layout at least the first character of the requested string range */
            if (font.canDisplay(codePoint)) {
                /* If found, add this font to the selectedFonts list so it can be looked up faster next time */
                selectedFonts.add(font);
                ModernUI.LOGGER.debug(MARKER, "Extra font {} was loaded", font.getName());

                /* Return a font instance of the proper point size and style; allFonts has only 1pt sized plain style fonts */
                return font.deriveFont(fontStyle, fontSize);
            }
        }

        /* If no supported fonts found, use the default one (first in selectedFonts) so it can draw its unknown character glyphs */
        Font font = selectedFonts.get(0);

        /* Return a font instance of the proper point size and style; selectedFonts only 1pt sized plain style fonts */
        return font.deriveFont(fontStyle, fontSize);
    }

    /**
     * Given an OpenType font and a glyph code within that font, locate the glyph's pre-rendered image in the glyph cache and return its
     * cache entry,. The entry stores the texture ID with the pre-rendered glyph image, as well as the position and size of that image
     * within the texture. This function assumes that any glyph lookup requests passed to it have been already cached by an earlier call
     * to cacheGlyphs().
     *
     * @param font      the font to which this glyphCode belongs and which was used to pre-render the glyph image in cacheGlyphs(),
     *                  this font param also includes style and font size
     * @param glyphCode the font specific glyph code to lookup in the cache, for digits {@link #lookupDigits(Font)}
     * @return the cache textured glyph
     */
    @Nonnull
    public TexturedGlyph lookupGlyph(Font font, int glyphCode) {
        // the key should be cached in layout step
        long fontKey = (long) fontKeyMap.getInt(font) << 32;
        return glyphCache.computeIfAbsent(fontKey | glyphCode,
                l -> cacheGlyph(font, glyphCode));
    }

    /**
     * Helper method, a combination of {@link #lookupFont(int, int, int)} and {@link #lookupGlyph(Font, int)}
     *
     * @param codePoint the codePoint to check against the font
     * @param fontStyle combination of the Font.PLAIN, Font.BOLD, and Font.ITALIC to request a particular font style
     * @param fontSize  the font size required for the text
     * @return the cache textured glyph
     */
    @Nonnull
    @Deprecated
    private TexturedGlyph lookupGlyph(int codePoint, int fontStyle, int fontSize) {
        return lookupGlyph(lookupFont(codePoint, fontStyle, fontSize), codePoint);
    }

    /**
     * Create a textured glyph with given character and font, draw and upload image data to OpenGL texture.
     *
     * @param font      the font used to draw the character, includes font size and style (italic or bold)
     * @param glyphCode the font specific glyph code to lookup in the cache
     * @return created textured glyph
     */
    @Nonnull
    private TexturedGlyph cacheGlyph(@Nonnull Font font, int glyphCode) {

        /* There's no need to layout glyph vector, we only draw the specific glyphCode
         * which is already laid-out in TextProcessor */
        GlyphVector vector = font.createGlyphVector(glyphTextureGraphics.getFontRenderContext(), new int[]{glyphCode});

        Rectangle renderBounds = vector.getGlyphPixelBounds(0, glyphTextureGraphics.getFontRenderContext(), 0, 0);
        int renderWidth = (int) renderBounds.getWidth();
        int renderHeight = (int) renderBounds.getHeight();

        if (currPosX + renderWidth + GLYPH_SPACING >= TEXTURE_SIZE) {
            currPosX = GLYPH_SPACING;
            currPosY += currLineHeight + GLYPH_SPACING * 2;
            currLineHeight = 0;
        }
        if (currPosY + renderHeight + GLYPH_SPACING >= TEXTURE_SIZE) {
            currPosX = GLYPH_SPACING;
            currPosY = GLYPH_SPACING;
            allocateGlyphTexture();
        }

        int baselineX = (int) renderBounds.getX();
        int baselineY = (int) renderBounds.getY();
        float advance = vector.getGlyphMetrics(0).getAdvanceX();

        glyphTextureGraphics.setFont(font);

        int x = currPosX - GLYPH_BORDER;
        int y = currPosY - GLYPH_BORDER;
        int width = renderWidth + GLYPH_BORDER * 2;
        int height = renderHeight + GLYPH_BORDER * 2;

        glyphTextureGraphics.drawGlyphVector(vector, currPosX - baselineX, currPosY - baselineY);

        uploadTexture(x, y, width, height);

        currLineHeight = Math.max(currLineHeight, renderHeight);
        currPosX += renderWidth + GLYPH_SPACING * 2;
        final float f = getResolutionFactor();

        return new TexturedGlyph(textureName, advance / f, baselineX / f, baselineY / f,
                width / f, height / f,
                (float) x / TEXTURE_SIZE, (float) y / TEXTURE_SIZE,
                (float) (x + width) / TEXTURE_SIZE, (float) (y + height) / TEXTURE_SIZE);
    }

    /**
     * Lookup digit glyphs with given font
     *
     * @param font derived font including style and font size
     * @return array of all digit glyphs 0-9 (in that order)
     */
    public TexturedGlyph[] lookupDigits(Font font) {
        // the key should be cached in layout step
        int fontKey = fontKeyMap.getInt(font);
        return digitsMap.computeIfAbsent(fontKey,
                l -> cacheDigits(font));
    }

    /**
     * Helper method, a combination of {@link #lookupFont(int, int, int)} and {@link #lookupDigits(Font)}
     *
     * @param fontStyle combination of the Font.PLAIN, Font.BOLD, and Font.ITALIC to request a particular font style
     * @param fontSize  the font size required for the text
     * @return an array of digits 0-9
     */
    @Deprecated
    private TexturedGlyph[] lookupDigits(int fontStyle, int fontSize) {
        return lookupDigits(lookupFont(48, fontStyle, fontSize));
    }

    /**
     * Basically same as {@link #cacheGlyph(Font, int)}, but all digits width are equal to '0' width
     * and drawn center aligned based on '0' width
     *
     * @param font derived font
     * @return 0-9 digits (in that order)
     */
    @Nonnull
    private TexturedGlyph[] cacheDigits(@Nonnull Font font) {
        TexturedGlyph[] digits = new TexturedGlyph[10];

        char[] chars = new char[1];

        glyphTextureGraphics.setFont(font);

        float standardAdvance = 0.0f;
        int standardRenderWidth = 0;
        final float f = getResolutionFactor();

        // cache '0-9'
        for (int i = 0; i < 10; i++) {
            chars[0] = (char) (48 + i);
            GlyphVector vector = font.createGlyphVector(glyphTextureGraphics.getFontRenderContext(), chars);

            Rectangle renderBounds = vector.getGlyphPixelBounds(0, glyphTextureGraphics.getFontRenderContext(), 0, 0);
            int renderWidth = (int) renderBounds.getWidth();
            int renderHeight = (int) renderBounds.getHeight();

            if (i == 0) {
                if (currPosX + renderWidth + GLYPH_SPACING >= TEXTURE_SIZE) {
                    currPosX = GLYPH_SPACING;
                    currPosY += currLineHeight + GLYPH_SPACING * 2;
                    currLineHeight = 0;
                }
            } else {
                if (currPosX + standardRenderWidth + GLYPH_SPACING >= TEXTURE_SIZE) {
                    currPosX = GLYPH_SPACING;
                    currPosY += currLineHeight + GLYPH_SPACING * 2;
                    currLineHeight = 0;
                }
            }
            if (currPosY + renderHeight + GLYPH_SPACING >= TEXTURE_SIZE) {
                currPosX = GLYPH_SPACING;
                currPosY = GLYPH_SPACING;
                allocateGlyphTexture();
            }

            int baselineX = (int) renderBounds.getX();
            int baselineY = (int) renderBounds.getY();
            if (i == 0) {
                standardAdvance = vector.getGlyphMetrics(0).getAdvanceX();
                standardRenderWidth = renderWidth;
            }

            int x = currPosX - GLYPH_BORDER;
            int y = currPosY - GLYPH_BORDER;
            int width;
            if (i == 0) {
                width = renderWidth + GLYPH_BORDER * 2;
            } else {
                width = standardRenderWidth + GLYPH_BORDER * 2;
            }
            int height = renderHeight + GLYPH_BORDER * 2;

            // ASCII digits are not allowed to be laid-out into other code points
            if (i == 0) {
                glyphTextureGraphics.drawString(String.valueOf(chars), currPosX - baselineX, currPosY - baselineY);
            } else {
                // align to center
                int offset = Math.round((standardRenderWidth - renderWidth) / 2.0f);
                glyphTextureGraphics.drawString(String.valueOf(chars), currPosX + offset - baselineX, currPosY - baselineY);
            }

            uploadTexture(x, y, width, height);

            currLineHeight = Math.max(currLineHeight, renderHeight);
            currPosX += standardRenderWidth + GLYPH_SPACING * 2;

            digits[i] = new TexturedGlyph(textureName,
                    standardAdvance / f, baselineX / f, baselineY / f,
                    width / f, height / f,
                    (float) x / TEXTURE_SIZE, (float) y / TEXTURE_SIZE,
                    (float) (x + width) / TEXTURE_SIZE, (float) (y + height) / TEXTURE_SIZE);
        }

        return digits;
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
     * @deprecated {@link #cacheGlyph(Font, int)}
     */
    @Deprecated
    private void cacheGlyphs(Font font, char[] text, int start, int limit, int layoutFlags) {
        /* Create new GlyphVector so glyphs can be moved around (kerning workaround; see below) without affecting caller */
        GlyphVector vector = layoutGlyphVector(font, text, start, limit, layoutFlags);

        /* Pixel aligned bounding box for the entire vector; only set if the vector has to be drawn to cache a glyph image */
        Rectangle vectorBounds = null;

        /* This forms the upper 32 bits of the fontCache key to make every font/glyph code point unique */
        long fontKey = (long) fontKeyMap.get(font) << 32;

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
                vectorBounds = vector.getPixelBounds(glyphTextureGraphics.getFontRenderContext(), 0, 0);

                /* Enlarge the stringImage if it is too small to store the entire rendered string */
                if (vectorBounds.width > tempStringImage.getWidth() || vectorBounds.height > tempStringImage.getHeight()) {
                    int width = Math.max(vectorBounds.width, tempStringImage.getWidth());
                    int height = Math.max(vectorBounds.height, tempStringImage.getHeight());
                    allocateStringImage(width, height);
                }

                /* Erase the upper-left corner where the string will get drawn*/
                tempStringGraphics.clearRect(0, 0, vectorBounds.width, vectorBounds.height);

                /* Draw string with opaque white color and baseline adjustment so the upper-left corner of the image is at (0,0) */
                tempStringGraphics.drawGlyphVector(vector, -vectorBounds.x, -vectorBounds.y);
            }

            /*
             * Get the glyph's pixel-aligned bounding box. The JavaDoc claims that the "The outline returned
             * by this method is positioned around the origin of each individual glyph." However, the actual
             * bounds are all relative to the start of the entire GlyphVector, which is actually more useful
             * for extracting the glyph's image from the rendered string.
             */
            Rectangle rect = vector.getGlyphPixelBounds(index, null, -vectorBounds.x, -vectorBounds.y);

            /* If the current line in cache image is full, then advance to the next line */
            if (currPosX + rect.width + GLYPH_SPACING > TEXTURE_WIDTH) {
                currPosX = GLYPH_SPACING;
                currPosY += currLineHeight + GLYPH_SPACING;
                currLineHeight = 0;
            }

            /*
             * If the entire image is full, update the current OpenGL texture with everything changed so far in the image
             * (i.e. the dirty rectangle), allocate a new cache texture, and then continue storing glyph images to the
             * upper-left corner of the new texture.
             */
            if (currPosY + rect.height + GLYPH_SPACING > TEXTURE_HEIGHT) {
                updateTexture(dirty);

                /* Note that allocateAndSetupTexture() will leave the GL texture already bound */
                allocateGlyphTexture();
                allocateStringImage(STRING_WIDTH, STRING_HEIGHT);

                currPosY = currPosX = GLYPH_SPACING;
                currLineHeight = 0;

                /* re-draw glyph layout to ensure rest chars be rendered correctly on the new texture */
                cacheGlyphs(font, text, start + index, limit, layoutFlags);
                return;
            }

            /* The tallest glyph on this line determines the total vertical advance in the texture */
            if (rect.height > currLineHeight) {
                currLineHeight = rect.height;
            }

            /*
             * Blit the individual glyph from it's position in the temporary string buffer to its (cachePosX,
             * cachePosY) position in the texture. NOTE: We don't have to erase the area in the texture image
             * first because the composite method in the Graphics object is always set to AlphaComposite.Src.
             */
            glyphTextureGraphics.drawImage(tempStringImage,
                    currPosX, currPosY, currPosX + rect.width, currPosY + rect.height,
                    rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, null);

            /*
             * Store this glyph's position in texture and its origin offset. Note that "rect" will not be modified after
             * this point, and getGlyphPixelBounds() always returns a new Rectangle.
             */
            rect.setLocation(currPosX, currPosY);

            /*
             * Create new cache entry to record both the texture used by the glyph and its position within that texture.
             * Texture coordinates are normalized to 0.0-1.0 by dividing with TEXTURE_WIDTH and TEXTURE_HEIGHT.
             */
            /*TexturedGlyph texturedGlyph = new TexturedGlyph();
            texturedGlyph.textureName = textureName;
            texturedGlyph.renderType = TextRenderType.getOrCacheType(textureName);
            texturedGlyph.advance = rect.width;
            texturedGlyph.height = rect.height;
            texturedGlyph.u1 = (float) rect.x / TEXTURE_WIDTH;
            texturedGlyph.v1 = (float) rect.y / TEXTURE_HEIGHT;
            texturedGlyph.u2 = (float) (rect.x + rect.width) / TEXTURE_WIDTH;
            texturedGlyph.v2 = (float) (rect.y + rect.height) / TEXTURE_HEIGHT;*/

            /*
             * The lower 32 bits of the glyphCache key are the glyph codepoint. The upper 64 bits are the font number
             * stored in the fontCache. This creates a unique numerical id for every font/glyph combination.
             */
            //glyphCache.put(fontKey | glyphCode, texturedGlyph);

            /*
             * Track the overall modified region in the texture by performing a union of this glyph's texture position
             * with the update region created so far. Reusing "rect" here makes it easier to extend the dirty rectangle
             * region than using the add(x, y) method to extend by a single point. Also note that creating the first
             * dirty rectangle here avoids having to deal with the special rules for empty/non-existent rectangles.
             */
            if (dirty == null) {
                dirty = new Rectangle(currPosX, currPosY, rect.width, rect.height);
            } else {
                dirty.add(rect);
            }

            /* Advance cachePosX so the next glyph can be stored immediately to the right of this one */
            currPosX += rect.width + GLYPH_SPACING;
        }

        /* Update OpenGL texture if any part of the glyphCacheImage has changed */
        updateTexture(dirty);
    }

    /**
     * Update a portion of the current glyph cache texture using the contents of the glyphCacheImage with glTexSubImage2D().
     *
     * @param dirty The rectangular region in glyphCacheImage that has changed and needs to be copied into the texture
     *              bleeding when interpolation is active or add a small "fudge factor" to the UV coordinates like already n FontRenderer
     */
    @Deprecated
    private void updateTexture(@Nullable Rectangle dirty) {
        /* Only update OpenGL texture if changes were made to the texture */
        if (dirty != null) {
            /* Load imageBuffer with pixel data ready for transfer to OpenGL texture */
            updateImageBuffer(dirty.x, dirty.y, dirty.width, dirty.height);

            GlStateManager._bindTexture(textureName);

            /* Due to changes in 1.14+, so this ensure pixels are correctly stored from CPU to GPU */
            GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, dirty.width);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4); // 4 is RGBA

            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, dirty.x, dirty.y, dirty.width, dirty.height,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, uploadBuffer);

            /* Auto generate mipmap texture */
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        }
    }

    /**
     * Upload texture data of current texture from CPU to GPU with given dirty area.
     *
     * @param x      left pos
     * @param y      top pos
     * @param width  width
     * @param height height
     */
    private void uploadTexture(int x, int y, int width, int height) {
        /* Load imageBuffer with pixel data ready for transfer to OpenGL texture */
        updateImageBuffer(x, y, width, height);

        GlStateManager._bindTexture(textureName);

        /* Due to changes in 1.14+, so this ensures pixels are correctly stored from CPU to GPU */
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, width); // not full texture
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1); // 1 is alpha, 4 bytes

        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, width, height,
                GL11.GL_ALPHA, GL11.GL_UNSIGNED_BYTE, uploadBuffer);

        // WRONG CODE HERE
        /* int mipmapLevel = sEnableMipmap ? sMipmapLevel : 0;

        for (int level = 0; level <= mipmapLevel; level++) {
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, level, x, y, width, height,
                    GL11.GL_ALPHA, GL11.GL_UNSIGNED_BYTE, uploadBuffer);
        }*/

        /* Auto generate mipmap texture */
        if (sEnableMipmap) {
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        }
    }

    /**
     * Copy pixel data from a region in glyphCacheImage into imageBuffer and prepare it for use with glText(Sub)Image2D(). This
     * function takes care of converting the ARGB format used with BufferedImage into the LA format used by OpenGL.
     *
     * @param x      the horizontal coordinate of the region's upper-left corner
     * @param y      the vertical coordinate of the region's upper-left corner
     * @param width  the width of the pixel region that will be copied into the buffer
     * @param height the height of the pixel region that will be copied into the buffer
     */
    private void updateImageBuffer(int x, int y, int width, int height) {
        /* Copy raw pixel data from BufferedImage to imageData array with one integer per pixel in 0xAARRGGBB form */
        glyphTextureImage.getRGB(x, y, width, height, imageData, 0, width);

        /* Copy int array to direct buffer */
        uploadBuffer.clear();

        /* Swizzle each color integer from Java's ARGB format to OpenGL's grayscale */
        final int size = width * height;
        for (int i = 0; i < size; i++) {
            //int color = imageData[i];
            //uploadData[i] = (color << 8) | (color >>> 24);

            /* alpha channel for grayscale texture */
            uploadBuffer.put((byte) (imageData[i] >>> 24));
        }

        uploadBuffer.flip();
    }

    /**
     * Set rendering hints on stringGraphics object. Enable anti-aliasing and is therefore called both from
     * allocateStringImage() when expanding the size of the BufferedImage and from constructor
     */
    private void setRenderingHints() {
        // this only for shape rendering, so we turn it off
        glyphTextureGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        if (sAntiAliasing) {
            glyphTextureGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            glyphTextureGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
        if (sHighPrecision) {
            glyphTextureGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        } else {
            glyphTextureGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        }
    }

    /**
     * Allocate a new OpenGL texture for caching pre-rendered glyph images. The new texture is initialized to fully transparent
     * white so the individual glyphs images within can have a transparent border between them. The new texture remains bound
     * after returning from the function.
     * Do similar to {@link com.mojang.blaze3d.platform.TextureUtil#prepareImage(int, int, int)}
     */
    private void allocateGlyphTexture() {
        /* Initialize the background to all black but fully transparent. */
        glyphTextureGraphics.clearRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);

        /* Allocate new OpenGL texture */
        textureGenBuffer.position(0);
        GL11.glGenTextures(textureGenBuffer);
        textureName = textureGenBuffer.get(0);

        /* Load imageBuffer with pixel data ready for transfer to OpenGL texture */
        //updateImageBuffer(0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        /*
         * Initialize texture with the now cleared BufferedImage. Using a texture with GL_ALPHA8 internal format may result in
         * faster rendering since the GPU has to only fetch 1 byte per texel instead of 4 with a regular RGBA texture.
         */
        GlStateManager._bindTexture(textureName);

        /* Due to changes in 1.14+, so this ensure pixels are correctly stored from CPU to GPU */
        /*GlStateManager.pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0); // 0 is unspecific
        GlStateManager.pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager.pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager.pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4); // 4 is RGBA, has 4 channels*/

        int mipmapLevel = sEnableMipmap ? sMipmapLevel : 0;

        if (mipmapLevel >= 0) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, mipmapLevel);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, mipmapLevel);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0.0f);
        }

        for (int level = 0; level <= mipmapLevel; level++) {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, level, GL11.GL_ALPHA4, TEXTURE_SIZE >> level,
                    TEXTURE_SIZE >> level, 0, GL11.GL_ALPHA, GL11.GL_UNSIGNED_BYTE, (IntBuffer) null);
        }

        /* We set MinMag params here, just call once for a texture */
        if (sAntiAliasing) {
            if (sEnableMipmap) {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            } else {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            }
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        } else {
            if (sEnableMipmap) {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR);
            } else {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            }
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }

        /* Auto generate mipmap */
        if (sEnableMipmap) {
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        }

        /* Clear mipmap data, current grayscale value is zero, this is a little inefficient */
        // 64 here is the max size of a glyph, the max res level is 3, the max config font size is 20
        final int size = (TEXTURE_SIZE >> 6) * (TEXTURE_SIZE >> 6);
        for (int i = 0; i < size; i++) {
            int x = (i & ((TEXTURE_SIZE >> 6) - 1)) << 6;
            int y = (i / (TEXTURE_SIZE >> 6)) << 6;
            uploadTexture(x, y, 1 << 6, 1 << 6);
        }
    }

    /**
     * Allocate and initialize a new BufferedImage and Graphics2D context for rendering strings into. May need to be called
     * at runtime to re-allocate a bigger BufferedImage if cacheGlyphs() is called with a very long string.
     */
    @Deprecated
    private void allocateStringImage(int width, int height) {
        //tempStringImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        //tempStringGraphics = tempStringImage.createGraphics();
        //setRenderingHints();

        /* Set background color for use with clearRect() */
        //tempStringGraphics.setBackground(BG_COLOR);

        //
        // Full white (1.0, 1.0, 1.0, 1.0) can be modulated by vertex color to produce a full gamut of text colors, although with
        // a GL_ALPHA8 texture, only the alpha component of the color will actually get loaded into the texture.
        //
        //tempStringGraphics.setPaint(Color.WHITE);
    }
}
