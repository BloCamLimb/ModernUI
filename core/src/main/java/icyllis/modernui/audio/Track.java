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

import icyllis.modernui.math.FourierTransform;
import org.lwjgl.openal.EXTFloat32;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.function.Consumer;

import static org.lwjgl.openal.AL11.*;

public class Track implements AutoCloseable {

    private int mSource;

    private final SoundSample mSample;
    private int mBaseOffset;

    private FourierTransform mFFT;
    private Consumer<FourierTransform> mFFTCallback;

    private float[] mMixedSamples;
    private int mMixedSampleCount;

    public Track(@Nonnull SoundSample sample) {
        mSource = alGenSources();
        mSample = sample;
        alSourcef(mSource, AL_GAIN, 0.75f);
        forward(2);
        AudioManager.getInstance().addTrack(this);
    }

    public void play() {
        if (mSource != 0 && alGetSourcei(mSource, AL_SOURCE_STATE) != AL_PLAYING) {
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
        return ((float) mBaseOffset / mSample.getSampleRate()) + alGetSourcef(mSource, AL_SEC_OFFSET);
    }

    public float getLength() {
        return mSample.getLength();
    }

    public int getSampleRate() {
        return mSample.mSampleRate;
    }

    public void tick() {
        int fr = releaseUsedBuffers();
        if (fr > 0) {
            forward(fr);
        }
        if (alGetSourcei(mSource, AL_SOURCE_STATE) == AL_PLAYING && mFFT != null) {
            int offset = alGetSourcei(mSource, AL_SAMPLE_OFFSET);
            mFFT.forward(mMixedSamples,
                    offset);
            if (mFFTCallback != null) {
                mFFTCallback.accept(mFFT);
            }
        }
    }

    public void setAnalyzer(@Nullable FourierTransform fft, @Nullable Consumer<FourierTransform> callback) {
        if (fft != null && fft.getSampleRate() != mSample.getSampleRate()) {
            throw new IllegalArgumentException("Mismatched sample rate");
        }
        mFFT = fft;
        mFFTCallback = callback;
    }

    private void forward(int count) {
        FloatBuffer buffer = null;
        try {
            final int targetSamples =
                    mSample.getChannels() * mSample.getSampleRate() / 2;
            for (int i = 0; i < count; i++) {
                if (buffer != null) {
                    buffer.position(0);
                }
                while (buffer == null || buffer.position() < targetSamples) {
                    FloatBuffer ret = mSample.decodeFrame(buffer);
                    if (ret == null) {
                        break;
                    }
                    buffer = ret;
                }
                if (buffer != null && buffer.position() > 0) {
                    buffer.flip();
                    int buf = alGenBuffers();
                    alBufferData(buf, mSample.getChannels() == 1 ? EXTFloat32.AL_FORMAT_MONO_FLOAT32 :
                                    EXTFloat32.AL_FORMAT_STEREO_FLOAT32, buffer,
                            mSample.getSampleRate());
                    alSourceQueueBuffers(mSource, buf);
                    int samp = buffer.limit() / mSample.mChannels;
                    if (mMixedSamples == null) {
                        mMixedSamples = new float[mMixedSampleCount + samp];
                    } else if (mMixedSamples.length < mMixedSampleCount + samp) {
                        mMixedSamples = Arrays.copyOf(mMixedSamples, mMixedSampleCount + samp);
                    }
                    for (int j = mMixedSampleCount; j < mMixedSampleCount + samp; j++) {
                        float sam = 0;
                        for (int k = 0; k < mSample.mChannels; k++) {
                            sam += buffer.get();
                        }
                        sam /= mSample.mChannels;
                        mMixedSamples[j] = sam;
                    }
                    mMixedSampleCount += samp;
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
            int samples = alGetBufferi(buf, AL_SIZE) / 4 / mSample.mChannels;
            mBaseOffset += samples;
            alDeleteBuffers(buf);
            System.arraycopy(mMixedSamples, samples, mMixedSamples, 0, mMixedSampleCount - samples);
            mMixedSampleCount -= samples;
        }
        return count;
    }

    @Override
    public void close() throws IOException {
        if (mSource != 0) {
            alDeleteSources(mSource);
            mSource = 0;
        }
        mSample.close();
    }
}
