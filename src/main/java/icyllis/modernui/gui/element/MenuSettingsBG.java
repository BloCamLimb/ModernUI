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

import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.DrawTools;

public class MenuSettingsBG extends Element {

    private float sizeW, sizeH;

    private float opacity = 0;

    public MenuSettingsBG() {
        super(w -> 24f, h -> 16f);
        moduleManager.addAnimation(new Animation(2, true)
                .applyTo(new Applier(24, 40, this::setX))
                .onFinish(() -> xResizer = w -> 40f));
        moduleManager.addAnimation(new Animation(2)
                .applyTo(new Applier(0, 0.5f, this::setOpacity)));
    }

    private void setX(float x) {
        this.x = x;
    }

    private void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    @Override
    public void draw(float currentTime) {
        DrawTools.fillRectWithColor(x, y, x + sizeW, y + sizeH, 0, 0, 0, opacity);
        DrawTools.fillRectWithColor(x, y, x + sizeW, y + 20, 0, 0, 0, opacity);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        sizeW = width - 80;
        sizeH = height - 32;
    }
}
