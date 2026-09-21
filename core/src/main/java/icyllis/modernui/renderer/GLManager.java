/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.renderer;

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.ContextOptions;
import icyllis.arc3d.engine.ImmediateContext;
import icyllis.arc3d.opengl.GLUtil;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.util.Log;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageAMDCallback;
import org.lwjgl.opengl.GLDebugMessageARBCallback;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengles.GLES;
import org.lwjgl.system.SharedLibrary;

import java.util.function.Supplier;

import static icyllis.arc3d.opengl.GLUtil.*;
import static icyllis.modernui.util.Log.LOGGER;
import static org.lwjgl.opengl.AMDDebugOutput.glDebugMessageCallbackAMD;
import static org.lwjgl.opengl.ARBDebugOutput.glDebugMessageCallbackARB;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL43C.*;
import static org.lwjgl.sdl.SDLError.*;
import static org.lwjgl.sdl.SDLVideo.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * This class contains the shared global WGL or EGL context.
 *
 * @hidden
 */
@ApiStatus.Internal
public final class GLManager {

    // used only for WGL
    private long mBootstrapWindow;
    // EGLContext/HGLRC
    private long mGLContext;
    private Supplier<Object> mCapsCreator;

    // On Windows:
    //   windows: WGL + GL;  EGL + GLES (ANGLE)
    //   offscreen: EGL + GLES (ANGLE)
    // On FreeBSD/Linux:
    //   offscreen, x11, wayland: EGL + GL/GLES

    public void initializeGL() {
        if (mGLContext != NULL) {
            return;
        }
        SharedLibrary library = (SharedLibrary) GL.getFunctionProvider();
        if (library == null) {
            throw new IllegalStateException("GL library is not loaded from LWJGL side");
        }
        String libraryPath = library.getPath();
        SDL_ClearError();
        if (!SDL_GL_LoadLibrary(libraryPath)) {
            throw new IllegalStateException("Failed to load GL library from SDL side, " +
                    "path: " + libraryPath + ", error: " + SDL_GetError());
        }
        Log.LOGGER.debug(GLUtil.MARKER, "Loaded GL library, path: {}", libraryPath);

        // for offscreen, this is pbuffer surface
        mBootstrapWindow = SDL_CreateWindow(
                "OpenGL Offscreen Context", 64, 64,
                SDL_WINDOW_OPENGL | SDL_WINDOW_HIDDEN | SDL_WINDOW_BORDERLESS
        );
        if (mBootstrapWindow == NULL) {
            throw new IllegalStateException("Failed to create bootstrap window for OpenGL context setup");
        }

        SDL_GL_SetAttribute(SDL_GL_CONTEXT_FLAGS, SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4);
        long context = NULL;
        for (int minor = 6; minor >= 0; minor--) {
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, minor);
            context = SDL_GL_CreateContext(mBootstrapWindow);
            if (context != NULL) {
                break;
            }
        }
        if (context == NULL) {
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 3);
            context = SDL_GL_CreateContext(mBootstrapWindow);
            if (context == NULL) {
                throw new IllegalStateException("OpenGL 3.3 Core is not available");
            }
        }

        mGLContext = context;
        mCapsCreator = GL::createCapabilities;

        SDL_GL_MakeCurrent(NULL, NULL);
    }

    public void initializeGLES() {
        GLES.getFunctionProvider();
    }

    public long getGLContext() {
        return mGLContext;
    }

    @RenderThread
    public boolean makeCurrent(long window) {
        return SDL_GL_MakeCurrent(window != NULL ? window : mBootstrapWindow, mGLContext);
    }

    public boolean swapBuffers(long window) {
        return SDL_GL_SwapWindow(window);
    }

    @Nullable
    @SharedPtr
    public ImmediateContext createContext(@NonNull ContextOptions options) {
        makeCurrent(NULL);
        Object capabilities = mCapsCreator.get();
        return GLUtil.makeOpenGL(capabilities, options);
    }

    @RenderThread
    public void glSetupDebugCallback() {

        GLCapabilities caps = GL.getCapabilities();

        if (glGetPointer(GL_DEBUG_CALLBACK_FUNCTION) == NULL) {
            if (caps.OpenGL43 || caps.GL_KHR_debug) {
                LOGGER.debug(MARKER, "Using OpenGL 4.3 for debug logging");
                glDebugMessageCallback(this::glDebugMessage, NULL);
                glEnable(GL_DEBUG_OUTPUT);
            } else if (caps.GL_ARB_debug_output) {
                LOGGER.debug(MARKER, "Using ARB_debug_output for debug logging");
                GLDebugMessageARBCallback proc = new GLDebugMessageARBCallback() {
                    @Override
                    public void invoke(int source, int type, int id, int severity, int length, long message,
                                       long userParam) {
                        LOGGER.info(MARKER, "0x{}[{},{},{}]: {}", Integer.toHexString(id),
                                getSourceARB(source), getTypeARB(type), getSeverityARB(severity),
                                GLDebugMessageARBCallback.getMessage(length, message));
                    }
                };
                glDebugMessageCallbackARB(proc, NULL);
            } else if (caps.GL_AMD_debug_output) {
                LOGGER.debug(MARKER, "Using AMD_debug_output for debug logging");
                GLDebugMessageAMDCallback proc = new GLDebugMessageAMDCallback() {
                    @Override
                    public void invoke(int id, int category, int severity, int length, long message,
                                       long userParam) {
                        LOGGER.info(MARKER, "0x{}[{},{}]: {}", Integer.toHexString(id),
                                getCategoryAMD(category), getSeverityAMD(severity),
                                GLDebugMessageAMDCallback.getMessage(length, message));
                    }
                };
                glDebugMessageCallbackAMD(proc, NULL);
            } else {
                LOGGER.debug(MARKER, "No debug callback function was used...");
            }
        } else {
            LOGGER.debug(MARKER, "The debug callback function is already set.");
        }
    }

    public void glDebugMessage(int source, int type, int id, int severity, int length, long message,
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

    public void destroy() {
        if (mBootstrapWindow != NULL) {
            SDL_DestroyWindow(mBootstrapWindow);
            mBootstrapWindow = NULL;
        }
        if (mGLContext != NULL) {
            SDL_GL_DestroyContext(mGLContext);
            mGLContext = NULL;
        }
    }
}
