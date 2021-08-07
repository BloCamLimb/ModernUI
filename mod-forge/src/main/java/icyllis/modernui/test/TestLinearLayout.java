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
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.math.Icon;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.Orientation;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class TestLinearLayout extends LinearLayout {

    private static final Icon ICON = new Icon(new ResourceLocation(ModernUI.ID, "textures/gui/suk.png"), 0, 0, 1, 1, true);

    private float c = 10;
    private float f = 0;

    private final Animation cAnim;

    private final Animation circleAnimation1;
    private final Animation circleAnimation2;
    private final Animation circleAnimation3;
    private final Animation circleAnimation4;
    private final Animation iconRadiusAni;
    private final Animation arcStartAni;
    private final Animation arcEndAni;

    private final Animator mRoundRectLenAnim;
    private final Animation roundRectAlphaAni;

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

    public TestLinearLayout() {
        setOrientation(Orientation.VERTICAL);
        setGravity(Gravity.CENTER);
        setDivider(new Drawable() {
            @Override
            public void draw(@Nonnull Canvas canvas) {
                /*canvas.moveTo(this);
                canvas.setRGBA(192, 192, 192, 128);
                canvas.drawLine(0, 0, getWidth(), 0);*/
            }

            @Override
            public int getIntrinsicHeight() {
                return 1;
            }
        });
        setShowDividers(SHOW_DIVIDER_MIDDLE | SHOW_DIVIDER_END);
        setDividerPadding(8);

        for (int i = 0; i < 8; i++) {
            View v = new CView();
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(200, 36);
            v.setEnabled(true);
            v.setClickable(true);
            addView(v, p);
        }
        addView(new DView(Interpolator.DECELERATE, 0), new LinearLayout.LayoutParams(120, 40));

        //addView(new DView(ITimeInterpolator.VISCOUS_FLUID, 30), new LinearLayout.LayoutParams(60, 20));
        cAnim = new Animation(200).applyTo(new Applier(20, 0, () -> c, v -> c = v).setInterpolator(Interpolator.DECELERATE));

        circleAnimation1 = new Animation(600)
                .applyTo(
                        new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc1, v -> circleAcc1 = v)
                                .setInterpolator(Interpolator.ACCELERATE_DECELERATE)
                );
        circleAnimation2 = new Animation(600)
                .applyTo(
                        new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc2, v -> circleAcc2 = v)
                                .setInterpolator(Interpolator.ACCELERATE_DECELERATE)
                );
        circleAnimation3 = new Animation(600)
                .applyTo(
                        new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc3, v -> circleAcc3 = v)
                                .setInterpolator(Interpolator.ACCELERATE_DECELERATE)
                );
        circleAnimation4 = new Animation(600)
                .applyTo(
                        new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc4, v -> circleAcc4 = v)
                                .setInterpolator(Interpolator.ACCELERATE_DECELERATE)
                );
        iconRadiusAni = new Animation(300)
                .applyTo(new Applier(40, 80, () -> iconRadius, v -> iconRadius = v)
                        .setInterpolator(Interpolator.DECELERATE));

        arcStartAni = new Animation(800)
                .applyTo(new Applier(-90, 270, () -> arcStart, v -> arcStart = v)
                        .setInterpolator(Interpolator.DECELERATE));
        arcEndAni = new Animation(800)
                .applyTo(new Applier(-90, 270, () -> arcEnd, v -> arcEnd = v)
                        .setInterpolator(Interpolator.ACCELERATE));

        ObjectAnimator anim = ObjectAnimator.ofFloat(this, sRoundRectLengthProp, 0, 80);
        anim.setDuration(400);
        anim.setInterpolator(Interpolator.OVERSHOOT);
        anim.addUpdateListener(a -> invalidate());
        mRoundRectLenAnim = anim;

        roundRectAlphaAni = new Animation(250)
                .applyTo(new Applier(0, 1, () -> roundRectAlpha, v -> roundRectAlpha = v));
    }

    private static final FloatProperty<TestLinearLayout> sRoundRectLengthProp = new FloatProperty<>() {
        @Override
        public void setValue(@Nonnull TestLinearLayout target, float value) {
            target.mRoundRectLen = value;
        }

        @Override
        public Float get(@Nonnull TestLinearLayout target) {
            return target.mRoundRectLen;
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
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(6, 90, 46, 104, 7, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4.0f);
        canvas.save();
        canvas.rotate(30);
        canvas.drawRoundRect(6, 110, 86, 124, 6, paint);
        canvas.restore();

        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(6, 126, 86, 156, paint);

        String tcc = "今日も一日頑張るぞい";
        canvas.drawTextRun(tcc, 0, tcc.length(), 20, 270, false, new TextPaint());

        //canvas.drawRoundImage(ICON, 6, 160, 166, 320, iconRadius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setSmoothRadius(2.0f);
        paint.setStrokeWidth(10.0f);
        canvas.drawArc(80, 400, 60, arcStart, arcStart - arcEnd, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha((int) (roundRectAlpha * 255));
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

        paint.reset();

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
        canvas.restore();


        // 5

        /*canvas.drawRect(35, 55, 45, 65);
        RenderSystem.blendFuncSeparate(GL11.GL_ONE_MINUS_DST_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE_MINUS_DST_ALPHA, GL11.GL_ZERO);
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

    @Override
    protected boolean onMousePressed(double mouseX, double mouseY, int mouseButton) {
        if (!b) {
            cAnim.start();
            b = true;
        } else {
            cAnim.invert();
            b = false;
        }
        f = 0.95f;
        return true;
    }

    @Override
    protected boolean onMouseReleased(double mouseX, double mouseY, int mouseButton) {
        f = 1;
        return true;
    }

    @Override
    protected void tick(int ticks) {
        super.tick(ticks);
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
        }
    }

    private static class CView extends View {

        @Override
        protected void onDraw(@Nonnull Canvas canvas) {
            String str = ChatFormatting.UNDERLINE + "Modern" + ChatFormatting.AQUA + " UI"/* + TextFormatting.OBFUSCATED + "\u0629\u064a\u0628\u0631\u0639\u0644\u0627" + TextFormatting.STRIKETHROUGH + "\u2642"*/;
            if (isHovered()) {
                Paint paint = Paint.take();
                paint.setRGBA(140, 200, 240, 128);
                canvas.drawRoundRect(0, 1, getWidth(), getHeight() - 2, 4, paint);
            }
            /*canvas.resetColor();
            canvas.setTextAlign(TextAlign.CENTER);
            canvas.drawText(str, getWidth() >> 1, 4);*/
        }
    }

    private static class DView extends View {

        private final Animation animation;

        private float offsetY;

        private final int offset;

        public DView(Interpolator interpolator, int offset) {
            this.offset = offset;
            animation = new Animation(200)
                    .applyTo(new Applier(0, 60, () -> offsetY, v -> offsetY = v).setInterpolator(interpolator));
            animation.invertFull();
        }

        @Override
        protected void onDraw(@Nonnull Canvas canvas) {
            /*canvas.setTextAlign(TextAlign.LEFT);
            canvas.drawText("" + ChatFormatting.RED + ChatFormatting.BLUE + "G", offset, offsetY + 4);*/
        }

        @Override
        protected void tick(int ticks) {
            super.tick(ticks);
            /*if (ticks % 40 == 0) {
                animation.invert();
            } else if (ticks % 20 == 0) {
                animation.start();
            }*/
        }
    }
}
