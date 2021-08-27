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
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.platform.RenderCore;
import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

@SuppressWarnings("unused")
public abstract class ViewGroup extends View implements ViewParent {

    private static final int ARRAY_CAPACITY_INCREMENT = 12;

    /**
     * When set, ViewGroup invalidates only the child's rectangle
     * Set by default
     */
    static final int FLAG_CLIP_CHILDREN = 0x1;

    /**
     * When set, the drawing method will call {@link #getChildDrawingOrder(int, int)}
     * to get the index of the child to draw for that iteration.
     */
    static final int FLAG_USE_CHILD_DRAWING_ORDER = 0x400;

    /**
     * When set, this ViewGroup's drawable states also include those
     * of its children.
     */
    private static final int FLAG_ADD_STATES_FROM_CHILDREN = 0x2000;

    /**
     * When set, this ViewGroup should not intercept touch events.
     */
    private static final int FLAG_DISALLOW_INTERCEPT = 0x80000;

    private int mGroupFlags;

    // child views
    private View[] mChildren = new View[ARRAY_CAPACITY_INCREMENT];

    // number of valid children in the children array, the rest
    // should be null or not considered as children
    private int mChildrenCount = 0;

    // Lazily-created holder for point computations.
    private float[] mTempPosition;

    // First touch target in the linked list of touch targets.
    private TouchTarget mTouchTarget;

    // First hover target in the linked list of hover targets.
    // The hover targets are children which have received ACTION_HOVER_ENTER.
    // They might not have actually handled the hover event, but we will
    // continue sending hover events to them as long as the pointer remains over
    // their bounds and the view group does not intercept hover.
    private HoverTarget mFirstHoverTarget;

    // True if the view group itself received a hover event.
    // It might not have actually handled the hover event.
    private boolean mHoveredSelf;

    public ViewGroup() {
        mGroupFlags |= FLAG_CLIP_CHILDREN;
    }

    @Override
    protected void dispatchDraw(@Nonnull Canvas canvas) {
        final View[] views = mChildren;
        final int count = mChildrenCount;
        for (int i = 0; i < count; i++) {
            views[i].draw(canvas, this, (mGroupFlags & FLAG_CLIP_CHILDREN) != 0);
        }
    }

    @Override
    public final void layout(int left, int top, int right, int bottom) {
        super.layout(left, top, right, bottom);
    }

    @Override
    protected abstract void onLayout(boolean changed, int left, int top, int right, int bottom);

    private int getAndVerifyPreorderedIndex(int childrenCount, int i, boolean customOrder) {
        final int childIndex;
        if (customOrder) {
            final int childIndex1 = getChildDrawingOrder(childrenCount, i);
            if (childIndex1 >= childrenCount) {
                throw new IndexOutOfBoundsException("getChildDrawingOrder() "
                        + "returned invalid index " + childIndex1
                        + " (child count is " + childrenCount + ")");
            }
            childIndex = childIndex1;
        } else {
            childIndex = i;
        }
        return childIndex;
    }

    @Override
    protected boolean dispatchHoverEvent(MotionEvent event) {
        final int action = event.getAction();

        final boolean intercepted = onInterceptHoverEvent(event);
        event.setAction(action); // restore action in case it was changed

        boolean handled = false;

        // Send events to the hovered children and build a new list of hover targets until
        // one is found that handles the event.
        HoverTarget firstOldHoverTarget = mFirstHoverTarget;
        mFirstHoverTarget = null;
        if (!intercepted && action != MotionEvent.ACTION_HOVER_EXIT
                && mChildrenCount != 0) {
            final float x = event.getX();
            final float y = event.getY();
            final View[] children = mChildren;
            final int childrenCount = mChildrenCount;
            final boolean customOrder = isChildrenDrawingOrderEnabled();
            HoverTarget lastHoverTarget = null;
            for (int i = childrenCount - 1; i >= 0; i--) {
                final int childIndex = getAndVerifyPreorderedIndex(
                        childrenCount, i, customOrder);
                final View child = getAndVerifyPreorderedView(
                        null, children, childIndex);
                if (!child.canReceivePointerEvents()
                        || !isTransformedTouchPointInView(x, y, child, null)) {
                    continue;
                }

                // Obtain a hover target for this child.  Dequeue it from the
                // old hover target list if the child was previously hovered.
                HoverTarget hoverTarget = firstOldHoverTarget;
                final boolean wasHovered;
                for (HoverTarget predecessor = null; ; ) {
                    if (hoverTarget == null) {
                        hoverTarget = HoverTarget.obtain(child);
                        wasHovered = false;
                        break;
                    }

                    if (hoverTarget.child == child) {
                        if (predecessor != null) {
                            predecessor.next = hoverTarget.next;
                        } else {
                            firstOldHoverTarget = hoverTarget.next;
                        }
                        hoverTarget.next = null;
                        wasHovered = true;
                        break;
                    }

                    predecessor = hoverTarget;
                    hoverTarget = hoverTarget.next;
                }

                // Enqueue the hover target onto the new hover target list.
                if (lastHoverTarget != null) {
                    lastHoverTarget.next = hoverTarget;
                } else {
                    mFirstHoverTarget = hoverTarget;
                }
                lastHoverTarget = hoverTarget;

                // Dispatch the event to the child.
                if (action == MotionEvent.ACTION_HOVER_ENTER) {
                    if (!wasHovered) {
                        // Send the enter as is.
                        handled = dispatchTransformedGenericPointerEvent(
                                event, child); // enter
                    }
                } else if (action == MotionEvent.ACTION_HOVER_MOVE) {
                    if (!wasHovered) {
                        event.setAction(MotionEvent.ACTION_HOVER_ENTER);
                        handled = dispatchTransformedGenericPointerEvent(
                                event, child); // enter
                        event.setAction(action);

                    }
                    // Send the move as is.
                    handled |= dispatchTransformedGenericPointerEvent(
                            event, child); // move
                }
                if (handled) {
                    break;
                }
            }
        }

        // Send exit events to all previously hovered children that are no longer hovered.
        while (firstOldHoverTarget != null) {
            final View child = firstOldHoverTarget.child;

            // Exit the old hovered child.
            if (action == MotionEvent.ACTION_HOVER_EXIT) {
                // Send the exit as is.
                handled |= dispatchTransformedGenericPointerEvent(
                        event, child); // exit
            } else {
                // Synthesize an exit from a move or enter.
                // Ignore the result because hover focus has moved to a different view.
                if (action == MotionEvent.ACTION_HOVER_MOVE) {
                    final boolean hoverExitPending = event.isHoverExitPending();
                    event.setHoverExitPending(true);
                    dispatchTransformedGenericPointerEvent(
                            event, child); // move
                    event.setHoverExitPending(hoverExitPending);
                }
                event.setAction(MotionEvent.ACTION_HOVER_EXIT);
                dispatchTransformedGenericPointerEvent(
                        event, child); // exit
                event.setAction(action);
            }

            final HoverTarget nextOldHoverTarget = firstOldHoverTarget.next;
            firstOldHoverTarget.recycle();
            firstOldHoverTarget = nextOldHoverTarget;
        }

        // Send events to the view group itself if no children have handled it and the view group
        // itself is not currently being hover-exited.
        boolean newHoveredSelf = !handled &&
                (action != MotionEvent.ACTION_HOVER_EXIT) && !event.isHoverExitPending();
        if (newHoveredSelf == mHoveredSelf) {
            if (newHoveredSelf) {
                // Send event to the view group as before.
                handled = super.dispatchHoverEvent(event);
            }
        } else {
            if (mHoveredSelf) {
                // Exit the view group.
                if (action == MotionEvent.ACTION_HOVER_EXIT) {
                    // Send the exit as is.
                    handled |= super.dispatchHoverEvent(event); // exit
                } else {
                    // Synthesize an exit from a move or enter.
                    // Ignore the result because hover focus is moving to a different view.
                    if (action == MotionEvent.ACTION_HOVER_MOVE) {
                        super.dispatchHoverEvent(event); // move
                    }
                    event.setAction(MotionEvent.ACTION_HOVER_EXIT);
                    super.dispatchHoverEvent(event); // exit
                    event.setAction(action);
                }
                mHoveredSelf = false;
            }

            if (newHoveredSelf) {
                // Enter the view group.
                if (action == MotionEvent.ACTION_HOVER_ENTER) {
                    // Send the enter as is.
                    handled = super.dispatchHoverEvent(event); // enter
                    mHoveredSelf = true;
                } else if (action == MotionEvent.ACTION_HOVER_MOVE) {
                    // Synthesize an enter from a move.
                    event.setAction(MotionEvent.ACTION_HOVER_ENTER);
                    handled = super.dispatchHoverEvent(event); // enter
                    event.setAction(action);

                    handled |= super.dispatchHoverEvent(event); // move
                    mHoveredSelf = true;
                }
            }
        }

        return handled;
    }


    /**
     * Dispatches a generic pointer event to a child, taking into account
     * transformations that apply to the child.
     *
     * @param event The event to send.
     * @param child The view to send the event to.
     * @return {@code true} if the child handled the event.
     */
    private boolean dispatchTransformedGenericPointerEvent(MotionEvent event, View child) {
        boolean handled;
        final float offsetX = mScrollX - child.mLeft;
        final float offsetY = mScrollY - child.mTop;
        event.offsetLocation(offsetX, offsetY);
        handled = child.dispatchGenericMotionEvent(event);
        event.offsetLocation(-offsetX, -offsetY);
        return handled;
    }

    /**
     * Implement this method to intercept hover events before they are handled
     * by child views.
     * <p>
     * This method is called before dispatching a hover event to a child of
     * the view group or to the view group's own {@link #onHoverEvent} to allow
     * the view group a chance to intercept the hover event.
     * This method can also be used to watch all pointer motions that occur within
     * the bounds of the view group even when the pointer is hovering over
     * a child of the view group rather than over the view group itself.
     * </p><p>
     * The view group can prevent its children from receiving hover events by
     * implementing this method and returning <code>true</code> to indicate
     * that it would like to intercept hover events.  The view group must
     * continuously return <code>true</code> from this method for as long as
     * it wishes to continue intercepting hover events from its children.
     * </p><p>
     * Interception preserves the invariant that at most one view can be
     * hovered at a time by transferring hover focus from the currently hovered
     * child to the view group or vice-versa as needed.
     * </p><p>
     * If this method returns <code>true</code> and a child is already hovered, then the
     * child view will first receive a hover exit event and then the view group
     * itself will receive a hover enter event in {@link #onHoverEvent}.
     * Likewise, if this method had previously returned <code>true</code> to intercept hover
     * events and instead returns <code>false</code> while the pointer is hovering
     * within the bounds of one of a child, then the view group will first receive a
     * hover exit event in {@link #onHoverEvent} and then the hovered child will
     * receive a hover enter event.
     * </p><p>
     * The default implementation handles mouse hover on the scroll bars.
     * </p>
     *
     * @param event The motion event that describes the hover.
     * @return True if the view group would like to intercept the hover event
     * and prevent its children from receiving it.
     */
    public boolean onInterceptHoverEvent(MotionEvent event) {
        /*final int action = event.getAction();
        final float x = event.getX();
        final float y = event.getY();
        return (action == MotionEvent.ACTION_HOVER_MOVE
                || action == MotionEvent.ACTION_HOVER_ENTER) && isOnScrollbar(x, y);*/
        return false;
    }

    @Override
    protected boolean dispatchGenericPointerEvent(MotionEvent event) {
        // Send the event to the child under the pointer.
        final int childrenCount = mChildrenCount;
        if (childrenCount != 0) {
            final float x = event.getX();
            final float y = event.getY();

            final boolean customOrder = isChildrenDrawingOrderEnabled();
            final View[] children = mChildren;
            for (int i = childrenCount - 1; i >= 0; i--) {
                final int childIndex = getAndVerifyPreorderedIndex(childrenCount, i, customOrder);
                final View child = getAndVerifyPreorderedView(null, children, childIndex);
                if (!child.canReceivePointerEvents()
                        || !isTransformedTouchPointInView(x, y, child, null)) {
                    continue;
                }

                if (dispatchTransformedGenericPointerEvent(event, child)) {
                    return true;
                }
            }
        }

        // Send to this view group.
        return super.dispatchGenericPointerEvent(event);
    }

    private static View getAndVerifyPreorderedView(ArrayList<View> preorderedList, View[] children,
                                                   int childIndex) {
        final View child;
        if (preorderedList != null) {
            child = preorderedList.get(childIndex);
            if (child == null) {
                throw new RuntimeException("Invalid preorderedList contained null child at index "
                        + childIndex);
            }
        } else {
            child = children[childIndex];
        }
        return child;
    }

    /*@Override
    public final boolean dispatchGenericMotionEvent(@Nonnull MotionEvent event) {
        final int action = event.getAction();

        float scrollX = getScrollX();
        float scrollY = getScrollY();
        event.offsetLocation(scrollX, scrollY);

        final View[] views = children;
        View child;
        boolean anyHovered = false;

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                for (int i = childrenCount - 1; i >= 0; i--) {
                    child = views[i];
                    if (!anyHovered && child.onGenericMotionEvent(event)) {
                        anyHovered = true;
                    } else {
                        child.ensureMouseHoverExit();
                    }
                }
                return anyHovered;
            *//*case MotionEvent.ACTION_PRESS:
            case MotionEvent.ACTION_RELEASE:
            case MotionEvent.ACTION_DOUBLE_CLICK:*//*
            case MotionEvent.ACTION_SCROLL:
                for (int i = childrenCount - 1; i >= 0; i--) {
                    child = views[i];
                    if (child.onGenericMotionEvent(event)) {
                        return true;
                    }
                }
                return false;
        }

        event.offsetLocation(-scrollX, -scrollY);
        return super.dispatchGenericMotionEvent(event);
    }*/

    /*@Deprecated
    @Override
    final boolean dispatchUpdateMouseHover(double mouseX, double mouseY) {
        final double mx = mouseX + getScrollX();
        final double my = mouseY + getScrollY();
        final View[] views = children;
        boolean anyHovered = false;
        View child;
        for (int i = childrenCount - 1; i >= 0; i--) {
            child = views[i];
            if (!anyHovered && child.updateMouseHover(mx, my)) {
                anyHovered = true;
            } else {
                child.ensureMouseHoverExit();
            }
        }
        return anyHovered;
    }*/

    /*@Override
    final void ensureMouseHoverExit() {
        super.ensureMouseHoverExit();
        final View[] views = children;
        final int count = childrenCount;
        for (int i = 0; i < count; i++) {
            views[i].ensureMouseHoverExit();
        }
    }*/

    /**
     * Resets the cancel next up flag.
     *
     * @return true if the flag was previously set.
     */
    private static boolean resetCancelNextUpFlag(@Nonnull View view) {
        if ((view.mPrivateFlags & PFLAG_CANCEL_NEXT_UP_EVENT) != 0) {
            view.mPrivateFlags &= ~PFLAG_CANCEL_NEXT_UP_EVENT;
            return true;
        }
        return false;
    }

    /**
     * Clears all touch targets.
     */
    private void clearTouchTargets() {
        TouchTarget target = mTouchTarget;
        if (target != null) {
            target.recycle();
            mTouchTarget = null;
        }
    }

    /**
     * Resets all touch state in preparation for a new cycle.
     */
    private void resetTouchState() {
        clearTouchTargets();
        resetCancelNextUpFlag(this);
        mGroupFlags &= ~FLAG_DISALLOW_INTERCEPT;
    }

    /**
     * Cancels and clears all touch targets.
     */
    private void cancelAndClearTouchTargets(@Nullable MotionEvent event) {
        TouchTarget target = mTouchTarget;
        if (target != null) {
            boolean syntheticEvent = false;
            if (event == null) {
                final long time = RenderCore.timeNanos();
                event = MotionEvent.obtain(time, time,
                        MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
                syntheticEvent = true;
            }

            resetCancelNextUpFlag(target.child);
            dispatchTransformedTouchEvent(event, target.child, true);
            clearTouchTargets();

            if (syntheticEvent) {
                event.recycle();
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(@Nonnull MotionEvent ev) {
        boolean handled = false;

        final int action = ev.getAction();

        // Handle an initial down.
        if (action == MotionEvent.ACTION_DOWN) {
            // Throw away all previous state when starting a new touch gesture.
            // The framework may have dropped the up or cancel event for the previous gesture
            // due to an app switch, ANR, or some other state change.
            cancelAndClearTouchTargets(ev);
            resetTouchState();
        }

        // Check for interception.
        final boolean intercepted;
        if (action == MotionEvent.ACTION_DOWN || mTouchTarget != null) {
            final boolean allowIntercept = (mGroupFlags & FLAG_DISALLOW_INTERCEPT) == 0;
            if (allowIntercept) {
                intercepted = onInterceptTouchEvent(ev);
                ev.setAction(action); // restore action in case it was changed
            } else {
                intercepted = false;
            }
        } else {
            // There are no touch targets and this action is not an initial down
            // so this view group continues to intercept touches.
            intercepted = true;
        }

        // Check for cancellation.
        final boolean canceled = resetCancelNextUpFlag(this)
                || action == MotionEvent.ACTION_CANCEL;

        // Update list of touch targets for pointer down, if needed.
        TouchTarget newTouchTarget = null;
        boolean dispatchedToNewTarget = false;
        if (!canceled && !intercepted && action == MotionEvent.ACTION_DOWN && mChildrenCount > 0) {
            final float x = ev.getX();
            final float y = ev.getY();

            //TODO ordering
            final View[] children = mChildren;
            for (int i = mChildrenCount - 1; i >= 0; i--) {
                final View child = children[i];
                if (!child.canReceivePointerEvents()
                        || !isTransformedTouchPointInView(x, y, child, null)) {
                    continue;
                }

                resetCancelNextUpFlag(child);
                if (dispatchTransformedTouchEvent(ev, child, false)) {
                    mTouchTarget = TouchTarget.obtain(child);
                    dispatchedToNewTarget = true;
                    break;
                }
            }
        }

        // Dispatch to touch targets.
        if (mTouchTarget == null) {
            // No touch targets so treat this as an ordinary view.
            handled = dispatchTransformedTouchEvent(ev, null, canceled);
        } else {
            // Dispatch to touch targets, excluding the new touch target if we already
            // dispatched to it.  Cancel touch targets if necessary.
            if (dispatchedToNewTarget) {
                handled = true;
            } else {
                final TouchTarget target = mTouchTarget;
                final boolean cancelChild = resetCancelNextUpFlag(target.child)
                        || intercepted;
                if (dispatchTransformedTouchEvent(ev, target.child, cancelChild)) {
                    handled = true;
                }
                if (cancelChild) {
                    mTouchTarget = null;
                    target.recycle();
                }
            }
        }

        // Update list of touch targets for pointer up or cancel, if needed.
        if (canceled || action == MotionEvent.ACTION_UP) {
            resetTouchState();
        }
        return handled;
    }

    /**
     * Implement this method to intercept all touch screen motion events.  This
     * allows you to watch events as they are dispatched to your children, and
     * take ownership of the current gesture at any point.
     *
     * <p>Using this function takes some care, as it has a fairly complicated
     * interaction with {@link View#onTouchEvent(MotionEvent)
     * View.onTouchEvent(MotionEvent)}, and using it requires implementing
     * that method as well as this one in the correct way.  Events will be
     * received in the following order:
     *
     * <ol>
     * <li> You will receive the down event here.
     * <li> The down event will be handled either by a child of this view
     * group, or given to your own onTouchEvent() method to handle; this means
     * you should implement onTouchEvent() to return true, so you will
     * continue to see the rest of the gesture (instead of looking for
     * a parent view to handle it).  Also, by returning true from
     * onTouchEvent(), you will not receive any following
     * events in onInterceptTouchEvent() and all touch processing must
     * happen in onTouchEvent() like normal.
     * <li> For as long as you return false from this function, each following
     * event (up to and including the final up) will be delivered first here
     * and then to the target's onTouchEvent().
     * <li> If you return true from here, you will not receive any
     * following events: the target view will receive the same event but
     * with the action {@link MotionEvent#ACTION_CANCEL}, and all further
     * events will be delivered to your onTouchEvent() method and no longer
     * appear here.
     * </ol>
     *
     * @param ev The motion event being dispatched down the hierarchy.
     * @return Return true to steal motion events from the children and have
     * them dispatched to this ViewGroup through onTouchEvent().
     * The current target will receive an ACTION_CANCEL event, and no further
     * messages will be delivered here.
     */
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN
                && ev.isButtonPressed(MotionEvent.BUTTON_PRIMARY)
                && isOnScrollbarThumb(ev.getX(), ev.getY())) {
            return true;
        }
        return false;
    }

    private float[] getTempLocationF() {
        if (mTempPosition == null) {
            mTempPosition = new float[2];
        }
        return mTempPosition;
    }

    /**
     * Transforms a motion event into the coordinate space of a particular child view,
     * filters out irrelevant pointer ids, and overrides its action if necessary.
     * If child is null, assumes the MotionEvent will be sent to this ViewGroup instead.
     */
    private boolean dispatchTransformedTouchEvent(@Nonnull MotionEvent event,
                                                  @Nullable View child, boolean cancel) {
        final boolean handled;

        // Canceling motions is a special case.  We don't need to perform any transformations
        // or filtering.  The important part is the action, not the contents.
        final int oldAction = event.getAction();
        if (cancel || oldAction == MotionEvent.ACTION_CANCEL) {
            event.setAction(MotionEvent.ACTION_CANCEL);
            if (child == null) {
                handled = super.dispatchTouchEvent(event);
            } else {
                handled = child.dispatchTouchEvent(event);
            }
            event.setAction(oldAction);
            return handled;
        }

        if (child == null || child.hasIdentityMatrix()) {
            if (child == null) {
                handled = super.dispatchTouchEvent(event);
            } else {
                final float offsetX = mScrollX - child.mLeft;
                final float offsetY = mScrollY - child.mTop;
                event.offsetLocation(offsetX, offsetY);

                handled = child.dispatchTouchEvent(event);

                event.offsetLocation(-offsetX, -offsetY);
            }
        } else {
            final MotionEvent transformedEvent = MotionEvent.obtain(event);

            final float offsetX = mScrollX - child.mLeft;
            final float offsetY = mScrollY - child.mTop;
            transformedEvent.offsetLocation(offsetX, offsetY);
            //TODO
            //transformedEvent.transform(child.getInverseMatrix());

            handled = child.dispatchTouchEvent(transformedEvent);

            transformedEvent.recycle();
        }
        return handled;
    }

    /**
     * Returns true if a child view contains the specified point when transformed
     * into its coordinate space.
     * Child must not be null.
     */
    boolean isTransformedTouchPointInView(float x, float y, View child,
                                          float[] outLocalPoint) {
        final float[] point = getTempLocationF();
        point[0] = x;
        point[1] = y;
        transformPointToViewLocal(point, child);
        final boolean isInView = child.pointInView(point[0], point[1]);
        if (isInView && outLocalPoint != null) {
            outLocalPoint[0] = point[0];
            outLocalPoint[1] = point[1];
        }
        return isInView;
    }

    void transformPointToViewLocal(float[] point, View child) {
        point[0] += mScrollX - child.mLeft;
        point[1] += mScrollY - child.mTop;
    }

    /**
     * Add a child view to the end of array with default layout params
     *
     * @param child child view to add
     */
    public void addView(@Nonnull View child) {
        addView(child, -1);
    }

    /**
     * Add a child view to specified index of array with default layout params
     *
     * @param child child view to add
     * @param index target index
     */
    public void addView(@Nonnull View child, int index) {
        LayoutParams params = child.getLayoutParams();
        if (params == null) {
            params = createDefaultLayoutParams();
        }
        addView(child, index, params);
    }

    /**
     * Add a child view to end of array with specified width and height
     *
     * @param child  child view to add
     * @param width  view layout width
     * @param height view layout height
     */
    public void addView(@Nonnull View child, int width, int height) {
        LayoutParams params = createDefaultLayoutParams();
        params.width = width;
        params.height = height;
        addView(child, -1, params);
    }

    /**
     * Add a child view to the end of array with specified layout params
     *
     * @param child  child view to add
     * @param params layout params of the view
     */
    public void addView(@Nonnull View child, @Nonnull LayoutParams params) {
        addView(child, -1, params);
    }

    /**
     * Add a child view to specified index of array with specified layout params
     *
     * @param child  child view to add
     * @param index  target index
     * @param params layout params of the view
     */
    public void addView(@Nonnull View child, int index, @Nonnull LayoutParams params) {
        addViewInner(child, index, params);
    }

    private void addViewInner(@Nonnull final View child, int index, @Nonnull LayoutParams params) {
        if (child.getParent() != null) {
            ModernUI.LOGGER.fatal(MARKER,
                    "Failed to add child view {} to {}. The child has already a parent.", child, this);
            return;
        }

        requestLayout();

        if (!checkLayoutParams(params)) {
            params = convertLayoutParams(params);
        }

        child.setLayoutParams(params);

        if (index < 0) {
            index = mChildrenCount;
        }

        addInArray(child, index);

        child.assignParent(this);

        AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
            child.dispatchAttachedToWindow(attachInfo);
        }
    }

    public View getChildAt(int index) {
        if (index < 0 || index >= mChildrenCount) {
            return null;
        }
        return mChildren[index];
    }

    public int getChildCount() {
        return mChildrenCount;
    }

    private void addInArray(@Nonnull View child, int index) {
        View[] views = mChildren;
        final int count = mChildrenCount;
        final int size = views.length;
        if (index == count) {
            if (size == count) {
                mChildren = new View[size + ARRAY_CAPACITY_INCREMENT];
                System.arraycopy(views, 0, mChildren, 0, size);
                views = mChildren;
            }
            views[mChildrenCount++] = child;
        } else if (index < count) {
            if (size == count) {
                mChildren = new View[size + ARRAY_CAPACITY_INCREMENT];
                System.arraycopy(views, 0, mChildren, 0, index);
                System.arraycopy(views, index, mChildren, index + 1, count - index);
                views = mChildren;
            } else {
                System.arraycopy(views, index, views, index + 1, count - index);
            }
            views[index] = child;
            mChildrenCount++;
        } else {
            throw new IndexOutOfBoundsException("index=" + index + " count=" + count);
        }
    }

    /**
     * Call this method to remove all child views from the
     * ViewGroup.
     *
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(Canvas)}, {@link View#onDraw(icyllis.modernui.graphics.Canvas)},
     * {@link View#dispatchDraw(Canvas)} or any related method.</p>
     */
    public void removeAllViews() {
        removeAllViewsInLayout();
        requestLayout();
    }

    /**
     * Called by a ViewGroup subclass to remove child views from itself,
     * when it must first know its size on screen before it can calculate how many
     * child views it will render. An example is a Gallery or a ListView, which
     * may "have" 50 children, but actually only render the number of children
     * that can currently fit inside the object on screen. Do not call
     * this method unless you are extending ViewGroup and understand the
     * view measuring and layout pipeline.
     *
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(Canvas)}, {@link View#onDraw(icyllis.modernui.graphics.Canvas)},
     * {@link View#dispatchDraw(Canvas)} or any related method.</p>
     */
    public void removeAllViewsInLayout() {
        final int count = mChildrenCount;
        if (count <= 0) {
            return;
        }

        final View[] children = mChildren;
        mChildrenCount = 0;

        /*final View focused = mFocused;
        final boolean detach = mAttachInfo != null;
        boolean clearChildFocus = false;

        needGlobalAttributesUpdate(false);*/

        for (int i = count - 1; i >= 0; i--) {
            final View view = children[i];

            /*if (mTransition != null) {
                mTransition.removeChild(this, view);
            }

            if (view == focused) {
                view.unFocus(null);
                clearChildFocus = true;
            }

            view.clearAccessibilityFocus();

            cancelTouchTarget(view);
            cancelHoverTarget(view);

            if (view.getAnimation() != null ||
                    (mTransitioningViews != null && mTransitioningViews.contains(view))) {
                addDisappearingView(view);
            } else if (detach) {
                view.dispatchDetachedFromWindow();
            }

            if (view.hasTransientState()) {
                childHasTransientStateChanged(view, false);
            }

            dispatchViewRemoved(view);

            view.mParent = null;*/
            children[i] = null;
        }

        /*if (mDefaultFocus != null) {
            clearDefaultFocus(mDefaultFocus);
        }
        if (mFocusedInCluster != null) {
            clearFocusedInCluster(mFocusedInCluster);
        }
        if (clearChildFocus) {
            clearChildFocus(focused);
            if (!rootViewRequestFocus()) {
                notifyGlobalFocusCleared(focused);
            }
        }*/
    }


    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    final <T extends View> T findViewTraversal(int id) {
        if (id == mId) {
            return (T) this;
        }

        final View[] views = mChildren;
        final int count = mChildrenCount;

        View v;
        for (int i = 0; i < count; i++) {
            v = views[i].findViewTraversal(id);

            if (v != null) {
                return (T) v;
            }
        }

        return null;
    }

    private void setBooleanFlag(int flag, boolean value) {
        if (value) {
            mGroupFlags |= flag;
        } else {
            mGroupFlags &= ~flag;
        }
    }

    /**
     * Indicates whether the ViewGroup is drawing its children in the order defined by
     * {@link #getChildDrawingOrder(int, int)}.
     *
     * @return true if children drawing order is defined by {@link #getChildDrawingOrder(int, int)},
     * false otherwise
     * @see #setChildrenDrawingOrderEnabled(boolean)
     * @see #getChildDrawingOrder(int, int)
     */
    protected boolean isChildrenDrawingOrderEnabled() {
        return (mGroupFlags & FLAG_USE_CHILD_DRAWING_ORDER) == FLAG_USE_CHILD_DRAWING_ORDER;
    }

    /**
     * Tells the ViewGroup whether to draw its children in the order defined by the method
     * {@link #getChildDrawingOrder(int, int)}.
     * <p>
     * Note that View#getZ() reordering, done by {@link View#dispatchDraw(Canvas)},
     * will override custom child ordering done via this method.
     *
     * @param enabled true if the order of the children when drawing is determined by
     *                {@link #getChildDrawingOrder(int, int)}, false otherwise
     * @see #isChildrenDrawingOrderEnabled()
     * @see #getChildDrawingOrder(int, int)
     */
    protected void setChildrenDrawingOrderEnabled(boolean enabled) {
        setBooleanFlag(FLAG_USE_CHILD_DRAWING_ORDER, enabled);
    }

    /**
     * Converts drawing order position to container position. Override this
     * if you want to change the drawing order of children. By default, it
     * returns drawingPosition.
     * <p>
     * NOTE: In order for this method to be called, you must enable child ordering
     * first by calling {@link #setChildrenDrawingOrderEnabled(boolean)}.
     *
     * @param drawingPosition the drawing order position.
     * @return the container position of a child for this drawing order position.
     * @see #setChildrenDrawingOrderEnabled(boolean)
     * @see #isChildrenDrawingOrderEnabled()
     */
    protected int getChildDrawingOrder(int childCount, int drawingPosition) {
        return drawingPosition;
    }

    /**
     * Converts drawing order position to container position.
     * <p>
     * Children are not necessarily drawn in the order in which they appear in the container.
     * ViewGroups can enable a custom ordering via {@link #setChildrenDrawingOrderEnabled(boolean)}.
     * This method returns the container position of a child that appears in the given position
     * in the current drawing order.
     *
     * @param drawingPosition the drawing order position.
     * @return the container position of a child for this drawing order position.
     * @see #getChildDrawingOrder(int, int)}
     */
    public final int getChildDrawingOrder(int drawingPosition) {
        return getChildDrawingOrder(getChildCount(), drawingPosition);
    }

    /**
     * Sets whether this ViewGroup's drawable states also include
     * its children's drawable states.  This is used, for example, to
     * make a group appear to be focused when its child EditText or button
     * is focused.
     */
    public void setAddStatesFromChildren(boolean addsStates) {
        if (addsStates) {
            mGroupFlags |= FLAG_ADD_STATES_FROM_CHILDREN;
        } else {
            mGroupFlags &= ~FLAG_ADD_STATES_FROM_CHILDREN;
        }

        refreshDrawableState();
    }

    /**
     * Returns whether this ViewGroup's drawable states also include
     * its children's drawable states.  This is used, for example, to
     * make a group appear to be focused when its child EditText or button
     * is focused.
     */
    public boolean addStatesFromChildren() {
        return (mGroupFlags & FLAG_ADD_STATES_FROM_CHILDREN) != 0;
    }

    /**
     * If {@link #addStatesFromChildren} is true, refreshes this group's
     * drawable state (to include the states from its children).
     */
    @Override
    public void childDrawableStateChanged(View child) {
        if ((mGroupFlags & FLAG_ADD_STATES_FROM_CHILDREN) != 0) {
            refreshDrawableState();
        }
    }

    /**
     * Returns whether this group's children are clipped to their bounds before drawing.
     * The default value is true.
     *
     * @return True if the group's children will be clipped to their bounds,
     * false otherwise.
     * @see #setClipChildren(boolean)
     */
    public boolean getClipChildren() {
        return ((mGroupFlags & FLAG_CLIP_CHILDREN) != 0);
    }

    /**
     * By default, children are clipped to their bounds before drawing. This
     * allows view groups to override this behavior for animations, etc.
     *
     * @param clipChildren true to clip children to their bounds,
     *                     false otherwise
     */
    public void setClipChildren(boolean clipChildren) {
        boolean previousValue = (mGroupFlags & FLAG_CLIP_CHILDREN) == FLAG_CLIP_CHILDREN;
        if (clipChildren != previousValue) {
            setBooleanFlag(FLAG_CLIP_CHILDREN, clipChildren);
            invalidate();
        }
    }

    /*@Override
    final boolean onCursorPosEvent(LinkedList<View> route, double x, double y) {
        if (x >= mLeft && x < mRight && y >= mTop && y < mBottom) {
            if ((mViewFlags & ENABLED_MASK) == ENABLED) {
                route.add(this);
            }
            x += getScrollX();
            y += getScrollY();
            if (x >= mLeft && x < mRight && y >= mTop && y < mBottom) {
                for (int i = mChildrenCount - 1; i >= 0; i--) {
                    if (mChildren[i].onCursorPosEvent(route, x, y)) {
                        break;
                    }
                }
            }
            return true;
        }
        return false;
    }*/

    /**
     * Ask all of the children of this view to measure themselves, taking into
     * account both the MeasureSpec requirements for this view and its padding.
     * We skip children that are in the GONE state The heavy lifting is done in
     * getChildMeasureSpec.
     *
     * @param widthMeasureSpec  The width requirements for this view
     * @param heightMeasureSpec The height requirements for this view
     */
    protected void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
        final int size = mChildrenCount;
        final View[] children = this.mChildren;
        for (int i = 0; i < size; i++) {
            View child = children[i];
            if (child.getVisibility() != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
            }
        }
    }

    /**
     * Ask one of the children of this view to measure itself, taking into
     * account both the MeasureSpec requirements for this view and its padding.
     * The heavy lifting is done in getChildMeasureSpec.
     *
     * @param child            The child to measure
     * @param parentWidthSpec  The width requirements for this view
     * @param parentHeightSpec The height requirements for this view
     */
    protected void measureChild(@Nonnull View child, int parentWidthSpec,
                                int parentHeightSpec) {
        LayoutParams lp = child.getLayoutParams();

        int childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec,
                0, lp.width);
        int childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec,
                0, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    /**
     * Ask one of the children of this view to measure itself, taking into
     * account both the MeasureSpec requirements for this view and its padding
     * and margins. The child must have MarginLayoutParams The heavy lifting is
     * done in getChildMeasureSpec.
     *
     * @param child            The child to measure
     * @param parentWidthSpec  The width requirements for this view
     * @param widthUsed        Extra space that has been used up by the parent
     *                         horizontally (possibly by other children of the parent)
     * @param parentHeightSpec The height requirements for this view
     * @param heightUsed       Extra space that has been used up by the parent
     *                         vertically (possibly by other children of the parent)
     */
    protected void measureChildWithMargins(@Nonnull View child,
                                           int parentWidthSpec, int widthUsed,
                                           int parentHeightSpec, int heightUsed) {
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        int childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec,
                lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width);
        int childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec,
                lp.topMargin + lp.bottomMargin
                        + heightUsed, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    /**
     * Does the hard part of measureChildren: figuring out the MeasureSpec to
     * pass to a particular child. This method figures out the right MeasureSpec
     * for one dimension (height or width) of one child view.
     * <p>
     * The goal is to combine information from our MeasureSpec with the
     * LayoutParams of the child to get the best possible results. For example,
     * if the this view knows its size (because its MeasureSpec has a mode of
     * EXACTLY), and the child has indicated in its LayoutParams that it wants
     * to be the same size as the parent, the parent should ask the child to
     * layout given an exact size.
     *
     * @param spec           The requirements for this view
     * @param padding        The padding of this view for the current dimension and
     *                       margins, if applicable
     * @param childDimension How big the child wants to be in the current
     *                       dimension
     * @return a MeasureSpec integer for the child
     */
    public static int getChildMeasureSpec(int spec, int padding, int childDimension) {
        int specSize = MeasureSpec.getSize(spec);

        int size = Math.max(0, specSize - padding);

        int resultSize = 0;
        MeasureSpec.Mode resultMode = MeasureSpec.Mode.UNSPECIFIED;

        switch (MeasureSpec.getMode(spec)) {
            // Parent has imposed an exact size on us
            case EXACTLY:
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.Mode.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    // Child wants to be our size. So be it.
                    resultSize = size;
                    resultMode = MeasureSpec.Mode.EXACTLY;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    // Child wants to determine its own size. It can't be
                    // bigger than us.
                    resultSize = size;
                    resultMode = MeasureSpec.Mode.AT_MOST;
                }
                break;

            // Parent has imposed a maximum size on us
            case AT_MOST:
                if (childDimension >= 0) {
                    // Child wants a specific size... so be it
                    resultSize = childDimension;
                    resultMode = MeasureSpec.Mode.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    // Child wants to be our size, but our size is not fixed.
                    // Constrain child to not be bigger than us.
                    resultSize = size;
                    resultMode = MeasureSpec.Mode.AT_MOST;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    // Child wants to determine its own size. It can't be
                    // bigger than us.
                    resultSize = size;
                    resultMode = MeasureSpec.Mode.AT_MOST;
                }
                break;

            // Parent asked to see how big we want to be
            case UNSPECIFIED:
                if (childDimension >= 0) {
                    // Child wants a specific size... let him have it
                    resultSize = childDimension;
                    resultMode = MeasureSpec.Mode.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    // Child wants to be our size... find out how big it should
                    // be
                    resultSize = size;
                    resultMode = MeasureSpec.Mode.UNSPECIFIED;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    // Child wants to determine its own size.... find out how
                    // big it should be
                    resultSize = size;
                    resultMode = MeasureSpec.Mode.UNSPECIFIED;
                }
                break;
        }

        return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
    }

    @Override
    final void dispatchAttachedToWindow(AttachInfo info) {
        super.dispatchAttachedToWindow(info);
        for (int i = 0; i < mChildrenCount; i++) {
            mChildren[i].dispatchAttachedToWindow(info);
        }
    }

    @Override
    protected void tick(int ticks) {
        final View[] views = mChildren;
        final int count = mChildrenCount;
        for (int i = 0; i < count; i++) {
            views[i].tick(ticks);
        }
    }

    /**
     * Create a safe layout params base on the given params to fit to this view group
     * <p>
     * See also {@link #createDefaultLayoutParams()}
     * See also {@link #checkLayoutParams(LayoutParams)}
     *
     * @param params the layout params to convert
     * @return safe layout params
     */
    @Nonnull
    protected LayoutParams convertLayoutParams(@Nonnull ViewGroup.LayoutParams params) {
        return params;
    }

    /**
     * Create default layout params if child params is null
     * <p>
     * See also {@link #convertLayoutParams(LayoutParams)}
     * See also {@link #checkLayoutParams(LayoutParams)}
     *
     * @return default layout params
     */
    @Nonnull
    protected LayoutParams createDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    /**
     * Check whether given params fit to this view group
     * <p>
     * See also {@link #convertLayoutParams(LayoutParams)}
     * See also {@link #createDefaultLayoutParams()}
     *
     * @param params layout params to check
     * @return if params matched to this view group
     */
    protected boolean checkLayoutParams(@Nullable LayoutParams params) {
        return params != null;
    }

    /**
     * LayoutParams are used by views to tell their parents how they want to
     * be laid out.
     * <p>
     * The base LayoutParams class just describes how big the view wants to be
     * for both width and height.
     * <p>
     * There are subclasses of LayoutParams for different subclasses of
     * ViewGroup
     */
    public static class LayoutParams {

        /**
         * Special value for width or height, which means
         * views want to be as big as parent view,
         * but not greater than parent
         */
        public static final int MATCH_PARENT = -1;

        /**
         * Special value for width or height, which means
         * views want to be just large enough to fit
         * its own content
         */
        public static final int WRAP_CONTENT = -2;

        /**
         * The width that views want to be.
         * Can be one of MATCH_PARENT or WARP_CONTENT, or exact value
         */
        public int width;

        /**
         * The height that views want to be.
         * Can be one of MATCH_PARENT or WARP_CONTENT, or exact value
         */
        public int height;

        /**
         * Creates a new set of layout parameters with the specified width
         * and height.
         *
         * @param width  either {@link #WRAP_CONTENT},
         *               {@link #MATCH_PARENT}, or a fixed value
         * @param height either {@link #WRAP_CONTENT},
         *               {@link #MATCH_PARENT}, or a fixed value
         */
        public LayoutParams(int width, int height) {
            this.width = width >= 0 ? ViewConfig.get().getViewSize(width) : width;
            this.height = height >= 0 ? ViewConfig.get().getViewSize(height) : height;
        }

        /**
         * Copy constructor. Clones the width and height values of the source.
         *
         * @param source the layout params to copy from.
         */
        public LayoutParams(@Nonnull LayoutParams source) {
            width = source.width;
            height = source.height;
        }

        /**
         * Create a new object copied from this params
         *
         * @return copied layout params
         */
        @Nonnull
        public LayoutParams copy() {
            return new LayoutParams(this);
        }

    }

    public static class MarginLayoutParams extends LayoutParams {

        /**
         * The left margin
         */
        public int leftMargin;

        /**
         * The top margin
         */
        public int topMargin;

        /**
         * The right margin
         */
        public int rightMargin;

        /**
         * The bottom margin
         */
        public int bottomMargin;

        /**
         * Creates a new set of layout parameters with the specified width
         * and height.
         *
         * @param width  either {@link #WRAP_CONTENT},
         *               {@link #MATCH_PARENT}, or a fixed value
         * @param height either {@link #WRAP_CONTENT},
         *               {@link #MATCH_PARENT}, or a fixed value
         */
        public MarginLayoutParams(int width, int height) {
            super(width, height);
        }

        /**
         * Sets the margins, and its values should be positive.
         *
         * @param left   the left margin size
         * @param top    the top margin size
         * @param right  the right margin size
         * @param bottom the bottom margin size
         */
        public void setMargins(int left, int top, int right, int bottom) {
            leftMargin = left;
            topMargin = top;
            rightMargin = right;
            bottomMargin = bottom;
        }

        /**
         * Copy constructor. Clones the width, height and margin values of the source.
         *
         * @param source the layout params to copy from.
         */
        public MarginLayoutParams(@Nonnull MarginLayoutParams source) {
            super(source.width, source.height);

            leftMargin = source.leftMargin;
            topMargin = source.topMargin;
            rightMargin = source.rightMargin;
            bottomMargin = source.bottomMargin;
        }

        public MarginLayoutParams(@Nonnull LayoutParams source) {
            super(source);
        }

        @Nonnull
        @Override
        public MarginLayoutParams copy() {
            return new MarginLayoutParams(this);
        }
    }

    /**
     * Describes a hovered view.
     */
    private static final class HoverTarget {

        private static final int MAX_RECYCLED = 32;
        private static final Object sRecyclerLock = new Object();
        private static HoverTarget sRecyclerTop;
        private static int sRecyclerUsed;

        // The hovered view, one of the child of this ViewGroup
        public View child;

        // The next target in the linked list.
        public HoverTarget next;

        private HoverTarget() {
        }

        @Nonnull
        public static HoverTarget obtain(@Nonnull View child) {
            final HoverTarget target;
            synchronized (sRecyclerLock) {
                if (sRecyclerTop == null) {
                    target = new HoverTarget();
                } else {
                    target = sRecyclerTop;
                    sRecyclerTop = target.next;
                    sRecyclerUsed--;
                    target.next = null;
                }
            }
            target.child = child;
            return target;
        }

        public void recycle() {
            if (child == null) {
                throw new IllegalStateException(this + " already recycled");
            }
            synchronized (sRecyclerLock) {
                if (sRecyclerUsed < MAX_RECYCLED) {
                    sRecyclerUsed++;
                    next = sRecyclerTop;
                    sRecyclerTop = this;
                } else {
                    next = null;
                }
                child = null;
            }
        }
    }

    /**
     * Describes a touched view and the ids of the pointers that it has captured.
     * <p>
     * This code assumes that pointer ids are always in the range 0..31 such that
     * it can use a bitfield to track which pointer ids are present.
     * As it happens, the lower layers of the input dispatch pipeline also use the
     * same trick so the assumption should be safe here...
     */
    private static final class TouchTarget {

        private static final Pool<TouchTarget> sPool = Pools.concurrent(12);

        // The touched view, one of the child of this ViewGroup
        public View child;

        private TouchTarget() {
        }

        @Nonnull
        public static TouchTarget obtain(@Nonnull View child) {
            TouchTarget target = sPool.acquire();
            if (target == null) {
                target = new TouchTarget();
            }
            target.child = child;
            return target;
        }

        public void recycle() {
            if (child == null) {
                throw new IllegalStateException(this + " already recycled");
            }
            sPool.release(this);
            child = null;
        }
    }
}
