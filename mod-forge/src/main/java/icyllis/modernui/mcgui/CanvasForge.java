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

package icyllis.modernui.mcgui;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.GLCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * CanvasForge is an extension to Canvas, which provides more drawing
 * methods used in Minecraft.
 *
 * @author BloCamLimb
 */
//TODO core profile in 1.17
public class CanvasForge {

    private static final CanvasForge INSTANCE = new CanvasForge();

    private ItemRenderer mItemRenderer;

    private volatile GLCanvas mCanvas;

    private CanvasForge() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            mItemRenderer = minecraft.getItemRenderer();
        }
    }

    /**
     * Gets a CanvasForge from the given Canvas, if available.
     *
     * @return the instance
     * @throws IllegalArgumentException no capability found with the canvas
     */
    @Nonnull
    public static CanvasForge of(@Nonnull Canvas canvas) {
        if (INSTANCE.mCanvas == null) {
            synchronized (INSTANCE) {
                if (INSTANCE.mCanvas == null) {
                    INSTANCE.mCanvas = GLCanvas.getInstance();
                }
            }
        }
        if (canvas == INSTANCE.mCanvas) {
            return INSTANCE;
        }
        throw new IllegalArgumentException();
    }

    /**
     * Draw item default instance, without any NBT data
     * Size on screen: 16 * 16 * GuiScale
     *
     * @param item item
     * @param x    x pos
     * @param y    y pos
     */
    public void drawItem(@Nonnull Item item, float x, float y) {
        mItemRenderer.renderGuiItem(item.getDefaultInstance(), (int) (x), (int) (y));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * Draw item stack with NBT
     *
     * @param stack item stack to draw
     * @param x     x pos
     * @param y     y pos
     */
    public void drawItemStack(@Nonnull ItemStack stack, float x, float y) {
        mItemRenderer.renderGuiItem(stack, (int) (x), (int) (y));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * Draw item stack with NBT and their damage bar, amount etc
     *
     * @param stack item stack to draw
     * @param x     x pos
     * @param y     y pos
     */
    public void drawItemStackWithOverlays(@Nonnull ItemStack stack, float x, float y) {
        mItemRenderer.renderGuiItem(stack, (int) (x), (int) (y));
        mItemRenderer.renderGuiItemDecorations(Minecraft.getInstance().font, stack, (int) (x), (int) (y));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }
}
