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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Bitmap;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;

import javax.annotation.concurrent.NotThreadSafe;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memPutAddress;

/**
 * Represents a Window to operating system, which provides 3D graphics context.
 */
@NotThreadSafe
public class Window implements AutoCloseable {

    private static final Long2ObjectMap<Window> sWindows = new Long2ObjectArrayMap<>();

    /**
     * Start of window types that represent normal application windows.
     */
    public static final int FIRST_APPLICATION_WINDOW = 1;

    /**
     * Window type: an application window that serves as the "base" window
     * of the overall application; all other application windows will
     * appear on top of it.
     * In multiuser systems shows only on the owning user's window.
     */
    public static final int TYPE_BASE_APPLICATION = 1;

    /**
     * Window type: a normal application window.
     * In multiuser systems shows only on the owning user's window.
     */
    public static final int TYPE_APPLICATION = 2;

    /**
     * Window type: special application window that is displayed while the
     * application is starting.  Not for use by applications themselves;
     * this is used by the system to display something until the
     * application can show its own windows.
     * In multiuser systems shows on all users' windows.
     */
    public static final int TYPE_APPLICATION_STARTING = 3;

    /**
     * Window type: a variation on TYPE_APPLICATION that ensures the window
     * manager will wait for this window to be drawn before the app is shown.
     * In multiuser systems shows only on the owning user's window.
     */
    public static final int TYPE_DRAWN_APPLICATION = 4;

    /**
     * End of types of application windows.
     */
    public static final int LAST_APPLICATION_WINDOW = 99;

    /**
     * Start of types of sub-windows.  These types of
     * windows are kept next to their attached window in Z-order, and their
     * coordinate space is relative to their attached window.
     */
    public static final int FIRST_SUB_WINDOW = 1000;

    /**
     * Window type: a panel on top of an application window.  These windows
     * appear on top of their attached window.
     */
    public static final int TYPE_APPLICATION_PANEL = FIRST_SUB_WINDOW;

    /**
     * Window type: window for showing media (such as video).  These windows
     * are displayed behind their attached window.
     */
    public static final int TYPE_APPLICATION_MEDIA = FIRST_SUB_WINDOW + 1;

    /**
     * Window type: a sub-panel on top of an application window.  These
     * windows are displayed on top their attached window and any
     * {@link #TYPE_APPLICATION_PANEL} panels.
     */
    public static final int TYPE_APPLICATION_SUB_PANEL = FIRST_SUB_WINDOW + 2;

    /**
     * Window type: like {@link #TYPE_APPLICATION_PANEL}, but layout
     * of the window happens as that of a top-level window, <em>not</em>
     * as a child of its container.
     */
    public static final int TYPE_APPLICATION_ATTACHED_DIALOG = FIRST_SUB_WINDOW + 3;

    /**
     * Window type: window for showing overlays on top of media windows.
     * These windows are displayed between TYPE_APPLICATION_MEDIA and the
     * application window.  They should be translucent to be useful.  This
     * is a big ugly hack so:
     */
    public static final int TYPE_APPLICATION_MEDIA_OVERLAY = FIRST_SUB_WINDOW + 4;

    /**
     * Window type: an above sub-panel on top of an application window and it's
     * sub-panel windows. These windows are displayed on top of their attached window
     * and any {@link #TYPE_APPLICATION_SUB_PANEL} panels.
     */
    public static final int TYPE_APPLICATION_ABOVE_SUB_PANEL = FIRST_SUB_WINDOW + 5;

    /**
     * End of types of sub-windows.
     */
    public static final int LAST_SUB_WINDOW = 1999;

    /**
     * Start of system-specific window types.  These are not normally
     * created by applications.
     */
    public static final int FIRST_SYSTEM_WINDOW = 2000;

    /**
     * Window type: the search bar.  There can be only one search bar
     * window; it is placed at the top of the screen.
     * In multiuser systems shows on all users' windows.
     */
    public static final int TYPE_SEARCH_BAR = FIRST_SYSTEM_WINDOW;

    /**
     * Window type: panel that slides out from the status bar
     * In multiuser systems shows on all users' windows.
     */
    public static final int TYPE_SYSTEM_DIALOG = FIRST_SYSTEM_WINDOW + 1;

    /**
     * Window type: wallpaper window, placed behind any window that wants
     * to sit on top of the wallpaper.
     * In multiuser systems shows only on the owning user's window.
     */
    public static final int TYPE_WALLPAPER = FIRST_SYSTEM_WINDOW + 2;

    /**
     * Window type: the drag-and-drop pseudo-window.  There is only one
     * drag layer (at most), and it is placed on top of all other windows.
     * In multiuser systems shows only on the owning user's window.
     */
    public static final int TYPE_DRAG = FIRST_SYSTEM_WINDOW + 3;

    /**
     * Window type: Application overlay windows are displayed above all activity windows
     * (types between {@link #FIRST_APPLICATION_WINDOW} and {@link #LAST_APPLICATION_WINDOW})
     * but below critical system windows like the status bar or IME.
     * <p>
     * The system may change the position, size, or visibility of these windows at anytime
     * to reduce visual clutter to the user and also manage resources.
     * <p>
     * The system will adjust the importance of processes with this window type to reduce the
     * chance of the low-memory-killer killing them.
     * <p>
     * In multi-user systems shows only on the owning user's screen.
     */
    public static final int TYPE_APPLICATION_OVERLAY = FIRST_SYSTEM_WINDOW + 4;

    /**
     * End of types of system windows.
     */
    public static final int LAST_SYSTEM_WINDOW = 2999;

    // for directly access in subclasses
    protected final long mHandle;

    protected Window(long handle) {
        Core.checkMainThread();
        mHandle = handle;
        if (sWindows.put(handle, this) != null) {
            throw new IllegalStateException("Duplicated window: 0x" + Long.toHexString(handle));
        }
    }

    @Nullable
    public static Window get(long handle) {
        return sWindows.get(handle);
    }

    /**
     * Get the pointer of this window in the GLFW window system.
     *
     * @return the handle of the window
     */
    public final long getHandle() {
        return mHandle;
    }

    /**
     * Gets whether this window is marked should be closed. For example, by clicking
     * the close button in the title bar.
     *
     * @return {@code true} if this window should be closed
     */
    public boolean shouldClose() {
        return glfwWindowShouldClose(mHandle);
    }

    /**
     * Sets the value of the close flag of the specified window. This can be used to
     * override the user's attempt to close the window, or to signal that it should
     * be closed.
     *
     * @param value should close
     */
    public void setShouldClose(boolean value) {
        glfwSetWindowShouldClose(mHandle, value);
    }

    /**
     * Sets window title.
     */
    public void setTitle(@NonNull String title) {
        glfwSetWindowTitle(mHandle, title);
    }

    /**
     * Sets window icon.
     */
    public void setIcon(@NonNull Bitmap... icons) {
        if (icons.length == 0) {
            nglfwSetWindowIcon(mHandle, 0, NULL);
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage.Buffer images = GLFWImage.malloc(icons.length, stack);
            for (int i = 0; i < icons.length; i++) {
                Bitmap icon = icons[i];
                images.position(i);
                images.width(icon.getWidth());
                images.height(icon.getHeight());
                memPutAddress(images.address() + GLFWImage.PIXELS, icon.getAddress());
            }
            images.flip();
            glfwSetWindowIcon(mHandle, images);
        }
    }

    /**
     * Makes the OpenGL context of this window on the current calling thread.
     * It can only be on one thread at the same time.
     */
    public void makeCurrent() {
        glfwMakeContextCurrent(mHandle);
    }

    /**
     * Swaps the front and back buffers of the specified window when rendering
     * with OpenGL.
     */
    public void swapBuffers() {
        glfwSwapBuffers(mHandle);
    }

    /**
     * Sets the swap interval for the current OpenGL context.
     *
     * @param interval the interval
     */
    public void swapInterval(int interval) {
        glfwSwapInterval(interval);
    }

    /**
     * Returns the framebuffer width for this window in pixels.
     *
     * @return the framebuffer width
     */
    public int getWidth() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            glfwGetFramebufferSize(mHandle, w, null);
            return w.get(0);
        }
    }

    /**
     * Returns the framebuffer height for this window in pixels.
     *
     * @return the framebuffer height
     */
    public int getHeight() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer h = stack.mallocInt(1);
            glfwGetFramebufferSize(mHandle, null, h);
            return h.get(0);
        }
    }

    /**
     * Returns the x-coordinate of the top-left corner of this window
     * in virtual screen coordinate system.
     *
     * @return the x-coordinate of this window
     */
    public int getScreenY() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            glfwGetWindowPos(mHandle, w, null);
            return w.get(0);
        }
    }

    /**
     * Returns the y-coordinate of the top-left corner of this window
     * in virtual screen coordinate system.
     *
     * @return the y-coordinate of this window
     */
    public int getScreenX() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer h = stack.mallocInt(1);
            glfwGetWindowPos(mHandle, null, h);
            return h.get(0);
        }
    }

    /**
     * Returns the window width in virtual screen coordinates.
     *
     * @return window width
     */
    public int getScreenWidth() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            glfwGetWindowSize(mHandle, w, null);
            return w.get(0);
        }
    }

    /**
     * Returns the window height in virtual screen coordinates.
     *
     * @return window height
     */
    public int getScreenHeight() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer h = stack.mallocInt(1);
            glfwGetWindowSize(mHandle, null, h);
            return h.get(0);
        }
    }

    public void minimize() {
        glfwIconifyWindow(mHandle);
    }

    public void restore() {
        glfwRestoreWindow(mHandle);
    }

    public void maximize() {
        glfwMaximizeWindow(mHandle);
    }

    public void show() {
        glfwShowWindow(mHandle);
    }

    public void hide() {
        glfwHideWindow(mHandle);
    }

    /**
     * Destroys the window and remove all callbacks.
     */
    @Override
    public void close() {
        if (sWindows.remove(mHandle) == null) {
            return;
        }
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
