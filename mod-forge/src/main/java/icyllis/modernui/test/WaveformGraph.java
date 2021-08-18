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

import java.nio.channels.FileChannel;
import java.nio.file.Path;

public class WaveformGraph {

    public WaveDecoder mWaveDecoder;

    private final float[] mAmplitudes = new float[120];
    private final float[] mTempAmplitudes = new float[1024];

    private final FFT mFFT;

    public int mSongLength;

    public WaveformGraph() throws Exception {
        mWaveDecoder = new WaveDecoder(FileChannel.open(Path.of("F:", "2.wav")));
        mFFT = new FFT(mTempAmplitudes.length, mWaveDecoder.mSampleRate);
        //mFFT.linAverages(mAmplitudes.length + 1);
        mFFT.logAverages(220, 24);
        mSongLength = (int) ((float) mWaveDecoder.mSamples.length / mWaveDecoder.mSampleRate * 1000);
    }

    public void update(long time, long delta) {
        float[] temp = mTempAmplitudes;

        if (time < mSongLength) {
            int sampleStart = (int) (time / 1000f * mWaveDecoder.mSampleRate) - mTempAmplitudes.length;
            if (sampleStart < 0)
                sampleStart = 0;
            /*int sampleEnd = sampleStart + (int) (deltaMillis / 1000f * mWaveDecoder.mSampleRate);
            for (int i = sampleStart; i < sampleEnd; i++) {
                temp[(int) (mWaveDecoder.mSamples[i] * temp.length)] += 1;
            }*/
            System.arraycopy(mWaveDecoder.mSamples, sampleStart, temp, 0, mTempAmplitudes.length);
            mFFT.forward(temp);

            int len = Math.min(mFFT.avgSize() - 24, mAmplitudes.length);
            for (int i = 0; i < len; i++) {
                float dec = mAmplitudes[i] - delta * 0.0008f * (mAmplitudes[i] + 0.03f);
                mAmplitudes[i] = Math.max(dec, mFFT.getAvg(i + 6) / delta / 10);
            }
        }
    }

    public void draw(Canvas canvas) {
        Paint paint = Paint.take();
        for (int i = 0; i < mAmplitudes.length; i++) {
            paint.setRGBA(100 + i, 220 - i, 240 - i * 2, 255);
            canvas.drawRect(81 + i * 12, 800 - mAmplitudes[i] * 300, 91 + i * 12, 800, paint);
        }
    }
}
