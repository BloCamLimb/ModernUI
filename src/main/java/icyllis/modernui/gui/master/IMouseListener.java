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

import org.lwjgl.glfw.GLFW;

public interface IMouseListener {

    /**
     * Check if mouse hover this widget, and update member variable for mouse event
     * @param mouseX scaled mouse X pos
     * @param mouseY scaled mouse Y pos
     * @return return true to cancel the event
     */
    default boolean updateMouseHover(double mouseX, double mouseY) {
        return false;
    }

    /**
     * Called when mouse hover and a mouse button clicked
     * See {@link #isMouseHovered()}
     * @param mouseButton See {@link GLFW}
     * @return return true to cancel the event
     */
    default boolean mouseClicked(int mouseButton) {
        return false;
    }

    /**
     * Called when mouse hover and a mouse button released
     * See {@link #isMouseHovered()}
     * @param mouseButton See {@link GLFW}
     * @return return true to cancel the event
     */
    default boolean mouseReleased(int mouseButton) {
        return false;
    }

    /**
     * Called when mouse hover and a mouse button released
     * See {@link #isMouseHovered()}
     * @param amount scroll amount
     * @return return true to cancel the event
     */
    default boolean mouseScrolled(double amount) {
        return false;
    }

    /**
     * Return if mouse over this widget
     * See {@link #updateMouseHover(double, double)}
     * @return is mouse over
     */
    default boolean isMouseHovered() {
        return false;
    }
}
