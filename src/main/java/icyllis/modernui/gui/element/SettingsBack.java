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

package icyllis.modernui.gui.element;

import icyllis.modernui.gui.animation.DisposableUniAnimation;
import icyllis.modernui.gui.animation.DpsResizerSinAnimation;
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.master.GlobalModuleManager;

public class SettingsBack extends StandardRect {

    public SettingsBack() {
        super(w -> 24f, h -> 16f, w -> w - 80f, h -> h - 32f, 0x00000000);
        GlobalModuleManager.INSTANCE.addAnimation(new DpsResizerSinAnimation(2, false, value -> x = value, resizer -> xResizer = resizer).init(q -> 24f, q -> 40f));
        GlobalModuleManager.INSTANCE.addAnimation(new DisposableUniAnimation(0, 0.5f, 2, value -> opacity = value));
    }

    @Override
    public void draw(float currentTime) {
        super.draw(currentTime);
        DrawTools.fillRectWithColor(x, y, x + sizeW, y + 20, colorR, colorG, colorB, opacity);
    }
}
