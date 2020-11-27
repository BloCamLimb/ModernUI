/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.ui.layout;

import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @since 2.0
 */
@SuppressWarnings("unused")
public class LinearLayout extends ViewGroup {

    /**
     * Don't show any dividers.
     */
    public static final int SHOW_DIVIDER_NONE      = 0;
    /**
     * Show a divider at the beginning of the group.
     */
    public static final int SHOW_DIVIDER_BEGINNING = 1;
    /**
     * Show dividers between each item in the group.
     */
    public static final int SHOW_DIVIDER_MIDDLE    = 1 << 1;
    /**
     * Show a divider at the end of the group.
     */
    public static final int SHOW_DIVIDER_END       = 1 << 2;


    /**
     * @see #setOrientation(Orientation)
     */
    private Orientation orientation;

    /**
     * @see #setGravity(int)
     */
    private int gravity = Gravity.TOP_LEFT;

    /**
     * @see #setWeightSum(float)
     */
    private float weightSum;

    // inner
    private int totalLength;

    // inner
    private int[] maxAscent;
    private int[] maxDescent;

    // inner
    private static final int INDEX_CENTER_VERTICAL = 0;
    private static final int INDEX_TOP             = 1;
    private static final int INDEX_BOTTOM          = 2;
    private static final int INDEX_FILL            = 3;

    /**
     * {@link #setDivider(Drawable)}
     */
    private Drawable divider;

    /**
     * {@link #setShowDividers(int)}
     * {@link #setDividerPadding(int)}
     */
    private int showDividers;
    private int dividerWidth;
    private int dividerHeight;
    private int dividerPadding;

    public LinearLayout() {

    }

    /**
     * @return {@code true} if this layout is currently configured to show at least one
     * divider.
     */
    public final boolean isShowingDividers() {
        return showDividers != 0 && divider != null;
    }

    /**
     * Set how dividers should be shown between items in this layout
     *
     * @param showDividers show dividers a combination of
     *                     {@link #SHOW_DIVIDER_BEGINNING} or {@link #SHOW_DIVIDER_MIDDLE}
     *                     or {@link #SHOW_DIVIDER_END}, or {@link #SHOW_DIVIDER_NONE} to show no dividers.
     */
    public void setShowDividers(int showDividers) {
        if (this.showDividers == showDividers) {
            return;
        }
        this.showDividers = showDividers;
        requestLayout();
    }

    public int getShowDividers() {
        return showDividers;
    }

    /**
     * Set a drawable to be used as a divider between items.
     *
     * @param divider drawable that will divide each item.
     * @see #setShowDividers(int)
     */
    public void setDivider(@Nullable Drawable divider) {
        if (this.divider == divider) {
            return;
        }
        this.divider = divider;
        if (divider != null) {
            dividerWidth = divider.getIntrinsicWidth();
            dividerHeight = divider.getIntrinsicHeight();
        } else {
            dividerWidth = 0;
            dividerHeight = 0;
        }
        requestLayout();
    }

    @Nullable
    public Drawable getDivider() {
        return divider;
    }

    public int getDividerWidth() {
        return dividerWidth;
    }

    public int getDividerHeight() {
        return dividerHeight;
    }

    /**
     * Set padding displayed on both ends of dividers. For a vertical layout, the padding is applied
     * to left and right end of dividers. For a horizontal layout, the padding is applied to top and
     * bottom end of dividers.
     *
     * @param padding Padding value in pixels that will be applied to each end
     * @see #setShowDividers(int)
     * @see #setDivider(Drawable)
     * @see #getDividerPadding()
     */
    public void setDividerPadding(int padding) {
        if (padding == dividerPadding) {
            return;
        }
        dividerPadding = padding;

        if (isShowingDividers()) {
            requestLayout();
        }
    }

    /**
     * Get the padding size used to inset dividers in pixels
     *
     * @see #setShowDividers(int)
     * @see #setDivider(Drawable)
     * @see #setDividerPadding(int)
     */
    public int getDividerPadding() {
        return dividerPadding;
    }

    /**
     * Defines the desired weights sum. If unspecified the weights sum is computed
     * at layout time by adding the layout_weight of each child.
     * <p>
     * This can be used for instance to give a single child 50% of the total
     * available space by giving it a layout_weight of 0.5 and setting the
     * weightSum to 1.0.
     *
     * @param weightSum a number greater than 0.0f, or a number lower than or equals
     *                  to 0.0f if the weight sum should be computed from the children's weight
     */
    public void setWeightSum(float weightSum) {
        this.weightSum = Math.max(0.0f, weightSum);
    }

    /**
     * Returns the desired weights sum.
     *
     * @return A number greater than 0.0f if the weight sum is defined, or
     * a number lower than or equals to 0.0f if not weight sum is to be used.
     */
    public float getWeightSum() {
        return weightSum;
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas) {
        if (divider != null) {
            if (orientation == Orientation.VERTICAL) {
                drawDividersVertical(canvas);
            } else {
                drawDividersHorizontal(canvas);
            }
        }
    }

    private void drawDividersVertical(@Nonnull Canvas canvas) {
        // draw the divider before first non-GONE child
        // faster than Android API, because we draw every frame
        boolean began = false;
        View lastDraw = null;

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int top = child.getTop() - lp.topMargin - dividerHeight;
                if (!began) {
                    if ((showDividers & SHOW_DIVIDER_BEGINNING) != 0) {
                        drawHorizontalDivider(canvas, top);
                    }
                    began = true;
                } else {
                    if ((showDividers & SHOW_DIVIDER_MIDDLE) != 0) {
                        drawHorizontalDivider(canvas, top);
                    }
                }
                lastDraw = child;
            }
        }

        // draw last one
        if ((showDividers & SHOW_DIVIDER_END) != 0) {
            int bottom;
            if (lastDraw != null) {
                LayoutParams lp = (LayoutParams) lastDraw.getLayoutParams();
                bottom = lastDraw.getBottom() + lp.bottomMargin;
            } else {
                bottom = getBottom() - dividerHeight;
            }
            drawHorizontalDivider(canvas, bottom);
        }
    }

    private void drawHorizontalDivider(@Nonnull Canvas canvas, int top) {
        divider.setBounds(getLeft() + dividerPadding, top, getRight() - dividerPadding, top + dividerHeight);
        divider.draw(canvas);
    }

    private void drawDividersHorizontal(@Nonnull Canvas canvas) {
        boolean began = false;
        View lastDraw = null;

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int left = child.getLeft() - lp.leftMargin - dividerWidth;
                if (!began) {
                    if ((showDividers & SHOW_DIVIDER_BEGINNING) != 0) {
                        drawVerticalDivider(canvas, left);
                    }
                    began = true;
                } else {
                    if ((showDividers & SHOW_DIVIDER_MIDDLE) != 0) {
                        drawVerticalDivider(canvas, left);
                    }
                }
                lastDraw = child;
            }
        }

        if ((showDividers & SHOW_DIVIDER_END) != 0) {
            int right;
            if (lastDraw != null) {
                LayoutParams lp = (LayoutParams) lastDraw.getLayoutParams();
                right = lastDraw.getRight() + lp.rightMargin;
            } else {
                right = getRight() - dividerWidth;
            }
            drawVerticalDivider(canvas, right);
        }
    }

    private void drawVerticalDivider(@Nonnull Canvas canvas, int left) {
        divider.setBounds(left, getTop() + dividerPadding, left + dividerWidth, getBottom() - dividerPadding);
        divider.draw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (orientation == Orientation.VERTICAL) {
            measureVertical(widthMeasureSpec, heightMeasureSpec);
        } else {
            measureHorizontal(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
        totalLength = 0;

        int maxWidth = 0;
        int alternativeMaxWidth = 0;
        int weightedMaxWidth = 0;

        boolean allFillParent = true;
        float totalWeight = 0;

        boolean began = false;

        final int count = getChildCount();

        final MeasureSpec.Mode widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final MeasureSpec.Mode heightMode = MeasureSpec.getMode(heightMeasureSpec);

        boolean matchWidth = false;
        boolean skippedMeasure = false;

        int consumedExcessSpace = 0;

        int nonSkippedChildCount = 0;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            ++nonSkippedChildCount;

            if (!began) {
                if ((showDividers & SHOW_DIVIDER_BEGINNING) != 0) {
                    totalLength += dividerHeight;
                }
                began = true;
            } else {
                if ((showDividers & SHOW_DIVIDER_MIDDLE) != 0) {
                    totalLength += dividerHeight;
                }
            }

            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            totalWeight += lp.weight;

            boolean useExcessSpace = lp.height == 0 && lp.weight > 0;
            if (heightMode.isExactly() && useExcessSpace) {
                // Optimization: don't bother measuring children who are only
                // laid out using excess space. These views will get measured
                // later if we have space to distribute.
                totalLength = Math.max(totalLength, totalLength + lp.topMargin + lp.bottomMargin);
                skippedMeasure = true;
            } else {
                if (useExcessSpace) {
                    // The heightMode is either UNSPECIFIED or AT_MOST, and
                    // this child is only laid out using excess space. Measure
                    // using WRAP_CONTENT so that we can find out the view's
                    // optimal height. We'll restore the original height of 0
                    // after measurement.
                    lp.height = LayoutParams.WRAP_CONTENT;
                }

                // Determine how big this child would like to be. If this or
                // previous children have given a weight, then we allow it to
                // use all available space (and we will shrink things later
                // if needed).
                int usedHeight = totalWeight == 0 ? totalLength : 0;
                measureChildWithMargins(child, widthMeasureSpec, 0,
                        heightMeasureSpec, usedHeight);

                int childHeight = child.getMeasuredHeight();
                if (useExcessSpace) {
                    // Restore the original height and record how much space
                    // we've allocated to excess-only children so that we can
                    // match the behavior of EXACTLY measurement.
                    lp.height = 0;
                    consumedExcessSpace += childHeight;
                }

                totalLength = Math.max(totalLength, totalLength + childHeight + lp.topMargin +
                        lp.bottomMargin);
            }

            boolean matchWidthLocally = false;
            if (widthMode.isVariable() && lp.width == LayoutParams.MATCH_PARENT) {
                // The width of the linear layout will scale, and at least one
                // child said it wanted to match our width. Set a flag
                // indicating that we need to remeasure at least that view when
                // we know our width.
                matchWidth = true;
                matchWidthLocally = true;
            }

            int margin = lp.leftMargin + lp.rightMargin;
            int measuredWidth = child.getMeasuredWidth() + margin;
            maxWidth = Math.max(maxWidth, measuredWidth);

            allFillParent = allFillParent && lp.width == LayoutParams.MATCH_PARENT;
            if (lp.weight > 0) {
                // Widths of weighted Views are bogus if we end up
                // remeasuring, so keep them separate.
                weightedMaxWidth = Math.max(weightedMaxWidth,
                        matchWidthLocally ? margin : measuredWidth);
            } else {
                alternativeMaxWidth = Math.max(alternativeMaxWidth,
                        matchWidthLocally ? margin : measuredWidth);
            }
        }

        // add last, if the beginning added and to show end
        if (nonSkippedChildCount > 0 && (showDividers & SHOW_DIVIDER_END) != 0) {
            totalLength += dividerHeight;
        }

        int heightSize = totalLength;

        // Check against our minimum height
        heightSize = Math.max(heightSize, getMinHeight());

        // Reconcile our calculated size with the heightMeasureSpec
        heightSize = resolveSize(heightSize, heightMeasureSpec);
        // Either expand children with weight to take up available space or
        // shrink them if they extend beyond our current bounds. If we skipped
        // measurement on any children, we need to measure them now.
        int remainingExcess = heightSize - totalLength + consumedExcessSpace;

        if (skippedMeasure || (totalWeight > 0.0f)) {
            float remainingWeightSum = weightSum > 0.0f ? weightSum : totalWeight;

            totalLength = 0;

            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) {
                    continue;
                }

                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                float childWeight = lp.weight;
                if (childWeight > 0) {
                    int share = (int) (childWeight * remainingExcess / remainingWeightSum);
                    remainingExcess -= share;
                    remainingWeightSum -= childWeight;

                    int childHeight;
                    if (lp.height == 0) {
                        // This child needs to be laid out from scratch using
                        // only its share of excess space.
                        childHeight = share;
                    } else {
                        // This child had some intrinsic height to which we
                        // need to add its share of excess space.
                        childHeight = child.getMeasuredHeight() + share;
                    }

                    int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                            Math.max(0, childHeight), MeasureSpec.Mode.EXACTLY);
                    int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                            lp.leftMargin + lp.rightMargin, lp.width);
                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                }

                int margin = lp.leftMargin + lp.rightMargin;
                int measuredWidth = child.getMeasuredWidth() + margin;
                maxWidth = Math.max(maxWidth, measuredWidth);

                boolean matchWidthLocally = widthMode.isVariable() &&
                        lp.width == LayoutParams.MATCH_PARENT;

                alternativeMaxWidth = Math.max(alternativeMaxWidth,
                        matchWidthLocally ? margin : measuredWidth);

                allFillParent = allFillParent && lp.width == LayoutParams.MATCH_PARENT;

                totalLength = Math.max(totalLength, totalLength + child.getMeasuredHeight() +
                        lp.topMargin + lp.bottomMargin);
            }
        } else {
            alternativeMaxWidth = Math.max(alternativeMaxWidth, weightedMaxWidth);
        }

        if (!allFillParent && widthMode.isVariable()) {
            maxWidth = alternativeMaxWidth;
        }

        maxWidth = Math.max(maxWidth, getMinWidth());

        setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec), heightSize);

        if (matchWidth) {
            // Pretend that the linear layout has an exact size.
            int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(),
                    MeasureSpec.Mode.EXACTLY);
            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() != GONE) {
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();

                    if (lp.width == LayoutParams.MATCH_PARENT) {
                        // temporarily force children to reuse their old measured height
                        int lpHeight = lp.height;
                        lp.height = child.getMeasuredHeight();

                        // remeasure with new dimensions
                        measureChildWithMargins(child, uniformMeasureSpec, 0,
                                heightMeasureSpec, 0);
                        lp.height = lpHeight;
                    }
                }
            }
        }
    }

    private void measureHorizontal(int widthMeasureSpec, int heightMeasureSpec) {
        totalLength = 0;

        int maxHeight = 0;
        int alternativeMaxHeight = 0;
        int weightedMaxHeight = 0;

        boolean allFillParent = true;
        float totalWeight = 0;

        boolean began = false;

        final int count = getChildCount();

        final MeasureSpec.Mode widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final MeasureSpec.Mode heightMode = MeasureSpec.getMode(heightMeasureSpec);

        boolean matchHeight = false;
        boolean skippedMeasure = false;

        if (maxAscent == null || maxDescent == null) {
            maxAscent = new int[4];
            maxDescent = new int[4];
        }

        int[] maxAscent = this.maxAscent;
        int[] maxDescent = this.maxDescent;

        maxAscent[0] = maxAscent[1] = maxAscent[2] = maxAscent[3] = -1;
        maxDescent[0] = maxDescent[1] = maxDescent[2] = maxDescent[3] = -1;

        int consumedExcessSpace = 0;

        int nonSkippedChildCount = 0;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            ++nonSkippedChildCount;

            if (!began) {
                if ((showDividers & SHOW_DIVIDER_BEGINNING) != 0) {
                    totalLength += dividerWidth;
                }
                began = true;
            } else {
                if ((showDividers & SHOW_DIVIDER_MIDDLE) != 0) {
                    totalLength += dividerWidth;
                }
            }

            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            totalWeight += lp.weight;

            boolean useExcessSpace = lp.width == 0 && lp.weight > 0;
            if (widthMode.isExactly() && useExcessSpace) {
                // Optimization: don't bother measuring children who are only
                // laid out using excess space. These views will get measured
                // later if we have space to distribute.
                totalLength = Math.max(totalLength, totalLength + lp.leftMargin + lp.rightMargin);
                skippedMeasure = true;
            } else {
                if (useExcessSpace) {
                    // The widthMode is either UNSPECIFIED or AT_MOST, and
                    // this child is only laid out using excess space. Measure
                    // using WRAP_CONTENT so that we can find out the view's
                    // optimal width. We'll restore the original width of 0
                    // after measurement.
                    lp.width = LayoutParams.WRAP_CONTENT;
                }

                // Determine how big this child would like to be. If this or
                // previous children have given a weight, then we allow it to
                // use all available space (and we will shrink things later
                // if needed).
                int usedWidth = totalWeight == 0 ? totalLength : 0;
                measureChildWithMargins(child, widthMeasureSpec, usedWidth,
                        heightMeasureSpec, 0);

                int childWidth = child.getMeasuredWidth();
                if (useExcessSpace) {
                    // Restore the original width and record how much space
                    // we've allocated to excess-only children so that we can
                    // match the behavior of EXACTLY measurement.
                    lp.width = 0;
                    consumedExcessSpace += childWidth;
                }

                if (widthMode.isExactly()) {
                    totalLength += childWidth + lp.leftMargin + lp.rightMargin;
                } else {
                    totalLength = Math.max(totalLength, totalLength + childWidth + lp.leftMargin
                            + lp.rightMargin);
                }
            }

            boolean matchHeightLocally = false;
            if (heightMode.isVariable() && lp.height == LayoutParams.MATCH_PARENT) {
                // The height of the linear layout will scale, and at least one
                // child said it wanted to match our height. Set a flag indicating that
                // we need to remeasure at least that view when we know our height.
                matchHeight = true;
                matchHeightLocally = true;
            }

            int margin = lp.topMargin + lp.bottomMargin;
            int childHeight = child.getMeasuredHeight() + margin;

            maxHeight = Math.max(maxHeight, childHeight);

            allFillParent = allFillParent && lp.height == LayoutParams.MATCH_PARENT;
            if (lp.weight > 0) {
                // Heights of weighted Views are bogus if we end up
                // remeasuring, so keep them separate.
                weightedMaxHeight = Math.max(weightedMaxHeight,
                        matchHeightLocally ? margin : childHeight);
            } else {
                alternativeMaxHeight = Math.max(alternativeMaxHeight,
                        matchHeightLocally ? margin : childHeight);
            }
        }

        // add last, if the beginning added and to show end
        if (nonSkippedChildCount > 0 && (showDividers & SHOW_DIVIDER_END) != 0) {
            totalLength += dividerWidth;
        }

        // Check mMaxAscent[INDEX_TOP] first because it maps to Gravity.TOP,
        // the most common case
        if (maxAscent[INDEX_TOP] != -1 ||
                maxAscent[INDEX_CENTER_VERTICAL] != -1 ||
                maxAscent[INDEX_BOTTOM] != -1 ||
                maxAscent[INDEX_FILL] != -1) {
            int ascent = Math.max(maxAscent[INDEX_FILL],
                    Math.max(maxAscent[INDEX_CENTER_VERTICAL],
                            Math.max(maxAscent[INDEX_TOP], maxAscent[INDEX_BOTTOM])));
            int descent = Math.max(maxDescent[INDEX_FILL],
                    Math.max(maxDescent[INDEX_CENTER_VERTICAL],
                            Math.max(maxDescent[INDEX_TOP], maxDescent[INDEX_BOTTOM])));
            maxHeight = Math.max(maxHeight, ascent + descent);
        }

        int widthSize = totalLength;

        // Check against our minimum width
        widthSize = Math.max(widthSize, getMinWidth());

        // Reconcile our calculated size with the widthMeasureSpec
        widthSize = resolveSize(widthSize, widthMeasureSpec);

        // Either expand children with weight to take up available space or
        // shrink them if they extend beyond our current bounds. If we skipped
        // measurement on any children, we need to measure them now.
        int remainingExcess = widthSize - totalLength + consumedExcessSpace;

        if (skippedMeasure || (totalWeight > 0.0f)) {
            float remainingWeightSum = weightSum > 0.0f ? weightSum : totalWeight;

            maxAscent[0] = maxAscent[1] = maxAscent[2] = maxAscent[3] = -1;
            maxDescent[0] = maxDescent[1] = maxDescent[2] = maxDescent[3] = -1;
            maxHeight = -1;

            totalLength = 0;

            for (int i = 0; i < count; ++i) {
                View child = getChildAt(i);
                if (child.getVisibility() == View.GONE) {
                    continue;
                }

                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                float childWeight = lp.weight;
                if (childWeight > 0) {
                    int share = (int) (childWeight * remainingExcess / remainingWeightSum);
                    remainingExcess -= share;
                    remainingWeightSum -= childWeight;

                    int childWidth;
                    if (lp.width == 0) {
                        // This child needs to be laid out from scratch using
                        // only its share of excess space.
                        childWidth = share;
                    } else {
                        // This child had some intrinsic width to which we
                        // need to add its share of excess space.
                        childWidth = child.getMeasuredWidth() + share;
                    }

                    int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                            Math.max(0, childWidth), MeasureSpec.Mode.EXACTLY);
                    int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                            lp.topMargin + lp.bottomMargin, lp.height);
                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                }

                if (widthMode.isExactly()) {
                    totalLength += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
                } else {
                    totalLength = Math.max(totalLength, totalLength + child.getMeasuredWidth() +
                            lp.leftMargin + lp.rightMargin);
                }

                boolean matchHeightLocally = heightMode.isVariable() &&
                        lp.height == LayoutParams.MATCH_PARENT;

                int margin = lp.topMargin + lp.bottomMargin;
                int childHeight = child.getMeasuredHeight() + margin;
                maxHeight = Math.max(maxHeight, childHeight);
                alternativeMaxHeight = Math.max(alternativeMaxHeight,
                        matchHeightLocally ? margin : childHeight);

                allFillParent = allFillParent && lp.height == LayoutParams.MATCH_PARENT;
            }

            // Check mMaxAscent[INDEX_TOP] first because it maps to Gravity.TOP,
            // the most common case
            if (maxAscent[INDEX_TOP] != -1 ||
                    maxAscent[INDEX_CENTER_VERTICAL] != -1 ||
                    maxAscent[INDEX_BOTTOM] != -1 ||
                    maxAscent[INDEX_FILL] != -1) {
                int ascent = Math.max(maxAscent[INDEX_FILL],
                        Math.max(maxAscent[INDEX_CENTER_VERTICAL],
                                Math.max(maxAscent[INDEX_TOP], maxAscent[INDEX_BOTTOM])));
                int descent = Math.max(maxDescent[INDEX_FILL],
                        Math.max(maxDescent[INDEX_CENTER_VERTICAL],
                                Math.max(maxDescent[INDEX_TOP], maxDescent[INDEX_BOTTOM])));
                maxHeight = Math.max(maxHeight, ascent + descent);
            }
        } else {
            alternativeMaxHeight = Math.max(alternativeMaxHeight, weightedMaxHeight);
        }

        if (!allFillParent && heightMode.isVariable()) {
            maxHeight = alternativeMaxHeight;
        }

        // Check against our minimum height
        maxHeight = Math.max(maxHeight, getMinHeight());

        setMeasuredDimension(widthSize, resolveSize(maxHeight, heightMeasureSpec));

        if (matchHeight) {
            // Pretend that the linear layout has an exact size. This is the measured height of
            // ourselves. The measured height should be the max height of the children, changed
            // to accommodate the heightMeasureSpec from the parent
            int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(),
                    MeasureSpec.Mode.EXACTLY);
            for (int i = 0; i < count; ++i) {
                View child = getChildAt(i);
                if (child.getVisibility() != GONE) {
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();

                    if (lp.height == LayoutParams.MATCH_PARENT) {
                        // temporarily force children to reuse their old measured width
                        int lpWidth = lp.width;
                        lp.width = child.getMeasuredWidth();

                        // remeasure with new dimensions
                        measureChildWithMargins(child, widthMeasureSpec, 0, uniformMeasureSpec, 0);
                        lp.width = lpWidth;
                    }
                }
            }
        }
    }

    /**
     * Determines where to position dividers between children.
     * Only used in measure or layout
     *
     * @param childIndex index of child to check for preceding divider
     * @return true if there should be a divider before the child at childIndex
     * @deprecated performance not good enough
     */
    @Deprecated
    private boolean hasDividerBeforeChildAt(int childIndex) {
        if (childIndex == getChildCount()) {
            // whether the end divider should draw
            return (showDividers & SHOW_DIVIDER_END) != 0;
        }
        boolean allViewsAreGoneBefore = allViewsAreGoneBefore(childIndex);
        if (allViewsAreGoneBefore) {
            // the first non-gone view, check if beginning divider is enabled
            return (showDividers & SHOW_DIVIDER_BEGINNING) != 0;
        } else {
            return (showDividers & SHOW_DIVIDER_MIDDLE) != 0;
        }
    }

    @Deprecated
    private boolean allViewsAreGoneBefore(int childIndex) {
        for (int i = childIndex - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child != null && child.getVisibility() != GONE) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onLayout(boolean changed) {
        if (orientation == Orientation.VERTICAL) {
            layoutVertical();
        } else {
            layoutHorizontal();
        }
    }

    private void layoutVertical() {
        int count = getChildCount();

        int parentLeft = 0;
        int parentRight = getWidth();

        int parentTop = 0;
        int parentBottom = getHeight();

        int parentWidth = getWidth();
        int parentHeight = getHeight();

        int parentHorizontalGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;

        int childTop;
        int childLeft;

        switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.BOTTOM:
                childTop = parentTop + parentHeight - totalLength;
                break;
            case Gravity.VERTICAL_CENTER:
                childTop = parentTop + (parentHeight - totalLength) / 2;
                break;
            default:
                childTop = parentTop;
                break;
        }

        boolean began = false;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();

                int width = child.getMeasuredWidth();
                int height = child.getMeasuredHeight();

                int gravity = lp.gravity;
                if (gravity < 0) {
                    gravity = parentHorizontalGravity;
                }

                int horizontalGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;

                switch (horizontalGravity) {
                    case Gravity.HORIZONTAL_CENTER:
                        childLeft = parentLeft + (parentWidth - width) / 2 +
                                lp.leftMargin - lp.rightMargin;
                        break;
                    case Gravity.RIGHT:
                        childLeft = parentRight + parentWidth - width - lp.rightMargin;
                        break;
                    default:
                        childLeft = parentLeft + lp.leftMargin;
                        break;
                }

                if (!began) {
                    if ((showDividers & SHOW_DIVIDER_BEGINNING) != 0) {
                        childTop += dividerHeight;
                    }
                    began = true;
                } else {
                    if ((showDividers & SHOW_DIVIDER_MIDDLE) != 0) {
                        childTop += dividerHeight;
                    }
                }

                childTop += lp.topMargin;

                child.layout(childLeft, childTop, childLeft + width, childTop + height);

                childTop += height + lp.bottomMargin;
            }
        }
    }

    private void layoutHorizontal() {
        int count = getChildCount();

        int parentLeft = getLeft();
        int parentRight = getRight();

        int parentTop = getTop();
        int parentBottom = getBottom();

        int parentWidth = getWidth();
        int parentHeight = getHeight();

        int parentVerticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

        int childTop;
        int childLeft;

        switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.RIGHT:
                childLeft = parentLeft + parentWidth - totalLength;
                break;
            case Gravity.HORIZONTAL_CENTER:
                childLeft = parentLeft + (parentWidth - totalLength) / 2;
                break;
            default:
                childLeft = parentLeft;
                break;
        }

        boolean began = false;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();

                int width = child.getMeasuredWidth();
                int height = child.getMeasuredHeight();

                int gravity = lp.gravity;
                if (gravity < 0) {
                    gravity = parentVerticalGravity;
                }

                int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

                switch (verticalGravity) {
                    case Gravity.VERTICAL_CENTER:
                        childTop = parentTop + (parentHeight - height) / 2 +
                                lp.topMargin - lp.bottomMargin;
                        break;
                    case Gravity.BOTTOM:
                        childTop = parentTop + parentHeight - height - lp.bottomMargin;
                        break;
                    default:
                        childTop = parentTop + lp.topMargin;
                        break;
                }

                if (!began) {
                    if ((showDividers & SHOW_DIVIDER_BEGINNING) != 0) {
                        childLeft += dividerWidth;
                    }
                    began = true;
                } else {
                    if ((showDividers & SHOW_DIVIDER_MIDDLE) != 0) {
                        childLeft += dividerWidth;
                    }
                }

                childLeft += lp.leftMargin;

                child.layout(childLeft, childTop, childLeft + width, childTop + height);

                childLeft += width + lp.rightMargin;
            }
        }
    }

    /**
     * Should the layout be a column or a row.
     *
     * @param orientation orientation to set, default is {@link Orientation#HORIZONTAL}
     * @see #getOrientation()
     */
    public void setOrientation(Orientation orientation) {
        if (this.orientation != orientation) {
            this.orientation = orientation;
            requestLayout();
        }
    }

    /**
     * Returns the current orientation.
     *
     * @return either {@link Orientation#HORIZONTAL} or {@link Orientation#VERTICAL}
     * @see #setOrientation(Orientation)
     */
    public Orientation getOrientation() {
        return orientation;
    }

    /**
     * Describes how the child views are positioned. Defaults to GRAVITY_TOP. If
     * this layout has a VERTICAL orientation, this controls where all the child
     * views are placed if there is extra vertical space. If this layout has a
     * HORIZONTAL orientation, this controls the alignment of the children.
     *
     * @param gravity See {@link Gravity}
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

    public int getGravity() {
        return gravity;
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
        if (orientation == Orientation.HORIZONTAL) {
            return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(@Nullable ViewGroup.LayoutParams params) {
        return params instanceof LayoutParams;
    }

    public static class LayoutParams extends MarginLayoutParams {

        /**
         * Indicates how much of the extra space in the LinearLayout will be
         * allocated to the view associated with these LayoutParams. Specify
         * 0 if the view should not be stretched. Otherwise the extra pixels
         * will be pro-rated among all views whose weight is greater than 0.
         */
        public float weight;

        /**
         * Gravity for the view associated with these LayoutParams.
         *
         * @see Gravity
         */
        public int gravity = -1;

        public LayoutParams(int width, int height) {
            super(width, height);
            weight = 0;
        }

        /**
         * Creates a new set of layout parameters with the specified width, height
         * and weight.
         *
         * @param width  either {@link #WRAP_CONTENT},
         *               {@link #MATCH_PARENT}, or a fixed value
         * @param height either {@link #WRAP_CONTENT},
         *               {@link #MATCH_PARENT}, or a fixed value
         * @param weight the weight
         */
        public LayoutParams(int width, int height, float weight) {
            super(width, height);
            this.weight = weight;
        }

        public LayoutParams(@Nonnull MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(@Nonnull ViewGroup.LayoutParams source) {
            super(source);
        }

        /**
         * Copy constructor. Clones the width, height, margin values, weight,
         * and gravity of the source.
         *
         * @param source The layout params to copy from.
         */
        public LayoutParams(LayoutParams source) {
            super(source);

            this.weight = source.weight;
            this.gravity = source.gravity;
        }

        @Nonnull
        @Override
        public LayoutParams copy() {
            return new LayoutParams(this);
        }
    }
}
