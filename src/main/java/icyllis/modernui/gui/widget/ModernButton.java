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
import icyllis.modernui.api.element.IElement;
import icyllis.modernui.gui.animation.DisposableUniAnimation;
import icyllis.modernui.gui.animation.HighActiveUniAnimation;
import icyllis.modernui.gui.animation.HighStatusAnimation;
import icyllis.modernui.gui.element.Base;
import icyllis.modernui.gui.element.SideFrameText;
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.master.GlobalModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.function.Function;

public class ModernButton extends Base implements IElement {

    protected TextureManager textureManager = Minecraft.getInstance().textureManager;

    protected EventListener listener;

    protected ResourceLocation res;

    protected float sizeW, sizeH, u, v;

    protected float brightness = 0.5f, scale;

    protected HighActiveUniAnimation animation = new HighActiveUniAnimation(0.5f, 1.0f, 4, value -> brightness = value);

    public ModernButton(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, ResourceLocation res, float sizeW, float sizeH, float u, float v, float scale, Runnable onLeftClick) {
        super(xResizer, yResizer);
        listener = new EventListener(xResizer, yResizer, new Shape.RectShape(sizeW * scale, sizeH * scale));
        this.res = res;
        this.sizeW = sizeW;
        this.sizeH = sizeH;
        this.u = u;
        this.v = v;
        this.scale = scale;
        listener.addHoverOn(() -> animation.setStatus(true));
        listener.addHoverOff(() -> animation.setStatus(false));
        listener.addLeftClick(onLeftClick);
        GlobalModuleManager.INSTANCE.addEventListener(listener);
    }

    @Override
    public void draw(float currentTime) {
        animation.update(currentTime);
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.pushMatrix();
        RenderSystem.color4f(brightness, brightness, brightness, opacity);
        RenderSystem.scalef(scale, scale, scale);
        textureManager.bindTexture(res);
        DrawTools.blit(x / scale, y / scale, u, v, sizeW, sizeH);
        RenderSystem.popMatrix();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        listener.resize(width, height);
    }

    public static class A extends ModernButton {

        private SideFrameText text;

        public A(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, String text, ResourceLocation res, float sizeW, float sizeH, float u, float v, float scale, Runnable onLeftClick) {
            super(xResizer, yResizer, res, sizeW, sizeH, u, v, scale, onLeftClick);
            this.text = new SideFrameText(text);
            GlobalModuleManager.INSTANCE.addAnimation(new DisposableUniAnimation(0, 1, 3, value -> opacity = value).withDelay(1));
            listener.addHoverOn(() -> this.text.startOpen());
            listener.addHoverOff(() -> this.text.startClose());
            opacity = 0f;
        }

        @Override
        public void draw(float currentTime) {
            super.draw(currentTime);
            text.draw(currentTime);
        }

        @Override
        public void resize(int width, int height) {
            super.resize(width, height);
            text.setPos(x + sizeW * scale + 15, y + (sizeH * scale) / 2 - 5);
        }
    }
}
