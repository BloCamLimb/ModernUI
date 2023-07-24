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

package icyllis.modernui.audio;

import icyllis.arc3d.core.MathUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.*;
import java.nio.channels.FileChannel;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Support for Ogg Vorbis.
 */
public class OggDecoder extends SoundSample {

    private final FileChannel mChannel;
    private ByteBuffer mBuffer;

    private long mDecoder;

    public OggDecoder(@Nonnull FileChannel channel) throws IOException {
        mChannel = channel;
        // ogg page is 4 KB ~ 16 KB
        mBuffer = MemoryUtil.memAlloc(0x1000).flip();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer consumed = stack.mallocInt(1);
            final IntBuffer error = stack.mallocInt(1);
            long decoder;
            do {
                if (read()) {
                    throw new IOException("No Ogg header found");
                }
                decoder = STBVorbis.stb_vorbis_open_pushdata(mBuffer, consumed, error, null);
                int er = error.get(0);
                if (er == STBVorbis.VORBIS_need_more_data) {
                    forward();
                } else if (er != STBVorbis.VORBIS__no_error) {
                    throw new IOException("Failed to open Ogg file " + er);
                }
            } while (decoder == NULL);
            mDecoder = decoder;
            int headerSize = consumed.get(0);
            mBuffer.position(mBuffer.position() + headerSize);

            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(decoder, info);
            mSampleRate = info.sample_rate();
            int channels = info.channels();
            if (channels != 1 && channels != 2) {
                throw new IOException("Not 1 or 2 channels but " + channels);
            }
            mChannels = channels;

            // a slow way to find the length (last page)
            ByteBuffer temp = stack.malloc(4 + 2 + 8);
            temp.order(ByteOrder.LITTLE_ENDIAN);
            final long size = channel.size();
            for (int i = 1 + temp.capacity(); i <= 0x4000; i++) {
                channel.read(temp, size - i);
                // 'OggS' magic
                if (temp.getInt(0) == 0x5367674f) {
                    mTotalSamples = temp.getInt(4 + 2);
                    break;
                }
                temp.clear();
            }
        }
    }

    public void getSamplesShortInterleaved(ShortBuffer buffer) {
        STBVorbis.stb_vorbis_get_samples_short_interleaved(mDecoder, mChannels, buffer);
    }

    /**
     * Read more data from channel or stream if buffer can.
     *
     * @return {@code true} meaning EOF
     * @throws IOException failed to read
     */
    private boolean read() throws IOException {
        ByteBuffer buffer = mBuffer;
        int limit = buffer.limit();
        int require = buffer.capacity() - limit;
        if (require > 0) {
            int pos = buffer.position();
            buffer.position(limit);
            buffer.limit(limit + require);
            int read = mChannel.read(buffer);
            if (read == -1) {
                return true;
            }
            buffer.position(pos);
            buffer.limit(limit + read);
        }
        return false;
    }

    private void forward() {
        int pos = mBuffer.position();
        if (pos > 0 && pos == mBuffer.limit()) {
            // there's data but all consumed, empty the buffer
            mBuffer.rewind().flip();
        } else {
            // if need more data, increment the capacity or back to start
            ByteBuffer buffer = MemoryUtil.memAlloc(mBuffer.capacity() << (pos == 0 ? 1 : 0));
            buffer.put(mBuffer);
            MemoryUtil.memFree(mBuffer);
            mBuffer = buffer.flip();
        }
    }

    @Override
    @Nullable
    public FloatBuffer decodeFrame(@Nullable FloatBuffer output) throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final PointerBuffer samples = stack.mallocPointer(1);
            final IntBuffer count = stack.mallocInt(1);
            for (;;) {
                int n = STBVorbis.stb_vorbis_decode_frame_pushdata(mDecoder, mBuffer, null, samples, count);
                mBuffer.position(mBuffer.position() + n);
                if (n == 0) {
                    forward();
                    if (read()) {
                        return null;
                    }
                } else if ((n = count.get(0)) > 0) {
                    mOffset = STBVorbis.stb_vorbis_get_sample_offset(mDecoder);
                    PointerBuffer data = samples.getPointerBuffer(mChannels);
                    if (mChannels == 1) {
                        FloatBuffer src = data.getFloatBuffer(0, n);
                        while (src.hasRemaining()) {
                            if (output == null || !output.hasRemaining()) {
                                output = MemoryUtil.memRealloc(output, output == null ? 256 :
                                        output.capacity() + 256);
                            }
                            output.put(src.get());
                        }
                    } else if (mChannels == 2) {
                        FloatBuffer srcR = data.getFloatBuffer(0, n);
                        FloatBuffer srcL = data.getFloatBuffer(1, n);
                        while (srcR.hasRemaining()) {
                            if (output == null || output.remaining() < 2) {
                                output = MemoryUtil.memRealloc(output, output == null ? 256 :
                                        output.capacity() + 256);
                            }
                            output.put(srcR.get())
                                    .put(srcL.get());
                        }
                    } else {
                        throw new IllegalStateException();
                    }
                    return output;
                }
            }
        }
    }

    private static short f2s16(float s) {
        return (short)MathUtil.clamp((int)(s * 32767.5f - 0.5f), Short.MIN_VALUE, Short.MAX_VALUE);
    }

    @Override
    public void close() throws IOException {
        if (mDecoder != NULL) {
            STBVorbis.stb_vorbis_close(mDecoder);
            mDecoder = NULL;
        }
        MemoryUtil.memFree(mBuffer);
        mBuffer = null;
        mChannel.close();
    }
}
