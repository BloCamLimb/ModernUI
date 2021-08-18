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

package icyllis.modernui.math;

public class FFT extends FourierTransform {

    /**
     * Constructs an FFT that will accept sample buffers that are
     * <code>timeSize</code> long and have been recorded with a sample rate of
     * <code>sampleRate</code>. <code>timeSize</code> <em>must</em> be a
     * power of two. This will throw an exception if it is not.
     *
     * @param timeSize
     *          the length of the sample buffers you will be analyzing
     * @param sampleRate
     *          the sample rate of the audio you will be analyzing
     */
    public FFT(int timeSize, float sampleRate)
    {
        super(timeSize, sampleRate);
        if ((timeSize & (timeSize - 1)) != 0)
            throw new IllegalArgumentException(
                    "FFT: timeSize must be a power of two.");
        buildReverseTable();
        buildTrigTables();
    }

    protected void allocateArrays()
    {
        spectrum = new float[timeSize / 2 + 1];
        real = new float[timeSize];
        imag = new float[timeSize];
    }

    public void scaleBand(int i, float s)
    {
        if (s < 0)
        {
            throw new IllegalArgumentException("Can't scale a frequency band by a negative value.");
        }
        if (spectrum[i] != 0)
        {
            real[i] /= spectrum[i];
            imag[i] /= spectrum[i];
            spectrum[i] *= s;
            real[i] *= spectrum[i];
            imag[i] *= spectrum[i];
        }
        if (i != 0 && i != timeSize / 2)
        {
            real[timeSize - i] = real[i];
            imag[timeSize - i] = -imag[i];
        }
    }

    public void setBand(int i, float a)
    {
        if (a < 0)
        {
            throw new IllegalArgumentException("Can't set a frequency band to a negative value.");
        }
        if (real[i] == 0 && imag[i] == 0)
        {
            real[i] = a;
            spectrum[i] = a;
        }
        else
        {
            real[i] /= spectrum[i];
            imag[i] /= spectrum[i];
            spectrum[i] = a;
            real[i] *= spectrum[i];
            imag[i] *= spectrum[i];
        }
        if (i != 0 && i != timeSize / 2)
        {
            real[timeSize - i] = real[i];
            imag[timeSize - i] = -imag[i];
        }
    }

    // performs an in-place fft on the data in the real and imag arrays
    // bit reversing is not necessary as the data will already be bit reversed
    private void fft()
    {
        for (int halfSize = 1; halfSize < real.length; halfSize *= 2)
        {
            // float k = -(float)Math.PI/halfSize;
            // phase shift step
            // float phaseShiftStepR = (float)Math.cos(k);
            // float phaseShiftStepI = (float)Math.sin(k);
            // using lookup table
            float phaseShiftStepR = cos(halfSize);
            float phaseShiftStepI = sin(halfSize);
            // current phase shift
            float currentPhaseShiftR = 1.0f;
            float currentPhaseShiftI = 0.0f;
            for (int fftStep = 0; fftStep < halfSize; fftStep++)
            {
                for (int i = fftStep; i < real.length; i += 2 * halfSize)
                {
                    int off = i + halfSize;
                    float tr = (currentPhaseShiftR * real[off]) - (currentPhaseShiftI * imag[off]);
                    float ti = (currentPhaseShiftR * imag[off]) + (currentPhaseShiftI * real[off]);
                    real[off] = real[i] - tr;
                    imag[off] = imag[i] - ti;
                    real[i] += tr;
                    imag[i] += ti;
                }
                float tmpR = currentPhaseShiftR;
                currentPhaseShiftR = (tmpR * phaseShiftStepR) - (currentPhaseShiftI * phaseShiftStepI);
                currentPhaseShiftI = (tmpR * phaseShiftStepI) + (currentPhaseShiftI * phaseShiftStepR);
            }
        }
    }

    public void forward(float[] buffer)
    {
        if (buffer.length != timeSize)
        {
            throw new IllegalArgumentException("FFT.forward: The length of the passed sample buffer must be equal to timeSize().");
        }
        doWindow(buffer);
        // copy samples to real/imag in bit-reversed order
        bitReverseSamples(buffer);
        // perform the fft
        fft();
        // fill the spectrum buffer with amplitudes
        fillSpectrum();
    }

    /**
     * Performs a forward transform on the passed buffers.
     *
     * @param buffReal the real part of the time domain signal to transform
     * @param buffImag the imaginary part of the time domain signal to transform
     */
    public void forward(float[] buffReal, float[] buffImag)
    {
        if (buffReal.length != timeSize || buffImag.length != timeSize)
        {
            throw new IllegalArgumentException("FFT.forward: The length of the passed buffers must be equal to timeSize().");
        }
        setComplex(buffReal, buffImag);
        bitReverseComplex();
        fft();
        fillSpectrum();
    }

    public void inverse(float[] buffer)
    {
        if (buffer.length > real.length)
        {
            throw new IllegalArgumentException("FFT.inverse: the passed array's length must equal FFT.timeSize().");
        }
        // conjugate
        for (int i = 0; i < timeSize; i++)
        {
            imag[i] *= -1;
        }
        bitReverseComplex();
        fft();
        // copy the result in real into buffer, scaling as we do
        for (int i = 0; i < buffer.length; i++)
        {
            buffer[i] = real[i] / real.length;
        }
    }

    private int[] reverse;

    private void buildReverseTable()
    {
        int N = timeSize;
        reverse = new int[N];

        // set up the bit reversing table
        reverse[0] = 0;
        for (int limit = 1, bit = N / 2; limit < N; limit <<= 1, bit >>= 1)
            for (int i = 0; i < limit; i++)
                reverse[i + limit] = reverse[i] + bit;
    }

    // copies the values in the samples array into the real array
    // in bit reversed order. the imag array is filled with zeros.
    private void bitReverseSamples(float[] samples)
    {
        for (int i = 0; i < samples.length; i++)
        {
            real[i] = samples[reverse[i]];
            imag[i] = 0.0f;
        }
    }

    // bit reverse real[] and imag[]
    private void bitReverseComplex()
    {
        float[] revReal = new float[real.length];
        float[] revImag = new float[imag.length];
        for (int i = 0; i < real.length; i++)
        {
            revReal[i] = real[reverse[i]];
            revImag[i] = imag[reverse[i]];
        }
        real = revReal;
        imag = revImag;
    }

    // lookup tables

    private float[] sinlookup;
    private float[] coslookup;

    private float sin(int i)
    {
        return sinlookup[i];
    }

    private float cos(int i)
    {
        return coslookup[i];
    }

    private void buildTrigTables()
    {
        int N = timeSize;
        sinlookup = new float[N];
        coslookup = new float[N];
        for (int i = 0; i < N; i++)
        {
            sinlookup[i] = (float) Math.sin(-(float) Math.PI / i);
            coslookup[i] = (float) Math.cos(-(float) Math.PI / i);
        }
    }

    public static void fft(float[] x, float[] y) {
        int n = 256;
        int m = 8;

        int i, j, k, n1, n2, a;
        float c, s, e, t1, t2;

        // Bit-reverse
        j = 0;
        n2 = n / 2;
        for (i = 1; i < n - 1; i++) {
            n1 = n2;
            while (j >= n1) {
                j = j - n1;
                n1 = n1 / 2;
            }
            j = j + n1;

            if (i < j) {
                t1 = x[i];
                x[i] = x[j];
                x[j] = t1;
                t1 = y[i];
                y[i] = y[j];
                y[j] = t1;
            }
        }

        // FFT
        n1 = 0;
        n2 = 1;

        for (i = 0; i < m; i++) {
            n1 = n2;
            n2 = n2 + n2;
            a = 0;

            for (j = 0; j < n1; j++) {
                c = MathUtil.cos(a);
                s = MathUtil.sin(a);
                a += 1 << (m - i - 1);

                for (k = j; k < n; k = k + n2) {
                    t1 = c * x[k + n1] - s * y[k + n1];
                    t2 = s * x[k + n1] + c * y[k + n1];
                    x[k + n1] = x[k] - t1;
                    y[k + n1] = y[k] - t2;
                    x[k] = x[k] + t1;
                    y[k] = y[k] + t2;
                }
            }
        }
    }
}
