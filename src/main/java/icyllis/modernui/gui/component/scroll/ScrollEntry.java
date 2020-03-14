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

import icyllis.modernui.gui.font.FontRendererTools;
import icyllis.modernui.gui.font.IFontRenderer;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;

public abstract class ScrollEntry {

    protected TextureManager textureManager = Minecraft.getInstance().textureManager;

    protected IFontRenderer fontRenderer = FontRendererTools.CURRENT_RENDERER;

    public final int height;

    public ScrollEntry(int height) {
        this.height = height;
    }

    public abstract void draw(float centerX, float y, float maxY, float currentTime);

    /**
     * Called when mouseY move to this height range, whatever mouseX is
     * @param rcx Relative move to Center X (Range: -scrollWindowWidth / 2 <= rcx <= scrollWindowWidth / 2)
     * @param rty Relative move to Top Y (Range: 0 <= rty <= this.height)
     */
    public void mouseMoved(double rcx, double rty) {

    }

    /**
     * Same as above
     * @param rcx Relative move to Center X (Range: -scrollWindowWidth / 2 <= rcx <= scrollWindowWidth / 2)
     * @param rty Relative move to Top Y (Range: 0 <= rty <= this.height)
     */
    public boolean mouseClicked(double rcx, double rty, int mouseButton) {
        return false;
    }

    /**
     * Same as above
     * @param rcx Relative move to Center X (Range: -scrollWindowWidth / 2 <= rcx <= scrollWindowWidth / 2)
     * @param rty Relative move to Top Y (Range: 0 <= rty <= this.height)
     */
    public boolean mouseReleased(double rcx, double rty, int mouseButton) {
        return false;
    }

    /**
     * Same as above
     * @param rcx Relative move to Center X (Range: -scrollWindowWidth / 2 <= rcx <= scrollWindowWidth / 2)
     * @param rty Relative move to Top Y (Range: 0 <= rty <= this.height)
     */
    public boolean mouseDragged(double rcx, double rty, int mouseButton, double rmx, double rmy) {
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

    public static class Test extends ScrollEntry {

        private final int line;

        public Test(int line) {
            super(24);
            this.line = line;
        }

        @Override
        public void draw(float centerX, float y, float maxY, float currentTime) {
            fontRenderer.drawString("This is Line " + line, centerX, y + 9, 1, 1, 1, 1, 0.25f);
        }

        @Override
        public boolean mouseClicked(double rcx, double rty, int mouseButton) {
            ModernUI.LOGGER.info("Mouse click on line {}, RCX{} RTY{}", line, rcx, rty);
            return true;
        }
    }
}
