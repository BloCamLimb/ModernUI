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
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.Icon;
import icyllis.modernui.gui.master.Module;
import icyllis.modernui.gui.master.Widget;
import icyllis.modernui.system.ConstantsLibrary;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class CheckboxButton extends Widget {

    private final Icon icon;

    private final Consumer<Boolean> leftClickFunc;

    private boolean checked;

    private float markAlpha = 0;

    private float brightness = 0.7f;

    public CheckboxButton(Module module, float size, Consumer<Boolean> leftClick, boolean checked) {
        super(module, size, size);
        this.icon = new Icon(ConstantsLibrary.ICONS, 0, 0.125f, 0.125f, 0.25f, true);
        this.leftClickFunc = leftClick;
        this.checked = checked;
    }

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        canvas.setRGBA(brightness, brightness, brightness, 1.0f);
        canvas.drawRectOutline(x1, y1, x2, y2, 0.51f);
        if (markAlpha > 0) {
            canvas.setAlpha(markAlpha);
            if (listening) {
                canvas.setRGB(1, 1, 1);
            }
            canvas.drawIcon(icon, x1, y1, x2, y2);
        }
    }

    @Override
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
        module.addAnimation(new Animation(2)
                .applyTo(new Applier(brightness, 1.0f, this::setBrightness)));
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        module.addAnimation(new Animation(2)
                .applyTo(new Applier(brightness, 0.7f, this::setBrightness)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (listening && mouseButton == 0) {
            setChecked(!checked);
            leftClickFunc.accept(isChecked());
            return true;
        }
        return false;
    }

    public void setListening(boolean listening) {
        if (this.listening != listening) {
            this.listening = listening;
            if (listening) {
                module.addAnimation(new Animation(2)
                        .applyTo(new Applier(brightness, 1.0f, this::setBrightness)));
            } else {
                module.addAnimation(new Animation(2)
                        .applyTo(new Applier(brightness, 0.3f, this::setBrightness)));
            }
        }
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        if (checked) {
            module.addAnimation(new Animation(2)
                    .applyTo(new Applier(0, 1, this::setMarkAlpha)));
        } else {
            module.addAnimation(new Animation(2)
                    .applyTo(new Applier(1, 0, this::setMarkAlpha)));
        }
    }

    private void setMarkAlpha(float markAlpha) {
        this.markAlpha = markAlpha;
    }

    private void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    public boolean isChecked() {
        return checked;
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
