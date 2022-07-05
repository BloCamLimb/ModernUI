/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.opengl;

import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.core.Core;
import icyllis.modernui.core.Window;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.opengl.*;
import org.lwjgl.system.Platform;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.annotation.Nonnull;
import java.util.*;

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
public final class GLCore extends GL45C {

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

    //private static Redirector sRedirector;

    /**
     * The value is determined when we have a OpenGL context.
     */
    private static int sMaxTextureSize = 1024;
    private static int sMaxRenderbufferSize = 2048;
    private static int sMaxArrayTextureLayers = 256;

    private static List<String> sUnsupportedList = Collections.emptyList();

    // enabled or disabled
    //private static boolean sCullState = false;
    //private static int sCullMode = GL_BACK;

    //private static int sDrawFramebuffer = DEFAULT_FRAMEBUFFER;
    //private static int sReadFramebuffer = DEFAULT_FRAMEBUFFER;

    //private static int sVertexArray = GLOBAL_VERTEX_ARRAY;

    //private static final Deque<Rect> sViewportStack = new ArrayDeque<>();

    //private static int sActiveTexture = 0;
    // texture unit (index) to texture target to texture name
    //private static final Int2IntMap[] sBindTextures;

    static {
        /*Int2IntMap[] bindTextures = new Int2IntMap[32];
        for (int i = 0; i < 32; i++) {
            // since texture_2d is most commonly used, an array map would be faster
            Int2IntMap o = new Int2IntArrayMap();
            o.defaultReturnValue(DEFAULT_TEXTURE);
            bindTextures[i] = o;
        }
        sBindTextures = bindTextures;*/
    }

    private GLCore() {
        throw new UnsupportedOperationException();
    }

    /*// call before initialization
    public static synchronized void setRedirector(@Nonnull Redirector redirector) {
        if (sRedirector == null) {
            sRedirector = redirector;
        }
    }*/

    @RenderThread
    public static void initialize(@Nonnull GLCapabilities caps) {
        Core.checkRenderThread();
        if (sInitialized) {
            return;
        }

        if (glGetPointer(GL_DEBUG_CALLBACK_FUNCTION) == NULL) {
            if (caps.OpenGL43) {
                LOGGER.debug(MARKER, "Using OpenGL 4.3 for error logging");
                GLDebugMessageCallback proc = GLDebugMessageCallback.create(GLCore::onDebugMessage);
                glDebugMessageCallback(proc, NULL);
                glEnable(GL_DEBUG_OUTPUT);
            } else if (caps.GL_KHR_debug) {
                LOGGER.debug(MARKER, "Using KHR_debug for error logging");
                GLDebugMessageCallback proc = GLDebugMessageCallback.create(GLCore::onDebugMessage);
                KHRDebug.glDebugMessageCallback(proc, NULL);
                glEnable(GL_DEBUG_OUTPUT);
            } else if (caps.GL_ARB_debug_output) {
                LOGGER.debug(MARKER, "Using ARB_debug_output for error logging");
                GLDebugMessageARBCallback proc = GLDebugMessageARBCallback.create((source, type, id, severity, length
                        , message, userParam) ->
                        LOGGER.info(MARKER, "0x{}[{},{},{}]: {}",
                                Integer.toHexString(id), getSourceARB(source), getTypeARB(type),
                                getSeverityARB(severity),
                                GLDebugMessageARBCallback.getMessage(length, message)));
                glDebugMessageCallbackARB(proc, NULL);
            } else if (caps.GL_AMD_debug_output) {
                LOGGER.debug(MARKER, "Using AMD_debug_output for error logging");
                GLDebugMessageAMDCallback proc = GLDebugMessageAMDCallback.create((id, category, severity, length,
                                                                                   message, userParam) ->
                        LOGGER.info(MARKER, "0x{}[{},{}]: {}",
                                Integer.toHexString(id), getCategoryAMD(category), getSeverityAMD(severity),
                                GLDebugMessageAMDCallback.getMessage(length, message)));
                glDebugMessageCallbackAMD(proc, NULL);
            } else {
                LOGGER.debug(MARKER, "No debug callback function was used...");
            }
        } else {
            LOGGER.debug(MARKER, "The debug callback function is already set.");
        }

        final String glVendor = glGetString(GL_VENDOR);
        final String glRenderer = glGetString(GL_RENDERER);
        final String glVersion = glGetString(GL_VERSION);

        LOGGER.info(MARKER, "OpenGL vendor: {}", glVendor);
        LOGGER.info(MARKER, "OpenGL renderer: {}", glRenderer);
        LOGGER.info(MARKER, "OpenGL version: {}", glVersion);

        sMaxTextureSize = glGetInteger(GL_MAX_TEXTURE_SIZE);
        sMaxRenderbufferSize = glGetInteger(GL_MAX_RENDERBUFFER_SIZE);
        sMaxArrayTextureLayers = glGetInteger(GL_MAX_ARRAY_TEXTURE_LAYERS);

        LOGGER.info(MARKER, "Max texture size: {}", sMaxTextureSize);
        LOGGER.info(MARKER, "Max renderbuffer size: {}", sMaxRenderbufferSize);
        LOGGER.info(MARKER, "Max array texture layers: {}", sMaxArrayTextureLayers);

        final List<String> unsupported;
        if (!caps.OpenGL45) {
            unsupported = new ArrayList<>();
            // we don't check CONTEXT_PROFILE_MASK, we assume it's always core profile.
            if (!caps.OpenGL32) {
                throw new RuntimeException("OpenGL 3.2 core profile is unavailable");
            }
            if (!caps.OpenGL33) {
                if (!caps.GL_ARB_explicit_attrib_location) {
                    unsupported.add("ARB_explicit_attrib_location (OpenGL 3.3)");
                }
                if (!caps.GL_ARB_instanced_arrays) {
                    unsupported.add("ARB_instanced_arrays (OpenGL 3.3)");
                }
                if (!caps.GL_ARB_texture_swizzle) {
                    unsupported.add("ARB_texture_swizzle (OpenGL 3.3)");
                }
            }
            if (!caps.OpenGL42) {
                if (!caps.GL_ARB_base_instance) {
                    unsupported.add("ARB_base_instance (OpenGL 4.2)");
                }
                if (!caps.GL_ARB_texture_storage) {
                    unsupported.add("ARB_texture_storage (OpenGL 4.2)");
                }
            }
            if (!caps.OpenGL43) {
                if (!caps.GL_ARB_explicit_uniform_location) {
                    unsupported.add("ARB_explicit_uniform_location (OpenGL 4.3)");
                }
                // we use the new API introduced in OpenGL 4.3, rather than glVertexAttrib*
                if (!caps.GL_ARB_vertex_attrib_binding) {
                    unsupported.add("ARB_vertex_attrib_binding (OpenGL 4.3)");
                }
            }
            if (!caps.OpenGL44) {
                if (!caps.GL_ARB_clear_texture) {
                    unsupported.add("ARB_clear_texture (OpenGL 4.4)");
                }
            }
            // DSA, OpenGL 4.5
            if (!caps.GL_ARB_direct_state_access) {
                unsupported.add("ARB_direct_state_access (OpenGL 4.5)");
            }
        } else {
            unsupported = null;
        }

        // test optional extensions
        if (caps.GL_NV_blend_equation_advanced) {
            LOGGER.debug(MARKER, "NV blend equation advanced enabled");
            if (caps.GL_NV_blend_equation_advanced_coherent) {
                LOGGER.debug(MARKER, "NV blend equation advanced coherent enabled");
            } else {
                LOGGER.debug(MARKER, "NV blend equation advanced coherent disabled");
            }
        } else if (caps.GL_KHR_blend_equation_advanced) {
            LOGGER.debug(MARKER, "KHR blend equation advanced enabled");
            if (caps.GL_KHR_blend_equation_advanced_coherent) {
                LOGGER.debug(MARKER, "KHR blend equation advanced coherent enabled");
            } else {
                LOGGER.debug(MARKER, "KHR blend equation advanced coherent disabled");
            }
        } else {
            LOGGER.debug(MARKER, "NV or KHR blend equation advanced disabled");
        }

        if (unsupported != null && !unsupported.isEmpty()) {
            for (String s : unsupported) {
                LOGGER.fatal(MARKER, "{} is unavailable", s);
            }
            sUnsupportedList = unsupported;
        } else if (unsupported != null) {
            LOGGER.debug(MARKER, "Using OpenGL 4.5 ARB");
        } else {
            LOGGER.debug(MARKER, "Using OpenGL 4.5 Core");
        }

        /*if (sRedirector == null) {
            sRedirector = () -> {
            };
        } else {
            sRedirector.onInit();
        }*/

        sInitialized = true;
    }

    private static void onDebugMessage(int source, int type, int id, int severity, int length, long message,
                                       long userParam) {
        switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH -> LOGGER.error(MARKER, "({}|{}|0x{}) {}",
                    getDebugSource(source), getDebugType(type), Integer.toHexString(id),
                    GLDebugMessageCallback.getMessage(length, message));
            case GL_DEBUG_SEVERITY_MEDIUM -> LOGGER.warn(MARKER, "({}|{}|0x{}) {}",
                    getDebugSource(source), getDebugType(type), Integer.toHexString(id),
                    GLDebugMessageCallback.getMessage(length, message));
            case GL_DEBUG_SEVERITY_LOW -> LOGGER.info(MARKER, "({}|{}|0x{}) {}",
                    getDebugSource(source), getDebugType(type), Integer.toHexString(id),
                    GLDebugMessageCallback.getMessage(length, message));
            case GL_DEBUG_SEVERITY_NOTIFICATION -> LOGGER.debug(MARKER, "({}|{}|0x{}) {}",
                    getDebugSource(source), getDebugType(type), Integer.toHexString(id),
                    GLDebugMessageCallback.getMessage(length, message));
        }
    }

    public static int getMaxTextureSize() {
        return sMaxTextureSize;
    }

    public static int getMaxRenderbufferSize() {
        return sMaxRenderbufferSize;
    }

    public static int getMaxArrayTextureLayers() {
        return sMaxArrayTextureLayers;
    }

    public static List<String> getUnsupportedList() {
        return sUnsupportedList;
    }

    /**
     * Show a dialog that lists unsupported extensions after initialized.
     */
    @RenderThread
    public static void showCapsErrorDialog() {
        Core.checkRenderThread();
        if (!sInitialized || sUnsupportedList.isEmpty()) {
            return;
        }
        final String glRenderer = glGetString(GL_RENDERER);
        final String glVersion = glGetString(GL_VERSION);
        new Thread(() -> {
            String solution;
            if (Platform.get() == Platform.MACOSX) {
                solution = "For macOS, click OK for help, click Cancel to ignore this error.";
            } else {
                solution = "For Windows and Linux, first update your GPU drivers. " +
                        "If you have integrated GPU, run Java applications with dedicated GPU. " +
                        "Otherwise, click OK for help, click Cancel to ignore this error.";
            }
            String extensions = String.join("\n", sUnsupportedList);
            boolean ok = TinyFileDialogs.tinyfd_messageBox("Failed to launch Modern UI",
                    "GPU: " + glRenderer + ", OpenGL: " + glVersion + ". " +
                            "The following ARB extensions are required:\n" + extensions + "\n" + solution,
                    "okcancel", "error", true);
            if (ok) {
                Core.openURI("https://github.com/BloCamLimb/ModernUI/wiki/OpenGL-4.5-support");
            }
        }, "GL-Error-Dialog").start();
    }

    /**
     * Resets states before rendering a new frame.
     *
     * @param window the window for rendering.
     */
    @RenderThread
    public static void resetFrame(@Nonnull Window window) {
        Core.checkRenderThread();
        /*sViewportStack.clear();

        final Rect viewport = new Rect(0, 0, window.getWidth(), window.getHeight());
        sViewportStack.push(viewport);*/
        glViewport(0, 0, window.getWidth(), window.getHeight());

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    /*
     * Resets all OpenGL states managed by GLWrapper for compatibility.
     */
    /*public static void resetStates() {
    }*/

    /*@RenderThread
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
    }*/

    /*@RenderThread
    public static void bindTexture(int target, int texture) {
        if (sRedirector.bindTexture(target, texture))
            return;
        if (sBindTextures[sActiveTexture].put(target, texture) != texture)
            glBindTexture(target, texture);
    }*/

    /*public static void bindTextureUnit(int unit, int texture) {

    }*/

    /*@RenderThread
    public static void deleteTexture(int texture) {
        int target = glGetTextureParameteri(texture, GL_TEXTURE_TARGET);
        for (var m : sBindTextures)
            if (m.get(target) == texture)
                m.put(target, DEFAULT_TEXTURE);
        glDeleteTextures(texture);
    }*/

    // r - the runnable that calls this method
    /*public static void deleteTextureAsync(int texture, @Nullable Runnable r) {
        if (Core.isOnRenderThread()) {
            glDeleteTextures(texture);
        } else {
            Core.postOnRenderThread(Objects.requireNonNullElseGet(r,
                    () -> () -> glDeleteTextures(texture)));
        }
    }*/

    // select active texture unit, min 0-7, max 31, def 0
    // the unit is passed to sampler
    /*@RenderThread
    public static void activeTexture(int unit) {
        if (unit != sActiveTexture) {
            sActiveTexture = unit;
            glActiveTexture(GL_TEXTURE0 + unit);
        }
    }*/

    // ret active texture unit 0-7, max 31, not GL_TEXTURE0[1-31]
    // used for sampler value
    /*@RenderThread
    public static int getActiveTexture() {
        return sActiveTexture;
    }*/

    /*public static void deleteBufferAsync(int buffer, @Nullable Runnable r) {
        if (Core.isOnRenderThread()) {
            glDeleteBuffers(buffer);
        } else {
            Core.postOnRenderThread(Objects.requireNonNullElseGet(r,
                    () -> () -> glDeleteBuffers(buffer)));
        }
    }

    public static void deleteFramebufferAsync(int framebuffer, @Nullable Runnable r) {
        if (Core.isOnRenderThread()) {
            glDeleteFramebuffers(framebuffer);
        } else {
            Core.postOnRenderThread(Objects.requireNonNullElseGet(r,
                    () -> () -> glDeleteFramebuffers(framebuffer)));
        }
    }

    public static void deleteRenderbufferAsync(int renderbuffer, @Nullable Runnable r) {
        if (Core.isOnRenderThread()) {
            glDeleteRenderbuffers(renderbuffer);
        } else {
            Core.postOnRenderThread(Objects.requireNonNullElseGet(r,
                    () -> () -> glDeleteRenderbuffers(renderbuffer)));
        }
    }*/

    /*@RenderThread
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
    }*/

    /*
     * Specifies whether front- or back-facing facets are candidates for culling.
     * Symbolic constants {@link #GL_FRONT}, {@link #GL_BACK}, and
     * {@link #GL_FRONT_AND_BACK} are accepted. The initial value is {@link #GL_BACK}.
     *
     * @param mode culling mode
     */
    /*@RenderThread
    public static void cullFace(int mode) {
        if (mode != sCullMode) {
            sCullMode = mode;
            glCullFace(mode);
        }
    }*/

    /*
     * Use undefined shader program.
     */
    /*@RenderThread
    public static void stopProgram() {
        glUseProgram(0);
    }*/

    /*
     * Applies a new viewport rect and pushes it into the stack.
     *
     * @param viewport the viewport rect.
     */
    /*@RenderThread
    public static void pushViewport(@Nonnull Rect viewport) {
        if (viewport.isEmpty())
            return;
        final Rect top = sViewportStack.peek();
        sViewportStack.push(viewport);
        if (viewport.equals(top))
            return;
        glViewport(viewport.left, viewport.top, viewport.width(), viewport.height());
    }*/

    /*
     * Applies the last viewport rect.
     */
    /*@RenderThread
    public static void popViewport() {
        final Rect last;
        if (!Objects.equals(sViewportStack.peek(), last = sViewportStack.pop()))
            glViewport(last.left, last.top, last.width(), last.height());
        if (sViewportStack.isEmpty())
            throw new IllegalStateException("Popping the main viewport");
    }*/

    @Nonnull
    private static String getDebugSource(int source) {
        return switch (source) {
            case GL_DEBUG_SOURCE_API -> "API";
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "Window System";
            case GL_DEBUG_SOURCE_SHADER_COMPILER -> "Shader Compiler";
            case GL_DEBUG_SOURCE_THIRD_PARTY -> "Third Party";
            case GL_DEBUG_SOURCE_APPLICATION -> "Application";
            case GL_DEBUG_SOURCE_OTHER -> "Other";
            default -> apiUnknownToken(source);
        };
    }

    @Nonnull
    private static String getDebugType(int type) {
        return switch (type) {
            case GL_DEBUG_TYPE_ERROR -> "Error";
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "Deprecated Behavior";
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "Undefined Behavior";
            case GL_DEBUG_TYPE_PORTABILITY -> "Portability";
            case GL_DEBUG_TYPE_PERFORMANCE -> "Performance";
            case GL_DEBUG_TYPE_OTHER -> "Other";
            case GL_DEBUG_TYPE_MARKER -> "Marker";
            default -> apiUnknownToken(type);
        };
    }

    @Nonnull
    private static String getDebugSeverity(int severity) {
        return switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH -> "High";
            case GL_DEBUG_SEVERITY_MEDIUM -> "Medium";
            case GL_DEBUG_SEVERITY_LOW -> "Low";
            case GL_DEBUG_SEVERITY_NOTIFICATION -> "Notification";
            default -> apiUnknownToken(severity);
        };
    }

    @Nonnull
    private static String getSourceARB(int source) {
        return switch (source) {
            case GL_DEBUG_SOURCE_API_ARB -> "API";
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM_ARB -> "Window System";
            case GL_DEBUG_SOURCE_SHADER_COMPILER_ARB -> "Shader Compiler";
            case GL_DEBUG_SOURCE_THIRD_PARTY_ARB -> "Third Party";
            case GL_DEBUG_SOURCE_APPLICATION_ARB -> "Application";
            case GL_DEBUG_SOURCE_OTHER_ARB -> "Other";
            default -> apiUnknownToken(source);
        };
    }

    @Nonnull
    private static String getTypeARB(int type) {
        return switch (type) {
            case GL_DEBUG_TYPE_ERROR_ARB -> "Error";
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR_ARB -> "Deprecated Behavior";
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR_ARB -> "Undefined Behavior";
            case GL_DEBUG_TYPE_PORTABILITY_ARB -> "Portability";
            case GL_DEBUG_TYPE_PERFORMANCE_ARB -> "Performance";
            case GL_DEBUG_TYPE_OTHER_ARB -> "Other";
            default -> apiUnknownToken(type);
        };
    }

    @Nonnull
    private static String getSeverityARB(int severity) {
        return switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH_ARB -> "High";
            case GL_DEBUG_SEVERITY_MEDIUM_ARB -> "Medium";
            case GL_DEBUG_SEVERITY_LOW_ARB -> "Low";
            default -> apiUnknownToken(severity);
        };
    }

    @Nonnull
    private static String getCategoryAMD(int category) {
        return switch (category) {
            case GL_DEBUG_CATEGORY_API_ERROR_AMD -> "API Error";
            case GL_DEBUG_CATEGORY_WINDOW_SYSTEM_AMD -> "Window System";
            case GL_DEBUG_CATEGORY_DEPRECATION_AMD -> "Deprecation";
            case GL_DEBUG_CATEGORY_UNDEFINED_BEHAVIOR_AMD -> "Undefined Behavior";
            case GL_DEBUG_CATEGORY_PERFORMANCE_AMD -> "Performance";
            case GL_DEBUG_CATEGORY_SHADER_COMPILER_AMD -> "Shader Compiler";
            case GL_DEBUG_CATEGORY_APPLICATION_AMD -> "Application";
            case GL_DEBUG_CATEGORY_OTHER_AMD -> "Other";
            default -> apiUnknownToken(category);
        };
    }

    @Nonnull
    private static String getSeverityAMD(int severity) {
        return switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH_AMD -> "High";
            case GL_DEBUG_SEVERITY_MEDIUM_AMD -> "Medium";
            case GL_DEBUG_SEVERITY_LOW_AMD -> "Low";
            default -> apiUnknownToken(severity);
        };
    }

    // redirect default methods, return true instead
    /*@Deprecated
    @FunctionalInterface
    private interface Redirector {

        void onInit();

        default boolean bindTexture(int target, int texture) {
            return false;
        }

        default boolean deleteTexture(int target, int texture) {
            return false;
        }
    }*/
}
