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

package icyllis.modernui.ui.master;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.graphics.math.Color3i;
import icyllis.modernui.graphics.renderer.Canvas;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

//TODO Really Experimental, all changes won't be saved
public enum UIEditor {
    INSTANCE;

    @Nullable
    private View hoveredView;

    private final Canvas canvas = new Canvas();

    private boolean working = false;

    private boolean dragging = false;

    private double accDragX = 0;
    private double accDragY = 0;

    private final List<String> treeInfo = new ArrayList<>();

    private int bottom = 14;

    private int[] hoveredLocation = new int[2];

    public void setHoveredWidget(@Nullable Object obj) {
        if (obj == null) {
            hoveredView = null;
            treeInfo.clear();
            bottom = 14;
        } else if (obj instanceof View) {
            hoveredView = (View) obj;
            UITools.useDefaultCursor();

            treeInfo.clear();
            List<String> temp = new ArrayList<>();
            IViewParent parent = hoveredView.getParent();
            temp.add(hoveredView.getClass().getSimpleName());
            temp.add(0, parent.getClass().getSimpleName());
            while (parent != UIManager.INSTANCE) {
                parent = parent.getParent();
                temp.add(0, parent.getClass().getSimpleName());
            }
            for (int i = 0; i < temp.size(); i++) {
                StringBuilder builder = new StringBuilder();
                if (i != 0) {
                    for (int j = 0; j < i; j++) {
                        builder.append("   ");
                    }
                }
                builder.append(temp.get(i));
                if (i == 0) {
                    builder.append(" (System)");
                } else if (i == 1) {
                    builder.append(" (Root)");
                }
                treeInfo.add(builder.toString());
            }
            bottom = 14 + temp.size() * 9;
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
        canvas.drawRoundedRect(1, 1, 120, bottom, 4);

        canvas.setColor(Color3i.BLUE_C, 1);
        canvas.drawText(TextFormatting.GOLD + "Gui Editing Mode: ON", 4, 3);
        if (hoveredView != null) {
            /*Locator l = hoveredView.getLocator();
            if (l != null) {
                canvas.drawText("X Offset: " + l.getXOffset(), 4, 12);
                canvas.drawText("Y Offset: " + l.getYOffset(), 4, 21);
            }*/
            /*canvas.drawText(hoveredView.getClass().getSimpleName(), 4, 12);

            IViewParent parent = hoveredView.getParent();
            canvas.drawText(parent.getClass().getSimpleName() + '\u2191', 4, 21);

            float ty = 30;
            while (parent != UIManager.INSTANCE) {
                parent = parent.getParent();
                canvas.drawText(parent.getClass().getSimpleName() + '\u2191', 4, ty);
                ty += 9;
            }*/
            float ty = 12;
            for (String str : treeInfo) {
                canvas.drawText(str, 4, ty);
                ty += 9;
            }

            hoveredView.getLocationInWindow(hoveredLocation);
            canvas.drawRoundedRectFrame(
                    hoveredLocation[0] - 1,
                    hoveredLocation[1] - 1,
                    hoveredLocation[0] + hoveredView.getWidth() + 1,
                    hoveredLocation[1] + hoveredView.getHeight() + 1,
                    2
            );
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
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
