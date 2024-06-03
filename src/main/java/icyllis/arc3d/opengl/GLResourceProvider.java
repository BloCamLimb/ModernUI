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
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;

import javax.annotation.Nullable;

import static org.lwjgl.opengl.GL33C.GL_RENDERBUFFER;

/**
 * Provides OpenGL objects with cache.
 */
public final class GLResourceProvider extends ResourceProvider {

    private static final int SAMPLER_CACHE_SIZE = 32;

    private final GLDevice mDevice;

    // LRU cache, samplers are shared by mHWTextureSamplers and mSamplerCache
    private final Int2ObjectLinkedOpenHashMap<GLSampler> mSamplerCache =
            new Int2ObjectLinkedOpenHashMap<>(SAMPLER_CACHE_SIZE);

    private final GLFramebuffer.ResourceKey mFramebufferKey = new GLFramebuffer.ResourceKey();

    GLResourceProvider(GLDevice device, Context context) {
        super(device, context);
        mDevice = device;
    }

    void discard() {
        mSamplerCache.values().forEach(GLSampler::discard);
        release();
    }

    void release() {
        mSamplerCache.values().forEach(GLSampler::unref);
        mSamplerCache.clear();
    }

    @SharedPtr
    @Override
    protected GLGraphicsPipeline createGraphicsPipeline(PipelineDesc pipelineDesc,
                                                      RenderPassDesc renderPassDesc) {
        return GLGraphicsPipelineBuilder.createGraphicsPipeline(mDevice, pipelineDesc);
    }

    @Nullable
    @SharedPtr
    @Override
    protected GLImage onCreateNewImage(ImageDesc desc,
                                       boolean budgeted) {
        if (!(desc instanceof GLImageDesc glImageDesc)) {
            return null;
        }
        if (glImageDesc.mTarget == GL_RENDERBUFFER) {
            return GLRenderbuffer.make(mContext, glImageDesc, budgeted);
        } else {
            return GLTexture.make(mContext, glImageDesc, budgeted);
        }
    }

    @Nullable
    @SharedPtr
    public GLFramebuffer findOrCreateFramebuffer(FramebufferDesc framebufferDesc) {
        @SharedPtr
        GLFramebuffer framebuffer = (GLFramebuffer) mResourceCache.findAndRefResource(
                mFramebufferKey.compute(framebufferDesc),
                /*budgeted*/true);
        if (framebuffer != null) {
            return framebuffer;
        }
        framebuffer = GLFramebuffer.make(mContext, framebufferDesc);
        if (framebuffer == null) {
            return null;
        }
        var key = mFramebufferKey.copy();
        framebuffer.setKey(key);
        mResourceCache.insertResource(framebuffer);
        return framebuffer;
    }

    @Nullable
    @SharedPtr
    @Override
    protected GLBuffer onCreateNewBuffer(long size, int usage) {
        return GLBuffer.make(mContext, size, usage);
    }

    /**
     * Finds or creates a compatible {@link GLSampler} based on the SamplerState.
     *
     * @param samplerState see {@link SamplerState}
     * @return the sampler object, or null if failed
     */
    @Nullable
    @SharedPtr
    public GLSampler findOrCreateCompatibleSampler(int samplerState) {
        GLSampler sampler = mSamplerCache.getAndMoveToFirst(samplerState);
        if (sampler == null) {
            sampler = GLSampler.create(mDevice, samplerState);
            if (sampler == null) {
                return null;
            }
            while (mSamplerCache.size() >= SAMPLER_CACHE_SIZE) {
                mSamplerCache.removeLast().unref();
            }
            mSamplerCache.putAndMoveToFirst(samplerState, sampler);
        }
        sampler.ref();
        return sampler;
    }
}
