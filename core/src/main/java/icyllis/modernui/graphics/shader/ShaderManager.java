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
import icyllis.modernui.core.Context;
import icyllis.modernui.platform.RenderCore;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * This class helps you create shaders and programs.
 */
public class ShaderManager {

    private static final ShaderManager instance = new ShaderManager();

    private final Set<Listener> mListeners = new HashSet<>();

    private final Map<Context, Object2IntMap<Path>> mShaders = new HashMap<>();

    public static ShaderManager getInstance() {
        return instance;
    }

    /**
     * Registers a listener to obtain shaders.
     *
     * @param listener the listener
     */
    public void addListener(@Nonnull Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(@Nonnull Listener listener) {
        mListeners.remove(listener);
    }

    // internal use
    public void reload() {
        RenderCore.checkRenderThread();
        for (var map : mShaders.values()) {
            for (int shard : map.values()) {
                glDeleteShader(shard);
            }
        }
        mShaders.clear();
        for (Listener l : mListeners) {
            l.onReload(this);
        }
        for (var map : mShaders.values()) {
            for (int shard : map.values()) {
                glDeleteShader(shard);
            }
        }
        mShaders.clear();
    }

    /**
     * Get or create a shader shard, call this on listener callback.
     *
     * @param context  the application context
     * @param subPaths sub paths to the shader source, parent is 'shaders'
     * @return the shader shard handle or 0 on failure
     * @see #getShard(Context, Path, int)
     */
    public int getShard(@Nonnull Context context, @Nonnull String... subPaths) {
        return getShard(context, Path.of("shaders", subPaths), 0);
    }

    /**
     * Get or create a shader shard, call this on listener callback.
     * <p>
     * Standard file extension:
     * <table border="1">
     *   <tr>
     *     <td>.vert</th>
     *     <td>vertex shader</th>
     *   </tr>
     *   <tr>
     *     <td>.tesc</th>
     *     <td>tessellation control shader</th>
     *   </tr>
     *   <tr>
     *     <td>.tese</th>
     *     <td>tessellation evaluation shader</th>
     *   </tr>
     *   <tr>
     *     <td>.geom</th>
     *     <td>geometry shader</th>
     *   </tr>
     *   <tr>
     *     <td>.frag</th>
     *     <td>fragment shader</th>
     *   </tr>
     *   <tr>
     *     <td>.comp</th>
     *     <td>compute shader</th>
     *   </tr>
     * </table>
     *
     * @param context the application context
     * @param path    the path of shader source
     * @param type    the shader type to create, can be 0 for standard file extension
     * @return the shader shard handle or 0 on failure
     */
    public int getShard(@Nonnull Context context, @Nonnull Path path, int type) {
        RenderCore.checkRenderThread();
        int shader = mShaders.computeIfAbsent(context, c -> new Object2IntOpenHashMap<>()).getInt(path);
        if (shader != 0) {
            return shader;
        }
        try (ReadableByteChannel channel = context.getResource(path)) {
            String source = RenderCore.readStringUTF8(channel);
            if (source == null) {
                ModernUI.LOGGER.error(MARKER, "Failed to read shader source: {}", path);
                return 0;
            }
            if (type == 0) {
                String s = path.toString();
                if (s.endsWith(".vert")) {
                    type = GL_VERTEX_SHADER;
                } else if (s.endsWith(".frag")) {
                    type = GL_FRAGMENT_SHADER;
                } else if (s.endsWith(".geom")) {
                    type = GL_GEOMETRY_SHADER;
                } else if (s.endsWith(".tesc")) {
                    type = GL_TESS_CONTROL_SHADER;
                } else if (s.endsWith(".tese")) {
                    type = GL_TESS_EVALUATION_SHADER;
                } else if (s.endsWith(".comp")) {
                    type = GL_COMPUTE_SHADER;
                } else {
                    return 0;
                }
            }
            shader = glCreateShader(type);
            glShaderSource(shader, source);
            glCompileShader(shader);
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                String log = glGetShaderInfoLog(shader, 8192).trim();
                ModernUI.LOGGER.error(MARKER, "Failed to compile shader {}:\n{}", path, log);
                glDeleteShader(shader);
                return 0;
            }
            return shader;
        } catch (IOException e) {
            ModernUI.LOGGER.error(MARKER, "Failed to get shader source {}\n", path, e);
        }
        return 0;
    }

    /**
     * Create a shader object representing a shader program.
     * If fails, program will be 0.
     *
     * @param shader the existing shader object
     * @param shards shader shards for the shader
     * @param <T>    custom shader subclasses
     * @return shader instance
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public <T extends Shader> T create(@Nullable T shader, int... shards) {
        RenderCore.checkRenderThread();
        int program;
        if (shader != null && shader.mProgram != 0) {
            program = shader.mProgram;
        } else {
            program = glCreateProgram();
        }
        for (int s : shards) {
            glAttachShader(program, s);
        }
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(program, 8192);
            ModernUI.LOGGER.error(MARKER, "Failed to link shader program:\n{}", log);
            // deletion detaches all shader shards
            glDeleteProgram(program);
            program = 0;
        } else {
            for (int s : shards) {
                glDetachShader(program, s);
            }
        }
        if (shader == null) {
            shader = (T) new Shader();
        }
        shader.mProgram = program;
        return shader;
    }

    /**
     * Callback function to reload shaders.
     */
    @FunctionalInterface
    public interface Listener {

        @RenderThread
        void onReload(@Nonnull ShaderManager manager);
    }
}
