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

package icyllis.modernui.graphics.item;

import com.mojang.blaze3d.matrix.MatrixStack;
import icyllis.modernui.resources.model.ProjectBuilderModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class ProjectBuilderRenderer extends ItemStackTileEntityRenderer {

    @Override
    public void func_239207_a_(@Nonnull ItemStack stack, @Nonnull ItemCameraTransforms.TransformType transformType,
                               @Nonnull MatrixStack matrixStack, @Nonnull IRenderTypeBuffer buffer, int combinedLight, int combinedOverlay) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        ProjectBuilderModel model = (ProjectBuilderModel) itemRenderer
                .getItemModelWithOverrides(stack, null, null);
        IBakedModel main = model.main;
        IBakedModel cube = model.cube;

        matrixStack.push();
        matrixStack.translate(0.5, 0.5, 0.5);

        itemRenderer.renderItem(stack, transformType, true, matrixStack, buffer, combinedLight, combinedOverlay, main);

        long time = Util.milliTime();
        float angel = time * -0.08f;
        angel %= 360;
        matrixStack.translate(0, 0, -0.671875f);
        matrixStack.rotate(Vector3f.YN.rotationDegrees(angel));

        int glow = (int) (Math.sin(time / 200D) * 120 + 120);
        int glowX = Math.min(glow + combinedLight >> 16, 240);
        int glowY = Math.min(glow + combinedLight & 0xffff, 240);
        itemRenderer.renderItem(stack, transformType, true, matrixStack, buffer, glowX << 16 | glowY, combinedOverlay, cube);

        matrixStack.pop();
    }
}
