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

package icyllis.modernui.gui.component.scroll;

import icyllis.modernui.gui.widget.Shape;
import net.minecraft.client.gui.IGuiEventListener;

public class ScrollCategory implements IGuiEventListener {

    protected float x, y;

    protected boolean visible = true;

    protected boolean mouseHovered = false;

    protected Shape shape;

    /**
     * In vertical list, it's height
     * In horizontal list, it's width
     */
    private final int size;

    public ScrollCategory(int size, Shape shape) {
        this.size = size;
        this.shape = shape;
    }

    /**
     * Draw =w=
     * @param alpha make entry fade in or fade out when it is on the list window edge
     */
    public void draw(float alpha) {

    }

    public void setPos(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public int getSize() {
        return size;
    }

    public void onHoverStateChanged(boolean mouseHovered) {
        this.mouseHovered = mouseHovered;
    }

    public boolean isMouseHovered() {
        return mouseHovered;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public final boolean isMouseOver(double mouseX, double mouseY) {
        return shape.isMouseInShape(x, y, mouseX, mouseY);
    }
}
