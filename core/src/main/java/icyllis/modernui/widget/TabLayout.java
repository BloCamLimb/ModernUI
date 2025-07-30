/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   Copyright (C) 2015 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package icyllis.modernui.widget;

import icyllis.modernui.R;
import icyllis.modernui.animation.AnimatorListener;
import icyllis.modernui.animation.MotionEasingUtils;
import icyllis.modernui.animation.PropertyValuesHolder;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.animation.ValueAnimator;
import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.annotation.Dimension;
import icyllis.modernui.annotation.FloatRange;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.BlendMode;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Color;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.RectF;
import icyllis.modernui.graphics.drawable.ColorDrawable;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.RippleDrawable;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.TextAppearance;
import icyllis.modernui.resources.TypedArray;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.text.Layout;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.util.AttributeSet;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.DataSetObserver;
import icyllis.modernui.util.Log;
import icyllis.modernui.util.Pools;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.MeasureSpec;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.SoundEffectConstants;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.view.ViewParent;
import org.intellij.lang.annotations.MagicConstant;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static icyllis.modernui.widget.ViewPager.*;

/**
 * TabLayout provides a horizontal layout to display tabs.
 *
 * <p>Population of the tabs to display is done through {@link Tab} instances. You create tabs via
 * {@link #newTab()}. From there you can change the tab's label or icon via {@link Tab#setText}
 * and {@link Tab#setIcon} respectively. To display the tab, you need to add it to the layout
 * via one of the {@link #addTab(Tab)} methods. For example:
 *
 * <pre>
 * TabLayout tabLayout = ...;
 * tabLayout.addTab(tabLayout.newTab().setText("Tab 1"));
 * tabLayout.addTab(tabLayout.newTab().setText("Tab 2"));
 * tabLayout.addTab(tabLayout.newTab().setText("Tab 3"));
 * </pre>
 *
 * You should add a listener via {@link #addOnTabSelectedListener(OnTabSelectedListener)} to be
 * notified when any tab's selection state has been changed.
 *
 * <h3>ViewPager integration</h3>
 *
 * <p>If you're using a {@link ViewPager} together with this layout, you
 * can call {@link #setupWithViewPager(ViewPager)} to link the two together. This layout will be
 * automatically populated from the {@link PagerAdapter}'s page titles.
 *
 * <p>This view also supports being used as part of a ViewPager's decor, and can be added directly
 * to the ViewPager.
 *
 * <p>For more information, see the <a
 * href="https://github.com/material-components/material-components-android/blob/master/docs/components/Tabs.md">component
 * developer guidance</a> and <a href="https://m3.material.io/components/tabs/overview">design
 * guidelines</a>.
 */
// Modified from MDC-Android
@SuppressWarnings("ForLoopReplaceableByForEach")
@ViewPager.DecorView
public class TabLayout extends HorizontalScrollView {

    @Dimension(unit = Dimension.DP)
    private static final int DEFAULT_HEIGHT_WITH_TEXT_ICON = 64;

    @Dimension(unit = Dimension.DP)
    static final int DEFAULT_GAP_TEXT_ICON = 8;

    @Dimension(unit = Dimension.DP)
    private static final int DEFAULT_HEIGHT = 48;

    @Dimension(unit = Dimension.DP)
    private static final int TAB_MIN_WIDTH_MARGIN = 56;

    @Dimension(unit = Dimension.DP)
    static final int FIXED_WRAP_GUTTER_MIN = 16;

    private static final int INVALID_WIDTH = -1;

    private static final int ANIMATION_DURATION = 300;

    private static final Marker MARKER = MarkerFactory.getMarker("TabLayout");

    /**
     * Scrollable tabs display a subset of tabs at any given moment, and can contain longer tab labels
     * and a larger number of tabs. They are best used for browsing contexts in touch interfaces when
     * users don't need to directly compare the tab labels.
     *
     * @see #setTabMode(int)
     * @see #getTabMode()
     */
    public static final int MODE_SCROLLABLE = 0;

    /**
     * Fixed tabs display all tabs concurrently and are best used with content that benefits from
     * quick pivots between tabs. The maximum number of tabs is limited by the view's width. Fixed
     * tabs have equal width, based on the widest tab label.
     *
     * @see #setTabMode(int)
     * @see #getTabMode()
     */
    public static final int MODE_FIXED = 1;

    /**
     * Auto-sizing tabs behave like MODE_FIXED with GRAVITY_CENTER while the tabs fit within the
     * TabLayout's content width. Fixed tabs have equal width, based on the widest tab label. Once the
     * tabs outgrow the view's width, auto-sizing tabs behave like MODE_SCROLLABLE, allowing for a
     * dynamic number of tabs without requiring additional layout logic.
     *
     * @see #setTabMode(int)
     * @see #getTabMode()
     */
    public static final int MODE_AUTO = 2;

    /**
     * If a tab is instantiated with {@link Tab#setText(CharSequence)}, and this mode is set, the text
     * will be saved and utilized for the content description, but no visible labels will be created.
     *
     * @see Tab#setTabLabelVisibility(int)
     */
    public static final int TAB_LABEL_VISIBILITY_UNLABELED = 0;

    /**
     * This mode is set by default. If a tab is instantiated with {@link Tab#setText(CharSequence)}, a
     * visible label will be created.
     *
     * @see Tab#setTabLabelVisibility(int)
     */
    public static final int TAB_LABEL_VISIBILITY_LABELED = 1;

    /**
     * Gravity used to fill the {@link TabLayout} as much as possible. This option only takes effect
     * when used with {@link #MODE_FIXED} on non-landscape screens less than 600dp wide.
     *
     * @see #setTabGravity(int)
     * @see #getTabGravity()
     */
    public static final int GRAVITY_FILL = 0;

    /**
     * Gravity used to lay out the tabs in the center of the {@link TabLayout}.
     *
     * @see #setTabGravity(int)
     * @see #getTabGravity()
     */
    public static final int GRAVITY_CENTER = 1;

    /**
     * Gravity used to lay out the tabs aligned to the start of the {@link TabLayout}.
     *
     * @see #setTabGravity(int)
     * @see #getTabGravity()
     */
    public static final int GRAVITY_START = 2;

    // indicatorPosition keeps track of where the indicator is.
    int indicatorPosition = -1;

    /**
     * Indicator gravity used to align the tab selection indicator to the bottom of the {@link
     * TabLayout}. This will only take effect if the indicator height is set via the custom indicator
     * drawable's intrinsic height (preferred). Otherwise, the
     * indicator will not be shown. This is the default value.
     *
     * @see #setSelectedTabIndicatorGravity(int)
     * @see #getTabIndicatorGravity()
     */
    public static final int INDICATOR_GRAVITY_BOTTOM = 0;

    /**
     * Indicator gravity used to align the tab selection indicator to the center of the {@link
     * TabLayout}. This will only take effect if the indicator height is set via the custom indicator
     * drawable's intrinsic height (preferred). Otherwise, the
     * indicator will not be shown.
     *
     * @see #setSelectedTabIndicatorGravity(int)
     * @see #getTabIndicatorGravity()
     */
    public static final int INDICATOR_GRAVITY_CENTER = 1;

    /**
     * Indicator gravity used to align the tab selection indicator to the top of the {@link
     * TabLayout}. This will only take effect if the indicator height is set via the custom indicator
     * drawable's intrinsic height (preferred). Otherwise, the
     * indicator will not be shown.
     *
     * @see #setSelectedTabIndicatorGravity(int)
     * @see #getTabIndicatorGravity()
     */
    public static final int INDICATOR_GRAVITY_TOP = 2;

    /**
     * Indicator gravity used to stretch the tab selection indicator across the entire height
     * of the {@link TabLayout}. This will disregard {@code tabIndicatorHeight} and the
     * indicator drawable's intrinsic height, if set.
     *
     * @see #setSelectedTabIndicatorGravity(int)
     * @see #getTabIndicatorGravity()
     */
    public static final int INDICATOR_GRAVITY_STRETCH = 3;

    /**
     * Indicator animation mode used to translate the selected tab indicator between two tabs using a
     * linear motion.
     *
     * <p>The left and right side of the selection indicator translate in step over the duration of
     * the animation. The only exception to this is when the indicator needs to change size to fit the
     * width of its new destination tab's label.
     *
     * @see #setTabIndicatorAnimationMode(int)
     * @see #getTabIndicatorAnimationMode()
     */
    public static final int INDICATOR_ANIMATION_MODE_LINEAR = 0;

    /**
     * Indicator animation mode used to translate the selected tab indicator by growing and then
     * shrinking the indicator, making the indicator look like it is stretching while translating
     * between destinations.
     *
     * <p>The left and right side of the selection indicator translate out of step - with the right
     * decelerating and the left accelerating (when moving right). This difference in velocity between
     * the sides of the indicator, over the duration of the animation, make the indicator look like it
     * grows and then shrinks back down to fit it's new destination's width.
     *
     * @see #setTabIndicatorAnimationMode(int)
     * @see #getTabIndicatorAnimationMode()
     */
    public static final int INDICATOR_ANIMATION_MODE_ELASTIC = 1;

    /**
     * Indicator animation mode used to switch the selected tab indicator from one tab to another
     * by sequentially fading it out from the current destination and in at its new destination.
     *
     * @see #setTabIndicatorAnimationMode(int)
     * @see #getTabIndicatorAnimationMode()
     */
    public static final int INDICATOR_ANIMATION_MODE_FADE = 2;

    /**
     * Callback interface invoked when a tab's selection state changes.
     */
    public interface OnTabSelectedListener {
        /**
         * Called when a tab enters the selected state.
         *
         * @param tab The tab that was selected
         */
        default void onTabSelected(@NonNull Tab tab) {}

        /**
         * Called when a tab exits the selected state.
         *
         * @param tab The tab that was unselected
         */
        default void onTabUnselected(@NonNull Tab tab) {}

        /**
         * Called when a tab that is already selected is chosen again by the user. Some applications may
         * use this action to return to the top level of a category.
         *
         * @param tab The tab that was reselected.
         */
        default void onTabReselected(@NonNull Tab tab) {}
    }

    private final ArrayList<Tab> tabs = new ArrayList<>();
    @Nullable private Tab selectedTab;

    @NonNull final SlidingTabIndicator slidingTabIndicator;

    int tabPaddingStart;
    int tabPaddingTop;
    int tabPaddingEnd;
    int tabPaddingBottom;

    private final ResourceId tabTextAppearance;
    ColorStateList tabTextColors;
    ColorStateList tabIconTint;
    ColorStateList tabRippleColorStateList;
    Drawable tabSelectedIndicator;
    private int tabSelectedIndicatorColor = Color.TRANSPARENT;

    BlendMode tabIconTintMode;
    float tabTextSize;
    float tabTextMultiLineSize;

    //final int tabBackgroundResId;

    int tabMaxWidth = Integer.MAX_VALUE;
    private final int requestedTabMinWidth;
    private final int requestedTabMaxWidth;
    private final int scrollableTabMinWidth;

    private int contentInsetStart;

    int tabGravity;
    int tabIndicatorAnimationDuration;
    int tabIndicatorGravity;
    int mode;
    boolean inlineLabel;
    boolean tabIndicatorFullWidth;
    int tabIndicatorAnimationMode;
    boolean unboundedRipple;

    private TabIndicatorInterpolator tabIndicatorInterpolator;
    private final TimeInterpolator tabIndicatorTimeInterpolator;

    private final ArrayList<OnTabSelectedListener> selectedListeners = new ArrayList<>();
    @Nullable private ViewPagerOnTabSelectedListener currentVpSelectedListener;

    private ValueAnimator scrollAnimator;

    @Nullable ViewPager viewPager;
    @Nullable private PagerAdapter pagerAdapter;
    private DataSetObserver pagerAdapterObserver;
    private TabLayoutOnPageChangeListener pageChangeListener;
    private AdapterChangeListener adapterChangeListener;
    private boolean setupViewPagerImplicitly;
    private int viewPagerScrollState;

    // Pool we use as a simple RecyclerBin
    private final Pools.Pool<TabView> tabViewPool = new Pools.SimplePool<>(12);

    private static final String[] STYLEABLE = {
            /* 0*/R.ns, R.attr.tabContentStart,
            /* 1*/R.ns, R.attr.tabGravity,
            /* 2*/R.ns, R.attr.tabIconTint,
            /* 3*/R.ns, R.attr.tabIndicator,
            /* 4*/R.ns, R.attr.tabIndicatorAnimationDuration,
            /* 5*/R.ns, R.attr.tabIndicatorAnimationMode,
            /* 6*/R.ns, R.attr.tabIndicatorColor,
            /* 7*/R.ns, R.attr.tabIndicatorFullWidth,
            /* 8*/R.ns, R.attr.tabIndicatorGravity,
            /* 9*/R.ns, R.attr.tabInlineLabel,
            /*10*/R.ns, R.attr.tabMaxWidth,
            /*11*/R.ns, R.attr.tabMinWidth,
            /*12*/R.ns, R.attr.tabMode,
            /*13*/R.ns, R.attr.tabPadding,
            /*14*/R.ns, R.attr.tabPaddingBottom,
            /*15*/R.ns, R.attr.tabPaddingEnd,
            /*16*/R.ns, R.attr.tabPaddingStart,
            /*17*/R.ns, R.attr.tabPaddingTop,
            /*18*/R.ns, R.attr.tabRippleColor,
            /*19*/R.ns, R.attr.tabTextAppearance,
            /*20*/R.ns, R.attr.tabTextColor,
            /*21*/R.ns, R.attr.tabUnboundedRipple,
    };

    public TabLayout(@NonNull Context context) {
        this(context, null);
    }

    public TabLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.tabStyle);
    }

    public TabLayout(@NonNull Context context, @Nullable AttributeSet attrs,
                     @Nullable @AttrRes ResourceId defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    public TabLayout(@NonNull Context context, @Nullable AttributeSet attrs,
                     @Nullable @AttrRes ResourceId defStyleAttr,
                     @Nullable @StyleRes ResourceId defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        // Disable the Scroll Bar
        setHorizontalScrollBarEnabled(false);

        // Add the TabStrip
        super.addView(
                slidingTabIndicator = this.
                        new SlidingTabIndicator(context),
                0,
                new HorizontalScrollView.LayoutParams(
                        WRAP_CONTENT, LayoutParams.MATCH_PARENT));

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, defStyleAttr, defStyleRes, STYLEABLE
        );

        setSelectedTabIndicator(
                a.getDrawable(3)); // tabIndicator
        setSelectedTabIndicatorColor(
                a.getColor(6, Color.TRANSPARENT)); // tabIndicatorColor
        // noinspection MagicConstant
        setSelectedTabIndicatorGravity(
                a.getInt(8, INDICATOR_GRAVITY_BOTTOM)); // tabIndicatorGravity
        // noinspection MagicConstant
        setTabIndicatorAnimationMode(
                a.getInt(5, INDICATOR_ANIMATION_MODE_LINEAR)); // tabIndicatorAnimationMode
        setTabIndicatorFullWidth(a.getBoolean(7, true)); // tabIndicatorFullWidth

        tabPaddingStart =
                tabPaddingTop =
                        tabPaddingEnd =
                                tabPaddingBottom = a.getDimensionPixelSize(13, 0); // tabPadding
        tabPaddingStart =
                a.getDimensionPixelSize(16, tabPaddingStart); // tabPaddingStart
        tabPaddingTop = a.getDimensionPixelSize(17, tabPaddingTop); // tabPaddingTop
        tabPaddingEnd = a.getDimensionPixelSize(15, tabPaddingEnd); // tabPaddingEnd
        tabPaddingBottom =
                a.getDimensionPixelSize(14, tabPaddingBottom); // tabPaddingBottom

        tabTextAppearance = Objects.requireNonNullElse(a.getResourceId(19), // tabTextAppearance
                R.style.TextAppearance_Material3_TitleSmall);

        // Text colors/sizes come from the text appearance first
        final TextAppearance ta = new TextAppearance(context, tabTextAppearance);
        tabTextSize = ta.mTextSize;

        if (a.hasValue(20)) { // tabTextColor
            // If we have an explicit text color set, use it instead
            tabTextColors =
                    a.getColorStateList(20);
        }

        tabIconTint =
                a.getColorStateList(2); // tabIconTint
        /*tabIconTintMode =
                ViewUtils.parseTintMode(a.getInt(R.styleable.TabLayout_tabIconTintMode, -1), null);*/

        tabRippleColorStateList =
                a.getColorStateList(18); // tabRippleColor

        tabIndicatorAnimationDuration =
                a.getInt(4, ANIMATION_DURATION); // tabIndicatorAnimationDuration
        tabIndicatorTimeInterpolator =
                MotionEasingUtils.MOTION_EASING_EMPHASIZED;

        requestedTabMinWidth =
                a.getDimensionPixelSize(11, INVALID_WIDTH); // tabMinWidth
        requestedTabMaxWidth =
                a.getDimensionPixelSize(10, INVALID_WIDTH); // tabMaxWidth
        //tabBackgroundResId = a.getResourceId(R.styleable.TabLayout_tabBackground, 0);
        contentInsetStart = a.getDimensionPixelSize(0, 0); // tabContentStart
        mode = a.getInt(12, MODE_FIXED); // tabMode
        tabGravity = a.getInt(1, GRAVITY_FILL); // tabGravity
        inlineLabel = a.getBoolean(9, false); // tabInlineLabel
        unboundedRipple = a.getBoolean(21, false); // tabUnboundedRipple
        a.recycle();

        if (tabTextSize == -1) {
            throw new IllegalStateException("Text size is not defined in tabTextAppearance " + tabTextAppearance);
        }

        // TODO add attr for these
        tabTextMultiLineSize = sp(12);
        scrollableTabMinWidth = dp(160);

        // Now apply the tab mode and gravity
        applyModeAndGravity();
    }

    /**
     * Sets the tab indicator's color for the currently selected tab.
     *
     * <p>If the tab indicator color is not {@code Color.TRANSPARENT}, the indicator will be wrapped
     * and tinted right before it is drawn by {@link SlidingTabIndicator#draw(Canvas)}. If you'd like
     * the inherent color or the tinted color of a custom drawable to be used, make sure this color is
     * set to {@code Color.TRANSPARENT} to avoid your color/tint being overridden.
     *
     * @param color color to use for the indicator
     */
    public void setSelectedTabIndicatorColor(@ColorInt int color) {
        this.tabSelectedIndicatorColor = color;
        tabSelectedIndicator.setTint(tabSelectedIndicatorColor);
        updateTabViews(false);
    }

    /**
     * Set the scroll position of the {@link TabLayout}.
     *
     * @param position Position of the tab to scroll.
     * @param positionOffset Value from [0, 1) indicating the offset from {@code position}.
     * @param updateSelectedTabView Whether to draw the tab at the specified position + positionOffset
     *     as selected.
     *     <p>Note that calling the method with {@code updateSelectedTabView = true}
     *     <em>does not</em> select a tab at the specified position, but only <em>draws it
     *     as selected</em>. This can be useful for when the TabLayout behavior needs to be linked to
     *     another view, such as {@link ViewPager}.
     * @see #setScrollPosition(int, float, boolean, boolean)
     */
    public void setScrollPosition(int position, float positionOffset, boolean updateSelectedTabView) {
        setScrollPosition(position, positionOffset, updateSelectedTabView, true);
    }

    /**
     * Set the scroll position of the {@link TabLayout}.
     *
     * @param position Position of the tab to scroll.
     * @param positionOffset Value from [0, 1) indicating the offset from {@code position}.
     * @param updateSelectedTabView Whether to draw the tab at the specified position + positionOffset
     *     as selected.
     *     <p>Note that calling the method with {@code updateSelectedTabView = true}
     *     <em>does not</em> select a tab at the specified position, but only <em>draws it
     *     as selected</em>. This can be useful for when the TabLayout behavior needs to be linked to
     *     another view, such as {@link ViewPager}.
     * @param updateIndicatorPosition Whether to set the indicator to the specified position and
     *     offset.
     *     <p>Note that calling the method with {@code updateIndicatorPosition = true}
     *     <em>does not</em> select a tab at the specified position, but only updates the indicator
     *     position. This can be useful for when the TabLayout behavior needs to be linked to
     *     another view, such as {@link ViewPager}.
     * @see #setScrollPosition(int, float, boolean)
     */
    public void setScrollPosition(
            int position,
            float positionOffset,
            boolean updateSelectedTabView,
            boolean updateIndicatorPosition) {
        setScrollPosition(
                position,
                positionOffset,
                updateSelectedTabView,
                updateIndicatorPosition,
                /* alwaysScroll= */ true);
    }

    void setScrollPosition(
            int position,
            float positionOffset,
            boolean updateSelectedTabView,
            boolean updateIndicatorPosition,
            boolean alwaysScroll) {
        final int roundedPosition = Math.round(position + positionOffset);
        if (roundedPosition < 0 || roundedPosition >= slidingTabIndicator.getChildCount()) {
            return;
        }

        // Set the indicator position, if enabled
        if (updateIndicatorPosition) {
            slidingTabIndicator.setIndicatorPositionFromTabPosition(position, positionOffset);
        }

        // Now update the scroll position, canceling any running animation
        if (scrollAnimator != null && scrollAnimator.isRunning()) {
            scrollAnimator.cancel();
        }
        int scrollXForPosition = calculateScrollXForTab(position, positionOffset);
        int scrollX = getScrollX();
        // If the position is smaller than the selected tab position, the position is getting larger
        // to reach the selected tab position so scrollX is increasing.
        // We only want to update the scroll position if the new scroll position is greater than
        // the current scroll position.
        // Conversely if the position is greater than the selected tab position, the position is
        // getting smaller to reach the selected tab position so scrollX is decreasing.
        // We only update the scroll position if the new scroll position is less than the current
        // scroll position.
        // Lastly if the position is equal to the selected position, we want to set the scroll
        // position which also updates the selected tab view and the indicator.
        boolean toMove =
                (position < getSelectedTabPosition() && scrollXForPosition >= scrollX)
                        || (position > getSelectedTabPosition() && scrollXForPosition <= scrollX)
                        || (position == getSelectedTabPosition());
        // If the layout direction is RTL, the scrollXForPosition and scrollX comparisons are
        // reversed since scrollX values remain the same in RTL but tab positions go RTL.
        if (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            toMove =
                    (position < getSelectedTabPosition() && scrollXForPosition <= scrollX)
                            || (position > getSelectedTabPosition()
                            && scrollXForPosition >= scrollX)
                            || (position == getSelectedTabPosition());
        }
        // We want to scroll if alwaysScroll is true, the viewpager is being dragged, or if we should
        // scroll by the rules above.
        if (toMove || viewPagerScrollState == SCROLL_STATE_DRAGGING || alwaysScroll) {
            scrollTo(position < 0 ? 0 : scrollXForPosition, 0);
        }

        // Update the 'selected state' view as we scroll, if enabled
        if (updateSelectedTabView) {
            setSelectedTabView(roundedPosition);
        }
    }

    /**
     * Add a tab to this layout. The tab will be added at the end of the list. If this is the first
     * tab to be added it will become the selected tab.
     *
     * @param tab Tab to add
     */
    public void addTab(@NonNull Tab tab) {
        addTab(tab, tabs.isEmpty());
    }

    /**
     * Add a tab to this layout. The tab will be inserted at <code>position</code>. If this is the
     * first tab to be added it will become the selected tab.
     *
     * @param tab The tab to add
     * @param position The new position of the tab
     */
    public void addTab(@NonNull Tab tab, int position) {
        addTab(tab, position, tabs.isEmpty());
    }

    /**
     * Add a tab to this layout. The tab will be added at the end of the list.
     *
     * @param tab Tab to add
     * @param setSelected True if the added tab should become the selected tab.
     */
    public void addTab(@NonNull Tab tab, boolean setSelected) {
        addTab(tab, tabs.size(), setSelected);
    }

    /**
     * Add a tab to this layout. The tab will be inserted at <code>position</code>.
     *
     * @param tab The tab to add
     * @param position The new position of the tab
     * @param setSelected True if the added tab should become the selected tab.
     */
    public void addTab(@NonNull Tab tab, int position, boolean setSelected) {
        if (tab.parent != this) {
            throw new IllegalArgumentException("Tab belongs to a different TabLayout.");
        }
        configureTab(tab, position);
        addTabView(tab);

        if (setSelected) {
            tab.select();
        }
    }

    /*private void addTabFromItemView(@NonNull TabItem item) {
        final Tab tab = newTab();
        if (item.text != null) {
            tab.setText(item.text);
        }
        if (item.icon != null) {
            tab.setIcon(item.icon);
        }
        if (item.customLayout != 0) {
            tab.setCustomView(item.customLayout);
        }
        if (!TextUtils.isEmpty(item.getContentDescription())) {
            tab.setContentDescription(item.getContentDescription());
        }
        addTab(tab);
    }*/

    private boolean isScrollingEnabled() {
        return getTabMode() == MODE_SCROLLABLE || getTabMode() == MODE_AUTO;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // When a touch event is intercepted and the tab mode is fixed, do not continue to process the
        // touch event. This will prevent unexpected scrolling from occurring in corner cases (i.e. a
        // layout in fixed mode that has padding should not scroll for the width of the padding).
        return isScrollingEnabled() && super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_SCROLL && !isScrollingEnabled()) {
            return false;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Add a {@link TabLayout.OnTabSelectedListener} that will be invoked when tab selection changes.
     *
     * <p>Components that add a listener should take care to remove it when finished via {@link
     * #removeOnTabSelectedListener(OnTabSelectedListener)}.
     *
     * @param listener listener to add
     */
    public void addOnTabSelectedListener(@NonNull OnTabSelectedListener listener) {
        if (!selectedListeners.contains(listener)) {
            selectedListeners.add(listener);
        }
    }

    /**
     * Remove the given {@link TabLayout.OnTabSelectedListener} that was previously added via {@link
     * #addOnTabSelectedListener(OnTabSelectedListener)}.
     *
     * @param listener listener to remove
     */
    public void removeOnTabSelectedListener(@NonNull OnTabSelectedListener listener) {
        selectedListeners.remove(listener);
    }

    /** Remove all previously added {@link TabLayout.OnTabSelectedListener}s. */
    public void clearOnTabSelectedListeners() {
        selectedListeners.clear();
    }

    /**
     * Create and return a new {@link Tab}. You need to manually add this using {@link #addTab(Tab)}
     * or a related method.
     *
     * @return A new Tab
     * @see #addTab(Tab)
     */
    @NonNull
    public Tab newTab() {
        Tab tab = new Tab();
        tab.parent = this;
        tab.view = createTabView(tab);
        if (tab.id != NO_ID) {
            tab.view.setId(tab.id);
        }

        return tab;
    }

    /**
     * Returns the number of tabs currently registered with the tab layout.
     *
     * @return Tab count
     */
    public int getTabCount() {
        return tabs.size();
    }

    /** Returns the tab at the specified index. */
    @Nullable
    public Tab getTabAt(int index) {
        return (index < 0 || index >= getTabCount()) ? null : tabs.get(index);
    }

    /**
     * Returns the position of the current selected tab.
     *
     * @return selected tab position, or {@code -1} if there isn't a selected tab.
     */
    public int getSelectedTabPosition() {
        return selectedTab != null ? selectedTab.getPosition() : -1;
    }

    /**
     * Remove a tab from the layout. If the removed tab was selected it will be deselected and another
     * tab will be selected if present.
     *
     * @param tab The tab to remove
     */
    public void removeTab(@NonNull Tab tab) {
        if (tab.parent != this) {
            throw new IllegalArgumentException("Tab does not belong to this TabLayout.");
        }

        removeTabAt(tab.getPosition());
    }

    /**
     * Remove a tab from the layout. If the removed tab was selected it will be deselected and another
     * tab will be selected if present.
     *
     * @param position Position of the tab to remove
     */
    public void removeTabAt(int position) {
        final int selectedTabPosition = selectedTab != null ? selectedTab.getPosition() : 0;
        removeTabViewAt(position);

        final Tab removedTab = tabs.remove(position);
        if (removedTab != null) {
            removedTab.reset();
        }

        final int newTabCount = tabs.size();
        int newIndicatorPosition = -1;
        for (int i = position; i < newTabCount; i++) {
            // If the current tab position is the indicator position, mark its new position as the new
            // indicator position.
            if (tabs.get(i).getPosition() == indicatorPosition) {
                newIndicatorPosition = i;
            }
            tabs.get(i).setPosition(i);
        }
        // Update the indicator position to the correct selected tab after refreshing tab positions.
        indicatorPosition = newIndicatorPosition;

        if (selectedTabPosition == position) {
            selectTab(tabs.isEmpty() ? null : tabs.get(Math.max(0, position - 1)));
        }
    }

    /** Remove all tabs from the tab layout and deselect the current tab. */
    public void removeAllTabs() {
        // Remove all the views
        for (int i = slidingTabIndicator.getChildCount() - 1; i >= 0; i--) {
            removeTabViewAt(i);
        }

        for (final Iterator<Tab> i = tabs.iterator(); i.hasNext(); ) {
            final Tab tab = i.next();
            i.remove();
            tab.reset();
        }

        selectedTab = null;
    }

    /**
     * Set the behavior mode for the Tabs in this layout. The valid input options are:
     *
     * <ul>
     *   <li>{@link #MODE_FIXED}: Fixed tabs display all tabs concurrently and are best used with
     *       content that benefits from quick pivots between tabs.
     *   <li>{@link #MODE_SCROLLABLE}: Scrollable tabs display a subset of tabs at any given moment,
     *       and can contain longer tab labels and a larger number of tabs. They are best used for
     *       browsing contexts in touch interfaces when users don't need to directly compare the tab
     *       labels. This mode is commonly used with a {@link ViewPager}.
     * </ul>
     *
     * @param mode one of {@link #MODE_FIXED}, {@link #MODE_SCROLLABLE} or {@link #MODE_AUTO}.
     */
    public void setTabMode(@MagicConstant(intValues = {MODE_SCROLLABLE, MODE_FIXED, MODE_AUTO}) int mode) {
        if (mode != this.mode) {
            this.mode = mode;
            applyModeAndGravity();
        }
    }

    /**
     * Returns the current mode used by this {@link TabLayout}.
     *
     * @see #setTabMode(int)
     */
    @MagicConstant(intValues = {MODE_SCROLLABLE, MODE_FIXED, MODE_AUTO})
    public int getTabMode() {
        return mode;
    }

    /**
     * Set the gravity to use when laying out the tabs.
     *
     * @param gravity one of {@link #GRAVITY_CENTER}, {@link #GRAVITY_FILL} or {@link #GRAVITY_START}.
     */
    public void setTabGravity(@MagicConstant(intValues = {GRAVITY_FILL, GRAVITY_CENTER, GRAVITY_START}) int gravity) {
        if (tabGravity != gravity) {
            tabGravity = gravity;
            applyModeAndGravity();
        }
    }

    /**
     * The current gravity used for laying out tabs.
     *
     * @see #setTabGravity(int)
     */
    @MagicConstant(intValues = {GRAVITY_FILL, GRAVITY_CENTER, GRAVITY_START})
    public int getTabGravity() {
        return tabGravity;
    }

    /**
     * Set the indicator gravity used to align the tab selection indicator in the {@link TabLayout}.
     * You must set the indicator height via the custom indicator drawable's intrinsic height
     * (preferred). Otherwise, the indicator will not be shown
     * unless gravity is set to {@link #INDICATOR_GRAVITY_STRETCH}, in which case it will ignore
     * indicator height and stretch across the entire height of the {@link TabLayout}. This
     * defaults to {@link #INDICATOR_GRAVITY_BOTTOM} if not set.
     *
     * @param indicatorGravity one of {@link #INDICATOR_GRAVITY_BOTTOM}, {@link
     *     #INDICATOR_GRAVITY_CENTER}, {@link #INDICATOR_GRAVITY_TOP}, or {@link
     *     #INDICATOR_GRAVITY_STRETCH}
     */
    public void setSelectedTabIndicatorGravity(@MagicConstant(intValues = {
            INDICATOR_GRAVITY_BOTTOM,
            INDICATOR_GRAVITY_CENTER,
            INDICATOR_GRAVITY_TOP,
            INDICATOR_GRAVITY_STRETCH
    }) int indicatorGravity) {
        if (tabIndicatorGravity != indicatorGravity) {
            tabIndicatorGravity = indicatorGravity;
            slidingTabIndicator.postInvalidateOnAnimation();
        }
    }

    /**
     * Get the current indicator gravity used to align the tab selection indicator in the {@link
     * TabLayout}.
     *
     * @return one of {@link #INDICATOR_GRAVITY_BOTTOM}, {@link #INDICATOR_GRAVITY_CENTER}, {@link
     *     #INDICATOR_GRAVITY_TOP}, or {@link #INDICATOR_GRAVITY_STRETCH}
     */
    @MagicConstant(intValues = {
            INDICATOR_GRAVITY_BOTTOM,
            INDICATOR_GRAVITY_CENTER,
            INDICATOR_GRAVITY_TOP,
            INDICATOR_GRAVITY_STRETCH
    })
    public int getTabIndicatorGravity() {
        return tabIndicatorGravity;
    }

    /**
     * Set the mode by which the selection indicator should animate when moving between destinations.
     *
     * <p>Defaults to {@link #INDICATOR_ANIMATION_MODE_LINEAR}. Changing this is useful as a stylistic
     * choice.
     *
     * @param tabIndicatorAnimationMode one of {@link #INDICATOR_ANIMATION_MODE_LINEAR} or {@link
     *     #INDICATOR_ANIMATION_MODE_ELASTIC}
     * @see #getTabIndicatorAnimationMode()
     */
    public void setTabIndicatorAnimationMode(
            @MagicConstant(intValues = {INDICATOR_ANIMATION_MODE_LINEAR,
                    INDICATOR_ANIMATION_MODE_ELASTIC,
                    INDICATOR_ANIMATION_MODE_FADE}) int tabIndicatorAnimationMode) {
        this.tabIndicatorAnimationMode = tabIndicatorAnimationMode;
        switch (tabIndicatorAnimationMode) {
            case INDICATOR_ANIMATION_MODE_LINEAR:
                this.tabIndicatorInterpolator = new TabIndicatorInterpolator();
                break;
            case INDICATOR_ANIMATION_MODE_ELASTIC:
                this.tabIndicatorInterpolator = new ElasticTabIndicatorInterpolator();
                break;
            case INDICATOR_ANIMATION_MODE_FADE:
                this.tabIndicatorInterpolator = new FadeTabIndicatorInterpolator();
                break;
            default:
                throw new IllegalArgumentException(
                        tabIndicatorAnimationMode + " is not a valid TabIndicatorAnimationMode");
        }
    }

    /**
     * Get the current indicator animation mode used to animate the selection indicator between
     * destinations.
     *
     * @return one of {@link #INDICATOR_ANIMATION_MODE_LINEAR} or {@link
     *     #INDICATOR_ANIMATION_MODE_ELASTIC}
     * @see #setTabIndicatorAnimationMode(int)
     */
    @MagicConstant(intValues = {
            INDICATOR_ANIMATION_MODE_LINEAR,
            INDICATOR_ANIMATION_MODE_ELASTIC,
            INDICATOR_ANIMATION_MODE_FADE
    })
    public int getTabIndicatorAnimationMode() {
        return tabIndicatorAnimationMode;
    }

    /**
     * Enable or disable option to fit the tab selection indicator to the full width of the tab item
     * rather than to the tab item's content.
     *
     * <p>Defaults to true. If set to false and the tab item has a text label, the selection indicator
     * width will be set to the width of the text label. If the tab item has no text label, but does
     * have an icon, the selection indicator width will be set to the icon. If the tab item has
     * neither of these, or if the calculated width is less than a minimum width value, the selection
     * indicator width will be set to the minimum width value.
     *
     * @param tabIndicatorFullWidth Whether or not to fit selection indicator width to full width of
     *     the tab item
     * @see #isTabIndicatorFullWidth()
     */
    public void setTabIndicatorFullWidth(boolean tabIndicatorFullWidth) {
        this.tabIndicatorFullWidth = tabIndicatorFullWidth;
        slidingTabIndicator.jumpIndicatorToSelectedPosition();
        slidingTabIndicator.postInvalidateOnAnimation();
    }

    /**
     * Get whether or not selection indicator width is fit to full width of the tab item, or fit to
     * the tab item's content.
     *
     * @return whether or not selection indicator width is fit to the full width of the tab item
     * @see #setTabIndicatorFullWidth(boolean)
     */
    public boolean isTabIndicatorFullWidth() {
        return tabIndicatorFullWidth;
    }

    /**
     * Set whether tab labels will be displayed inline with tab icons, or if they will be displayed
     * underneath tab icons.
     *
     * @see #isInlineLabel()
     */
    public void setInlineLabel(boolean inline) {
        if (inlineLabel != inline) {
            inlineLabel = inline;
            for (int i = 0; i < slidingTabIndicator.getChildCount(); i++) {
                View child = slidingTabIndicator.getChildAt(i);
                if (child instanceof TabView) {
                    ((TabView) child).updateOrientation();
                }
            }
            applyModeAndGravity();
        }
    }

    /**
     * Returns whether tab labels will be displayed inline with tab icons, or if they will be
     * displayed underneath tab icons.
     *
     * @see #setInlineLabel(boolean)
     */
    public boolean isInlineLabel() {
        return inlineLabel;
    }

    /**
     * Set whether this {@link TabLayout} will have an unbounded ripple effect or if ripple will be
     * bound to the tab item size.
     *
     * <p>Defaults to false.
     *
     * @see #hasUnboundedRipple()
     */
    public void setUnboundedRipple(boolean unboundedRipple) {
        if (this.unboundedRipple != unboundedRipple) {
            this.unboundedRipple = unboundedRipple;
            for (int i = 0; i < slidingTabIndicator.getChildCount(); i++) {
                View child = slidingTabIndicator.getChildAt(i);
                if (child instanceof TabView) {
                    ((TabView) child).updateBackgroundDrawable(getContext());
                }
            }
        }
    }

    /**
     * Returns whether this {@link TabLayout} has an unbounded ripple effect, or if ripple is bound to
     * the tab item size.
     *
     * @see #setUnboundedRipple(boolean)
     */
    public boolean hasUnboundedRipple() {
        return unboundedRipple;
    }

    /**
     * Sets the text colors for the different states (normal, selected) used for the tabs.
     *
     * @see #getTabTextColors()
     */
    public void setTabTextColors(@Nullable ColorStateList textColor) {
        if (tabTextColors != textColor) {
            tabTextColors = textColor;
            updateAllTabs();
        }
    }

    /** Gets the text colors for the different states (normal, selected) used for the tabs. */
    @Nullable
    public ColorStateList getTabTextColors() {
        return tabTextColors;
    }

    /**
     * Sets the text colors for the different states (normal, selected) used for the tabs.
     */
    public void setTabTextColors(int normalColor, int selectedColor) {
        setTabTextColors(createColorStateList(normalColor, selectedColor));
    }

    /**
     * Sets the icon tint for the different states (normal, selected) used for the tabs.
     *
     * @see #getTabIconTint()
     */
    public void setTabIconTint(@Nullable ColorStateList iconTint) {
        if (tabIconTint != iconTint) {
            tabIconTint = iconTint;
            updateAllTabs();
        }
    }

    /** Gets the icon tint for the different states (normal, selected) used for the tabs. */
    @Nullable
    public ColorStateList getTabIconTint() {
        return tabIconTint;
    }

    /**
     * Returns the ripple color for this TabLayout.
     *
     * @return the color (or ColorStateList) used for the ripple
     * @see #setTabRippleColor(ColorStateList)
     */
    @Nullable
    public ColorStateList getTabRippleColor() {
        return tabRippleColorStateList;
    }

    /**
     * Sets the ripple color for this TabLayout.
     *
     * <p>When running on devices with KitKat, we draw this color as a filled overlay rather than a
     * ripple.
     *
     * @param color color (or ColorStateList) to use for the ripple
     * @see #getTabRippleColor()
     */
    public void setTabRippleColor(@Nullable ColorStateList color) {
        if (tabRippleColorStateList != color) {
            tabRippleColorStateList = color;
            for (int i = 0; i < slidingTabIndicator.getChildCount(); i++) {
                View child = slidingTabIndicator.getChildAt(i);
                if (child instanceof TabView) {
                    ((TabView) child).updateBackgroundDrawable(getContext());
                }
            }
        }
    }

    /**
     * Returns the selection indicator drawable for this TabLayout.
     *
     * @return The drawable used as the tab selection indicator, if set.
     * @see #setSelectedTabIndicator(Drawable)
     */
    @NonNull
    public Drawable getTabSelectedIndicator() {
        return tabSelectedIndicator;
    }

    /**
     * Sets the selection indicator for this TabLayout. By default, this is a line along the bottom of
     * the tab. If {@code tabIndicatorColor} is specified via the TabLayout's style or via {@link
     * #setSelectedTabIndicatorColor(int)} the selection indicator will be tinted that color.
     * Otherwise, it will use the colors specified in the drawable.
     *
     * <p>Setting the indicator drawable to null will cause {@link TabLayout} to use the default,
     * {@link ShapeDrawable} line indicator.
     *
     * @param tabSelectedIndicator A drawable to use as the selected tab indicator.
     * @see #setSelectedTabIndicatorColor(int)
     */
    public void setSelectedTabIndicator(@Nullable Drawable tabSelectedIndicator) {
        if (tabSelectedIndicator == null) {
            tabSelectedIndicator = new ShapeDrawable();
        }
        this.tabSelectedIndicator = tabSelectedIndicator.mutate();
        this.tabSelectedIndicator.setTint(tabSelectedIndicatorColor);
        int indicatorHeight = this.tabSelectedIndicator.getIntrinsicHeight();
        slidingTabIndicator.setSelectedIndicatorHeight(indicatorHeight);
    }

    /**
     * The one-stop shop for setting up this {@link TabLayout} with a {@link ViewPager}.
     *
     * <p>This is the same as calling {@link #setupWithViewPager(ViewPager, boolean)} with
     * auto-refresh enabled.
     *
     * @param viewPager the ViewPager to link to, or {@code null} to clear any previous link
     */
    public void setupWithViewPager(@Nullable ViewPager viewPager) {
        setupWithViewPager(viewPager, true);
    }

    /**
     * The one-stop shop for setting up this {@link TabLayout} with a {@link ViewPager}.
     *
     * <p>This method will link the given ViewPager and this TabLayout together so that changes in one
     * are automatically reflected in the other. This includes scroll state changes and clicks. The
     * tabs displayed in this layout will be populated from the ViewPager adapter's page titles.
     *
     * <p>If {@code autoRefresh} is {@code true}, any changes in the {@link PagerAdapter} will trigger
     * this layout to re-populate itself from the adapter's titles.
     *
     * <p>If the given ViewPager is non-null, it needs to already have a {@link PagerAdapter} set.
     *
     * @param viewPager the ViewPager to link to, or {@code null} to clear any previous link
     * @param autoRefresh whether this layout should refresh its contents if the given ViewPager's
     *     content changes
     */
    public void setupWithViewPager(@Nullable final ViewPager viewPager, boolean autoRefresh) {
        setupWithViewPager(viewPager, autoRefresh, false);
    }

    private void setupWithViewPager(
            @Nullable final ViewPager viewPager, boolean autoRefresh, boolean implicitSetup) {
        if (this.viewPager != null) {
            // If we've already been setup with a ViewPager, remove us from it
            if (pageChangeListener != null) {
                this.viewPager.removeOnPageChangeListener(pageChangeListener);
            }
            if (adapterChangeListener != null) {
                this.viewPager.removeOnAdapterChangeListener(adapterChangeListener);
            }
        }

        if (currentVpSelectedListener != null) {
            // If we already have a tab selected listener for the ViewPager, remove it
            removeOnTabSelectedListener(currentVpSelectedListener);
            currentVpSelectedListener = null;
        }

        if (viewPager != null) {
            this.viewPager = viewPager;

            // Add our custom OnPageChangeListener to the ViewPager
            if (pageChangeListener == null) {
                pageChangeListener = new TabLayoutOnPageChangeListener(this);
            }
            pageChangeListener.reset();
            viewPager.addOnPageChangeListener(pageChangeListener);

            // Now we'll add a tab selected listener to set ViewPager's current item
            currentVpSelectedListener = new ViewPagerOnTabSelectedListener(viewPager);
            addOnTabSelectedListener(currentVpSelectedListener);

            final PagerAdapter adapter = viewPager.getAdapter();
            if (adapter != null) {
                // Now we'll populate ourselves from the pager adapter, adding an observer if
                // autoRefresh is enabled
                setPagerAdapter(adapter, autoRefresh);
            }

            // Add a listener so that we're notified of any adapter changes
            if (adapterChangeListener == null) {
                adapterChangeListener = new AdapterChangeListener();
            }
            adapterChangeListener.setAutoRefresh(autoRefresh);
            viewPager.addOnAdapterChangeListener(adapterChangeListener);

            // Now update the scroll position to match the ViewPager's current item
            setScrollPosition(viewPager.getCurrentItem(), 0f, true);
        } else {
            // We've been given a null ViewPager so we need to clear out the internal state,
            // listeners and observers
            this.viewPager = null;
            setPagerAdapter(null, false);
        }

        setupViewPagerImplicitly = implicitSetup;
    }

    void updateViewPagerScrollState(int scrollState) {
        this.viewPagerScrollState = scrollState;
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        // Only delay the pressed state if the tabs can scroll
        return getTabScrollRange() > 0;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (viewPager == null) {
            // If we don't have a ViewPager already, check if our parent is a ViewPager to
            // setup with it automatically
            final ViewParent vp = getParent();
            if (vp instanceof ViewPager) {
                // If we have a ViewPager parent and we've been added as part of its decor, let's
                // assume that we should automatically setup to display any titles
                setupWithViewPager((ViewPager) vp, true, true);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (setupViewPagerImplicitly) {
            // If we've been setup with a ViewPager implicitly, let's clear out any listeners, etc
            setupWithViewPager(null);
            setupViewPagerImplicitly = false;
        }
    }

    private int getTabScrollRange() {
        return Math.max(
                0, slidingTabIndicator.getWidth() - getWidth() - getPaddingLeft() - getPaddingRight());
    }

    void setPagerAdapter(@Nullable final PagerAdapter adapter, final boolean addObserver) {
        if (pagerAdapter != null && pagerAdapterObserver != null) {
            // If we already have a PagerAdapter, unregister our observer
            pagerAdapter.unregisterDataSetObserver(pagerAdapterObserver);
        }

        pagerAdapter = adapter;

        if (addObserver && adapter != null) {
            // Register our observer on the new adapter
            if (pagerAdapterObserver == null) {
                pagerAdapterObserver = new PagerAdapterObserver();
            }
            adapter.registerDataSetObserver(pagerAdapterObserver);
        }

        // Finally make sure we reflect the new adapter
        populateFromPagerAdapter();
    }

    void populateFromPagerAdapter() {
        removeAllTabs();

        if (pagerAdapter != null) {
            final int adapterCount = pagerAdapter.getCount();
            for (int i = 0; i < adapterCount; i++) {
                addTab(newTab().setText(pagerAdapter.getPageTitle(i)), false);
            }

            // Make sure we reflect the currently set ViewPager item
            if (viewPager != null && adapterCount > 0) {
                final int curItem = viewPager.getCurrentItem();
                if (curItem != getSelectedTabPosition() && curItem < getTabCount()) {
                    selectTab(getTabAt(curItem));
                }
            }
        }
    }

    private void updateAllTabs() {
        for (int i = 0, z = tabs.size(); i < z; i++) {
            tabs.get(i).updateView();
        }
    }

    @NonNull
    private TabView createTabView(@NonNull final Tab tab) {
        TabView tabView = tabViewPool != null ? tabViewPool.acquire() : null;
        if (tabView == null) {
            tabView = new TabView(getContext());
        }
        tabView.setTab(tab);
        tabView.setFocusable(true);
        tabView.setMinimumWidth(getTabMinWidth());
        if (TextUtils.isEmpty(tab.contentDesc)) {
            tabView.setContentDescription(tab.text);
        } else {
            tabView.setContentDescription(tab.contentDesc);
        }
        return tabView;
    }

    private void configureTab(@NonNull Tab tab, int position) {
        tab.setPosition(position);
        tabs.add(position, tab);

        final int count = tabs.size();
        int newIndicatorPosition = -1;
        for (int i = position + 1; i < count; i++) {
            // If the current tab position is the indicator position, mark its new position as the new
            // indicator position.
            if (tabs.get(i).getPosition() == indicatorPosition) {
                newIndicatorPosition = i;
            }
            tabs.get(i).setPosition(i);
        }
        indicatorPosition = newIndicatorPosition;
    }

    private void addTabView(@NonNull Tab tab) {
        final TabView tabView = tab.view;
        tabView.setSelected(false);
        tabView.setActivated(false);
        slidingTabIndicator.addView(tabView, tab.getPosition(), createLayoutParamsForTabs());
    }

    @Override
    public void addView(@NonNull View child) {
        addViewInternal(child);
    }

    @Override
    public void addView(@NonNull View child, int index) {
        addViewInternal(child);
    }

    @Override
    public void addView(@NonNull View child, @NonNull ViewGroup.LayoutParams params) {
        addViewInternal(child);
    }

    @Override
    public void addView(@NonNull View child, int index, @NonNull ViewGroup.LayoutParams params) {
        addViewInternal(child);
    }

    private void addViewInternal(final View child) {
        /*if (child instanceof TabItem) {
            addTabFromItemView((TabItem) child);
        } else */{
            throw new IllegalArgumentException("Only TabItem instances can be added to TabLayout");
        }
    }

    @NonNull
    private LinearLayout.LayoutParams createLayoutParamsForTabs() {
        final LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        updateTabViewLayoutParams(lp);
        return lp;
    }

    private void updateTabViewLayoutParams(@NonNull LinearLayout.LayoutParams lp) {
        if (mode == MODE_FIXED && tabGravity == GRAVITY_FILL) {
            lp.width = 0;
            lp.weight = 1;
        } else {
            lp.width = WRAP_CONTENT;
            lp.weight = 0;
        }
    }

    /*@Override
    public void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        AccessibilityNodeInfoCompat infoCompat = AccessibilityNodeInfoCompat.wrap(info);
        infoCompat.setCollectionInfo(
                CollectionInfoCompat.obtain(
                        *//* rowCount= *//* 1,
                        *//* columnCount= *//* getTabCount(),
                        *//* hierarchical= *//* false,
                        *//* selectionMode = *//* CollectionInfoCompat.SELECTION_MODE_SINGLE));
    }*/

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        // Draw tab background layer for each tab item
        for (int i = 0; i < slidingTabIndicator.getChildCount(); i++) {
            View tabView = slidingTabIndicator.getChildAt(i);
            if (tabView instanceof TabView) {
                ((TabView) tabView).drawBackground(canvas);
            }
        }

        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // If we have a MeasureSpec which allows us to decide our height, try and use the default
        // height
        final int idealHeight = dp(getDefaultHeight());
        switch (MeasureSpec.getMode(heightMeasureSpec)) {
            case MeasureSpec.AT_MOST:
                if (getChildCount() == 1 && MeasureSpec.getSize(heightMeasureSpec) >= idealHeight) {
                    getChildAt(0).setMinimumHeight(idealHeight);
                }
                break;
            case MeasureSpec.UNSPECIFIED:
                heightMeasureSpec =
                        MeasureSpec.makeMeasureSpec(
                                idealHeight + getPaddingTop() + getPaddingBottom(), MeasureSpec.EXACTLY);
                break;
            default:
                break;
        }

        final int specWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            // If we don't have an unspecified width spec, use the given size to calculate
            // the max tab width
            tabMaxWidth =
                    requestedTabMaxWidth > 0
                            ? requestedTabMaxWidth
                            : (specWidth - dp(TAB_MIN_WIDTH_MARGIN));
        }

        // Now super measure itself using the (possibly) modified height spec
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (getChildCount() == 1) {
            // If we're in fixed mode then we need to make sure the tab strip is the same width as us
            // so we don't scroll
            final View child = getChildAt(0);
            boolean remeasure = false;

            switch (mode) {
                case MODE_AUTO:
                case MODE_SCROLLABLE:
                    // We only need to resize the child if it's smaller than us. This is similar
                    // to fillViewport
                    remeasure = child.getMeasuredWidth() < getMeasuredWidth();
                    break;
                case MODE_FIXED:
                    // Resize the child so that it doesn't scroll
                    remeasure = child.getMeasuredWidth() != getMeasuredWidth();
                    break;
            }

            if (remeasure) {
                // Re-measure the child with a widthSpec set to be exactly our measure width
                int childHeightMeasureSpec =
                        getChildMeasureSpec(
                                heightMeasureSpec,
                                getPaddingTop() + getPaddingBottom(),
                                child.getLayoutParams().height);

                int childWidthMeasureSpec =
                        MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY);
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    private void removeTabViewAt(int position) {
        final TabView view = (TabView) slidingTabIndicator.getChildAt(position);
        slidingTabIndicator.removeViewAt(position);
        if (view != null) {
            view.reset();
            tabViewPool.release(view);
        }
        requestLayout();
    }

    private void animateToTab(int newPosition) {
        if (newPosition == Tab.INVALID_POSITION) {
            return;
        }

        if (!isAttachedToWindow() || !isLaidOut() || slidingTabIndicator.childrenNeedLayout()) {
            // If we don't have a window token, or we haven't been laid out yet just draw the new
            // position now
            setScrollPosition(newPosition, 0f, true);
            return;
        }

        final int startScrollX = getScrollX();
        final int targetScrollX = calculateScrollXForTab(newPosition, 0);

        if (startScrollX != targetScrollX) {
            ensureScrollAnimator();

            scrollAnimator.setValues(PropertyValuesHolder.ofInt(startScrollX, targetScrollX));
            scrollAnimator.start();
        }

        // Now animate the indicator
        slidingTabIndicator.animateIndicatorToPosition(newPosition, tabIndicatorAnimationDuration);
    }

    private void ensureScrollAnimator() {
        if (scrollAnimator == null) {
            scrollAnimator = new ValueAnimator();
            scrollAnimator.setInterpolator(tabIndicatorTimeInterpolator);
            scrollAnimator.setDuration(tabIndicatorAnimationDuration);
            scrollAnimator.addUpdateListener(
                    animator -> scrollTo((int) animator.getAnimatedValue(), 0));
        }
    }

    void setScrollAnimatorListener(AnimatorListener listener) {
        ensureScrollAnimator();
        scrollAnimator.addListener(listener);
    }

    /**
     * Called when a tab is selected. Unselects all other tabs in the TabLayout.
     *
     * @param position Position of the selected tab.
     */
    private void setSelectedTabView(int position) {
        final int tabCount = slidingTabIndicator.getChildCount();
        if (position < tabCount) {
            for (int i = 0; i < tabCount; i++) {
                final View child = slidingTabIndicator.getChildAt(i);
                // Update the tab view if it needs to be updated (eg. it's newly selected and it is not
                // yet selected, or it is selected and something else was selected).
                if ((i == position && !child.isSelected()) || (i != position && child.isSelected())) {
                    child.setSelected(i == position);
                    child.setActivated(i == position);
                    if (child instanceof TabView) {
                        ((TabView) child).updateTab();
                    }
                    continue;
                }
                child.setSelected(i == position);
                child.setActivated(i == position);
            }
        }
    }

    /**
     * Selects the given tab.
     *
     * @param tab The tab to select, or {@code null} to select none.
     * @see #selectTab(Tab, boolean)
     */
    public void selectTab(@Nullable Tab tab) {
        selectTab(tab, true);
    }

    /**
     * Selects the given tab. Will always animate to the selected tab if the current tab is
     * reselected, regardless of the value of {@code updateIndicator}.
     *
     * @param tab The tab to select, or {@code null} to select none.
     * @param updateIndicator Whether to update the indicator.
     * @see #selectTab(Tab)
     */
    public void selectTab(@Nullable final Tab tab, boolean updateIndicator) {
        final Tab currentTab = selectedTab;

        if (currentTab == tab) {
            if (currentTab != null) {
                dispatchTabReselected(tab);
                animateToTab(tab.getPosition());
            }
        } else {
            final int newPosition = tab != null ? tab.getPosition() : Tab.INVALID_POSITION;
            if (updateIndicator) {
                if ((currentTab == null || currentTab.getPosition() == Tab.INVALID_POSITION)
                        && newPosition != Tab.INVALID_POSITION) {
                    // If we don't currently have a tab, just draw the indicator
                    setScrollPosition(newPosition, 0f, true);
                } else {
                    animateToTab(newPosition);
                }
                if (newPosition != Tab.INVALID_POSITION) {
                    setSelectedTabView(newPosition);
                }
            }
            // Setting selectedTab before dispatching 'tab unselected' events, so that currentTab's state
            // will be interpreted as unselected
            selectedTab = tab;
            // If the current tab is still attached to the TabLayout.
            if (currentTab != null && currentTab.parent != null) {
                dispatchTabUnselected(currentTab);
            }
            if (tab != null) {
                dispatchTabSelected(tab);
            }
        }
    }

    private void dispatchTabSelected(@NonNull final Tab tab) {
        for (int i = selectedListeners.size() - 1; i >= 0; i--) {
            selectedListeners.get(i).onTabSelected(tab);
        }
    }

    private void dispatchTabUnselected(@NonNull final Tab tab) {
        for (int i = selectedListeners.size() - 1; i >= 0; i--) {
            selectedListeners.get(i).onTabUnselected(tab);
        }
    }

    private void dispatchTabReselected(@NonNull final Tab tab) {
        for (int i = selectedListeners.size() - 1; i >= 0; i--) {
            selectedListeners.get(i).onTabReselected(tab);
        }
    }

    private int calculateScrollXForTab(int position, float positionOffset) {
        if (mode == MODE_SCROLLABLE || mode == MODE_AUTO) {
            final View selectedChild = slidingTabIndicator.getChildAt(position);
            if (selectedChild == null) {
                return 0;
            }
            final View nextChild =
                    position + 1 < slidingTabIndicator.getChildCount()
                            ? slidingTabIndicator.getChildAt(position + 1)
                            : null;
            final int selectedWidth = selectedChild.getWidth();
            final int nextWidth = nextChild != null ? nextChild.getWidth() : 0;

            // base scroll amount: places center of tab in center of parent
            int scrollBase = selectedChild.getLeft() + (selectedWidth / 2) - (getWidth() / 2);
            // offset amount: fraction of the distance between centers of tabs
            int scrollOffset = (int) ((selectedWidth + nextWidth) * 0.5f * positionOffset);

            return (getLayoutDirection() == View.LAYOUT_DIRECTION_LTR)
                    ? scrollBase + scrollOffset
                    : scrollBase - scrollOffset;
        }
        return 0;
    }

    private void applyModeAndGravity() {
        int paddingStart = 0;
        if (mode == MODE_SCROLLABLE || mode == MODE_AUTO) {
            // If we're scrollable, or fixed at start, inset using padding
            paddingStart = Math.max(0, contentInsetStart - tabPaddingStart);
        }
        slidingTabIndicator.setPaddingRelative(paddingStart, 0, 0, 0);

        switch (mode) {
            case MODE_AUTO:
            case MODE_FIXED:
                if (tabGravity == GRAVITY_START) {
                    Log.LOGGER.warn(
                            MARKER,
                            "GRAVITY_START is not supported with the current tab mode, GRAVITY_CENTER will be"
                                    + " used instead");
                }
                slidingTabIndicator.setGravity(Gravity.CENTER_HORIZONTAL);
                break;
            case MODE_SCROLLABLE:
                applyGravityForModeScrollable(tabGravity);
                break;
        }

        updateTabViews(true);
    }

    private void applyGravityForModeScrollable(int tabGravity) {
        switch (tabGravity) {
            case GRAVITY_CENTER:
                slidingTabIndicator.setGravity(Gravity.CENTER_HORIZONTAL);
                break;
            case GRAVITY_FILL:
                Log.LOGGER.warn(
                        MARKER,
                        "MODE_SCROLLABLE + GRAVITY_FILL is not supported, GRAVITY_START will be used"
                                + " instead");
                // Fall through
            case GRAVITY_START:
                slidingTabIndicator.setGravity(Gravity.START);
                break;
            default:
                break;
        }
    }

    void updateTabViews(final boolean requestLayout) {
        for (int i = 0; i < slidingTabIndicator.getChildCount(); i++) {
            View child = slidingTabIndicator.getChildAt(i);
            child.setMinimumWidth(getTabMinWidth());
            updateTabViewLayoutParams((LinearLayout.LayoutParams) child.getLayoutParams());
            if (requestLayout) {
                child.requestLayout();
            }
        }
    }

    /** A tab in this layout. Instances can be created via {@link #newTab()}. */
    public static final class Tab {

        /**
         * An invalid position for a tab.
         *
         * @see #getPosition()
         */
        public static final int INVALID_POSITION = -1;

        @Nullable private Object tag;
        @Nullable private Drawable icon;
        @Nullable private CharSequence text;
        // This represents the content description that has been explicitly set on the Tab or TabItem
        // in XML or through #setContentDescription. If the content description is empty, text should
        // be used as the content description instead, but contentDesc should remain empty.
        @Nullable private CharSequence contentDesc;
        private int position = INVALID_POSITION;
        @Nullable private View customView;
        private int labelVisibilityMode = TAB_LABEL_VISIBILITY_LABELED;

        TabLayout parent;
        TabView view;
        private int id = NO_ID;

        Tab() {
            // Private constructor
        }

        /** @return This Tab's tag object. */
        @Nullable
        public Object getTag() {
            return tag;
        }

        /**
         * Give this Tab an arbitrary object to hold for later use.
         *
         * @param tag Object to store
         * @return The current instance for call chaining
         */
        @NonNull
        public Tab setTag(@Nullable Object tag) {
            this.tag = tag;
            return this;
        }

        /**
         * Give this tab an id, useful for testing.
         *
         * <p>Do not rely on this if using {@link TabLayout#setupWithViewPager(ViewPager)}
         *
         * @param id unique id for this tab
         */
        @NonNull
        public Tab setId(int id) {
            this.id = id;
            if (view != null) {
                view.setId(id);
            }
            return this;
        }

        /** Returns the id for this tab, {@code View.NO_ID} if not set. */
        public int getId() {
            return id;
        }

        /**
         * Returns the custom view used for this tab.
         *
         * @see #setCustomView(View)
         */
        @Nullable
        public View getCustomView() {
            return customView;
        }

        /**
         * Set a custom view to be used for this tab.
         *
         * <p>If the provided view contains a {@link TextView} with an ID of {@link R.id#text1}
         * then that will be updated with the value given to {@link #setText(CharSequence)}. Similarly,
         * if this layout contains an {@link ImageView} with ID {@link R.id#icon} then it will
         * be updated with the value given to {@link #setIcon(Drawable)}.
         *
         * @param view Custom view to be used as a tab.
         * @return The current instance for call chaining
         */
        @NonNull
        public Tab setCustomView(@Nullable View view) {
            customView = view;
            updateView();
            return this;
        }

        /**
         * Return the icon associated with this tab.
         *
         * @return The tab's icon
         */
        @Nullable
        public Drawable getIcon() {
            return icon;
        }

        /**
         * Return the current position of this tab in the tab layout.
         *
         * @return Current position, or {@link #INVALID_POSITION} if this tab is not currently in the
         *     tab layout.
         */
        public int getPosition() {
            return position;
        }

        void setPosition(int position) {
            this.position = position;
        }

        /**
         * Return the text of this tab.
         *
         * @return The tab's text
         */
        @Nullable
        public CharSequence getText() {
            return text;
        }

        /**
         * Set the icon displayed on this tab.
         *
         * @param icon The drawable to use as an icon
         * @return The current instance for call chaining
         */
        @NonNull
        public Tab setIcon(@Nullable Drawable icon) {
            this.icon = icon;
            if ((parent.tabGravity == GRAVITY_CENTER) || parent.mode == MODE_AUTO) {
                parent.updateTabViews(true);
            }
            updateView();
            return this;
        }

        /**
         * Set the text displayed on this tab. Text may be truncated if there is not room to display the
         * entire string.
         *
         * @param text The text to display
         * @return The current instance for call chaining
         */
        @NonNull
        public Tab setText(@Nullable CharSequence text) {
            if (TextUtils.isEmpty(contentDesc) && !TextUtils.isEmpty(text)) {
                // If no content description has been set, use the text as the content description of the
                // TabView. If the text is null, don't update the content description.
                view.setContentDescription(text);
            }

            this.text = text;
            updateView();
            return this;
        }

        /*
         * Creates an instance of {@link BadgeDrawable} if none exists. Initializes (if needed) and
         * returns the associated instance of {@link BadgeDrawable}.
         *
         * @return an instance of BadgeDrawable associated with {@code Tab}.
         */
        /*@NonNull
        public BadgeDrawable getOrCreateBadge() {
            return view.getOrCreateBadge();
        }*/

        /*
         * Removes the {@link BadgeDrawable}. Do nothing if none exists. Consider changing the
         * visibility of the {@link BadgeDrawable} if you only want to hide it temporarily.
         */
        /*public void removeBadge() {
            view.removeBadge();
        }*/

        /*
         * Returns an instance of {@link BadgeDrawable} associated with this tab, null if none was
         * initialized.
         */
        /*@Nullable
        public BadgeDrawable getBadge() {
            return view.getBadge();
        }*/

        /**
         * Sets the visibility mode for the Labels in this Tab. The valid input options are:
         *
         * <ul>
         *   <li>{@link #TAB_LABEL_VISIBILITY_UNLABELED}: Tabs will appear without labels regardless of
         *       whether text is set.
         *   <li>{@link #TAB_LABEL_VISIBILITY_LABELED}: Tabs will appear labeled if text is set.
         * </ul>
         *
         * @param mode one of {@link #TAB_LABEL_VISIBILITY_UNLABELED} or {@link
         *     #TAB_LABEL_VISIBILITY_LABELED}.
         * @return The current instance for call chaining.
         */
        @NonNull
        public Tab setTabLabelVisibility(@MagicConstant(intValues = {TAB_LABEL_VISIBILITY_UNLABELED,
                TAB_LABEL_VISIBILITY_LABELED}) int mode) {
            this.labelVisibilityMode = mode;
            if ((parent.tabGravity == GRAVITY_CENTER) || parent.mode == MODE_AUTO) {
                parent.updateTabViews(true);
            }
            this.updateView();
            return this;
        }

        /**
         * Gets the visibility mode for the Labels in this Tab.
         *
         * @return the label visibility mode, one of {@link #TAB_LABEL_VISIBILITY_UNLABELED} or {@link
         *     #TAB_LABEL_VISIBILITY_LABELED}.
         * @see #setTabLabelVisibility(int)
         */
        @MagicConstant(intValues = {TAB_LABEL_VISIBILITY_UNLABELED, TAB_LABEL_VISIBILITY_LABELED})
        public int getTabLabelVisibility() {
            return this.labelVisibilityMode;
        }

        /** Select this tab. Only valid if the tab has been added to the tab layout. */
        public void select() {
            if (parent == null) {
                throw new IllegalArgumentException("Tab not attached to a TabLayout");
            }
            parent.selectTab(this);
        }

        /** Returns true if this tab is currently selected. */
        public boolean isSelected() {
            if (parent == null) {
                throw new IllegalArgumentException("Tab not attached to a TabLayout");
            }
            int selectedPosition = parent.getSelectedTabPosition();
            return selectedPosition != INVALID_POSITION && selectedPosition == position;
        }

        /**
         * Set a description of this tab's content for use in accessibility support. If no content
         * description is provided the title will be used.
         *
         * @param contentDesc Description of this tab's content
         * @return The current instance for call chaining
         * @see #getContentDescription()
         */
        @NonNull
        public Tab setContentDescription(@Nullable CharSequence contentDesc) {
            this.contentDesc = contentDesc;
            updateView();
            return this;
        }

        /**
         * Gets a brief description of this tab's content for use in accessibility support.
         *
         * @return Description of this tab's content
         * @see #setContentDescription(CharSequence)
         */
        @Nullable
        public CharSequence getContentDescription() {
            // This returns the view's content description instead of contentDesc because if the title
            // is used as a replacement for the content description, contentDesc will be empty.
            return (view == null) ? null : view.getContentDescription();
        }

        void updateView() {
            if (view != null) {
                view.update();
            }
        }

        void reset() {
            parent = null;
            view = null;
            tag = null;
            icon = null;
            id = NO_ID;
            text = null;
            contentDesc = null;
            position = INVALID_POSITION;
            customView = null;
        }
    }

    /** A {@link LinearLayout} containing {@link Tab} instances for use with {@link TabLayout}. */
    public final class TabView extends LinearLayout {
        private Tab tab;
        private TextView textView;
        private ImageView iconView;
        @Nullable private View badgeAnchorView;
        //@Nullable private BadgeDrawable badgeDrawable;

        @Nullable private View customView;
        @Nullable private TextView customTextView;
        @Nullable private ImageView customIconView;
        @Nullable private Drawable baseBackgroundDrawable;

        private int defaultMaxLines = 2;

        public TabView(@NonNull Context context) {
            super(context);
            updateBackgroundDrawable(context);
            setPaddingRelative(tabPaddingStart, tabPaddingTop, tabPaddingEnd, tabPaddingBottom);
            setGravity(Gravity.CENTER);
            setOrientation(inlineLabel ? HORIZONTAL : VERTICAL);
            setClickable(true);
        }

        private void updateBackgroundDrawable(Context context) {
            /*if (tabBackgroundResId != 0) {
                baseBackgroundDrawable = AppCompatResources.getDrawable(context, tabBackgroundResId);
                if (baseBackgroundDrawable != null && baseBackgroundDrawable.isStateful()) {
                    baseBackgroundDrawable.setState(getDrawableState());
                }
            } else */{
                baseBackgroundDrawable = null;
            }

            Drawable background;
            if (tabRippleColorStateList != null) {
                background =
                        new RippleDrawable(
                                tabRippleColorStateList,
                                null,
                                unboundedRipple ? null : new ColorDrawable(~0));
            } else {
                background = null;
            }
            setBackground(background);
            TabLayout.this.invalidate();
        }

        /**
         * Draw the background drawable specified by tabBackground attribute onto the canvas provided.
         * This method will draw the background to the full bounds of this TabView. We provide a
         * separate method for drawing this background rather than just setting this background on the
         * TabView so that we can control when this background gets drawn. This allows us to draw the
         * tab background underneath the TabLayout selection indicator, and then draw the TabLayout
         * content (icons + labels) on top of the selection indicator.
         *
         * @param canvas canvas to draw the background on
         */
        private void drawBackground(@NonNull Canvas canvas) {
            if (baseBackgroundDrawable != null) {
                baseBackgroundDrawable.setBounds(getLeft(), getTop(), getRight(), getBottom());
                baseBackgroundDrawable.draw(canvas);
            }
        }

        @Override
        protected void drawableStateChanged() {
            super.drawableStateChanged();
            boolean changed = false;
            int[] state = getDrawableState();
            if (baseBackgroundDrawable != null && baseBackgroundDrawable.isStateful()) {
                changed |= baseBackgroundDrawable.setState(state);
            }

            if (changed) {
                invalidate();
                TabLayout.this.invalidate(); // Invalidate TabLayout, which draws mBaseBackgroundDrawable
            }
        }

        @Override
        public boolean performClick() {
            final boolean handled = super.performClick();

            if (tab != null) {
                if (!handled) {
                    playSoundEffect(SoundEffectConstants.CLICK);
                }
                tab.select();
                return true;
            } else {
                return handled;
            }
        }

        @Override
        public void setSelected(final boolean selected) {
            final boolean changed = isSelected() != selected;

            super.setSelected(selected);

            // Always dispatch this to the child views, regardless of whether the value has
            // changed
            if (textView != null) {
                textView.setSelected(selected);
            }
            if (iconView != null) {
                iconView.setSelected(selected);
            }
            if (customView != null) {
                customView.setSelected(selected);
            }
        }

        /*@Override
        public void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            AccessibilityNodeInfoCompat infoCompat = AccessibilityNodeInfoCompat.wrap(info);
            if (badgeDrawable != null && badgeDrawable.isVisible()) {
                infoCompat.setContentDescription(badgeDrawable.getContentDescription());
            }
            infoCompat.setCollectionItemInfo(
                    CollectionItemInfoCompat.obtain(
                            *//* rowIndex= *//* 0,
                            *//* rowSpan= *//* 1,
                            *//* columnIndex= *//* tab.getPosition(),
                            *//* columnSpan= *//* 1,
                            *//* heading= *//* false,
                            *//* selected= *//* isSelected()));
            if (isSelected()) {
                infoCompat.setClickable(false);
                infoCompat.removeAction(AccessibilityActionCompat.ACTION_CLICK);
            }
            infoCompat.setRoleDescription(getResources().getString(R.string.item_view_role_description));
        }*/

        @Override
        public void onMeasure(final int origWidthMeasureSpec, final int origHeightMeasureSpec) {
            final int specWidthSize = MeasureSpec.getSize(origWidthMeasureSpec);
            final int specWidthMode = MeasureSpec.getMode(origWidthMeasureSpec);
            final int maxWidth = getTabMaxWidth();

            final int widthMeasureSpec;
            final int heightMeasureSpec = origHeightMeasureSpec;

            if (maxWidth > 0 && (specWidthMode == MeasureSpec.UNSPECIFIED || specWidthSize > maxWidth)) {
                // If we have a max width and a given spec which is either unspecified or
                // larger than the max width, update the width spec using the same mode
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(tabMaxWidth, MeasureSpec.AT_MOST);
            } else {
                // Else, use the original width spec
                widthMeasureSpec = origWidthMeasureSpec;
            }

            // Now lets measure
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            // We need to switch the text size based on whether the text is spanning 2 lines or not
            if (textView != null) {
                float textSize = tabTextSize;
                int maxLines = defaultMaxLines;

                if (iconView != null && iconView.getVisibility() == VISIBLE) {
                    // If the icon view is being displayed, we limit the text to 1 line
                    maxLines = 1;
                } else if (/*textView != null && */textView.getLineCount() > 1) {
                    // Otherwise when we have text which wraps we reduce the text size
                    textSize = tabTextMultiLineSize;
                }

                final float curTextSize = textView.getTextSize();
                final int curLineCount = textView.getLineCount();
                final int curMaxLines = textView.getMaxLines();

                if (textSize != curTextSize || (curMaxLines >= 0 && maxLines != curMaxLines)) {
                    // We've got a new text size and/or max lines...
                    boolean updateTextView = true;

                    if (mode == MODE_FIXED && textSize > curTextSize && curLineCount == 1) {
                        // If we're in fixed mode, going up in text size and currently have 1 line
                        // then it's very easy to get into an infinite recursion.
                        // To combat that we check to see if the change in text size
                        // will cause a line count change. If so, abort the size change and stick
                        // to the smaller size.
                        final Layout layout = textView.getLayout();
                        if (layout == null
                                || approximateLineWidth(layout, 0, textSize)
                                > getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) {
                            updateTextView = false;
                        }
                    }

                    if (updateTextView) {
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                        textView.setMaxLines(maxLines);
                        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    }
                }
            }
        }

        void setTab(@Nullable final Tab tab) {
            if (tab != this.tab) {
                this.tab = tab;
                update();
            }
        }

        void reset() {
            setTab(null);
            setSelected(false);
        }

        void updateTab() {
            final Tab tab = this.tab;
            final View custom = tab != null ? tab.getCustomView() : null;
            if (custom != null) {
                final ViewParent customParent = custom.getParent();
                if (customParent != this) {
                    if (customParent != null) {
                        ((ViewGroup) customParent).removeView(custom);
                    }
                    if (customView != null) {
                        final ViewParent customViewParent = customView.getParent();
                        if (customViewParent != null) {
                            ((ViewGroup) customViewParent).removeView(customView);
                        }
                    }
                    addView(custom);
                }
                customView = custom;
                if (this.textView != null) {
                    this.textView.setVisibility(GONE);
                }
                if (this.iconView != null) {
                    this.iconView.setVisibility(GONE);
                    this.iconView.setImageDrawable(null);
                }

                customTextView = custom.findViewById(R.id.text1);
                if (customTextView != null) {
                    defaultMaxLines = customTextView.getMaxLines();
                }
                customIconView = custom.findViewById(R.id.icon);
            } else {
                // We do not have a custom view. Remove one if it already exists
                if (customView != null) {
                    removeView(customView);
                    customView = null;
                }
                customTextView = null;
                customIconView = null;
            }

            if (customView == null) {
                // If there isn't a custom view, we'll us our own in-built layouts
                if (this.iconView == null) {
                    inflateAndAddDefaultIconView();
                }
                if (this.textView == null) {
                    inflateAndAddDefaultTextView();
                    defaultMaxLines = this.textView.getMaxLines();
                }
                this.textView.setTextAppearance(tabTextAppearance);
                if (tabTextColors != null) {
                    this.textView.setTextColor(tabTextColors);
                }
                updateTextAndIcon(this.textView, this.iconView, /* addDefaultMargins= */ true);

                tryUpdateBadgeAnchor();
                addOnLayoutChangeListener(iconView);
                addOnLayoutChangeListener(textView);
            } else {
                // Else, we'll see if there is a TextView or ImageView present and update them
                if (customTextView != null || customIconView != null) {
                    updateTextAndIcon(customTextView, customIconView, /* addDefaultMargins= */ false);
                }
            }

            if (tab != null && !TextUtils.isEmpty(tab.contentDesc)) {
                // Only update the TabView's content description from Tab if the Tab's content description
                // has been explicitly set.
                setContentDescription(tab.contentDesc);
            }
        }

        void update() {
            updateTab();
            // Finally update our selected state
            setSelected(this.tab != null && this.tab.isSelected());
        }

        private void inflateAndAddDefaultIconView() {
            this.iconView = new ImageView(getContext());
            iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            int size = dp(24);
            iconView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
            addView(iconView, 0);
        }

        private void inflateAndAddDefaultTextView() {
            this.textView = new TextView(getContext());
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setGravity(Gravity.CENTER);
            textView.setMaxLines(2);
            textView.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
            addView(textView);
        }

        /*
         * Creates an instance of {@link BadgeDrawable} if none exists. Initializes (if needed) and
         * returns the associated instance of {@link BadgeDrawable}.
         *
         * @return an instance of BadgeDrawable associated with {@code Tab}.
         */
        /*@NonNull
        private BadgeDrawable getOrCreateBadge() {
            // Creates a new instance if one is not already initialized for this TabView.
            if (badgeDrawable == null) {
                badgeDrawable = BadgeDrawable.create(getContext());
            }
            tryUpdateBadgeAnchor();
            if (badgeDrawable == null) {
                throw new IllegalStateException("Unable to create badge");
            }
            return badgeDrawable;
        }

        @Nullable
        private BadgeDrawable getBadge() {
            return badgeDrawable;
        }

        private void removeBadge() {
            if (badgeAnchorView != null) {
                tryRemoveBadgeFromAnchor();
            }
            badgeDrawable = null;
        }*/

        private void addOnLayoutChangeListener(@Nullable final View view) {
            if (view == null) {
                return;
            }
            view.addOnLayoutChangeListener(
                    (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                        if (view.getVisibility() == VISIBLE) {
                            tryUpdateBadgeDrawableBounds(view);
                        }
                    });
        }

        private void tryUpdateBadgeAnchor() {
            if (!hasBadgeDrawable()) {
                return;
            }
            if (customView != null) {
                // TODO Support badging on custom tab views.
                tryRemoveBadgeFromAnchor();
            } else {
                if (iconView != null && tab != null && tab.getIcon() != null) {
                    if (badgeAnchorView != iconView) {
                        tryRemoveBadgeFromAnchor();
                        // Anchor badge to icon.
                        tryAttachBadgeToAnchor(iconView);
                    } else {
                        tryUpdateBadgeDrawableBounds(iconView);
                    }
                } else if (textView != null
                        && tab != null
                        && tab.getTabLabelVisibility() == TAB_LABEL_VISIBILITY_LABELED) {
                    if (badgeAnchorView != textView) {
                        tryRemoveBadgeFromAnchor();
                        // Anchor badge to label.
                        tryAttachBadgeToAnchor(textView);
                    } else {
                        tryUpdateBadgeDrawableBounds(textView);
                    }
                } else {
                    tryRemoveBadgeFromAnchor();
                }
            }
        }

        private void tryAttachBadgeToAnchor(@Nullable View anchorView) {
            if (!hasBadgeDrawable()) {
                return;
            }
            if (anchorView != null) {
                clipViewToPaddingForBadge(false);
                //BadgeUtils.attachBadgeDrawable(badgeDrawable, anchorView, null);
                badgeAnchorView = anchorView;
            }
        }

        private void tryRemoveBadgeFromAnchor() {
            if (!hasBadgeDrawable()) {
                return;
            }
            clipViewToPaddingForBadge(true);
            if (badgeAnchorView != null) {
                //BadgeUtils.detachBadgeDrawable(badgeDrawable, badgeAnchorView);
                badgeAnchorView = null;
            }
        }

        private void clipViewToPaddingForBadge(boolean flag) {
            // Avoid clipping a badge if it's displayed.
            // Clip children / view to padding when no badge is displayed.
            setClipChildren(flag);
            setClipToPadding(flag);
            ViewGroup parent = (ViewGroup) getParent();
            if (parent != null) {
                parent.setClipChildren(flag);
                parent.setClipToPadding(flag);
            }
        }

        final void updateOrientation() {
            setOrientation(inlineLabel ? HORIZONTAL : VERTICAL);
            if (customTextView != null || customIconView != null) {
                updateTextAndIcon(customTextView, customIconView, /* addDefaultMargins= */ false);
            } else {
                updateTextAndIcon(textView, iconView, /* addDefaultMargins= */ true);
            }
        }

        private void updateTextAndIcon(
                @Nullable final TextView textView,
                @Nullable final ImageView iconView,
                final boolean addDefaultMargins) {
            final Drawable icon =
                    (tab != null && tab.getIcon() != null)
                            ? tab.getIcon().mutate()
                            : null;
            if (icon != null) {
                icon.setTintList(tabIconTint);
                if (tabIconTintMode != null) {
                    icon.setTintBlendMode(tabIconTintMode);
                }
            }

            final CharSequence text = tab != null ? tab.getText() : null;

            if (iconView != null) {
                if (icon != null) {
                    iconView.setImageDrawable(icon);
                    iconView.setVisibility(VISIBLE);
                    setVisibility(VISIBLE);
                } else {
                    iconView.setVisibility(GONE);
                    iconView.setImageDrawable(null);
                }
            }

            final boolean hasText = !TextUtils.isEmpty(text);
            final boolean showingText;
            if (textView != null) {
                showingText = hasText && tab.labelVisibilityMode == TAB_LABEL_VISIBILITY_LABELED;
                textView.setText(hasText ? text : "");
                textView.setVisibility(showingText ? VISIBLE : GONE);

                if (hasText) {
                    setVisibility(VISIBLE);
                }
            } else {
                showingText = false;
            }

            if (addDefaultMargins && iconView != null) {
                MarginLayoutParams lp = ((MarginLayoutParams) iconView.getLayoutParams());
                int iconMargin = 0;
                if (showingText && iconView.getVisibility() == VISIBLE) {
                    // If we're showing both text and icon, add some margin to the icon
                    iconMargin = dp(DEFAULT_GAP_TEXT_ICON);
                }
                if (inlineLabel) {
                    if (iconMargin != lp.getMarginEnd()) {
                        lp.setMarginEnd(iconMargin);
                        lp.bottomMargin = 0;
                        // Calls resolveLayoutParams(), necessary for layout direction
                        iconView.setLayoutParams(lp);
                        iconView.requestLayout();
                    }
                }/* else {
                    if (iconMargin != lp.bottomMargin) {
                        lp.bottomMargin = iconMargin;
                        lp.setMarginEnd(0);
                        // Calls resolveLayoutParams(), necessary for layout direction
                        iconView.setLayoutParams(lp);
                        iconView.requestLayout();
                    }
                }*/
                // No vertical gap between icon and text
            }

            //XXX: Currently we don't set tooltip, this may be needed only when title is ellipsized
            /*final CharSequence contentDesc = tab != null ? tab.contentDesc : null;
            setTooltipText(hasText ? text : contentDesc);*/
        }

        private void tryUpdateBadgeDrawableBounds(@NonNull View anchor) {
            // Check that this view is the badge's current anchor view.
            /*if (hasBadgeDrawable() && anchor == badgeAnchorView) {
                BadgeUtils.setBadgeDrawableBounds(badgeDrawable, anchor, null);
            }*/
        }

        private boolean hasBadgeDrawable() {
            return false;//badgeDrawable != null;
        }

        /**
         * Calculates the width of the TabView's content.
         *
         * @return Width of the tab label, if present, or the width of the tab icon, if present. If tabs
         *     is in inline mode, returns the sum of both the icon and tab label widths.
         */
        int getContentWidth() {
            boolean initialized = false;
            int left = 0;
            int right = 0;

            for (View view : new View[] {textView, iconView, customView}) {
                if (view != null && view.getVisibility() == View.VISIBLE) {
                    left = initialized ? Math.min(left, view.getLeft()) : view.getLeft();
                    right = initialized ? Math.max(right, view.getRight()) : view.getRight();
                    initialized = true;
                }
            }

            return right - left;
        }

        /**
         * Calculates the height of the TabView's content.
         *
         * @return Height of the tab label, if present, or the height of the tab icon, if present. If
         *     the tab contains both a label and icon, the combined will be returned.
         */
        int getContentHeight() {
            boolean initialized = false;
            int top = 0;
            int bottom = 0;

            for (View view : new View[] {textView, iconView, customView}) {
                if (view != null && view.getVisibility() == View.VISIBLE) {
                    top = initialized ? Math.min(top, view.getTop()) : view.getTop();
                    bottom = initialized ? Math.max(bottom, view.getBottom()) : view.getBottom();
                    initialized = true;
                }
            }

            return bottom - top;
        }

        @Nullable
        public Tab getTab() {
            return tab;
        }

        /** Approximates a given lines width with the new provided text size. */
        private float approximateLineWidth(@NonNull Layout layout, int line, float textSize) {
            return layout.getLineWidth(line) * (textSize / layout.getPaint().getTextSize());
        }
    }

    class SlidingTabIndicator extends LinearLayout {
        ValueAnimator indicatorAnimator;

        SlidingTabIndicator(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        void setSelectedIndicatorHeight(int height) {
            Rect bounds = tabSelectedIndicator.getBounds();
            tabSelectedIndicator.setBounds(bounds.left, 0, bounds.right, height);
            this.requestLayout();
        }

        boolean childrenNeedLayout() {
            for (int i = 0, z = getChildCount(); i < z; i++) {
                final View child = getChildAt(i);
                if (child.getWidth() <= 0) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Set the indicator position based on an offset between two adjacent tabs.
         *
         * @param position Position index of the first tab (with less index) currently being displayed.
         *     Tab position+1 will be visible if positionOffset is nonzero.
         * @param positionOffset Value from [0, 1) indicating the offset from the tab at position.
         */
        void setIndicatorPositionFromTabPosition(int position, float positionOffset) {
            // Since we are tweening the indicator in between the position and position+positionOffset,
            // we set the indicator position to whichever is closer.
            indicatorPosition = Math.round(position + positionOffset);
            if (indicatorAnimator != null && indicatorAnimator.isRunning()) {
                indicatorAnimator.cancel();
            }

            // The title view refers to the one indicated when offset is 0.
            final View firstTitle = getChildAt(position);
            // The title view refers to the one indicated when offset is 1.
            final View nextTitle = getChildAt(position + 1);

            tweenIndicatorPosition(firstTitle, nextTitle, positionOffset);
        }

        @Override
        protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
                // HorizontalScrollView will first measure use with UNSPECIFIED, and then with
                // EXACTLY. Ignore the first call since anything we do will be overwritten anyway
                return;
            }

            // GRAVITY_CENTER will make all tabs the same width as the largest tab, and center them in the
            // SlidingTabIndicator's width (with a "gutter" of padding on either side). If the Tabs do not
            // fit in the SlidingTabIndicator, then fall back to GRAVITY_FILL behavior.
            if ((tabGravity == GRAVITY_CENTER) || mode == MODE_AUTO) {
                final int count = getChildCount();

                // First we'll find the widest tab
                int largestTabWidth = 0;
                for (int i = 0, z = count; i < z; i++) {
                    View child = getChildAt(i);
                    if (child.getVisibility() == VISIBLE) {
                        largestTabWidth = Math.max(largestTabWidth, child.getMeasuredWidth());
                    }
                }

                if (largestTabWidth <= 0) {
                    // If we don't have a largest child yet, skip until the next measure pass
                    return;
                }

                final int gutter = dp(FIXED_WRAP_GUTTER_MIN);
                boolean remeasure = false;

                if (largestTabWidth * count <= getMeasuredWidth() - gutter * 2) {
                    // If the tabs fit within our width minus gutters, we will set all tabs to have
                    // the same width
                    for (int i = 0; i < count; i++) {
                        final LinearLayout.LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
                        if (lp.width != largestTabWidth || lp.weight != 0) {
                            lp.width = largestTabWidth;
                            lp.weight = 0;
                            remeasure = true;
                        }
                    }
                } else {
                    // If the tabs will wrap to be larger than the width minus gutters, we need
                    // to switch to GRAVITY_FILL.
                    // TODO (b/129799806): This overrides the user TabGravity setting.
                    tabGravity = GRAVITY_FILL;
                    updateTabViews(false);
                    remeasure = true;
                }

                if (remeasure) {
                    // Now re-measure after our changes
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                }
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);

            if (indicatorAnimator != null && indicatorAnimator.isRunning()) {
                // It's possible that the tabs' layout is modified while the indicator is animating (ex. a
                // new tab is added, or a tab is removed in onTabSelected). This would change the target end
                // position of the indicator, since the tab widths are different. We need to modify the
                // animation's updateListener to pick up the new target positions.
                updateOrRecreateIndicatorAnimation(
                        /* recreateAnimation= */ false, getSelectedTabPosition(), /* duration= */ -1);
            } else {
                // If we've been laid out, update the indicator position
                jumpIndicatorToIndicatorPosition();
            }
        }

        /**
         * Immediately update the indicator position to the specified position, unless we are mid-scroll
         * in a viewpager.
         */
        private void jumpIndicatorToPosition(int position) {
            // Don't update the indicator position if the scroll state is not idle, and the indicator
            // is drawn.
            if (viewPagerScrollState != SCROLL_STATE_IDLE
                    && !(getTabSelectedIndicator().getBounds().left == -1
                    && getTabSelectedIndicator().getBounds().right == -1)) {
                return;
            }
            final View currentView = getChildAt(position);
            tabIndicatorInterpolator.setIndicatorBoundsForTab(
                    TabLayout.this, currentView, tabSelectedIndicator);
            indicatorPosition = position;
        }

        /** Immediately update the indicator position to the currently selected position. */
        private void jumpIndicatorToSelectedPosition() {
            jumpIndicatorToPosition(getSelectedTabPosition());
        }

        /** Immediately update the indicator position to the current indicator position. */
        private void jumpIndicatorToIndicatorPosition() {
            // If indicator position has not yet been set, set indicator to the selected tab position.
            if (indicatorPosition == -1) {
                indicatorPosition = getSelectedTabPosition();
            }
            jumpIndicatorToPosition(indicatorPosition);
        }

        /**
         * Update the position of the indicator by tweening between the currently selected tab and the
         * destination tab.
         *
         * <p>This method is called for each frame when either animating the indicator between
         * destinations or driving an animation through gesture, such as with a viewpager.
         *
         * @param startTitle The tab which should be selected (as marked by the indicator), when
         *     fraction is 0.0.
         * @param endTitle The tab which should be selected (as marked by the indicator), when fraction
         *     is 1.0.
         * @param fraction A value between 0.0 and 1.0 that indicates how far between currentTitle and
         *     endTitle the indicator should be drawn. e.g. If a viewpager attached to this TabLayout is
         *     currently half way slid between page 0 and page 1, fraction will be 0.5.
         */
        private void tweenIndicatorPosition(View startTitle, View endTitle, float fraction) {
            boolean hasVisibleTitle = startTitle != null && startTitle.getWidth() > 0;
            if (hasVisibleTitle) {
                tabIndicatorInterpolator.updateIndicatorForOffset(
                        TabLayout.this, startTitle, endTitle, fraction, tabSelectedIndicator);
            } else {
                // Hide the indicator by setting the drawable's width to 0 and off screen.
                tabSelectedIndicator.setBounds(
                        -1, tabSelectedIndicator.getBounds().top, -1, tabSelectedIndicator.getBounds().bottom);
            }

            postInvalidateOnAnimation();
        }

        /**
         * Animate the position of the indicator from its current position to a new position.
         *
         * <p>This is typically used when a tab destination is tapped. If the indicator should be moved
         * as a result of a gesture, see {@link #setIndicatorPositionFromTabPosition(int, float)}.
         *
         * @param position The new position to animate the indicator to.
         * @param duration The duration over which the animation should take place.
         */
        void animateIndicatorToPosition(final int position, int duration) {
            if (indicatorAnimator != null
                    && indicatorAnimator.isRunning()
                    && indicatorPosition != position) {
                indicatorAnimator.cancel();
            }

            updateOrRecreateIndicatorAnimation(/* recreateAnimation= */ true, position, duration);
        }

        /**
         * Animate the position of the indicator from its current position to a new position.
         *
         * @param recreateAnimation Whether a currently running animator should be re-targeted to move
         *     the indicator to it's new position.
         * @param position The new position to animate the indicator to.
         * @param duration The duration over which the animation should take place.
         */
        private void updateOrRecreateIndicatorAnimation(
                boolean recreateAnimation, final int position, int duration) {
            // If the indicator position is already the target position, we don't need to update the
            // indicator animation because nothing has changed.
            if (indicatorPosition == position) {
                return;
            }
            final View currentView = getChildAt(getSelectedTabPosition());
            final View targetView = getChildAt(position);
            if (targetView == null) {
                // If we don't have a view, just update the position now and return
                jumpIndicatorToSelectedPosition();
                return;
            }
            indicatorPosition = position;

            // Create the update listener with the new target indicator positions. If we're not recreating
            // then animationStartLeft/Right will be the same as when the previous animator was created.
            ValueAnimator.AnimatorUpdateListener updateListener =
                    animation -> tweenIndicatorPosition(currentView, targetView, animation.getAnimatedFraction());

            if (recreateAnimation) {
                // Create & start a new indicatorAnimator.
                ValueAnimator animator = indicatorAnimator = ValueAnimator.ofFloat(0F, 1F);
                animator.setInterpolator(tabIndicatorTimeInterpolator);
                animator.setDuration(duration);
                animator.addUpdateListener(updateListener);
                animator.start();
            } else {
                // Reuse the existing animator. Updating the listener only modifies the target positions.
                indicatorAnimator.removeAllUpdateListeners();
                indicatorAnimator.addUpdateListener(updateListener);
            }
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            int indicatorHeight = tabSelectedIndicator.getBounds().height();
            if (indicatorHeight < 0) {
                indicatorHeight = tabSelectedIndicator.getIntrinsicHeight();
            }

            int indicatorTop = 0;
            int indicatorBottom = 0;

            switch (tabIndicatorGravity) {
                case INDICATOR_GRAVITY_BOTTOM:
                    indicatorTop = getHeight() - indicatorHeight;
                    indicatorBottom = getHeight();
                    break;
                case INDICATOR_GRAVITY_CENTER:
                    indicatorTop = (getHeight() - indicatorHeight) / 2;
                    indicatorBottom = (getHeight() + indicatorHeight) / 2;
                    break;
                case INDICATOR_GRAVITY_TOP:
                    indicatorTop = 0;
                    indicatorBottom = indicatorHeight;
                    break;
                case INDICATOR_GRAVITY_STRETCH:
                    indicatorTop = 0;
                    indicatorBottom = getHeight();
                    break;
                default:
                    break;
            }

            // Ensure the drawable actually has a width and is worth drawing
            if (tabSelectedIndicator.getBounds().width() > 0) {
                // Use the left and right bounds of the drawable, as set by the indicator interpolator.
                // Update the top and bottom to respect the indicator gravity property.
                Rect indicatorBounds = tabSelectedIndicator.getBounds();
                tabSelectedIndicator.setBounds(
                        indicatorBounds.left, indicatorTop, indicatorBounds.right, indicatorBottom);
                tabSelectedIndicator.draw(canvas);
            }

            // Draw the tab item contents (icon and label) on top of the background + indicator layers
            super.draw(canvas);
        }
    }

    @NonNull
    private static ColorStateList createColorStateList(int defaultColor, int selectedColor) {
        final int[][] states = new int[2][];
        final int[] colors = new int[2];
        int i = 0;

        states[i] = SELECTED_STATE_SET;
        colors[i] = selectedColor;
        i++;

        // Default enabled state
        states[i] = EMPTY_STATE_SET;
        colors[i] = defaultColor;
        i++;

        return new ColorStateList(states, colors);
    }

    @Dimension(unit = Dimension.DP)
    private int getDefaultHeight() {
        boolean hasIconAndText = false;
        for (int i = 0, count = tabs.size(); i < count; i++) {
            Tab tab = tabs.get(i);
            if (tab != null && tab.getIcon() != null && !TextUtils.isEmpty(tab.getText())) {
                hasIconAndText = true;
                break;
            }
        }
        return (hasIconAndText && !inlineLabel) ? DEFAULT_HEIGHT_WITH_TEXT_ICON : DEFAULT_HEIGHT;
    }

    private int getTabMinWidth() {
        if (requestedTabMinWidth != INVALID_WIDTH) {
            // If we have been given a min width, use it
            return requestedTabMinWidth;
        }
        // Else, we'll use the default value
        return (mode == MODE_SCROLLABLE || mode == MODE_AUTO) ? scrollableTabMinWidth : 0;
    }

    /*@Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        // We don't care about the layout params of any views added to us, since we don't actually
        // add them. The only view we add is the SlidingTabStrip, which is done manually.
        // We return the default layout params so that we don't blow up if we're given a TabItem
        // without android:layout_* values.
        return generateDefaultLayoutParams();
    }*/

    int getTabMaxWidth() {
        return tabMaxWidth;
    }

    /**
     * A {@link ViewPager.OnPageChangeListener} class which contains the necessary calls back to the
     * provided {@link TabLayout} so that the tab position is kept in sync.
     *
     * <p>This class stores the provided TabLayout weakly, meaning that you can use {@link
     * ViewPager#addOnPageChangeListener(ViewPager.OnPageChangeListener)
     * addOnPageChangeListener(OnPageChangeListener)} without removing the listener and not cause a
     * leak.
     */
    public static class TabLayoutOnPageChangeListener implements ViewPager.OnPageChangeListener {
        @NonNull private final WeakReference<TabLayout> tabLayoutRef;
        private int previousScrollState;
        private int scrollState;

        public TabLayoutOnPageChangeListener(TabLayout tabLayout) {
            tabLayoutRef = new WeakReference<>(tabLayout);
        }

        @Override
        public void onPageScrollStateChanged(final int state) {
            previousScrollState = scrollState;
            scrollState = state;
            TabLayout tabLayout = tabLayoutRef.get();
            if (tabLayout != null) {
                tabLayout.updateViewPagerScrollState(scrollState);
            }
        }

        @Override
        public void onPageScrolled(
                final int position, final float positionOffset, final int positionOffsetPixels) {
            final TabLayout tabLayout = tabLayoutRef.get();
            if (tabLayout != null) {
                // Only update the tab view selection if we're not settling, or we are settling after
                // being dragged
                final boolean updateSelectedTabView =
                        scrollState != SCROLL_STATE_SETTLING || previousScrollState == SCROLL_STATE_DRAGGING;
                // Update the indicator if we're not settling after being idle. This is caused
                // from a setCurrentItem() call and will be handled by an animation from
                // onPageSelected() instead.
                final boolean updateIndicator =
                        !(scrollState == SCROLL_STATE_SETTLING && previousScrollState == SCROLL_STATE_IDLE);
                tabLayout.setScrollPosition(
                        position, positionOffset, updateSelectedTabView, updateIndicator, false);
            }
        }

        @Override
        public void onPageSelected(final int position) {
            final TabLayout tabLayout = tabLayoutRef.get();
            if (tabLayout != null
                    && tabLayout.getSelectedTabPosition() != position
                    && position < tabLayout.getTabCount()) {
                // Select the tab, only updating the indicator if we're not being dragged/settled
                // (since onPageScrolled will handle that).
                final boolean updateIndicator =
                        scrollState == SCROLL_STATE_IDLE
                                || (scrollState == SCROLL_STATE_SETTLING
                                && previousScrollState == SCROLL_STATE_IDLE);
                tabLayout.selectTab(tabLayout.getTabAt(position), updateIndicator);
            }
        }

        void reset() {
            previousScrollState = scrollState = SCROLL_STATE_IDLE;
        }
    }

    /**
     * A {@link TabLayout.OnTabSelectedListener} class which contains the necessary calls back to the
     * provided {@link ViewPager} so that the tab position is kept in sync.
     */
    public static class ViewPagerOnTabSelectedListener implements TabLayout.OnTabSelectedListener {
        private final ViewPager viewPager;

        public ViewPagerOnTabSelectedListener(ViewPager viewPager) {
            this.viewPager = viewPager;
        }

        @Override
        public void onTabSelected(@NonNull TabLayout.Tab tab) {
            viewPager.setCurrentItem(tab.getPosition());
        }
    }

    private class PagerAdapterObserver implements DataSetObserver {
        PagerAdapterObserver() {}

        @Override
        public void onChanged() {
            populateFromPagerAdapter();
        }

        @Override
        public void onInvalidated() {
            populateFromPagerAdapter();
        }
    }

    private class AdapterChangeListener implements ViewPager.OnAdapterChangeListener {
        private boolean autoRefresh;

        AdapterChangeListener() {}

        @Override
        public void onAdapterChanged(
                @NonNull ViewPager viewPager,
                @Nullable PagerAdapter oldAdapter,
                @Nullable PagerAdapter newAdapter) {
            if (TabLayout.this.viewPager == viewPager) {
                setPagerAdapter(newAdapter, autoRefresh);
            }
        }

        void setAutoRefresh(boolean autoRefresh) {
            this.autoRefresh = autoRefresh;
        }
    }

    /**
     * A class used to manipulate the {@link SlidingTabIndicator}'s indicator {@link Drawable} at any
     * point at or between tabs.
     *
     * <p>By default, this class will size the indicator according to {@link
     * TabLayout#isTabIndicatorFullWidth()} and linearly move the indicator between tabs.
     *
     * <p>Subclasses can override {@link #setIndicatorBoundsForTab(TabLayout, View, Drawable)} and
     * {@link #updateIndicatorForOffset(TabLayout, View, View, float, Drawable)} (TabLayout, View, View,
     * float, Drawable)} to define how the indicator should be drawn for a single tab or at any point
     * between two tabs.
     *
     * <p>Additionally, subclasses can use the provided helpers {@link
     * #calculateIndicatorWidthForTab(TabLayout, View)} and {@link
     * #calculateTabViewContentBounds(TabView, int)} to capture the bounds of the tab or tab's content.
     */
    static class TabIndicatorInterpolator {

        @Dimension(unit = Dimension.DP)
        private static final int MIN_INDICATOR_WIDTH = 24;

        /**
         * A helper method that calculates the bounds of a {@link TabView}'s content.
         *
         * <p>For width, if only text label is present, calculates the width of the text label. If only
         * icon is present, calculates the width of the icon. If both are present, the text label bounds
         * take precedence. If both are present and inline mode is enabled, the sum of the bounds of the
         * both the text label and icon are calculated. If neither are present or if the calculated
         * difference between the left and right bounds is less than 24dp, then left and right bounds are
         * adjusted such that the difference between them is equal to 24dp.
         *
         * <p>For height, this method calculates the combined height of the icon (if present) and label
         * (if present).
         *
         * @param tabView {@link TabView} for which to calculate left and right content bounds.
         * @param minWidth the min width between the returned RectF's left and right bounds. Useful if
         *     enforcing a min width of the indicator.
         */
        @NonNull
        static RectF calculateTabViewContentBounds(
                @NonNull TabView tabView, @Dimension(unit = Dimension.DP) int minWidth) {
            int tabViewContentWidth = tabView.getContentWidth();
            int tabViewContentHeight = tabView.getContentHeight();
            int minWidthPx = tabView.dp(minWidth);

            if (tabViewContentWidth < minWidthPx) {
                tabViewContentWidth = minWidthPx;
            }

            int tabViewCenterX = (tabView.getLeft() + tabView.getRight()) / 2;
            int tabViewCenterY = (tabView.getTop() + tabView.getBottom()) / 2;
            int contentLeftBounds = tabViewCenterX - (tabViewContentWidth / 2);
            int contentTopBounds = tabViewCenterY - (tabViewContentHeight / 2);
            int contentRightBounds = tabViewCenterX + (tabViewContentWidth / 2);
            int contentBottomBounds = tabViewCenterY + (tabViewCenterX / 2);

            return new RectF(contentLeftBounds, contentTopBounds, contentRightBounds, contentBottomBounds);
        }

        /**
         * A helper method to calculate the left and right bounds of an indicator when {@code tab} is
         * selected.
         *
         * <p>This method accounts for {@link TabLayout#isTabIndicatorFullWidth()}'s value. If true, the
         * returned left and right bounds will span the full width of {@code tab}. If false, the returned
         * bounds will span the width of the {@code tab}'s content.
         *
         * @param tabLayout The tab's parent {@link TabLayout}
         * @param tab The view of the tab under which the indicator will be positioned
         * @return A {@link RectF} containing the left and right bounds that the indicator should span
         *     when {@code tab} is selected.
         */
        @NonNull
        static RectF calculateIndicatorWidthForTab(TabLayout tabLayout, @Nullable View tab) {
            if (tab == null) {
                return new RectF();
            }

            // If the indicator should fit to the tab's content, calculate the content's width
            if (!tabLayout.isTabIndicatorFullWidth() && tab instanceof TabView) {
                return calculateTabViewContentBounds((TabView) tab, MIN_INDICATOR_WIDTH);
            }

            // Return the entire width of the tab
            return new RectF(tab.getLeft(), tab.getTop(), tab.getRight(), tab.getBottom());
        }

        /**
         * Called whenever {@code indicator} should be drawn to show the given {@code tab} as selected.
         *
         * <p>This method should update the bounds of indicator to be correctly positioned to indicate
         * {@code tab} as selected.
         *
         * @param tabLayout The {@link TabLayout} parent of the tab and indicator being drawn.
         * @param tab The tab that should be marked as selected
         * @param indicator The drawable to be drawn to indicate the selected tab. Update the drawable's
         *     bounds, color, etc to mark the given tab as selected.
         */
        void setIndicatorBoundsForTab(TabLayout tabLayout, View tab, @NonNull Drawable indicator) {
            RectF startIndicator = calculateIndicatorWidthForTab(tabLayout, tab);
            indicator.setBounds(
                    (int) startIndicator.left,
                    indicator.getBounds().top,
                    (int) startIndicator.right,
                    indicator.getBounds().bottom);
        }

        /**
         * Called whenever the {@code indicator} should be drawn between two destinations and the {@link
         * Drawable}'s bounds should be changed. When {@code offset} is 0.0, the tab {@code indicator}
         * should indicate that the {@code startTitle} tab is selected. When {@code offset} is 1.0, the
         * tab {@code indicator} should indicate that the {@code endTitle} tab is selected. When offset is
         * between 0.0 and 1.0, the {@code indicator} is moving between the startTitle and endTitle and
         * the indicator should reflect this movement.
         *
         * <p>By default, this class will move the indicator linearly between tab destinations.
         *
         * @param tabLayout The TabLayout parent of the indicator being drawn.
         * @param startTitle The title that should be indicated as selected when offset is 0.0.
         * @param endTitle The title that should be indicated as selected when offset is 1.0.
         * @param offset The fraction between startTitle and endTitle where the indicator is for a given
         *     frame
         * @param indicator The drawable to be drawn to indicate the selected tab. Update the drawable's
         *     bounds, color, etc as {@code offset} changes to show the indicator in the correct position.
         */
        void updateIndicatorForOffset(
                TabLayout tabLayout,
                View startTitle,
                View endTitle,
                @FloatRange(from = 0.0, to = 1.0) float offset,
                @NonNull Drawable indicator) {
            RectF startIndicator = calculateIndicatorWidthForTab(tabLayout, startTitle);
            // Linearly interpolate the indicator's position, using it's left and right bounds, between the
            // two destinations.
            RectF endIndicator = calculateIndicatorWidthForTab(tabLayout, endTitle);
            indicator.setBounds(
                    MotionEasingUtils.lerp((int) startIndicator.left, (int) endIndicator.left, offset),
                    indicator.getBounds().top,
                    MotionEasingUtils.lerp((int) startIndicator.right, (int) endIndicator.right, offset),
                    indicator.getBounds().bottom);
        }
    }

    /**
     * An implementation of {@link TabIndicatorInterpolator} that sequentially fades out the selected
     * tab indicator from the current destination and fades it back in at its new destination.
     */
    static class FadeTabIndicatorInterpolator extends TabIndicatorInterpolator {

        // When the indicator will disappear from the current tab and begin to reappear at the newly
        // selected tab.
        private static final float FADE_THRESHOLD = 0.5F;

        @Override
        void updateIndicatorForOffset(
                TabLayout tabLayout,
                View startTitle,
                View endTitle,
                float offset,
                @NonNull Drawable indicator) {
            View tab = offset < FADE_THRESHOLD ? startTitle : endTitle;
            RectF bounds = calculateIndicatorWidthForTab(tabLayout, tab);
            float alpha = offset < FADE_THRESHOLD
                    ? MotionEasingUtils.lerp(1F, 0F, 0F, FADE_THRESHOLD, offset)
                    : MotionEasingUtils.lerp(0F, 1F, FADE_THRESHOLD, 1F, offset);

            indicator.setBounds(
                    (int) bounds.left,
                    indicator.getBounds().top,
                    (int) bounds.right,
                    indicator.getBounds().bottom
            );
            indicator.setAlpha((int) (alpha * 255F));
        }
    }

    /**
     * An implementation of {@link TabIndicatorInterpolator} that translates the left and right sides of
     * a selected tab indicator independently to make the indicator grow and shrink between
     * destinations.
     */
    static class ElasticTabIndicatorInterpolator extends TabIndicatorInterpolator {

        /** Fit a linear 0F - 1F curve to an ease out sine (decelerating) curve. */
        private static float decInterp(@FloatRange(from = 0.0, to = 1.0) float fraction) {
            // Ease out sine
            return (float) Math.sin((fraction * Math.PI) / 2.0);
        }

        /** Fit a linear 0F - 1F curve to an ease in sine (accelerating) curve. */
        private static float accInterp(@FloatRange(from = 0.0, to = 1.0) float fraction) {
            // Ease in sine
            return (float) (1.0 - Math.cos((fraction * Math.PI) / 2.0));
        }

        @Override
        void updateIndicatorForOffset(
                TabLayout tabLayout,
                View startTitle,
                View endTitle,
                float offset,
                @NonNull Drawable indicator) {
            // The indicator should be positioned somewhere between start and end title. Override the
            // super implementation and adjust the indicator's left and right bounds independently.
            RectF startIndicator = calculateIndicatorWidthForTab(tabLayout, startTitle);
            RectF endIndicator = calculateIndicatorWidthForTab(tabLayout, endTitle);

            float leftFraction;
            float rightFraction;

            final boolean isMovingRight = startIndicator.left < endIndicator.left;
            // If the selection indicator should grow and shrink during the animation, interpolate
            // the left and right bounds of the indicator using separate easing functions.
            // The side in which the indicator is moving should always be the accelerating
            // side.
            if (isMovingRight) {
                leftFraction = accInterp(offset);
                rightFraction = decInterp(offset);
            } else {
                leftFraction = decInterp(offset);
                rightFraction = accInterp(offset);
            }
            indicator.setBounds(
                    MotionEasingUtils.lerp((int) startIndicator.left, (int) endIndicator.left, leftFraction),
                    indicator.getBounds().top,
                    MotionEasingUtils.lerp((int) startIndicator.right, (int) endIndicator.right, rightFraction),
                    indicator.getBounds().bottom);
        }
    }
}
