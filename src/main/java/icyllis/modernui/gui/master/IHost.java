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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Used to host a widget
 * Used to access core logic methods
 * Used to focus a draggable or keyboard listener
 */
public interface IHost {

    /**
     * Get parent host if present
     */
    default IHost getHost() {
        throw new IllegalStateException();
    }

    default int getWindowWidth() {
        return getHost().getWindowWidth();
    }

    default int getWindowHeight() {
        return getHost().getWindowHeight();
    }

    /**
     * Get mouse x relative to global coordinate system
     */
    default double getAbsoluteMouseX() {
        return getHost().getAbsoluteMouseX();
    }

    /**
     * Get mouse y relative to global coordinate system
     */
    default double getAbsoluteMouseY() {
        return getHost().getAbsoluteMouseY();
    }

    /**
     * Get mouse x relative to current coordinate system
     */
    default double getRelativeMouseX() {
        return getHost().getRelativeMouseX();
    }

    /**
     * Get mouse y relative to current coordinate system
     */
    default double getRelativeMouseY() {
        return getHost().getRelativeMouseY();
    }

    /**
     * Transform relative x to absolute x
     */
    default float toAbsoluteX(float rx) {
        return getHost().toAbsoluteX(rx);
    }

    /**
     * Transform relative y to absolute y
     */
    default float toAbsoluteY(float ry) {
        return getHost().toAbsoluteY(ry);
    }

    default int getElapsedTicks() {
        return getHost().getElapsedTicks();
    }

    default float getAnimationTime() {
        return getHost().getAnimationTime();
    }

    default void refocusMouseCursor() {
        getHost().refocusMouseCursor();
    }

    default void setDraggable(@Nonnull IDraggable draggable) {
        getHost().setDraggable(draggable);
    }

    @Nullable
    default IDraggable getDraggable() {
        return getHost().getDraggable();
    }

    default void setKeyboardListener(@Nullable IKeyboardListener keyboardListener) {
        getHost().setKeyboardListener(keyboardListener);
    }

    @Nullable
    default IKeyboardListener getKeyboardListener() {
        return getHost().getKeyboardListener();
    }
}
