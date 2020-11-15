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

package icyllis.modernui.ui.test;

import icyllis.modernui.animation.AnimationControl;

@Deprecated
public class SideFrameText extends AnimationControl {
    public SideFrameText() {
        super(null, null);
    }


    /*private String text;

    private float frameAlpha = 0, textAlpha = 0;

    private float sizeW = 0;

    public SideFrameText(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, String text) {
        super(xResizer, yResizer);
        this.text = text;
    }

    @Override
    public void draw(float time) {
        checkState();
        if (openState == 0) {
            return;
        }
        RenderSystem.pushMatrix();
        DrawTools.fillRectWithFrame(x - 4, y - 3, x + sizeW, y + 11, 0.51f, 0x000000, 0.4f * frameAlpha, 0x404040, 0.8f * frameAlpha);
        RenderSystem.enableBlend();
        fontRenderer.drawString(text, x, y, Color3I.WHILE, textAlpha, TextAlign.LEFT);
        RenderSystem.popMatrix();
    }

    @Override
    protected void onAnimationOpen() {
        super.onAnimationOpen();
        float textLength = fontRenderer.getStringWidth(text);
        manager.addAnimation(new Animation(3, true)
                .applyTo(new Applier(-4, textLength + 4, value -> sizeW = value)));
        manager.addAnimation(new Animation(3)
                .applyTo(new Applier(1, value -> frameAlpha = value)));
        manager.addAnimation(new Animation(3)
                .applyTo(new Applier(1, value -> textAlpha = value))
                .withDelay(2)
                .onFinish(() -> openState = 2));
    }

    @Override
    protected void onAnimationClose() {
        super.onAnimationClose();
        manager.addAnimation(new Animation(5)
                .applyTo(new Applier(1, 0, value -> textAlpha = frameAlpha = value))
                .onFinish(() -> openState = 0));
    }*/

}
