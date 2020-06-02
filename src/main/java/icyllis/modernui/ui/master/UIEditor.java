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

package icyllis.modernui.ui.master;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.graphics.math.Color3i;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.ui.test.Locator;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;

//TODO Experimental, all changes won't be saved
public enum UIEditor {
    INSTANCE;

    @Nullable
    private View hoveredView;

    private final Canvas canvas = new Canvas();

    private boolean working = false;

    private boolean dragging = false;

    private double accDragX = 0;
    private double accDragY = 0;

    public void setHoveredWidget(@Nullable Object obj) {
        if (true/*!dragging*/) {
            if (obj == null) {
                hoveredView = null;
            } else if (obj instanceof View) {
                hoveredView = (View) obj;
                UITools.useDefaultCursor();
            }
        }
    }

    public void iterateWorking() {
        working = !working;
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
        if (hoveredView != null) {
            canvas.drawRoundedRect(1, 1, 80, 68, 4);
        } else {
            canvas.drawRoundedRect(1, 1, 80, 14, 4);
        }
        canvas.setColor(Color3i.BLUE_C, 1);
        canvas.drawText(TextFormatting.GOLD + "Gui Editing Mode: ON", 4, 3);
        if (hoveredView != null) {
            /*Locator l = hoveredView.getLocator();
            if (l != null) {
                canvas.drawText("X Offset: " + l.getXOffset(), 4, 12);
                canvas.drawText("Y Offset: " + l.getYOffset(), 4, 21);
            }*/
            canvas.drawText(hoveredView.getClass().getSimpleName(), 4, 12);

            IViewParent parent = hoveredView.getParent();
            canvas.drawText(parent.getClass().getSimpleName() + '\u2191', 4, 21);

            float ty = 30;
            while (parent != UIManager.INSTANCE) {
                parent = parent.getParent();
                canvas.drawText(parent.getClass().getSimpleName() + '\u2191', 4, ty);
                ty += 9;
            }

            canvas.drawRoundedRectFrame(
                    hoveredView.getAbsoluteLeft() - 1,
                    hoveredView.getAbsoluteTop() - 1,
                    hoveredView.getAbsoluteRight() + 1,
                    hoveredView.getAbsoluteBottom() + 1,
                    2
            );
        }

        Canvas.setLineAA0(false);
        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public boolean mouseClicked(int button) {
        if (working && button == 0) {
            if (hoveredView != null) {
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
        if (dragging && hoveredView != null) {
            /*accDragX += dx;
            accDragY += dy;
            int x = (int) accDragX;
            int y = (int) accDragY;
            Locator l = hoveredView.getLocator();
            if (l != null && (x != 0 || y != 0)) {
                l.translateXOffset(x);
                l.translateYOffset(y);
                hoveredView.relocate();
                accDragX -= x;
                accDragY -= y;
                UITools.useDefaultCursor();
                return true;
            }*/
        }
        return false;
    }
}
