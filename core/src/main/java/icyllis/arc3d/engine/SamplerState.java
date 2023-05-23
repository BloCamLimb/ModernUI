/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.engine;

import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.graphics.SamplingOptions;

/**
 * Represents the filtering and tile modes used to access a texture. It's packed as an
 * <code>int</code>.
 * <ul>
 * <li>0-4 bits: address mode (x direction)</li>
 * <li>4-8 bits: address mode (y direction)</li>
 * <li>8-12 bits: filter mode</li>
 * <li>12-16 bits: mipmap mode</li>
 * <li>16-32 bits: anisotropy filtering level (integer value)</li>
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
     * Address modes, or wrap modes. Specify behavior of sampling with texture coordinates
     * outside an image.
     */
    public static final int
            ADDRESS_MODE_REPEAT = 0,
            ADDRESS_MODE_MIRROR_REPEAT = 1,
            ADDRESS_MODE_CLAMP_TO_EDGE = 2,
            ADDRESS_MODE_CLAMP_TO_BORDER = 3;

    // default value
    public static final int DEFAULT = 0x10022;

    static {
        // make them inline at compile-time
        assert make(FILTER_MODE_NEAREST) == DEFAULT;
        assert make(FILTER_MODE_NEAREST, MIPMAP_MODE_NONE) == DEFAULT;
        assert make(ADDRESS_MODE_CLAMP_TO_EDGE, FILTER_MODE_NEAREST, MIPMAP_MODE_NONE) == DEFAULT;
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
     * @param address the address mode
     * @param filter  the filter mode
     * @param mipmap  the mipmap mode
     */
    public static int make(int address, int filter, int mipmap) {
        return make(address, address, filter, mipmap);
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     *
     * @param addressX the address mode X
     * @param addressY the address mode Y
     * @param filter   the filter mode
     * @param mipmap   the mipmap mode
     */
    public static int make(int addressX, int addressY, int filter, int mipmap) {
        assert (addressX == ADDRESS_MODE_REPEAT ||
                addressX == ADDRESS_MODE_MIRROR_REPEAT ||
                addressX == ADDRESS_MODE_CLAMP_TO_EDGE ||
                addressX == ADDRESS_MODE_CLAMP_TO_BORDER);
        assert (addressY == ADDRESS_MODE_REPEAT ||
                addressY == ADDRESS_MODE_MIRROR_REPEAT ||
                addressY == ADDRESS_MODE_CLAMP_TO_EDGE ||
                addressY == ADDRESS_MODE_CLAMP_TO_BORDER);
        assert (filter == FILTER_MODE_NEAREST ||
                filter == FILTER_MODE_LINEAR);
        assert (mipmap == MIPMAP_MODE_NONE ||
                mipmap == MIPMAP_MODE_NEAREST ||
                mipmap == MIPMAP_MODE_LINEAR);
        return 0x10000 | addressX | (addressY << 4) | (filter << 8) | (mipmap << 12);
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     * <p>
     * We require 'isMipmapped' for APIs that allow MIP filtering to be specified orthogonally to anisotropy.
     *
     * @param addressX      the address mode X
     * @param addressY      the address mode Y
     * @param maxAnisotropy the max anisotropy filtering level
     */
    public static int makeAnisotropy(int addressX, int addressY, int maxAnisotropy, boolean isMipmapped) {
        assert (addressX == ADDRESS_MODE_REPEAT ||
                addressX == ADDRESS_MODE_MIRROR_REPEAT ||
                addressX == ADDRESS_MODE_CLAMP_TO_EDGE ||
                addressX == ADDRESS_MODE_CLAMP_TO_BORDER);
        assert (addressY == ADDRESS_MODE_REPEAT ||
                addressY == ADDRESS_MODE_MIRROR_REPEAT ||
                addressY == ADDRESS_MODE_CLAMP_TO_EDGE ||
                addressY == ADDRESS_MODE_CLAMP_TO_BORDER);
        // filter mode is always linear
        return 0x100 | addressX | (addressY << 4) |
                ((isMipmapped ? MIPMAP_MODE_LINEAR : MIPMAP_MODE_NONE) << 12) |
                (MathUtil.clamp(maxAnisotropy, 1, 1024) << 16);
    }

    //////// Unpack Methods \\\\\\\\

    public static int getAddressModeX(int samplerState) {
        return samplerState & 0xF;
    }

    public static int getAddressModeY(int samplerState) {
        return (samplerState >> 4) & 0xF;
    }

    public static boolean isRepeatedX(int samplerState) {
        int addressX = getAddressModeX(samplerState);
        return addressX == ADDRESS_MODE_REPEAT || addressX == ADDRESS_MODE_MIRROR_REPEAT;
    }

    public static boolean isRepeatedY(int samplerState) {
        int addressY = getAddressModeY(samplerState);
        return addressY == ADDRESS_MODE_REPEAT || addressY == ADDRESS_MODE_MIRROR_REPEAT;
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

    //////// Helper Methods \\\\\\\\

    /**
     * Reset mipmap mode to {@link #MIPMAP_MODE_NONE}.
     * Return value is a valid sampler state.
     */
    public static int resetMipmapMode(int samplerState) {
        return samplerState & ~0xF000;
    }
}
