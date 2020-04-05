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

    private List<IElement> backgrounds = new ArrayList<>();

    private List<IWidget> widgets = new ArrayList<>();

    @Nullable
    private IDraggable draggable;

    @Nullable
    private IKeyboardListener keyboardListener;

    public Module() {

    }

    @Override
    public void draw(float time) {
        backgrounds.forEach(e -> e.draw(time));
        widgets.forEach(e -> e.draw(time));
    }

    @Override
    public void resize(int width, int height) {
        backgrounds.forEach(e -> e.resize(width, height));
        widgets.forEach(e -> e.resize(width, height));
    }

    @Override
    public void tick(int ticks) {
        backgrounds.forEach(e -> e.tick(ticks));
        widgets.forEach(e -> e.tick(ticks));
    }

    protected void addBackground(IElement element) {
        backgrounds.add(element);
    }

    protected void addWidget(IWidget widget) {
        widgets.add(widget);
    }

    /**
     * Called when switch child module
     * If return false, this will be continue called every tick until return true
     * @param id new child module id
     * @return false to delay the changing
     */
    public boolean changingModule(int id) {
        return true;
    }

    /**
     * Called when module group changed a module successfully
     * @param id new module id
     */
    public void moduleChanged(int id) {

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
        for (IWidget widget : widgets) {
            if (!result && widget.updateMouseHover(mouseX, mouseY)) {
                result = true;
            } else {
                widget.setMouseHoverExit();
            }
        }
        return result;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        for (IWidget widget : widgets) {
            if (widget.isMouseHovered() && widget.mouseClicked(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (draggable != null) {
            draggable.onStopDragging(mouseX, mouseY);
        }
        for (IWidget widget : widgets) {
            if (widget.isMouseHovered() && widget.mouseReleased(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        for (IWidget widget : widgets) {
            if (widget.isMouseHovered() && widget.mouseScrolled(amount)) {
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
