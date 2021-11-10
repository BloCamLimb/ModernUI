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

import icyllis.modernui.ModernUI;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.math.Rect;
import icyllis.modernui.platform.Clipboard;
import icyllis.modernui.text.*;
import icyllis.modernui.text.method.*;
import icyllis.modernui.text.style.CharacterStyle;
import icyllis.modernui.view.*;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * A user interface element that displays text to the user and provides user-editable text.
 */
//TODO
public class TextView extends View implements ViewTreeObserver.OnPreDrawListener {

    public static final int ID_CUT = 1;
    public static final int ID_COPY = 2;
    public static final int ID_PASTE = 3;
    public static final int ID_SELECT_ALL = 4;

    @VisibleForTesting
    public static final BoringLayout.Metrics UNKNOWN_BORING = new BoringLayout.Metrics();

    // Minimum or maximum modes.
    private static final int LINES = 1;
    private static final int PIXELS = 2;

    // Maximum text length for single line input.
    private static final int MAX_LENGTH_FOR_SINGLE_LINE_EDIT_TEXT = 5000;
    private InputFilter.LengthFilter mSingleLineLengthFilter = null;

    static final int VERY_WIDE = 1024 * 1024; // XXX should be much larger
    private static final int ANIMATED_SCROLL_GAP = 250;

    private static final InputFilter[] NO_FILTERS = new InputFilter[0];
    private static final Spanned EMPTY_SPANNED = new SpannedString("");

    private static final int CHANGE_WATCHER_PRIORITY = 100;

    private static final int FLOATING_TOOLBAR_SELECT_ALL_REFRESH_DELAY = 500;

    // System-wide time for last cut, copy or text changed action.
    static long sLastCutCopyOrTextChangedTime;

    private int mCurTextColor;

    private int mCurHintTextColor;
    private boolean mFreezesText;

    private Editable.Factory mEditableFactory = Editable.DEFAULT_FACTORY;
    private Spannable.Factory mSpannableFactory = Spannable.DEFAULT_FACTORY;

    private boolean mPreDrawRegistered;
    private boolean mPreDrawListenerDetached;

    // A flag to prevent repeated movements from escaping the enclosing text view. The idea here is
    // that if a user is holding down a movement key to traverse text, we shouldn't also traverse
    // the view hierarchy. On the other hand, if the user is using the movement key to traverse
    // views (i.e. the first movement was to traverse out of this view, or this view was traversed
    // into by the user holding the movement key down) then we shouldn't prevent the focus from
    // changing.
    private boolean mPreventDefaultMovement;

    private TextUtils.TruncateAt mEllipsize;

    private boolean mCursorVisible = true;

    Drawables mDrawables;

    // Do not update following mText/mSpannable/mPrecomputed except for setTextInternal()
    @Nonnull
    private CharSequence mText = "";
    @Nullable
    private Spannable mSpannable;
    @Nullable
    private PrecomputedText mPrecomputed;

    @Nonnull
    private CharSequence mTransformed = "";
    @Nonnull
    private BufferType mBufferType = BufferType.NORMAL;

    @Nullable
    private CharSequence mHint;
    @Nullable
    private Layout mHintLayout;

    @Nullable
    private MovementMethod mMovement;
    @Nullable
    private TransformationMethod mTransformation;

    // lazy-loading
    private ChangeWatcher mChangeWatcher;
    private ArrayList<TextWatcher> mListeners;

    // display attributes
    private final TextPaint mTextPaint = new TextPaint();
    private Layout mLayout;

    // True if fallback fonts that end up getting used should be allowed to affect line spacing.
    boolean mUseFallbackLineSpacing = true;

    private int mGravity = Gravity.TOP | Gravity.START;
    private boolean mHorizontallyScrolling;

    private int mAutoLinkMask;
    private boolean mLinksClickable = true;

    private int mMaximum = Integer.MAX_VALUE;
    private int mMaxMode = LINES;
    private int mMinimum = 0;
    private int mMinMode = LINES;

    private int mOldMaximum = mMaximum;
    private int mOldMaxMode = mMaxMode;

    private int mMaxWidth = Integer.MAX_VALUE;
    private int mMinWidth = 0;

    private boolean mSingleLine;
    private int mDesiredHeightAtMeasure = -1;
    private boolean mIncludePad = true;
    private int mDeferScroll = -1;

    // tmp primitives, so we don't alloc them on each draw
    private Rect mTempRect;
    private long mLastScroll;
    private Scroller mScroller;
    private TextPaint mTempTextPaint;

    private BoringLayout.Metrics mBoring;
    private BoringLayout.Metrics mHintBoring;
    private BoringLayout mSavedLayout;
    private BoringLayout mSavedHintLayout;

    private TextDirectionHeuristic mTextDir;

    @Nonnull
    private InputFilter[] mFilters = NO_FILTERS;

    private boolean mHighlightPathBogus = true;

    /**
     * Created on demand when one of the Editor fields is used.
     * See {@link #createEditorIfNeeded()}.
     */
    private Editor mEditor;

    public TextView() {
    }

    /**
     * Gets the {@link MovementMethod} being used for this TextView,
     * which provides positioning, scrolling, and text selection functionality.
     * This will frequently be null for non-editable TextViews.
     *
     * @return the movement method being used for this TextView.
     * @see MovementMethod
     */
    @Nullable
    public final MovementMethod getMovementMethod() {
        return mMovement;
    }

    /**
     * Sets the {@link MovementMethod} for handling arrow key movement for this TextView.
     * This can be null to disallow using the arrow keys to move the cursor or scroll
     * the view.
     * <p>
     * Be warned that if you want a TextView with a movement method not to be focusable,
     * or if you want a TextView without a movement method to be focusable, you must call
     * {@link #setFocusable} again after calling this to get the focusability back the
     * way you want it.
     *
     * @param movement the movement method to set
     */
    public final void setMovementMethod(@Nullable MovementMethod movement) {
        if (mMovement != movement) {
            mMovement = movement;

            if (movement != null && mSpannable == null) {
                setText(mText);
            }

            if (mMovement != null || (mEditor != null && mBufferType == BufferType.EDITABLE)) {
                setFocusable(FOCUSABLE);
                setClickable(true);
                setLongClickable(true);
            } else {
                setFocusable(FOCUSABLE_AUTO);
                setClickable(false);
                setLongClickable(false);
            }
        }
    }

    /**
     * Gets the current {@link TransformationMethod} for the TextView.
     * This is frequently null, except for single-line and password fields.
     *
     * @return the current transformation method for this TextView.
     */
    @Nullable
    public final TransformationMethod getTransformationMethod() {
        return mTransformation;
    }

    /**
     * Sets the transformation that is applied to the text that this
     * TextView is displaying.
     *
     * @param method the transformation method to set
     */
    public final void setTransformationMethod(@Nullable TransformationMethod method) {
        if (method == mTransformation) {
            // Avoid the setText() below if the transformation is
            // the same.
            return;
        }
        if (mTransformation != null) {
            if (mSpannable != null) {
                mSpannable.removeSpan(mTransformation);
            }
        }

        mTransformation = method;

        setText(mText);

        // PasswordTransformationMethod always have LTR text direction heuristics returned by
        // getTextDirectionHeuristic, needs to be reset
        mTextDir = getTextDirectionHeuristic();
    }

    /**
     * Returns the top padding of the view, plus space for the top
     * Drawable if any.
     */
    public int getCompoundPaddingTop() {
        final Drawables dr = mDrawables;
        if (dr == null || dr.mShowing[Drawables.TOP] == null) {
            return mPaddingTop;
        } else {
            return mPaddingTop + dr.mDrawablePadding + dr.mDrawableSizeTop;
        }
    }

    /**
     * Returns the bottom padding of the view, plus space for the bottom
     * Drawable if any.
     */
    public int getCompoundPaddingBottom() {
        final Drawables dr = mDrawables;
        if (dr == null || dr.mShowing[Drawables.BOTTOM] == null) {
            return mPaddingBottom;
        } else {
            return mPaddingBottom + dr.mDrawablePadding + dr.mDrawableSizeBottom;
        }
    }

    /**
     * Returns the left padding of the view, plus space for the left
     * Drawable if any.
     */
    public int getCompoundPaddingLeft() {
        final Drawables dr = mDrawables;
        if (dr == null || dr.mShowing[Drawables.LEFT] == null) {
            return mPaddingLeft;
        } else {
            return mPaddingLeft + dr.mDrawablePadding + dr.mDrawableSizeLeft;
        }
    }

    /**
     * Returns the right padding of the view, plus space for the right
     * Drawable if any.
     */
    public int getCompoundPaddingRight() {
        final Drawables dr = mDrawables;
        if (dr == null || dr.mShowing[Drawables.RIGHT] == null) {
            return mPaddingRight;
        } else {
            return mPaddingRight + dr.mDrawablePadding + dr.mDrawableSizeRight;
        }
    }

    /**
     * Returns the start padding of the view, plus space for the start
     * Drawable if any.
     */
    public int getCompoundPaddingStart() {
        resolveDrawables();
        if (isLayoutRtl()) {
            return getCompoundPaddingRight();
        } else {
            return getCompoundPaddingLeft();
        }
    }

    /**
     * Returns the end padding of the view, plus space for the end
     * Drawable if any.
     */
    public int getCompoundPaddingEnd() {
        resolveDrawables();
        if (isLayoutRtl()) {
            return getCompoundPaddingLeft();
        } else {
            return getCompoundPaddingRight();
        }
    }

    /**
     * Returns the extended top padding of the view, including both the
     * top Drawable if any and any extra space to keep more than maxLines
     * of text from showing.  It is only valid to call this after measuring.
     */
    public int getExtendedPaddingTop() {
        if (mMaxMode != LINES) {
            return getCompoundPaddingTop();
        }

        if (mLayout == null) {
            assumeLayout();
        }

        if (mLayout.getLineCount() <= mMaximum) {
            return getCompoundPaddingTop();
        }

        int top = getCompoundPaddingTop();
        int bottom = getCompoundPaddingBottom();
        int viewht = getHeight() - top - bottom;
        int layoutht = mLayout.getLineTop(mMaximum);

        if (layoutht >= viewht) {
            return top;
        }

        final int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        if (gravity == Gravity.TOP) {
            return top;
        } else if (gravity == Gravity.BOTTOM) {
            return top + viewht - layoutht;
        } else { // (gravity == Gravity.CENTER_VERTICAL)
            return top + (viewht - layoutht) / 2;
        }
    }

    /**
     * Returns the extended bottom padding of the view, including both the
     * bottom Drawable if any and any extra space to keep more than maxLines
     * of text from showing.  It is only valid to call this after measuring.
     */
    public int getExtendedPaddingBottom() {
        if (mMaxMode != LINES) {
            return getCompoundPaddingBottom();
        }

        if (mLayout == null) {
            assumeLayout();
        }

        if (mLayout.getLineCount() <= mMaximum) {
            return getCompoundPaddingBottom();
        }

        int top = getCompoundPaddingTop();
        int bottom = getCompoundPaddingBottom();
        int viewht = getHeight() - top - bottom;
        int layoutht = mLayout.getLineTop(mMaximum);

        if (layoutht >= viewht) {
            return bottom;
        }

        final int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        if (gravity == Gravity.TOP) {
            return bottom + viewht - layoutht;
        } else if (gravity == Gravity.BOTTOM) {
            return bottom;
        } else { // (gravity == Gravity.CENTER_VERTICAL)
            return bottom + (viewht - layoutht) / 2;
        }
    }

    /**
     * Returns the total left padding of the view, including the left
     * Drawable if any.
     */
    public int getTotalPaddingLeft() {
        return getCompoundPaddingLeft();
    }

    /**
     * Returns the total right padding of the view, including the right
     * Drawable if any.
     */
    public int getTotalPaddingRight() {
        return getCompoundPaddingRight();
    }

    /**
     * Returns the total start padding of the view, including the start
     * Drawable if any.
     */
    public int getTotalPaddingStart() {
        return getCompoundPaddingStart();
    }

    /**
     * Returns the total end padding of the view, including the end
     * Drawable if any.
     */
    public int getTotalPaddingEnd() {
        return getCompoundPaddingEnd();
    }

    /**
     * Returns the total top padding of the view, including the top
     * Drawable if any, the extra space to keep more than maxLines
     * from showing, and the vertical offset for gravity, if any.
     */
    public int getTotalPaddingTop() {
        return getExtendedPaddingTop() + getVerticalOffset(true);
    }

    /**
     * Returns the total bottom padding of the view, including the bottom
     * Drawable if any, the extra space to keep more than maxLines
     * from showing, and the vertical offset for gravity, if any.
     */
    public int getTotalPaddingBottom() {
        return getExtendedPaddingBottom() + getBottomVerticalOffset(true);
    }

    /**
     * Sets the Drawables (if any) to appear to the left of, above, to the
     * right of, and below the text. Use {@code null} if you do not want a
     * Drawable there. The Drawables must already have had
     * {@link Drawable#setBounds} called.
     * <p>
     * Calling this method will overwrite any Drawables previously set using
     * {@link #setCompoundDrawablesRelative} or related methods.
     */
    public void setCompoundDrawables(@Nullable Drawable left, @Nullable Drawable top,
                                     @Nullable Drawable right, @Nullable Drawable bottom) {
        Drawables dr = mDrawables;

        // We're switching to absolute, discard relative.
        if (dr != null) {
            if (dr.mDrawableStart != null) dr.mDrawableStart.setCallback(null);
            dr.mDrawableStart = null;
            if (dr.mDrawableEnd != null) dr.mDrawableEnd.setCallback(null);
            dr.mDrawableEnd = null;
            dr.mDrawableSizeStart = dr.mDrawableHeightStart = 0;
            dr.mDrawableSizeEnd = dr.mDrawableHeightEnd = 0;
        }

        final boolean drawables = left != null || top != null || right != null || bottom != null;
        if (!drawables) {
            // Clearing drawables...  can we free the data structure?
            if (dr != null) {
                if (!dr.hasMetadata()) {
                    mDrawables = null;
                } else {
                    // We need to retain the last set padding, so just clear
                    // out all of the fields in the existing structure.
                    for (int i = dr.mShowing.length - 1; i >= 0; i--) {
                        if (dr.mShowing[i] != null) {
                            dr.mShowing[i].setCallback(null);
                        }
                        dr.mShowing[i] = null;
                    }
                    dr.mDrawableSizeLeft = dr.mDrawableHeightLeft = 0;
                    dr.mDrawableSizeRight = dr.mDrawableHeightRight = 0;
                    dr.mDrawableSizeTop = dr.mDrawableWidthTop = 0;
                    dr.mDrawableSizeBottom = dr.mDrawableWidthBottom = 0;
                }
            }
        } else {
            if (dr == null) {
                mDrawables = dr = new Drawables();
            }

            mDrawables.mOverride = false;

            if (dr.mShowing[Drawables.LEFT] != left && dr.mShowing[Drawables.LEFT] != null) {
                dr.mShowing[Drawables.LEFT].setCallback(null);
            }
            dr.mShowing[Drawables.LEFT] = left;

            if (dr.mShowing[Drawables.TOP] != top && dr.mShowing[Drawables.TOP] != null) {
                dr.mShowing[Drawables.TOP].setCallback(null);
            }
            dr.mShowing[Drawables.TOP] = top;

            if (dr.mShowing[Drawables.RIGHT] != right && dr.mShowing[Drawables.RIGHT] != null) {
                dr.mShowing[Drawables.RIGHT].setCallback(null);
            }
            dr.mShowing[Drawables.RIGHT] = right;

            if (dr.mShowing[Drawables.BOTTOM] != bottom && dr.mShowing[Drawables.BOTTOM] != null) {
                dr.mShowing[Drawables.BOTTOM].setCallback(null);
            }
            dr.mShowing[Drawables.BOTTOM] = bottom;

            final Rect compoundRect = dr.mCompoundRect;
            int[] state;

            state = getDrawableState();

            if (left != null) {
                left.setState(state);
                left.copyBounds(compoundRect);
                left.setCallback(this);
                dr.mDrawableSizeLeft = compoundRect.width();
                dr.mDrawableHeightLeft = compoundRect.height();
            } else {
                dr.mDrawableSizeLeft = dr.mDrawableHeightLeft = 0;
            }

            if (right != null) {
                right.setState(state);
                right.copyBounds(compoundRect);
                right.setCallback(this);
                dr.mDrawableSizeRight = compoundRect.width();
                dr.mDrawableHeightRight = compoundRect.height();
            } else {
                dr.mDrawableSizeRight = dr.mDrawableHeightRight = 0;
            }

            if (top != null) {
                top.setState(state);
                top.copyBounds(compoundRect);
                top.setCallback(this);
                dr.mDrawableSizeTop = compoundRect.height();
                dr.mDrawableWidthTop = compoundRect.width();
            } else {
                dr.mDrawableSizeTop = dr.mDrawableWidthTop = 0;
            }

            if (bottom != null) {
                bottom.setState(state);
                bottom.copyBounds(compoundRect);
                bottom.setCallback(this);
                dr.mDrawableSizeBottom = compoundRect.height();
                dr.mDrawableWidthBottom = compoundRect.width();
            } else {
                dr.mDrawableSizeBottom = dr.mDrawableWidthBottom = 0;
            }
        }

        // Save initial left/right drawables
        if (dr != null) {
            dr.mDrawableLeftInitial = left;
            dr.mDrawableRightInitial = right;
        }

        resetResolvedDrawables();
        resolveDrawables();
        invalidate();
        requestLayout();
    }

    /**
     * Sets the Drawables (if any) to appear to the left of, above, to the
     * right of, and below the text. Use {@code null} if you do not want a
     * Drawable there. The Drawables' bounds will be set to their intrinsic
     * bounds.
     * <p>
     * Calling this method will overwrite any Drawables previously set using
     * {@link #setCompoundDrawablesRelative} or related methods.
     */
    public void setCompoundDrawablesWithIntrinsicBounds(@Nullable Drawable left, @Nullable Drawable top,
                                                        @Nullable Drawable right, @Nullable Drawable bottom) {
        if (left != null) {
            left.setBounds(0, 0, left.getIntrinsicWidth(), left.getIntrinsicHeight());
        }
        if (right != null) {
            right.setBounds(0, 0, right.getIntrinsicWidth(), right.getIntrinsicHeight());
        }
        if (top != null) {
            top.setBounds(0, 0, top.getIntrinsicWidth(), top.getIntrinsicHeight());
        }
        if (bottom != null) {
            bottom.setBounds(0, 0, bottom.getIntrinsicWidth(), bottom.getIntrinsicHeight());
        }
        setCompoundDrawables(left, top, right, bottom);
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end
     * of, and below the text. Use {@code null} if you do not want a Drawable
     * there. The Drawables must already have had {@link Drawable#setBounds}
     * called.
     * <p>
     * Calling this method will overwrite any Drawables previously set using
     * {@link #setCompoundDrawables} or related methods.
     */
    public void setCompoundDrawablesRelative(@Nullable Drawable start, @Nullable Drawable top,
                                             @Nullable Drawable end, @Nullable Drawable bottom) {
        Drawables dr = mDrawables;

        // We're switching to relative, discard absolute.
        if (dr != null) {
            if (dr.mShowing[Drawables.LEFT] != null) {
                dr.mShowing[Drawables.LEFT].setCallback(null);
            }
            dr.mShowing[Drawables.LEFT] = dr.mDrawableLeftInitial = null;
            if (dr.mShowing[Drawables.RIGHT] != null) {
                dr.mShowing[Drawables.RIGHT].setCallback(null);
            }
            dr.mShowing[Drawables.RIGHT] = dr.mDrawableRightInitial = null;
            dr.mDrawableSizeLeft = dr.mDrawableHeightLeft = 0;
            dr.mDrawableSizeRight = dr.mDrawableHeightRight = 0;
        }

        final boolean drawables = start != null || top != null
                || end != null || bottom != null;

        if (!drawables) {
            // Clearing drawables...  can we free the data structure?
            if (dr != null) {
                if (!dr.hasMetadata()) {
                    mDrawables = null;
                } else {
                    // We need to retain the last set padding, so just clear
                    // out all of the fields in the existing structure.
                    if (dr.mDrawableStart != null) dr.mDrawableStart.setCallback(null);
                    dr.mDrawableStart = null;
                    if (dr.mShowing[Drawables.TOP] != null) {
                        dr.mShowing[Drawables.TOP].setCallback(null);
                    }
                    dr.mShowing[Drawables.TOP] = null;
                    if (dr.mDrawableEnd != null) {
                        dr.mDrawableEnd.setCallback(null);
                    }
                    dr.mDrawableEnd = null;
                    if (dr.mShowing[Drawables.BOTTOM] != null) {
                        dr.mShowing[Drawables.BOTTOM].setCallback(null);
                    }
                    dr.mShowing[Drawables.BOTTOM] = null;
                    dr.mDrawableSizeStart = dr.mDrawableHeightStart = 0;
                    dr.mDrawableSizeEnd = dr.mDrawableHeightEnd = 0;
                    dr.mDrawableSizeTop = dr.mDrawableWidthTop = 0;
                    dr.mDrawableSizeBottom = dr.mDrawableWidthBottom = 0;
                }
            }
        } else {
            if (dr == null) {
                mDrawables = dr = new Drawables();
            }

            mDrawables.mOverride = true;

            if (dr.mDrawableStart != start && dr.mDrawableStart != null) {
                dr.mDrawableStart.setCallback(null);
            }
            dr.mDrawableStart = start;

            if (dr.mShowing[Drawables.TOP] != top && dr.mShowing[Drawables.TOP] != null) {
                dr.mShowing[Drawables.TOP].setCallback(null);
            }
            dr.mShowing[Drawables.TOP] = top;

            if (dr.mDrawableEnd != end && dr.mDrawableEnd != null) {
                dr.mDrawableEnd.setCallback(null);
            }
            dr.mDrawableEnd = end;

            if (dr.mShowing[Drawables.BOTTOM] != bottom && dr.mShowing[Drawables.BOTTOM] != null) {
                dr.mShowing[Drawables.BOTTOM].setCallback(null);
            }
            dr.mShowing[Drawables.BOTTOM] = bottom;

            final Rect compoundRect = dr.mCompoundRect;
            int[] state;

            state = getDrawableState();

            if (start != null) {
                start.setState(state);
                start.copyBounds(compoundRect);
                start.setCallback(this);
                dr.mDrawableSizeStart = compoundRect.width();
                dr.mDrawableHeightStart = compoundRect.height();
            } else {
                dr.mDrawableSizeStart = dr.mDrawableHeightStart = 0;
            }

            if (end != null) {
                end.setState(state);
                end.copyBounds(compoundRect);
                end.setCallback(this);
                dr.mDrawableSizeEnd = compoundRect.width();
                dr.mDrawableHeightEnd = compoundRect.height();
            } else {
                dr.mDrawableSizeEnd = dr.mDrawableHeightEnd = 0;
            }

            if (top != null) {
                top.setState(state);
                top.copyBounds(compoundRect);
                top.setCallback(this);
                dr.mDrawableSizeTop = compoundRect.height();
                dr.mDrawableWidthTop = compoundRect.width();
            } else {
                dr.mDrawableSizeTop = dr.mDrawableWidthTop = 0;
            }

            if (bottom != null) {
                bottom.setState(state);
                bottom.copyBounds(compoundRect);
                bottom.setCallback(this);
                dr.mDrawableSizeBottom = compoundRect.height();
                dr.mDrawableWidthBottom = compoundRect.width();
            } else {
                dr.mDrawableSizeBottom = dr.mDrawableWidthBottom = 0;
            }
        }

        resetResolvedDrawables();
        resolveDrawables();
        invalidate();
        requestLayout();
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end
     * of, and below the text. Use {@code null} if you do not want a Drawable
     * there. The Drawables' bounds will be set to their intrinsic bounds.
     * <p>
     * Calling this method will overwrite any Drawables previously set using
     * {@link #setCompoundDrawables} or related methods.
     */
    public void setCompoundDrawablesRelativeWithIntrinsicBounds(@Nullable Drawable start, @Nullable Drawable top,
                                                                @Nullable Drawable end, @Nullable Drawable bottom) {
        if (start != null) {
            start.setBounds(0, 0, start.getIntrinsicWidth(), start.getIntrinsicHeight());
        }
        if (end != null) {
            end.setBounds(0, 0, end.getIntrinsicWidth(), end.getIntrinsicHeight());
        }
        if (top != null) {
            top.setBounds(0, 0, top.getIntrinsicWidth(), top.getIntrinsicHeight());
        }
        if (bottom != null) {
            bottom.setBounds(0, 0, bottom.getIntrinsicWidth(), bottom.getIntrinsicHeight());
        }
        setCompoundDrawablesRelative(start, top, end, bottom);
    }

    /**
     * Returns drawables for the left, top, right, and bottom borders.
     */
    @Nonnull
    public Drawable[] getCompoundDrawables() {
        final Drawables dr = mDrawables;
        if (dr != null) {
            return dr.mShowing.clone();
        } else {
            return new Drawable[]{null, null, null, null};
        }
    }

    /**
     * Returns drawables for the start, top, end, and bottom borders.
     */
    @Nonnull
    public Drawable[] getCompoundDrawablesRelative() {
        final Drawables dr = mDrawables;
        if (dr != null) {
            return new Drawable[]{
                    dr.mDrawableStart, dr.mShowing[Drawables.TOP],
                    dr.mDrawableEnd, dr.mShowing[Drawables.BOTTOM]
            };
        } else {
            return new Drawable[]{null, null, null, null};
        }
    }

    /**
     * Sets the size of the padding between the compound drawables and
     * the text.
     */
    public void setCompoundDrawablePadding(int pad) {
        Drawables dr = mDrawables;
        if (pad == 0) {
            if (dr != null) {
                dr.mDrawablePadding = pad;
            }
        } else {
            if (dr == null) {
                mDrawables = dr = new Drawables();
            }
            dr.mDrawablePadding = pad;
        }

        invalidate();
        requestLayout();
    }

    /**
     * Returns the padding between the compound drawables and the text.
     */
    public int getCompoundDrawablePadding() {
        final Drawables dr = mDrawables;
        return dr != null ? dr.mDrawablePadding : 0;
    }

    /**
     * @inheritDoc
     * @see #setFirstBaselineToTopHeight(int)
     * @see #setLastBaselineToBottomHeight(int)
     */
    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        if (left != mPaddingLeft
                || right != mPaddingRight
                || top != mPaddingTop
                || bottom != mPaddingBottom) {
            nullLayouts();
        }

        // the super call will requestLayout()
        super.setPadding(left, top, right, bottom);
        invalidate();
    }

    /**
     * @inheritDoc
     * @see #setFirstBaselineToTopHeight(int)
     * @see #setLastBaselineToBottomHeight(int)
     */
    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        if (start != getPaddingStart()
                || end != getPaddingEnd()
                || top != mPaddingTop
                || bottom != mPaddingBottom) {
            nullLayouts();
        }

        // the super call will requestLayout()
        super.setPaddingRelative(start, top, end, bottom);
        invalidate();
    }

    /**
     * Updates the top padding of the TextView so that {@code firstBaselineToTopHeight} is
     * the distance between the top of the TextView and first line's baseline.
     *
     * <strong>Note</strong> that if {@code FontMetrics.top} or {@code FontMetrics.ascent} was
     * already greater than {@code firstBaselineToTopHeight}, the top padding is not updated.
     * Moreover, since this function sets the top padding, if the height of the TextView is less than
     * the sum of top padding, line height and bottom padding, top of the line will be pushed
     * down and bottom will be clipped.
     *
     * @param firstBaselineToTopHeight distance between first baseline to top of the container
     *                                 in pixels
     * @see #getFirstBaselineToTopHeight()
     * @see #setLastBaselineToBottomHeight(int)
     * @see #setPadding(int, int, int, int)
     * @see #setPaddingRelative(int, int, int, int)
     */
    public void setFirstBaselineToTopHeight(int firstBaselineToTopHeight) {
        if (firstBaselineToTopHeight < 0) {
            firstBaselineToTopHeight = 0;
        }

        final FontMetricsInt fontMetrics = getPaint().getFontMetricsInt();
        final int fontMetricsTop = fontMetrics.ascent;

        if (firstBaselineToTopHeight > fontMetricsTop) {
            final int paddingTop = firstBaselineToTopHeight - fontMetricsTop;
            setPadding(getPaddingLeft(), paddingTop, getPaddingRight(), getPaddingBottom());
        }
    }

    /**
     * Updates the bottom padding of the TextView so that {@code lastBaselineToBottomHeight} is
     * the distance between the bottom of the TextView and the last line's baseline.
     *
     * <strong>Note</strong> that if {@code FontMetrics.bottom} or {@code FontMetrics.descent} was
     * already greater than {@code lastBaselineToBottomHeight}, the bottom padding is not updated.
     * Moreover, since this function sets the bottom padding, if the height of the TextView is less
     * than the sum of top padding, line height and bottom padding, bottom of the text will be
     * clipped.
     *
     * @param lastBaselineToBottomHeight distance between last baseline to bottom of the container
     *                                   in pixels
     * @see #getLastBaselineToBottomHeight()
     * @see #setFirstBaselineToTopHeight(int)
     * @see #setPadding(int, int, int, int)
     * @see #setPaddingRelative(int, int, int, int)
     */
    public void setLastBaselineToBottomHeight(int lastBaselineToBottomHeight) {
        if (lastBaselineToBottomHeight < 0) {
            lastBaselineToBottomHeight = 0;
        }

        final FontMetricsInt fontMetrics = getPaint().getFontMetricsInt();
        final int fontMetricsBottom = fontMetrics.descent;

        if (lastBaselineToBottomHeight > fontMetricsBottom) {
            final int paddingBottom = lastBaselineToBottomHeight - fontMetricsBottom;
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), paddingBottom);
        }
    }

    /**
     * Returns the distance between the first text baseline and the top of this TextView.
     *
     * @see #setFirstBaselineToTopHeight(int)
     */
    public int getFirstBaselineToTopHeight() {
        return getPaddingTop() + getPaint().getFontMetricsInt().ascent;
    }

    /**
     * Returns the distance between the last text baseline and the bottom of this TextView.
     *
     * @see #setLastBaselineToBottomHeight(int)
     */
    public int getLastBaselineToBottomHeight() {
        return getPaddingBottom() + getPaint().getFontMetricsInt().descent;
    }

    /**
     * Get the default primary {@link Locale} of the text in this TextView.
     *
     * @return the default primary {@link Locale} of the text in this TextView.
     */
    @Nonnull
    public Locale getTextLocale() {
        return mTextPaint.getTextLocale();
    }

    /**
     * Set the default {@link Locale} of the text in this TextView to the given Locale.
     *
     * @param locale the {@link Locale} for drawing text, must not be null.
     */
    public void setTextLocale(@Nonnull Locale locale) {
        mTextPaint.setTextLocale(locale);
        if (mLayout != null) {
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }

    /**
     * @return the size (in pixels) of the default text size in this TextView.
     */
    public float getTextSize() {
        return mTextPaint.getFontSize();
    }

    /**
     * Set the default text size to the given value, interpreted as "scaled
     * pixel" units.  This size is adjusted based on the current density and
     * user font size preference.
     *
     * @param size The scaled pixel size.
     */
    public void setTextSize(float size) {
        if (size != mTextPaint.getFontSize()) {
            mTextPaint.setFontSize(ViewConfiguration.get().getTextSize(size));

            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    /**
     * Sets the typeface and style in which the text should be displayed.
     *
     * @see #getTypeface()
     */
    public void setTypeface(@Nonnull Typeface tf) {
        if (mTextPaint.getTypeface() != tf) {
            mTextPaint.setTypeface(tf);

            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    /**
     * Gets the current {@link Typeface} that is used to style the text.
     *
     * @return The current Typeface.
     * @see #setTypeface(Typeface)
     */
    @Nonnull
    public Typeface getTypeface() {
        return mTextPaint.getTypeface();
    }

    /**
     * Gets the vertical distance between lines of text, in pixels.
     * Note that markup within the text can cause individual lines
     * to be taller or shorter than this height, and the layout may
     * contain additional first-or last-line padding.
     *
     * @return The height of one standard line in pixels.
     */
    public int getLineHeight() {
        return mTextPaint.getFontMetricsInt(null);
    }

    /**
     * Set whether to respect the ascent and descent of the fallback fonts that are used in
     * displaying the text (which is needed to avoid text from consecutive lines running into
     * each other). If set, fallback fonts that end up getting used can increase the ascent
     * and descent of the lines that they are used on.
     * <p/>
     * It is required to be true if text could be in languages like Burmese or Tibetan where text
     * is typically much taller or deeper than Latin text.
     *
     * @param enabled whether to expand linespacing based on fallback fonts, {@code true} by default
     * @see StaticLayout.Builder#setFallbackLineSpacing(boolean)
     */
    public void setFallbackLineSpacing(boolean enabled) {
        if (mUseFallbackLineSpacing != enabled) {
            mUseFallbackLineSpacing = enabled;
            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    /**
     * @return whether fallback line spacing is enabled, {@code true} by default
     * @see #setFallbackLineSpacing(boolean)
     */
    public boolean isFallbackLineSpacing() {
        return mUseFallbackLineSpacing;
    }

    //TODO color setting

    /**
     * Gets the {@link TextPaint} used for the text.
     * Use this only to consult the Paint's properties and not to change them.
     *
     * @return The base paint used for the text.
     */
    @Nonnull
    public TextPaint getPaint() {
        return mTextPaint;
    }

    /**
     * Gets the {@link Layout} that is currently being used to display the text.
     * This value can be null if the text or width has recently changed.
     *
     * @return The Layout that is currently being used to display the text.
     */
    public final Layout getLayout() {
        return mLayout;
    }

    /**
     * Sets the autolink mask of the text.
     */
    public final void setAutoLinkMask(int mask) {
        mAutoLinkMask = mask;
    }

    /**
     * Gets the autolink mask of the text.
     */
    public final int getAutoLinkMask() {
        return mAutoLinkMask;
    }

    /**
     * Sets whether the movement method will automatically be set to
     * {@link LinkMovementMethod} if {@link #setAutoLinkMask} has been
     * set to nonzero and links are detected in {@link #setText}.
     * The default is true.
     */
    public final void setLinksClickable(boolean whether) {
        mLinksClickable = whether;
    }

    /**
     * Returns whether the movement method will automatically be set to
     * {@link LinkMovementMethod} if {@link #setAutoLinkMask} has been
     * set to nonzero and links are detected in {@link #setText}.
     * The default is true.
     */
    public final boolean getLinksClickable() {
        return mLinksClickable;
    }

    /**
     * Sets the horizontal alignment of the text and the
     * vertical gravity that will be used when there is extra space
     * in the TextView beyond what is required for the text itself.
     *
     * @see Gravity
     */
    public void setGravity(int gravity) {
        if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
            gravity |= Gravity.START;
        }
        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
            gravity |= Gravity.TOP;
        }

        boolean newLayout = (gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)
                != (mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK);

        if (gravity != mGravity) {
            invalidate();
        }

        mGravity = gravity;

        if (mLayout != null && newLayout) {
            // XXX this is heavy-handed because no actual content changes.
            int want = mLayout.getWidth();
            int hintWant = mHintLayout == null ? 0 : mHintLayout.getWidth();

            makeNewLayout(want, hintWant, UNKNOWN_BORING, UNKNOWN_BORING,
                    getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight(), true);
        }
    }

    /**
     * Returns the horizontal and vertical alignment of this TextView.
     *
     * @see Gravity
     */
    public int getGravity() {
        return mGravity;
    }

    /**
     * Sets whether the text should be allowed to be wider than the
     * View is.  If false, it will be wrapped to the width of the View.
     */
    public void setHorizontallyScrolling(boolean whether) {
        if (mHorizontallyScrolling != whether) {
            mHorizontallyScrolling = whether;

            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    /**
     * Returns whether the text is allowed to be wider than the View.
     * If false, the text will be wrapped to the width of the View.
     *
     * @see #setHorizontallyScrolling(boolean)
     */
    public final boolean isHorizontallyScrollable() {
        return mHorizontallyScrolling;
    }

    /**
     * Sets the height of the TextView to be at least {@code minLines} tall.
     * <p>
     * This value is used for height calculation if LayoutParams does not force TextView to have an
     * exact height. Setting this value overrides other previous minimum height configurations such
     * as {@link #setMinHeight(int)} or {@link #setHeight(int)}. {@link #setSingleLine()} will set
     * this value to 1.
     *
     * @param minLines the minimum height of TextView in terms of number of lines
     * @see #getMinLines()
     * @see #setLines(int)
     */
    public void setMinLines(int minLines) {
        mMinimum = minLines;
        mMinMode = LINES;

        requestLayout();
        invalidate();
    }

    /**
     * Returns the minimum height of TextView in terms of number of lines or -1 if the minimum
     * height was set using {@link #setMinHeight(int)} or {@link #setHeight(int)}.
     *
     * @return the minimum height of TextView in terms of number of lines or -1 if the minimum
     * height is not defined in lines
     * @see #setMinLines(int)
     * @see #setLines(int)
     */
    public int getMinLines() {
        return mMinMode == LINES ? mMinimum : -1;
    }

    /**
     * Sets the height of the TextView to be at least {@code minPixels} tall.
     * <p>
     * This value is used for height calculation if LayoutParams does not force TextView to have an
     * exact height. Setting this value overrides previous minimum height configurations such as
     * {@link #setMinLines(int)} or {@link #setLines(int)}.
     * <p>
     * The value given here is different than {@link #setMinimumHeight(int)}. Between
     * {@code minHeight} and the value set in {@link #setMinimumHeight(int)}, the greater one is
     * used to decide the final height.
     *
     * @param minPixels the minimum height of TextView in terms of pixels
     * @see #getMinHeight()
     * @see #setHeight(int)
     */
    public void setMinHeight(int minPixels) {
        mMinimum = minPixels;
        mMinMode = PIXELS;

        requestLayout();
        invalidate();
    }

    /**
     * Returns the minimum height of TextView in terms of pixels or -1 if the minimum height was
     * set using {@link #setMinLines(int)} or {@link #setLines(int)}.
     *
     * @return the minimum height of TextView in terms of pixels or -1 if the minimum height is not
     * defined in pixels
     * @see #setMinHeight(int)
     * @see #setHeight(int)
     */
    public int getMinHeight() {
        return mMinMode == PIXELS ? mMinimum : -1;
    }

    /**
     * Sets the height of the TextView to be at most {@code maxLines} tall.
     * <p>
     * This value is used for height calculation if LayoutParams does not force TextView to have an
     * exact height. Setting this value overrides previous maximum height configurations such as
     * {@link #setMaxHeight(int)} or {@link #setLines(int)}.
     *
     * @param maxLines the maximum height of TextView in terms of number of lines
     * @see #getMaxLines()
     * @see #setLines(int)
     */
    public void setMaxLines(int maxLines) {
        mMaximum = maxLines;
        mMaxMode = LINES;

        requestLayout();
        invalidate();
    }

    /**
     * Returns the maximum height of TextView in terms of number of lines or -1 if the
     * maximum height was set using {@link #setMaxHeight(int)} or {@link #setHeight(int)}.
     *
     * @return the maximum height of TextView in terms of number of lines. -1 if the maximum height
     * is not defined in lines.
     * @see #setMaxLines(int)
     * @see #setLines(int)
     */
    public int getMaxLines() {
        return mMaxMode == LINES ? mMaximum : -1;
    }

    /**
     * Sets the height of the TextView to be at most {@code maxPixels} tall.
     * <p>
     * This value is used for height calculation if LayoutParams does not force TextView to have an
     * exact height. Setting this value overrides previous maximum height configurations such as
     * {@link #setMaxLines(int)} or {@link #setLines(int)}.
     *
     * @param maxPixels the maximum height of TextView in terms of pixels
     * @see #getMaxHeight()
     * @see #setHeight(int)
     */
    public void setMaxHeight(int maxPixels) {
        mMaximum = maxPixels;
        mMaxMode = PIXELS;

        requestLayout();
        invalidate();
    }

    /**
     * Returns the maximum height of TextView in terms of pixels or -1 if the maximum height was
     * set using {@link #setMaxLines(int)} or {@link #setLines(int)}.
     *
     * @return the maximum height of TextView in terms of pixels or -1 if the maximum height
     * is not defined in pixels
     * @see #setMaxHeight(int)
     * @see #setHeight(int)
     */
    public int getMaxHeight() {
        return mMaxMode == PIXELS ? mMaximum : -1;
    }

    /**
     * Sets the height of the TextView to be exactly {@code lines} tall.
     * <p>
     * This value is used for height calculation if LayoutParams does not force TextView to have an
     * exact height. Setting this value overrides previous minimum/maximum height configurations
     * such as {@link #setMinLines(int)} or {@link #setMaxLines(int)}. {@link #setSingleLine()} will
     * set this value to 1.
     *
     * @param lines the exact height of the TextView in terms of lines
     * @see #setHeight(int)
     */
    public void setLines(int lines) {
        mMaximum = mMinimum = lines;
        mMaxMode = mMinMode = LINES;

        requestLayout();
        invalidate();
    }

    /**
     * Sets the height of the TextView to be exactly <code>pixels</code> tall.
     * <p>
     * This value is used for height calculation if LayoutParams does not force TextView to have an
     * exact height. Setting this value overrides previous minimum/maximum height configurations
     * such as {@link #setMinHeight(int)} or {@link #setMaxHeight(int)}.
     *
     * @param pixels the exact height of the TextView in terms of pixels
     * @see #setLines(int)
     */
    public void setHeight(int pixels) {
        mMaximum = mMinimum = pixels;
        mMaxMode = mMinMode = PIXELS;

        requestLayout();
        invalidate();
    }

    /**
     * Sets the width of the TextView to be at least {@code minPixels} wide.
     * <p>
     * This value is used for width calculation if LayoutParams does not force TextView to have an
     * exact width.
     * <p>
     * The value given here is different from {@link #setMinimumWidth(int)}. Between
     * {@code minWidth} and the value set in {@link #setMinimumWidth(int)}, the greater one is used
     * to decide the final width.
     *
     * @param minPixels the minimum width of TextView in terms of pixels
     * @see #getMinWidth()
     * @see #setWidth(int)
     */
    public void setMinWidth(int minPixels) {
        mMinWidth = minPixels;

        requestLayout();
        invalidate();
    }

    /**
     * Returns the minimum width of TextView in terms of pixels.
     *
     * @return the minimum width of TextView in terms of pixels.
     * @see #setMinWidth(int)
     * @see #setWidth(int)
     */
    public int getMinWidth() {
        return mMinWidth;
    }

    /**
     * Sets the width of the TextView to be at most {@code maxPixels} wide.
     * <p>
     * This value is used for width calculation if LayoutParams does not force TextView to have an
     * exact width.
     *
     * @param maxPixels the maximum width of TextView in terms of pixels
     * @see #getMaxWidth()
     * @see #setWidth(int)
     */
    public void setMaxWidth(int maxPixels) {
        mMaxWidth = maxPixels;

        requestLayout();
        invalidate();
    }

    /**
     * Returns the maximum width of TextView in terms of pixels.
     *
     * @return the maximum width of TextView in terms of pixels.
     * @see #setMaxWidth(int)
     * @see #setWidth(int)
     */
    public int getMaxWidth() {
        return mMaxWidth;
    }

    /**
     * Sets the width of the TextView to be exactly {@code pixels} wide.
     * <p>
     * This value is used for width calculation if LayoutParams does not force TextView to have an
     * exact width. Setting this value overrides previous minimum/maximum width configurations
     * such as {@link #setMinWidth(int)} or {@link #setMaxWidth(int)}.
     *
     * @param pixels the exact width of the TextView in terms of pixels
     */
    public void setWidth(int pixels) {
        mMaxWidth = mMinWidth = pixels;

        requestLayout();
        invalidate();
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns the length, in characters, of the text managed by this TextView
     *
     * @return The length of the text managed by the TextView in characters.
     */
    public int length() {
        return mText.length();
    }

    /**
     * Return the text that TextView is displaying. If {@link #setText(CharSequence)} was called
     * with an argument of {@link BufferType#SPANNABLE BufferType.SPANNABLE}
     * or {@link BufferType#EDITABLE BufferType.EDITABLE}, you can cast
     * the return value from this method to Spannable or Editable, respectively.
     *
     * <p>The content of the return value should not be modified. If you want a modifiable one, you
     * should make your own copy first.</p>
     *
     * @return The text displayed by the text view.
     */
    @Nonnull
    public CharSequence getText() {
        return mText;
    }

    /**
     * Return the text that TextView is displaying as an Editable object. If the text is not
     * editable, null is returned.
     *
     * @see #getText
     */
    public Editable getEditableText() {
        return (mText instanceof Editable) ? (Editable) mText : null;
    }

    /**
     * Sets the Factory used to create new {@link Editable Editables}.
     *
     * @param factory {@link Editable.Factory} to be used
     * @see Editable.Factory
     * @see BufferType#EDITABLE
     */
    public final void setEditableFactory(@Nonnull Editable.Factory factory) {
        mEditableFactory = factory;
        setText(mText);
    }

    /**
     * Sets the Factory used to create new {@link Spannable Spannables}.
     *
     * @param factory {@link Spannable.Factory} to be used
     * @see Spannable.Factory
     * @see BufferType#SPANNABLE
     */
    public final void setSpannableFactory(@Nonnull Spannable.Factory factory) {
        mSpannableFactory = factory;
        setText(mText);
    }

    /**
     * Sets the text to be displayed. To style your strings, attach
     * {@link CharacterStyle} to a {@link SpannableString}.
     * <p>
     * When required, TextView will use {@link Spannable.Factory} to create final or
     * intermediate {@link Spannable Spannables}. Likewise, it will use
     * {@link Editable.Factory} to create final or intermediate
     * {@link Editable Editables}.
     * <p>
     * If the passed text is a {@link PrecomputedText} but the parameters used to create the
     * PrecomputedText mismatches with this TextView, IllegalArgumentException is thrown.
     *
     * @param text text to be displayed
     * @throws IllegalArgumentException if the passed text is a {@link PrecomputedText} but the
     *                                  parameters used to create the PrecomputedText mismatches
     *                                  with this TextView.
     */
    public final void setText(@Nonnull CharSequence text) {
        setText(text, mBufferType);
    }

    /**
     * Sets the text to be displayed but retains the cursor position. Same as
     * {@link #setText(CharSequence)} except that the cursor position (if any) is retained in the
     * new text.
     * <p/>
     * When required, TextView will use {@link Spannable.Factory} to create final or
     * intermediate {@link Spannable Spannables}. Likewise, it will use
     * {@link Editable.Factory} to create final or intermediate
     * {@link Editable Editables}.
     *
     * @param text text to be displayed
     * @see #setText(CharSequence)
     */
    public final void setTextKeepState(@Nonnull CharSequence text) {
        setTextKeepState(text, mBufferType);
    }

    /**
     * Sets the text to be displayed and the {@link BufferType} but retains
     * the cursor position. Same as
     * {@link #setText(CharSequence, BufferType)} except that the cursor
     * position (if any) is retained in the new text.
     * <p/>
     * When required, TextView will use {@link Spannable.Factory} to create final or
     * intermediate {@link Spannable Spannables}. Likewise, it will use
     * {@link Editable.Factory} to create final or intermediate
     * {@link Editable Editables}.
     *
     * @param text text to be displayed
     * @param type a {@link BufferType} which defines whether the text is
     *             stored as a static text, styleable/spannable text, or editable text
     * @see #setText(CharSequence, BufferType)
     */
    public final void setTextKeepState(@Nonnull CharSequence text, @Nonnull BufferType type) {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        int len = text.length();

        setText(text, type);

        if (start >= 0 || end >= 0) {
            if (mSpannable != null) {
                Selection.setSelection(mSpannable,
                        Math.max(0, Math.min(start, len)),
                        Math.max(0, Math.min(end, len)));
            }
        }
    }

    /**
     * Sets the text to be displayed and the {@link BufferType}.
     * <p/>
     * When required, TextView will use {@link Spannable.Factory} to create final or
     * intermediate {@link Spannable Spannables}. Likewise, it will use
     * {@link Editable.Factory} to create final or intermediate
     * {@link Editable Editables}.
     * <p>
     * Subclasses overriding this method should ensure that the following post condition holds,
     * in order to guarantee the safety of the view's measurement and layout operations:
     * regardless of the input, after calling #setText both {@code mText} and {@code mTransformed}
     * will be different from {@code null}.
     *
     * @param text text to be displayed
     * @param type a {@link BufferType} which defines whether the text is
     *             stored as a static text, styleable/spannable text, or editable text
     * @see #setText(CharSequence)
     * @see BufferType
     * @see #setSpannableFactory(Spannable.Factory)
     * @see #setEditableFactory(Editable.Factory)
     */
    public void setText(@Nonnull CharSequence text, @Nonnull BufferType type) {
        for (InputFilter filter : mFilters) {
            CharSequence out = filter.filter(text, 0, text.length(), EMPTY_SPANNED, 0, 0);
            if (out != null) {
                text = out;
            }
        }

        int oldLength = mText.length();
        sendBeforeTextChanged(mText, 0, oldLength, text.length());

        boolean needEditableForNotification = mListeners != null && !mListeners.isEmpty();

        if (type == BufferType.EDITABLE || needEditableForNotification) {
            createEditorIfNeeded();
            Editable t = mEditableFactory.newEditable(text);
            text = t;
            t.setFilters(mFilters);
        } else if (type == BufferType.SPANNABLE || mMovement != null) {
            text = mSpannableFactory.newSpannable(text);
        } else {
            text = TextUtils.stringOrSpannedString(text);
        }

        if (mAutoLinkMask != 0) {
            //TODO links
        }

        mBufferType = type;
        setTextInternal(text);

        if (mTransformation == null) {
            mTransformed = text;
        } else {
            mTransformed = mTransformation.getTransformation(text, this);
        }

        final int textLength = text.length();

        if (text instanceof Spannable sp) {

            // Remove any ChangeWatchers that might have come from other TextViews.
            final ChangeWatcher[] watchers = sp.getSpans(0, sp.length(), ChangeWatcher.class);
            if (watchers != null) {
                for (ChangeWatcher watcher : watchers) {
                    sp.removeSpan(watcher);
                }
            }

            if (mChangeWatcher == null) mChangeWatcher = new ChangeWatcher();

            sp.setSpan(mChangeWatcher, 0, textLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE
                    | (CHANGE_WATCHER_PRIORITY << Spanned.SPAN_PRIORITY_SHIFT));

            if (mEditor != null) mEditor.addSpanWatchers(sp);

            if (mTransformation != null) {
                sp.setSpan(mTransformation, 0, textLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }

            if (mMovement != null) {
                mMovement.initialize(this, (Spannable) text);

                /*
                 * Initializing the movement method will have set the
                 * selection, so reset mSelectionMoved to keep that from
                 * interfering with the normal on-focus selection-setting.
                 */
                if (mEditor != null) mEditor.mSelectionMoved = false;
            }
        }

        if (mLayout != null) {
            checkForRelayout();
        }

        sendOnTextChanged(text, 0, oldLength, textLength);
        onTextChanged(text, 0, oldLength, textLength);

        if (needEditableForNotification) {
            sendAfterTextChanged((Editable) text);
        }
    }

    // Update mText and mPrecomputed
    private void setTextInternal(@Nonnull CharSequence text) {
        mText = text;
        mSpannable = (text instanceof Spannable) ? (Spannable) text : null;
        mPrecomputed = (text instanceof PrecomputedText) ? (PrecomputedText) text : null;
    }

    /**
     * Returns the hint that is displayed when the text of the TextView
     * is empty.
     */
    @Nullable
    public CharSequence getHint() {
        return mHint;
    }

    /**
     * Sets the text to be displayed when the text of the TextView is empty.
     * Null means to use the normal empty text. The hint does not currently
     * participate in determining the size of the view.
     */
    public final void setHint(@Nullable CharSequence hint) {
        setHintInternal(hint);

        if (mEditor != null && isFocused()) {
            mEditor.reportExtractedText();
        }
    }

    private void setHintInternal(@Nullable CharSequence hint) {
        mHint = TextUtils.stringOrSpannedString(hint);

        if (mLayout != null) {
            checkForRelayout();
        }

        if (mText.length() == 0) {
            invalidate();
        }

        // Invalidate display list if hint is currently used
        if (mEditor != null && mText.length() == 0 && mHint != null) {
            mEditor.invalidateTextDisplayList();
        }
    }

    /**
     * It would be better to rely on the input type for everything. A password inputType should have
     * a password transformation. We should hence use isPasswordInputType instead of this method.
     * <p>
     * We should:
     * - Call setInputType in setKeyListener instead of changing the input type directly (which
     * would install the correct transformation).
     * - Refuse the installation of a non-password transformation in setTransformation if the input
     * type is password.
     * <p>
     * However, this is like this for legacy reasons, and we cannot break existing apps. This method
     * is useful since it matches what the user can see (obfuscated text or not).
     *
     * @return true if the current transformation method is of the password type.
     */
    boolean hasPasswordTransformationMethod() {
        //TODO
        return false;
    }

    /**
     * Sets the list of input filters that will be used if the buffer is
     * Editable. Has no effect otherwise.
     */
    public void setFilters(@Nonnull InputFilter[] filters) {
        mFilters = filters;

        if (mText instanceof Editable) {
            ((Editable) mText).setFilters(filters);
        }
    }

    /**
     * Returns the current list of input filters.
     */
    @Nonnull
    public InputFilter[] getFilters() {
        return mFilters;
    }

    /////////////////////////////////////////////////////////////////////////

    private int getBoxHeight(Layout l) {
        int padding = (l == mHintLayout)
                ? getCompoundPaddingTop() + getCompoundPaddingBottom()
                : getExtendedPaddingTop() + getExtendedPaddingBottom();
        return getMeasuredHeight() - padding;
    }

    private int getVerticalOffset(boolean forceNormal) {
        int voffset = 0;
        final int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;

        Layout l = mLayout;
        if (!forceNormal && mText.length() == 0 && mHintLayout != null) {
            l = mHintLayout;
        }

        if (gravity != Gravity.TOP) {
            int boxht = getBoxHeight(l);
            int textht = l.getHeight();

            if (textht < boxht) {
                if (gravity == Gravity.BOTTOM) {
                    voffset = boxht - textht;
                } else { // (gravity == Gravity.CENTER_VERTICAL)
                    voffset = (boxht - textht) >> 1;
                }
            }
        }
        return voffset;
    }

    private int getBottomVerticalOffset(boolean forceNormal) {
        int voffset = 0;
        final int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;

        Layout l = mLayout;
        if (!forceNormal && mText.length() == 0 && mHintLayout != null) {
            l = mHintLayout;
        }

        if (gravity != Gravity.BOTTOM) {
            int boxht = getBoxHeight(l);
            int textht = l.getHeight();

            if (textht < boxht) {
                if (gravity == Gravity.TOP) {
                    voffset = boxht - textht;
                } else { // (gravity == Gravity.CENTER_VERTICAL)
                    voffset = (boxht - textht) >> 1;
                }
            }
        }
        return voffset;
    }

    private void registerForPreDraw() {
        if (!mPreDrawRegistered) {
            getViewTreeObserver().addOnPreDrawListener(this);
            mPreDrawRegistered = true;
        }
    }

    private void unregisterForPreDraw() {
        getViewTreeObserver().removeOnPreDrawListener(this);
        mPreDrawRegistered = false;
        mPreDrawListenerDetached = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onPreDraw() {
        if (mLayout == null) {
            assumeLayout();
        }

        unregisterForPreDraw();

        return true;
    }

    @Override
    protected boolean verifyDrawable(@Nonnull Drawable who) {
        final boolean verified = super.verifyDrawable(who);
        if (!verified && mDrawables != null) {
            for (Drawable dr : mDrawables.mShowing) {
                if (who == dr) {
                    return true;
                }
            }
        }
        return verified;
    }

    /**
     * Returns the state of the {@code textIsSelectable} flag (See
     * {@link #setTextIsSelectable setTextIsSelectable()}).
     *
     * @return True if the text displayed in this TextView can be selected by the user.
     */
    public boolean isTextSelectable() {
        return mEditor != null && mEditor.mTextIsSelectable;
    }

    /**
     * Sets whether the content of this view is selectable by the user. The default is
     * {@code false}, meaning that the content is not selectable.
     * <p>
     * When you use a TextView to display a useful piece of information to the user (such as a
     * contact's address), make it selectable, so that the user can select and copy its
     * content.
     * <p>
     * When you call this method to set the value of {@code textIsSelectable}, it sets
     * the flags {@code focusable}, {@code focusableInTouchMode}, {@code clickable},
     * and {@code longClickable} to the same value. To restore any of these
     * flags to a state you had set previously, call one or more of the following methods:
     * {@link #setFocusable(boolean) setFocusable()},
     * {@link #setFocusableInTouchMode(boolean) setFocusableInTouchMode()},
     * {@link #setClickable(boolean) setClickable()} or
     * {@link #setLongClickable(boolean) setLongClickable()}.
     *
     * @param selectable Whether the content of this TextView should be selectable.
     */
    public void setTextIsSelectable(boolean selectable) {
        if (!selectable && mEditor == null) return; // false is default value with no edit data

        createEditorIfNeeded();
        if (mEditor.mTextIsSelectable == selectable) return;

        mEditor.mTextIsSelectable = selectable;
        setFocusableInTouchMode(selectable);
        setFocusable(FOCUSABLE_AUTO);
        setClickable(selectable);
        setLongClickable(selectable);

        setMovementMethod(selectable ? ArrowKeyMovementMethod.getInstance() : null);
        setText(mText, selectable ? BufferType.SPANNABLE : BufferType.NORMAL);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        //TODO states
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        return super.onCreateDrawableState(extraSpace);
    }

    private BiConsumer<Canvas, Paint> getUpdatedHighlightPath() {
        return null;
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas) {
        if (mLayout == null) {
            assumeLayout();
        }

        Layout layout = mLayout;

        if (mHint != null && mText.length() == 0) {
            layout = mHintLayout;
        }

        assert layout != null;

        final int compoundPaddingLeft = getCompoundPaddingLeft();
        final int compoundPaddingTop = getCompoundPaddingTop();
        final int compoundPaddingRight = getCompoundPaddingRight();
        final int compoundPaddingBottom = getCompoundPaddingBottom();
        final int scrollX = mScrollX;
        final int scrollY = mScrollY;

        final int vspace = getHeight() - compoundPaddingBottom - compoundPaddingTop;
        final int maxScrollY = mLayout.getHeight() - vspace;

        float clipLeft = compoundPaddingLeft + scrollX;
        float clipTop = (scrollY == 0) ? 0 : compoundPaddingTop + scrollY;
        float clipRight = getWidth() - getCompoundPaddingRight() + scrollX;
        float clipBottom = getHeight() + scrollY
                - ((scrollY == maxScrollY) ? 0 : compoundPaddingBottom);

        canvas.save();
        canvas.clipRect(clipLeft, clipTop, clipRight, clipBottom);

        final long range = layout.getLineRangeForDraw(canvas);
        if (range >= 0) {
            int firstLine = (int) (range >>> 32);
            int lastLine = (int) (range & 0xFFFFFFFFL);
            layout.drawBackground(canvas, firstLine, lastLine);

            if (mMovement != null && mEditor != null && (isFocused() || isPressed())) {
                int selStart = getSelectionStart();
                if (selStart >= 0) {
                    int line = layout.getLineForOffset(selStart);
                    int top = layout.getLineTop(line);
                    int bottom = layout.getLineBottom(line);
                    int selEnd = getSelectionEnd();
                    float h = layout.getPrimaryHorizontal(selStart);
                    Paint paint = Paint.take();
                    paint.setStrokeWidth(4);
                    if (mEditor.shouldRenderCursor() && selStart == selEnd) {
                        int diff = (bottom - top) >> 3;
                        bottom -= diff;
                        top += diff;
                        paint.setColor(mTextPaint.getColor());
                        canvas.drawRoundLine(h, top, h, bottom, paint);
                    } else {
                        float h2 = layout.getPrimaryHorizontal(selEnd);
                        paint.setColor(0x6633B5E5);
                        canvas.drawRect(Math.min(h, h2), top, Math.max(h, h2), bottom, paint);
                    }
                }
            }

            layout.drawText(canvas, firstLine, lastLine);
        }

        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        BoringLayout.Metrics boring = UNKNOWN_BORING;
        BoringLayout.Metrics hintBoring = UNKNOWN_BORING;

        if (mTextDir == null) {
            mTextDir = getTextDirectionHeuristic();
        }

        int des = -1;
        boolean fromexisting = false;
        final float widthLimit = (widthMode == MeasureSpec.AT_MOST)
                ? (float) widthSize : Float.MAX_VALUE;

        if (widthMode == MeasureSpec.EXACTLY) {
            // Parent has told us how big to be. So be it.
            width = widthSize;
        } else {
            if (mLayout != null && mEllipsize == null) {
                des = desired(mLayout);
            }

            if (des < 0) {
                boring = BoringLayout.isBoring(mTransformed, mTextPaint, mTextDir, mBoring);
                if (boring != null) {
                    mBoring = boring;
                }
            } else {
                fromexisting = true;
            }

            if (boring == null || boring == UNKNOWN_BORING) {
                if (des < 0) {
                    des = (int) Math.ceil(Layout.getDesiredWidthWithLimit(mTransformed, 0,
                            mTransformed.length(), mTextPaint, mTextDir, widthLimit));
                }
                width = des;
            } else {
                width = boring.width;
            }

            final Drawables dr = mDrawables;
            if (dr != null) {
                width = Math.max(width, dr.mDrawableWidthTop);
                width = Math.max(width, dr.mDrawableWidthBottom);
            }

            if (mHint != null) {
                int hintDes = -1;
                int hintWidth;

                if (mHintLayout != null && mEllipsize == null) {
                    hintDes = desired(mHintLayout);
                }

                if (hintDes < 0) {
                    hintBoring = BoringLayout.isBoring(mHint, mTextPaint, mTextDir, mHintBoring);
                    if (hintBoring != null) {
                        mHintBoring = hintBoring;
                    }
                }

                if (hintBoring == null || hintBoring == UNKNOWN_BORING) {
                    if (hintDes < 0) {
                        hintDes = (int) Math.ceil(Layout.getDesiredWidthWithLimit(mHint, 0,
                                mHint.length(), mTextPaint, mTextDir, widthLimit));
                    }
                    hintWidth = hintDes;
                } else {
                    hintWidth = hintBoring.width;
                }

                if (hintWidth > width) {
                    width = hintWidth;
                }
            }

            width += getCompoundPaddingLeft() + getCompoundPaddingRight();

            width = Math.min(width, mMaxWidth);

            width = Math.max(width, mMinWidth);

            // Check against our minimum width
            width = Math.max(width, getSuggestedMinimumWidth());

            if (widthMode == MeasureSpec.AT_MOST) {
                width = Math.min(widthSize, width);
            }
        }

        int want = width - getCompoundPaddingLeft() - getCompoundPaddingRight();
        int unpaddedWidth = want;

        if (mHorizontallyScrolling) want = VERY_WIDE;

        int hintWant = want;
        int hintWidth = (mHintLayout == null) ? hintWant : mHintLayout.getWidth();

        if (mLayout == null) {
            makeNewLayout(want, hintWant, boring, hintBoring,
                    width - getCompoundPaddingLeft() - getCompoundPaddingRight(), false);
        } else {
            final boolean layoutChanged = (mLayout.getWidth() != want) || (hintWidth != hintWant)
                    || (mLayout.getEllipsizedWidth()
                    != width - getCompoundPaddingLeft() - getCompoundPaddingRight());

            final boolean widthChanged = (mHint == null) && (mEllipsize == null)
                    && (want > mLayout.getWidth())
                    && (mLayout instanceof BoringLayout
                    || (fromexisting && des >= 0 && des <= want));

            final boolean maximumChanged = (mMaxMode != mOldMaxMode) || (mMaximum != mOldMaximum);

            if (layoutChanged || maximumChanged) {
                if (!maximumChanged && widthChanged) {
                    mLayout.increaseWidthTo(want);
                } else {
                    makeNewLayout(want, hintWant, boring, hintBoring,
                            width - getCompoundPaddingLeft() - getCompoundPaddingRight(), false);
                }
            }
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            // Parent has told us how big to be. So be it.
            height = heightSize;
            mDesiredHeightAtMeasure = -1;
        } else {
            int desired = getDesiredHeight();

            height = desired;
            mDesiredHeightAtMeasure = desired;

            if (heightMode == MeasureSpec.AT_MOST) {
                height = Math.min(desired, heightSize);
            }
        }

        int unpaddedHeight = height - getCompoundPaddingTop() - getCompoundPaddingBottom();
        if (mMaxMode == LINES && mLayout.getLineCount() > mMaximum) {
            unpaddedHeight = Math.min(unpaddedHeight, mLayout.getLineTop(mMaximum));
        }

        /*
         * We didn't let makeNewLayout() register to bring the cursor into view,
         * so do it here if there is any possibility that it is needed.
         */
        if (mMovement != null
                || mLayout.getWidth() > unpaddedWidth
                || mLayout.getHeight() > unpaddedHeight) {
            registerForPreDraw();
        } else {
            scrollTo(0, 0);
        }

        setMeasuredDimension(width, height);
    }

    private static int desired(@Nonnull Layout layout) {
        int n = layout.getLineCount();
        CharSequence text = layout.getText();
        float max = 0;

        // if any line was wrapped, we can't use it.
        // but it's ok for the last line not to have a newline

        for (int i = 0; i < n - 1; i++) {
            if (text.charAt(layout.getLineEnd(i) - 1) != '\n') {
                return -1;
            }
        }

        for (int i = 0; i < n; i++) {
            max = Math.max(max, layout.getLineMax(i));
        }

        return (int) Math.ceil(max);
    }

    private int getDesiredHeight() {
        return Math.max(
                getDesiredHeight(mLayout, true),
                getDesiredHeight(mHintLayout, mEllipsize != null));
    }

    private int getDesiredHeight(Layout layout, boolean cap) {
        if (layout == null) {
            return 0;
        }

        /*
         * Don't cap the hint to a certain number of lines.
         * (Do cap it, though, if we have a maximum pixel height.)
         */
        int desired = layout.getHeight(cap);

        final Drawables dr = mDrawables;
        if (dr != null) {
            desired = Math.max(desired, dr.mDrawableHeightLeft);
            desired = Math.max(desired, dr.mDrawableHeightRight);
        }

        int linecount = layout.getLineCount();
        final int padding = getCompoundPaddingTop() + getCompoundPaddingBottom();
        desired += padding;

        if (mMaxMode != LINES) {
            desired = Math.min(desired, mMaximum);
        } else if (cap && linecount > mMaximum && (layout instanceof DynamicLayout
                || layout instanceof BoringLayout)) {
            desired = layout.getLineTop(mMaximum);

            if (dr != null) {
                desired = Math.max(desired, dr.mDrawableHeightLeft);
                desired = Math.max(desired, dr.mDrawableHeightRight);
            }

            desired += padding;
            linecount = mMaximum;
        }

        if (mMinMode == LINES) {
            if (linecount < mMinimum) {
                desired += getLineHeight() * (mMinimum - linecount);
            }
        } else {
            desired = Math.max(desired, mMinimum);
        }

        // Check against our minimum height
        desired = Math.max(desired, getSuggestedMinimumHeight());

        return desired;
    }


    /**
     * Check whether entirely new text requires a new view layout
     * or merely a new text layout.
     */
    private void checkForRelayout() {
        // If we have a fixed width, we can just swap in a new text layout
        // if the text height stays the same or if the view height is fixed.
        ViewGroup.LayoutParams params = getLayoutParams();
        if ((params.width != ViewGroup.LayoutParams.WRAP_CONTENT
                || (mMaxWidth == mMinWidth))
                && (mHint == null || mHintLayout != null)
                && (getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight() > 0)) {
            // Static width, so try making a new text layout.

            int oldht = mLayout.getHeight();
            int want = mLayout.getWidth();
            int hintWant = mHintLayout == null ? 0 : mHintLayout.getWidth();

            /*
             * No need to bring the text into view, since the size is not
             * changing (unless we do the requestLayout(), in which case it
             * will happen at measure).
             */
            makeNewLayout(want, hintWant, UNKNOWN_BORING, UNKNOWN_BORING,
                    getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight(),
                    false);

            if (mEllipsize != TextUtils.TruncateAt.MARQUEE) {
                // In a fixed-height view, so use our new text layout.
                if (params.height != ViewGroup.LayoutParams.WRAP_CONTENT
                        && params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                    invalidate();
                    return;
                }

                // Dynamic height, but height has stayed the same,
                // so use our new text layout.
                if (mLayout.getHeight() == oldht
                        && (mHintLayout == null || mHintLayout.getHeight() == oldht)) {
                    invalidate();
                    return;
                }
            }

            // We lose: the height has changed and we have a dynamic height.
            // Request a new view layout using our new text layout.
        } else {
            // Dynamic width, so we have no choice but to request a new
            // view layout with a new text layout.
            nullLayouts();
        }
        requestLayout();
        invalidate();
    }

    @Override
    protected void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
    }

    /**
     * Returns resolved {@link TextDirectionHeuristic} that will be used for text layout.
     * The {@link TextDirectionHeuristic} that is used by TextView is only available after
     * {@link #getTextDirection()} and {@link #getLayoutDirection()} is resolved. Therefore the
     * return value may not be the same as the one TextView uses if the View's layout direction is
     * not resolved or detached from parent root view.
     */
    @Nonnull
    public TextDirectionHeuristic getTextDirectionHeuristic() {
        if (hasPasswordTransformationMethod()) {
            // passwords fields should be LTR
            return TextDirectionHeuristics.LTR;
        }

        /*if (mEditor != null
                && (mEditor.mInputType & EditorInfo.TYPE_MASK_CLASS)
                == EditorInfo.TYPE_CLASS_PHONE) {
            // Phone numbers must be in the direction of the locale's digits. Most locales have LTR
            // digits, but some locales, such as those written in the Adlam or N'Ko scripts, have
            // RTL digits.
            final DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(getTextLocale());
            final String zero = symbols.getDigitStrings()[0];
            // In case the zero digit is multi-codepoint, just use the first codepoint to determine
            // direction.
            final int firstCodepoint = zero.codePointAt(0);
            final byte digitDirection = Character.getDirectionality(firstCodepoint);
            if (digitDirection == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                    || digitDirection == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return TextDirectionHeuristics.RTL;
            } else {
                return TextDirectionHeuristics.LTR;
            }
        }*/

        // Now, we can select the heuristic
        return switch (getTextDirection()) {
            case TEXT_DIRECTION_ANY_RTL -> TextDirectionHeuristics.ANYRTL_LTR;
            case TEXT_DIRECTION_LTR -> TextDirectionHeuristics.LTR;
            case TEXT_DIRECTION_RTL -> TextDirectionHeuristics.RTL;
            case TEXT_DIRECTION_LOCALE -> TextDirectionHeuristics.LOCALE;
            case TEXT_DIRECTION_FIRST_STRONG_LTR -> TextDirectionHeuristics.FIRSTSTRONG_LTR;
            case TEXT_DIRECTION_FIRST_STRONG_RTL -> TextDirectionHeuristics.FIRSTSTRONG_RTL;
            default -> isLayoutRtl() ? TextDirectionHeuristics.FIRSTSTRONG_RTL :
                    TextDirectionHeuristics.FIRSTSTRONG_LTR;
        };
    }

    /**
     * @hide
     */
    @VisibleForTesting
    public void nullLayouts() {
        if (mLayout instanceof BoringLayout && mSavedLayout == null) {
            mSavedLayout = (BoringLayout) mLayout;
        }
        if (mHintLayout instanceof BoringLayout && mSavedHintLayout == null) {
            mSavedHintLayout = (BoringLayout) mHintLayout;
        }

        mBoring = mHintBoring = null;
    }

    /**
     * Make a new Layout based on the already-measured size of the view,
     * on the assumption that it was measured correctly at some point.
     */
    private void assumeLayout() {
        int width = getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();

        if (width < 1) {
            width = 0;
        }

        int physicalWidth = width;

        if (mHorizontallyScrolling) {
            width = VERY_WIDE;
        }

        makeNewLayout(width, physicalWidth, UNKNOWN_BORING, UNKNOWN_BORING,
                physicalWidth, false);
    }

    private Layout.Alignment getLayoutAlignment() {
        return switch (getTextAlignment()) {
            case TEXT_ALIGNMENT_GRAVITY -> switch (mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
                case Gravity.END -> Layout.Alignment.ALIGN_OPPOSITE;
                case Gravity.LEFT -> Layout.Alignment.ALIGN_LEFT;
                case Gravity.RIGHT -> Layout.Alignment.ALIGN_RIGHT;
                case Gravity.CENTER_HORIZONTAL -> Layout.Alignment.ALIGN_CENTER;
                default -> Layout.Alignment.ALIGN_NORMAL;
            };
            case TEXT_ALIGNMENT_TEXT_END -> Layout.Alignment.ALIGN_OPPOSITE;
            case TEXT_ALIGNMENT_CENTER -> Layout.Alignment.ALIGN_CENTER;
            case TEXT_ALIGNMENT_VIEW_START -> isLayoutRtl()
                    ? Layout.Alignment.ALIGN_RIGHT : Layout.Alignment.ALIGN_LEFT;
            case TEXT_ALIGNMENT_VIEW_END -> isLayoutRtl()
                    ? Layout.Alignment.ALIGN_LEFT : Layout.Alignment.ALIGN_RIGHT;
            default -> Layout.Alignment.ALIGN_NORMAL;
        };
    }

    /**
     * The width passed in is now the desired layout width,
     * not the full view width with padding.
     * {@hide}
     */
    @VisibleForTesting
    public final void makeNewLayout(int wantWidth, int hintWidth,
                                    BoringLayout.Metrics boring,
                                    BoringLayout.Metrics hintBoring,
                                    int ellipsisWidth, boolean bringIntoView) {
        // Update "old" cached values
        mOldMaximum = mMaximum;
        mOldMaxMode = mMaxMode;

        mHighlightPathBogus = true;

        if (wantWidth < 0) {
            wantWidth = 0;
        }
        if (hintWidth < 0) {
            hintWidth = 0;
        }

        Layout.Alignment alignment = getLayoutAlignment();
        final boolean testDirChange = mSingleLine && mLayout != null
                && (alignment == Layout.Alignment.ALIGN_NORMAL
                || alignment == Layout.Alignment.ALIGN_OPPOSITE);
        int oldDir = 0;
        if (testDirChange) oldDir = mLayout.getParagraphDirection(0);
        boolean shouldEllipsize = mEllipsize != null;
        TextUtils.TruncateAt effectiveEllipsize = mEllipsize;

        if (mTextDir == null) {
            mTextDir = getTextDirectionHeuristic();
        }

        mLayout = makeSingleLayout(wantWidth, boring, ellipsisWidth, alignment, shouldEllipsize,
                effectiveEllipsize, effectiveEllipsize == mEllipsize);

        shouldEllipsize = mEllipsize != null;
        mHintLayout = null;

        if (mHint != null) {
            if (shouldEllipsize) hintWidth = wantWidth;

            if (hintBoring == UNKNOWN_BORING) {
                hintBoring = BoringLayout.isBoring(mHint, mTextPaint, mTextDir,
                        mHintBoring);
                if (hintBoring != null) {
                    mHintBoring = hintBoring;
                }
            }

            if (hintBoring != null) {
                if (hintBoring.width <= hintWidth
                        && (!shouldEllipsize || hintBoring.width <= ellipsisWidth)) {
                    if (mSavedHintLayout != null) {
                        mHintLayout = mSavedHintLayout.replaceOrMake(mHint, mTextPaint,
                                hintWidth, alignment,
                                hintBoring, mIncludePad);
                    } else {
                        mHintLayout = BoringLayout.make(mHint, mTextPaint,
                                hintWidth, alignment,
                                hintBoring, mIncludePad);
                    }

                    mSavedHintLayout = (BoringLayout) mHintLayout;
                } else if (shouldEllipsize && hintBoring.width <= hintWidth) {
                    if (mSavedHintLayout != null) {
                        mHintLayout = mSavedHintLayout.replaceOrMake(mHint, mTextPaint,
                                hintWidth, alignment,
                                hintBoring, mIncludePad, mEllipsize,
                                ellipsisWidth);
                    } else {
                        mHintLayout = BoringLayout.make(mHint, mTextPaint,
                                hintWidth, alignment,
                                hintBoring, mIncludePad, mEllipsize,
                                ellipsisWidth);
                    }
                }
            }
            // TODO: code duplication with makeSingleLayout()
            if (mHintLayout == null) {
                StaticLayout.Builder builder = StaticLayout.builder(mHint, 0,
                                mHint.length(), mTextPaint, hintWidth)
                        .setAlignment(alignment)
                        .setTextDirection(mTextDir)
                        .setIncludePad(mIncludePad)
                        .setFallbackLineSpacing(mUseFallbackLineSpacing)
                        .setMaxLines(mMaxMode == LINES ? mMaximum : Integer.MAX_VALUE);
                if (shouldEllipsize) {
                    builder.setEllipsize(mEllipsize)
                            .setEllipsizedWidth(ellipsisWidth);
                }
                mHintLayout = builder.build();
            }
        }

        if (bringIntoView || (testDirChange && oldDir != mLayout.getParagraphDirection(0))) {
            registerForPreDraw();
        }
    }

    /**
     * Returns true if DynamicLayout is required
     *
     * @hide
     */
    @VisibleForTesting
    public final boolean useDynamicLayout() {
        return isTextSelectable() || (mSpannable != null && mPrecomputed == null);
    }

    /**
     * @hide
     */
    protected Layout makeSingleLayout(int wantWidth, BoringLayout.Metrics boring, int ellipsisWidth,
                                      Layout.Alignment alignment, boolean shouldEllipsize,
                                      TextUtils.TruncateAt effectiveEllipsize, boolean useSaved) {
        Layout result = null;
        if (useDynamicLayout()) {
            final DynamicLayout.Builder builder = DynamicLayout.builder(mText, mTextPaint,
                            wantWidth)
                    .setDisplayText(mTransformed)
                    .setAlignment(alignment)
                    .setTextDirection(mTextDir)
                    .setIncludePad(mIncludePad)
                    .setFallbackLineSpacing(mUseFallbackLineSpacing)
                    .setEllipsize(effectiveEllipsize)
                    .setEllipsizedWidth(ellipsisWidth);
            result = builder.build();
        } else {
            if (boring == UNKNOWN_BORING) {
                boring = BoringLayout.isBoring(mTransformed, mTextPaint, mTextDir, mBoring);
                if (boring != null) {
                    mBoring = boring;
                }
            }

            if (boring != null) {
                if (boring.width <= wantWidth
                        && (effectiveEllipsize == null || boring.width <= ellipsisWidth)) {
                    if (useSaved && mSavedLayout != null) {
                        result = mSavedLayout.replaceOrMake(mTransformed, mTextPaint,
                                wantWidth, alignment,
                                boring, mIncludePad);
                    } else {
                        result = BoringLayout.make(mTransformed, mTextPaint,
                                wantWidth, alignment,
                                boring, mIncludePad);
                    }

                    if (useSaved) {
                        mSavedLayout = (BoringLayout) result;
                    }
                } else if (shouldEllipsize && boring.width <= wantWidth) {
                    if (useSaved && mSavedLayout != null) {
                        result = mSavedLayout.replaceOrMake(mTransformed, mTextPaint,
                                wantWidth, alignment,
                                boring, mIncludePad, effectiveEllipsize,
                                ellipsisWidth);
                    } else {
                        result = BoringLayout.make(mTransformed, mTextPaint,
                                wantWidth, alignment,
                                boring, mIncludePad, effectiveEllipsize,
                                ellipsisWidth);
                    }
                }
            }
        }
        if (result == null) {
            StaticLayout.Builder builder = StaticLayout.builder(mTransformed,
                            0, mTransformed.length(), mTextPaint, wantWidth)
                    .setAlignment(alignment)
                    .setTextDirection(mTextDir)
                    .setIncludePad(mIncludePad)
                    .setFallbackLineSpacing(mUseFallbackLineSpacing)
                    .setMaxLines(mMaxMode == LINES ? mMaximum : Integer.MAX_VALUE);
            if (shouldEllipsize) {
                builder.setEllipsize(effectiveEllipsize)
                        .setEllipsizedWidth(ellipsisWidth);
            }
            result = builder.build();
        }
        return result;
    }

    @Override
    public void onHoverChanged(boolean hovered) {
        if (hovered && (isTextSelectable() || (mText instanceof Editable && isEnabled()))) {
            setPointerIcon(PointerIcon.getSystemIcon(PointerIcon.TYPE_TEXT));
        } else {
            setPointerIcon(null);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        // Will change text color
        if (mEditor != null) {
            mEditor.invalidateTextDisplayList();

            // start or stop the cursor blinking as appropriate
            mEditor.makeBlink();
        }
    }

    /**
     * Convenience for {@link Selection#getSelectionStart}.
     */
    public int getSelectionStart() {
        return Selection.getSelectionStart(getText());
    }

    /**
     * Convenience for {@link Selection#getSelectionEnd}.
     */
    public int getSelectionEnd() {
        return Selection.getSelectionEnd(getText());
    }

    /**
     * Return true iff there is a selection of nonzero length inside this text view.
     */
    public boolean hasSelection() {
        final int selectionStart = getSelectionStart();
        final int selectionEnd = getSelectionEnd();

        return selectionStart >= 0 && selectionEnd > 0 && selectionStart != selectionEnd;
    }

    /**
     * This method is used by the ArrowKeyMovementMethod to jump from one word to the other.
     * Made available to achieve a consistent behavior.
     *
     * @hide
     */
    public WordIterator getWordIterator() {
        if (mEditor != null)
            return mEditor.getWordIterator();
        return null;
    }

    private void sendBeforeTextChanged(CharSequence text, int start, int before, int after) {
        if (mListeners != null) {
            final ArrayList<TextWatcher> list = mListeners;
            for (TextWatcher textWatcher : list) {
                textWatcher.beforeTextChanged(text, start, before, after);
            }
        }
    }

    /**
     * Not private so it can be called from an inner class without going
     * through a thunk.
     */
    void sendOnTextChanged(CharSequence text, int start, int before, int after) {
        if (mListeners != null) {
            final ArrayList<TextWatcher> list = mListeners;
            for (TextWatcher textWatcher : list) {
                textWatcher.onTextChanged(text, start, before, after);
            }
        }

        if (mEditor != null) mEditor.sendOnTextChanged(start, before, after);
    }

    /**
     * This method is called when the text is changed, in case any subclasses
     * would like to know.
     * <p>
     * Within <code>text</code>, the <code>lengthAfter</code> characters
     * beginning at <code>start</code> have just replaced old text that had
     * length <code>lengthBefore</code>. It is an error to attempt to make
     * changes to <code>text</code> from this callback.
     *
     * @param text         The text the TextView is displaying
     * @param start        The offset of the start of the range of the text that was
     *                     modified
     * @param lengthBefore The length of the former text that has been replaced
     * @param lengthAfter  The length of the replacement modified text
     */
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        // intentionally empty, template pattern method can be overridden by subclasses
    }

    /**
     * Not private so it can be called from an inner class without going
     * through a thunk.
     */
    void sendAfterTextChanged(Editable text) {
        if (mListeners != null) {
            final ArrayList<TextWatcher> list = mListeners;
            for (TextWatcher textWatcher : list) {
                textWatcher.afterTextChanged(text);
            }
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction) {
        if (mEditor != null) mEditor.onFocusChanged(focused, direction);

        super.onFocusChanged(focused, direction);
    }

    /**
     * @return True if this TextView contains a text that can be edited, or if this is
     * a selectable TextView.
     */
    boolean isTextEditable() {
        return mText instanceof Editable && mEditor != null && isEnabled();
    }

    /**
     * Move the cursor, if needed, so that it is at an offset that is visible
     * to the user.  This will not move the cursor if it represents more than
     * one character (a selection range).  This will only work if the
     * TextView contains spannable text; otherwise it will do nothing.
     *
     * @return True if the cursor was actually moved, false otherwise.
     */
    public boolean moveCursorToVisibleOffset() {
        if (mSpannable == null) {
            return false;
        }
        int start = getSelectionStart();
        int end = getSelectionEnd();
        if (start != end) {
            return false;
        }

        // First: make sure the line is visible on screen:

        int line = mLayout.getLineForOffset(start);

        final int top = mLayout.getLineTop(line);
        final int bottom = mLayout.getLineTop(line + 1);
        final int vspace = getHeight() - getExtendedPaddingTop() - getExtendedPaddingBottom();
        int vslack = (bottom - top) / 2;
        if (vslack > vspace / 4) {
            vslack = vspace / 4;
        }
        final int vs = mScrollY;

        if (top < (vs + vslack)) {
            line = mLayout.getLineForVertical(vs + vslack + (bottom - top));
        } else if (bottom > (vspace + vs - vslack)) {
            line = mLayout.getLineForVertical(vspace + vs - vslack - (bottom - top));
        }

        // Next: make sure the character is visible on screen:

        final int hspace = getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();
        final int hs = mScrollX;
        final int leftChar = mLayout.getOffsetForHorizontal(line, hs);
        final int rightChar = mLayout.getOffsetForHorizontal(line, hspace + hs);

        // line might contain bidirectional text
        final int lowChar = Math.min(leftChar, rightChar);
        final int highChar = Math.max(leftChar, rightChar);

        int newStart = start;
        if (newStart < lowChar) {
            newStart = lowChar;
        } else if (newStart > highChar) {
            newStart = highChar;
        }

        if (newStart != start) {
            Selection.setSelection(mSpannable, newStart);
            return true;
        }

        return false;
    }

    /**
     * Returns true, only while processing a touch gesture, if the initial
     * touch down event caused focus to move to the text view and as a result
     * its selection changed.  Only valid while processing the touch gesture
     * of interest, in an editable text view.
     */
    public boolean didTouchFocusSelect() {
        return mEditor != null && mEditor.mTouchFocusSelected;
    }

    @Override
    public boolean onTouchEvent(@Nonnull MotionEvent event) {
        final int action = event.getAction();
        if (mEditor != null) {
            mEditor.onTouchEvent(event);
        }

        final boolean superResult = super.onTouchEvent(event);

        /*
         * Don't handle the release after a long press, because it will move the selection away from
         * whatever the menu action was trying to affect. If the long press should have triggered an
         * insertion action mode, we can now actually show it.
         */
        if (mEditor != null && mEditor.mDiscardNextActionUp && action == MotionEvent.ACTION_UP) {
            mEditor.mDiscardNextActionUp = false;
            return superResult;
        }

        final boolean touchIsFinished = (action == MotionEvent.ACTION_UP)
                && (mEditor == null || !mEditor.mIgnoreActionUpEvent) && isFocused();

        if ((mMovement != null || mEditor != null) && isEnabled()
                && mSpannable != null && mLayout != null) {
            boolean handled = false;

            if (mMovement != null) {
                handled |= mMovement.onTouchEvent(this, mSpannable, event);
            }

            final boolean textIsSelectable = isTextSelectable();
            if (touchIsFinished && mLinksClickable && mAutoLinkMask != 0 && textIsSelectable) {
                //TODO links clicking
            }

            if (touchIsFinished && (isTextEditable() || textIsSelectable)) {
                // The above condition ensures that the mEditor is not null
                mEditor.onTouchUpEvent(event);

                handled = true;
            }

            if (handled) {
                return true;
            }
        }

        return superResult;
    }

    @Override
    public boolean onKeyDown(int keyCode, @Nonnull KeyEvent event) {
        if (!isEnabled()) {
            return super.onKeyDown(keyCode, event);
        }

        // If this is the initial keydown, we don't want to prevent a movement away from this view.
        // While this shouldn't be necessary because any time we're preventing default movement we
        // should be restricting the focus to remain within this view, thus we'll also receive
        // the key up event, occasionally key up events will get dropped and we don't want to
        // prevent the user from traversing out of this on the next key down.
        if (event.getRepeatCount() == 0 && !KeyEvent.isModifierKey(keyCode)) {
            mPreventDefaultMovement = false;
        }

        switch (keyCode) {
            case KeyEvent.KEY_ENTER:
            case KeyEvent.KEY_KP_ENTER:
                if (event.hasNoModifiers()) {
                    if (mSingleLine && mBufferType == BufferType.EDITABLE) {
                        if (hasOnClickListeners()) {
                            return super.onKeyDown(keyCode, event);
                        }
                        return true;
                    }
                }
                break;
            case KeyEvent.KEY_TAB:
                if (event.hasNoModifiers() || event.isShiftPressed()) {
                    // Tab is used to move focus.
                    return super.onKeyDown(keyCode, event);
                }
                break;
        }

        if (mEditor != null && mBufferType == BufferType.EDITABLE) {
            if (TextKeyListener.getInstance().onKeyDown(this, (Editable) mText, keyCode, event)) {
                return true;
            }
        }

        if (mMovement != null && mLayout != null) {
            if (mSpannable != null && mMovement.onKeyDown(this, mSpannable, keyCode, event)) {
                if (event.getRepeatCount() == 0 && !KeyEvent.isModifierKey(keyCode)) {
                    mPreventDefaultMovement = true;
                }
                return true;
            }
            // Consume arrows from keyboard devices to prevent focus leaving the editor.
            // DPAD/JOY devices (Gamepads, TV remotes) often lack a TAB key so allow those
            // to move focus with arrows.
            switch (keyCode) {
                case KeyEvent.KEY_UP:
                case KeyEvent.KEY_DOWN:
                case KeyEvent.KEY_LEFT:
                case KeyEvent.KEY_RIGHT:
                    return true;
            }
        }

        return mPreventDefaultMovement && !KeyEvent.isModifierKey(keyCode) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, @Nonnull KeyEvent event) {
        if (!isEnabled()) {
            return super.onKeyUp(keyCode, event);
        }

        if (!KeyEvent.isModifierKey(keyCode)) {
            mPreventDefaultMovement = false;
        }

        switch (keyCode) {
            case KeyEvent.KEY_ENTER:
            case KeyEvent.KEY_KP_ENTER:
                if (event.hasNoModifiers()) {
                    if (mSingleLine && mBufferType == BufferType.EDITABLE) {
                        /*
                         * If there is a click listener, just call through to
                         * super, which will invoke it.
                         *
                         * If there isn't a click listener, try to advance focus,
                         * but still call through to super, which will reset the
                         * pressed state and long-press state.  (It will also
                         * call performClick(), but that won't do anything in
                         * this case.)
                         */
                        if (!hasOnClickListeners()) {
                            View v = focusSearch(FOCUS_DOWN);

                            if (v != null) {
                                if (!v.requestFocus(FOCUS_DOWN)) {
                                    throw new IllegalStateException("focus search returned a view "
                                            + "that wasn't able to take focus!");
                                }

                                /*
                                 * Return true because we handled the key; super
                                 * will return false because there was no click
                                 * listener.
                                 */
                                super.onKeyUp(keyCode, event);
                                return true;
                            }
                        }
                    }
                    return super.onKeyUp(keyCode, event);
                }
                break;
        }

        if (mEditor != null && mBufferType == BufferType.EDITABLE) {
            if (TextKeyListener.getInstance().onKeyUp(this, (Editable) mText, keyCode, event)) {
                return true;
            }
        }

        if (mMovement != null && mLayout != null) {
            if (mSpannable != null && mMovement.onKeyUp(this, mSpannable, keyCode, event)) {
                return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyShortcut(int keyCode, @Nonnull KeyEvent event) {
        if (event.isCtrlPressed()) {
            // Handle Ctrl-only shortcuts.
            switch (keyCode) {
                case KeyEvent.KEY_X:
                    if (canCut()) {
                        return onTextContextMenuItem(ID_CUT);
                    }
                    break;
                case KeyEvent.KEY_C:
                    if (canCopy()) {
                        return onTextContextMenuItem(ID_COPY);
                    }
                    break;
                case KeyEvent.KEY_V:
                    if (canPaste()) {
                        return onTextContextMenuItem(ID_PASTE);
                    }
                    break;
                case KeyEvent.KEY_A:
                    if (canSelectAllText()) {
                        return onTextContextMenuItem(ID_SELECT_ALL);
                    }
                    break;
            }
        }
        return super.onKeyShortcut(keyCode, event);
    }

    /**
     * Called when a context menu option for the text view is selected.  Currently,
     * this will be one of {@link #ID_CUT}, {@link #ID_COPY}, {@link #ID_PASTE} or
     * {@link #ID_SELECT_ALL}.
     *
     * @return true if the context menu item action was performed.
     */
    public boolean onTextContextMenuItem(int id) {
        int min = 0;
        int max = mText.length();

        if (isFocused()) {
            final int selStart = getSelectionStart();
            final int selEnd = getSelectionEnd();

            min = Math.max(0, Math.min(selStart, selEnd));
            max = Math.max(0, Math.max(selStart, selEnd));
        }

        switch (id) {
            case ID_CUT -> {
                CharSequence cut = mTransformed.subSequence(min, max);
                Clipboard.setText(cut);
                getEditableText().delete(min, max);
                return true;
            }
            case ID_COPY -> {
                CharSequence copy = mTransformed.subSequence(min, max);
                Clipboard.setText(copy);
                return true;
            }
            case ID_PASTE -> {
                String replacement = Clipboard.getText();
                if (replacement != null) {
                    Editable editable = getEditableText();
                    Selection.setSelection(editable, max);
                    editable.replace(min, max, replacement);
                }
                return true;
            }
            case ID_SELECT_ALL -> {
                selectAllText();
                return true;
            }
        }
        return false;
    }

    /**
     * Get the character offset closest to the specified absolute position. A typical use case is to
     * pass the result of {@link MotionEvent#getX()} and {@link MotionEvent#getY()} to this method.
     *
     * @param x The horizontal absolute position of a point on screen
     * @param y The vertical absolute position of a point on screen
     * @return the character offset for the character whose position is closest to the specified
     * position. Returns -1 if there is no layout.
     */
    public int getOffsetForPosition(float x, float y) {
        if (getLayout() == null)
            return -1;
        final int line = getLineAtCoordinate(y);
        return getOffsetAtCoordinate(line, x);
    }

    int getLineAtCoordinate(float y) {
        y -= getTotalPaddingTop();
        // Clamp the position to inside of the view.
        y = Math.max(0.0f, y);
        y = Math.min(getHeight() - getTotalPaddingBottom() - 1, y);
        y += getScrollY();
        return getLayout().getLineForVertical((int) y);
    }

    float convertToLocalHorizontalCoordinate(float x) {
        x -= getTotalPaddingLeft();
        // Clamp the position to inside of the view.
        x = Math.max(0.0f, x);
        x = Math.min(getWidth() - getTotalPaddingRight() - 1, x);
        x += getScrollX();
        return x;
    }

    int getOffsetAtCoordinate(int line, float x) {
        x = convertToLocalHorizontalCoordinate(x);
        return getLayout().getOffsetForHorizontal(line, x);
    }

    boolean canCut() {
        if (hasPasswordTransformationMethod()) {
            return false;
        }

        return mText.length() > 0 && hasSelection() && mText instanceof Editable
                && mEditor != null && mBufferType == BufferType.EDITABLE;
    }

    boolean canCopy() {
        if (hasPasswordTransformationMethod()) {
            return false;
        }

        return mText.length() > 0 && hasSelection() && mEditor != null;
    }

    boolean canPaste() {
        return mText instanceof Editable
                && mEditor != null && mBufferType == BufferType.EDITABLE
                && getSelectionStart() >= 0
                && getSelectionEnd() >= 0;
    }

    boolean canSelectAllText() {
        return mText.length() != 0 && mEditor != null
                && mLayout != null && textCanBeSelected()
                && !hasPasswordTransformationMethod()
                && !(getSelectionStart() == 0 && getSelectionEnd() == mText.length());
    }

    boolean selectAllText() {
        if (mSpannable == null) {
            return false;
        }
        final int length = mText.length();
        Selection.setSelection(mSpannable, 0, length);
        return length > 0;
    }

    /**
     * Test based on the <i>intrinsic</i> characteristics of the TextView.
     * The text must be spannable and the movement method must allow for arbitrary selection.
     */
    boolean textCanBeSelected() {
        // prepareCursorController() relies on this method.
        // If you change this condition, make sure prepareCursorController is called anywhere
        // the value of this condition might be changed.
        if (mMovement == null || !mMovement.canSelectArbitrarily()) return false;
        return isTextEditable()
                || (isTextSelectable() && mText instanceof Spannable && isEnabled());
    }


    /**
     * An Editor should be created as soon as any of the editable-specific fields (grouped
     * inside the Editor object) is assigned to a non-default value.
     * This method will create the Editor if needed.
     * <p>
     * A standard TextView (as well as buttons, checkboxes...) should not qualify and hence will
     * have a null Editor, unlike an EditText. Inconsistent in-between states will have an
     * Editor for backward compatibility, as soon as one of these fields is assigned.
     * <p>
     * Also note that for performance reasons, the mEditor is created when needed, but not
     * reset when no more edit-specific fields are needed.
     */
    private void createEditorIfNeeded() {
        if (mEditor == null) {
            mEditor = new Editor(this);
        }
    }

    /**
     * Type of the text buffer that defines the characteristics of the text such as static,
     * styleable, or editable.
     */
    public enum BufferType {
        NORMAL,
        SPANNABLE,
        EDITABLE
    }

    static class Drawables {
        static final int LEFT = 0;
        static final int TOP = 1;
        static final int RIGHT = 2;
        static final int BOTTOM = 3;

        static final int DRAWABLE_NONE = -1;
        static final int DRAWABLE_RIGHT = 0;
        static final int DRAWABLE_LEFT = 1;

        final Rect mCompoundRect = new Rect();

        final Drawable[] mShowing = new Drawable[4];

        boolean mHasTint;
        boolean mHasTintMode;

        Drawable mDrawableStart, mDrawableEnd, mDrawableError, mDrawableTemp;
        Drawable mDrawableLeftInitial, mDrawableRightInitial;

        boolean mIsRtlCompatibilityMode;
        boolean mOverride;

        int mDrawableSizeTop, mDrawableSizeBottom, mDrawableSizeLeft, mDrawableSizeRight,
                mDrawableSizeStart, mDrawableSizeEnd, mDrawableSizeError, mDrawableSizeTemp;

        int mDrawableWidthTop, mDrawableWidthBottom, mDrawableHeightLeft, mDrawableHeightRight,
                mDrawableHeightStart, mDrawableHeightEnd, mDrawableHeightError, mDrawableHeightTemp;

        int mDrawablePadding;

        int mDrawableSaved = DRAWABLE_NONE;

        public Drawables() {
            mIsRtlCompatibilityMode = !ModernUI.get().hasRtlSupport();
            mOverride = false;
        }

        /**
         * @return {@code true} if this object contains metadata that needs to
         * be retained, {@code false} otherwise
         */
        public boolean hasMetadata() {
            return mDrawablePadding != 0 || mHasTintMode || mHasTint;
        }

        /**
         * Updates the list of displayed drawables to account for the current
         * layout direction.
         *
         * @param layoutDirection the current layout direction
         * @return {@code true} if the displayed drawables changed
         */
        public boolean resolveWithLayoutDirection(int layoutDirection) {
            final Drawable previousLeft = mShowing[Drawables.LEFT];
            final Drawable previousRight = mShowing[Drawables.RIGHT];

            // First reset "left" and "right" drawables to their initial values
            mShowing[Drawables.LEFT] = mDrawableLeftInitial;
            mShowing[Drawables.RIGHT] = mDrawableRightInitial;

            if (mIsRtlCompatibilityMode) {
                // Use "start" drawable as "left" drawable if the "left" drawable was not defined
                if (mDrawableStart != null && mShowing[Drawables.LEFT] == null) {
                    mShowing[Drawables.LEFT] = mDrawableStart;
                    mDrawableSizeLeft = mDrawableSizeStart;
                    mDrawableHeightLeft = mDrawableHeightStart;
                }
                // Use "end" drawable as "right" drawable if the "right" drawable was not defined
                if (mDrawableEnd != null && mShowing[Drawables.RIGHT] == null) {
                    mShowing[Drawables.RIGHT] = mDrawableEnd;
                    mDrawableSizeRight = mDrawableSizeEnd;
                    mDrawableHeightRight = mDrawableHeightEnd;
                }
            } else {
                // JB-MR1+ normal case: "start" / "end" drawables are overriding "left" / "right"
                // drawable if and only if they have been defined
                switch (layoutDirection) {
                    case LAYOUT_DIRECTION_RTL:
                        if (mOverride) {
                            mShowing[Drawables.RIGHT] = mDrawableStart;
                            mDrawableSizeRight = mDrawableSizeStart;
                            mDrawableHeightRight = mDrawableHeightStart;

                            mShowing[Drawables.LEFT] = mDrawableEnd;
                            mDrawableSizeLeft = mDrawableSizeEnd;
                            mDrawableHeightLeft = mDrawableHeightEnd;
                        }
                        break;

                    case LAYOUT_DIRECTION_LTR:
                    default:
                        if (mOverride) {
                            mShowing[Drawables.LEFT] = mDrawableStart;
                            mDrawableSizeLeft = mDrawableSizeStart;
                            mDrawableHeightLeft = mDrawableHeightStart;

                            mShowing[Drawables.RIGHT] = mDrawableEnd;
                            mDrawableSizeRight = mDrawableSizeEnd;
                            mDrawableHeightRight = mDrawableHeightEnd;
                        }
                        break;
                }
            }

            applyErrorDrawableIfNeeded(layoutDirection);

            return mShowing[Drawables.LEFT] != previousLeft
                    || mShowing[Drawables.RIGHT] != previousRight;
        }

        public void setErrorDrawable(Drawable dr, TextView tv) {
            if (mDrawableError != dr && mDrawableError != null) {
                mDrawableError.setCallback(null);
            }
            mDrawableError = dr;

            if (mDrawableError != null) {
                final Rect compoundRect = mCompoundRect;
                final int[] state = tv.getDrawableState();

                mDrawableError.setState(state);
                mDrawableError.copyBounds(compoundRect);
                mDrawableError.setCallback(tv);
                mDrawableSizeError = compoundRect.width();
                mDrawableHeightError = compoundRect.height();
            } else {
                mDrawableSizeError = mDrawableHeightError = 0;
            }
        }

        private void applyErrorDrawableIfNeeded(int layoutDirection) {
            // first restore the initial state if needed
            switch (mDrawableSaved) {
                case DRAWABLE_LEFT:
                    mShowing[Drawables.LEFT] = mDrawableTemp;
                    mDrawableSizeLeft = mDrawableSizeTemp;
                    mDrawableHeightLeft = mDrawableHeightTemp;
                    break;
                case DRAWABLE_RIGHT:
                    mShowing[Drawables.RIGHT] = mDrawableTemp;
                    mDrawableSizeRight = mDrawableSizeTemp;
                    mDrawableHeightRight = mDrawableHeightTemp;
                    break;
                case DRAWABLE_NONE:
                default:
            }
            // then, if needed, assign the Error drawable to the correct location
            if (mDrawableError != null) {
                if (layoutDirection == LAYOUT_DIRECTION_RTL) {
                    mDrawableSaved = DRAWABLE_LEFT;
                    mDrawableTemp = mShowing[Drawables.LEFT];
                    mDrawableSizeTemp = mDrawableSizeLeft;
                    mDrawableHeightTemp = mDrawableHeightLeft;
                    mShowing[Drawables.LEFT] = mDrawableError;
                    mDrawableSizeLeft = mDrawableSizeError;
                    mDrawableHeightLeft = mDrawableHeightError;
                } else {
                    mDrawableSaved = DRAWABLE_RIGHT;
                    mDrawableTemp = mShowing[Drawables.RIGHT];
                    mDrawableSizeTemp = mDrawableSizeRight;
                    mDrawableHeightTemp = mDrawableHeightRight;
                    mShowing[Drawables.RIGHT] = mDrawableError;
                    mDrawableSizeRight = mDrawableSizeError;
                    mDrawableHeightRight = mDrawableHeightError;
                }
            }
        }
    }

    //TODO
    private class ChangeWatcher implements TextWatcher, SpanWatcher {

        @Override
        public void onSpanAdded(Spannable text, Object what, int start, int end) {

        }

        @Override
        public void onSpanRemoved(Spannable text, Object what, int start, int end) {

        }

        @Override
        public void onSpanChanged(Spannable text, Object what, int ost, int oen, int nst, int nen) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }
}
