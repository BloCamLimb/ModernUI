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

package icyllis.modernui.gui.background;

import icyllis.modernui.gui.math.Align3H;
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.IDrawable;

import javax.annotation.Nonnull;

public class TextDrawable implements IDrawable {

    private String text = "";

    private float x, y;

    private final Align3H align;

    public TextDrawable(Align3H align) {
        this.align = align;
    }

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        canvas.resetColor();
        canvas.setTextAlign(align);
        canvas.drawText(text, x, y);
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setPos(float x, float y) {
        this.x = x;
        this.y = y;
    }


}
