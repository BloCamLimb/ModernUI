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

import icyllis.akashigi.engine.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;

import javax.annotation.Nullable;

public class GLPipelineStateCache extends ThreadSafePipelineBuilder {

    private final GLServer mServer;

    private final int mCacheSize;
    private final Object2ObjectLinkedOpenCustomHashMap<Object, GLPipelineState> mCache;

    private final PipelineDesc mLookupDesc = new PipelineDesc();

    GLPipelineStateCache(GLServer server, int cacheSize) {
        mServer = server;
        mCacheSize = cacheSize;
        mCache = new Object2ObjectLinkedOpenCustomHashMap<>(cacheSize, PipelineDesc.HASH_STRATEGY);
    }

    public void discard() {
        mCache.values().forEach(GLPipelineState::discard);
        destroy();
    }

    public void destroy() {
        mCache.values().forEach(GLPipelineState::destroy);
        mCache.clear();
    }

    @Nullable
    public GLPipelineState findOrCreatePipelineState(final PipelineInfo pipelineInfo) {
        final Caps caps = mServer.getCaps();
        final PipelineDesc desc = caps.makeDesc(mLookupDesc, /*renderTarget*/null, pipelineInfo);
        assert (!desc.isEmpty());
        GLPipelineState pipelineState = findOrCreatePipelineStateImpl(desc, pipelineInfo);
        if (pipelineState == null) {
            mStats.incNumInlineCompilationFailures();
        }
        return pipelineState;
    }

    @Nullable
    public GLPipelineState findOrCreatePipelineState(final PipelineDesc desc,
                                                     final PipelineInfo pipelineInfo) {
        assert (!desc.isEmpty());
        GLPipelineState pipelineState = findOrCreatePipelineStateImpl(desc, pipelineInfo);
        if (pipelineState == null) {
            mStats.incNumPreCompilationFailures();
        }
        return pipelineState;
    }

    @Nullable
    private GLPipelineState findOrCreatePipelineStateImpl(final PipelineDesc desc,
                                                          final PipelineInfo pipelineInfo) {
        GLPipelineState entry = mCache.get(desc);
        if (entry != null) {
            return entry;
        }
        // We have a cache miss
        GLPipelineState pipelineState = GLPipelineStateBuilder.createPipelineState(mServer, desc, pipelineInfo);
        if (pipelineState == null) {
            mStats.incNumCompilationFailures();
            return null;
        }
        mStats.incNumCompilationSuccesses();
        if (mCache.size() >= mCacheSize) {
            mCache.removeFirst().destroy();
            assert (mCache.size() < mCacheSize);
        }
        if (mCache.put(desc.toKey(), pipelineState) != null) {
            throw new IllegalStateException();
        }
        return pipelineState;
    }

    @Override
    public void close() {
        assert (mCache.isEmpty());
    }
}
