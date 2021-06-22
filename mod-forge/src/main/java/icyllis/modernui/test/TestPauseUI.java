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

import icyllis.modernui.animation.Animation;
import icyllis.modernui.animation.Applier;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.math.TextAlign;
import icyllis.modernui.screen.ScreenCallback;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.Orientation;
import icyllis.modernui.widget.ScrollView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.LanguageInfo;

import javax.annotation.Nonnull;
import java.awt.*;

import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class TestPauseUI extends ScreenCallback {

    @Override
    public void onCreate() {
        FrameLayout frameLayout = new FrameLayout();
        View child = new NavigationBar();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(32, MATCH_PARENT);
        frameLayout.addView(child, params);
        params = new FrameLayout.LayoutParams(160, MATCH_PARENT, Gravity.CENTER);
        params.setMargins(0, 20, 0, 20);
        ScrollView scrollView = new ScrollView();
        LinearLayout linearLayout = new LinearLayout();
        linearLayout.setOrientation(Orientation.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setDivider(new Drawable() {
            @Override
            public void draw(@Nonnull Canvas canvas) {
                canvas.setRGBA(192, 192, 192, 128);
                canvas.drawLine(0, 0, getWidth(), 0);
            }

            @Override
            public int getIntrinsicHeight() {
                return 1;
            }
        });
        linearLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE | LinearLayout.SHOW_DIVIDER_END);
        linearLayout.setDividerPadding(8);
        LanguageInfo lang = Minecraft.getInstance().getLanguageManager().getSelected();
        String[] list = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(lang.getJavaLocale());
        for (String typeface : list) {
            linearLayout.addView(new Ent(typeface), new LinearLayout.LayoutParams(MATCH_PARENT, 10));
        }
        scrollView.addView(linearLayout, new FrameLayout.LayoutParams(MATCH_PARENT, 3000));
        frameLayout.addView(scrollView, params);
        setContentView(frameLayout, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    private static class Ent extends View {

        private final String k;

        public Ent(String k) {
            this.k = k;
        }

        @Override
        protected void onDraw(@Nonnull Canvas canvas) {
            super.onDraw(canvas);
            canvas.resetColor();
            canvas.setTextAlign(TextAlign.CENTER);
            canvas.drawText(k, getWidth() >> 1, 0);
        }
    }

    private static class NavigationBar extends View {

        private float a = 0;

        public NavigationBar() {
            new Animation(200).applyTo(new Applier(0, 0.51f, () -> a, v -> a = v)).start();
        }

        @Override
        protected void onDraw(@Nonnull Canvas canvas) {
            Paint paint = Paint.take();
            paint.setSmoothRadius(0);
            paint.setStyle(Paint.Style.FILL);
            paint.setRGBA(96, 96, 96, (int) (a * 255));
            canvas.drawRect(0, 0, getRight(), getBottom(), paint);
        }
    }
}
