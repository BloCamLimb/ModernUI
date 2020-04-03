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

    public ModuleGroup() {

    }

    /**
     * Add child module
     * @param id must >= 1, the parent is 0, and -1 for invalid
     * @param module module supplier
     */
    protected void addChildModule(int id, Supplier<Module> module) {
        childModules.put(id, module);
    }

    public void switchChildModule(int id) {
        if (id == cid || cid != tid) {
            return;
        }
        tid = id;
        if (child != null) {
             if (!child.changingModule(id)) {
                 return;
             }
        }
        child = childModules.getOrDefault(tid, () -> null).get();
        cid = tid;
        moduleChanged(cid);
    }

    public int getCid() {
        return cid;
    }

    @Override
    public void draw(float time) {
        super.draw(time);
        if (child != null) {
            child.draw(time);
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
                child = childModules.getOrDefault(tid, () -> null).get();
                cid = tid;
                moduleChanged(cid);
            }
        }
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
}
