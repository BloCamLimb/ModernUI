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

import icyllis.modernui.system.MouseTools;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Contain itself and its child modules
 * It must exist this parent module and can exist at most one of child modules at the same time
 */
public class ModuleGroup extends Module {

    private Map<Integer, Supplier<Module>> childModules = new HashMap<>();

    @Nullable
    private Module child;

    /**
     * Current id, target id. 0 = no child
     * Not always same, such as there's exit animation
     */
    private int cid = 0, tid = 0;

    /**
     * If true, this module will draw over child module
     */
    private boolean overDraw = false;

    public ModuleGroup() {

    }

    /**
     * Add child module
     * @param id must >= 1, the parent is 0, and -1 for invalid
     * @param module module supplier
     */
    protected void addChildModule(int id, Supplier<Module> module) {
        if (id < 1) {
            throw new RuntimeException();
        }
        childModules.put(id, module);
    }

    public void switchChildModule(int id) {
        if (id < 0) {
            return;
        }
        if (id == cid || cid != tid) {
            return;
        }
        tid = id;
        if (child != null) {
             if (!child.changingModule(id)) {
                 return;
             }
            child.upperModuleExit();
        }
        child = childModules.getOrDefault(tid, () -> null).get();
        if (child != null) {
            GlobalModuleManager.INSTANCE.resizeModule(child);
        }
        cid = tid;
        moduleChanged(cid);
        GlobalModuleManager.INSTANCE.refreshMouse();
        MouseTools.useDefaultCursor();
    }

    protected void makeOverDraw() {
        overDraw = true;
    }

    public int getCid() {
        return cid;
    }

    @Override
    public void draw(float time) {
        if (overDraw) {
            if (child != null) {
                child.draw(time);
            }
            super.draw(time);
        } else {
            super.draw(time);
            if (child != null) {
                child.draw(time);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (child != null) {
            child.resize(width, height);
        }
    }

    @Override
    public void tick(int ticks) {
        super.tick(ticks);
        if (child != null) {
            child.tick(ticks);
        }
        if (cid != tid && child != null ) {
            if (child.changingModule(tid)) {
                child.upperModuleExit();
                child = childModules.getOrDefault(tid, () -> null).get();
                if (child != null) {
                    GlobalModuleManager.INSTANCE.resizeModule(child);
                }
                cid = tid;
                moduleChanged(cid);
                GlobalModuleManager.INSTANCE.refreshMouse();
                MouseTools.useDefaultCursor();
            }
        }
    }

    @Override
    public boolean onBack() {
        if (child != null) {
            return child.onBack();
        }
        return false;
    }

    @Override
    public void moduleChanged(int id) {
        super.moduleChanged(id);
    }

    @Override
    public boolean mouseMoved(double mouseX, double mouseY) {
        if (super.mouseMoved(mouseX, mouseY)) {
            return true;
        } else if (child != null) {
            return child.mouseMoved(mouseX, mouseY);
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        } else if (child != null) {
            return child.mouseClicked(mouseX, mouseY, mouseButton);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseReleased(mouseX, mouseY, mouseButton)) {
            return true;
        } else if (child != null) {
            return child.mouseReleased(mouseX, mouseY, mouseButton);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (super.mouseScrolled(mouseX, mouseY, amount)) {
            return true;
        } else if (child != null) {
            return child.mouseScrolled(mouseX, mouseY, amount);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (super.mouseDragged(mouseX, mouseY, deltaX, deltaY)) {
            return true;
        } else if (child != null) {
            return child.mouseDragged(mouseX, mouseY, deltaX, deltaY);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (child != null) {
            return child.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (super.keyReleased(keyCode, scanCode, modifiers)) {
            return true;
        } else if (child != null) {
            return child.keyReleased(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (super.charTyped(codePoint, modifiers)) {
            return true;
        } else if (child != null) {
            return child.charTyped(codePoint, modifiers);
        }
        return false;
    }
}
