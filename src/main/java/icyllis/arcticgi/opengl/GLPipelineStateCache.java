/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.opengl;

import icyllis.arcticgi.engine.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;

import javax.annotation.Nullable;

public class GLPipelineStateCache extends ThreadSafePipelineBuilder {

    private final GLServer mServer;
    private final int mRuntimeProgramCacheSize;
    private final Object2ObjectLinkedOpenCustomHashMap<Object, GLPipelineState> mCache;
    private final ProgramDesc mLookupDesc = new ProgramDesc();

    GLPipelineStateCache(GLServer server, int runtimeProgramCacheSize) {
        mServer = server;
        mRuntimeProgramCacheSize = runtimeProgramCacheSize;
        mCache = new Object2ObjectLinkedOpenCustomHashMap<>(runtimeProgramCacheSize, ProgramDesc.HASH_STRATEGY);
    }

    public void discard() {
        mCache.values().forEach(GLPipelineState::discard);
        reset();
    }

    public void reset() {
        mCache.values().forEach(GLPipelineState::reset);
        mCache.clear();
    }

    @Nullable
    public GLPipelineState findOrCreatePipelineState(final ProgramInfo programInfo) {
        final Caps caps = mServer.getCaps();
        final ProgramDesc desc = caps.makeDesc(mLookupDesc, /*renderTarget*/null, programInfo);
        assert (!desc.isEmpty());
        GLPipelineState pipelineState = findOrCreatePipelineStateImpl(desc, programInfo);
        if (pipelineState == null) {
            mStats.incNumInlineCompilationFailures();
        }
        return pipelineState;
    }

    @Nullable
    public GLPipelineState findOrCreatePipelineState(final ProgramDesc desc,
                                                     final ProgramInfo programInfo) {
        assert (!desc.isEmpty());
        GLPipelineState pipelineState = findOrCreatePipelineStateImpl(desc, programInfo);
        if (pipelineState == null) {
            mStats.incNumPreCompilationFailures();
        }
        return pipelineState;
    }

    @Nullable
    private GLPipelineState findOrCreatePipelineStateImpl(final ProgramDesc desc,
                                                          final ProgramInfo programInfo) {
        GLPipelineState entry = mCache.get(desc);
        if (entry != null) {
            return entry;
        }
        // We have a cache miss
        GLPipelineState pipelineState = GLPipelineStateBuilder.createPipelineState(mServer, desc, programInfo);
        if (pipelineState == null) {
            mStats.incNumCompilationFailures();
            return null;
        }
        mStats.incNumCompilationSuccesses();
        if (mCache.size() >= mRuntimeProgramCacheSize) {
            mCache.removeFirst().reset();
            assert (mCache.size() < mRuntimeProgramCacheSize);
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
