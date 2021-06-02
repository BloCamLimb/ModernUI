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
import icyllis.modernui.platform.RenderCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL43C.*;

/**
 * A shader program object with a vertex shader and a fragment shader attached.
 */
public class ShaderProgram {

    private static final List<ShaderProgram> PROGRAMS = new ArrayList<>();

    private Shader mVertex;
    private Shader mFragment;

    @Nonnull
    private final ResourceLocation mVert;
    @Nonnull
    private final ResourceLocation mFrag;

    protected final int mId;

    @RenderThread
    public ShaderProgram(@Nonnull ResourceLocation vert, @Nonnull ResourceLocation frag) {
        mVert = vert;
        mFrag = frag;
        mId = glCreateProgram();
        PROGRAMS.add(this);
    }

    public void link(ResourceManager manager) throws IOException {
        detach();
        mVertex = Shader.getOrCreate(manager, mVert, Shader.Type.VERTEX);
        mFragment = Shader.getOrCreate(manager, mFrag, Shader.Type.FRAGMENT);
        mVertex.attach(this);
        mFragment.attach(this);
        glLinkProgram(mId);
        if (glGetProgrami(mId, GL_LINK_STATUS) == GL_FALSE) {
            ModernUI.LOGGER.error(RenderCore.MARKER, "Failed to link shader program {}, detailed info:\n{}", 
                    this, glGetProgramInfoLog(mId, 8192));
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

    /**
     * Use this shader program
     */
    public void use() {
        glUseProgram(mId);
    }

    /**
     * Use undefined shader program.
     */
    public static void stop() {
        glUseProgram(0);
    }

    public static void linkAll(ResourceManager manager) {
        PROGRAMS.forEach(program -> {
            try {
                program.link(manager);
            } catch (IOException e) {
                ModernUI.LOGGER.error(RenderCore.MARKER, "An error occurred while linking program {}\n", program, e);
            }
        });
        ModernUI.LOGGER.debug(RenderCore.MARKER, "Shader programs linked");
    }

    public static void detachAll() {
        PROGRAMS.forEach(ShaderProgram::detach);
    }

    public static void recompile(ResourceManager manager) {
        detachAll();
        Shader.deleteAll();
        linkAll(manager);
    }
}
