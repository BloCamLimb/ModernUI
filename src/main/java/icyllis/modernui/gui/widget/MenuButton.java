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
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.util.Color3I;
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.system.ConstantsLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import org.lwjgl.opengl.GL11;

import java.util.function.Function;

public class MenuButton extends AnimatedWidget {

    protected TextureManager textureManager = Minecraft.getInstance().getTextureManager();

    private Function<Integer, Float> xResizer;

    private Function<Integer, Float> yResizer;

    private float u;

    private float brightness = 0.5f;

    private String text;

    private float frameAlpha = 0, textAlpha = 0;

    private float frameSizeW = 0;

    protected Runnable leftClickFunc;

    private int id;

    public MenuButton(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, String text, int index, Runnable leftClick, int id) {
        super(16, 16);
        this.xResizer = xResizer;
        this.yResizer = yResizer;
        this.text = text;
        this.u = index * 32;
        this.leftClickFunc = leftClick;
        this.id = id;
    }

    @Override
    public boolean mouseClicked(int mouseButton) {
        if (listening && mouseButton == 0) {
            if (canChangeState()) {
                leftClickFunc.run();
                return true;
            }
        }
        return false;
    }

    @Override
    public void draw(float time) {
        super.draw(time);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();

        RenderSystem.pushMatrix();

        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.color3f(brightness, brightness, brightness);
        RenderSystem.scalef(0.5f, 0.5f, 1);
        textureManager.bindTexture(ConstantsLibrary.ICONS);
        DrawTools.blit(x1 * 2, y1 * 2, u, 0, 32, 32);

        // right side text box
        if (isAnimationOpen()) {
            DrawTools.fillRectWithFrame(x1 + 27, y1 + 1, x1 + 31 + frameSizeW, y1 + 15, 0.51f, 0x000000, 0.4f * frameAlpha, 0x404040, 0.8f * frameAlpha);
            fontRenderer.drawString(text, x1 + 31, y1 + 4, Color3I.WHILE, textAlpha, TextAlign.LEFT);
        }

        RenderSystem.popMatrix();
    }

    @Override
    protected void onAnimationOpen() {
        float textLength = fontRenderer.getStringWidth(text);
        manager.addAnimation(new Animation(4)
                .applyTo(new Applier(0.5f, 1.0f, value -> brightness = value)));
        manager.addAnimation(new Animation(3, true)
                .applyTo(new Applier(-4.0f, textLength + 4.0f, value -> frameSizeW = value)));
        manager.addAnimation(new Animation(3)
                .applyTo(new Applier(1.0f, value -> frameAlpha = value)));
        manager.addAnimation(new Animation(3)
                .applyTo(new Applier(1.0f, value -> textAlpha = value))
                .withDelay(2)
                .onFinish(() -> setOpenState(true)));
    }

    @Override
    protected void onAnimationClose() {
        manager.addAnimation(new Animation(4)
                .applyTo(new Applier(1.0f, 0.5f, value -> brightness = value)));
        manager.addAnimation(new Animation(5)
                .applyTo(new Applier(1.0f, 0.0f, value -> textAlpha = frameAlpha = value))
                .onFinish(() -> setOpenState(false)));
    }

    @Override
    public void resize(int width, int height) {
        float x = xResizer.apply(width);
        float y = yResizer.apply(height);
        setPos(x, y);
    }

    public void onModuleChanged(int id) {
        setLockState(this.id == id);
    }
}
