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

import com.mojang.blaze3d.shaders.Effect;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.ProgramManager;
import icyllis.modernui.ModernUI;
import icyllis.modernui.graphics.RenderCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ShaderProgram implements Effect {

    private int program;

    private Program vertex;
    private Program fragment;

    @Nonnull
    private final ResourceLocation vert;
    @Nonnull
    private final ResourceLocation frag;

    public ShaderProgram(@Nonnull ResourceLocation vert, @Nonnull ResourceLocation frag) {
        this.vert = vert;
        this.frag = frag;
    }

    public ShaderProgram(@Nonnull String namespace, @Nonnull String vert, @Nonnull String frag) {
        this(new ResourceLocation(namespace, String.format("shaders/%s.vert", vert)),
                new ResourceLocation(namespace, String.format("shaders/%s.frag", frag)));
    }

    public void compile(ResourceManager manager) {
        if (vertex != null || fragment != null) {
            ProgramManager.releaseProgram(this);
        }
        try {
            vertex = createShader(manager, vert, Program.Type.VERTEX);
            fragment = createShader(manager, frag, Program.Type.FRAGMENT);
            program = ProgramManager.createProgram();
            ProgramManager.linkProgram(this);
        } catch (IOException e) {
            ModernUI.LOGGER.fatal(RenderCore.MARKER, "An error occurred while compiling shader: {}", this, e);
        }
    }

    @Nonnull
    private Program createShader(ResourceManager manager, @Nonnull ResourceLocation location, Program.Type type) throws IOException {
        try (InputStream stream = new BufferedInputStream(manager.getResource(location).getInputStream())) {
            return Program.compileShader(type, location.toString(), stream, getClass().getSimpleName());
        }
    }

    @Override
    public int getId() {
        return program;
    }

    @Override
    public void markDirty() {

    }

    @Nonnull
    @Override
    public Program getVertexProgram() {
        return vertex;
    }

    @Nonnull
    @Override
    public Program getFragmentProgram() {
        return fragment;
    }

}
