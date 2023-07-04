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

import icyllis.arc3d.SharedPtr;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.geom.RoundRectGeoProc;
import icyllis.arc3d.opengl.*;
import icyllis.modernui.core.Core;
import icyllis.modernui.core.MainWindow;
import icyllis.modernui.graphics.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Objects;

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
                Surface.FLAG_BUDGETED | Surface.FLAG_RENDERABLE
        );
        Objects.requireNonNull(rt);

        var geomProc = new RoundRectGeoProc(false);
        var pso = (GLPipelineState) dContext.findOrCreatePipelineState(
                new PipelineInfo(new SurfaceProxyView(rt),
                        geomProc,
                        null,
                        null,
                        null,
                        null,
                        0));
        rt.unref();
        Objects.requireNonNull(pso);
        System.out.println(dContext.getPipelineStateCache().getStates());

        System.out.printf("Uniform Buffer Offset Alignment: %d\n", GLCore.glGetInteger(GLCore.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT));

        ByteBuffer data = MemoryUtil.memAlloc(2048);
        data.putFloat(2f / window.getWidth())
                .putFloat(-1)
                .putFloat(2f / window.getHeight())
                .putFloat(-1);
        data.rewind();
        int renderUniformBuffer = GLCore.glCreateBuffers();
        GLCore.glNamedBufferStorage(renderUniformBuffer, 256, GLCore.GL_DYNAMIC_STORAGE_BIT);
        GLCore.nglNamedBufferSubData(renderUniformBuffer, 0, 256, MemoryUtil.memAddress(data));

        GLCore.glBindBufferBase(GLCore.GL_UNIFORM_BUFFER, 0, renderUniformBuffer);

        GLBuffer vertexBuffer = GLBuffer.make(engine, 32, Engine.BufferUsageFlags.kVertex |
                Engine.BufferUsageFlags.kStatic);
        Objects.requireNonNull(vertexBuffer);
        data.putFloat(-1).putFloat(1);
        data.putFloat(1).putFloat(1);
        data.putFloat(-1).putFloat(-1);
        data.putFloat(1).putFloat(-1);
        data.rewind();
        vertexBuffer.updateData(MemoryUtil.memAddress(data), 0, 32);
        MemoryUtil.memFree(data);

        GLCore.glViewport(0, 0, window.getWidth(), window.getHeight());

        Buffer[] buffers = new Buffer[2];
        ByteBuffer instanceWriter = engine.getInstancePool().makeWriter(new Mesh() {
            @Override
            public int getInstanceSize() {
                return geomProc.instanceStride();
            }

            @Override
            public int getInstanceCount() {
                return 8;
            }

            @Override
            public void setInstanceBuffer(Buffer buffer, int baseInstance, int actualInstanceCount) {
                buffers[1] = buffer;
            }
        });
        Objects.requireNonNull(instanceWriter);
        for (int i = 0; i < 8; ) {
            int shift = i++ * 120;
            float cx = 50 + shift;
            float cy = 40 + shift / 2f;
            // color
            instanceWriter.putFloat(1 - i * 0.12f).putFloat(i * 0.075f).putFloat(i * 0.11f).putFloat(1);
            // local rect
            instanceWriter.putFloat((i & 1) == 1 ? 30 : 20).putFloat(cx).putFloat(20).putFloat(cy);
            // radii
            instanceWriter.putFloat(i * 2).putFloat(i);
            var m4 = Matrix4.identity();
            m4.m34 = 1 / 1920f;
            m4.preTranslate(cx, cy);
            //m4.preScale(3, 3);
            m4.preRotateY(MathUtil.PI_O_6);
            m4.preTranslate(-cx, -cy);
            var m3 = m4.toM33NoZ();
            m3.store(MemoryUtil.memAddress(instanceWriter));
            // model view
            instanceWriter.position(instanceWriter.position() + 36);
        }
        engine.getInstancePool().flush();

        if (!pso.bindPipeline(engine.currentCommandBuffer())) {
            throw new IllegalStateException();
        }
        pso.bindBuffers(null, vertexBuffer, 0, buffers[1], 0);
        GLCore.glDrawArraysInstancedBaseInstance(GLCore.GL_TRIANGLE_STRIP, 0, 4, 8, 0);
        window.swapBuffers();
        while (!window.shouldClose()) {
            GLFW.glfwWaitEvents();
        }
        buffers[1].unref();
        vertexBuffer.unref();

        dContext.unref();
        window.close();
        Core.terminate();
    }
}
