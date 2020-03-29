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

package icyllis.modernui.gui.module;

import icyllis.modernui.gui.element.IElement;
import net.minecraft.client.gui.IGuiEventListener;

import java.util.Collections;
import java.util.List;

public interface IGuiModule extends IElement, IGuiEventListener {

    default List<? extends IElement> getElements() {
        return Collections.emptyList();
    }

    default List<? extends IGuiEventListener> getEventListeners() {
        return Collections.emptyList();
    }

    default void onModuleChanged(int newID) {}

    default void draw(float currentTime) {
        getElements().forEach(e -> e.draw(currentTime));
    }

    default void resize(int width, int height) {
        getElements().forEach(e -> e.resize(width, height));
    }

    default void tick(int ticks) {
        getElements().forEach(e -> e.tick(ticks));
    }

    default void mouseMoved(double mouseX, double mouseY) {
        getEventListeners().forEach(e -> e.mouseMoved(mouseX, mouseY));
    }

    default boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        for (IGuiEventListener listener : getEventListeners()) {
            if (listener.mouseClicked(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    default boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        for (IGuiEventListener listener : getEventListeners()) {
            if (listener.mouseReleased(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    default boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        for (IGuiEventListener listener : getEventListeners()) {
            if (listener.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY)) {
                return true;
            }
        }
        return false;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        for (IGuiEventListener listener : getEventListeners()) {
            if (listener.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return false;
    }

    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (IGuiEventListener listener : getEventListeners()) {
            if (listener.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    default boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        for (IGuiEventListener listener : getEventListeners()) {
            if (listener.keyReleased(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    default boolean charTyped(char ch, int modifiers) {
        for (IGuiEventListener listener : getEventListeners()) {
            if (listener.charTyped(ch, modifiers)) {
                return true;
            }
        }
        return false;
    }
}
