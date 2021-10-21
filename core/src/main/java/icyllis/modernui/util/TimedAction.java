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
import java.util.Objects;

/**
 * A runnable action with an execution time.
 */
public class TimedAction {

    private static final Pool<TimedAction> sPool = Pools.concurrent(20);

    public Runnable action;
    public long time;

    private TimedAction() {
    }

    /**
     * Obtains a TimedAction instance from the global pool.
     *
     * @return an instance
     */
    @Nonnull
    public static TimedAction obtain() {
        TimedAction a = sPool.acquire();
        return a == null ? new TimedAction() : a;
    }

    /**
     * Obtains a TimedAction instance from the global pool.
     *
     * @param r    the action
     * @param time the time
     * @return an instance
     */
    @Nonnull
    public static TimedAction obtain(@Nonnull Runnable r, long time) {
        TimedAction a = sPool.acquire();
        if (a == null) {
            a = new TimedAction();
        }
        a.action = r;
        a.time = time;
        return a;
    }

    /**
     * Executes the task. The action is executed and recycled if <code>now>=time</code>.
     *
     * @param now current time, with the same measurement of that in constructor
     * @return {@code true} if the action is handled
     */
    public boolean execute(long now) {
        if (now >= time) {
            action.run();
            recycle();
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if <code>otherAction</code> is equal to <code>action</code>,
     * and this TimedAction is recycled.
     *
     * @param otherAction action to compare
     * @return matches and recycled
     */
    public boolean remove(Runnable otherAction) {
        if (Objects.equals(action, otherAction)) {
            recycle();
            return true;
        }
        return false;
    }

    /**
     * Releases a TimedAction instance to the global pool.
     */
    public void recycle() {
        action = null;
        sPool.release(this);
    }
}
