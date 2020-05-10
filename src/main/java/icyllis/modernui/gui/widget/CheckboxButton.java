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
import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.gui.math.Locator;
import icyllis.modernui.system.ConstantsLibrary;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class CheckboxButton extends Widget {

    @Nonnull
    private final Icon icon;

    @Nullable
    private Consumer<Boolean> callback;

    private boolean checked;

    private float markAlpha = 0;

    private float brightness = 0.7f;

    /**
     * Color/alpha animations
     */
    private final Animation brightAnimation;
    private final Animation inactiveAnimation;
    private final Animation markAnimation;

    public CheckboxButton(IHost host, Builder builder) {
        super(host, builder);
        this.icon = new Icon(ConstantsLibrary.ICONS, 0, 0.125f, 0.125f, 0.25f, true);
        brightAnimation = new Animation(100)
                .applyTo(new Applier(0.7f, 1.0f, this::getBrightness, this::setBrightness));
        inactiveAnimation = new Animation(100)
                .applyTo(new Applier(0.3f, 1.0f, this::getBrightness, this::setBrightness));
        markAnimation = new Animation(100)
                .applyTo(new Applier(0.0f, 1.0f, this::getMarkAlpha, this::setMarkAlpha));
    }

    public CheckboxButton buildCallback(boolean b, @Nullable Consumer<Boolean> c) {
        this.checked = b;
        if (b) {
            markAlpha = 1;
        }
        callback = c;
        return this;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        canvas.setRGBA(brightness, brightness, brightness, 1.0f);
        canvas.drawRectOutline(x1, y1, x2, y2, 0.51f);
        if (markAlpha > 0) {
            canvas.setAlpha(markAlpha);
            if (getStatus().isListening()) {
                canvas.setRGB(1, 1, 1);
            }
            canvas.drawIcon(icon, x1, y1, x2, y2);
        }
    }

    @Override
    protected void onMouseHoverEnter(double mouseX, double mouseY) {
        super.onMouseHoverEnter(mouseX, mouseY);
        brightAnimation.start();
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        brightAnimation.invert();
    }

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        setChecked(!checked);
        if (callback != null) {
            callback.accept(isChecked());
        }
        return true;
    }

    @Override
    protected void onStatusChanged(WidgetStatus status) {
        super.onStatusChanged(status);
        if (status.isListening()) {
            inactiveAnimation.start();
        } else {
            inactiveAnimation.invert();
        }
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        if (checked) {
            markAnimation.start();
        } else {
            markAnimation.invert();
        }
    }

    private void setMarkAlpha(float markAlpha) {
        this.markAlpha = markAlpha;
    }

    private void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    private float getMarkAlpha() {
        return markAlpha;
    }

    private float getBrightness() {
        return brightness;
    }

    public boolean isChecked() {
        return checked;
    }

    public static class Builder extends Widget.Builder {

        public Builder(int size) {
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
        public CheckboxButton build(IHost host) {
            return new CheckboxButton(host, this);
        }
    }

    /*protected WidgetArea shape;

    protected boolean mouseHovered = false;

    protected boolean checked = false;

    protected float frameBrightness = 0.7f;

    protected float markAlpha = 0;

    protected float scale;

    public CheckboxButton(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer) {
        super(xResizer, yResizer);
        shape = new WidgetArea.Rect(8, 8);
        scale = 0.25f;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        if (checked) {
            moduleManager.addAnimation(new Animation(2)
                    .applyTo(new Applier(1, this::setMarkAlpha)));
        } else {
            moduleManager.addAnimation(new Animation(2)
                    .applyTo(new Applier(1, 0, this::setMarkAlpha)));
        }
    }

    public void setMarkAlpha(float markAlpha) {
        this.markAlpha = markAlpha;
    }

    public void setFrameBrightness(float frameBrightness) {
        this.frameBrightness = frameBrightness;
    }

    @Override
    public void draw(float time) {
        DrawTools.fillFrameWithWhite(x, y, x + 8, y + 8, 0.51f, frameBrightness, 1);
        if (markAlpha > 0) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableAlphaTest();
            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            RenderSystem.pushMatrix();
            RenderSystem.color4f(1, 1, 1, markAlpha);
            RenderSystem.scalef(scale, scale, 1);
            textureManager.bindTexture(ConstantsLibrary.ICONS);
            DrawTools.blit(x / scale, y / scale, 0, 32, 32, 32);
            RenderSystem.popMatrix();
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        boolean prev = mouseHovered;
        mouseHovered = shape.isMouseInArea(x, y, mouseX, mouseY);
        if (prev != mouseHovered) {
            if (mouseHovered) {
                moduleManager.addAnimation(new Animation(2)
                        .applyTo(new Applier(0.7f, 1.0f, this::setFrameBrightness)));
            } else {
                moduleManager.addAnimation(new Animation(2)
                        .applyTo(new Applier(1.0f, 0.7f, this::setFrameBrightness)));
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseHovered && mouseButton == 0) {
            setChecked(!checked);
            return true;
        }
        return false;
    }*/
}
