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

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.animation.IAnimation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class Module implements IModule, IFocuser {

    private final GlobalModuleManager manager = GlobalModuleManager.INSTANCE;

    private final Canvas canvas = new Canvas();

    private List<IDrawable> drawables = new ArrayList<>();

    private List<IMouseListener> mouseListeners = new ArrayList<>();

    @Nullable
    private IDraggable draggable;

    @Nullable
    private IKeyboardListener keyboardListener;

    /**
     * If true, this module will draw over child module
     */
    private boolean overDraw = false;

    public Module() {

    }

    @Override
    public final void draw(float time) {
        RenderSystem.pushMatrix();
        if (overDraw) {
            drawChild(time);
            for (IDrawable drawable : drawables) {
                drawable.draw(canvas, time);
            }
        } else {
            for (IDrawable drawable : drawables) {
                drawable.draw(canvas, time);
            }
            drawChild(time);
        }
        for (IDrawable drawable : drawables) {
            drawable.drawForegroundLayer(canvas, (float) getMouseX(), (float) getMouseY(), time);
        }
        RenderSystem.popMatrix();
    }

    protected void drawChild(float time) {

    }

    protected void makeOverDraw() {
        overDraw = true;
    }

    @Override
    public void resize(int width, int height) {
        for (IDrawable drawable : drawables) {
            drawable.resize(width, height);
        }
    }

    @Override
    public void tick(int ticks) {
        for (IDrawable drawable : drawables) {
            drawable.tick(ticks);
        }
    }

    protected void addDrawable(IDrawable drawable) {
        drawables.add(drawable);
    }

    protected void addWidget(IWidget widget) {
        drawables.add(widget);
        mouseListeners.add(widget);
    }

    public void addAnimation(IAnimation animation) {
        manager.addAnimation(animation);
    }

    public int getWindowWidth() {
        return manager.getWindowWidth();
    }

    public int getWindowHeight() {
        return manager.getWindowHeight();
    }

    public double getMouseX() {
        return manager.getMouseX();
    }

    public double getMouseY() {
        return manager.getMouseY();
    }

    public void refocusCursor() {
        manager.refreshMouse();
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
