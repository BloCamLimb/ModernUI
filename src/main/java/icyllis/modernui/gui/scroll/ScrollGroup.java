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

package icyllis.modernui.gui.scroll;

public abstract class ScrollGroup {

    public int height;

    public float lastHeight; // you can't change this manually!

    public float centerX, y; // you can't change this manually!

    public ScrollGroup(int height) {
        this.height = height;
    }

    public abstract void draw(float maxY, float currentTime);

    /**
     * Called when mouseY move to this height range, whatever mouseX is
     * Use less this
     */
    public void mouseMoved(double mouseX, double mouseY) {

    }

    /**
     * Same as above
     */
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    /**
     * Same as above
     */
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    /**
     * Same as above
     */
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        return false;
    }

    /**
     * Same as above
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

}
