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
import icyllis.modernui.gui.math.Color3i;
import icyllis.modernui.gui.math.Locator;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;

//TODO Experimental
public enum LocationEditor {
    INSTANCE;

    @Nullable
    private Widget hoveredWidget;

    private final Canvas canvas = new Canvas();

    private boolean working = false;

    private boolean dragging = false;

    private double accDragX = 0;
    private double accDragY = 0;

    public void setHoveredWidget(@Nullable Object obj) {
        if (working && !dragging) {
            if (obj == null) {
                hoveredWidget = null;
            } else if (obj instanceof Widget) {
                hoveredWidget = (Widget) obj;
                MouseTools.useDefaultCursor();
            }
        }
    }

    public void iterateWorking() {
        working = !working;
        if (!working) {
            hoveredWidget = null;
        }
    }

    public void draw() {
        if (!working) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableDepthTest();

        canvas.setRGBA(0.5f, 0.5f, 0.5f, 0.25f);
        if (hoveredWidget != null) {
            canvas.drawRoundedRect(1, 1, 80, 32, 4);
        } else {
            canvas.drawRoundedRect(1, 1, 80, 14, 4);
        }
        canvas.setColor(Color3i.BLUE_C, 1);
        canvas.drawText(TextFormatting.GOLD + "Gui Editing Mode: ON", 4, 3);
        if (hoveredWidget != null) {
            Locator l = hoveredWidget.getLocator();
            if (l != null) {
                canvas.drawText("X Offset: " + l.getXOffset(), 4, 12);
                canvas.drawText("Y Offset: " + l.getYOffset(), 4, 21);
            }
            canvas.setLineAntiAliasing(true);
            canvas.setLineWidth(2.0f);
            canvas.drawRectLines(hoveredWidget.getLeft() - 1, hoveredWidget.getTop() - 1, hoveredWidget.getRight() + 1, hoveredWidget.getBottom() + 1);
            canvas.setLineAntiAliasing(false);
        }

        Canvas.setLineAA0(false);
        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public boolean mouseClicked(int button) {
        if (working && button == 0) {
            if (hoveredWidget != null) {
                dragging = true;
                return true;
            }
        }
        return false;
    }

    public void mouseReleased() {
        dragging = false;
    }

    public boolean mouseDragged(double dx, double dy) {
        if (dragging && hoveredWidget != null) {
            accDragX += dx;
            accDragY += dy;
            int x = (int) accDragX;
            int y = (int) accDragY;
            Locator l = hoveredWidget.getLocator();
            if (l != null && (x != 0 || y != 0)) {
                l.translateXOffset(x);
                l.translateYOffset(y);
                hoveredWidget.relocate();
                accDragX -= x;
                accDragY -= y;
                return true;
            }
        }
        return false;
    }
}
