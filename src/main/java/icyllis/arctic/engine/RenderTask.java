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

package icyllis.arctic.engine;

import icyllis.arctic.core.RefCnt;
import icyllis.arctic.core.SharedPtr;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class abstracts a task that targets a single {@link SurfaceProxy}, participates in the
 * {@link DrawingManager}'s DAG, and implements the onExecute method to modify its target proxy's
 * contents. (e.g., an opsTask that executes a command buffer, a task to regenerate mipmaps, etc.)
 */
public abstract class RenderTask extends RefCnt {

    private static final AtomicInteger sNextID = new AtomicInteger(1);

    private static int createUniqueID() {
        for (;;) {
            final int value = sNextID.get();
            final int newValue = value == -1 ? 1 : value + 1; // 0 is reserved
            if (sNextID.weakCompareAndSetVolatile(value, newValue)) {
                return value;
            }
        }
    }

    protected static final int
            CLOSED_FLAG = 0x01,     // This task can't accept any more dependencies.
            DETACHED_FLAG = 0x02,   // This task is detached from its creating DrawingManager.
            SKIPPABLE_FLAG = 0x04,  // This task can be skipped.
            ATLAS_FLAG = 0x08,      // This task is texture atlas.
            IN_RESULT_FLAG = 0x10,  // Flag for topological sorting
            TEMP_MARK_FLAG = 0x20;  // Flag for topological sorting

    static final TopologicalSort.Accessor<RenderTask> SORT_ACCESSOR = new TopologicalSort.Accessor<>() {
        @Override
        public void setIndex(RenderTask node, int index) {
            node.setIndex(index);
            node.setBooleanFlag(IN_RESULT_FLAG, true);
        }

        @Override
        public int getIndex(RenderTask node) {
            return node.getIndex();
        }

        @Override
        public boolean isInResult(RenderTask node) {
            return node.hasBooleanFlag(IN_RESULT_FLAG);
        }

        @Override
        public void setTempMarked(RenderTask node, boolean marked) {
            node.setBooleanFlag(TEMP_MARK_FLAG, marked);
        }

        @Override
        public boolean isTempMarked(RenderTask node) {
            return node.hasBooleanFlag(TEMP_MARK_FLAG);
        }

        @Override
        public List<RenderTask> getEdges(RenderTask node) {
            return node.mDependencies;
        }
    };

    private final int mUniqueID;
    private int mFlags;

    // 'this' RenderTask relies on the output of the RenderTasks in 'fDependencies'
    private final List<RenderTask> mDependencies = new ArrayList<>();
    // 'this' RenderTask's output is relied on by the RenderTasks in 'fDependents'
    private final List<RenderTask> mDependents = new ArrayList<>();

    @SharedPtr
    protected final List<SurfaceProxy> mTargets = new ArrayList<>();

    private DrawingManager mDrawingManager;

    public RenderTask() {
        mUniqueID = createUniqueID();
    }

    public int getUniqueID() {
        return mUniqueID;
    }

    protected void setBooleanFlag(int flag, boolean value) {
        if (value) {
            mFlags |= flag;
        } else {
            mFlags &= ~flag;
        }
    }

    protected boolean hasBooleanFlag(int flag) {
        return (mFlags & flag) != 0;
    }

    protected void setIndex(int index) {
        assert !hasBooleanFlag(IN_RESULT_FLAG);
        assert (index < (1 << 26));
        mFlags |= index << 6;
    }

    protected int getIndex() {
        assert hasBooleanFlag(IN_RESULT_FLAG);
        return mFlags >> 6;
    }

    @Override
    protected void onFree() {
        assert hasBooleanFlag(DETACHED_FLAG);
    }
}
