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
import com.google.gson.annotations.SerializedName;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.IHost;
import icyllis.modernui.gui.master.Icon;
import icyllis.modernui.gui.math.Align3H;
import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.gui.math.Direction4D;
import icyllis.modernui.gui.math.Locator;
import net.minecraft.client.resources.I18n;

import javax.annotation.Nonnull;

public class TextIconButton extends IconButton {

    private final Direction4D direction;

    private final String text;
    private final int id;

    private float textAlpha = 0;

    public TextIconButton(IHost host, Builder builder) {
        super(host, builder);
        this.text = I18n.format(builder.text);
        this.direction = builder.direction;
        this.id = builder.id;
    }

    @Override
    public TextIconButton setDefaultClickable(boolean b) {
        super.setDefaultClickable(b);
        return this;
    }

    @Override
    public TextIconButton setCallback(Runnable r) {
        super.setCallback(r);
        return this;
    }

    @Override
    public TextIconButton setOnetimeCallback(Runnable r) {
        super.setOnetimeCallback(r);
        return this;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        super.onDraw(canvas, time);
        if (textAlpha > 0) {
            canvas.setRGBA(1, 1, 1, textAlpha);
            switch (direction) {
                case UP:
                    canvas.setTextAlign(Align3H.CENTER);
                    canvas.drawText(text, x1 + width / 2f, y1 - 12);
                    break;
                case DOWN:
                    canvas.setTextAlign(Align3H.CENTER);
                    canvas.drawText(text, x1 + width / 2f, y2 + 3);
                    break;
                case LEFT:
                    canvas.setTextAlign(Align3H.RIGHT);
                    canvas.drawText(text, x1 - 4, y1 + (height - 8) / 2f);
                    break;
                case RIGHT:
                    canvas.setTextAlign(Align3H.LEFT);
                    canvas.drawText(text, x2 + 4, y1 + (height - 8) / 2f);
                    break;
            }
        }
    }

    @Override
    protected void onMouseHoverEnter(double mouseX, double mouseY) {
        super.onMouseHoverEnter(mouseX, mouseY);
        getHost().addAnimation(new Animation(4)
                .applyTo(new Applier(0, 1, this::setTextAlpha)));
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        getHost().addAnimation(new Animation(4)
                .applyTo(new Applier(1, 0, this::setTextAlpha)));
    }

    private void setTextAlpha(float textAlpha) {
        this.textAlpha = textAlpha;
    }

    public void onModuleChanged(int id) {
        brightAC.setLockState(this.id == id);
        if (brightAC.canChangeState()) {
            if (!isMouseHovered()) {
                brightAC.startCloseAnimation();
            }
        }
    }

    public static class Builder extends IconButton.Builder {

        @Expose
        @SerializedName("textDirection")
        protected Direction4D direction = Direction4D.UP;

        @Expose
        protected final String text;

        @Expose
        @SerializedName("moduleId")
        protected int id = 0;

        public Builder(@Nonnull Icon icon, @Nonnull String text) {
            super(icon);
            this.text = text;
        }

        public Builder setTextDirection(Direction4D textDirection) {
            this.direction = textDirection;
            return this;
        }

        public Builder setModuleId(int id) {
            this.id = id;
            return this;
        }

        @Override
        public Builder setWidth(float width) {
            super.setWidth(width);
            return this;
        }

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
        public TextIconButton build(IHost host) {
            return new TextIconButton(host, this);
        }
    }
}
