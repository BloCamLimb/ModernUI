/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.opengl;

import icyllis.arcui.engine.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;

import javax.annotation.Nullable;

public class GLProgramCache extends ThreadSafePipelineBuilder {

    private final int mRuntimeProgramCacheSize;
    private final Object2ObjectLinkedOpenCustomHashMap<Object, GLProgram> mCache;
    private final ProgramDesc mLookupDesc = new ProgramDesc();

    GLProgramCache(int runtimeProgramCacheSize) {
        mRuntimeProgramCacheSize = runtimeProgramCacheSize;
        mCache = new Object2ObjectLinkedOpenCustomHashMap<>(runtimeProgramCacheSize, ProgramDesc.HASH_STRATEGY);
    }

    public void discard() {
        mCache.values().forEach(GLProgram::discard);
        reset();
    }

    public void reset() {
        mCache.values().forEach(GLProgram::close);
        mCache.clear();
    }

    @Nullable
    public GLProgram findOrCreateProgram(DirectContext dContext,
                                         final ProgramInfo programInfo) {
        final Caps caps = dContext.caps();
        final ProgramDesc desc = caps.makeDesc(mLookupDesc, /*renderTarget*/null, programInfo);
        assert (!desc.isEmpty());
        GLProgram program = findOrCreateProgramImpl(dContext, desc, programInfo);
        if (program == null) {
            mStats.incNumInlineCompilationFailures();
        }
        return program;
    }

    @Nullable
    public GLProgram findOrCreateProgram(DirectContext dContext,
                                         final ProgramDesc desc,
                                         final ProgramInfo programInfo) {
        assert (!desc.isEmpty());
        GLProgram program = findOrCreateProgramImpl(dContext, desc, programInfo);
        if (program == null) {
            mStats.incNumPreCompilationFailures();
        }
        return program;
    }

    @Nullable
    private GLProgram findOrCreateProgramImpl(DirectContext dContext,
                                              final ProgramDesc desc,
                                              final ProgramInfo programInfo) {
        GLProgram entry = mCache.get(desc);
        if (entry != null) {
            return entry;
        }
        // We have a cache miss
        GLProgram program = GLProgramBuilder.createProgram(dContext, desc, programInfo);
        if (program == null) {
            mStats.incNumCompilationFailures();
            return null;
        }
        mStats.incNumCompilationSuccesses();
        if (mCache.size() >= mRuntimeProgramCacheSize) {
            mCache.removeFirst().close();
            assert (mCache.size() < mRuntimeProgramCacheSize);
        }
        if (mCache.put(desc.toKey(), program) != null) {
            throw new IllegalStateException();
        }
        return program;
    }

    @Override
    public void close() {
        assert (mCache.isEmpty());
    }
}
