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

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class WaveDecoder {

    public int mSampleRate;
    public float[] mSamples;

    public WaveDecoder(@Nonnull FileChannel channel) throws Exception {
        ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        if (buffer.getInt() != 0x46464952) {
            throw new IllegalArgumentException("Not RIFF");
        }
        // file size
        buffer.getInt();
        if (buffer.getInt() != 0x45564157) {
            throw new IllegalArgumentException("Not WAVE");
        }
        if (buffer.getInt() != 0x20746d66) {
            throw new IllegalArgumentException("Not fmt chunk");
        }
        int chunkSize = buffer.getInt();
        if (chunkSize < 16) {
            throw new IllegalArgumentException("Chunk size is invalid");
        }
        short format = buffer.getShort();
        if (format != 1) {
            throw new IllegalArgumentException("Not PCM format");
        }
        short channels = buffer.getShort();
        ModernUI.LOGGER.info("Channels: {}", channels);
        int sampleRate = buffer.getInt();
        ModernUI.LOGGER.info("Sample Rate: {}", sampleRate);
        if (buffer.getInt() != sampleRate * channels << 1 ||
                buffer.getShort() != channels << 1 ||
                buffer.getShort() != 16) {
            throw new IllegalArgumentException("Not 16-bit sample");
        }
        if (chunkSize > 16) {
            buffer.position(buffer.position() + chunkSize - 16);
        }
        if (buffer.getInt() != 0x61746164) {
            throw new IllegalArgumentException("Not data chunk");
        }
        float f = 1.0f / 65536;
        int dataSize = buffer.getInt();
        float[] samples = new float[dataSize / 2 / channels];
        for (int i = 0; i < samples.length; i++) {
            float sample = 0;
            for (int j = 0; j < channels; j++) {
                int val = buffer.getShort() + 32768;
                sample += f * val;
            }
            samples[i] = sample / channels;
        }
        ModernUI.LOGGER.info("Song Length: {} secs", samples.length / sampleRate);

        mSampleRate = sampleRate;
        mSamples = samples;
    }
}
