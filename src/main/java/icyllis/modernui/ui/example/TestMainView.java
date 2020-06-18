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

package icyllis.modernui.ui.example;

import icyllis.modernui.graphics.font.TextAlign;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.ui.master.View;

import javax.annotation.Nonnull;

public class TestMainView extends View {

    public TestMainView() {

    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas, float time) {
        super.onDraw(canvas, time);
        canvas.setTextAlign(TextAlign.CENTER);
        canvas.drawText("AAA", getLeft() + getWidth() / 2.0f, getTop() + 6);
    }

    @Override
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
        ModernUI.LOGGER.info("mouse enter");
    }

    @Override
    protected boolean onMouseLeftClicked(double mouseX, double mouseY) {
        ModernUI.LOGGER.info("left click");
        return true;
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        ModernUI.LOGGER.info("mouse exit");
    }
}
