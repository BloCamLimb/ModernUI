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
import icyllis.modernui.math.FourierTransform;
import icyllis.modernui.math.MathUtil;
import icyllis.modernui.platform.RenderCore;

import java.nio.channels.FileChannel;
import java.nio.file.Path;

public class SpectrumGraph {

    public WaveDecoder mWaveDecoder;

    private final float[] mAmplitudes = new float[60];

    private final FourierTransform mFFT;

    public int mSongLength;

    public SpectrumGraph() throws Exception {
        mWaveDecoder = new WaveDecoder(FileChannel.open(Path.of("F:", "9.wav")));
        mFFT = FourierTransform.create(1024, mWaveDecoder.mSampleRate);
        mFFT.setLogAverages(250, 14);
        mFFT.setWindowFunc(FourierTransform.NONE);
        mSongLength = (int) ((float) mWaveDecoder.mSamples.length / mWaveDecoder.mSampleRate * 1000);
    }

    public void update(long time, long delta) {
        if (time < mSongLength) {
            int sampleStart = (int) (time / 1000f * mWaveDecoder.mSampleRate);
            mFFT.forward(mWaveDecoder.mSamples, sampleStart);

            int len = Math.min(mFFT.getAverageSize() - 5, mAmplitudes.length);
            int iOff = (int) (time / 200);
            for (int i = 0; i < len; i++) {
                float dec = mAmplitudes[i] - delta * 0.0012f * (mAmplitudes[i] + 0.03f);
                mAmplitudes[i] = Math.max(dec, mFFT.getAverage(((i + iOff) % len) + 5) / mFFT.getBandSize());
            }
        }
    }

    public void draw(Canvas canvas, float cx, float cy) {
        var paint = Paint.take();
        long time = RenderCore.timeMillis();
        float b = 1.5f + MathUtil.sin(time / 600f) / 2;
        paint.setRGBA(160, 155, 230, (int) (64 * b));
        paint.setSmoothRadius(100);
        paint.setStrokeWidth(200);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(cx, cy, 130, paint);
        paint.reset();
        for (int i = 0; i < mAmplitudes.length; i++) {
            // lines
            //paint.setRGBA(100 + i * 2, 220 - i * 2, 240 - i * 4, 255);
            //canvas.drawRect(320 + i * 16, 800 - mAmplitudes[i] * 300, 334 + i * 16, 800, paint);
            // circular
            float f = Math.abs((i + (int) (time / 100)) % mAmplitudes.length - (mAmplitudes.length - 1) / 2f)
                    / (mAmplitudes.length - 1) * b;
            paint.setRGBA(100 + (int) (f * 120), 220 - (int) (f * 130), 240 - (int) (f * 20), 255);
            canvas.rotate(-360f / mAmplitudes.length, cx, cy);
            canvas.drawRect(cx - 6, cy - 120 - mAmplitudes[i] * 300, cx + 6, cy - 120, paint);
        }
    }
}
