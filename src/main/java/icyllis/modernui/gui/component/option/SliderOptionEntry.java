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

package icyllis.modernui.gui.component.option;

import icyllis.modernui.gui.component.Slider;
import icyllis.modernui.gui.window.SettingScrollWindow;

import java.util.function.Consumer;

public class SliderOptionEntry extends OptionEntry {

    private Slider slider;

    private float minValue;

    private float maxValue;

    private float stepSize;

    private float currentValue;

    private Consumer<Float> saveOption;

    public SliderOptionEntry(SettingScrollWindow windowString, String optionTitle, float minValue, float maxValue, float currentValue, float stepSize, Consumer<Float> saveOption) {
        super(windowString, optionTitle);
        slider = new Slider(84, (currentValue - minValue) / (maxValue - minValue), this::onValueChange);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.currentValue = currentValue;
        this.stepSize = stepSize;
        this.saveOption = saveOption;
    }

    @Override
    public void drawExtra(float centerX, float y, float currentTime) {
        slider.draw(centerX + 40, y + 9);
        fontRenderer.drawString(String.valueOf(currentValue), centerX + 154, y + 6, 1, 1, 1, 1, 0.5f);
    }

    @Override
    public void mouseMoved(double deltaCenterX, double deltaY, double mouseX, double mouseY) {
        slider.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double deltaCenterX, double deltaY, double mouseX, double mouseY, int mouseButton) {
        return slider.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double deltaCenterX, double deltaY, double mouseX, double mouseY, int mouseButton) {
        return slider.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double deltaCenterX, double deltaY, double mouseX, double mouseY, int mouseButton, double deltaMouseX, double deltaMouseY) {
        return slider.mouseDragged(mouseX, mouseY, mouseButton, deltaMouseX, deltaMouseY);
    }

    protected void onValueChange(float percent) {
        currentValue = minValue + (maxValue - minValue) * percent;
        currentValue = stepSize * (Math.round(currentValue / stepSize));
        saveOption.accept(currentValue);
    }
}
