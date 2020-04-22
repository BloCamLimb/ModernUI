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
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.*;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Used in confirm popup
 */
public class DynamicFrameButton extends Widget {

    private final AnimationControl ac = new Control(this);

    private String text;

    private float textBrightness = 0.7f;

    private float frameAlpha = 0;

    private float fwo, fho;

    private Runnable leftClickFunc;

    public DynamicFrameButton(Module module, String text, Runnable onLeftClick) {
        super(module);
        this.text = text;
        this.width = Math.max(28, FontTools.getStringWidth(text) + 6);
        this.height = 13;
        this.fwo = width;
        this.fho = height;
        this.leftClickFunc = onLeftClick;
    }

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        ac.update();
        canvas.setRGBA(textBrightness, textBrightness, textBrightness, 1.0f);
        canvas.setTextAlign(TextAlign.CENTER);
        canvas.drawText(text, x1 + width / 2f, y1 + 2);
        if (frameAlpha > 0) {
            canvas.setRGBA(0.5f, 0.5f, 0.5f, frameAlpha);
            canvas.drawRectOutline(x1 - fwo, y1 - fho, x2 + fwo, y2 + fho, 0.51f);
        }
    }

    /*@Override
    public void draw(float time) {
        super.draw(time);
        fontRenderer.drawString(text, x1 + width / 2f, y1 + 2, textBrightness, TextAlign.CENTER);
        if (frameAlpha > 0) {
            DrawTools.INSTANCE.setRGBA(0.5f, 0.5f, 0.5f, frameAlpha);
            DrawTools.INSTANCE.drawRectOutline(x1 - fwo, y1 - fho, x2 + fwo, y2 + fho, 0.51f);
        }
    }*/

    @Override
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
        ac.startOpenAnimation();
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        ac.startCloseAnimation();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (listening && mouseButton == 0) {
            leftClickFunc.run();
            return true;
        }
        return false;
    }

    public void setTextBrightness(float textBrightness) {
        this.textBrightness = textBrightness;
    }

    public void setFrameAlpha(float frameAlpha) {
        this.frameAlpha = frameAlpha;
    }

    public void setFwo(float fwo) {
        this.fwo = fwo;
    }

    public void setFho(float fho) {
        this.fho = fho;
    }

    public static class Countdown extends DynamicFrameButton {

        private final int startTick = GlobalModuleManager.INSTANCE.getTicks();

        private final int countdown;

        private boolean counting = true;

        private int displayCount;

        public Countdown(Module module, String text, Runnable leftClick, int countdown) {
            super(module, text, leftClick);
            this.displayCount = this.countdown = countdown;
            listening = false;
        }

        /*@Override
        public void draw(float time) {
            super.draw(time);
            if (counting) {
                DrawTools.INSTANCE.setRGBA(0.03f, 0.03f, 0.03f, 0.7f);
                DrawTools.INSTANCE.drawRect(x1, y1, x2, y2);
                fontRenderer.drawString(displayCount + "s", x1 + width / 2f, y1 + 2, 1, TextAlign.CENTER);
            }
        }*/

        @Override
        public void draw(@Nonnull Canvas canvas, float time) {
            super.draw(canvas, time);
            if (counting) {
                canvas.setRGBA(0.03f, 0.03f, 0.03f, 0.7f);
                canvas.drawRect(x1, y1, x2, y2);
                canvas.resetColor();
                canvas.drawText(displayCount + "s", x1 + width / 2f, y1 + 2);
            }
        }

        @Override
        public void tick(int ticks) {
            if (counting) {
                counting = ticks < startTick + countdown * 20;
                displayCount = countdown - (ticks - startTick) / 20;
                if (!counting) {
                    listening = true;
                    module.refocusCursor();
                }
            }
        }
    }

    private static class Control extends AnimationControl {

        private final DynamicFrameButton instance;

        public Control(DynamicFrameButton instance) {
            this.instance = instance;
        }

        @Override
        protected void createOpenAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(2, true)
                    .applyTo(new Applier(instance.getWidth() / 2f, 0, instance::setFwo),
                            new Applier(6, 0, instance::setFho)));
            list.add(new Animation(2)
                    .applyTo(new Applier(0, 1, instance::setFrameAlpha),
                            new Applier(0.7f, 1, instance::setTextBrightness)));
        }

        @Override
        protected void createCloseAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(4)
                    .applyTo(new Applier(1, 0, instance::setFrameAlpha),
                            new Applier(1, 0.7f, instance::setTextBrightness)));
        }
    }

}
