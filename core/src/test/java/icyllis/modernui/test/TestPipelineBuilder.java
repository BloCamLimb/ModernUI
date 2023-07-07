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

import icyllis.arc3d.*;
import icyllis.arc3d.engine.Surface;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.ops.RoundRectOp;
import icyllis.arc3d.opengl.*;
import icyllis.modernui.core.Core;
import icyllis.modernui.core.MainWindow;
import icyllis.modernui.graphics.Matrix3;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.Set;

public class TestPipelineBuilder {

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

        var window = MainWindow.initialize("Window", 1600, 900);
        window.makeCurrent();
        if (!Core.initOpenGL())
            throw new RuntimeException("Failed to initialize OpenGL");
        GLCore.setupDebugCallback();

        var dContext = Core.requireDirectContext();
        var engine = (GLEngine) dContext.getEngine();

        @SharedPtr
        var rt = dContext.getProxyProvider().createRenderTextureProxy(
                GLBackendFormat.make(GLCore.GL_RGBA8),
                800, 800, 4,
                Surface.FLAG_BUDGETED
        );
        Objects.requireNonNull(rt);

        System.out.printf("Uniform Buffer Offset Alignment: %d\n",
                GLCore.glGetInteger(GLCore.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT));

        GLCore.glViewport(0, 0, window.getWidth(), window.getHeight());

        DrawingManager drawingManager = dContext.getDrawingManager();

        var rtv = new SurfaceProxyView(rt, Engine.SurfaceOrigin.kLowerLeft, Swizzle.RGBA);
        var op = new RoundRectOp(new float[]{0.88f, 0.075f, 0.11f, 1},
                new Rect2f(90, 90, 180, 180), 10, 5, Matrix3.identity());
        op.onPrepare(drawingManager.getFlushState(), rtv, 0);
        engine.getVertexPool().flush();
        engine.getInstancePool().flush();
        OpsRenderPass opsRenderPass = drawingManager.getFlushState().beginOpsRenderPass(rtv,
                new Rect2i(0, 0, 900, 900),
                Engine.LoadStoreOps.DontLoad_Store,
                Engine.LoadStoreOps.DontLoad_Store,
                new float[]{0, 0, 0, 0},
                Set.of(),
                0);
        op.onExecute(drawingManager.getFlushState(), new Rect2f(0, 0, 900, 900));
        opsRenderPass.end();
        op.onEndFlush();
        rtv.close();
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
