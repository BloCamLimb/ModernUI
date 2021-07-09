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
import icyllis.modernui.graphics.texture.Texture2D;
import icyllis.modernui.platform.Bitmap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.function.IntFunction;

import static icyllis.modernui.graphics.GLWrapper.GL_ALPHA;
import static icyllis.modernui.graphics.GLWrapper.GL_UNSIGNED_BYTE;

/**
 * Maintains a font texture atlas, which is specified with a font family, size and style.
 * The glyphs in the texture are tightly packed, dynamically generated with mipmaps. Each
 * glyph is represented as a TexturedGlyph.
 * <p>
 * The initial texture size is 256*256, and each enlargement double the height and width
 * alternately. The max texture size would be 16384*16384 and the image is 8-bit grayscale.
 * The OpenGL texture id may change due to enlarging the texture size.
 */
@ThreadSafe
public class GlyphAtlas {

    /**
     * The width in pixels of a transparent border between individual glyphs in the atlas.
     * This border keeps neighboring glyphs from "bleeding through" when mipmap used.
     */
    private static final int GLYPH_BORDER = 1;

    private static final int INITIAL_SIZE = 256;

    private static final int MIPMAP_LEVEL = 4;

    // OpenHashMap uses less memory than RBTree/AVLTree, but higher than ArrayMap
    private final Int2ObjectMap<GlyphInfo> mGlyphs =
            Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());

    // texture object is immutable, but texture ID (the int) can change by resizing
    private final Texture2D mTexture = new Texture2D();

    private final IntFunction<GlyphInfo> mFactory = i -> new GlyphInfo(mTexture);

    private int mPosX = GLYPH_BORDER;
    private int mPosY = GLYPH_BORDER;

    private int mLineHeight;

    private int mWidth;
    private int mHeight;

    public GlyphAtlas() {
    }

    @Nonnull
    public GlyphInfo getGlyph(int glyphCode) {
        return mGlyphs.computeIfAbsent(glyphCode, mFactory);
    }

    public void export() {
        try {
            Bitmap.download(Bitmap.Format.RGBA, mTexture)
                    .saveDialog(Bitmap.SaveFormat.PNG, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RenderThread
    public void stitch(GlyphInfo glyph, long data) {
        if (mWidth == 0) {
            enlarge();
        }
        if (mPosX + glyph.width + GLYPH_BORDER >= mWidth) {
            mPosX = GLYPH_BORDER;
            if (mWidth == mHeight && mWidth != INITIAL_SIZE) {
                mPosX += mWidth >> 1;
            }
            mPosY += mLineHeight + GLYPH_BORDER << 1;
            mLineHeight = 0;
        }
        if (mPosY + glyph.height + GLYPH_BORDER >= mHeight) {
            if (mWidth != mHeight) {
                mPosX = GLYPH_BORDER + mWidth;
                mPosY = GLYPH_BORDER;
            }
            enlarge();
        }

        mTexture.upload(0, mPosX, mPosY, glyph.width, glyph.height, glyph.width,
                0, 0, 1, GL_ALPHA, GL_UNSIGNED_BYTE, data);

        mPosX += glyph.width + GLYPH_BORDER << 1;
        mLineHeight = Math.max(mLineHeight, glyph.height);
    }

    private void enlarge() {
        if (mWidth == 0) {
            mWidth = mHeight = INITIAL_SIZE;
            mTexture.initCompat(GL_ALPHA, INITIAL_SIZE, INITIAL_SIZE, MIPMAP_LEVEL);
            return;
        } else if (mHeight != mWidth) {
            mWidth <<= 1;
        } else {
            mHeight <<= 1;
        }
        mTexture.resize(mWidth, mHeight, true);
    }
}
