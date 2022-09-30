/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.engine;

import icyllis.akashigi.core.MathUtil;
import icyllis.akashigi.core.SamplingOptions;

/**
 * Represents the filtering and tile modes used to access a texture. It's packed as an
 * <code>int</code>.
 * <ul>
 * <li>0-4 bits: wrap mode (x direction)</li>
 * <li>4-8 bits: wrap mode (y direction)</li>
 * <li>8-12 bits: filter mode</li>
 * <li>12-16 bits: mipmap mode</li>
 * <li>16-32 bits: anisotropy filtering level</li>
 * </ul>
 */
public final class SamplerState {

    /**
     * Filter modes.
     */
    public static final int
            FILTER_MODE_NEAREST = SamplingOptions.FILTER_MODE_NEAREST,
            FILTER_MODE_LINEAR = SamplingOptions.FILTER_MODE_LINEAR;

    /**
     * Mipmap modes.
     */
    public static final int
            MIPMAP_MODE_NONE = SamplingOptions.MIPMAP_MODE_NONE,
            MIPMAP_MODE_NEAREST = SamplingOptions.MIPMAP_MODE_NEAREST,
            MIPMAP_MODE_LINEAR = SamplingOptions.MIPMAP_MODE_LINEAR;

    /**
     * Wrap modes, or address modes. Specify behavior of sampling with texture coordinates
     * outside an image.
     */
    public static final int
            WRAP_MODE_REPEAT = 0,
            WRAP_MODE_MIRROR_REPEAT = 1,
            WRAP_MODE_CLAMP_TO_EDGE = 2,
            WRAP_MODE_CLAMP_TO_BORDER = 3;

    // default value
    public static final int DEFAULT = 0x10022;

    static {
        // make them inline at compile-time
        assert make(FILTER_MODE_NEAREST) == DEFAULT;
        assert make(FILTER_MODE_NEAREST, MIPMAP_MODE_NONE) == DEFAULT;
        assert make(WRAP_MODE_CLAMP_TO_EDGE, FILTER_MODE_NEAREST, MIPMAP_MODE_NONE) == DEFAULT;
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     *
     * @param filter the filter mode
     */
    public static int make(int filter) {
        assert (filter == FILTER_MODE_NEAREST || filter == FILTER_MODE_LINEAR);
        return 0x10022 | (filter << 8);
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     *
     * @param filter the filter mode
     * @param mipmap the mipmap mode
     */
    public static int make(int filter, int mipmap) {
        assert (filter == FILTER_MODE_NEAREST ||
                filter == FILTER_MODE_LINEAR);
        assert (mipmap == MIPMAP_MODE_NONE ||
                mipmap == MIPMAP_MODE_NEAREST ||
                mipmap == MIPMAP_MODE_LINEAR);
        return 0x10022 | (filter << 8) | (mipmap << 12);
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     *
     * @param wrap   the wrap mode
     * @param filter the filter mode
     * @param mipmap the mipmap mode
     */
    public static int make(int wrap, int filter, int mipmap) {
        return make(wrap, wrap, filter, mipmap);
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     *
     * @param wrapX  the wrap mode X
     * @param wrapY  the wrap mode Y
     * @param filter the filter mode
     * @param mipmap the mipmap mode
     */
    public static int make(int wrapX, int wrapY, int filter, int mipmap) {
        assert (wrapX == WRAP_MODE_REPEAT ||
                wrapX == WRAP_MODE_MIRROR_REPEAT ||
                wrapX == WRAP_MODE_CLAMP_TO_EDGE ||
                wrapX == WRAP_MODE_CLAMP_TO_BORDER);
        assert (wrapY == WRAP_MODE_REPEAT ||
                wrapY == WRAP_MODE_MIRROR_REPEAT ||
                wrapY == WRAP_MODE_CLAMP_TO_EDGE ||
                wrapY == WRAP_MODE_CLAMP_TO_BORDER);
        assert (filter == FILTER_MODE_NEAREST ||
                filter == FILTER_MODE_LINEAR);
        assert (mipmap == MIPMAP_MODE_NONE ||
                mipmap == MIPMAP_MODE_NEAREST ||
                mipmap == MIPMAP_MODE_LINEAR);
        return 0x10000 | wrapX | (wrapY << 4) | (filter << 8) | (mipmap << 12);
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     * <p>
     * We require 'isMipmapped' for APIs that allow MIP filtering to be specified orthogonally to anisotropy.
     *
     * @param wrapX         the wrap mode X
     * @param wrapY         the wrap mode Y
     * @param maxAnisotropy the max anisotropy filtering level
     */
    public static int makeAnisotropy(int wrapX, int wrapY, int maxAnisotropy, boolean isMipmapped) {
        assert (wrapX == WRAP_MODE_REPEAT ||
                wrapX == WRAP_MODE_MIRROR_REPEAT ||
                wrapX == WRAP_MODE_CLAMP_TO_EDGE ||
                wrapX == WRAP_MODE_CLAMP_TO_BORDER);
        assert (wrapY == WRAP_MODE_REPEAT ||
                wrapY == WRAP_MODE_MIRROR_REPEAT ||
                wrapY == WRAP_MODE_CLAMP_TO_EDGE ||
                wrapY == WRAP_MODE_CLAMP_TO_BORDER);
        // filter mode is always linear
        return 0x100 | wrapX | (wrapY << 4) |
                ((isMipmapped ? MIPMAP_MODE_LINEAR : MIPMAP_MODE_NONE) << 12) |
                (MathUtil.clamp(maxAnisotropy, 1, 1024) << 16);
    }

    //////// Unpack Methods \\\\\\\\

    public static int getWrapModeX(int samplerState) {
        return samplerState & 0xF;
    }

    public static int getWrapModeY(int samplerState) {
        return (samplerState >> 4) & 0xF;
    }

    public static boolean isRepeatedX(int samplerState) {
        int wrapX = getWrapModeX(samplerState);
        return wrapX == WRAP_MODE_REPEAT || wrapX == WRAP_MODE_MIRROR_REPEAT;
    }

    public static boolean isRepeatedY(int samplerState) {
        int wrapY = getWrapModeY(samplerState);
        return wrapY == WRAP_MODE_REPEAT || wrapY == WRAP_MODE_MIRROR_REPEAT;
    }

    public static boolean isRepeated(int samplerState) {
        return isRepeatedX(samplerState) || isRepeatedY(samplerState);
    }

    public static int getFilterMode(int samplerState) {
        return (samplerState >> 8) & 0xF;
    }

    public static int getMipmapMode(int samplerState) {
        return (samplerState >> 12) & 0xF;
    }

    public static boolean isMipmapped(int samplerState) {
        return getMipmapMode(samplerState) != MIPMAP_MODE_NONE;
    }

    public static int getMaxAnisotropy(int samplerState) {
        return samplerState >>> 16;
    }

    public static boolean isAnisotropy(int samplerState) {
        return getMaxAnisotropy(samplerState) > 1;
    }

    //////// Screen Methods \\\\\\\\

    /**
     * Clear mipmap mode to {@link #MIPMAP_MODE_NONE}.
     * Return value is a valid sampler state.
     */
    public static int screenMipmapMode(int samplerState) {
        return samplerState & ~0xF000;
    }
}
