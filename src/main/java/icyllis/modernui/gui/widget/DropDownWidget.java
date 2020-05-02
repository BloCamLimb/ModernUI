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

import icyllis.modernui.font.FontTools;
import icyllis.modernui.gui.math.Align3H;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.*;
import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.gui.math.Locator;
import icyllis.modernui.gui.popup.DropDownMenu;
import icyllis.modernui.gui.popup.PopupMenu;
import icyllis.modernui.system.ConstantsLibrary;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The widget used to open a drop down menu
 * and show the current option
 */
public class DropDownWidget extends Widget {

    private final Icon icon = new Icon(ConstantsLibrary.ICONS, 0.25f, 0.125f, 0.375f, 0.25f, true);

    private List<String> list;

    private String text;

    private int index;

    private float brightness = 0.85f;
    private float backAlpha = 0;

    @Nullable
    private IntConsumer operation;

    private final Animation bgAnimation;

    public DropDownWidget(IHost host, Builder builder) {
        super(host, builder);
        list = builder.list;
        index = builder.index;
        updateList(list, index);

        bgAnimation = new Animation(100)
                .addAppliers(new Applier(0, 0.25f, () -> backAlpha, this::setBackAlpha));
    }

    public DropDownWidget buildCallback(IntConsumer c) {
        this.operation = c;
        return this;
    }

    @Override
    public void onDraw(Canvas canvas, float time) {
        if (backAlpha > 0) {
            canvas.setRGBA(0.377f, 0.377f, 0.377f, backAlpha);
            canvas.drawRect(x1, y1, x2, y2);
        }
        canvas.setTextAlign(Align3H.RIGHT);
        canvas.setRGBA(brightness, brightness, brightness, 1);
        canvas.drawText(text, x2 - 10, y1 + 4);
        canvas.drawIcon(icon, x2 - 8, y1 + 5, x2, y2 - 3);
    }

    @Override
    public void locate(float px, float py) {
        super.locate(px, py);
    }

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        DropDownMenu menu = new DropDownMenu.Builder(list, index).setAlign(align).build(getHost()).buildCallback(this::updateValue);
        menu.locate(getHost().toAbsoluteX(x2 - 4), getHost().toAbsoluteY(y2));
        GlobalModuleManager.INSTANCE.openPopup(new PopupMenu(menu), false);
        return true;
    }

    @Override
    protected void onMouseHoverEnter(double mouseX, double mouseY) {
        super.onMouseHoverEnter(mouseX, mouseY);
        bgAnimation.start();
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        bgAnimation.invert();
    }

    private void setBackAlpha(float a) {
        backAlpha = a;
    }

    @Override
    protected void onStatusChanged(WidgetStatus status) {
        super.onStatusChanged(status);
        if (status.isListening()) {
            brightness = 0.85f;
        } else {
            brightness = 0.5f;
        }
    }

    public void updateList(@Nonnull List<String> list, int index) {
        this.list = list;
        updateValue(index);
    }

    public void updateValue(int index) {
        this.index = index;
        text = list.get(index);
        float textLength = FontTools.getStringWidth(text) + 3;
        width = textLength + 6 + 4;
        relocate();
        if (operation != null) {
            operation.accept(index);
        }
    }

    public int getIndex() {
        return index;
    }

    @Nonnull
    @Override
    public Class<? extends Widget.Builder> getBuilder() {
        return Builder.class;
    }

    public static class Builder extends Widget.Builder {

        protected final List<String> list;
        protected final int index;

        public Builder(List<String> list, int index) {
            this.list = list;
            this.index = index;
        }

        @Deprecated
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
        public DropDownWidget build(IHost host) {
            return new DropDownWidget(host, this);
        }
    }
}
