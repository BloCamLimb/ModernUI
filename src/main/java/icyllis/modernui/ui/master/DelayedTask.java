/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.ui.master;

import javax.annotation.Nonnull;

/**
 * Delayed task used in UI
 *
 * @see UIManager#postTask(Runnable, int)
 */
public class DelayedTask {

    @Nonnull
    private final Runnable runnable;

    private final int finishTick;

    private boolean finish = false;

    DelayedTask(@Nonnull Runnable runnable, int delayedTicks) {
        this.runnable = runnable;
        finishTick = UIManager.INSTANCE.getElapsedTicks() + delayedTicks;
    }

    void tick(int ticks) {
        if (ticks >= finishTick) {
            runnable.run();
            finish = true;
        }
    }

    boolean shouldRemove() {
        return finish;
    }
}
