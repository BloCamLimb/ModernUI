/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.shader;

import icyllis.modernui.graphics.renderer.RenderCore;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.shader.IShaderManager;
import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.client.shader.ShaderLoader;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ShaderProgram implements IShaderManager {

    private int program;

    private ShaderLoader vertex;
    private ShaderLoader fragment;

    @Nonnull
    private final ResourceLocation vertLoc;
    @Nonnull
    private final ResourceLocation fragLoc;

    public ShaderProgram(@Nonnull ResourceLocation vertLoc, @Nonnull ResourceLocation fragLoc) {
        this.vertLoc = vertLoc;
        this.fragLoc = fragLoc;
    }

    public ShaderProgram(@Nonnull String vertLoc, @Nonnull String fragLoc) {
        this(new ResourceLocation(ModernUI.MODID, String.format("shaders/%s.vert", vertLoc)),
                new ResourceLocation(ModernUI.MODID, String.format("shaders/%s.frag", fragLoc)));
    }

    public void compile(IResourceManager manager) {
        if (vertex != null || fragment != null) {
            ShaderLinkHelper.deleteShader(this);
        }
        try {
            vertex = createShader(manager, vertLoc, ShaderLoader.ShaderType.VERTEX);
            fragment = createShader(manager, fragLoc, ShaderLoader.ShaderType.FRAGMENT);
            program = ShaderLinkHelper.createProgram();
            ShaderLinkHelper.linkProgram(this);
        } catch (IOException e) {
            ModernUI.LOGGER.fatal(RenderCore.MARKER, "An error occurred while compiling shaders", e);
        }
    }

    @Nonnull
    private ShaderLoader createShader(IResourceManager manager, @Nonnull ResourceLocation location, ShaderLoader.ShaderType type) throws IOException {
        try (InputStream stream = new BufferedInputStream(manager.getResource(location).getInputStream())) {
            return ShaderLoader.func_216534_a(type, location.toString(), stream, getClass().getSimpleName());
        }
    }

    @Override
    public int getProgram() {
        return program;
    }

    @Override
    public void markDirty() {

    }

    @Nonnull
    @Override
    public ShaderLoader getVertexShaderLoader() {
        return vertex;
    }

    @Nonnull
    @Override
    public ShaderLoader getFragmentShaderLoader() {
        return fragment;
    }

}
