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

package icyllis.modernui.graphics.opengl;

import icyllis.modernui.graphics.engine.SamplerState;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;

import javax.annotation.Nullable;

/**
 * Provides OpenGL state objects with cache.
 */
public final class GLResourceProvider {

    private static final int SAMPLER_CACHE_SIZE = 32;

    private final GLServer mServer;

    private final Int2ObjectLinkedOpenHashMap<GLSampler> mSamplerCache =
            new Int2ObjectLinkedOpenHashMap<>(SAMPLER_CACHE_SIZE);

    GLResourceProvider(GLServer server) {
        mServer = server;
    }

    void discard() {
        mSamplerCache.values().forEach(GLSampler::discard);
        destroy();
    }

    void destroy() {
        mSamplerCache.values().forEach(GLSampler::unref);
        mSamplerCache.clear();
    }

    /**
     * Finds or creates a compatible {@link GLSampler} based on the SamplerState.
     *
     * @param samplerState see {@link SamplerState}
     * @return raw ptr to the sampler object, or null if failed
     */
    @Nullable
    public GLSampler findOrCreateCompatibleSampler(int samplerState) {
        GLSampler cached = mSamplerCache.get(samplerState);
        if (cached != null) {
            return cached;
        }
        GLSampler sampler = GLSampler.create(mServer, samplerState);
        if (sampler == null) {
            return null;
        }
        if (mSamplerCache.size() >= SAMPLER_CACHE_SIZE) {
            mSamplerCache.removeFirst().unref();
            assert (mSamplerCache.size() < SAMPLER_CACHE_SIZE);
        }
        if (mSamplerCache.put(samplerState, sampler) != null) {
            throw new IllegalStateException();
        }
        return sampler;
    }
}
