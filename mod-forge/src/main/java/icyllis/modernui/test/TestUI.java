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
import icyllis.modernui.text.FontPaint;
import icyllis.modernui.text.Spannable;
import icyllis.modernui.text.SpannableString;
import icyllis.modernui.text.TextLine;
import icyllis.modernui.text.style.AbsoluteSizeSpan;
import icyllis.modernui.text.style.ForegroundColorSpan;
import icyllis.modernui.text.style.StyleSpan;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.ScrollView;

import javax.annotation.Nonnull;

public class TestUI extends ScreenCallback {

    private TextLine mTextLine;

    @Override
    public void onCreate() {
        ScrollView contentView = new ScrollView();
        FrameLayout.LayoutParams contentViewParams = new FrameLayout.LayoutParams(500, 280);
        contentViewParams.gravity = Gravity.CENTER;

        View ll = new TestLinearLayout();
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(280, 480);
        ll.setLayoutParams(cl);
        contentView.addView(ll);

        String text = "My name is Van, I'm 30 years old, and I'm from Japan. I'm an artist, I'm a performance artist." +
                " " +
                "I'm hired for people to fulfill their fantasies, their deep dark fantasies.";
        Spannable spannable = SpannableString.valueOf(text);
        spannable.setSpan(new ForegroundColorSpan(0xfff699b4), 54, text.length(), 0);
        spannable.setSpan(new AbsoluteSizeSpan(18), 16, text.length() - 20, 0);
        spannable.setSpan(new StyleSpan(FontPaint.BOLD), text.length() - 20, text.length(), 0);
        mTextLine = new TextLine(spannable);

        contentView.setBackground(new Drawable() {
            long lastTime = AnimationHandler.currentTimeMillis();

            @Override
            public void draw(@Nonnull Canvas canvas) {
                Paint paint = Paint.take();
                Rect b = getBounds();
                paint.setRGBA(8, 8, 8, 80);
                canvas.drawRoundRect(b.left, b.top, b.right, b.bottom, 8, paint);

                mTextLine.draw(canvas, 0, 60);

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
