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

import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.master.IElement;

public class ResourcePackBG implements IElement {

    private float x11, x12, y1, y2;

    private boolean drawSide = false;

    private float x21, x22, x31, x32;

    @Override
    public void draw(float time) {
        DrawTools.fillRectWithColor(x11, y1, x12, y2, 0x60000000);
        if (drawSide) {
            DrawTools.fillRectWithColor(x21, y1, x22, y2, 0x90000000);
            DrawTools.fillRectWithColor(x31, y1, x32, y2, 0x90000000);
        }
    }

    @Override
    public void resize(int width, int height) {
        x11 = width / 2f - 8f;
        x12 = width / 2f + 8f;
        y1 = 36f;
        y2 = height - 36f;
        float crd = (width - 80) / 2f - 8f;
        if (crd > 240) {
            drawSide = true;
            x21 = 40f;
            x22 = width / 2f - 248;
            x31 = width / 2f + 248;
            x32 = width - 40f;
        } else {
            drawSide = false;
        }
    }
}
