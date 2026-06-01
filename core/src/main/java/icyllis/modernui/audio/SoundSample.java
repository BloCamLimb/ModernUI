/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.audio;

import java.io.IOException;
import java.nio.ShortBuffer;

/**
 * Sampled sound provides uncompressed PCM audio samples decoded from an
 * {@link java.io.InputStream InputStream} or a {@link java.nio.channels.FileChannel FileChannel}.
 * This is the pulling API.
 */
public abstract class SoundSample implements AutoCloseable {

    protected int mSampleRate;
    protected int mChannels;
    protected int mTotalSamples;

    public int getSampleRate() {
        return mSampleRate;
    }

    /**
     * Get numbers of channels, either 1 or 2.
     *
     * @return numbers of channels
     */
    public int getChannels() {
        return mChannels;
    }

    /**
     * Get numbers of samples in one channel.
     *
     * @return total samples
     */
    public int getTotalSamples() {
        return mTotalSamples;
    }

    /**
     * Get the length of the sound in seconds.
     *
     * @return total length
     */
    public float getTotalLength() {
        return (float) mTotalSamples / mSampleRate;
    }

    /**
     * @return success or not
     */
    public abstract boolean seek(int sampleOffset);

    /**
     * @return samples per channel
     */
    public abstract int getSamplesShortInterleaved(ShortBuffer pcmBuffer);

    @Override
    public abstract void close();
}
