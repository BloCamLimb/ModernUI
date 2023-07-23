/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.ops.OpsTask;
import icyllis.arc3d.engine.ops.RoundRectOp;
import icyllis.arc3d.opengl.*;
import icyllis.modernui.core.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

public class TestPipelineBuilder {

    public static final int WIDTH = 1600;
    public static final int HEIGHT = 900;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        Configurator.setRootLevel(Level.ALL);

        Core.initialize();

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_NATIVE_CONTEXT_API);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 5);
        //GLFW.glfwWindowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, GLFW.GLFW_TRUE);

        var window = MainWindow.initialize("Window", WIDTH, HEIGHT);
        Monitor monitor = Monitor.getPrimary();
        if (monitor != null) {
            window.center(monitor);
        }
        window.makeCurrent();
        if (!Core.initOpenGL())
            throw new RuntimeException("Failed to initialize OpenGL");
        GLCore.setupDebugCallback();
        System.out.println("Red size: " + GLCore.glGetFramebufferAttachmentParameteri(
                GLCore.GL_FRAMEBUFFER, GLCore.GL_BACK_LEFT, GLCore.GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE));
        System.out.println("Green size: " + GLCore.glGetFramebufferAttachmentParameteri(
                GLCore.GL_FRAMEBUFFER, GLCore.GL_BACK_LEFT, GLCore.GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE));
        System.out.println("Blue size: " + GLCore.glGetFramebufferAttachmentParameteri(
                GLCore.GL_FRAMEBUFFER, GLCore.GL_BACK_LEFT, GLCore.GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE));
        System.out.println("Alpha size: " + GLCore.glGetFramebufferAttachmentParameteri(
                GLCore.GL_FRAMEBUFFER, GLCore.GL_BACK_LEFT, GLCore.GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE));

        var dContext = Core.requireDirectContext();
        var server = (GLServer) dContext.getServer();

        System.out.printf("Uniform Buffer Offset Alignment: %d\n",
                GLCore.glGetInteger(GLCore.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT));

        GLFramebufferInfo framebufferInfo = new GLFramebufferInfo();
        framebufferInfo.mFramebuffer = GLCore.DEFAULT_FRAMEBUFFER;
        framebufferInfo.mFormat = GLCore.GL_RGBA8;

        @SharedPtr
        var fsp = dContext.getProxyProvider().wrapBackendRenderTarget(
                new GLBackendRenderTarget(
                        WIDTH, HEIGHT, 0, 8, framebufferInfo
                ),
                null
        );
        Objects.requireNonNull(fsp);

        DrawingManager drawingManager = dContext.getDrawingManager();

        var target = new SurfaceProxyView(fsp, Engine.SurfaceOrigin.kLowerLeft, Swizzle.RGBA);
        var task = new OpsTask(drawingManager, target);
        {
            var op = new RoundRectOp(new float[]{0.88f, 0.075f, 0.11f, 1},
                    new Rect2f(90, 90, 280, 180),
                    10, 5, Matrix.identity(), true);
            task.addDrawOp(op, null, 0);
        }
        task.makeClosed(dContext);

        task.prepare(drawingManager.getFlushState());
        server.getVertexPool().flush();
        server.getInstancePool().flush();

        task.execute(drawingManager.getFlushState());
        task.detach(drawingManager);
        task.unref();
        target.close();

        window.swapBuffers();
        while (!window.shouldClose()) {
            GLFW.glfwWaitEvents();
        }

        System.out.println(dContext.getPipelineStateCache().getStates());

        dContext.unref();
        window.close();
        Core.terminate();
    }
}
