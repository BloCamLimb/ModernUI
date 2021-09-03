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

import javax.annotation.Nonnull;

import static org.lwjgl.openal.AL11.*;

public class Track implements AutoCloseable {

    private int mSource;
    private int mBuffer;

    public Track(@Nonnull WaveDecoder waveDecoder) {
        mSource = alGenSources();
        mBuffer = alGenBuffers();
        alBufferData(mBuffer, AL_FORMAT_STEREO16, waveDecoder.mData, waveDecoder.mSampleRate);
        alSourcei(mSource, AL_BUFFER, mBuffer);
        alSourcef(mSource, AL_GAIN, 0.75f);
    }

    public void play() {
        if (mSource != 0) {
            alSourcePlay(mSource);
        }
    }

    public void pause() {
        if (mSource != 0) {
            alSourcePause(mSource);
        }
    }

    public float getTime() {
        if (mSource == 0) {
            return 0;
        }
        return alGetSourcef(mSource, AL_SEC_OFFSET);
    }

    @Override
    public void close() {
        if (mSource != 0) {
            alDeleteBuffers(mBuffer);
            alDeleteBuffers(mSource);
            mSource = 0;
        }
    }
}
