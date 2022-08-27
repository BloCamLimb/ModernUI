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
import icyllis.modernui.ModernUI;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.BakedModelWrapper;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;

@ApiStatus.Experimental
final class ProjectBuilderModel extends BakedModelWrapper<BakedModel> {

    public final BakedModel main;
    public final BakedModel cube;

    ProjectBuilderModel(BakedModel originalModel, ModelBakery bakery) {
        super(originalModel);
        main = bakeCustomModel(bakery, "item/project_builder_main");
        cube = bakeCustomModel(bakery, "item/project_builder_cube");
    }

    private static BakedModel bakeCustomModel(@Nonnull ModelBakery bakery, String name) {
        ResourceLocation location = new ResourceLocation(ModernUI.ID, name);
        return bakery.bake(location, BlockModelRotation.X0_Y0, Material::sprite);
    }

    @Nonnull
    @Override
    public BakedModel applyTransform(@Nonnull ItemTransforms.TransformType transformType,
                                     @Nonnull PoseStack poseStack,
                                     boolean applyLeftHandTransform) {
        super.applyTransform(transformType, poseStack, applyLeftHandTransform);
        return this;
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }
}
