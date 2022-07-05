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

package icyllis.modernui.graphics.opengl;

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.core.Core;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.lwjgl.opengl.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class helps you create shaders and programs.
 */
public class ShaderManager {

    private static final ShaderManager INSTANCE = new ShaderManager();

    private final Set<Listener> mListeners = new HashSet<>();

    private Map<String, Object2IntMap<String>> mShaders = new HashMap<>();

    private ShaderManager() {
    }

    /**
     * @return the global shader manager instance
     */
    public static ShaderManager getInstance() {
        return INSTANCE;
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
        Core.checkRenderThread();
        for (var map : mShaders.values()) {
            for (int shard : map.values()) {
                GL20C.glDeleteShader(shard);
            }
        }
        mShaders.clear();
        for (Listener l : mListeners) {
            l.onReload(this);
        }
        for (var map : mShaders.values()) {
            for (int shard : map.values()) {
                GL20C.glDeleteShader(shard);
            }
        }
        mShaders.clear();
        mShaders = new HashMap<>();
    }

    /**
     * Get or create a shader shard, call this on listener callback.
     *
     * @param namespace the application namespace
     * @param subPath   sub paths to the shader source, parent is 'shaders'
     * @return the shader shard handle or 0 on failure
     * @see #getShard(String, String, int)
     * @see #addListener(Listener)
     */
    public int getShard(@Nonnull String namespace, @Nonnull String subPath) {
        return getShard(namespace, "shaders/" + subPath, 0);
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
     * @param namespace the application namespace
     * @param path      the path of shader source
     * @param type      the shader type to create, can be 0 for standard file extension
     * @return the shader shard or 0 on failure
     */
    public int getShard(@Nonnull String namespace, @Nonnull String path, int type) {
        Core.checkRenderThread();
        int shader = mShaders.computeIfAbsent(namespace, n -> {
            Object2IntMap<String> r = new Object2IntOpenHashMap<>();
            r.defaultReturnValue(-1);
            return r;
        }).getInt(path);
        if (shader != -1) {
            return shader;
        }
        if (type == 0) {
            if (path.endsWith(".vert")) {
                type = GL20C.GL_VERTEX_SHADER;
            } else if (path.endsWith(".frag")) {
                type = GL20C.GL_FRAGMENT_SHADER;
            } else if (path.endsWith(".geom")) {
                type = GL32C.GL_GEOMETRY_SHADER;
            } else if (path.endsWith(".tesc")) {
                type = GL40C.GL_TESS_CONTROL_SHADER;
            } else if (path.endsWith(".tese")) {
                type = GL40C.GL_TESS_EVALUATION_SHADER;
            } else if (path.endsWith(".comp")) {
                type = GL43C.GL_COMPUTE_SHADER;
            } else {
                ModernUI.LOGGER.warn(GLCore.MARKER, "Unknown type identifier for shader source {}:{}", namespace, path);
                return 0;
            }
        }
        try (ReadableByteChannel channel = ModernUI.getInstance().getResourceChannel(namespace, path)) {
            String source = Core.readUTF8(channel);
            if (source == null) {
                ModernUI.LOGGER.error(GLCore.MARKER, "Failed to read shader source {}:{}", namespace, path);
                mShaders.get(namespace).putIfAbsent(path, 0);
                return 0;
            }
            shader = GL20C.glCreateShader(type);
            GL20C.glShaderSource(shader, source);
            GL20C.glCompileShader(shader);
            if (GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS) == GL11C.GL_FALSE) {
                String log = GL20C.glGetShaderInfoLog(shader, 8192).trim();
                ModernUI.LOGGER.error(GLCore.MARKER, "Failed to compile shader {}:{}\n{}", namespace, path, log);
                GL20C.glDeleteShader(shader);
                mShaders.get(namespace).putIfAbsent(path, 0);
                return 0;
            }
            mShaders.get(namespace).putIfAbsent(path, shader);
            return shader;
        } catch (IOException e) {
            ModernUI.LOGGER.error(GLCore.MARKER, "Failed to get shader source {}:{}\n", namespace, path, e);
        }
        mShaders.get(namespace).putIfAbsent(path, 0);
        return 0;
    }

    /**
     * Create a program object representing a shader program.
     * If fails, program will be 0 (undefined).
     *
     * @param t      the existing program object
     * @param shards shader shards for the program
     * @param <T>    custom program subclasses
     * @return program
     * @see #addListener(Listener)
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public <T extends GLProgram> T create(@Nullable T t, int... shards) {
        Core.checkRenderThread();
        int program;
        if (t != null && t.mProgram != 0) {
            program = t.mProgram;
        } else {
            program = GL20C.glCreateProgram();
        }
        for (int s : shards) {
            GL20C.glAttachShader(program, s);
        }
        GL20C.glLinkProgram(program);
        if (GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS) == GL11C.GL_FALSE) {
            String log = GL20C.glGetProgramInfoLog(program, 8192);
            ModernUI.LOGGER.error(GLCore.MARKER, "Failed to link shader program\n{}", log);
            // also detaches all shaders
            GL20C.glDeleteProgram(program);
            program = 0;
        } else {
            // clear attachment states, for further re-creation
            for (int s : shards) {
                GL20C.glDetachShader(program, s);
            }
        }
        if (t == null) {
            t = (T) new GLProgram();
        }
        t.mProgram = program;
        return t;
    }

    /**
     * Callback function to reload shaders.
     */
    @FunctionalInterface
    public interface Listener {

        /**
         * This method is invoked on reloading. You may call {@link #getShard(String, String)}
         * to obtain shaders to create programs.
         *
         * @param manager the shader manager
         */
        @RenderThread
        void onReload(@Nonnull ShaderManager manager);
    }
}
