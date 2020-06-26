/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.widget;

import com.google.gson.annotations.Expose;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.graphics.font.TextAlign;
import icyllis.modernui.ui.test.Align9D;
import icyllis.modernui.ui.test.Locator;
import icyllis.modernui.ui.test.IHost;
import icyllis.modernui.ui.test.Widget;
import net.minecraft.client.resources.I18n;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Text, Frame, Click, Repeatability
 */
public class StaticFrameButton extends Button {

    private final String text;

    public StaticFrameButton(IHost host, Builder builder) {
        super(host, builder);
        this.text = I18n.format(builder.text);
    }

    @Override
    public StaticFrameButton buildCallback(@Nullable Runnable r) {
        super.buildCallback(r);
        return this;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        super.onDraw(canvas, time);
        //canvas.setColor(getModulatedBrightness(), getModulatedBrightness(), getModulatedBrightness(), 1.0f);
        canvas.drawRectOutline(x1, y1, x2, y2, 0.51f);
        canvas.setTextAlign(TextAlign.CENTER);
        canvas.drawText(text, x1 + width / 2f, y1 + 2);
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

        @Nonnull
        @Override
        public StaticFrameButton build(IHost host) {
            return new StaticFrameButton(host, this);
        }
    }
}
