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

import icyllis.arc3d.engine.*;
import icyllis.modernui.annotation.Nullable;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import org.jetbrains.annotations.VisibleForTesting;

//TODO compile shader multi-threaded but create backend object on render thread
public class GLPipelineStateCache extends ThreadSafePipelineBuilder {

    private final GLDevice mDevice;

    private final int mCacheSize;
    private final Object2ObjectLinkedOpenHashMap<Key, GLPipelineState> mCache;

    private final PipelineDesc mLookupDesc = new PipelineDesc();

    @VisibleForTesting
    public GLPipelineStateCache(GLDevice device, int cacheSize) {
        mDevice = device;
        mCacheSize = cacheSize;
        mCache = new Object2ObjectLinkedOpenHashMap<>(cacheSize, Hash.FAST_LOAD_FACTOR);
    }

    public void discard() {
        mCache.values().forEach(GLPipelineState::discard);
        release();
    }

    public void release() {
        mCache.values().forEach(GLPipelineState::release);
        mCache.clear();
    }

    @Nullable
    public GLPipelineState findOrCreatePipelineState(final PipelineInfo pipelineInfo) {
        final Caps caps = mDevice.getCaps();
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
        GLPipelineState pipelineState = GLPipelineStateBuilder.createPipelineState(mDevice, desc, pipelineInfo);
        if (pipelineState == null) {
            mStats.incNumCompilationFailures();
            return null;
        }
        mStats.incNumCompilationSuccesses();
        if (mCache.size() >= mCacheSize) {
            mCache.removeFirst().release();
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
