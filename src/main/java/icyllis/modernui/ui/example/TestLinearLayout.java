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

package icyllis.modernui.ui.example;

import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.ui.animation.Animation;
import icyllis.modernui.ui.animation.Applier;
import icyllis.modernui.ui.animation.ITimeInterpolator;
import icyllis.modernui.ui.layout.Gravity;
import icyllis.modernui.ui.layout.LinearLayout;
import icyllis.modernui.ui.layout.Orientation;
import icyllis.modernui.ui.master.View;

import javax.annotation.Nonnull;

public class TestLinearLayout extends LinearLayout {

    public TestLinearLayout() {
        setOrientation(Orientation.VERTICAL);
        setGravity(Gravity.CENTER);
        setDivider(new Drawable() {
            @Override
            public void draw(@Nonnull Canvas canvas, int left, int top, int right, int bottom) {
                canvas.setRGBA(0.75f, 0.75f, 0.75f, 0.25f);
                canvas.drawRect(left, top, right, bottom);
            }

            @Override
            public int getIntrinsicHeight() {
                return 1;
            }
        });
        setShowDividers(SHOW_DIVIDER_MIDDLE | SHOW_DIVIDER_END);
        setDividerPadding(2);
        addView(new CView(), new LinearLayout.LayoutParams(60, 20));
        addView(new CView(), new LinearLayout.LayoutParams(60, 20));
        addView(new CView(), new LinearLayout.LayoutParams(60, 20));
        addView(new DView(ITimeInterpolator.SINE, 0), new LinearLayout.LayoutParams(60, 20));
        //addView(new DView(ITimeInterpolator.VISCOUS_FLUID, 30), new LinearLayout.LayoutParams(60, 20));
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas, int time) {
        super.onDraw(canvas, time);
        canvas.resetColor();
        canvas.drawText("LinearLayout", getLeft(), getTop());
    }

    @Override
    protected boolean onMouseLeftClicked(int mouseX, int mouseY) {
        ModernUI.LOGGER.info("left click");
        return true;
    }

    private static class CView extends View {

        @Override
        protected void onDraw(@Nonnull Canvas canvas, int time) {
            canvas.resetColor();
            canvas.drawText("Child", getLeft(), getTop() + 4);
        }
    }

    private static class DView extends View {

        private final Animation animation;

        private float offsetX;

        private int offset;

        public DView(ITimeInterpolator interpolator, int offset) {
            this.offset = offset;
            animation = new Animation(300)
                    .applyTo(new Applier(0, 60, () -> offsetX, v -> offsetX = v).setInterpolator(interpolator));
        }

        @Override
        protected void onDraw(@Nonnull Canvas canvas, int time) {
            canvas.drawText("Go My Way", getLeft() + offset, getTop() + offsetX);
        }

        @Override
        protected void tick(int ticks) {
            super.tick(ticks);
            if (ticks % 40 == 0) {
                animation.invert();
            } else if (ticks % 20 == 0) {
                animation.start();
            }
        }
    }
}
