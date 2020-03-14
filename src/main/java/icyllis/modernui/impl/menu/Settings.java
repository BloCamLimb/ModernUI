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

package icyllis.modernui.impl.menu;

import icyllis.modernui.api.manager.IModuleManager;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.component.ScrollWindow;
import icyllis.modernui.gui.component.scroll.ScrollEntry;
import icyllis.modernui.gui.element.IElement;
import icyllis.modernui.gui.element.MenuSettingsBG;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.widget.CheckboxButton;
import icyllis.modernui.gui.widget.LineTextButton;
import net.minecraft.client.gui.widget.list.AbstractList;

import java.util.function.Consumer;

public class Settings {

    public Settings(IModuleManager manager) {
        manager.addElement(new MenuSettingsBG());
        manager.addElement(new LineTextButton(w -> w / 2f - 152f, h -> 20f, "General", 48f, 31));
        manager.addElement(new LineTextButton(w -> w / 2f - 88f, h -> 20f, "Video", 48f, 32));
        manager.addElement(new LineTextButton(w -> w / 2f - 24f, h -> 20f, "Audio", 48f, 33));
        manager.addElement(new LineTextButton(w -> w / 2f + 40f, h -> 20f, "Controls", 48f, 34));
        manager.addElement(new LineTextButton(w -> w / 2f + 104f, h -> 20f, "Assets", 48f, 35));
        manager.addAnimation(new Animation(4)
                .onFinish(() -> GlobalModuleManager.INSTANCE.switchTo(31)));
    }

    public static class General {

        public General(IModuleManager manager) {
            ScrollWindow<ScrollEntry.Test> window = new ScrollWindow<>(w -> 40f, h -> 36f, w -> w - 80f, h -> h - 64f);
            for (int i = 0; i < 16; i++) {
                window.addEntry(new ScrollEntry.Test(i));
            }
            manager.addElement(window);
        }
    }
}
