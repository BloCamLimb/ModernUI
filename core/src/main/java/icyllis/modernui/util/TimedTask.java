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

package icyllis.modernui.util;

import javax.annotation.Nonnull;

/**
 * Scheduled runnable task with an execution time.
 */
public class TimedTask {

    @Nonnull
    public final Runnable mRunnable;
    public final long mExecutionTime;

    public TimedTask(@Nonnull Runnable runnable, long executionTime) {
        mRunnable = runnable;
        mExecutionTime = executionTime;
    }

    /**
     * Ticks the task.
     *
     * @param currTime current time, keep the same unit of the constructor
     * @return {@code true} task is handled
     */
    public boolean doExecuteTask(long currTime) {
        if (currTime >= mExecutionTime) {
            mRunnable.run();
            return true;
        }
        return false;
    }
}
