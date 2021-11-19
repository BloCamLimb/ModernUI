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
import icyllis.modernui.animation.*;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.math.Rect;
import icyllis.modernui.mcgui.ScreenCallback;
import icyllis.modernui.text.TextPaint;
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
            params.setMarginsRelative(i == 7 ? 26 : 2, 2, 2, 6);
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
            var v = new TextView();
            v.setText(switch (i) {
                case 0:
                    yield "Flux Point";
                case 1:
                    yield "0";
                default:
                    yield "800000";
            }, TextView.BufferType.EDITABLE);
            v.setHint(switch (i) {
                case 0:
                    yield "Flux Point";
                case 1:
                    yield "Priority";
                default:
                    yield "Transfer Limit";
            });
            v.setFocusableInTouchMode(true);
            v.setMovementMethod(ArrowKeyMovementMethod.getInstance());
            v.setSingleLine();
            v.setBackground(new TextFieldBackground());
            v.setGravity(Gravity.CENTER_VERTICAL);
            v.setTextSize(16);
            v.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    new TextFieldStart(mButtonIcon, (((i + 1) % 3) + 1) * 64), null, null, null);
            v.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

            var params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(c.getViewSize(20), c.getViewSize(i == 0 ? 50 : 2), c.getViewSize(20),
                    c.getViewSize(2));

            content.postDelayed(() -> tab.addView(v, params), (i + 1) * 100);
        }

        {
            var v = new ConnectorView(mButtonIcon);
            var params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            params.setMargins(c.getViewSize(8), c.getViewSize(2), c.getViewSize(8), c.getViewSize(8));
            content.postDelayed(() -> tab.addView(v, params), 400);
        }

        int tabSize = c.getViewSize(340);
        content.addView(tab, new LinearLayout.LayoutParams(tabSize, tabSize));

        setContentView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
    }

    private static class TabBackground extends Drawable {

        private final float mRadius;
        private final TextPaint mTextPaint;

        public TabBackground() {
            mRadius = ViewConfiguration.get().getViewSize(16);
            mTextPaint = new TextPaint();
            mTextPaint.setFontSize(ViewConfiguration.get().getTextSize(16));
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

            canvas.drawText("BloCamLimb's Network", 0, 20, b.exactCenterX(), b.top + mRadius * 1.8f,
                    Gravity.CENTER_HORIZONTAL,
                    mTextPaint);
        }
    }

    private static class TextFieldStart extends Drawable {

        private final Image mImage;
        private final int mSrcLeft;
        private final int mSize;

        public TextFieldStart(Image image, int srcLeft) {
            mImage = image;
            mSrcLeft = srcLeft;
            mSize = ViewConfiguration.get().getViewSize(24);
        }

        @Override
        public void draw(@Nonnull Canvas canvas) {
            Rect b = getBounds();
            canvas.drawImage(mImage, mSrcLeft, 192, mSrcLeft + 64, 256, b.left, b.top, b.right, b.bottom, null);
        }

        @Override
        public int getIntrinsicWidth() {
            return mSize;
        }

        @Override
        public int getIntrinsicHeight() {
            return mSize;
        }

        @Override
        public boolean getPadding(@Nonnull Rect padding) {
            int h = Math.round(mSize / 4f);
            int v = Math.round(mSize / 6f);
            padding.set(h, v, h, v);
            return true;
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
            int h = Math.round(mRadius);
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

    private static class ConnectorView extends View {

        private final Image mImage;
        private final int mSize;
        private float mRodLength;
        private final Paint mBoxPaint = new Paint();

        private final ObjectAnimator mRodAnimator;
        private final ObjectAnimator mBoxAnimator;

        public ConnectorView(Image image) {
            mImage = image;
            mSize = ViewConfiguration.get().getViewSize(32);
            mRodAnimator = ObjectAnimator.ofFloat(this, new FloatProperty<>() {
                @Override
                public void setValue(@Nonnull ConnectorView target, float value) {
                    target.mRodLength = value;
                    invalidate();
                }

                @Override
                public Float get(@Nonnull ConnectorView target) {
                    return target.mRodLength;
                }
            }, 0, ViewConfiguration.get().getViewSize(32));
            mRodAnimator.setInterpolator(TimeInterpolator.DECELERATE);
            mRodAnimator.setDuration(400);
            mRodAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationEnd(@Nonnull Animator animation, boolean isReverse) {
                    mBoxAnimator.start();
                }
            });
            mBoxAnimator = ObjectAnimator.ofInt(mBoxPaint, new IntProperty<>() {
                @Override
                public void setValue(@Nonnull Paint target, int value) {
                    target.setAlpha(value);
                    invalidate();
                }

                @Override
                public Integer get(@Nonnull Paint target) {
                    return target.getColor() >>> 24;
                }
            }, 0, 128);
            mRodAnimator.setInterpolator(TimeInterpolator.LINEAR);
            mBoxAnimator.setDuration(400);
            mBoxPaint.setRGBA(64, 64, 64, 0);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            mRodAnimator.start();
        }

        @Override
        protected void onDraw(@Nonnull Canvas canvas) {
            Paint paint = Paint.take();
            paint.setColor(NETWORK_COLOR);
            paint.setAlpha(192);
            paint.setStrokeWidth(mSize / 8f);

            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;

            int boxAlpha = mBoxPaint.getColor() >>> 24;

            float px1l = centerX - (15 / 64f) * mSize;
            float py1 = centerY + (8 / 64f) * mSize;
            canvas.save();
            canvas.rotate(22.5f, px1l, py1);
            canvas.drawRoundLine(px1l, py1, px1l - mRodLength * 2, py1, paint);
            canvas.restore();

            if (boxAlpha > 0) {
                canvas.drawRect(px1l - mSize * 2.9f, py1 - mSize * 1.1f,
                        px1l - mSize * 1.9f, py1 - mSize * 0.1f, mBoxPaint);
            }

            float px1r = centerX + (15 / 64f) * mSize;
            canvas.save();
            canvas.rotate(-22.5f, px1r, py1);
            canvas.drawRoundLine(px1r, py1, px1r + mRodLength * 2, py1, paint);
            canvas.restore();

            if (boxAlpha > 0) {
                canvas.drawRect(px1r + mSize * 1.9f, py1 - mSize * 1.1f,
                        px1r + mSize * 2.9f, py1 - mSize * 0.1f, mBoxPaint);
            }

            float py2 = centerY + (19 / 64f) * mSize;
            canvas.drawRoundLine(centerX, py2, centerX, py2 + mRodLength, paint);

            if (boxAlpha > 0) {
                canvas.drawRect(centerX - mSize * .5f, py2 + mSize * 1.1f,
                        centerX + mSize * .5f, py2 + mSize * 2.1f, mBoxPaint);
            }

            float offset = mSize / 2f;
            canvas.drawImage(mImage, 0, 192, 64, 256,
                    centerX - offset, centerY - offset,
                    centerX + offset, centerY + offset, null);

            canvas.save();
            canvas.rotate(-22.5f, px1l, py1);
            canvas.drawRoundLine(px1l, py1, px1l - mRodLength * 2, py1, paint);
            canvas.restore();

            if (boxAlpha > 0) {
                canvas.drawRect(px1l - mSize * 2.9f, py1 + mSize * 0.1f,
                        px1l - mSize * 1.9f, py1 + mSize * 1.1f, mBoxPaint);
            }

            canvas.save();
            canvas.rotate(22.5f, px1r, py1);
            canvas.drawRoundLine(px1r, py1, px1r + mRodLength * 2, py1, paint);
            canvas.restore();

            if (boxAlpha > 0) {
                canvas.drawRect(px1r + mSize * 1.9f, py1 + mSize * 0.1f,
                        px1r + mSize * 2.9f, py1 + mSize * 1.1f, mBoxPaint);
            }

            py2 = centerY - (19 / 64f) * mSize;
            canvas.drawRoundLine(centerX, py2, centerX, py2 - mRodLength, paint);

            if (boxAlpha > 0) {
                canvas.drawRect(centerX - mSize * .5f, py2 - mSize * 2.1f,
                        centerX + mSize * .5f, py2 - mSize * 1.1f, mBoxPaint);
            }
        }
    }
}
