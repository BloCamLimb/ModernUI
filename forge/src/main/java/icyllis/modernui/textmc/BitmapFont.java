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
import icyllis.modernui.core.Core;
import icyllis.modernui.forge.UIManager;
import icyllis.modernui.graphics.font.*;
import icyllis.modernui.graphics.opengl.GLTexture;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static icyllis.modernui.graphics.opengl.GLCore.*;

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

    private final ResourceLocation mLocation;

    private NativeImage mImage;
    private final Int2ObjectMap<Glyph> mGlyphs = new Int2ObjectOpenHashMap<>();

    private final GLTexture mTexture = new GLTexture(GL_TEXTURE_2D);

    private final int mAscent;  // positive
    private final int mDescent; // positive

    private final int mSpriteWidth;
    private final int mSpriteHeight;
    private final float mScaleFactor;

    private BitmapFont(ResourceLocation location, NativeImage image,
                       int[][] map, int rows, int cols,
                       int height, int ascent) {
        super(null);
        mLocation = location;
        mImage = image;
        mAscent = ascent;
        mDescent = height - ascent;
        mSpriteWidth = image.getWidth() / cols;
        mSpriteHeight = image.getHeight() / rows;
        mScaleFactor = (float) height / mSpriteHeight;

        for (int i = 0; i < rows; i++) {
            int[] data = map[i];
            for (int j = 0; j < cols; j++) {
                int ch = data[j];
                if (ch == '\u0000') {
                    continue; // padding
                }
                int actualWidth = getActualGlyphWidth(image, mSpriteWidth, mSpriteHeight, j, i);
                Glyph glyph = new Glyph(Math.round(actualWidth * mScaleFactor) + 1);
                glyph.x = 0;
                glyph.y = -mAscent * TextLayoutEngine.BITMAP_SCALE;
                glyph.width = Math.round(mSpriteWidth * mScaleFactor * TextLayoutEngine.BITMAP_SCALE);
                glyph.height = Math.round(mSpriteHeight * mScaleFactor * TextLayoutEngine.BITMAP_SCALE);
                glyph.u1 = (float) (j * mSpriteWidth) / image.getWidth();
                glyph.v1 = (float) (i * mSpriteHeight) / image.getHeight();
                glyph.u2 = (float) (j * mSpriteWidth + mSpriteWidth) / image.getWidth();
                glyph.v2 = (float) (i * mSpriteHeight + mSpriteHeight) / image.getHeight();
                if (mGlyphs.put(ch, glyph) != null) {
                    ModernUI.LOGGER.warn("Codepoint '{}' declared multiple times in {}",
                            Integer.toHexString(ch), mLocation);
                }
            }
        }
    }

    @Nonnull
    public static BitmapFont create(JsonObject metadata, ResourceManager manager) {
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
        int rows = map.length;
        int cols = map[0].length;
        var file = new ResourceLocation(GsonHelper.getAsString(metadata, "file"));
        var location = new ResourceLocation(file.getNamespace(), "textures/" + file.getPath());
        try (InputStream stream = manager.open(location)) {
            // Minecraft doesn't use texture views, read swizzles may not work, so we always use RGBA (colored)
            NativeImage image = NativeImage.read(NativeImage.Format.RGBA, stream);
            return new BitmapFont(location, image, map, rows, cols, height, ascent);
        } catch (Exception e) {
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

    private void bakeAtlas() {
        mTexture.allocate2DCompat(GL_RGBA8, mImage.getWidth(), mImage.getHeight(), 0);
        try {
            long pixels = UIManager.IMAGE_PIXELS.getLong(mImage);
            mTexture.uploadCompat(0, 0, 0,
                    mImage.getWidth(), mImage.getHeight(),
                    0, 0, 0, 1,
                    GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            mTexture.setFilterCompat(GL_NEAREST, GL_NEAREST);
            for (Glyph glyph : mGlyphs.values()) {
                glyph.texture = mTexture.get();
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            mImage.close();
            mImage = null;
        }
    }

    public void dumpAtlas(String path) {
        if (path != null && mImage == null && Core.isOnRenderThread()) {
            ModernUI.LOGGER.info(GlyphManager.MARKER, "Glyphs: {}", mGlyphs.size());
            try (var image = icyllis.modernui.core.NativeImage.download(
                    icyllis.modernui.core.NativeImage.Format.RGBA,
                    mTexture, false)) {
                image.saveToPath(Path.of(path), icyllis.modernui.core.NativeImage.SaveFormat.PNG, 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void ensureClose() {
        if (mImage != null) {
            mImage.close();
            mImage = null;
        }
        // no worry about GLTexture
    }

    @Nullable
    public Glyph getGlyph(int ch) {
        if (mImage != null) {
            bakeAtlas();
        }
        return mGlyphs.get(ch);
    }

    public int getAscent() {
        return mAscent;
    }

    public int getDescent() {
        return mDescent;
    }

    public int getSpriteWidth() {
        return mSpriteWidth;
    }

    public int getSpriteHeight() {
        return mSpriteHeight;
    }

    public float getScaleFactor() {
        return mScaleFactor;
    }

    @Override
    public boolean hasGlyph(int codePoint) {
        return mGlyphs.containsKey(codePoint);
    }

    @Override
    public String getFamilyName() {
        // the bitmap name
        return mLocation.toString();
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + mLocation.hashCode();
        result = 31 * result + mAscent;
        result = 31 * result + mDescent;
        result = 31 * result + mSpriteWidth;
        result = 31 * result + mSpriteHeight;
        result = 31 * result + Float.hashCode(mScaleFactor);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BitmapFont that = (BitmapFont) o;
        if (mAscent != that.mAscent) return false;
        if (mDescent != that.mDescent) return false;
        if (mSpriteWidth != that.mSpriteWidth) return false;
        if (mSpriteHeight != that.mSpriteHeight) return false;
        if (mScaleFactor != that.mScaleFactor) return false;
        return mLocation.equals(that.mLocation);
    }

    public static class Glyph extends GLBakedGlyph {

        public final float advance;

        public Glyph(int advance) {
            this.advance = advance;
        }
    }
}
