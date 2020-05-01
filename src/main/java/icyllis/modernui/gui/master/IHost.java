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

package icyllis.modernui.gui.master;

import icyllis.modernui.gui.animation.IAnimation;

import javax.annotation.Nullable;

/**
 * Used to host a widget
 * Used to access core logic methods
 * Used to focus a draggable or keyboard listener
 */
public interface IHost {

    void addAnimation(IAnimation animation);

    int getWindowWidth();

    int getWindowHeight();

    double getAbsoluteMouseX();

    double getAbsoluteMouseY();

    double getRelativeMouseX();

    double getRelativeMouseY();

    float toAbsoluteX(float rx);

    float toAbsoluteY(float ry);

    int getElapsedTicks();

    void refocusMouseCursor();

    void setDraggable(@Nullable IDraggable draggable);

    @Nullable
    IDraggable getDraggable();

    void setKeyboardListener(@Nullable IKeyboardListener keyboardListener);

    @Nullable
    IKeyboardListener getKeyboardListener();
}
