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
import icyllis.modernui.gui.master.WidgetStatus;
import icyllis.modernui.gui.math.Align3H;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.Module;
import icyllis.modernui.gui.master.Widget;
import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.gui.math.Locator;
import net.minecraft.client.resources.I18n;

import javax.annotation.Nonnull;

/**
 * Text, Frame, Click, Repeatability
 */
public class StaticFrameButton extends Widget {

    private final String text;

    private Runnable callback = () -> {};
    private float brightness = 0.7f;

    public StaticFrameButton(Module module, Builder builder) {
        super(module, builder);
        this.text = I18n.format(builder.text);
    }

    public StaticFrameButton setDefaultClickable(boolean b) {
        if (!b) {
            brightness = 0.3f;
        }
        return this;
    }

    public StaticFrameButton setCallback(Runnable r) {
        this.callback = r;
        return this;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        canvas.setRGBA(brightness, brightness, brightness, 1.0f);
        canvas.drawRectOutline(x1, y1, x2, y2, 0.51f);
        canvas.setTextAlign(Align3H.CENTER);
        canvas.drawText(text, x1 + width / 2f, y1 + 2);
    }

    @Override
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
        getModule().addAnimation(new Animation(2)
                .applyTo(new Applier(brightness, 1.0f, this::setBrightness)));
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        getModule().addAnimation(new Animation(2)
                .applyTo(new Applier(brightness, 0.7f, this::setBrightness)));
    }

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        callback.run();
        brightness = 0.85f;
        return true;
    }

    @Override
    protected boolean onMouseLeftRelease(double mouseX, double mouseY) {
        brightness = 1.0f;
        return true;
    }

    @Override
    protected void onStatusChanged(WidgetStatus status) {
        super.onStatusChanged(status);
        if (status.isListening()) {
            getModule().addAnimation(new Animation(2)
                    .applyTo(new Applier(brightness, 0.7f, this::setBrightness)));
        } else {
            brightness = 0.3f;
        }
    }

    private void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    @Nonnull
    @Override
    public Class<? extends Widget.Builder> getBuilder() {
        return Builder.class;
    }

    public static class Builder extends Widget.Builder {

        @Expose
        public final String text;

        public Builder(@Nonnull String text) {
            this.text = text;
            super.setHeight(12);
        }

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

        @Override
        public StaticFrameButton build(Module module) {
            return new StaticFrameButton(module, this);
        }
    }
}
