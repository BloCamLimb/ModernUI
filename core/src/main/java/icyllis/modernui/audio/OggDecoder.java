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

import icyllis.modernui.ModernUI;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

import static org.lwjgl.system.MemoryUtil.NULL;

public class OggDecoder extends SampledSound {

    private final FileChannel mChannel;
    private ByteBuffer mBuffer;
    private long mDecoder;

    public OggDecoder(@Nonnull FileChannel channel) throws IOException {
        mChannel = channel;
        // ogg page is 4 KB ~ 16 KB
        mBuffer = MemoryUtil.memAlloc(1 << 12).flip();
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

            STBVorbisInfo info = STBVorbisInfo.mallocStack(stack);
            STBVorbis.stb_vorbis_get_info(decoder, info);
            mSampleRate = info.sample_rate();
            int channels = info.channels();
            if (channels != 1 && channels != 2) {
                throw new IOException("Not 1 or 2 channels but " + channels);
            }
            mChannels = channels;
        }
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

    public void decodeFrame() throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final PointerBuffer samples = stack.mallocPointer(1);
            final IntBuffer count = stack.mallocInt(1);
            for (; ; ) {
                int used = STBVorbis.stb_vorbis_decode_frame_pushdata(mDecoder, mBuffer, null, samples, count);
                mBuffer.position(mBuffer.position() + used);
                if (used == 0) {
                    forward();
                    if (read()) {
                        return;
                    }
                } else if (count.get(0) > 0) {
                    int off = STBVorbis.stb_vorbis_get_sample_offset(mDecoder);
                    ModernUI.LOGGER.info("Decode frame, used: {}, samples: {}, offset: {}, {}",
                            used, count.get(), off, mBuffer);
                    return;
                }
            }
        }
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
