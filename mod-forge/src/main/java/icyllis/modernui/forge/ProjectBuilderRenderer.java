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

package icyllis.modernui.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class ProjectBuilderRenderer extends BlockEntityWithoutLevelRenderer {

    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemTransforms.TransformType transformType,
                               @Nonnull PoseStack matrixStack, @Nonnull MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        ProjectBuilderModel model = (ProjectBuilderModel) itemRenderer.getModel(stack, null, null);

        matrixStack.pushPose();
        matrixStack.translate(0.5, 0.5, 0.5);

        itemRenderer.render(stack, transformType, true, matrixStack, buffer, combinedLight, combinedOverlay, model.main);

        long time = Util.getMillis();
        float angel = time * -0.08f;
        angel %= 360;
        matrixStack.translate(0, 0, -0.671875f);
        matrixStack.mulPose(Vector3f.YN.rotationDegrees(angel));

        float f = ((float) Math.sin(time / 200D) + 1) * 0.5f;
        int glowX = (int) Mth.lerp(f, combinedLight >> 16, 240);
        int glowY = (int) Mth.lerp(f, combinedLight & 0xffff, 240);
        itemRenderer.render(stack, transformType, true, matrixStack, buffer, glowX << 16 | glowY, combinedOverlay, model.cube);

        matrixStack.popPose();
    }
}
