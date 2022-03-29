/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.forge.mixin;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.opengl.GLSurfaceCanvas;
import icyllis.modernui.opengl.GLCore;
import icyllis.modernui.forge.CanvasForge;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nonnull;
import java.util.List;

@Mixin(ShaderInstance.class)
public class MixinShaderInstance implements CanvasForge.FastShader {

    @Shadow
    private boolean dirty;

    @Shadow
    @Final
    private int programId;

    @Shadow
    @Final
    private List<Integer> samplerLocations;

    @Shadow
    @Final
    private List<String> samplerNames;

    @Shadow
    @Final
    private List<Uniform> uniforms;

    @Override
    public void fastApply(@Nonnull GLSurfaceCanvas canvas, @Nonnull Object2IntMap<String> units) {
        dirty = false;
        canvas.useProgram(programId);

        for (int i = 0; i < samplerLocations.size(); ++i) {
            int unit = units.getInt(samplerNames.get(i));
            if (unit != -1) {
                int location = samplerLocations.get(i);
                GLCore.glUniform1i(location, unit);
                int texture = RenderSystem.getShaderTexture(unit);
                if (unit == 0) {
                    canvas.bindTexture(texture);
                } else {
                    GLCore.glBindTextureUnit(unit, texture);
                }
            }
        }

        for (Uniform uniform : uniforms) {
            uniform.upload();
        }
    }
}
