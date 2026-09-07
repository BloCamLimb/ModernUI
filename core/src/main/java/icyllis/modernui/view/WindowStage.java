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

package icyllis.modernui.view;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.renderer.RenderPipeline;
import icyllis.modernui.renderer.WindowSurface;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLKeycode;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDL_KeyboardEvent;
import org.lwjgl.sdl.SDL_MouseButtonEvent;
import org.lwjgl.sdl.SDL_MouseMotionEvent;
import org.lwjgl.sdl.SDL_WindowEvent;

import java.util.ArrayList;

/**
 * A platform window acts as the parent of all virtual windows.
 * Due to its ability to run without a desktop environment, this can represent the
 * entire display/monitor and is therefore also known as a Screen.
 *
 * @hidden
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
@ApiStatus.Internal
public final class WindowStage implements Stage {

    // pointer to SDL_Window
    private long mWindow;

    // position and size in screen coordinates
    private int mScreenX;
    private int mScreenY;
    private int mScreenWidth;
    private int mScreenHeight;

    // size in pixels
    private int mWidth;
    private int mHeight;
    // keep sync with (0, 0, mWidth, mHeight)
    final Rect mFrame = new Rect();

    // Z-ordered
    ArrayList<ViewRoot> mRoots = new ArrayList<>();

    ViewRoot mFocused;


    ViewRoot mTouchTarget;
    ViewRoot mHoverTarget;

    Compositor mCompositor;

    WindowSurface mSurface;


    /**
     * Returns the framebuffer width for this window in pixels.
     *
     * @return the framebuffer width
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Returns the framebuffer height for this window in pixels.
     *
     * @return the framebuffer height
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Returns the y-coordinate of the top-left corner of this window
     * in virtual screen coordinate system.
     *
     * @return the y-coordinate of this window
     */
    public int getScreenX() {
        return mScreenX;
    }

    /**
     * Returns the x-coordinate of the top-left corner of this window
     * in virtual screen coordinate system.
     *
     * @return the x-coordinate of this window
     */
    public int getScreenY() {
        return mScreenY;
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

    public void onKeyboardEvent(@NonNull SDL_KeyboardEvent event) {
        if (mFocused != null) {
            int keycode = event.key();

            KeyEvent ev = KeyEvent.obtain(
                    event.timestamp(),
                    event.down() ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP,
                    event.scancode(),
                    event.mod() & 0xFFFF,
                    event.which(),
                    event.raw() & 0xFFFF,
                    event.repeat() ? KeyEvent.FLAG_REPEAT : 0,
                    (keycode & (SDLKeycode.SDLK_EXTENDED_MASK | SDLKeycode.SDLK_SCANCODE_MASK)) == 0 ? keycode : 0
            );

            mFocused.enqueueInputEvent(ev);
        }
    }

    public void onMouseMotion(@NonNull SDL_MouseMotionEvent event) {

        float x = event.x() * mWidth / mScreenWidth;
        float y = event.y() * mHeight / mScreenHeight;

        ViewRoot hovered = null;

        for (int i = mRoots.size() - 1; i >= 0; i--) {
            ViewRoot root = mRoots.get(i);
            int flags = root.mWindowAttributes.flags;

            if ((flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0) {
                continue;
            }

            boolean inside = root.mWinFrame.contains((int) x, (int) y);
            if (inside) {
                hovered = root;
                break;
            }
        }

        if (mHoverTarget != hovered) {
            if (mHoverTarget != null) {
                MotionEvent ev = MotionEvent.obtain(
                        event.timestamp(), MotionEvent.ACTION_HOVER_EXIT,
                        x - mHoverTarget.mWinFrame.left,
                        y - mHoverTarget.mWinFrame.top,
                        0
                );
                mHoverTarget.enqueueInputEvent(ev);
            }
            if (hovered != null) {
                MotionEvent ev = MotionEvent.obtain(
                        event.timestamp(), MotionEvent.ACTION_HOVER_ENTER,
                        x - hovered.mWinFrame.left,
                        y - hovered.mWinFrame.top,
                        0
                );
                hovered.enqueueInputEvent(ev);
            }
            mHoverTarget = hovered;
        }

        if (hovered != null) {
            MotionEvent ev = MotionEvent.obtain(
                    event.timestamp(), MotionEvent.ACTION_HOVER_MOVE,
                    x - hovered.mWinFrame.left,
                    y - hovered.mWinFrame.top,
                    0
            );
            hovered.enqueueInputEvent(ev);

            int buttonState = event.state();
            if (buttonState != 0) {
                ev = MotionEvent.obtain(
                        event.timestamp(), MotionEvent.ACTION_MOVE,
                        0,
                        x - hovered.mWinFrame.left,
                        y - hovered.mWinFrame.top,
                        0,
                        buttonState,
                        0
                );
                hovered.enqueueInputEvent(ev);
            }
        }
    }

    public void onMouseButton(@NonNull SDL_MouseButtonEvent event) {

        float x = event.x() * mWidth / mScreenWidth;
        float y = event.y() * mHeight / mScreenHeight;

        boolean isDown = event.down();

        if (isDown) {
            ViewRoot touched = null;
            ViewRoot focused = null;

            for (int i = mRoots.size() - 1; i >= 0; i--) {
                ViewRoot root = mRoots.get(i);
                int flags = root.mWindowAttributes.flags;

                if ((flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0) {
                    continue;
                }

                boolean inside = root.mWinFrame.contains((int) x, (int) y);
                if (inside ||
                        root.mWindowAttributes.isModal()) {

                    if ((flags & WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) == 0) {
                        focused = root;
                    }

                    touched = root;
                    break;
                }

                if ((flags & WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH) != 0) {
                    MotionEvent ev = MotionEvent.obtain(
                            event.timestamp(), MotionEvent.ACTION_OUTSIDE,
                            x - root.mWinFrame.left,
                            y - root.mWinFrame.top,
                            0
                    );
                    root.enqueueInputEvent(ev);
                }
            }

            if (focused != null) {
                setFocused(focused);
            }
            mTouchTarget = touched;
        }

        if (mTouchTarget != null) {

            int actionButton = event.button();
            int buttonState = SDLMouse.SDL_GetMouseState(null, null);

            int mouseAction = isDown ?
                    MotionEvent.ACTION_BUTTON_PRESS : MotionEvent.ACTION_BUTTON_RELEASE;
            int touchAction = isDown ?
                    MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP;

            int mods = SDLKeyboard.SDL_GetModState() & 0xFFFF;

            if ((touchAction == MotionEvent.ACTION_DOWN && (buttonState ^ actionButton) == 0)
                    || (touchAction == MotionEvent.ACTION_UP && buttonState == 0)) {
                MotionEvent ev = MotionEvent.obtain(
                        event.timestamp(), touchAction,
                        actionButton,
                        x - mTouchTarget.mWinFrame.left,
                        y - mTouchTarget.mWinFrame.top,
                        mods, buttonState, 0
                );
                mTouchTarget.enqueueInputEvent(ev);
            }

            MotionEvent ev = MotionEvent.obtain(
                    event.timestamp(), mouseAction,
                    actionButton,
                    x - mTouchTarget.mWinFrame.left,
                    y - mTouchTarget.mWinFrame.top,
                    mods, buttonState, 0
            );
            mTouchTarget.enqueueInputEvent(ev);
        }
    }

    public void onWindowEvent(@NonNull SDL_WindowEvent event) {
        int type = event.type();
        switch (type) {
            case SDLEvents.SDL_EVENT_WINDOW_MOVED -> {

                mScreenX = event.data1();
                mScreenY = event.data2();
            }

            case SDLEvents.SDL_EVENT_WINDOW_RESIZED -> {

                mScreenWidth = event.data1();
                mScreenHeight = event.data2();
            }

            case SDLEvents.SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED -> {

                mWidth = event.data1();
                mHeight = event.data2();

                handleResize();
            }
        }
    }

    public void handleResize() {
        mFrame.set(0, 0, mWidth, mHeight);

        for (int i = 0; i < mRoots.size(); i++) {
            mRoots.get(i).requestLayout();
        }
    }

    public void setFocused(@Nullable ViewRoot newFocus) {
        if (mFocused == newFocus) {
            return;
        }

        if (newFocus == null) {
            mFocused = null;
            return;
        }

        int i = mRoots.indexOf(newFocus);

        // bring to front
        int j = i;
        for (; j + 1 < mRoots.size(); j++) {
            ViewRoot root = mRoots.get(j + 1);
            if (newFocus.mWindowAttributes.type != root.mWindowAttributes.type) {
                break;
            }
        }

        if (i != j) {
            mRoots.remove(i);
            mRoots.add(j, newFocus);
            //TODO notify redraw
        }

        mFocused = newFocus;
    }

    public void addWindow(@NonNull View view, @NonNull WindowManager.LayoutParams params) {

    }

    public void updateWindowLayout(@NonNull View view, @NonNull WindowManager.LayoutParams params) {

    }

    public void removeWindow(@NonNull View view, boolean immediate) {

    }

    public void scheduleComposition() {
        mCompositor.scheduleComposition();
    }

    public RenderPipeline getRenderPipeline() {
        return mCompositor.getRenderPipeline();
    }

    public void markForComposition() {

    }
}
