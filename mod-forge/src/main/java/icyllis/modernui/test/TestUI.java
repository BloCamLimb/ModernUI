/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.test;

import icyllis.modernui.animation.AnimationHandler;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.math.Rect;
import icyllis.modernui.screen.ScreenCallback;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewConfig;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.ScrollView;

import javax.annotation.Nonnull;

public class TestUI extends ScreenCallback {

    @Override
    public void onCreate() {
        ScrollView contentView = new ScrollView();
        ViewConfig c = ViewConfig.get();
        FrameLayout.LayoutParams contentViewParams = new FrameLayout.LayoutParams(c.getViewSize(500), c.getViewSize(280));
        contentViewParams.gravity = Gravity.CENTER;

        View ll = new TestLinearLayout();
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(c.getViewSize(280), c.getViewSize(480));
        ll.setLayoutParams(cl);
        contentView.addView(ll);

        contentView.setBackground(new Drawable() {
            long lastTime = AnimationHandler.currentTimeMillis();

            @Override
            public void draw(@Nonnull Canvas canvas) {
                Paint paint = Paint.take();
                Rect b = getBounds();
                paint.setRGBA(8, 8, 8, 80);
                canvas.drawRoundRect(b.left, b.top, b.right, b.bottom, 8, paint);

                SpectrumGraph graph = TestMain.sGraph;
                long time = AnimationHandler.currentTimeMillis();
                long delta = time - lastTime;
                lastTime = time;
                if (graph != null) {
                    float playTime = TestMain.sTrack.getTime();
                    graph.update(delta);
                    graph.draw(canvas, getBounds().centerX(), getBounds().centerY());
                    invalidateSelf();
                }
            }
        });

        setContentView(contentView, contentViewParams);
    }
}
