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
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import static org.lwjgl.opengl.GL43C.*;

/**
 * Represents an OpenGL shader object.
 */
//TODO Independent API without Minecraft
public final class Shader {

    private static final Object2ObjectMap<ResourceLocation, Shader> SHADERS = new Object2ObjectAVLTreeMap<>();

    private final ResourceLocation mLocation;
    private final int mId;
    private int mAttachCount = 0;
    private boolean mDeleted = false;

    private Shader(ResourceLocation location, int id) {
        mLocation = location;
        mId = id;
    }

    public void attach(@Nonnull ShaderProgram program) {
        if (mAttachCount == Integer.MIN_VALUE) {
            throw new IllegalStateException(this + " has been deleted.");
        }
        ++mAttachCount;
        glAttachShader(program.mId, mId);
        if (mDeleted) {
            ModernUI.LOGGER.warn(RenderCore.MARKER,
                    "{} is marked as deleted, but the shader is still trying to attach to program {}",
                    this, program);
        }
    }

    public void detach(@Nonnull ShaderProgram program) {
        if (mAttachCount > 0) {
            --mAttachCount;
            glDetachShader(program.mId, mId);
            if (mAttachCount == 0 && mDeleted) {
                SHADERS.remove(mLocation);
                mAttachCount = Integer.MIN_VALUE;
            }
        } else {
            ModernUI.LOGGER.warn(RenderCore.MARKER,
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
    public static Shader getOrCreate(ResourceManager manager, ResourceLocation location, Type type) throws IOException {
        Shader shader = SHADERS.get(location);
        if (shader != null) {
            return shader;
        }
        String path = manager == null ? System.getenv().getOrDefault("MOD_ASSETS", "")
                .replace('\\', '/') + location.getNamespace() + '/' + location.getPath() : "";
        try (InputStream stream = new BufferedInputStream(manager == null ?
                new FileInputStream(path) : manager.getResource(location).getInputStream())) {
            String src = RenderCore.readStringASCII(stream);
            if (src != null) {
                int id = glCreateShader(type.mGlType);
                glShaderSource(id, src);
                glCompileShader(id);
                if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_TRUE) {
                    SHADERS.put(location, shader = new Shader(location, id));
                } else {
                    String y = StringUtils.trim(glGetShaderInfoLog(id, 32768));
                    throw new IOException("Failed to compile " + type.getName() + " shader (" + location + ") : " + y);
                }
            } else {
                throw new IOException("Failed to read shader source (" + location + ")");
            }
        }
        return shader;
    }

    public static void deleteAll() {
        SHADERS.values().forEach(Shader::delete);
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

        private final int mGlType;

        Type(@NativeType("GLenum") int glType) {
            mGlType = glType;
        }

        @Nonnull
        private String getName() {
            return name().toLowerCase(Locale.ROOT).replace('_', ' ');
        }
    }
}
