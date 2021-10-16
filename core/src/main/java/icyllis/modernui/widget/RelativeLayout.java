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

package icyllis.modernui.widget;

import icyllis.modernui.math.Rect;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.MeasureSpec;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * @since 2.0
 */
@SuppressWarnings("unused")
public class RelativeLayout extends ViewGroup {

    /**
     * Rule that aligns a child's right edge with another child's left edge.
     */
    public static final int LEFT_OF = 0;
    /**
     * Rule that aligns a child's left edge with another child's right edge.
     */
    public static final int RIGHT_OF = 1;
    /**
     * Rule that aligns a child's bottom edge with another child's top edge.
     */
    public static final int ABOVE = 2;
    /**
     * Rule that aligns a child's top edge with another child's bottom edge.
     */
    public static final int BELOW = 3;
    /**
     * Rule that aligns a child's left edge with another child's left edge.
     */
    public static final int ALIGN_LEFT = 4;
    /**
     * Rule that aligns a child's top edge with another child's top edge.
     */
    public static final int ALIGN_TOP = 5;
    /**
     * Rule that aligns a child's right edge with another child's right edge.
     */
    public static final int ALIGN_RIGHT = 6;
    /**
     * Rule that aligns a child's bottom edge with another child's bottom edge.
     */
    public static final int ALIGN_BOTTOM = 7;

    private static final int VERB_COUNT = 8;


    private static final int[] RULES_VERTICAL = {
            ABOVE, BELOW, ALIGN_TOP, ALIGN_BOTTOM
    };

    private static final int[] RULES_HORIZONTAL = {
            LEFT_OF, RIGHT_OF, ALIGN_LEFT, ALIGN_RIGHT
    };

    /**
     * Used to indicate left/right/top/bottom should be inferred from constraints
     */
    private static final int VALUE_NOT_SET = Integer.MIN_VALUE;


    /**
     * See {@link #setGravity(int)}
     */
    private int gravity = Gravity.TOP | Gravity.START;

    /**
     * See {@link #setIgnoreGravity(int)}
     */
    private int ignoreGravity = NO_ID;

    // inner
    private boolean dirtyHierarchy;

    // inner
    private View[] sortedHorizontalChildren;
    private View[] sortedVerticalChildren;

    // inner
    private final Rect inBounds = new Rect();
    private final Rect outBounds = new Rect();

    // inner
    private final DependencyGraph mGraph = new DependencyGraph();

    public RelativeLayout() {

    }

    private void sortChildren() {
        int count = getChildCount();

        if (sortedVerticalChildren == null || sortedVerticalChildren.length != count) {
            sortedVerticalChildren = new View[count];
        }

        if (sortedHorizontalChildren == null || sortedHorizontalChildren.length != count) {
            sortedHorizontalChildren = new View[count];
        }

        mGraph.clear();

        for (int i = 0; i < count; i++) {
            mGraph.add(getChildAt(i));
        }

        mGraph.getSortedViews(sortedVerticalChildren, RULES_VERTICAL);
        mGraph.getSortedViews(sortedHorizontalChildren, RULES_HORIZONTAL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (dirtyHierarchy) {
            sortChildren();
            dirtyHierarchy = false;
        }

        int myWidth = -1;
        int myHeight = -1;

        int width = 0;
        int height = 0;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        // Record our dimensions if they are known;
        if (widthMode != MeasureSpec.UNSPECIFIED) {
            myWidth = widthSize;
        }

        if (heightMode != MeasureSpec.UNSPECIFIED) {
            myHeight = heightSize;
        }

        if (widthMode == MeasureSpec.EXACTLY) {
            width = myWidth;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = myHeight;
        }

        View ignore = null;
        int gravity = this.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        boolean horizontalGravity = gravity != Gravity.LEFT && gravity != 0;
        gravity = this.gravity & Gravity.VERTICAL_GRAVITY_MASK;
        boolean verticalGravity = gravity != Gravity.TOP && gravity != 0;

        int left = Integer.MAX_VALUE;
        int top = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        int bottom = Integer.MIN_VALUE;

        boolean offsetHorizontalAxis = false;
        boolean offsetVerticalAxis = false;

        if ((horizontalGravity || verticalGravity) && ignoreGravity != View.NO_ID) {
            ignore = findViewById(ignoreGravity);
        }

        boolean isWrapContentWidth = widthMode != MeasureSpec.EXACTLY;
        boolean isWrapContentHeight = heightMode != MeasureSpec.EXACTLY;

        View[] views = sortedHorizontalChildren;
        int count = views.length;

        for (int i = 0; i < count; i++) {
            View child = views[i];
            if (child.getVisibility() != GONE) {
                LayoutParams params = (LayoutParams) child.getLayoutParams();
                int[] rules = params.rules;

                applyHorizontalSizeRules(params, myWidth, rules);
                measureChildHorizontal(child, params, myWidth, myHeight);

                if (positionChildHorizontal(child, params, myWidth, isWrapContentWidth)) {
                    offsetHorizontalAxis = true;
                }
            }
        }

        views = sortedVerticalChildren;
        count = views.length;

        for (int i = 0; i < count; i++) {
            View child = views[i];
            if (child.getVisibility() != GONE) {
                LayoutParams params = (LayoutParams) child.getLayoutParams();
                int[] rules = params.rules;

                applyVerticalSizeRules(params, myHeight, rules);
                measureChild(child, params, myWidth, myHeight);
                if (positionChildVertical(child, params, myHeight, isWrapContentHeight)) {
                    offsetVerticalAxis = true;
                }

                if (isWrapContentWidth) {
                    width = Math.max(width, params.mRight + params.rightMargin);
                }

                if (isWrapContentHeight) {
                    height = Math.max(height, params.mBottom + params.bottomMargin);
                }

                if (child != ignore || verticalGravity) {
                    left = Math.min(left, params.mLeft - params.leftMargin);
                    top = Math.min(top, params.mTop - params.topMargin);
                }

                if (child != ignore || horizontalGravity) {
                    right = Math.max(right, params.mRight + params.rightMargin);
                    bottom = Math.max(bottom, params.mBottom + params.bottomMargin);
                }
            }
        }

        if (isWrapContentWidth) {

            if (getLayoutParams() != null && getLayoutParams().width >= 0) {
                width = Math.max(width, getLayoutParams().width);
            }

            width = Math.max(width, getMinimumWidth());
            width = resolveSize(width, widthMeasureSpec);

            if (offsetHorizontalAxis) {
                for (int i = 0; i < count; i++) {
                    View child = views[i];
                    if (child.getVisibility() != GONE) {
                        LayoutParams params = (LayoutParams) child.getLayoutParams();
                        int childHorizontalGravity = params.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;

                        if (childHorizontalGravity == Gravity.CENTER_HORIZONTAL) {
                            centerHorizontal(child, params, width);
                        } else if (childHorizontalGravity == Gravity.RIGHT) {
                            int childWidth = child.getMeasuredWidth();
                            params.mLeft = width - childWidth;
                            params.mRight = params.mLeft + childWidth;
                        }
                    }
                }
            }
        }

        if (isWrapContentHeight) {

            if (getLayoutParams() != null && getLayoutParams().height >= 0) {
                height = Math.max(height, getLayoutParams().height);
            }

            height = Math.max(height, getMinimumHeight());
            height = resolveSize(height, heightMeasureSpec);

            if (offsetVerticalAxis) {
                for (int i = 0; i < count; i++) {
                    View child = views[i];
                    if (child.getVisibility() != GONE) {
                        LayoutParams params = (LayoutParams) child.getLayoutParams();
                        int childVerticalGravity = params.gravity & Gravity.VERTICAL_GRAVITY_MASK;
                        if (childVerticalGravity == Gravity.CENTER_VERTICAL) {
                            centerVertical(child, params, height);
                        } else if (childVerticalGravity == Gravity.BOTTOM) {
                            int childHeight = child.getMeasuredHeight();
                            params.mTop = height - childHeight;
                            params.mBottom = params.mTop + childHeight;
                        }
                    }
                }
            }
        }

        if (horizontalGravity || verticalGravity) {
            Rect inBounds = this.inBounds;
            inBounds.set(0, 0, width, height);
            Gravity.apply(gravity, right - left, bottom - top, inBounds, 0, 0, outBounds);

            int horizontalOffset = outBounds.left - left;
            int verticalOffset = outBounds.top - top;

            if (horizontalOffset != 0 || verticalOffset != 0) {
                for (int i = 0; i < count; i++) {
                    View child = views[i];
                    if (child.getVisibility() != GONE && child != ignore) {
                        LayoutParams params = (LayoutParams) child.getLayoutParams();
                        if (horizontalGravity) {
                            params.mLeft += horizontalOffset;
                            params.mRight += horizontalOffset;
                        }
                        if (verticalGravity) {
                            params.mTop += verticalOffset;
                            params.mBottom += verticalOffset;
                        }
                    }
                }
            }
        }

        setMeasuredDimension(width, height);
    }

    /**
     * Measure a child. The child should have left, top, right and bottom information
     * stored in its LayoutParams. If any of these values is VALUE_NOT_SET it means
     * that the view can extend up to the corresponding edge.
     *
     * @param child    Child to measure
     * @param params   LayoutParams associated with child
     * @param myWidth  Width of the the RelativeLayout
     * @param myHeight Height of the RelativeLayout
     */
    private void measureChild(@Nonnull View child, @Nonnull LayoutParams params, int myWidth, int myHeight) {
        int childWidthMeasureSpec = getChildMeasureSpec(params.mLeft,
                params.mRight, params.width,
                params.leftMargin, params.rightMargin,
                myWidth);
        int childHeightMeasureSpec = getChildMeasureSpec(params.mTop,
                params.mBottom, params.height,
                params.topMargin, params.bottomMargin,
                myHeight);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    private void measureChildHorizontal(@Nonnull View child, @Nonnull LayoutParams params, int myWidth, int myHeight) {
        int childWidthMeasureSpec = getChildMeasureSpec(params.mLeft, params.mRight,
                params.width, params.leftMargin, params.rightMargin, myWidth);

        int childHeightMeasureSpec;
        if (myHeight < 0) {
            if (params.height >= 0) {
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        params.height, MeasureSpec.EXACTLY);
            } else {
                // Negative values in a mySize/myWidth/myWidth value in
                // RelativeLayout measurement is code for, "we got an
                // unspecified mode in the RelativeLayout's measure spec."
                // Carry it forward.
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            }
        } else {
            int maxHeight = Math.max(0, myHeight);

            int heightMode;
            if (params.height == LayoutParams.MATCH_PARENT) {
                heightMode = MeasureSpec.EXACTLY;
            } else {
                heightMode = MeasureSpec.AT_MOST;
            }

            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, heightMode);
        }

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    /**
     * Get a measure spec that accounts for all of the constraints on this view.
     * This includes size constraints imposed by the RelativeLayout as well as
     * the View's desired dimension.
     *
     * @param childStart  The left or top field of the child's layout params
     * @param childEnd    The right or bottom field of the child's layout params
     * @param childSize   The child's desired size (the width or height field of
     *                    the child's layout params)
     * @param startMargin The left or top margin
     * @param endMargin   The right or bottom margin
     * @param mySize      The width or height of this view (the RelativeLayout)
     * @return MeasureSpec for the child
     */
    private int getChildMeasureSpec(int childStart, int childEnd,
                                    int childSize, int startMargin, int endMargin, int mySize) {
        int childSpecMode = MeasureSpec.UNSPECIFIED;
        int childSpecSize = 0;

        // Negative values in a mySize value in RelativeLayout
        // measurement is code for, "we got an unspecified mode in the
        // RelativeLayout's measure spec."
        boolean isUnspecified = mySize < 0;
        if (isUnspecified) {
            if (childStart != VALUE_NOT_SET && childEnd != VALUE_NOT_SET) {
                // Constraints fixed both edges, so child has an exact size.
                childSpecSize = Math.max(0, childEnd - childStart);
                childSpecMode = MeasureSpec.EXACTLY;
            } else if (childSize >= 0) {
                // The child specified an exact size.
                childSpecSize = childSize;
                childSpecMode = MeasureSpec.EXACTLY;
            } else {
                // Allow the child to be whatever size it wants.
                childSpecSize = 0;
                childSpecMode = MeasureSpec.UNSPECIFIED;
            }

            return MeasureSpec.makeMeasureSpec(childSpecSize, childSpecMode);
        }

        // Figure out start and end bounds.
        int tempStart = childStart;
        int tempEnd = childEnd;

        // If the view did not express a layout constraint for an edge, use
        // view's margins and our padding
        if (tempStart == VALUE_NOT_SET) {
            tempStart = startMargin;
        }
        if (tempEnd == VALUE_NOT_SET) {
            tempEnd = mySize - endMargin;
        }

        // Figure out maximum size available to this view
        int maxAvailable = tempEnd - tempStart;

        if (childStart != VALUE_NOT_SET && childEnd != VALUE_NOT_SET) {
            // Constraints fixed both edges, so child must be an exact size.
            childSpecMode = MeasureSpec.EXACTLY;
            childSpecSize = Math.max(0, maxAvailable);
        } else {
            if (childSize >= 0) {
                // Child wanted an exact size. Give as much as possible.
                childSpecMode = MeasureSpec.EXACTLY;

                if (maxAvailable >= 0) {
                    // We have a maximum size in this dimension.
                    childSpecSize = Math.min(maxAvailable, childSize);
                } else {
                    // We can grow in this dimension.
                    childSpecSize = childSize;
                }
            } else if (childSize == LayoutParams.MATCH_PARENT) {
                // Child wanted to be as big as possible. Give all available
                // space.
                childSpecMode = MeasureSpec.EXACTLY;
                childSpecSize = Math.max(0, maxAvailable);
            } else if (childSize == LayoutParams.WRAP_CONTENT) {
                // Child wants to wrap content. Use AT_MOST to communicate
                // available space if we know our max size.
                if (maxAvailable >= 0) {
                    // We have a maximum size in this dimension.
                    childSpecMode = MeasureSpec.AT_MOST;
                    childSpecSize = maxAvailable;
                }  // We can grow in this dimension. Child can be as big as it
                // wants.

            }
        }

        return MeasureSpec.makeMeasureSpec(childSpecSize, childSpecMode);
    }

    private boolean positionChildHorizontal(View child, @Nonnull LayoutParams params, int myWidth,
                                            boolean wrapContent) {

        int gravity = params.gravity;

        if (params.mLeft == VALUE_NOT_SET && params.mRight != VALUE_NOT_SET) {
            // Right is fixed, but left varies
            params.mLeft = params.mRight - child.getMeasuredWidth();
        } else if (params.mLeft != VALUE_NOT_SET && params.mRight == VALUE_NOT_SET) {
            // Left is fixed, but right varies
            params.mRight = params.mLeft + child.getMeasuredWidth();
        } else if (params.mLeft == VALUE_NOT_SET) {
            // Both left and right vary
            if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.CENTER_HORIZONTAL) {
                if (!wrapContent) {
                    centerHorizontal(child, params, myWidth);
                } else {
                    positionAtEdge(child, params, myWidth);
                }
                return true;
            } else {
                // This is the default case. For RTL we start from the right and for LTR we start
                // from the left. This will give LEFT/TOP for LTR and RIGHT/TOP for RTL.
                positionAtEdge(child, params, myWidth);
            }
        }
        return (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.RIGHT;
    }

    private void positionAtEdge(@Nonnull View child, @Nonnull LayoutParams params, int myWidth) {
        params.mLeft = params.leftMargin;
        params.mRight = params.mLeft + child.getMeasuredWidth();
    }

    private boolean positionChildVertical(View child, @Nonnull LayoutParams params, int myHeight,
                                          boolean wrapContent) {

        int gravity = params.gravity;

        if (params.mTop == VALUE_NOT_SET && params.mBottom != VALUE_NOT_SET) {
            // Bottom is fixed, but top varies
            params.mTop = params.mBottom - child.getMeasuredHeight();
        } else if (params.mTop != VALUE_NOT_SET && params.mBottom == VALUE_NOT_SET) {
            // Top is fixed, but bottom varies
            params.mBottom = params.mTop + child.getMeasuredHeight();
        } else if (params.mTop == VALUE_NOT_SET) {
            // Both top and bottom vary
            if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.CENTER_VERTICAL) {
                if (!wrapContent) {
                    centerVertical(child, params, myHeight);
                } else {
                    params.mTop = params.topMargin;
                    params.mBottom = params.mTop + child.getMeasuredHeight();
                }
                return true;
            } else {
                params.mTop = params.topMargin;
                params.mBottom = params.mTop + child.getMeasuredHeight();
            }
        }
        return (gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM;
    }

    private void applyHorizontalSizeRules(@Nonnull LayoutParams childParams, int myWidth, int[] rules) {
        LayoutParams anchorParams;

        // VALUE_NOT_SET indicates a "soft requirement" in that direction. For example:
        // left=10, right=VALUE_NOT_SET means the view must start at 10, but can go as far as it
        // wants to the right
        // left=VALUE_NOT_SET, right=10 means the view must end at 10, but can go as far as it
        // wants to the left
        // left=10, right=20 means the left and right ends are both fixed
        childParams.mLeft = VALUE_NOT_SET;
        childParams.mRight = VALUE_NOT_SET;

        anchorParams = getRelatedViewParams(rules, LEFT_OF);
        if (anchorParams != null) {
            childParams.mRight = anchorParams.mLeft - (anchorParams.leftMargin +
                    childParams.rightMargin);
        } else if (rules[LEFT_OF] != 0) {
            if (myWidth >= 0) {
                childParams.mRight = myWidth - childParams.rightMargin;
            }
        }

        anchorParams = getRelatedViewParams(rules, RIGHT_OF);
        if (anchorParams != null) {
            childParams.mLeft = anchorParams.mRight + (anchorParams.rightMargin +
                    childParams.leftMargin);
        } else if (rules[RIGHT_OF] != 0) {
            childParams.mLeft = childParams.leftMargin;
        }

        anchorParams = getRelatedViewParams(rules, ALIGN_LEFT);
        if (anchorParams != null) {
            childParams.mLeft = anchorParams.mLeft + childParams.leftMargin;
        } else if (rules[ALIGN_LEFT] != 0) {
            childParams.mLeft = childParams.leftMargin;
        }

        anchorParams = getRelatedViewParams(rules, ALIGN_RIGHT);
        if (anchorParams != null) {
            childParams.mRight = anchorParams.mRight - childParams.rightMargin;
        } else if (rules[ALIGN_RIGHT] != 0) {
            if (myWidth >= 0) {
                childParams.mRight = myWidth - childParams.rightMargin;
            }
        }

        if ((childParams.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.LEFT) {
            childParams.mLeft = childParams.leftMargin;
        }

        if ((childParams.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.RIGHT) {
            if (myWidth >= 0) {
                childParams.mRight = myWidth - childParams.rightMargin;
            }
        }
    }

    private void applyVerticalSizeRules(@Nonnull LayoutParams childParams, int myHeight, int[] rules) {
        LayoutParams anchorParams;

        childParams.mTop = VALUE_NOT_SET;
        childParams.mBottom = VALUE_NOT_SET;

        anchorParams = getRelatedViewParams(rules, ABOVE);
        if (anchorParams != null) {
            childParams.mBottom = anchorParams.mTop - (anchorParams.topMargin +
                    childParams.bottomMargin);
        } else if (rules[ABOVE] != 0) {
            if (myHeight >= 0) {
                childParams.mBottom = myHeight - childParams.bottomMargin;
            }
        }

        anchorParams = getRelatedViewParams(rules, BELOW);
        if (anchorParams != null) {
            childParams.mTop = anchorParams.mBottom + (anchorParams.bottomMargin +
                    childParams.topMargin);
        } else if (rules[BELOW] != 0) {
            childParams.mTop = childParams.topMargin;
        }

        anchorParams = getRelatedViewParams(rules, ALIGN_TOP);
        if (anchorParams != null) {
            childParams.mTop = anchorParams.mTop + childParams.topMargin;
        } else if (rules[ALIGN_TOP] != 0) {
            childParams.mTop = childParams.topMargin;
        }

        anchorParams = getRelatedViewParams(rules, ALIGN_BOTTOM);
        if (anchorParams != null) {
            childParams.mBottom = anchorParams.mBottom - childParams.bottomMargin;
        } else if (rules[ALIGN_BOTTOM] != 0) {
            if (myHeight >= 0) {
                childParams.mBottom = myHeight - childParams.bottomMargin;
            }
        }

        if ((childParams.gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.TOP) {
            childParams.mTop = childParams.topMargin;
        }

        if ((childParams.gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
            if (myHeight >= 0) {
                childParams.mBottom = myHeight - childParams.bottomMargin;
            }
        }
    }

    @Nullable
    private LayoutParams getRelatedViewParams(int[] rules, int relation) {
        View v = getRelatedView(rules, relation);
        if (v != null) {
            ViewGroup.LayoutParams params = v.getLayoutParams();
            if (params instanceof LayoutParams) {
                return (LayoutParams) v.getLayoutParams();
            }
        }
        return null;
    }

    @Nullable
    private View getRelatedView(@Nonnull int[] rules, int relation) {
        int anchor = rules[relation];
        if (anchor != 0) {
            DependencyGraph.Node node = mGraph.keyNodes.get(anchor);
            if (node == null) {
                return null;
            }
            View v = node.view;

            // Find the first non-GONE view up the chain
            while (v.getVisibility() == View.GONE) {
                rules = ((LayoutParams) v.getLayoutParams()).rules;
                node = mGraph.keyNodes.get((rules[relation]));
                // ignore self dependency. for more info look in git commit: da3003
                if (node == null || v == node.view) {
                    return null;
                }
                v = node.view;
            }

            return v;
        }

        return null;
    }

    private void centerHorizontal(@Nonnull View child, @Nonnull LayoutParams params, int myWidth) {
        int childWidth = child.getMeasuredWidth();
        int left = (myWidth - childWidth) / 2;

        params.mLeft = left;
        params.mRight = left + childWidth;
    }

    private void centerVertical(@Nonnull View child, @Nonnull LayoutParams params, int myHeight) {
        int childHeight = child.getMeasuredHeight();
        int top = (myHeight - childHeight) / 2;

        params.mTop = top;
        params.mBottom = top + childHeight;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                child.layout(lp.mLeft, lp.mTop, lp.mRight, lp.mBottom);
            }
        }
    }

    /**
     * Defines which view is ignored when the gravity is applied. This setting has no
     * effect if the gravity is TOP_LEFT
     *
     * @param viewId the ID of the view to be ignored by gravity, or 0 if no view
     *               should be ignored.
     * @see #setGravity(int)
     */
    public void setIgnoreGravity(int viewId) {
        ignoreGravity = viewId;
    }

    /**
     * Get the id of the View to be ignored by gravity
     */
    public int getIgnoreGravity() {
        return ignoreGravity;
    }

    /**
     * Describes how the child views are positioned. Defaults to TOP_LEFT
     *
     * <p>Note that since RelativeLayout considers the positioning of each child
     * relative to one another to be significant, setting gravity will affect
     * the positioning of all children as a single unit within the parent.
     * This happens after children have been relatively positioned.</p>
     *
     * @param gravity See {@link Gravity}
     * @see #setHorizontalGravity(int)
     * @see #setVerticalGravity(int)
     */
    public void setGravity(int gravity) {
        if (this.gravity != gravity) {
            if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == 0) {
                gravity |= Gravity.LEFT;
            }

            if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
                gravity |= Gravity.TOP;
            }

            this.gravity = gravity;
            requestLayout();
        }
    }

    public void setHorizontalGravity(int horizontalGravity) {
        horizontalGravity = horizontalGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) != horizontalGravity) {
            gravity = (gravity & ~Gravity.HORIZONTAL_GRAVITY_MASK) | horizontalGravity;
            requestLayout();
        }
    }

    public void setVerticalGravity(int verticalGravity) {
        verticalGravity = verticalGravity & Gravity.VERTICAL_GRAVITY_MASK;
        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) != verticalGravity) {
            gravity = (gravity & ~Gravity.VERTICAL_GRAVITY_MASK) | verticalGravity;
            requestLayout();
        }
    }

    /**
     * Describes how the child views are positioned.
     *
     * @return the gravity.
     * @see #setGravity(int)
     */
    public int getGravity() {
        return gravity;
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        dirtyHierarchy = true;
    }

    @Nonnull
    @Override
    protected ViewGroup.LayoutParams convertLayoutParams(@Nonnull ViewGroup.LayoutParams params) {
        if (params instanceof LayoutParams) {
            return params;
        } else if (params instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) params);
        }
        return new LayoutParams(params);
    }

    @Nonnull
    @Override
    protected ViewGroup.LayoutParams createDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(@Nullable ViewGroup.LayoutParams params) {
        return params instanceof LayoutParams;
    }

    /**
     * Specifies how a view is positioned within a {@link RelativeLayout}.
     * The relative layout containing the view uses the value of these layout parameters to
     * determine where to position the view on the screen.
     */
    public static class LayoutParams extends MarginLayoutParams {

        /**
         * The anchor view id to which this view should relative.
         * If the anchor view is not found,
         * it will use the parent as the anchor.
         */
        private final int[] rules = new int[VERB_COUNT];

        /**
         * The align or position relation to the anchor or parent.
         * Default is NO_GRAVITY. FILL doesn't support here
         */
        public int gravity = 0;

        // cached values for layout
        private int mLeft;
        private int mTop;
        private int mRight;
        private int mBottom;

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(@Nonnull MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(@Nonnull ViewGroup.LayoutParams source) {
            super(source);
        }

        /**
         * Adds a layout rule to be interpreted by the RelativeLayout.
         *
         * @param verb   a layout verb, such as {@link #ABOVE} or {@link #ALIGN_RIGHT}
         * @param anchor the ID of another view to use as an anchor
         */
        public void setRule(int verb, int anchor) {
            rules[verb] = anchor;
        }

        /**
         * Returns the layout rule associated with a specific verb.
         *
         * @param verb a layout verb, such as {@link #ABOVE} or {@link #ALIGN_RIGHT}
         * @return the id of another view to use as an anchor
         * @see #setRule(int, int)
         */
        public int getRule(int verb) {
            return rules[verb];
        }

        /**
         * Copy constructor. Clones the width, height, margin values, gravity
         * and rules of the source.
         *
         * @param source the layout params to copy from.
         */
        public LayoutParams(@Nonnull LayoutParams source) {
            super(source);
            System.arraycopy(source.rules, LEFT_OF, rules, LEFT_OF, VERB_COUNT);
            gravity = source.gravity;
        }

        @Nonnull
        @Override
        public LayoutParams copy() {
            return new LayoutParams(this);
        }
    }

    private static class DependencyGraph {

        /**
         * List of all views in the graph.
         */
        private final List<Node> nodes = new ObjectArrayList<>();

        /**
         * List of nodes in the graph. Each node is identified by its
         * view id (see View#getId()).
         */
        private final Int2ObjectMap<Node> keyNodes = new Int2ObjectArrayMap<>();

        /**
         * Temporary data structure used to build the list of roots
         * for this graph.
         */
        private final Deque<Node> roots = new ArrayDeque<>();

        /**
         * Clears the graph.
         */
        void clear() {
            nodes.clear();
            keyNodes.clear();
            roots.clear();
        }

        /**
         * Adds a view to the graph.
         *
         * @param view The view to be added as a node to the graph.
         */
        void add(@Nonnull View view) {
            int id = view.getId();
            Node node = new Node();
            node.view = view;

            if (id != View.NO_ID) {
                keyNodes.put(id, node);
            }

            nodes.add(node);
        }

        /**
         * Builds a sorted list of views. The sorting order depends on the dependencies
         * between the view. For instance, if view C needs view A to be processed first
         * and view A needs view B to be processed first, the dependency graph
         * is: B -> A -> C. The sorted array will contain views B, A and C in this order.
         *
         * @param sorted The sorted list of views. The length of this array must
         *               be equal to getChildCount().
         * @param rules  The list of rules to take into account.
         */
        void getSortedViews(View[] sorted, int... rules) {
            Deque<Node> roots = findRoots(rules);
            int index = 0;

            Node node;
            View view;
            int viewId;
            Map<Integer, Node> dependencies;
            while ((node = roots.pollLast()) != null) {
                view = node.view;
                viewId = view.getId();

                sorted[index++] = view;

                for (Node dependent : node.dependents) {
                    dependencies = dependent.dependencies;

                    dependencies.remove(viewId);
                    if (dependencies.isEmpty()) {
                        roots.add(dependent);
                    }
                }
            }

            if (index < sorted.length) {
                throw new IllegalStateException("Circular dependencies cannot exist"
                        + " in RelativeLayout");
            }
        }

        /**
         * Finds the roots of the graph. A root is a node with no dependency and
         * with [0..n] dependents.
         *
         * @param rulesFilter The list of rules to consider when building the
         *                    dependencies
         * @return A list of node, each being a root of the graph
         */
        @Nonnull
        private Deque<Node> findRoots(int[] rulesFilter) {

            // Find roots can be invoked several times, so make sure to clear
            // all dependents and dependencies before running the algorithm
            for (Node node : nodes) {
                node.dependents.clear();
                node.dependencies.clear();
            }

            LayoutParams layoutParams;
            Node dependency;
            int anchorViewId;
            // Builds up the dependents and dependencies for each node of the graph
            for (Node node : nodes) {
                layoutParams = (LayoutParams) node.view.getLayoutParams();

                // Look only the the rules passed in parameter, this way we build only the
                // dependencies for a specific set of rules
                for (int verb : rulesFilter) {
                    anchorViewId = layoutParams.getRule(verb);
                    if (anchorViewId > 0) {
                        // The node this node depends on
                        dependency = keyNodes.get(anchorViewId);
                        // Skip unknowns and self dependencies
                        if (dependency == null || dependency == node) {
                            continue;
                        }
                        // Add the current node as a dependent
                        dependency.dependents.add(node);
                        // Add a dependency to the current node
                        node.dependencies.put(anchorViewId, dependency);
                    }
                }
            }

            roots.clear();

            // Finds all the roots in the graph: all nodes with no dependencies
            for (Node node : nodes) {
                if (node.dependencies.isEmpty()) {
                    roots.addLast(node);
                }
            }

            return roots;
        }

        /**
         * A node in the dependency graph. A node is a view, its list of dependencies
         * and its list of dependents.
         * <p>
         * A node with no dependent is considered a root of the graph.
         */
        static class Node {

            /**
             * The view representing this node in the layout.
             */
            View view;

            /**
             * The list of dependents for this node; a dependent is a node
             * that needs this node to be processed first.
             */
            final List<Node> dependents = new ObjectArrayList<>();

            /**
             * The list of dependencies for this node.
             */
            final Int2ObjectMap<Node> dependencies = new Int2ObjectArrayMap<>();
        }
    }
}
