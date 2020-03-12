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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.element.Element;
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.system.ReferenceLibrary;
import net.minecraft.client.gui.IGuiEventListener;
import org.lwjgl.opengl.GL11;

import java.util.function.Function;

public class CheckboxButton extends Element implements IGuiEventListener {

    protected Shape shape;

    protected boolean mouseHovered = false;

    protected boolean checked = false;

    protected float frameBrightness = 0.7f;

    protected float markAlpha = 0;

    protected float scale;

    public CheckboxButton(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer) {
        super(xResizer, yResizer);
        shape = new Shape.Rect(8, 8);
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
    public void draw(float currentTime) {
        DrawTools.fillFrameWithWhite(x, y, x + 8, y + 8, 0.51f, frameBrightness, 1);
        if (markAlpha > 0) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableAlphaTest();
            GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            RenderSystem.pushMatrix();
            RenderSystem.color4f(1, 1, 1, markAlpha);
            RenderSystem.scalef(scale, scale, 1);
            textureManager.bindTexture(ReferenceLibrary.ICONS);
            DrawTools.blit(x / scale, y / scale, 0, 32, 32, 32);
            RenderSystem.popMatrix();
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        boolean prev = mouseHovered;
        mouseHovered = shape.isMouseInShape(x, y, mouseX, mouseY);
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
    }
}
