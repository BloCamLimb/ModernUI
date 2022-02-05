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

import icyllis.modernui.animation.LayoutTransition;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.core.ArchCore;
import icyllis.modernui.core.Handler;
import icyllis.modernui.core.Looper;
import icyllis.modernui.core.Message;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.math.Rect;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The top of a view hierarchy, implementing the needed protocol between View and the Window.
 */
@ApiStatus.Internal
public abstract class ViewRoot implements ViewParent, AttachInfo.Callbacks {

    protected static final Marker MARKER = MarkerManager.getMarker("ViewRoot");

    private final AttachInfo mAttachInfo;

    private final ConcurrentLinkedQueue<InputEvent> mInputEvents = new ConcurrentLinkedQueue<>();

    private boolean mTraversalScheduled;
    private boolean mWillDrawSoon;
    private boolean mIsDrawing;
    private boolean mLayoutRequested;
    private boolean mInvalidated;
    private boolean mKeepInvalidated;

    private boolean hasDragOperation;

    protected final Object mRenderLock = new Object();
    protected boolean mRedrawn;

    protected View mView;
    private int mWidth;
    private int mHeight;

    public final Handler mHandler;

    private ArrayList<LayoutTransition> mPendingTransitions;

    /*private final int[] inBounds  = new int[]{0, 0, 0, 0};
    private final int[] outBounds = new int[4];*/

    protected ViewRoot() {
        ArchCore.initUiThread();
        mHandler = new Handler(Looper.myLooper(), this::handleMessage);
        mAttachInfo = new AttachInfo(this, mHandler, this);

        try {
            Class.forName("icyllis.modernui.text.BoringLayout");
            Class.forName("icyllis.modernui.text.CharArrayIterator");
            Class.forName("icyllis.modernui.text.CharSequenceIterator");
            Class.forName("icyllis.modernui.text.Directions");
            Class.forName("icyllis.modernui.text.DynamicLayout");
            Class.forName("icyllis.modernui.text.GlyphManager");
            Class.forName("icyllis.modernui.text.GraphemeBreak");
            Class.forName("icyllis.modernui.text.Layout");
            Class.forName("icyllis.modernui.text.LayoutCache");
            Class.forName("icyllis.modernui.text.LayoutPiece");
            Class.forName("icyllis.modernui.text.LineBreaker");
            Class.forName("icyllis.modernui.text.MeasuredParagraph");
            Class.forName("icyllis.modernui.text.MeasuredText");
            Class.forName("icyllis.modernui.text.PrecomputedText");
            Class.forName("icyllis.modernui.text.Selection");
            Class.forName("icyllis.modernui.text.SpannableString");
            Class.forName("icyllis.modernui.text.SpannableStringBuilder");
            Class.forName("icyllis.modernui.text.SpannableStringInternal");
            Class.forName("icyllis.modernui.text.StaticLayout");
            Class.forName("icyllis.modernui.text.TabStops");
            Class.forName("icyllis.modernui.text.TextDirectionHeuristics");
            Class.forName("icyllis.modernui.text.TextLine");
            Class.forName("icyllis.modernui.text.TextPaint");
            Class.forName("icyllis.modernui.text.TextUtils");
            Class.forName("icyllis.modernui.text.Typeface");

            Class.forName("icyllis.modernui.widget.Editor");
            Class.forName("icyllis.modernui.widget.LinearLayout");
            Class.forName("icyllis.modernui.widget.ScrollView");
            Class.forName("icyllis.modernui.widget.TextView");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean handleMessage(@Nonnull Message msg) {
        return true;
    }

    public void setView(@Nonnull View view) {
        synchronized (this) {
            if (mView == null) {
                mView = view;
            /*ViewGroup.LayoutParams params = view.getLayoutParams();
            // convert layout params
            if (!(params instanceof LayoutParams)) {
                params = new LayoutParams();
                view.setLayoutParams(params);
            }*/
                mAttachInfo.mRootView = view;
                mAttachInfo.mWindowVisibility = View.VISIBLE;
                view.assignParent(this);
                view.dispatchAttachedToWindow(mAttachInfo);
            }
        }
    }

    protected void setFrame(int width, int height) {
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

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        return false;
    }

    @UiThread
    protected void doTraversal() {
        if (mTraversalScheduled) {
            mTraversalScheduled = false;
            performTraversal();
        }
    }

    public void scheduleTraversals() {
        if (!mTraversalScheduled) {
            mTraversalScheduled = true;
            mHandler.sendEmptyMessage(0);
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
            //long startTime = RenderCore.timeNanos();

            int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

            host.measure(widthSpec, heightSpec);

            assert width == host.getMeasuredWidth() && height == host.getMeasuredHeight();
            host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());

            /*ModernUI.LOGGER.info(MARKER, "Layout done in {} ms, window size: {}x{}",
                    (RenderCore.timeNanos() - startTime) / 1000000.0, width, height);*/
            mLayoutRequested = false;

            mAttachInfo.mTreeObserver.dispatchOnGlobalLayout();
        }

        mWillDrawSoon = false;

        boolean cancelDraw = mAttachInfo.mTreeObserver.dispatchOnPreDraw();

        synchronized (mRenderLock) {
            if (!cancelDraw && !mRedrawn) {
                if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
                    for (LayoutTransition pendingTransition : mPendingTransitions) {
                        pendingTransition.startChangingAnimations();
                    }
                    mPendingTransitions.clear();
                }
                if (mInvalidated) {
                    mIsDrawing = true;
                    Canvas canvas = beginRecording(width, height);
                    host.draw(canvas);
                    mIsDrawing = false;
                    if (mKeepInvalidated) {
                        mKeepInvalidated = false;
                    } else {
                        mInvalidated = false;
                    }
                    mRedrawn = true;
                }
            } else {
                scheduleTraversals();
            }
        }
    }

    @Nonnull
    protected abstract Canvas beginRecording(int width, int height);

    public void enqueueInputEvent(@Nonnull InputEvent event) {
        mInputEvents.offer(event);
    }

    protected void doProcessInputEvents() {
        ArchCore.checkUiThread();
        if (mView != null) {
            InputEvent e;
            while ((e = mInputEvents.poll()) != null) {
                try {
                    if (e instanceof KeyEvent event) {
                        if (mView.dispatchKeyEvent(event)) {
                            continue;
                        }
                        int groupNavigationDirection = 0;

                        if (event.getAction() == KeyEvent.ACTION_DOWN
                                && event.getKeyCode() == KeyEvent.KEY_TAB) {
                            if (event.isShiftPressed()) {
                                groupNavigationDirection = View.FOCUS_BACKWARD;
                            } else {
                                groupNavigationDirection = View.FOCUS_FORWARD;
                            }
                        }

                        // If a modifier is held, try to interpret the key as a shortcut.
                        if (event.getAction() == KeyEvent.ACTION_DOWN
                                && !event.hasNoModifiers()
                                && event.getRepeatCount() == 0
                                && !KeyEvent.isModifierKey(event.getKeyCode())
                                && groupNavigationDirection == 0) {
                            if (mView.dispatchKeyShortcutEvent(event)) {
                                continue;
                            }
                        }

                        // Handle automatic focus changes.
                        /*if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            if (groupNavigationDirection != 0) {
                                if (performKeyboardGroupNavigation(groupNavigationDirection)) {
                                    continue;
                                }
                            } else {
                                if (performFocusNavigation(event)) {
                                    continue;
                                }
                            }
                        }*/
                        //TODO focus
                        onKeyEvent(event);
                    } else {
                        mView.dispatchPointerEvent((MotionEvent) e);
                    }
                } finally {
                    e.recycle();
                }
            }
        } else {
            // drop all
            mInputEvents.clear();
        }
    }

    protected void onKeyEvent(KeyEvent event) {
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

    void performDragEvent(DragEvent event) {
        if (hasDragOperation) {

        }
    }

    void invalidate() {
        ArchCore.checkUiThread();
        mInvalidated = true;
        if (!mWillDrawSoon) {
            if (mIsDrawing) {
                mKeepInvalidated = true;
            }
            scheduleTraversals();
        }
    }

    protected void updatePointerIcon(@Nullable PointerIcon pointerIcon) {
    }

    /// START - Handler

    protected void postOnAnimation(@Nonnull Runnable r) {
        postOnAnimationDelayed(r, 0);
    }

    protected void postOnAnimationDelayed(@Nonnull Runnable r, long delayMillis) {
    }

    protected void removeCallbacks(@Nonnull Runnable r) {
    }

    /// END - Handler

    /**
     * Return true if child is an ancestor of parent, (or equal to the parent).
     */
    public static boolean isViewDescendantOf(View child, View parent) {
        if (child == parent) {
            return true;
        }

        final ViewParent theParent = child.getParent();
        return (theParent instanceof ViewGroup) && isViewDescendantOf((View) theParent, parent);
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
        ArchCore.checkUiThread();
        mLayoutRequested = true;
        scheduleTraversals();
    }

    @Override
    public boolean isLayoutRequested() {
        return mLayoutRequested;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        ArchCore.checkUiThread();
        scheduleTraversals();
    }

    @Override
    public void clearChildFocus(View child) {
        ArchCore.checkUiThread();
        scheduleTraversals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View focusSearch(View focused, int direction) {
        ArchCore.checkUiThread();
        if (!(mView instanceof ViewGroup)) {
            return null;
        }
        //return FocusFinder.getInstance().findNextFocus((ViewGroup) mView, focused, direction);
        return null;
    }

    /*@Override
    public View keyboardNavigationClusterSearch(View currentCluster,
                                                @FocusDirection int direction) {
        ArchCore.checkUiThread();
        return FocusFinder.getInstance().findNextKeyboardNavigationCluster(
                mView, currentCluster, direction);
    }*/

    @Override
    public void bringChildToFront(View child) {
    }

    @Override
    public void focusableViewAvailable(View v) {
        ArchCore.checkUiThread();
        if (mView != null) {
            if (!mView.hasFocus()) {
                // the one case where will transfer focus away from the current one
                // is if the current view is a view group that prefers to give focus
                // to its children first AND the view is a descendant of it.
                View focused = mView.findFocus();
                if (focused instanceof ViewGroup group) {
                    if (group.getDescendantFocusability() == ViewGroup.FOCUS_AFTER_DESCENDANTS
                            && isViewDescendantOf(v, focused)) {
                        v.requestFocus();
                    }
                }
            }
        }
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
    public boolean canResolveTextAlignment() {
        return true;
    }

    @Override
    public boolean isTextAlignmentResolved() {
        return true;
    }

    @Override
    public int getTextAlignment() {
        return View.TEXT_ALIGNMENT_RESOLVED_DEFAULT;
    }

    @Override
    public boolean showContextMenuForChild(View originalView, float x, float y) {
        return false;
    }

    @Override
    public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback, int type) {
        return null;
    }

    @Override
    public void createContextMenu(ContextMenu menu) {
    }

    @Override
    public void childDrawableStateChanged(View child) {
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // no op
    }

    /**
     * Add LayoutTransition to the list of transitions to be started in the next traversal.
     * This list will be cleared after the transitions on the list are start()'ed. These
     * transitionsa re added by LayoutTransition itself when it sets up animations. The setup
     * happens during the layout phase of traversal, which we want to complete before any of the
     * animations are started (because those animations may side-effect properties that layout
     * depends upon, like the bounding rectangles of the affected views). So we add the transition
     * to the list and it is started just prior to starting the drawing phase of traversal.
     *
     * @param transition The LayoutTransition to be started on the next traversal.
     * @hide
     */
    public void requestTransitionStart(LayoutTransition transition) {
        if (mPendingTransitions == null || !mPendingTransitions.contains(transition)) {
            if (mPendingTransitions == null) {
                mPendingTransitions = new ArrayList<>();
            }
            mPendingTransitions.add(transition);
        }
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
