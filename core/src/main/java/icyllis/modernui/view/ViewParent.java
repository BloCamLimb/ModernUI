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
    //View keyboardNavigationClusterSearch(View currentCluster, int direction);

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
     * This method is called on the parent when a child's drawable state
     * has changed.
     *
     * @param child The child whose drawable state has changed.
     */
    void childDrawableStateChanged(View child);

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
}
