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

package icyllis.modernui.gui.option;

import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.widget.SliderDiscrete;
import icyllis.modernui.gui.scroll.SettingScrollWindow;

import java.util.function.Consumer;
import java.util.function.Function;

public class DSliderOptionEntry extends OptionEntry {

    protected SliderDiscrete slider;

    protected int minValue;

    protected int currentValue;

    protected int originalValue;

    private Consumer<Integer> saveChangeFunc;

    private Consumer<Integer> applyChangeFunc = i -> {};

    protected Function<Integer, String> displayStringFunc;

    protected String displayString;

    public DSliderOptionEntry(SettingScrollWindow window, String title, int minValue, int maxValue, int currentValue, Consumer<Integer> saveOptionFunc, Function<Integer, String> displayStringFunc) {
        super(window, title);
        slider = new SliderDiscrete(window, 84, currentValue - minValue, maxValue - minValue, this::onDiscreteChange).setApplier(this::applyChange);
        this.minValue = minValue;
        this.originalValue = currentValue;
        this.currentValue = currentValue;
        this.saveChangeFunc = saveOptionFunc;
        this.displayStringFunc = displayStringFunc;
        this.displayString = displayStringFunc.apply(currentValue);
    }

    public DSliderOptionEntry setApplyChange(Consumer<Integer> c) {
        this.applyChangeFunc = c;
        return this;
    }

    @Override
    public void layout(float x1, float x2, float y) {
        super.layout(x1, x2, y);
        slider.setPos(centerX + 40, y + 9);
    }

    @Override
    public void drawExtra(float time) {
        slider.draw(time);
        fontRenderer.drawString(displayString, x2 - 6, y1 + 6, titleBrightness, 1.0f, TextAlign.RIGHT);
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

    protected void onDiscreteChange(int offset) {
        currentValue = minValue + offset;
        displayString = displayStringFunc.apply(currentValue);
        saveChange();
    }

    public void saveChange() {
        saveChangeFunc.accept(currentValue);
    }

    public void applyChange() {
        if (currentValue != originalValue) {
            applyChangeFunc.accept(currentValue);
            originalValue = currentValue;
        }
    }
}
