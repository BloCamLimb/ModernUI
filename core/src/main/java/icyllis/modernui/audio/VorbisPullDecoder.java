/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.nio.*;

import static org.lwjgl.system.MemoryUtil.NULL;

public class VorbisPullDecoder extends SoundSample {

    private ByteBuffer mPayload;

    private long mHandle;

    public VorbisPullDecoder(@Nonnull ByteBuffer nativeEncodedAudioBuffer) {
        mPayload = nativeEncodedAudioBuffer;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer error = stack.mallocInt(1);
            mHandle = STBVorbis.stb_vorbis_open_memory(nativeEncodedAudioBuffer, error, null);
            int er = error.get(0);
            if (er != STBVorbis.VORBIS__no_error) {
                throw new IllegalStateException("Failed to open Vorbis file " + er);
            }
            if (mHandle == NULL) {
                throw new AssertionError();
            }
            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(mHandle, info);
            mSampleRate = info.sample_rate();
            int channels = info.channels();
            if (channels != 1 && channels != 2) {
                throw new IllegalStateException("Not 1 or 2 channels but " + channels);
            }
            mChannels = channels;
        }
        mTotalSamples = STBVorbis.stb_vorbis_stream_length_in_samples(mHandle);
    }

    @Override
    public boolean seek(int sampleOffset) {
        return STBVorbis.stb_vorbis_seek(mHandle, sampleOffset);
    }

    @Override
    public int getSamplesShortInterleaved(ShortBuffer pcmBuffer) {
        return STBVorbis.stb_vorbis_get_samples_short_interleaved(mHandle, mChannels, pcmBuffer);
    }

    @Override
    public void close() {
        MemoryUtil.memFree((Buffer) mPayload);
        mPayload = null;
        if (mHandle != NULL) {
            STBVorbis.stb_vorbis_close(mHandle);
            mHandle = NULL;
        }
    }
}
