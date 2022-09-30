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

package icyllis.akashigi.opengl;

import icyllis.akashigi.core.SharedPtr;
import icyllis.akashigi.engine.ManagedResource;
import icyllis.akashigi.engine.SamplerState;
import org.lwjgl.opengl.GL46C;

import javax.annotation.Nullable;

import static icyllis.akashigi.opengl.GLCore.*;

/**
 * Represents OpenGL sampler objects.
 */
public final class GLSampler extends ManagedResource {

    private int mSampler;

    private GLSampler(GLServer server, int sampler) {
        super(server);
        mSampler = sampler;
    }

    @Nullable
    @SharedPtr
    public static GLSampler create(GLServer server,
                                   int samplerState) {
        int sampler = glCreateSamplers();
        if (sampler == 0) {
            return null;
        }
        int filterMode = SamplerState.getFilterMode(samplerState);
        int mipmapMode = SamplerState.getMipmapMode(samplerState);
        int magFilter = filterModeToMagFilter(filterMode);
        int minFilter = filterModeToMinFilter(filterMode, mipmapMode);
        int wrapX = wrapModeToWrap(SamplerState.getWrapModeX(samplerState));
        int wrapY = wrapModeToWrap(SamplerState.getWrapModeY(samplerState));
        glSamplerParameteri(sampler, GL_TEXTURE_MAG_FILTER, magFilter);
        glSamplerParameteri(sampler, GL_TEXTURE_MIN_FILTER, minFilter);
        glSamplerParameteri(sampler, GL_TEXTURE_WRAP_S, wrapX);
        glSamplerParameteri(sampler, GL_TEXTURE_WRAP_T, wrapY);
        if (server.getCaps().anisotropySupport()) {
            float maxAnisotropy = Math.min(SamplerState.getMaxAnisotropy(samplerState),
                    server.getCaps().maxTextureMaxAnisotropy());
            assert (maxAnisotropy >= 1.0f);
            glSamplerParameterf(sampler, GL46C.GL_TEXTURE_MAX_ANISOTROPY, maxAnisotropy);
        } else {
            assert (!SamplerState.isAnisotropy(samplerState));
        }
        return new GLSampler(server, sampler);
    }

    private static int filterModeToMagFilter(int filterMode) {
        return switch (filterMode) {
            case SamplerState.FILTER_MODE_NEAREST -> GL_NEAREST;
            case SamplerState.FILTER_MODE_LINEAR -> GL_LINEAR;
            default -> throw new IllegalArgumentException();
        };
    }

    private static int filterModeToMinFilter(int filterMode, int mipmapMode) {
        return switch (mipmapMode) {
            case SamplerState.MIPMAP_MODE_NONE -> filterModeToMagFilter(filterMode);
            case SamplerState.MIPMAP_MODE_NEAREST -> switch (filterMode) {
                case SamplerState.FILTER_MODE_NEAREST -> GL_NEAREST_MIPMAP_NEAREST;
                case SamplerState.FILTER_MODE_LINEAR -> GL_LINEAR_MIPMAP_NEAREST;
                default -> throw new IllegalArgumentException();
            };
            case SamplerState.MIPMAP_MODE_LINEAR -> switch (filterMode) {
                case SamplerState.FILTER_MODE_NEAREST -> GL_NEAREST_MIPMAP_LINEAR;
                case SamplerState.FILTER_MODE_LINEAR -> GL_LINEAR_MIPMAP_LINEAR;
                default -> throw new IllegalArgumentException();
            };
            default -> throw new IllegalArgumentException();
        };
    }

    private static int wrapModeToWrap(int wrapMode) {
        return switch (wrapMode) {
            case SamplerState.WRAP_MODE_REPEAT -> GL_REPEAT;
            case SamplerState.WRAP_MODE_MIRROR_REPEAT -> GL_MIRRORED_REPEAT;
            case SamplerState.WRAP_MODE_CLAMP_TO_EDGE -> GL_CLAMP_TO_EDGE;
            case SamplerState.WRAP_MODE_CLAMP_TO_BORDER -> GL_CLAMP_TO_BORDER;
            default -> throw new IllegalArgumentException();
        };
    }

    @Override
    protected void dispose() {
        if (mSampler != 0) {
            glDeleteSamplers(mSampler);
            mSampler = 0;
        }
    }

    public void discard() {
        mSampler = 0;
    }

    public int getSamplerID() {
        return mSampler;
    }
}
