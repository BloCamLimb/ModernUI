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

package icyllis.modernui.test.discard;

import net.minecraft.client.gui.IGuiEventListener;

@Deprecated
public class ModuleController implements IGuiEventListener {

    /*private final IntPredicate availability;

    private final Supplier<IModule> supplier;

    @Nullable
    private IModule moduleInstance;

    public ModuleController(IntPredicate availability, Supplier<IModule> supplier) {
        this.availability = availability;
        this.supplier = supplier;
    }

    public void draw(float currentTime) {
        if (moduleInstance != null) {
            moduleInstance.draw(currentTime);
        }
    }

    public void resize(int width, int height) {
        if (moduleInstance != null) {
            moduleInstance.resize(width, height);
        }
    }

    public void tick(int ticks) {
        if (moduleInstance != null) {
            moduleInstance.tick(ticks);
        }
    }

    public void build() {
        if (moduleInstance == null) {
            moduleInstance = supplier.get();
        }
    }

    public void onModuleChanged(int newID) {
        if (moduleInstance != null) {
            moduleInstance.onModuleChanged(newID);
        }
    }

    public void removed() {
        moduleInstance = null;
    }

    public boolean test(int value) {
        return availability.test(value);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (moduleInstance != null) {
            moduleInstance.mouseMoved(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (moduleInstance != null) {
            return moduleInstance.mouseClicked(mouseX, mouseY, mouseButton);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (moduleInstance != null) {
            return moduleInstance.mouseReleased(mouseX, mouseY, mouseButton);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        if (moduleInstance != null) {
            return moduleInstance.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (moduleInstance != null) {
            return moduleInstance.mouseScrolled(mouseX, mouseY, amount);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifier) {
        if (moduleInstance != null) {
            return moduleInstance.keyPressed(key, scanCode, modifier);
        }
        return false;
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifier) {
        if (moduleInstance != null) {
            return moduleInstance.keyReleased(key, scanCode, modifier);
        }
        return false;
    }

    @Override
    public boolean charTyped(char charCode, int modifier) {
        if (moduleInstance != null) {
            return moduleInstance.charTyped(charCode, modifier);
        }
        return false;
    }

    @Override
    public boolean changeFocus(boolean searchNext) {
        if (moduleInstance != null) {
            return moduleInstance.changeFocus(searchNext);
        }
        return false;
    }*/
}
