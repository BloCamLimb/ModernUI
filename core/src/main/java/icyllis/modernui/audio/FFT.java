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

package icyllis.modernui.audio;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Provides Fast Fourier Transform. It is an efficient way to calculate the Complex Discrete Fourier
 * Transform, which is commonly used to analyze the spectrum of an audio buffer.
 */
public class FFT {

    /**
     * Constants indicating which window should be used on sample buffers.
     *
     * @see #setWindowFunc(int)
     */
    public static final int NONE = 0;
    public static final int BARTLETT = 1;
    public static final int HAMMING = 2;
    public static final int HANN = 3;
    public static final int BLACKMAN = 4;

    private static final int LINEAR = 1;
    private static final int LOG = 2;

    private final int mTimeSize;
    private final int mSampleRate;
    private final float mBandWidth;

    private final float[] mReal;
    private final float[] mImag;
    private final float[] mSpectrum;

    // bit reversing table
    private final int[] mReverse;

    @Nullable
    private float[] mWindow;

    @Nullable
    private float[] mAverages;
    private int mAverageMode;
    private int mOctaves;
    private int mAvgPerOctave;

    private FFT(int timeSize, int sampleRate) {
        mTimeSize = timeSize;
        mSampleRate = sampleRate;
        mBandWidth = (float) sampleRate / timeSize;

        mReal = new float[timeSize];
        mImag = new float[timeSize];
        mSpectrum = new float[(timeSize >> 1) + 1];

        int[] reverse = new int[timeSize];
        for (int limit = 1, bit = timeSize >> 1; limit < timeSize; limit <<= 1, bit >>= 1) {
            for (int i = 0; i < limit; i++) {
                reverse[i + limit] = reverse[i] + bit;
            }
        }
        mReverse = reverse;
    }

    /**
     * Creates an FFT that will accept sample buffers that are <code>timeSize</code> long and have
     * been recorded with a sample rate of <code>sampleRate</code>. <code>timeSize</code>
     * <em>must</em> be a power of two.
     *
     * @param timeSize   the length of the sample buffers you will be analyzing
     * @param sampleRate the sample rate of the audio you will be analyzing
     */
    @Nonnull
    public static FFT create(int timeSize, int sampleRate) {
        if (timeSize > 0 & (timeSize & (timeSize - 1)) != 0) {
            throw new IllegalArgumentException("timeSize must be a power of two");
        }
        return new FFT(timeSize, sampleRate);
    }

    /**
     * Performs a forward transform on values in <code>samples</code>.
     *
     * @param samples the buffer of samples
     * @param offset  the offset to start at in the buffer, the exceeded part of next timeSize()
     *                samples from the starting index in the buffer will be filled with zeros.
     */
    public void forward(@Nonnull float[] samples, int offset) {
        if (offset < 0 || offset >= samples.length) {
            throw new IllegalArgumentException();
        }
        // copy samples to real/imag in bit-reversed order
        // the imag array is filled with zeros
        for (int i = 0; i < mTimeSize; i++) {
            int j = mReverse[i];
            if (j + offset >= samples.length) {
                mReal[i] = 0.0f;
            } else {
                float sample = samples[j + offset];
                // window the data in samples
                if (mWindow != null) {
                    sample *= mWindow[j];
                }
                mReal[i] = sample;
            }
            mImag[i] = 0.0f;
        }
        // perform the fft
        fft();
        // fill the spectrum buffer with amplitudes
        fillSpectrum();
    }

    /**
     * Performs a forward transform on the passed buffers.
     *
     * @param real   the real part of the time domain signal to transform
     * @param imag   the imaginary part of the time domain signal to transform
     * @param offset the offset to start at in the buffer, the exceeded part of next timeSize() *
     *               samples from the starting index in the buffer will be filled with zeros.
     */
    public void forward(@Nonnull float[] real, @Nonnull float[] imag, int offset) {
        if (offset < 0 || real.length != imag.length || offset >= real.length) {
            throw new IllegalArgumentException();
        }
        // copy samples to real/imag in bit-reversed order
        // the imag array is filled with zeros
        for (int i = 0; i < mTimeSize; i++) {
            int j = mReverse[i];
            if (j + offset >= real.length) {
                mReal[i] = 0.0f;
                mImag[i] = 0.0f;
            } else {
                mReal[i] = real[j + offset];
                mImag[i] = imag[j + offset];
            }
        }
        fft();
        fillSpectrum();
    }

    // performs an in-place fft on the data in the real and imag arrays
    // bit reversing is not necessary as the data will already be bit reversed
    private void fft() {
        for (int halfSize = 1; halfSize < mTimeSize; halfSize <<= 1) {
            double k = -Math.PI / halfSize;
            float phaseShiftStepR = (float) Math.cos(k);
            float phaseShiftStepI = (float) Math.sin(k);
            // current phase shift
            float currentPhaseShiftR = 1.0f;
            float currentPhaseShiftI = 0.0f;
            for (int fftStep = 0; fftStep < halfSize; fftStep++) {
                for (int i = fftStep; i < mTimeSize; i += halfSize << 1) {
                    int off = i + halfSize;
                    float tr =
                            (currentPhaseShiftR * mReal[off]) - (currentPhaseShiftI * mImag[off]);
                    float ti =
                            (currentPhaseShiftR * mImag[off]) + (currentPhaseShiftI * mReal[off]);
                    mReal[off] = mReal[i] - tr;
                    mImag[off] = mImag[i] - ti;
                    mReal[i] += tr;
                    mImag[i] += ti;
                }
                float tmpR = currentPhaseShiftR;
                currentPhaseShiftR =
                        (tmpR * phaseShiftStepR) - (currentPhaseShiftI * phaseShiftStepI);
                currentPhaseShiftI =
                        (tmpR * phaseShiftStepI) + (currentPhaseShiftI * phaseShiftStepR);
            }
        }
    }

    // fill the spectrum array with the amps of the data in real and imag
    // used so that this class can handle creating the average array
    // and also do spectrum shaping if necessary
    private void fillSpectrum() {
        for (int i = 0; i < mSpectrum.length; i++) {
            mSpectrum[i] = (float) Math.sqrt(mReal[i] * mReal[i] + mImag[i] * mImag[i]);
        }

        if (mAverages == null) {
            return;
        }
        if (mAverageMode == LINEAR) {
            int avgWidth = mSpectrum.length / mAverages.length;
            for (int i = 0; i < mAverages.length; i++) {
                float avg = 0;
                int j;
                for (j = 0; j < avgWidth; j++) {
                    int offset = j + i * avgWidth;
                    if (offset < mSpectrum.length) {
                        avg += mSpectrum[offset];
                    } else {
                        break;
                    }
                }
                avg /= j + 1;
                mAverages[i] = avg;
            }
        } else if (mAverageMode == LOG) {
            for (int i = 0; i < mOctaves; i++) {
                float lowFreq, hiFreq, freqStep;
                if (i == 0) {
                    lowFreq = 0;
                } else {
                    lowFreq = (mSampleRate / 2f) / (1L << mOctaves - i);
                }
                hiFreq = (mSampleRate / 2f) / (1L << mOctaves - i - 1);
                freqStep = (hiFreq - lowFreq) / mAvgPerOctave;
                float f = lowFreq;
                for (int j = 0; j < mAvgPerOctave; j++) {
                    int offset = j + i * mAvgPerOctave;
                    mAverages[offset] = getAverage(f, f + freqStep);
                    f += freqStep;
                }
            }
        }
    }

    /**
     * Sets the object to not compute averages.
     */
    public void setNoAverages() {
        mAverages = null;
        mAverageMode = NONE;
    }

    /**
     * Sets the number of averages used when computing the spectrum and spaces the averages in a
     * linear manner. In other words, each average band will be
     * <code>specSize() / numAvg</code> bands wide.
     *
     * @param num how many averages to compute
     */
    public void setLinearAverages(int num) {
        if (num > mSpectrum.length / 2) {
            throw new IllegalArgumentException("The number of averages for this transform can be " +
                    "at most " + mSpectrum.length / 2 + ".");
        }
        mAverages = new float[num];
        mAverageMode = LINEAR;
    }

    /**
     * Sets the number of averages used when computing the spectrum based on the minimum bandwidth
     * for an octave and the number of bands per octave. For example, with audio that has a sample
     * rate of 44100 Hz,
     * <code>logAverages(11, 1)</code> will result in 12 averages, each
     * corresponding to an octave, the first spanning 0 to 11 Hz. To ensure that each octave band is
     * a full octave, the number of octaves is computed by dividing the Nyquist frequency by two,
     * and then the result of that by two, and so on. This means that the actual bandwidth of the
     * lowest octave may not be exactly the value specified.
     *
     * @param minBandwidth   the minimum bandwidth used for an octave
     * @param bandsPerOctave how many bands to split each octave into
     */
    public void setLogAverages(int minBandwidth, int bandsPerOctave) {
        float nyq = mSampleRate / 2f;
        mOctaves = 1;
        while ((nyq /= 2) > minBandwidth) {
            mOctaves++;
        }
        mAvgPerOctave = bandsPerOctave;
        mAverages = new float[mOctaves * bandsPerOctave];
        mAverageMode = LOG;
    }

    /**
     * Sets the window to use on the samples before taking the forward transform. If an invalid
     * window is asked for, an error will be reported and the current window will not be changed.
     * {@link #NONE} is the default, equivalent to the rectangular window.
     *
     * @param func window function, such as {@link #HAMMING}
     * @throws IllegalArgumentException invalid window function
     */
    public void setWindowFunc(int func) {
        if (func == NONE) {
            mWindow = null;
            return;
        }
        float[] window = mWindow;
        if (window == null) {
            window = new float[mTimeSize];
        }
        final int n = window.length - 1;
        switch (func) {
            case HAMMING:
                for (int i = 0; i <= n; i++) {
                    window[i] = (float) (0.54f - 0.46f * Math.cos(2 * Math.PI * i / n));
                }
                break;

            case BLACKMAN:
                for (int i = 0; i <= n; i++) {
                    window[i] = (float) (0.42f - 0.5f * Math.cos(2 * Math.PI * i / n)
                            + 0.08f * Math.cos(4 * Math.PI * i / n));
                }
                break;

            case HANN:
                for (int i = 0; i <= n; i++) {
                    final float sin = (float) Math.sin(Math.PI * i / n);
                    window[i] = sin * sin;
                }
                break;

            case BARTLETT:
                for (int i = 0; i <= n; i++) {
                    window[i] = 1 - Math.abs((i * 2 - n) / n);
                }
                break;

            default:
                throw new IllegalArgumentException("Unrecognized window function " + func);
        }
        mWindow = window;
    }

    /**
     * Returns the length of the time domain signal expected by this transform.
     *
     * @return the length of the time domain signal expected by this transform
     */
    public int getTimeSize() {
        return mTimeSize;
    }

    /**
     * Returns the sample rate of signal samples expected by this transform.
     *
     * @return the sample rate of signal samples expected by this transform
     */
    public int getSampleRate() {
        return mSampleRate;
    }

    /**
     * Returns the size of the spectrum created by this transform. In other words, the number of
     * frequency bands produced by this transform. This is typically equal to <code>timeSize()/2 +
     * 1</code>, see above for an explanation.
     *
     * @return the size of the spectrum
     */
    public int getBandSize() {
        return mSpectrum.length;
    }

    /**
     * Returns the width of each frequency band in the spectrum (in Hz). It should be noted that the
     * bandwidth of the first and last frequency bands is half as large as the value returned by
     * this function.
     *
     * @return the width of each frequency band in Hz.
     */
    public float getBandWidth() {
        return mBandWidth;
    }

    /**
     * Returns the amplitude of the requested frequency band.
     *
     * @param i the index of a frequency band
     * @return the amplitude of the requested frequency band
     */
    public float getBand(int i) {
        return mSpectrum[i];
    }

    /**
     * Sets the amplitude of the <code>i<sup>th</sup></code> frequency band to
     * <code>a</code>. You can use this to shape the spectrum before using
     * <code>inverse()</code>.
     *
     * @param band      the frequency band to modify
     * @param amplitude the new amplitude
     */
    public void setBand(int band, float amplitude) {
        if (amplitude < 0) {
            throw new IllegalArgumentException("Can't set a frequency band to a negative value.");
        }
        if (mReal[band] == 0 && mImag[band] == 0) {
            mReal[band] = amplitude;
            mSpectrum[band] = amplitude;
        } else {
            mReal[band] /= mSpectrum[band];
            mImag[band] /= mSpectrum[band];
            mSpectrum[band] = amplitude;
            mReal[band] *= mSpectrum[band];
            mImag[band] *= mSpectrum[band];
        }
        if (band != 0 && band != mTimeSize / 2) {
            mReal[mTimeSize - band] = mReal[band];
            mImag[mTimeSize - band] = -mImag[band];
        }
    }

    /**
     * Scales the amplitude of the <code>i<sup>th</sup></code> frequency band by <code>s</code>. You
     * can use this to shape the spectrum before using
     * <code>inverse()</code>.
     *
     * @param band  the frequency band to modify
     * @param scale the scaling factor
     */
    public void scaleBand(int band, float scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("Can't scale a frequency band by a negative value.");
        }
        if (mSpectrum[band] != 0) {
            mReal[band] /= mSpectrum[band];
            mImag[band] /= mSpectrum[band];
            mSpectrum[band] *= scale;
            mReal[band] *= mSpectrum[band];
            mImag[band] *= mSpectrum[band];
        }
        if (band != 0 && band != mTimeSize / 2) {
            mReal[mTimeSize - band] = mReal[band];
            mImag[mTimeSize - band] = -mImag[band];
        }
    }

    /**
     * Returns the index of the frequency band that contains the requested frequency.
     *
     * @param freq the frequency you want the index for (in Hz)
     * @return the index of the frequency band that contains freq
     */
    public int freqToIndex(float freq) {
        // special case: freq is lower than the bandwidth of spectrum[0]
        if (freq < getBandWidth() / 2)
            return 0;
        // special case: freq is within the bandwidth of spectrum[spectrum.length - 1]
        if (freq > (mSampleRate - getBandWidth()) / 2)
            return mSpectrum.length - 1;
        // all other cases
        float fraction = freq / mSampleRate;
        return Math.round(mTimeSize * fraction);
    }

    /**
     * Returns the middle frequency of the i<sup>th</sup> band.
     *
     * @param i the index of the band you want to middle frequency of
     */
    public float indexToFreq(int i) {
        float bw = getBandWidth();
        // special case: the width of the first bin is half that of the others.
        //               so the center frequency is a quarter of the way.
        if (i == 0)
            return bw * 0.25f;
        // special case: the width of the last bin is half that of the others.
        if (i == mSpectrum.length - 1) {
            float lastBinBeginFreq = (mSampleRate - bw) / 2;
            float binHalfWidth = bw * 0.25f;
            return lastBinBeginFreq + binHalfWidth;
        }
        // the center frequency of the ith band is simply i*bw
        // because the first band is half the width of all others.
        // treating it as if it wasn't offsets us to the middle
        // of the band.
        return i * bw;
    }

    /**
     * Returns the center frequency of the i<sup>th</sup> average band.
     *
     * @param i which average band you want the center frequency of.
     */
    public float getAverageCenterFrequency(int i) {
        if (mAverages == null) {
            return 0;
        }
        if (mAverageMode == LINEAR) {
            // an average represents a certain number of bands in the spectrum
            int avgWidth = mSpectrum.length / mAverages.length;
            // the "center" bin of the average, this is fudgy.
            int centerBinIndex = i * avgWidth + avgWidth / 2;
            return indexToFreq(centerBinIndex);
        } else if (mAverageMode == LOG) {
            // which "octave" is this index in?
            int octave = i / mAvgPerOctave;
            // which band within that octave is this?
            int offset = i % mAvgPerOctave;
            float lowFreq, hiFreq, freqStep;
            // figure out the low frequency for this octave
            if (octave == 0) {
                lowFreq = 0;
            } else {
                lowFreq = (mSampleRate / 2f) / (1L << mOctaves - octave);
            }
            // and the high frequency for this octave
            hiFreq = (mSampleRate / 2f) / (1L << mOctaves - octave - 1);
            // each average band within the octave will be this big
            freqStep = (hiFreq - lowFreq) / mAvgPerOctave;
            // figure out the low frequency of the band we care about
            float f = lowFreq + offset * freqStep;
            // the center of the band will be the low plus half the width
            return f + freqStep / 2;
        }
        return 0;
    }

    /**
     * Gets the amplitude of the requested frequency in the spectrum.
     *
     * @param freq the frequency in Hz
     * @return the amplitude of the frequency in the spectrum
     */
    public float getFrequency(float freq) {
        return getBand(freqToIndex(freq));
    }

    /**
     * Sets the amplitude of the requested frequency in the spectrum to
     * <code>a</code>.
     *
     * @param freq      the frequency in Hz
     * @param amplitude the new amplitude
     */
    public void setFrequency(float freq, float amplitude) {
        setBand(freqToIndex(freq), amplitude);
    }

    /**
     * Scales the amplitude of the requested frequency by <code>a</code>.
     *
     * @param freq  the frequency in Hz
     * @param scale the scaling factor
     */
    public void scaleFrequency(float freq, float scale) {
        scaleBand(freqToIndex(freq), scale);
    }

    /**
     * Returns the number of averages currently being calculated.
     *
     * @return the length of the averages array
     */
    public int getAverageSize() {
        return mAverages == null ? 0 : mAverages.length;
    }

    /**
     * Gets the value of the <code>i<sup>th</sup></code> average.
     *
     * @param i the average you want the value of
     * @return the value of the requested average band
     */
    public float getAverage(int i) {
        return mAverages == null ? 0 : mAverages[i];
    }

    /**
     * Calculate the average amplitude of the frequency band bounded by
     * <code>lowFreq</code> and <code>hiFreq</code>, inclusive.
     *
     * @param lowFreq the lower bound of the band
     * @param hiFreq  the upper bound of the band
     * @return the average of all spectrum values within the bounds
     */
    public float getAverage(float lowFreq, float hiFreq) {
        int lowBound = freqToIndex(lowFreq);
        int hiBound = freqToIndex(hiFreq);
        float avg = 0;
        for (int i = lowBound; i <= hiBound; i++) {
            avg += mSpectrum[i];
        }
        avg /= (hiBound - lowBound + 1);
        return avg;
    }
}
