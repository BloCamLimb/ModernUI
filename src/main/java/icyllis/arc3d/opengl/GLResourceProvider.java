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

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;

import javax.annotation.Nullable;

/**
 * Provides OpenGL objects with cache.
 */
public final class GLResourceProvider extends ResourceProvider {

    private static final int SAMPLER_CACHE_SIZE = 32;

    private final GLServer mServer;

    // LRU cache, samplers are shared by mHWTextureSamplers and mSamplerCache
    private final Int2ObjectLinkedOpenHashMap<GLSampler> mSamplerCache =
            new Int2ObjectLinkedOpenHashMap<>(SAMPLER_CACHE_SIZE);

    GLResourceProvider(GLServer server, DirectContext context) {
        super(server, context);
        mServer = server;
    }

    void discard() {
        mSamplerCache.values().forEach(GLSampler::discard);
        release();
    }

    void release() {
        mSamplerCache.values().forEach(GLSampler::unref);
        mSamplerCache.clear();
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
            sampler = GLSampler.create(mServer, samplerState);
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
