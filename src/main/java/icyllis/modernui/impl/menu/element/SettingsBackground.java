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

package icyllis.modernui.impl.menu.element;

import icyllis.modernui.api.ModernUI_API;
import icyllis.modernui.api.global.MotionType;
import icyllis.modernui.api.handler.IModuleManager;
import icyllis.modernui.gui.element.Rectangle;
import icyllis.modernui.gui.font.StringRenderer;
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.master.GlobalAnimationManager;

public class SettingsBackground extends Rectangle {

    public SettingsBackground() {
        init(w -> 24f, h -> 16f, w -> w - 80f, h -> h - 32f, 0x00000000);
        IModuleManager manager = ModernUI_API.INSTANCE.getModuleManager();
        manager.addModuleSwitchEvent(i -> {
            if (i != 0) {
                GlobalAnimationManager.INSTANCE.create(a -> a
                                .setInit(0)
                                .setTarget(0.5f)
                                .setTiming(2),
                        r -> alpha = r);
                GlobalAnimationManager.INSTANCE.create(a -> a
                                .setInit(q -> 24f)
                                .setTarget(q -> 40f, false)
                                .setTiming(2)
                                .setMotion(MotionType.SINE),
                        r -> renderX = r,
                        rs -> fakeX = rs);
            }
        });
    }

    @Override
    public void draw() {
        super.draw();
        DrawTools.fillRectWithColor(renderX, renderY, renderX + sizeW, renderY + 20, colorR, colorG, colorB, alpha);
    }
}
