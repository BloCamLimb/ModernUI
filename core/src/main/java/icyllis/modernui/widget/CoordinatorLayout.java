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

package icyllis.modernui.widget;

import icyllis.modernui.core.Core;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.graphics.Matrix4;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.RectF;
import icyllis.modernui.util.Pools;
import icyllis.modernui.view.*;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.UnmodifiableView;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * CoordinatorLayout is a super-powered {@link FrameLayout}.
 *
 * <p>CoordinatorLayout is intended for two primary use cases:</p>
 * <ol>
 *     <li>As a top-level application decor or chrome layout</li>
 *     <li>As a container for a specific interaction with one or more child views</li>
 * </ol>
 *
 * <p>By specifying {@link Behavior Behaviors} for child views of a
 * CoordinatorLayout you can provide many different interactions within a single parent and those
 * views can also interact with one another. View classes can specify a default behavior when
 * used as a child of a CoordinatorLayout by implementing the
 * {@link AttachedBehavior} interface.</p>
 *
 * <p>Behaviors may be used to implement a variety of interactions and additional layout
 * modifications ranging from sliding drawers and panels to swipe-dismissable elements and buttons
 * that stick to other elements as they move and animate.</p>
 *
 * <p>Children of a CoordinatorLayout may have an
 * {@link LayoutParams#setAnchorId(int) anchor}. This view id must correspond
 * to an arbitrary descendant of the CoordinatorLayout, but it may not be the anchored child itself
 * or a descendant of the anchored child. This can be used to place floating views relative to
 * other arbitrary content panes.</p>
 *
 * <p>Children can specify {@link LayoutParams#insetEdge} to describe how the
 * view insets the CoordinatorLayout. Any child views which are set to dodge the same inset edges by
 * {@link LayoutParams#dodgeInsetEdges} will be moved appropriately so that the
 * views do not overlap.</p>
 */
@SuppressWarnings("unused")
public class CoordinatorLayout extends ViewGroup {

    private static final ThreadLocal<Matrix4> sMatrix = ThreadLocal.withInitial(Matrix4::identity);
    private static final ThreadLocal<RectF> sRectF = ThreadLocal.withInitial(RectF::new);

    private static final int TYPE_ON_INTERCEPT = 0;
    private static final int TYPE_ON_TOUCH = 1;

    @Retention(RetentionPolicy.SOURCE)
    @MagicConstant(intValues = {EVENT_PRE_DRAW, EVENT_NESTED_SCROLL, EVENT_VIEW_REMOVED})
    public @interface DispatchChangeEvent {
    }

    static final int EVENT_PRE_DRAW = 0;
    static final int EVENT_NESTED_SCROLL = 1;
    static final int EVENT_VIEW_REMOVED = 2;

    /**
     * Sorts child views with higher Z values to the beginning of a collection.
     */
    static final Comparator<View> TOP_SORTED_CHILDREN_COMPARATOR = (lhs, rhs) -> Float.compare(rhs.getZ(), lhs.getZ());

    private static final Pools.Pool<Rect> sRectPool = Pools.newSynchronizedPool(12);

    @Nonnull
    private static Rect acquireTempRect() {
        Rect rect = sRectPool.acquire();
        if (rect == null) {
            rect = new Rect();
        }
        return rect;
    }

    private static void releaseTempRect(@Nonnull Rect rect) {
        rect.setEmpty();
        sRectPool.release(rect);
    }

    private final List<View> mDependencySortedChildren = new ArrayList<>();
    private final DirectedAcyclicGraph<View> mChildDag = new DirectedAcyclicGraph<>();

    private final List<View> mTempList1 = new ArrayList<>();

    private final int[] mBehaviorConsumed = new int[2];

    private boolean mDisallowInterceptReset;

    private View mBehaviorTouchView;
    private View mNestedScrollingTarget;

    private OnPreDrawListener mOnPreDrawListener;
    private boolean mNeedsPreDrawListener;

    public CoordinatorLayout() {
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        resetTouchBehaviors();
        if (mNeedsPreDrawListener) {
            if (mOnPreDrawListener == null) {
                mOnPreDrawListener = new OnPreDrawListener();
            }
            final ViewTreeObserver vto = getViewTreeObserver();
            vto.addOnPreDrawListener(mOnPreDrawListener);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        resetTouchBehaviors();
        if (mNeedsPreDrawListener && mOnPreDrawListener != null) {
            final ViewTreeObserver vto = getViewTreeObserver();
            vto.removeOnPreDrawListener(mOnPreDrawListener);
        }
        if (mNestedScrollingTarget != null) {
            onStopNestedScroll(mNestedScrollingTarget, TYPE_TOUCH);
        }
    }

    private void cancelInterceptBehaviors() {
        MotionEvent cancelEvent = null;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final Behavior<View> b = lp.getBehavior();
            if (b != null) {
                if (cancelEvent == null) {
                    final long now = Core.timeNanos();
                    cancelEvent = MotionEvent.obtain(now,
                            MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
                }
                b.onInterceptTouchEvent(this, child, cancelEvent);
            }
        }
        if (cancelEvent != null) {
            cancelEvent.recycle();
        }
    }

    /**
     * Reset all Behavior-related tracking records either to clean up or in preparation
     * for a new event stream. This should be called when attached or detached from a window,
     * in response to an UP or CANCEL event, when intercept is request-disallowed
     * and similar cases where an event stream in progress will be aborted.
     */
    private void resetTouchBehaviors() {
        if (mBehaviorTouchView != null) {
            final LayoutParams lp = (LayoutParams) mBehaviorTouchView.getLayoutParams();
            final Behavior<View> b = lp.getBehavior();
            if (b != null) {
                final long now = Core.timeNanos();
                final MotionEvent cancelEvent = MotionEvent.obtain(now,
                        MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
                b.onTouchEvent(this, mBehaviorTouchView, cancelEvent);
                cancelEvent.recycle();
            }
            mBehaviorTouchView = null;
        }

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.resetTouchBehaviorTracking();
        }
        mDisallowInterceptReset = false;
    }

    /**
     * Populate a list with the current child views, sorted such that the topmost views
     * in z-order are at the front of the list. Useful for hit testing and event dispatch.
     */
    private void getTopSortedChildren(@Nonnull List<View> out) {
        out.clear();

        final boolean useCustomOrder = isChildrenDrawingOrderEnabled();
        final int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            final int childIndex = useCustomOrder ? getChildDrawingOrder(childCount, i) : i;
            final View child = getChildAt(childIndex);
            out.add(child);
        }

        out.sort(TOP_SORTED_CHILDREN_COMPARATOR);
    }

    private boolean performIntercept(@Nonnull final MotionEvent ev, final int type) {
        boolean intercepted = false;
        boolean newBlock = false;

        final int action = ev.getAction();

        MotionEvent cancelEvent = null;

        final List<View> topmostChildList = mTempList1;
        getTopSortedChildren(topmostChildList);

        // Let topmost child views inspect first
        final int childCount = topmostChildList.size();
        for (int i = 0; i < childCount; i++) {
            final View child = topmostChildList.get(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final Behavior<View> b = lp.getBehavior();

            if ((intercepted || newBlock) && action != MotionEvent.ACTION_DOWN) {
                // Cancel all behaviors beneath the one that intercepted.
                // If the event is "down" then we don't have anything to cancel yet.
                if (b != null) {
                    if (cancelEvent == null) cancelEvent = obtainCancelEvent(ev);
                    performEvent(b, child, cancelEvent, type);
                }
                continue;
            }

            if (!newBlock && !intercepted && b != null) {
                intercepted = performEvent(b, child, ev, type);
                if (intercepted) {
                    mBehaviorTouchView = child;
                    // If a behavior intercepted an event then send cancel events to all the prior
                    // behaviors.
                    if (action != MotionEvent.ACTION_CANCEL && action != MotionEvent.ACTION_UP) {
                        for (int j = 0; j < i; j++) {
                            final View priorChild = topmostChildList.get(j);
                            final Behavior<View> priorBehavior =
                                    ((LayoutParams) priorChild.getLayoutParams()).getBehavior();
                            if (priorBehavior != null) {
                                if (cancelEvent == null) cancelEvent = obtainCancelEvent(ev);
                                performEvent(priorBehavior, priorChild, cancelEvent, type);
                            }
                        }
                    }
                }
            }

            // Don't keep going if we're not allowing interaction below this.
            // Setting newBlock will make sure we cancel the rest of the behaviors.
            final boolean wasBlocking = lp.didBlockInteraction();
            final boolean isBlocking = lp.isBlockingInteractionBelow(this, child);
            newBlock = isBlocking && !wasBlocking;
            if (isBlocking && !newBlock) {
                // Stop here since we don't have anything more to cancel - we already did
                // when the behavior first started blocking things below this point.
                break;
            }
        }

        topmostChildList.clear();

        if (cancelEvent != null) {
            cancelEvent.recycle();
        }

        return intercepted;
    }

    private boolean performEvent(final Behavior<View> behavior, final View child, final MotionEvent ev,
                                 final int type) {
        switch (type) {
            case TYPE_ON_INTERCEPT:
                return behavior.onInterceptTouchEvent(this, child, ev);
            case TYPE_ON_TOUCH:
                return behavior.onTouchEvent(this, child, ev);
        }
        throw new IllegalArgumentException();
    }

    @Nonnull
    private MotionEvent obtainCancelEvent(@Nonnull MotionEvent other) {
        MotionEvent event = other.copy();
        event.setAction(MotionEvent.ACTION_CANCEL);
        return event;
    }

    @Override
    public boolean onInterceptTouchEvent(@Nonnull MotionEvent ev) {
        final int action = ev.getAction();

        // Make sure we reset in case we had missed a previous important event.
        if (action == MotionEvent.ACTION_DOWN) {
            resetTouchBehaviors();
        }

        final boolean intercepted = performIntercept(ev, TYPE_ON_INTERCEPT);

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            // We have already sent this event to the behavior that was handling the touch so clear
            // it out before reseting the touch state to avoid sending it another cancel event.
            mBehaviorTouchView = null;
            resetTouchBehaviors();
        }

        return intercepted;
    }

    @Override
    public boolean onTouchEvent(@Nonnull MotionEvent ev) {
        boolean handled = false;
        boolean cancelSuper = false;

        final int action = ev.getAction();

        if (mBehaviorTouchView != null) {
            final LayoutParams lp = (LayoutParams) mBehaviorTouchView.getLayoutParams();
            final Behavior<View> b = lp.getBehavior();
            if (b != null) {
                handled = b.onTouchEvent(this, mBehaviorTouchView, ev);
            }
        } else {
            handled = performIntercept(ev, TYPE_ON_TOUCH);
            cancelSuper = action != MotionEvent.ACTION_DOWN && handled;
        }

        // Keep the super implementation correct
        if (mBehaviorTouchView == null || action == MotionEvent.ACTION_CANCEL) {
            handled |= super.onTouchEvent(ev);
        } else if (cancelSuper) {
            MotionEvent cancelEvent = obtainCancelEvent(ev);
            super.onTouchEvent(cancelEvent);
            cancelEvent.recycle();
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            // We have already sent this event to the behavior that was handling the touch so clear
            // it out before reseting the touch state to avoid sending it another cancel event.
            mBehaviorTouchView = null;
            resetTouchBehaviors();
        }

        return handled;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
        if (disallowIntercept && !mDisallowInterceptReset) {
            // If there is no behavior currently handling touches then send a cancel event to all
            // behavior's intercept methods.
            if (mBehaviorTouchView == null) {
                cancelInterceptBehaviors();
            }
            resetTouchBehaviors();
            mDisallowInterceptReset = true;
        }
    }

    LayoutParams getResolvedLayoutParams(@Nonnull View child) {
        final LayoutParams result = (LayoutParams) child.getLayoutParams();
        if (!result.mBehaviorResolved) {
            if (child instanceof AttachedBehavior) {
                Behavior<?> attachedBehavior = ((AttachedBehavior) child).getBehavior();
                result.setBehavior(attachedBehavior);
                result.mBehaviorResolved = true;
            }
        }
        return result;
    }

    private void prepareChildren() {
        mDependencySortedChildren.clear();
        mChildDag.clear();

        for (int i = 0, count = getChildCount(); i < count; i++) {
            final View view = getChildAt(i);

            final LayoutParams lp = getResolvedLayoutParams(view);
            lp.findAnchorView(this, view);

            mChildDag.addNode(view);

            // Now iterate again over the other children, adding any dependencies to the graph
            for (int j = 0; j < count; j++) {
                if (j == i) {
                    continue;
                }
                final View other = getChildAt(j);
                if (lp.dependsOn(this, view, other)) {
                    if (!mChildDag.contains(other)) {
                        // Make sure that the other node is added
                        mChildDag.addNode(other);
                    }
                    // Now add the dependency to the graph
                    mChildDag.addEdge(other, view);
                }
            }
        }

        // Finally add the sorted graph list to our list
        mDependencySortedChildren.addAll(mChildDag.getSortedList());
        // We also need to reverse the result since we want the start of the list to contain
        // Views which have no dependencies, then dependent views after that
        Collections.reverse(mDependencySortedChildren);
    }

    /**
     * Retrieve the transformed bounding rect of an arbitrary descendant view.
     * This does not need to be a direct child.
     *
     * @param descendant descendant view to reference
     * @param out        rect to set to the bounds of the descendant view
     */
    void getDescendantRect(@Nonnull View descendant, @Nonnull Rect out) {
        out.set(0, 0, descendant.getWidth(), descendant.getHeight());
        offsetDescendantRect(descendant, out);
    }

    /**
     * This is a port of the common
     * {@link ViewGroup#offsetDescendantRectToMyCoords(View, Rect)}
     * from the framework, but adapted to take transformations into account. The result
     * will be the bounding rect of the real transformed rect.
     *
     * @param descendant view defining the original coordinate system of rect
     * @param rect       (in/out) the rect to offset from descendant to this view's coordinate system
     */
    void offsetDescendantRect(View descendant, Rect rect) {
        Matrix4 m = sMatrix.get();
        m.setIdentity();

        offsetDescendantMatrix(descendant, m);

        RectF rectF = sRectF.get();
        rectF.set(rect);
        m.mapRect(rectF);
        rectF.round(rect);
    }

    private void offsetDescendantMatrix(@Nonnull View view, Matrix4 m) {
        final ViewParent parent = view.getParent();
        if (parent instanceof final View vp && parent != this) {
            offsetDescendantMatrix(vp, m);
            m.preTranslate(-vp.getScrollX(), -vp.getScrollY());
        }

        m.preTranslate(view.getLeft(), view.getTop());

        if (!view.getMatrix().isIdentity()) {
            m.preConcat(view.getMatrix());
        }
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return Math.max(super.getSuggestedMinimumWidth(), getPaddingLeft() + getPaddingRight());
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return Math.max(super.getSuggestedMinimumHeight(), getPaddingTop() + getPaddingBottom());
    }

    /**
     * Called to measure each individual child view unless a
     * {@link Behavior Behavior} is present. The Behavior may choose to delegate
     * child measurement to this method.
     *
     * @param child                   the child to measure
     * @param parentWidthMeasureSpec  the width requirements for this view
     * @param widthUsed               extra space that has been used up by the parent
     *                                horizontally (possibly by other children of the parent)
     * @param parentHeightMeasureSpec the height requirements for this view
     * @param heightUsed              extra space that has been used up by the parent
     *                                vertically (possibly by other children of the parent)
     */
    public void onMeasureChild(View child, int parentWidthMeasureSpec, int widthUsed,
                               int parentHeightMeasureSpec, int heightUsed) {
        measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed,
                parentHeightMeasureSpec, heightUsed);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        prepareChildren();
        ensurePreDrawListener();

        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int paddingRight = getPaddingRight();
        final int paddingBottom = getPaddingBottom();

        final int widthPadding = paddingLeft + paddingRight;
        final int heightPadding = paddingTop + paddingBottom;
        int widthUsed = getSuggestedMinimumWidth();
        int heightUsed = getSuggestedMinimumHeight();
        int childState = 0;

        for (final View child : mDependencySortedChildren) {
            if (child.getVisibility() == GONE) {
                // If the child is GONE, skip...
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            final Behavior<View> b = lp.getBehavior();
            if (b == null || !b.onMeasureChild(this, child, widthMeasureSpec, 0,
                    heightMeasureSpec, 0)) {
                onMeasureChild(child, widthMeasureSpec, 0,
                        heightMeasureSpec, 0);
            }

            widthUsed = Math.max(widthUsed, widthPadding + child.getMeasuredWidth() +
                    lp.leftMargin + lp.rightMargin);

            heightUsed = Math.max(heightUsed, heightPadding + child.getMeasuredHeight() +
                    lp.topMargin + lp.bottomMargin);
            childState = View.combineMeasuredStates(childState, child.getMeasuredState());
        }

        final int width = View.resolveSizeAndState(widthUsed, widthMeasureSpec,
                childState & View.MEASURED_STATE_MASK);
        final int height = View.resolveSizeAndState(heightUsed, heightMeasureSpec,
                childState << View.MEASURED_HEIGHT_STATE_SHIFT);
        setMeasuredDimension(width, height);
    }

    /**
     * Called to lay out each individual child view unless a
     * {@link Behavior Behavior} is present. The Behavior may choose to
     * delegate child measurement to this method.
     *
     * @param child           child view to lay out
     * @param layoutDirection the resolved layout direction for the CoordinatorLayout, such as
     *                        {@link #LAYOUT_DIRECTION_LTR} or
     *                        {@link #LAYOUT_DIRECTION_RTL}.
     */
    public void onLayoutChild(@Nonnull View child, int layoutDirection) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.checkAnchorChanged()) {
            throw new IllegalStateException("An anchor may not be changed after CoordinatorLayout"
                    + " measurement begins before layout is complete.");
        }
        if (lp.mAnchorView != null) {
            layoutChildWithAnchor(child, lp.mAnchorView, layoutDirection);
        } else {
            layoutChild(child, layoutDirection);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int layoutDirection = getLayoutDirection();
        for (final View child : mDependencySortedChildren) {
            if (child.getVisibility() == GONE) {
                // If the child is GONE, skip...
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final Behavior<View> behavior = lp.getBehavior();

            if (behavior == null || !behavior.onLayoutChild(this, child, layoutDirection)) {
                onLayoutChild(child, layoutDirection);
            }
        }
    }

    /**
     * Mark the last known child position rect for the given child view.
     * This will be used when checking if a child view's position has changed between frames.
     * The rect used here should be one returned by
     * {@link #getChildRect(View, boolean, Rect)}, with translation
     * disabled.
     *
     * @param child child view to set for
     * @param r     rect to set
     */
    void recordLastChildRect(@Nonnull View child, Rect r) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        lp.setLastChildRect(r);
    }

    /**
     * Get the last known child rect recorded by
     * {@link #recordLastChildRect(View, Rect)}.
     *
     * @param child child view to retrieve from
     * @param out   rect to set to the output values
     */
    void getLastChildRect(@Nonnull View child, @Nonnull Rect out) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        out.set(lp.getLastChildRect());
    }

    /**
     * Get the position rect for the given child. If the child has currently requested layout
     * or has a visibility of GONE.
     *
     * @param child     child view to check
     * @param transform true to include transformation in the output rect, false to
     *                  only account for the base position
     * @param out       rect to set to the output values
     */
    void getChildRect(@Nonnull View child, boolean transform, Rect out) {
        if (child.isLayoutRequested() || child.getVisibility() == View.GONE) {
            out.setEmpty();
            return;
        }
        if (transform) {
            getDescendantRect(child, out);
        } else {
            out.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
        }
    }

    private void getDesiredAnchoredChildRectWithoutConstraints(
            int layoutDirection, @Nonnull Rect anchorRect, Rect out,
            @Nonnull LayoutParams lp, int childWidth, int childHeight) {
        final int absGravity = Gravity.getAbsoluteGravity(
                resolveAnchoredChildGravity(lp.gravity), layoutDirection);
        final int absAnchorGravity = Gravity.getAbsoluteGravity(
                resolveGravity(lp.anchorGravity),
                layoutDirection);

        final int hgrav = absGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        final int vgrav = absGravity & Gravity.VERTICAL_GRAVITY_MASK;
        final int anchorHgrav = absAnchorGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        final int anchorVgrav = absAnchorGravity & Gravity.VERTICAL_GRAVITY_MASK;

        int left;
        int top;

        // Align to the anchor. This puts us in an assumed right/bottom child view gravity.
        // If this is not the case we will subtract out the appropriate portion of
        // the child size below.
        left = switch (anchorHgrav) {
            case Gravity.RIGHT -> anchorRect.right;
            case Gravity.CENTER_HORIZONTAL -> anchorRect.left + anchorRect.width() / 2;
            default -> anchorRect.left;
        };

        top = switch (anchorVgrav) {
            case Gravity.BOTTOM -> anchorRect.bottom;
            case Gravity.CENTER_VERTICAL -> anchorRect.top + anchorRect.height() / 2;
            default -> anchorRect.top;
        };

        // Offset by the child view's gravity itself. The above assumed right/bottom gravity.
        switch (hgrav) {
            default:
            case Gravity.LEFT:
                left -= childWidth;
                break;
            case Gravity.RIGHT:
                // Do nothing, we're already in position.
                break;
            case Gravity.CENTER_HORIZONTAL:
                left -= childWidth / 2;
                break;
        }

        switch (vgrav) {
            default:
            case Gravity.TOP:
                top -= childHeight;
                break;
            case Gravity.BOTTOM:
                // Do nothing, we're already in position.
                break;
            case Gravity.CENTER_VERTICAL:
                top -= childHeight / 2;
                break;
        }

        out.set(left, top, left + childWidth, top + childHeight);
    }

    private void constrainChildRect(@Nonnull LayoutParams lp, Rect out, int childWidth, int childHeight) {
        final int width = getWidth();
        final int height = getHeight();

        // Obey margins and padding
        int left = Math.max(getPaddingLeft() + lp.leftMargin,
                Math.min(out.left,
                        width - getPaddingRight() - childWidth - lp.rightMargin));
        int top = Math.max(getPaddingTop() + lp.topMargin,
                Math.min(out.top,
                        height - getPaddingBottom() - childHeight - lp.bottomMargin));

        out.set(left, top, left + childWidth, top + childHeight);
    }

    /**
     * Calculate the desired child rect relative to an anchor rect, respecting both
     * gravity and anchorGravity.
     *
     * @param child           child view to calculate a rect for
     * @param layoutDirection the desired layout direction for the CoordinatorLayout
     * @param anchorRect      rect in CoordinatorLayout coordinates of the anchor view area
     * @param out             rect to set to the output values
     */
    void getDesiredAnchoredChildRect(@Nonnull View child, int layoutDirection, Rect anchorRect, Rect out) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final int childWidth = child.getMeasuredWidth();
        final int childHeight = child.getMeasuredHeight();
        getDesiredAnchoredChildRectWithoutConstraints(layoutDirection, anchorRect, out, lp,
                childWidth, childHeight);
        constrainChildRect(lp, out, childWidth, childHeight);
    }

    /**
     * CORE ASSUMPTION: anchor has been laid out by the time this is called for a given child view.
     *
     * @param child           child to lay out
     * @param anchor          view to anchor child relative to; already laid out.
     * @param layoutDirection ViewCompat constant for layout direction
     */
    private void layoutChildWithAnchor(View child, View anchor, int layoutDirection) {
        final Rect anchorRect = acquireTempRect();
        final Rect childRect = acquireTempRect();
        try {
            getDescendantRect(anchor, anchorRect);
            getDesiredAnchoredChildRect(child, layoutDirection, anchorRect, childRect);
            child.layout(childRect.left, childRect.top, childRect.right, childRect.bottom);
        } finally {
            releaseTempRect(anchorRect);
            releaseTempRect(childRect);
        }
    }

    /**
     * Lay out a child view with no special handling. This will position the child as
     * if it were within a FrameLayout or similar simple frame.
     *
     * @param child           child view to lay out
     * @param layoutDirection ViewCompat constant for the desired layout direction
     */
    private void layoutChild(@Nonnull View child, int layoutDirection) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final Rect parent = acquireTempRect();
        parent.set(getPaddingLeft() + lp.leftMargin,
                getPaddingTop() + lp.topMargin,
                getWidth() - getPaddingRight() - lp.rightMargin,
                getHeight() - getPaddingBottom() - lp.bottomMargin);

        final Rect out = acquireTempRect();
        Gravity.apply(resolveGravity(lp.gravity), child.getMeasuredWidth(),
                child.getMeasuredHeight(), parent, out, layoutDirection);
        child.layout(out.left, out.top, out.right, out.bottom);

        releaseTempRect(parent);
        releaseTempRect(out);
    }

    /**
     * Return the given gravity value, but if either or both of the axes doesn't have any gravity
     * specified, the default value (start or top) is specified. This should be used for children
     * that are not anchored to another view.
     */
    private static int resolveGravity(int gravity) {
        if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.NO_GRAVITY) {
            gravity |= Gravity.START;
        }
        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.NO_GRAVITY) {
            gravity |= Gravity.TOP;
        }
        return gravity;
    }

    /**
     * Return the given gravity value or the default if the passed value is NO_GRAVITY.
     * This should be used for children that are anchored to another view.
     */
    private static int resolveAnchoredChildGravity(int gravity) {
        return gravity == Gravity.NO_GRAVITY ? Gravity.CENTER : gravity;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void drawChild(@Nonnull Canvas canvas, @Nonnull View child, long drawingTime) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mBehavior != null) {
            final float scrimAlpha = lp.mBehavior.getScrimOpacity(this, child);
            if (scrimAlpha > 0f) {
                Paint paint = Paint.obtain();
                paint.setColor(lp.mBehavior.getScrimColor(this, child));
                paint.setAlpha(MathUtil.clamp(Math.round(255 * scrimAlpha), 0, 255));

                // Now draw the rectangle for the scrim
                canvas.drawRect(getPaddingLeft(), getPaddingTop(),
                        getWidth() - getPaddingRight(), getHeight() - getPaddingBottom(),
                        paint);
                paint.recycle();
            }
        }
        super.drawChild(canvas, child, drawingTime);
    }

    /**
     * Dispatch any dependent view changes to the relevant {@link Behavior} instances.
     * <p>
     * Usually run as part of the pre-draw step when at least one child view has a reported
     * dependency on another view. This allows CoordinatorLayout to account for layout
     * changes and animations that occur outside of the normal layout pass.
     * <p>
     * It can also be run as part of the nested scrolling dispatch to ensure that any offsetting
     * is completed within the correct coordinate window.
     * <p>
     * The offsetting behavior implemented here does not store the computed offset in
     * the LayoutParams; instead it expects that the layout process will always reconstruct
     * the proper positioning.
     *
     * @param type the type of event which has caused this call
     */
    final void onChildViewsChanged(@DispatchChangeEvent final int type) {
        final int layoutDirection = getLayoutDirection();
        final int childCount = mDependencySortedChildren.size();
        final Rect inset = acquireTempRect();
        final Rect drawRect = acquireTempRect();
        final Rect lastDrawRect = acquireTempRect();

        for (int i = 0; i < childCount; i++) {
            final View child = mDependencySortedChildren.get(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (type == EVENT_PRE_DRAW && child.getVisibility() == View.GONE) {
                // Do not try to update GONE child views in pre draw updates.
                continue;
            }

            // Check child views before for anchor
            for (int j = 0; j < i; j++) {
                final View checkChild = mDependencySortedChildren.get(j);

                if (lp.mAnchorDirectChild == checkChild) {
                    offsetChildToAnchor(child, layoutDirection);
                }
            }

            // Get the current draw rect of the view
            getChildRect(child, true, drawRect);

            // Accumulate inset sizes
            if (lp.insetEdge != Gravity.NO_GRAVITY && !drawRect.isEmpty()) {
                final int absInsetEdge = Gravity.getAbsoluteGravity(
                        lp.insetEdge, layoutDirection);
                switch (absInsetEdge & Gravity.VERTICAL_GRAVITY_MASK) {
                    case Gravity.TOP -> inset.top = Math.max(inset.top, drawRect.bottom);
                    case Gravity.BOTTOM -> inset.bottom = Math.max(inset.bottom, getHeight() - drawRect.top);
                }
                switch (absInsetEdge & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.LEFT -> inset.left = Math.max(inset.left, drawRect.right);
                    case Gravity.RIGHT -> inset.right = Math.max(inset.right, getWidth() - drawRect.left);
                }
            }

            // Dodge inset edges if necessary
            if (lp.dodgeInsetEdges != Gravity.NO_GRAVITY && child.getVisibility() == View.VISIBLE) {
                offsetChildByInset(child, inset, layoutDirection);
            }

            if (type != EVENT_VIEW_REMOVED) {
                // Did it change? if not continue
                getLastChildRect(child, lastDrawRect);
                if (lastDrawRect.equals(drawRect)) {
                    continue;
                }
                recordLastChildRect(child, drawRect);
            }

            // Update any behavior-dependent views for the change
            for (int j = i + 1; j < childCount; j++) {
                final View checkChild = mDependencySortedChildren.get(j);
                final LayoutParams checkLp = (LayoutParams) checkChild.getLayoutParams();
                final Behavior<View> b = checkLp.getBehavior();

                if (b != null && b.layoutDependsOn(this, checkChild, child)) {
                    if (type == EVENT_PRE_DRAW && checkLp.getChangedAfterNestedScroll()) {
                        // If this is from a pre-draw and we have already been changed
                        // from a nested scroll, skip the dispatch and reset the flag
                        checkLp.resetChangedAfterNestedScroll();
                        continue;
                    }

                    final boolean handled;
                    if (type == EVENT_VIEW_REMOVED) {// EVENT_VIEW_REMOVED means that we need to dispatch
                        // onDependentViewRemoved() instead
                        b.onDependentViewRemoved(this, checkChild, child);
                        handled = true;
                    } else {// Otherwise we dispatch onDependentViewChanged()
                        handled = b.onDependentViewChanged(this, checkChild, child);
                    }

                    if (type == EVENT_NESTED_SCROLL) {
                        // If this is from a nested scroll, set the flag so that we may skip
                        // any resulting onPreDraw dispatch (if needed)
                        checkLp.setChangedAfterNestedScroll(handled);
                    }
                }
            }
        }

        releaseTempRect(inset);
        releaseTempRect(drawRect);
        releaseTempRect(lastDrawRect);
    }

    private void offsetChildByInset(@Nonnull final View child, final Rect inset, final int layoutDirection) {
        if (!child.isLaidOut()) {
            // The view has not been laid out yet, so we can't obtain its bounds.
            return;
        }

        if (child.getWidth() <= 0 || child.getHeight() <= 0) {
            // Bounds are empty so there is nothing to dodge against, skip...
            return;
        }

        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final Behavior<View> behavior = lp.getBehavior();
        final Rect dodgeRect = acquireTempRect();
        final Rect bounds = acquireTempRect();
        bounds.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());

        if (behavior != null && behavior.getInsetDodgeRect(this, child, dodgeRect)) {
            // Make sure that the rect is within the view's bounds
            if (!bounds.contains(dodgeRect)) {
                throw new IllegalArgumentException("Rect should be within the child's bounds."
                        + " Rect:" + dodgeRect.toShortString()
                        + " | Bounds:" + bounds.toShortString());
            }
        } else {
            dodgeRect.set(bounds);
        }

        // We can release the bounds rect now
        releaseTempRect(bounds);

        if (dodgeRect.isEmpty()) {
            // Rect is empty so there is nothing to dodge against, skip...
            releaseTempRect(dodgeRect);
            return;
        }

        final int absDodgeInsetEdges = Gravity.getAbsoluteGravity(lp.dodgeInsetEdges,
                layoutDirection);

        boolean offsetY = false;
        if ((absDodgeInsetEdges & Gravity.TOP) == Gravity.TOP) {
            int distance = dodgeRect.top - lp.topMargin - lp.mInsetOffsetY;
            if (distance < inset.top) {
                setInsetOffsetY(child, inset.top - distance);
                offsetY = true;
            }
        }
        if ((absDodgeInsetEdges & Gravity.BOTTOM) == Gravity.BOTTOM) {
            int distance = getHeight() - dodgeRect.bottom - lp.bottomMargin + lp.mInsetOffsetY;
            if (distance < inset.bottom) {
                setInsetOffsetY(child, distance - inset.bottom);
                offsetY = true;
            }
        }
        if (!offsetY) {
            setInsetOffsetY(child, 0);
        }

        boolean offsetX = false;
        if ((absDodgeInsetEdges & Gravity.LEFT) == Gravity.LEFT) {
            int distance = dodgeRect.left - lp.leftMargin - lp.mInsetOffsetX;
            if (distance < inset.left) {
                setInsetOffsetX(child, inset.left - distance);
                offsetX = true;
            }
        }
        if ((absDodgeInsetEdges & Gravity.RIGHT) == Gravity.RIGHT) {
            int distance = getWidth() - dodgeRect.right - lp.rightMargin + lp.mInsetOffsetX;
            if (distance < inset.right) {
                setInsetOffsetX(child, distance - inset.right);
                offsetX = true;
            }
        }
        if (!offsetX) {
            setInsetOffsetX(child, 0);
        }

        releaseTempRect(dodgeRect);
    }

    private void setInsetOffsetX(@Nonnull View child, int offsetX) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mInsetOffsetX != offsetX) {
            final int dx = offsetX - lp.mInsetOffsetX;
            child.offsetLeftAndRight(dx);
            lp.mInsetOffsetX = offsetX;
        }
    }

    private void setInsetOffsetY(@Nonnull View child, int offsetY) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mInsetOffsetY != offsetY) {
            final int dy = offsetY - lp.mInsetOffsetY;
            child.offsetTopAndBottom(dy);
            lp.mInsetOffsetY = offsetY;
        }
    }

    /**
     * Allows the caller to manually dispatch
     * {@link Behavior#onDependentViewChanged(CoordinatorLayout, View, View)} to the associated
     * {@link Behavior} instances of views which depend on the provided {@link View}.
     *
     * <p>You should not normally need to call this method as the it will be automatically done
     * when the view has changed.
     *
     * @param view the View to find dependents of to dispatch the call.
     */
    public void dispatchDependentViewsChanged(@Nonnull View view) {
        final List<View> dependents = mChildDag.getIncomingEdgesInternal(view);
        if (dependents != null && !dependents.isEmpty()) {
            for (final View child : dependents) {
                LayoutParams lp = (LayoutParams)
                        child.getLayoutParams();
                Behavior<View> b = lp.getBehavior();
                if (b != null) {
                    b.onDependentViewChanged(this, child, view);
                }
            }
        }
    }

    /**
     * Returns a new list containing the views on which the provided view depends.
     *
     * @param child the view to find dependencies for
     * @return a new list of views on which {@code child} depends
     */
    @Nonnull
    public List<View> getDependencies(@Nonnull View child) {
        List<View> result = mChildDag.getOutgoingEdges(child);
        return result == null ? Collections.emptyList() : result;
    }

    /**
     * Returns a new list of views which depend on the provided view.
     *
     * @param child the view to find dependents of
     * @return a new list of views which depend on {@code child}
     */
    @Nonnull
    public List<View> getDependents(@Nonnull View child) {
        List<View> result = mChildDag.getIncomingEdges(child);
        return result == null ? Collections.emptyList() : result;
    }

    @Nonnull
    @UnmodifiableView
    @VisibleForTesting
    public final List<View> getDependencySortedChildren() {
        prepareChildren();
        return Collections.unmodifiableList(mDependencySortedChildren);
    }

    /**
     * Add or remove the pre-draw listener as necessary.
     */
    void ensurePreDrawListener() {
        boolean hasDependencies = false;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (hasDependencies(child)) {
                hasDependencies = true;
                break;
            }
        }

        if (hasDependencies != mNeedsPreDrawListener) {
            if (hasDependencies) {
                addPreDrawListener();
            } else {
                removePreDrawListener();
            }
        }
    }

    /**
     * Check if the given child has any layout dependencies on other child views.
     */
    private boolean hasDependencies(View child) {
        return mChildDag.hasOutgoingEdges(child);
    }

    /**
     * Add the pre-draw listener if we're attached to a window and mark that we currently
     * need it when attached.
     */
    void addPreDrawListener() {
        if (isAttachedToWindow()) {
            // Add the listener
            if (mOnPreDrawListener == null) {
                mOnPreDrawListener = new OnPreDrawListener();
            }
            final ViewTreeObserver vto = getViewTreeObserver();
            vto.addOnPreDrawListener(mOnPreDrawListener);
        }

        // Record that we need the listener regardless of whether or not we're attached.
        // We'll add the real listener when we become attached.
        mNeedsPreDrawListener = true;
    }

    /**
     * Remove the pre-draw listener if we're attached to a window and mark that we currently
     * do not need it when attached.
     */
    void removePreDrawListener() {
        if (isAttachedToWindow()) {
            if (mOnPreDrawListener != null) {
                final ViewTreeObserver vto = getViewTreeObserver();
                vto.removeOnPreDrawListener(mOnPreDrawListener);
            }
        }
        mNeedsPreDrawListener = false;
    }

    /**
     * Adjust the child left, top, right, bottom rect to the correct anchor view position,
     * respecting gravity and anchor gravity.
     * <p>
     * Note that child translation properties are ignored in this process, allowing children
     * to be animated away from their anchor. However, if the anchor view is animated,
     * the child will be offset to match the anchor's translated position.
     */
    void offsetChildToAnchor(@Nonnull View child, int layoutDirection) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mAnchorView != null) {
            final Rect anchorRect = acquireTempRect();
            final Rect childRect = acquireTempRect();
            final Rect desiredChildRect = acquireTempRect();

            getDescendantRect(lp.mAnchorView, anchorRect);
            getChildRect(child, false, childRect);

            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            getDesiredAnchoredChildRectWithoutConstraints(layoutDirection, anchorRect,
                    desiredChildRect, lp, childWidth, childHeight);
            boolean changed = desiredChildRect.left != childRect.left ||
                    desiredChildRect.top != childRect.top;
            constrainChildRect(lp, desiredChildRect, childWidth, childHeight);

            final int dx = desiredChildRect.left - childRect.left;
            final int dy = desiredChildRect.top - childRect.top;

            if (dx != 0) {
                child.offsetLeftAndRight(dx);
            }
            if (dy != 0) {
                child.offsetTopAndBottom(dy);
            }

            if (changed) {
                // If we have needed to move, make sure to notify the child's Behavior
                final Behavior<View> b = lp.getBehavior();
                if (b != null) {
                    b.onDependentViewChanged(this, child, lp.mAnchorView);
                }
            }

            releaseTempRect(anchorRect);
            releaseTempRect(childRect);
            releaseTempRect(desiredChildRect);
        }
    }

    /**
     * Check if a given point in the CoordinatorLayout's coordinates are within the view bounds
     * of the given direct child view.
     *
     * @param child child view to test
     * @param x     X coordinate to test, in the CoordinatorLayout's coordinate system
     * @param y     Y coordinate to test, in the CoordinatorLayout's coordinate system
     * @return true if the point is within the child view's bounds, false otherwise
     */
    public boolean isPointInChildBounds(@Nonnull View child, int x, int y) {
        final Rect r = acquireTempRect();
        offsetDescendantRectToMyCoords(child, r);
        try {
            return r.contains(x, y);
        } finally {
            releaseTempRect(r);
        }
    }

    /**
     * Check whether two views overlap each other. The views need to be descendants of this
     * {@link CoordinatorLayout} in the view hierarchy.
     *
     * @param first  first child view to test
     * @param second second child view to test
     * @return true if both views are visible and overlap each other
     */
    public boolean doViewsOverlap(@Nonnull View first, @Nonnull View second) {
        if (first.getVisibility() == VISIBLE && second.getVisibility() == VISIBLE) {
            final Rect firstRect = acquireTempRect();
            getChildRect(first, first.getParent() != this, firstRect);
            final Rect secondRect = acquireTempRect();
            getChildRect(second, second.getParent() != this, secondRect);
            try {
                return !(firstRect.left > secondRect.right || firstRect.top > secondRect.bottom
                        || firstRect.right < secondRect.left || firstRect.bottom < secondRect.top);
            } finally {
                releaseTempRect(firstRect);
                releaseTempRect(secondRect);
            }
        }
        return false;
    }

    @Override
    protected void onViewRemoved(View child) {
        super.onViewRemoved(child);
        onChildViewsChanged(EVENT_VIEW_REMOVED);
    }

    @Override
    public boolean requestChildRectangleOnScreen(@Nonnull View child, Rect rectangle, boolean immediate) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final Behavior<View> behavior = lp.getBehavior();

        if (behavior != null
                && behavior.onRequestChildRectangleOnScreen(this, child, rectangle, immediate)) {
            return true;
        }

        return super.requestChildRectangleOnScreen(child, rectangle, immediate);
    }

    @Nonnull
    @Override
    protected LayoutParams generateLayoutParams(@Nonnull ViewGroup.LayoutParams p) {
        if (p instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) p);
        } else if (p instanceof FrameLayout.LayoutParams vp) {
            // consider CoordinatorLayout is a subclass of FrameLayout
            LayoutParams lp = new LayoutParams(vp);
            if (vp.gravity != FrameLayout.LayoutParams.UNSPECIFIED_GRAVITY) {
                lp.gravity = vp.gravity;
            }
            return lp;
        } else if (p instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) p);
        }
        return new LayoutParams(p);
    }

    @Nonnull
    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    public boolean onStartNestedScroll(@Nonnull View child, @Nonnull View target, int axes, int type) {
        boolean handled = false;

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            if (view.getVisibility() == View.GONE) {
                // If it's GONE, don't dispatch
                continue;
            }
            final LayoutParams lp = (LayoutParams) view.getLayoutParams();
            final Behavior<View> viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                final boolean accepted = viewBehavior.onStartNestedScroll(this, view, child,
                        target, axes, type);
                handled |= accepted;
                lp.setNestedScrollAccepted(type, accepted);
            } else {
                lp.setNestedScrollAccepted(type, false);
            }
        }
        return handled;
    }

    @Override
    public void onNestedScrollAccepted(@Nonnull View child, @Nonnull View target, int axes, int type) {
        super.onNestedScrollAccepted(child, target, axes, type);
        mNestedScrollingTarget = target;

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            final LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.isNestedScrollDenied(type)) {
                continue;
            }

            final Behavior<View> viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                viewBehavior.onNestedScrollAccepted(this, view, child, target,
                        axes, type);
            }
        }
    }

    @Override
    public void onStopNestedScroll(@Nonnull View target, int type) {
        super.onStopNestedScroll(target, type);

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            final LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.isNestedScrollDenied(type)) {
                continue;
            }

            final Behavior<View> viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                viewBehavior.onStopNestedScroll(this, view, target, type);
            }
            lp.resetNestedScroll(type);
            lp.resetChangedAfterNestedScroll();
        }
        mNestedScrollingTarget = null;
    }

    @Override
    public void onNestedScroll(@Nonnull View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                               int dyUnconsumed, int type, @Nonnull int[] consumed) {
        final int childCount = getChildCount();
        boolean accepted = false;
        int xConsumed = 0;
        int yConsumed = 0;

        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            if (view.getVisibility() == GONE) {
                // If the child is GONE, skip...
                continue;
            }

            final LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.isNestedScrollDenied(type)) {
                continue;
            }

            final Behavior<View> viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {

                mBehaviorConsumed[0] = 0;
                mBehaviorConsumed[1] = 0;

                viewBehavior.onNestedScroll(this, view, target, dxConsumed, dyConsumed,
                        dxUnconsumed, dyUnconsumed, type, mBehaviorConsumed);

                xConsumed = dxUnconsumed > 0 ? Math.max(xConsumed, mBehaviorConsumed[0])
                        : Math.min(xConsumed, mBehaviorConsumed[0]);
                yConsumed = dyUnconsumed > 0 ? Math.max(yConsumed, mBehaviorConsumed[1])
                        : Math.min(yConsumed, mBehaviorConsumed[1]);

                accepted = true;
            }
        }

        consumed[0] += xConsumed;
        consumed[1] += yConsumed;

        if (accepted) {
            onChildViewsChanged(EVENT_NESTED_SCROLL);
        }
    }

    @Override
    public void onNestedPreScroll(@Nonnull View target, int dx, int dy, @Nonnull int[] consumed, int type) {
        int xConsumed = 0;
        int yConsumed = 0;
        boolean accepted = false;

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            if (view.getVisibility() == GONE) {
                // If the child is GONE, skip...
                continue;
            }

            final LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.isNestedScrollDenied(type)) {
                continue;
            }

            final Behavior<View> viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                mBehaviorConsumed[0] = 0;
                mBehaviorConsumed[1] = 0;
                viewBehavior.onNestedPreScroll(this, view, target, dx, dy, mBehaviorConsumed, type);

                xConsumed = dx > 0 ? Math.max(xConsumed, mBehaviorConsumed[0])
                        : Math.min(xConsumed, mBehaviorConsumed[0]);
                yConsumed = dy > 0 ? Math.max(yConsumed, mBehaviorConsumed[1])
                        : Math.min(yConsumed, mBehaviorConsumed[1]);

                accepted = true;
            }
        }

        consumed[0] = xConsumed;
        consumed[1] = yConsumed;

        if (accepted) {
            onChildViewsChanged(EVENT_NESTED_SCROLL);
        }
    }

    @Override
    public boolean onNestedFling(@Nonnull View target, float velocityX, float velocityY, boolean consumed) {
        boolean handled = false;

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            if (view.getVisibility() == GONE) {
                // If the child is GONE, skip...
                continue;
            }

            final LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.isNestedScrollDenied(TYPE_TOUCH)) {
                continue;
            }

            final Behavior<View> viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                handled |= viewBehavior.onNestedFling(this, view, target, velocityX, velocityY,
                        consumed);
            }
        }
        if (handled) {
            onChildViewsChanged(EVENT_NESTED_SCROLL);
        }
        return handled;
    }

    @Override
    public boolean onNestedPreFling(@Nonnull View target, float velocityX, float velocityY) {
        boolean handled = false;

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            if (view.getVisibility() == GONE) {
                // If the child is GONE, skip...
                continue;
            }

            final LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.isNestedScrollDenied(TYPE_TOUCH)) {
                continue;
            }

            final Behavior<View> viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                handled |= viewBehavior.onNestedPreFling(this, view, target, velocityX, velocityY);
            }
        }
        return handled;
    }

    class OnPreDrawListener implements ViewTreeObserver.OnPreDrawListener {

        @Override
        public boolean onPreDraw() {
            onChildViewsChanged(EVENT_PRE_DRAW);
            return true;
        }
    }

    /**
     * Defines the default attached {@link Behavior} of a {@link View} class
     *
     * <p>When writing a custom view, implement this interface to return the default behavior
     * when used as a direct child of an {@link CoordinatorLayout}. The default behavior
     * can be overridden using {@link LayoutParams#setBehavior}.</p>
     */
    public interface AttachedBehavior {

        /**
         * Returns the behavior associated with the matching {@link View} class.
         *
         * @return The behavior associated with the matching {@link View} class. Must be
         * non-null.
         */
        @Nonnull
        Behavior<?> getBehavior();
    }

    /**
     * Interaction behavior plugin for child views of {@link CoordinatorLayout}.
     *
     * <p>A Behavior implements one or more interactions that a user can take on a child view.
     * These interactions may include drags, swipes, flings, or any other gestures.</p>
     *
     * @param <V> The View type that this Behavior operates on
     */
    public static abstract class Behavior<V extends View> {

        /**
         * Default constructor for instantiating Behaviors.
         */
        protected Behavior() {
        }

        /**
         * Called when the Behavior has been attached to a LayoutParams instance.
         *
         * <p>This will be called after the LayoutParams has been instantiated and can be
         * modified.</p>
         *
         * @param params the LayoutParams instance that this Behavior has been attached to
         */
        public void onAttachedToLayoutParams(@Nonnull CoordinatorLayout.LayoutParams params) {
        }

        /**
         * Called when the Behavior has been detached from its holding LayoutParams instance.
         *
         * <p>This will only be called if the Behavior has been explicitly removed from the
         * LayoutParams instance via {@link LayoutParams#setBehavior(Behavior)}. It will not be
         * called if the associated view is removed from the CoordinatorLayout or similar.</p>
         */
        public void onDetachedFromLayoutParams() {
        }

        /**
         * Respond to CoordinatorLayout touch events before they are dispatched to child views.
         *
         * <p>Behaviors can use this to monitor inbound touch events until one decides to
         * intercept the rest of the event stream to take an action on its associated child view.
         * This method will return false until it detects the proper intercept conditions, then
         * return true once those conditions have occurred.</p>
         *
         * <p>Once a Behavior intercepts touch events, the rest of the event stream will
         * be sent to the {@link #onTouchEvent} method.</p>
         *
         * <p>This method will be called regardless of the visibility of the associated child
         * of the behavior. If you only wish to handle touch events when the child is visible, you
         * should add a check to {@link View#isShown()} on the given child.</p>
         *
         * <p>The default implementation of this method always returns false.</p>
         *
         * @param parent the parent view currently receiving this touch event
         * @param child  the child view associated with this Behavior
         * @param ev     the MotionEvent describing the touch event being processed
         * @return true if this Behavior would like to intercept and take over the event stream.
         * The default always returns false.
         */
        public boolean onInterceptTouchEvent(@Nonnull CoordinatorLayout parent, @Nonnull V child,
                                             @Nonnull MotionEvent ev) {
            return false;
        }

        /**
         * Respond to CoordinatorLayout touch events after this Behavior has started
         * {@link #onInterceptTouchEvent intercepting} them.
         *
         * <p>Behaviors may intercept touch events in order to help the CoordinatorLayout
         * manipulate its child views. For example, a Behavior may allow a user to drag a
         * UI pane open or closed. This method should perform actual mutations of view
         * layout state.</p>
         *
         * <p>This method will be called regardless of the visibility of the associated child
         * of the behavior. If you only wish to handle touch events when the child is visible, you
         * should add a check to {@link View#isShown()} on the given child.</p>
         *
         * @param parent the parent view currently receiving this touch event
         * @param child  the child view associated with this Behavior
         * @param ev     the MotionEvent describing the touch event being processed
         * @return true if this Behavior handled this touch event and would like to continue
         * receiving events in this stream. The default always returns false.
         */
        public boolean onTouchEvent(@Nonnull CoordinatorLayout parent, @Nonnull V child,
                                    @Nonnull MotionEvent ev) {
            return false;
        }

        /**
         * Supply a scrim color that will be painted behind the associated child view.
         *
         * <p>A scrim may be used to indicate that the other elements beneath it are not currently
         * interactive or actionable, drawing user focus and attention to the views above the scrim.
         * </p>
         *
         * <p>The default implementation returns black.</p>
         *
         * @param parent the parent view of the given child
         * @param child  the child view above the scrim
         * @return the desired scrim color in 0xRRGGBB format. The default return value is black.
         * @see #getScrimOpacity(CoordinatorLayout, View)
         */
        public int getScrimColor(@Nonnull CoordinatorLayout parent, @Nonnull V child) {
            return 0;
        }

        /**
         * Determine the current opacity of the scrim behind a given child view
         *
         * <p>A scrim may be used to indicate that the other elements beneath it are not currently
         * interactive or actionable, drawing user focus and attention to the views above the scrim.
         * </p>
         *
         * <p>The default implementation returns 0.0f.</p>
         *
         * @param parent the parent view of the given child
         * @param child  the child view above the scrim
         * @return the desired scrim opacity from 0.0f to 1.0f. The default return value is 0.0f.
         */
        public float getScrimOpacity(@Nonnull CoordinatorLayout parent, @Nonnull V child) {
            return 0.0f;
        }

        /**
         * Determine whether interaction with views behind the given child in the child order
         * should be blocked.
         *
         * <p>The default implementation returns true if
         * {@link #getScrimOpacity(CoordinatorLayout, View)} would return > 0.0f.</p>
         *
         * @param parent the parent view of the given child
         * @param child  the child view to test
         * @return true if {@link #getScrimOpacity(CoordinatorLayout, View)} would
         * return > 0.0f.
         */
        public boolean blocksInteractionBelow(@Nonnull CoordinatorLayout parent, @Nonnull V child) {
            return getScrimOpacity(parent, child) > 0.0f;
        }

        /**
         * Determine whether the supplied child view has another specific sibling view as a
         * layout dependency.
         *
         * <p>This method will be called at least once in response to a layout request. If it
         * returns true for a given child and dependency view pair, the parent CoordinatorLayout
         * will:</p>
         * <ol>
         *     <li>Always lay out this child after the dependent child is laid out, regardless
         *     of child order.</li>
         *     <li>Call {@link #onDependentViewChanged} when the dependency view's layout or
         *     position changes.</li>
         * </ol>
         *
         * @param parent     the parent view of the given child
         * @param child      the child view to test
         * @param dependency the proposed dependency of child
         * @return true if child's layout depends on the proposed dependency's layout,
         * false otherwise
         * @see #onDependentViewChanged(CoordinatorLayout, View, View)
         */
        public boolean layoutDependsOn(@Nonnull CoordinatorLayout parent, @Nonnull V child,
                                       @Nonnull View dependency) {
            return false;
        }

        /**
         * Respond to a change in a child's dependent view
         *
         * <p>This method is called whenever a dependent view changes in size or position outside
         * of the standard layout flow. A Behavior may use this method to appropriately update
         * the child view in response.</p>
         *
         * <p>A view's dependency is determined by
         * {@link #layoutDependsOn(CoordinatorLayout, View, View)} or
         * if {@code child} has set another view as it's anchor.</p>
         *
         * <p>Note that if a Behavior changes the layout of a child via this method, it should
         * also be able to reconstruct the correct position in
         * {@link #onLayoutChild(CoordinatorLayout, View, int) onLayoutChild}.
         * <code>onDependentViewChanged</code> will not be called during normal layout since
         * the layout of each child view will always happen in dependency order.</p>
         *
         * <p>If the Behavior changes the child view's size or position, it should return true.
         * The default implementation returns false.</p>
         *
         * @param parent     the parent view of the given child
         * @param child      the child view to manipulate
         * @param dependency the dependent view that changed
         * @return true if the Behavior changed the child view's size or position, false otherwise
         */
        public boolean onDependentViewChanged(@Nonnull CoordinatorLayout parent, @Nonnull V child,
                                              @Nonnull View dependency) {
            return false;
        }

        /**
         * Respond to a child's dependent view being removed.
         *
         * <p>This method is called after a dependent view has been removed from the parent.
         * A Behavior may use this method to appropriately update the child view in response.</p>
         *
         * <p>A view's dependency is determined by
         * {@link #layoutDependsOn(CoordinatorLayout, View, View)} or
         * if {@code child} has set another view as it's anchor.</p>
         *
         * @param parent     the parent view of the given child
         * @param child      the child view to manipulate
         * @param dependency the dependent view that has been removed
         */
        public void onDependentViewRemoved(@Nonnull CoordinatorLayout parent, @Nonnull V child,
                                           @Nonnull View dependency) {
        }

        /**
         * Called when the parent CoordinatorLayout is about to measure the given child view.
         *
         * <p>This method can be used to perform custom or modified measurement of a child view
         * in place of the default child measurement behavior. The Behavior's implementation
         * can delegate to the standard CoordinatorLayout measurement behavior by calling
         * {@link CoordinatorLayout#onMeasureChild(View, int, int, int, int)
         * parent.onMeasureChild}.</p>
         *
         * @param parent                  the parent CoordinatorLayout
         * @param child                   the child to measure
         * @param parentWidthMeasureSpec  the width requirements for this view
         * @param widthUsed               extra space that has been used up by the parent
         *                                horizontally (possibly by other children of the parent)
         * @param parentHeightMeasureSpec the height requirements for this view
         * @param heightUsed              extra space that has been used up by the parent
         *                                vertically (possibly by other children of the parent)
         * @return true if the Behavior measured the child view, false if the CoordinatorLayout
         * should perform its default measurement
         */
        public boolean onMeasureChild(@Nonnull CoordinatorLayout parent, @Nonnull V child,
                                      int parentWidthMeasureSpec, int widthUsed,
                                      int parentHeightMeasureSpec, int heightUsed) {
            return false;
        }

        /**
         * Called when the parent CoordinatorLayout is about the lay out the given child view.
         *
         * <p>This method can be used to perform custom or modified layout of a child view
         * in place of the default child layout behavior. The Behavior's implementation can
         * delegate to the standard CoordinatorLayout measurement behavior by calling
         * {@link CoordinatorLayout#onLayoutChild(View, int)
         * parent.onLayoutChild}.</p>
         *
         * <p>If a Behavior implements
         * {@link #onDependentViewChanged(CoordinatorLayout, View, View)}
         * to change the position of a view in response to a dependent view changing, it
         * should also implement <code>onLayoutChild</code> in such a way that respects those
         * dependent views. <code>onLayoutChild</code> will always be called for a dependent view
         * <em>after</em> its dependency has been laid out.</p>
         *
         * @param parent          the parent CoordinatorLayout
         * @param child           child view to lay out
         * @param layoutDirection the resolved layout direction for the CoordinatorLayout, such as
         *                        {@link View#LAYOUT_DIRECTION_LTR} or
         *                        {@link View#LAYOUT_DIRECTION_RTL}.
         * @return true if the Behavior performed layout of the child view, false to request
         * default layout behavior
         */
        public boolean onLayoutChild(@Nonnull CoordinatorLayout parent, @Nonnull V child,
                                     int layoutDirection) {
            return false;
        }

        // Utility methods for accessing child-specific, behavior-modifiable properties.

        /**
         * Associate a Behavior-specific tag object with the given child view.
         * This object will be stored with the child view's LayoutParams.
         *
         * @param child child view to set tag with
         * @param tag   tag object to set
         */
        public static void setTag(@Nonnull View child, @Nullable Object tag) {
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.mBehaviorTag = tag;
        }

        /**
         * Get the behavior-specific tag object with the given child view.
         * This object is stored with the child view's LayoutParams.
         *
         * @param child child view to get tag with
         * @return the previously stored tag object
         */
        @Nullable
        public static Object getTag(@Nonnull View child) {
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            return lp.mBehaviorTag;
        }

        /**
         * Called when a descendant of the CoordinatorLayout attempts to initiate a nested scroll.
         *
         * <p>Any Behavior associated with any direct child of the CoordinatorLayout may respond
         * to this event and return true to indicate that the CoordinatorLayout should act as
         * a nested scrolling parent for this scroll. Only Behaviors that return true from
         * this method will receive subsequent nested scroll events.</p>
         *
         * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is
         *                          associated with
         * @param child             the child view of the CoordinatorLayout this Behavior is associated with
         * @param directTargetChild the child view of the CoordinatorLayout that either is or
         *                          contains the target of the nested scroll operation
         * @param target            the descendant view of the CoordinatorLayout initiating the nested scroll
         * @param axes              the axes that this nested scroll applies to. See
         *                          {@link View#SCROLL_AXIS_HORIZONTAL},
         *                          {@link View#SCROLL_AXIS_VERTICAL}
         * @param type              the type of input which cause this scroll event
         * @return true if the Behavior wishes to accept this nested scroll
         * @see ViewParent#onStartNestedScroll(View, View, int, int)
         */
        public boolean onStartNestedScroll(@Nonnull CoordinatorLayout coordinatorLayout,
                                           @Nonnull V child, @Nonnull View directTargetChild, @Nonnull View target,
                                           @ScrollAxis int axes, @NestedScrollType int type) {
            return false;
        }

        /**
         * Called when a nested scroll has been accepted by the CoordinatorLayout.
         *
         * <p>Any Behavior associated with any direct child of the CoordinatorLayout may elect
         * to accept the nested scroll as part of {@link #onStartNestedScroll}. Each Behavior
         * that returned true will receive subsequent nested scroll events for that nested scroll.
         * </p>
         *
         * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is
         *                          associated with
         * @param child             the child view of the CoordinatorLayout this Behavior is associated with
         * @param directTargetChild the child view of the CoordinatorLayout that either is or
         *                          contains the target of the nested scroll operation
         * @param target            the descendant view of the CoordinatorLayout initiating the nested scroll
         * @param axes              the axes that this nested scroll applies to. See
         *                          {@link View#SCROLL_AXIS_HORIZONTAL},
         *                          {@link View#SCROLL_AXIS_VERTICAL}
         * @param type              the type of input which cause this scroll event
         * @see ViewParent#onNestedScrollAccepted(View, View, int, int)
         */
        public void onNestedScrollAccepted(@Nonnull CoordinatorLayout coordinatorLayout,
                                           @Nonnull V child, @Nonnull View directTargetChild, @Nonnull View target,
                                           @ScrollAxis int axes, @NestedScrollType int type) {
        }

        /**
         * Called when a nested scroll has ended.
         *
         * <p>Any Behavior associated with any direct child of the CoordinatorLayout may elect
         * to accept the nested scroll as part of {@link #onStartNestedScroll}. Each Behavior
         * that returned true will receive subsequent nested scroll events for that nested scroll.
         * </p>
         *
         * <p><code>onStopNestedScroll</code> marks the end of a single nested scroll event
         * sequence. This is a good place to clean up any state related to the nested scroll.
         * </p>
         *
         * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is
         *                          associated with
         * @param child             the child view of the CoordinatorLayout this Behavior is associated with
         * @param target            the descendant view of the CoordinatorLayout that initiated
         *                          the nested scroll
         * @param type              the type of input which cause this scroll event
         * @see ViewParent#onStopNestedScroll(View, int)
         */
        public void onStopNestedScroll(@Nonnull CoordinatorLayout coordinatorLayout,
                                       @Nonnull V child, @Nonnull View target, @NestedScrollType int type) {
        }

        /**
         * Called when a nested scroll in progress has updated and the target has scrolled or
         * attempted to scroll.
         *
         * <p>Any Behavior associated with the direct child of the CoordinatorLayout may elect
         * to accept the nested scroll as part of {@link #onStartNestedScroll}. Each Behavior
         * that returned true will receive subsequent nested scroll events for that nested scroll.
         * </p>
         *
         * <p><code>onNestedScroll</code> is called each time the nested scroll is updated by the
         * nested scrolling child, with both consumed and unconsumed components of the scroll
         * supplied in pixels. <em>Each Behavior responding to the nested scroll will receive the
         * same values.</em>
         * </p>
         *
         * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is
         *                          associated with
         * @param child             the child view of the CoordinatorLayout this Behavior is associated with
         * @param target            the descendant view of the CoordinatorLayout performing the nested scroll
         * @param dxConsumed        horizontal pixels consumed by the target's own scrolling operation
         * @param dyConsumed        vertical pixels consumed by the target's own scrolling operation
         * @param dxUnconsumed      horizontal pixels not consumed by the target's own scrolling
         *                          operation, but requested by the user
         * @param dyUnconsumed      vertical pixels not consumed by the target's own scrolling operation,
         *                          but requested by the user
         * @param type              the type of input which cause this scroll event
         * @param consumed          output. Upon this method returning, should contain the scroll
         *                          distances consumed by this Behavior
         * @see ViewParent#onNestedScroll(View, int, int, int, int, int, int[])
         */
        public void onNestedScroll(@Nonnull CoordinatorLayout coordinatorLayout, @Nonnull V child,
                                   @Nonnull View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                                   int dyUnconsumed, @NestedScrollType int type, @Nonnull int[] consumed) {
        }

        /**
         * Called when a nested scroll in progress is about to update, before the target has
         * consumed any of the scrolled distance.
         *
         * <p>Any Behavior associated with the direct child of the CoordinatorLayout may elect
         * to accept the nested scroll as part of {@link #onStartNestedScroll}. Each Behavior
         * that returned true will receive subsequent nested scroll events for that nested scroll.
         * </p>
         *
         * <p><code>onNestedPreScroll</code> is called each time the nested scroll is updated
         * by the nested scrolling child, before the nested scrolling child has consumed the scroll
         * distance itself. <em>Each Behavior responding to the nested scroll will receive the
         * same values.</em> The CoordinatorLayout will report as consumed the maximum number
         * of pixels in either direction that any Behavior responding to the nested scroll reported
         * as consumed.</p>
         *
         * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is
         *                          associated with
         * @param child             the child view of the CoordinatorLayout this Behavior is associated with
         * @param target            the descendant view of the CoordinatorLayout performing the nested scroll
         * @param dx                the raw horizontal number of pixels that the user attempted to scroll
         * @param dy                the raw vertical number of pixels that the user attempted to scroll
         * @param consumed          out parameter. consumed[0] should be set to the distance of dx that
         *                          was consumed, consumed[1] should be set to the distance of dy that
         *                          was consumed
         * @param type              the type of input which cause this scroll event
         * @see ViewParent#onNestedPreScroll(View, int, int, int[], int)
         */
        public void onNestedPreScroll(@Nonnull CoordinatorLayout coordinatorLayout,
                                      @Nonnull V child, @Nonnull View target, int dx, int dy, @Nonnull int[] consumed,
                                      @NestedScrollType int type) {
        }

        /**
         * Called when a nested scrolling child is starting a fling or an action that would
         * be a fling.
         *
         * <p>Any Behavior associated with the direct child of the CoordinatorLayout may elect
         * to accept the nested scroll as part of {@link #onStartNestedScroll}. Each Behavior
         * that returned true will receive subsequent nested scroll events for that nested scroll.
         * </p>
         *
         * <p><code>onNestedFling</code> is called when the current nested scrolling child view
         * detects the proper conditions for a fling. It reports if the child itself consumed
         * the fling. If it did not, the child is expected to show some sort of overscroll
         * indication. This method should return true if it consumes the fling, so that a child
         * that did not itself take an action in response can choose not to show an overfling
         * indication.</p>
         *
         * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is
         *                          associated with
         * @param child             the child view of the CoordinatorLayout this Behavior is associated with
         * @param target            the descendant view of the CoordinatorLayout performing the nested scroll
         * @param velocityX         horizontal velocity of the attempted fling
         * @param velocityY         vertical velocity of the attempted fling
         * @param consumed          true if the nested child view consumed the fling
         * @return true if the Behavior consumed the fling
         * @see ViewParent#onNestedFling(View, float, float, boolean)
         */
        public boolean onNestedFling(@Nonnull CoordinatorLayout coordinatorLayout,
                                     @Nonnull V child, @Nonnull View target, float velocityX, float velocityY,
                                     boolean consumed) {
            return false;
        }

        /**
         * Called when a nested scrolling child is about to start a fling.
         *
         * <p>Any Behavior associated with the direct child of the CoordinatorLayout may elect
         * to accept the nested scroll as part of {@link #onStartNestedScroll}. Each Behavior
         * that returned true will receive subsequent nested scroll events for that nested scroll.
         * </p>
         *
         * <p><code>onNestedPreFling</code> is called when the current nested scrolling child view
         * detects the proper conditions for a fling, but it has not acted on it yet. A
         * Behavior can return true to indicate that it consumed the fling. If at least one
         * Behavior returns true, the fling should not be acted upon by the child.</p>
         *
         * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is
         *                          associated with
         * @param child             the child view of the CoordinatorLayout this Behavior is associated with
         * @param target            the descendant view of the CoordinatorLayout performing the nested scroll
         * @param velocityX         horizontal velocity of the attempted fling
         * @param velocityY         vertical velocity of the attempted fling
         * @return true if the Behavior consumed the fling
         * @see ViewParent#onNestedPreFling(View, float, float)
         */
        public boolean onNestedPreFling(@Nonnull CoordinatorLayout coordinatorLayout,
                                        @Nonnull V child, @Nonnull View target, float velocityX, float velocityY) {
            return false;
        }

        /**
         * Called when a child of the view associated with this behavior wants a particular
         * rectangle to be positioned onto the screen.
         *
         * <p>The contract for this method is the same as
         * {@link ViewParent#requestChildRectangleOnScreen(View, Rect, boolean)}.</p>
         *
         * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is
         *                          associated with
         * @param child             the child view of the CoordinatorLayout this Behavior is
         *                          associated with
         * @param rectangle         The rectangle which the child wishes to be on the screen
         *                          in the child's coordinates
         * @param immediate         true to forbid animated or delayed scrolling, false otherwise
         * @return true if the Behavior handled the request
         * @see ViewParent#requestChildRectangleOnScreen(View, Rect, boolean)
         */
        public boolean onRequestChildRectangleOnScreen(@Nonnull CoordinatorLayout coordinatorLayout,
                                                       @Nonnull V child, @Nonnull Rect rectangle, boolean immediate) {
            return false;
        }

        /**
         * Called when a view is set to dodge view insets.
         *
         * <p>This method allows a behavior to update the rectangle that should be dodged.
         * The rectangle should be in the parent's coordinate system and within the child's
         * bounds. If not, a {@link IllegalArgumentException} is thrown.</p>
         *
         * @param parent the CoordinatorLayout parent of the view this Behavior is
         *               associated with
         * @param child  the child view of the CoordinatorLayout this Behavior is associated with
         * @param rect   the rect to update with the dodge rectangle
         * @return true the rect was updated, false if we should use the child's bounds
         */
        public boolean getInsetDodgeRect(@Nonnull CoordinatorLayout parent, @Nonnull V child,
                                         @Nonnull Rect rect) {
            return false;
        }
    }

    /**
     * Parameters describing the desired layout for a child of a {@link CoordinatorLayout}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static class LayoutParams extends MarginLayoutParams {

        /**
         * A {@link Behavior} that the child view should obey.
         */
        Behavior mBehavior;

        boolean mBehaviorResolved = false;

        /**
         * A {@link Gravity} value describing how this child view should lay out.
         * If either or both of the axes are not specified, they are treated by CoordinatorLayout
         * as {@link Gravity#TOP} or {@link Gravity#START}. If an
         * {@link #setAnchorId(int) anchor} is also specified, the gravity describes how this child
         * view should be positioned relative to its anchored position.
         */
        public int gravity = Gravity.NO_GRAVITY;

        /**
         * A {@link Gravity} value describing which edge of a child view's
         * {@link #getAnchorId() anchor} view the child should position itself relative to.
         */
        public int anchorGravity = Gravity.NO_GRAVITY;

        /**
         * A {@link View#getId() view id} of a descendant view of the CoordinatorLayout that
         * this child should position relative to.
         */
        int mAnchorId = View.NO_ID;

        /**
         * A {@link Gravity} value describing how this child view insets the CoordinatorLayout.
         * Other child views which are set to dodge the same inset edges will be moved appropriately
         * so that the views do not overlap.
         */
        public int insetEdge = Gravity.NO_GRAVITY;

        /**
         * A {@link Gravity} value describing how this child view dodges any inset child views in
         * the CoordinatorLayout. Any views which are inset on the same edge as this view is set to
         * dodge will result in this view being moved so that the views do not overlap.
         */
        public int dodgeInsetEdges = Gravity.NO_GRAVITY;

        int mInsetOffsetX;
        int mInsetOffsetY;

        View mAnchorView;
        View mAnchorDirectChild;

        private boolean mDidBlockInteraction;
        private boolean mDidAcceptNestedScrollTouch;
        private boolean mDidAcceptNestedScrollNonTouch;
        private boolean mDidChangeAfterNestedScroll;

        final Rect mLastChildRect = new Rect();

        Object mBehaviorTag;

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams p) {
            super(p);
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
        }

        /**
         * Get the id of this view's anchor.
         *
         * @return A {@link View#getId() view id} or {@link View#NO_ID} if there is no anchor
         */
        public int getAnchorId() {
            return mAnchorId;
        }

        /**
         * Set the id of this view's anchor.
         *
         * <p>The view with this id must be a descendant of the CoordinatorLayout containing
         * the child view this LayoutParams belongs to. It may not be the child view with
         * this LayoutParams or a descendant of it.</p>
         *
         * @param id The {@link View#getId() view id} of the anchor or
         *           {@link View#NO_ID} if there is no anchor
         */
        public void setAnchorId(int id) {
            invalidateAnchor();
            mAnchorId = id;
        }

        /**
         * Get the behavior governing the layout and interaction of the child view within
         * a parent CoordinatorLayout.
         *
         * @return The current behavior or null if no behavior is specified
         */
        @Nullable
        public <V extends View> Behavior<V> getBehavior() {
            return mBehavior;
        }

        /**
         * Set the behavior governing the layout and interaction of the child view within
         * a parent CoordinatorLayout.
         *
         * <p>Setting a new behavior will remove any currently associated
         * {@link Behavior#setTag(View, Object) Behavior tag}.</p>
         *
         * @param behavior The behavior to set or null for no special behavior
         */
        public void setBehavior(@Nullable Behavior<?> behavior) {
            if (mBehavior != behavior) {
                if (mBehavior != null) {
                    // First detach any old behavior
                    mBehavior.onDetachedFromLayoutParams();
                }

                mBehavior = behavior;
                mBehaviorTag = null;
                mBehaviorResolved = true;

                if (behavior != null) {
                    // Now dispatch that the Behavior has been attached
                    behavior.onAttachedToLayoutParams(this);
                }
            }
        }

        /**
         * Set the last known position rect for this child view
         *
         * @param r the rect to set
         */
        void setLastChildRect(Rect r) {
            mLastChildRect.set(r);
        }

        /**
         * Get the last known position rect for this child view.
         * Note: do not mutate the result of this call.
         */
        Rect getLastChildRect() {
            return mLastChildRect;
        }

        /**
         * Returns true if the anchor id changed to another valid view id since the anchor view
         * was resolved.
         */
        boolean checkAnchorChanged() {
            return mAnchorView == null && mAnchorId != View.NO_ID;
        }

        /**
         * Returns true if the associated Behavior previously blocked interaction with other views
         * below the associated child since the touch behavior tracking was last
         * {@link #resetTouchBehaviorTracking() reset}.
         *
         * @see #isBlockingInteractionBelow(CoordinatorLayout, View)
         */
        boolean didBlockInteraction() {
            if (mBehavior == null) {
                mDidBlockInteraction = false;
            }
            return mDidBlockInteraction;
        }

        /**
         * Check if the associated Behavior wants to block interaction below the given child
         * view. The given child view should be the child this LayoutParams is associated with.
         *
         * <p>Once interaction is blocked, it will remain blocked until touch interaction tracking
         * is {@link #resetTouchBehaviorTracking() reset}.</p>
         *
         * @param parent the parent CoordinatorLayout
         * @param child  the child view this LayoutParams is associated with
         * @return true to block interaction below the given child
         */
        boolean isBlockingInteractionBelow(CoordinatorLayout parent, View child) {
            if (mDidBlockInteraction) {
                return true;
            }

            return mDidBlockInteraction = mBehavior != null && mBehavior.blocksInteractionBelow(parent, child);
        }

        /**
         * Reset tracking of Behavior-specific touch interactions. This includes
         * interaction blocking.
         *
         * @see #isBlockingInteractionBelow(CoordinatorLayout, View)
         * @see #didBlockInteraction()
         */
        void resetTouchBehaviorTracking() {
            mDidBlockInteraction = false;
        }

        void resetNestedScroll(int type) {
            setNestedScrollAccepted(type, false);
        }

        void setNestedScrollAccepted(int type, boolean accept) {
            switch (type) {
                case View.TYPE_TOUCH -> mDidAcceptNestedScrollTouch = accept;
                case View.TYPE_NON_TOUCH -> mDidAcceptNestedScrollNonTouch = accept;
            }
        }

        boolean isNestedScrollDenied(int type) {
            return switch (type) {
                case View.TYPE_TOUCH -> !mDidAcceptNestedScrollTouch;
                case View.TYPE_NON_TOUCH -> !mDidAcceptNestedScrollNonTouch;
                default -> true;
            };
        }

        boolean getChangedAfterNestedScroll() {
            return mDidChangeAfterNestedScroll;
        }

        void setChangedAfterNestedScroll(boolean changed) {
            mDidChangeAfterNestedScroll = changed;
        }

        void resetChangedAfterNestedScroll() {
            mDidChangeAfterNestedScroll = false;
        }

        /**
         * Check if an associated child view depends on another child view of the CoordinatorLayout.
         *
         * @param parent     the parent CoordinatorLayout
         * @param child      the child to check
         * @param dependency the proposed dependency to check
         * @return true if child depends on dependency
         */
        boolean dependsOn(CoordinatorLayout parent, View child, View dependency) {
            return dependency == mAnchorDirectChild
                    || shouldDodge(dependency, parent.getLayoutDirection())
                    || (mBehavior != null && mBehavior.layoutDependsOn(parent, child, dependency));
        }

        /**
         * Invalidate the cached anchor view and direct child ancestor of that anchor.
         * The anchor will need to be
         * {@link #findAnchorView(CoordinatorLayout, View) found} before
         * being used again.
         */
        void invalidateAnchor() {
            mAnchorView = mAnchorDirectChild = null;
        }

        /**
         * Locate the appropriate anchor view by the current {@link #setAnchorId(int) anchor id}
         * or return the cached anchor view if already known.
         *
         * @param parent   the parent CoordinatorLayout
         * @param forChild the child this LayoutParams is associated with
         * @return the located descendant anchor view, or null if the anchor id is
         * {@link View#NO_ID}.
         */
        View findAnchorView(CoordinatorLayout parent, View forChild) {
            if (mAnchorId == View.NO_ID) {
                mAnchorView = mAnchorDirectChild = null;
                return null;
            }

            if (mAnchorView == null || !verifyAnchorView(forChild, parent)) {
                resolveAnchorView(forChild, parent);
            }
            return mAnchorView;
        }

        /**
         * Determine the anchor view for the child view this LayoutParams is assigned to.
         * Assumes mAnchorId is valid.
         */
        private void resolveAnchorView(final View forChild, @Nonnull final CoordinatorLayout parent) {
            mAnchorView = parent.findViewById(mAnchorId);
            if (mAnchorView != null) {
                if (mAnchorView == parent) {
                    throw new IllegalStateException(
                            "View can not be anchored to the the parent CoordinatorLayout");
                }

                View directChild = mAnchorView;
                for (ViewParent p = mAnchorView.getParent();
                     p != parent && p != null;
                     p = p.getParent()) {
                    if (p == forChild) {
                        throw new IllegalStateException(
                                "Anchor must not be a descendant of the anchored view");
                    }
                    if (p instanceof View) {
                        directChild = (View) p;
                    }
                }
                mAnchorDirectChild = directChild;
            } else {
                throw new IllegalStateException("Could not find CoordinatorLayout descendant view"
                        + " with id " + mAnchorId + " to anchor view " + forChild);
            }
        }

        /**
         * Verify that the previously resolved anchor view is still valid - that it is still
         * a descendant of the expected parent view, it is not the child this LayoutParams
         * is assigned to or a descendant of it, and it has the expected id.
         */
        private boolean verifyAnchorView(View forChild, CoordinatorLayout parent) {
            if (mAnchorView.getId() != mAnchorId) {
                return false;
            }

            View directChild = mAnchorView;
            for (ViewParent p = mAnchorView.getParent();
                 p != parent;
                 p = p.getParent()) {
                if (p == null || p == forChild) {
                    mAnchorView = mAnchorDirectChild = null;
                    return false;
                }
                if (p instanceof View) {
                    directChild = (View) p;
                }
            }
            mAnchorDirectChild = directChild;
            return true;
        }

        /**
         * Checks whether the view with this LayoutParams should dodge the specified view.
         */
        private boolean shouldDodge(@Nonnull View other, int layoutDirection) {
            LayoutParams lp = (LayoutParams) other.getLayoutParams();
            final int absInset = Gravity.getAbsoluteGravity(lp.insetEdge, layoutDirection);
            return absInset != Gravity.NO_GRAVITY && (absInset &
                    Gravity.getAbsoluteGravity(dodgeInsetEdges, layoutDirection)) == absInset;
        }
    }
}
