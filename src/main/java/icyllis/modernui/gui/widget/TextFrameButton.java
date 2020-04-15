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

import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.DrawTools;

public class TextFrameButton extends AnimatedWidget {

    private String text;

    private float textBrightness = 0.7f;

    private float frameAlpha = 0;

    private float fwo, fho;

    private Runnable leftClickFunc;

    public TextFrameButton(String text, Runnable onLeftClick) {
        this.text = text;
        this.width = Math.max(28, fontRenderer.getStringWidth(text) + 6);
        this.height = 13;
        this.fwo = width;
        this.fho = height;
        this.leftClickFunc = onLeftClick;
    }

    @Override
    public void draw(float time) {
        super.draw(time);
        fontRenderer.drawString(text, x1 + width / 2f, y1 + 2, textBrightness, TextAlign.CENTER);
        if (frameAlpha > 0) {
            DrawTools.INSTANCE.setRGBA(0.5f, 0.5f, 0.5f, frameAlpha);
            DrawTools.INSTANCE.drawRectOutline(x1 - fwo, y1 - fho, x2 + fwo, y2 + fho, 0.51f);
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    protected void createOpenAnimations() {
        manager.addAnimation(new Animation(2, true)
                .applyTo(new Applier(width / 2f, 0, value -> fwo = value),
                        new Applier(6, 0, value -> fho = value)));
        manager.addAnimation(new Animation(2)
                .applyTo(new Applier(1, value -> frameAlpha = value),
                        new Applier(0.7f, 1, value -> textBrightness = value))
                .onFinish(() -> setOpenState(true)));
    }

    @Override
    protected void createCloseAnimations() {
        manager.addAnimation(new Animation(4)
                .applyTo(new Applier(1, 0, value -> frameAlpha = value),
                        new Applier(1, 0.7f, value -> textBrightness = value))
                .onFinish(() -> setOpenState(false)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (listening && mouseButton == 0) {
            leftClickFunc.run();
            return true;
        }
        return false;
    }

    public static class Countdown extends TextFrameButton {

        private final int startTick = GlobalModuleManager.INSTANCE.getTicks();

        private final int countdown;

        private boolean counting = true;

        private int displayCount;

        public Countdown(String text, Runnable leftClick, int countdown) {
            super(text, leftClick);
            this.displayCount = this.countdown = countdown;
            listening = false;
        }

        @Override
        public void draw(float time) {
            super.draw(time);
            if (counting) {
                DrawTools.INSTANCE.setRGBA(0.03f, 0.03f, 0.03f, 0.7f);
                DrawTools.INSTANCE.drawRect(x1, y1, x2, y2);
                fontRenderer.drawString(displayCount + "s", x1 + width / 2f, y1 + 2, 1, TextAlign.CENTER);
            }
        }

        @Override
        public void tick(int ticks) {
            if (counting) {
                counting = ticks < startTick + countdown * 20;
                displayCount = countdown - (ticks - startTick) / 20;
                if (!counting) {
                    listening = true;
                    manager.refreshMouse();
                }
            }
        }
    }

}
