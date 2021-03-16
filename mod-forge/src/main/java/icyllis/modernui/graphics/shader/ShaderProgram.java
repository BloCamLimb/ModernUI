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

package icyllis.modernui.graphics.shader;

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.graphics.RenderCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL43;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A shader program object with a vertex shader and a fragment shader attached.
 */
public class ShaderProgram {

    private static final List<ShaderProgram> PROGRAMS = new ArrayList<>();

    private Shader mVertex;
    private Shader mFragment;

    @Nonnull
    private final ResourceLocation mVertLoc;
    @Nonnull
    private final ResourceLocation mFragLoc;

    final int mId;

    @RenderThread
    public ShaderProgram(@Nonnull ResourceLocation vertLoc, @Nonnull ResourceLocation fragLoc) {
        mVertLoc = vertLoc;
        mFragLoc = fragLoc;
        mId = GL43.glCreateProgram();
        PROGRAMS.add(this);
    }

    @RenderThread
    public ShaderProgram(@Nonnull String namespace, @Nonnull String vertLoc, @Nonnull String fragLoc) {
        this(new ResourceLocation(namespace, String.format("shaders/%s.vert", vertLoc)),
                new ResourceLocation(namespace, String.format("shaders/%s.frag", fragLoc)));
    }

    public void link(ResourceManager manager) {
        detach();
        try {
            mVertex = Shader.getOrCreate(manager, mVertLoc, Shader.Type.VERTEX);
            mFragment = Shader.getOrCreate(manager, mFragLoc, Shader.Type.FRAGMENT);
            mVertex.attach(this);
            mFragment.attach(this);
            GL43.glLinkProgram(mId);
        } catch (IOException e) {
            ModernUI.LOGGER.error(RenderCore.MARKER, "An error occurred while compiling shader: {}", this, e);
        }
    }

    public void detach() {
        if (mVertex != null) {
            mVertex.detach(this);
            mVertex = null;
        }
        if (mFragment != null) {
            mFragment.detach(this);
            mFragment = null;
        }
    }

    public void use() {
        GL43.glUseProgram(mId);
    }

    public static void linkAll(ResourceManager manager) {
        PROGRAMS.forEach(program -> program.link(manager));
    }

    public static void detachAll() {
        PROGRAMS.forEach(ShaderProgram::detach);
    }
}
