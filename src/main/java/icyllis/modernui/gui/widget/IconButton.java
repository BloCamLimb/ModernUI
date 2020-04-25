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
import java.util.function.BiFunction;

public class IconButton extends Widget {

    protected final AnimationControl iconAC = new Control(this);

    private final Icon icon;

    private Runnable leftClickFunc = () -> {};

    private float brightness = 0.7f;

    public IconButton(Module module, Builder builder) {
        super(module, builder);
        this.icon = builder.icon;
    }

    public IconButton setCallback(Runnable r) {
        this.leftClickFunc = r;
        return this;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        iconAC.update();
        canvas.setRGBA(brightness, brightness, brightness, 1.0f);
        canvas.drawIcon(icon, x1, y1, x2, y2);
    }

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        if (iconAC.canChangeState()) {
            leftClickFunc.run();
            return true;
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

    private void setIconBrightness(float brightness) {
        this.brightness = brightness;
    }

    @Nonnull
    @Override
    public Class<? extends Widget.Builder> getBuilder() {
        return Builder.class;
    }

    public static class Builder extends Widget.Builder {

        @Expose
        public final Icon icon;

        public Builder(@Nonnull Icon icon) {
            this.icon = icon;
        }

        @Override
        public Builder setWidth(float width) {
            super.setWidth(width);
            return this;
        }

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

        @Override
        public IconButton build(Module module) {
            return new IconButton(module, this);
        }
    }

    private static class Control extends AnimationControl {

        private final IconButton instance;

        public Control(IconButton instance) {
            this.instance = instance;
        }

        @Override
        protected void createOpenAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(4)
                    .applyTo(new Applier(instance.brightness, 1.0f, instance::setIconBrightness)));
        }

        @Override
        protected void createCloseAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(4)
                    .applyTo(new Applier(instance.brightness, 0.7f, instance::setIconBrightness)));
        }
    }
}
