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

import com.google.common.collect.Lists;
import com.google.gson.annotations.Expose;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.*;
import icyllis.modernui.gui.math.Align3H;
import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.gui.math.Locator;
import net.minecraft.client.resources.I18n;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class LineTextButton extends Widget {

    private final AnimationControl lineAC;

    private final String text;

    @Nullable
    private Runnable callback;

    @Nonnull
    private Predicate<Integer> selected = b -> true;

    private float lineOffset;
    private float textBrightness = 0.7f;

    private final Animation textAnimation;

    public LineTextButton(IHost host, Builder builder) {
        super(host, builder);
        this.text = I18n.format(builder.text);
        this.lineOffset = width / 2f;

        textAnimation = new Animation(150)
                .addAppliers(new Applier(0.7f, 1, () -> textBrightness, this::setTextBrightness));

        lineAC = new AnimationControl(
                Lists.newArrayList(new Animation(150)
                        .addAppliers(new Applier(width / 2f, 0, () -> lineOffset, this::setLineOffset),
                                new Applier(textBrightness, 1, () -> textBrightness, this::setTextBrightness))),
                Lists.newArrayList(new Animation(150)
                        .addAppliers(new Applier(0, width / 2f, () -> lineOffset, this::setLineOffset),
                                new Applier(textBrightness, 0.7f, () -> textBrightness, this::setTextBrightness)))
        );
    }

    public LineTextButton buildCallback(@Nullable Runnable r, @Nullable Predicate<Integer> q) {
        callback = r;
        if (q != null) {
            selected = q;
        }
        return this;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        lineAC.update();
        canvas.setRGBA(textBrightness, textBrightness, textBrightness, 1.0f);
        canvas.setTextAlign(Align3H.CENTER);
        canvas.drawText(text, x1 + width / 2f, y1 + 2);
        canvas.resetColor();
        canvas.drawRect(x1 + lineOffset, y1 + 11, x2 - lineOffset, y1 + 12);
    }

    /*@Override
    public void draw(float time) {
        super.draw(time);
        fontRenderer.drawString(text, x1 + width / 2f, y1 + 2, textBrightness, TextAlign.CENTER);
        DrawTools.INSTANCE.resetColor();
        DrawTools.INSTANCE.drawRect(x1 + sizeWOffset, y1 + 11, x2 - sizeWOffset, y1 + 12);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }*/

    @Override
    protected void onMouseHoverEnter(double mouseX, double mouseY) {
        super.onMouseHoverEnter(mouseX, mouseY);
        if (lineAC.canChangeState()) {
            textAnimation.start();
        }
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        if (lineAC.canChangeState()) {
            textAnimation.invert();
        }
    }

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        if (callback != null) {
            callback.run();
        }
        return true;
    }

    public void onModuleChanged(int id) {
        if (selected.test(id)) {
            lineAC.startOpenAnimation();
            lineAC.setLockState(true);
        } else {
            lineAC.setLockState(false);
            lineAC.startCloseAnimation();
        }
    }

    private void setTextBrightness(float textBrightness) {
        this.textBrightness = textBrightness;
    }

    private void setLineOffset(float lineOffset) {
        this.lineOffset = lineOffset;
    }

    @Nonnull
    @Override
    public Class<? extends Widget.Builder> getBuilder() {
        return Builder.class;
    }

    public static class Builder extends Widget.Builder {

        @Expose
        public final String text;

        public Builder(@Nonnull String text) {
            this.text = text;
            super.setHeight(12);
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
        public LineTextButton build(IHost host) {
            return new LineTextButton(host, this);
        }
    }

    /*private static class Control extends AnimationControl {

        private final LineTextButton instance;

        public Control(LineTextButton instance) {
            super(openList, closeList);
            this.instance = instance;
        }

        @Override
        protected void createOpenAnimations(@Nonnull List<Animation> list) {

        }

        @Override
        protected void createCloseAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(3)
                    .addAppliers(new Applier(0, instance.width / 2f, getter, instance::setLineOffset),
                            new Applier(instance.textBrightness, 0.7f, getter, instance::setTextBrightness)));
        }
    }*/

}
