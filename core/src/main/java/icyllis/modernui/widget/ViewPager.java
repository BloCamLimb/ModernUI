/*
 * Modern UI.
 * Copyright (C) 2023-2025 BloCamLimb. All rights reserved.
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
 *   Copyright (C) 2018 The Android Open Source Project
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

import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.CallSuper;
import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.core.Core;
import icyllis.modernui.graphics.BlendMode;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.util.AttributeSet;
import icyllis.modernui.util.DataSetObserver;
import icyllis.modernui.view.*;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static icyllis.modernui.util.Log.LOGGER;

/**
 * Layout manager that allows the user to flip left and right
 * through pages of data.<br>You supply an implementation of a
 * {@link PagerAdapter} to generate the pages that the view shows.
 *
 * <p>Views which are annotated with the {@link DecorView} annotation are treated as
 * part of the view pagers 'decor'.
 */
// Modified from Android, fixed some bugs, added RTL support
@SuppressWarnings("ForLoopReplaceableByForEach")
public class ViewPager extends ViewGroup {

    private static final Marker MARKER = MarkerFactory.getMarker("ViewPager");

    private static final int DEFAULT_OFFSCREEN_PAGES = 1;
    private static final int MAX_SETTLE_DURATION = 600; // ms
    private static final int MIN_DISTANCE_FOR_FLING = 25; // dips

    private static final int DEFAULT_GUTTER_SIZE = 16; // dips

    private static final int MIN_FLING_VELOCITY = 400; // dips

    /**
     * Used to track what the expected number of items in the adapter should be.
     * If the app changes this when we don't expect it, we'll throw a big obnoxious exception.
     */
    private int mExpectedAdapterCount;

    static class ItemInfo implements Comparable<ItemInfo> {
        Object object;
        int position;
        boolean scrolling;
        float widthFactor;
        float offset;

        @Override
        public int compareTo(@NonNull ItemInfo o) {
            return position - o.position;
        }
    }

    private final ArrayList<ItemInfo> mItems = new ArrayList<>();
    private final ItemInfo mTempItem = new ItemInfo();

    private final Rect mTempRect = new Rect();

    PagerAdapter mAdapter;
    int mCurItem;   // Index of currently displayed page.

    private final OverScroller mScroller;
    private boolean mIsScrollStarted;

    private PagerObserver mObserver;

    private int mPageMargin;
    private Drawable mMarginDrawable;
    private int mTopPageBounds;
    private int mBottomPageBounds;

    // Offsets of the first and last items, if known.
    // Set during population, used to determine if we are at the beginning
    // or end of the pager data set during touch scrolling.
    private float mFirstOffset = -Float.MAX_VALUE;
    private float mLastOffset = Float.MAX_VALUE;

    private boolean mInLayout;

    private boolean mPopulatePending;
    private int mOffscreenPageLimit = DEFAULT_OFFSCREEN_PAGES;

    private boolean mIsBeingDragged;
    private boolean mIsUnableToDrag;
    private final int mDefaultGutterSize;
    private int mGutterSize;
    private final int mTouchSlop;

    private boolean mDragInGutterEnabled = true;

    /**
     * Position of the last motion event.
     */
    private float mLastMotionX;
    private float mLastMotionY;
    private float mInitialMotionX;
    private float mInitialMotionY;
    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;
    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;
    private final int mMinimumVelocity;
    private final int mMaximumVelocity;
    private final int mFlingDistance;
    private final int mCloseEnough;

    // If the pager is at least this close to its final position, complete the scroll
    // on touch down and let the user interact with the content inside instead of
    // "catching" the flinging pager.
    private static final int CLOSE_ENOUGH = 2; // dp

    private boolean mFakeDragging;
    private long mFakeDragBeginTime;

    private final EdgeEffect mLeftEdge;
    private final EdgeEffect mRightEdge;

    private boolean mFirstLayout = true;
    private boolean mCalledSuper;
    private int mDecorChildCount;

    private List<OnPageChangeListener> mOnPageChangeListeners;
    private List<OnAdapterChangeListener> mAdapterChangeListeners;
    private PageTransformer mPageTransformer;
    private int mPageTransformerLayerType;

    private static final int DRAW_ORDER_DEFAULT = 0;
    private static final int DRAW_ORDER_FORWARD = 1;
    private static final int DRAW_ORDER_REVERSE = 2;
    private int mDrawingOrder;
    private ArrayList<View> mDrawingOrderedChildren;
    private static final Comparator<View> sPositionComparator = (View lhs, View rhs) -> {
        final LayoutParams llp = (LayoutParams) lhs.getLayoutParams();
        final LayoutParams rlp = (LayoutParams) rhs.getLayoutParams();
        if (llp.isDecor != rlp.isDecor) {
            return llp.isDecor ? 1 : -1;
        }
        return llp.position - rlp.position;
    };

    /**
     * Indicates that the pager is in an idle, settled state. The current page
     * is fully in view and no animation is in progress.
     */
    public static final int SCROLL_STATE_IDLE = 0;

    /**
     * Indicates that the pager is currently being dragged by the user.
     */
    public static final int SCROLL_STATE_DRAGGING = 1;

    /**
     * Indicates that the pager is in the process of settling to a final position.
     */
    public static final int SCROLL_STATE_SETTLING = 2;

    private final Runnable mEndScrollRunnable = () -> {
        setScrollState(SCROLL_STATE_IDLE);
        populate();
    };

    private int mScrollState = SCROLL_STATE_IDLE;

    private int mLayoutDirection = LAYOUT_DIRECTION_LTR;

    /**
     * Callback interface for responding to changing state of the selected page.
     */
    public interface OnPageChangeListener {

        /**
         * This method will be invoked when the current page is scrolled, either as part
         * of a programmatically initiated smooth scroll or a user initiated touch scroll.
         *
         * @param position             Position index of the first page currently being displayed.
         *                             Page position+1 will be visible if positionOffset is nonzero.
         * @param positionOffset       Value from [0, 1) indicating the offset from the page at position.
         * @param positionOffsetPixels Value in pixels indicating the offset from position.
         */
        default void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        /**
         * This method will be invoked when a new page becomes selected. Animation is not
         * necessarily complete.
         *
         * @param position Position index of the new selected page.
         */
        default void onPageSelected(int position) {
        }

        /**
         * Called when the scroll state changes. Useful for discovering when the user
         * begins dragging, when the pager is automatically settling to the current page,
         * or when it is fully stopped/idle.
         *
         * @param state The new scroll state.
         * @see ViewPager#SCROLL_STATE_IDLE
         * @see ViewPager#SCROLL_STATE_DRAGGING
         * @see ViewPager#SCROLL_STATE_SETTLING
         */
        default void onPageScrollStateChanged(int state) {
        }
    }

    /**
     * A PageTransformer is invoked whenever a visible/attached page is scrolled.
     * This offers an opportunity for the application to apply a custom transformation
     * to the page views using animation properties.
     */
    public interface PageTransformer {
        /**
         * Apply a property transformation to the given page.
         *
         * @param page     Apply the transformation to this page
         * @param position Position of page relative to the current front-and-center
         *                 position of the pager. 0 is front and center. 1 is one full
         *                 page position to the right, and -1 is one page position to the left.
         */
        void transformPage(@NonNull View page, float position);
    }

    /**
     * Callback interface for responding to adapter changes.
     */
    public interface OnAdapterChangeListener {
        /**
         * Called when the adapter for the given view pager has changed.
         *
         * @param viewPager  ViewPager where the adapter change has happened
         * @param oldAdapter the previously set adapter
         * @param newAdapter the newly set adapter
         */
        void onAdapterChanged(@NonNull ViewPager viewPager,
                              @Nullable PagerAdapter oldAdapter, @Nullable PagerAdapter newAdapter);
    }

    /**
     * Annotation which allows marking of views to be decoration views when added to a view
     * pager.
     *
     * <p>Views marked with this annotation can be added to the view pager with a layout resource.</p>
     *
     * <p>You can also control whether a view is a decor view but setting
     * {@link LayoutParams#isDecor} on the child's layout params.</p>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface DecorView {
    }

    public ViewPager(Context context) {
        this(context, null);
    }

    public ViewPager(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, null);
    }

    public ViewPager(Context context, @Nullable AttributeSet attrs,
                     @Nullable @AttrRes ResourceId defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    public ViewPager(Context context, @Nullable AttributeSet attrs,
                     @Nullable @AttrRes ResourceId defStyleAttr,
                     @Nullable @StyleRes ResourceId defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setWillNotDraw(false);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setFocusable(true);
        mScroller = new OverScroller(context, TimeInterpolator.DECELERATE_QUINTIC);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        final float density = context.getResources().getDisplayMetrics().density;

        mTouchSlop = configuration.getScaledPagingTouchSlop();
        mMinimumVelocity = (int) (MIN_FLING_VELOCITY * density);
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mLeftEdge = new EdgeEffect(context);
        mRightEdge = new EdgeEffect(context);

        mFlingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);
        mCloseEnough = (int) (CLOSE_ENOUGH * density);
        mDefaultGutterSize = (int) (DEFAULT_GUTTER_SIZE * density);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(mEndScrollRunnable);
        // To be on the safe side, abort the scroller
        if ((mScroller != null) && !mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        super.onDetachedFromWindow();
    }

    void setScrollState(int newState) {
        if (mScrollState == newState) {
            return;
        }

        mScrollState = newState;
        if (mPageTransformer != null) {
            // PageTransformers can do complex things that benefit from hardware layers.
            enableLayers(newState != SCROLL_STATE_IDLE);
        }
        dispatchOnScrollStateChanged(newState);
    }

    /**
     * Set a PagerAdapter that will supply views for this pager as needed.
     *
     * @param adapter Adapter to use
     */
    public void setAdapter(@Nullable PagerAdapter adapter) {
        if (mAdapter != null) {
            mAdapter.setViewPagerObserver(null);
            mAdapter.startUpdate(this);
            for (int i = 0; i < mItems.size(); i++) {
                final ItemInfo ii = mItems.get(i);
                mAdapter.destroyItem(this, mirrorPosition(ii.position), ii.object);
            }
            mAdapter.finishUpdate(this);
            mItems.clear();
            removeNonDecorViews();
            mCurItem = 0;
            scrollTo(0, 0);
        }

        final PagerAdapter oldAdapter = mAdapter;
        mAdapter = adapter;
        mExpectedAdapterCount = 0;

        if (mAdapter != null) {
            if (mObserver == null) {
                mObserver = new PagerObserver();
            }
            mAdapter.setViewPagerObserver(mObserver);
            mPopulatePending = false;
            final boolean wasFirstLayout = mFirstLayout;
            mFirstLayout = true;
            mExpectedAdapterCount = mAdapter.getCount();
            /*if (mRestoredCurItem >= 0) {
                mAdapter.restoreState(mRestoredAdapterState, mRestoredClassLoader);
                setCurrentItemInternal(mRestoredCurItem, false, true);
                mRestoredCurItem = -1;
                mRestoredAdapterState = null;
                mRestoredClassLoader = null;
            } else*/
            if (!wasFirstLayout) {
                mCurItem = mirrorPosition(0);
                populate();
            } else {
                requestLayout();
            }
        }

        // Dispatch the change to any listeners
        if (mAdapterChangeListeners != null && !mAdapterChangeListeners.isEmpty()) {
            for (int i = 0, count = mAdapterChangeListeners.size(); i < count; i++) {
                mAdapterChangeListeners.get(i).onAdapterChanged(this, oldAdapter, adapter);
            }
        }
    }

    private void removeNonDecorViews() {
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (!lp.isDecor) {
                removeViewAt(i);
                i--;
            }
        }
    }

    /**
     * Retrieve the current adapter supplying pages.
     *
     * @return The currently registered PagerAdapter
     */
    @Nullable
    public PagerAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Add a listener that will be invoked whenever the adapter for this ViewPager changes.
     *
     * @param listener listener to add
     */
    public void addOnAdapterChangeListener(@NonNull OnAdapterChangeListener listener) {
        if (mAdapterChangeListeners == null) {
            mAdapterChangeListeners = new ArrayList<>();
        }
        mAdapterChangeListeners.add(listener);
    }

    /**
     * Remove a listener that was previously added via
     * {@link #addOnAdapterChangeListener(OnAdapterChangeListener)}.
     *
     * @param listener listener to remove
     */
    public void removeOnAdapterChangeListener(@NonNull OnAdapterChangeListener listener) {
        if (mAdapterChangeListeners != null) {
            mAdapterChangeListeners.remove(listener);
        }
    }

    private int getClientWidth() {
        return getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
    }

    /**
     * Set the currently selected page. If the ViewPager has already been through its first
     * layout with its current adapter there will be a smooth animated transition between
     * the current item and the specified item.
     *
     * @param item Item index to select
     */
    public void setCurrentItem(int item) {
        mPopulatePending = false;
        setCurrentItemInternal(mirrorPosition(item), !mFirstLayout, false);
    }

    /**
     * Set the currently selected page.
     *
     * @param item         Item index to select
     * @param smoothScroll True to smoothly scroll to the new item, false to transition immediately
     */
    public void setCurrentItem(int item, boolean smoothScroll) {
        mPopulatePending = false;
        setCurrentItemInternal(mirrorPosition(item), smoothScroll, false);
    }

    public int getCurrentItem() {
        return mirrorPosition(mCurItem);
    }

    boolean setCurrentItemInternal(int item, boolean smoothScroll, boolean always) {
        return setCurrentItemInternal(item, smoothScroll, always, 0);
    }

    boolean setCurrentItemInternal(int item, boolean smoothScroll, boolean always, int velocity) {
        if (mAdapter == null || mAdapter.getCount() <= 0) {
            return false;
        }

        item = MathUtil.clamp(item, 0, mAdapter.getCount() - 1);
        if (!always && mCurItem == item && mItems.size() != 0) {
            return false;
        }

        final int pageLimit = mOffscreenPageLimit;
        if (item > (mCurItem + pageLimit) || item < (mCurItem - pageLimit)) {
            // We are doing a jump by more than one page.  To avoid
            // glitches, we want to keep all current pages in the view
            // until the scroll ends.
            for (int i = 0; i < mItems.size(); i++) {
                mItems.get(i).scrolling = true;
            }
        }
        final boolean dispatchSelected = mCurItem != item;

        if (mFirstLayout) {
            // We don't have any idea how big we are yet and shouldn't have any pages either.
            // Just set things up and let the pending layout handle things.
            mCurItem = item;
            if (dispatchSelected) {
                dispatchOnPageSelected(item);
            }
            requestLayout();
        } else {
            populate(item);
            scrollToItem(item, smoothScroll, velocity, dispatchSelected);
        }

        return true;
    }

    private void scrollToItem(int position, boolean smoothScroll, int velocity,
                              boolean dispatchSelected) {
        final ItemInfo info = infoForPosition(position);
        int destX = 0;
        if (info != null) {
            final int width = getClientWidth();
            destX = (int) (width * MathUtil.clamp(
                    info.offset, mFirstOffset, mLastOffset));
        }
        if (smoothScroll) {
            smoothScrollTo(destX, 0, velocity);
            if (dispatchSelected) {
                dispatchOnPageSelected(position);
            }
        } else {
            if (dispatchSelected) {
                dispatchOnPageSelected(position);
            }
            completeScroll(false);
            scrollTo(destX, 0);
            pageScrolled(destX);
        }
    }

    /**
     * Add a listener that will be invoked whenever the page changes or is incrementally
     * scrolled. See {@link OnPageChangeListener}.
     *
     * <p>Components that add a listener should take care to remove it when finished.
     * Other components that take ownership of a view may call {@link #clearOnPageChangeListeners()}
     * to remove all attached listeners.</p>
     *
     * @param listener listener to add
     */
    public void addOnPageChangeListener(@NonNull OnPageChangeListener listener) {
        if (mOnPageChangeListeners == null) {
            mOnPageChangeListeners = new ArrayList<>();
        }
        mOnPageChangeListeners.add(listener);
    }

    /**
     * Remove a listener that was previously added via
     * {@link #addOnPageChangeListener(OnPageChangeListener)}.
     *
     * @param listener listener to remove
     */
    public void removeOnPageChangeListener(@NonNull OnPageChangeListener listener) {
        if (mOnPageChangeListeners != null) {
            mOnPageChangeListeners.remove(listener);
        }
    }

    /**
     * Remove all listeners that are notified of any changes in scroll state or position.
     */
    public void clearOnPageChangeListeners() {
        if (mOnPageChangeListeners != null) {
            mOnPageChangeListeners.clear();
        }
    }

    /**
     * Sets a {@link PageTransformer} that will be called for each attached page whenever
     * the scroll position is changed. This allows the application to apply custom property
     * transformations to each page, overriding the default sliding behavior.
     *
     * @param reverseDrawingOrder true if the supplied PageTransformer requires page views
     *                            to be drawn from last to first instead of first to last.
     * @param transformer         PageTransformer that will modify each page's animation properties
     */
    public void setPageTransformer(boolean reverseDrawingOrder,
                                   @Nullable PageTransformer transformer) {
        setPageTransformer(reverseDrawingOrder, transformer, 2);
    }

    /**
     * Sets a {@link PageTransformer} that will be called for each attached page whenever
     * the scroll position is changed. This allows the application to apply custom property
     * transformations to each page, overriding the default sliding behavior.
     *
     * @param reverseDrawingOrder true if the supplied PageTransformer requires page views
     *                            to be drawn from last to first instead of first to last.
     * @param transformer         PageTransformer that will modify each page's animation properties
     * @param pageLayerType       View layer type that should be used for ViewPager pages. It should be
     *                            either {@link View#LAYER_TYPE_HARDWARE}, or
     *                            {@link View#LAYER_TYPE_NONE}.
     */
    public void setPageTransformer(boolean reverseDrawingOrder,
                                   @Nullable PageTransformer transformer, int pageLayerType) {
        final boolean hasTransformer = transformer != null;
        final boolean needsPopulate = hasTransformer == (mPageTransformer == null);
        mPageTransformer = transformer;
        setChildrenDrawingOrderEnabled(hasTransformer);
        if (hasTransformer) {
            mDrawingOrder = reverseDrawingOrder ? DRAW_ORDER_REVERSE : DRAW_ORDER_FORWARD;
            mPageTransformerLayerType = pageLayerType;
        } else {
            mDrawingOrder = DRAW_ORDER_DEFAULT;
        }
        if (needsPopulate) populate();
    }

    /**
     * Returns the number of pages that will be retained to either side of the
     * current page in the view hierarchy in an idle state. Defaults to 1.
     *
     * @return How many pages will be kept offscreen on either side
     * @see #setOffscreenPageLimit(int)
     */
    public int getOffscreenPageLimit() {
        return mOffscreenPageLimit;
    }

    /**
     * Set the number of pages that should be retained to either side of the
     * current page in the view hierarchy in an idle state. Pages beyond this
     * limit will be recreated from the adapter when needed.
     *
     * <p>This is offered as an optimization. If you know in advance the number
     * of pages you will need to support or have lazy-loading mechanisms in place
     * on your pages, tweaking this setting can have benefits in perceived smoothness
     * of paging animations and interaction. If you have a small number of pages (3-4)
     * that you can keep active all at once, less time will be spent in layout for
     * newly created view subtrees as the user pages back and forth.</p>
     *
     * <p>You should keep this limit low, especially if your pages have complex layouts.
     * This setting defaults to 1.</p>
     *
     * @param limit How many pages will be kept offscreen in an idle state.
     */
    public void setOffscreenPageLimit(int limit) {
        if (limit < DEFAULT_OFFSCREEN_PAGES) {
            LOGGER.warn(MARKER, "Requested offscreen page limit {} too small; defaulting to {}",
                    limit, DEFAULT_OFFSCREEN_PAGES);
            limit = DEFAULT_OFFSCREEN_PAGES;
        }
        if (limit != mOffscreenPageLimit) {
            mOffscreenPageLimit = limit;
            populate();
        }
    }

    /**
     * Set the margin between pages.
     *
     * @param marginPixels Distance between adjacent pages in pixels
     * @see #getPageMargin()
     * @see #setPageMarginDrawable(Drawable)
     */
    public void setPageMargin(int marginPixels) {
        final int oldMargin = mPageMargin;
        mPageMargin = marginPixels;

        final int width = getWidth();
        recomputeScrollPosition(width, width, marginPixels, oldMargin);

        requestLayout();
    }

    /**
     * Return the margin between pages.
     *
     * @return The size of the margin in pixels
     */
    public int getPageMargin() {
        return mPageMargin;
    }

    /**
     * Set a drawable that will be used to fill the margin between pages.
     *
     * @param d Drawable to display between pages
     */
    public void setPageMarginDrawable(@Nullable Drawable d) {
        mMarginDrawable = d;
        if (d != null)
            refreshDrawableState();
        // Fixed by Modern UI: we have edge effect, cannot skip draw
        //setWillNotDraw(d == null);
        invalidate();
    }

    /**
     * Sets the edge effect color for both left and right edge effects.
     *
     * @param color The color for the edge effects.
     * @see #setLeftEdgeEffectColor(int)
     * @see #setRightEdgeEffectColor(int)
     * @see #getLeftEdgeEffectColor()
     * @see #getRightEdgeEffectColor()
     */
    public void setEdgeEffectColor(@ColorInt int color) {
        setLeftEdgeEffectColor(color);
        setRightEdgeEffectColor(color);
    }

    /**
     * Sets the left edge effect color.
     *
     * @param color The color for the left edge effect.
     * @see #setRightEdgeEffectColor(int)
     * @see #setEdgeEffectColor(int)
     * @see #getLeftEdgeEffectColor()
     * @see #getRightEdgeEffectColor()
     */
    public void setLeftEdgeEffectColor(@ColorInt int color) {
        mLeftEdge.setColor(color);
    }

    /**
     * Sets the right edge effect color.
     *
     * @param color The color for the right edge effect.
     * @see #setLeftEdgeEffectColor(int)
     * @see #setEdgeEffectColor(int)
     * @see #getLeftEdgeEffectColor()
     * @see #getRightEdgeEffectColor()
     */
    public void setRightEdgeEffectColor(@ColorInt int color) {
        mRightEdge.setColor(color);
    }

    /**
     * Returns the left edge effect color.
     *
     * @return The left edge effect color.
     * @see #setEdgeEffectColor(int)
     * @see #setRightEdgeEffectColor(int)
     * @see #setLeftEdgeEffectColor(int)
     * @see #getRightEdgeEffectColor()
     */
    @ColorInt
    public int getLeftEdgeEffectColor() {
        return mLeftEdge.getColor();
    }

    /**
     * Returns the right edge effect color.
     *
     * @return The right edge effect color.
     * @see #setEdgeEffectColor(int)
     * @see #setRightEdgeEffectColor(int)
     * @see #setLeftEdgeEffectColor(int)
     * @see #getLeftEdgeEffectColor()
     */
    @ColorInt
    public int getRightEdgeEffectColor() {
        return mRightEdge.getColor();
    }

    /**
     * Sets the left edge effect blend mode, the default is {@link EdgeEffect#DEFAULT_BLEND_MODE}.
     *
     * @param blendMode The blend mode for the left edge effect.
     */
    public void setLeftEdgeEffectBlendMode(@Nullable BlendMode blendMode) {
        mLeftEdge.setBlendMode(blendMode);
    }

    /**
     * Sets the right edge effect blend mode, the default is {@link EdgeEffect#DEFAULT_BLEND_MODE}.
     *
     * @param blendMode The blend mode for the right edge effect.
     */
    public void setRightEdgeEffectBlendMode(@Nullable BlendMode blendMode) {
        mRightEdge.setBlendMode(blendMode);
    }

    @Nullable
    public BlendMode getLeftEdgeEffectBlendMode() {
        return mLeftEdge.getBlendMode();
    }

    @Nullable
    public BlendMode getRightEdgeEffectBlendMode() {
        return mRightEdge.getBlendMode();
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable drawable) {
        return super.verifyDrawable(drawable) || drawable == mMarginDrawable;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        final Drawable d = mMarginDrawable;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
    }

    // We want the duration of the page snap animation to be influenced by the distance that
    // the screen has to travel, however, we don't want this duration to be effected in a
    // purely linear fashion. Instead, we use this method to moderate the effect that the distance
    // of travel has on the overall snap duration.
    float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * (float) Math.PI / 2.0f;
        return (float) Math.sin(f);
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param x the number of pixels to scroll by on the X axis
     * @param y the number of pixels to scroll by on the Y axis
     */
    void smoothScrollTo(int x, int y) {
        smoothScrollTo(x, y, 0);
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param x        the number of pixels to scroll by on the X axis
     * @param y        the number of pixels to scroll by on the Y axis
     * @param velocity the velocity associated with a fling, if applicable. (0 otherwise)
     */
    void smoothScrollTo(int x, int y, int velocity) {
        if (getChildCount() == 0) {
            // Nothing to do.
            return;
        }

        int sx;
        boolean wasScrolling = (mScroller != null) && !mScroller.isFinished();
        if (wasScrolling) {
            // We're in the middle of a previously initiated scrolling. Check to see
            // whether that scrolling has actually started (if we always call getStartX
            // we can get a stale value from the scroller if it hadn't yet had its first
            // computeScrollOffset call) to decide what is the current scrolling position.
            sx = mIsScrollStarted ? mScroller.getCurrX() : mScroller.getStartX();
            // And abort the current scrolling.
            mScroller.abortAnimation();
        } else {
            sx = getScrollX();
        }
        int sy = getScrollY();
        int dx = x - sx;
        int dy = y - sy;
        if (dx == 0 && dy == 0) {
            completeScroll(false);
            populate();
            setScrollState(SCROLL_STATE_IDLE);
            return;
        }

        setScrollState(SCROLL_STATE_SETTLING);

        final int width = getClientWidth();
        final int halfWidth = width / 2;
        final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dx) / width);
        final float distance = halfWidth + halfWidth
                * distanceInfluenceForSnapDuration(distanceRatio);

        int duration;
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
        } else {
            final float pageWidth = width * mAdapter.getPageWidth(mirrorPosition(mCurItem));
            final float pageDelta = (float) Math.abs(dx) / (pageWidth + mPageMargin);
            duration = (int) ((pageDelta + 1) * 100);
        }
        duration = Math.min(duration, MAX_SETTLE_DURATION);

        // Reset the "scroll started" flag. It will be flipped to true in all places
        // where we call computeScrollOffset().
        mIsScrollStarted = false;
        mScroller.startScroll(sx, sy, dx, dy, duration);
        postInvalidateOnAnimation();
    }

    ItemInfo addNewItem(int position, int index) {
        ItemInfo ii = new ItemInfo();
        ii.position = position;
        ii.object = mAdapter.instantiateItem(this, mirrorPosition(position));
        ii.widthFactor = mAdapter.getPageWidth(mirrorPosition(position));
        if (index < 0 || index >= mItems.size()) {
            mItems.add(ii);
        } else {
            mItems.add(index, ii);
        }
        return ii;
    }

    void dataSetChanged() {
        // This method only gets called if our observer is attached, so mAdapter is non-null.

        final int adapterCount = mAdapter.getCount();
        mExpectedAdapterCount = adapterCount;
        boolean needPopulate = mItems.size() < mOffscreenPageLimit * 2 + 1
                && mItems.size() < adapterCount;
        int newCurrItem = mCurItem;

        boolean isUpdating = false;
        for (int i = 0; i < mItems.size(); i++) {
            final ItemInfo ii = mItems.get(i);
            int newPos = mAdapter.getItemPosition(ii.object);

            if (newPos == PagerAdapter.POSITION_UNCHANGED) {
                continue;
            }

            if (newPos == PagerAdapter.POSITION_NONE) {
                mItems.remove(i);
                i--;

                if (!isUpdating) {
                    mAdapter.startUpdate(this);
                    isUpdating = true;
                }

                mAdapter.destroyItem(this, mirrorPosition(ii.position), ii.object);
                needPopulate = true;

                if (mCurItem == ii.position) {
                    // Keep the current item in the valid range
                    newCurrItem = Math.max(0, Math.min(mCurItem, adapterCount - 1));
                }
                continue;
            }

            newPos = mirrorPosition(newPos);

            if (ii.position != newPos) {
                if (ii.position == mCurItem) {
                    // Our current item changed position. Follow it.
                    newCurrItem = newPos;
                }

                ii.position = newPos;
                needPopulate = true;
            }
        }

        if (isUpdating) {
            mAdapter.finishUpdate(this);
        }

        mItems.sort(null);

        if (needPopulate) {
            // Reset our known page widths; populate will recompute them.
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (!lp.isDecor) {
                    lp.widthFactor = 0.f;
                }
            }

            setCurrentItemInternal(newCurrItem, false, true);
            requestLayout();
        }
    }

    void populate() {
        populate(mCurItem);
    }

    void populate(int newCurrentItem) {
        ItemInfo oldCurInfo = null;
        int focusDirection = View.FOCUS_FORWARD;
        if (mCurItem != newCurrentItem) {
            focusDirection = mCurItem < newCurrentItem ? View.FOCUS_RIGHT : View.FOCUS_LEFT;
            oldCurInfo = infoForPosition(mCurItem);
            mCurItem = newCurrentItem;
        }

        if (mAdapter == null) {
            sortChildDrawingOrder();
            return;
        }

        // Bail now if we are waiting to populate.  This is to hold off
        // on creating views from the time the user releases their finger to
        // fling to a new position until we have finished the scroll to
        // that position, avoiding glitches from happening at that point.
        if (mPopulatePending) {
            sortChildDrawingOrder();
            return;
        }

        // Also, don't populate until we are attached to a window.  This is to
        // avoid trying to populate before we have restored our view hierarchy
        // state and conflicting with what is restored.
        if (!isAttachedToWindow()) {
            return;
        }

        mAdapter.startUpdate(this);

        final int pageLimit = mOffscreenPageLimit;
        final int startPos = Math.max(0, mCurItem - pageLimit);
        final int N = mAdapter.getCount();
        final int endPos = Math.min(N - 1, mCurItem + pageLimit);

        if (N != mExpectedAdapterCount) {
            throw new IllegalStateException("The application's PagerAdapter changed the adapter's"
                    + " contents without calling PagerAdapter#notifyDataSetChanged!"
                    + " Expected adapter item count: " + mExpectedAdapterCount + ", found: " + N
                    + " Pager class: " + getClass()
                    + " Problematic adapter: " + mAdapter.getClass());
        }

        // Locate the currently focused item or add it if needed.
        int curIndex = -1;
        ItemInfo curItem = null;
        for (curIndex = 0; curIndex < mItems.size(); curIndex++) {
            final ItemInfo ii = mItems.get(curIndex);
            if (ii.position >= mCurItem) {
                if (ii.position == mCurItem) curItem = ii;
                break;
            }
        }

        if (curItem == null && N > 0) {
            curItem = addNewItem(mCurItem, curIndex);
        }

        // Fill 3x the available width or up to the number of offscreen
        // pages requested to either side, whichever is larger.
        // If we have no current item we have no work to do.
        if (curItem != null) {
            float extraWidthLeft = 0.f;
            int itemIndex = curIndex - 1;
            ItemInfo ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
            final int clientWidth = getClientWidth();
            final float leftWidthNeeded = clientWidth <= 0 ? 0 :
                    2.f - curItem.widthFactor + (float) getPaddingLeft() / (float) clientWidth;
            for (int pos = mCurItem - 1; pos >= 0; pos--) {
                if (extraWidthLeft >= leftWidthNeeded && pos < startPos) {
                    if (ii == null) {
                        break;
                    }
                    if (pos == ii.position && !ii.scrolling) {
                        mItems.remove(itemIndex);
                        mAdapter.destroyItem(this, mirrorPosition(pos), ii.object);
                        itemIndex--;
                        curIndex--;
                        ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
                    }
                } else if (ii != null && pos == ii.position) {
                    extraWidthLeft += ii.widthFactor;
                    itemIndex--;
                    ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
                } else {
                    ii = addNewItem(pos, itemIndex + 1);
                    extraWidthLeft += ii.widthFactor;
                    curIndex++;
                    ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
                }
            }

            float extraWidthRight = curItem.widthFactor;
            itemIndex = curIndex + 1;
            if (extraWidthRight < 2.f) {
                ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                final float rightWidthNeeded = clientWidth <= 0 ? 0 :
                        (float) getPaddingRight() / (float) clientWidth + 2.f;
                for (int pos = mCurItem + 1; pos < N; pos++) {
                    if (extraWidthRight >= rightWidthNeeded && pos > endPos) {
                        if (ii == null) {
                            break;
                        }
                        if (pos == ii.position && !ii.scrolling) {
                            mItems.remove(itemIndex);
                            mAdapter.destroyItem(this, mirrorPosition(pos), ii.object);
                            ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                        }
                    } else if (ii != null && pos == ii.position) {
                        extraWidthRight += ii.widthFactor;
                        itemIndex++;
                        ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                    } else {
                        ii = addNewItem(pos, itemIndex);
                        itemIndex++;
                        extraWidthRight += ii.widthFactor;
                        ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                    }
                }
            }

            calculatePageOffsets(curItem, curIndex, oldCurInfo);

            mAdapter.setPrimaryItem(this, mirrorPosition(mCurItem), curItem.object);
        }

        mAdapter.finishUpdate(this);

        // Check width measurement of current pages and drawing sort order.
        // Update LayoutParams as needed.
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.childIndex = i;
            if (!lp.isDecor && lp.widthFactor == 0.f) {
                // 0 means requery the adapter for this, it doesn't have a valid width.
                final ItemInfo ii = infoForChild(child);
                if (ii != null) {
                    lp.widthFactor = ii.widthFactor;
                    lp.position = ii.position;
                }
            }
        }
        sortChildDrawingOrder();

        if (hasFocus()) {
            View currentFocused = findFocus();
            ItemInfo ii = currentFocused != null ? infoForAnyChild(currentFocused) : null;
            if (ii == null || ii.position != mCurItem) {
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    ii = infoForChild(child);
                    if (ii != null && ii.position == mCurItem) {
                        final Rect focusRect;
                        if (currentFocused == null) {
                            focusRect = null;
                        } else {
                            focusRect = mTempRect;
                            currentFocused.getFocusedRect(mTempRect);
                            offsetDescendantRectToMyCoords(currentFocused, mTempRect);
                            offsetRectIntoDescendantCoords(child, mTempRect);
                        }
                        if (child.requestFocus(focusDirection, focusRect)) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private void sortChildDrawingOrder() {
        if (mDrawingOrder != DRAW_ORDER_DEFAULT) {
            if (mDrawingOrderedChildren == null) {
                mDrawingOrderedChildren = new ArrayList<>();
            } else {
                mDrawingOrderedChildren.clear();
            }
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                mDrawingOrderedChildren.add(child);
            }
            mDrawingOrderedChildren.sort(sPositionComparator);
        }
    }

    private void calculatePageOffsets(ItemInfo curItem, int curIndex, ItemInfo oldCurInfo) {
        final int N = mAdapter.getCount();
        final int width = getClientWidth();
        final float marginOffset = width > 0 ? (float) mPageMargin / width : 0;
        // Fix up offsets for later layout.
        if (oldCurInfo != null) {
            final int oldCurPosition = oldCurInfo.position;
            // Base offsets off of oldCurInfo.
            if (oldCurPosition < curItem.position) {
                int itemIndex = 0;
                ItemInfo ii = null;
                float offset = oldCurInfo.offset + oldCurInfo.widthFactor + marginOffset;
                for (int pos = oldCurPosition + 1;
                     pos <= curItem.position && itemIndex < mItems.size(); pos++) {
                    ii = mItems.get(itemIndex);
                    while (pos > ii.position && itemIndex < mItems.size() - 1) {
                        itemIndex++;
                        ii = mItems.get(itemIndex);
                    }
                    while (pos < ii.position) {
                        // We don't have an item populated for this,
                        // ask the adapter for an offset.
                        offset += mAdapter.getPageWidth(mirrorPosition(pos)) + marginOffset;
                        pos++;
                    }
                    ii.offset = offset;
                    offset += ii.widthFactor + marginOffset;
                }
            } else if (oldCurPosition > curItem.position) {
                int itemIndex = mItems.size() - 1;
                ItemInfo ii = null;
                float offset = oldCurInfo.offset;
                for (int pos = oldCurPosition - 1;
                     pos >= curItem.position && itemIndex >= 0; pos--) {
                    ii = mItems.get(itemIndex);
                    while (pos < ii.position && itemIndex > 0) {
                        itemIndex--;
                        ii = mItems.get(itemIndex);
                    }
                    while (pos > ii.position) {
                        // We don't have an item populated for this,
                        // ask the adapter for an offset.
                        offset -= mAdapter.getPageWidth(mirrorPosition(pos)) + marginOffset;
                        pos--;
                    }
                    offset -= ii.widthFactor + marginOffset;
                    ii.offset = offset;
                }
            }
        }

        // Base all offsets off of curItem.
        final int itemCount = mItems.size();
        float offset = curItem.offset;
        int pos = curItem.position - 1;
        mFirstOffset = curItem.position == 0 ? curItem.offset : -Float.MAX_VALUE;
        mLastOffset = curItem.position == N - 1
                ? curItem.offset + curItem.widthFactor - 1 : Float.MAX_VALUE;
        // Previous pages
        for (int i = curIndex - 1; i >= 0; i--, pos--) {
            final ItemInfo ii = mItems.get(i);
            while (pos > ii.position) {
                offset -= mAdapter.getPageWidth(mirrorPosition(pos--)) + marginOffset;
            }
            offset -= ii.widthFactor + marginOffset;
            ii.offset = offset;
            if (ii.position == 0) mFirstOffset = offset;
        }
        offset = curItem.offset + curItem.widthFactor + marginOffset;
        pos = curItem.position + 1;
        // Next pages
        for (int i = curIndex + 1; i < itemCount; i++, pos++) {
            final ItemInfo ii = mItems.get(i);
            while (pos < ii.position) {
                offset += mAdapter.getPageWidth(mirrorPosition(pos++)) + marginOffset;
            }
            if (ii.position == N - 1) {
                mLastOffset = offset + ii.widthFactor - 1;
            }
            ii.offset = offset;
            offset += ii.widthFactor + marginOffset;
        }
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        final int index = mDrawingOrder == DRAW_ORDER_REVERSE ? childCount - 1 - i : i;
        return ((LayoutParams) mDrawingOrderedChildren.get(index).getLayoutParams()).childIndex;
    }

    @Override
    public void addView(@NonNull View child, int index, @NonNull ViewGroup.LayoutParams params) {
        if (!checkLayoutParams(params)) {
            params = generateLayoutParams(params);
        }
        final LayoutParams lp = (LayoutParams) params;
        // Any views added via inflation should be classed as part of the decor
        lp.isDecor |= isDecorView(child);
        if (mInLayout) {
            if (lp.isDecor) {
                throw new IllegalStateException("Cannot add pager decor view during layout");
            }
            lp.needsMeasure = true;
            addViewInLayout(child, index, params);
        } else {
            super.addView(child, index, params);
        }
    }

    private static boolean isDecorView(@NonNull View view) {
        Class<?> clazz = view.getClass();
        return clazz.getAnnotation(DecorView.class) != null;
    }

    @Override
    public void removeView(@NonNull View view) {
        if (mInLayout) {
            removeViewInLayout(view);
        } else {
            super.removeView(view);
        }
    }

    ItemInfo infoForChild(View child) {
        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            if (mAdapter.isViewFromObject(child, ii.object)) {
                return ii;
            }
        }
        return null;
    }

    ItemInfo infoForAnyChild(View child) {
        ViewParent parent;
        while ((parent = child.getParent()) != this) {
            if (!(parent instanceof View)) {
                return null;
            }
            child = (View) parent;
        }
        return infoForChild(child);
    }

    ItemInfo infoForPosition(int position) {
        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            if (ii.position == position) {
                return ii;
            }
        }
        return null;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // For simple implementation, our internal size is always 0.
        // We depend on the container to specify the layout size of
        // our view.  We can't really know what it is since we will be
        // adding and removing different arbitrary views and do not
        // want the layout to change as this happens.
        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec),
                getDefaultSize(0, heightMeasureSpec));

        final int measuredWidth = getMeasuredWidth();
        final int maxGutterSize = measuredWidth / 10;
        mGutterSize = Math.min(maxGutterSize, mDefaultGutterSize);

        // Children are just made to fill our space.
        int childWidthSize = measuredWidth - getPaddingLeft() - getPaddingRight();
        int childHeightSize = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

        /*
         * Make sure all children have been properly measured. Decor views first.
         * Right now we cheat and make this less complicated by assuming decor
         * views won't intersect. We will pin to edges based on gravity.
         */
        int size = getChildCount();
        for (int i = 0; i < size; ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp != null && lp.isDecor) {
                    final int hgrav = lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                    final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
                    int widthMode = MeasureSpec.AT_MOST;
                    int heightMode = MeasureSpec.AT_MOST;
                    boolean consumeVertical = vgrav == Gravity.TOP || vgrav == Gravity.BOTTOM;
                    boolean consumeHorizontal = hgrav == Gravity.LEFT || hgrav == Gravity.RIGHT;

                    if (consumeVertical) {
                        widthMode = MeasureSpec.EXACTLY;
                    } else if (consumeHorizontal) {
                        heightMode = MeasureSpec.EXACTLY;
                    }

                    int widthSize = childWidthSize;
                    int heightSize = childHeightSize;
                    if (lp.width != LayoutParams.WRAP_CONTENT) {
                        widthMode = MeasureSpec.EXACTLY;
                        if (lp.width != LayoutParams.MATCH_PARENT) {
                            widthSize = lp.width;
                        }
                    }
                    if (lp.height != LayoutParams.WRAP_CONTENT) {
                        heightMode = MeasureSpec.EXACTLY;
                        if (lp.height != LayoutParams.MATCH_PARENT) {
                            heightSize = lp.height;
                        }
                    }
                    final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, widthMode);
                    final int heightSpec = MeasureSpec.makeMeasureSpec(heightSize, heightMode);
                    child.measure(widthSpec, heightSpec);

                    if (consumeVertical) {
                        childHeightSize -= child.getMeasuredHeight();
                    } else if (consumeHorizontal) {
                        childWidthSize -= child.getMeasuredWidth();
                    }
                }
            }
        }

        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidthSize, MeasureSpec.EXACTLY);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeightSize, MeasureSpec.EXACTLY);

        if (mFirstLayout) {
            mCurItem = mirrorPosition(0);
        }

        // Make sure we have created all fragments that we need to have shown.
        mInLayout = true;
        populate();
        mInLayout = false;

        // Page views next.
        size = getChildCount();
        for (int i = 0; i < size; ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {

                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (!lp.isDecor) {
                    final int widthSpec = MeasureSpec.makeMeasureSpec(
                            (int) (childWidthSize * lp.widthFactor), MeasureSpec.EXACTLY);
                    child.measure(widthSpec, childHeightMeasureSpec);
                }
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Make sure scroll position is set correctly.
        if (w != oldw) {
            recomputeScrollPosition(w, oldw, mPageMargin, mPageMargin);
        }
    }

    private void recomputeScrollPosition(int width, int oldWidth, int margin, int oldMargin) {
        if (oldWidth > 0 && !mItems.isEmpty()) {
            if (!mScroller.isFinished()) {
                mScroller.setFinalX(mCurItem * getClientWidth());
            } else {
                final int widthWithMargin = width - getPaddingLeft() - getPaddingRight() + margin;
                final int oldWidthWithMargin = oldWidth - getPaddingLeft() - getPaddingRight()
                        + oldMargin;
                final int xpos = getScrollX();
                final float pageOffset = (float) xpos / oldWidthWithMargin;
                final int newOffsetPixels = (int) (pageOffset * widthWithMargin);

                scrollTo(newOffsetPixels, getScrollY());
            }
        } else {
            final ItemInfo ii = infoForPosition(mCurItem);
            final float scrollOffset = ii != null ? Math.min(ii.offset, mLastOffset) : 0;
            final int scrollPos =
                    (int) (scrollOffset * (width - getPaddingLeft() - getPaddingRight()));
            if (scrollPos != getScrollX()) {
                completeScroll(false);
                scrollTo(scrollPos, getScrollY());
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount();
        int width = r - l;
        int height = b - t;
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        final int scrollX = getScrollX();

        int decorCount = 0;

        // First pass - decor views. We need to do this in two passes so that
        // we have the proper offsets for non-decor views later.
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int childLeft;
                int childTop;
                if (lp.isDecor) {
                    final int hgrav = lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                    final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
                    switch (hgrav) {
                        default -> childLeft = paddingLeft;
                        case Gravity.LEFT -> {
                            childLeft = paddingLeft;
                            paddingLeft += child.getMeasuredWidth();
                        }
                        case Gravity.CENTER_HORIZONTAL ->
                                childLeft = Math.max((width - child.getMeasuredWidth()) / 2, paddingLeft);
                        case Gravity.RIGHT -> {
                            childLeft = width - paddingRight - child.getMeasuredWidth();
                            paddingRight += child.getMeasuredWidth();
                        }
                    }
                    switch (vgrav) {
                        default -> childTop = paddingTop;
                        case Gravity.TOP -> {
                            childTop = paddingTop;
                            paddingTop += child.getMeasuredHeight();
                        }
                        case Gravity.CENTER_VERTICAL ->
                                childTop = Math.max((height - child.getMeasuredHeight()) / 2, paddingTop);
                        case Gravity.BOTTOM -> {
                            childTop = height - paddingBottom - child.getMeasuredHeight();
                            paddingBottom += child.getMeasuredHeight();
                        }
                    }
                    childLeft += scrollX;
                    child.layout(childLeft, childTop,
                            childLeft + child.getMeasuredWidth(),
                            childTop + child.getMeasuredHeight());
                    decorCount++;
                }
            }
        }

        final int childWidth = width - paddingLeft - paddingRight;
        // Page views. Do this once we have the right padding offsets from above.
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                ItemInfo ii;
                if (!lp.isDecor && (ii = infoForChild(child)) != null) {
                    if (lp.needsMeasure) {
                        // This was added during layout and needs measurement.
                        // Do it now that we know what we're working with.
                        lp.needsMeasure = false;
                        final int widthSpec = MeasureSpec.makeMeasureSpec(
                                (int) (childWidth * lp.widthFactor),
                                MeasureSpec.EXACTLY);
                        final int heightSpec = MeasureSpec.makeMeasureSpec(
                                height - paddingTop - paddingBottom,
                                MeasureSpec.EXACTLY);
                        child.measure(widthSpec, heightSpec);
                    }

                    final int leftOffset = (int) (childWidth * ii.offset);
                    final int childLeft = paddingLeft + leftOffset;
                    final int childTop = paddingTop;
                    child.layout(childLeft, childTop,
                            childLeft + child.getMeasuredWidth(),
                            childTop + child.getMeasuredHeight());
                }
            }
        }
        mTopPageBounds = paddingTop;
        mBottomPageBounds = height - paddingBottom;
        mDecorChildCount = decorCount;

        if (mFirstLayout) {
            scrollToItem(mCurItem, false, 0, false);
        }
        mFirstLayout = false;
    }

    @Override
    public void computeScroll() {
        mIsScrollStarted = true;
        if (!mScroller.isFinished() && mScroller.computeScrollOffset()) {
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();

            if (oldX != x || oldY != y) {
                scrollTo(x, y);
                if (!pageScrolled(x)) {
                    mScroller.abortAnimation();
                    scrollTo(0, y);
                }
            }

            // Keep on drawing until the animation has finished.
            postInvalidateOnAnimation();
            return;
        }

        // Done with scroll, clean up state.
        completeScroll(true);
    }

    private boolean pageScrolled(int scrollX) {
        if (mItems.size() == 0) {
            if (mFirstLayout) {
                // If we haven't been laid out yet, we probably just haven't been populated yet.
                // Let's skip this call since it doesn't make sense in this state
                return false;
            }
            mCalledSuper = false;
            onPageScrolled(0, 0, 0);
            if (!mCalledSuper) {
                throw new IllegalStateException(
                        "onPageScrolled did not call superclass implementation");
            }
            return false;
        }
        final ItemInfo ii = infoForCurrentScrollPosition();
        final int width = getClientWidth();
        final int widthWithMargin = width + mPageMargin;
        final float marginOffset = (float) mPageMargin / width;
        final int currentPage = ii.position;
        final float pageOffset = (((float) scrollX / width) - ii.offset)
                / (ii.widthFactor + marginOffset);
        final int offsetPixels = (int) (pageOffset * widthWithMargin);

        mCalledSuper = false;
        onPageScrolled(currentPage, pageOffset, offsetPixels);
        if (!mCalledSuper) {
            throw new IllegalStateException(
                    "onPageScrolled did not call superclass implementation");
        }
        return true;
    }

    /**
     * This method will be invoked when the current page is scrolled, either as part
     * of a programmatically initiated smooth scroll or a user initiated touch scroll.
     * If you override this method you must call through to the superclass implementation
     * (e.g. super.onPageScrolled(position, offset, offsetPixels)) before onPageScrolled
     * returns.
     *
     * @param position     Position index of the first page currently being displayed.
     *                     Page position+1 will be visible if positionOffset is nonzero.
     * @param offset       Value from [0, 1) indicating the offset from the page at position.
     * @param offsetPixels Value in pixels indicating the offset from position.
     */
    @CallSuper
    @MustBeInvokedByOverriders
    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        // Offset any decor views if needed - keep them on-screen at all times.
        if (mDecorChildCount > 0) {
            final int scrollX = getScrollX();
            int paddingLeft = getPaddingLeft();
            int paddingRight = getPaddingRight();
            final int width = getWidth();
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (!lp.isDecor) continue;

                final int hgrav = lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                int childLeft;
                switch (hgrav) {
                    default -> childLeft = paddingLeft;
                    case Gravity.LEFT -> {
                        childLeft = paddingLeft;
                        paddingLeft += child.getWidth();
                    }
                    case Gravity.CENTER_HORIZONTAL ->
                            childLeft = Math.max((width - child.getMeasuredWidth()) / 2, paddingLeft);
                    case Gravity.RIGHT -> {
                        childLeft = width - paddingRight - child.getMeasuredWidth();
                        paddingRight += child.getMeasuredWidth();
                    }
                }
                childLeft += scrollX;

                final int childOffset = childLeft - child.getLeft();
                if (childOffset != 0) {
                    child.offsetLeftAndRight(childOffset);
                }
            }
        }

        dispatchOnPageScrolled(position, offset, offsetPixels);

        if (mPageTransformer != null) {
            final int scrollX = getScrollX();
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                if (lp.isDecor) continue;
                final float transformPos = (float) (child.getLeft() - scrollX) / getClientWidth();
                mPageTransformer.transformPage(child, transformPos);
            }
        }

        mCalledSuper = true;
    }

    private void dispatchOnPageScrolled(int position, float offset, int offsetPixels) {
        if (mOnPageChangeListeners != null) {
            if (mLayoutDirection == LAYOUT_DIRECTION_RTL) {
                //TODO this is not correct if pageWidths are not uniform??
                int width = getWidth();
                int count = mAdapter.getCount();
                int remainingWidth = (int) (width * (1 - mAdapter.getPageWidth(count - position - 1))) + offsetPixels;
                while (position < count && remainingWidth > 0) {
                    position += 1;
                    remainingWidth -= (int) (width * mAdapter.getPageWidth(count - position - 1));
                }
                offsetPixels = -remainingWidth;
                offset = offsetPixels / (width * mAdapter.getPageWidth(position));
                position = count - position - 1;
            }
            for (int i = 0, z = mOnPageChangeListeners.size(); i < z; i++) {
                OnPageChangeListener listener = mOnPageChangeListeners.get(i);
                if (listener != null) {
                    listener.onPageScrolled(position, offset, offsetPixels);
                }
            }
        }
    }

    private void dispatchOnPageSelected(int position) {
        if (mOnPageChangeListeners != null) {
            position = mirrorPosition(position);
            for (int i = 0, z = mOnPageChangeListeners.size(); i < z; i++) {
                OnPageChangeListener listener = mOnPageChangeListeners.get(i);
                if (listener != null) {
                    listener.onPageSelected(position);
                }
            }
        }
    }

    private void dispatchOnScrollStateChanged(int state) {
        if (mOnPageChangeListeners != null) {
            for (int i = 0, z = mOnPageChangeListeners.size(); i < z; i++) {
                OnPageChangeListener listener = mOnPageChangeListeners.get(i);
                if (listener != null) {
                    listener.onPageScrollStateChanged(state);
                }
            }
        }
    }

    private void completeScroll(boolean postEvents) {
        boolean needPopulate = mScrollState == SCROLL_STATE_SETTLING;
        if (needPopulate) {
            boolean wasScrolling = !mScroller.isFinished();
            if (wasScrolling) {
                mScroller.abortAnimation();
                int oldX = getScrollX();
                int oldY = getScrollY();
                int x = mScroller.getCurrX();
                int y = mScroller.getCurrY();
                if (oldX != x || oldY != y) {
                    scrollTo(x, y);
                    if (x != oldX) {
                        pageScrolled(x);
                    }
                }
            }
        }
        mPopulatePending = false;
        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            if (ii.scrolling) {
                needPopulate = true;
                ii.scrolling = false;
            }
        }
        if (needPopulate) {
            if (postEvents) {
                postOnAnimation(mEndScrollRunnable);
            } else {
                mEndScrollRunnable.run();
            }
        }
    }

    /**
     * @return Whether dragging in the gutter (left and right edges) of the ViewPager is enabled.
     */
    public boolean isDragInGutterEnabled() {
        return mDragInGutterEnabled;
    }

    /**
     * Set whether ViewPager should consume drag events if they are within the gutter
     * (left and right edges) of the ViewPager. The default value {@code false}.
     *
     * @param enabled true if ViewPager should allow drag in gutter, false otherwise
     */
    public void setDragInGutterEnabled(boolean enabled) {
        mDragInGutterEnabled = enabled;
    }

    private boolean isGutterDrag(float x, float dx) {
        if (mDragInGutterEnabled) {
            return false;
        }
        return (x < mGutterSize && dx > 0) || (x > getWidth() - mGutterSize && dx < 0);
    }

    private void enableLayers(boolean enable) {
        //TODO layers
        final int childCount = getChildCount();
        /*for (int i = 0; i < childCount; i++) {
            final int layerType = enable
                    ? mPageTransformerLayerType : View.LAYER_TYPE_NONE;
            getChildAt(i).setLayerType(layerType, null);
        }*/
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        final int action = ev.getAction() & MotionEvent.ACTION_MASK;

        // Always take care of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the drag.
            resetTouch();
            return false;
        }

        // Nothing more to do here if we have decided whether or not we
        // are dragging.
        if (action != MotionEvent.ACTION_DOWN) {
            if (mIsBeingDragged) {
                return true;
            }
            if (mIsUnableToDrag) {
                return false;
            }
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE -> {
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                /*
                 * Locally do absolute value. mLastMotionY is set to the y value
                 * of the down event.
                 */
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                }

                final float x = ev.getX();
                final float dx = x - mLastMotionX;
                final float xDiff = Math.abs(dx);
                final float y = ev.getY();
                final float yDiff = Math.abs(y - mInitialMotionY);

                if (dx != 0 && !isGutterDrag(mLastMotionX, dx)
                        && canScroll(this, false, (int) dx, (int) x, (int) y)) {
                    // Nested view has scrollable area under this point. Let it be handled there.
                    mLastMotionX = x;
                    mLastMotionY = y;
                    mIsUnableToDrag = true;
                    return false;
                }
                if (xDiff > mTouchSlop && xDiff * 0.5f > yDiff) {
                    mIsBeingDragged = true;
                    requestParentDisallowInterceptTouchEvent(true);
                    setScrollState(SCROLL_STATE_DRAGGING);
                    mLastMotionX = dx > 0
                            ? mInitialMotionX + mTouchSlop : mInitialMotionX - mTouchSlop;
                    mLastMotionY = y;
                } else if (yDiff > mTouchSlop) {
                    // The finger has moved enough in the vertical
                    // direction to be counted as a drag...  abort
                    // any attempt to drag horizontally, to work correctly
                    // with children that have scrolling containers.
                    mIsUnableToDrag = true;
                }
                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    if (performDrag(x, y)) {
                        postInvalidateOnAnimation();
                    }
                }
            }
            case MotionEvent.ACTION_DOWN -> {
                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionX = mInitialMotionX = ev.getX();
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = 0;
                mIsUnableToDrag = false;

                mIsScrollStarted = true;
                mScroller.computeScrollOffset();
                if (mScrollState == SCROLL_STATE_SETTLING
                        && Math.abs(mScroller.getFinalX() - mScroller.getCurrX()) > mCloseEnough) {
                    // Let the user 'catch' the pager as it animates.
                    mScroller.abortAnimation();
                    mPopulatePending = false;
                    populate();
                    mIsBeingDragged = true;
                    requestParentDisallowInterceptTouchEvent(true);
                    setScrollState(SCROLL_STATE_DRAGGING);
                } else if (mLeftEdge.getDistance() != 0
                        || mRightEdge.getDistance() != 0) {
                    // Caught the edge glow animation
                    mIsBeingDragged = true;
                    setScrollState(SCROLL_STATE_DRAGGING);
                    if (mLeftEdge.getDistance() != 0) {
                        mLeftEdge.onPullDistance(0f, 1 - mLastMotionY / getHeight());
                    }
                    if (mRightEdge.getDistance() != 0) {
                        mRightEdge.onPullDistance(0f, mLastMotionY / getHeight());
                    }
                } else {
                    completeScroll(false);
                    mIsBeingDragged = false;
                }

            }
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        if (mFakeDragging) {
            // A fake drag is in progress already, ignore this real one
            // but still eat the touch events.
            // (It is likely that the user is multi-touching the screen.)
            return true;
        }

        if (mAdapter == null || mAdapter.getCount() == 0) {
            // Nothing to present or scroll; nothing to touch.
            return false;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();
        boolean needsInvalidate = false;

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN -> {
                mScroller.abortAnimation();
                mPopulatePending = false;
                populate();

                // Remember where the motion event started
                mLastMotionX = mInitialMotionX = ev.getX();
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = ev.getPointerId(0);
            }
            case MotionEvent.ACTION_MOVE -> {
                if (!mIsBeingDragged) {
                    final float x = ev.getX();
                    final float xDiff = Math.abs(x - mLastMotionX);
                    final float y = ev.getY();
                    final float yDiff = Math.abs(y - mLastMotionY);
                    if (xDiff > mTouchSlop && xDiff > yDiff) {
                        mIsBeingDragged = true;
                        requestParentDisallowInterceptTouchEvent(true);
                        mLastMotionX = x - mInitialMotionX > 0 ? mInitialMotionX + mTouchSlop :
                                mInitialMotionX - mTouchSlop;
                        mLastMotionY = y;
                        setScrollState(SCROLL_STATE_DRAGGING);

                        // Disallow Parent Intercept, just in case
                        ViewParent parent = getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }
                    }
                }
                // Not else! Note that mIsBeingDragged can be set above.
                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    final float x = ev.getX();
                    needsInvalidate |= performDrag(x, ev.getY());
                }
            }
            case MotionEvent.ACTION_UP -> {
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) velocityTracker.getXVelocity();
                    mPopulatePending = true;
                    final int width = getClientWidth();
                    final float scrollX = getScrollX();
                    final float scrolledPages = scrollX / width;
                    final ItemInfo ii = infoForCurrentScrollPosition();
                    final float marginOffset = (float) mPageMargin / width;
                    final int currentPage = ii.position;
                    final float nextPageOffset = (scrolledPages - ii.offset) / (ii.widthFactor + marginOffset);
                    final float x = ev.getX();
                    final int totalDelta = (int) (x - mInitialMotionX);
                    final int nextPage = determineTargetPage(
                            currentPage, nextPageOffset, initialVelocity, totalDelta);
                    setCurrentItemInternal(nextPage, true, true, initialVelocity);

                    needsInvalidate = resetTouch();
                    //TODO onAbsorb is buggy, do we need it?
                    /*if (nextPage == currentPage && needsInvalidate) {
                        if (mRightEdge.getDistance() != 0) {
                            mRightEdge.onAbsorb(-initialVelocity);
                        } else if (mLeftEdge.getDistance() != 0) {
                            mLeftEdge.onAbsorb(initialVelocity);
                        }
                    }*/
                }
            }
            case MotionEvent.ACTION_CANCEL -> {
                if (mIsBeingDragged) {
                    scrollToItem(mCurItem, true, 0, false);
                    needsInvalidate = resetTouch();
                }
            }
        }
        if (needsInvalidate) {
            postInvalidateOnAnimation();
        }
        return true;
    }

    private boolean resetTouch() {
        boolean needsInvalidate;
        mActivePointerId = INVALID_POINTER;
        endDrag();
        mLeftEdge.onRelease();
        mRightEdge.onRelease();
        needsInvalidate = !mLeftEdge.isFinished() || !mRightEdge.isFinished();
        return needsInvalidate;
    }

    private void requestParentDisallowInterceptTouchEvent(boolean disallowIntercept) {
        final ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    /**
     * If either of the horizontal edge glows are currently active, this consumes part or all of
     * deltaX on the edge glow.
     *
     * @param deltaX The pointer motion, in pixels, in the horizontal direction, positive
     *               for moving down and negative for moving up.
     * @param y      The vertical position of the pointer.
     * @return The amount of <code>deltaX</code> that has been consumed by the
     * edge glow.
     */
    private float releaseHorizontalGlow(float deltaX, float y) {
        // First allow releasing existing overscroll effect:
        float consumed = 0;
        float displacement = y / getHeight();
        float pullDistance = deltaX / getWidth();
        if (mLeftEdge.getDistance() != 0) {
            consumed = -mLeftEdge.onPullDistance(-pullDistance, 1 - displacement);
        } else if (mRightEdge.getDistance() != 0) {
            consumed = mRightEdge.onPullDistance(pullDistance, displacement);
        }
        return consumed * getWidth();
    }

    private boolean performDrag(float x, float y) {
        boolean needsInvalidate = false;

        final float dX = mLastMotionX - x;
        mLastMotionX = x;
        final float releaseConsumed = releaseHorizontalGlow(dX, y);
        final float deltaX = dX - releaseConsumed;
        if (releaseConsumed != 0) {
            needsInvalidate = true;
        }
        if (Math.abs(deltaX) < 0.0001f) { // ignore rounding errors from releaseHorizontalGlow()
            return needsInvalidate;
        }

        float oldScrollX = getScrollX();
        float scrollX = oldScrollX + deltaX;
        final int width = getClientWidth();

        float leftBound = width * mFirstOffset;
        float rightBound = width * mLastOffset;
        boolean leftAbsolute = true;
        boolean rightAbsolute = true;

        final ItemInfo firstItem = mItems.get(0);
        final ItemInfo lastItem = mItems.get(mItems.size() - 1);
        if (firstItem.position != 0) {
            leftAbsolute = false;
            leftBound = firstItem.offset * width;
        }
        if (lastItem.position != mAdapter.getCount() - 1) {
            rightAbsolute = false;
            rightBound = lastItem.offset * width;
        }

        if (scrollX < leftBound) {
            if (leftAbsolute) {
                float over = leftBound - scrollX;
                mLeftEdge.onPullDistance(over / width, 1 - y / getHeight());
                needsInvalidate = true;
            }
            scrollX = leftBound;
        } else if (scrollX > rightBound) {
            if (rightAbsolute) {
                float over = scrollX - rightBound;
                mRightEdge.onPullDistance(over / width, y / getHeight());
                needsInvalidate = true;
            }
            scrollX = rightBound;
        }
        // Don't lose the rounded component
        mLastMotionX += scrollX - (int) scrollX;
        scrollTo((int) scrollX, getScrollY());
        pageScrolled((int) scrollX);

        return needsInvalidate;
    }

    /**
     * @return Info about the page at the current scroll position.
     * This can be synthetic for a missing middle page; the 'object' field can be null.
     */
    private ItemInfo infoForCurrentScrollPosition() {
        final int width = getClientWidth();
        final float scrollOffset = width > 0 ? (float) getScrollX() / width : 0;
        final float marginOffset = width > 0 ? (float) mPageMargin / width : 0;
        int lastPos = -1;
        float lastOffset = 0.f;
        float lastWidth = 0.f;
        boolean first = true;

        ItemInfo lastItem = null;
        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            float offset;
            if (!first && ii.position != lastPos + 1) {
                // Create a synthetic item for a missing page.
                ii = mTempItem;
                ii.offset = lastOffset + lastWidth + marginOffset;
                ii.position = lastPos + 1;
                ii.widthFactor = mAdapter.getPageWidth(mirrorPosition(ii.position));
                i--;
            }
            offset = ii.offset;

            final float leftBound = offset;
            final float rightBound = offset + ii.widthFactor + marginOffset;
            if (first || scrollOffset >= leftBound) {
                if (scrollOffset < rightBound || i == mItems.size() - 1) {
                    return ii;
                }
            } else {
                return lastItem;
            }
            first = false;
            lastPos = ii.position;
            lastOffset = offset;
            lastWidth = ii.widthFactor;
            lastItem = ii;
        }

        return lastItem;
    }

    private int determineTargetPage(int currentPage, float pageOffset, int velocity, int deltaX) {
        int targetPage;
        if (Math.abs(deltaX) > mFlingDistance && Math.abs(velocity) > mMinimumVelocity
                && mLeftEdge.getDistance() == 0 // don't fling while stretched
                && mRightEdge.getDistance() == 0) {
            targetPage = velocity > 0 ? currentPage : currentPage + 1;
        } else {
            final float truncator = currentPage >= mCurItem ? 0.4f : 0.6f;
            targetPage = currentPage + (int) (pageOffset + truncator);
        }

        if (mItems.size() > 0) {
            final ItemInfo firstItem = mItems.get(0);
            final ItemInfo lastItem = mItems.get(mItems.size() - 1);

            // Only let the user target pages we have items for
            targetPage = MathUtil.clamp(targetPage, firstItem.position, lastItem.position);
        }

        return targetPage;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);
        boolean needsInvalidate = false;

        final int overScrollMode = getOverScrollMode();
        if (overScrollMode == View.OVER_SCROLL_ALWAYS
                || (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS
                && mAdapter != null && mAdapter.getCount() > 1)) {
            if (!mLeftEdge.isFinished()) {
                final int restoreCount = canvas.save();
                final int height = getHeight() - getPaddingTop() - getPaddingBottom();
                // Fixed by Modern UI
                final int width = getClientWidth();

                canvas.rotate(270);
                // Fixed by Modern UI
                canvas.translate(-height + getPaddingTop(), mFirstOffset * width + getPaddingRight());
                mLeftEdge.setSize(/* width= */height, /* height= */width);
                needsInvalidate |= mLeftEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
            }
            if (!mRightEdge.isFinished()) {
                final int restoreCount = canvas.save();
                // Fixed by Modern UI
                final int width = getClientWidth();
                final int height = getHeight() - getPaddingTop() - getPaddingBottom();

                canvas.rotate(90);
                // Fixed by Modern UI
                canvas.translate(-getPaddingTop(), -(mLastOffset + 1) * width - getPaddingLeft());
                mRightEdge.setSize(/* width= */height, /* height= */width);
                needsInvalidate |= mRightEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
            }
        } else {
            mLeftEdge.finish();
            mRightEdge.finish();
        }

        if (needsInvalidate) {
            // Keep animating
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Draw the margin drawable between pages if needed.
        if (mPageMargin > 0 && mMarginDrawable != null && mItems.size() > 0 && mAdapter != null) {
            final int scrollX = getScrollX();
            final int width = getWidth();

            final float marginOffset = (float) mPageMargin / width;
            int itemIndex = 0;
            ItemInfo ii = mItems.get(0);
            float offset = ii.offset;
            final int itemCount = mItems.size();
            final int firstPos = ii.position;
            final int lastPos = mItems.get(itemCount - 1).position;
            for (int pos = firstPos; pos < lastPos; pos++) {
                while (pos > ii.position && itemIndex < itemCount) {
                    ii = mItems.get(++itemIndex);
                }

                float drawAt;
                if (pos == ii.position) {
                    drawAt = (ii.offset + ii.widthFactor) * width;
                    offset = ii.offset + ii.widthFactor + marginOffset;
                } else {
                    float widthFactor = mAdapter.getPageWidth(mirrorPosition(pos));
                    drawAt = (offset + widthFactor) * width;
                    offset += widthFactor + marginOffset;
                }

                if (drawAt + mPageMargin > scrollX) {
                    mMarginDrawable.setBounds(Math.round(drawAt), mTopPageBounds,
                            Math.round(drawAt + mPageMargin), mBottomPageBounds);
                    mMarginDrawable.draw(canvas);
                }

                if (drawAt > scrollX + width) {
                    break; // No more visible, no sense in continuing
                }
            }
        }
    }

    /**
     * Start a fake drag of the pager.
     *
     * <p>A fake drag can be useful if you want to synchronize the motion of the ViewPager
     * with the touch scrolling of another view, while still letting the ViewPager
     * control the snapping motion and fling behavior. (e.g. parallax-scrolling tabs.)
     * Call {@link #fakeDragBy(float)} to simulate the actual drag motion. Call
     * {@link #endFakeDrag()} to complete the fake drag and fling as necessary.
     *
     * <p>During a fake drag the ViewPager will ignore all touch events. If a real drag
     * is already in progress, this method will return false.
     *
     * @return true if the fake drag began successfully, false if it could not be started.
     * @see #fakeDragBy(float)
     * @see #endFakeDrag()
     */
    public boolean beginFakeDrag() {
        if (mIsBeingDragged) {
            return false;
        }
        mFakeDragging = true;
        setScrollState(SCROLL_STATE_DRAGGING);
        mInitialMotionX = mLastMotionX = 0;
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
        final long time = Core.timeNanos();
        final MotionEvent ev = MotionEvent.obtain(time, MotionEvent.ACTION_DOWN, 0, 0, 0);
        mVelocityTracker.addMovement(ev);
        ev.recycle();
        mFakeDragBeginTime = time;
        return true;
    }

    /**
     * End a fake drag of the pager.
     *
     * @see #beginFakeDrag()
     * @see #fakeDragBy(float)
     */
    public void endFakeDrag() {
        if (!mFakeDragging) {
            throw new IllegalStateException("No fake drag in progress. Call beginFakeDrag first.");
        }

        if (mAdapter != null) {
            final VelocityTracker velocityTracker = mVelocityTracker;
            velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
            int initialVelocity = (int) velocityTracker.getXVelocity();
            mPopulatePending = true;
            final int width = getClientWidth();
            final int scrollX = getScrollX();
            final ItemInfo ii = infoForCurrentScrollPosition();
            final int currentPage = ii.position;
            final float pageOffset = (((float) scrollX / width) - ii.offset) / ii.widthFactor;
            final int totalDelta = (int) (mLastMotionX - mInitialMotionX);
            int nextPage = determineTargetPage(currentPage, pageOffset, initialVelocity,
                    totalDelta);
            setCurrentItemInternal(nextPage, true, true, initialVelocity);
        }
        endDrag();

        mFakeDragging = false;
    }

    /**
     * Fake drag by an offset in pixels. You must have called {@link #beginFakeDrag()} first.
     *
     * @param xOffset Offset in pixels to drag by.
     * @see #beginFakeDrag()
     * @see #endFakeDrag()
     */
    public void fakeDragBy(float xOffset) {
        if (!mFakeDragging) {
            throw new IllegalStateException("No fake drag in progress. Call beginFakeDrag first.");
        }

        if (mAdapter == null) {
            return;
        }

        mLastMotionX += xOffset;

        float oldScrollX = getScrollX();
        float scrollX = oldScrollX - xOffset;
        final int width = getClientWidth();

        float leftBound = width * mFirstOffset;
        float rightBound = width * mLastOffset;

        final ItemInfo firstItem = mItems.get(0);
        final ItemInfo lastItem = mItems.get(mItems.size() - 1);
        if (firstItem.position != 0) {
            leftBound = firstItem.offset * width;
        }
        if (lastItem.position != mAdapter.getCount() - 1) {
            rightBound = lastItem.offset * width;
        }

        if (scrollX < leftBound) {
            scrollX = leftBound;
        } else if (scrollX > rightBound) {
            scrollX = rightBound;
        }
        // Don't lose the rounded component
        mLastMotionX += scrollX - (int) scrollX;
        scrollTo((int) scrollX, getScrollY());
        pageScrolled((int) scrollX);

        // Synthesize an event for the VelocityTracker.
        final long time = Core.timeNanos();
        final MotionEvent ev = MotionEvent.obtain(time, MotionEvent.ACTION_MOVE,
                mLastMotionX, 0, 0);
        mVelocityTracker.addMovement(ev);
        ev.recycle();
    }

    /**
     * Returns true if a fake drag is in progress.
     *
     * @return true if currently in a fake drag, false otherwise.
     * @see #beginFakeDrag()
     * @see #fakeDragBy(float)
     * @see #endFakeDrag()
     */
    public boolean isFakeDragging() {
        return mFakeDragging;
    }

    private void endDrag() {
        mIsBeingDragged = false;
        mIsUnableToDrag = false;

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    /**
     * Check if this ViewPager can be scrolled horizontally in a certain direction.
     *
     * @param direction Negative to check scrolling left, positive to check scrolling right.
     * @return Whether this ViewPager can be scrolled in the specified direction. It will always
     * return false if the specified direction is 0.
     */
    @Override
    public boolean canScrollHorizontally(int direction) {
        if (mAdapter == null) {
            return false;
        }

        final int width = getClientWidth();
        final int scrollX = getScrollX();
        if (direction < 0) {
            return (scrollX > (int) (width * mFirstOffset));
        } else if (direction > 0) {
            return (scrollX < (int) (width * mLastOffset));
        } else {
            return false;
        }
    }

    /**
     * Tests scrollability within child views of v given a delta of dx.
     *
     * @param v      View to test for horizontal scrollability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true),
     *               or just its children (false).
     * @param dx     Delta scrolled in pixels
     * @param x      X coordinate of the active touch point
     * @param y      Y coordinate of the active touch point
     * @return true if child views of v can be scrolled by delta of dx.
     */
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof final ViewGroup group) {
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            for (int i = count - 1; i >= 0; i--) {
                // TODO: Add versioned support here for transformed views.
                // This will not work for transformed views in Honeycomb+
                final View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight()
                        && y + scrollY >= child.getTop() && y + scrollY < child.getBottom()
                        && canScroll(child, true, dx, x + scrollX - child.getLeft(),
                        y + scrollY - child.getTop())) {
                    return true;
                }
            }
        }

        return checkV && v.canScrollHorizontally(-dx);
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        // Let the focused view and/or our descendants get the key first
        return super.dispatchKeyEvent(event) || executeKeyEvent(event);
    }

    /**
     * You can call this function yourself to have the scroll view perform
     * scrolling from a key event, just as if the event had been dispatched to
     * it by the view hierarchy.
     *
     * @param event The key event to execute.
     * @return Return true if the event was handled, else false.
     */
    public boolean executeKeyEvent(@NonNull KeyEvent event) {
        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEY_LEFT -> {
                    if (event.hasModifiers(KeyEvent.META_ALT_ON)) {
                        handled = pageLeft();
                    } else {
                        handled = arrowScroll(FOCUS_LEFT);
                    }
                }
                case KeyEvent.KEY_RIGHT -> {
                    if (event.hasModifiers(KeyEvent.META_ALT_ON)) {
                        handled = pageRight();
                    } else {
                        handled = arrowScroll(FOCUS_RIGHT);
                    }
                }
                case KeyEvent.KEY_TAB -> {
                    // Added RTL support
                    boolean isLayoutRtl = isLayoutRtl();
                    if (event.hasNoModifiers()) {
                        handled = arrowScroll(isLayoutRtl ? FOCUS_BACKWARD : FOCUS_FORWARD);
                    } else if (event.hasModifiers(KeyEvent.META_SHIFT_ON)) {
                        handled = arrowScroll(isLayoutRtl ? FOCUS_FORWARD : FOCUS_BACKWARD);
                    }
                }
            }
        }
        return handled;
    }

    /**
     * Handle scrolling in response to a left or right arrow click.
     *
     * @param direction The direction corresponding to the arrow key that was pressed. It should be
     *                  either {@link View#FOCUS_LEFT} or {@link View#FOCUS_RIGHT}.
     * @return Whether the scrolling was handled successfully.
     */
    public boolean arrowScroll(int direction) {
        View currentFocused = findFocus();
        if (currentFocused == this) {
            currentFocused = null;
        } else if (currentFocused != null) {
            boolean isChild = false;
            for (ViewParent parent = currentFocused.getParent(); parent instanceof ViewGroup;
                 parent = parent.getParent()) {
                if (parent == this) {
                    isChild = true;
                    break;
                }
            }
            if (!isChild) {
                // This would cause the focus search down below to fail in fun ways.
                final StringBuilder sb = new StringBuilder();
                sb.append(currentFocused.getClass().getSimpleName());
                for (ViewParent parent = currentFocused.getParent(); parent instanceof ViewGroup;
                     parent = parent.getParent()) {
                    sb.append(" => ").append(parent.getClass().getSimpleName());
                }
                LOGGER.warn(MARKER, "arrowScroll tried to find focus based on non-child "
                        + "current focused view " + sb);
                currentFocused = null;
            }
        }

        boolean handled = false;

        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused,
                direction);
        if (nextFocused != null && nextFocused != currentFocused) {
            if (direction == View.FOCUS_LEFT) {
                // If there is nothing to the left, or this is causing us to
                // jump to the right, then what we really want to do is page left.
                final int nextLeft = getChildRectInPagerCoordinates(mTempRect, nextFocused).left;
                final int currLeft = getChildRectInPagerCoordinates(mTempRect, currentFocused).left;
                if (currentFocused != null && nextLeft >= currLeft) {
                    handled = pageLeft();
                } else {
                    handled = nextFocused.requestFocus();
                }
            } else if (direction == View.FOCUS_RIGHT) {
                // If there is nothing to the right, or this is causing us to
                // jump to the left, then what we really want to do is page right.
                final int nextLeft = getChildRectInPagerCoordinates(mTempRect, nextFocused).left;
                final int currLeft = getChildRectInPagerCoordinates(mTempRect, currentFocused).left;
                if (currentFocused != null && nextLeft <= currLeft) {
                    handled = pageRight();
                } else {
                    handled = nextFocused.requestFocus();
                }
            }
        } else if (direction == FOCUS_LEFT || direction == FOCUS_BACKWARD) {
            // Trying to move left and nothing there; try to page.
            handled = pageLeft();
        } else if (direction == FOCUS_RIGHT || direction == FOCUS_FORWARD) {
            // Trying to move right and nothing there; try to page.
            handled = pageRight();
        }
        if (handled) {
            //TODO
            //playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
        }
        return handled;
    }

    private Rect getChildRectInPagerCoordinates(Rect outRect, View child) {
        if (outRect == null) {
            outRect = new Rect();
        }
        if (child == null) {
            outRect.set(0, 0, 0, 0);
            return outRect;
        }
        outRect.left = child.getLeft();
        outRect.right = child.getRight();
        outRect.top = child.getTop();
        outRect.bottom = child.getBottom();

        ViewParent parent = child.getParent();
        while (parent != this && parent instanceof final ViewGroup group) {
            outRect.left += group.getLeft();
            outRect.right += group.getRight();
            outRect.top += group.getTop();
            outRect.bottom += group.getBottom();

            parent = group.getParent();
        }
        return outRect;
    }

    boolean pageLeft() {
        return setCurrentItemInternal(mCurItem - 1, true, false);
    }

    boolean pageRight() {
        return setCurrentItemInternal(mCurItem + 1, true, false);
    }

    @Override
    public void onRtlPropertiesChanged(@ResolvedLayoutDir int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        if (mLayoutDirection != layoutDirection) {
            int curItem = 0;
            if (mAdapter != null && !mFirstLayout) {
                curItem = getCurrentItem();
            }
            mLayoutDirection = layoutDirection;
            if (mAdapter != null && !mFirstLayout) {
                // If getItemPosition() is implemented correctly by user, then dataSetChanged()
                // is enough to handle RTL change.
                dataSetChanged();
                // If it returns POSITION_NONE, the item will be reinitialized and this is needed,
                // otherwise this is a no-op.
                setCurrentItem(curItem, false);
            }
        }
    }

    int mirrorPosition(int position) {
        if (mAdapter != null && mLayoutDirection == LAYOUT_DIRECTION_RTL) {
            return mAdapter.getCount() - position - 1;
        } else {
            return position;
        }
    }

    /**
     * We only want the current page that is being shown to be focusable.
     */
    @Override
    public void addFocusables(@NonNull ArrayList<View> views, int direction, int focusableMode) {
        final int focusableCount = views.size();

        final int descendantFocusability = getDescendantFocusability();

        if (descendantFocusability != FOCUS_BLOCK_DESCENDANTS) {
            for (int i = 0; i < getChildCount(); i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() == VISIBLE) {
                    ItemInfo ii = infoForChild(child);
                    if (ii != null && ii.position == mCurItem) {
                        child.addFocusables(views, direction, focusableMode);
                    }
                }
            }
        }

        // we add ourselves (if focusable) in all cases except for when we are
        // FOCUS_AFTER_DESCENDANTS and there are some descendants focusable.  this is
        // to avoid the focus search finding layouts when a more precise search
        // among the focusable children would be more interesting.
        if (descendantFocusability != FOCUS_AFTER_DESCENDANTS
                || (focusableCount == views.size())) { // No focusable descendants
            // Note that we can't call the superclass here, because it will
            // add all views in.  So we need to do the same thing View does.
            if (!isFocusable()) {
                return;
            }
            if ((focusableMode & FOCUSABLES_TOUCH_MODE) == FOCUSABLES_TOUCH_MODE
                    && isInTouchMode() && !isFocusableInTouchMode()) {
                return;
            }
            views.add(this);
        }
    }

    /**
     * We only want the current page that is being shown to be touchable.
     */
    @Override
    public void addTouchables(@NonNull ArrayList<View> views) {
        // Note that we don't call super.addTouchables(), which means that
        // we don't call View.addTouchables().  This is okay because a ViewPager
        // is itself not touchable.
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE) {
                ItemInfo ii = infoForChild(child);
                if (ii != null && ii.position == mCurItem) {
                    child.addTouchables(views);
                }
            }
        }
    }

    /**
     * We only want the current page that is being shown to be focusable.
     */
    @Override
    protected boolean onRequestFocusInDescendants(int direction,
                                                  Rect previouslyFocusedRect) {
        int index;
        int increment;
        int end;
        int count = getChildCount();
        if ((direction & FOCUS_FORWARD) != 0) {
            index = 0;
            increment = 1;
            end = count;
        } else {
            index = count - 1;
            increment = -1;
            end = -1;
        }
        for (int i = index; i != end; i += increment) {
            View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE) {
                ItemInfo ii = infoForChild(child);
                if (ii != null && ii.position == mCurItem) {
                    if (child.requestFocus(direction, previouslyFocusedRect)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @NonNull
    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @NonNull
    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(@NonNull ViewGroup.LayoutParams p) {
        return generateDefaultLayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    private class PagerObserver implements DataSetObserver {
        PagerObserver() {
        }

        @Override
        public void onChanged() {
            dataSetChanged();
        }

        @Override
        public void onInvalidated() {
            dataSetChanged();
        }
    }

    /**
     * Layout parameters that should be supplied for views added to a
     * ViewPager.
     */
    public static class LayoutParams extends ViewGroup.LayoutParams {
        /**
         * true if this view is a decoration on the pager itself and not
         * a view supplied by the adapter.
         */
        public boolean isDecor;

        /**
         * Gravity setting for use on decor views only:
         * Where to position the view page within the overall ViewPager
         * container; constants are defined in {@link Gravity}.
         */
        public int gravity;

        /**
         * Width as a 0-1 multiplier of the measured pager width
         */
        float widthFactor = 0.f;

        /**
         * true if this view was added during layout and needs to be measured
         * before being positioned.
         */
        boolean needsMeasure;

        /**
         * Adapter position this view is for if !isDecor
         */
        int position;

        /**
         * Current child index within the ViewPager that this view occupies
         */
        int childIndex;

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }
    }
}
