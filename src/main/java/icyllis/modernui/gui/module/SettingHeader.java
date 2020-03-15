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

package icyllis.modernui.gui.module;

import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.element.IElement;
import icyllis.modernui.gui.element.MenuSettingsBG;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.IGuiModule;
import icyllis.modernui.gui.widget.LineTextButton;

import java.util.ArrayList;
import java.util.List;

public class SettingHeader implements IGuiModule {

    private List<IElement> elements = new ArrayList<>();

    public SettingHeader() {
        elements.add(new MenuSettingsBG());
        elements.add(new LineTextButton(w -> w / 2f - 152f, h -> 20f, "General", 48f, 31));
        elements.add(new LineTextButton(w -> w / 2f - 88f, h -> 20f, "Video", 48f, 32));
        elements.add(new LineTextButton(w -> w / 2f - 24f, h -> 20f, "Audio", 48f, 33));
        elements.add(new LineTextButton(w -> w / 2f + 40f, h -> 20f, "Controls", 48f, 34));
        elements.add(new LineTextButton(w -> w / 2f + 104f, h -> 20f, "Assets", 48f, 35));
        GlobalModuleManager.INSTANCE.addAnimation(new Animation(2)
                .onFinish(() -> GlobalModuleManager.INSTANCE.switchModule(31)));
    }

    @Override
    public void draw(float currentTime) {
        elements.forEach(e -> e.draw(currentTime));
    }

    @Override
    public void resize(int width, int height) {

    }
}
