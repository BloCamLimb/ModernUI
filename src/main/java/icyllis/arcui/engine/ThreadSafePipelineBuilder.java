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

package icyllis.arcui.engine;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class ThreadSafePipelineBuilder {

    protected final Stats mStats = new Stats();

    public ThreadSafePipelineBuilder() {
    }

    protected abstract void close();

    public final Stats stats() {
        return mStats;
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
    }
}
