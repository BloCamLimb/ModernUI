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
 * Represents the filtering and tile modes used to access a texture. It's represented as an int.
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
            WRAP_MODE_MIRRORED_REPEAT = 1,
            WRAP_MODE_CLAMP_TO_EDGE = 2,
            WRAP_MODE_CLAMP_TO_BORDER = 3,
            WRAP_MODE_MIRROR_CLAMP_TO_EDGE = 4;

    // default value
    public static final int DEFAULT = 0x10022;

    static {
        // make them inline at compile-time
        assert make(FILTER_MODE_NEAREST) == DEFAULT;
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     *
     * @param filter the filter mode
     */
    public static int make(int filter) {
        return make(WRAP_MODE_CLAMP_TO_EDGE, WRAP_MODE_CLAMP_TO_EDGE, filter, MIPMAP_MODE_NONE);
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     *
     * @param filter the filter mode
     * @param mipmap the mipmap mode
     */
    public static int make(int filter, int mipmap) {
        return make(WRAP_MODE_CLAMP_TO_EDGE, WRAP_MODE_CLAMP_TO_EDGE, filter, mipmap);
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     *
     * @param wrap   the wrap mode X and Y
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
        assert wrapX == WRAP_MODE_REPEAT || wrapX == WRAP_MODE_MIRRORED_REPEAT ||
                wrapX == WRAP_MODE_CLAMP_TO_EDGE || wrapX == WRAP_MODE_CLAMP_TO_BORDER ||
                wrapX == WRAP_MODE_MIRROR_CLAMP_TO_EDGE;
        assert wrapY == WRAP_MODE_REPEAT || wrapY == WRAP_MODE_MIRRORED_REPEAT ||
                wrapY == WRAP_MODE_CLAMP_TO_EDGE || wrapY == WRAP_MODE_CLAMP_TO_BORDER ||
                wrapY == WRAP_MODE_MIRROR_CLAMP_TO_EDGE;
        assert filter == FILTER_MODE_NEAREST || filter == FILTER_MODE_LINEAR;
        assert mipmap == MIPMAP_MODE_NONE || mipmap == MIPMAP_MODE_NEAREST || mipmap == MIPMAP_MODE_LINEAR;
        return wrapX | (wrapY << 4) | (filter << 8) | (mipmap << 12) | (1 << 16);
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     * We require 'viewIsMipmapped' for APIs that allow MIP filtering to be specified orthogonally to anisotropy.
     */
    public static int makeAnisotropy(int wrapX, int wrapY, int maxAnisotropy, boolean viewIsMipmapped) {
        assert wrapX == WRAP_MODE_REPEAT || wrapX == WRAP_MODE_MIRRORED_REPEAT ||
                wrapX == WRAP_MODE_CLAMP_TO_EDGE || wrapX == WRAP_MODE_CLAMP_TO_BORDER ||
                wrapX == WRAP_MODE_MIRROR_CLAMP_TO_EDGE;
        assert wrapY == WRAP_MODE_REPEAT || wrapY == WRAP_MODE_MIRRORED_REPEAT ||
                wrapY == WRAP_MODE_CLAMP_TO_EDGE || wrapY == WRAP_MODE_CLAMP_TO_BORDER ||
                wrapY == WRAP_MODE_MIRROR_CLAMP_TO_EDGE;
        return wrapX | (wrapY << 4) |
                (FILTER_MODE_LINEAR << 8) |
                ((viewIsMipmapped ? MIPMAP_MODE_LINEAR : MIPMAP_MODE_NEAREST) << 12) |
                (MathUtil.clamp(maxAnisotropy, 1, 1024) << 16);
    }

    public static int getWrapModeX(int key) {
        return key & 0xF;
    }

    public static int getWrapModeY(int key) {
        return (key >> 4) & 0xF;
    }

    public static boolean isRepeatedX(int key) {
        int wrapX = getWrapModeX(key);
        return wrapX == WRAP_MODE_REPEAT || wrapX == WRAP_MODE_MIRRORED_REPEAT;
    }

    public static boolean isRepeatedY(int key) {
        int wrapY = getWrapModeY(key);
        return wrapY == WRAP_MODE_REPEAT || wrapY == WRAP_MODE_MIRRORED_REPEAT;
    }

    public static boolean isRepeated(int key) {
        return isRepeatedX(key) || isRepeatedY(key);
    }

    public static int getFilterMode(int key) {
        return (key >> 8) & 0xF;
    }

    public static int getMipmapMode(int key) {
        return (key >> 12) & 0xF;
    }

    public static boolean isMipmapped(int key) {
        return getMipmapMode(key) != MIPMAP_MODE_NONE;
    }

    public static int getMaxAnisotropy(int key) {
        return key >>> 16;
    }
}
