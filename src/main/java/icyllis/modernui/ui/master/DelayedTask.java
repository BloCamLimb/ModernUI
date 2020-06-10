/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.master;

import javax.annotation.Nonnull;

public class DelayedTask {

    @Nonnull
    private final Runnable runnable;

    private final int finishTick;

    private boolean finish = false;

    public DelayedTask(@Nonnull Runnable runnable, int delayedTick) {
        this.runnable = runnable;
        this.finishTick = UIManager.INSTANCE.getTicks() + delayedTick;
    }

    public void tick(int ticks) {
        if (ticks >= finishTick) {
            runnable.run();
            finish = true;
        }
    }

    public boolean shouldRemove() {
        return finish;
    }
}
