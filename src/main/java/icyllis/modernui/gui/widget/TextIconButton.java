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

import icyllis.modernui.gui.math.Align3H;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.Icon;
import icyllis.modernui.gui.master.Module;

import javax.annotation.Nonnull;

public class TextIconButton extends IconButton {

    private final Direction direction;

    private final String text;
    private int id = 0;

    private float textAlpha = 0;

    public TextIconButton(Module module, String text, float width, float height, Icon icon, Runnable leftClick, Direction direction) {
        super(module, width, height, icon, leftClick);
        this.text = text;
        this.direction = direction;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDefaultOpen() {
        setIconBrightness(1.0f);
        iconAC.startOpenAnimation();
    }

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        super.draw(canvas, time);
        if (textAlpha > 0) {
            canvas.setRGBA(1, 1, 1, textAlpha);
            switch (direction) {
                case UP:
                    canvas.setTextAlign(Align3H.CENTER);
                    canvas.drawText(text, x1 + width / 2f, y1 - 12);
                    break;
                case DOWN:
                    canvas.setTextAlign(Align3H.CENTER);
                    canvas.drawText(text, x1 + width / 2f, y2 + 3);
                    break;
                case LEFT:
                    canvas.setTextAlign(Align3H.RIGHT);
                    canvas.drawText(text, x1 - 4, y1 + (height - 8) / 2f);
                    break;
                case RIGHT:
                    canvas.setTextAlign(Align3H.LEFT);
                    canvas.drawText(text, x2 + 4, y1 + (height - 8) / 2f);
                    break;
            }
        }
    }

    @Override
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
        getModule().addAnimation(new Animation(4)
                .applyTo(new Applier(0, 1, this::setTextAlpha)));
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        getModule().addAnimation(new Animation(4)
                .applyTo(new Applier(1, 0, this::setTextAlpha)));
    }

    private void setTextAlpha(float textAlpha) {
        this.textAlpha = textAlpha;
    }

    public void onModuleChanged(int id) {
        /*if (this.id == id && !iconAC.isAnimationOpen()) {
            iconAC.startOpenAnimation();
        }*/
        iconAC.setLockState(this.id == id);
        if (iconAC.canChangeState()) {
            if (!isMouseHovered()) {
                iconAC.startCloseAnimation();
            }
        }
    }

    public enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }
}
