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
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.IDrawable;

public class Background implements IDrawable {

    private float alpha = 0f;

    private int width, height;

    public Background(int fadeInTime) {
        if (fadeInTime > 0) {
            new Animation(fadeInTime)
                    .addAppliers(
                            new Applier(0, 0.5f, () -> alpha, value -> alpha = value))
                    .start();
        } else {
            alpha = 0.5f;
        }
    }

    @Override
    public void draw(Canvas canvas, float time) {
        canvas.setRGBA(0, 0, 0, alpha);
        canvas.drawRect(0, 0, width, height);
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

}
