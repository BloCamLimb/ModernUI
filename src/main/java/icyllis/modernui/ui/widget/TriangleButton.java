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

import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.graphics.renderer.Icon;
import icyllis.modernui.ui.layout.Align9D;
import icyllis.modernui.ui.layout.Direction4D;
import icyllis.modernui.ui.test.Locator;
import icyllis.modernui.system.ConstantsLibrary;
import icyllis.modernui.ui.test.IHost;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TriangleButton extends IconButton {

    public TriangleButton(IHost host, Builder builder) {
        super(host, builder);
    }

    @Override
    public TriangleButton buildCallback(@Nullable Runnable r) {
        super.buildCallback(r);
        return this;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        super.onDraw(canvas, time);
        /*RenderSystem.pushMatrix();
        RenderSystem.scalef(0.375f, 0.375f, 1);
        RenderSystem.color3f(brightness, brightness, brightness);
        textureManager.bindTexture(ConstantsLibrary.ICONS);
        DrawTools.blit(tx, ty, u, 64, 32, 32);
        RenderSystem.popMatrix();*/
    }

    /*public void setClickable(boolean clickable) {
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
    }*/

    public static class Builder extends IconButton.Builder {

        public Builder(@Nonnull Direction4D direction, float size) {
            super(new Icon(ConstantsLibrary.ICONS, 64 * direction.ordinal() / 512f, 0.25f, (direction.ordinal() + 1) * 64 / 512f, 0.375f, true));
            super.setWidth(size);
            super.setHeight(size);
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
        public TriangleButton build(IHost host) {
            return new TriangleButton(host, this);
        }
    }
}
