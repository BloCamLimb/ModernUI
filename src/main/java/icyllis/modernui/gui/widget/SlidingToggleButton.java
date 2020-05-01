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

import com.google.gson.annotations.Expose;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.*;
import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.gui.math.Locator;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sliding toggle button
 */
public class SlidingToggleButton extends Widget {

    private final AnimationControl ac = new Control(this);

    private final int onColor;

    private final int offColor;

    private Consumer<Boolean> callback = b -> {};

    /**
     * Current status, on or off
     */
    private boolean checked = false;

    /**
     * Background color
     */
    private float r, g, b, a;

    /**
     * Frame brightness
     */
    private float brightness = 0.7f;

    private float circleOffset;

    public SlidingToggleButton(IHost host, Builder builder) {
        super(host, builder);
        this.onColor = builder.onColor;
        this.offColor = builder.offColor;
        a = (offColor >> 24 & 0xff) / 255f;
        r = (offColor >> 16 & 0xff) / 255f;
        g = (offColor >> 8 & 0xff) / 255f;
        b = (offColor & 0xff) / 255f;
        circleOffset = height / 2f;
    }

    public SlidingToggleButton setDefaultToggled(boolean v) {
        if (v) {
            this.checked = true;
            this.circleOffset = width - height / 2f;
            a = (onColor >> 24 & 0xff) / 255f;
            r = (onColor >> 16 & 0xff) / 255f;
            g = (onColor >> 8 & 0xff) / 255f;
            b = (onColor & 0xff) / 255f;
        }
        return this;
    }

    public SlidingToggleButton setCallback(Consumer<Boolean> r) {
        this.callback = r;
        return this;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        ac.update();
        canvas.setRGBA(r, g, b, a);
        canvas.drawRoundedRect(x1, y1, x2, y2, height / 2f);
        canvas.setRGBA(brightness, brightness, brightness, 1);
        canvas.drawRoundedRectFrame(x1, y1, x2, y2, height / 2f);
        canvas.setAlpha(0.9f);
        canvas.drawCircle(x1 + circleOffset, y1 + height / 2f, height / 2f - 0.5f);
    }

    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (listening && mouseButton == 0) {

            return true;
        }
        return false;
    }*/

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        setChecked(!checked);
        callback.accept(checked);
        return true;
    }

    @Override
    protected void onMouseHoverEnter(double mouseX, double mouseY) {
        super.onMouseHoverEnter(mouseX, mouseY);
        getHost().addAnimation(new Animation(3)
                .applyTo(new Applier(brightness, 1.0f, this::setBrightness)));
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        getHost().addAnimation(new Animation(3)
                .applyTo(new Applier(brightness, 0.7f, this::setBrightness)));
    }

    @Nonnull
    @Override
    public Class<? extends Widget.Builder> getBuilder() {
        return Builder.class;
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

    public boolean isToggledOn() {
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

    public static class Builder extends Widget.Builder {

        @Expose
        private final int onColor;

        @Expose
        private final int offColor;

        /**
         * Builder, RGBA color
         * @param size size multiplier, 4 is middle size
         * @param onColor background color when is on (RGBA)
         * @param offColor background color when is off (RGBA)
         */
        public Builder(int onColor, int offColor, int size) {
            this.onColor = onColor;
            this.offColor = offColor;
            super.setWidth(size * 5);
            super.setHeight(size * 2);
        }

        @Deprecated
        @Override
        public Builder setWidth(float width) {
            super.setWidth(width);
            return this;
        }

        @Deprecated
        @Override
        public Builder setHeight(float height) {
            super.setHeight(height);
            return this;
        }

        @Override
        public Builder setLocator(@Nonnull Locator locator) {
            super.setLocator(locator);
            return this;
        }

        @Override
        public Builder setAlign(@Nonnull Align9D align) {
            super.setAlign(align);
            return this;
        }

        @Nonnull
        @Override
        public SlidingToggleButton build(IHost host) {
            return new SlidingToggleButton(host, this);
        }
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
