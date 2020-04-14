/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.gui.widget;

import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.IElement;

public abstract class AnimatedElement implements IElement {

    protected GlobalModuleManager manager = GlobalModuleManager.INSTANCE;

    // 0=close 1=opening 2=open 3=closing
    private int openState = 0;

    private boolean lockState = false;

    private boolean prepareToOpen = false;

    private boolean prepareToClose = false;

    public AnimatedElement() {

    }

    @Override
    public void draw(float time) {
        checkState();
    }

    private void checkState() {
        if (prepareToOpen && openState == 0) {
            openState = 1;
            createOpenAnimations();
            prepareToOpen = false;
        } else if (prepareToClose && openState == 2) {
            openState = 3;
            createCloseAnimations();
            prepareToClose = false;
        }
    }

    /**
     * Create open animations and set open state to 2 on last animation finished
     */
    protected abstract void createOpenAnimations();

    /**
     * Create close animations and set open state to 0 on last animation finished
     */
    protected abstract void createCloseAnimations();

    protected final void startOpenAnimation() {
        if (!lockState && openState != 2) {
            prepareToOpen = true;
            prepareToClose = false;
        }
    }

    protected final void startCloseAnimation() {
        if (!lockState && openState != 0) {
            prepareToClose = true;
            prepareToOpen = false;
        }
    }

    public final void setOpenState(boolean fullOpen) {
        this.openState = fullOpen ? 2 : 0;
    }

    public final boolean isAnimationOpen() {
        return openState != 0;
    }

    public final void setLockState(boolean lock) {
        this.lockState = lock;
    }

    public final boolean canChangeState() {
        return !lockState;
    }
}
