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

import icyllis.arcui.gl.GLBackendFormat;
import icyllis.arcui.gl.GLCore;
import icyllis.arcui.hgi.DirectContext;
import icyllis.arcui.hgi.Swizzle;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TestManagedResource {

    public static void main(String[] args) {
        long time = System.nanoTime();
        GLFW.glfwInit();
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(1600, 900, "Test Window", 0, 0);
        if (window == 0) {
            throw new RuntimeException();
        }
        GLFW.glfwMakeContextCurrent(window);
        DirectContext direct = DirectContext.makeOpenGL();
        if (direct == null) {
            throw new RuntimeException();
        }
        PrintWriter pw = new PrintWriter(System.out, true, StandardCharsets.UTF_8);

        if (direct.getCaps().isFormatTexturable(new GLBackendFormat(EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, GLCore.GL_TEXTURE_2D))) {
            pw.println("OK");
        }
        Swizzle.make("rgb1");

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
            pw.printf("width:%d height:%s layers:%s channels:%s size:%s\n", x.get(0), y.get(0), z.get(0), channels.get(0), image.limit());
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

        direct.close();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();

        try {
            assert false;
        } catch (AssertionError e) {
            System.out.println("Assertion works " + (System.nanoTime() - time) / 1000000);
        }
    }
}
