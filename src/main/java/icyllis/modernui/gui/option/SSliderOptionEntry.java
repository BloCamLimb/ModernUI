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
import icyllis.modernui.gui.widget.SliderSmooth;
import icyllis.modernui.gui.scroll.SettingScrollWindow;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;
import java.util.function.Function;

public class SSliderOptionEntry extends OptionEntry {

    private SliderSmooth slider;

    private double minValue;

    private double maxValue;

    private float stepSize;

    private double currentValue;

    private Consumer<Double> saveOptionFunc;

    private Function<Double, String> displayStringFunc;

    private String displayString;

    public SSliderOptionEntry(SettingScrollWindow window, String optionTitle, double minValue, double maxValue, float stepSize, double currentValue, Consumer<Double> saveOption, Function<Double, String> displayStringFunc) {
        super(window, optionTitle);
        currentValue = MathHelper.clamp(currentValue, minValue, maxValue);
        slider = new SliderSmooth(window, 84, (currentValue - minValue) / (maxValue - minValue), this::onPercentageChange);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.stepSize = stepSize == 0 ? 0.01f / (float) (maxValue - minValue) : stepSize;
        this.currentValue = stepSize * (Math.round(currentValue / stepSize));
        this.saveOptionFunc = saveOption;
        this.displayStringFunc = displayStringFunc;
        this.displayString = displayStringFunc.apply(currentValue);
        
    }

    public SSliderOptionEntry(SettingScrollWindow window, String optionTitle, double minValue, double maxValue, float stepSize, double currentValue, Consumer<Double> saveOption) {
        this(window, optionTitle, minValue, maxValue, stepSize, currentValue, saveOption, String::valueOf);
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

    protected void onPercentageChange(double percentage) {
        currentValue = minValue + (maxValue - minValue) * percentage;
        currentValue = stepSize * (Math.round(currentValue / stepSize));
        displayString = displayStringFunc.apply(currentValue);
        saveOption();
    }

    public void saveOption() {
        saveOptionFunc.accept(currentValue);
    }
}
