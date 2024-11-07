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

import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.trash.GraphicsPipelineDesc_Old;
import icyllis.arc3d.engine.trash.PipelineKey_old;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated
public class GLPipelineCache {

    private final GLDevice mDevice;

    private final int mCacheSize;
    private final ConcurrentHashMap<Key, GLGraphicsPipeline> mCache;

    @VisibleForTesting
    public GLPipelineCache(GLDevice device, int cacheSize) {
        mDevice = device;
        mCacheSize = cacheSize;
        mCache = new ConcurrentHashMap<>(cacheSize, 0.5f);
    }

    public void discard() {
        mCache.values().forEach(GLGraphicsPipeline::discard);
        release();
    }

    public void release() {
        mCache.values().forEach(GLGraphicsPipeline::unref);
        mCache.clear();
    }

    @Nullable
    public GLGraphicsPipeline findOrCreateGraphicsPipeline(
            final PipelineKey_old desc,
            final GraphicsPipelineDesc_Old graphicsPipelineDesc) {
        if (desc.isEmpty()) {
            final Caps caps = mDevice.getCaps();
            caps.makeDesc(desc, /*renderTarget*/null, graphicsPipelineDesc);
        }
        assert (!desc.isEmpty());
        return findOrCreateGraphicsPipelineImpl(desc, graphicsPipelineDesc);
    }

    @Nonnull
    private GLGraphicsPipeline findOrCreateGraphicsPipelineImpl(
            PipelineKey_old desc,
            final GraphicsPipelineDesc_Old graphicsPipelineDesc) {
        GLGraphicsPipeline existing = mCache.get(desc);
        if (existing != null) {
            return existing;
        }
        // We have a cache miss
        desc = new PipelineKey_old(desc);
        GLGraphicsPipeline newPipeline = null;/*GLGraphicsPipelineBuilder.createGraphicsPipeline(mDevice, desc,
                graphicsPipelineDesc);*/
        existing = mCache.putIfAbsent(desc.toStorageKey(), newPipeline);
        if (existing != null) {
            // there's a race, reuse existing
            newPipeline.discard();
            return existing;
        }
        /*if (mCache.size() >= mCacheSize) {
            mCache.removeFirst().release();
            assert (mCache.size() < mCacheSize);
        }*/
        return newPipeline;
    }

    public void close() {
        assert (mCache.isEmpty());
    }
}
