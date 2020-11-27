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

package icyllis.modernui.ui.test;

import org.lwjgl.glfw.GLFW;

/**
 * Listens mouse events
 *
 * @since 1.6 reworked
 */
@Deprecated
public interface IMouseListener {

    /**
     * Check if mouse hover this widget, and update member variable for mouse event
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @return return {@code true} to cancel the event
     */
    default boolean updateMouseHover(double mouseX, double mouseY) {
        return false;
    }

    /**
     * Called when mouse start to hover on this listener
     *  @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     */
    default void onMouseHoverEnter(double mouseX, double mouseY) {

    }

    /**
     * Called when mouse no longer hover on this listener
     */
    default void onMouseHoverExit() {

    }

    /**
     * Called when mouse hover and a mouse button clicked
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @param button see {@link GLFW}
     * @return return {@code true} if action performed
     */
    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    /**
     * Called when mouse hover and a mouse button released
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @param button see {@link GLFW}
     * @return return {@code true} if action performed
     */
    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    /**
     * Called when mouse hover and a mouse button released
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @param amount scroll amount
     * @return return {@code true} if action performed
     */
    default boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

}
