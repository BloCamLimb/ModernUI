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

import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.Module;
import icyllis.modernui.gui.master.Widget;
import icyllis.modernui.system.ModernUI;

import javax.annotation.Nonnull;

public class StaticFrameButton extends Widget {

    private final String text;

    private final Runnable leftClickFunc;

    private float brightness;

    public StaticFrameButton(Module module, float width, String text, Runnable leftClickFunc, boolean listening) {
        super(module, width, 12);
        this.text = text;
        this.leftClickFunc = leftClickFunc;
        this.listening = listening;
        if (listening) {
            brightness = 0.7f;
        } else {
            brightness = 0.3f;
        }
    }

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        canvas.setRGBA(brightness, brightness, brightness, 1.0f);
        canvas.drawRectOutline(x1, y1, x2, y2, 0.51f);
        canvas.setTextAlign(TextAlign.CENTER);
        canvas.drawText(text, x1 + width / 2f, y1 + 2);
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
            leftClickFunc.run();
            brightness = 0.85f;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (listening && mouseButton == 0) {
            brightness = 1.0f;
            return true;
        }
        return false;
    }

    public void setListening(boolean listening) {
        if (this.listening != listening) {
            this.listening = listening;
            if (listening) {
                module.addAnimation(new Animation(2)
                        .applyTo(new Applier(brightness, 0.7f, this::setBrightness)));
            } else {
                brightness = 0.3f;
            }
        }
    }

    private void setBrightness(float brightness) {
        this.brightness = brightness;
    }
}
