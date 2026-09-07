/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
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

import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.core.ColorSpaces;
import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.sketch.Surface;
import icyllis.modernui.animation.LayoutTransition;
import icyllis.modernui.annotation.*;
import icyllis.modernui.core.*;
import icyllis.modernui.graphics.*;
import icyllis.modernui.graphics.pipeline.ArcCanvas;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.util.DisplayMetrics;
import icyllis.modernui.view.View.FocusDirection;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;

/**
 * The top of a view hierarchy, implementing the needed protocol between View
 * and the Stage.  This is for the most part an internal implementation
 * detail of {@link WindowStage}.
 *
 * @hidden
 */
@ApiStatus.Internal
public class ViewRoot implements ViewParent, AttachInfo.Callbacks {

    protected static final Marker MARKER = MarkerFactory.getMarker("ViewRoot");

    private final AttachInfo mAttachInfo;

    private static final int MSG_INVALIDATE = 1;
    protected static final int MSG_PROCESS_INPUT_EVENTS = 19;
    private static final int MSG_INVALIDATE_WORLD = 22;

    private final ConcurrentLinkedQueue<InputEvent> mInputEvents = new ConcurrentLinkedQueue<>();

    public boolean mTraversalScheduled;
    int mTraversalBarrier;
    boolean mWillDrawSoon;
    /** Set to true while in performTraversals for detecting when die(true) is called from internal
     * callbacks such as onMeasure, onPreDraw, onDraw and deferring doDie() until later. */
    boolean mIsInTraversal;
    boolean mLayoutRequested;
    boolean mFirst;

    boolean mReportNextDraw;

    boolean mFullRedrawNeeded;
    boolean mForceNextWindowRelayout;

    boolean mIsDrawing;

    private boolean mInLayout = false;
    final ArrayList<View> mLayoutRequesters = new ArrayList<>();
    boolean mHandlingLayoutInLayoutRequest = false;

    private boolean hasDragOperation;

    boolean mProcessInputEventsScheduled;

    protected final Object mRenderLock = new Object();

    private int mPointerIconType = PointerIcon.TYPE_DEFAULT;

    boolean mAdded;

    // window frame in screen
    final Rect mWinFrame = new Rect();
    private final Rect mLastLayoutFrame = new Rect();

    private int mMeasuredWidth;
    private int mMeasuredHeight;

    protected View mView;
    private int mWidth;
    private int mHeight;
    private Rect mDirty;

    int mViewVisibility;
    boolean mAppVisible = true;
    // Used for tracking app visibility updates separately in case we get double change. This will
    // make sure that we always call relayout for the corresponding window.
    private boolean mAppVisibilityChanged;

    // Set to true if the owner of this window is in the stopped state,
    // so the window should no longer be active.
    boolean mStopped = false;

    public final Handler mHandler;
    public final Choreographer mChoreographer;

    private ArrayList<LayoutTransition> mPendingTransitions;

    final Rect mTempRect = new Rect(); // used in the transaction to not thrash the heap.

    /*private final int[] inBounds  = new int[]{0, 0, 0, 0};
    private final int[] outBounds = new int[4];*/

    boolean mWindowAttributesChanged = false;
    public final WindowManager.LayoutParams mWindowAttributes = new WindowManager.LayoutParams();

    public WindowStage mStage;

    public Surface mSurface;
    public boolean mNeedsRendererSetup;

    protected ViewRoot() {
        mHandler = new Handler(Looper.myLooper(), this::handleMessage);
        mWidth = -1;
        mHeight = -1;
        mDirty = new Rect();
        mChoreographer = Choreographer.getInstance();
        mAttachInfo = new AttachInfo(this, mHandler, this);
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
            mStage.scheduleComposition();
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

    /**
     * Figures out the measure spec for the root view in a window based on its
     * layout params.
     *
     * @param windowSize The available width or height of the window.
     * @param measurement The layout width or height requested in the layout params.
     * @return The measure spec to use to measure the root view.
     */
    private static int getRootMeasureSpec(int windowSize, int measurement) {
        int measureSpec;
        switch (measurement) {
            case ViewGroup.LayoutParams.MATCH_PARENT:
                // Window can't resize. Force root view to be windowSize.
                measureSpec = MeasureSpec.makeMeasureSpec(windowSize, MeasureSpec.EXACTLY);
                break;
            case ViewGroup.LayoutParams.WRAP_CONTENT:
                // Window can resize. Set max size for root view.
                measureSpec = MeasureSpec.makeMeasureSpec(windowSize, MeasureSpec.AT_MOST);
                break;
            default:
                // Window wants to be an exact size. Force root view to be that size.
                measureSpec = MeasureSpec.makeMeasureSpec(measurement, MeasureSpec.EXACTLY);
                break;
        }
        return measureSpec;
    }

    private boolean measureHierarchy(
            final View host, final WindowManager.LayoutParams lp,
            final Resources res, final int desiredWindowWidth, final int desiredWindowHeight) {
        int childWidthMeasureSpec;
        int childHeightMeasureSpec;
        boolean windowSizeMayChange = false;

        boolean goodMeasure = false;
        if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            // On large screens, we don't want to allow dialogs to just
            // stretch to fill the entire width of the screen to display
            // one line of text.  First try doing the layout at a smaller
            // size to see if it will fit.
            final DisplayMetrics displayMetrics = res.getDisplayMetrics();
            int baseSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DP, InternalConfig.prefDialogWidth,
                    displayMetrics);
            if (baseSize != 0 && desiredWindowWidth > baseSize) {
                childWidthMeasureSpec = getRootMeasureSpec(baseSize, lp.width);
                childHeightMeasureSpec = getRootMeasureSpec(desiredWindowHeight, lp.height);
                performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
                if ((host.getMeasuredWidthAndState()&View.MEASURED_STATE_TOO_SMALL) == 0) {
                    goodMeasure = true;
                } else {
                    // Didn't fit in that size... try expanding a bit.
                    baseSize = (baseSize+desiredWindowWidth)/2;
                    childWidthMeasureSpec = getRootMeasureSpec(baseSize, lp.width);
                    performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
                    if ((host.getMeasuredWidthAndState()&View.MEASURED_STATE_TOO_SMALL) == 0) {
                        goodMeasure = true;
                    }
                }
            }
        }

        if (!goodMeasure) {
            childWidthMeasureSpec = getRootMeasureSpec(desiredWindowWidth, lp.width);
            childHeightMeasureSpec = getRootMeasureSpec(desiredWindowHeight, lp.height);
            performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
            if (mWidth != host.getMeasuredWidth() || mHeight != host.getMeasuredHeight()) {
                windowSizeMayChange = true;
            }
        }

        return windowSizeMayChange;
    }

    private void performMeasure(int childWidthMeasureSpec, int childHeightMeasureSpec) {
        if (mView == null) {
            return;
        }
        mView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        mMeasuredWidth = mView.getMeasuredWidth();
        mMeasuredHeight = mView.getMeasuredHeight();
    }

    private void performLayout(WindowManager.LayoutParams lp, int desiredWindowWidth,
                               int desiredWindowHeight) {
        mInLayout = true;

        final View host = mView;
        if (host == null) {
            return;
        }

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
                int numValidRequests = validLayoutRequesters.size();
                for (int i = 0; i < numValidRequests; ++i) {
                    final View view = validLayoutRequesters.get(i);
                    view.requestLayout();
                }
                measureHierarchy(host, lp, mView.getContext().getResources(),
                        desiredWindowWidth, desiredWindowHeight);
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
                        int finalNumValidRequests = finalRequesters.size();
                        for (int i = 0; i < finalNumValidRequests; ++i) {
                            final View view = finalRequesters.get(i);
                            view.requestLayout();
                        }
                    });
                }
            }

        }
        mInLayout = false;
    }

    private void performTraversal() {
        final View host = mView;

        if (host == null || !mAdded) {
            return;
        }

        mIsInTraversal = true;
        mWillDrawSoon = true;

        boolean windowSizeMayChange = false;
        WindowManager.LayoutParams lp = mWindowAttributes;

        int desiredWindowWidth;
        int desiredWindowHeight;

        final int viewVisibility = getHostVisibility();
        final boolean viewVisibilityChanged = !mFirst
                && (mViewVisibility != viewVisibility
                // Also check for possible double visibility update, which will make current
                // viewVisibility value equal to mViewVisibility and we may miss it.
                || mAppVisibilityChanged);
        mAppVisibilityChanged = false;
        final boolean viewUserVisibilityChanged = !mFirst &&
                ((mViewVisibility == View.VISIBLE) != (viewVisibility == View.VISIBLE));

        WindowManager.LayoutParams params = null;
        Rect frame = mWinFrame;
        if (mFirst) {
            mFullRedrawNeeded = true;
            mLayoutRequested = true;

            if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT
                    || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                // For wrap content, we have to remeasure later on anyways. Use size consistent with
                // below so we get best use of the measure cache.
                desiredWindowWidth = mStage.getWidth();
                desiredWindowHeight = mStage.getHeight();
            } else {
                // After addToDisplay, the frame contains the frameHint from window manager, which
                // for most windows is going to be the same size as the result of relayoutWindow.
                // Using this here allows us to avoid remeasuring after relayoutWindow
                desiredWindowWidth = frame.width();
                desiredWindowHeight = frame.height();
            }

            mAttachInfo.mWindowVisibility = viewVisibility;
            // Set the layout direction if it has not been set before (inherit is the default)
            //TODO
            /*if (mViewLayoutDirectionInitial == View.LAYOUT_DIRECTION_INHERIT) {
                host.setLayoutDirection(config.getLayoutDirection());
            }*/
            host.dispatchAttachedToWindow(mAttachInfo, 0);
            //mAttachInfo.mTreeObserver.dispatchOnWindowAttachedChange(true);
        } else {
            desiredWindowWidth = frame.width();
            desiredWindowHeight = frame.height();
            if (desiredWindowWidth != mWidth || desiredWindowHeight != mHeight) {
                mFullRedrawNeeded = true;
                mLayoutRequested = true;
                windowSizeMayChange = true;
            }
        }

        if (viewVisibilityChanged) {
            mAttachInfo.mWindowVisibility = viewVisibility;
            host.dispatchWindowVisibilityChanged(viewVisibility);
            //mAttachInfo.mTreeObserver.dispatchOnWindowVisibilityChange(viewVisibility);
            if (viewUserVisibilityChanged) {
                host.dispatchVisibilityAggregated(viewVisibility == View.VISIBLE);
            }
        }

        boolean layoutRequested = mLayoutRequested && (!mStopped || mReportNextDraw);
        if (layoutRequested) {
            if (!mFirst) {
                if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT
                        || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    windowSizeMayChange = true;

                    desiredWindowWidth = mStage.getWidth();
                    desiredWindowHeight = mStage.getHeight();
                }
            }

            // Ask host how big it wants to be
            windowSizeMayChange |= measureHierarchy(host, lp, mView.getContext().getResources(),
                    desiredWindowWidth, desiredWindowHeight);
        }

        if (mFirst || mAttachInfo.mViewVisibilityChanged) {
            mAttachInfo.mViewVisibilityChanged = false;
        }

        if (layoutRequested) {
            // Clear this now, so that if anything requests a layout in the
            // rest of this function we will catch it and re-run a full
            // layout pass.
            mLayoutRequested = false;
        }

        boolean windowShouldResize = layoutRequested && windowSizeMayChange
                && ((mWidth != host.getMeasuredWidth() || mHeight != host.getMeasuredHeight())
                || (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT &&
                frame.width() < desiredWindowWidth && frame.width() != mWidth)
                || (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT &&
                frame.height() < desiredWindowHeight && frame.height() != mHeight));

        final boolean isViewVisible = viewVisibility == View.VISIBLE;

        final boolean windowAttributesChanged = mWindowAttributesChanged;
        if (windowAttributesChanged) {
            mWindowAttributesChanged = false;
            params = lp;
        }

        if (mFirst || windowShouldResize || viewVisibilityChanged || params != null
                || mForceNextWindowRelayout) {

            mForceNextWindowRelayout = false;

            relayoutWindow(lp);

            //mAttachInfo.mWindowLeft = frame.left;
            //mAttachInfo.mWindowTop = frame.top;

            // !!FIXME!! This next section handles the case where we did not get the
            // window size we asked for. We should avoid this by getting a maximum size from
            // the window session beforehand.
            if (mWidth != frame.width() || mHeight != frame.height()) {
                mWidth = frame.width();
                mHeight = frame.height();
            }

            Point surfaceSize = new Point();
            computeSurfaceSize(lp, frame, surfaceSize);

            if (mSurface == null ||
                    mSurface.getWidth() != surfaceSize.x ||
                    mSurface.getHeight() != surfaceSize.y ||
                    mNeedsRendererSetup) {
                if (mSurface != null) {
                    mSurface.unref();
                }
                mSurface = mStage.getRenderPipeline().createSurface(
                        ImageInfo.make(surfaceSize.x, surfaceSize.y,
                                ColorInfo.CT_RGBA_8888, ColorInfo.AT_PREMUL,
                                ColorSpaces.SRGB)
                );
                mNeedsRendererSetup = false;

                mFullRedrawNeeded = true;
            }
        }

        final boolean didLayout = layoutRequested && (!mStopped || mReportNextDraw);
        if (didLayout) {
            performLayout(lp, mWidth, mHeight);

            mAttachInfo.mTreeObserver.dispatchOnGlobalLayout();
        }

        if (mFirst) {
            if (!mAttachInfo.mInTouchMode) {
                // handle first focus request
                if (mView != null) {
                    if (!mView.hasFocus()) {
                        mView.restoreDefaultFocus();
                    }
                }
            } else {
                // Some views (like ScrollView) won't hand focus to descendants that aren't within
                // their viewport. Before layout, there's a good change these views are size 0
                // which means no children can get focus. After layout, this view now has size, but
                // is not guaranteed to hand-off focus to a focusable child (specifically, the edge-
                // case where the child has a size prior to layout and thus won't trigger
                // focusableViewAvailable).
                View focused = mView.findFocus();
                if (focused instanceof ViewGroup
                        && ((ViewGroup) focused).getDescendantFocusability()
                        == ViewGroup.FOCUS_AFTER_DESCENDANTS) {
                    focused.restoreDefaultFocus();
                }
            }
        }

        mFirst = false;
        mWillDrawSoon = false;
        mViewVisibility = viewVisibility;

        boolean cancelDraw = mAttachInfo.mTreeObserver.dispatchOnPreDraw();

        if (!isViewVisible) {

        } else if (cancelDraw) {
            // Try again
            scheduleTraversals();
        } else {
            if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
                for (LayoutTransition pendingTransition : mPendingTransitions) {
                    pendingTransition.startChangingAnimations();
                }
                mPendingTransitions.clear();
            }

            performDraw();
        }

        mIsInTraversal = false;

        if (!cancelDraw) {
            mReportNextDraw = false;
        }
    }

    private void performDraw() {
        if (mAttachInfo.mViewScrollChanged) {
            mAttachInfo.mViewScrollChanged = false;
            mAttachInfo.mTreeObserver.dispatchOnScrollChanged();
        }

        mIsDrawing = true;

        final Rect dirty = mDirty;
        if (mFullRedrawNeeded) {
            dirty.set(0, 0, mWidth, mHeight);
        }
        mFullRedrawNeeded = false;

        View host = mView;
        assert host != null;

        if (!dirty.isEmpty() && mSurface != null) {
            Canvas canvas = new ArcCanvas(mSurface.getCanvas());

            canvas.save();
            if (!dirty.contains(0, 0, mWidth, mHeight)) {
                // clip only when there's subset, to include surface insets if full draw needed
                canvas.clipRect(dirty);
            }

            canvas.drawColor(0, BlendMode.CLEAR);

            dirty.setEmpty();

            host.mPrivateFlags |= View.PFLAG_DRAWN;

            host.draw(canvas);
            canvas.restore();

            mStage.markForComposition();
            endDrawLocked(canvas);
        }

        mIsDrawing = false;
    }

    @Deprecated
    protected void endDrawLocked(@NonNull Canvas canvas) {
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

    public void relayoutWindow(WindowManager.LayoutParams params) {
        computeFrames(params, mStage.mFrame, mMeasuredWidth, mMeasuredHeight,
                mWinFrame);

        mLastLayoutFrame.set(mWinFrame);
    }

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
                                && !event.isRepeat()
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
                        boolean isFastScrolling = event.isRepeat();
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
        mDirty.set(0, 0, mWidth, mHeight);
        if (!mWillDrawSoon) {
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

    @Override
    public void invalidateChild(View child, Rect dirty) {
        invalidateChildInParent(null, dirty);
    }

    @Override
    public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
        Core.checkUiThread();

        if (dirty == null) {
            invalidate();
            return null;
        } else if (dirty.isEmpty()) {
            return null;
        }

        invalidateRectOnScreen(dirty);

        return null;
    }

    private void invalidateRectOnScreen(Rect dirty) {
        final Rect localDirty = mDirty;

        // Add the new dirty rect to the current one
        localDirty.union(dirty.left, dirty.top, dirty.right, dirty.bottom);
        // Intersect with the bounds of the window to skip
        // updates that lie outside of the visible region
        final boolean intersected = localDirty.intersect(0, 0,
                mWidth, mHeight);
        if (!intersected) {
            localDirty.setEmpty();
        }
        if (!mWillDrawSoon && intersected) {
            scheduleTraversals();
        }
    }

    @Override
    public void playSoundEffect(int effectId) {
    }

    @Override
    public boolean performHapticFeedback(int effectId, boolean always) {
        return false;
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

    int getHostVisibility() {
        return mView != null && mAppVisible
                ? mView.getVisibility() : View.GONE;
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

    public static final int UNSPECIFIED_LENGTH = -1;

    public static void computeFrames(
            @NonNull WindowManager.LayoutParams attrs,
            @NonNull Rect parentFrame,
            int requestedWidth, int requestedHeight,
            @NonNull Rect outFrame) {

        final int pw = parentFrame.width();
        final int ph = parentFrame.height();
        int rw = requestedWidth;
        int rh = requestedHeight;
        float x, y;
        int w, h;

        if (rw == UNSPECIFIED_LENGTH) {
            rw = attrs.width >= 0 ? attrs.width : pw;
        }
        if (rh == UNSPECIFIED_LENGTH) {
            rh = attrs.height >= 0 ? attrs.height : ph;
        }

        if (attrs.width == WindowManager.LayoutParams.MATCH_PARENT) {
            w = pw;
        } else {
            w = rw;
        }
        if (attrs.height == WindowManager.LayoutParams.MATCH_PARENT) {
            h = ph;
        } else {
            h = rh;
        }

        x = attrs.x;
        y = attrs.y;

        w = Math.min(w, pw);
        h = Math.min(h, ph);

        final boolean fitToDisplay = (attrs.type != WindowManager.LayoutParams.TYPE_BASE_APPLICATION);

        // Set frame
        Gravity.apply(attrs.gravity, w, h, parentFrame,
                (int) (x + attrs.horizontalMargin * pw),
                (int) (y + attrs.verticalMargin * ph), outFrame);

        if (fitToDisplay) {
            Gravity.applyDisplay(attrs.gravity, parentFrame, outFrame);
        }
    }

    public static void computeSurfaceSize(
            @NonNull WindowManager.LayoutParams attrs,
            @NonNull Rect winFrame,
            @NonNull Point outSurfaceSize) {
        int width;
        int height;
        width = winFrame.width();
        height = winFrame.height();

        if (width < 1) {
            width = 1;
        }
        if (height < 1) {
            height = 1;
        }

        final Rect surfaceInsets = attrs.surfaceInsets;
        width += surfaceInsets.left + surfaceInsets.right;
        height += surfaceInsets.top + surfaceInsets.bottom;

        outSurfaceSize.set(width, height);
    }
}
