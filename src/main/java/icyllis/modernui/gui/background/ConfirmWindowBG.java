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

package icyllis.modernui.gui.background;

import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.IElement;

public class ConfirmWindowBG implements IElement {

    private float x, y;

    private float frameSizeHOffset = 16;

    public ConfirmWindowBG() {
        GlobalModuleManager manager = GlobalModuleManager.INSTANCE;
        manager.addAnimation(new Animation(3, true)
                .applyTo(new Applier(frameSizeHOffset, 80, value -> frameSizeHOffset = value)));
    }

    @Override
    public void draw(float time) {
        DrawTools.INSTANCE.setRGBA(0.06f, 0.06f, 0.06f, 0.7f);
        DrawTools.INSTANCE.drawRect(x, y, x + 180, y + frameSizeHOffset);

        DrawTools.INSTANCE.setAlpha(0.85f);
        DrawTools.INSTANCE.drawRect(x, y, x + 180, y + 16);

        DrawTools.INSTANCE.setRGBA(0.5f, 0.5f, 0.5f, 1.0f);
        DrawTools.INSTANCE.drawRectOutline(x, y, x + 180, y + frameSizeHOffset, 0.51f);
    }

    @Override
    public void resize(int width, int height) {
        this.x = width / 2f - 90;
        this.y = height / 2f - 40;
    }
}
