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

import icyllis.modernui.api.animation.Animation;
import icyllis.modernui.api.animation.Applier;
import icyllis.modernui.api.element.StateAnimatedElement;
import icyllis.modernui.api.widget.EventListener;
import icyllis.modernui.api.widget.Shape;
import icyllis.modernui.gui.master.DrawTools;

import java.util.function.Function;

public class LineTextButton extends StateAnimatedElement {

    protected EventListener listener;

    protected String text;

    protected final float width, halfWidth;

    protected float widthOffset;

    protected float textBrightness = 0.7f;

    protected float opacity = 0;

    protected boolean lock = false;

    public LineTextButton(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, String text, float width, int moduleID) {
        super(xResizer, yResizer);
        this.text = text;
        this.width = width;
        this.widthOffset = this.halfWidth = width / 2f;
        listener = new EventListener(xResizer, yResizer, new Shape.RectShape(width, 12));
        listener.addHoverOn(this::startOpen);
        listener.addHoverOff(this::startClose);
        moduleManager.addEventListener(listener);
        moduleManager.addModuleEvent(i -> {
            if (i == moduleID) {
                lock = true;
                startOpen();
            } else {
                lock = false;
                startClose();
            }
        });
        moduleManager.addAnimation(new Animation(3)
                .applyTo(new Applier(1f, value -> opacity = value))
                .withDelay(1));
    }

    @Override
    public void draw(float currentTime) {
        super.checkState();
        fontRenderer.drawString(text, x + halfWidth, y + 2, textBrightness, textBrightness, textBrightness, opacity, 0.25f);
        DrawTools.fillRectWithColor(x + widthOffset, y + 11, x + width - widthOffset, y + 12, 0xffffff, opacity);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        listener.resize(width, height);
    }

    @Override
    protected void open() {
        super.open();
        moduleManager.addAnimation(new Animation(3)
                .applyTo(new Applier(halfWidth, 0, value -> widthOffset = value),
                        new Applier(0.7f, 1, value -> textBrightness = value))
                .onFinish(() -> openState = 2));
    }

    @Override
    protected void close() {
        super.close();
        moduleManager.addAnimation(new Animation(3)
                .applyTo(new Applier(0, halfWidth, value -> widthOffset = value),
                        new Applier(1, 0.7f, value -> textBrightness = value))
                .onFinish(() -> openState = 0));
    }

    @Override
    public void startClose() {
        if (!lock)
            super.startClose();
    }
}
