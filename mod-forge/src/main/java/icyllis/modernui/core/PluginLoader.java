/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.core;

import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

@ApiStatus.Internal
public final class PluginLoader {

    private static PluginLoader sInstance;

    @Nonnull
    private final ExecutorService mParallelThreadPool;

    private PluginLoader(@Nullable ExecutorService parallelThreadPool) {
        if (parallelThreadPool == null) {
            mParallelThreadPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                    pool -> {
                        ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                        thread.setName("mui-loading-worker-" + thread.getPoolIndex());
                        thread.setContextClassLoader(Thread.currentThread().getContextClassLoader());
                        return thread;
                    }, null, true);
        } else
            mParallelThreadPool = parallelThreadPool;
    }

    @Nonnull
    public static PluginLoader create(@Nullable ExecutorService parallel) {
        if (sInstance == null)
            synchronized (PluginLoader.class) {
                if (sInstance == null)
                    sInstance = new PluginLoader(parallel);
            }
        else throw new IllegalStateException();
        return sInstance;
    }

    @Nonnull
    public static PluginLoader get() {
        return sInstance;
    }
}
