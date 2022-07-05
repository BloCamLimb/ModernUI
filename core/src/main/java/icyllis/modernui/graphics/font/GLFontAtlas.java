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
import icyllis.modernui.graphics.opengl.GLTexture;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

import static icyllis.modernui.graphics.opengl.GLCore.*;

/**
 * Maintains a font texture atlas, which is specified with a font family, size and style.
 * The glyphs in the texture are tightly packed, dynamically generated with mipmaps. Each
 * glyph is represented as a {@link GLBakedGlyph}.
 * <p>
 * The initial texture size is 256*256, and each resize double the height and width
 * alternately. For example, 256*256 -> 256*512 -> 512*512 -> 512*1024 -> 1024*1024.
 * The max texture size would be 16384*16384 and the image is 8-bit grayscale.
 * The OpenGL texture will change due to increasing the texture size.
 *
 * @see GlyphManager
 * @see GLBakedGlyph
 */
@RenderThread
public class GLFontAtlas implements AutoCloseable {

    public static final int INITIAL_SIZE = 256;
    /**
     * Max mipmap level.
     */
    public static final int MIPMAP_LEVEL = 4;

    /**
     * Config values.
     * Linear sampling with mipmaps;
     */
    public static volatile boolean sLinearSampling = true;

    /**
     * A framebuffer used to copy texture to texture for compatibility
     */
    private static int sCopyFramebuffer;

    // OpenHashMap uses less memory than RBTree/AVLTree, but higher than ArrayMap
    private final Int2ObjectMap<GLBakedGlyph> mGlyphs = new Int2ObjectOpenHashMap<>();

    // texture can change by resizing
    private GLTexture mTexture = new GLTexture(GL_TEXTURE_2D);

    // position for next glyph sprite
    private int mPosX = GlyphManager.GLYPH_BORDER;
    private int mPosY = GlyphManager.GLYPH_BORDER;

    // max height of current line
    private int mLineHeight;

    // current texture size
    private int mWidth;
    private int mHeight;

    // create from any thread
    public GLFontAtlas() {
    }

    @Nullable
    public GLBakedGlyph getGlyph(int glyphCode) {
        // cached factory
        return mGlyphs.computeIfAbsent(glyphCode, i -> new GLBakedGlyph());
    }

    // needed when the glyph has nothing to render
    public void setEmpty(int glyphCode) {
        mGlyphs.put(glyphCode, null);
    }

    public void stitch(@Nonnull GLBakedGlyph glyph, long pixels) {
        glyph.texture = mTexture.get();
        if (mWidth == 0) {
            resize();
        }
        if (mPosX + glyph.width + GlyphManager.GLYPH_BORDER >= mWidth) {
            mPosX = GlyphManager.GLYPH_BORDER;
            // we are on the right half
            if (mWidth == mHeight && mWidth != INITIAL_SIZE) {
                mPosX += mWidth >> 1;
            }
            mPosY += mLineHeight + GlyphManager.GLYPH_BORDER * 2;
            mLineHeight = 0;
        }
        if (mPosY + glyph.height + GlyphManager.GLYPH_BORDER >= mHeight) {
            // move to the right half
            if (mWidth != mHeight) {
                mPosX = GlyphManager.GLYPH_BORDER + mWidth;
                mPosY = GlyphManager.GLYPH_BORDER;
            }
            resize();
        }

        // include border
        mTexture.uploadCompat(0, mPosX - GlyphManager.GLYPH_BORDER, mPosY - GlyphManager.GLYPH_BORDER,
                glyph.width + GlyphManager.GLYPH_BORDER * 2, glyph.height + GlyphManager.GLYPH_BORDER * 2,
                0, 0, 0, 1,
                GL_RED, GL_UNSIGNED_BYTE, pixels);
        mTexture.generateMipmapCompat();

        glyph.u1 = (float) mPosX / mWidth;
        glyph.v1 = (float) mPosY / mHeight;
        glyph.u2 = (float) (mPosX + glyph.width) / mWidth;
        glyph.v2 = (float) (mPosY + glyph.height) / mHeight;

        mPosX += glyph.width + GlyphManager.GLYPH_BORDER * 2;
        mLineHeight = Math.max(mLineHeight, glyph.height);
    }

    private void resize() {
        // never initialized
        if (mWidth == 0) {
            mWidth = mHeight = INITIAL_SIZE;
            mTexture.allocate2DCompat(GL_R8, INITIAL_SIZE, INITIAL_SIZE, MIPMAP_LEVEL);
            // we have border that not upload data, so generate mipmap may leave undefined data
            //mTexture.clear(0);
        } else {
            final int oldWidth = mWidth;
            final int oldHeight = mHeight;

            final boolean vertical;
            if (mHeight != mWidth) {
                mWidth <<= 1;
                vertical = false;
            } else {
                mHeight <<= 1;
                vertical = true;
            }

            // copy to new texture
            GLTexture newTexture = new GLTexture(GL_TEXTURE_2D);
            newTexture.allocate2DCompat(GL_R8, mWidth, mHeight, MIPMAP_LEVEL);
            if (sCopyFramebuffer == 0) {
                sCopyFramebuffer = glGenFramebuffers();
            }
            final int pt = glGetInteger(GL_TEXTURE_BINDING_2D);
            glBindTexture(GL_TEXTURE_2D, newTexture.get());
            final int pfb = glGetInteger(GL_FRAMEBUFFER_BINDING);
            glBindFramebuffer(GL_FRAMEBUFFER, sCopyFramebuffer);

            glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, mTexture.get(), 0);
            glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, oldWidth, oldHeight);
            glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, 0, 0);

            glBindTexture(GL_TEXTURE_2D, pt);
            glBindFramebuffer(GL_FRAMEBUFFER, pfb);

            mTexture.close();
            mTexture = newTexture;

            if (vertical) {
                //mTexture.clear(0, 0, mHeight >> 1, mWidth, mHeight >> 1);
                for (GLBakedGlyph glyph : mGlyphs.values()) {
                    if (glyph == null) {
                        continue;
                    }
                    glyph.v1 *= 0.5;
                    glyph.v2 *= 0.5;
                    // texture id changed
                    glyph.texture = mTexture.get();
                }
            } else {
                //mTexture.clear(0, mWidth >> 1, 0, mWidth >> 1, mHeight);
                for (GLBakedGlyph glyph : mGlyphs.values()) {
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
        mTexture.setFilterCompat(sLinearSampling ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST, GL_NEAREST);
        mTexture.setSwizzleCompat(GL_ONE, GL_ONE, GL_ONE, GL_RED);
    }

    public void debug(@Nullable String path) {
        if (path == null) {
            for (var glyph : mGlyphs.int2ObjectEntrySet()) {
                ModernUI.LOGGER.info(GlyphManager.MARKER, "GlyphCode {}: {}", glyph.getIntKey(), glyph.getValue());
            }
        } else if (Core.isOnRenderThread()) {
            ModernUI.LOGGER.info(GlyphManager.MARKER, "Glyphs: {}", mGlyphs.size());
            try (NativeImage image = NativeImage.download(NativeImage.Format.RED, mTexture, false)) {
                image.saveToPath(Path.of(path), NativeImage.SaveFormat.PNG, 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        if (mTexture != null) {
            mTexture.close();
        }
        mTexture = null;
    }

    public void dumpShortInfo(@Nonnull PrintWriter pw) {
        pw.print("Glyphs: " + mGlyphs.size() + ", Memory: " + mTexture.getWidth() * mTexture.getHeight());
    }
}
