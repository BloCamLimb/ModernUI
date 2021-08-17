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

import icyllis.modernui.ModernUI;
import icyllis.modernui.audio.WaveDecoder;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Arrays;

public class WaveformGraph {

    private WaveDecoder mWaveDecoder;

    private float[] mAmplitudes = new float[120];
    private float[] mTempAmplitudes = new float[120];

    private long mTime;

    public WaveformGraph() throws Exception {
        mWaveDecoder = new WaveDecoder(FileChannel.open(Path.of("F:", "2.wav")));
    }

    public void update(long deltaMillis) {
        float[] temp = mTempAmplitudes;
        Arrays.fill(temp, 0);
        if (mTime + deltaMillis < mWaveDecoder.mSamples.length / mWaveDecoder.mSampleRate * 1000L) {
            int sampleStart = (int) (mTime / 1000f * mWaveDecoder.mSampleRate);
            int sampleEnd = sampleStart + (int) (deltaMillis / 1000f * mWaveDecoder.mSampleRate);
            for (int i = sampleStart; i < sampleEnd; i++) {
                temp[(int) (mWaveDecoder.mSamples[i] * temp.length)] += 1;
            }
            for (int i = 0; i < temp.length; i++) {
                float dec = mAmplitudes[i] - deltaMillis * 0.0008f * (mAmplitudes[i] + 0.03f);
                mAmplitudes[i] = Math.max(dec, temp[i] / deltaMillis / 10);
            }
        }
        mTime += deltaMillis;
    }

    public void draw(Canvas canvas) {
        Paint paint = Paint.take();
        paint.setRGBA(100, 220, 240, 255);
        for (int i = 0; i < mAmplitudes.length; i++) {
            canvas.drawRect(30 + i * 12, 600 - mAmplitudes[i] * 300, 40 + i * 12, 600, paint);
        }
    }
}
