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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
                               @Nonnull Matrix ptsToUnit /*internalStorage*/) {
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

    public float[] getColors() {
        return mColors;
    }

    public float[] getPositions() {
        return mPositions;
    }

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

    public static class ColorTransformer {

        public float[] mColors;     // unmodifiable
        public float[] mPositions;  // unmodifiable, nullable
        public ColorSpace mIntermediateColorSpace;

        public ColorTransformer(Gradient1DShader shader,
                                ColorSpace dst) {
            int colorCount = shader.mColorCount;
            int interpolation = shader.mInterpolation;

            // 0) Copy the shader's position pointer. Certain interpolation modes might force us to add
            //    new stops, in which case we'll allocate & edit the positions.
            mPositions = shader.mPositions;

            // 1) Determine the color space of our intermediate colors.
            mIntermediateColorSpace = intermediate_color_space(Interpolation.getColorSpace(interpolation), dst);

            // 2) Convert all colors to the intermediate color space
            if (shader.mColorSpace.equals(mIntermediateColorSpace)) {
                mColors = shader.mColors;
            } else {
                mColors = new float[colorCount * 4];
                float[] col = new float[4];
                var connector = ColorSpace.connect(shader.mColorSpace, mIntermediateColorSpace);
                for (int i = 0; i < colorCount; i++) {
                    System.arraycopy(shader.mColors, i * 4, col, 0, 4);
                    System.arraycopy(connector.transform(col), 0, mColors, i * 4, 4);
                }
            }

            //TODO
        }

        static ColorSpace intermediate_color_space(int cs,
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
