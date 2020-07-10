/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.impl.background;

import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.ui.test.IDrawable;

import javax.annotation.Nonnull;

public class ResourcePackBG implements IDrawable {

    private float x1, x2, y1, y2;

    private boolean drawSide = false;

    private float x3, x4, x5, x6;

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        //canvas.setColor(0, 0, 0, 0.27f);
        canvas.drawRect(x1, y1, x2, y2);
        if (drawSide) {
            canvas.drawRect(x3, y1, x4, y2);
            canvas.drawRect(x5, y1, x6, y2);
        }
    }

    public void resize(int width, int height) {
        x1 = width / 2f - 8f;
        x2 = width / 2f + 8f;
        y1 = 36f;
        y2 = height - 36f;
        float crd = (width - 80) / 2f - 8f;
        if (crd > 240) {
            drawSide = true;
            x3 = 40f;
            x4 = width / 2f - 248;
            x5 = width / 2f + 248;
            x6 = width - 40f;
        } else {
            drawSide = false;
        }
    }
}
