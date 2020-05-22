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

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.animation.IInterpolator;
import icyllis.modernui.gui.animation.OvershootInterpolator;
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.IDrawable;
import icyllis.modernui.gui.math.Color3i;

import javax.annotation.Nonnull;

public class TestDraw implements IDrawable {

    private float xOffset;

    private float yOffset;

    private Animation animation;

    public TestDraw() {
        animation = new Animation(600)
                .applyTo(
                        new Applier(-70, 100, () -> xOffset, v -> xOffset = v)
                                .setInterpolator(new OvershootInterpolator(2)),
                        new Applier(0, 50, () -> yOffset, v -> yOffset = v)
                                .setInterpolator(IInterpolator.DECELERATE)
                );
        animation.startFull();
    }

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        canvas.save();
        canvas.translate(xOffset, 0);
        /*canvas.setLineWidth(2);
        canvas.setLineAntiAliasing(true);
        canvas.drawOctagonRectFrame(100, 40, 200, 60, 3);
        canvas.setLineAntiAliasing(false);*/
        canvas.setColor(Color3i.BLUE_C, 0.5f);
        canvas.drawCircle(150, 50, 11);
        canvas.restore();

        canvas.save();
        canvas.translate(yOffset / 2.0f, yOffset);
        canvas.drawCircle(100, 80, 6);
        canvas.restore();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        canvas.setZ(20);
        canvas.drawCircle(20, 120, 8);
        RenderSystem.depthMask(true);
        canvas.setZ(0);
        canvas.setRGB(1, 1, 1);
        canvas.drawCircle(20, 130, 8);
        RenderSystem.disableDepthTest();

        canvas.resetColor();
        canvas.drawText("Modern UI Library", 20, 64);

        canvas.save();
        canvas.translate((float) Math.sin(Math.sin(time / 4) * Math.PI) * 16, (float) Math.cos(Math.sin(time / 4) * Math.PI) * 16);
        canvas.setColor(Color3i.LIGHT_PURPLE, 0.5f);
        canvas.drawCircle(60, 160, 5);
        canvas.restore();

        canvas.resetColor();
        canvas.drawFeatheredRect(80, 114, 110, 116, 0.5f);
    }

    @Override
    public void tick(int ticks) {
        if ((ticks + 16) % 32 == 0) {
            animation.invert();
        } else if ((ticks) % 32 == 0) {
            animation.startFull();
        }
    }
}
