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

package icyllis.arc3d.opengl;

import icyllis.arc3d.engine.ManagedResource;
import icyllis.arc3d.engine.SamplerState;
import icyllis.arc3d.core.SharedPtr;
import org.lwjgl.opengl.GL46C;

import javax.annotation.Nullable;

import static icyllis.arc3d.opengl.GLCore.*;

/**
 * Represents OpenGL sampler objects.
 */
public final class GLSampler extends ManagedResource {

    private int mSampler;

    private GLSampler(GLEngine engine, int sampler) {
        super(engine);
        mSampler = sampler;
    }

    @Nullable
    @SharedPtr
    public static GLSampler create(GLEngine engine,
                                   int samplerState) {
        int sampler = glGenSamplers();
        if (sampler == 0) {
            return null;
        }
        int magFilter = filter_to_mag_filter(
                SamplerState.getMagFilter(samplerState)
        );
        int minFilter = filter_to_min_filter(
                SamplerState.getMinFilter(samplerState),
                SamplerState.getMipmapMode(samplerState)
        );
        int wrapX = address_mode_to_wrap(
                SamplerState.getAddressModeX(samplerState)
        );
        int wrapY = address_mode_to_wrap(
                SamplerState.getAddressModeY(samplerState)
        );
        glSamplerParameteri(sampler, GL_TEXTURE_MAG_FILTER, magFilter);
        glSamplerParameteri(sampler, GL_TEXTURE_MIN_FILTER, minFilter);
        glSamplerParameteri(sampler, GL_TEXTURE_WRAP_S, wrapX);
        glSamplerParameteri(sampler, GL_TEXTURE_WRAP_T, wrapY);
        if (engine.getCaps().hasAnisotropySupport()) {
            float maxAnisotropy = Math.min(SamplerState.getMaxAnisotropy(samplerState),
                    engine.getCaps().maxTextureMaxAnisotropy());
            assert (maxAnisotropy >= 1.0f);
            glSamplerParameterf(sampler, GL46C.GL_TEXTURE_MAX_ANISOTROPY, maxAnisotropy);
        }
        return new GLSampler(engine, sampler);
    }

    //@formatter:off
    private static int filter_to_mag_filter(int filter) {
        return switch (filter) {
            case SamplerState.FILTER_NEAREST -> GL_NEAREST;
            case SamplerState.FILTER_LINEAR  -> GL_LINEAR;
            default -> throw new AssertionError(filter);
        };
    }

    private static int filter_to_min_filter(int filter, int mipmapMode) {
        return switch (mipmapMode) {
            case SamplerState.MIPMAP_MODE_NONE    -> filter_to_mag_filter(filter);
            case SamplerState.MIPMAP_MODE_NEAREST -> switch (filter) {
                case SamplerState.FILTER_NEAREST  -> GL_NEAREST_MIPMAP_NEAREST;
                case SamplerState.FILTER_LINEAR   -> GL_LINEAR_MIPMAP_NEAREST;
                default -> throw new AssertionError(filter);
            };
            case SamplerState.MIPMAP_MODE_LINEAR  -> switch (filter) {
                case SamplerState.FILTER_NEAREST  -> GL_NEAREST_MIPMAP_LINEAR;
                case SamplerState.FILTER_LINEAR   -> GL_LINEAR_MIPMAP_LINEAR;
                default -> throw new AssertionError(filter);
            };
            default -> throw new AssertionError(mipmapMode);
        };
    }

    private static int address_mode_to_wrap(int addressMode) {
        return switch (addressMode) {
            case SamplerState.ADDRESS_MODE_REPEAT          -> GL_REPEAT;
            case SamplerState.ADDRESS_MODE_MIRRORED_REPEAT -> GL_MIRRORED_REPEAT;
            case SamplerState.ADDRESS_MODE_CLAMP_TO_EDGE   -> GL_CLAMP_TO_EDGE;
            case SamplerState.ADDRESS_MODE_CLAMP_TO_BORDER -> GL_CLAMP_TO_BORDER;
            default -> throw new AssertionError(addressMode);
        };
    }
    //@formatter:on

    @Override
    protected void deallocate() {
        if (mSampler != 0) {
            glDeleteSamplers(mSampler);
        }
        discard();
    }

    public void discard() {
        mSampler = 0;
    }

    public int getHandle() {
        return mSampler;
    }
}
