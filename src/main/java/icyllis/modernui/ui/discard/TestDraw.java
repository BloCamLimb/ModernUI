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

package icyllis.modernui.ui.discard;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.animation.Animation;
import icyllis.modernui.animation.Applier;
import icyllis.modernui.animation.ITimeInterpolator;
import icyllis.modernui.animation.interpolator.OvershootInterpolator;
import icyllis.modernui.graphics.renderer.Canvas;

import javax.annotation.Nonnull;

@Deprecated
public class TestDraw implements IDrawable {

    private float xOffset;

    private float yOffset;

    private Animation animation;
    private Animation accAnm;

    private float circleAcc;

    public TestDraw() {
        animation = new Animation(600)
                .applyTo(
                        new Applier(-70, 100, () -> xOffset, v -> xOffset = v)
                                .setInterpolator(new OvershootInterpolator(2)),
                        new Applier(0, 50, () -> yOffset, v -> yOffset = v)
                                .setInterpolator(ITimeInterpolator.DECELERATE)
                );
        animation.startFull();
        accAnm = new Animation(600)
                .applyTo(
                        new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc, v -> circleAcc = v)
                            .setInterpolator(ITimeInterpolator.ACC_DEC)
                );
        accAnm.startFull();
    }

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        canvas.save();
        canvas.translate(xOffset, 0);
        /*canvas.setLineWidth(2);
        canvas.setLineAntiAliasing(true);
        canvas.drawOctagonRectFrame(100, 40, 200, 60, 3);
        canvas.setLineAntiAliasing(false);*/
        //canvas.setColor(Color3i.BLUE_C, 0.5f);
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
        canvas.setColor(1, 1, 1);
        canvas.drawCircle(20, 130, 8);
        RenderSystem.disableDepthTest();

        canvas.resetColor();
        canvas.drawText("Modern UI Library", 20, 64);

        canvas.save();
        canvas.translate((float) Math.sin(circleAcc) * 16, (float) Math.cos(circleAcc) * 16);
        //canvas.setColor(Color3i.LIGHT_PURPLE, 0.5f);
        canvas.drawCircle(60, 160, 3);
        canvas.restore();

        canvas.resetColor();
        canvas.drawFeatheredRect(80, 114, 110, 116, 0.5f);
    }

    @Override
    public void tick(int ticks) {
        if ((ticks + 16) % 32 == 0) {
            animation.invert();
            accAnm.startFull();
        } else if ((ticks) % 32 == 0) {
            animation.startFull();
            accAnm.startFull();
        }
    }
}
