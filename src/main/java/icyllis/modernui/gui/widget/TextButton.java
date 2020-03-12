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
import icyllis.modernui.gui.element.StateAnimatedElement;
import icyllis.modernui.gui.master.DrawTools;
import net.minecraft.client.gui.IGuiEventListener;

import java.util.function.Function;

public class TextButton extends StateAnimatedWidget {

    private String text;

    public float sizeW;

    private float frameOpacity = 0;

    private float textBrightness = 0.7f;

    protected float textOpacity = 1;

    private float frameWidthOffset, frameHeightOffset;

    protected Runnable leftClick;

    public TextButton(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, String text, Runnable leftClick) {
        super(xResizer, yResizer);
        this.text = text;
        this.sizeW = Math.max(28, fontRenderer.getStringWidth(text) + 6);
        frameWidthOffset = sizeW;
        frameHeightOffset = 13;
        shape = new Shape.Rect(sizeW, 13);
        this.leftClick = leftClick;
        moduleManager.addEventListener(this);
    }

    public void rightAlign() {
        xResizer = w -> xResizer.apply(w) - sizeW;
    }

    public void setTextOpacity(float a) {
        textOpacity = a;
    }

    @Override
    public void draw(float currentTime) {
        checkState();
        fontRenderer.drawString(text, x + sizeW / 2f, y + 2, textBrightness, textBrightness, textBrightness, textOpacity, 0.25f);
        if (frameOpacity > 0)
            DrawTools.fillFrameWithColor(x - frameWidthOffset, y - frameHeightOffset, x + frameWidthOffset + sizeW, y + 13 + frameHeightOffset, 0.51f, 0x808080, frameOpacity);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    protected void open() {
        super.open();
        moduleManager.addAnimation(new Animation(2, true)
                .applyTo(new Applier(sizeW / 2, 0, value -> frameWidthOffset = value),
                        new Applier(6, 0, value -> frameHeightOffset = value)));
        moduleManager.addAnimation(new Animation(2)
                .applyTo(new Applier(1, value -> frameOpacity = value),
                        new Applier(0.7f, 1, value -> textBrightness = value))
                .onFinish(() -> openState = 2));
    }

    @Override
    protected void close() {
        super.close();
        moduleManager.addAnimation(new Animation(4)
                .applyTo(new Applier(1, 0, value -> frameOpacity = value),
                        new Applier(1, 0.7f, value -> textBrightness = value))
                .onFinish(() -> openState = 0));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (available && mouseHovered && mouseButton == 0) {
            leftClick.run();
            return true;
        }
        return false;
    }

    public static class Countdown extends TextButton {

        private final int countdown, startTick = moduleManager.getTicks();

        private boolean counting = true;

        private int displayCount;

        public Countdown(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, String text, Runnable leftClick, int countdown) {
            super(xResizer, yResizer, text, leftClick);
            this.displayCount = this.countdown = countdown;
            available = false;
        }

        @Override
        public void draw(float currentTime) {
            super.draw(currentTime);
            if (counting) {
                DrawTools.fillRectWithColor(x - sizeW, y, x, y + 13, 0x101010, textOpacity * 0.7f);
                fontRenderer.drawString(displayCount + "s", x - sizeW / 2f, y + 2, 1, 1, 1, textOpacity, 0.25f);
            }
        }

        @Override
        public void tick(int ticks) {
            if (counting) {
                counting = ticks < startTick + countdown * 20;
                displayCount = countdown - (ticks - startTick) / 20;
                if (!counting) {
                    available = true;
                    moduleManager.refreshCursor();
                }
            }
        }
    }

}
