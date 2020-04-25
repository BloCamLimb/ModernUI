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
import icyllis.modernui.gui.popup.PopupMenu;
import icyllis.modernui.system.ConstantsLibrary;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * The widget used to open a drop down menu
 * and show the current option
 */
public class DropDownWidget extends Widget {

    private final Icon icon = new Icon(ConstantsLibrary.ICONS, 0.25f, 0.125f, 0.375f, 0.25f, true);

    public List<String> list;

    private String text;

    private int index;

    private float brightness = 0.85f;
    private float backAlpha = 0;

    private final Consumer<Integer> operation;

    private final DropDownMenu.Align align;

    public DropDownWidget(Module module, @Nonnull List<String> list, int index, Consumer<Integer> operation, DropDownMenu.Align align) {
        super(module, 0, 16);
        this.operation = operation;
        this.align = align;
        updateList(list, index);
        if (align == DropDownMenu.Align.LEFT) {
            super.setAlign(Align9D.TOP_LEFT);
        } else {
            super.setAlign(Align9D.TOP_RIGHT);
        }
    }

    @Deprecated
    @Override
    public void setAlign(Align9D align) {
        throw new RuntimeException();
    }

    @Override
    public void draw(Canvas canvas, float time) {
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
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (listening && mouseButton == 0) {
            DropDownMenu menu = new DropDownMenu(getModule(), list, index, 16, this::updateValue, align);
            menu.locate(x2 - 4, (float) (y2 - (mouseY - getModule().getMouseY())));
            GlobalModuleManager.INSTANCE.openPopup(new PopupMenu(menu), false);
            return true;
        }
        return false;
    }

    @Override
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
        getModule().addAnimation(new Animation(2)
                .applyTo(new Applier(0.25f, this::setBackAlpha)));
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        getModule().addAnimation(new Animation(2)
                .applyTo(new Applier(0.25f, 0, this::setBackAlpha)));
    }

    private void setBackAlpha(float a) {
        backAlpha = a;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
        if (listening) {
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
        if (align == DropDownMenu.Align.LEFT) {
            locate(x1, y1);
        } else {
            locate(x2, y1);
        }
        operation.accept(index);
    }

    public int getIndex() {
        return index;
    }
}
