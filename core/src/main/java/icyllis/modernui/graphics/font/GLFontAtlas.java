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

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.opengl.*;
import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.*;
import icyllis.modernui.core.Core;
import icyllis.modernui.graphics.Bitmap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static icyllis.arc3d.opengl.GLCore.*;

/**
 * Maintains a font texture atlas, which is specified with a font strike (style and
 * size). Glyphs are dynamically generated with mipmaps, each glyph is represented as
 * a {@link BakedGlyph}.
 * <p>
 * The initial texture size is 1024*1024, and each resize double the height and width
 * alternately. For example, 1024*1024 -> 1024*2048 -> 2048*2048.
 * Each 512*512 area becomes a chunk, and has its {@link RectanglePacker}.
 * The OpenGL texture ID will change due to expanding the texture size.
 *
 * @see GlyphManager
 * @see BakedGlyph
 */
//TODO handle too many glyphs?
@RenderThread
public class GLFontAtlas implements AutoCloseable {

    // max texture size is 1024 at least
    // we compact texture at 1/4 max area
    public static final int CHUNK_SIZE = 512;
    /*
     * Max mipmap level.
     */
    //public static final int MIPMAP_LEVEL = 4;

    /**
     * Config values.
     * Linear sampling with mipmaps;
     */
    public static volatile boolean sLinearSampling = true;

    // OpenHashMap uses less memory than RBTree/AVLTree, but higher than ArrayMap
    private final Long2ObjectMap<BakedGlyph> mGlyphs = new Long2ObjectOpenHashMap<>();

    // texture can change by resizing
    GLTexture mTexture = null;

    private final List<Chunk> mChunks = new ArrayList<>();

    // current texture size
    private int mWidth = 0;
    private int mHeight = 0;

    private final Rect2i mRect = new Rect2i();

    private record Chunk(int x, int y, RectanglePacker packer) {
    }

    private final DirectContext mContext;
    private final int mMaskFormat;
    private final int mBorderWidth;
    private final int mMaxTextureSize;

    // overflow and wrap
    private int mLastCompactChunkIndex;

    @RenderThread
    public GLFontAtlas(DirectContext context, int maskFormat, int borderWidth) {
        mContext = context;
        mMaskFormat = maskFormat;
        mBorderWidth = borderWidth;
        // 64MB at most
        mMaxTextureSize = Math.min(
                mContext.getMaxTextureSize(),
                maskFormat == Engine.MASK_FORMAT_A8
                        ? 8192
                        : 4096
        );
        assert mMaxTextureSize >= 1024;
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
    public BakedGlyph getGlyph(long key) {
        // static factory
        return mGlyphs.computeIfAbsent(key, __ -> new BakedGlyph());
    }

    public void setNoPixels(long key) {
        mGlyphs.put(key, null);
    }

    public boolean stitch(@NonNull BakedGlyph glyph, long pixels) {
        boolean invalidated = false;
        if (mWidth == 0) {
            resize(); // first init
        }

        // the source image includes border, but glyph.width/height does not include
        var rect = mRect;
        rect.set(0, 0,
                glyph.width + mBorderWidth * 2, glyph.height + mBorderWidth * 2);
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
            invalidated = resize();
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
        int colorType = mMaskFormat == Engine.MASK_FORMAT_ARGB
                ? ImageInfo.CT_RGBA_8888
                : ImageInfo.CT_ALPHA_8;
        int rowBytes = rect.width() * ImageInfo.bytesPerPixel(colorType);
        boolean res = mContext.getDevice().writePixels(
                mTexture,
                rect.x(), rect.y(),
                rect.width(), rect.height(),
                colorType,
                colorType,
                rowBytes,
                pixels
        );
        if (!res) {
            ModernUI.LOGGER.warn(GlyphManager.MARKER, "Failed to write glyph pixels");
        }

        // exclude border
        glyph.u1 = (float) (rect.mLeft + mBorderWidth) / mWidth;
        glyph.v1 = (float) (rect.mTop + mBorderWidth) / mHeight;
        glyph.u2 = (float) (rect.mRight - mBorderWidth) / mWidth;
        glyph.v2 = (float) (rect.mBottom - mBorderWidth) / mHeight;

        return invalidated;
    }

    private boolean resize() {
        if (mTexture == null) {
            // initialize 4 chunks
            mWidth = mHeight = CHUNK_SIZE * 2;
            mTexture = createTexture();
            for (int x = 0; x < mWidth; x += CHUNK_SIZE) {
                for (int y = 0; y < mHeight; y += CHUNK_SIZE) {
                    mChunks.add(new Chunk(x, y, RectanglePacker.make(CHUNK_SIZE, CHUNK_SIZE)));
                }
            }
        } else {
            final int oldWidth = mWidth;
            final int oldHeight = mHeight;

            if (oldWidth == mMaxTextureSize && oldHeight == mMaxTextureSize) {
                ModernUI.LOGGER.warn(GlyphManager.MARKER, "Font atlas reached max texture size, " +
                        "mask format: {}, max size: {}, current texture: {}", mMaskFormat, mMaxTextureSize, mTexture);
                return false;
            }

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
            GLTexture newTexture = createTexture();
            boolean res = mContext.getDevice().copySurface(
                    mTexture,
                    0, 0,
                    newTexture,
                    0, 0,
                    oldWidth, oldHeight
            );
            if (!res) {
                ModernUI.LOGGER.warn(GlyphManager.MARKER, "Failed to copy to new texture");
            }

            mTexture = GpuResource.move(mTexture, newTexture);

            if (vertical) {
                //mTexture.clear(0, 0, mHeight >> 1, mWidth, mHeight >> 1);
                for (BakedGlyph glyph : mGlyphs.values()) {
                    if (glyph == null) {
                        continue;
                    }
                    glyph.v1 *= 0.5f;
                    glyph.v2 *= 0.5f;
                }
            } else {
                //mTexture.clear(0, mWidth >> 1, 0, mWidth >> 1, mHeight);
                for (BakedGlyph glyph : mGlyphs.values()) {
                    if (glyph == null) {
                        continue;
                    }
                    glyph.u1 *= 0.5f;
                    glyph.u2 *= 0.5f;
                }
            }

            // we later generate mipmap
        }

        int boundTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, mTexture.getHandle());

        glTexParameteri(
                GL_TEXTURE_2D,
                GL_TEXTURE_MAG_FILTER,
                GL_NEAREST
        );
        glTexParameteri(
                GL_TEXTURE_2D,
                GL_TEXTURE_MIN_FILTER,
                sLinearSampling
                        ? GL_LINEAR_MIPMAP_LINEAR
                        : GL_NEAREST
        );

        if (mMaskFormat == Engine.MASK_FORMAT_A8) {
            //XXX: un-premultiplied
            try (var stack = MemoryStack.stackPush()) {
                var swizzle = stack.ints(GL_ONE, GL_ONE, GL_ONE, GL_RED);
                glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_RGBA, swizzle);
            }
        }

        glBindTexture(GL_TEXTURE_2D, boundTexture);

        return true;
    }

    private GLTexture createTexture() {
        return (GLTexture) Objects.requireNonNull(mContext
                .getResourceProvider()
                .createTexture(
                        mWidth, mHeight,
                        GLBackendFormat.make(
                                mMaskFormat == Engine.MASK_FORMAT_ARGB
                                        ? GL_RGBA8
                                        : GL_R8
                        ),
                        1,
                        ISurface.FLAG_BUDGETED | ISurface.FLAG_MIPMAPPED,
                        "FontAtlas" + mMaskFormat
                ), "Failed to create font atlas");
    }

    public GLTexture getTexture() {
        return mTexture;
    }

    public boolean compact() {
        if (mWidth < mMaxTextureSize &&
                mHeight < mMaxTextureSize) {
            // not reach 1/4 of max area
            return false;
        }
        assert mChunks.size() > 1;
        //TODO this implementation is not ideal and we need a review
        double coverage = 0;
        for (Chunk chunk : mChunks) {
            coverage += chunk.packer.getCoverage();
        }
        int chunksPerDim = mMaxTextureSize / CHUNK_SIZE;
        // clear 1/4 coverage of max
        double maxCoverage = chunksPerDim * chunksPerDim * 0.25f;
        if (coverage <= maxCoverage) {
            return false;
        }
        double coverageToClean = Math.max(coverage - maxCoverage, maxCoverage);
        boolean cleared = false;
        // clear 16 chunks at most
        for (int iChunk = 0;
             iChunk < Math.min(16, mChunks.size()) && coverageToClean > 0;
             iChunk++) {
            // let index overflow and wrap
            assert MathUtil.isPow2(mChunks.size());
            int index = (mLastCompactChunkIndex++) & (mChunks.size() - 1);
            Chunk chunk = mChunks.get(index);
            double cc = chunk.packer.getCoverage();
            if (cc == 0) {
                continue;
            }
            coverageToClean -= cc;
            chunk.packer.clear();
            float cu1 = (float) chunk.x / mWidth;
            float cv1 = (float) chunk.y / mHeight;
            float cu2 = cu1 + (float) CHUNK_SIZE / mWidth;
            float cv2 = cv1 + (float) CHUNK_SIZE / mHeight;
            for (var glyph : mGlyphs.values()) {
                if (glyph.u1 >= cu1 && glyph.u2 < cu2 &&
                        glyph.v1 >= cv1 && glyph.v2 < cv2) {
                    // invalidate glyph image
                    glyph.x = Short.MIN_VALUE;
                }
            }
            cleared = true;
        }
        return cleared;
    }

    public void debug(@Nullable String path) {
        if (path == null) {
            for (var glyph : mGlyphs.long2ObjectEntrySet()) {
                ModernUI.LOGGER.info(GlyphManager.MARKER, "Key 0x{}: {}",
                        Long.toHexString(glyph.getLongKey()), glyph.getValue());
            }
        } else if (Core.isOnRenderThread()) {
            ModernUI.LOGGER.info(GlyphManager.MARKER, "Glyphs: {}", mGlyphs.size());
            if (mTexture == null)
                return;
            dumpAtlas((GLCaps) mContext.getCaps(), mTexture,
                    mMaskFormat == Engine.MASK_FORMAT_ARGB
                            ? Bitmap.Format.RGBA_8888
                            : Bitmap.Format.GRAY_8,
                    path);
        }
    }

    @RenderThread
    public static void dumpAtlas(GLCaps caps, GLTexture texture, Bitmap.Format format, String path) {
        // debug only
        if (caps.hasDSASupport()) {
            final int width = texture.getWidth();
            final int height = texture.getHeight();
            @SuppressWarnings("resource") final Bitmap bitmap =
                    Bitmap.createBitmap(width, height, format);
            glPixelStorei(GL_PACK_ROW_LENGTH, 0);
            glPixelStorei(GL_PACK_SKIP_ROWS, 0);
            glPixelStorei(GL_PACK_SKIP_PIXELS, 0);
            glPixelStorei(GL_PACK_ALIGNMENT, 1);
            int externalGlFormat = switch (format) {
                case GRAY_8 -> GL_RED;
                case GRAY_ALPHA_88 -> GL_RG;
                case RGB_888 -> GL_RGB;
                case RGBA_8888 -> GL_RGBA;
                default -> throw new IllegalArgumentException();
            };
            glGetTextureImage(texture.getHandle(), 0, externalGlFormat, GL_UNSIGNED_BYTE,
                    (int) bitmap.getSize(), bitmap.getAddress());
            CompletableFuture.runAsync(() -> {
                try (bitmap) {
                    bitmap.saveToPath(Bitmap.SaveFormat.PNG, 0, Path.of(path));
                } catch (IOException e) {
                    ModernUI.LOGGER.warn(GlyphManager.MARKER, "Failed to save font atlas", e);
                }
            });
        }
    }

    @Override
    public void close() {
        mTexture = GpuResource.move(mTexture);
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getGlyphCount() {
        return mGlyphs.size();
    }

    public long getMemorySize() {
        return mTexture != null ? mTexture.getMemorySize() : 0;
    }

    /**
     * @return 0..1
     */
    public double getCoverage() {
        if (mChunks.isEmpty()) {
            return 0;
        }
        double coverage = 0;
        for (Chunk chunk : mChunks) {
            coverage += chunk.packer.getCoverage();
        }
        return coverage / mChunks.size();
    }
}
