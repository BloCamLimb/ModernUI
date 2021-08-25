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

import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;

//TODO WIP
public class AudioManager {

    private static final AudioManager instance = new AudioManager();

    private long mDevice;
    private long mContext;

    private int mSource;

    private AudioManager() {
    }

    public static AudioManager getInstance() {
        return instance;
    }

    public void init() {
        mDevice = ALC10.nalcOpenDevice(MemoryUtil.NULL);
        ALCCapabilities alcCapabilities = ALC.createCapabilities(mDevice);
        mContext = ALC10.nalcCreateContext(mDevice, MemoryUtil.NULL);
        ALC10.alcMakeContextCurrent(mContext);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
        if (alCapabilities.AL_EXT_source_distance_model) {
            AL10.alEnable(EXTSourceDistanceModel.AL_SOURCE_DISTANCE_MODEL);
        }
    }

    public void play(@Nonnull WaveDecoder waveDecoder) {
        mSource = AL10.alGenSources();
        int mBuffer = AL10.alGenBuffers();
        AL10.alBufferData(mBuffer, AL10.AL_FORMAT_STEREO16, waveDecoder.mData, waveDecoder.mSampleRate);
        AL10.alSourcei(mSource, AL10.AL_BUFFER, mBuffer);
        AL10.alSourcef(mSource, AL10.AL_GAIN, 0.75f);
        AL10.alSourcePlay(mSource);
    }

    public float getTime() {
        if (mSource == 0) {
            return 0;
        }
        return AL11.alGetSourcef(mSource, AL11.AL_SEC_OFFSET);
    }

    public void close() {
        ALC11.alcCloseDevice(mDevice);
        ALC11.alcDestroyContext(mContext);
    }
}
