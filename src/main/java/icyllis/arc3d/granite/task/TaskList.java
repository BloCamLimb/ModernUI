/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite.task;

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.RecordingContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Consumer;

public class TaskList implements Consumer<@SharedPtr Task>, AutoCloseable {

    private final ObjectArrayList<@SharedPtr Task> mTasks = new ObjectArrayList<>();

    public TaskList() {
    }

    public void appendTask(@SharedPtr Task task) {
        mTasks.add(task);
    }

    public void prependTask(@SharedPtr Task task) {
        mTasks.add(0, task);
    }

    /**
     * Same as {@link #appendTask(Task)}
     */
    @Override
    public void accept(@SharedPtr Task task) {
        mTasks.add(task);
    }

    /**
     * This method moves the given task list.
     */
    public void appendTasks(@NonNull TaskList tasks) {
        assert tasks != this;
        mTasks.addAll(tasks.mTasks);
        tasks.mTasks.clear();
    }

    /**
     * This method moves the given task list.
     */
    public void appendTasks(@NonNull List<@SharedPtr ? extends Task> tasks) {
        mTasks.addAll(tasks);
        tasks.clear();
    }

    public int size() {
        return mTasks.size();
    }

    public boolean isEmpty() {
        return mTasks.isEmpty();
    }

    public void clear() {
        for (var task : mTasks) {
            if (task != null) {
                task.unref();
            }
        }
        mTasks.clear();
    }

    public int prepare(RecordingContext context) {
        int discardCount = 0;
        for (var it = mTasks.listIterator(); it.hasNext(); ) {
            var task = it.next();
            if (task == null) {
                discardCount++;
                continue;
            }

            int result = task.prepare(context);
            if (result == Task.RESULT_FAILURE) {
                return Task.RESULT_FAILURE;
            } else if (result == Task.RESULT_DISCARD) {
                task.unref();
                it.set(null);
                discardCount++;
            }
        }

        return discardCount == mTasks.size() ? Task.RESULT_DISCARD : Task.RESULT_SUCCESS;
    }

    public int execute(ImmediateContext context,
                       CommandBuffer commandBuffer) {
        int discardCount = 0;
        for (var it = mTasks.listIterator(); it.hasNext(); ) {
            var task = it.next();
            if (task == null) {
                discardCount++;
                continue;
            }

            int result = task.execute(context, commandBuffer);
            if (result == Task.RESULT_FAILURE) {
                return Task.RESULT_FAILURE;
            } else if (result == Task.RESULT_DISCARD) {
                task.unref();
                it.set(null);
                discardCount++;
            }
        }

        return discardCount == mTasks.size() ? Task.RESULT_DISCARD : Task.RESULT_SUCCESS;
    }

    @Override
    public void close() {
        clear();
    }
}
