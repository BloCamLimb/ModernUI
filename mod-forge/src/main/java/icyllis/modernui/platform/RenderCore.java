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

package icyllis.modernui.platform;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.graphics.font.GlyphManager;
import icyllis.modernui.graphics.shader.Shader;
import icyllis.modernui.graphics.shader.ShaderProgram;
import icyllis.modernui.graphics.shader.program.ArcProgram;
import icyllis.modernui.graphics.shader.program.CircleProgram;
import icyllis.modernui.graphics.shader.program.RectProgram;
import icyllis.modernui.graphics.shader.program.RoundRectProgram;
import icyllis.modernui.graphics.textmc.TextLayoutProcessor;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.Version;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import static icyllis.modernui.ModernUI.LOGGER;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class RenderCore {

    public static final Marker MARKER = MarkerManager.getMarker("Graphics");

    public static int glCapabilitiesErrors;

    static boolean initialized = false;

    /**
     * First initialize GLFW, and then create a Window.
     * Call on JVM main thread.
     */
    public static void initBackend() {
        LOGGER.info(MARKER, "Backend Library: LWJGL {}", Version.getVersion());
        glfwSetErrorCallback(RenderCore::callbackError);
        if (!glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }
        Monitor.init();
    }

    private static void callbackError(int errorCode, long descPtr) {
        String desc = descPtr == NULL ? "" : MemoryUtil.memUTF8(descPtr);
        LOGGER.error(MARKER, "GLFW Error: 0x{}, {}", Integer.toHexString(errorCode), desc);
    }

    /**
     * Call after creating a Window.
     */
    @RenderThread
    public static void initEngine() {
        if (initialized) {
            return;
        }
        GLCapabilities capabilities = GL.getCapabilities();
        int i = 0;
        if (!capabilities.GL_ARB_vertex_buffer_object) {
            LOGGER.fatal(MARKER, "Vertex buffer object is not supported");
            i++;
        }
        if (!capabilities.GL_ARB_explicit_attrib_location) {
            LOGGER.fatal(MARKER, "Explicit attrib location is not supported");
            i++;
        }
        if (!capabilities.GL_ARB_vertex_array_object) {
            LOGGER.fatal(MARKER, "Vertex array object is not supported");
            i++;
        }
        if (!capabilities.GL_ARB_framebuffer_object) {
            LOGGER.fatal(MARKER, "Framebuffer object is not supported");
            i++;
        }
        if (!capabilities.GL_ARB_uniform_buffer_object) {
            LOGGER.fatal(MARKER, "Uniform buffer object is not supported");
            i++;
        }
        if (!capabilities.GL_ARB_separate_shader_objects) {
            LOGGER.fatal(MARKER, "Separate shader objects is not supported");
            i++;
        }
        if (!capabilities.GL_ARB_explicit_uniform_location) {
            LOGGER.fatal(MARKER, "Explicit uniform location is not supported");
            i++;
        }

        int v;
        if ((v = RenderSystem.maxSupportedTextureSize()) < GlyphManager.TEXTURE_SIZE ||
                (GlyphManager.TEXTURE_SIZE <= 1024 && (v = GL43.glGetInteger(GL43.GL_MAX_TEXTURE_SIZE)) < GlyphManager.TEXTURE_SIZE)) {
            LOGGER.fatal(MARKER, "Max texture size is too small, supplies {} but requires {}", v, GlyphManager.TEXTURE_SIZE);
            i++;
        }

        if (!capabilities.OpenGL43) {
            String glVersion = GL43.glGetString(GL43.GL_VERSION);
            if (glVersion == null) glVersion = "UNKNOWN";
            else glVersion = glVersion.split(" ")[0];
            ModernUI.get().warnSetup("warning.modernui.old_opengl", "4.3", glVersion);
        }

        if (i != 0) {
            glCapabilitiesErrors = i;
        }

        ArcProgram.createPrograms();
        CircleProgram.createPrograms();
        RectProgram.createPrograms();
        RoundRectProgram.createPrograms();

        TextLayoutProcessor.getInstance().initRenderer();

        initialized = true;
        LOGGER.info(MARKER, "Backend API: OpenGL {}", GL43.glGetString(GL43.GL_VERSION));
        LOGGER.info(MARKER, "OpenGL Renderer: {} {}", GL43.glGetString(GL43.GL_VENDOR), GL43.glGetString(GL43.GL_RENDERER));
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void compileShaders(ResourceManager manager) {
        ShaderProgram.detachAll();
        Shader.deleteAll();
        ShaderProgram.linkAll(manager);
    }

    public static ByteBuffer readRawBuffer(InputStream inputStream) throws IOException {
        ByteBuffer buffer;
        if (inputStream instanceof FileInputStream) {
            final FileChannel channel = ((FileInputStream) inputStream).getChannel();
            buffer = MemoryUtil.memAlloc((int) channel.size() + 1);
            for (; ; )
                if (channel.read(buffer) == -1)
                    break;
        } else {
            final ReadableByteChannel channel = Channels.newChannel(inputStream);
            buffer = MemoryUtil.memAlloc(8192);
            while (channel.read(buffer) != -1)
                if (buffer.remaining() == 0)
                    buffer = MemoryUtil.memRealloc(buffer, buffer.capacity() << 1);
        }
        return buffer;
    }

    @Nullable
    public static String readStringASCII(InputStream inputStream) {
        ByteBuffer buffer = null;
        try {
            buffer = readRawBuffer(inputStream);
            int i = buffer.position();
            buffer.rewind();
            return MemoryUtil.memASCII(buffer, i);
        } catch (IOException ignored) {
        } finally {
            if (buffer != null)
                MemoryUtil.memFree(buffer);
        }
        return null;
    }

    /*@Nonnull
    public static UniformFloat getUniformFloat(@Nonnull ShaderProgram shader, String name) {
        return new UniformFloat(GL20.glGetUniformLocation(shader.getProgram(), name));
    }

    @Nonnull
    public static UniformMatrix4f getUniformMatrix4f(@Nonnull ShaderProgram shader, String name) {
        int loc = GL20.glGetUniformLocation(shader.getProgram(), name);
        if (loc == -1) {
            throw new RuntimeException();
        }
        return new UniformMatrix4f(loc);
    }*/
}
