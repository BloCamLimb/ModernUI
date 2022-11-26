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

package icyllis.modernui.textmc;

import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import icyllis.modernui.ModernUI;
import icyllis.modernui.graphics.font.FontFamily;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

/**
 * Behaves like FontFamily, but directly provides a bitmap (which maybe colored) to replace
 * Unicode code points without text shaping. If such a font family wins the font itemization,
 * the layout engine will create a ReplacementRun, just like color emojis.
 * <p>
 * The bitmap is just a single texture atlas.
 *
 * @author BloCamLimb
 * @see net.minecraft.client.gui.font.providers.BitmapProvider
 * @since 3.6
 */
public class BitmapFont extends FontFamily {

    private final NativeImage mImage;
    private final Int2ObjectMap<Glyph> mGlyphs;

    private final int mSpriteWidth;
    private final int mSpriteHeight;

    private final int mAscent;
    private final float mScale;

    private BitmapFont(NativeImage image, Int2ObjectMap<Glyph> glyphs,
                       int spriteWidth, int spriteHeight,
                       int ascent, float scale) {
        super(null);
        mImage = image;
        mGlyphs = glyphs;
        mSpriteWidth = spriteWidth;
        mSpriteHeight = spriteHeight;
        mAscent = ascent;
        mScale = scale;
    }

    @Nonnull
    public static BitmapFont create(ResourceManager manager, JsonObject metadata) {
        int height = GsonHelper.getAsInt(metadata, "height", TextLayoutProcessor.DEFAULT_BASE_FONT_SIZE);
        int ascent = GsonHelper.getAsInt(metadata, "ascent");
        if (ascent > height) {
            throw new JsonParseException("Ascent " + ascent + " higher than height " + height);
        }
        JsonArray chars = GsonHelper.getAsJsonArray(metadata, "chars");
        int[][] map = new int[chars.size()][]; // indices to code points
        for (int i = 0; i < map.length; i++) {
            String row = GsonHelper.convertToString(chars.get(i), "chars[" + i + "]");
            int[] data = row.codePoints().toArray();
            if (i > 0) {
                int length = map[0].length;
                if (data.length != length) {
                    throw new JsonParseException("Elements of chars have to be the same length (found: " +
                            data.length + ", expected: " + length + "), pad with space or \\u0000");
                }
            }
            map[i] = data;
        }
        if (map.length == 0 || map[0].length == 0) {
            throw new JsonParseException("Expected to find data in chars, found none.");
        }
        int numRows = map.length;
        int numCols = map[0].length;
        ResourceLocation file = new ResourceLocation(GsonHelper.getAsString(metadata, "file"));
        ResourceLocation location = new ResourceLocation(file.getNamespace(), "textures/" + file.getPath());
        try (InputStream stream = manager.open(location)) {
            NativeImage image = NativeImage.read(NativeImage.Format.RGBA, stream);
            int spriteWidth = image.getWidth() / numCols;
            int spriteHeight = image.getHeight() / numRows;
            float scale = (float) height / spriteHeight;

            Int2ObjectMap<Glyph> glyphs = new Int2ObjectOpenHashMap<>();
            for (int row = 0; row < numRows; row++) {
                int[] data = map[row];
                for (int col = 0; col < numCols; col++) {
                    int ch = data[col];
                    if (ch == '\u0000') {
                        continue; // padding
                    }
                    int actualWidth = getActualGlyphWidth(image, spriteWidth, spriteHeight, col, row);
                    Glyph glyph = new Glyph(col * spriteWidth, row * spriteHeight,
                            Math.round(actualWidth * scale) + 1);
                    if (glyphs.put(ch, glyph) != null) {
                        ModernUI.LOGGER.warn("Codepoint '{}' declared multiple times in {}",
                                Integer.toHexString(ch), location);
                    }
                }
            }

            return new BitmapFont(image, glyphs, spriteWidth, spriteHeight, ascent, scale);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int getActualGlyphWidth(NativeImage image, int width, int height, int col, int row) {
        int i;
        for (i = width - 1; i >= 0; i--) {
            int x = col * width + i;
            for (int j = 0; j < height; j++) {
                int y = row * height + j;
                if (image.getLuminanceOrAlpha(x, y) == 0) {
                    continue;
                }
                return i + 1;
            }
        }
        return i + 1;
    }

    public NativeImage getImage() {
        return mImage;
    }

    public Glyph getGlyph(int ch) {
        return mGlyphs.get(ch);
    }

    public int getSpriteWidth() {
        return mSpriteWidth;
    }

    public int getSpriteHeight() {
        return mSpriteHeight;
    }

    public int getAscent() {
        return mAscent;
    }

    public float getScale() {
        return mScale;
    }

    @Override
    public boolean hasGlyph(int codePoint) {
        return mGlyphs.containsKey(codePoint);
    }

    @Override
    public String getFamilyName() {
        return "Bitmap"; // TODO use a MC font name
    }

    public record Glyph(int offsetX, int offsetY, int advance) {
    }
}
