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

import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.*;
import icyllis.arc3d.granite.geom.SDFRoundRectStep;
import icyllis.arc3d.opengl.GLDevice;
import icyllis.arc3d.opengl.GLGraphicsPipeline;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Objects;

public class TestDrawPass {

    public static final Logger LOGGER = LoggerFactory.getLogger("Arc3D");

    public static void main(String[] args) {

        GLFW.glfwInit();
        Objects.requireNonNull(GL.getFunctionProvider());
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(800, 600, "Test Window", 0, 0);
        if (window == 0) {
            throw new RuntimeException();
        }
        GLFW.glfwMakeContextCurrent(window);

        ContextOptions contextOptions = new ContextOptions();
        contextOptions.mLogger = LOGGER;
        ImmediateContext immediateContext = ImmediateContext.makeOpenGL(
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

        DrawCommandList commandList = new DrawCommandList();
        MeshDrawWriter drawWriter = new MeshDrawWriter(recordingContext.getDynamicBufferManager(),
                commandList);
        
        drawWriter.newPipelineState(
                0,
                1,
                20,
                56
        );
        commandList.bindGraphicsPipeline(0);
        drawWriter.beginInstances(null, null, 6);
        ByteBuffer writer = drawWriter.append(1);
        for (int i = 0; i < 14; i++) {
            writer.putFloat(i);
        }
        writer = drawWriter.append(1);
        for (int i = 0; i < 14; i++) {
            writer.putFloat(i);
        }
        drawWriter.endAppender();

        drawWriter.flush();
        commandList.finish();
        LOGGER.info("CommandList primitive size: {}", commandList.mPrimitives.limit());
        ObjectArrayList<Resource> resourceRefs = new ObjectArrayList<>();
        recordingContext.getDynamicBufferManager().flush(null, resourceRefs);

        LOGGER.info(resourceRefs.toString());

        PrintWriter pw = new PrintWriter(System.out, true);
        commandList.debug(pw);

        GraphicsPipelineDesc graphicsPipelineDesc = new GraphicsPipelineDesc(new SDFRoundRectStep());

        var pipeline = recordingContext.getResourceProvider().findOrCreateGraphicsPipeline(
                graphicsPipelineDesc, new RenderPassDesc());
        LOGGER.info(String.valueOf(pipeline));
        if (pipeline != null) {
            ((GLGraphicsPipeline) pipeline).bindPipeline(((GLDevice) immediateContext.getDevice()).currentCommandBuffer());
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
