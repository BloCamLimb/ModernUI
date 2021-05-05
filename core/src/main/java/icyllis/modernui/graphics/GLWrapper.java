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
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GLCapabilities;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static icyllis.modernui.ModernUI.LOGGER;

/**
 * For managing OpenGL-related things on render thread.
 * All methods are underlying, they do not check if args are legal.
 */
public final class GLWrapper extends GL43C {

    /**
     * Represents an invalid/unassigned OpenGL object compared to {@link #GL_NONE}.
     */
    public static final int INVALID_ID = -1;

    /**
     * The reserved framebuffer that used for swapping buffers with window.
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

    private static int sDrawFramebuffer = DEFAULT_FRAMEBUFFER;
    private static int sReadFramebuffer = DEFAULT_FRAMEBUFFER;

    private static final Deque<Rect> sViewportStack = new ArrayDeque<>();

    private static int sActiveTexture = 0;
    private static final Int2IntMap[] sBindTextures;

    static {
        Int2IntMap[] bindTextures = new Int2IntMap[32];
        for (int i = 0; i < 32; i++) {
            Int2IntMap o = new Int2IntArrayMap();
            o.defaultReturnValue(GL_NONE);
            bindTextures[i] = o;
        }
        sBindTextures = bindTextures;
    }

    private GLWrapper() {
        throw new UnsupportedOperationException();
    }

    @RenderThread
    public static void initialize(@Nonnull GLCapabilities caps) {
        RenderCore.ensureRenderThread();
        if (sInitialized) {
            return;
        }

        sMaxTextureSize = glGetInteger(GL_MAX_TEXTURE_SIZE);
        sMaxRenderBufferSize = glGetInteger(GL_MAX_RENDERBUFFER_SIZE);

        if (!caps.OpenGL43) {
            String glVersion = glGetString(GL_VERSION);
            if (glVersion == null)
                glVersion = "UNKNOWN";
            else {
                try {
                    Matcher matcher = Pattern.compile("([0-9]+)\\\\.([0-9]+)(\\\\.([0-9]+))?(.+)?")
                            .matcher(glVersion);
                    glVersion = String.format("%s.%s", matcher.group(1), matcher.group(2));
                } catch (Exception ignored) {

                }
            }
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

            if (count > 0) {
                ModernUI.get().warnSetup("warning.modernui.old_opengl", "4.3", glVersion);
                LOGGER.fatal(RenderCore.MARKER, "OpenGL is too old, your version is {} but requires OpenGL 4.3", glVersion);
                LOGGER.fatal(RenderCore.MARKER, "There are {} GL capabilities that are not supported by your graphics environment", count);
                LOGGER.fatal(RenderCore.MARKER, "Try to use dedicated GPU for Java applications or upgrade your graphics card driver");
            }
        }

        //FIXME remove ResourceLocation
        /*ArcProgram.createPrograms();
        CircleProgram.createPrograms();
        RectProgram.createPrograms();
        RoundRectProgram.createPrograms();*/

        LOGGER.info(RenderCore.MARKER, "Backend API: OpenGL {}", glGetString(GL_VERSION));
        LOGGER.info(RenderCore.MARKER, "OpenGL Renderer: {} {}", glGetString(GL_VENDOR), glGetString(GL_RENDERER));

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
        RenderCore.ensureRenderThread();
        sViewportStack.clear();

        final Rect viewport = new Rect(0, 0, window.getWidth(), window.getHeight());
        sViewportStack.push(viewport);
        glViewport(0, 0, window.getWidth(), window.getHeight());

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    @RenderThread
    public static void bindFramebuffer(int framebuffer) {
        if (framebuffer != sDrawFramebuffer || framebuffer != sReadFramebuffer)
            glBindFramebuffer(GL_FRAMEBUFFER, sDrawFramebuffer = sReadFramebuffer = framebuffer);
    }

    @RenderThread
    public static void bindDrawFramebuffer(int framebuffer) {
        if (framebuffer != sDrawFramebuffer)
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, sDrawFramebuffer = framebuffer);
    }

    @RenderThread
    public static void bindReadFramebuffer(int framebuffer) {
        if (framebuffer != sReadFramebuffer)
            glBindFramebuffer(GL_READ_FRAMEBUFFER, sReadFramebuffer = framebuffer);
    }

    @RenderThread
    public static void bindTexture(int target, int texture) {
        if (sBindTextures[sActiveTexture].put(target, texture) != texture)
            glBindTexture(target, texture);
    }

    @RenderThread
    public static void deleteTexture(int texture) {
        for (Int2IntMap m : sBindTextures)
            for (Int2IntMap.Entry e : m.int2IntEntrySet())
                if (e.getIntValue() == texture)
                    m.put(e.getIntKey(), GL_NONE);
        glDeleteTextures(texture);
    }

    // select active texture unit, min 0-7, max 31, def 0
    // the unit is passed to sampler
    @RenderThread
    public static void activeTexture(int unit) {
        if (unit != sActiveTexture) {
            sActiveTexture = unit;
            glActiveTexture(GL_TEXTURE0 + unit);
        }
    }

    // ret 0-7, max 31, not GL_TEXTURE0[1-31]
    @RenderThread
    public static int getActiveTexture() {
        return sActiveTexture;
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
     * Symbolic constants {@link #GL_FRONT}, {@link #GL_BACK}, and
     * {@link #GL_FRONT_AND_BACK} are accepted. The initial value is {@link #GL_BACK}.
     *
     * @param mode culling mode
     */
    @RenderThread
    public static void cullFace(int mode) {
        if (mode != sCullMode) {
            sCullMode = mode;
            glCullFace(mode);
        }
    }

    /**
     * Applies a new viewport rect and pushes it into the stack.
     *
     * @param viewport the viewport rect.
     */
    @RenderThread
    public static void pushViewport(@Nonnull Rect viewport) {
        if (viewport.isEmpty())
            return;
        final Rect top = sViewportStack.peek();
        sViewportStack.push(viewport);
        if (viewport.equals(top))
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
