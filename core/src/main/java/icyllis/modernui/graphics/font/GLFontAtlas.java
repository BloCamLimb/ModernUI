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
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;

import static icyllis.modernui.graphics.opengl.GLCore.*;

/**
 * Maintains a font texture atlas, which is specified with a font size. In this way,
 * the glyphs can have similar sizes so that it will help to tightly packed these
 * sprites. Glyphs are dynamically generated with mipmaps. Each glyph is represented
 * as a {@link GLBakedGlyph}.
 * <p>
 * The initial texture size is 256*256, and each resize double the height and width
 * alternately. For example, 512*512 -> 512*1024 -> 1024*1024.
 * The max texture size would be 16384*16384 and the image is 8-bit grayscale.
 * The OpenGL texture will change due to increasing the texture size.
 *
 * @see GlyphManager
 * @see GLBakedGlyph
 */
//TODO handle too many glyphs?
@RenderThread
public class GLFontAtlas implements AutoCloseable {

    public static final int INITIAL_SIZE = 512;
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
    private final Long2ObjectMap<GLBakedGlyph> mGlyphs = new Long2ObjectOpenHashMap<>();

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

    private final boolean mColored;

    // create from any thread
    public GLFontAtlas() {
        this(false);
    }

    public GLFontAtlas(boolean colored) {
        mColored = colored;
    }

    @Nullable
    public GLBakedGlyph getGlyph(long key) {
        // cached factory
        return mGlyphs.computeIfAbsent(key, __ -> new GLBakedGlyph());
    }

    // needed when the glyph has nothing to render
    public void setNull(long key) {
        mGlyphs.put(key, null);
    }

    public boolean stitch(@Nonnull GLBakedGlyph glyph, long pixels) {
        boolean resized = false;
        glyph.texture = mTexture.get();
        if (mWidth == 0) {
            resize(); // first init
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
            resized = true;
        }

        // include border
        mTexture.uploadCompat(0, mPosX - GlyphManager.GLYPH_BORDER, mPosY - GlyphManager.GLYPH_BORDER,
                glyph.width + GlyphManager.GLYPH_BORDER * 2, glyph.height + GlyphManager.GLYPH_BORDER * 2,
                0, 0, 0, 1,
                mColored ? GL_RGBA : GL_RED, GL_UNSIGNED_BYTE, pixels);
        mTexture.generateMipmapCompat();

        glyph.u1 = (float) mPosX / mWidth;
        glyph.v1 = (float) mPosY / mHeight;
        glyph.u2 = (float) (mPosX + glyph.width) / mWidth;
        glyph.v2 = (float) (mPosY + glyph.height) / mHeight;

        mPosX += glyph.width + GlyphManager.GLYPH_BORDER * 2;
        mLineHeight = Math.max(mLineHeight, glyph.height);

        return resized;
    }

    private void resize() {
        // never initialized
        if (mWidth == 0) {
            mWidth = mHeight = INITIAL_SIZE;
            mTexture.allocate2DCompat(mColored ? GL_RGBA8 : GL_R8, INITIAL_SIZE, INITIAL_SIZE, MIPMAP_LEVEL);
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
            newTexture.allocate2DCompat(mColored ? GL_RGBA8 : GL_R8, mWidth, mHeight, MIPMAP_LEVEL);
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
        if (!mColored) {
            mTexture.setSwizzleCompat(GL_ONE, GL_ONE, GL_ONE, GL_RED);
        }
    }

    public void debug(@Nullable String path) {
        if (path == null) {
            for (var glyph : mGlyphs.long2ObjectEntrySet()) {
                ModernUI.LOGGER.info(GlyphManager.MARKER, "Key {}: {}",
                        Long.toHexString(glyph.getLongKey()), glyph.getValue());
            }
        } else if (Core.isOnRenderThread()) {
            ModernUI.LOGGER.info(GlyphManager.MARKER, "Glyphs: {}", mGlyphs.size());
            try (NativeImage image = NativeImage.download(mColored ? NativeImage.Format.RGBA : NativeImage.Format.RED,
                    mTexture, false)) {
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
            mTexture = null;
        }
    }

    public int getGlyphCount() {
        return mGlyphs.size();
    }

    public int getMemorySize() {
        int size = mTexture.getWidth() * mTexture.getHeight();
        if (mColored) {
            size <<= 2;
        }
        size = ((size - (size >> ((MIPMAP_LEVEL + 1) << 1))) << 2) / 3;
        return size;
    }
}
