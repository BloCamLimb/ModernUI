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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class Module implements IModule, IFocuser {

    private final GlobalModuleManager manager = GlobalModuleManager.INSTANCE;

    private List<IElement> elements = new ArrayList<>();

    private List<IMouseListener> mouseListeners = new ArrayList<>();

    @Nullable
    private IDraggable draggable;

    @Nullable
    private IKeyboardListener keyboardListener;

    public Module() {

    }

    @Override
    public void draw(float time) {
        elements.forEach(e -> e.draw(time));
    }

    @Override
    public void resize(int width, int height) {
        elements.forEach(e -> e.resize(width, height));
    }

    @Override
    public void tick(int ticks) {
        elements.forEach(e -> e.tick(ticks));
    }

    protected void addElements(IElement element) {
        elements.add(element);
    }

    protected void addMouseListener(IMouseListener listener) {
        mouseListeners.add(listener);
    }

    protected void addTickListener(ITickListener listener) {
        manager.addTickListener(listener);
    }

    /**
     * Called when upper module group want to switch another child module
     * and this onBack return false
     * First value is the delay to switch to another module
     * Second value is that after new module switched, the duration that current
     * module should keep (only) to draw. (oh sorry, my statement is too vague)
     * Both two values must be positive number or 0 (no delay)
     * Unit: ticks
     *
     * @return a array with length of 2
     */
    public int[] changingModule() {
        return new int[]{0, 0};
    }

    @Override
    public void setDraggable(@Nullable IDraggable draggable) {
        this.draggable = draggable;
    }

    @Nullable
    @Override
    public IDraggable getDraggable() {
        return draggable;
    }

    @Override
    public void setKeyboardListener(@Nullable IKeyboardListener keyboardListener) {
        this.keyboardListener = keyboardListener;
    }

    @Nullable
    @Override
    public IKeyboardListener getKeyboardListener() {
        return keyboardListener;
    }

    @Override
    public boolean mouseMoved(double mouseX, double mouseY) {
        boolean result = false;
        for (IMouseListener listener : mouseListeners) {
            if (!result && listener.updateMouseHover(mouseX, mouseY)) {
                result = true;
            } else {
                listener.setMouseHoverExit();
            }
        }
        return result;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        for (IMouseListener listener : mouseListeners) {
            if (listener.isMouseHovered() && listener.mouseClicked(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (draggable != null) {
            draggable.onStopDragging(mouseX, mouseY);
            return true;
        }
        for (IMouseListener listener : mouseListeners) {
            if (listener.isMouseHovered() && listener.mouseReleased(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        for (IMouseListener listener : mouseListeners) {
            if (listener.isMouseHovered() && listener.mouseScrolled(amount)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (draggable != null) {
            return draggable.mouseDragged(mouseX, mouseY, deltaX, deltaY);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyboardListener != null) {
            return keyboardListener.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyboardListener != null) {
            return keyboardListener.keyReleased(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (keyboardListener != null) {
            return keyboardListener.charTyped(codePoint, modifiers);
        }
        return false;
    }

}
