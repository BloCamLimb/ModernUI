/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.test;

import icyllis.arc3d.core.Matrix4;
import icyllis.arc3d.core.RoundRect;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.*;
import icyllis.arc3d.granite.geom.SDFRoundRectStep;
import icyllis.arc3d.opengl.*;
import icyllis.arc3d.opengl.GLUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.Objects;
import java.util.Scanner;

public class TestDrawPass {

    public static final Logger LOGGER = LoggerFactory.getLogger("Arc3D");

    public static void main(String[] args) {

        LOGGER.info("Process ID: {}", ProcessHandle.current().pid());
        // allow injection with RenderDoc
        new Scanner(System.in).next();
        GLFW.glfwInit();
        Objects.requireNonNull(GL.getFunctionProvider());
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 4);
        //GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(1280, 720, "Test Window", 0, 0);
        if (window == 0) {
            throw new RuntimeException();
        }
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);

        ContextOptions contextOptions = new ContextOptions();
        contextOptions.mLogger = LOGGER;
        ImmediateContext immediateContext = GLUtil.makeOpenGL(
                GL.createCapabilities(),
                contextOptions
        );
        if (immediateContext == null) {
            throw new RuntimeException();
        }
        RecordingContext recordingContext = immediateContext.makeRecordingContext();
        if (recordingContext == null)  {
            throw new RuntimeException();
        }

        //new Scanner(System.in).next();

        DrawCommandList commandList = new DrawCommandList();
        MeshDrawWriter drawWriter = new MeshDrawWriter(recordingContext.getDynamicBufferManager(),
                commandList);

        var step  = new SDFRoundRectStep();
        
        drawWriter.newPipelineState(
                step.vertexBinding(),
                step.instanceBinding(),
                step.vertexStride(),
                step.instanceStride()
        );
        commandList.bindGraphicsPipeline(0);

        int nRects = 80;
        for (int i = 0; i < nRects; i++) {
            Draw draw = new Draw();
            RoundRect rrect = new RoundRect();
            rrect.mLeft = (int) (Math.random() * 910);
            rrect.mTop = (int) (Math.random() * 450);
            int w = (int) (Math.random() * Math.random() * Math.random() * Math.random() * 350)+20;
            int h = (int) (Math.random() * Math.random() * Math.random() * Math.random() * 250)+20;
            rrect.mRight = rrect.mLeft + w;
            rrect.mBottom =  rrect.mTop + h;
            rrect.mRadiusUL = Math.min((int) (Math.random() * 50), Math.min(w,h)/2);
            draw.mGeometry = rrect;
            draw.mTransform = Matrix4.identity();
            float cx = (rrect.mLeft + rrect.mRight)*0.5f;
            float cy = (rrect.mTop + rrect.mBottom)*0.5f;
            draw.mTransform.preTranslate( cx, cy);
            draw.mTransform.preRotateZ(i);
            draw.mTransform.preTranslate( -cx, -cy);
            int stroke = (int) (Math.random() * 50);
            draw.mStrokeRadius = stroke < 25 ? -1 : stroke - 20;
            step.writeVertices(drawWriter, draw, new float[]{(float) (Math.random()*0.2f),(float) (Math.random()*0.2f),(float) (Math.random()*0.2f),0.2f}); // premultiplied color
        }

        drawWriter.flush();
        commandList.finish();
        LOGGER.info("CommandList primitive size: {}", commandList.mPrimitives.limit());
        ObjectArrayList<Resource> resourceRefs = new ObjectArrayList<>();
        recordingContext.getDynamicBufferManager().flush(null, resourceRefs);

        LOGGER.info(resourceRefs.toString());

        PrintWriter pw = new PrintWriter(System.out, true);
        commandList.debug(pw);

        GraphicsPipelineDesc graphicsPipelineDesc = new GraphicsPipelineDesc(step);

        int ubo = GL33C.glGenBuffers();
        GL33C.glBindBufferBase(GL33C.GL_UNIFORM_BUFFER, 0, ubo);
        GL44C.glBufferStorage(GL33C.GL_UNIFORM_BUFFER, new float[]{2.0f / 1280, -1.0f, -2.0f / 720, 1.0f}, 0);

        GL33C.glEnable(GL33C.GL_BLEND);
        GL33C.glBlendFunc(GLCore.GL_ONE, GLCore.GL_ONE_MINUS_SRC_ALPHA);

        var pipeline = recordingContext.getResourceProvider().findOrCreateGraphicsPipeline(
                graphicsPipelineDesc, new RenderPassDesc());
        LOGGER.info(String.valueOf(pipeline));
        if (pipeline != null) {
            var device = (GLDevice) immediateContext.getDevice();
            device.flushRenderCalls();
            device.getGL().glViewport(0,0,1280, 720);
            var cmdBuffer = (GLCommandBuffer) immediateContext.currentCommandBuffer();
            assert cmdBuffer != null;
            cmdBuffer.bindGraphicsPipeline(pipeline);

            commandList.execute(cmdBuffer);
            GLFW.glfwSwapBuffers(window);

            while (!GLFW.glfwWindowShouldClose(window)) {
                GLFW.glfwWaitEvents();
                cmdBuffer.resetStates(~0);
                GL33C.glClearBufferfv(GL33C.GL_COLOR, 0, new float[]{(float) (GLFW.glfwGetTime()%1.0),0.5f,0.5f,1.0f});
                cmdBuffer.bindGraphicsPipeline(pipeline);
                commandList.execute(cmdBuffer);
                GLFW.glfwSwapBuffers(window);
            }

            pipeline.unref();
        }

        //DrawPass.make()

        //deviceGpu.unref();

        recordingContext.unref();
        immediateContext.unref();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}
