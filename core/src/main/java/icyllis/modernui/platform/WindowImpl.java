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

public final class WindowImpl extends Window {

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

    private boolean mNeedRefresh;

    WindowImpl(long handle, @Nonnull State state, boolean borderless, boolean fullscreen) {
        super(handle);

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
            ModernUI.LOGGER.info(
                    MarkerManager.getMarker("Input"), "OnKeyEvent{action: {}, key: {}}", action, keycode);
            if (action == GLFW_PRESS && (mods & GLFW_MOD_CONTROL) != 0 && keycode == GLFW_KEY_V) {
                ModernUI.LOGGER.info("Paste: {}", Clipboard.getText());
            }
        });
        glfwSetCharCallback(handle, (window, ch) -> {
            ModernUI.LOGGER.info(MarkerManager.getMarker("Input"), "InputChar: {}", ch);
        });

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

        mNeedRefresh = true;
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
        if (!mNeedRefresh) {
            mNeedRefresh = true;
            RenderCore.interrupt();
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

    @Override
    public void makeCurrent() {
        glfwMakeContextCurrent(mHandle);
    }

    @Override
    public boolean shouldClose() {
        return glfwWindowShouldClose(mHandle);
    }

    @Override
    public boolean isRefreshNeeded() {
        return mNeedRefresh;
    }

    @Override
    public void swapBuffers() {
        glfwSwapBuffers(mHandle);
        mNeedRefresh = false;
    }

    @Override
    public void destroy() {
        super.destroy();
        Callbacks.glfwFreeCallbacks(mHandle);
        glfwDestroyWindow(mHandle);
    }

    /**
     * Returns the x-coordinate of the top-left corner of this window
     * in virtual screen coordinate system.
     *
     * @return the x-coordinate of this window
     */
    @Override
    public int getXPos() {
        return mXPos;
    }

    /**
     * Returns the y-coordinate of the top-left corner of this window
     * in virtual screen coordinate system.
     *
     * @return the y-coordinate of this window
     */
    @Override
    public int getYPos() {
        return mYPos;
    }

    /**
     * Returns the framebuffer width for this window in pixels.
     *
     * @return framebuffer width
     */
    @Override
    public int getWidth() {
        return mFramebufferWidth;
    }

    /**
     * Returns the framebuffer height for this window in pixels.
     *
     * @return framebuffer height
     */
    @Override
    public int getHeight() {
        return mFramebufferHeight;
    }

    @Override
    public int getScreenWidth() {
        return mScreenWidth;
    }

    @Override
    public int getScreenHeight() {
        return mScreenHeight;
    }

}
