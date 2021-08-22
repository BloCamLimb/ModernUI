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
import icyllis.modernui.math.FourierTransform;

import java.nio.channels.FileChannel;
import java.nio.file.Path;

public class SpectrumGraph {

    public WaveDecoder mWaveDecoder;

    private final float[] mAmplitudes = new float[60];

    private final FourierTransform mFFT;

    public int mSongLength;

    public SpectrumGraph() throws Exception {
        mWaveDecoder = new WaveDecoder(FileChannel.open(Path.of("F:", "5.wav")));
        mFFT = FourierTransform.create(1024, mWaveDecoder.mSampleRate);
        mFFT.setLogAverages(250, 14);
        mFFT.setWindowFunc(FourierTransform.BLACKMAN);
        mSongLength =
                (int) ((float) mWaveDecoder.mSamples.length / mWaveDecoder.mSampleRate * 1000);
        ModernUI.LOGGER.info("Average size: {}", mFFT.getAverageSize());
    }

    public void update(long time, long delta) {
        if (time < mSongLength) {
            int sampleStart = (int) (time / 1000f * mWaveDecoder.mSampleRate);
            mFFT.forward(mWaveDecoder.mSamples, sampleStart);

            int len = Math.min(mFFT.getAverageSize() - 4, mAmplitudes.length);
            for (int i = 0; i < len; i++) {
                float dec = mAmplitudes[i] - delta * 0.0012f * (mAmplitudes[i] + 0.03f);
                mAmplitudes[i] = Math.max(dec, mFFT.getAverage(i + 4) / 160);
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
