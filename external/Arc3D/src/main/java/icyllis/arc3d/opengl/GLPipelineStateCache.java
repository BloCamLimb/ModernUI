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

import icyllis.arc3d.engine.*;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

//TODO cache trim
public class GLPipelineStateCache extends PipelineStateCache {

    private final GLDevice mDevice;

    private final int mCacheSize;
    private final ConcurrentHashMap<Key, GLGraphicsPipelineState> mCache;

    @VisibleForTesting
    public GLPipelineStateCache(GLDevice device, int cacheSize) {
        mDevice = device;
        mCacheSize = cacheSize;
        mCache = new ConcurrentHashMap<>(cacheSize, 0.5f);
    }

    public void discard() {
        mCache.values().forEach(GLGraphicsPipelineState::discard);
        release();
    }

    public void release() {
        mCache.values().forEach(GLGraphicsPipelineState::release);
        mCache.clear();
    }

    @Nullable
    public GLGraphicsPipelineState findOrCreateGraphicsPipelineState(
            final PipelineDesc desc,
            final PipelineInfo pipelineInfo) {
        if (desc.isEmpty()) {
            final Caps caps = mDevice.getCaps();
            caps.makeDesc(desc, /*renderTarget*/null, pipelineInfo);
        }
        assert (!desc.isEmpty());
        return findOrCreateGraphicsPipelineStateImpl(desc, pipelineInfo);
    }

    @Nonnull
    private GLGraphicsPipelineState findOrCreateGraphicsPipelineStateImpl(
            PipelineDesc desc,
            final PipelineInfo pipelineInfo) {
        GLGraphicsPipelineState existing = mCache.get(desc);
        if (existing != null) {
            return existing;
        }
        // We have a cache miss
        desc = new PipelineDesc(desc);
        GLGraphicsPipelineState newPipelineState = GLPipelineStateBuilder.createGraphicsPipelineState(mDevice, desc,
                pipelineInfo);
        existing = mCache.putIfAbsent(desc.toStorageKey(), newPipelineState);
        if (existing != null) {
            // there's a race, reuse existing
            newPipelineState.discard();
            return existing;
        }
        /*if (mCache.size() >= mCacheSize) {
            mCache.removeFirst().release();
            assert (mCache.size() < mCacheSize);
        }*/
        return newPipelineState;
    }

    @Override
    public void close() {
        assert (mCache.isEmpty());
    }
}
