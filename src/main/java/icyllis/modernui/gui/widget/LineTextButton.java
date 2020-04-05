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
import icyllis.modernui.gui.master.DrawTools;

import java.util.function.Predicate;

public class LineTextButton extends AnimatedWidget {

    private final String text;

    private float sizeWOffset;

    private float textBrightness = 0.7f;

    private Runnable leftClickFunc;

    private Predicate<Integer> isSelectedFunc;

    public LineTextButton(String text, float width, Runnable leftClick, Predicate<Integer> isSelected) {
        super(width, 12);
        this.text = text;
        this.sizeWOffset = width / 2f;
        this.leftClickFunc = leftClick;
        this.isSelectedFunc = isSelected;
    }

    @Override
    public void draw(float time) {
        super.draw(time);
        fontRenderer.drawString(text, x1 + width / 2f, y1 + 2, textBrightness, TextAlign.CENTER);
        DrawTools.fillRectWithColor(x1 + sizeWOffset, y1 + 11, x2 - sizeWOffset, y1 + 12, 0xffffff, 1);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    protected void onAnimationOpen() {
        manager.addAnimation(new Animation(3)
                .applyTo(new Applier(width / 2f, 0, value -> sizeWOffset = value),
                        new Applier(textBrightness, 1, value -> textBrightness = value))
                .onFinish(() -> setOpenState(true)));
    }

    @Override
    protected void onAnimationClose() {
        manager.addAnimation(new Animation(3)
                .applyTo(new Applier(0, width / 2f, value -> sizeWOffset = value),
                        new Applier(textBrightness, 0.7f, value -> textBrightness = value))
                .onFinish(() -> setOpenState(false)));
    }

    @Override
    protected void onMouseHoverEnter() {
        if (canChangeState())
            manager.addAnimation(new Animation(3)
                    .applyTo(new Applier(textBrightness, 1, value -> textBrightness = value)));
    }

    @Override
    protected void onMouseHoverExit() {
        if (canChangeState())
            manager.addAnimation(new Animation(3)
                    .applyTo(new Applier(textBrightness, 0.7f, value -> textBrightness = value)));
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

    public void onModuleChanged(int id) {
        if (isSelectedFunc.test(id)) {
            startOpenAnimation();
            setLockState(true);
        } else {
            setLockState(false);
            startCloseAnimation();
        }
    }

}
