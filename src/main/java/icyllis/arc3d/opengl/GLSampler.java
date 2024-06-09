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

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;

import javax.annotation.Nullable;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13C.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL14C.GL_MIRRORED_REPEAT;
import static org.lwjgl.opengl.GL46C.GL_TEXTURE_MAX_ANISOTROPY;

/**
 * Represents OpenGL sampler objects.
 */
public final class GLSampler extends Sampler {

    private int mSampler;

    private GLSampler(Context context, int sampler) {
        super(context);
        mSampler = sampler;
    }

    @Nullable
    @SharedPtr
    public static GLSampler create(Context context,
                                   SamplerDesc desc) {
        GLDevice device = (GLDevice) context.getDevice();
        int sampler = device.getGL().glGenSamplers();
        if (sampler == 0) {
            return null;
        }
        int magFilter = filter_to_mag_filter(
                desc.getMagFilter()
        );
        int minFilter = filter_to_min_filter(
                desc.getMinFilter(),
                desc.getMipmapMode()
        );
        int wrapX = address_mode_to_wrap(
                desc.getAddressModeX()
        );
        int wrapY = address_mode_to_wrap(
                desc.getAddressModeY()
        );
        device.getGL().glSamplerParameteri(sampler, GL_TEXTURE_MAG_FILTER, magFilter);
        device.getGL().glSamplerParameteri(sampler, GL_TEXTURE_MIN_FILTER, minFilter);
        device.getGL().glSamplerParameteri(sampler, GL_TEXTURE_WRAP_S, wrapX);
        device.getGL().glSamplerParameteri(sampler, GL_TEXTURE_WRAP_T, wrapY);
        if (device.getCaps().hasAnisotropySupport()) {
            float maxAnisotropy = Math.min(desc.getMaxAnisotropy(),
                    device.getCaps().maxTextureMaxAnisotropy());
            assert (maxAnisotropy >= 1.0f);
            device.getGL().glSamplerParameterf(sampler, GL_TEXTURE_MAX_ANISOTROPY, maxAnisotropy);
        }
        return new GLSampler(context, sampler);
    }

    //@formatter:off
    private static int filter_to_mag_filter(int filter) {
        return switch (filter) {
            case SamplerDesc.FILTER_NEAREST -> GL_NEAREST;
            case SamplerDesc.FILTER_LINEAR  -> GL_LINEAR;
            default -> throw new AssertionError(filter);
        };
    }

    private static int filter_to_min_filter(int filter, int mipmapMode) {
        return switch (mipmapMode) {
            case SamplerDesc.MIPMAP_MODE_NONE    -> filter_to_mag_filter(filter);
            case SamplerDesc.MIPMAP_MODE_NEAREST -> switch (filter) {
                case SamplerDesc.FILTER_NEAREST  -> GL_NEAREST_MIPMAP_NEAREST;
                case SamplerDesc.FILTER_LINEAR   -> GL_LINEAR_MIPMAP_NEAREST;
                default -> throw new AssertionError(filter);
            };
            case SamplerDesc.MIPMAP_MODE_LINEAR  -> switch (filter) {
                case SamplerDesc.FILTER_NEAREST  -> GL_NEAREST_MIPMAP_LINEAR;
                case SamplerDesc.FILTER_LINEAR   -> GL_LINEAR_MIPMAP_LINEAR;
                default -> throw new AssertionError(filter);
            };
            default -> throw new AssertionError(mipmapMode);
        };
    }

    private static int address_mode_to_wrap(int addressMode) {
        return switch (addressMode) {
            case SamplerDesc.ADDRESS_MODE_REPEAT          -> GL_REPEAT;
            case SamplerDesc.ADDRESS_MODE_MIRRORED_REPEAT -> GL_MIRRORED_REPEAT;
            case SamplerDesc.ADDRESS_MODE_CLAMP_TO_EDGE   -> GL_CLAMP_TO_EDGE;
            case SamplerDesc.ADDRESS_MODE_CLAMP_TO_BORDER -> GL_CLAMP_TO_BORDER;
            default -> throw new AssertionError(addressMode);
        };
    }
    //@formatter:on

    @Override
    protected void onRelease() {
        if (mSampler != 0) {
            ((GLDevice) getDevice()).getGL().glDeleteSamplers(mSampler);
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
