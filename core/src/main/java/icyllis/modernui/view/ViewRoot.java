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
import icyllis.modernui.annotation.*;
import icyllis.modernui.core.*;
import icyllis.modernui.graphics.*;
import icyllis.modernui.view.View.FocusDirection;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;

/**
 * The top of a view hierarchy, implementing the needed protocol between View and the Window.
 */
@ApiStatus.Internal
public abstract class ViewRoot implements ViewParent, AttachInfo.Callbacks {

    protected static final Marker MARKER = MarkerManager.getMarker("ViewRoot");

    private final AttachInfo mAttachInfo;

    private static final int MSG_INVALIDATE = 1;
    protected static final int MSG_PROCESS_INPUT_EVENTS = 19;
    private static final int MSG_INVALIDATE_WORLD = 22;

    private final ConcurrentLinkedQueue<InputEvent> mInputEvents = new ConcurrentLinkedQueue<>();

    protected boolean mTraversalScheduled;
    int mTraversalBarrier;
    private boolean mWillDrawSoon;
    private boolean mIsDrawing;
    private boolean mLayoutRequested;
    private boolean mInvalidated;
    private boolean mKeepInvalidated;

    private boolean mInLayout = false;
    ArrayList<View> mLayoutRequesters = new ArrayList<>();
    boolean mHandlingLayoutInLayoutRequest = false;

    private boolean hasDragOperation;

    boolean mProcessInputEventsScheduled;

    protected final Object mRenderLock = new Object();

    private int mPointerIconType = PointerIcon.TYPE_DEFAULT;

    protected View mView;
    private int mWidth;
    private int mHeight;

    public final Handler mHandler;
    public final Choreographer mChoreographer;

    private ArrayList<LayoutTransition> mPendingTransitions;

    final Rect mTempRect = new Rect(); // used in the transaction to not thrash the heap.

    /*private final int[] inBounds  = new int[]{0, 0, 0, 0};
    private final int[] outBounds = new int[4];*/

    protected ViewRoot() {
        mHandler = new Handler(Looper.myLooper(), this::handleMessage);
        mChoreographer = Choreographer.getInstance();
        mAttachInfo = new AttachInfo(this, mHandler, this);

        try {
            Class.forName("icyllis.modernui.text.BoringLayout");
            Class.forName("icyllis.modernui.graphics.text.CharArrayIterator");
            Class.forName("icyllis.modernui.text.CharSequenceIterator");
            Class.forName("icyllis.modernui.text.Directions");
            Class.forName("icyllis.modernui.text.DynamicLayout");
            Class.forName("icyllis.modernui.graphics.font.GlyphManager");
            Class.forName("icyllis.modernui.graphics.text.GraphemeBreak");
            Class.forName("icyllis.modernui.text.Layout");
            Class.forName("icyllis.modernui.graphics.text.LayoutCache");
            Class.forName("icyllis.modernui.graphics.text.LayoutPiece");
            Class.forName("icyllis.modernui.graphics.text.LineBreaker");
            Class.forName("icyllis.modernui.text.MeasuredParagraph");
            Class.forName("icyllis.modernui.graphics.text.MeasuredText");
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

    protected boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_INVALIDATE -> ((View) msg.obj).invalidate();
            case MSG_PROCESS_INPUT_EVENTS -> {
                mProcessInputEventsScheduled = false;
                doProcessInputEvents();
            }
            case MSG_INVALIDATE_WORLD -> {
                if (mView != null) {
                    invalidateWorld(mView);
                }
            }
        }
        return true;
    }

    public void setView(@NonNull View view) {
        synchronized (this) {
            if (mView == null) {
                mView = view;
                mAttachInfo.mRootView = view;
                mAttachInfo.mWindowVisibility = View.VISIBLE;
                view.assignParent(this);
                view.dispatchAttachedToWindow(mAttachInfo, View.VISIBLE);
                view.dispatchWindowVisibilityChanged(View.VISIBLE);
            }
        }
    }

    public void setFrame(int width, int height) {
        if (width != mWidth || height != mHeight) {
            mWidth = width;
            mHeight = height;
            requestLayout();
        }
    }

    public View getView() {
        return mView;
    }

    boolean startDragAndDrop(@NonNull View view, @Nullable Object data, @Nullable View.DragShadow shadow, int flags) {
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

    private void scheduleProcessInputEvents() {
        if (!mProcessInputEventsScheduled) {
            mProcessInputEventsScheduled = true;
            Message msg = mHandler.obtainMessage(MSG_PROCESS_INPUT_EVENTS);
            msg.setAsynchronous(true);
            mHandler.sendMessage(msg);
        }
    }

    final Runnable mTraversalRunnable = this::doTraversal;

    @UiThread
    protected void scheduleTraversals() {
        if (!mTraversalScheduled) {
            mTraversalScheduled = true;
            mTraversalBarrier = mHandler.getQueue().postSyncBarrier();
            mChoreographer.postCallback(Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
        }
    }

    @UiThread
    protected void unscheduleTraversals() {
        if (mTraversalScheduled) {
            mTraversalScheduled = false;
            mHandler.getQueue().removeSyncBarrier(mTraversalBarrier);
            mChoreographer.removeCallbacks(Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
        }
    }

    @UiThread
    protected void doTraversal() {
        if (mTraversalScheduled) {
            mTraversalScheduled = false;
            mHandler.getQueue().removeSyncBarrier(mTraversalBarrier);

            performTraversal();
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

            mInLayout = true;
            host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
            mInLayout = false;

            int numViewsRequestingLayout = mLayoutRequesters.size();
            if (numViewsRequestingLayout > 0) {
                // requestLayout() was called during layout.
                // If no layout-request flags are set on the requesting views, there is no problem.
                // If some requests are still pending, then we need to clear those flags and do
                // a full request/measure/layout pass to handle this situation.
                ArrayList<View> validLayoutRequesters = getValidLayoutRequesters(mLayoutRequesters,
                        false);
                if (validLayoutRequesters != null) {
                    // Set this flag to indicate that any further requests are happening during
                    // the second pass, which may result in posting those requests to the next
                    // frame instead
                    mHandlingLayoutInLayoutRequest = true;

                    // Process fresh layout requests, then measure and layout
                    for (final View view : validLayoutRequesters) {
                        view.requestLayout();
                    }
                    host.measure(widthSpec, heightSpec);
                    mInLayout = true;
                    host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());

                    mHandlingLayoutInLayoutRequest = false;

                    // Check the valid requests again, this time without checking/clearing the
                    // layout flags, since requests happening during the second pass get noop'd
                    validLayoutRequesters = getValidLayoutRequesters(mLayoutRequesters, true);
                    if (validLayoutRequesters != null) {
                        final ArrayList<View> finalRequesters = validLayoutRequesters;
                        // Post second-pass requests to the next frame
                        mHandler.post(() -> {
                            for (final View view : finalRequesters) {
                                view.requestLayout();
                            }
                        });
                    }
                }
            }
            mInLayout = false;

            /*ModernUI.LOGGER.info(MARKER, "Layout done in {} ms, window size: {}x{}",
                    (RenderCore.timeNanos() - startTime) / 1000000.0, width, height);*/
            mLayoutRequested = false;

            mAttachInfo.mTreeObserver.dispatchOnGlobalLayout();
        }

        mWillDrawSoon = false;

        boolean cancelDraw = mAttachInfo.mTreeObserver.dispatchOnPreDraw();

        synchronized (mRenderLock) {
            if (!cancelDraw) {
                if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
                    for (LayoutTransition pendingTransition : mPendingTransitions) {
                        pendingTransition.startChangingAnimations();
                    }
                    mPendingTransitions.clear();
                }

                if (mAttachInfo.mViewScrollChanged) {
                    mAttachInfo.mViewScrollChanged = false;
                    mAttachInfo.mTreeObserver.dispatchOnScrollChanged();
                }

                if (mInvalidated) {
                    mIsDrawing = true;
                    Canvas canvas = beginDrawLocked(width, height);
                    if (canvas != null) {
                        host.draw(canvas);
                        endDrawLocked(canvas);
                    }
                    mIsDrawing = false;
                    if (mKeepInvalidated) {
                        mKeepInvalidated = false;
                    } else {
                        mInvalidated = false;
                    }
                }
            } else {
                scheduleTraversals();
            }
        }
    }

    /**
     * This method is called during layout when there have been calls to requestLayout() during
     * layout. It walks through the list of views that requested layout to determine which ones
     * still need it, based on visibility in the hierarchy and whether they have already been
     * handled (as is usually the case with ListView children).
     *
     * @param layoutRequesters     The list of views that requested layout during layout
     * @param secondLayoutRequests Whether the requests were issued during the second layout pass.
     *                             If so, the FORCE_LAYOUT flag was not set on requesters.
     * @return A list of the actual views that still need to be laid out.
     */
    private ArrayList<View> getValidLayoutRequesters(@NonNull ArrayList<View> layoutRequesters,
                                                     boolean secondLayoutRequests) {
        ArrayList<View> validLayoutRequesters = null;
        for (View view : layoutRequesters) {
            if (view != null && view.mAttachInfo != null && view.mParent != null &&
                    (secondLayoutRequests || (view.mPrivateFlags & View.PFLAG_FORCE_LAYOUT) ==
                            View.PFLAG_FORCE_LAYOUT)) {
                boolean gone = false;
                View parent = view;
                // Only trigger new requests for views in a non-GONE hierarchy
                while (parent != null) {
                    if ((parent.mViewFlags & View.VISIBILITY_MASK) == View.GONE) {
                        gone = true;
                        break;
                    }
                    if (parent.mParent instanceof View) {
                        parent = (View) parent.mParent;
                    } else {
                        parent = null;
                    }
                }
                if (!gone) {
                    if (validLayoutRequesters == null) {
                        validLayoutRequesters = new ArrayList<>();
                    }
                    validLayoutRequesters.add(view);
                }
            }
        }
        if (!secondLayoutRequests) {
            // If we're checking the layout flags, then we need to clean them up also
            for (View view : layoutRequesters) {
                while (view != null &&
                        (view.mPrivateFlags & View.PFLAG_FORCE_LAYOUT) != 0) {
                    view.mPrivateFlags &= ~View.PFLAG_FORCE_LAYOUT;
                    if (view.mParent instanceof View) {
                        view = (View) view.mParent;
                    } else {
                        view = null;
                    }
                }
            }
        }
        layoutRequesters.clear();
        return validLayoutRequesters;
    }

    /**
     * Called by {@link View#isInLayout()} to determine whether the view hierarchy
     * is currently undergoing a layout pass.
     *
     * @return whether the view hierarchy is currently undergoing a layout pass
     */
    boolean isInLayout() {
        return mInLayout;
    }

    /**
     * Called by {@link View#requestLayout()} if the view hierarchy is currently
     * undergoing a layout pass. requestLayout() should not generally be called during layout,
     * unless the container hierarchy knows what it is doing (i.e., it is fine as long as
     * all children in that container hierarchy are measured and laid out at the end of the layout
     * pass for that container). If requestLayout() is called anyway, we handle it correctly
     * by registering all requesters during a frame as it proceeds. At the end of the frame,
     * we check all of those views to see if any still have pending layout requests, which
     * indicates that they were not correctly handled by their container hierarchy. If that is
     * the case, we clear all such flags in the tree, to remove the buggy flag state that leads
     * to blank containers, and force a second request/measure/layout pass in this frame. If
     * more requestLayout() calls are received during that second layout pass, we post those
     * requests to the next frame to avoid possible infinite loops.
     *
     * <p>The return value from this method indicates whether the request should proceed
     * (if it is a request during the first layout pass) or should be skipped and posted to the
     * next frame (if it is a request during the second layout pass).</p>
     *
     * @param view the view that requested the layout.
     * @return true if request should proceed, false otherwise.
     */
    boolean requestLayoutDuringLayout(@NonNull final View view) {
        if (view.mParent == null || view.mAttachInfo == null) {
            // Would not normally trigger another layout, so just let it pass through as usual
            return true;
        }
        if (!mLayoutRequesters.contains(view)) {
            mLayoutRequesters.add(view);
        }
        // Let the request proceed normally; it will be processed in a second layout pass
        // if necessary.
        // Otherwise, don't let the request proceed during the second layout pass.
        // It will post to the next frame instead.
        return !mHandlingLayoutInLayoutRequest;
    }

    @Nullable
    protected abstract Canvas beginDrawLocked(int width, int height);
    
    protected abstract void endDrawLocked(@NonNull Canvas canvas);

    @MainThread
    public void enqueueInputEvent(@NonNull InputEvent event) {
        mInputEvents.offer(event);
        scheduleProcessInputEvents();
    }

    private void doProcessInputEvents() {
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
                            if (event.hasModifiers(KeyEvent.META_SHIFT_ON | KeyEvent.META_ALT_ON)) {
                                groupNavigationDirection = View.FOCUS_BACKWARD;
                            } else if (event.hasModifiers(KeyEvent.META_ALT_ON)) {
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
                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            if (groupNavigationDirection != 0) {
                                if (performKeyboardGroupNavigation(groupNavigationDirection)) {
                                    continue;
                                }
                            } else {
                                if (performFocusNavigation(event)) {
                                    continue;
                                }
                            }
                        }
                        onKeyEvent(event);
                    } else {
                        MotionEvent ev = (MotionEvent) e;
                        if (dispatchTouchEvent(ev)) {
                            return;
                        }
                        boolean handled = mView.dispatchPointerEvent(ev);
                        if (ev.getAction() == MotionEvent.ACTION_HOVER_ENTER
                                || ev.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                            // Other apps or the window manager may change the icon type outside of
                            // this app, therefore the icon type has to be reset on enter/exit event.
                            mPointerIconType = PointerIcon.TYPE_DEFAULT;
                        }

                        if (ev.getAction() != MotionEvent.ACTION_HOVER_EXIT) {
                            if (!updatePointerIcon(ev) && ev.getAction() == MotionEvent.ACTION_HOVER_MOVE) {
                                mPointerIconType = PointerIcon.TYPE_DEFAULT;
                            }
                        }
                        maybeUpdateTooltip(ev);
                        if (handled) continue;
                        onTouchEvent(ev);
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

    private boolean performFocusNavigation(@NonNull KeyEvent event) {
        int direction = 0;
        switch (event.getKeyCode()) {
            case KeyEvent.KEY_LEFT:
                if (event.hasNoModifiers()) {
                    direction = View.FOCUS_LEFT;
                }
                break;
            case KeyEvent.KEY_RIGHT:
                if (event.hasNoModifiers()) {
                    direction = View.FOCUS_RIGHT;
                }
                break;
            case KeyEvent.KEY_UP:
                if (event.hasNoModifiers()) {
                    direction = View.FOCUS_UP;
                }
                break;
            case KeyEvent.KEY_DOWN:
                if (event.hasNoModifiers()) {
                    direction = View.FOCUS_DOWN;
                }
                break;
            case KeyEvent.KEY_TAB:
                if (event.hasNoModifiers()) {
                    direction = View.FOCUS_FORWARD;
                } else if (event.hasModifiers(KeyEvent.META_SHIFT_ON)) {
                    direction = View.FOCUS_BACKWARD;
                }
                break;
        }
        if (direction != 0) {
            View focused = mView.findFocus();
            if (focused != null) {
                View v = focused.focusSearch(direction);
                if (v != null && v != focused) {
                    // do the math the get the interesting rect
                    // of previous focused into the coord system of
                    // newly focused view
                    focused.getFocusedRect(mTempRect);
                    if (mView instanceof ViewGroup) {
                        ((ViewGroup) mView).offsetDescendantRectToMyCoords(
                                focused, mTempRect);
                        ((ViewGroup) mView).offsetRectIntoDescendantCoords(
                                v, mTempRect);
                    }
                    if (v.requestFocus(direction, mTempRect)) {
                        boolean isFastScrolling = event.getRepeatCount() > 0;
                        /*playSoundEffect(
                                SoundEffectConstants.getConstantForFocusDirection(direction,
                                        isFastScrolling));*/
                        return true;
                    }
                }

                // Give the focused view a last chance to handle the dpad key.
                /*if (mView.dispatchUnhandledMove(focused, direction)) {
                    return true;
                }*/
            } else {
                if (mView.restoreDefaultFocus()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean performKeyboardGroupNavigation(int direction) {
        final View focused = mView.findFocus();
        if (focused == null && mView.restoreDefaultFocus()) {
            return true;
        }
        View cluster = focused == null ? keyboardNavigationClusterSearch(null, direction)
                : focused.keyboardNavigationClusterSearch(null, direction);

        // Since requestFocus only takes "real" focus directions (and therefore also
        // restoreFocusInCluster), convert forward/backward focus into FOCUS_DOWN.
        int realDirection = direction;
        if (direction == View.FOCUS_FORWARD || direction == View.FOCUS_BACKWARD) {
            realDirection = View.FOCUS_DOWN;
        }

        if (cluster != null && cluster.isRootNamespace()) {
            // the default cluster. Try to find a non-clustered view to focus.
            if (cluster.restoreFocusNotInCluster()) {
                //playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
                return true;
            }
            // otherwise skip to next actual cluster
            cluster = keyboardNavigationClusterSearch(null, direction);
        }

        if (cluster != null && cluster.restoreFocusInCluster(realDirection)) {
            //playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
            return true;
        }

        return false;
    }

    protected boolean dispatchTouchEvent(MotionEvent event) {
        return false;
    }

    protected void onTouchEvent(MotionEvent event) {
    }

    protected void onKeyEvent(KeyEvent event) {
    }

    public void loadSystemProperties(BooleanSupplier debugLayoutSupplier) {
        mHandler.post(() -> {
            // Layout debugging
            boolean layout = debugLayoutSupplier.getAsBoolean();
            if (layout != mAttachInfo.mDebugLayout) {
                mAttachInfo.mDebugLayout = layout;
                if (!mHandler.hasMessages(MSG_INVALIDATE_WORLD)) {
                    mHandler.sendEmptyMessageDelayed(MSG_INVALIDATE_WORLD, 200);
                }
            }
        });
    }

    /*boolean onCursorPosEvent(LinkedList<View> route, double x, double y) {
        if (view != null) {
            return view.onCursorPosEvent(route, x, y);
        }
        return false;
    }

    boolean onMouseEvent(MotionEvent event) {
        if (view != null) {
            final boolean handled = view.onGenericMotionEvent(event);
            if (!handled && event.getAction() == MotionEvent.ACTION_MOVE) {
                view.ensureMouseHoverExit();
            }
            return handled;
        }
        return false;
    }

    void ensureMouseHoverExit() {
        if (view != null) {
            view.ensureMouseHoverExit();
        }
    }*/

    void performDragEvent(DragEvent event) {
        if (hasDragOperation) {

        }
    }

    void invalidate() {
        Core.checkUiThread();
        mInvalidated = true;
        if (!mWillDrawSoon) {
            if (mIsDrawing) {
                mKeepInvalidated = true;
            }
            scheduleTraversals();
        }
    }

    void invalidateWorld(@NonNull View view) {
        view.invalidate();
        if (view instanceof ViewGroup parent) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                invalidateWorld(parent.getChildAt(i));
            }
        }
    }

    final class InvalidateOnAnimationRunnable implements Runnable {

        private boolean mPosted;
        private final ArrayList<View> mViews = new ArrayList<>();
        private View[] mTempViews;

        public void addView(View view) {
            synchronized (this) {
                mViews.add(view);
                postIfNeededLocked();
            }
        }

        public void removeView(View view) {
            synchronized (this) {
                mViews.remove(view);

                if (mPosted && mViews.isEmpty()) {
                    mChoreographer.removeCallbacks(Choreographer.CALLBACK_ANIMATION, this, null);
                    mPosted = false;
                }
            }
        }

        @Override
        public void run() {
            final int viewCount;
            synchronized (this) {
                mPosted = false;

                viewCount = mViews.size();
                if (viewCount != 0) {
                    mTempViews = mViews.toArray(mTempViews != null
                            ? mTempViews : new View[viewCount]);
                    mViews.clear();
                }
            }

            for (int i = 0; i < viewCount; i++) {
                mTempViews[i].invalidate();
                mTempViews[i] = null;
            }
        }

        private void postIfNeededLocked() {
            if (!mPosted) {
                mChoreographer.postCallback(Choreographer.CALLBACK_ANIMATION, this, null);
                mPosted = true;
            }
        }
    }

    final InvalidateOnAnimationRunnable mInvalidateOnAnimationRunnable =
            new InvalidateOnAnimationRunnable();

    public void dispatchInvalidateDelayed(View view, long delayMilliseconds) {
        Message msg = mHandler.obtainMessage(MSG_INVALIDATE, view);
        mHandler.sendMessageDelayed(msg, delayMilliseconds);
    }

    public void dispatchInvalidateOnAnimation(View view) {
        mInvalidateOnAnimationRunnable.addView(view);
    }

    public void cancelInvalidate(View view) {
        mHandler.removeMessages(MSG_INVALIDATE, view);
        mInvalidateOnAnimationRunnable.removeView(view);
    }

    private boolean updatePointerIcon(@NonNull MotionEvent e) {
        if (mView == null) {
            return false;
        }
        final PointerIcon pointerIcon = mView.onResolvePointerIcon(e);
        final int pointerType = (pointerIcon != null) ?
                pointerIcon.getType() : PointerIcon.TYPE_DEFAULT;
        if (mPointerIconType != pointerType) {
            mPointerIconType = pointerType;
            applyPointerIcon(pointerType);
        }
        return true;
    }

    protected void applyPointerIcon(int pointerType) {
    }

    private void maybeUpdateTooltip(MotionEvent event) {
        if (event.getPointerCount() != 1) {
            return;
        }
        final int action = event.getActionMasked();
        if (action != MotionEvent.ACTION_HOVER_ENTER
                && action != MotionEvent.ACTION_HOVER_MOVE
                && action != MotionEvent.ACTION_HOVER_EXIT) {
            return;
        }
        if (mView == null) {
            return;
        }
        mView.dispatchTooltipHoverEvent(event);
    }

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

    @Override
    public boolean getChildVisibleRect(View child, Rect r, @Nullable Point offset) {
        if (child != mView) {
            throw new RuntimeException();
        }
        return r.intersect(0, 0, mWidth, mHeight);
    }

    /**
     * Request layout all views with layout mark in layout pass
     *
     * @see View#requestLayout()
     * @see View#forceLayout()
     */
    @Override
    public void requestLayout() {
        if (!mHandlingLayoutInLayoutRequest) {
            Core.checkUiThread();
            mLayoutRequested = true;
            scheduleTraversals();
        }
    }

    @Override
    public boolean isLayoutRequested() {
        return mLayoutRequested;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        Core.checkUiThread();
        scheduleTraversals();
    }

    @Override
    public void clearChildFocus(View child) {
        Core.checkUiThread();
        scheduleTraversals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View focusSearch(View focused, int direction) {
        Core.checkUiThread();
        if (!(mView instanceof ViewGroup)) {
            return null;
        }
        return FocusFinder.getInstance().findNextFocus((ViewGroup) mView, focused, direction);
    }

    @Override
    public View keyboardNavigationClusterSearch(View currentCluster,
                                                @FocusDirection int direction) {
        Core.checkUiThread();
        return FocusFinder.getInstance().findNextKeyboardNavigationCluster(
                mView, currentCluster, direction);
    }

    @Override
    public void childHasTransientStateChanged(View child, boolean hasTransientState) {
        // Do nothing.
    }

    @Override
    public void bringChildToFront(View child) {
    }

    @Override
    public void focusableViewAvailable(View v) {
        Core.checkUiThread();
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

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
        return false;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                               int dyUnconsumed, int type, @NonNull int[] consumed) {
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
    }

    @Override
    public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
        return false;
    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public int getNestedScrollAxes() {
        return View.SCROLL_AXIS_NONE;
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
