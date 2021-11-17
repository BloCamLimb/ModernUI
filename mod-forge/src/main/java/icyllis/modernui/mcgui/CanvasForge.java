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

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.GLCanvas;
import icyllis.modernui.math.Matrix4;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.BufferUtils;

import javax.annotation.Nonnull;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11C.*;

/**
 * CanvasForge is an extension to Canvas, which provides more drawing
 * methods used in Minecraft.
 *
 * @author BloCamLimb
 * @deprecated If you want to draw item stacks in Modern UI,
 * schedule a task on vanilla rendering pipeline with a custom rendering
 * target, and draw the texture in Modern UI.
 */
@Deprecated
public class CanvasForge {

    private static final CanvasForge INSTANCE = new CanvasForge();

    private final FloatBuffer mMatBuf = BufferUtils.createFloatBuffer(16);
    private final Matrix4f mProjection = new Matrix4f();
    private final Matrix4f mModelView = new Matrix4f();
    private volatile GLCanvas mCanvas;

    private CanvasForge() {
    }

    /**
     * Get a CanvasForge from the given Canvas, if available.
     *
     * @return the instance
     * @throws IllegalArgumentException no capability found with the canvas
     */
    @Nonnull
    private static CanvasForge get(@Nonnull Canvas canvas) {
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
     * Draw an item stack. The dimension is 32 * 32 in scaling-independent pixels.
     * <p>
     * This method is used when you need an item as an icon.
     *
     * @param stack the item stack to draw
     * @param x     the x pos
     * @param y     the y pos
     */
    public void drawItemStack(@Nonnull ItemStack stack, float x, float y, int size) {
        Matrix4 mv = mCanvas.getMatrix().copy();

        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = renderer.getModel(stack, null, Minecraft.getInstance().player, 0);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        Matrix4f projection = RenderSystem.getProjectionMatrix();

        UIManager.sInstance.mProjectionMatrix.get(mMatBuf.rewind());
        mProjection.load(mMatBuf.rewind());
        RenderSystem.setProjectionMatrix(mProjection);

        PoseStack mvs = RenderSystem.getModelViewStack();
        mvs.pushPose();

        mv.get(mMatBuf.rewind());
        mModelView.load(mMatBuf.rewind());
        mvs.mulPoseMatrix(mModelView);

        mvs.translate(x + size / 2., y + size / 2., 100);
        mvs.scale(size, -size, size);
        RenderSystem.applyModelViewMatrix();

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        boolean light2D = !model.usesBlockLight();
        if (light2D) {
            Lighting.setupForFlatItems();
        }
        // give it an identity matrix
        PoseStack transformation = new PoseStack();
        renderer.render(stack, ItemTransforms.TransformType.GUI, false, transformation, bufferSource,
                0x00f000f0, OverlayTexture.NO_OVERLAY, model);
        bufferSource.endBatch();
        if (light2D) {
            Lighting.setupFor3DItems();
        }

        mvs.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(projection);

        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    }
}
