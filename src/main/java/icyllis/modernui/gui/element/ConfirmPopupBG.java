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

import icyllis.modernui.gui.animation.IInterpolator;
import icyllis.modernui.gui.math.Align3H;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.*;

import javax.annotation.Nonnull;

public class ConfirmPopupBG extends Background {

    private String title = "";

    private String[] desc = new String[0];

    private float x, y;

    private float heightOffset = 16;

    public ConfirmPopupBG() {
        super(200);
        new Animation(150)
                .addAppliers(
                        new Applier(heightOffset, 80, () -> heightOffset, value -> heightOffset = value)
                                .setInterpolator(IInterpolator.SINE))
                .start();
    }

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        super.draw(canvas, time);
        canvas.setRGBA(0.064f, 0.064f, 0.064f, 0.7f);
        canvas.drawRect(x, y, x + 180, y + heightOffset);

        canvas.setRGBA(0.032f, 0.032f, 0.032f, 0.85f);
        canvas.drawRect(x, y, x + 180, y + 16);

        canvas.setRGBA(0.5f, 0.5f, 0.5f, 1.0f);
        canvas.drawRectOutline(x, y, x + 180, y + heightOffset, 0.51f);

        canvas.resetColor();
        canvas.setTextAlign(Align3H.CENTER);
        canvas.drawText(title, x + 90, y + 4);

        canvas.setTextAlign(Align3H.LEFT);
        int i = 0;
        for (String t : desc) {
            canvas.drawText(t, x + 8, y + 24 + i++ * 12);
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.x = width / 2f - 90;
        this.y = height / 2f - 40;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDesc(String[] desc) {
        this.desc = desc;
    }
}
