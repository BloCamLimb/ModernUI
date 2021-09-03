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

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.FloatBuffer;

/**
 * Sampled sound provides uncompressed PCM audio samples decoded from an
 * {@link java.io.InputStream InputStream} or a {@link java.nio.channels.FileChannel FileChannel}.
 */
public abstract class SampledSound implements AutoCloseable {

    protected int mSampleRate;
    protected int mChannels;
    protected int mTotalSamples;
    protected int mOffset;

    public int getSampleRate() {
        return mSampleRate;
    }

    public int getChannels() {
        return mChannels;
    }

    public int getOffset() {
        return mOffset;
    }

    public float getSecOffset() {
        return (float) mOffset / mSampleRate;
    }

    public int getTotalSamples() {
        return mTotalSamples;
    }

    public float getLength() {
        return (float) mTotalSamples / mSampleRate;
    }

    @Nullable
    public abstract FloatBuffer decodeFrame(@Nullable FloatBuffer output) throws IOException;

    @Override
    public abstract void close() throws IOException;
}
