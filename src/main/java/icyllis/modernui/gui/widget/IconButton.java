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

import javax.annotation.Nonnull;
import java.util.List;

public class IconButton extends Widget {

    protected final AnimationControl iconAC = new Control(this);

    private final Icon icon;
    private final Runnable leftClickFunc;

    private float brightness = 0.5f;

    public IconButton(Module module, float width, float height, Icon icon, Runnable leftClick) {
        super(module, width, height);
        this.icon = icon;
        this.leftClickFunc = leftClick;
    }

    public IconButton(Module module, Icon icon, Runnable leftClick) {
        super(module);
        this.icon = icon;
        this.leftClickFunc = leftClick;
    }

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        iconAC.update();
        canvas.setRGBA(brightness, brightness, brightness, 1.0f);
        canvas.drawIcon(icon, x1, y1, x2, y2);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (listening && mouseButton == 0) {
            if (iconAC.canChangeState()) {
                leftClickFunc.run();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
        iconAC.startOpenAnimation();
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        iconAC.startCloseAnimation();
    }

    public void setIconBrightness(float brightness) {
        this.brightness = brightness;
    }

    private static class Control extends AnimationControl {

        private final IconButton instance;

        public Control(IconButton instance) {
            this.instance = instance;
        }

        @Override
        protected void createOpenAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(4)
                    .applyTo(new Applier(0.5f, 1.0f, instance::setIconBrightness)));
        }

        @Override
        protected void createCloseAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(4)
                    .applyTo(new Applier(1.0f, 0.5f, instance::setIconBrightness)));
        }
    }
}
