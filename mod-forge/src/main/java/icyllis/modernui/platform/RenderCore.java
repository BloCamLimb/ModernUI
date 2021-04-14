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

import icyllis.modernui.graphics.GLWrapper;
import icyllis.modernui.graphics.shader.Shader;
import icyllis.modernui.graphics.shader.ShaderProgram;
import icyllis.modernui.graphics.shader.program.ArcProgram;
import icyllis.modernui.graphics.shader.program.CircleProgram;
import icyllis.modernui.graphics.shader.program.RectProgram;
import icyllis.modernui.graphics.shader.program.RoundRectProgram;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.Version;
import org.lwjgl.opengl.GL;
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
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class RenderCore {

    public static final Marker MARKER = MarkerManager.getMarker("Graphics");

    public static int glCapabilitiesErrors;

    static boolean sInitialized = false;

    private static Thread sRenderThread;

    /**
     * Initialize GLFW, call on JVM main thread.
     */
    public static void initBackend() {
        LOGGER.info(MARKER, "Backend Library: LWJGL {}", Version.getVersion());
        if (glfwSetErrorCallback(RenderCore::callbackError) != null || !glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }
    }

    private static void callbackError(int errorCode, long descPtr) {
        String desc = descPtr == NULL ? "" : MemoryUtil.memUTF8(descPtr);
        LOGGER.error(MARKER, "GLFW Error: 0x{}, {}", Integer.toHexString(errorCode), desc);
    }

    public static void ensureRenderThread() {
        if (Thread.currentThread() != sRenderThread)
            throw new IllegalStateException("Not called from render thread");
    }

    public static Thread getRenderThread() {
        return sRenderThread;
    }

    /**
     * Call after creating a Window on render thread.
     */
    public static synchronized void initEngine() {
        if (sInitialized) {
            return;
        }
        sRenderThread = Thread.currentThread();

        GLCapabilities caps;
        try {
            caps = GL.getCapabilities();
        } catch (IllegalStateException e) {
            caps = GL.createCapabilities();
        }
        GLWrapper.initialize(caps);

        ArcProgram.createPrograms();
        CircleProgram.createPrograms();
        RectProgram.createPrograms();
        RoundRectProgram.createPrograms();

        sInitialized = true;
        LOGGER.info(MARKER, "Backend API: OpenGL {}", glGetString(GL_VERSION));
        LOGGER.info(MARKER, "OpenGL Renderer: {} {}", glGetString(GL_VENDOR), glGetString(GL_RENDERER));
    }

    public static boolean isInitialized() {
        return sInitialized;
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