/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.view;

import icyllis.modernui.core.forge.ModernUIForge;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.math.Color3i;
import icyllis.modernui.graphics.math.TextAlign;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Deprecated
@OnlyIn(Dist.CLIENT)
public enum UIEditor {
    INSTANCE;

    @Nullable
    private View hoveredView;

    private boolean working = false;

    private boolean dragging = false;

    private double accDragX = 0;
    private double accDragY = 0;

    private final List<String> treeInfo = new ArrayList<>();

    private int bottom = 14;

    private int[] hoveredLocation = new int[2];

    public void setHoveredWidget(@Nullable Object obj) {
        if (!working) {
            return;
        }
        if (obj == null) {
            hoveredView = null;
            treeInfo.clear();
            bottom = 14;
        } else if (obj instanceof View) {
            hoveredView = (View) obj;
            UITools.useDefaultCursor();

            treeInfo.clear();
            List<String> temp = new ArrayList<>();
            ViewParent parent = hoveredView.getParent();
            if (parent != null) {
                temp.add(hoveredView.getClass().getSimpleName());
                do {
                    temp.add(0, parent.getClass().getSimpleName());
                    parent = parent.getParent();
                } while (parent != null);
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
                    }
                    treeInfo.add(builder.toString());
                }
            }
            bottom = 14 + temp.size() * 9;
        }
    }

    private void iterateWorking() {
        working = !working;
    }

    void draw(@Nonnull Canvas canvas) {
        if (!working) {
            return;
        }
        canvas.setColor(128, 128, 128, 64);
        canvas.drawRoundedRect(1, 1, 120, bottom, 4);
        canvas.setTextAlign(TextAlign.LEFT);

        canvas.setColor(Color3i.BLUE_C, 1);
        canvas.drawText(ChatFormatting.GOLD + "Gui Editing Mode: ON", 4, 3);
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
            canvas.drawRoundedFrame(
                    hoveredLocation[0] - 1,
                    hoveredLocation[1] - 1,
                    hoveredLocation[0] + hoveredView.getWidth() + 1,
                    hoveredLocation[1] + hoveredView.getHeight() + 1,
                    2
            );
        }
    }

    @SubscribeEvent
    void gMouseClicked(@Nonnull GuiScreenEvent.MouseClickedEvent.Pre event) {
        int button = event.getButton();
        if (working && button == 0) {
            if (hoveredView != null) {
                dragging = true;
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    void gMouseReleased(@Nonnull GuiScreenEvent.MouseReleasedEvent.Pre event) {
        dragging = false;
    }

    @SubscribeEvent
    void gMouseDragged(@Nonnull GuiScreenEvent.MouseDragEvent.Pre event) {
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
    }

    @SubscribeEvent
    void gKeyInput(@Nonnull InputEvent.KeyInputEvent event) {
        if (!ModernUIForge.isDeveloperMode() || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        if (!Screen.hasControlDown()) {
            return;
        }
        if (event.getKey() == GLFW.GLFW_KEY_T) {
            if (UIManager.getInstance().hasOpenGUI()) {
                iterateWorking();
            }
        }
    }
}
