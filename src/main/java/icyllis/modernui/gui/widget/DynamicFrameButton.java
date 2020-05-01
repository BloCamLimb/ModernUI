/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.gui.widget;

import com.google.gson.annotations.Expose;
import icyllis.modernui.gui.math.Align3H;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.*;
import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.gui.math.Locator;
import net.minecraft.client.resources.I18n;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Used in confirm popup
 */
public class DynamicFrameButton extends Button {

    private final AnimationControl frameAC = new Control(this);

    private String text;

    private float frameAlpha = 0;

    private float fwo, fho;

    public DynamicFrameButton(IHost host, Builder builder) {
        super(host, builder);
        this.text = I18n.format(builder.text);
        this.fwo = width;
        this.fho = height;
    }

    @Override
    public DynamicFrameButton setDefaultClickable(boolean b) {
        super.setDefaultClickable(b);
        return this;
    }

    @Override
    public DynamicFrameButton setCallback(Runnable r) {
        super.setCallback(r);
        return this;
    }

    @Override
    public DynamicFrameButton setOnetimeCallback(Runnable r) {
        super.setOnetimeCallback(r);
        return this;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        super.onDraw(canvas, time);
        frameAC.update();
        canvas.setRGBA(brightness, brightness, brightness, 1.0f);
        canvas.setTextAlign(Align3H.CENTER);
        canvas.drawText(text, x1 + width / 2f, y1 + 2);
        if (frameAlpha > 0) {
            canvas.setRGBA(0.5f, 0.5f, 0.5f, frameAlpha);
            canvas.drawRectOutline(x1 - fwo, y1 - fho, x2 + fwo, y2 + fho, 0.51f);
        }
    }

    /*@Override
    public void draw(float time) {
        super.draw(time);
        fontRenderer.drawString(text, x1 + width / 2f, y1 + 2, textBrightness, TextAlign.CENTER);
        if (frameAlpha > 0) {
            DrawTools.INSTANCE.setRGBA(0.5f, 0.5f, 0.5f, frameAlpha);
            DrawTools.INSTANCE.drawRectOutline(x1 - fwo, y1 - fho, x2 + fwo, y2 + fho, 0.51f);
        }
    }*/

    @Override
    protected void onMouseHoverEnter(double mouseX, double mouseY) {
        super.onMouseHoverEnter(mouseX, mouseY);
        frameAC.startOpenAnimation();
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        frameAC.startCloseAnimation();
    }

    @Nonnull
    @Override
    public Class<? extends Widget.Builder> getBuilder() {
        return Builder.class;
    }

    public void setFrameAlpha(float frameAlpha) {
        this.frameAlpha = frameAlpha;
    }

    public void setFwo(float fwo) {
        this.fwo = fwo;
    }

    public void setFho(float fho) {
        this.fho = fho;
    }

    public static class Countdown extends DynamicFrameButton {

        private final int startTick = GlobalModuleManager.INSTANCE.getTicks();

        private final int countdown;

        private boolean counting = true;

        private int displayCount;

        public Countdown(IHost host, Builder builder) {
            super(host, builder);
            this.displayCount = this.countdown = builder.countdown;
            super.setDefaultClickable(false);
        }

        @Deprecated
        @Override
        public DynamicFrameButton setDefaultClickable(boolean b) {
            return super.setDefaultClickable(b);
        }

        /*@Override
        public void draw(float time) {
            super.draw(time);
            if (counting) {
                DrawTools.INSTANCE.setRGBA(0.03f, 0.03f, 0.03f, 0.7f);
                DrawTools.INSTANCE.drawRect(x1, y1, x2, y2);
                fontRenderer.drawString(displayCount + "s", x1 + width / 2f, y1 + 2, 1, TextAlign.CENTER);
            }
        }*/

        @Override
        public void onDraw(@Nonnull Canvas canvas, float time) {
            super.onDraw(canvas, time);
            if (counting) {
                canvas.setRGBA(0.03f, 0.03f, 0.03f, 0.7f);
                canvas.drawRect(x1, y1, x2, y2);
                canvas.resetColor();
                canvas.drawText(displayCount + "s", x1 + width / 2f, y1 + 2);
            }
        }

        @Override
        public void tick(int ticks) {
            if (counting) {
                counting = ticks < startTick + countdown * 20;
                displayCount = countdown - (ticks - startTick) / 20;
                if (!counting) {
                    setStatus(WidgetStatus.ACTIVE);
                    getHost().refocusMouseCursor();
                }
            }
        }

        @Nonnull
        @Override
        public Class<? extends Widget.Builder> getBuilder() {
            return Builder.class;
        }

        public static class Builder extends DynamicFrameButton.Builder {

            @Expose
            protected final int countdown;

            public Builder(@Nonnull String text, int countdown) {
                super(text);
                this.countdown = countdown;
            }

            @Override
            public Builder setWidth(float width) {
                super.setWidth(width);
                return this;
            }

            @Override
            public Builder setLocator(@Nonnull Locator locator) {
                super.setLocator(locator);
                return this;
            }

            @Override
            public Builder setAlign(@Nonnull Align9D align) {
                super.setAlign(align);
                return this;
            }

            @Nonnull
            @Override
            public DynamicFrameButton.Countdown build(IHost host) {
                return new DynamicFrameButton.Countdown(host, this);
            }
        }
    }

    public static class Builder extends Widget.Builder {

        @Expose
        protected final String text;

        public Builder(@Nonnull String text) {
            this.text = text;
            super.setHeight(13);
        }

        @Override
        public Builder setWidth(float width) {
            super.setWidth(width);
            return this;
        }

        @Deprecated
        @Override
        public Builder setHeight(float height) {
            super.setHeight(height);
            return this;
        }

        @Override
        public Builder setLocator(@Nonnull Locator locator) {
            super.setLocator(locator);
            return this;
        }

        @Override
        public Builder setAlign(@Nonnull Align9D align) {
            super.setAlign(align);
            return this;
        }

        @Nonnull
        @Override
        public Widget build(IHost host) {
            return new DynamicFrameButton(host, this);
        }
    }

    private static class Control extends AnimationControl {

        private final DynamicFrameButton instance;

        public Control(DynamicFrameButton instance) {
            this.instance = instance;
        }

        @Override
        protected void createOpenAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(2, true)
                    .applyTo(new Applier(instance.getWidth() / 2f, 0, instance::setFwo),
                            new Applier(6, 0, instance::setFho)));
            list.add(new Animation(2)
                    .applyTo(new Applier(0, 1, instance::setFrameAlpha)));
        }

        @Override
        protected void createCloseAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(4)
                    .applyTo(new Applier(1, 0, instance::setFrameAlpha)));
        }
    }

}
