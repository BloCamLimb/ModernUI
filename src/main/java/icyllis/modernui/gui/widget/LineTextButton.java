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
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.AnimationControl;
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.Module;
import icyllis.modernui.gui.master.Widget;
import icyllis.modernui.gui.math.Align3H;
import net.minecraft.client.resources.I18n;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Predicate;

public class LineTextButton extends Widget {

    private final AnimationControl ac = new Control(this);

    private final String text;

    private Runnable leftClickFunc = () -> {};
    private Predicate<Integer> selected = i -> false;

    private float sizeWOffset;
    private float textBrightness = 0.7f;

    public LineTextButton(Module module, Builder builder) {
        super(module, builder);
        this.text = I18n.format(builder.text);
        this.sizeWOffset = width / 2f;
    }

    public LineTextButton setCallback(Runnable r) {
        leftClickFunc = r;
        return this;
    }

    public LineTextButton setSelected(Predicate<Integer> q) {
        selected = q;
        return this;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        ac.update();
        canvas.setRGBA(textBrightness, textBrightness, textBrightness, 1.0f);
        canvas.setTextAlign(Align3H.CENTER);
        canvas.drawText(text, x1 + width / 2f, y1 + 2);
        canvas.resetColor();
        canvas.drawRect(x1 + sizeWOffset, y1 + 11, x2 - sizeWOffset, y1 + 12);
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
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
        if (ac.canChangeState()) {
            getModule().addAnimation(new Animation(3)
                    .applyTo(new Applier(textBrightness, 1, this::setTextBrightness)));
        }
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        if (ac.canChangeState()) {
            getModule().addAnimation(new Animation(3)
                    .applyTo(new Applier(textBrightness, 0.7f, this::setTextBrightness)));
        }
    }

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        leftClickFunc.run();
        return true;
    }

    public void onModuleChanged(int id) {
        if (selected.test(id)) {
            ac.startOpenAnimation();
            ac.setLockState(true);
        } else {
            ac.setLockState(false);
            ac.startCloseAnimation();
        }
    }

    private void setTextBrightness(float textBrightness) {
        this.textBrightness = textBrightness;
    }

    private void setSizeWOffset(float sizeWOffset) {
        this.sizeWOffset = sizeWOffset;
    }

    @Nonnull
    @Override
    public Class<? extends Widget.Builder<?, ?>> getBuilder() {
        return Builder.class;
    }

    public static class Builder extends Widget.Builder<Builder, LineTextButton> {

        @Expose
        public final String text;

        public Builder(@Nonnull String text) {
            this.text = text;
            super.setHeight(12);
        }

        @Deprecated
        @Override
        public Widget.Builder<Builder, LineTextButton> setHeight(float height) {
            return super.setHeight(height);
        }

        @Override
        public LineTextButton build(Module module) {
            return new LineTextButton(module, this);
        }
    }

    private static class Control extends AnimationControl {

        private final LineTextButton instance;

        public Control(LineTextButton instance) {
            this.instance = instance;
        }

        @Override
        protected void createOpenAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(3)
                    .applyTo(new Applier(instance.getWidth() / 2f, 0, instance::setSizeWOffset),
                            new Applier(instance.textBrightness, 1, instance::setTextBrightness)));
        }

        @Override
        protected void createCloseAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(3)
                    .applyTo(new Applier(0, instance.getWidth() / 2f, instance::setSizeWOffset),
                            new Applier(instance.textBrightness, 0.7f, instance::setTextBrightness)));
        }
    }

}
