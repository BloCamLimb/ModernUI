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

package icyllis.modernui.view.textinput;

import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.graphics.Matrix;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewRoot;
import icyllis.modernui.view.WindowManager;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLProperties;
import org.lwjgl.sdl.SDLVersion;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.sdl.SDL_TextEditingEvent;
import org.lwjgl.sdl.SDL_TextInputEvent;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import static icyllis.modernui.util.Log.LOGGER;

public final class TextInputManager {
    private static final boolean DEBUG = false;
    private static final Marker MARKER = MarkerFactory.getMarker("TextInputManager");

    /**
     * This is the view that should currently be served by an input method,
     * regardless of the state of setting that up.
     */
    @Nullable
    private View mServedView;

    /**
     * This is the next view that will be served by the input method, when
     * we get around to updating things.
     */
    @Nullable
    private View mNextServedView;

    /**
     * The latest {@link ViewRoot} that has, or most recently had, input method focus.
     *
     * <p>This value will be cleared when it becomes inactive and no longer has window focus.
     */
    @Nullable
    ViewRoot mCurViewRoot;

    boolean mCurViewRootWindowFocused;

    private EditorInfo mCurrentEditorInfo;

    private InputConnection mServedInputConnection;

    // compat
    private final boolean mSelectionUnitIsUTF16;

    public TextInputManager() {
        // On Windows, the sub-clause selection from SDL_TextEditingEvent was in UTF-16 units,
        // while Cocoa, X11, Wayland use UTF-32. This was fixed in SDL 3.4.22.
        // See https://github.com/libsdl-org/SDL/issues/12344
        int ver = SDLVersion.SDL_GetVersion();
        String videoDriver = SDLVideo.SDL_GetCurrentVideoDriver();
        mSelectionUnitIsUTF16 = ver < SDLVersion.SDL_VERSIONNUM(3, 4, 22) &&
                "windows".equals(videoDriver);
    }

    public void onPreWindowGainedFocus(ViewRoot viewRoot) {
        mCurViewRoot = viewRoot;
        mCurViewRootWindowFocused = true;
    }

    public void onPostWindowGainedFocus(@Nullable View viewForWindowFocus,
                                        WindowManager.@NonNull LayoutParams windowAttribute) {
        boolean forceFocus = false;
        // Update mNextServedView when focusedView changed.
        onViewFocusChanged(viewForWindowFocus, true);

        // Starting new input when the next focused view is same as served view but the
        // currently active connection (if any) is not associated with it.
        final boolean nextFocusIsServedView = mServedView == viewForWindowFocus;

        if (nextFocusIsServedView
                && !hasActiveInputConnectionInternal(viewForWindowFocus)) {
            forceFocus = true;
        }

        boolean checkFocusResult;
        if (mCurViewRoot == null) {
            return;
        }
        checkFocusResult = checkFocusInternal(forceFocus, mCurViewRoot);

        if (checkFocusResult) {
            // We need to restart input on the current focus view.  This
            // should be done in conjunction with telling the system service
            // about the window gaining focus, to help make the transition
            // smooth.
            if (startInputOnWindowFocusGainInternal()) {
                return;
            }
        }
    }

    public void onWindowLostFocus(@NonNull ViewRoot viewRoot) {
        if (mCurViewRoot == viewRoot) {
            mCurViewRootWindowFocused = false;
        }
    }

    @UiThread
    public void onViewFocusChanged(@Nullable View view, boolean hasFocus) {
        if (view == null || view.isTemporarilyDetached()) {
            return;
        }
        final ViewRoot viewRoot = view.getViewRoot();
        if (mCurViewRoot != viewRoot) {
            return;
        }
        if (viewRoot == null || !viewRoot.hasImeFocus() || !view.hasWindowFocus()) {
            return;
        }
        if (DEBUG) {
            LOGGER.debug(MARKER, "onViewFocusChangedInternal, view={}", dumpViewInfo(view));
        }

        // We don't need to track the next served view when the view lost focus here
        // because:
        // 1) The current view focus may be cleared temporary when in touch mode, closing
        //    input at this moment isn't the right way.
        // 2) We only care about the served view change when it focused, since changing
        //    input connection when the focus target changed is reasonable.
        // 3) Setting the next served view as null when no more served view should be
        //    handled in other special events (e.g. view detached from window or the window
        //    dismissed).
        if (hasFocus) {
            mNextServedView = view;
        }

        viewRoot.dispatchCheckFocus();
    }

    public void onScheduledCheckFocus(ViewRoot viewRoot) {
        if (!checkFocusInternal(false, viewRoot)) {
            return;
        }
        startInputOnWindowFocusGainInternal();
    }

    public void onViewDetachedFromWindow(View view, ViewRoot viewRoot) {
        if (mCurViewRoot != view.getViewRoot()) {
            return;
        }
        if (mNextServedView == view) {
            mNextServedView = null;
        }
        if (mServedView == view) {
            viewRoot.dispatchCheckFocus();
        }
    }

    void closeCurrentInput() {
        final ViewRoot viewRoot = mCurViewRoot;
        if (viewRoot == null) {
            return;
        }
        SDLKeyboard.SDL_StopTextInput(viewRoot.mStage.getWindow());
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public void checkFocus() {
        if (mCurViewRoot == null) {
            return;
        }
        if (!checkFocusInternal(false /* forceNewFocus */, mCurViewRoot)) {
            return;
        }
        startInputOnWindowFocusGainInternal();
    }

    /**
     * Check the next served view if needs to start input.
     */
    private boolean checkFocusInternal(boolean forceNewFocus, ViewRoot viewRoot) {
        if (mCurViewRoot != viewRoot) {
            return false;
        }
        if (mServedView == mNextServedView && !forceNewFocus) {
            return false;
        }
        if (DEBUG) {
            LOGGER.trace(MARKER, "checkFocus: view={} next={} force={}", mServedView, mNextServedView, forceNewFocus);
        }
        // Close the connection when no next served view coming.
        if (mNextServedView == null) {
            finishInput();
            closeCurrentInput();
            return false;
        }
        mServedView = mNextServedView;
        if (mServedInputConnection != null) {
            mServedInputConnection.finishComposingText();
        }
        SDLKeyboard.SDL_ClearComposition(viewRoot.mStage.getWindow());
        return true;
    }


    /**
     * Reset all of the state associated with a served view being connected
     * to an input method
     */
    private void clearConnection() {
        mCurrentEditorInfo = null;
        if (mServedInputConnection != null) {
            mServedInputConnection.closeConnection();
            mServedInputConnection = null;
        }
    }

    /**
     * Disconnect any existing input connection, clearing the served view.
     */
    void finishInput() {
        View clearedView = null;
        mNextServedView = null;
        if (mServedView != null) {
            clearedView = mServedView;
            mServedView = null;
        }
        if (clearedView != null) {
            if (DEBUG) {
                LOGGER.trace(MARKER, "FINISH INPUT: mServedView={}", dumpViewInfo(clearedView));
            }
            clearConnection();
        }
    }

    private boolean hasActiveInputConnectionInternal(@Nullable View view) {
        if (!hasServedByInputMethod(view)) {
            return false;
        }

        return mServedInputConnection != null;
    }

    private boolean startInputOnWindowFocusGainInternal() {
        mCurrentEditorInfo = null;
        return startInputInner();
    }

    private View getServedView() {
        return mCurViewRoot != null ? mServedView : null;
    }

    private boolean hasServedByInputMethod(View view) {
        final View servedView = getServedView();
        return (servedView == view
                || (servedView != null && servedView.checkInputConnectionProxy(view)));
    }

    private boolean startInputInner() {
        final View view;
        view = getServedView();

        // Make sure we have a window token for the served view.
        if (DEBUG) {
            LOGGER.trace(MARKER, "Starting input: view={}", dumpViewInfo(view), new Exception());
        }
        if (view == null) {
            if (DEBUG) LOGGER.trace(MARKER,"ABORT input: no served view!");
            return false;
        }

        final EditorInfo editorInfo = new EditorInfo();
        final InputConnection ic = view.onCreateInputConnection(editorInfo);

        mCurrentEditorInfo = editorInfo;
        final var previouslyServedConnection = mServedInputConnection;

        if (mServedInputConnection != null) {
            mServedInputConnection.closeConnection();
        }
        mServedInputConnection = ic;

        if (previouslyServedConnection == null && ic == null) {
            return false;
        }

        if (ic == null) {
            closeCurrentInput();
            return true;
        }

        int props = SDLProperties.SDL_CreateProperties();
        //TODO
        /*SDLProperties.SDL_SetNumberProperty(
                props,
                SDLKeyboard.SDL_PROP_TEXTINPUT_TYPE_NUMBER,

        )*/
        SDLKeyboard.SDL_StartTextInputWithProperties(
            mCurViewRoot.mStage.getWindow(), props
        );
        return true;
    }

    /**
     * Report the current cursor location in its window.
     * <p>
     * The bounds are the current line used for typing. Values are in local coordinates in pixels,
     * relative to view bounds. <var>cursor</var> is the cursor location relative to
     * <var>left</var> in pixels.
     */
    public void updateCursor(@NonNull View view,
                             float left, float top, float right, float bottom, float cursor) {
        checkFocus();

        if (!hasServedByInputMethod(view) || mCurrentEditorInfo == null) {
            return;
        }

        Matrix viewToScreenMatrix = new Matrix();
        view.transformMatrixToGlobal(viewToScreenMatrix);

        Rect b = new Rect();
        viewToScreenMatrix.mapRect(
                left, top, right, bottom, b
        );
        float[] p = new float[]{cursor, (top + bottom) / 2};
        viewToScreenMatrix.mapPoint(p);

        try (var stack = MemoryStack.stackPush()) {
            var rect = SDL_Rect.malloc(1, stack)
                    .x(b.left)
                    .y(b.top)
                    .w(b.width())
                    .h(b.height());
            assert mCurViewRoot != null;
            SDLKeyboard.SDL_SetTextInputArea(
                    mCurViewRoot.mStage.getWindow(),
                    rect,
                    Math.round(p[0] - b.left)
            );
        }
    }

    public void onTextEditing(SDL_TextEditingEvent e) {
        if (mCurViewRoot == null) {
            return;
        }
        InputConnection ic = mServedInputConnection;
        if (ic == null) {
            return;
        }
        long dWindow = SDLVideo.SDL_GetWindowFromID(e.windowID());
        if (dWindow != mCurViewRoot.mStage.getWindow()) {
            return;
        }
        String text = e.textString();
        if (text == null || text.isEmpty()) {
            // we can't distinguish between finish composing text and replace composing text,
            // we just consider it as replacement
            ic.setComposingText("", 0, 0);
            return;
        }
        // these are in code points
        int oSelStart = e.start();
        int oSelLength = e.length();
        // these are in code units
        int selStart;
        int selEnd;
        if (oSelStart >= 0) {
            if (mSelectionUnitIsUTF16) {
                selStart = MathUtil.clamp(oSelStart, 0, text.length());
                if (oSelLength > 0) {
                    selEnd = MathUtil.clamp(oSelStart + oSelLength, selStart, text.length());
                } else {
                    selEnd = selStart;
                }
            } else {
                // UTF32
                try {
                    selStart = text.offsetByCodePoints(0, oSelStart);
                } catch (IndexOutOfBoundsException ex) {
                    selStart = text.length();
                }
                if (oSelLength > 0) {
                    try {
                        selEnd = text.offsetByCodePoints(0, oSelStart + oSelLength);
                    } catch (IndexOutOfBoundsException ex) {
                        selEnd = text.length();
                    }
                    selEnd = Math.max(selEnd, selStart);
                } else {
                    selEnd = selStart;
                }
            }
        } else {
            selStart = selEnd = text.length();
        }
        ic.setComposingText(text, selStart, selEnd);
    }

    public void onTextInput(SDL_TextInputEvent e) {
        if (mCurViewRoot == null) {
            return;
        }
        InputConnection ic = mServedInputConnection;
        if (ic == null) {
            return;
        }
        long dWindow = SDLVideo.SDL_GetWindowFromID(e.windowID());
        if (dWindow != mCurViewRoot.mStage.getWindow()) {
            return;
        }
        ic.commitText(e.textString());
    }

    public static String dumpViewInfo(@Nullable View view) {
        if (view == null) {
            return "null";
        }

        return view +
                ",focus=" + view.hasFocus() +
                ",windowFocus=" + view.hasWindowFocus() +
                ",window=" + view.getViewRoot() +
                ",stage=" + (view.getViewRoot() != null ? view.getViewRoot().mStage : null) +
                ",temporaryDetach=" + view.isTemporarilyDetached() +
                ",hasImeFocus=" + (view.getViewRoot() != null && view.getViewRoot().hasImeFocus());
    }
}
