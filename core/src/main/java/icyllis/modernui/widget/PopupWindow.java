/*
 * Modern UI.
 * Copyright (C) 2022-2025 BloCamLimb. All rights reserved.
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
 *   Copyright (C) 2007 The Android Open Source Project
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
import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.core.Window;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.StateListDrawable;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.TypedArray;
import icyllis.modernui.transition.Transition;
import icyllis.modernui.transition.TransitionListener;
import icyllis.modernui.transition.TransitionManager;
import icyllis.modernui.util.AttributeSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.View;
import icyllis.modernui.view.View.OnTouchListener;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.view.ViewGroup.LayoutParams;
import icyllis.modernui.view.ViewParent;
import icyllis.modernui.view.ViewTreeObserver;
import icyllis.modernui.view.WindowGroup;
import icyllis.modernui.view.WindowManager;

import java.lang.ref.WeakReference;

import static icyllis.modernui.view.ViewGroup.LayoutParams.*;

/**
 * <p>
 * This class represents a popup window that can be used to display an
 * arbitrary view. The popup window is a floating container that appears on top
 * of the current application window.
 * </p>
 * <a name="Animation"></a>
 * <h3>Animation</h3>
 * <p>
 * Popup window enter and exit transitions may be specified by calling either
 * {@link #setEnterTransition(Transition)} or {@link #setExitTransition(Transition)}
 * and passing a {@link Transition}.
 * </p>
 * <p>
 * This is a modified version from Android. Modern UI implementations require the
 * root view must be a {@link WindowGroup} for its behavior to intercept input events.
 * We don't need its anchoring feature, because it will calculate all transformation matrices.
 * Firstly, the performance is slightly lower, and we don't want the position of pop-up window
 * to change too frequently.
 * </p>
 *
 * @see Spinner
 */
public class PopupWindow {

    private static final int DEFAULT_ANCHORED_GRAVITY = Gravity.TOP | Gravity.START;

    private final int[] mTmpDrawingLocation = new int[2];
    private final int[] mTmpScreenLocation = new int[2];
    private final int[] mTmpAppLocation = new int[2];
    private final Rect mTempRect = new Rect();

    private Context mContext;
    private WindowManager mWindowManager;

    boolean mIsShowing;
    boolean mIsTransitioningToDismiss;
    boolean mIsDropdown;

    /**
     * View that handles event dispatch and content transitions.
     */
    private DecorView mDecorView;

    /**
     * View that holds the background and may animate during a transition.
     */
    private View mBackgroundView;

    /**
     * The contents of the popup. May be identical to the background view.
     */
    private View mContentView;

    private boolean mFocusable;
    private boolean mTouchable = true;
    private boolean mOutsideTouchable = false;
    private boolean mClippingEnabled = true;
    private boolean mClipToScreen;
    private boolean mNotTouchModal;

    private OnTouchListener mTouchInterceptor;

    private int mWidth = WRAP_CONTENT;
    private int mHeight = WRAP_CONTENT;

    private float mElevation;

    private Drawable mBackground;
    private Drawable mAboveAnchorBackgroundDrawable;
    private Drawable mBelowAnchorBackgroundDrawable;

    private Transition mEnterTransition;
    private Transition mExitTransition;
    private Rect mEpicenterBounds;

    private boolean mAboveAnchor;
    private int mWindowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;

    private OnDismissListener mOnDismissListener;

    private int mGravity = Gravity.NO_GRAVITY;

    private static final int[] ABOVE_ANCHOR_STATE_SET = {
            R.attr.state_above_anchor
    };

    private final View.OnAttachStateChangeListener mOnAnchorDetachedListener =
            new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    // Anchor might have been reattached in a different position.
                    alignToAnchor();
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    // Leave the popup in its current position.
                    // The anchor might become attached again.
                }
            };

    private final View.OnAttachStateChangeListener mOnAnchorRootDetachedListener =
            new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    mIsAnchorRootAttached = false;
                }
            };

    private WeakReference<View> mAnchor;
    private WeakReference<View> mAnchorRoot;
    private boolean mIsAnchorRootAttached;

    private final ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener = () -> {
        //TODO WindowGroup is buggy, view tree will be laid out frequently. Disable aligning now.
        //alignToAnchor();
    };

    private final View.OnLayoutChangeListener mOnLayoutChangeListener =
            (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                //TODO WindowGroup is buggy, view tree will be laid out frequently. Disable aligning now.
                //alignToAnchor();
            };

    private int mAnchorXOff;
    private int mAnchorYOff;
    private int mAnchoredGravity;
    private boolean mOverlapAnchor;

    private boolean mPopupViewInitialLayoutDirectionInherited;

    private static final String[] STYLEABLE = {
            /*0*/R.ns, R.attr.overlapAnchor,
            /*1*/R.ns, R.attr.popupBackground,
            /*2*/R.ns, R.attr.popupElevation,
            /*3*/R.ns, R.attr.popupEnterTransition,
            /*4*/R.ns, R.attr.popupExitTransition,
    };

    public PopupWindow(Context context) {
        this(context, null);
    }

    public PopupWindow(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, null);
    }

    public PopupWindow(Context context, @Nullable AttributeSet attrs,
                       @Nullable @AttrRes ResourceId defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    public PopupWindow(Context context, @Nullable AttributeSet attrs,
                       @Nullable @AttrRes ResourceId defStyleAttr,
                       @Nullable @StyleRes ResourceId defStyleRes) {
        mContext = context;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        final TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, defStyleAttr, defStyleRes, STYLEABLE);
        final Drawable bg = a.getDrawable(1);
        mElevation = a.getDimension(2, 0);
        mOverlapAnchor = a.getBoolean(0, false);

        a.recycle();

        setBackgroundDrawable(bg);
    }

    /**
     * <p>Create a new empty, non focusable popup window of dimension (0,0).</p>
     *
     * <p>The popup does not provide any background. This should be handled
     * by the content view.</p>
     */
    public PopupWindow() {
        this(null, 0, 0);
    }

    /**
     * <p>Create a new non focusable popup window which can display the
     * <tt>contentView</tt>. The dimension of the window are (0,0).</p>
     *
     * <p>The popup does not provide any background. This should be handled
     * by the content view.</p>
     *
     * @param contentView the popup's content
     */
    public PopupWindow(View contentView) {
        this(contentView, 0, 0);
    }

    /**
     * <p>Create a new empty, non focusable popup window. The dimension of the
     * window must be passed to this constructor.</p>
     *
     * <p>The popup does not provide any background. This should be handled
     * by the content view.</p>
     *
     * @param width  the popup's width
     * @param height the popup's height
     */
    public PopupWindow(int width, int height) {
        this(null, width, height);
    }

    /**
     * <p>Create a new non focusable popup window which can display the
     * <tt>contentView</tt>. The dimension of the window must be passed to
     * this constructor.</p>
     *
     * <p>The popup does not provide any background. This should be handled
     * by the content view.</p>
     *
     * @param contentView the popup's content
     * @param width       the popup's width
     * @param height      the popup's height
     */
    public PopupWindow(View contentView, int width, int height) {
        this(contentView, width, height, false);
    }

    /**
     * <p>Create a new popup window which can display the <tt>contentView</tt>.
     * The dimension of the window must be passed to this constructor.</p>
     *
     * <p>The popup does not provide any background. This should be handled
     * by the content view.</p>
     *
     * @param contentView the popup's content
     * @param width       the popup's width
     * @param height      the popup's height
     * @param focusable   true if the popup can be focused, false otherwise
     */
    public PopupWindow(View contentView, int width, int height, boolean focusable) {
        if (contentView != null) {
            mContext = contentView.getContext();
            mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        }

        setContentView(contentView);
        setWidth(width);
        setHeight(height);
        setFocusable(focusable);
    }

    /**
     * Sets the enter transition to be used when the popup window is shown.
     *
     * @param enterTransition the enter transition, or {@code null} to clear
     * @see #getEnterTransition()
     */
    public void setEnterTransition(@Nullable Transition enterTransition) {
        mEnterTransition = enterTransition;
    }

    /**
     * Returns the enter transition to be used when the popup window is shown.
     *
     * @return the enter transition, or {@code null} if not set
     * @see #setEnterTransition(Transition)
     */
    @Nullable
    public Transition getEnterTransition() {
        return mEnterTransition;
    }

    /**
     * Sets the exit transition to be used when the popup window is dismissed.
     *
     * @param exitTransition the exit transition, or {@code null} to clear
     * @see #getExitTransition()
     */
    public void setExitTransition(@Nullable Transition exitTransition) {
        mExitTransition = exitTransition;
    }

    /**
     * Returns the exit transition to be used when the popup window is
     * dismissed.
     *
     * @return the exit transition, or {@code null} if not set
     * @see #setExitTransition(Transition)
     */
    @Nullable
    public Transition getExitTransition() {
        return mExitTransition;
    }

    /**
     * <p>Returns bounds which are used as a center of the enter and exit transitions.<p/>
     *
     * <p>Transitions use Rect, referred to as the epicenter, to orient
     * the direction of travel. For popup windows, the anchor view bounds are
     * used as the default epicenter.</p>
     *
     * <p>See {@link Transition#setEpicenterCallback(Transition.EpicenterCallback)} for more
     * information about how transition epicenters work.</p>
     *
     * @return bounds relative to anchor view, or {@code null} if not set
     * @see #setEpicenterBounds(Rect)
     */
    @Nullable
    public Rect getEpicenterBounds() {
        return mEpicenterBounds != null ? mEpicenterBounds.copy() : null;
    }

    /**
     * <p>Sets the bounds used as the epicenter of the enter and exit transitions.</p>
     *
     * <p>Transitions use Rect, referred to as the epicenter, to orient
     * the direction of travel. For popup windows, the anchor view bounds are
     * used as the default epicenter.</p>
     *
     * <p>See {@link Transition#setEpicenterCallback(Transition.EpicenterCallback)} for more
     * information about how transition epicenters work.</p>
     *
     * @param bounds the epicenter bounds relative to the anchor view, or
     *               {@code null} to use the default epicenter
     * @see #getEpicenterBounds()
     */
    public void setEpicenterBounds(@Nullable Rect bounds) {
        mEpicenterBounds = bounds != null ? bounds.copy() : null;
    }

    /**
     * Return the drawable used as the popup window's background.
     *
     * @return the background drawable or {@code null} if not set
     * @see #setBackgroundDrawable(Drawable)
     */
    @Nullable
    public Drawable getBackground() {
        return mBackground;
    }

    /**
     * Specifies the background drawable for this popup window. The background
     * can be set to {@code null}.
     *
     * @param background the popup's background
     * @see #getBackground()
     */
    public void setBackgroundDrawable(@Nullable Drawable background) {
        mBackground = background;

        // If this is a StateListDrawable, try to find and store the drawable to be
        // used when the drop-down is placed above its anchor view, and the one to be
        // used when the drop-down is placed below its anchor view. We extract
        // the drawables ourselves to work around a problem with using refreshDrawableState
        // that it will take into account the padding of all drawables specified in a
        // StateListDrawable, thus adding superfluous padding to drop-down views.
        //
        // We assume a StateListDrawable will have a drawable for ABOVE_ANCHOR_STATE_SET and
        // at least one other drawable, intended for the 'below-anchor state'.
        if (mBackground instanceof StateListDrawable stateList) {

            // Find the above-anchor view - this one's easy, it should be labeled as such.
            int aboveAnchorStateIndex = stateList.findStateDrawableIndex(ABOVE_ANCHOR_STATE_SET);

            // Now, for the below-anchor view, look for any other drawable specified in the
            // StateListDrawable which is not for the above-anchor state and use that.
            int count = stateList.getStateCount();
            int belowAnchorStateIndex = -1;
            for (int i = 0; i < count; i++) {
                if (i != aboveAnchorStateIndex) {
                    belowAnchorStateIndex = i;
                    break;
                }
            }

            // Store the drawables we found, if we found them. Otherwise, set them both
            // to null so that we'll just use refreshDrawableState.
            if (aboveAnchorStateIndex != -1 && belowAnchorStateIndex != -1) {
                mAboveAnchorBackgroundDrawable = stateList.getStateDrawable(aboveAnchorStateIndex);
                mBelowAnchorBackgroundDrawable = stateList.getStateDrawable(belowAnchorStateIndex);
            } else {
                mBelowAnchorBackgroundDrawable = null;
                mAboveAnchorBackgroundDrawable = null;
            }
        }
    }

    /**
     * @return the elevation for this popup window in pixels
     * @see #setElevation(float)
     */
    public float getElevation() {
        return mElevation;
    }

    /**
     * Specifies the elevation for this popup window.
     *
     * @param elevation the popup's elevation in pixels
     * @see #getElevation()
     */
    public void setElevation(float elevation) {
        mElevation = elevation;
    }

    /**
     * <p>Return the view used as the content of the popup window.</p>
     *
     * @return a {@link View} representing the popup's content
     * @see #setContentView(View)
     */
    public View getContentView() {
        return mContentView;
    }

    /**
     * <p>Change the popup's content. The content is represented by an instance
     * of {@link View}.</p>
     *
     * <p>This method has no effect if called when the popup is showing.</p>
     *
     * @param contentView the new content for the popup
     * @see #getContentView()
     * @see #isShowing()
     */
    public void setContentView(View contentView) {
        if (mIsShowing) {
            return;
        }

        mContentView = contentView;

        if (mContext == null && mContentView != null) {
            mContext = mContentView.getContext();
        }

        if (mWindowManager == null && mContentView != null) {
            mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        }
    }

    /**
     * Set a callback for all touch events being dispatched to the popup
     * window.
     */
    public void setTouchInterceptor(@Nullable OnTouchListener l) {
        mTouchInterceptor = l;
    }

    /**
     * <p>Indicate whether the popup window can grab the focus.</p>
     *
     * @return true if the popup is focusable, false otherwise
     * @see #setFocusable(boolean)
     */
    public boolean isFocusable() {
        return mFocusable;
    }

    /**
     * <p>Changes the focusability of the popup window. When focusable, the
     * window will grab the focus from the current focused widget if the popup
     * contains a focusable {@link View}.  By default a popup
     * window is not focusable.</p>
     *
     * <p>If the popup is showing, calling this method will take effect only
     * the next time the popup is shown or through a manual call to one of
     * the {@link #update()} methods.</p>
     *
     * @param focusable true if the popup should grab focus, false otherwise.
     * @see #isFocusable()
     * @see #isShowing()
     * @see #update()
     */
    public void setFocusable(boolean focusable) {
        mFocusable = focusable;
    }

    /**
     * <p>Indicates whether the popup window receives touch events.</p>
     *
     * @return true if the popup is touchable, false otherwise
     * @see #setTouchable(boolean)
     */
    public boolean isTouchable() {
        return mTouchable;
    }

    /**
     * <p>Changes the touchability of the popup window. When touchable, the
     * window will receive touch events, otherwise touch events will go to the
     * window below it. By default the window is touchable.</p>
     *
     * <p>If the popup is showing, calling this method will take effect only
     * the next time the popup is shown or through a manual call to one of
     * the {@link #update()} methods.</p>
     *
     * @param touchable true if the popup should receive touch events, false otherwise
     * @see #isTouchable()
     * @see #isShowing()
     * @see #update()
     */
    public void setTouchable(boolean touchable) {
        mTouchable = touchable;
    }

    /**
     * <p>Indicates whether the popup window will be informed of touch events
     * outside of its window.</p>
     *
     * @return true if the popup is outside touchable, false otherwise
     * @see #setOutsideTouchable(boolean)
     */
    public boolean isOutsideTouchable() {
        return mOutsideTouchable;
    }

    /**
     * <p>Controls whether the pop-up will be informed of touch events outside
     * of its window.  This only makes sense for pop-ups that are touchable
     * but not focusable, which means touches outside of the window will
     * be delivered to the window behind.  The default is false.</p>
     *
     * <p>If the popup is showing, calling this method will take effect only
     * the next time the popup is shown or through a manual call to one of
     * the {@link #update()} methods.</p>
     *
     * @param touchable true if the popup should receive outside
     *                  touch events, false otherwise
     * @see #isOutsideTouchable()
     * @see #isShowing()
     * @see #update()
     */
    public void setOutsideTouchable(boolean touchable) {
        mOutsideTouchable = touchable;
    }

    /**
     * <p>Indicates whether clipping of the popup window is enabled.</p>
     *
     * @return true if the clipping is enabled, false otherwise
     * @see #setClippingEnabled(boolean)
     */
    public boolean isClippingEnabled() {
        return mClippingEnabled;
    }

    /**
     * <p>Allows the popup window to extend beyond the bounds of the screen. By default the
     * window is clipped to the screen boundaries. Setting this to false will allow windows to be
     * accurately positioned.</p>
     *
     * <p>If the popup is showing, calling this method will take effect only
     * the next time the popup is shown or through a manual call to one of
     * the {@link #update()} methods.</p>
     *
     * @param enabled false if the window should be allowed to extend outside of the screen
     * @see #isShowing()
     * @see #isClippingEnabled()
     * @see #update()
     */
    public void setClippingEnabled(boolean enabled) {
        mClippingEnabled = enabled;
    }

    /**
     * <p>Indicates whether this popup will be clipped to the screen and not to the
     * containing window<p/>
     *
     * @return true if popup will be clipped to the screen instead of the window, false otherwise
     * @see #setIsClippedToScreen(boolean)
     */
    public boolean isClippedToScreen() {
        return mClipToScreen;
    }

    /**
     * <p>Clip this popup window to the screen, but not to the containing window.</p>
     *
     * <p>If the popup is showing, calling this method will take effect only
     * the next time the popup is shown or through a manual call to one of
     * the {@link #update()} methods.</p>
     *
     * @param enabled true to clip to the screen.
     * @see #isClippedToScreen()
     */
    public void setIsClippedToScreen(boolean enabled) {
        mClipToScreen = enabled;
    }

    /**
     * Set the layout type for this window.
     * <p>
     * See {@link Window} for possible values.
     *
     * @param layoutType Layout type for this window.
     * @see Window
     */
    public void setWindowLayoutType(int layoutType) {
        mWindowLayoutType = layoutType;
    }

    /**
     * Returns the layout type for this window.
     *
     * @see #setWindowLayoutType(int)
     */
    public int getWindowLayoutType() {
        return mWindowLayoutType;
    }

    /**
     * <p>Indicates whether outside touches will be sent to this window
     * or other windows behind it<p/>
     *
     * @return true if touches will be sent to this window, false otherwise
     * @see #setTouchModal(boolean)
     */
    public boolean isTouchModal() {
        return !mNotTouchModal;
    }

    /**
     * <p>Set whether this window is touch modal or if outside touches will be sent to
     * other windows behind it.<p/>
     *
     * <p>If the popup is showing, calling this method will take effect only
     * the next time the popup is shown or through a manual call to one of
     * the {@link #update()} methods.</p>
     *
     * @param touchModal true to send all outside touches to this window,
     *                   false to other windows behind it
     * @see #isTouchModal()
     */
    public void setTouchModal(boolean touchModal) {
        mNotTouchModal = !touchModal;
    }

    /**
     * Returns the popup's requested height. May be a layout constant such as
     * {@link LayoutParams#WRAP_CONTENT} or {@link LayoutParams#MATCH_PARENT}.
     * <p>
     * The actual size of the popup may depend on other factors such as
     * clipping and window layout.
     *
     * @return the popup height in pixels or a layout constant
     * @see #setHeight(int)
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Sets the popup's requested height. May be a layout constant such as
     * {@link LayoutParams#WRAP_CONTENT} or {@link LayoutParams#MATCH_PARENT}.
     * <p>
     * The actual size of the popup may depend on other factors such as
     * clipping and window layout.
     * <p>
     * If the popup is showing, calling this method will take effect the next
     * time the popup is shown.
     *
     * @param height the popup height in pixels or a layout constant
     * @see #getHeight()
     * @see #isShowing()
     */
    public void setHeight(int height) {
        mHeight = height;
    }

    /**
     * Returns the popup's requested width. May be a layout constant such as
     * {@link LayoutParams#WRAP_CONTENT} or {@link LayoutParams#MATCH_PARENT}.
     * <p>
     * The actual size of the popup may depend on other factors such as
     * clipping and window layout.
     *
     * @return the popup width in pixels or a layout constant
     * @see #setWidth(int)
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Sets the popup's requested width. May be a layout constant such as
     * {@link LayoutParams#WRAP_CONTENT} or {@link LayoutParams#MATCH_PARENT}.
     * <p>
     * The actual size of the popup may depend on other factors such as
     * clipping and window layout.
     * <p>
     * If the popup is showing, calling this method will take effect the next
     * time the popup is shown.
     *
     * @param width the popup width in pixels or a layout constant
     * @see #getWidth()
     * @see #isShowing()
     */
    public void setWidth(int width) {
        mWidth = width;
    }

    /**
     * Sets whether the popup window should overlap its anchor view when
     * displayed as a drop-down.
     * <p>
     * If the popup is showing, calling this method will take effect only
     * the next time the popup is shown.
     *
     * @param overlapAnchor Whether the popup should overlap its anchor.
     * @see #getOverlapAnchor()
     * @see #isShowing()
     */
    public void setOverlapAnchor(boolean overlapAnchor) {
        mOverlapAnchor = overlapAnchor;
    }

    /**
     * Returns whether the popup window should overlap its anchor view when
     * displayed as a drop-down.
     *
     * @return Whether the popup should overlap its anchor.
     * @see #setOverlapAnchor(boolean)
     */
    public boolean getOverlapAnchor() {
        return mOverlapAnchor;
    }

    /**
     * <p>Indicate whether this popup window is showing on screen.</p>
     *
     * @return true if the popup is showing, false otherwise
     */
    public boolean isShowing() {
        return mIsShowing;
    }

    /**
     * <p>
     * Display the content view in a popup window at the specified location. If the popup window
     * cannot fit on screen, it will be clipped. Specifying a gravity of
     * {@link Gravity#NO_GRAVITY} is similar to specifying <code>Gravity.LEFT | Gravity.TOP</code>.
     * </p>
     *
     * @param parent  a parent view to get the window
     * @param gravity the gravity which controls the placement of the popup window
     * @param x       the popup's x location offset
     * @param y       the popup's y location offset
     */
    public void showAtLocation(@NonNull View parent, int gravity, int x, int y) {
        if (mIsShowing || mContentView == null) {
            return;
        }

        TransitionManager.endTransitions(mDecorView);

        detachFromAnchor();

        mIsShowing = true;
        mIsDropdown = false;
        mGravity = gravity;

        final WindowManager.LayoutParams p = createPopupLayoutParams();
        p.gravity = gravity;
        p.x = x;
        p.y = y;
        preparePopup();

        invokePopup(p);
    }

    /**
     * Display the content view in a popup window anchored to the bottom-left
     * corner of the anchor view. If there is not enough room on screen to show
     * the popup in its entirety, this method tries to find a parent scroll
     * view to scroll. If no parent scroll view can be scrolled, the
     * bottom-left corner of the popup is pinned in the top left corner of the
     * anchor view.
     *
     * @param anchor the view on which to pin the popup window
     * @see #dismiss()
     */
    public final void showAsDropDown(@NonNull View anchor) {
        showAsDropDown(anchor, 0, 0, DEFAULT_ANCHORED_GRAVITY);
    }

    /**
     * Display the content view in a popup window anchored to the bottom-left
     * corner of the anchor view offset by the specified x and y coordinates.
     * If there is not enough room on screen to show the popup in its entirety,
     * this method tries to find a parent scroll view to scroll. If no parent
     * scroll view can be scrolled, the bottom-left corner of the popup is
     * pinned in the top left corner of the anchor view.
     * <p>
     * If the view later scrolls to move <code>anchor</code> to a different
     * location, the popup will be moved correspondingly.
     *
     * @param anchor the view on which to pin the popup window
     * @param xOff   A horizontal offset from the anchor in pixels
     * @param yOff   A vertical offset from the anchor in pixels
     * @see #dismiss()
     */
    public final void showAsDropDown(@NonNull View anchor, int xOff, int yOff) {
        showAsDropDown(anchor, xOff, yOff, DEFAULT_ANCHORED_GRAVITY);
    }

    /**
     * Displays the content view in a popup window anchored to the corner of
     * another view. The window is positioned according to the specified
     * gravity and offset by the specified x and y coordinates.
     * <p>
     * If there is not enough room on screen to show the popup in its entirety,
     * this method tries to find a parent scroll view to scroll. If no parent
     * view can be scrolled, the specified vertical gravity will be ignored and
     * the popup will anchor itself such that it is visible.
     * <p>
     * If the view later scrolls to move <code>anchor</code> to a different
     * location, the popup will be moved correspondingly.
     *
     * @param anchor  the view on which to pin the popup window
     * @param xOff    A horizontal offset from the anchor in pixels
     * @param yOff    A vertical offset from the anchor in pixels
     * @param gravity Alignment of the popup relative to the anchor
     * @see #dismiss()
     */
    public void showAsDropDown(@NonNull View anchor, int xOff, int yOff, int gravity) {
        if (mIsShowing || mContentView == null) {
            return;
        }

        TransitionManager.endTransitions(mDecorView);

        attachToAnchor(anchor, xOff, yOff, gravity);

        mIsShowing = true;
        mIsDropdown = true;

        final var p = createPopupLayoutParams();
        preparePopup();

        final boolean aboveAnchor = findDropDownPosition(anchor, p, xOff, yOff,
                p.width, p.height, gravity, true);
        updateAboveAnchor(aboveAnchor);

        invokePopup(p);
    }

    final void updateAboveAnchor(boolean aboveAnchor) {
        if (aboveAnchor != mAboveAnchor) {
            mAboveAnchor = aboveAnchor;

            if (mBackground != null && mBackgroundView != null) {
                // If the background drawable provided was a StateListDrawable
                // with above-anchor and below-anchor states, use those.
                // Otherwise, rely on refreshDrawableState to do the job.
                if (mAboveAnchorBackgroundDrawable != null) {
                    if (mAboveAnchor) {
                        mBackgroundView.setBackground(mAboveAnchorBackgroundDrawable);
                    } else {
                        mBackgroundView.setBackground(mBelowAnchorBackgroundDrawable);
                    }
                } else {
                    mBackgroundView.refreshDrawableState();
                }
            }
        }
    }

    /**
     * Indicates whether the popup is showing above (the y coordinate of the popup's bottom
     * is less than the y coordinate of the anchor) or below the anchor view (the y coordinate
     * of the popup is greater than y coordinate of the anchor's bottom).
     * <p>
     * The value returned
     * by this method is meaningful only after {@link #showAsDropDown(View)}
     * or {@link #showAsDropDown(View, int, int)} was invoked.
     *
     * @return True if this popup is showing above the anchor view, false otherwise.
     */
    public boolean isAboveAnchor() {
        return mAboveAnchor;
    }

    /**
     * Prepare the popup by embedding it into a new ViewGroup if the background
     * drawable is not null. If embedding is required, the layout parameters'
     * height is modified to take into account the background's padding.
     */
    private void preparePopup() {
        // The old decor view may be transitioning out. Make sure it finishes
        // and cleans up before we try to create another one.
        if (mDecorView != null) {
            mDecorView.cancelTransitions();
        }

        // When a background is available, we embed the content view within
        // another view that owns the background drawable.
        if (mBackground != null) {
            mBackgroundView = createBackgroundView(mContentView);
            mBackgroundView.setBackground(mBackground);
        } else {
            mBackgroundView = mContentView;
        }

        mDecorView = createDecorView(mBackgroundView);
        mDecorView.setIsRootNamespace(true);

        // The background owner should be elevated so that it casts a shadow.
        mBackgroundView.setElevation(mElevation);

        mDecorView.setFocusable(mFocusable);

        mPopupViewInitialLayoutDirectionInherited =
                (mContentView.getRawLayoutDirection() == View.LAYOUT_DIRECTION_INHERIT);
    }

    /**
     * Wraps a content view in a PopupViewContainer.
     *
     * @param contentView the content view to wrap
     * @return a PopupViewContainer that wraps the content view
     */
    @NonNull
    private BackgroundView createBackgroundView(@NonNull View contentView) {
        final LayoutParams layoutParams = mContentView.getLayoutParams();
        final int height;
        if (layoutParams != null && layoutParams.height == WRAP_CONTENT) {
            height = WRAP_CONTENT;
        } else {
            height = MATCH_PARENT;
        }

        final var backgroundView = new BackgroundView(mContext);
        backgroundView.addView(contentView, MATCH_PARENT, height);

        return backgroundView;
    }

    /**
     * Wraps a content view in a FrameLayout.
     *
     * @param contentView the content view to wrap
     * @return a FrameLayout that wraps the content view
     */
    @NonNull
    private DecorView createDecorView(View contentView) {
        final ViewGroup.LayoutParams layoutParams = mContentView.getLayoutParams();
        final int height;
        if (layoutParams != null && layoutParams.height == WRAP_CONTENT) {
            height = WRAP_CONTENT;
        } else {
            height = MATCH_PARENT;
        }

        final var decorView = new DecorView(mContext);
        decorView.addView(contentView, MATCH_PARENT, height);
        decorView.setClipChildren(false);
        decorView.setClipToPadding(false);

        return decorView;
    }

    /**
     * <p>Invoke the popup window by adding the content view to the window
     * manager.</p>
     *
     * <p>The content view must be non-null when this method is invoked.</p>
     *
     * @param p the layout parameters of the popup's content view
     */
    private void invokePopup(@NonNull WindowManager.LayoutParams p) {
        final DecorView decorView = mDecorView;

        setLayoutDirectionFromAnchor();

        mWindowManager.addView(decorView, p);

        if (mEnterTransition != null) {
            decorView.requestEnterTransition(mEnterTransition);
        }
    }

    private void setLayoutDirectionFromAnchor() {
        if (mAnchor != null) {
            View anchor = mAnchor.get();
            if (anchor != null && mPopupViewInitialLayoutDirectionInherited) {
                mDecorView.setLayoutDirection(anchor.getLayoutDirection());
            }
        }
    }

    private int computeGravity() {
        int gravity = mGravity == Gravity.NO_GRAVITY ? Gravity.START | Gravity.TOP : mGravity;
        if (mIsDropdown && (mClipToScreen || mClippingEnabled)) {
            gravity |= Gravity.DISPLAY_CLIP_VERTICAL;
        }
        return gravity;
    }

    /**
     * <p>Generate the layout parameters for the popup window.</p>
     *
     * @return the layout parameters to pass to the window manager
     */
    @NonNull
    final WindowManager.LayoutParams createPopupLayoutParams() {
        final var p = new WindowManager.LayoutParams();

        // These gravity settings put the view in the top left corner of the
        // screen. The view is then positioned to the appropriate location by
        // setting the x and y offsets to match the anchor's bottom-left
        // corner.
        p.gravity = computeGravity();
        p.flags = computeFlags(p.flags);
        p.type = mWindowLayoutType;

        p.height = mHeight;
        p.width = mWidth;

        return p;
    }

    private int computeFlags(int curFlags) {
        curFlags &= ~(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        if (!mFocusable) {
            curFlags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        if (mNotTouchModal) {
            curFlags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        }
        return curFlags;
    }

    /**
     * Positions the popup window on screen. When the popup window is too tall
     * to fit under the anchor, a parent scroll view is seeked and scrolled up
     * to reclaim space. If scrolling is not possible or not enough, the popup
     * window gets moved on top of the anchor.
     * <p>
     * The results of positioning are placed in {@code outParams}.
     *
     * @param anchor      the view on which the popup window must be anchored
     * @param outParams   the layout parameters used to display the drop down
     * @param xOffset     absolute horizontal offset from the left of the anchor
     * @param yOffset     absolute vertical offset from the top of the anchor
     * @param gravity     horizontal gravity specifying popup alignment
     * @param allowScroll whether the anchor view's parent may be scrolled
     *                    when the popup window doesn't fit on screen
     * @return true if the popup is translated upwards to fit on screen
     * @hide
     */
    boolean findDropDownPosition(@NonNull View anchor, WindowManager.LayoutParams outParams,
                                 int xOffset, int yOffset, int width, int height, int gravity,
                                 boolean allowScroll) {
        final int anchorHeight = anchor.getHeight();
        final int anchorWidth = anchor.getWidth();
        if (mOverlapAnchor) {
            yOffset -= anchorHeight;
        }

        // Initially, align to the bottom-left corner of the anchor plus offsets.
        final int[] appScreenLocation = mTmpAppLocation;
        final View appRootView = anchor.getRootView();
        appRootView.getLocationInWindow(appScreenLocation);

        final int[] screenLocation = mTmpScreenLocation;
        anchor.getLocationInWindow(screenLocation);

        final int[] drawingLocation = mTmpDrawingLocation;
        drawingLocation[0] = screenLocation[0] - appScreenLocation[0];
        drawingLocation[1] = screenLocation[1] - appScreenLocation[1];
        outParams.x = (drawingLocation[0] + xOffset);
        outParams.y = drawingLocation[1] + anchorHeight + yOffset;

        // Let the window manager know to align the top to y.
        outParams.gravity = computeGravity();
        outParams.width = width;
        outParams.height = height;

        // If we need to adjust for gravity RIGHT, align to the bottom-right
        // corner of the anchor (still accounting for offsets).
        final int hGrav = Gravity.getAbsoluteGravity(gravity, anchor.getLayoutDirection())
                & Gravity.HORIZONTAL_GRAVITY_MASK;
        if (hGrav == Gravity.RIGHT) {
            outParams.x -= width - anchorWidth;
        }

        // First, attempt to fit the popup vertically without resizing.
        final boolean fitsVertical = tryFitVertical(outParams, yOffset, height,
                anchorHeight, drawingLocation[1], screenLocation[1], appScreenLocation[1],
                appScreenLocation[1] + appRootView.getHeight(), false);

        // Next, attempt to fit the popup horizontally without resizing.
        final boolean fitsHorizontal = tryFitHorizontal(outParams, width,
                drawingLocation[0], screenLocation[0], appScreenLocation[0],
                appScreenLocation[0] + appRootView.getWidth(), false);

        // If the popup still doesn't fit, attempt to scroll the parent.
        if (!fitsVertical || !fitsHorizontal) {
            final int scrollX = anchor.getScrollX();
            final int scrollY = anchor.getScrollY();
            final Rect r = new Rect(scrollX, scrollY, scrollX + width + xOffset,
                    scrollY + height + anchorHeight + yOffset);
            if (allowScroll && anchor.requestRectangleOnScreen(r, true)) {
                // Reset for the new anchor position.
                anchor.getLocationInWindow(screenLocation);
                drawingLocation[0] = screenLocation[0] - appScreenLocation[0];
                drawingLocation[1] = screenLocation[1] - appScreenLocation[1];
                outParams.x = drawingLocation[0] + xOffset;
                outParams.y = drawingLocation[1] + anchorHeight + yOffset;

                // Preserve the gravity adjustment.
                if (hGrav == Gravity.RIGHT) {
                    outParams.x -= width - anchorWidth;
                }
            }

            // Try to fit the popup again and allowing resizing.
            tryFitVertical(outParams, yOffset, height, anchorHeight, drawingLocation[1],
                    screenLocation[1], appScreenLocation[1],
                    appScreenLocation[1] + appRootView.getHeight(), mClipToScreen);
            tryFitHorizontal(outParams, width, drawingLocation[0],
                    screenLocation[0], appScreenLocation[0],
                    appScreenLocation[0] + appRootView.getWidth(), mClipToScreen);
        }

        // Return whether the popup's top edge is above the anchor's top edge.
        return outParams.y < drawingLocation[1];
    }

    private boolean tryFitVertical(@NonNull WindowManager.LayoutParams outParams, int yOffset, int height,
                                   int anchorHeight, int drawingLocationY, int screenLocationY, int displayFrameTop,
                                   int displayFrameBottom, boolean allowResize) {
        final int winOffsetY = screenLocationY - drawingLocationY;
        final int anchorTopInScreen = outParams.y + winOffsetY;
        final int spaceBelow = displayFrameBottom - anchorTopInScreen;
        if (anchorTopInScreen >= displayFrameTop && height <= spaceBelow) {
            return true;
        }

        final int spaceAbove = anchorTopInScreen - anchorHeight - displayFrameTop;
        if (height <= spaceAbove) {
            // Move everything up.
            if (mOverlapAnchor) {
                yOffset += anchorHeight;
            }
            outParams.y = drawingLocationY - height + yOffset;

            return true;
        }

        return positionInDisplayVertical(outParams, height, drawingLocationY, screenLocationY,
                displayFrameTop, displayFrameBottom, allowResize);
    }

    private boolean positionInDisplayVertical(@NonNull WindowManager.LayoutParams outParams, int height,
                                              int drawingLocationY, int screenLocationY, int displayFrameTop,
                                              int displayFrameBottom,
                                              boolean canResize) {
        boolean fitsInDisplay = true;

        final int winOffsetY = screenLocationY - drawingLocationY;
        outParams.y += winOffsetY;
        outParams.height = height;

        final int bottom = outParams.y + height;
        if (bottom > displayFrameBottom) {
            // The popup is too far down, move it back in.
            outParams.y -= bottom - displayFrameBottom;
        }

        if (outParams.y < displayFrameTop) {
            // The popup is too far up, move it back in and clip if
            // it's still too large.
            outParams.y = displayFrameTop;

            final int displayFrameHeight = displayFrameBottom - displayFrameTop;
            if (canResize && height > displayFrameHeight) {
                outParams.height = displayFrameHeight;
            } else {
                fitsInDisplay = false;
            }
        }

        outParams.y -= winOffsetY;

        return fitsInDisplay;
    }

    private boolean tryFitHorizontal(@NonNull WindowManager.LayoutParams outParams, int width,
                                     int drawingLocationX, int screenLocationX, int displayFrameLeft,
                                     int displayFrameRight, boolean allowResize) {
        final int winOffsetX = screenLocationX - drawingLocationX;
        final int anchorLeftInScreen = outParams.x + winOffsetX;
        final int spaceRight = displayFrameRight - anchorLeftInScreen;
        if (anchorLeftInScreen >= displayFrameLeft && width <= spaceRight) {
            return true;
        }

        return positionInDisplayHorizontal(outParams, width, drawingLocationX, screenLocationX,
                displayFrameLeft, displayFrameRight, allowResize);
    }

    private boolean positionInDisplayHorizontal(@NonNull WindowManager.LayoutParams outParams, int width,
                                                int drawingLocationX, int screenLocationX, int displayFrameLeft,
                                                int displayFrameRight,
                                                boolean canResize) {
        boolean fitsInDisplay = true;

        // Use screen coordinates for comparison against display frame.
        final int winOffsetX = screenLocationX - drawingLocationX;
        outParams.x += winOffsetX;

        final int right = outParams.x + width;
        if (right > displayFrameRight) {
            // The popup is too far right, move it back in.
            outParams.x -= right - displayFrameRight;
        }

        if (outParams.x < displayFrameLeft) {
            // The popup is too far left, move it back in and clip if it's
            // still too large.
            outParams.x = displayFrameLeft;

            final int displayFrameWidth = displayFrameRight - displayFrameLeft;
            if (canResize && width > displayFrameWidth) {
                outParams.width = displayFrameWidth;
            } else {
                fitsInDisplay = false;
            }
        }

        outParams.x -= winOffsetX;

        return fitsInDisplay;
    }

    /**
     * Returns the maximum height that is available for the popup to be
     * completely shown. It is recommended that this height be the maximum for
     * the popup's height, otherwise it is possible that the popup will be
     * clipped.
     *
     * @param anchor The view on which the popup window must be anchored.
     * @return The maximum available height for the popup to be completely
     * shown.
     */
    public int getMaxAvailableHeight(@NonNull View anchor) {
        return getMaxAvailableHeight(anchor, 0);
    }

    /**
     * Returns the maximum height that is available for the popup to be
     * completely shown. It is recommended that this height be the maximum for
     * the popup's height, otherwise it is possible that the popup will be
     * clipped.
     *
     * @param anchor  The view on which the popup window must be anchored.
     * @param yOffset y offset from the view's bottom edge
     * @return The maximum available height for the popup to be completely
     * shown.
     */
    public int getMaxAvailableHeight(@NonNull View anchor, int yOffset) {
        final View appView = anchor.getRootView();

        final int[] anchorPos = mTmpDrawingLocation;
        anchor.getLocationInWindow(anchorPos);

        final int bottomEdge = appView.getBottom();

        final int distanceToBottom;
        if (mOverlapAnchor) {
            distanceToBottom = bottomEdge - anchorPos[1] - yOffset;
        } else {
            distanceToBottom = bottomEdge - (anchorPos[1] + anchor.getHeight()) - yOffset;
        }
        final int distanceToTop = anchorPos[1] - appView.getTop() + yOffset;

        // anchorPos[1] is distance from anchor to top of screen
        int returnedHeight = Math.max(distanceToBottom, distanceToTop);
        if (mBackground != null) {
            mBackground.getPadding(mTempRect);
            returnedHeight -= mTempRect.top + mTempRect.bottom;
        }

        return returnedHeight;
    }

    /**
     * Disposes of the popup window. This method can be invoked only after
     * {@link #showAsDropDown(View)} has been executed. Failing
     * that, calling this method will have no effect.
     *
     * @see #showAsDropDown(View)
     */
    public void dismiss() {
        if (!mIsShowing || mIsTransitioningToDismiss) {
            return;
        }

        final DecorView decorView = mDecorView;
        final View contentView = mContentView;

        final ViewGroup contentHolder;
        final ViewParent contentParent = contentView.getParent();
        if (contentParent instanceof ViewGroup) {
            contentHolder = ((ViewGroup) contentParent);
        } else {
            contentHolder = null;
        }

        // Ensure any ongoing or pending transitions are canceled.
        decorView.cancelTransitions();

        mIsShowing = false;
        mIsTransitioningToDismiss = true;

        // This method may be called as part of window detachment, in which
        // case the anchor view (and its root) will still return true from
        // isAttachedToWindow() during execution of this method; however, we
        // can expect the OnAttachStateChangeListener to have been called prior
        // to executing this method, so we can rely on that instead.
        final Transition exitTransition = mExitTransition;
        if (exitTransition != null && decorView.isLaidOut()
                && (mIsAnchorRootAttached || mAnchorRoot == null)) {

            decorView.setFocusable(false);

            final View anchorRoot = mAnchorRoot != null ? mAnchorRoot.get() : null;
            final Rect epicenter = getTransitionEpicenter();

            // Once we start dismissing the decor view, all state (including
            // the anchor root) needs to be moved to the decor view since we
            // may open another popup while it's busy exiting.
            decorView.startExitTransition(exitTransition, anchorRoot, epicenter,
                    new TransitionListener() {
                        @Override
                        public void onTransitionEnd(@NonNull Transition transition) {
                            dismissImmediate(decorView, contentHolder, contentView);
                        }
                    });
        } else {
            dismissImmediate(decorView, contentHolder, contentView);
        }

        // Clears the anchor view.
        detachFromAnchor();

        if (mOnDismissListener != null) {
            mOnDismissListener.onDismiss();
        }
    }

    /**
     * Returns the window-relative epicenter bounds to be used by enter and
     * exit transitions.
     * <p>
     * <strong>Note:</strong> This is distinct from the rect passed to
     * {@link #setEpicenterBounds(Rect)}, which is anchor-relative.
     *
     * @return the window-relative epicenter bounds to be used by enter and
     * exit transitions
     * @hide
     */
    @Nullable
    protected final Rect getTransitionEpicenter() {
        final View anchor = mAnchor != null ? mAnchor.get() : null;
        final View decor = mDecorView;
        if (anchor == null || decor == null) {
            return null;
        }

        final int[] anchorLocation = mTmpScreenLocation;
        anchor.getLocationInWindow(anchorLocation);
        final int[] popupLocation = mTmpAppLocation;
        mDecorView.getLocationInWindow(popupLocation);

        // Compute the position of the anchor relative to the popup.
        final Rect bounds = new Rect(0, 0, anchor.getWidth(), anchor.getHeight());
        bounds.offset(anchorLocation[0] - popupLocation[0], anchorLocation[1] - popupLocation[1]);

        // Use anchor-relative epicenter, if specified.
        if (mEpicenterBounds != null) {
            final int offsetX = bounds.left;
            final int offsetY = bounds.top;
            bounds.set(mEpicenterBounds);
            bounds.offset(offsetX, offsetY);
        }

        return null;
    }

    /**
     * Removes the popup from the window manager and tears down the supporting
     * view hierarchy, if necessary.
     */
    private void dismissImmediate(@NonNull View decorView, @Nullable ViewGroup contentHolder, View contentView) {
        // If this method gets called and the decor view doesn't have a parent,
        // then it was either never added or was already removed. That should
        // never happen, but it's worth checking to avoid potential crashes.
        if (decorView.getParent() != null) {
            mWindowManager.removeView(decorView);
        }

        if (contentHolder != null) {
            contentHolder.removeView(contentView);
        }

        // This needs to stay until after all transitions have ended since we
        // need the reference to cancel transitions in preparePopup().
        mDecorView = null;
        mBackgroundView = null;
        mIsTransitioningToDismiss = false;
    }

    /**
     * Sets the listener to be called when the window is dismissed.
     *
     * @param onDismissListener The listener.
     */
    public void setOnDismissListener(OnDismissListener onDismissListener) {
        mOnDismissListener = onDismissListener;
    }

    /**
     * @hide
     */
    protected final OnDismissListener getOnDismissListener() {
        return mOnDismissListener;
    }

    /**
     * Updates the state of the popup window, if it is currently being displayed,
     * from the currently set state.
     * <p>
     * This includes:
     * <ul>
     *     <li>{@link #setClippingEnabled(boolean)}</li>
     *     <li>{@link #setFocusable(boolean)}</li>
     *     <li>{@link #setTouchable(boolean)}</li>
     *     <li>{@link #setTouchModal(boolean)}</li>
     *     <li>{@link #setIsClippedToScreen(boolean)}</li>
     * </ul>
     */
    public void update() {
        if (!mIsShowing || mContentView == null) {
            return;
        }

        final WindowManager.LayoutParams p = getDecorViewLayoutParams();

        boolean update = false;

        final int newFlags = computeFlags(p.flags);
        if (newFlags != p.flags) {
            p.flags = newFlags;
            update = true;
        }

        final int newGravity = computeGravity();
        if (newGravity != p.gravity) {
            p.gravity = newGravity;
            update = true;
        }

        if (update) {
            update(mAnchor != null ? mAnchor.get() : null, p);
        }
    }

    /**
     * @hide
     */
    protected void update(View anchor, LayoutParams params) {
        setLayoutDirectionFromAnchor();
        mWindowManager.updateViewLayout(mDecorView, params);
    }

    /**
     * Updates the dimension of the popup window.
     * <p>
     * Calling this function also updates the window with the current popup
     * state as described for {@link #update()}.
     *
     * @param width  the new width in pixels, must be >= 0 or -1 to ignore
     * @param height the new height in pixels, must be >= 0 or -1 to ignore
     */
    public void update(int width, int height) {
        final WindowManager.LayoutParams p = getDecorViewLayoutParams();
        update(p.x, p.y, width, height, false);
    }

    /**
     * Updates the position and the dimension of the popup window.
     * <p>
     * Width and height can be set to -1 to update location only. Calling this
     * function also updates the window with the current popup state as
     * described for {@link #update()}.
     *
     * @param x      the new x location
     * @param y      the new y location
     * @param width  the new width in pixels, must be >= 0 or -1 to ignore
     * @param height the new height in pixels, must be >= 0 or -1 to ignore
     */
    public void update(int x, int y, int width, int height) {
        update(x, y, width, height, false);
    }

    /**
     * Updates the position and the dimension of the popup window.
     * <p>
     * Width and height can be set to -1 to update location only. Calling this
     * function also updates the window with the current popup state as
     * described for {@link #update()}.
     *
     * @param x      the new x location
     * @param y      the new y location
     * @param width  the new width in pixels, must be >= 0 or -1 to ignore
     * @param height the new height in pixels, must be >= 0 or -1 to ignore
     * @param force  {@code true} to reposition the window even if the specified
     *               position already seems to correspond to the LayoutParams,
     *               {@code false} to only reposition if needed
     */
    public void update(int x, int y, int width, int height, boolean force) {
        if (width >= 0) {
            setWidth(width);
        }

        if (height >= 0) {
            setHeight(height);
        }

        if (!mIsShowing || mContentView == null) {
            return;
        }

        final WindowManager.LayoutParams p = getDecorViewLayoutParams();

        boolean update = force;

        if (width != -1 && p.width != mWidth) {
            p.width = mWidth;
            update = true;
        }

        if (height != -1 && p.height != mHeight) {
            p.height = mHeight;
            update = true;
        }

        if (p.x != x) {
            p.x = x;
            update = true;
        }

        if (p.y != y) {
            p.y = y;
            update = true;
        }

        final int newFlags = computeFlags(p.flags);
        if (newFlags != p.flags) {
            p.flags = newFlags;
            update = true;
        }

        final int newGravity = computeGravity();
        if (newGravity != p.gravity) {
            p.gravity = newGravity;
            update = true;
        }

        View anchor = null;

        if (mAnchor != null && mAnchor.get() != null) {
            anchor = mAnchor.get();
        }

        if (update) {
            update(anchor, p);
        }
    }

    /**
     * @hide
     */
    protected boolean hasContentView() {
        return mContentView != null;
    }

    /**
     * @hide
     */
    protected boolean hasDecorView() {
        return mDecorView != null;
    }

    /**
     * @hide
     */
    protected WindowManager.LayoutParams getDecorViewLayoutParams() {
        return (WindowManager.LayoutParams) mDecorView.getLayoutParams();
    }

    /**
     * Updates the position and the dimension of the popup window.
     * <p>
     * Calling this function also updates the window with the current popup
     * state as described for {@link #update()}.
     *
     * @param anchor the popup's anchor view
     * @param width  the new width in pixels, must be >= 0 or -1 to ignore
     * @param height the new height in pixels, must be >= 0 or -1 to ignore
     */
    public void update(View anchor, int width, int height) {
        update(anchor, false, 0, 0, width, height);
    }

    /**
     * Updates the position and the dimension of the popup window.
     * <p>
     * Width and height can be set to -1 to update location only. Calling this
     * function also updates the window with the current popup state as
     * described for {@link #update()}.
     * <p>
     * If the view later scrolls to move {@code anchor} to a different
     * location, the popup will be moved correspondingly.
     *
     * @param anchor the popup's anchor view
     * @param xoff   x offset from the view's left edge
     * @param yoff   y offset from the view's bottom edge
     * @param width  the new width in pixels, must be >= 0 or -1 to ignore
     * @param height the new height in pixels, must be >= 0 or -1 to ignore
     */
    public void update(View anchor, int xoff, int yoff, int width, int height) {
        update(anchor, true, xoff, yoff, width, height);
    }

    private void update(View anchor, boolean updateLocation, int xoff, int yoff,
                        int width, int height) {

        if (!isShowing() || !hasContentView()) {
            return;
        }

        final WeakReference<View> oldAnchor = mAnchor;
        final int gravity = mAnchoredGravity;

        final boolean needsUpdate = updateLocation && (mAnchorXOff != xoff || mAnchorYOff != yoff);
        if (oldAnchor == null || oldAnchor.get() != anchor || (needsUpdate && !mIsDropdown)) {
            attachToAnchor(anchor, xoff, yoff, gravity);
        } else if (needsUpdate) {
            // No need to register again if this is a DropDown, showAsDropDown already did.
            mAnchorXOff = xoff;
            mAnchorYOff = yoff;
        }

        final WindowManager.LayoutParams p = getDecorViewLayoutParams();
        final int oldGravity = p.gravity;
        final int oldWidth = p.width;
        final int oldHeight = p.height;
        final int oldX = p.x;
        final int oldY = p.y;

        // If an explicit width/height has not specified, use the most recent
        // explicitly specified value (either from setWidth/Height or update).
        if (width < 0) {
            width = mWidth;
        }
        if (height < 0) {
            height = mHeight;
        }

        final boolean aboveAnchor = findDropDownPosition(anchor, p, mAnchorXOff, mAnchorYOff,
                width, height, gravity, true);
        updateAboveAnchor(aboveAnchor);

        final boolean paramsChanged = oldGravity != p.gravity || oldX != p.x || oldY != p.y
                || oldWidth != p.width || oldHeight != p.height;

        // If width and mWidth were both < 0 then we have a MATCH_PARENT or
        // WRAP_CONTENT case. findDropDownPosition will have resolved this to
        // absolute values, but we don't want to update mWidth/mHeight to these
        // absolute values.
        final int newWidth = width < 0 ? width : p.width;
        final int newHeight = height < 0 ? height : p.height;
        update(p.x, p.y, newWidth, newHeight, paramsChanged);
    }

    /**
     * Listener that is called when this popup window is dismissed.
     */
    @FunctionalInterface
    public interface OnDismissListener {

        /**
         * Called when this popup window is dismissed.
         */
        void onDismiss();
    }

    void detachFromAnchor() {
        final View anchor = getAnchor();
        if (anchor != null) {
            final ViewTreeObserver treeObserver = anchor.getViewTreeObserver();
            treeObserver.removeOnScrollChangedListener(mOnScrollChangedListener);
            anchor.removeOnAttachStateChangeListener(mOnAnchorDetachedListener);
        }

        final View anchorRoot = mAnchorRoot != null ? mAnchorRoot.get() : null;
        if (anchorRoot != null) {
            anchorRoot.removeOnAttachStateChangeListener(mOnAnchorRootDetachedListener);
            anchorRoot.removeOnLayoutChangeListener(mOnLayoutChangeListener);
        }

        mAnchor = null;
        mAnchorRoot = null;
        mIsAnchorRootAttached = false;
    }

    void attachToAnchor(@NonNull View anchor, int xOff, int yOff, int gravity) {
        detachFromAnchor();

        final ViewTreeObserver treeObserver = anchor.getViewTreeObserver();
        treeObserver.addOnScrollChangedListener(mOnScrollChangedListener);
        anchor.addOnAttachStateChangeListener(mOnAnchorDetachedListener);

        final View anchorRoot = anchor.getRootView();
        anchorRoot.addOnAttachStateChangeListener(mOnAnchorRootDetachedListener);
        anchorRoot.addOnLayoutChangeListener(mOnLayoutChangeListener);

        mAnchor = new WeakReference<>(anchor);
        mAnchorRoot = new WeakReference<>(anchorRoot);
        mIsAnchorRootAttached = anchorRoot.isAttachedToWindow();

        mAnchorXOff = xOff;
        mAnchorYOff = yOff;
        mAnchoredGravity = gravity;
    }

    /**
     * @hide
     */
    @Nullable
    protected View getAnchor() {
        return mAnchor != null ? mAnchor.get() : null;
    }

    private void alignToAnchor() {
        final View anchor = mAnchor != null ? mAnchor.get() : null;
        if (anchor != null && anchor.isAttachedToWindow() && hasDecorView()) {
            final WindowManager.LayoutParams p = getDecorViewLayoutParams();

            updateAboveAnchor(findDropDownPosition(anchor, p, mAnchorXOff, mAnchorYOff,
                    p.width, p.height, mAnchoredGravity, false));
            update(p.x, p.y, -1, -1, true);
        }
    }

    private class DecorView extends FrameLayout {

        private final OnAttachStateChangeListener mOnAnchorRootDetachedListener = new OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                v.removeOnAttachStateChangeListener(this);

                if (isAttachedToWindow()) {
                    TransitionManager.endTransitions(DecorView.this);
                }
            }
        };

        /**
         * Runnable used to clean up listeners after exit transition.
         */
        private Runnable mCleanupAfterExit;

        public DecorView(Context context) {
            super(context);
        }

        @Override
        public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
            if (mTouchInterceptor != null && mTouchInterceptor.onTouch(this, ev)) {
                return true;
            }
            return super.onInterceptTouchEvent(ev);
        }

        @Override
        public boolean onTouchEvent(@NonNull MotionEvent event) {
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            if (event.getAction() == MotionEvent.ACTION_DOWN &&
                    (x < 0 || x >= getWidth() || y < 0 || y >= getHeight())) {
                dismiss();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                dismiss();
                return true;
            }
            return super.onTouchEvent(event);
        }

        @Override
        public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEY_ESCAPE) {
                if (getKeyDispatcherState() == null) {
                    return super.dispatchKeyEvent(event);
                }

                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                    final KeyEvent.DispatcherState state = getKeyDispatcherState();
                    state.startTracking(event, this);
                    return true;
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    final KeyEvent.DispatcherState state = getKeyDispatcherState();
                    if (state.isTracking(event) && !event.isCanceled()) {
                        dismiss();
                        return true;
                    }
                }
            }
            return super.dispatchKeyEvent(event);
        }

        /**
         * Requests that an enter transition run after the next layout pass.
         */
        public void requestEnterTransition(@Nullable Transition transition) {
            final ViewTreeObserver observer = getViewTreeObserver();
            if (transition != null) {
                final Transition enterTransition = transition.clone();

                // Postpone the enter transition after the first layout pass.
                observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        final ViewTreeObserver observer = getViewTreeObserver();
                        observer.removeOnGlobalLayoutListener(this);

                        final Rect epicenter = getTransitionEpicenter();
                        enterTransition.setEpicenterCallback(t -> epicenter);
                        startEnterTransition(enterTransition);
                    }
                });
            }
        }

        /**
         * Starts the pending enter transition, if one is set.
         */
        private void startEnterTransition(@NonNull Transition enterTransition) {
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                enterTransition.addTarget(child);
                child.setTransitionVisibility(View.INVISIBLE);
            }

            TransitionManager.beginDelayedTransition(this, enterTransition);

            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                child.setTransitionVisibility(View.VISIBLE);
            }
        }

        /**
         * Starts an exit transition immediately.
         * <p>
         * <strong>Note:</strong> The transition listener is guaranteed to have
         * its {@code onTransitionEnd} method called even if the transition
         * never starts.
         */
        public void startExitTransition(@NonNull Transition transition,
                                        @Nullable final View anchorRoot, @Nullable final Rect epicenter,
                                        @NonNull final TransitionListener listener) {
            // The anchor view's window may go away while we're executing our
            // transition, in which case we need to end the transition
            // immediately and execute the listener to remove the popup.
            if (anchorRoot != null) {
                anchorRoot.addOnAttachStateChangeListener(mOnAnchorRootDetachedListener);
            }

            // The cleanup runnable MUST be called even if the transition is
            // canceled before it starts (and thus can't call onTransitionEnd).
            mCleanupAfterExit = () -> {
                listener.onTransitionEnd(transition);

                if (anchorRoot != null) {
                    anchorRoot.removeOnAttachStateChangeListener(mOnAnchorRootDetachedListener);
                }

                // The listener was called. Our job here is done.
                mCleanupAfterExit = null;
            };

            final Transition exitTransition = transition.clone();
            exitTransition.addListener(new TransitionListener() {
                @Override
                public void onTransitionEnd(@NonNull Transition t) {
                    t.removeListener(this);

                    // This null check shouldn't be necessary, but it's easier
                    // to check here than it is to test every possible case.
                    if (mCleanupAfterExit != null) {
                        mCleanupAfterExit.run();
                    }
                }
            });
            exitTransition.setEpicenterCallback(t -> epicenter);

            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                exitTransition.addTarget(child);
            }

            TransitionManager.beginDelayedTransition(this, exitTransition);

            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                child.setVisibility(View.INVISIBLE);
            }
        }

        /**
         * Cancels all pending or current transitions.
         */
        public void cancelTransitions() {
            TransitionManager.endTransitions(this);

            // If the cleanup runnable is still around, that means the
            // transition never started. We should run it now to clean up.
            if (mCleanupAfterExit != null) {
                mCleanupAfterExit.run();
            }
        }
    }

    private class BackgroundView extends FrameLayout {

        public BackgroundView(Context context) {
            super(context);
        }

        @NonNull
        @Override
        protected int[] onCreateDrawableState(int extraSpace) {
            if (mAboveAnchor) {
                final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
                View.mergeDrawableStates(drawableState, ABOVE_ANCHOR_STATE_SET);
                return drawableState;
            } else {
                return super.onCreateDrawableState(extraSpace);
            }
        }
    }
}
