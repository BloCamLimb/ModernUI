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

package icyllis.modernui.view;

import icyllis.modernui.core.Handler;
import icyllis.modernui.util.GrowingArrayUtils;
import icyllis.modernui.util.TimedAction;

/**
 * Class used to enqueue pending work from Views when no Window is attached.
 */
public class HandlerActionQueue {

    private TimedAction[] mActions;
    private int mCount;

    public void post(Runnable action) {
        postDelayed(action, 0);
    }

    public void postDelayed(Runnable action, long delayMillis) {
        final TimedAction timedAction = TimedAction.obtain(action, delayMillis);

        synchronized (this) {
            if (mActions == null) {
                mActions = new TimedAction[2];
            }
            mActions = GrowingArrayUtils.append(mActions, mCount, timedAction);
            mCount++;
        }
    }

    public void removeCallbacks(Runnable action) {
        synchronized (this) {
            final int count = mCount;
            int j = 0;

            final TimedAction[] actions = mActions;
            for (int i = 0; i < count; i++) {
                if (actions[i].remove(action)) {
                    // Remove this action by overwriting it within
                    // this loop or nulling it out later.
                    continue;
                }

                if (j != i) {
                    // At least one previous entry was removed, so
                    // this one needs to move to the "new" list.
                    actions[j] = actions[i];
                }

                j++;
            }

            // The "new" list only has j entries.
            mCount = j;

            // Null out any remaining entries.
            for (; j < count; j++) {
                actions[j] = null;
            }
        }
    }

    public void executeActions(Handler handler) {
        synchronized (this) {
            final TimedAction[] actions = mActions;
            for (int i = 0, count = mCount; i < count; i++) {
                final TimedAction action = actions[i];
                handler.postDelayed(action.action, action.time);
            }

            mActions = null;
            mCount = 0;
        }
    }

    public int size() {
        return mCount;
    }

    public Runnable getRunnable(int index) {
        if (index >= mCount) {
            throw new IndexOutOfBoundsException();
        }
        return mActions[index].action;
    }

    public long getDelay(int index) {
        if (index >= mCount) {
            throw new IndexOutOfBoundsException();
        }
        return mActions[index].time;
    }
}
