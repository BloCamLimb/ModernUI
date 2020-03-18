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

import icyllis.modernui.gui.component.option.OptionEntry;
import icyllis.modernui.gui.element.IElement;
import icyllis.modernui.gui.element.MenuSettingsBG;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.IGuiModule;
import icyllis.modernui.gui.master.TickEvent;
import icyllis.modernui.gui.widget.LineTextButton;
import icyllis.modernui.gui.widget.MenuButton;
import icyllis.modernui.gui.widget.StateAnimatedButton;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.settings.AbstractOption;
import net.minecraft.client.settings.SliderMultiplierOption;
import net.minecraft.client.settings.SliderPercentageOption;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MenuSettingHeader implements IGuiModule {

    private List<IElement> elements = new ArrayList<>();

    private List<LineTextButton> buttons = new ArrayList<>();

    public MenuSettingHeader() {
        elements.add(new MenuSettingsBG());
        Consumer<LineTextButton> consumer = s -> {
            elements.add(s);
            buttons.add(s);
        };
        consumer.accept(new LineTextButton(w -> w / 2f - 152f, h -> 20f, "General", 48f, 31));
        consumer.accept(new LineTextButton(w -> w / 2f - 88f, h -> 20f, "Video", 48f, 32));
        consumer.accept(new LineTextButton(w -> w / 2f - 24f, h -> 20f, "Audio", 48f, 33));
        consumer.accept(new LineTextButton(w -> w / 2f + 40f, h -> 20f, "Controls", 48f, 34));
        consumer.accept(new LineTextButton(w -> w / 2f + 104f, h -> 20f, "Assets", 48f, 35));
        GlobalModuleManager.INSTANCE.addTickEvent(new TickEvent(2, () -> GlobalModuleManager.INSTANCE.switchModule(31)));
    }

    @Override
    public void draw(float currentTime) {
        elements.forEach(e -> e.draw(currentTime));
    }

    @Override
    public void resize(int width, int height) {
        elements.forEach(e -> e.resize(width, height));
    }

    @Override
    public void onModuleChanged(int newID) {
        buttons.forEach(e -> e.onModuleChanged(newID));
    }

    @Override
    public List<? extends IGuiEventListener> getEventListeners() {
        return buttons;
    }

}
