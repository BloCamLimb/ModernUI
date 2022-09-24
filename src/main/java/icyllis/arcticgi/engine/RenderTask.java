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

package icyllis.arcticgi.engine;

import icyllis.arcticgi.core.RefCnt;
import icyllis.arcticgi.core.SharedPtr;
import icyllis.arcticgi.engine.ops.OpsTask;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class abstracts a task that targets a single {@link SurfaceProxy}, participates in the
 * {@link DrawingManager}'s DAG, and implements the {@link #execute(OpFlushState)} method to
 * modify its target proxy's contents. (e.g., an {@link OpsTask} that executes a command buffer,
 * a {@link TextureResolveTask} that regenerates mipmaps, etc.)
 */
public abstract class RenderTask extends RefCnt {

    private static final AtomicInteger sNextID = new AtomicInteger(1);

    private static int createUniqueID() {
        for (; ; ) {
            final int value = sNextID.get();
            final int newValue = value == -1 ? 1 : value + 1; // 0 is reserved
            if (sNextID.weakCompareAndSetVolatile(value, newValue)) {
                return value;
            }
        }
    }

    protected static final int
            Closed_Flag = 0x01,     // This task can't accept any more dependencies.
            Detached_Flag = 0x02,   // This task is detached from its creating DrawingManager.
            Skippable_Flag = 0x04,  // This task can be skipped.
            Atlas_Flag = 0x08,      // This task is texture atlas.
            InResult_Flag = 0x10,   // Flag for topological sorting
            TempMark_Flag = 0x20;   // Flag for topological sorting

    static final TopologicalSort.Accessor<RenderTask> SORT_ACCESSOR = new TopologicalSort.Accessor<>() {
        @Override
        public void setIndex(@Nonnull RenderTask node, int index) {
            node.setIndex(index);
            node.mFlags |= InResult_Flag;
        }

        @Override
        public int getIndex(@Nonnull RenderTask node) {
            return node.getIndex();
        }

        @Override
        public boolean isInResult(@Nonnull RenderTask node) {
            return (node.mFlags & InResult_Flag) != 0;
        }

        @Override
        public void setTempMarked(@Nonnull RenderTask node, boolean marked) {
            if (marked) {
                node.mFlags |= TempMark_Flag;
            } else {
                node.mFlags &= ~TempMark_Flag;
            }
        }

        @Override
        public boolean isTempMarked(@Nonnull RenderTask node) {
            return (node.mFlags & TempMark_Flag) != 0;
        }

        @Override
        public List<RenderTask> getEdges(@Nonnull RenderTask node) {
            return node.mDependencies;
        }
    };

    private final int mUniqueID;
    private int mFlags;

    // 'this' RenderTask relies on the output of the RenderTasks in 'mDependencies'
    private final List<RenderTask> mDependencies = new ArrayList<>();
    // 'this' RenderTask's output is relied on by the RenderTasks in 'mDependents'
    private final List<RenderTask> mDependents = new ArrayList<>();

    @SharedPtr
    protected SurfaceProxy mTarget;

    public RenderTask() {
        mUniqueID = createUniqueID();
    }

    public final int getUniqueID() {
        return mUniqueID;
    }

    public int numTargets() {
        return mTarget != null ? 1 : 0;
    }

    public SurfaceProxy target(int index) {
        assert index == 0 && numTargets() == 1;
        return mTarget;
    }

    public final SurfaceProxy target() {
        assert numTargets() == 1;
        return mTarget;
    }

    protected void setIndex(int index) {
        assert (mFlags & InResult_Flag) == 0;
        assert (index >= 0 && index < (1 << 26));
        mFlags |= index << 6;
    }

    protected int getIndex() {
        assert (mFlags & InResult_Flag) != 0;
        return mFlags >>> 6;
    }

    @Override
    protected void dispose() {
        assert (mFlags & Detached_Flag) != 0;
        mTarget = RefCnt.move(mTarget);
    }

    /**
     * This method will be invoked at record-time to create pipeline information
     * and pre-bake input data on a background thread.
     */
    public void prePrepare(RecordingContext context) {
        // OpsTask and DeferredListTask override this
    }

    /**
     * This method will be invoked at flush-time to create pipeline information
     * and build input buffers.
     */
    public void prepare(OpFlushState flushState) {
        // OpsTask and DeferredListTask override this
    }

    /**
     * This method will be invoked at flush-time to execute commands.
     */
    public abstract boolean execute(OpFlushState flushState);

    public void makeClosed(RecordingContext context) {
        if (isClosed()) {
            return;
        }

        mFlags |= Closed_Flag;
    }

    public final boolean isClosed() {
        return (mFlags & Closed_Flag) != 0;
    }

    /**
     * This method "disowns" all the SurfaceProxies this RenderTask modifies. In
     * practice this just means telling the drawing manager to forget the relevant
     * mappings from surface proxy to last modifying render task.
     */
    public void detach(DrawingManager drawingManager) {
        assert isClosed();
        if ((mFlags & Detached_Flag) != 0) {
            return;
        }
        mFlags |= Detached_Flag;
    }

    /**
     * Make this task skippable. This must be used purely for optimization purposes
     * at this point as not all tasks will actually skip their work. It would be better if we could
     * detect tasks that can be skipped automatically. We'd need to support minimal flushes (i.e.,
     * only flush that which is required for SkSurfaces/SkImages) and the ability to detect
     * "orphaned tasks" and clean them out from the DAG so they don't indefinitely accumulate.
     * Finally, we'd probably have to track whether a proxy's backing store was imported or ever
     * exported to the client in case the client is doing direct reads outside of Skia and thus
     * may require tasks targeting the proxy to execute even if our DAG contains no reads.
     */
    public void makeSkippable() {

    }

    public final boolean isSkippable() {
        return (mFlags & Skippable_Flag) != 0;
    }
}
