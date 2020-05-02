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

package icyllis.modernui.gui.test;

import icyllis.modernui.gui.animation.*;
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.IDrawable;
import icyllis.modernui.gui.math.Color3f;

import javax.annotation.Nonnull;

public class TestDraw implements IDrawable {

    private float xOffset;

    private float yOffset;

    private Animation animation;

    public TestDraw() {
        animation = new Animation(600)
                .addAppliers(
                        new Applier(-70, 100, () -> xOffset, v -> xOffset = v)
                                .setInterpolator(new OvershootInterpolator(2)),
                        new Applier(0, 100, () -> yOffset, v -> yOffset = v)
                                .setInterpolator(IInterpolator.SINE)
                );
        animation.restart();
    }

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        canvas.save();
        canvas.translate(xOffset, 0);
        /*canvas.setLineWidth(2);
        canvas.setLineAntiAliasing(true);
        canvas.drawOctagonRectFrame(100, 40, 200, 60, 3);
        canvas.setLineAntiAliasing(false);*/
        canvas.setColor(Color3f.BLUE_C, 0.5f);
        canvas.drawCircle(150, 50, 11);
        canvas.restore();

        canvas.save();
        canvas.translate(yOffset / 2.0f, yOffset);
        canvas.drawCircle(100, 80, 6);
        canvas.restore();
    }

    @Override
    public void tick(int ticks) {
        if ((ticks + 16) % 32 == 0) {
            animation.invert();
        } else if ((ticks) % 32 == 0) {
            animation.restart();
        }
    }
}