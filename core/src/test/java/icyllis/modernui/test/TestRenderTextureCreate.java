/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.test;

import icyllis.arc3d.engine.ISurface;
import icyllis.arc3d.opengl.*;
import icyllis.modernui.core.ActivityWindow;
import icyllis.modernui.core.Core;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

public class TestRenderTextureCreate {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        Configurator.setRootLevel(Level.ALL);

        Core.initialize();

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_NATIVE_CONTEXT_API);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);

        var window = ActivityWindow.createMainWindow("ProxyWindow", 16, 16);
        window.makeCurrent();
        if (!Core.initOpenGL()) {
            throw new RuntimeException("Failed to initialize OpenGL");
        }
        Core.glSetupDebugCallback();

        var dContext = Core.requireDirectContext();

        var proxy = dContext.getSurfaceProvider().createRenderTexture(
                GLBackendFormat.make(GLCore.GL_RGBA8),
                1600, 900, 4,
                ISurface.FLAG_BUDGETED + ISurface.FLAG_MIPMAPPED
        );
        Objects.requireNonNull(proxy, "Failed to create RT");
        if (!proxy.instantiate(dContext.getResourceProvider())) {
            throw new RuntimeException();
        }

        var rt = (GLRenderTarget) proxy.getGpuRenderTarget();
        Objects.requireNonNull(rt);
        System.out.println(rt);
        System.out.println(rt.getStencilBuffer());
        System.out.println(rt.getSampleFramebuffer());
        System.out.println(rt.getResolveFramebuffer());

        dContext.unref();
        window.close();
        Core.terminate();
    }
}
