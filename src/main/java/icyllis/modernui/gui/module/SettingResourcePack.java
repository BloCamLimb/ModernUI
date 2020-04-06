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

import com.google.common.collect.Lists;
import icyllis.modernui.gui.background.ResourcePackBG;
import icyllis.modernui.gui.master.Module;
import icyllis.modernui.gui.option.ResourcePackGroup;
import icyllis.modernui.gui.scroll.ScrollWindow;
import net.minecraft.client.Minecraft;

import java.util.function.Function;

public class SettingResourcePack extends Module {

    private Minecraft minecraft;

    private ScrollWindow<ResourcePackGroup> aWindow;

    private ScrollWindow<ResourcePackGroup> sWindow;

    public SettingResourcePack() {
        minecraft = Minecraft.getInstance();
        addBackground(new ResourcePackBG());

        Function<Integer, Float> widthFunc = w -> Math.min((w - 80) / 2f - 8f, 240);
        Function<Integer, Float> leftXFunc = w -> w / 2f - widthFunc.apply(w) - 8f;

        aWindow = new ScrollWindow<>(this, leftXFunc, h -> 36f, widthFunc, h -> h - 72f);
        sWindow = new ScrollWindow<>(this, w -> w / 2f + 8, h -> 36f, widthFunc, h -> h - 72f);

        ResourcePackGroup aGroup = new ResourcePackGroup(aWindow, minecraft.getResourcePackList(), ResourcePackGroup.Type.AVAILABLE);
        ResourcePackGroup sGroup = new ResourcePackGroup(sWindow, minecraft.getResourcePackList(), ResourcePackGroup.Type.SELECTED);

        aWindow.addGroups(Lists.newArrayList(aGroup));
        sWindow.addGroups(Lists.newArrayList(sGroup));

        addWidget(aWindow);
        addWidget(sWindow);
    }

}
