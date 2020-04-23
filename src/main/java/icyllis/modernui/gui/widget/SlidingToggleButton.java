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
import icyllis.modernui.gui.master.AnimationControl;
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.Module;
import icyllis.modernui.gui.master.Widget;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sliding toggle button
 */
public class SlidingToggleButton extends Widget {

    private final AnimationControl ac = new Control(this);

    private final Consumer<Boolean> leftClickFunc;

    private final int onColor;

    private final int offColor;

    /**
     * Current status, on or off
     */
    private boolean checked;

    /**
     * Background color
     */
    private float r, g, b, a;

    /**
     * Frame brightness
     */
    private float brightness = 0.7f;

    private float circleOffset;

    /**
     * Main constructor
     *
     * @param module parent module
     * @param size size multiplier, 4 is middle size
     * @param leftClickFunc left click function
     * @param onColor background color when is on (RGBA)
     * @param offColor background color when is off (RGBA)
     * @param isOn current status, on or off
     */
    public SlidingToggleButton(Module module, float size, Consumer<Boolean> leftClickFunc, int onColor, int offColor, boolean isOn) {
        super(module, size * 5, size * 2);
        this.leftClickFunc = leftClickFunc;
        this.onColor = onColor;
        this.offColor = offColor;
        this.checked = isOn;
        this.circleOffset = isOn ? width - height / 2f : height / 2f;
        if (isOn) {
            a = (onColor >> 24 & 0xff) / 255f;
            r = (onColor >> 16 & 0xff) / 255f;
            g = (onColor >> 8 & 0xff) / 255f;
            b = (onColor & 0xff) / 255f;
        } else {
            a = (offColor >> 24 & 0xff) / 255f;
            r = (offColor >> 16 & 0xff) / 255f;
            g = (offColor >> 8 & 0xff) / 255f;
            b = (offColor & 0xff) / 255f;
        }
    }

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        ac.update();
        canvas.setRGBA(r, g, b, a);
        canvas.drawRoundedRect(x1, y1, x2, y2, height / 2f);
        canvas.setRGBA(brightness, brightness, brightness, 1);
        canvas.drawRoundedRectFrame(x1, y1, x2, y2, height / 2f);
        canvas.setAlpha(0.9f);
        canvas.drawCircle(x1 + circleOffset, y1 + height / 2f, height / 2f - 0.5f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (listening && mouseButton == 0) {
            setChecked(!checked);
            leftClickFunc.accept(checked);
            return true;
        }
        return false;
    }

    @Override
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
        getModule().addAnimation(new Animation(3)
                .applyTo(new Applier(brightness, 1.0f, this::setBrightness)));
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        getModule().addAnimation(new Animation(3)
                .applyTo(new Applier(brightness, 0.7f, this::setBrightness)));
    }

    private void setChecked(boolean checked) {
        if (checked) {
            ac.startOpenAnimation();
            this.checked = true;
        } else {
            ac.startCloseAnimation();
            this.checked = false;
        }
    }

    public boolean isChecked() {
        return checked;
    }

    private void setR(float r) {
        this.r = r;
    }

    private void setG(float g) {
        this.g = g;
    }

    private void setB(float b) {
        this.b = b;
    }

    private void setA(float a) {
        this.a = a;
    }

    private void setCircleOffset(float circleOffset) {
        this.circleOffset = circleOffset;
    }

    private void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    private static class Control extends AnimationControl {

        private final SlidingToggleButton instance;

        public Control(SlidingToggleButton instance) {
            this.instance = instance;
        }

        @Override
        protected void createOpenAnimations(@Nonnull List<Animation> list) {
            float a = (instance.onColor >> 24 & 0xff) / 255f;
            float r = (instance.onColor >> 16 & 0xff) / 255f;
            float g = (instance.onColor >> 8 & 0xff) / 255f;
            float b = (instance.onColor & 0xff) / 255f;
            list.add(new Animation(4)
                    .applyTo(new Applier(instance.a, a, instance::setA),
                            new Applier(instance.r, r, instance::setR),
                            new Applier(instance.g, g, instance::setG),
                            new Applier(instance.b, b, instance::setB)));
            float c = instance.width - instance.height / 2f;
            list.add(new Animation(4, true)
                    .applyTo(new Applier(instance.circleOffset, c, instance::setCircleOffset)));
        }

        @Override
        protected void createCloseAnimations(@Nonnull List<Animation> list) {
            float a = (instance.offColor >> 24 & 0xff) / 255f;
            float r = (instance.offColor >> 16 & 0xff) / 255f;
            float g = (instance.offColor >> 8 & 0xff) / 255f;
            float b = (instance.offColor & 0xff) / 255f;
            list.add(new Animation(4)
                    .applyTo(new Applier(instance.a, a, instance::setA),
                            new Applier(instance.r, r, instance::setR),
                            new Applier(instance.g, g, instance::setG),
                            new Applier(instance.b, b, instance::setB)));
            float c = instance.height / 2f;
            list.add(new Animation(4, true)
                    .applyTo(new Applier(instance.circleOffset, c, instance::setCircleOffset)));
        }
    }
}
