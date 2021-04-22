/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.math.Rect;
import icyllis.modernui.platform.RenderCore;
import icyllis.modernui.platform.Window;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;
import java.util.Stack;

import static icyllis.modernui.ModernUI.LOGGER;
import static org.lwjgl.opengl.GL43.*;

public final class GLWrapper {

    /**
     * The id that represents an null OpenGL object.
     */
    public static final int UNASSIGNED_ID = -1;

    /**
     * The default framebuffer that used for swapping buffers with window.
     */
    public static final int DEFAULT_FRAMEBUFFER = 0;

    private static boolean sInitialized = false;

    /**
     * The value is determined when we have a OpenGL context.
     */
    private static int sMaxTextureSize = 1024;
    private static int sMaxRenderBufferSize = 2048;

    // enabled or disabled
    private static boolean sCullState = false;
    private static int sCullMode = GL_BACK;

    private static final Stack<Rect> sViewportStack = new Stack<>();

    @RenderThread
    public static void initialize(@Nonnull GLCapabilities caps) {
        RenderCore.ensureThread();
        if (sInitialized) {
            return;
        }

        sMaxTextureSize = glGetInteger(GL_MAX_TEXTURE_SIZE);
        sMaxRenderBufferSize = glGetInteger(GL_MAX_RENDERBUFFER_SIZE);

        int count = 0;
        if (!caps.GL_ARB_vertex_buffer_object) {
            LOGGER.fatal(RenderCore.MARKER, "ARB vertex buffer object is not supported");
            count++;
        }
        if (!caps.GL_ARB_explicit_attrib_location) {
            LOGGER.fatal(RenderCore.MARKER, "ARB explicit attrib location is not supported");
            count++;
        }
        if (!caps.GL_ARB_vertex_array_object) {
            LOGGER.fatal(RenderCore.MARKER, "ARB vertex array object is not supported");
            count++;
        }
        if (!caps.GL_ARB_framebuffer_object) {
            LOGGER.fatal(RenderCore.MARKER, "ARB framebuffer object is not supported");
            count++;
        }
        if (!caps.GL_ARB_uniform_buffer_object) {
            LOGGER.fatal(RenderCore.MARKER, "ARB uniform buffer object is not supported");
            count++;
        }
        if (!caps.GL_ARB_separate_shader_objects) {
            LOGGER.fatal(RenderCore.MARKER, "ARB separate shader objects is not supported");
            count++;
        }
        if (!caps.GL_ARB_explicit_uniform_location) {
            LOGGER.fatal(RenderCore.MARKER, "ARB explicit uniform location is not supported");
            count++;
        }

        if (!caps.OpenGL43) {
            String glVersion = glGetString(GL_VERSION);
            if (glVersion == null) glVersion = "UNKNOWN";
            else glVersion = glVersion.split(" ")[0];
            ModernUI.get().warnSetup("warning.modernui.old_opengl", "4.3", glVersion);
            LOGGER.fatal(RenderCore.MARKER, "OpenGL is too old, your version is {} but requires OpenGL 4.3", glVersion);
        }

        if (count > 0) {
            LOGGER.fatal(RenderCore.MARKER, "There are {} GL capabilities that are not supported by your graphics environment", count);
            LOGGER.fatal(RenderCore.MARKER, "Try to use dedicated GPU for Java applications or upgrade your graphics card driver");
        }

        //FIXME remove ResourceLocation
        /*ArcProgram.createPrograms();
        CircleProgram.createPrograms();
        RectProgram.createPrograms();
        RoundRectProgram.createPrograms();*/

        sInitialized = true;
    }

    public static int getMaxTextureSize() {
        return sMaxTextureSize;
    }

    public static int getMaxRenderBufferSize() {
        return sMaxRenderBufferSize;
    }

    /**
     * Resets states before rendering a new frame.
     *
     * @param window the window for rendering.
     */
    @RenderThread
    public static void reset(@Nonnull Window window) {
        RenderCore.ensureThread();
        sViewportStack.clear();

        final Rect viewport = new Rect(0, 0, window.getWidth(), window.getHeight());
        sViewportStack.push(viewport);
        glViewport(0, 0, window.getWidth(), window.getHeight());

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    @RenderThread
    public static void enableCull() {
        if (!sCullState) {
            sCullState = true;
            glEnable(GL_CULL_FACE);
        }
    }

    @RenderThread
    public static void disableCull() {
        if (sCullState) {
            sCullState = false;
            glDisable(GL_CULL_FACE);
        }
    }

    /**
     * Specifies whether front- or back-facing facets are candidates for culling.
     * Symbolic constants {@link GL43#GL_FRONT}, {@link GL43#GL_BACK}, and
     * {@link GL43#GL_FRONT_AND_BACK} are accepted. The initial value is {@link GL43#GL_BACK}.
     *
     * @param mode One of {@link GL43#GL_FRONT}, {@link GL43#GL_BACK} and
     *             {@link GL43#GL_FRONT_AND_BACK}
     */
    @RenderThread
    public static void cullFace(@NativeType("GLenum") int mode) {
        if (mode != sCullMode) {
            sCullMode = mode;
            glCullFace(mode);
        }
    }

    /**
     * Applies a new viewport rect and pushes it into the stack.
     *
     * @param viewport the viewport rect.
     * @throws java.util.EmptyStackException not called reset()
     */
    @RenderThread
    public static void pushViewport(@Nonnull Rect viewport) {
        if (viewport.isEmpty())
            return;
        final Rect top = sViewportStack.peek();
        sViewportStack.push(viewport);
        if (top.equals(viewport))
            return;
        glViewport(viewport.left, viewport.top, viewport.width(), viewport.height());
    }

    /**
     * Applies the last viewport rect.
     */
    @RenderThread
    public static void popViewport() {
        final Rect last;
        if (!sViewportStack.peek().equals(last = sViewportStack.pop()))
            glViewport(last.left, last.top, last.width(), last.height());
        if (sViewportStack.isEmpty())
            throw new IllegalStateException("Popping the main viewport");
    }
}
