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

import icyllis.modernui.graphics.MathUtil;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.FloatBuffer;

/**
 * Sound stream provides uncompressed PCM audio samples decoded from an
 * {@link java.io.InputStream InputStream} or a {@link java.nio.channels.FileChannel FileChannel}.
 * This is the pushing API, no seeking.
 */
public abstract class SoundStream implements AutoCloseable {

    protected int mSampleRate;
    protected int mChannels;
    protected int mSampleOffset;

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
     * Get sample offset of the start of next frame.
     *
     * @return offset in samples
     */
    public int getSampleOffset() {
        return mSampleOffset;
    }

    @Nullable
    public abstract FloatBuffer decodeFrame(@Nullable FloatBuffer output) throws IOException;

    @Override
    public abstract void close() throws IOException;

    // there are programs using 32767, which is not correct

    // [-1,1] to [-32768,32767]
    public static short f_to_s16(float s) {
        return (short) (s * 32767.5f - 0.5f);
    }

    // [-32768,32767] to [-1,1]
    public static float s16_to_f(short s) {
        return (s + 0.5f) / 32767.5f;
    }
}
