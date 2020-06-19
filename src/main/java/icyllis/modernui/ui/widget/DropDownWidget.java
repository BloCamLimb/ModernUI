/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.widget;

import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.graphics.renderer.Icon;
import icyllis.modernui.ui.master.UITools;
import icyllis.modernui.ui.master.UIManager;
import icyllis.modernui.ui.animation.Animation;
import icyllis.modernui.ui.animation.Applier;
import icyllis.modernui.ui.test.Align9D;
import icyllis.modernui.graphics.font.TextAlign;
import icyllis.modernui.ui.overlay.DropDownMenu;
import icyllis.modernui.ui.overlay.PopupMenu;
import icyllis.modernui.system.ConstantsLibrary;
import icyllis.modernui.ui.test.IHost;
import icyllis.modernui.ui.test.Locator;
import icyllis.modernui.ui.test.Widget;
import icyllis.modernui.ui.test.WidgetStatus;

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

    private final Animation bgAnimation;

    private List<String> list;
    private String text;
    private int index;

    private float brightness = 0.85f;
    private float backAlpha = 0;

    @Nullable
    private IntConsumer operation;

    public DropDownWidget(IHost host, Builder builder) {
        super(host, builder);
        list = builder.list;
        index = builder.index;
        updateList(list, index);

        bgAnimation = new Animation(100)
                .applyTo(new Applier(0, 0.25f, () -> backAlpha, this::setBackAlpha));
    }

    public DropDownWidget buildCallback(IntConsumer c) {
        this.operation = c;
        return this;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        if (backAlpha > 0) {
            canvas.setRGBA(0.377f, 0.377f, 0.377f, backAlpha);
            canvas.drawRect(x1, y1, x2, y2);
        }
        canvas.setTextAlign(TextAlign.RIGHT);
        canvas.setRGBA(brightness, brightness, brightness, 1);
        canvas.drawText(text, x2 - 10, y1 + 4);
        canvas.drawIcon(icon, x2 - 8, y1 + 5, x2, y2 - 3);
    }

    @Override
    public void locate(float px, float py) {
        super.locate(px, py);
    }

    @Override
    protected void onStatusChanged(WidgetStatus status, boolean allowAnimation) {
        super.onStatusChanged(status, allowAnimation);
        if (status.isListening()) {
            brightness = 0.85f;
        } else {
            brightness = 0.5f;
        }
    }

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        DropDownMenu menu = new DropDownMenu.Builder(list, index).setAlign(align).build(getParent()).buildCallback(this::updateValue);
        menu.locate(getParent().toAbsoluteX(x2 - 4), getParent().toAbsoluteY(y2));
        UIManager.INSTANCE.openPopup(new PopupMenu(menu), false);
        return true;
    }

    @Override
    public void onMouseHoverEnter(double mouseX, double mouseY) {
        super.onMouseHoverEnter(mouseX, mouseY);
        bgAnimation.start();
    }

    @Override
    public void onMouseHoverExit() {
        super.onMouseHoverExit();
        bgAnimation.invert();
    }

    private void setBackAlpha(float a) {
        backAlpha = a;
    }

    public void updateList(@Nonnull List<String> list, int index) {
        this.list = list;
        updateValue(index);
    }

    public void updateValue(int index) {
        this.index = index;
        text = list.get(index);
        float textLength = UITools.getStringWidth(text) + 3;
        setWidth(textLength + 6 + 4);
        if (operation != null) {
            operation.accept(index);
        }
    }

    public int getIndex() {
        return index;
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
