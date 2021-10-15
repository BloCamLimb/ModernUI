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

package icyllis.modernui.view;

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.graphics.GLCanvas;
import icyllis.modernui.platform.RenderCore;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;

/**
 * The top of a view hierarchy, implementing the needed protocol between View and
 * Window. There must also be a class handle events from Window to ViewRootImpl,
 * so methods are public for external calls. You may need to modify this class
 * to run your own stand-alone application.
 */
public final class ViewRootImpl implements ViewParent {

    private static final Marker MARKER = MarkerManager.getMarker("ViewRootImpl");

    private final AttachInfo mAttachInfo;
    private final Thread mThread;
    private final GLCanvas mCanvas;
    private final Handler mHandler;

    private final LinkedList<InputEvent> mInputEvents = new LinkedList<>();

    private boolean mTraversalScheduled;
    private boolean mWillDrawSoon;
    private boolean mIsDrawing;
    private boolean mHasDrawn;
    private boolean mLayoutRequested;
    private boolean mInvalidated;
    private boolean mKeepInvalidated;

    private boolean hasDragOperation;

    private View mView;
    private int mWidth;
    private int mHeight;

    /*private final int[] inBounds  = new int[]{0, 0, 0, 0};
    private final int[] outBounds = new int[4];*/

    public ViewRootImpl(GLCanvas canvas, Handler handler) {
        mAttachInfo = new AttachInfo(this);
        mThread = Thread.currentThread();
        mCanvas = canvas;
        mHandler = handler;
    }

    public void setView(@Nonnull View view) {
        if (mView == null) {
            mView = view;
            /*ViewGroup.LayoutParams params = view.getLayoutParams();
            // convert layout params
            if (!(params instanceof LayoutParams)) {
                params = new LayoutParams();
                view.setLayoutParams(params);
            }*/
            mAttachInfo.mRootView = view;
            view.assignParent(this);
            view.dispatchAttachedToWindow(mAttachInfo);
        }
    }

    public void setFrame(int width, int height) {
        if (width != mWidth || height != mHeight) {
            mWidth = width;
            mHeight = height;
            requestLayout();
        }
    }

    boolean startDragAndDrop(@Nonnull View view, @Nullable Object data, @Nullable View.DragShadow shadow, int flags) {
        /*if (master.dragEvent != null) {
            ModernUI.LOGGER.error(View.MARKER, "startDragAndDrop failed by another ongoing operation");
            return false;
        }*/

        /*Point center = new Point();
        if (shadow == null) {
            shadow = new View.DragShadow(view);
            if (view.isHovered()) {
                // default strategy
                center.x = (int) master.getViewMouseX(view);
                center.y = (int) master.getViewMouseY(view);
            } else {
                shadow.onProvideShadowCenter(center);
            }
        } else {
            shadow.onProvideShadowCenter(center);
        }*/

        /*master.dragEvent = new DragEvent(data);
        master.dragShadow = shadow;
        master.dragShadowCenter = center;*/

        hasDragOperation = true;

        //master.performDrag(DragEvent.ACTION_DRAG_STARTED);
        return true;
    }

    private void checkThread() {
        if (mThread != Thread.currentThread()) {
            throw new IllegalStateException("Not called from UI thread");
        }
    }

    @UiThread
    public void doTraversal() {
        if (mTraversalScheduled) {
            mTraversalScheduled = false;
            performTraversal();
        }
    }

    public void scheduleTraversal() {
        if (!mTraversalScheduled) {
            mTraversalScheduled = true;
        }
    }

    private void performTraversal() {
        final View host = mView;

        if (host == null)
            return;

        mWillDrawSoon = true;

        int width = mWidth;
        int height = mHeight;
        if (mLayoutRequested || width != host.getMeasuredWidth() || height != host.getMeasuredHeight()) {
            long startTime = RenderCore.timeNanos();

            int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.Mode.EXACTLY);
            int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.Mode.EXACTLY);

            host.measure(widthSpec, heightSpec);

            host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());

            ModernUI.LOGGER.info(MARKER, "Layout done in {} \u03bcs, window size: {}x{}",
                    (RenderCore.timeNanos() - startTime) / 1000.0f, width, height);
            mLayoutRequested = false;
        }

        mWillDrawSoon = false;

        if (mInvalidated) {
            mIsDrawing = true;
            mCanvas.reset(width, height);
            host.draw(mCanvas);
            mIsDrawing = false;
            if (mKeepInvalidated) {
                mKeepInvalidated = false;
            } else {
                mInvalidated = false;
            }
            mHasDrawn = true;
        }
    }

    public void enqueueInputEvent(@Nonnull InputEvent event) {
        mInputEvents.add(event);
    }

    public void doProcessInputEvents() {
        checkThread();
        if (mView != null) {
            InputEvent event;
            while ((event = mInputEvents.poll()) != null) {
                try {
                    if (event instanceof KeyEvent) {
                        processKeyEvent((KeyEvent) event);
                    } else {
                        processPointerEvent((MotionEvent) event);
                    }
                } finally {
                    event.recycle();
                }
            }
        } else {
            mInputEvents.clear();
        }
    }

    private boolean processKeyEvent(KeyEvent event) {
        return false;
    }

    private boolean processPointerEvent(MotionEvent event) {
        return mView.dispatchPointerEvent(event);
    }

    /*boolean onCursorPosEvent(LinkedList<View> route, double x, double y) {
        if (mView != null) {
            return mView.onCursorPosEvent(route, x, y);
        }
        return false;
    }

    boolean onMouseEvent(MotionEvent event) {
        if (mView != null) {
            final boolean handled = mView.onGenericMotionEvent(event);
            if (!handled && event.getAction() == MotionEvent.ACTION_MOVE) {
                mView.ensureMouseHoverExit();
            }
            return handled;
        }
        return false;
    }

    void ensureMouseHoverExit() {
        if (mView != null) {
            mView.ensureMouseHoverExit();
        }
    }*/

    public boolean hasDrawn() {
        boolean b = mHasDrawn;
        mHasDrawn = false;
        return b;
    }

    void performDragEvent(DragEvent event) {
        if (hasDragOperation) {

        }
    }

    public void tick() {
        if (mView != null) {
            mView.tick();
        }
    }

    void invalidate() {
        checkThread();
        mInvalidated = true;
        if (!mWillDrawSoon) {
            if (mIsDrawing) {
                mKeepInvalidated = true;
            }
            scheduleTraversal();
        }
    }

    void postTask(@Nonnull Runnable action, long delay) {
        mHandler.postTask(action, delay);
    }

    void removeTask(@Nonnull Runnable action) {
        mHandler.removeTask(action);
    }

    @Nullable
    @Override
    public ViewParent getParent() {
        return null;
    }

    /**
     * Request layout all views with layout mark in layout pass
     *
     * @see View#requestLayout()
     * @see View#forceLayout()
     */
    @Override
    public void requestLayout() {
        checkThread();
        mLayoutRequested = true;
        scheduleTraversal();
    }

    @Override
    public boolean canResolveLayoutDirection() {
        return true;
    }

    @Override
    public boolean isLayoutDirectionResolved() {
        return true;
    }

    @Override
    public int getLayoutDirection() {
        return View.LAYOUT_DIRECTION_RESOLVED_DEFAULT;
    }

    @Override
    public boolean canResolveTextDirection() {
        return true;
    }

    @Override
    public boolean isTextDirectionResolved() {
        return true;
    }

    @Override
    public int getTextDirection() {
        return View.TEXT_DIRECTION_RESOLVED_DEFAULT;
    }

    @Override
    public void childDrawableStateChanged(View child) {

    }

    public interface Handler {

        void postTask(@Nonnull Runnable action, long delay);

        void removeTask(@Nonnull Runnable action);
    }

    /*@Deprecated
    public static class LayoutParams extends ViewGroup.LayoutParams {

        *//*
     * X position for this window.  With the default gravity it is ignored.
     * When using {@link Gravity#LEFT} or {@link Gravity#RIGHT} it provides
     * an offset from the given edge.
     *//*
        public int x;

        *//*
     * Y position for this window.  With the default gravity it is ignored.
     * When using {@link Gravity#TOP} or {@link Gravity#BOTTOM} it provides
     * an offset from the given edge.
     *//*
        public int y;

        *//*
     * Placement of window within the screen as per {@link Gravity}.
     *
     * @see Gravity
     *//*
        public int gravity = Gravity.TOP_LEFT;

        public LayoutParams() {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }
    }*/
}
