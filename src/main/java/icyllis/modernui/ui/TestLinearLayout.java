/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.renderer.Plotter;
import icyllis.modernui.animation.Animation;
import icyllis.modernui.animation.Applier;
import icyllis.modernui.animation.ITimeInterpolator;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.Orientation;
import icyllis.modernui.view.View;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;

public class TestLinearLayout extends LinearLayout {

    private float c = 10;
    private float f = 0;

    private Animation cAnim;

    private Animation circleAnimation1;
    private Animation circleAnimation2;
    private Animation circleAnimation3;
    private Animation circleAnimation4;

    private float circleAcc1;
    private float circleAcc2;
    private float circleAcc3;
    private float circleAcc4;

    private boolean b;

    public TestLinearLayout() {
        setOrientation(Orientation.VERTICAL);
        setGravity(Gravity.CENTER);
        setDivider(new Drawable() {
            @Override
            public void draw(@Nonnull Plotter plotter) {
                plotter.moveTo(this);
                plotter.setColor(192, 192, 192, 128);
                plotter.drawLine(0, 0, getWidth(), 0);
            }

            @Override
            public int getIntrinsicHeight() {
                return 1;
            }
        });
        setShowDividers(SHOW_DIVIDER_MIDDLE | SHOW_DIVIDER_END);
        setDividerPadding(8);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(100, 18);
        for (int i = 0; i < 8; i++) {
            addView(new CView(), p);
        }
        addView(new DView(ITimeInterpolator.DECELERATE, 0), new LinearLayout.LayoutParams(60, 20));
        //addView(new DView(ITimeInterpolator.VISCOUS_FLUID, 30), new LinearLayout.LayoutParams(60, 20));
        cAnim = new Animation(200).applyTo(new Applier(10, 0, () -> c, v -> c = v).setInterpolator(ITimeInterpolator.DECELERATE));

        circleAnimation1 = new Animation(600)
                .applyTo(
                        new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc1, v -> circleAcc1 = v)
                                .setInterpolator(ITimeInterpolator.ACC_DEC)
                );
        circleAnimation2 = new Animation(600)
                .applyTo(
                        new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc2, v -> circleAcc2 = v)
                                .setInterpolator(ITimeInterpolator.ACC_DEC)
                );
        circleAnimation3 = new Animation(600)
                .applyTo(
                        new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc3, v -> circleAcc3 = v)
                                .setInterpolator(ITimeInterpolator.ACC_DEC)
                );
        circleAnimation4 = new Animation(600)
                .applyTo(
                        new Applier((float) Math.PI, (float) -Math.PI, () -> circleAcc4, v -> circleAcc4 = v)
                                .setInterpolator(ITimeInterpolator.ACC_DEC)
                );
    }

    @Override
    protected void onDraw(@Nonnull Plotter plotter) {
        super.onDraw(plotter);
        plotter.moveTo(this);
        plotter.resetColor();
        plotter.drawText("Ll" + TextFormatting.BOLD + "Ll " + plotter.getDrawingTime(), 0, 0);

        // 1

        plotter.save();
        RenderSystem.depthMask(true);

        //plotter.scale(f, f, getLeft() + 10, getTop() + 10);
        RenderSystem.translatef(0, 0, 0.001f);
        RenderSystem.colorMask(false, false, false, false);
        //plotter.setColor(0, 0, 0, 128);

        plotter.drawRect(c, c, 20 - c, 20 - c);
        RenderSystem.translatef(0, 0, -0.001f);
        RenderSystem.colorMask(true, true, true, true);


        plotter.setColor(80, 210, 240, 128);
        plotter.drawRoundedRect(0, 0, 20, 20, 3);

        plotter.restore();
        RenderSystem.depthMask(false);



        // 4

        plotter.setAlpha(255);
        plotter.save();
        plotter.translate((float) Math.sin(circleAcc1) * 8, (float) Math.cos(circleAcc1) * 8);
        plotter.drawCircle(40, 18, 3);
        plotter.restore();
        plotter.save();
        plotter.translate((float) Math.sin(circleAcc2) * 8, (float) Math.cos(circleAcc2) * 8);
        plotter.drawCircle(40, 18, 2.5f);
        plotter.restore();
        plotter.save();
        plotter.translate((float) Math.sin(circleAcc3) * 8, (float) Math.cos(circleAcc3) * 8);
        plotter.drawCircle(40, 18, 2);
        plotter.restore();
        plotter.save();
        plotter.translate((float) Math.sin(circleAcc4) * 8, (float) Math.cos(circleAcc4) * 8);
        plotter.drawCircle(40, 18, 1.5f);
        plotter.restore();


        // 2
        /*GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

        GL11.glStencilMask(0xff);

        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xff);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        plotter.setColor(255, 255, 255, 128);
        plotter.drawRect(5, 2, 15, 8);

        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glStencilFunc(GL11.GL_NOTEQUAL, 1, 0xff);

        plotter.setColor(0, 0, 0, 128);
        plotter.drawRect(0, 0, 20, 10);

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
                b = true;
            } else {
                cAnim.invert();
                b = false;
            }
        }
        int a = ticks % 20;
        if (a == 1) {
            circleAnimation1.startFull();
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
        protected void onDraw(@Nonnull Plotter plotter) {
            plotter.moveTo(this);
            String str = "AAAAAAAAAAAA";//TextFormatting.UNDERLINE + "Modern" + TextFormatting.AQUA + " UI " + TextFormatting.OBFUSCATED + "\u0629\u064a\u0628\u0631\u0639\u0644\u0627" + TextFormatting.STRIKETHROUGH + "\u2642";
            if (isMouseHovered()) {
                plotter.setColor(140, 200, 240, 128);
                plotter.drawRoundedRect(0, 1, getWidth(), getHeight() - 2, 4);
            }
            plotter.resetColor();
            plotter.drawText(str, 4, 4);
        }
    }

    private static class DView extends View {

        private final Animation animation;

        private float offsetY;

        private final int offset;

        public DView(ITimeInterpolator interpolator, int offset) {
            this.offset = offset;
            animation = new Animation(200)
                    .applyTo(new Applier(0, 60, () -> offsetY, v -> offsetY = v).setInterpolator(interpolator));
            animation.invertFull();
        }

        @Override
        protected void onDraw(@Nonnull Plotter plotter) {
            plotter.moveTo(this);
            plotter.drawText("" + TextFormatting.RED + TextFormatting.BLUE + "G", offset, offsetY + 4);
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
