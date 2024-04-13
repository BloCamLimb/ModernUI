/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.core.MathUtil;
import org.jetbrains.annotations.Contract;

/**
 * Represents the filtering and tile modes used to access a texture. It's packed as an
 * <code>int</code> value.
 * <ul>
 * <li>0-4 bits: mag filter</li>
 * <li>4-8 bits: min filter</li>
 * <li>8-12 bits: mipmap mode</li>
 * <li>12-16 bits: address mode (x direction)</li>
 * <li>16-20 bits: address mode (y direction)</li>
 * <li>20-24 bits: address mode (z direction)</li>
 * <li>24-32 bits: max anisotropy (integer value)</li>
 * </ul>
 * <b>Do NOT change the packing format and the default value</b>.
 */
public final class SamplerState {

    //@formatter:off
    /**
     * Filters.
     */
    public static final int
            FILTER_NEAREST = 0, // single sample point (nearest neighbor)
            FILTER_LINEAR  = 1; // interpolate between 2x2 sample points (bilinear interpolation)

    /**
     * Mipmap modes.
     */
    public static final int
            MIPMAP_MODE_NONE    = 0, // ignore mipmap levels, sample from the "base"
            MIPMAP_MODE_NEAREST = 1, // sample from the nearest level
            MIPMAP_MODE_LINEAR  = 2; // interpolate between the two nearest levels

    /**
     * Address modes, or wrap modes. Specify behavior of sampling with texture coordinates
     * outside an image.
     */
    public static final int
            ADDRESS_MODE_REPEAT          = 0,
            ADDRESS_MODE_MIRRORED_REPEAT = 1,
            ADDRESS_MODE_CLAMP_TO_EDGE   = 2,
            ADDRESS_MODE_CLAMP_TO_BORDER = 3;
    //@formatter:on

    /**
     * Default value. mag linear, min linear, mipmap_none, address_clamp_to_edge, anisotropy=1.
     */
    public static final int DEFAULT = 0x1222011;

    static {
        // make them inline at compile-time
        assert make(FILTER_LINEAR) == DEFAULT;
        assert make(FILTER_LINEAR, MIPMAP_MODE_NONE) == DEFAULT;
        assert make(FILTER_LINEAR, MIPMAP_MODE_NONE, ADDRESS_MODE_CLAMP_TO_EDGE) == DEFAULT;
        assert getMagFilter(DEFAULT) == FILTER_LINEAR;
        assert getMinFilter(DEFAULT) == FILTER_LINEAR;
        assert getMipmapMode(DEFAULT) == MIPMAP_MODE_NONE;
        assert getAddressModeX(DEFAULT) == ADDRESS_MODE_CLAMP_TO_EDGE;
        assert getAddressModeY(DEFAULT) == ADDRESS_MODE_CLAMP_TO_EDGE;
        assert getAddressModeZ(DEFAULT) == ADDRESS_MODE_CLAMP_TO_EDGE;
        assert getMaxAnisotropy(DEFAULT) == 1;
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     *
     * @param filter the filter
     */
    @Contract(pure = true)
    public static int make(int filter) {
        assert (filter == FILTER_NEAREST || filter == FILTER_LINEAR);
        return 0x1222000 | filter | (filter << 4);
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     *
     * @param filter the filter
     * @param mipmap the mipmap mode
     */
    @Contract(pure = true)
    public static int make(int filter, int mipmap) {
        assert (filter == FILTER_NEAREST ||
                filter == FILTER_LINEAR);
        assert (mipmap == MIPMAP_MODE_NONE ||
                mipmap == MIPMAP_MODE_NEAREST ||
                mipmap == MIPMAP_MODE_LINEAR);
        return 0x1222000 | filter | (filter << 4) | (mipmap << 8);
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     *
     * @param filter  the filter
     * @param mipmap  the mipmap mode
     * @param address the address mode
     */
    @Contract(pure = true)
    public static int make(int filter, int mipmap, int address) {
        return make(filter, filter, mipmap, address, address, address);
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     *
     * @param magFilter    the mag filter
     * @param minFilter    the min filter
     * @param mipmapMode   the mipmap mode
     * @param addressModeX the address mode X
     * @param addressModeY the address mode Y
     */
    @Contract(pure = true)
    public static int make(int magFilter, int minFilter, int mipmapMode,
                           int addressModeX, int addressModeY, int addressModeZ) {
        assert (magFilter == FILTER_NEAREST ||
                magFilter == FILTER_LINEAR);
        assert (minFilter == FILTER_NEAREST ||
                minFilter == FILTER_LINEAR);
        assert (mipmapMode == MIPMAP_MODE_NONE ||
                mipmapMode == MIPMAP_MODE_NEAREST ||
                mipmapMode == MIPMAP_MODE_LINEAR);
        assert (addressModeX >= 0 && addressModeX <= ADDRESS_MODE_CLAMP_TO_BORDER);
        assert (addressModeY >= 0 && addressModeY <= ADDRESS_MODE_CLAMP_TO_BORDER);
        assert (addressModeZ >= 0 && addressModeZ <= ADDRESS_MODE_CLAMP_TO_BORDER);
        return magFilter |
                (minFilter << 4) |
                (mipmapMode << 8) |
                (addressModeX << 12) |
                (addressModeY << 16) |
                (addressModeZ << 20) |
                0x1000000;
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     * <p>
     * We require 'isMipmapped' for APIs that allow MIP filtering to be specified orthogonally to anisotropy.
     *
     * @param addressModeX  the address mode X
     * @param addressModeY  the address mode Y
     * @param maxAnisotropy the max anisotropy filtering level
     */
    @Contract(pure = true)
    public static int makeAnisotropy(int addressModeX, int addressModeY, int addressModeZ,
                                     int maxAnisotropy, boolean isMipmapped) {
        assert (addressModeX >= 0 && addressModeX <= ADDRESS_MODE_CLAMP_TO_BORDER);
        assert (addressModeY >= 0 && addressModeY <= ADDRESS_MODE_CLAMP_TO_BORDER);
        assert (addressModeZ >= 0 && addressModeZ <= ADDRESS_MODE_CLAMP_TO_BORDER);
        // filter mode is always linear
        return 0x11 | (addressModeX << 12) | (addressModeY << 16) | (addressModeZ << 20) |
                (isMipmapped ? MIPMAP_MODE_LINEAR << 8 : MIPMAP_MODE_NONE << 8) |
                (MathUtil.clamp(maxAnisotropy, 1, 64) << 24);
    }

    //////// Unpack Methods \\\\\\\\

    @Contract(pure = true)
    public static int getMagFilter(int samplerState) {
        return samplerState & 0xF;
    }

    @Contract(pure = true)
    public static int getMinFilter(int samplerState) {
        return (samplerState >> 4) & 0xF;
    }

    @Contract(pure = true)
    public static int getMipmapMode(int samplerState) {
        return (samplerState >> 8) & 0xF;
    }

    @Contract(pure = true)
    public static int getAddressModeX(int samplerState) {
        return (samplerState >> 12) & 0xF;
    }

    @Contract(pure = true)
    public static int getAddressModeY(int samplerState) {
        return (samplerState >> 16) & 0xF;
    }

    @Contract(pure = true)
    public static int getAddressModeZ(int samplerState) {
        return (samplerState >> 20) & 0xF;
    }

    @Contract(pure = true)
    public static boolean isMipmapped(int samplerState) {
        return getMipmapMode(samplerState) != MIPMAP_MODE_NONE;
    }

    @Contract(pure = true)
    public static boolean isRepeatedX(int samplerState) {
        int addressX = getAddressModeX(samplerState);
        return addressX == ADDRESS_MODE_REPEAT || addressX == ADDRESS_MODE_MIRRORED_REPEAT;
    }

    @Contract(pure = true)
    public static boolean isRepeatedY(int samplerState) {
        int addressY = getAddressModeY(samplerState);
        return addressY == ADDRESS_MODE_REPEAT || addressY == ADDRESS_MODE_MIRRORED_REPEAT;
    }

    @Contract(pure = true)
    public static boolean isRepeatedZ(int samplerState) {
        int addressZ = getAddressModeZ(samplerState);
        return addressZ == ADDRESS_MODE_REPEAT || addressZ == ADDRESS_MODE_MIRRORED_REPEAT;
    }

    @Contract(pure = true)
    public static boolean isRepeated(int samplerState) {
        return isRepeatedX(samplerState) || isRepeatedY(samplerState) || isRepeatedZ(samplerState);
    }

    @Contract(pure = true)
    public static int getMaxAnisotropy(int samplerState) {
        return samplerState >>> 24;
    }

    @Contract(pure = true)
    public static boolean isAnisotropy(int samplerState) {
        return getMaxAnisotropy(samplerState) > 1;
    }

    //////// Helper Methods \\\\\\\\

    /**
     * Reset mipmap mode to {@link #MIPMAP_MODE_NONE}.
     * Return value is a valid sampler state.
     */
    @Contract(pure = true)
    public static int resetMipmapMode(int samplerState) {
        return samplerState & ~0xF00;
    }
}
