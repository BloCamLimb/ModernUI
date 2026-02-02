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

import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.Buffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.function.Consumer;

import static org.lwjgl.openal.AL11.*;
import static org.lwjgl.openal.SOFTDirectChannels.AL_DIRECT_CHANNELS_SOFT;

public class Track implements AutoCloseable {

    // double buffering
    private static final int BUFFER_COUNT = 2;

    private int mSource;

    private final SoundSample mSample;
    private int mBaseSampleOffset;

    private FFT mFFT;
    private Consumer<FFT> mFFTCallback;

    private float[] mMixedSamples;
    private int mMixedSampleCount;

    private int[] mBuffers = new int[BUFFER_COUNT];

    private ShortBuffer mClientBuffer;

    private static final int STATE_IDLE = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_PAUSED = 2;

    private int mClientState = STATE_IDLE;

    public Track(@Nonnull SoundSample sample) {
        mSample = sample;
        mSource = alGenSources();
        alSourcef(mSource, AL_GAIN, 1.0f);
        alSourcei(mSource, AL_DIRECT_CHANNELS_SOFT, AL_TRUE);

        // 0.5 seconds
        final int targetSamples =
                mSample.getChannels() * mSample.getSampleRate() / 2;
        mClientBuffer = MemoryUtil.memAllocShort(targetSamples);

        alGenBuffers(mBuffers);

        AudioManager.getInstance().addTrack(this);
    }

    public boolean isPlaying() {
        return mClientState == STATE_PLAYING;
    }

    public void play() {
        if (mSource != 0 && alGetSourcei(mSource, AL_SOURCE_STATE) != AL_PLAYING) {
            if (mClientState == STATE_IDLE) {
                if (mBaseSampleOffset != 0) {
                    mSample.seek(0);
                    mBaseSampleOffset = 0;
                    int count = alGetSourcei(mSource, AL_BUFFERS_QUEUED);
                    while (count-- != 0) {
                        alSourceUnqueueBuffers(mSource);
                    }
                    mMixedSampleCount = 0;
                }
                for (int i = 0; i < BUFFER_COUNT; i++) {
                    forward(mBuffers[i]);
                }
            }
            alSourcePlay(mSource);
            mClientState = STATE_PLAYING;
        }
    }

    public void pause() {
        if (mSource != 0) {
            alSourcePause(mSource);
            mClientState = STATE_PAUSED;
        }
    }

    public void setPosition(float x, float y, float z) {
        alSourcefv(mSource, AL_POSITION, new float[]{x, y, z});
    }

    public void setGain(float gain) {
        alSourcef(mSource, AL_GAIN, gain);
    }

    public float getTime() {
        if (mSource == 0) {
            return 0;
        }
        return ((float) mBaseSampleOffset / mSample.getSampleRate()) + alGetSourcef(mSource, AL_SEC_OFFSET);
    }

    public float getLength() {
        return mSample.getTotalLength();
    }

    public int getSampleRate() {
        return mSample.getSampleRate();
    }

    public boolean seek(int sampleOffset) {
        if (mSample.seek(sampleOffset)) {
            alSourceStop(mSource);
            swapBuffers(false);
            mBaseSampleOffset = sampleOffset;
            if (mClientState == STATE_PLAYING) {
                play();
            }
            return true;
        }
        return false;
    }

    public boolean seekToSeconds(float seconds) {
        return seek((int) (seconds * getSampleRate()));
    }

    private int swapBuffers(boolean onlyProcessed) {
        int count = alGetSourcei(mSource,
                onlyProcessed ? AL_BUFFERS_PROCESSED : AL_BUFFERS_QUEUED);
        for (int i = 0; i < count; i++) {
            int buf = alSourceUnqueueBuffers(mSource);
            int samplesPerChannel = alGetBufferi(buf, AL_SIZE) / 2 / mSample.getChannels();
            mBaseSampleOffset += samplesPerChannel;
            System.arraycopy(mMixedSamples, samplesPerChannel,
                    mMixedSamples, 0,
                    mMixedSampleCount - samplesPerChannel);
            mMixedSampleCount -= samplesPerChannel;
            int forwardedSamples = forward(buf);
            if (forwardedSamples == 0) {
                if (i == 0 && count == 1) {
                    mClientState = STATE_IDLE;
                }
                return i + 1;
            }
        }
        return count;
    }

    // Audio-Thread
    public void tick() {
        if (mClientState == STATE_PLAYING) {
            int count = swapBuffers(true);
            if (count == BUFFER_COUNT) {
                play();
            }
            if (mFFT != null) {
                int offset = alGetSourcei(mSource, AL_SAMPLE_OFFSET);
                mFFT.forward(mMixedSamples,
                        offset);
                if (mFFTCallback != null) {
                    mFFTCallback.accept(mFFT);
                }
            }
        }
    }

    public void setAnalyzer(@Nullable FFT fft, @Nullable Consumer<FFT> callback) {
        if (fft != null && fft.getSampleRate() != mSample.getSampleRate()) {
            throw new IllegalArgumentException("Mismatched sample rate");
        }
        mFFT = fft;
        mFFTCallback = callback;
    }

    private int forward(int buf) {
        final ShortBuffer buffer = mClientBuffer;
        final int channels = mSample.getChannels();
        int samples = 0;
        final int maxSamples = buffer.capacity();
        while (samples < maxSamples) {
            buffer.position(samples);
            int samplesPerChannel = mSample.getSamplesShortInterleaved(buffer);
            if (samplesPerChannel == 0) {
                break;
            }
            samples += samplesPerChannel * channels;
        }
        if (samples != 0) {
            buffer.position(0);
            buffer.limit(samples);
            alBufferData(buf,
                    channels == 1
                            ? AL_FORMAT_MONO16
                            : AL_FORMAT_STEREO16,
                    buffer,
                    mSample.getSampleRate()
            );
            buffer.limit(maxSamples);
            alSourceQueueBuffers(mSource, buf);

            int samplesPerChannel = samples / channels;
            if (mMixedSamples == null) {
                mMixedSamples = new float[mMixedSampleCount + samplesPerChannel];
            } else if (mMixedSamples.length < mMixedSampleCount + samplesPerChannel) {
                mMixedSamples = Arrays.copyOf(mMixedSamples, mMixedSampleCount + samplesPerChannel);
            }
            for (int j = mMixedSampleCount; j < mMixedSampleCount + samplesPerChannel; j++) {
                float mixedSample = 0;
                for (int k = 0; k < channels; k++) {
                    mixedSample += SoundStream.s16_to_f(buffer.get());
                }
                mixedSample /= channels;
                mMixedSamples[j] = mixedSample;
            }
            mMixedSampleCount += samplesPerChannel;
        }
        return samples;
    }

    @Override
    public void close() {
        AudioManager.getInstance().removeTrack(this);
        MemoryUtil.memFree((Buffer) mClientBuffer);
        mClientBuffer = null;
        if (mBuffers != null) {
            alDeleteBuffers(mBuffers);
            mBuffers = null;
        }
        if (mSource != 0) {
            alDeleteSources(mSource);
            mSource = 0;
        }
        mSample.close();
    }
}
