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

public interface IKeyboardListener {

    /**
     * Called when a key pressed
     * @param keyCode see {@link GLFW}
     * @param scanCode keyboard scan code
     * @param modifiers modifier key, see {@link GLFW}
     * @return return true to cancel the event
     */
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * Called when a key released
     * @param keyCode see {@link GLFW}
     * @param scanCode keyboard scan code
     * @param modifiers modifier key, see {@link GLFW}
     * @return return true to cancel the event
     */
    default boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * Called when a unicode key pressed
     * @param codePoint chat code
     * @param modifiers modifier key, see {@link GLFW}
     * @return return true to cancel the event
     */
    default boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

}
