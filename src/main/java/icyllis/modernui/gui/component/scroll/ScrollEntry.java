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

public abstract class ScrollEntry {

    public int height;

    public float lastHeight;

    public ScrollEntry(int height) {
        this.height = height;
    }

    public abstract void draw(float centerX, float y, float maxY, float currentTime);

    /**
     * Called when mouseY move to this height range, whatever mouseX is
     * Use less this
     * @param deltaCenterX Relative move to Center X (Range: -scrollWindowWidth / 2 <= deltaCenterX <= scrollWindowWidth / 2)
     * @param deltaY Relative move to Top Y (Range: 0 <= deltaY <= this.height)
     */
    public void mouseMoved(double deltaCenterX, double deltaY, double mouseX, double mouseY) {

    }

    /**
     * Same as above
     * @param deltaCenterX Relative move to Center X (Range: -scrollWindowWidth / 2 <= deltaCenterX <= scrollWindowWidth / 2)
     * @param deltaY Relative move to Top Y (Range: 0 <= deltaY <= this.height)
     */
    public boolean mouseClicked(double deltaCenterX, double deltaY, double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    /**
     * Same as above
     * @param deltaCenterX Relative move to Center X (Range: -scrollWindowWidth / 2 <= deltaCenterX <= scrollWindowWidth / 2)
     * @param deltaY Relative move to Top Y (Range: 0 <= deltaY <= this.height)
     */
    public boolean mouseReleased(double deltaCenterX, double deltaY, double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    /**
     * Same as above
     * @param deltaCenterX Relative move to Center X (Range: -scrollWindowWidth / 2 <= deltaCenterX <= scrollWindowWidth / 2)
     * @param deltaY Relative move to Top Y (Range: 0 <= deltaY <= this.height)
     */
    public boolean mouseDragged(double deltaCenterX, double deltaY, double mouseX, double mouseY, int mouseButton, double deltaMouseX, double deltaMouseY) {
        return false;
    }

    /**
     * Same as above
     * @param rcx Relative move to Center X (Range: -scrollWindowWidth / 2 <= rcx <= scrollWindowWidth / 2)
     * @param rty Relative move to Top Y (Range: 0 <= rty <= this.height)
     */
    public boolean mouseScrolled(double rcx, double rty, double scrollAmount) {
        return false;
    }

}
