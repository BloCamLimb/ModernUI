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

package icyllis.modernui.gui.scroll;

import icyllis.modernui.gui.option.OptionCategoryGroup;
import net.minecraft.client.gui.IGuiEventListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SettingScrollWindow extends ScrollWindow<OptionCategoryGroup> {

    @Nullable
    protected IGuiEventListener focused;

    public SettingScrollWindow() {
        super(w -> 40f, h -> 36f, w -> w - 80f, h -> h - 72f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (focused != null) {
            if (focused.mouseClicked(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focused != null) {
            return focused.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (focused != null) {
            return focused.keyReleased(keyCode, scanCode, modifiers);
        }
        return false;
    }

    public void setFocused(@Nonnull IGuiEventListener eventListener) {
        this.focused = eventListener;
    }
}
