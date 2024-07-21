/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.ClipStack;
import icyllis.arc3d.granite.Device_Granite;
import icyllis.arc3d.opengl.GLUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class TestClipStack {

    public static final Logger LOGGER = LoggerFactory.getLogger("Arc3D");

    // -Dorg.slf4j.simpleLogger.logFile=System.out -ea
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
        ImmediateContext immediateContext = GLUtil.makeOpenGL(
                GL.createCapabilities(),
                contextOptions
        );
        if (immediateContext == null) {
            throw new RuntimeException();
        }
        RecordingContext recordingContext = immediateContext.makeRecordingContext();

        var drawDevice = Device_Granite.make(
                recordingContext,
                ImageInfo.make(800, 600, ColorInfo.CT_RGBA_8888,
                        ColorInfo.AT_PREMUL, ColorSpace.get(ColorSpace.Named.SRGB)),
                ISurface.FLAG_SAMPLED_IMAGE | ISurface.FLAG_RENDERABLE | ISurface.FLAG_BUDGETED,
                Engine.SurfaceOrigin.kLowerLeft,
                Engine.LoadOp.kLoad,
                "TestDevice"
        );
        assert drawDevice != null;

        drawDevice.clipRect(
                new Rect2f(0, 0, 60, 60),
                ClipOp.CLIP_OP_INTERSECT, false);

        drawDevice.save();
        //viewMatrix.preRotateZ(MathUtil.DEG_TO_RAD * 5);
        drawDevice.clipRect(
                new Rect2f(20, 20, 70, 60),
                ClipOp.CLIP_OP_INTERSECT, false);
        LOGGER.info(stateToString(drawDevice.getClipStack().currentClipState()));
        drawDevice.getClipStack().elements().forEach(e -> LOGGER.info(e.toString()));

        RoundRect rrect = new RoundRect();
        rrect.setRect(15, 20, 35, 40);
        drawDevice.drawRoundRect(rrect, new Paint());

        /*var elementsForMask = new ArrayList<ClipStack.Element>();
        var draw = new DrawOp();
        draw.mTransform = Matrix4.identity();
        draw.mTransform.preTranslate(25, 25);
        draw.mTransform.preScale(0.5f, 0.5f);
        //draw.mTransform.preRotateZ(MathUtil.DEG_TO_RAD * 20);
        draw.mTransform.preTranslate(-25, -25);
        boolean clippedOut = clipStack.prepareForDraw(
                draw,
                new Rect2f(15, 20, 35, 40),
                true,
                elementsForMask
        );
        LOGGER.info("ClippedOut: " + clippedOut);
        LOGGER.info("DrawBounds: " + draw.mDrawBounds);
        LOGGER.info("TransformedShapeBounds: " + draw.mTransformedShapeBounds);
        LOGGER.info("ScissorRect: "  + draw.mScissorRect);
        LOGGER.info("ElementsForMask: " + elementsForMask);
        clipStack.updateForDraw(draw, elementsForMask, boundsManager, 0);

        LOGGER.info(stateToString(clipStack.currentClipState()));
        clipStack.elements().forEach(e -> LOGGER.info(e.toString()));*/

        drawDevice.unref();
        recordingContext.unref();
        immediateContext.unref();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static String stateToString(int state) {
        return switch (state) {
            case ClipStack.STATE_EMPTY -> "Empty";
            case ClipStack.STATE_WIDE_OPEN -> "WideOpen";
            case ClipStack.STATE_DEVICE_RECT -> "DeviceRect";
            case ClipStack.STATE_COMPLEX -> "Complex";
            default -> throw new AssertionError(state);
        };
    }
}
