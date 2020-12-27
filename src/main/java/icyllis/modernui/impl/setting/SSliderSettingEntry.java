/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.impl.setting;

import icyllis.modernui.font.text.TextAlign;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.test.widget.SliderSmooth;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Function;

public class SSliderSettingEntry extends SettingEntry implements SliderSmooth.IListener {

    private final SliderSmooth slider;

    private final Consumer<Double> applyFunc;

    private final Function<Double, String> displayStringFunc;

    private final boolean realtimeApply;

    /*private double minValue;

    private double maxValue;

    private float stepSize;

    private double currentValue;

    private double originalValue;*/

    private String displayString;

    public SSliderSettingEntry(SettingScrollWindow window, String optionTitle, double minValue, double maxValue, float stepSize, double currentValue, Consumer<Double> applyFunc, @Nonnull Function<Double, String> displayStringFunc, boolean realtimeApply) {
        super(window, optionTitle);
        //currentValue = MathHelper.clamp(currentValue, minValue, maxValue);
        //this.slider = new SliderSmooth(window.getModule(), 84, (currentValue - minValue) / (maxValue - minValue), this);
        this.slider = new SliderSmooth.Builder(minValue, maxValue)
                .setStepSize(stepSize)
                .setWidth(84)
                .build(window)
                .buildCallback(currentValue, this);
        /*this.minValue = minValue;
        this.maxValue = maxValue;
        this.stepSize = stepSize == 0 ? 0.01f / (float) (maxValue - minValue) : stepSize;
        this.currentValue = stepSize * (Math.round(currentValue / stepSize));
        this.originalValue = currentValue;*/
        this.applyFunc = applyFunc;
        this.displayStringFunc = displayStringFunc;
        this.displayString = displayStringFunc.apply(currentValue);
        this.realtimeApply = realtimeApply;
        
    }

    /*@Override
    public void onLayout(float left, float right, float y) {
        super.onLayout(left, right, y);
        slider.locate(centerX + 40, y + 9);
    }*/

    @Override
    public void locate(float px, float py) {
        super.locate(px, py);
        slider.locate(centerX + 40, py + 9);
    }

    @Override
    public void drawExtra(Canvas canvas, float time) {
        slider.draw(canvas, time);
        //canvas.setColor(titleBrightness, titleBrightness, titleBrightness, 1);
        canvas.setTextAlign(TextAlign.RIGHT);
        canvas.drawText(displayString, x2 - 6, y1 + 6);
        //fontRenderer.drawString(displayString, x2 - 6, y1 + 6, titleBrightness, 1.0f, TextAlign.RIGHT);
    }

    @Override
    protected boolean dispatchMouseClick(double mouseX, double mouseY, int mouseButton) {
        return slider.isMouseHovered() && slider.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected boolean dispatchMouseRelease(double mouseX, double mouseY, int mouseButton) {
        return slider.isMouseHovered() && slider.mouseReleased(mouseX, mouseY, mouseButton);
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
    public void onMouseHoverEnter(double mouseX, double mouseY) {
        super.onMouseHoverEnter(mouseX, mouseY);
    }

    @Override
    public void onMouseHoverExit() {
        super.onMouseHoverExit();
        slider.setMouseHoverExit();
    }

    /*private void applyChange() {
        if (currentValue != originalValue) {
            applyFunc.accept(currentValue);
            originalValue = currentValue;
        }
    }*/

    @Override
    public void onSliderChanged(SliderSmooth slider, double value) {
        displayString = displayStringFunc.apply(value);
        if (realtimeApply) {
            applyFunc.accept(value);
        }
    }

    @Override
    public void onSliderStopChange(SliderSmooth slider, double value) {
        if (!realtimeApply) {
            applyFunc.accept(value);
        }
    }
}
