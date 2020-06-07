/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.background;

import icyllis.modernui.ui.animation.Animation;
import icyllis.modernui.ui.animation.Applier;
import icyllis.modernui.ui.animation.IInterpolator;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.ui.master.IDrawable;

import javax.annotation.Nonnull;

public class SSExpandableBox implements IDrawable, Animation.IListener {

    public static final int RIGHT_TOP_BOTTOM = 0;

    private final Animation barAnimation;
    private final Animation panelAnimation;

    private final float width, height;

    private float x, y;

    private float barLength;
    private float panelLength;

    private boolean c;

    public SSExpandableBox(float width, float height, int mode) {
        this.width = width;
        this.height = height;
        barAnimation = new Animation(200)
                .applyTo(new Applier(0, height, () -> barLength, v -> barLength = v)
                        .setInterpolator(IInterpolator.DECELERATE))
                .listen(this);
        panelAnimation = new Animation(200)
                .applyTo(new Applier(0, width - 4, () -> panelLength, v -> panelLength = v)
                        .setInterpolator(IInterpolator.DECELERATE))
                .listen(this);
    }

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        if (panelLength > 0) {
            canvas.setRGBA(0, 0, 0, 0.5f);
            canvas.drawFeatheredRect(x + width - 3 - panelLength, y + 1, x + width - 1, y + barLength - 1, 1);
            canvas.resetColor();
            canvas.drawFeatheredRect(x + width - 4 - panelLength, y, x + width - 2 - panelLength, y + barLength, 1);
        }
        if (barLength > 0) {
            canvas.resetColor();
            canvas.drawFeatheredRect(x + width - 2, y, x + width, y + barLength, 1);
        }
    }

    @Override
    public void resize(int width, int height) {
        x = width / 2f - 140;
        y = height / 2f;
    }

    @Override
    public void onAnimationStart(@Nonnull Animation animation, boolean isReverse) {
        if (animation == panelAnimation) {
            if (isReverse) {
                onPanelClose();
            }
        }
    }

    @Override
    public void onAnimationEnd(@Nonnull Animation animation, boolean isReverse) {
        if (animation == panelAnimation) {
            if (isReverse) {
                barAnimation.invert();
            } else {
                onPanelFullOpen();
            }
        } else if (animation == barAnimation) {
            if (!isReverse) {
                panelAnimation.start();
            }
        }
    }

    public void iterateOpen() {
        c = !c;
        if (c)
            barAnimation.start();
        else
            panelAnimation.invert();
    }

    protected void onPanelFullOpen() {

    }

    protected void onPanelClose() {

    }
}
