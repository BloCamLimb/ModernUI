/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.core.awt;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import org.lwjgl.system.APIUtil.APIVersion;
import org.lwjgl.system.jawt.*;
import org.lwjgl.system.linux.X11;

import java.awt.*;
import java.nio.IntBuffer;
import java.util.List;
import java.util.*;

import static org.lwjgl.opengl.GLX.*;
import static org.lwjgl.opengl.GLX13.*;
import static org.lwjgl.opengl.GLXARBCreateContext.*;
import static org.lwjgl.opengl.GLXARBCreateContextProfile.*;
import static org.lwjgl.opengl.GLXARBCreateContextRobustness.*;
import static org.lwjgl.opengl.GLXARBRobustnessApplicationIsolation.GLX_CONTEXT_RESET_ISOLATION_BIT_ARB;
import static org.lwjgl.opengl.GLXEXTCreateContextESProfile.GLX_CONTEXT_ES_PROFILE_BIT_EXT;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.jawt.JAWTFunctions.*;

public class PlatformLinuxGLCanvas implements PlatformGLCanvas {
    public static final JAWT awt;

    static {
        awt = JAWT.calloc();
        awt.version(JAWT_VERSION_1_4);
        if (!JAWT_GetAWT(awt))
            throw new AssertionError("GetAWT failed");
    }

    public long display;
    public long drawable;
    public JAWTDrawingSurface ds;

    private long create(int depth, GLData attribs, GLData effective) throws AWTException {
        int screen = X11.XDefaultScreen(display);
        IntBuffer attrib_list = BufferUtils.createIntBuffer(16 * 2);
        attrib_list.put(GLX_DRAWABLE_TYPE).put(GLX_WINDOW_BIT);
        attrib_list.put(GLX_RENDER_TYPE).put(GLX_RGBA_BIT);
        attrib_list.put(GLX_RED_SIZE).put(attribs.redSize);
        attrib_list.put(GLX_GREEN_SIZE).put(attribs.greenSize);
        attrib_list.put(GLX_BLUE_SIZE).put(attribs.blueSize);
        attrib_list.put(GLX_DEPTH_SIZE).put(attribs.depthSize);
        attrib_list.put(GLX_DOUBLEBUFFER).put(attribs.doubleBuffer ? 1 : 0);
        attrib_list.put(0);
        attrib_list.flip();
        PointerBuffer fbConfigs = glXChooseFBConfig(display, screen, attrib_list);
        if (fbConfigs == null || fbConfigs.capacity() == 0) {
            // No framebuffer configurations supported!
            throw new AWTException("No supported framebuffer configurations found");
        }

        verifyGLXCapabilities(display, screen, attribs);
        IntBuffer gl_attrib_list = bufferGLAttribs(attribs);

        long share_context = NULL;
        if (Objects.nonNull(attribs.shareContext)) {
            if (attribs.shareContext.context == NULL) {
                throw new IllegalStateException(
                        "Attributes specified shareContext but it is not yet created and thus cannot be shared");
            }
            share_context = attribs.shareContext.context;
        }

        long context = glXCreateContextAttribsARB(display, fbConfigs.get(0), share_context, true, gl_attrib_list);
        if (context == 0) {
            throw new AWTException("Unable to create GLX context");
        }

        populateEffectiveGLXAttribs(display, fbConfigs.get(0), effective);

        if (!makeCurrent(context)) {
            throw new AWTException("Unable to make context current");
        }
        populateEffectiveGLAttribs(effective);
        makeCurrent(0 /* no context */);

        return context;
    }

    public void lock() throws AWTException {
        int lock = JAWT_DrawingSurface_Lock(ds, ds.Lock());
        if ((lock & JAWT_LOCK_ERROR) != 0)
            throw new AWTException("JAWT_DrawingSurface_Lock() failed");
    }

    public void unlock() throws AWTException {
        JAWT_DrawingSurface_Unlock(ds, ds.Unlock());
    }

    public long create(Canvas canvas, GLData attribs, GLData effective) throws AWTException {
        this.ds = JAWT_GetDrawingSurface(canvas, awt.GetDrawingSurface());
        JAWTDrawingSurface ds = JAWT_GetDrawingSurface(canvas, awt.GetDrawingSurface());
        try {
            lock();
            try {
                JAWTDrawingSurfaceInfo dsi = JAWT_DrawingSurface_GetDrawingSurfaceInfo(ds, ds.GetDrawingSurfaceInfo());
                try {
                    JAWTX11DrawingSurfaceInfo dsiWin = JAWTX11DrawingSurfaceInfo.create(dsi.platformInfo());
                    int depth = dsiWin.depth();
                    this.display = dsiWin.display();
                    this.drawable = dsiWin.drawable();
                    return create(depth, attribs, effective);
                } finally {
                    JAWT_DrawingSurface_FreeDrawingSurfaceInfo(dsi, ds.FreeDrawingSurfaceInfo());
                }
            } finally {
                unlock();
            }
        } finally {
            JAWT_FreeDrawingSurface(ds, awt.FreeDrawingSurface());
        }
    }

    public boolean deleteContext(long context) {
        return false;
    }

    public boolean makeCurrent(long context) {
        if (context == 0L)
            return glXMakeCurrent(display, 0L, 0L);
        return glXMakeCurrent(display, drawable, context);
    }

    public boolean isCurrent(long context) {
        return glXGetCurrentContext() == context;
    }

    public boolean swapBuffers() {
        glXSwapBuffers(display, drawable);
        return true;
    }

    public boolean delayBeforeSwapNV(float seconds) {
        throw new UnsupportedOperationException("NYI");
    }

    public void dispose() {
        if (this.ds != null) {
            JAWT_FreeDrawingSurface(this.ds, awt.FreeDrawingSurface());
            this.ds = null;
        }
    }

    private static void verifyGLXCapabilities(long display, int screen, GLData data) throws AWTException {
        List<String> extensions = Arrays.asList(glXQueryExtensionsString(display, screen).split(" "));
        if (!extensions.contains("GLX_ARB_create_context")) {
            throw new AWTException("GLX_ARB_create_context is unavailable");
        }
        if (data.api == GLData.API.GLES && !extensions.contains("GLX_EXT_create_context_es_profile")) {
            throw new AWTException("OpenGL ES API requested but GLX_EXT_create_context_es_profile is unavailable");
        }
        if (data.profile != null && !extensions.contains("GLX_ARB_create_context_profile")) {
            throw new AWTException("OpenGL profile requested but GLX_ARB_create_context_profile is unavailable");
        }
        if (data.robustness && !extensions.contains("GLX_ARB_create_context_robustness")) {
            throw new AWTException("OpenGL robustness requested but GLX_ARB_create_context_robustness is unavailable");
        }
        if (data.contextResetIsolation && !extensions.contains("GLX_ARB_robustness_application_isolation")) {
            throw new AWTException("OpenGL robustness requested but GLX_ARB_robustness_application_isolation is " +
					"unavailable");
        }
    }

    private static IntBuffer bufferGLAttribs(GLData data) throws AWTException {
        IntBuffer gl_attrib_list = BufferUtils.createIntBuffer(16 * 2);

        // Set the render type and version
        gl_attrib_list.put(GLX_RENDER_TYPE).put(GLX_RGBA_TYPE);

        if (data.majorVersion > 0) {
            gl_attrib_list
                    .put(GLX_CONTEXT_MAJOR_VERSION_ARB).put(data.majorVersion)
                    .put(GLX_CONTEXT_MINOR_VERSION_ARB).put(data.minorVersion);
        }

        // Set the profile based on GLData.api and GLData.profile
        int profile_attrib = -1;
        if (data.api == GLData.API.GLES) {
            if (data.profile != null) {
                throw new AWTException("Cannot request both OpenGL ES and profile: " + data.profile);
            }
            profile_attrib = GLX_CONTEXT_ES_PROFILE_BIT_EXT;
        } else if (data.api == GLData.API.GL || data.api == null) {
            if (data.profile == GLData.Profile.CORE) {
                profile_attrib = GLX_CONTEXT_CORE_PROFILE_BIT_ARB;
            } else if (data.profile == GLData.Profile.COMPATIBILITY) {
                profile_attrib = GLX_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB;
            } else if (data.profile != null) {
                throw new AWTException("Unknown requested profile: " + data.profile);
            }
        } else {
            throw new AWTException("Unknown requested API: " + data.api);
        }
        if (profile_attrib != -1) {
            gl_attrib_list.put(GLX_CONTEXT_PROFILE_MASK_ARB).put(profile_attrib);
        }

        // Set debugging and forward compatibility
        int context_flags = 0;
        if (data.debug) {
            context_flags |= GLX_CONTEXT_DEBUG_BIT_ARB;
        }
        if (data.forwardCompatible) {
            context_flags |= GLX_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB;
        }
        if (data.robustness) {
            context_flags |= GLX_CONTEXT_ROBUST_ACCESS_BIT_ARB;

            int notificationStrategy;
            if (data.loseContextOnReset) {
                notificationStrategy = GLX_LOSE_CONTEXT_ON_RESET_ARB;

                if (data.contextResetIsolation) {
                    context_flags |= GLX_CONTEXT_RESET_ISOLATION_BIT_ARB;
                }
            } else {
                notificationStrategy = GLX_NO_RESET_NOTIFICATION_ARB;
            }
            gl_attrib_list.put(GLX_CONTEXT_RESET_NOTIFICATION_STRATEGY_ARB).put(notificationStrategy);
        }
        gl_attrib_list.put(GLX_CONTEXT_FLAGS_ARB).put(context_flags);

        gl_attrib_list.put(0).flip();
        return gl_attrib_list;
    }

    private static void populateEffectiveGLXAttribs(long display, long fbId, GLData effective)
            throws AWTException {
        IntBuffer buffer = BufferUtils.createIntBuffer(1);

        glXGetFBConfigAttrib(display, fbId, GLX_RED_SIZE, buffer);
        effective.redSize = buffer.get(0);

        glXGetFBConfigAttrib(display, fbId, GLX_GREEN_SIZE, buffer);
        effective.greenSize = buffer.get(0);

        glXGetFBConfigAttrib(display, fbId, GLX_BLUE_SIZE, buffer);
        effective.blueSize = buffer.get(0);

        glXGetFBConfigAttrib(display, fbId, GLX_DEPTH_SIZE, buffer);
        effective.depthSize = buffer.get(0);

        glXGetFBConfigAttrib(display, fbId, GLX_DOUBLEBUFFER, buffer);
        effective.doubleBuffer = buffer.get(0) == 1;
    }

    private static void populateEffectiveGLAttribs(GLData effective) throws AWTException {
        long glGetIntegerv = GL.getFunctionProvider().getFunctionAddress("glGetIntegerv");
        long glGetString = GL.getFunctionProvider().getFunctionAddress("glGetString");
        APIVersion version = APIUtil.apiParseVersion(getString(GL11.GL_VERSION, glGetString));

        effective.majorVersion = version.major;
        effective.minorVersion = version.minor;

        int profileFlags = getInteger(GL32.GL_CONTEXT_PROFILE_MASK, glGetIntegerv);

        if ((profileFlags & GLX_CONTEXT_ES_PROFILE_BIT_EXT) != 0) {
            effective.api = GLData.API.GLES;
        } else {
            effective.api = GLData.API.GL;
        }

        if (version.major >= 3) {
            if (version.major >= 4 || version.minor >= 2) {
                if ((profileFlags & GL32.GL_CONTEXT_CORE_PROFILE_BIT) != 0) {
                    effective.profile = GLData.Profile.CORE;
                } else if ((profileFlags & GL32.GL_CONTEXT_COMPATIBILITY_PROFILE_BIT) != 0) {
                    effective.profile = GLData.Profile.COMPATIBILITY;
                } else if (
                        (profileFlags & GLX_CONTEXT_ES_PROFILE_BIT_EXT) != 0) {
                    // OpenGL ES allows checking for profiles at versions below 3.2, so avoid branching into
                    // the if and actually check later.
                } else if (profileFlags != 0) {
                    throw new AWTException("Unknown profile " + profileFlags);
                }
            }

            int effectiveContextFlags = getInteger(GL30.GL_CONTEXT_FLAGS, glGetIntegerv);
            effective.debug = (effectiveContextFlags & GL43.GL_CONTEXT_FLAG_DEBUG_BIT) != 0;
            effective.forwardCompatible =
                    (effectiveContextFlags & GL30.GL_CONTEXT_FLAG_FORWARD_COMPATIBLE_BIT) != 0;
            effective.robustness =
                    (effectiveContextFlags & ARBRobustness.GL_CONTEXT_FLAG_ROBUST_ACCESS_BIT_ARB) != 0;
            effective.contextResetIsolation =
                    (effectiveContextFlags & GLX_CONTEXT_RESET_ISOLATION_BIT_ARB) != 0;
        }

        if (effective.robustness) {
            int effectiveNotificationStrategy = getInteger(ARBRobustness.GL_RESET_NOTIFICATION_STRATEGY_ARB,
					glGetIntegerv);
            effective.loseContextOnReset =
					(effectiveNotificationStrategy & ARBRobustness.GL_LOSE_CONTEXT_ON_RESET_ARB) != 0;
        }

        effective.samples = getInteger(GL13.GL_SAMPLES, glGetIntegerv);
    }

    private static int getInteger(int pname, long function) {
        MemoryStack stack = MemoryStack.stackGet();
        int stackPointer = stack.getPointer();
        try {
            IntBuffer params = stack.callocInt(1);
            JNI.callPV(pname, memAddress(params), function);
            return params.get(0);
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    private static String getString(int pname, long function) {
        return memUTF8(Checks.check(JNI.callP(pname, function)));
    }
}
