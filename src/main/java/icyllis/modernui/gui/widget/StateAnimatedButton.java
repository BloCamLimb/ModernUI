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

import icyllis.modernui.gui.element.StateAnimatedElement;
import net.minecraft.client.gui.IGuiEventListener;

import java.util.function.Function;

public abstract class StateAnimatedButton extends StateAnimatedElement implements IGuiEventListener {

    protected boolean available = true;

    protected boolean mouseHovered = false;

    protected FixedShape shape;

    public StateAnimatedButton(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer) {
        super(xResizer, yResizer);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (available) {
            boolean prev = mouseHovered;
            mouseHovered = shape.isMouseInShape(x, y, mouseX, mouseY);
            if (prev != mouseHovered) {
                if (mouseHovered) {
                    onMouseHoverOn();
                } else {
                    onMouseHoverOff();
                }
            }
        }
    }

    protected void onMouseHoverOn() {
        startOpen();
    }

    protected void onMouseHoverOff() {
        startClose();
    }
}
