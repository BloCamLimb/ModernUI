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

package icyllis.modernui.core;

import icyllis.modernui.ModernUI;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memPutAddress;

/**
 * Represents a window to operating system, which provides OpenGL context.
 */
public final class Window implements AutoCloseable {

    private static final Long2ObjectMap<Window> sWindows = new Long2ObjectArrayMap<>();

    private final long mHandle;

    private int mXPos;
    private int mYPos;
    private int mScreenWidth;
    private int mScreenHeight;

    private int mFramebufferWidth;
    private int mFramebufferHeight;

    private int mWindowedX;
    private int mWindowedY;

    private float mContentScaleX;
    private float mContentScaleY;

    @Nonnull
    private State mState;
    // previously maximized
    private boolean mMaximized;
    private boolean mBorderless;
    private boolean mFullscreen;

    private boolean mRefresh;

    private Window(long handle, @Nonnull State state, boolean borderless, boolean fullscreen) {
        mHandle = handle;
        sWindows.putIfAbsent(handle, this);

        // set callbacks
        glfwSetWindowPosCallback(handle, this::callbackPos);
        glfwSetWindowSizeCallback(handle, this::callbackSize);
        glfwSetWindowRefreshCallback(handle, this::callbackRefresh);
        glfwSetWindowFocusCallback(handle, this::callbackFocus);
        glfwSetWindowIconifyCallback(handle, this::callbackIconify);
        glfwSetWindowMaximizeCallback(handle, this::callbackMaximize);
        glfwSetFramebufferSizeCallback(handle, this::callbackFramebufferSize);
        glfwSetWindowContentScaleCallback(handle, this::callbackContentScale);

        glfwSetKeyCallback(handle, (window, keycode, scancode, action, mods) -> {
            if (keycode == GLFW_KEY_ESCAPE) {
                glfwSetWindowShouldClose(mHandle, true);
            }
            if (action == GLFW_PRESS && (mods & GLFW_MOD_CONTROL) != 0 && keycode == GLFW_KEY_V) {
                ModernUI.LOGGER.info("Paste: {}", Clipboard.getText());
            }
        });
        /*glfwSetCharCallback(handle, (window, ch) -> {
            ModernUI.LOGGER.info(MarkerManager.getMarker("Input"), "InputChar: {}", ch);
        });*/

        // initialize values
        Monitor monitor = Monitor.getPrimary();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            glfwGetWindowSize(handle, w, h);
            mScreenWidth = w.get(0);
            mScreenHeight = h.get(0);

            w.position(0);
            h.position(0);
            glfwGetFramebufferSize(handle, w, h);
            mFramebufferWidth = w.get(0);
            mFramebufferHeight = h.get(0);

            // center window
            if (!mFullscreen && monitor != null) {
                VideoMode m = monitor.getCurrentMode();
                glfwSetWindowPos(handle, (m.getWidth() - mScreenWidth) / 2 + monitor.getXPos(),
                        (m.getHeight() - mScreenHeight) / 2 + monitor.getYPos());
            }
        }

        mState = state;
        mBorderless = borderless;
        mFullscreen = fullscreen;

        mRefresh = true;
    }

    @Nonnull
    public static Window create(@Nonnull String title, @Nonnull State state, int width, int height) {
        // set hints
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_ANY_PROFILE);
        glfwWindowHintString(GLFW_X11_CLASS_NAME, title);
        glfwWindowHintString(GLFW_X11_INSTANCE_NAME, title);
        glfwWindowHint(GLFW_SAMPLES, 4);

        // create window
        Monitor monitor = Monitor.getPrimary();
        long handle;
        boolean borderless = false;
        boolean fullscreen = false;
        switch (state) {
            case FULLSCREEN_BORDERLESS:
                glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
                if (monitor != null) {
                    VideoMode m = monitor.getCurrentMode();
                    handle = glfwCreateWindow(m.getWidth(), m.getHeight(), title, NULL, NULL);
                } else {
                    handle = glfwCreateWindow(width, height, title, NULL, NULL);
                }
                borderless = true;
                break;
            case FULLSCREEN:
                if (monitor != null) {
                    if (width <= 0 || height <= 0) {
                        VideoMode m = monitor.getCurrentMode();
                        handle = glfwCreateWindow(m.getWidth(), m.getHeight(), title, monitor.getHandle(), NULL);
                    } else {
                        handle = glfwCreateWindow(width, height, title, monitor.getHandle(), NULL);
                    }
                    fullscreen = true;
                    break;
                }
                // FALLTHROUGH
            default:
                handle = glfwCreateWindow(width, height, title, NULL, NULL);
                break;
        }

        if (handle == NULL) {
            throw new IllegalStateException("Failed to create window");
        }
        return new Window(handle, state, borderless, fullscreen);
    }

    @Nullable
    public static Window get(long handle) {
        return sWindows.get(handle);
    }

    /**
     * Get the pointer of this window in the window system.
     *
     * @return the handle of the window
     */
    public final long getHandle() {
        return mHandle;
    }

    private void callbackPos(long window, int xPos, int yPos) {
        mXPos = xPos;
        mYPos = yPos;
    }

    private void callbackSize(long window, int width, int height) {
        mScreenWidth = width;
        mScreenHeight = height;
    }

    private void callbackRefresh(long window) {
        if (!mRefresh) {
            mRefresh = true;
        }
    }

    private void callbackFocus(long window, boolean focused) {

    }

    private void callbackIconify(long window, boolean iconified) {
        if (iconified) {
            mState = State.MINIMIZED;
        } else if (mMaximized) {
            mState = State.MAXIMIZED;
        } else {
            mState = State.WINDOWED;
        }
    }

    private void callbackMaximize(long window, boolean maximized) {
        if (maximized) {
            mState = State.MAXIMIZED;
        } else {
            mState = State.WINDOWED;
        }
        mMaximized = maximized;
    }

    private void callbackFramebufferSize(long window, int width, int height) {
        mFramebufferWidth = width;
        mFramebufferHeight = height;
    }

    private void callbackContentScale(long window, float scaleX, float scaleY) {

    }

    private void applyMode() {
        glfwSetWindowMonitor(mHandle, glfwGetPrimaryMonitor(), 0, 0, 1920, 1080, 144);
    }

    /**
     * Makes the OpenGL context of this window on the current calling thread.
     * It can only be on one thread at the same time.
     */
    public void makeCurrent() {
        glfwMakeContextCurrent(mHandle);
    }

    /**
     * Gets whether this window should be closed. For example, by clicking
     * the close button in the title bar.
     *
     * @return {@code true} if this window should be closed
     */
    public boolean shouldClose() {
        return glfwWindowShouldClose(mHandle);
    }

    public boolean isRefresh() {
        return mRefresh;
    }

    /**
     * Swaps the default framebuffer in the current OpenGL context to the
     * operating system.
     */
    public void swapBuffers() {
        glfwSwapBuffers(mHandle);
        mRefresh = false;
    }

    /**
     * Returns the x-coordinate of the top-left corner of this window
     * in virtual screen coordinate system.
     *
     * @return the x-coordinate of this window
     */
    public int getXPos() {
        return mXPos;
    }

    /**
     * Returns the y-coordinate of the top-left corner of this window
     * in virtual screen coordinate system.
     *
     * @return the y-coordinate of this window
     */
    public int getYPos() {
        return mYPos;
    }

    /**
     * Returns the framebuffer width for this window in pixels.
     *
     * @return framebuffer width
     */
    public int getWidth() {
        return mFramebufferWidth;
    }

    /**
     * Returns the framebuffer height for this window in pixels.
     *
     * @return framebuffer height
     */
    public int getHeight() {
        return mFramebufferHeight;
    }

    /**
     * Returns the window width in virtual screen coordinates.
     *
     * @return window width
     */
    public int getScreenWidth() {
        return mScreenWidth;
    }

    /**
     * Returns the window height in virtual screen coordinates.
     *
     * @return window height
     */
    public int getScreenHeight() {
        return mScreenHeight;
    }

    public final float getAspectRatio() {
        return (float) mFramebufferWidth / mFramebufferHeight;
    }

    public void maximize() {
        glfwMaximizeWindow(mHandle);
    }

    /**
     * Sets window icon.
     *
     * @param lp 16*16
     * @param mp 32*32
     * @param hp 48*48
     */
    public void setIcon(@Nonnull NativeImage lp, @Nonnull NativeImage mp, @Nonnull NativeImage hp) {
        GLFWImage.Buffer images = GLFWImage.mallocStack(3);
        images.position(0);
        images.width(lp.getWidth());
        images.height(lp.getHeight());
        memPutAddress(images.address() + GLFWImage.PIXELS, lp.getPixels());

        images.position(1);
        images.width(mp.getWidth());
        images.height(mp.getHeight());
        memPutAddress(images.address() + GLFWImage.PIXELS, mp.getPixels());

        images.position(2);
        images.width(hp.getWidth());
        images.height(hp.getHeight());
        memPutAddress(images.address() + GLFWImage.PIXELS, hp.getPixels());

        images.position(0);
        glfwSetWindowIcon(mHandle, images);
    }

    /**
     * Destroys the window and remove all callbacks.
     */
    @Override
    public void close() {
        sWindows.remove(mHandle);
        Callbacks.glfwFreeCallbacks(mHandle);
        glfwDestroyWindow(mHandle);
    }

    /**
     * Window states.
     */
    public enum State {
        /**
         * The window is movable and takes up a subsection of the screen.
         */
        WINDOWED,

        /**
         * The window is running in exclusive fullscreen and is potentially using a
         * different resolution to the desktop.
         */
        FULLSCREEN,

        /**
         * The window is running in non-exclusive fullscreen, where it expands to
         * fill the screen at the native desktop resolution.
         */
        FULLSCREEN_BORDERLESS,

        /**
         * The window is running in maximized mode, usually triggered by clicking
         * the operating system's maximize button.
         */
        MAXIMIZED,

        /**
         * The window is running in minimized mode, usually triggered by clicking
         * the operating system's minimize button.
         */
        MINIMIZED
    }
}
