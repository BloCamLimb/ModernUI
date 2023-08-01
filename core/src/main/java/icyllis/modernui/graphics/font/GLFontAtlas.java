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

import icyllis.arc3d.core.Rect2i;
import icyllis.arc3d.core.RectanglePacker;
import icyllis.arc3d.engine.Engine;
import icyllis.arc3d.opengl.GLTextureCompat;
import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.*;
import icyllis.modernui.core.Core;
import icyllis.modernui.graphics.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static icyllis.arc3d.opengl.GLCore.*;

/**
 * Maintains a font texture atlas, which is specified with a font strike (style and
 * size). Glyphs are dynamically generated with mipmaps, each glyph is represented as
 * a {@link GLBakedGlyph}.
 * <p>
 * The initial texture size is 1024*1024, and each resize double the height and width
 * alternately. For example, 1024*1024 -> 1024*2048 -> 2048*2048.
 * Each 512*512 area becomes a chunk, and has its {@link RectanglePacker}.
 * The OpenGL texture ID will change due to expanding the texture size.
 *
 * @see GlyphManager
 * @see GLBakedGlyph
 */
//TODO handle too many glyphs?
@RenderThread
public class GLFontAtlas implements AutoCloseable {

    public static final int CHUNK_SIZE = 512;
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
    private GLTextureCompat mTexture = new GLTextureCompat(GL_TEXTURE_2D);

    private final List<Chunk> mChunks = new ArrayList<>();

    // current texture size
    private int mWidth;
    private int mHeight;

    private final Rect2i mRect = new Rect2i();

    private record Chunk(int x, int y, RectanglePacker packer) {
    }

    private final int mMaskFormat;
    private final int mMaxTextureSize;

    // create from any thread
    @Deprecated
    public GLFontAtlas(boolean colored) {
        this(colored ? Engine.MASK_FORMAT_ARGB : Engine.MASK_FORMAT_A8);
    }

    // create from any thread
    public GLFontAtlas(int maskFormat) {
        mMaskFormat = maskFormat;
        mMaxTextureSize = Core.requireDirectContext().getMaxTextureSize();
    }

    /**
     * When the key is absent, this method computes a new instance and returns it.
     * When the key is present but was called {@link #setNoPixels(long)} with it,
     * then this method returns null, which means there's nothing to render.
     *
     * @param key a key
     * @return the baked glyph or null if no pixels
     */
    @Nullable
    public GLBakedGlyph getGlyph(long key) {
        // static factory
        return mGlyphs.computeIfAbsent(key, __ -> new GLBakedGlyph());
    }

    public void setNoPixels(long key) {
        mGlyphs.put(key, null);
    }

    public boolean stitch(@NonNull GLBakedGlyph glyph, long pixels) {
        boolean invalidated = false;
        glyph.texture = mTexture.get();
        if (mWidth == 0) {
            resize(); // first init
        }

        // the source image includes border, but glyph.width/height does not include
        var rect = mRect;
        rect.set(0, 0,
                glyph.width + GlyphManager.GLYPH_BORDER * 2, glyph.height + GlyphManager.GLYPH_BORDER * 2);
        boolean inserted = false;
        for (Chunk chunk : mChunks) {
            if (chunk.packer.addRect(rect)) {
                inserted = true;
                rect.offset(chunk.x, chunk.y);
                break;
            }
        }
        if (!inserted) {
            // add new chunks
            resize();
            invalidated = true;
            for (Chunk chunk : mChunks) {
                if (chunk.packer.addRect(rect)) {
                    inserted = true;
                    rect.offset(chunk.x, chunk.y);
                    break;
                }
            }
        }
        if (!inserted) {
            // failed...
            return invalidated;
        }

        // include border
        mTexture.upload(0, rect.x(), rect.y(),
                rect.width(), rect.height(),
                0, 0, 0, 1,
                mMaskFormat == Engine.MASK_FORMAT_ARGB ? GL_RGBA : GL_RED, GL_UNSIGNED_BYTE, pixels);
        mTexture.generateMipmap();

        // exclude border
        glyph.u1 = (float) (rect.mLeft + GlyphManager.GLYPH_BORDER) / mWidth;
        glyph.v1 = (float) (rect.mTop + GlyphManager.GLYPH_BORDER) / mHeight;
        glyph.u2 = (float) (rect.mRight - GlyphManager.GLYPH_BORDER) / mWidth;
        glyph.v2 = (float) (rect.mBottom - GlyphManager.GLYPH_BORDER) / mHeight;

        return invalidated;
    }

    private void resize() {
        if (mWidth == 0) {
            // initialize 4 chunks
            mWidth = mHeight = CHUNK_SIZE * 2;
            mTexture.allocate2D(mMaskFormat == Engine.MASK_FORMAT_ARGB ? GL_RGBA8 : GL_R8,
                    mWidth, mHeight, MIPMAP_LEVEL);
            for (int x = 0; x < mWidth; x += CHUNK_SIZE) {
                for (int y = 0; y < mHeight; y += CHUNK_SIZE) {
                    mChunks.add(new Chunk(x, y, RectanglePacker.make(CHUNK_SIZE, CHUNK_SIZE)));
                }
            }
        } else {
            final int oldWidth = mWidth;
            final int oldHeight = mHeight;

            final boolean vertical;
            if (mHeight != mWidth) {
                mWidth <<= 1;
                for (int x = mWidth / 2; x < mWidth; x += CHUNK_SIZE) {
                    for (int y = 0; y < mHeight; y += CHUNK_SIZE) {
                        mChunks.add(new Chunk(x, y, RectanglePacker.make(CHUNK_SIZE, CHUNK_SIZE)));
                    }
                }
                vertical = false;
            } else {
                mHeight <<= 1;
                for (int x = 0; x < mWidth; x += CHUNK_SIZE) {
                    for (int y = mHeight / 2; y < mHeight; y += CHUNK_SIZE) {
                        mChunks.add(new Chunk(x, y, RectanglePacker.make(CHUNK_SIZE, CHUNK_SIZE)));
                    }
                }
                vertical = true;
            }

            // copy to new texture
            GLTextureCompat newTexture = new GLTextureCompat(GL_TEXTURE_2D);
            newTexture.allocate2D(mMaskFormat == Engine.MASK_FORMAT_ARGB ? GL_RGBA8 : GL_R8, mWidth, mHeight,
                    MIPMAP_LEVEL);
            if (sCopyFramebuffer == 0) {
                sCopyFramebuffer = glGenFramebuffers();
            }
            final int lastKnownTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
            glBindTexture(GL_TEXTURE_2D, newTexture.get());
            final int lastKnownFramebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING);
            glBindFramebuffer(GL_FRAMEBUFFER, sCopyFramebuffer);

            glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, mTexture.get(), 0);
            glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, oldWidth, oldHeight);
            glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, 0, 0);

            // restore GL context
            glBindTexture(GL_TEXTURE_2D, lastKnownTexture);
            glBindFramebuffer(GL_FRAMEBUFFER, lastKnownFramebuffer);

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
        mTexture.setFilter(sLinearSampling ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST, GL_NEAREST);
        if (mMaskFormat == Engine.MASK_FORMAT_A8) {
            //XXX: un-premultiplied
            mTexture.setSwizzle(GL_ONE, GL_ONE, GL_ONE, GL_RED);
        }
    }

    public void debug(@Nullable String path) {
        if (path == null) {
            for (var glyph : mGlyphs.long2ObjectEntrySet()) {
                ModernUI.LOGGER.info(GlyphManager.MARKER, "Key 0x{}: {}",
                        Long.toHexString(glyph.getLongKey()), glyph.getValue());
            }
        } else if (Core.isOnRenderThread()) {
            ModernUI.LOGGER.info(GlyphManager.MARKER, "Glyphs: {}", mGlyphs.size());
            if (mWidth == 0)
                return;
            try (Bitmap bitmap = Bitmap.download(
                    mMaskFormat == Engine.MASK_FORMAT_ARGB
                            ? Bitmap.Format.RGBA_8888
                            : Bitmap.Format.GRAY_8,
                    mTexture)) {
                bitmap.saveToPath(Bitmap.SaveFormat.PNG, 100, Path.of(path));
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
        if (mMaskFormat == Engine.MASK_FORMAT_ARGB) {
            size <<= 2;
        }
        size = ((size - (size >> ((MIPMAP_LEVEL + 1) << 1))) << 2) / 3;
        return size;
    }
}
