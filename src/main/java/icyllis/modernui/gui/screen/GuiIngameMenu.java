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

package icyllis.modernui.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.master.ModernUIScreen;
import icyllis.modernui.gui.module.IngameMenuHome;
import icyllis.modernui.gui.module.MenuSettingHeader;
import icyllis.modernui.gui.module.SettingGeneral;
import icyllis.modernui.gui.module.SettingVideo;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TranslationTextComponent;

public class GuiIngameMenu extends ModernUIScreen {

    public GuiIngameMenu(boolean isFullMenu) {
        super(new TranslationTextComponent("menu.game"));
        if (isFullMenu) {
            if (RenderSystem.isOnRenderThread()) {
                manager.addModule(i -> i >= 30 && i < 50, MenuSettingHeader::new);
                manager.addModule(i -> i == 31, SettingGeneral::new);
                manager.addModule(i -> i == 32, SettingVideo::new);
                manager.addModule(i -> true, IngameMenuHome::new);
            }
        }
    }

    @Override
    public void removed() {
        super.removed();
        Minecraft.getInstance().gameSettings.saveOptions();
    }
}
