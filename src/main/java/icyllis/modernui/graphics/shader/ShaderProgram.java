/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.graphics.shader;

import icyllis.modernui.system.ModernUI;
import net.minecraft.client.shader.IShaderManager;
import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.client.shader.ShaderLoader;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ShaderProgram implements IShaderManager {

    public static final Marker MARKER = MarkerManager.getMarker("SHADER");

    protected static int compileCount = 0;

    private int program;

    private ShaderLoader vertex;

    private ShaderLoader fragment;

    private final String vert;

    private final String frag;

    public ShaderProgram(String vert, String frag) {
        this.vert = vert;
        this.frag = frag;
    }

    protected void compile(IResourceManager manager) {
        if (vertex != null || fragment != null) {
            ShaderLinkHelper.deleteShader(this);
        }
        try {
            this.vertex = createShader(manager, new ResourceLocation(ModernUI.MODID, String.format("shaders/%s.%s", vert, "vert")), ShaderLoader.ShaderType.VERTEX);
            this.fragment = createShader(manager, new ResourceLocation(ModernUI.MODID, String.format("shaders/%s.%s", frag, "frag")), ShaderLoader.ShaderType.FRAGMENT);
            this.program = ShaderLinkHelper.createProgram();
            ShaderLinkHelper.linkProgram(this);
            ++compileCount;
        } catch (IOException e) {
            ModernUI.LOGGER.fatal(MARKER, "Can't create program {}, please report this issue", getClass().getSimpleName());
            e.printStackTrace();
        }
    }

    @Nonnull
    private static ShaderLoader createShader(IResourceManager manager, @Nonnull ResourceLocation location, ShaderLoader.ShaderType type) throws IOException {
        try (InputStream is = new BufferedInputStream(manager.getResource(location).getInputStream())) {
            return ShaderLoader.func_216534_a(type, location.toString(), is);
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
