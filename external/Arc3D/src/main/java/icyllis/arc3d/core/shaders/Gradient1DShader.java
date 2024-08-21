/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core.shaders;

import icyllis.arc3d.core.*;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Base class for gradient colors that can be represented by a 1D function,
 * it generates one parameter for interpolation:
 * <pre>grad_layout(x,y) -> float</pre>
 * Such as linear gradient, radial gradient, angular gradient.
 */
public abstract class Gradient1DShader extends GradientShader {

    protected final Matrix mPtsToUnit;
    protected final int mTileMode;

    private final float[] mColors;
    private final float[] mPositions;
    private final int mColorCount;
    private final ColorSpace mColorSpace;
    private final int mInterpolation;
    final boolean mFirstStopIsImplicit;
    final boolean mLastStopIsImplicit;

    final boolean mColorsAreOpaque;

    protected Gradient1DShader(@Nonnull float[] inColors, @Nullable ColorSpace colorSpace,
                               @Nullable float[] inPositions, int inColorCount,
                               int tileMode, int interpolation,
                               @Nonnull Matrix ptsToUnit) {
        ptsToUnit.getType(); // Precache so reads are threadsafe.
        mPtsToUnit = ptsToUnit;

        assert inColorCount > 1;

        mColorSpace = colorSpace != null ? colorSpace : ColorSpace.get(ColorSpace.Named.SRGB);
        mInterpolation = interpolation;

        assert tileMode >= 0 && tileMode <= LAST_TILE_MODE;
        mTileMode = tileMode;

        /*  Note: we let the caller skip the first and/or last position.
            i.e. pos[0] = 0.3, pos[1] = 0.7
            In these cases, we insert entries to ensure that the final data
            will be bracketed by [0, 1].
            i.e. our_pos[0] = 0, our_pos[1] = 0.3, our_pos[2] = 0.7, our_pos[3] = 1

            Thus inColorCount (the caller's value, and colorCount (our value) may
            differ by up to 2. In the above example:
                inColorCount = 2
                colorCount = 4
         */
        int colorCount = inColorCount;

        boolean firstStopIsImplicit = false;
        boolean lastStopIsImplicit = false;

        // Check if we need to add in start and/or end position/colors
        if (inPositions != null) {
            firstStopIsImplicit = inPositions[0] > 0;
            lastStopIsImplicit = inPositions[inColorCount - 1] != 1;
            colorCount += (firstStopIsImplicit ? 1 : 0) + (lastStopIsImplicit ? 1 : 0);
        }

        float[] colors = new float[colorCount * 4];
        float[] positions = inPositions != null ? new float[colorCount] : null;

        {// Now copy over the colors, adding the duplicates at t=0 and t=1 as needed
            int colorIndex = 0;
            if (firstStopIsImplicit) {
                System.arraycopy(inColors, 0, colors, colorIndex, 4);
                colorIndex += 4;
            }
            System.arraycopy(inColors, 0, colors, colorIndex, inColorCount * 4);

            boolean colorsAreOpaque = true;
            for (int i = 0; i < inColorCount; i++) {
                colorsAreOpaque &= inColors[i * 4 + 3] == 1;
            }
            mColorsAreOpaque = colorsAreOpaque;

            if (lastStopIsImplicit) {
                colorIndex += inColorCount * 4;
                System.arraycopy(inColors, (inColorCount - 1) * 4, colors, colorIndex, 4);
            }
        }

        if (positions != null) {
            float prev = 0;
            int positionIndex = 0;
            positions[positionIndex++] = prev; // force the first pos to 0

            int startIndex = firstStopIsImplicit ? 0 : 1;
            int count = inColorCount + (lastStopIsImplicit ? 1 : 0);

            boolean uniformStops = true;
            final float uniformStep = inPositions[startIndex] - prev;
            for (int i = startIndex; i < count; i++) {
                // Pin the last value to 1.0, and make sure pos is monotonic.
                float curr = 1.0f;
                if (i != inColorCount) {
                    curr = MathUtil.pin(inPositions[i], prev, 1.0f);

                    if (curr == 1.0f && lastStopIsImplicit) {
                        lastStopIsImplicit = false;
                    }
                }

                uniformStops &= Math.abs(curr - prev - uniformStep) <= kDegenerateTolerance;

                positions[positionIndex++] = prev = curr;
            }

            if (uniformStops) {
                // If the stops are uniform, treat them as implicit.
                positions = null;
            } else {
                // Remove duplicate stops with more than two of the same stop,
                // keeping the leftmost and rightmost stop colors.
                // i.e.       0, 0, 0,   0.2, 0.2, 0.3, 0.3, 0.3, 1, 1
                // w/  clamp  0,    0,   0.2, 0.2, 0.3,      0.3, 1, 1
                // w/o clamp        0,   0.2, 0.2, 0.3,      0.3, 1
                int i = 0;
                int dedupedColorCount = 0;
                for (int j = 1; j <= colorCount; j++) {
                    // We can compare the current positions at i and j since once these fPosition
                    // values are overwritten, our i and j pointers will be past the overwritten values.
                    if (j == colorCount || positions[i] != positions[j]) {
                        boolean dupStop = j - i > 1;

                        // Ignore the leftmost stop (i) if it is a non-clamp tilemode with
                        // a duplicate stop on t = 0.
                        boolean ignoreLeftmost = dupStop && tileMode != TILE_MODE_CLAMP
                                && positions[i] == 0;
                        if (!ignoreLeftmost) {
                            positions[dedupedColorCount] = positions[i];
                            System.arraycopy(colors, i * 4, colors, dedupedColorCount * 4, 4);
                            dedupedColorCount++;
                        }

                        // Include the rightmost stop (j-1) only if the stop has a duplicate,
                        // ignoring the rightmost stop if it is a non-clamp tilemode with t = 1.
                        boolean ignoreRightmost = tileMode != TILE_MODE_CLAMP
                                && positions[j - 1] == 1;
                        if (dupStop && !ignoreRightmost) {
                            positions[dedupedColorCount] = positions[j - 1];
                            System.arraycopy(colors, (j - 1) * 4, colors, dedupedColorCount * 4, 4);
                            dedupedColorCount++;
                        }
                        i = j;
                    }
                }
                colorCount = dedupedColorCount;
            }
        }

        mColors = colors;
        mPositions = positions;
        mColorCount = colorCount;

        mFirstStopIsImplicit = firstStopIsImplicit;
        mLastStopIsImplicit = lastStopIsImplicit;
    }

    @VisibleForTesting
    public float[] getColors() {
        return mColors;
    }

    @VisibleForTesting
    public float[] getPositions() {
        return mPositions;
    }

    public float getPos(int i) {
        assert i < mColorCount;
        return mPositions != null ? mPositions[i] : (float) i / (mColorCount - 1);
    }

    @Nonnull
    public ColorSpace getColorSpace() {
        return mColorSpace;
    }

    public int getColorCount() {
        return mColorCount;
    }

    public int getInterpolation() {
        return mInterpolation;
    }

    public int getTileMode() {
        return mTileMode;
    }

    @Nonnull
    public Matrixc getGradientMatrix() {
        return mPtsToUnit;
    }

    public boolean colorsAreOpaque() {
        return mColorsAreOpaque;
    }

    protected static boolean checkGradient1D(float[] colors,
                                             float[] positions,
                                             int colorCount,
                                             int tileMode) {
        return colors != null && colorCount >= 1 &&
                colors.length >= (colorCount * 4) &&
                (positions == null || positions.length >= colorCount) &&
                tileMode >= 0 && tileMode <= LAST_TILE_MODE;
    }

    @Nullable
    @SharedPtr
    protected static Shader makeDegenerateGradient(float[] colors,
                                                   @Nullable ColorSpace colorSpace,
                                                   float[] positions,
                                                   int colorCount,
                                                   int tileMode) {
        switch (tileMode) {
            case TILE_MODE_REPEAT, TILE_MODE_MIRROR -> {
                // repeat and mirror are treated the same: the border colors are never visible,
                // but approximate the final color as infinite repetitions of the colors, so
                // it can be represented as the average color of the gradient.
                //TODO
                return new EmptyShader();
            }
            case TILE_MODE_CLAMP -> {
                // Depending on how the gradient shape degenerates, there may be a more specialized
                // fallback representation for the factories to use, but this is a reasonable default.
                int i = (colorCount - 1) * 4;
                return new Color4fShader(colors[i], colors[i + 1], colors[i + 2], colors[i + 3], colorSpace);
            }
            case TILE_MODE_DECAL -> {
                // normally this would reject the area outside of the interpolation region, so since
                // inside region is empty when the radii are equal, the entire draw region is empty
                return new EmptyShader();
            }
        }
        return null;
    }

    public static void srgb_to_hsl(int i, float[] colors,
                                   BitSet hueIsPowerless) {
        float r = colors[i * 4], g = colors[i * 4 + 1], b = colors[i * 4 + 2];
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float hue = 0, sat = 0, light = (max + min) / 2;
        float delta = max - min;

        if (delta != 0) {
            sat = (light == 0 || light == 1) ? 0 : (max - light) / Math.min(light, 1 - light);
            if (max == r) {
                hue = (g - b) / delta + (g < b ? 6 : 0);
            } else if (max == g) {
                hue = (b - r) / delta + 2;
            } else { // max == blue
                hue = (r - g) / delta + 4;
            }

            hue *= 60;
        }
        if (sat == 0) {
            hueIsPowerless.set(i);
        }
        colors[i * 4] = hue;
        colors[i * 4 + 1] = sat * 100;
        colors[i * 4 + 2] = light * 100;
    }

    public static void srgb_to_hwb(int i, float[] colors,
                                   BitSet hueIsPowerless) {
        float r = colors[i * 4], g = colors[i * 4 + 1], b = colors[i * 4 + 2];
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float hue = 0, sat = 0, light = (max + min) / 2;
        float delta = max - min;

        if (delta != 0) {
            sat = (light == 0 || light == 1) ? 0 : (max - light) / Math.min(light, 1 - light);
            if (max == r) {
                hue = (g - b) / delta + (g < b ? 6 : 0);
            } else if (max == g) {
                hue = (b - r) / delta + 2;
            } else { // max == blue
                hue = (r - g) / delta + 4;
            }

            hue *= 60;
        }
        if (sat == 0) {
            hueIsPowerless.set(i);
        }
        colors[i * 4] = hue;
        colors[i * 4 + 1] = min * 100;
        colors[i * 4 + 2] = (1 - max) * 100;
    }

    public static class ColorTransformer {

        // only first 'count' entries are meaningful
        public int mColorCount;
        @Size(multiple = 4)
        @Nonnull
        public float[] mColors; // unmodifiable view
        @Nullable
        public float[] mPositions; // unmodifiable view, COW ARRAY for constructor
        @Nonnull
        public ColorSpace mIntermediateColorSpace;

        public ColorTransformer(Gradient1DShader shader,
                                ColorSpace dstCS) {
            int colorCount = shader.mColorCount;
            mColorCount = colorCount;
            int interpolation = shader.mInterpolation;
            byte cs = Interpolation.getColorSpace(interpolation);
            boolean isPolarColorSpace = cs == Interpolation.kHSL_ColorSpace ||
                    cs == Interpolation.kHWB_ColorSpace ||
                    cs == Interpolation.kLCH_ColorSpace ||
                    cs == Interpolation.kOKLCH_ColorSpace ||
                    cs == Interpolation.kOKLCHGamutMap_ColorSpace;

            // 0) Copy the shader's position pointer. Certain interpolation modes might force us to add
            //    new stops, in which case we'll allocate & edit the positions.
            mPositions = shader.mPositions;

            // 1) Determine the color space of our intermediate colors.
            mIntermediateColorSpace = intermediate_color_space(cs, dstCS);
            if (mIntermediateColorSpace == null) {
                // keep consistent with FragmentUtils when dstCS is null
                mIntermediateColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
            }

            // 2) Convert all colors to the intermediate color space
            if (shader.mColorSpace.equals(mIntermediateColorSpace)) {
                mColors = Arrays.copyOf(shader.mColors, colorCount * 4);
            } else {
                mColors = new float[colorCount * 4];
                float[] col = new float[4];
                var connector = ColorSpace.connect(shader.mColorSpace, mIntermediateColorSpace);
                for (int i = 0; i < colorCount; i++) {
                    System.arraycopy(shader.mColors, i * 4, col, 0, 4);
                    System.arraycopy(connector.transform(col), 0, mColors, i * 4, 4);
                }
            }

            // 3) Transform to the interpolation color space (if it's special)
            BitSet hueIsPowerless = null;
            switch (cs) {
                case Interpolation.kHSL_ColorSpace -> {
                    hueIsPowerless = new BitSet(colorCount);
                    for (int i = 0; i < colorCount; i++) {
                        srgb_to_hsl(i, mColors, hueIsPowerless);
                    }
                }
                case Interpolation.kHWB_ColorSpace -> {
                    hueIsPowerless = new BitSet(colorCount);
                    for (int i = 0; i < colorCount; i++) {
                        srgb_to_hwb(i, mColors, hueIsPowerless);
                    }
                }
                case Interpolation.kLab_ColorSpace, Interpolation.kLCH_ColorSpace -> {
                    // xyz_to_lab
                    float[] col = new float[4];
                    ColorSpace labCS = ColorSpace.get(ColorSpace.Named.CIE_LAB);
                    for (int i = 0; i < colorCount; i++) {
                        System.arraycopy(mColors, i * 4, col, 0, 4);
                        System.arraycopy(labCS.fromXyz(col), 0, mColors, i * 4, 4);
                    }
                    if (cs == Interpolation.kLCH_ColorSpace) {
                        // The color space is technically LCH, but we produce HCL, so that all polar spaces have hue in
                        // the first component. This simplifies the hue handling for HueMethod and premul/unpremul.
                        hueIsPowerless = new BitSet(colorCount);
                        // lab_to_hcl
                        for (int i = 0; i < colorCount; i++) {
                            float hue = (float) Math.toDegrees(Math.atan2(mColors[i * 4 + 2], mColors[i * 4 + 1]));
                            float chroma = Point.length(mColors[i * 4 + 1], mColors[i * 4 + 2]);
                            if (chroma <= 1e-2f) {
                                hueIsPowerless.set(i);
                            }
                            mColors[i * 4 + 2] = mColors[i * 4];
                            mColors[i * 4] = hue >= 0 ? hue : hue + 360;
                            mColors[i * 4 + 1] = chroma;
                        }
                    }
                }
                case Interpolation.kOKLab_ColorSpace, Interpolation.kOKLabGamutMap_ColorSpace,
                        Interpolation.kOKLCH_ColorSpace, Interpolation.kOKLCHGamutMap_ColorSpace -> {
                    // https://bottosson.github.io/posts/oklab/#converting-from-linear-srgb-to-oklab
                    for (int i = 0; i < colorCount; i++) {
                        float l = 0.4122214708f * mColors[i * 4] + 0.5363325363f * mColors[i * 4 + 1] +
                                0.0514459929f * mColors[i * 4 + 2];
                        float m = 0.2119034982f * mColors[i * 4] + 0.6806995451f * mColors[i * 4 + 1] +
                                0.1073969566f * mColors[i * 4 + 2];
                        float s = 0.0883024619f * mColors[i * 4] + 0.2817188376f * mColors[i * 4 + 1] +
                                0.6299787005f * mColors[i * 4 + 2];
                        l = (float) Math.cbrt(l);
                        m = (float) Math.cbrt(m);
                        s = (float) Math.cbrt(s);
                        mColors[i * 4] = 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s;
                        mColors[i * 4 + 1] = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s;
                        mColors[i * 4 + 2] = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s;
                    }
                    if (cs == Interpolation.kOKLCH_ColorSpace || cs == Interpolation.kOKLCHGamutMap_ColorSpace) {
                        // The color space is technically OkLCH, but we produce HCL, so that all polar spaces have
                        // hue in the first component. This simplifies the hue handling for HueMethod and
                        // premul/unpremul.
                        hueIsPowerless = new BitSet(colorCount);
                        // lab_to_hcl
                        for (int i = 0; i < colorCount; i++) {
                            float hue = (float) Math.toDegrees(Math.atan2(mColors[i * 4 + 2], mColors[i * 4 + 1]));
                            float chroma = Point.length(mColors[i * 4 + 1], mColors[i * 4 + 2]);
                            if (chroma <= 1e-6f) {
                                hueIsPowerless.set(i);
                            }
                            mColors[i * 4 + 2] = mColors[i * 4];
                            mColors[i * 4] = hue >= 0 ? hue : hue + 360;
                            mColors[i * 4 + 1] = chroma;
                        }
                    }
                }
            }

            if (isPolarColorSpace && !hueIsPowerless.isEmpty()) {
                FloatArrayList newColors = new FloatArrayList();
                FloatArrayList newPositions = new FloatArrayList();

                for (int i = 0; i < colorCount; i++) {
                    float curPos = shader.getPos(i);

                    if (!hueIsPowerless.get(i)) {
                        newColors.addElements(newColors.size(), mColors, i * 4, 4);
                        newPositions.add(curPos);
                        continue;
                    }

                    // In each case, we might be copying a powerless (invalid) hue from the neighbor, but
                    // that should be fine, as it will match that neighbor perfectly, and any hue is ok.
                    if (i != 0) {
                        newPositions.add(curPos);
                        newColors.add(mColors[(i - 1) * 4]); // prev hue
                        newColors.add(mColors[i * 4 + 1]);
                        newColors.add(mColors[i * 4 + 2]);
                        newColors.add(mColors[i * 4 + 3]);
                    }
                    if (i != colorCount - 1) {
                        newPositions.add(curPos);
                        newColors.add(mColors[(i + 1) * 4]); // next hue
                        newColors.add(mColors[i * 4 + 1]);
                        newColors.add(mColors[i * 4 + 2]);
                        newColors.add(mColors[i * 4 + 3]);
                    }
                }

                mColors = newColors.elements();
                mPositions = newPositions.elements();
                assert newColors.size() / 4 == newPositions.size();
                colorCount = newPositions.size();
                mColorCount = colorCount;
            }

            // 4) For polar colors, adjust hue values to respect the hue method. We're using a trick here...
            //    The specification looks at adjacent colors, and adjusts one or the other. Because we store
            //    the stops in uniforms (and our backend conversions normalize the hue angle), we can
            //    instead always apply the adjustment to the *second* color. That lets us keep a running
            //    total, and do a single pass across all the colors to respect the requested hue method,
            //    without needing to do any extra work per-pixel.
            if (isPolarColorSpace) {
                float delta = 0;
                for (int i = 0; i < colorCount - 1; ++i) {
                    final float h1 = mColors[i * 4];
                    final int h2 = (i + 1) * 4;
                    mColors[h2] += delta;
                    switch (Interpolation.getHueMethod(interpolation)) {
                        case Interpolation.kShorter_HueMethod:
                            if (mColors[h2] - h1 > 180) {
                                mColors[h2] -= 360;  // i.e. h1 += 360
                                delta -= 360;
                            } else if (mColors[h2] - h1 < -180) {
                                mColors[h2] += 360;
                                delta += 360;
                            }
                            break;
                        case Interpolation.kLonger_HueMethod:
                            if ((i == 0 && shader.mFirstStopIsImplicit) ||
                                    (i == colorCount - 2 && shader.mLastStopIsImplicit)) {
                                // Do nothing. We don't want to introduce a full revolution for these stops
                                // Full rationale at skbug.com/13941
                            } else if (0 < mColors[h2] - h1 && mColors[h2] - h1 < 180) {
                                mColors[h2] -= 360;  // i.e. h1 += 360
                                delta -= 360;
                            } else if (-180 < mColors[h2] - h1 && mColors[h2] - h1 <= 0) {
                                mColors[h2] += 360;
                                delta += 360;
                            }
                            break;
                        case Interpolation.kIncreasing_HueMethod:
                            if (mColors[h2] < h1) {
                                mColors[h2] += 360;
                                delta += 360;
                            }
                            break;
                        case Interpolation.kDecreasing_HueMethod:
                            if (h1 < mColors[h2]) {
                                mColors[h2] -= 360;  // i.e. h1 += 360;
                                delta -= 360;
                            }
                            break;
                    }
                }
            }

            // 5) Apply premultiplication
            if (Interpolation.isInPremul(interpolation)) {
                if (isPolarColorSpace) {
                    for (int i = 0; i < colorCount; i++) {
                        // hue (R channel) is not premultiplied
                        float a = mColors[i * 4 + 3];
                        mColors[i * 4 + 1] *= a;
                        mColors[i * 4 + 2] *= a;
                    }
                } else {
                    for (int i = 0; i < colorCount; i++) {
                        float a = mColors[i * 4 + 3];
                        mColors[i * 4] *= a;
                        mColors[i * 4 + 1] *= a;
                        mColors[i * 4 + 2] *= a;
                    }
                }
            }
        }

        static ColorSpace intermediate_color_space(byte cs,
                                                   ColorSpace dst) {
            return switch (cs) {
                case Interpolation.kDestination_ColorSpace -> dst;
                case Interpolation.kSRGB_ColorSpace,
                        Interpolation.kHSL_ColorSpace,
                        Interpolation.kHWB_ColorSpace -> ColorSpace.get(ColorSpace.Named.SRGB);
                case Interpolation.kSRGBLinear_ColorSpace ->
                    // css-color-4 allows XYZD50 and XYZD65. For gradients, those are redundant. Interpolating
                    // in any linear RGB space, (regardless of white point), gives the same answer.
                        ColorSpace.get(ColorSpace.Named.LINEAR_SRGB);
                case Interpolation.kLab_ColorSpace,
                        Interpolation.kLCH_ColorSpace ->
                    // Conversion to Lab (and LCH) starts with XYZD50
                        ColorSpace.get(ColorSpace.Named.CIE_XYZ);
                case Interpolation.kOKLab_ColorSpace,
                        Interpolation.kOKLabGamutMap_ColorSpace,
                        Interpolation.kOKLCH_ColorSpace,
                        Interpolation.kOKLCHGamutMap_ColorSpace ->
                    // The "standard" conversion to these spaces starts with XYZD65. That requires extra
                    // effort to conjure. The author also has reference code for going directly from linear
                    // sRGB, so we use that.
                        ColorSpace.get(ColorSpace.Named.LINEAR_SRGB);
                default -> throw new AssertionError(cs);
            };
        }
    }
}
