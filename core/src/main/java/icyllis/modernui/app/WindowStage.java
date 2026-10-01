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

package icyllis.modernui.app;

import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.core.SamplingOptions;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.granite.GraniteSurface;
import icyllis.arc3d.sketch.NullSurface;
import icyllis.arc3d.sketch.Surface;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.renderer.WindowSurface;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.LayerSettings;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.Stage;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.view.ViewRoot;
import icyllis.modernui.view.WindowManager;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLKeycode;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_KeyboardEvent;
import org.lwjgl.sdl.SDL_MouseButtonEvent;
import org.lwjgl.sdl.SDL_MouseMotionEvent;
import org.lwjgl.sdl.SDL_WindowEvent;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.BiFunction;

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

    // Z-ordered
    ArrayList<ViewRoot> mRoots = new ArrayList<>();
    private final HashSet<View> mDyingViews = new HashSet<>();

    ViewRoot mFocused;


    ViewRoot mTouchTarget;
    ViewRoot mHoverTarget;

    Compositor mCompositor;

    private WindowSurface mSurface;
    private boolean mMarkForComposition = false;
    private boolean mMarkForSurfaceReconfigure = true;

    public WindowStage(long window) {
        mWindow = window;
        try (var stack = MemoryStack.stackPush()) {
            var x = stack.mallocInt(1);
            var y = stack.mallocInt(1);

            SDLVideo.SDL_GetWindowSizeInPixels(window, x, y);
            mWidth = x.get(0);
            mHeight = y.get(0);

            SDLVideo.SDL_GetWindowSize(window, x, y);
            mScreenWidth = x.get(0);
            mScreenHeight = y.get(0);

            SDLVideo.SDL_GetWindowPosition(window, x, y);
            mScreenX = x.get(0);
            mScreenY = y.get(0);
        }
    }

    @NativeType("SDL_Window *")
    public long getWindow() {
        return mWindow;
    }

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

    public void setSurface(WindowSurface surface) {
        mSurface = surface;
    }

    public WindowSurface getSurface() {
        return mSurface;
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

    public void onMouseMotionEvent(@NonNull SDL_MouseMotionEvent event) {

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

    public void onMouseButtonEvent(@NonNull SDL_MouseButtonEvent event) {

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

        for (int i = 0; i < mRoots.size(); i++) {
            var root = mRoots.get(i);
            root.mWinFrame.set(0, 0, mWidth, mHeight);
            root.requestLayout();
        }

        mMarkForSurfaceReconfigure = true;
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

    public void show() {
        SDLVideo.SDL_ShowWindow(mWindow);
    }

    public void addWindow(@NonNull BiFunction<Context, Stage, ViewRoot> factory,
                          @NonNull View view, @NonNull WindowManager.LayoutParams params) {
        int index = findView(view);
        if (index >= 0) {
            if (mDyingViews.contains(view)) {
                // Don't wait for MSG_DIE to make it's way through root's queue.
                mRoots.get(index).doDie();
            } else {
                throw new IllegalStateException("View " + view
                        + " has already been added to the window manager.");
            }
            // The previous removeView() had not completed executing. Now it has.
        }
        var root = factory.apply(view.getContext(), this);
        view.setLayoutParams(params);
        mRoots.add(root);
        root.setView(view, params);
    }

    public int findView(View view) {
        for (int i = 0; i < mRoots.size(); i++) {
            var root = mRoots.get(i);
            if (view.equals(root.getView())) {
                return i;
            }
        }
        return -1;
    }

    public void removeWindow(@NonNull View view, boolean immediate) {
        for (int i = 0; i < mRoots.size(); i++) {
            var root = mRoots.get(i);
            if (view.equals(root.getView())) {
                boolean deferred = root.die(immediate);
                if (deferred) {
                    mDyingViews.add(view);
                }
                return;
            }
        }
    }

    @Override
    public void doRemoveView(ViewRoot root) {
        final int index = mRoots.indexOf(root);
        if (index >= 0) {
            mRoots.remove(index);
            final View view = root.getView();
            mDyingViews.remove(view);
        }

        for (int i = mRoots.size() - 1; i >= 0; i--) {
            ViewRoot r = mRoots.get(i);
            int flags = r.mWindowAttributes.flags;

            if ((flags & WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) == 0) {
                setFocused(r);
                break;
            }
        }
    }

    @Override
    public void addView(@NonNull View view, @NonNull ViewGroup.LayoutParams params) {
        if (!(params instanceof LayoutParams)) {
            throw new IllegalArgumentException("Params must be WindowManager.LayoutParams");
        }
        addWindow(ViewRoot::new, view, (LayoutParams) params);
    }

    @Override
    public void updateViewLayout(@NonNull View view, @NonNull ViewGroup.LayoutParams params) {
        if (!(params instanceof LayoutParams)) {
            throw new IllegalArgumentException("Params must be WindowManager.LayoutParams");
        }
        var wparams = (LayoutParams) params;
        view.setLayoutParams(wparams);
        for (int i = 0; i < mRoots.size(); i++) {
            var root = mRoots.get(i);
            if (view.equals(root.getView())) {
                root.setLayoutParams(wparams);
                return;
            }
        }
        throw new IllegalArgumentException("View=" + view + " not attached to window manager");
    }

    @Override
    public void removeView(@NonNull View view) {
        removeWindow(view, false);
    }

    @Override
    public void postComposition() {
        if (mCompositor != null) {
            mCompositor.postComposition();
        }
    }

    @Override
    public void markForComposition() {
        mMarkForComposition = true;
    }

    public boolean checkForComposition() {
        if (mMarkForComposition) {
            mMarkForComposition = false;
            return true;
        }
        return false;
    }

    public boolean checkForSurfaceReconfigure() {
        if (mMarkForSurfaceReconfigure) {
            mMarkForSurfaceReconfigure = false;
            return true;
        }
        return false;
    }

    void doComposition(
            icyllis.arc3d.sketch.Canvas canvas) {
        ArrayList<@SharedPtr LayerSettings> layers = new ArrayList<>();
        for (int i = 0; i < mRoots.size(); i++) {
            mRoots.get(i).collectCompositionLayers(layers);
        }

        for (int i = 0; i < layers.size(); i++) {
            var layer = layers.get(i);

            @SharedPtr
            icyllis.arc3d.sketch.Image image;
            if (layer.sourceImage != null) {
                image = layer.sourceImage; // move
            } else if (layer.sourceSurf instanceof GraniteSurface graniteSurface) {
                //TODO delete this when Arc3D is updated
                graniteSurface.flush();
                image = graniteSurface.asImage();
            } else {
                image = layer.sourceSurf.makeImageSnapshot();
            }
            if (image != null) {
                canvas.drawImage(image,
                        layer.offsetX, layer.offsetY,
                        SamplingOptions.NEAREST, null);
                image.unref();
            }

            if (layer.sourceSurf != null) {
                layer.sourceSurf.unref();
            }
        }
    }

    @Override
    public Surface createSurface(ImageInfo info) {
        @SharedPtr
        Surface surf = null;
        if (mCompositor != null) {
            surf = mCompositor.createSurface(info);
        }
        if (surf != null) {
            return surf;
        }

        surf = NullSurface.make(info.width(), info.height());

        return surf;
    }
}
