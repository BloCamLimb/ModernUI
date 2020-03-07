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

package icyllis.modernui.gui.element;

import java.util.function.Function;

public abstract class StateAnimatedElement extends Element {

    // 0=close 1=opening 2=open 3=closing
    protected byte openState = 0;

    private boolean prepareToClose = false;

    private boolean prepareToOpen = false;

    public StateAnimatedElement(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer) {
        super(xResizer, yResizer);
    }

    @Override
    public void draw(float currentTime) {
        if (prepareToOpen && openState == 0) {
            openState = 1;
            open();
            prepareToOpen = false;
        } else if (prepareToClose && openState == 2) {
            openState = 3;
            close();
            prepareToClose = false;
        }
        if (openState == 0) {
            return; // do not draw anymore
        }
    }

    /**
     * Create open animations and set open state to 2 here
     */
    protected abstract void open();

    /**
     * Create close animations and set open state to 0 here
     */
    protected abstract void close();

    public void startOpen() {
        prepareToOpen = true;
        prepareToClose = false;
    }

    public void startClose() {
        prepareToClose = true;
        prepareToOpen = false;
    }
}
