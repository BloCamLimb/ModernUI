/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.RawPtr;

import java.util.ArrayDeque;

/**
 * A {@link QueueManager} represents a GPU queue and manages a pool of command buffers
 * in that queue.
 */
public abstract class QueueManager {

    private final Device mDevice;
    protected ImmediateContext mContext;
    protected CommandBuffer mCurrentCommandBuffer;

    private final ArrayDeque<CommandBuffer> mAvailableCommandBuffers = new ArrayDeque<>();
    private final ArrayDeque<CommandBuffer> mOutstandingCommandBuffers = new ArrayDeque<>();

    private int mMaxCommandBuffers;

    protected QueueManager(Device device) {
        mDevice = device;
    }

    public boolean addTask(@RawPtr Task task) {
        if (task == null) {
            return false;
        }

        if (!prepareCommandBuffer(mContext.getResourceProvider())) {
            return false;
        }

        return task.execute(mContext, mCurrentCommandBuffer) != Task.RESULT_FAILURE;
    }

    public boolean submit() {
        if (mCurrentCommandBuffer == null) {
            return true;
        }

        if (mCurrentCommandBuffer.submit(this)) {
            mOutstandingCommandBuffers.add(mCurrentCommandBuffer);
            mMaxCommandBuffers = Math.max(mMaxCommandBuffers, mOutstandingCommandBuffers.size());
            mCurrentCommandBuffer = null;
            return true;
        }

        return false;
    }

    public int getMaxCommandBuffers() {
        return mMaxCommandBuffers;
    }

    /**
     * Returns true if there is any unfinished GPU work.
     */
    public boolean hasOutstandingWork() {
        return !mOutstandingCommandBuffers.isEmpty();
    }

    /**
     * Blocks the current thread and waits for queue to finish outstanding works.
     */
    public void finishOutstandingWork() {
        // wait for the last submission to finish
        var last = mOutstandingCommandBuffers.peekLast();
        if (last != null) {
            last.waitUntilFinished();
        }
        checkForFinishedWork();
        assert mOutstandingCommandBuffers.isEmpty();
    }

    public void checkForFinishedWork() {
        // Iterate over all the outstanding submissions to see if any have finished. The work
        // submissions are in order from oldest to newest, so we start at the front to check if they
        // have finished. If so we pop it off and move onto the next.
        // Repeat till we find a submission that has not finished yet (and all others afterwards are
        // also guaranteed to not have finished).
        var front = mOutstandingCommandBuffers.peekFirst();
        while (front != null && front.checkFinishedAndReset()) {
            mAvailableCommandBuffers.push(mOutstandingCommandBuffers.removeFirst());
            front = mOutstandingCommandBuffers.peekFirst();
        }
    }

    protected boolean prepareCommandBuffer(ResourceProvider resourceProvider) {
        if (mCurrentCommandBuffer == null) {
            if (!mAvailableCommandBuffers.isEmpty()) {
                mCurrentCommandBuffer = mAvailableCommandBuffers.pop();
                mCurrentCommandBuffer.begin();
                return true;
            }
            mCurrentCommandBuffer = createNewCommandBuffer(resourceProvider);
            if (mCurrentCommandBuffer == null) {
                return false;
            }
            mCurrentCommandBuffer.begin();
        }
        return true;
    }

    protected abstract CommandBuffer createNewCommandBuffer(ResourceProvider resourceProvider);
}
