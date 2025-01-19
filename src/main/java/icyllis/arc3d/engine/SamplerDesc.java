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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.MathUtil;
import icyllis.arc3d.core.SamplingOptions;
import icyllis.arc3d.core.shaders.Shader;
import org.jetbrains.annotations.Contract;

import javax.annotation.concurrent.Immutable;

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
@Immutable
public final class SamplerDesc implements IResourceKey {

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
    static final int
            ADDRESS_MODE_LAST = ADDRESS_MODE_CLAMP_TO_BORDER;
    //@formatter:on

    /**
     * Nearest value. mag filter nearest, min filter nearest, mipmap mode none,
     * address mode clamp to edge, max anisotropy 1.
     */
    public static final SamplerDesc NEAREST = new SamplerDesc(0x1222000);
    /**
     * Default value. mag filter linear, min filter linear, mipmap mode none,
     * address mode clamp to edge, max anisotropy 1.
     */
    public static final SamplerDesc DEFAULT = new SamplerDesc(0x1222011);

    //@formatter:off
    static {
        assert DEFAULT.getMagFilter() == FILTER_LINEAR;
        assert DEFAULT.getMinFilter() == FILTER_LINEAR;
        assert DEFAULT.getMipmapMode() == MIPMAP_MODE_NONE;
        assert DEFAULT.getAddressModeX() == ADDRESS_MODE_CLAMP_TO_EDGE;
        assert DEFAULT.getAddressModeY() == ADDRESS_MODE_CLAMP_TO_EDGE;
        assert DEFAULT.getAddressModeZ() == ADDRESS_MODE_CLAMP_TO_EDGE;
        assert DEFAULT.getMaxAnisotropy() == 1;

        // We assume these enum constants everywhere

        //noinspection ConstantValue
        assert
                FILTER_NEAREST == SamplingOptions.FILTER_MODE_NEAREST &&
                FILTER_LINEAR  == SamplingOptions.FILTER_MODE_LINEAR  ;

        //noinspection ConstantValue
        assert
                MIPMAP_MODE_NONE    == SamplingOptions.MIPMAP_MODE_NONE    &&
                MIPMAP_MODE_NEAREST == SamplingOptions.MIPMAP_MODE_NEAREST &&
                MIPMAP_MODE_LINEAR  == SamplingOptions.MIPMAP_MODE_LINEAR  ;

        //noinspection ConstantValue
        assert
                ADDRESS_MODE_REPEAT          == Shader.TILE_MODE_REPEAT &&
                ADDRESS_MODE_MIRRORED_REPEAT == Shader.TILE_MODE_MIRROR &&
                ADDRESS_MODE_CLAMP_TO_EDGE   == Shader.TILE_MODE_CLAMP  &&
                ADDRESS_MODE_CLAMP_TO_BORDER == Shader.TILE_MODE_DECAL  ;
    }
    //@formatter:on

    private final int mState;

    SamplerDesc(int state) {
        mState = state;
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     *
     * @param filter the filter for mag and min
     */
    @Contract(pure = true)
    public static SamplerDesc make(int filter) {
        assert (filter == FILTER_NEAREST || filter == FILTER_LINEAR);
        return filter == FILTER_NEAREST ? NEAREST : DEFAULT;
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     *
     * @param filter the filter for mag and min
     * @param mipmap the mipmap mode
     */
    @Contract(pure = true)
    public static SamplerDesc make(int filter, int mipmap) {
        assert (filter == FILTER_NEAREST ||
                filter == FILTER_LINEAR);
        assert (mipmap == MIPMAP_MODE_NONE ||
                mipmap == MIPMAP_MODE_NEAREST ||
                mipmap == MIPMAP_MODE_LINEAR);
        return new SamplerDesc(0x1222000 | filter | (filter << 4) | (mipmap << 8));
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     *
     * @param filter  the filter for mag and min
     * @param mipmap  the mipmap mode
     * @param address the address mode for x, y and z
     */
    @Contract(pure = true)
    public static SamplerDesc make(int filter, int mipmap, int address) {
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
     * @param addressModeZ the address mode Z
     */
    @Contract(pure = true)
    public static SamplerDesc make(int magFilter, int minFilter, int mipmapMode,
                                   int addressModeX, int addressModeY, int addressModeZ) {
        assert (magFilter == FILTER_NEAREST ||
                magFilter == FILTER_LINEAR);
        assert (minFilter == FILTER_NEAREST ||
                minFilter == FILTER_LINEAR);
        assert (mipmapMode == MIPMAP_MODE_NONE ||
                mipmapMode == MIPMAP_MODE_NEAREST ||
                mipmapMode == MIPMAP_MODE_LINEAR);
        assert (addressModeX >= 0 && addressModeX <= ADDRESS_MODE_LAST);
        assert (addressModeY >= 0 && addressModeY <= ADDRESS_MODE_LAST);
        assert (addressModeZ >= 0 && addressModeZ <= ADDRESS_MODE_LAST);
        return new SamplerDesc(magFilter |
                (minFilter << 4) |
                (mipmapMode << 8) |
                (addressModeX << 12) |
                (addressModeY << 16) |
                (addressModeZ << 20) |
                0x1000000);
    }

    /**
     * Turn the sampler state into an integer for use as a key.
     * <p>
     * We require 'isMipmapped' for APIs that allow MIP filtering to be specified orthogonally to anisotropy.
     *
     * @param addressModeX  the address mode X
     * @param addressModeY  the address mode Y
     * @param addressModeZ  the address mode Z
     * @param maxAnisotropy the max anisotropy filtering level
     */
    @Contract(pure = true)
    public static SamplerDesc makeAnisotropy(int addressModeX, int addressModeY, int addressModeZ,
                                             int maxAnisotropy, boolean isMipmapped) {
        assert (addressModeX >= 0 && addressModeX <= ADDRESS_MODE_LAST);
        assert (addressModeY >= 0 && addressModeY <= ADDRESS_MODE_LAST);
        assert (addressModeZ >= 0 && addressModeZ <= ADDRESS_MODE_LAST);
        // filter mode is always linear
        return new SamplerDesc(0x11 | (addressModeX << 12) | (addressModeY << 16) | (addressModeZ << 20) |
                (isMipmapped ? MIPMAP_MODE_LINEAR << 8 : MIPMAP_MODE_NONE << 8) |
                (MathUtil.clamp(maxAnisotropy, 1, 64) << 24));
    }

    //////// Unpack Methods \\\\\\\\

    @Contract(pure = true)
    public int getMagFilter() {
        return mState & 0xF;
    }

    @Contract(pure = true)
    public int getMinFilter() {
        return (mState >> 4) & 0xF;
    }

    @Contract(pure = true)
    public int getMipmapMode() {
        return (mState >> 8) & 0xF;
    }

    @Contract(pure = true)
    public int getAddressModeX() {
        return (mState >> 12) & 0xF;
    }

    @Contract(pure = true)
    public int getAddressModeY() {
        return (mState >> 16) & 0xF;
    }

    @Contract(pure = true)
    public int getAddressModeZ() {
        return (mState >> 20) & 0xF;
    }

    @Contract(pure = true)
    public boolean isMipmapped() {
        return getMipmapMode() != MIPMAP_MODE_NONE;
    }

    @Contract(pure = true)
    public boolean isRepeatedX() {
        int addressX = getAddressModeX();
        return addressX == ADDRESS_MODE_REPEAT || addressX == ADDRESS_MODE_MIRRORED_REPEAT;
    }

    @Contract(pure = true)
    public boolean isRepeatedY() {
        int addressY = getAddressModeY();
        return addressY == ADDRESS_MODE_REPEAT || addressY == ADDRESS_MODE_MIRRORED_REPEAT;
    }

    @Contract(pure = true)
    public boolean isRepeatedZ() {
        int addressZ = getAddressModeZ();
        return addressZ == ADDRESS_MODE_REPEAT || addressZ == ADDRESS_MODE_MIRRORED_REPEAT;
    }

    @Contract(pure = true)
    public boolean isRepeated() {
        return isRepeatedX() || isRepeatedY() || isRepeatedZ();
    }

    @Contract(pure = true)
    public int getMaxAnisotropy() {
        return mState >>> 24;
    }

    @Contract(pure = true)
    public boolean isAnisotropy() {
        return getMaxAnisotropy() > 1;
    }

    //////// Helper Methods \\\\\\\\

    /**
     * Reset mipmap mode to {@link #MIPMAP_MODE_NONE}.
     * Return value is a valid sampler state.
     */
    @Contract(pure = true)
    public SamplerDesc resetMipmapMode() {
        return new SamplerDesc(mState & ~0xF00);
    }

    @Override
    public SamplerDesc copy() {
        return this;
    }

    @Override
    public int hashCode() {
        return mState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof SamplerDesc desc) {
            return mState == desc.mState;
        }
        return false;
    }
}
