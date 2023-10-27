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
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.Selection;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.EditText;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * The activity window is the default implementation for almost everything.
 */
public final class ActivityWindow extends Window {

    private static volatile ActivityWindow sMainWindow;

    private int mScreenX;
    private int mScreenY;
    private int mScreenWidth;
    private int mScreenHeight;

    private int mWidth;
    private int mHeight;

    private volatile ViewRoot mRoot;

    private int mButtonState;

    private final StringBuilder mCharInputBuffer = new StringBuilder();
    private final Runnable mCommitCharInput = this::commitCharInput;

    ActivityWindow(long handle) {
        super(handle);
        sMainWindow = this;

        // set callbacks
        glfwSetWindowPosCallback(handle, this::onPosCallback);
        glfwSetWindowSizeCallback(handle, this::onSizeCallback);
        glfwSetWindowRefreshCallback(handle, this::onRefreshCallback);
        glfwSetWindowFocusCallback(handle, this::onFocusCallback);
        glfwSetWindowIconifyCallback(handle, this::onMinimizeCallback);
        glfwSetWindowMaximizeCallback(handle, this::onMaximizeCallback);
        glfwSetFramebufferSizeCallback(handle, this::onFramebufferSizeCallback);
        glfwSetWindowContentScaleCallback(handle, this::onContentScaleCallback);

        glfwSetKeyCallback(handle, this::onKeyCallback);
        glfwSetCharCallback(handle, this::onCharCallback);
        glfwSetMouseButtonCallback(handle, this::onMouseButtonCallback);
        glfwSetCursorPosCallback(handle, this::onCursorPosCallback);
        glfwSetScrollCallback(handle, this::onScrollCallback);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);

            glfwGetWindowPos(handle, w, h);
            mScreenX = w.get(0);
            mScreenY = h.get(0);

            glfwGetWindowSize(handle, w, h);
            mScreenWidth = w.get(0);
            mScreenHeight = h.get(0);

            glfwGetFramebufferSize(handle, w, h);
            mWidth = w.get(0);
            mHeight = h.get(0);
        }
    }

    /**
     * Creates the main window and initialize the view system. Hints are set before.
     *
     * @param title  the initial title
     * @param width  the window width in virtual screen coordinates
     * @param height the window height in virtual screen coordinates
     * @return the main window
     */
    @NonNull
    public static ActivityWindow createMainWindow(@NonNull String title, int width, int height) {
        return createMainWindow(title, width, height, null);
    }

    @NonNull
    public static ActivityWindow createMainWindow(@NonNull String title, int width, int height,
                                                  @Nullable Monitor monitor) {
        Core.checkMainThread();
        if (sMainWindow != null) {
            throw new IllegalStateException("Multiple main windows");
        }
        long handle = glfwCreateWindow(width, height, title, monitor == null ? NULL : monitor.getHandle(), NULL);
        if (handle == NULL) {
            throw new IllegalStateException("Failed to create window");
        }
        return new ActivityWindow(handle);
    }

    private void onPosCallback(long w, int xPos, int yPos) {
        mScreenX = xPos;
        mScreenY = yPos;
    }

    @Override
    public int getScreenX() {
        return mScreenX;
    }

    @Override
    public int getScreenY() {
        return mScreenY;
    }

    private void onSizeCallback(long w, int width, int height) {
        mScreenWidth = width;
        mScreenHeight = height;
    }

    @Override
    public int getScreenWidth() {
        return mScreenWidth;
    }

    @Override
    public int getScreenHeight() {
        return mScreenHeight;
    }

    private void onRefreshCallback(long w) {
    }

    private void onFocusCallback(long w, boolean focused) {
    }

    private void onMinimizeCallback(long w, boolean minimized) {
    }

    private void onMaximizeCallback(long w, boolean maximized) {
    }

    private void onFramebufferSizeCallback(long w, int width, int height) {
        mWidth = width;
        mHeight = height;
        if (mRoot != null) {
            mRoot.mHandler.post(() -> mRoot.setFrame(mWidth, mHeight));
        }
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    private void onContentScaleCallback(long w, float xScale, float yScale) {
    }

    public void center(@NonNull Monitor monitor) {
        VideoMode mode = monitor.getCurrentMode();
        glfwSetWindowPos(mHandle, (mode.getWidth() - mScreenWidth) / 2 + monitor.getXPos(),
                (mode.getHeight() - mScreenHeight) / 2 + monitor.getYPos());
    }

    public void install(@NonNull ViewRoot root) {
        mRoot = root;
        root.setFrame(mWidth, mHeight);
    }

    private void onKeyCallback(long w, int key, int scancode, int action, int mods) {
        if (mRoot != null) {
            KeyEvent keyEvent = KeyEvent.obtain(Core.timeNanos(),
                    action == GLFW_RELEASE ? KeyEvent.ACTION_UP : KeyEvent.ACTION_DOWN,
                    key, 0, mods, scancode, 0);
            mRoot.enqueueInputEvent(keyEvent);
        }
    }

    private void onCharCallback(long w, int codepoint) {
        if (mRoot == null) {
            return;
        }
        // block NUL and DEL character
        if (codepoint == 0 || codepoint == 0x007F) {
            return;
        }
        mCharInputBuffer.appendCodePoint(codepoint);
        Core.postOnMainThread(mCommitCharInput);
    }

    private void commitCharInput() {
        if (mCharInputBuffer.isEmpty()) {
            return;
        }
        final String input = mCharInputBuffer.toString();
        mCharInputBuffer.setLength(0);
        Message msg = Message.obtain(mRoot.mHandler, () -> {
            if (mRoot != null && mRoot.getView().findFocus() instanceof EditText text) {
                final Editable content = text.getText();
                int selStart = text.getSelectionStart();
                int selEnd = text.getSelectionEnd();
                if (selStart >= 0 && selEnd >= 0) {
                    Selection.setSelection(content, Math.max(selStart, selEnd));
                    content.replace(Math.min(selStart, selEnd), Math.max(selStart, selEnd), input);
                }
            }
        });
        msg.setAsynchronous(true);
        msg.sendToTarget();
    }

    private void onMouseButtonCallback(long w, int button, int action, int mods) {
        if (mRoot == null) {
            return;
        }
        double cursorX;
        double cursorY;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer x = stack.mallocDouble(1);
            DoubleBuffer y = stack.mallocDouble(1);
            glfwGetCursorPos(w, x, y);
            cursorX = x.get(0);
            cursorY = y.get(0);
        }
        final long now = Core.timeNanos();
        float x = (float) (cursorX * mWidth / mScreenWidth);
        float y = (float) (cursorY * mHeight / mScreenHeight);
        int buttonState = 0;
        for (int i = 0; i < 5; i++) {
            if (glfwGetMouseButton(w, i) == GLFW_PRESS) {
                buttonState |= 1 << i;
            }
        }
        mButtonState = buttonState;
        int actionButton = 1 << button;
        action = action == GLFW_PRESS ?
                MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP;
        if ((action == MotionEvent.ACTION_DOWN && (buttonState ^ actionButton) == 0)
                || (action == MotionEvent.ACTION_UP && buttonState == 0)) {
            MotionEvent ev = MotionEvent.obtain(now, action, actionButton,
                    x, y, mods, buttonState, 0);
            mRoot.enqueueInputEvent(ev);
        }
    }

    private void onCursorPosCallback(long w, double cursorX, double cursorY) {
        if (mRoot == null) {
            return;
        }
        final long now = Core.timeNanos();
        float x = (float) (cursorX * mWidth / mScreenWidth);
        float y = (float) (cursorY * mHeight / mScreenHeight);
        MotionEvent event = MotionEvent.obtain(now, MotionEvent.ACTION_HOVER_MOVE,
                x, y, 0);
        mRoot.enqueueInputEvent(event);
        if (mButtonState > 0) {
            event = MotionEvent.obtain(now, MotionEvent.ACTION_MOVE, 0, x, y, 0, mButtonState, 0);
            mRoot.enqueueInputEvent(event);
        }
    }

    private void onScrollCallback(long w, double deltaX, double deltaY) {
        if (mRoot == null) {
            return;
        }
        double cursorX;
        double cursorY;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer x = stack.mallocDouble(1);
            DoubleBuffer y = stack.mallocDouble(1);
            glfwGetCursorPos(w, x, y);
            cursorX = x.get(0);
            cursorY = y.get(0);
        }
        final long now = Core.timeNanos();
        float x = (float) (cursorX * mWidth / mScreenWidth);
        float y = (float) (cursorY * mHeight / mScreenHeight);
        MotionEvent event = MotionEvent.obtain(now, MotionEvent.ACTION_SCROLL,
                x, y, 0);
        event.setAxisValue(MotionEvent.AXIS_HSCROLL, (float) deltaX);
        event.setAxisValue(MotionEvent.AXIS_VSCROLL, (float) deltaY);
        mRoot.enqueueInputEvent(event);
    }
}
