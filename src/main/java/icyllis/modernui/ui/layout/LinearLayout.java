/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.layout;

import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.ui.master.View;
import icyllis.modernui.ui.master.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    private Orientation orientation;

    private int gravity = FrameLayout.DEFAULT_GRAVITY;

    @Nullable
    private Drawable divider;

    private int showDividers;
    private int dividerWidth;
    private int dividerHeight;
    private int dividerPadding;

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
     *                     or {@link #SHOW_DIVIDER_END}
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
    public void setDividerDrawable(@Nullable Drawable divider) {
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
    public Drawable getDividerDrawable() {
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
     * @see #setDividerDrawable(Drawable)
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
     * @see #setDividerDrawable(Drawable)
     * @see #setDividerPadding(int)
     */
    public int getDividerPadding() {
        return dividerPadding;
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas, float time) {
        if (divider != null) {
            if (orientation.isVertical()) {
                drawDividersVertical(canvas);
            } else {
                drawDividersHorizontal(canvas);
            }
        }
    }

    void drawDividersVertical(@Nonnull Canvas canvas) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                if (hasDividerBeforeChildAt(i)) {
                    /*final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    final int top = child.getTop() - lp.topMargin - mDividerHeight;
                    drawHorizontalDivider(canvas, top);*/
                }
            }
        }
    }

    void drawDividersHorizontal(@Nonnull Canvas canvas) {

    }

    /**
     * Determines where to position dividers between children.
     *
     * @param childIndex Index of child to check for preceding divider
     * @return true if there should be a divider before the child at childIndex
     */
    protected boolean hasDividerBeforeChildAt(int childIndex) {
        if (childIndex == getChildCount()) {
            return (showDividers & SHOW_DIVIDER_END) != 0;
        }
        boolean allViewsAreGoneBefore = allViewsAreGoneBefore(childIndex);
        if (allViewsAreGoneBefore) {
            return (showDividers & SHOW_DIVIDER_BEGINNING) != 0;
        } else {
            return (showDividers & SHOW_DIVIDER_MIDDLE) != 0;
        }
    }

    /**
     * Checks whether all child views before the given index are gone.
     */
    private boolean allViewsAreGoneBefore(int childIndex) {
        for (int i = childIndex - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onLayout(boolean changed) {

    }

    public enum Orientation {
        HORIZONTAL,
        VERTICAL;

        public boolean isVertical() {
            return this == VERTICAL;
        }
    }
}
