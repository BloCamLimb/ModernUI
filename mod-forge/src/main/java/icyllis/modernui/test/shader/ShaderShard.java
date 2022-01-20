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

package icyllis.modernui.test.shader;

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.core.ArchCore;
import icyllis.modernui.graphics.opengl.GLProgram;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import static org.lwjgl.opengl.GL43C.*;

/**
 * Represents OpenGL shader objects.
 */
@Deprecated
public final class ShaderShard {

    private static final Object2ObjectMap<ResourceLocation, ShaderShard> SHADERS = new Object2ObjectAVLTreeMap<>();

    private final ResourceLocation mLocation;
    private final int mId;
    private int mAttachCount = 0;
    private boolean mDeleted = false;

    private ShaderShard(ResourceLocation location, int id) {
        mLocation = location;
        mId = id;
    }

    public void attach(@Nonnull GLProgram program) {
        if (mAttachCount == Integer.MIN_VALUE) {
            throw new IllegalStateException(this + " has been deleted.");
        }
        ++mAttachCount;
        glAttachShader(program.get(), mId);
        if (mDeleted) {
            ModernUI.LOGGER.warn(ArchCore.MARKER,
                    "{} is marked as deleted, but the shader is still trying to attach to program {}",
                    this, program);
        }
    }

    public void detach(@Nonnull GLProgram program) {
        if (mAttachCount > 0) {
            --mAttachCount;
            glDetachShader(program.get(), mId);
            if (mAttachCount == 0 && mDeleted) {
                SHADERS.remove(mLocation);
                mAttachCount = Integer.MIN_VALUE;
            }
        } else {
            ModernUI.LOGGER.warn(ArchCore.MARKER,
                    "Try to detach {} from {}, but the shader is not attached to any program",
                    this, program);
        }
    }

    /**
     * @return The number of programs to which this shader is attached.
     */
    public int getAttachCount() {
        return mAttachCount;
    }

    /**
     * Mark this shader to be deleted, however deletion will be performed only when
     * this shader is not attached to any program.
     *
     * @see #getAttachCount()
     */
    public void delete() {
        if (!mDeleted) {
            glDeleteShader(mId);
            if (mAttachCount <= 0) {
                SHADERS.remove(mLocation);
                mAttachCount = Integer.MIN_VALUE;
            }
            mDeleted = true;
        }
    }

    @RenderThread
    public static ShaderShard getOrCreate(ResourceManager manager, ResourceLocation location, Type type) throws IOException {
        ShaderShard shader = SHADERS.get(location);
        if (shader != null) {
            return shader;
        }
        try (InputStream stream = manager.getResource(location).getInputStream()) {
            String src = ArchCore.readStringUTF8(stream);
            if (src != null) {
                int id = glCreateShader(type.type);
                glShaderSource(id, src);
                glCompileShader(id);
                if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_TRUE) {
                    SHADERS.put(location, shader = new ShaderShard(location, id));
                } else {
                    String y = glGetShaderInfoLog(id, 32768).trim();
                    throw new IOException("Failed to compile " + type.getName() + " shader (" + location + ") : " + y);
                }
            } else {
                throw new IOException("Failed to read shader source (" + location + ")");
            }
        }
        return shader;
    }

    public static void deleteAll() {
        SHADERS.values().forEach(ShaderShard::delete);
        if (!SHADERS.isEmpty()) {
            throw new IllegalStateException("There are still " + SHADERS.size() + " shaders attaching to some programs.");
        }
    }

    public enum Type {
        // OpenGL 2.0
        VERTEX(GL_VERTEX_SHADER),

        // OpenGL 2.0
        FRAGMENT(GL_FRAGMENT_SHADER),

        // OpenGL 3.2
        GEOMETRY(GL_GEOMETRY_SHADER),

        // OpenGL 4.0
        TESS_CONTROL(GL_TESS_CONTROL_SHADER),

        // OpenGL 4.0
        TESS_EVALUATION(GL_TESS_EVALUATION_SHADER),

        // OpenGL 4.3
        COMPUTE(GL_COMPUTE_SHADER);

        private final int type;

        Type(int type) {
            this.type = type;
        }

        @Nonnull
        private String getName() {
            return name().toLowerCase(Locale.ROOT).replace('_', ' ');
        }
    }
}
