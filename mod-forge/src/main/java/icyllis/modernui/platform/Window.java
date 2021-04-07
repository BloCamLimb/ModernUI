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

package icyllis.modernui.platform;

import icyllis.modernui.ModernUI;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nonnull;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Window implements AutoCloseable {

    private final long mHandle;

    private int mXPos;
    private int mYPos;
    private int mWidth;
    private int mHeight;

    private int mFramebufferWidth;
    private int mFramebufferHeight;

    private int mWindowedX;
    private int mWindowedY;

    @Nonnull
    private WindowMode mMode;
    // previously maximized
    private boolean mMaximized;
    private boolean mBorderless;
    private boolean mFullscreen;

    public Window(@Nonnull String title, @Nonnull WindowMode mode, int width, int height) {
        // set hints
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_ANY_PROFILE);
        glfwWindowHintString(GLFW_X11_CLASS_NAME, title);
        glfwWindowHintString(GLFW_X11_INSTANCE_NAME, title);

        // create window
        Monitor monitor = Monitor.getPrimary();
        long handle;
        switch (mode) {
            case FULLSCREEN_BORDERLESS:
                glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
                if (monitor != null) {
                    VideoMode m = monitor.getCurrentMode();
                    handle = glfwCreateWindow(m.getWidth(), m.getHeight(), title, NULL, NULL);
                } else {
                    handle = glfwCreateWindow(width, height, title, NULL, NULL);
                }
                mBorderless = true;
                break;
            case FULLSCREEN:
                if (monitor != null) {
                    if (width <= 0 || height <= 0) {
                        VideoMode m = monitor.getCurrentMode();
                        handle = glfwCreateWindow(m.getWidth(), m.getHeight(), title, monitor.getHandle(), NULL);
                    } else {
                        handle = glfwCreateWindow(width, height, title, monitor.getHandle(), NULL);
                    }
                    mFullscreen = true;
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

        // set callbacks
        glfwSetWindowPosCallback(handle, this::callbackPos);
        glfwSetWindowSizeCallback(handle, this::callbackSize);
        glfwSetWindowFocusCallback(handle, this::callbackFocus);
        glfwSetWindowIconifyCallback(handle, this::callbackIconify);
        glfwSetWindowMaximizeCallback(handle, this::callbackMaximize);
        glfwSetFramebufferSizeCallback(handle, this::callbackFramebufferSize);
        glfwSetWindowContentScaleCallback(handle, this::callbackContentScale);

        glfwSetKeyCallback(handle, (window, keycode, scancode, action, mods) -> ModernUI.LOGGER.info(
                MarkerManager.getMarker("Input"), "OnKeyEvent{action: {}, key: {}}", action, keycode));

        // initialize values
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            glfwGetWindowSize(handle, w, h);
            mWidth = w.get(0);
            mHeight = h.get(0);

            w.position(0);
            h.position(0);
            glfwGetFramebufferSize(handle, w, h);
            mFramebufferWidth = w.get(0);
            mFramebufferHeight = h.get(0);

            // center window
            if (!mFullscreen && monitor != null) {
                VideoMode m = monitor.getCurrentMode();
                glfwSetWindowPos(handle, (m.getWidth() - mWidth) / 2 + monitor.getXPos(),
                        (m.getHeight() - mHeight) / 2 + monitor.getYPos());
            }
        }

        mHandle = handle;
        mMode = mode;
    }

    private void callbackPos(long window, int xPos, int yPos) {
        mXPos = xPos;
        mYPos = yPos;
    }

    private void callbackSize(long window, int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    private void callbackFocus(long window, boolean focused) {

    }

    private void callbackIconify(long window, boolean iconified) {
        if (iconified) {
            mMode = WindowMode.MINIMIZED;
        } else if (mMaximized) {
            mMode = WindowMode.MAXIMIZED;
        } else {
            mMode = WindowMode.WINDOWED;
        }
    }

    private void callbackMaximize(long window, boolean maximized) {
        if (maximized) {
            mMode = WindowMode.MAXIMIZED;
        } else {
            mMode = WindowMode.WINDOWED;
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

    public long getHandle() {
        return mHandle;
    }

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

    /**
     * A helper method that reverses should close result.
     *
     * @return {@code true} if this window still exists
     * @see #shouldClose()
     */
    public boolean exists() {
        return !glfwWindowShouldClose(mHandle);
    }

    public void swapBuffers() {
        glfwSwapBuffers(mHandle);
    }

    public void destroy() {
        Callbacks.glfwFreeCallbacks(mHandle);
        glfwDestroyWindow(mHandle);
    }

    @Override
    public void close() {
        destroy();
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
    public int getFramebufferWidth() {
        return mFramebufferWidth;
    }

    /**
     * Returns the framebuffer height for this window in pixels.
     *
     * @return framebuffer height
     */
    public int getFramebufferHeight() {
        return mFramebufferHeight;
    }
}
