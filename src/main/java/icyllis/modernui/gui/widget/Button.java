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

public abstract class Button extends Widget {

    protected final AnimationControl brightAC = new Control(this);

    private Runnable callback = () -> {};

    protected float brightness = 0.7f;

    private boolean playInactiveAnimation = true;

    public Button(IHost host, @Nonnull Builder builder) {
        super(host, builder);
    }

    public Button setDefaultClickable(boolean b) {
        if (!b) {
            boolean p = playInactiveAnimation;
            playInactiveAnimation = false;
            setStatus(WidgetStatus.INACTIVE);
            playInactiveAnimation = p;
        }
        return this;
    }

    public Button setCallback(Runnable r) {
        callback = r;
        return this;
    }

    public Button setOnetimeCallback(Runnable r) {
        callback = () -> {
          r.run();
          setStatus(WidgetStatus.INACTIVE);
        };
        playInactiveAnimation = false;
        return this;
    }

    @Override
    protected void onDraw(Canvas canvas, float time) {
        brightAC.update();
    }

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        if (brightAC.canChangeState()) {
            callback.run();
            brightness = 0.85f;
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseLeftRelease(double mouseX, double mouseY) {
        if (brightAC.canChangeState()) {
            brightness = 1.0f;
            return true;
        }
        return false;
    }

    @Override
    protected void onMouseHoverEnter(double mouseX, double mouseY) {
        super.onMouseHoverEnter(mouseX, mouseY);
        brightAC.startOpenAnimation();
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        brightAC.startCloseAnimation();
    }

    @Override
    protected void onStatusChanged(WidgetStatus status) {
        super.onStatusChanged(status);
        if (status.isListening()) {
            getHost().addAnimation(new Animation(2)
                    .applyTo(new Applier(brightness, 1.0f, this::setBrightness)));
        } else {
            if (playInactiveAnimation) {
                getHost().addAnimation(new Animation(2)
                        .applyTo(new Applier(brightness, 0.3f, this::setBrightness)));
            } else {
                brightness = 0.3f;
            }
        }
    }

    private void setBrightness(float f) {
        brightness = f;
    }

    private static class Control extends AnimationControl {

        private final Button instance;

        public Control(Button instance) {
            this.instance = instance;
        }

        @Override
        protected void createOpenAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(4)
                    .applyTo(new Applier(instance.brightness, 1.0f, instance::setBrightness)));
        }

        @Override
        protected void createCloseAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(4)
                    .applyTo(new Applier(instance.brightness, 0.7f, instance::setBrightness)));
        }
    }
}
