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

package icyllis.modernui.test.discard;

import icyllis.modernui.view.UIManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Define methods that generally don't need to implement in View
 * This generally handled by {@link UIManager}
 */
@Deprecated
public interface IHost {

    /**
     * Get parent host if present
     */
    default IHost getParent() {
        return null;
    }

    @Deprecated
    default int getGameWidth() {
        return getParent().getGameWidth();
    }

    @Deprecated
    default int getGameHeight() {
        return getParent().getGameHeight();
    }

    /**
     * Get mouse x relative to whole game window
     */
    @Deprecated
    default double getAbsoluteMouseX() {
        return getParent().getAbsoluteMouseX();
    }

    /**
     * Get mouse y relative to whole game window
     */
    @Deprecated
    default double getAbsoluteMouseY() {
        return getParent().getAbsoluteMouseY();
    }

    /**
     * Get mouse x relative to parent view
     */
    default double getRelativeMouseX() {
        return getParent().getRelativeMouseX();
    }

    /**
     * Get mouse y relative to parent view
     */
    default double getRelativeMouseY() {
        return getParent().getRelativeMouseY();
    }

    /**
     * Transform relative x to absolute x
     */
    default float toAbsoluteX(float rx) {
        return getParent().toAbsoluteX(rx);
    }

    /**
     * Transform relative y to absolute y
     */
    default float toAbsoluteY(float ry) {
        return getParent().toAbsoluteY(ry);
    }

    @Deprecated
    default int getElapsedTicks() {
        return getParent().getElapsedTicks();
    }

    @Deprecated
    default float getAnimationTime() {
        return getParent().getAnimationTime();
    }

    @Deprecated
    default void refocusMouseCursor() {
        getParent().refocusMouseCursor();
    }

    /**
     * Set a draggable widget, the draggable will be null immediately mouse released
     *
     * @param draggable draggable widget
     */
    @Deprecated
    default void setDraggable(@Nonnull IDraggable draggable) {
        getParent().setDraggable(draggable);
    }

    @Nullable
    @Deprecated
    default IDraggable getDraggable() {
        return getParent().getDraggable();
    }

    /**
     * Set keyboard listener to listen key events
     * Set to null to clear listener, but generally this is auto done by UIManager
     *
     * @param keyboardListener listener
     */
    @Deprecated
    default void setKeyboardListener(@Nullable IKeyboardListener keyboardListener) {
        getParent().setKeyboardListener(keyboardListener);
    }

    @Nullable
    @Deprecated
    default IKeyboardListener getKeyboardListener() {
        return getParent().getKeyboardListener();
    }
}
