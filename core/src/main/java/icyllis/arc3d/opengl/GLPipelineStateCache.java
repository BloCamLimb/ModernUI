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

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;

//TODO cache trim
public class GLPipelineStateCache extends PipelineStateCache {

    private final GLEngine mEngine;

    private final int mCacheSize;
    private final ConcurrentHashMap<Key, GLPipelineState> mCache;

    @VisibleForTesting
    public GLPipelineStateCache(GLEngine engine, int cacheSize) {
        mEngine = engine;
        mCacheSize = cacheSize;
        mCache = new ConcurrentHashMap<>(cacheSize, Hash.FAST_LOAD_FACTOR);
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
    public GLPipelineState findOrCreatePipelineState(final PipelineDesc desc,
                                                     final PipelineInfo pipelineInfo) {
        if (desc.isEmpty()) {
            final Caps caps = mEngine.getCaps();
            caps.makeDesc(desc, /*renderTarget*/null, pipelineInfo);
        }
        assert (!desc.isEmpty());
        return findOrCreatePipelineStateImpl(desc, pipelineInfo);
    }

    @Nonnull
    private GLPipelineState findOrCreatePipelineStateImpl(PipelineDesc desc,
                                                          final PipelineInfo pipelineInfo) {
        GLPipelineState entry = mCache.get(desc);
        if (entry != null) {
            return entry;
        }
        // We have a cache miss
        desc = new PipelineDesc(desc);
        GLPipelineState pipelineState = GLPipelineStateBuilder.createPipelineState(mEngine, desc, pipelineInfo);
        entry = mCache.putIfAbsent(desc.toKey(), pipelineState);
        if (entry != null) {
            // race
            pipelineState.discard();
            return entry;
        }
        /*if (mCache.size() >= mCacheSize) {
            mCache.removeFirst().release();
            assert (mCache.size() < mCacheSize);
        }*/
        return pipelineState;
    }

    @Override
    public void close() {
        assert (mCache.isEmpty());
    }
}
