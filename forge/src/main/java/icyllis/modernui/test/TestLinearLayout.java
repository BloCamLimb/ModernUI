/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

import icyllis.modernui.animation.Animator;
import icyllis.modernui.animation.LayoutTransition;
import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.material.MaterialCheckBox;
import icyllis.modernui.material.MaterialRadioButton;
import icyllis.modernui.text.FontPaint;
import icyllis.modernui.text.Spannable;
import icyllis.modernui.text.Spanned;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.text.method.ScrollingMovementMethod;
import icyllis.modernui.text.style.AbsoluteSizeSpan;
import icyllis.modernui.text.style.ForegroundColorSpan;
import icyllis.modernui.text.style.StyleSpan;
import icyllis.modernui.text.style.UnderlineSpan;
import icyllis.modernui.util.FloatProperty;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.*;

import javax.annotation.Nonnull;

import static icyllis.modernui.ModernUI.LOGGER;

public class TestLinearLayout extends LinearLayout {

    private float c = 10;
    private float f = 0;

    /*private final Animation cAnim;

    private final Animation circleAnimation1;
    private final Animation circleAnimation2;
    private final Animation circleAnimation3;
    private final Animation circleAnimation4;
    private final Animation iconRadiusAni;
    private final Animation arcStartAni;
    private final Animation arcEndAni;*/

    private final Animator mRoundRectLenAnim;
    //private final Animation roundRectAlphaAni;

    private float circleAcc1;
    private float circleAcc2;
    private float circleAcc3;
    private float circleAcc4;
    private float iconRadius = 40;
    private float arcStart = 0;
    private float arcEnd = 0;

    private float mRoundRectLen = 0;
    private float roundRectAlpha = 0;

    private boolean b;

    private int ticks;

    private final TextView mTextView;

    PopupWindow mPopupWindow = new PopupWindow();

    public TestLinearLayout() {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setDividerDrawable(new Drawable() {
            @Override
            public void draw(@Nonnull Canvas canvas) {
                Paint paint = Paint.take();
                paint.setRGBA(192, 192, 192, 128);
                canvas.drawRect(getBounds(), paint);
            }

            @Override
            public int getIntrinsicHeight() {
                return 2;
            }
        });
        setShowDividers(SHOW_DIVIDER_MIDDLE | SHOW_DIVIDER_END);

        setPadding(dp(12), dp(12), dp(12), dp(12));

        setDividerPadding(dp(8));

        setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);

        String text;
        text = "\t\t\u0639\u0646\u062f\u0645\u0627\u0020\u064a\u0631\u064a\u062f\u0020\u0627\u0644\u0639\u0627" +
                "\u0644\u0645\u0020\u0623\u0646\u0020\u202a\u064a\u062a\u0643\u0644\u0651\u0645\u0020\u202c\u0020" +
                "\u060c\u0020\u0641\u0647\u0648\u0020\u064a\u062a\u062d\u062f\u0651\u062b\u0020\u0628\u0644\u063a" +
                "\u0629\u0020\u064a\u0648\u0646\u064a\u0643\u0648\u062f\u002e\u0020\u062a\u0633\u062c\u0651\u0644" +
                "\u0020\u0627\u0644\u0622\u0646\u0020\u0644\u062d\u0636\u0648\u0631\u0020\u0627\u0644\u0645\u0624" +
                "\u062a\u0645\u0631\u0020\u0627\u0644\u062f\u0648\u0644\u064a\u0020\u0627\u0644\u0639\u0627\u0634" +
                "\u0631\u0020\u0644\u064a\u0648\u0646\u064a\u0643\u0648\u062f\u0020\u0028\u0055\u006e\u0069\u0063" +
                "\u006f\u0064\u0065\u0020\u0043\u006f\u006e\u0066\u0065\u0072\u0065\u006e\u0063\u0065\u0029\n";
        text += "\t\tMy name is Van, I'm 30 years old, and I'm from Japan. I'm an artist, I'm a performance artist. " +
                "I'm hired for people to fulfill their fantasies, their deep dark fantasies.\n" +
                "\t\t\u4f60\u770b\u8fd9\u4e2a\u5f6c\u5f6c\u0020\u624d\u559d\u51e0\u7f50\u0020\u5c31\u9189" +
                "\u4e86\u002e\u002e\u002e\u771f\u7684\u592a\u900a\u529b" +
                "\uff1b\u54e6\uff0c\u542c\u4f60" +
                "\u90a3\u4e48\u8bf4\u0020\u4f60\u5f88\u52c7" +
                "\u54e6\uff1b\u5f00\u73a9\u7b11\uff0c\u6211" +
                "\u8d85\u52c7\u7684\u597d\u4e0d\u597d\u0020\u6211\u8d85\u4f1a\u559d\u7684\u5566\n";
        text += "\t\t\u09b9\u09cd\u09af\u09be\u09b2\u09cb\u0020\u0645\u0631\u062d\u0628\u0627\u0020\ud808\udd99\ud808" +
                "\udd99";

        TextView tv = new TextView();
        tv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(100)));

        tv.setText(text, TextView.BufferType.SPANNABLE);
        Spannable spannable = (Spannable) tv.getText();
        spannable.setSpan(new ForegroundColorSpan(0xfff699b4), text.length() - 54, text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new AbsoluteSizeSpan(18), text.length() - 69, text.length() - 30,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(FontPaint.BOLD), text.length() - 50, text.length() - 40,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new UnderlineSpan(), text.length() / 2, text.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        tv.setMovementMethod(ScrollingMovementMethod.getInstance());

        mTextView = tv;

        for (int i = 0; i < 10; i++) {
            View v;
            LinearLayout.LayoutParams p;
            if (i == 4) {
                SwitchButton switchButton = new SwitchButton();
                v = switchButton;
                switchButton.setOnCheckedChangeListener((button, checked) -> {
                    /*if (checked) {
                        button.post(() -> addView(mTextView, 2));
                    } else {
                        button.post(() -> removeView(mTextView));
                    }*/
                    if (checked) {
                        mPopupWindow.setContentView(mTextView);
                        mPopupWindow.setWidth(400);
                        mPopupWindow.setHeight(200);
                        mPopupWindow.setOutsideTouchable(true);
                        mPopupWindow.showAsDropDown(button, 0, 0);
                    } else {
                        mPopupWindow.dismiss();
                    }
                });
                p = new LinearLayout.LayoutParams(dp(100), dp(36));
            } else if (i == 2) {
                continue;
            } else if (i == 3) {
                EditText textField = new EditText();
                v = textField;
                p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                textField.setHint("Your Name");
                textField.setTextSize(16);
                //textField.setSingleLine();
                //textField.setTransformationMethod(PasswordTransformationMethod.getInstance());
                textField.setPadding(dp(12), 0, dp(12), 0);
            } else if (i == 5) {
                RadioGroup group = new RadioGroup();
                v = group;
                for (int j = 0; j < 3; j++) {
                    RadioButton button = new MaterialRadioButton();
                    button.setText("Item " + j);
                    button.setTextSize(16);
                    button.setId(9 + j);
                    group.addView(button);
                }
                p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            } else if (i == 6) {
                CheckBox checkBox = new MaterialCheckBox();
                v = checkBox;
                checkBox.setText("Checkbox 0");
                checkBox.setTextSize(16);
                checkBox.setGravity(Gravity.END);
                p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            } else if (i == 7) {
                Spinner spinner = new Spinner();
                v = spinner;
                spinner.setAdapter(new TestListFragment.MyAdapter(25));
                p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            } else {
                v = new CView(i);
                p = new LinearLayout.LayoutParams(dp(200), dp(50));
            }
            if (i == 8) {
                v.setOnCreateContextMenuListener((menu, v1, menuInfo) -> {
                    menu.setQwertyMode(true);
                    menu.setGroupDividerEnabled(true);
                    MenuItem item;
                    item = menu.add(2, Menu.NONE, Menu.NONE, "Align start");
                    item.setAlphabeticShortcut('s', KeyEvent.META_CTRL_ON);
                    item.setChecked(true);
                    item = menu.add(2, Menu.NONE, Menu.NONE, "Align center");
                    item.setAlphabeticShortcut('d', KeyEvent.META_CTRL_ON);
                    item = menu.add(2, Menu.NONE, Menu.NONE, "Align end");
                    item.setAlphabeticShortcut('f', KeyEvent.META_CTRL_ON);
                    menu.setGroupCheckable(2, true, true);
                    SubMenu subMenu = menu.addSubMenu("New");
                    subMenu.add("Document");
                    subMenu.add("Image");
                    menu.add(1, Menu.NONE, Menu.NONE, "Delete");
                });
            }
            v.setClickable(true);
            p.gravity = Gravity.CENTER;
            addView(v, p);
        }
        addView(new DView(TimeInterpolator.DECELERATE, 0), new LinearLayout.LayoutParams(dp(120),
                dp(40)));

        //addView(new DView(ITimeInterpolator.VISCOUS_FLUID, 30), new LinearLayout.LayoutParams(60, 20));
        /*cAnim = new Animation(200)
                .applyTo(new Applier(20, 0, () -> this.c, v -> this.c = v)
                        .setInterpolator(TimeInterpolator.DECELERATE)
                );
        circleAnimation1 = new Animation(600)
                .applyTo(
                        new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc1, v -> circleAcc1 = v)
                                .setInterpolator(TimeInterpolator.ACCELERATE_DECELERATE)
                );
        circleAnimation2 = new Animation(600)
                .applyTo(
                        new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc2, v -> circleAcc2 = v)
                                .setInterpolator(TimeInterpolator.ACCELERATE_DECELERATE)
                );
        circleAnimation3 = new Animation(600)
                .applyTo(
                        new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc3, v -> circleAcc3 = v)
                                .setInterpolator(TimeInterpolator.ACCELERATE_DECELERATE)
                );
        circleAnimation4 = new Animation(600)
                .applyTo(
                        new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc4, v -> circleAcc4 = v)
                                .setInterpolator(TimeInterpolator.ACCELERATE_DECELERATE)
                );
        iconRadiusAni = new Animation(300)
                .applyTo(new Applier(40, 80, () -> iconRadius, v -> iconRadius = v)
                        .setInterpolator(TimeInterpolator.DECELERATE)
                );
        arcStartAni = new Animation(800)
                .applyTo(new Applier(-90, 270, () -> arcStart, v -> {
                            arcStart = v;
                            invalidate();
                        })
                                .setInterpolator(TimeInterpolator.DECELERATE)
                );
        arcEndAni = new Animation(800)
                .applyTo(new Applier(-90, 270, () -> arcEnd, v -> arcEnd = v)
                        .setInterpolator(TimeInterpolator.ACCELERATE)
                );*/

        ObjectAnimator anim = ObjectAnimator.ofFloat(this, sRoundRectLengthProp, 0, 80);
        anim.setDuration(400);
        anim.setInterpolator(TimeInterpolator.OVERSHOOT);
        anim.addUpdateListener(a -> invalidate());
        mRoundRectLenAnim = anim;

        /*roundRectAlphaAni = new Animation(250)
                .applyTo(new Applier(0, 1, () -> roundRectAlpha, v -> roundRectAlpha = v));*/

        setLayoutTransition(new LayoutTransition());
    }

    private static final FloatProperty<TestLinearLayout> sRoundRectLengthProp = new FloatProperty<>() {
        @Override
        public void setValue(@Nonnull TestLinearLayout object, float value) {
            object.mRoundRectLen = value;
        }

        @Override
        public Float get(@Nonnull TestLinearLayout object) {
            return object.mRoundRectLen;
        }
    };

    @Override
    protected void onDraw(@Nonnull Canvas canvas) {
        super.onDraw(canvas);
        /*canvas.moveTo(this);
        canvas.resetColor();
        canvas.setTextAlign(TextAlign.LEFT);
        canvas.save();
        canvas.scale(3, 3);
        canvas.drawText("A Text", 10, 0);
        canvas.drawText(ChatFormatting.BOLD + "A Text", 10, 10);
        canvas.drawText("\u0054\u0068\u0069\u0073\u0020\u0069\u0073\u0020\u0627\u0644\u0644\u063a\u0629\u0020" +
                "\u0627\u0644\u0639\u0631\u0628\u064a\u0629\u002c\u0020\u0061\u006e\u0064\u0020" +
                "\u0073\u0068\u0065\u0020\u0069\u0073\u0020\u6d77\u87ba", 10, 20);
        canvas.restore();*/

        Paint paint = Paint.take();
        paint.setAlpha(192);
        paint.setStyle(Paint.FILL);
        canvas.drawRoundRect(6, 90, 46, 104, 7, paint);

        paint.setStyle(Paint.STROKE);
        paint.setStrokeWidth(4.0f);
        canvas.save();
        canvas.rotate(-45);
        canvas.drawRoundRect(6, 110, 86, 124, 6, paint);

        paint.setStyle(Paint.FILL);
        canvas.drawRect(6, 126, 86, 156, paint);
        canvas.restore();

        //canvas.drawRoundImage(ICON, 6, 160, 166, 320, iconRadius, paint);

        paint.setStyle(Paint.STROKE);
        paint.setSmoothRadius(2.0f);
        paint.setStrokeWidth(10.0f);
        canvas.drawArc(80, 400, 60, arcStart, arcStart - arcEnd, paint);

        paint.setStyle(Paint.FILL);
        paint.setAlpha((int) (roundRectAlpha * 192));
        canvas.drawRoundRect(20, 480, 20 + mRoundRectLen * 1.6f, 480 + mRoundRectLen, 10, paint);
        paint.setAlpha(255);

        // 1

        /*canvas.save();
        RenderSystem.depthMask(true);

        //canvas.scale(f, f, getLeft() + 10, getTop() + 10);
        RenderSystem.translatef(0, 0, 0.001f);
        RenderSystem.colorMask(false, false, false, true);
        //canvas.setColor(0, 0, 0, 128);

        paint.setStyle(Paint.Style.FILL);
        paint.setSmoothRadius(0);
        canvas.drawRoundRect(c, c, 40 - c, 40 - c, 3, paint);

        RenderSystem.translatef(0, 0, -0.001f);
        RenderSystem.colorMask(true, true, true, true);

        paint.setSmoothRadius(1);
        paint.setRGBA(80, 210, 240, 128);
        canvas.drawRoundRect(0, 0, 40, 40, 6, paint);

        canvas.restore();
        RenderSystem.depthMask(false);*/


        // 4

        /*paint.reset();

        canvas.save();
        canvas.translate((float) Math.sin(circleAcc1) * 8, (float) Math.cos(circleAcc1) * 8);
        canvas.drawCircle(40, 18, 3, paint);
        canvas.restore();

        canvas.save();
        canvas.translate((float) Math.sin(circleAcc2) * 8, (float) Math.cos(circleAcc2) * 8);
        canvas.drawCircle(40, 18, 2.5f, paint);
        canvas.restore();

        canvas.save();
        canvas.translate((float) Math.sin(circleAcc3) * 8, (float) Math.cos(circleAcc3) * 8);
        canvas.drawCircle(40, 18, 2, paint);
        canvas.restore();

        canvas.save();
        canvas.translate((float) Math.sin(circleAcc4) * 8, (float) Math.cos(circleAcc4) * 8);
        canvas.drawCircle(40, 18, 1.5f, paint);
        canvas.restore();*/


        // 5

        /*canvas.drawRect(35, 55, 45, 65);
        RenderSystem.blendFuncSeparate(GL11.GL_ONE_MINUS_DST_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11
        .GL_ONE_MINUS_DST_ALPHA, GL11.GL_ZERO);
        canvas.drawCircle(40, 60, 4);
        RenderSystem.defaultBlendFunc();*/

        // 2
        /*GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

        GL11.glStencilMask(0xff);

        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xff);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        canvas.setColor(255, 255, 255, 128);
        canvas.drawRect(5, 2, 15, 8);

        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glStencilFunc(GL11.GL_NOTEQUAL, 1, 0xff);

        canvas.setColor(0, 0, 0, 128);
        canvas.drawRect(0, 0, 20, 10);

        GL11.glDisable(GL11.GL_STENCIL_TEST);*/
    }

    protected boolean onMousePressed(double mouseX, double mouseY, int mouseButton) {
        /*if (!b) {
            cAnim.start();
            b = true;
        } else {
            cAnim.invert();
            b = false;
        }
        f = 0.95f;*/
        return true;
    }

    protected boolean onMouseReleased(double mouseX, double mouseY, int mouseButton) {
        f = 1;
        return true;
    }

    public void tick() {
        /*ticks++;
        if ((ticks & 15) == 0) {
            if (!b) {
                cAnim.start();
                iconRadiusAni.start();
                b = true;
            } else {
                cAnim.invert();
                iconRadiusAni.invert();
                b = false;
            }
        }
        int a = ticks % 20;
        if (a == 1) {
            circleAnimation1.startFull();
            arcStartAni.startFull();
            arcEndAni.startFull();
            mRoundRectLenAnim.start();
            roundRectAlphaAni.startFull();
        } else if (a == 3) {
            circleAnimation2.startFull();
        } else if (a == 5) {
            circleAnimation3.startFull();
        } else if (a == 7) {
            circleAnimation4.startFull();
        }*/
    }

    private static class CView extends View {

        private final String mIndex;
        TextPaint mTextPaint = new TextPaint();

        public CView(int index) {
            mIndex = Integer.toString(index);
        }

        @Override
        protected void onDraw(@Nonnull Canvas canvas) {
            if (isHovered()) {
                Paint paint = Paint.take();
                paint.setRGBA(140, 200, 240, 128);
                canvas.drawRoundRect(0, 1, getWidth(), getHeight() - 2, 4, paint);
                canvas.drawText(mIndex, 0, mIndex.length(), 20, getHeight() >> 1, mTextPaint);
            }
        }

        @Override
        public void onHoverChanged(boolean hovered) {
            super.onHoverChanged(hovered);
            invalidate();
        }
    }

    private static class DView extends View {

        //private final Animation animation;

        private float offsetY;

        private final int offset;
        private final TextPaint mTextPaint = new TextPaint();
        private int mTicks;

        public DView(TimeInterpolator interpolator, int offset) {
            this.offset = offset;
            /*animation = new Animation(200)
                    .applyTo(new Applier(0, 60, () -> offsetY, v -> {
                        offsetY = v;
                        invalidate();
                    }).setInterpolator(interpolator));
            animation.invertFull();*/
        }

        @Override
        protected void onDraw(@Nonnull Canvas canvas) {
            canvas.drawText("DView", 0, 5, offset, offsetY + 24, mTextPaint);
        }

        /*public void tick() {
            mTicks++;
            if (mTicks % 40 == 0) {
                animation.invert();
            } else if (mTicks % 20 == 0) {
                animation.start();
            }
        }*/
    }
}
