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

package icyllis.modernui.platform;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class NativeImage implements AutoCloseable {

    @Nonnull
    private final Format mFormat;

    private final int mWidth;
    private final int mHeight;

    private long mPixels;
    private final boolean mFromSTB;

    public NativeImage(@Nonnull Format format, int width, int height, boolean initialize) {
        mFormat = format;
        mWidth = width;
        mHeight = height;
        mFromSTB = false;
        if (initialize) {
            mPixels = MemoryUtil.nmemCalloc(1L, (long) width * height * format.channels);
        } else {
            mPixels = MemoryUtil.nmemAlloc((long) width * height * format.channels);
        }
    }

    private NativeImage(@Nonnull Format format, int width, int height, long pixels) {
        mFormat = format;
        mWidth = width;
        mHeight = height;
        mPixels = pixels;
        mFromSTB = true;
    }

    /**
     * Read an image from input stream. This method doesn't close input stream.
     *
     * @param format the format to convert to, {@code null} use file intrinsic format
     * @param stream input stream
     */
    @Nonnull
    public static NativeImage read(@Nullable NativeImage.Format format, @Nonnull InputStream stream) throws IOException {
        ByteBuffer buffer = null;
        try {
            buffer = RenderCore.readRawBuffer(stream);
            buffer.rewind();
            return read(format, buffer);
        } finally {
            if (buffer != null)
                MemoryUtil.memFree(buffer);
        }
    }

    // this method doesn't close/free the buffer
    @Nonnull
    public static NativeImage read(@Nullable NativeImage.Format format, @Nonnull ByteBuffer buffer) throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            ByteBuffer data = STBImage.stbi_load_from_memory(buffer, width, height, channels, format == null ? 0 : format.channels);
            if (data == null) {
                throw new IOException("Failed to read image: " + STBImage.stbi_failure_reason());
            }
            return new NativeImage(format == null ? Format.values()[channels.get(0) - 1] : format, width.get(0), height.get(0), MemoryUtil.memAddress(data));
        }
    }

    @Nonnull
    public Format getFormat() {
        return mFormat;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public long getPixelsPtr() {
        return mPixels;
    }

    @Override
    public void close() throws Exception {
        if (mPixels != MemoryUtil.NULL) {
            if (mFromSTB) {
                STBImage.nstbi_image_free(mPixels);
            } else {
                MemoryUtil.nmemFree(mPixels);
            }
            mPixels = MemoryUtil.NULL;
        }
    }

    public enum Format {
        LUMINANCE(1),
        LUMINANCE_ALPHA(2),
        RGB(3),
        RGBA(4);

        public final int channels;

        Format(int channels) {
            this.channels = channels;
        }
    }
}
