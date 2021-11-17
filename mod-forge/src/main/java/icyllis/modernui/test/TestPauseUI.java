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

import icyllis.modernui.ModernUI;
import icyllis.modernui.animation.LayoutTransition;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.math.Rect;
import icyllis.modernui.mcgui.ScreenCallback;
import icyllis.modernui.text.method.ArrowKeyMovementMethod;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewConfiguration;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;

import javax.annotation.Nonnull;

public class TestPauseUI extends ScreenCallback {

    public static final int NETWORK_COLOR = 0xFF295E8A;

    private Image mButtonIcon;

    @Override
    public void onCreate() {
        final ViewConfiguration c = ViewConfiguration.get();

        var content = new LinearLayout();
        content.setOrientation(LinearLayout.VERTICAL);

        var navigation = new LinearLayout();
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);
        navigation.setLayoutTransition(new LayoutTransition());

        if (mButtonIcon == null) {
            mButtonIcon = Image.create(ModernUI.ID, "gui/gui_icon.png");
        }

        for (int i = 0; i < 8; i++) {
            var button = new NavigationButton(mButtonIcon, i * 32);
            var params = new LinearLayout.LayoutParams(c.getViewSize(32), c.getViewSize(32));
            button.setClickable(true);
            params.setMargins(i == 7 ? 26 : 2, 2, 2, 6);
            if (i == 0 || i == 7) {
                navigation.addView(button, params);
            } else {
                int index = i;
                content.postDelayed(() -> navigation.addView(button, index, params), i * 50);
            }
        }

        content.addView(navigation,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        var tab = new LinearLayout();
        tab.setOrientation(LinearLayout.VERTICAL);
        tab.setLayoutTransition(new LayoutTransition());
        tab.setBackground(new TabBackground());

        for (int i = 0; i < 3; i++) {
            var field = new TextView();
            field.setText("", TextView.BufferType.EDITABLE);
            field.setHint(switch (i) {
                case 0:
                    yield "Flux Point";
                case 1:
                    yield "Priority";
                default:
                    yield "Transfer Limit";
            });
            field.setFocusableInTouchMode(true);
            field.setMovementMethod(ArrowKeyMovementMethod.getInstance());
            field.setSingleLine();
            field.setBackground(new TextFieldBackground());
            field.setGravity(Gravity.CENTER_VERTICAL);
            field.setTextSize(16);

            var params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(c.getViewSize(20), c.getViewSize(i == 0 ? 50 : 2), c.getViewSize(20), c.getViewSize(2));

            content.postDelayed(() -> tab.addView(field, params), (i + 1) * 60);
        }

        int tabSize = c.getViewSize(340);
        content.addView(tab, new LinearLayout.LayoutParams(tabSize, tabSize));

        setContentView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
    }

    private static class TabBackground extends Drawable {

        private final float mRadius;

        public TabBackground() {
            mRadius = ViewConfiguration.get().getViewSize(16);
        }

        @Override
        public void draw(@Nonnull Canvas canvas) {
            Rect b = getBounds();
            float stroke = mRadius * 0.25f;
            float start = stroke * 0.5f;

            Paint paint = Paint.take();
            paint.setRGBA(0, 0, 0, 180);
            canvas.drawRoundRect(b.left + start, b.top + start, b.right - start, b.bottom - start, mRadius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(NETWORK_COLOR);
            canvas.drawRoundRect(b.left + start, b.top + start, b.right - start, b.bottom - start, mRadius, paint);
        }
    }

    private static class TextFieldBackground extends Drawable {

        private final float mRadius;

        public TextFieldBackground() {
            mRadius = ViewConfiguration.get().getViewSize(3);
        }

        @Override
        public void draw(@Nonnull Canvas canvas) {
            Rect b = getBounds();
            float start = mRadius * 0.5f;

            Paint paint = Paint.take();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(mRadius);
            paint.setColor(NETWORK_COLOR);
            canvas.drawRoundRect(b.left + start, b.top + start, b.right - start, b.bottom - start, mRadius, paint);
        }

        @Override
        public boolean getPadding(@Nonnull Rect padding) {
            int h = Math.round(mRadius * 2);
            int v = Math.round(mRadius * 0.5f);
            padding.set(h, v, h, v);
            return true;
        }
    }

    private static class NavigationButton extends View {

        private final Image mImage;
        private final int mSrcLeft;

        public NavigationButton(Image image, int srcLeft) {
            mImage = image;
            mSrcLeft = srcLeft;
        }

        @Override
        protected void onDraw(@Nonnull Canvas canvas) {
            Paint paint = Paint.take();
            if (!isHovered())
                paint.setRGBA(192, 192, 192, 255);
            canvas.drawImage(mImage, mSrcLeft, 352, mSrcLeft + 32, 384, 0, 0, getWidth(), getHeight(), paint);
        }

        @Override
        public void onHoverChanged(boolean hovered) {
            super.onHoverChanged(hovered);
            invalidate();
        }
    }
}
