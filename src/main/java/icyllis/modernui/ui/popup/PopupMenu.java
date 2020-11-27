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

package icyllis.modernui.ui.popup;

import icyllis.modernui.view.UIManager;
import icyllis.modernui.ui.test.IModule;
import icyllis.modernui.ui.test.IWidget;

/**
 * Open a drop down menu or context menu, resize and mouse release will close this popup
 */
public class PopupMenu implements IModule {

    //private final Canvas canvas = new Canvas();

    private final IWidget menu;

    private boolean init = false;

    public PopupMenu(IWidget menu) {
        this.menu = menu;
    }

    @Override
    public void draw(float time) {
        //menu.draw(canvas, time);
    }

    @Override
    public void resize(int width, int height) {
        if (!init) {
            init = true;
            return;
        }
        UIManager.getInstance().closePopup();
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
        UIManager.getInstance().closePopup();
        /*if (menu.isMouseHovered()) {
            menu.mouseClicked(mouseX, mouseY, mouseButton);
        }*/
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

}
