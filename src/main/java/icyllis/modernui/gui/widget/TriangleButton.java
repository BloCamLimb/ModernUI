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

package icyllis.modernui.gui.widget;

import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.*;
import icyllis.modernui.system.ConstantsLibrary;

import javax.annotation.Nonnull;
import java.util.List;

public class TriangleButton extends Widget {

    private final AnimationControl ac = new Control(this);

    private final Runnable leftClickFunc;
    private final Icon icon;

    private boolean clickable;
    private float brightness = 0.8f;

    public TriangleButton(Module module, @Nonnull Direction direction, float size, Runnable leftClick, boolean clickable) {
        super(module, size, size);
        int i = direction.ordinal();
        this.icon = new Icon(ConstantsLibrary.ICONS, 64 * i / 512f, 0.25f, (i + 1) * 64 / 512f, 0.375f, true);
        this.leftClickFunc = leftClick;
        this.clickable = clickable;
        if (!clickable) {
            brightness = 0.3f;
        }
    }

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        ac.update();
        canvas.setRGBA(brightness, brightness, brightness, 1.0f);
        canvas.drawIcon(icon, x1, y1, x2, y2);
        /*RenderSystem.pushMatrix();
        RenderSystem.scalef(0.375f, 0.375f, 1);
        RenderSystem.color3f(brightness, brightness, brightness);
        textureManager.bindTexture(ConstantsLibrary.ICONS);
        DrawTools.blit(tx, ty, u, 64, 32, 32);
        RenderSystem.popMatrix();*/
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
        if (clickable) {
            getModule().addAnimation(new Animation(2)
                    .applyTo(new Applier(brightness, 0.8f, this::setBrightness)));
        } else {
            getModule().addAnimation(new Animation(2)
                    .applyTo(new Applier(brightness, 0.3f, this::setBrightness)));
        }
    }

    @Override
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
        if (clickable)
            ac.startOpenAnimation();
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        if (clickable)
            ac.startCloseAnimation();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (listening && mouseButton == 0) {
            if (clickable) {
                brightness = 0.85f;
                leftClickFunc.run();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (listening && clickable && mouseButton == 0) {
            brightness = 1.0f;
            return true;
        }
        return false;
    }

    private void setBrightness(float b) {
        brightness = b;
    }

    private static class Control extends AnimationControl {

        private final TriangleButton instance;

        public Control(TriangleButton instance) {
            this.instance = instance;
        }

        @Override
        protected void createOpenAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(3)
                    .applyTo(new Applier(0.8f, 1.0f, instance::setBrightness)));
        }

        @Override
        protected void createCloseAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(3)
                    .applyTo(new Applier(1.0f, 0.8f, instance::setBrightness)));
        }
    }

    public enum Direction {
        LEFT,
        RIGHT,
        UP,
        DOWN
    }
}
