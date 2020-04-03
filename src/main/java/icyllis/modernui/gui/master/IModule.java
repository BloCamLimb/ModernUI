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

public interface IModule {

    /**
     * Draw content you want, called every frame
     * You'd better do animations update at the top of lines
     *
     * @param time elapsed time from a gui open
     *                    unit: floating point ticks, 20.0 ticks = 1 second
     */
    default void draw(float time) {}

    /**
     * Called when game window size changed, used to reset position, use layout for multiple elements
     *
     * @param width scaled game window width
     * @param height scaled game window height
     */
    default void resize(int width, int height) {}

    /**
     * Ticks something you like, used by % calculation to update gui values or state
     *
     * @param ticks elapsed ticks from a gui open, 20 tick = 1 second
     */
    default void tick(int ticks) {}

    /**
     * Called when mouse cursor moved
     * @param mouseX mouse x pos
     * @param mouseY mouse y pos
     * @return return true to cancel the event
     */
    default boolean mouseMoved(double mouseX, double mouseY) {
        return false;
    }

    /**
     * Called when mouse cursor moved
     * @param mouseX mouse x pos
     * @param mouseY mouse y pos
     * @param deltaX mouse x pos change
     * @param deltaY mouse y pos change
     * @return return true to cancel the event
     */
    default boolean mouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        return false;
    }

    /**
     * Called when a mouse button clicked
     * @param mouseX mouse x pos
     * @param mouseY mouse y pos
     * @param mouseButton See {@link GLFW}
     * @return return true to cancel the event
     */
    default boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    /**
     * Called when a mouse button released
     * @param mouseX mouse x pos
     * @param mouseY mouse y pos
     * @param mouseButton See {@link GLFW}
     * @return return true to cancel the event
     */
    default boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    /**
     * Called when a mouse scrolled
     * @param mouseX mouse x pos
     * @param mouseY mouse y pos
     * @param amount scroll amount
     * @return return true to cancel the event
     */
    default boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

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
