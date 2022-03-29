/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

import icyllis.modernui.audio.Track;
import icyllis.modernui.core.Core;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.math.FourierTransform;
import icyllis.modernui.math.MathUtil;

import javax.annotation.Nonnull;

public class SpectrumGraph {

    private final boolean mCircular;

    private final float[] mAmplitudes = new float[60];
    private final FourierTransform mFFT;
    private final int mHeight;

    public SpectrumGraph(Track track, boolean circular, int height) {
        mFFT = FourierTransform.create(1024, track.getSampleRate());
        mFFT.setLogAverages(250, 14);
        mFFT.setWindowFunc(FourierTransform.NONE);
        track.setAnalyzer(mFFT, f -> updateAmplitudes());
        mCircular = circular;
        mHeight = height;
    }

    public void updateAmplitudes() {
        int len = Math.min(mFFT.getAverageSize() - 5, mAmplitudes.length);
        long time = Core.timeMillis();
        int iOff;
        if (mCircular)
            iOff = (int) (time / 200);
        else
            iOff = 0;
        synchronized (mAmplitudes) {
            for (int i = 0; i < len; i++) {
                float va = mFFT.getAverage(((i + iOff) % len) + 5) / mFFT.getBandSize();
                mAmplitudes[i] = Math.max(mAmplitudes[i], va);
            }
        }
    }

    public void update(long delta) {
        int len = Math.min(mFFT.getAverageSize() - 5, mAmplitudes.length);
        synchronized (mAmplitudes) {
            for (int i = 0; i < len; i++) {
                mAmplitudes[i] = mAmplitudes[i] - delta * 0.002f * (mAmplitudes[i] + 0.03f);
            }
        }
    }

    public void draw(@Nonnull Canvas canvas, float cx, float cy) {
        var paint = Paint.take();
        if (mCircular) {
            long time = Core.timeMillis();
            float b = 1.5f + MathUtil.sin(time / 600f) / 2;
            paint.setRGBA(160, 155, 230, (int) (64 * b));
            paint.setSmoothRadius(100);
            paint.setStrokeWidth(200);
            paint.setStyle(Paint.STROKE);
            canvas.drawCircle(cx, cy, 130, paint);
            paint.reset();
            for (int i = 0; i < mAmplitudes.length; i++) {
                float f = Math.abs((i + (int) (time / 100)) % mAmplitudes.length - (mAmplitudes.length - 1) / 2f)
                        / (mAmplitudes.length - 1) * b;
                paint.setRGBA(100 + (int) (f * 120), 220 - (int) (f * 130), 240 - (int) (f * 20), 255);
                canvas.rotate(-360f / mAmplitudes.length, cx, cy);
                canvas.drawRect(cx - 6, cy - 120 - mAmplitudes[i] * mHeight, cx + 6, cy - 120, paint);
            }
        } else {
            for (int i = 0; i < mAmplitudes.length; i++) {
                paint.setRGBA(100 + i * 2, 220 - i * 2, 240 - i * 4, 255);
                canvas.drawRect(cx - 479 + i * 16, cy - mAmplitudes[i] * mHeight, cx - 465 + i * 16, cy, paint);
            }
        }
    }
}
