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
import icyllis.arc3d.engine.CommandBuffer;
import icyllis.arc3d.engine.ImmediateContext;
import icyllis.arc3d.granite.RecordingContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

/**
 * List of ref-counted tasks, only methods defined in this class can be called.
 */
public class TaskList extends ObjectArrayList<@SharedPtr Task>
        implements Consumer<@SharedPtr Task>, AutoCloseable {

    public TaskList() {
    }

    public void appendTask(@SharedPtr Task task) {
        add(task);
    }

    public void prependTask(@SharedPtr Task task) {
        add(0, task);
    }

    /**
     * Same as {@link #appendTask(Task)}
     */
    @Override
    public void accept(@SharedPtr Task task) {
        add(task);
    }

    /**
     * This method moves the given task list.
     */
    public void appendTasks(@NonNull ObjectList<@SharedPtr ? extends Task> tasks) {
        assert tasks != this;
        addAll(tasks);
        tasks.clear();
    }

    public void reset() {
        final Object[] a = this.a;
        for (int i = 0; i < size; i++) {
            var task = (Task) a[i];
            if (task != null) {
                task.unref();
                a[i] = null;
            }
        }
        size = 0;
    }

    public int prepare(RecordingContext context) {
        int discardCount = 0;
        final Object[] a = this.a;
        for (int i = 0; i < size; i++) {
            var task = (Task) a[i];
            if (task == null) {
                discardCount++;
                continue;
            }

            int result = task.prepare(context);
            if (result == Task.RESULT_FAILURE) {
                return Task.RESULT_FAILURE;
            } else if (result == Task.RESULT_DISCARD) {
                task.unref();
                a[i] = null;
                discardCount++;
            }
        }

        return discardCount == size ? Task.RESULT_DISCARD : Task.RESULT_SUCCESS;
    }

    public int execute(ImmediateContext context,
                       CommandBuffer commandBuffer) {
        int discardCount = 0;
        final Object[] a = this.a;
        for (int i = 0; i < size; i++) {
            var task = (Task) a[i];
            if (task == null) {
                discardCount++;
                continue;
            }

            int result = task.execute(context, commandBuffer);
            if (result == Task.RESULT_FAILURE) {
                return Task.RESULT_FAILURE;
            } else if (result == Task.RESULT_DISCARD) {
                task.unref();
                a[i] = null;
                discardCount++;
            }
        }

        return discardCount == size ? Task.RESULT_DISCARD : Task.RESULT_SUCCESS;
    }

    @Override
    public void close() {
        reset();
    }
}
