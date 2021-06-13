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
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.opengl.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static icyllis.modernui.ModernUI.LOGGER;
import static org.lwjgl.opengl.AMDDebugOutput.*;
import static org.lwjgl.opengl.ARBDebugOutput.*;
import static org.lwjgl.system.APIUtil.apiUnknownToken;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * For managing OpenGL-related things on the render thread, based on
 * OpenGL 4.5 core profile, all methods are at low-level.
 */
@SuppressWarnings("unused")
public final class GLWrapper extends GL45C {

    public static final Marker MARKER = MarkerManager.getMarker("OpenGL");

    /**
     * Represents an invalid/unassigned OpenGL object compared to {@link #GL_NONE}.
     */
    public static final int INVALID_ID = -1;

    /**
     * The reserved framebuffer that used for swapping buffers with window.
     */
    public static final int DEFAULT_FRAMEBUFFER = 0;

    /**
     * The global vertex array compared to custom vertex array objects.
     */
    public static final int GLOBAL_VERTEX_ARRAY = 0;

    public static final int DEFAULT_TEXTURE = 0;

    private static boolean sInitialized = false;

    private static Redirector sRedirector;

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

    private static int sVertexArray = GLOBAL_VERTEX_ARRAY;

    private static final Deque<Rect> sViewportStack = new ArrayDeque<>();

    private static int sActiveTexture = 0;
    // texture unit (index) to texture target to texture name
    private static final Int2IntMap[] sBindTextures;

    static {
        Int2IntMap[] bindTextures = new Int2IntMap[32];
        for (int i = 0; i < 32; i++) {
            // since texture_2d is most commonly used, an array map would be faster
            Int2IntMap o = new Int2IntArrayMap();
            o.defaultReturnValue(DEFAULT_TEXTURE);
            bindTextures[i] = o;
        }
        sBindTextures = bindTextures;
    }

    private GLWrapper() {
        throw new UnsupportedOperationException();
    }

    // call before initialization
    public static synchronized void setRedirector(@Nonnull Redirector redirector) {
        if (sRedirector == null) {
            sRedirector = redirector;
        }
    }

    @RenderThread
    public static void initialize(@Nonnull GLCapabilities caps) {
        RenderCore.checkRenderThread();
        if (sInitialized) {
            return;
        }

        if (caps.OpenGL43) {
            LOGGER.debug(RenderCore.MARKER, "Using OpenGL 4.3 for error logging");
            GLDebugMessageCallback proc = GLDebugMessageCallback.create(GLWrapper::onDebugMessage);
            glDebugMessageCallback(proc, NULL);
            glEnable(GL_DEBUG_OUTPUT);
        } else if (caps.GL_KHR_debug) {
            LOGGER.debug(RenderCore.MARKER, "Using KHR_debug for error logging");
            GLDebugMessageCallback proc = GLDebugMessageCallback.create(GLWrapper::onDebugMessage);
            KHRDebug.glDebugMessageCallback(proc, NULL);
            glEnable(GL_DEBUG_OUTPUT);
        } else if (caps.GL_ARB_debug_output) {
            LOGGER.debug(RenderCore.MARKER, "Using ARB_debug_output for error logging");
            GLDebugMessageARBCallback proc = GLDebugMessageARBCallback.create((source, type, id, severity, length, message, userParam) ->
                    LOGGER.info(MARKER, "0x{} [{}, {}, {}]: {}",
                            Integer.toHexString(id), getSourceARB(source), getTypeARB(type), getSeverityARB(severity),
                            GLDebugMessageARBCallback.getMessage(length, message)));
            glDebugMessageCallbackARB(proc, NULL);
        } else if (caps.GL_AMD_debug_output) {
            LOGGER.debug(RenderCore.MARKER, "Using AMD_debug_output for error logging");
            GLDebugMessageAMDCallback proc = GLDebugMessageAMDCallback.create((id, category, severity, length, message, userParam) ->
                    LOGGER.info(MARKER, "0x{} [{}, {}]: {}",
                            Integer.toHexString(id), getCategoryAMD(category), getSeverityAMD(severity),
                            GLDebugMessageAMDCallback.getMessage(length, message)));
            glDebugMessageCallbackAMD(proc, NULL);
        }

        sMaxTextureSize = glGetInteger(GL_MAX_TEXTURE_SIZE);
        sMaxRenderBufferSize = glGetInteger(GL_MAX_RENDERBUFFER_SIZE);

        if (!caps.OpenGL45) {
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
            // list all features used by Modern UI
            if (!caps.GL_ARB_vertex_buffer_object) {
                LOGGER.fatal(MARKER, "ARB vertex buffer object is not supported");
                count++;
            }
            if (!caps.GL_ARB_explicit_attrib_location) {
                LOGGER.fatal(MARKER, "ARB explicit attrib location is not supported");
                count++;
            }
            if (!caps.GL_ARB_vertex_array_object) {
                LOGGER.fatal(MARKER, "ARB vertex array object is not supported");
                count++;
            }
            if (!caps.GL_ARB_framebuffer_object) {
                LOGGER.fatal(MARKER, "ARB framebuffer object is not supported");
                count++;
            }
            if (!caps.GL_ARB_uniform_buffer_object) {
                LOGGER.fatal(MARKER, "ARB uniform buffer object is not supported");
                count++;
            }
            if (!caps.GL_ARB_instanced_arrays) {
                LOGGER.fatal(MARKER, "ARB instanced arrays is not supported");
                count++;
            }
            if (!caps.GL_ARB_separate_shader_objects) {
                LOGGER.fatal(MARKER, "ARB separate shader objects is not supported");
                count++;
            }
            if (!caps.GL_ARB_explicit_uniform_location) {
                LOGGER.fatal(MARKER, "ARB explicit uniform location is not supported");
                count++;
            }
            if (!caps.GL_ARB_texture_swizzle) {
                LOGGER.fatal(MARKER, "ARB texture swizzle is not supported");
                count++;
            }
            if (!caps.GL_ARB_base_instance) {
                LOGGER.fatal(MARKER, "ARB base instance is not supported");
                count++;
            }
            // we use the new API introduced in OpenGL 4.3, rather than glVertexAttrib*
            if (!caps.GL_ARB_vertex_attrib_binding) {
                LOGGER.fatal(MARKER, "ARB vertex attrib binding is not supported");
                count++;
            }
            // DSA, OpenGL 4.5
            if (!caps.GL_ARB_direct_state_access) {
                LOGGER.fatal(MARKER, "ARB DSA (direct state access) is not supported");
                count++;
            }

            if (count > 0) {
                ModernUI.get().warnSetup("warning.modernui.old_opengl", "4.5", glVersion);
                LOGGER.fatal(RenderCore.MARKER, "OpenGL is too old, your version is {} but requires OpenGL 4.5", glVersion);
                LOGGER.fatal(RenderCore.MARKER, "There are {} GL capabilities that are not supported by your graphics environment", count);
                LOGGER.fatal(RenderCore.MARKER, "Try to use dedicated GPU for Java applications and upgrade your graphics driver");
                throw new RuntimeException("Graphics card or driver does not meet the minimum requirement");
            }
        }

        //FIXME remove ResourceLocation
        /*ArcProgram.createPrograms();
        CircleProgram.createPrograms();
        RectProgram.createPrograms();
        RoundRectProgram.createPrograms();*/
        LOGGER.info(RenderCore.MARKER, "Graphics API: OpenGL {}", glGetString(GL_VERSION));
        LOGGER.info(RenderCore.MARKER, "OpenGL Renderer: {} {}", glGetString(GL_VENDOR), glGetString(GL_RENDERER));

        if (sRedirector == null) {
            sRedirector = () -> {
            };
        } else {
            sRedirector.onInit();
        }

        sInitialized = true;
    }

    private static void onDebugMessage(int source, int type, int id, int severity, int length, long message, long userParam) {
        switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH:
                LOGGER.error(MARKER, "({}|{}|0x{}) {}",
                        getDebugSource(source), getDebugType(type), Integer.toHexString(id),
                        GLDebugMessageCallback.getMessage(length, message));
                return;
            case GL_DEBUG_SEVERITY_MEDIUM:
                LOGGER.warn(MARKER, "({}|{}|0x{}) {}",
                        getDebugSource(source), getDebugType(type), Integer.toHexString(id),
                        GLDebugMessageCallback.getMessage(length, message));
                return;
            case GL_DEBUG_SEVERITY_NOTIFICATION:
                LOGGER.debug(MARKER, "({}|{}|0x{}) {}",
                        getDebugSource(source), getDebugType(type), Integer.toHexString(id),
                        GLDebugMessageCallback.getMessage(length, message));
                return;
            case GL_DEBUG_SEVERITY_LOW:
            default:
                LOGGER.info(MARKER, "({}|{}|0x{}) {}",
                        getDebugSource(source), getDebugType(type), Integer.toHexString(id),
                        GLDebugMessageCallback.getMessage(length, message));
        }
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
    public static void resetFrame(@Nonnull Window window) {
        RenderCore.checkRenderThread();
        sViewportStack.clear();

        final Rect viewport = new Rect(0, 0, window.getWidth(), window.getHeight());
        sViewportStack.push(viewport);
        glViewport(0, 0, window.getWidth(), window.getHeight());

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Resets all OpenGL states managed by GLWrapper for compatibility.
     */
    public static void resetStates() {

    }

    @RenderThread
    public static void bindFramebuffer(int framebuffer) {
        if (framebuffer != sDrawFramebuffer || framebuffer != sReadFramebuffer) {
            sDrawFramebuffer = sReadFramebuffer = framebuffer;
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        }
    }

    @RenderThread
    public static void bindDrawFramebuffer(int framebuffer) {
        if (framebuffer != sDrawFramebuffer) {
            sDrawFramebuffer = framebuffer;
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer);
        }
    }

    @RenderThread
    public static void bindReadFramebuffer(int framebuffer) {
        if (framebuffer != sReadFramebuffer) {
            sReadFramebuffer = framebuffer;
            glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer);
        }
    }

    @RenderThread
    public static void bindTexture(int target, int texture) {
        if (sRedirector.bindTexture(target, texture))
            return;
        if (sBindTextures[sActiveTexture].put(target, texture) != texture)
            glBindTexture(target, texture);
    }

    public static void bindTextureUnit(int unit, int texture) {

    }

    @RenderThread
    public static void deleteTexture(int texture) {
        int target = glGetTextureParameteri(texture, GL_TEXTURE_TARGET);
        for (var m : sBindTextures)
            if (m.get(target) == texture)
                m.put(target, DEFAULT_TEXTURE);
        glDeleteTextures(texture);
    }

    // r - the runnable that calls this method
    public static void deleteTextureAsync(int texture, @Nullable Runnable r) {
        if (RenderCore.isOnRenderThread()) {
            deleteTexture(texture);
        } else {
            RenderCore.recordRenderCall(Objects.requireNonNullElseGet(r,
                    () -> (Runnable) () -> deleteTexture(texture)));
        }
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

    // ret active texture unit 0-7, max 31, not GL_TEXTURE0[1-31]
    // used for sampler value
    @RenderThread
    public static int getActiveTexture() {
        return sActiveTexture;
    }

    public static void deleteBufferAsync(int buffer, @Nullable Runnable r) {
        if (RenderCore.isOnRenderThread()) {
            glDeleteBuffers(buffer);
        } else {
            RenderCore.recordRenderCall(Objects.requireNonNullElseGet(r,
                    () -> (Runnable) () -> glDeleteBuffers(buffer)));
        }
    }

    @RenderThread
    public static void bindVertexArray(int array) {
        if (array != sVertexArray) {
            sVertexArray = array;
            glBindVertexArray(array);
        }
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
        if (!Objects.equals(sViewportStack.peek(), last = sViewportStack.pop()))
            glViewport(last.left, last.top, last.width(), last.height());
        if (sViewportStack.isEmpty())
            throw new IllegalStateException("Popping the main viewport");
    }

    @Nonnull
    private static String getDebugSource(int source) {
        switch (source) {
            case GL_DEBUG_SOURCE_API:
                return "API";
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM:
                return "Window System";
            case GL_DEBUG_SOURCE_SHADER_COMPILER:
                return "Shader Compiler";
            case GL_DEBUG_SOURCE_THIRD_PARTY:
                return "Third Party";
            case GL_DEBUG_SOURCE_APPLICATION:
                return "Application";
            case GL_DEBUG_SOURCE_OTHER:
                return "Other";
            default:
                return apiUnknownToken(source);
        }
    }

    @Nonnull
    private static String getDebugType(int type) {
        switch (type) {
            case GL_DEBUG_TYPE_ERROR:
                return "Error";
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR:
                return "Deprecated Behavior";
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR:
                return "Undefined Behavior";
            case GL_DEBUG_TYPE_PORTABILITY:
                return "Portability";
            case GL_DEBUG_TYPE_PERFORMANCE:
                return "Performance";
            case GL_DEBUG_TYPE_OTHER:
                return "Other";
            case GL_DEBUG_TYPE_MARKER:
                return "Marker";
            default:
                return apiUnknownToken(type);
        }
    }

    @Nonnull
    private static String getDebugSeverity(int severity) {
        switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH:
                return "High";
            case GL_DEBUG_SEVERITY_MEDIUM:
                return "Medium";
            case GL_DEBUG_SEVERITY_LOW:
                return "Low";
            case GL_DEBUG_SEVERITY_NOTIFICATION:
                return "Notification";
            default:
                return apiUnknownToken(severity);
        }
    }

    @Nonnull
    private static String getSourceARB(int source) {
        switch (source) {
            case GL_DEBUG_SOURCE_API_ARB:
                return "API";
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM_ARB:
                return "Window System";
            case GL_DEBUG_SOURCE_SHADER_COMPILER_ARB:
                return "Shader Compiler";
            case GL_DEBUG_SOURCE_THIRD_PARTY_ARB:
                return "Third Party";
            case GL_DEBUG_SOURCE_APPLICATION_ARB:
                return "Application";
            case GL_DEBUG_SOURCE_OTHER_ARB:
                return "Other";
            default:
                return apiUnknownToken(source);
        }
    }

    @Nonnull
    private static String getTypeARB(int type) {
        switch (type) {
            case GL_DEBUG_TYPE_ERROR_ARB:
                return "Error";
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR_ARB:
                return "Deprecated Behavior";
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR_ARB:
                return "Undefined Behavior";
            case GL_DEBUG_TYPE_PORTABILITY_ARB:
                return "Portability";
            case GL_DEBUG_TYPE_PERFORMANCE_ARB:
                return "Performance";
            case GL_DEBUG_TYPE_OTHER_ARB:
                return "Other";
            default:
                return apiUnknownToken(type);
        }
    }

    @Nonnull
    private static String getSeverityARB(int severity) {
        switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH_ARB:
                return "High";
            case GL_DEBUG_SEVERITY_MEDIUM_ARB:
                return "Medium";
            case GL_DEBUG_SEVERITY_LOW_ARB:
                return "Low";
            default:
                return apiUnknownToken(severity);
        }
    }

    @Nonnull
    private static String getCategoryAMD(int category) {
        switch (category) {
            case GL_DEBUG_CATEGORY_API_ERROR_AMD:
                return "API Error";
            case GL_DEBUG_CATEGORY_WINDOW_SYSTEM_AMD:
                return "Window System";
            case GL_DEBUG_CATEGORY_DEPRECATION_AMD:
                return "Deprecation";
            case GL_DEBUG_CATEGORY_UNDEFINED_BEHAVIOR_AMD:
                return "Undefined Behavior";
            case GL_DEBUG_CATEGORY_PERFORMANCE_AMD:
                return "Performance";
            case GL_DEBUG_CATEGORY_SHADER_COMPILER_AMD:
                return "Shader Compiler";
            case GL_DEBUG_CATEGORY_APPLICATION_AMD:
                return "Application";
            case GL_DEBUG_CATEGORY_OTHER_AMD:
                return "Other";
            default:
                return apiUnknownToken(category);
        }
    }

    @Nonnull
    private static String getSeverityAMD(int severity) {
        switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH_AMD:
                return "High";
            case GL_DEBUG_SEVERITY_MEDIUM_AMD:
                return "Medium";
            case GL_DEBUG_SEVERITY_LOW_AMD:
                return "Low";
            default:
                return apiUnknownToken(severity);
        }
    }

    // redirect default methods, return true instead
    @Deprecated
    @FunctionalInterface
    public interface Redirector {

        void onInit();

        default boolean bindTexture(int target, int texture) {
            return false;
        }

        default boolean deleteTexture(int target, int texture) {
            return false;
        }
    }
}
