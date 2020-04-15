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
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.math.Color3i;
import icyllis.modernui.system.ConstantsLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.function.Function;

public class MenuButton extends AnimatedWidget {

    private TextureManager textureManager = Minecraft.getInstance().getTextureManager();

    private Function<Integer, Float> xResizer;
    private Function<Integer, Float> yResizer;

    private AnimatedElement sideText = new SideTextAnimator(this);

    private final String text;
    private final float u;
    private final Runnable leftClickFunc;
    private final int id;

    private float brightness = 0.5f;
    private float frameAlpha = 0;
    private float textAlpha = 0;
    private float frameSizeW = 5;

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
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
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
        sideText.draw(time);
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

        RenderSystem.popMatrix();

        // right side text box
        if (sideText.isAnimationOpen()) {
            DrawTools.INSTANCE.setRGBA(0.0f, 0.0f, 0.0f, 0.5f * frameAlpha);
            DrawTools.INSTANCE.drawRoundedRect(x1 + 27, y1 + 1, x1 + 32 + frameSizeW, y1 + 15, 6);
            DrawTools.INSTANCE.setRGBA(0.25f, 0.25f, 0.25f, frameAlpha);
            DrawTools.INSTANCE.drawRoundedRectFrame(x1 + 27, y1 + 1, x1 + 32 + frameSizeW, y1 + 15, 6);
            //DrawTools.fillRectWithFrame(x1 + 27, y1 + 1, x1 + 31 + frameSizeW, y1 + 15, 0.51f, 0x000000, 0.4f * frameAlpha, 0x404040, 0.8f * frameAlpha);
            fontRenderer.drawString(text, x1 + 32, y1 + 4, Color3i.WHILE, textAlpha, TextAlign.LEFT);
        }
    }

    @Override
    protected void createOpenAnimations() {
        manager.addAnimation(new Animation(4)
                .applyTo(new Applier(0.5f, 1.0f, value -> brightness = value))
                .onFinish(() -> setOpenState(true)));
    }

    @Override
    protected void createCloseAnimations() {
        manager.addAnimation(new Animation(4)
                .applyTo(new Applier(1.0f, 0.5f, value -> brightness = value))
                .onFinish(() -> setOpenState(false)));
    }

    @Override
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
        sideText.startOpenAnimation();
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        sideText.startCloseAnimation();
    }

    @Override
    public void resize(int width, int height) {
        float x = xResizer.apply(width);
        float y = yResizer.apply(height);
        setPos(x, y);
    }

    public float getTextLength() {
        return fontRenderer.getStringWidth(text);
    }

    public void setFrameAlpha(float frameAlpha) {
        this.frameAlpha = frameAlpha;
    }

    public void setTextAlpha(float textAlpha) {
        this.textAlpha = textAlpha;
    }

    public void setFrameSizeW(float frameSizeW) {
        this.frameSizeW = frameSizeW;
    }

    public void onModuleChanged(int id) {
        setLockState(this.id == id);
        if (canChangeState()) {
            if (!mouseHovered) {
                onMouseHoverExit();
            }
        }
    }

    private static class SideTextAnimator extends AnimatedElement {

        private final MenuButton instance;

        public SideTextAnimator(MenuButton instance) {
            this.instance = instance;
        }

        @Override
        public void draw(float time) {
            super.draw(time);
        }

        @Override
        protected void createOpenAnimations() {
            manager.addAnimation(new Animation(3, true)
                    .applyTo(new Applier(0.0f, instance.getTextLength() + 5.0f, instance::setFrameSizeW)));
            manager.addAnimation(new Animation(3)
                    .applyTo(new Applier(1.0f, instance::setFrameAlpha)));
            manager.addAnimation(new Animation(3)
                    .applyTo(new Applier(1.0f, instance::setTextAlpha))
                    .withDelay(2)
                    .onFinish(() -> setOpenState(true)));
        }

        @Override
        protected void createCloseAnimations() {
            manager.addAnimation(new Animation(5)
                    .applyTo(new Applier(1.0f, 0.0f, v -> {
                        instance.setTextAlpha(v);
                        instance.setFrameAlpha(v);
                    }))
                    .onFinish(() -> setOpenState(false)));
        }
    }
}
