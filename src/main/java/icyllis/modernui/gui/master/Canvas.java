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

package icyllis.modernui.gui.master;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.master.DrawTools;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class Canvas extends DrawTools {

    private final MainWindow mainWindow = Minecraft.getInstance().getMainWindow();

    public void save() {
        RenderSystem.pushMatrix();
    }

    public void restore() {
        RenderSystem.popMatrix();
    }

    public void translate(float dx, float dy) {
        RenderSystem.translatef(dx, dy, 0.0f);
    }

    public void scale(float sx, float sy) {
        RenderSystem.scalef(sx, sy, 1.0f);
    }

    /**
     * Scale the canvas and translate to pos
     *
     * @param sx x scale
     * @param sy y scale
     * @param px pivot x pos
     * @param py pivot y pos
     */
    public void scale(float sx, float sy, float px, float py) {
        RenderSystem.scalef(sx, sy, 1.0f);
        float kx;
        float ky;
        if (sx < 1) {
            kx = 1.0f / sx - 1.0f;
        } else {
            kx = sx - 1.0f;
        }
        kx *= px;
        if (sy < 1) {
            ky = 1.0f / sy - 1.0f;
        } else {
            ky = sy - 1.0f;
        }
        ky *= py;
        RenderSystem.translatef(kx, ky, 0.0f);
    }

    public void clipStart(float x, float y, float width, float height) {
        double scale = mainWindow.getGuiScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * scale), (int) (mainWindow.getFramebufferHeight() - ((y + height) * scale)),
                (int) (width * scale), (int) (height * scale));
    }

    public void clipEnd() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
