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

package icyllis.modernui.gui.popup;

import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.IModule;
import icyllis.modernui.gui.master.IWidget;

/**
 * Open a drop down menu or context menu, resize and mouse release will close this popup
 */
public class PopupMenu implements IModule {

    private final IWidget menu;

    private boolean init = false;

    public PopupMenu(IWidget menu) {
        this.menu = menu;
    }

    @Override
    public void draw(float time) {
        menu.draw(time);
    }

    @Override
    public void resize(int width, int height) {
        if (!init) {
            init = true;
            return;
        }
        GlobalModuleManager.INSTANCE.closePopup();
    }

    @Override
    public void tick(int ticks) {
        menu.tick(ticks);
    }

    @Override
    public boolean mouseMoved(double mouseX, double mouseY) {
        return menu.updateMouseHover(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        GlobalModuleManager.INSTANCE.closePopup();
        if (menu.isMouseHovered()) {
            menu.mouseClicked(mouseX, mouseY, mouseButton);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

}
