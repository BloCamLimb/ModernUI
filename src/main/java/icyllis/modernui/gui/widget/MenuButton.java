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

import icyllis.modernui.font.FontTools;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.*;
import icyllis.modernui.system.ConstantsLibrary;

import javax.annotation.Nonnull;
import java.util.List;

public class MenuButton extends IconButton {

    private final AnimationControl sideTextAC = new SideTextControl(this);

    private final String text;
    private final int id;

    private float frameAlpha = 0;
    private float textAlpha = 0;
    private float frameSizeW = 5;

    public MenuButton(Module module, String text, int uIndex, Runnable leftClick, int id) {
        super(module, 16, 16,
                new Icon(ConstantsLibrary.ICONS, uIndex * 64 / 512f, 0, (uIndex + 1) * 64 / 512f, 64 / 512f, true), leftClick);
        this.text = text;
        this.id = id;
    }

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        super.draw(canvas, time);
    }

    @Override
    public void drawForegroundLayer(Canvas canvas, float mouseX, float mouseY, float time) {
        sideTextAC.update();
        if (sideTextAC.isAnimationOpen()) {
            canvas.setRGBA(0.0f, 0.0f, 0.0f, 0.5f * frameAlpha);
            canvas.drawRoundedRect(x1 + 27, y1 + 1, x1 + 32 + frameSizeW, y1 + 15, 6);
            canvas.setRGBA(0.5f, 0.5f, 0.5f, frameAlpha);
            canvas.drawRoundedRectFrame(x1 + 27, y1 + 1, x1 + 32 + frameSizeW, y1 + 15, 6);
            canvas.setRGBA(1.0f, 1.0f, 1.0f, textAlpha);
            canvas.drawText(text, x1 + 32, y1 + 4);
        }
    }

    /*@Override
    public void draw(float time) {
        super.draw(time);
        sideText.draw(time);

        //RenderSystem.pushMatrix();

        //RenderSystem.color3f(brightness, brightness, brightness);
        DrawTools.INSTANCE.setRGBA(brightness, brightness, brightness, 1.0f);
        //RenderSystem.scalef(0.5f, 0.5f, 1);
        //textureManager.bindTexture(ConstantsLibrary.ICONS);
        *//*GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);*//*
        //DrawTools.blitFinal(x1, x1 + 16, y1, y1 + 16, u / 512, (u + 64) / 512, 0, 64 / 512f);
        DrawTools.INSTANCE.drawIcon(icon, x1, y1, x1 + 16, y1 + 16);

        //RenderSystem.popMatrix();

        // draw text box on right side
        // ingame menu would be opened in the game initialization phase, so font renderer shouldn't be called because there's no proper GL state !!
        if (sideText.isAnimationOpen()) {
            DrawTools.INSTANCE.setRGBA(0.0f, 0.0f, 0.0f, 0.5f * frameAlpha);
            DrawTools.INSTANCE.drawRoundedRect(x1 + 27, y1 + 1, x1 + 32 + frameSizeW, y1 + 15, 6);
            DrawTools.INSTANCE.setRGBA(0.5f, 0.5f, 0.5f, frameAlpha);
            DrawTools.INSTANCE.drawRoundedRectFrame(x1 + 27, y1 + 1, x1 + 32 + frameSizeW, y1 + 15, 6);
            //DrawTools.fillRectWithFrame(x1 + 27, y1 + 1, x1 + 31 + frameSizeW, y1 + 15, 0.51f, 0x000000, 0.4f * frameAlpha, 0x404040, 0.8f * frameAlpha);
            DrawTools.INSTANCE.setRGBA(1.0f, 1.0f, 1.0f, textAlpha);
            DrawTools.INSTANCE.drawText(text, x1 + 32, y1 + 4); // called font renderer
        }
    }*/

    @Override
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
        sideTextAC.startOpenAnimation();
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        sideTextAC.startCloseAnimation();
    }

    public float getTextLength() {
        return FontTools.getStringWidth(text);
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
        iconAC.setLockState(this.id == id);
        if (iconAC.canChangeState()) {
            if (!mouseHovered) {
                iconAC.startCloseAnimation();
            }
        }
    }

    private static class SideTextControl extends AnimationControl {

        private final MenuButton instance;

        public SideTextControl(MenuButton instance) {
            this.instance = instance;
        }

        @Override
        protected void createOpenAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(3, true)
                    .applyTo(new Applier(0.0f, instance.getTextLength() + 5.0f, instance::setFrameSizeW)));
            list.add(new Animation(3)
                    .applyTo(new Applier(1.0f, instance::setFrameAlpha)));
            list.add(new Animation(3)
                    .applyTo(new Applier(1.0f, instance::setTextAlpha))
                    .withDelay(2));
        }

        @Override
        protected void createCloseAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(5)
                    .applyTo(new Applier(1.0f, 0.0f, v -> {
                        instance.setTextAlpha(v);
                        instance.setFrameAlpha(v);
                    })));
        }
    }
}
