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

package icyllis.modernui.impl.setting;

import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.widget.IDiscreteSliderReceiver;
import icyllis.modernui.gui.widget.SliderDiscrete;
import icyllis.modernui.gui.scroll.SettingScrollWindow;

import java.util.function.Consumer;
import java.util.function.Function;

public class DSliderSettingEntry extends SettingEntry implements IDiscreteSliderReceiver {

    private final SliderDiscrete slider;

    private final Consumer<Integer> applyFunc;

    private final Function<Integer, String> displayStringFunc;

    private final boolean realtimeApply;

    private int minValue;

    private int currentValue;

    private int originalValue;

    private String displayString;

    public DSliderSettingEntry(SettingScrollWindow window, String title, int minValue, int maxValue, int currentValue, Consumer<Integer> applyFunc, Function<Integer, String> displayStringFunc, boolean realtimeApply) {
        super(window, title);
        this.slider = new SliderDiscrete(window.getModule(), 84, currentValue - minValue, maxValue - minValue, this);
        this.minValue = minValue;
        this.originalValue = currentValue;
        this.currentValue = currentValue;
        this.applyFunc = applyFunc;
        this.displayStringFunc = displayStringFunc;
        this.displayString = displayStringFunc.apply(currentValue);
        this.realtimeApply = realtimeApply;
    }

    @Override
    public void onLayout(float left, float right, float y) {
        super.onLayout(left, right, y);
        slider.locate(centerX + 40, y + 9);
    }

    @Override
    public void drawExtra(Canvas canvas, float time) {
        slider.draw(canvas, time);
        canvas.setRGBA(titleBrightness, titleBrightness, titleBrightness, 1);
        canvas.setTextAlign(TextAlign.RIGHT);
        canvas.drawText(displayString, x2 - 6, y1 + 6);
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (super.updateMouseHover(mouseX, mouseY)) {
            slider.updateMouseHover(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return slider.isMouseHovered() && slider.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        return slider.isMouseHovered() && slider.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        slider.setMouseHoverExit();
    }

    private void applyChange() {
        if (currentValue != originalValue) {
            applyFunc.accept(currentValue);
            originalValue = currentValue;
        }
    }

    @Override
    public void onSliderRealtimeChange(int offset) {
        currentValue = minValue + offset;
        displayString = displayStringFunc.apply(currentValue);
        if (realtimeApply) {
            applyChange();
        }
    }

    @Override
    public void onSliderFinalChange() {
        if (!realtimeApply) {
            applyChange();
        }
    }
}
