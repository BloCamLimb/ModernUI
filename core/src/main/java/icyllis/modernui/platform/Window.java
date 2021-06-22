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

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Represents a window to operating system, providing OpenGL context.
 */
public abstract class Window implements AutoCloseable {

    private static final Long2ObjectMap<Window> sWindows = new Long2ObjectArrayMap<>();

    protected final long mHandle;

    protected Window(long handle) {
        mHandle = handle;
        sWindows.putIfAbsent(handle, this);
    }

    @Nullable
    public static Window get(long handle) {
        return sWindows.get(handle);
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
        return new WindowImpl(handle, state, borderless, fullscreen);
    }

    public final long getHandle() {
        return mHandle;
    }

    public abstract void makeCurrent();

    /**
     * Gets whether this window should be closed. For example, by clicking
     * the close button in the title bar.
     *
     * @return {@code true} if this window should be closed
     */
    public abstract boolean shouldClose();

    /**
     * A helper method that inverts {@link #shouldClose()}
     *
     * @return {@code true} if this window still exists
     * @see #shouldClose()
     */
    public boolean exists() {
        return !shouldClose();
    }

    public abstract boolean isRefreshNeeded();

    public abstract void swapBuffers();

    public void destroy() {
        sWindows.remove(mHandle);
    }

    /**
     * Returns the x-coordinate of the top-left corner of this window
     * in virtual screen coordinate system.
     *
     * @return the x-coordinate of this window
     */
    public abstract int getXPos();

    /**
     * Returns the y-coordinate of the top-left corner of this window
     * in virtual screen coordinate system.
     *
     * @return the y-coordinate of this window
     */
    public abstract int getYPos();

    /**
     * Returns the framebuffer width for this window in pixels.
     *
     * @return framebuffer width
     */
    public abstract int getWidth();

    /**
     * Returns the framebuffer height for this window in pixels.
     *
     * @return framebuffer height
     */
    public abstract int getHeight();

    /**
     * Returns the window width in virtual screen coordinates.
     *
     * @return window width
     */
    public abstract int getScreenWidth();

    /**
     * Returns the window height in virtual screen coordinates.
     *
     * @return window height
     */
    public abstract int getScreenHeight();

    public float getAspectRatio() {
        return (float) getWidth() / getHeight();
    }

    public double getScreenPixelFactorX() {
        return (double) getWidth() / getScreenWidth();
    }

    public double getScreenPixelFactorY() {
        return (double) getHeight() / getScreenHeight();
    }

    @Override
    public final void close() throws Exception {
        destroy();
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
