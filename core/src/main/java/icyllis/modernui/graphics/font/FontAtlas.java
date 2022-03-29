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

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.core.Core;
import icyllis.modernui.core.NativeImage;
import icyllis.modernui.opengl.GLTexture;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.IntFunction;

import static icyllis.modernui.opengl.GLCore.*;

/**
 * Maintains a font texture atlas, which is specified with a font family, size and style.
 * The glyphs in the texture are tightly packed, dynamically generated with mipmaps. Each
 * glyph is represented as a {@link TexturedGlyph}.
 * <p>
 * The initial texture size is 256*256, and each enlargement double the height and width
 * alternately. The max texture size would be 16384*16384 and the image is 8-bit grayscale.
 * The OpenGL texture id may change due to enlarging the texture size.
 *
 * @see GlyphManager
 * @see TexturedGlyph
 */
@RenderThread
public class FontAtlas {

    /**
     * The width in pixels of a transparent border between individual glyphs in the atlas.
     * This border keeps neighboring glyphs from "bleeding through" when mipmap used.
     */
    private static final int GLYPH_BORDER = 1;
    private static final int INITIAL_SIZE = 256;
    /**
     * Max mipmap level.
     */
    public static final int MIPMAP_LEVEL = 4;

    /**
     * Linear sampling with mipmaps;
     */
    public static volatile boolean sLinearSampling = true;

    // cached factory
    private static final IntFunction<TexturedGlyph> sFactory = i -> new TexturedGlyph();

    // texture object is immutable, but texture ID (the int) can change by resizing
    public final GLTexture mTexture = new GLTexture(GL_TEXTURE_2D);

    // OpenHashMap uses less memory than RBTree/AVLTree, but higher than ArrayMap
    private final Int2ObjectMap<TexturedGlyph> mGlyphs = new Int2ObjectOpenHashMap<>();

    // position for next glyph sprite
    private int mPosX = GLYPH_BORDER;
    private int mPosY = GLYPH_BORDER;

    // max height of current line
    private int mLineHeight;

    // current texture size
    private int mWidth;
    private int mHeight;

    // create from any thread
    public FontAtlas() {
    }

    @Nullable
    public TexturedGlyph getGlyph(int glyphCode) {
        return mGlyphs.computeIfAbsent(glyphCode, sFactory);
    }

    // needed when the glyph has nothing to render
    public void setEmpty(int glyphCode) {
        mGlyphs.put(glyphCode, null);
    }

    public void debug() {
        for (var glyph : mGlyphs.int2ObjectEntrySet()) {
            ModernUI.LOGGER.info(GlyphManager.MARKER, "GlyphCode {}: {}", glyph.getIntKey(), glyph.getValue());
        }
        if (Core.isOnRenderThread()) {
            try {
                NativeImage.download(NativeImage.Format.RGBA, mTexture, false)
                        .saveDialog(NativeImage.SaveFormat.PNG);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stitch(@Nonnull TexturedGlyph glyph, long data) {
        if (mWidth == 0) {
            resize();
        }
        if (mPosX + glyph.width + GLYPH_BORDER >= mWidth) {
            mPosX = GLYPH_BORDER;
            // we are on the right half
            if (mWidth == mHeight && mWidth != INITIAL_SIZE) {
                mPosX += mWidth >> 1;
            }
            mPosY += mLineHeight + GLYPH_BORDER * 2;
            mLineHeight = 0;
        }
        if (mPosY + glyph.height + GLYPH_BORDER >= mHeight) {
            // move to the right half
            if (mWidth != mHeight) {
                mPosX = GLYPH_BORDER + mWidth;
                mPosY = GLYPH_BORDER;
            }
            resize();
        }

        mTexture.upload(0, mPosX, mPosY, glyph.width, glyph.height, glyph.width,
                0, 0, 1, GL_RED, GL_UNSIGNED_BYTE, data);
        mTexture.generateMipmap();

        glyph.u1 = (float) mPosX / mWidth;
        glyph.v1 = (float) mPosY / mHeight;
        glyph.u2 = (float) (mPosX + glyph.width) / mWidth;
        glyph.v2 = (float) (mPosY + glyph.height) / mHeight;

        mPosX += glyph.width + GLYPH_BORDER * 2;
        mLineHeight = Math.max(mLineHeight, glyph.height);
    }

    private void resize() {
        // never initialized
        if (mWidth == 0) {
            mWidth = mHeight = INITIAL_SIZE;
            mTexture.allocate2DM(GL_R8, INITIAL_SIZE, INITIAL_SIZE, MIPMAP_LEVEL);
            // we have border that not upload data, so generate mipmap may leave undefined data
            mTexture.clear(0);
        } else {
            final boolean vertical;
            if (mHeight != mWidth) {
                mWidth <<= 1;
                vertical = false;
            } else {
                mHeight <<= 1;
                vertical = true;
            }

            mTexture.resize(mWidth, mHeight, true);

            if (vertical) {
                mTexture.clear(0, 0, mHeight >> 1, mWidth, mHeight >> 1);
                for (TexturedGlyph glyph : mGlyphs.values()) {
                    if (glyph == null) {
                        continue;
                    }
                    glyph.v1 *= 0.5;
                    glyph.v2 *= 0.5;
                    // texture id changed
                    glyph.texture = mTexture.get();
                }
            } else {
                mTexture.clear(0, mWidth >> 1, 0, mWidth >> 1, mHeight);
                for (TexturedGlyph glyph : mGlyphs.values()) {
                    if (glyph == null) {
                        continue;
                    }
                    glyph.u1 *= 0.5;
                    glyph.u2 *= 0.5;
                    // texture id changed
                    glyph.texture = mTexture.get();
                }
            }

            // we later generate mipmap
        }
        mTexture.setFilter(sLinearSampling ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST, GL_NEAREST);
        mTexture.swizzleRGBA(GL_ONE, GL_ONE, GL_ONE, GL_RED);
    }
}
