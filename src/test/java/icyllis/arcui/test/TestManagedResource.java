/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.test;

import icyllis.arcui.core.Kernel32;
import icyllis.arcui.engine.*;
import icyllis.arcui.opengl.GLBackendFormat;
import icyllis.arcui.opengl.GLCore;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class TestManagedResource {

    public static void main(String[] args) {
        long time = System.nanoTime();
        PrintWriter pw = new PrintWriter(System.out, true, StandardCharsets.UTF_8);

        GLFW.glfwInit();
        // load first
        Objects.requireNonNull(GL.getFunctionProvider());
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(1600, 900, "Test Window", 0, 0);
        if (window == 0) {
            throw new RuntimeException();
        }
        GLFW.glfwMakeContextCurrent(window);

        DirectContext directContext = DirectContext.makeOpenGL();
        if (directContext == null) {
            throw new RuntimeException();
        }
        String glVersion = GLCore.glGetString(GLCore.GL_VERSION);
        pw.println("OpenGL version: " + glVersion);
        pw.println("OpenGL vendor: " + GLCore.glGetString(GLCore.GL_VENDOR));
        pw.println("OpenGL renderer: " + GLCore.glGetString(GLCore.GL_RENDERER));

        if (directContext.getCaps().isFormatTexturable(
                new GLBackendFormat(EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT,
                        EngineTypes.TEXTURE_TYPE_2D))) {
            pw.println("Compressed format: OK");
        }
        Swizzle.make("rgb1");
        SamplerState.make(SamplerState.FILTER_MODE_NEAREST, SamplerState.MIPMAP_MODE_NONE);

        TextureProxy proxy = directContext.getProxyProvider().createTextureProxy(
                new GLBackendFormat(GLCore.GL_RGBA8, EngineTypes.TEXTURE_TYPE_2D),
                1600, 900, EngineTypes.MIPMAPPED_YES, EngineTypes.BACKING_FIT_EXACT, true, 0, false);
        try (proxy) {
            pw.println(proxy);
        }

        if (Platform.get() == Platform.WINDOWS) {
            if (!Kernel32.CloseHandle(959595595959595959L)) {
                pw.println("Failed to close handle");
            }
        }

        {
            IntArrayList intArrayList = new IntArrayList();
            KeyBuilder keyBuilder = new KeyBuilder.StringKeyBuilder(intArrayList);
            keyBuilder.addBits(6, 0x2F, "");
            keyBuilder.add32(0xC1111111);
            keyBuilder.close();
            pw.println(keyBuilder);
        }

        ByteBuffer image = null;
        try (FileChannel channel = FileChannel.open(Path.of("F:/Photoshop/Untitled-1.gif"), StandardOpenOption.READ);
             MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer mapper = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            PointerBuffer delays = stack.mallocPointer(1);
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            IntBuffer z = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            image = STBImage.stbi_load_gif_from_memory(mapper, delays, x, y, z, channels, 0);
            if (image == null) {
                throw new IOException();
            }
            IntBuffer delay = delays.getIntBuffer(z.get(0));
            pw.printf("width:%d height:%s layers:%s channels:%s size_in_bytes:%s\n", x.get(0), y.get(0), z.get(0),
                    channels.get(0), image.limit());
            for (int i = 0; i < z.get(0); i++) {
                pw.print(delay.get(i) + " ");
            }
            pw.println();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (image != null) {
                STBImage.stbi_image_free(image);
            }
        }

        directContext.close();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();

        try {
            assert false;
        } catch (AssertionError e) {
            System.out.println("Assertion works " + (System.nanoTime() - time) / 1000000);
        }
    }
}
