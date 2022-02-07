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

import icyllis.modernui.math.Rect;
import icyllis.modernui.view.View.NestedScrollType;
import icyllis.modernui.view.View.ScrollAxis;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Defines an object that can work as a parent of a View.
 */
public interface ViewParent {

    /**
     * Returns the parent of this ViewParent.
     *
     * @return the parent or {@code null} if the parent is not available
     */
    @Nullable
    ViewParent getParent();

    /**
     * Called when something has changed which has invalidated the layout of a
     * child of this view parent. This will schedule a layout pass of the view tree.
     */
    void requestLayout();

    /**
     * Indicates whether layout was requested on this view parent.
     *
     * @return true if layout was requested, false otherwise
     */
    boolean isLayoutRequested();

    /**
     * Called when a child of this parent wants focus
     *
     * @param child   The child of this ViewParent that wants focus. This view
     *                will contain the focused view. It is not necessarily the view that
     *                actually has focus.
     * @param focused The view that is a descendant of child that actually has
     *                focus
     */
    void requestChildFocus(View child, View focused);

    /**
     * Called when a child of this parent is giving up focus
     *
     * @param child The view that is giving up focus
     */
    void clearChildFocus(View child);

    /**
     * Find the nearest view in the specified direction that wants to take focus
     *
     * @param v         The view that currently has focus
     * @param direction One of FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, and FOCUS_RIGHT
     */
    View focusSearch(View v, int direction);

    /**
     * Find the nearest keyboard navigation cluster in the specified direction.
     * This does not actually give focus to that cluster.
     *
     * @param currentCluster The starting point of the search. Null means the current cluster is not
     *                       found yet
     * @param direction      Direction to look
     * @return The nearest keyboard navigation cluster in the specified direction, or null if none
     * can be found
     */
    View keyboardNavigationClusterSearch(View currentCluster, int direction);

    /**
     * Change the z order of the child, so it's on top of all other children.
     * This ordering change may affect layout, if this container
     * uses an order-dependent layout scheme (e.g., LinearLayout).
     *
     * @param child The child to bring to the top of the z order
     */
    void bringChildToFront(View child);

    /**
     * Tells the parent that a new focusable view has become available. This is
     * to handle transitions from the case where there are no focusable views to
     * the case where the first focusable view appears.
     *
     * @param v The view that has become newly focusable
     */
    void focusableViewAvailable(View v);

    /**
     * Shows the context menu for the specified view or its ancestors anchored
     * to the specified view-relative coordinate.
     * <p>
     * In most cases, a subclass does not need to override this. However, if
     * the subclass is added directly to the window manager (for example,
     * {@link ViewManager#addView(View, ViewGroup.LayoutParams)})
     * then it should override this and show the context menu.
     *
     * @param originalView the source view where the context menu was first
     *                     invoked
     * @param x            the X coordinate in pixels relative to the original view to
     *                     which the menu should be anchored, or {@link Float#NaN} to
     *                     disable anchoring
     * @param y            the Y coordinate in pixels relative to the original view to
     *                     which the menu should be anchored, or {@link Float#NaN} to
     *                     disable anchoring
     * @return {@code true} if the context menu was shown, {@code false}
     * otherwise
     */
    boolean showContextMenuForChild(View originalView, float x, float y);

    /**
     * Have the parent populate the specified context menu if it has anything to
     * add (and then recurse on its parent).
     *
     * @param menu The menu to populate
     */
    void createContextMenu(ContextMenu menu);

    /**
     * Start an action mode of a specific type for the specified view.
     *
     * <p>In most cases, a subclass does not need to override this. However, if the
     * subclass is added directly to the window manager (for example,
     * {@link ViewManager#addView(View, ViewGroup.LayoutParams)})
     * then it should override this and start the action mode.</p>
     *
     * @param originalView The source view where the action mode was first invoked
     * @param callback     The callback that will handle lifecycle events for the action mode
     * @param type         One of {@link ActionMode#TYPE_PRIMARY} or {@link ActionMode#TYPE_FLOATING}.
     * @return The new action mode if it was started, null otherwise
     */
    ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback, int type);

    /**
     * This method is called on the parent when a child's drawable state
     * has changed.
     *
     * @param child The child whose drawable state has changed.
     */
    void childDrawableStateChanged(View child);

    /**
     * Called when a child does not want this parent and its ancestors to
     * intercept touch events with
     * {@link ViewGroup#onInterceptTouchEvent(MotionEvent)}.
     *
     * <p>This parent should pass this call onto its parents. This parent must obey
     * this request for the duration of the touch (that is, only clear the flag
     * after this parent has received an up or a cancel.</p>
     *
     * @param disallowIntercept True if the child does not want the parent to
     *                          intercept touch events.
     */
    void requestDisallowInterceptTouchEvent(boolean disallowIntercept);

    /**
     * Called when a child of this group wants a particular rectangle to be
     * positioned onto the screen.  {@link ViewGroup}s overriding this can trust
     * that:
     * <ul>
     *   <li>child will be a direct child of this group</li>
     *   <li>rectangle will be in the child's content coordinates</li>
     * </ul>
     *
     * <p>{@link ViewGroup}s overriding this should uphold the contract:</p>
     * <ul>
     *   <li>nothing will change if the rectangle is already visible</li>
     *   <li>the view port will be scrolled only just enough to make the
     *       rectangle visible</li>
     * <ul>
     *
     * @param child     The direct child making the request.
     * @param rectangle The rectangle in the child's coordinates the child
     *                  wishes to be on the screen.
     * @param immediate True to forbid animated or delayed scrolling,
     *                  false otherwise
     * @return Whether the group scrolled to handle the operation
     */
    boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate);

    /**
     * Called when a child view now has or no longer is tracking transient state.
     *
     * <p>"Transient state" is any state that a View might hold that is not expected to
     * be reflected in the data model that the View currently presents. This state only
     * affects the presentation to the user within the View itself, such as the current
     * state of animations in progress or the state of a text selection operation.</p>
     *
     * <p>Transient state is useful for hinting to other components of the View system
     * that a particular view is tracking something complex but encapsulated.
     * A <code>ListView</code> for example may acknowledge that list item Views
     * with transient state should be preserved within their position or stable item ID
     * instead of treating that view as trivially replaceable by the backing adapter.
     * This allows adapter implementations to be simpler instead of needing to track
     * the state of item view animations in progress such that they could be restored
     * in the event of an unexpected recycling and rebinding of attached item views.</p>
     *
     * <p>This method is called on a parent view when a child view or a view within
     * its subtree begins or ends tracking of internal transient state.</p>
     *
     * @param child             Child view whose state has changed
     * @param hasTransientState true if this child has transient state
     */
    void childHasTransientStateChanged(View child, boolean hasTransientState);

    /**
     * Tells if this view parent can resolve the layout direction.
     * See {@link View#setLayoutDirection(int)}
     *
     * @return True if this view parent can resolve the layout direction.
     */
    boolean canResolveLayoutDirection();

    /**
     * Tells if this view parent layout direction is resolved.
     * See {@link View#setLayoutDirection(int)}
     *
     * @return True if this view parent layout direction is resolved.
     */
    boolean isLayoutDirectionResolved();

    /**
     * Return this view parent layout direction. See {@link View#getLayoutDirection()}
     *
     * @return {@link View#LAYOUT_DIRECTION_RTL} if the layout direction is RTL or returns
     * {@link View#LAYOUT_DIRECTION_LTR} if the layout direction is not RTL.
     */
    int getLayoutDirection();

    /**
     * Tells if this view parent can resolve the text direction.
     * See {@link View#setTextDirection(int)}
     *
     * @return True if this view parent can resolve the text direction.
     */
    boolean canResolveTextDirection();

    /**
     * Tells if this view parent text direction is resolved.
     * See {@link View#setTextDirection(int)}
     *
     * @return True if this view parent text direction is resolved.
     */
    boolean isTextDirectionResolved();

    /**
     * Return this view parent text direction. See {@link View#getTextDirection()}
     *
     * @return the resolved text direction. Returns one of:
     * <p>
     * {@link View#TEXT_DIRECTION_FIRST_STRONG}
     * {@link View#TEXT_DIRECTION_ANY_RTL},
     * {@link View#TEXT_DIRECTION_LTR},
     * {@link View#TEXT_DIRECTION_RTL},
     * {@link View#TEXT_DIRECTION_LOCALE}
     */
    int getTextDirection();

    /**
     * Tells if this view parent can resolve the text alignment.
     * See {@link View#setTextAlignment(int)}
     *
     * @return True if this view parent can resolve the text alignment.
     */
    boolean canResolveTextAlignment();

    /**
     * Tells if this view parent text alignment is resolved.
     * See {@link View#setTextAlignment(int)}
     *
     * @return True if this view parent text alignment is resolved.
     */
    boolean isTextAlignmentResolved();

    /**
     * Return this view parent text alignment. See {@link View#getTextAlignment()}
     *
     * @return the resolved text alignment. Returns one of:
     * <p>
     * {@link View#TEXT_ALIGNMENT_GRAVITY},
     * {@link View#TEXT_ALIGNMENT_CENTER},
     * {@link View#TEXT_ALIGNMENT_TEXT_START},
     * {@link View#TEXT_ALIGNMENT_TEXT_END},
     * {@link View#TEXT_ALIGNMENT_VIEW_START},
     * {@link View#TEXT_ALIGNMENT_VIEW_END}
     */
    int getTextAlignment();

    /// Nested Scrolling \\\

    /**
     * React to a descendant view initiating a nestable scroll operation, claiming the
     * nested scroll operation if appropriate.
     *
     * <p>This method will be called in response to a descendant view invoking
     * {@link View#startNestedScroll(int, int)}. Each parent up the view hierarchy will be
     * given an opportunity to respond and claim the nested scrolling operation by returning
     * <code>true</code>.</p>
     *
     * <p>This method may be overridden by ViewParent implementations to indicate when the view
     * is willing to support a nested scrolling operation that is about to begin. If it returns
     * true, this ViewParent will become the target view's nested scrolling parent for the duration
     * of the scroll operation in progress. When the nested scroll is finished this ViewParent
     * will receive a call to {@link #onStopNestedScroll(View, int)}.
     * </p>
     *
     * @param child  Direct child of this ViewParent containing target
     * @param target View that initiated the nested scroll
     * @param axes   Flags consisting of {@link View#SCROLL_AXIS_HORIZONTAL},
     *               {@link View#SCROLL_AXIS_VERTICAL} or both
     * @param type   the type of input which cause this scroll event
     * @return true if this ViewParent accepts the nested scroll operation
     */
    boolean onStartNestedScroll(@Nonnull View child, @Nonnull View target, @ScrollAxis int axes,
                                @NestedScrollType int type);

    /**
     * React to the successful claiming of a nested scroll operation.
     *
     * <p>This method will be called after
     * {@link #onStartNestedScroll(View, View, int, int) onStartNestedScroll} returns true. It
     * offers an opportunity for the view and its superclasses to perform initial configuration
     * for the nested scroll. Implementations of this method should always call their superclasses'
     * implementation of this method if one is present.</p>
     *
     * @param child  Direct child of this ViewParent containing target
     * @param target View that initiated the nested scroll
     * @param axes   Flags consisting of {@link View#SCROLL_AXIS_HORIZONTAL},
     *               {@link View#SCROLL_AXIS_VERTICAL} or both
     * @param type   the type of input which cause this scroll event
     * @see #onStartNestedScroll(View, View, int, int)
     * @see #onStopNestedScroll(View, int)
     */
    void onNestedScrollAccepted(@Nonnull View child, @Nonnull View target, @ScrollAxis int axes,
                                @NestedScrollType int type);

    /**
     * React to a nested scroll operation ending.
     *
     * <p>Perform cleanup after a nested scrolling operation.
     * This method will be called when a nested scroll stops, for example when a nested touch
     * scroll ends with a {@link MotionEvent#ACTION_UP} or {@link MotionEvent#ACTION_CANCEL} event.
     * Implementations of this method should always call their superclasses' implementation of this
     * method if one is present.</p>
     *
     * @param target View that initiated the nested scroll
     * @param type   the type of input which cause this scroll event
     */
    void onStopNestedScroll(@Nonnull View target, @NestedScrollType int type);

    /**
     * React to a nested scroll in progress.
     *
     * <p>This method will be called when the ViewParent's current nested scrolling child view
     * dispatches a nested scroll event. To receive calls to this method the ViewParent must have
     * previously returned <code>true</code> for a call to
     * {@link #onStartNestedScroll(View, View, int, int)}.
     *
     * <p>Both the consumed and unconsumed portions of the scroll distance are reported to the
     * ViewParent. An implementation may choose to use the consumed portion to match or chase scroll
     * position of multiple child elements, for example. The unconsumed portion may be used to
     * allow continuous dragging of multiple scrolling or draggable elements, such as scrolling
     * a list within a vertical drawer where the drawer begins dragging once the edge of inner
     * scrolling content is reached.
     *
     * <p>This method is called when a nested scrolling child invokes
     * {@link View#dispatchNestedScroll(int, int, int, int, int[], int, int[])}} or
     * one of methods it overloads.
     *
     * <p>An implementation must report how many pixels of the x and y scroll distances were
     * consumed by this nested scrolling parent by adding the consumed distances to the
     * <code>consumed</code> parameter. <code>consumed</code> should also be passed up to
     * it's nested scrolling parent so that the parent may also add any scroll distance it consumes.
     * Index 0 corresponds to dx and index 1 corresponds to dy.
     *
     * @param target       The descendant view controlling the nested scroll
     * @param dxConsumed   Horizontal scroll distance in pixels already consumed by target
     * @param dyConsumed   Vertical scroll distance in pixels already consumed by target
     * @param dxUnconsumed Horizontal scroll distance in pixels not consumed by target
     * @param dyUnconsumed Vertical scroll distance in pixels not consumed by target
     * @param type         the type of input which cause this scroll event
     * @param consumed     Output. Upon this method returning, will contain the scroll
     *                     distances consumed by this nested scrolling parent and the scroll distances
     *                     consumed by any other parent up the view hierarchy
     * @see View#dispatchNestedScroll(int, int, int, int, int[], int, int[])
     */
    void onNestedScroll(@Nonnull View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                        int dyUnconsumed, @NestedScrollType int type, @Nonnull int[] consumed);

    /**
     * React to a nested scroll in progress before the target view consumes a portion of the scroll.
     *
     * <p>When working with nested scrolling often the parent view may want an opportunity
     * to consume the scroll before the nested scrolling child does. An example of this is a
     * drawer that contains a scrollable list. The user will want to be able to scroll the list
     * fully into view before the list itself begins scrolling.</p>
     *
     * <p><code>onNestedPreScroll</code> is called when a nested scrolling child invokes
     * {@link View#dispatchNestedPreScroll(int, int, int[], int[], int)}. The implementation should
     * report how any pixels of the scroll reported by dx, dy were consumed in the
     * <code>consumed</code> array. Index 0 corresponds to dx and index 1 corresponds to dy.
     * This parameter will never be null. Initial values for consumed[0] and consumed[1]
     * will always be 0.</p>
     *
     * @param target   View that initiated the nested scroll
     * @param dx       Horizontal scroll distance in pixels
     * @param dy       Vertical scroll distance in pixels
     * @param consumed Output. The horizontal and vertical scroll distance consumed by this parent
     * @param type     the type of input which cause this scroll event
     */
    void onNestedPreScroll(@Nonnull View target, int dx, int dy, @Nonnull int[] consumed,
                           @NestedScrollType int type);

    /**
     * Request a fling from a nested scroll.
     *
     * <p>This method signifies that a nested scrolling child has detected suitable conditions
     * for a fling. Generally this means that a touch scroll has ended with a
     * {@link VelocityTracker velocity} in the direction of scrolling that meets or exceeds
     * the {@link ViewConfiguration#getScaledMinimumFlingVelocity() minimum fling velocity}
     * along a scrollable axis.</p>
     *
     * <p>If a nested scrolling child view would normally fling but it is at the edge of
     * its own content, it can use this method to delegate the fling to its nested scrolling
     * parent instead. The parent may optionally consume the fling or observe a child fling.</p>
     *
     * @param target    View that initiated the nested scroll
     * @param velocityX Horizontal velocity in pixels per second
     * @param velocityY Vertical velocity in pixels per second
     * @param consumed  true if the child consumed the fling, false otherwise
     * @return true if this parent consumed or otherwise reacted to the fling
     */
    boolean onNestedFling(@Nonnull View target, float velocityX, float velocityY, boolean consumed);

    /**
     * React to a nested fling before the target view consumes it.
     *
     * <p>This method signifies that a nested scrolling child has detected a fling with the given
     * velocity along each axis. Generally this means that a touch scroll has ended with a
     * {@link VelocityTracker velocity} in the direction of scrolling that meets or exceeds
     * the {@link ViewConfiguration#getScaledMinimumFlingVelocity() minimum fling velocity}
     * along a scrollable axis.</p>
     *
     * <p>If a nested scrolling parent is consuming motion as part of a
     * {@link #onNestedPreScroll(View, int, int, int[], int) pre-scroll}, it may be appropriate for
     * it to also consume the pre-fling to complete that same motion. By returning
     * <code>true</code> from this method, the parent indicates that the child should not
     * fling its own internal content as well.</p>
     *
     * @param target    View that initiated the nested scroll
     * @param velocityX Horizontal velocity in pixels per second
     * @param velocityY Vertical velocity in pixels per second
     * @return true if this parent consumed the fling ahead of the target view
     */
    boolean onNestedPreFling(@Nonnull View target, float velocityX, float velocityY);

    /**
     * Return the current axes of nested scrolling for this NestedScrollingParent.
     *
     * <p>A NestedScrollingParent returning something other than {@link View#SCROLL_AXIS_NONE}
     * is currently acting as a nested scrolling parent for one or more descendant views in
     * the hierarchy.</p>
     *
     * @return Flags indicating the current axes of nested scrolling
     * @see View#SCROLL_AXIS_HORIZONTAL
     * @see View#SCROLL_AXIS_VERTICAL
     * @see View#SCROLL_AXIS_NONE
     */
    @ScrollAxis
    int getNestedScrollAxes();
}
