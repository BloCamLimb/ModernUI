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

package icyllis.modernui.test;

import icyllis.modernui.audio.WaveDecoder;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.math.FFT;
import icyllis.modernui.math.FourierTransform;

import java.nio.channels.FileChannel;
import java.nio.file.Path;

public class SpectrumGraph {

    public WaveDecoder mWaveDecoder;

    private final float[] mAmplitudes = new float[60];
    private final float[] mTempAmplitudes = new float[1024];

    private final FFT mFFT;

    public int mSongLength;

    public SpectrumGraph() throws Exception {
        mWaveDecoder = new WaveDecoder(FileChannel.open(Path.of("F:", "Rubia.wav")));
        mFFT = new FFT(mTempAmplitudes.length, mWaveDecoder.mSampleRate);
        //mFFT.linAverages(mAmplitudes.length + 1);
        mFFT.logAverages(250, 14);
        mSongLength = (int) ((float) mWaveDecoder.mSamples.length / mWaveDecoder.mSampleRate * 1000);
    }

    public void update(long time, long delta) {
        float[] temp = mTempAmplitudes;

        if (time < mSongLength) {
            int sampleStart = (int) (time / 1000f * mWaveDecoder.mSampleRate) - mTempAmplitudes.length;
            /*int sampleEnd = sampleStart + (int) (deltaMillis / 1000f * mWaveDecoder.mSampleRate);
            for (int i = sampleStart; i < sampleEnd; i++) {
                temp[(int) (mWaveDecoder.mSamples[i] * temp.length)] += 1;
            }*/
            System.arraycopy(mWaveDecoder.mSamples, sampleStart, temp, 0, mTempAmplitudes.length);
            mFFT.forward(temp);

            int len = Math.min(mFFT.avgSize() - 20, mAmplitudes.length);
            for (int i = 0; i < len; i++) {
                float dec = mAmplitudes[i] - delta * 0.0012f * (mAmplitudes[i] + 0.03f);
                mAmplitudes[i] = Math.max(dec, mFFT.getAvg(i + 2) / 160);
            }
        }
    }

    public void draw(Canvas canvas) {
        Paint paint = Paint.take();
        for (int i = 0; i < mAmplitudes.length; i++) {
            paint.setRGBA(100 + i * 2, 220 - i * 2, 240 - i * 4, 255);
            canvas.drawRect(320 + i * 16, 800 - mAmplitudes[i] * 300, 334 + i * 16, 800, paint);
        }
    }
}
