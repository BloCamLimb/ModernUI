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
import icyllis.arcui.gl.GLCaps;
import icyllis.arcui.hgi.ContextOptions;
import icyllis.arcui.hgi.Swizzle;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

public class TestManagedResource {

    public static void main(String[] args) {
        GLFW.glfwInit();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(1600, 900, "Test Window", 0, 0);
        GLFW.glfwMakeContextCurrent(window);
        GLCapabilities capabilities = GL.createCapabilities();
        GLCaps caps = new GLCaps(new ContextOptions(), capabilities);

        if (caps.isFormatTexturable(new GLBackendFormat(EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, GL45C.GL_TEXTURE_2D))) {
            System.out.println("OK");
        }
        short swizzle = Swizzle.make("rgb1");
        System.out.println(Swizzle.toString(swizzle));

        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();

        try {
            assert false;
        } catch (AssertionError e) {
            System.out.println("Assertion works");
        }
    }
}
