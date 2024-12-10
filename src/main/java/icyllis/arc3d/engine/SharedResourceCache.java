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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe class to get or create pipeline state objects (PSO) asynchronously.
 */
@ThreadSafe
public class SharedResourceCache {

    protected final Stats mStats = new Stats();

    @GuardedBy("itself")
    private final PipelineCache<@SharedPtr GraphicsPipeline> mGraphicsPipelineCache;
    @GuardedBy("itself")
    private final PipelineCache<@SharedPtr ComputePipeline> mComputePipelineCache;

    @GuardedBy("itself")
    private final ObjectArrayList<@SharedPtr Resource> mStaticResources =
            new ObjectArrayList<>();

    public SharedResourceCache() {
        //TODO configurable
        mGraphicsPipelineCache = new PipelineCache<>(256);
        mComputePipelineCache = new PipelineCache<>(128);
    }

    public void release() {
        synchronized (mGraphicsPipelineCache) {
            mGraphicsPipelineCache.values().forEach(RefCnt::unref);
            mGraphicsPipelineCache.clear();
        }
        synchronized (mComputePipelineCache) {
            mComputePipelineCache.values().forEach(RefCnt::unref);
            mComputePipelineCache.clear();
        }
        synchronized (mStaticResources) {
            mStaticResources.forEach(Resource::unref);
            mStaticResources.clear();
        }
    }

    @Nullable
    @SharedPtr
    public GraphicsPipeline findGraphicsPipeline(@NonNull IUniqueKey key) {
        GraphicsPipeline existing;
        synchronized (mGraphicsPipelineCache) {
            existing = mGraphicsPipelineCache.get(key);
        }
        return RefCnt.create(existing);
    }

    @NonNull
    @SharedPtr
    public GraphicsPipeline insertGraphicsPipeline(@NonNull IUniqueKey key,
                                                   @NonNull @SharedPtr GraphicsPipeline pipeline) {
        GraphicsPipeline existing;
        synchronized (mGraphicsPipelineCache) {
            existing = mGraphicsPipelineCache.putIfAbsent(key, pipeline);
        }
        if (existing != null) {
            // there's a race, reuse existing
            return RefCnt.create(pipeline, existing);
        } else {
            return RefCnt.create(pipeline);
        }
    }

    @Nullable
    @SharedPtr
    public ComputePipeline findComputePipeline(@NonNull IUniqueKey key) {
        ComputePipeline existing;
        synchronized (mComputePipelineCache) {
            existing = mComputePipelineCache.get(key);
        }
        return RefCnt.create(existing);
    }

    @NonNull
    @SharedPtr
    public ComputePipeline insertComputePipeline(@NonNull IUniqueKey key,
                                                 @NonNull @SharedPtr ComputePipeline pipeline) {
        ComputePipeline existing;
        synchronized (mComputePipelineCache) {
            existing = mComputePipelineCache.putIfAbsent(key, pipeline);
        }
        if (existing != null) {
            // there's a race, reuse existing
            return RefCnt.create(pipeline, existing);
        } else {
            return RefCnt.create(pipeline);
        }
    }

    public void addStaticResource(@NonNull @SharedPtr Resource resource) {
        synchronized (mStaticResources) {
            mStaticResources.add(resource);
        }
    }

    public final Stats getStats() {
        return mStats;
    }

    static class PipelineCache<T extends @SharedPtr ManagedResource> extends LinkedHashMap<IUniqueKey, T> {

        private final int maxEntries;

        PipelineCache(int maxEntries) {
            super(/*initialCapacity*/   maxEntries * 2 + 2,
                    /*loadFactor*/      0.5f,
                    /*accessOrder*/     true);
            this.maxEntries = maxEntries;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<IUniqueKey, T> eldest) {
            if (size() > maxEntries) {
                eldest.getValue().unref();
                return true;
            }
            return false;
        }
    }

    public static class Stats {

        private final AtomicInteger mShaderCompilations = new AtomicInteger();

        private final AtomicInteger mNumInlineCompilationFailures = new AtomicInteger();

        private final AtomicInteger mNumPreCompilationFailures = new AtomicInteger();

        private final AtomicInteger mNumCompilationFailures = new AtomicInteger();
        private final AtomicInteger mNumPartialCompilationSuccesses = new AtomicInteger();
        private final AtomicInteger mNumCompilationSuccesses = new AtomicInteger();

        public int shaderCompilations() {
            return mShaderCompilations.get();
        }

        public void incShaderCompilations() {
            mShaderCompilations.getAndIncrement();
        }

        public int numInlineCompilationFailures() {
            return mNumInlineCompilationFailures.get();
        }

        public void incNumInlineCompilationFailures() {
            mNumInlineCompilationFailures.getAndIncrement();
        }

        public int numPreCompilationFailures() {
            return mNumPreCompilationFailures.get();
        }

        public void incNumPreCompilationFailures() {
            mNumPreCompilationFailures.getAndIncrement();
        }

        public int numCompilationFailures() {
            return mNumCompilationFailures.get();
        }

        public void incNumCompilationFailures() {
            mNumCompilationFailures.getAndIncrement();
        }

        public int numPartialCompilationSuccesses() {
            return mNumPartialCompilationSuccesses.get();
        }

        public void incNumPartialCompilationSuccesses() {
            mNumPartialCompilationSuccesses.getAndIncrement();
        }

        public int numCompilationSuccesses() {
            return mNumCompilationSuccesses.get();
        }

        public void incNumCompilationSuccesses() {
            mNumCompilationSuccesses.getAndIncrement();
        }

        @Override
        public String toString() {
            return "SharedResourceCache.Stats{" +
                    "shaderCompilations=" + mShaderCompilations +
                    ", numInlineCompilationFailures=" + mNumInlineCompilationFailures +
                    ", numPreCompilationFailures=" + mNumPreCompilationFailures +
                    ", numCompilationFailures=" + mNumCompilationFailures +
                    ", numPartialCompilationSuccesses=" + mNumPartialCompilationSuccesses +
                    ", numCompilationSuccesses=" + mNumCompilationSuccesses +
                    '}';
        }
    }
}
