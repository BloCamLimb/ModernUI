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

package icyllis.modernui.resources.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ModelRotation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.ModelLoader;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class ProjectBuilderModel extends BakedModelWrapper<IBakedModel> {

    public final IBakedModel main;
    public final IBakedModel cube;

    public ProjectBuilderModel(IBakedModel originalModel, ModelLoader loader) {
        super(originalModel);
        main = bakeCustomModel(loader, "item/project_builder_main");
        cube = bakeCustomModel(loader, "item/project_builder_cube");
    }

    private static IBakedModel bakeCustomModel(@Nonnull ModelLoader loader, String name) {
        ResourceLocation location = new ResourceLocation(ModernUI.MODID, name);
        return loader.getBakedModel(location, ModelRotation.X0_Y0, ModelLoader.defaultTextureGetter());
    }

    @Nonnull
    @Override
    public IBakedModel handlePerspective(@Nonnull ItemCameraTransforms.TransformType cameraTransformType, @Nonnull MatrixStack mat) {
        super.handlePerspective(cameraTransformType, mat);
        return this;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return true;
    }
}
