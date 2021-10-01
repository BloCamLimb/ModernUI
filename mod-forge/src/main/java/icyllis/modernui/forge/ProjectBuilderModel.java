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
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.ModelLoader;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;

@ApiStatus.Experimental
public class ProjectBuilderModel extends BakedModelWrapper<BakedModel> {

    public final BakedModel main;
    public final BakedModel cube;

    public ProjectBuilderModel(BakedModel originalModel, ModelLoader loader) {
        super(originalModel);
        main = bakeCustomModel(loader, "item/project_builder_main");
        cube = bakeCustomModel(loader, "item/project_builder_cube");
    }

    private static BakedModel bakeCustomModel(@Nonnull ModelLoader loader, String name) {
        ResourceLocation location = new ResourceLocation(ModernUI.ID, name);
        return loader.bake(location, BlockModelRotation.X0_Y0, ModelLoader.defaultTextureGetter());
    }

    @Nonnull
    @Override
    public BakedModel handlePerspective(@Nonnull ItemTransforms.TransformType transformType, @Nonnull PoseStack ps) {
        super.handlePerspective(transformType, ps);
        return this;
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }
}
