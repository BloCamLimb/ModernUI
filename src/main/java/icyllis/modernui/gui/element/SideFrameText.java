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

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.DrawTools;

import java.util.function.Function;

public class SideFrameText extends StateAnimatedElement {

    private String text;

    private float frameOpacity = 0, textOpacity = 0;

    private float sizeW = 0;

    public SideFrameText(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, String text) {
        super(xResizer, yResizer);
        this.text = text;
    }

    @Override
    public void draw(float currentTime) {
        if (!checkState()) {
            return;
        }
        RenderSystem.pushMatrix();
        DrawTools.fillRectWithFrame(x - 4, y - 3, x + sizeW, y + 11, 0.51f, 0x000000, 0.4f * frameOpacity, 0x404040, 0.8f * frameOpacity);
        RenderSystem.enableBlend();
        fontRenderer.drawString(text, x, y, 1, 1, 1, textOpacity, 0);
        RenderSystem.popMatrix();
    }

    @Override
    protected void open() {
        super.open();
        float textLength = fontRenderer.getStringWidth(text);
        moduleManager.addAnimation(new Animation(3, true)
                .applyTo(new Applier(-4, textLength + 4, value -> sizeW = value)));
        moduleManager.addAnimation(new Animation(3)
                .applyTo(new Applier(1, value -> frameOpacity = value)));
        moduleManager.addAnimation(new Animation(3)
                .applyTo(new Applier(1, value -> textOpacity = value))
                .withDelay(2)
                .onFinish(() -> openState = 2));
    }

    @Override
    protected void close() {
        super.close();
        moduleManager.addAnimation(new Animation(5)
                .applyTo(new Applier(1, 0, value -> textOpacity = frameOpacity = value))
                .onFinish(() -> openState = 0));
    }

}
