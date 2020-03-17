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

package icyllis.modernui.gui.window;

import icyllis.modernui.gui.component.DropDownList;
import icyllis.modernui.gui.component.option.OptionCategory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SettingScrollWindow extends ScrollWindow<OptionCategory> {

    /**
     * Make mouseClicked return false to remove this
     */
    @Nullable
    protected DropDownList dropDownList;

    public SettingScrollWindow() {
        super(w -> 40f, h -> 36f, w -> w - 80f, h -> h - 72f);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        dropDownList = null;
    }

    @Override
    public void drawEndExtra() {
        if (dropDownList != null) {
            dropDownList.draw();
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (dropDownList != null) {
            dropDownList.mouseMoved(mouseX, mouseY);
        } else {
            super.mouseMoved(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (dropDownList != null) {
            dropDownList.mouseClicked(mouseX, mouseY, mouseButton);
            dropDownList = null;
            super.mouseMoved(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        if (dropDownList != null) {
            return false;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollAmount);
    }

    public boolean hasDropDownList() {
        return dropDownList != null;
    }

    public void setDropDownList(@Nonnull DropDownList list) {
        this.dropDownList = list;
    }
}
