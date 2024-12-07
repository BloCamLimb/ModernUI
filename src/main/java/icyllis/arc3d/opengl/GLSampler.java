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
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL46C.GL_TEXTURE_MAX_ANISOTROPY;

/**
 * Represents OpenGL sampler objects.
 */
public final class GLSampler extends Sampler {

    private final SamplerDesc mDesc;
    private int mSampler;

    private GLSampler(Context context, SamplerDesc desc) {
        super(context);
        mDesc = desc;
    }

    @Nullable
    @SharedPtr
    public static GLSampler create(Context context,
                                   SamplerDesc desc) {
        GLDevice device = (GLDevice) context.getDevice();
        GLSampler sampler = new GLSampler(context, desc);
        if (device.isOnExecutingThread()) {
            if (!sampler.initialize()) {
                sampler.unref();
                return null;
            }
        } else {
            device.executeRenderCall(dev -> {
                if (!sampler.isDestroyed() && !sampler.initialize()) {
                    sampler.setNonCacheable();
                }
            });
        }
        return sampler;
    }

    // OpenGL thread
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean initialize() {
        GLDevice device = (GLDevice) getDevice();
        int sampler = device.getGL().glGenSamplers();
        if (sampler == 0) {
            return false;
        }
        int magFilter = GLUtil.toGLMagFilter(
                mDesc.getMagFilter()
        );
        int minFilter = GLUtil.toGLMinFilter(
                mDesc.getMinFilter(),
                mDesc.getMipmapMode()
        );
        int wrapX = GLUtil.toGLWrapMode(
                mDesc.getAddressModeX()
        );
        int wrapY = GLUtil.toGLWrapMode(
                mDesc.getAddressModeY()
        );
        int wrapZ = GLUtil.toGLWrapMode(
                mDesc.getAddressModeZ()
        );
        device.getGL().glSamplerParameteri(sampler, GL_TEXTURE_MAG_FILTER, magFilter);
        device.getGL().glSamplerParameteri(sampler, GL_TEXTURE_MIN_FILTER, minFilter);
        device.getGL().glSamplerParameteri(sampler, GL_TEXTURE_WRAP_S, wrapX);
        device.getGL().glSamplerParameteri(sampler, GL_TEXTURE_WRAP_T, wrapY);
        device.getGL().glSamplerParameteri(sampler, GL_TEXTURE_WRAP_R, wrapZ);
        if (device.getCaps().hasAnisotropySupport()) {
            float maxAnisotropy = Math.min(mDesc.getMaxAnisotropy(),
                    device.getCaps().maxTextureMaxAnisotropy());
            assert (maxAnisotropy >= 1.0f);
            device.getGL().glSamplerParameterf(sampler, GL_TEXTURE_MAX_ANISOTROPY, maxAnisotropy);
        }
        // border color is (0,0,0,0) by default
        mSampler = sampler;
        return true;
    }

    @Override
    protected void onRelease() {
        ((GLDevice) getDevice()).executeRenderCall(dev -> {
            if (mSampler != 0) {
                dev.getGL().glDeleteSamplers(mSampler);
            }
            discard();
        });
    }

    public void discard() {
        mSampler = 0;
    }

    public int getHandle() {
        return mSampler;
    }
}
