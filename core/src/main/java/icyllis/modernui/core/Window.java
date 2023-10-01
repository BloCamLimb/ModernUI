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
import icyllis.modernui.util.LongSparseArray;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeType;

import javax.annotation.concurrent.NotThreadSafe;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Represents a GLFW platform window, with 3D API surface attached to it.
 * <p>
 * There's must be a main window in Modern UI, any other windows share
 * the same 3D context of the main one. Each window have its Activity Context
 * and its own presentation screen. Any sub-window (e.g. tooltips, toasts) will
 * not exceed its platform window area.
 * <p>
 * Most window methods must be only called on main thread.
 */
@NotThreadSafe
public class Window implements AutoCloseable {

    private static final LongSparseArray<Window> sWindows = new LongSparseArray<>();

    /**
     * End of types of system windows.
     */
    public static final int LAST_SYSTEM_WINDOW = 2999;

    // for directly access in subclasses
    protected final long mHandle;

    public Window(long handle) {
        Core.checkMainThread();
        mHandle = handle;
        if (sWindows.put(handle, this) != null) {
            throw new IllegalStateException("Duplicated window: 0x" + Long.toHexString(handle));
        }
    }

    @Nullable
    public static Window get(@NativeType("GLFWwindow *") long handle) {
        return sWindows.get(handle);
    }

    /**
     * Get the pointer of this window in the GLFW.
     *
     * @return the handle of the window
     */
    @NativeType("GLFWwindow *")
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
    public void setIcon(@Nullable Bitmap... icons) {
        if (icons == null || icons.length == 0) {
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
        //noinspection resource
        Window w = sWindows.remove(mHandle);
        if (w == null) {
            return;
        }
        assert w == this;
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
