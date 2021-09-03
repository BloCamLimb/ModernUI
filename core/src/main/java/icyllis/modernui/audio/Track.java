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

import org.lwjgl.openal.EXTFloat32;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.FloatBuffer;

import static org.lwjgl.openal.AL11.*;

public class Track implements AutoCloseable {

    private int mSource;

    private SampledSound mSound;
    private int mBaseOffset;

    public Track(@Nonnull SampledSound sound) {
        mSource = alGenSources();
        mSound = sound;
        alSourcef(mSource, AL_GAIN, 0.75f);
        forward(2);
        AudioManager.getInstance().addTrack(this);
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
        return ((float) mBaseOffset / mSound.getSampleRate()) + alGetSourcef(mSource, AL_SEC_OFFSET);
    }

    public float getLength() {
        return mSound.getLength();
    }

    public void tick() {
        forward(releaseUsedBuffers());
    }

    private void forward(int count) {
        FloatBuffer buffer = null;
        try {
            int samplesPerSeconds = mSound.getChannels() * mSound.getSampleRate();
            for (int i = 0; i < count; i++) {
                if (buffer != null) {
                    buffer.position(0);
                }
                int offset = mSound.getOffset();
                while (buffer == null || buffer.position() < samplesPerSeconds) {
                    buffer = mSound.decodeFrame(buffer);
                    if (buffer == null) {
                        break;
                    }
                }
                if (buffer != null) {
                    buffer.flip();
                    int buf = alGenBuffers();
                    alBufferData(buf, mSound.getChannels() == 1 ? EXTFloat32.AL_FORMAT_MONO_FLOAT32 :
                                    EXTFloat32.AL_FORMAT_STEREO_FLOAT32, buffer,
                            mSound.getSampleRate());
                    alSourceQueueBuffers(mSource, buf);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    private int releaseUsedBuffers() {
        int count = alGetSourcei(mSource, AL_BUFFERS_PROCESSED);
        for (int i = 0; i < count; i++) {
            int buf = alSourceUnqueueBuffers(mSource);
            mBaseOffset += alGetBufferi(buf, AL_SIZE) / 4 / mSound.mChannels;
            alDeleteBuffers(buf);
        }
        return count;
    }

    @Override
    public void close() throws IOException {
        if (mSource != 0) {
            alDeleteBuffers(mSource);
            mSource = 0;
        }
        mSound.close();
    }
}
