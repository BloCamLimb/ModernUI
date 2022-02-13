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
import icyllis.modernui.R;
import icyllis.modernui.core.ArchCore;
import icyllis.modernui.core.Clipboard;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.math.Rect;
import icyllis.modernui.text.*;
import icyllis.modernui.text.method.*;
import icyllis.modernui.text.style.CharacterStyle;
import icyllis.modernui.text.style.ParagraphStyle;
import icyllis.modernui.text.style.UpdateAppearance;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.view.*;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Locale;

/**
 * A user interface element that displays text to the user. To provide user-editable text,
 * see {@link EditText}.
 * <p>
 * To display a styled text, use a {@link Spannable} with markup objects, like {@link CharacterStyle}.
 * TextView will hold a copy of your text after calling {@link #setText(CharSequence, BufferType)}.
 * After you provide a String, you want it to have styles:
 * <pre>
 *  TextView textView = new TextView();
 *  textView.setText(string, TextView.BufferType.SPANNABLE);
 *  // add styles
 *  Spannable spannable = (Spannable) textView.getText();
 *  spannable.setSpan(new ForegroundColorSpan(0xfff699b4), 0, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
 * </pre>
 * If you want a non-editable text to be selected, see {@link #setTextIsSelectable(boolean)}.
 * <p>
 * Explore the methods in this class to find more usage
 */
@SuppressWarnings("unused")
public class TextView extends View implements ViewTreeObserver.OnPreDrawListener {

    /**
     * @see #onTextContextMenuItem(int)
     */
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

    private static final InputFilter[] NO_FILTERS = new InputFilter[0];
    private static final Spanned EMPTY_SPANNED = new SpannedString("");

    private static final int CHANGE_WATCHER_PRIORITY = 100;

    private ColorStateList mTextColor;
    private ColorStateList mHintTextColor;
    private ColorStateList mLinkTextColor;

    private int mCurTextColor;

    private int mCurHintTextColor;

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

    @Nullable
    private TextUtils.TruncateAt mEllipsize;

    Drawables mDrawables;

    private int mLastLayoutDirection = LAYOUT_DIRECTION_UNDEFINED;

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

    private BoringLayout.Metrics mBoring;
    private BoringLayout.Metrics mHintBoring;
    private BoringLayout mSavedLayout;
    private BoringLayout mSavedHintLayout;

    private TextDirectionHeuristic mTextDir;

    @Nonnull
    private InputFilter[] mFilters = NO_FILTERS;

    int mHighlightColor = 0x6633B5E5;
    private FloatArrayList mHighlightPath;
    private boolean mHighlightPathBogus = true;

    /**
     * Created on demand when one of the Editor fields is used.
     * See {@link #createEditorIfNeeded()}.
     */
    private Editor mEditor;

    public TextView() {
        setTextColor(0xFFFFFFFF);
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
            mTextPaint.setFontSize(sp(size));

            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    /**
     * Sets the default text style in which the text should be displayed.
     *
     * @param style the text style to set
     * @see #getTextStyle()
     */
    public void setTextStyle(int style) {
        if (style != mTextPaint.getFontStyle()) {
            mTextPaint.setFontStyle(style);

            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    /**
     * Gets the current text style that is used to style the text.
     *
     * @return the current text style
     * @see #setTextStyle(int)
     */
    public int getTextStyle() {
        return mTextPaint.getFontStyle();
    }

    /**
     * Sets the typeface in which the text should be displayed.
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
     * @param enabled whether to expand line spacing based on fallback fonts, {@code true} by default
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

    /**
     * Sets the text color for all the states (normal, selected,
     * focused) to be this color.
     *
     * @param color A color value in the form 0xAARRGGBB.
     * @see #setTextColor(ColorStateList)
     * @see #getTextColors()
     */
    public void setTextColor(int color) {
        mTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    /**
     * Sets the text color.
     *
     * @see #setTextColor(int)
     * @see #getTextColors()
     * @see #setHintTextColor(ColorStateList)
     * @see #setLinkTextColor(ColorStateList)
     */
    public void setTextColor(ColorStateList colors) {
        if (colors == null) {
            throw new NullPointerException();
        }

        mTextColor = colors;
        updateTextColors();
    }

    /**
     * Gets the text colors for the different states (normal, selected, focused) of the TextView.
     *
     * @see #setTextColor(ColorStateList)
     * @see #setTextColor(int)
     */
    public final ColorStateList getTextColors() {
        return mTextColor;
    }

    /**
     * Return the current color selected for normal text.
     *
     * @return Returns the current text color.
     */
    public final int getCurrentTextColor() {
        return mCurTextColor;
    }

    /**
     * Sets the color used to display the selection highlight.
     */
    public void setHighlightColor(int color) {
        if (mHighlightColor != color) {
            mHighlightColor = color;
            invalidate();
        }
    }

    /**
     * @return the color used to display the selection highlight
     * @see #setHighlightColor(int)
     */
    public int getHighlightColor() {
        return mHighlightColor;
    }

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
     * Sets the color of the hint text for all the states (disabled, focussed, selected...) of this
     * TextView.
     *
     * @see #setHintTextColor(ColorStateList)
     * @see #getHintTextColors()
     * @see #setTextColor(int)
     */
    public final void setHintTextColor(int color) {
        mHintTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    /**
     * Sets the color of the hint text.
     *
     * @see #getHintTextColors()
     * @see #setHintTextColor(int)
     * @see #setTextColor(ColorStateList)
     * @see #setLinkTextColor(ColorStateList)
     */
    public final void setHintTextColor(ColorStateList colors) {
        mHintTextColor = colors;
        updateTextColors();
    }

    /**
     * @return the color of the hint text, for the different states of this TextView.
     * @see #setHintTextColor(ColorStateList)
     * @see #setHintTextColor(int)
     * @see #setTextColor(ColorStateList)
     * @see #setLinkTextColor(ColorStateList)
     */
    public final ColorStateList getHintTextColors() {
        return mHintTextColor;
    }

    /**
     * <p>Return the current color selected to paint the hint text.</p>
     *
     * @return Returns the current hint text color.
     */
    public final int getCurrentHintTextColor() {
        return mCurHintTextColor;
    }

    /**
     * Sets the color of links in the text.
     *
     * @see #setLinkTextColor(ColorStateList)
     * @see #getLinkTextColors()
     */
    public final void setLinkTextColor(int color) {
        mLinkTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    /**
     * Sets the color of links in the text.
     *
     * @see #setLinkTextColor(int)
     * @see #getLinkTextColors()
     * @see #setTextColor(ColorStateList)
     * @see #setHintTextColor(ColorStateList)
     */
    public final void setLinkTextColor(ColorStateList colors) {
        mLinkTextColor = colors;
        updateTextColors();
    }

    /**
     * @return the list of colors used to paint the links in the text, for the different states of
     * this TextView
     * @see #setLinkTextColor(ColorStateList)
     * @see #setLinkTextColor(int)
     */
    public final ColorStateList getLinkTextColors() {
        return mLinkTextColor;
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

        final int oldLength = mText.length();
        sendBeforeTextChanged(mText, 0, oldLength, text.length());

        boolean needEditableForNotification = mListeners != null && mListeners.size() > 0;

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

            if (mChangeWatcher == null) {
                mChangeWatcher = new ChangeWatcher();
            }

            sp.setSpan(mChangeWatcher, 0, textLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE
                    | (CHANGE_WATCHER_PRIORITY << Spanned.SPAN_PRIORITY_SHIFT));

            if (mEditor != null) {
                mEditor.addSpanWatchers(sp);
            }

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
                if (mEditor != null) {
                    mEditor.mSelectionMoved = false;
                }
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
        mHint = TextUtils.stringOrSpannedString(hint);

        if (mLayout != null) {
            checkForRelayout();
        }

        if (mText.length() == 0) {
            invalidate();
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
    private boolean hasPasswordTransformationMethod() {
        return mTransformation instanceof PasswordTransformationMethod;
    }

    /**
     * Sets the list of input filters that will be used if the buffer is
     * Editable. Has no effect otherwise.
     */
    public void setFilters(@Nonnull InputFilter... filters) {
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
            int boxHeight = getBoxHeight(l);
            int textHeight = l.getHeight();

            if (textHeight < boxHeight) {
                if (gravity == Gravity.BOTTOM) {
                    voffset = boxHeight - textHeight;
                } else { // (gravity == Gravity.CENTER_VERTICAL)
                    voffset = (boxHeight - textHeight) >> 1;
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

    void invalidateCursorPath() {
        if (mHighlightPathBogus) {
            if (getSelectionEnd() >= 0) {
                invalidate();
            }
        } else {
            invalidate();
        }
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

        if (mMovement != null) {
            /* This code also provides auto-scrolling when a cursor is moved using a
             * CursorController (insertion point or selection limits).
             * For selection, ensure start or end is visible depending on controller's state.
             */
            int curs = getSelectionEnd();

            if (curs < 0 && (mGravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
                curs = mText.length();
            }

            if (curs >= 0) {
                bringPointIntoView(curs);
            }
        } else {
            bringTextIntoView();
        }

        unregisterForPreDraw();

        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mEditor != null) {
            mEditor.onAttachedToWindow();
        }

        if (mPreDrawListenerDetached) {
            getViewTreeObserver().addOnPreDrawListener(this);
            mPreDrawListenerDetached = false;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mPreDrawRegistered) {
            getViewTreeObserver().removeOnPreDrawListener(this);
            mPreDrawListenerDetached = true;
        }

        resetResolvedDrawables();

        if (mEditor != null) {
            mEditor.onDetachedFromWindow();
        }

        super.onDetachedFromWindow();
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

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mDrawables != null) {
            for (Drawable dr : mDrawables.mShowing) {
                if (dr != null) {
                    dr.jumpToCurrentState();
                }
            }
        }
    }

    /**
     * Returns the state of the {@code textIsSelectable} flag (See
     * {@link #setTextIsSelectable setTextIsSelectable()}). Although you have to set this flag
     * to allow users to select and copy text in a non-editable TextView, the content of an
     * EditText can always be selected, independently of the value of this flag.
     * <p>
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
     * <p>
     * The content of an EditText (A editable TextView) can always be selected, and
     * you should not the value of this flag.
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

    private void updateTextColors() {
        boolean inval = false;
        final int[] drawableState = getDrawableState();
        int color = mTextColor.getColorForState(drawableState, 0);
        if (color != mCurTextColor) {
            mCurTextColor = color;
            inval = true;
        }
        /*if (mLinkTextColor != null) {
            color = mLinkTextColor.getColorForState(drawableState, 0);
            if (color != mTextPaint.linkColor) {
                mTextPaint.linkColor = color;
                inval = true;
            }
        }*/
        if (mHintTextColor != null) {
            color = mHintTextColor.getColorForState(drawableState, 0);
            if (color != mCurHintTextColor) {
                mCurHintTextColor = color;
                if (mText.length() == 0) {
                    inval = true;
                }
            }
        }
        if (inval) {
            // Text needs to be redrawn with the new color
            invalidate();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (mTextColor != null && mTextColor.isStateful()
                || (mHintTextColor != null && mHintTextColor.isStateful())
                || (mLinkTextColor != null && mLinkTextColor.isStateful())) {
            updateTextColors();
        }

        if (mDrawables != null) {
            final int[] state = getDrawableState();
            for (Drawable dr : mDrawables.mShowing) {
                if (dr != null && dr.isStateful() && dr.setState(state)) {
                    invalidateDrawable(dr);
                }
            }
        }
    }

    @Nonnull
    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace);

        if (isTextSelectable()) {
            // Disable pressed state, which was introduced when TextView was made clickable.
            // Prevents text color change.
            // setClickable(false) would have a similar effect, but it also disables focus changes
            // and long press actions, which are both needed by text selection.
            final int length = drawableState.length;
            for (int i = 0; i < length; i++) {
                if (drawableState[i] == R.attr.state_pressed) {
                    final int[] nonPressedState = new int[length - 1];
                    System.arraycopy(drawableState, 0, nonPressedState, 0, i);
                    System.arraycopy(drawableState, i + 1, nonPressedState, i, length - i - 1);
                    return nonPressedState;
                }
            }
        }

        return drawableState;
    }

    private void drawHighlight(@Nonnull Canvas canvas, int cursorOffsetVertical) {
        final int selStart = getSelectionStart();
        if (mMovement != null && (isFocused() || isPressed()) && selStart >= 0) {
            final int selEnd = getSelectionEnd();
            Paint paint = Paint.take();
            paint.setStrokeWidth(4);
            if (selStart == selEnd) {
                if (mEditor != null && mEditor.shouldRenderCursor()) {
                    if (mHighlightPathBogus) {
                        if (mHighlightPath == null) {
                            mHighlightPath = new FloatArrayList();
                        }
                        mLayout.getCursorPath(selStart, mHighlightPath, mText);
                        mHighlightPathBogus = false;
                    }

                    paint.setColor(mTextPaint.getColor());
                    paint.setStyle(Paint.STROKE);

                    if (cursorOffsetVertical != 0) canvas.translate(0, cursorOffsetVertical);
                    canvas.drawRoundLines(mHighlightPath.elements(), 0, mHighlightPath.size(), false, paint);
                    if (cursorOffsetVertical != 0) canvas.translate(0, -cursorOffsetVertical);
                }
            } else {
                if (mHighlightPathBogus) {
                    if (mHighlightPath == null) {
                        mHighlightPath = new FloatArrayList();
                    }
                    mLayout.getSelectionPath(selStart, selEnd, mHighlightPath);
                    mHighlightPathBogus = false;
                }

                paint.setColor(mHighlightColor);
                paint.setStyle(Paint.FILL);

                if (cursorOffsetVertical != 0) canvas.translate(0, cursorOffsetVertical);
                final float[] src = mHighlightPath.elements();
                final int len = mHighlightPath.size();
                for (int i = 0; i < len; ) {
                    canvas.drawRect(src[i++], src[i++], src[i++], src[i++], paint);
                }
                if (cursorOffsetVertical != 0) canvas.translate(0, -cursorOffsetVertical);
            }
        }
    }

    /**
     * @hide
     */
    public int getHorizontalOffsetForDrawables() {
        return 0;
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas) {
        final int compoundPaddingLeft = getCompoundPaddingLeft();
        final int compoundPaddingTop = getCompoundPaddingTop();
        final int compoundPaddingRight = getCompoundPaddingRight();
        final int compoundPaddingBottom = getCompoundPaddingBottom();
        final int scrollX = mScrollX;
        final int scrollY = mScrollY;

        final Drawables dr = mDrawables;
        if (dr != null) {
            /*
             * Compound, not extended, because the icon is not clipped
             * if the text height is smaller.
             */

            int vspace = getHeight() - compoundPaddingBottom - compoundPaddingTop;
            int hspace = getWidth() - compoundPaddingRight - compoundPaddingLeft;

            final boolean isLayoutRtl = isLayoutRtl();
            int offset = getHorizontalOffsetForDrawables();
            final int leftOffset = isLayoutRtl ? 0 : offset;
            final int rightOffset = isLayoutRtl ? offset : 0;

            // IMPORTANT: The coordinates computed are also used in invalidateDrawable()
            // Make sure to update invalidateDrawable() when changing this code.
            if (dr.mShowing[Drawables.LEFT] != null) {
                canvas.save();
                offset = (vspace - dr.mDrawableHeightLeft) / 2;
                canvas.translate(scrollX + mPaddingLeft + leftOffset,
                        scrollY + compoundPaddingTop + offset);
                dr.mShowing[Drawables.LEFT].draw(canvas);
                canvas.restore();
            }

            // IMPORTANT: The coordinates computed are also used in invalidateDrawable()
            // Make sure to update invalidateDrawable() when changing this code.
            if (dr.mShowing[Drawables.RIGHT] != null) {
                canvas.save();
                offset = (vspace - dr.mDrawableHeightRight) / 2;
                canvas.translate(scrollX + getWidth() - mPaddingRight
                                - dr.mDrawableSizeRight - rightOffset,
                        scrollY + compoundPaddingTop + offset);
                dr.mShowing[Drawables.RIGHT].draw(canvas);
                canvas.restore();
            }

            // IMPORTANT: The coordinates computed are also used in invalidateDrawable()
            // Make sure to update invalidateDrawable() when changing this code.
            if (dr.mShowing[Drawables.TOP] != null) {
                canvas.save();
                offset = (hspace - dr.mDrawableWidthTop) / 2;
                canvas.translate(scrollX + compoundPaddingLeft
                        + offset, scrollY + mPaddingTop);
                dr.mShowing[Drawables.TOP].draw(canvas);
                canvas.restore();
            }

            // IMPORTANT: The coordinates computed are also used in invalidateDrawable()
            // Make sure to update invalidateDrawable() when changing this code.
            if (dr.mShowing[Drawables.BOTTOM] != null) {
                canvas.save();
                offset = (hspace - dr.mDrawableWidthBottom) / 2;
                canvas.translate(scrollX + compoundPaddingLeft + offset,
                        scrollY + getHeight() - mPaddingBottom - dr.mDrawableSizeBottom);
                dr.mShowing[Drawables.BOTTOM].draw(canvas);
                canvas.restore();
            }
        }

        int color = mCurTextColor;

        if (mLayout == null) {
            assumeLayout();
        }

        Layout layout = mLayout;

        if (mHint != null && mText.length() == 0) {
            if (mHintTextColor != null) {
                color = mCurHintTextColor;
            }

            layout = mHintLayout;
        }

        mTextPaint.setColor(color);

        int extendedPaddingTop = getExtendedPaddingTop();
        int extendedPaddingBottom = getExtendedPaddingBottom();

        final int vspace = getHeight() - compoundPaddingBottom - compoundPaddingTop;
        final int maxScrollY = mLayout.getHeight() - vspace;

        float clipLeft = compoundPaddingLeft + scrollX;
        float clipTop = (scrollY == 0) ? 0 : extendedPaddingTop + scrollY;
        float clipRight = getWidth() - getCompoundPaddingRight() + scrollX;
        float clipBottom = getHeight() + scrollY
                - ((scrollY == maxScrollY) ? 0 : extendedPaddingBottom);

        canvas.save();
        canvas.clipRect(clipLeft, clipTop, clipRight, clipBottom);

        int vOffsetText = 0;
        int vOffsetCursor = 0;

        // translate in by our padding
        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
            vOffsetText = getVerticalOffset(false);
            vOffsetCursor = getVerticalOffset(true);
        }
        canvas.translate(compoundPaddingLeft, extendedPaddingTop + vOffsetText);

        final long range = layout.getLineRangeForDraw(canvas);
        if (range >= 0) {
            int firstLine = (int) (range >>> 32);
            int lastLine = (int) (range & 0xFFFFFFFFL);
            layout.drawBackground(canvas, firstLine, lastLine);

            drawHighlight(canvas, vOffsetCursor - vOffsetText);

            layout.drawText(canvas, firstLine, lastLine);
        }

        canvas.restore();
    }

    @Override
    public void getFocusedRect(@Nonnull Rect r) {
        if (mLayout == null) {
            super.getFocusedRect(r);
            return;
        }

        int selEnd = getSelectionEnd();
        if (selEnd < 0) {
            super.getFocusedRect(r);
            return;
        }

        int selStart = getSelectionStart();
        if (selStart < 0 || selStart >= selEnd) {
            int line = mLayout.getLineForOffset(selEnd);
            r.top = mLayout.getLineTop(line);
            r.bottom = mLayout.getLineBottom(line);
            r.left = (int) mLayout.getPrimaryHorizontal(selEnd) - 2;
            r.right = r.left + 4;
        } else {
            int lineStart = mLayout.getLineForOffset(selStart);
            int lineEnd = mLayout.getLineForOffset(selEnd);
            r.top = mLayout.getLineTop(lineStart);
            r.bottom = mLayout.getLineBottom(lineEnd);
            if (lineStart == lineEnd) {
                r.left = (int) mLayout.getPrimaryHorizontal(selStart);
                r.right = (int) mLayout.getPrimaryHorizontal(selEnd);
            } else {
                // Simplify the operation...
                r.left = 0;
                r.right = mLayout.getWidth();
            }
        }

        // Adjust for padding and gravity.
        int paddingLeft = getCompoundPaddingLeft();
        int paddingTop = getExtendedPaddingTop();
        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
            paddingTop += getVerticalOffset(false);
        }
        r.offset(paddingLeft, paddingTop);
        int paddingBottom = getExtendedPaddingBottom();
        r.bottom += paddingBottom;
    }

    /**
     * Return the number of lines of text, or 0 if the internal Layout has not
     * been built.
     */
    public int getLineCount() {
        return mLayout != null ? mLayout.getLineCount() : 0;
    }

    /**
     * Return the baseline for the specified line (0...getLineCount() - 1)
     * If bounds is not null, return the top, left, right, bottom extents
     * of the specified line in it. If the internal Layout has not been built,
     * return 0 and set bounds to (0, 0, 0, 0)
     *
     * @param line   which line to examine (0..getLineCount() - 1)
     * @param bounds Optional. If not null, it returns the extent of the line
     * @return the Y-coordinate of the baseline
     */
    public int getLineBounds(int line, @Nullable Rect bounds) {
        if (mLayout == null) {
            if (bounds != null) {
                bounds.set(0, 0, 0, 0);
            }
            return 0;
        } else {
            int baseline = mLayout.getLineBounds(line, bounds);

            int voffset = getExtendedPaddingTop();
            if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
                voffset += getVerticalOffset(true);
            }
            if (bounds != null) {
                bounds.offset(getCompoundPaddingLeft(), voffset);
            }
            return baseline + voffset;
        }
    }

    @Override
    public int getBaseline() {
        if (mLayout == null) {
            return super.getBaseline();
        }

        return getBaselineOffset() + mLayout.getLineBaseline(0);
    }

    int getBaselineOffset() {
        int vOffset = 0;
        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
            vOffset = getVerticalOffset(true);
        }

        return getExtendedPaddingTop() + vOffset;
    }

    /**
     * @hide
     */
    @VisibleForTesting
    public final void nullLayouts() {
        if (mLayout instanceof BoringLayout && mSavedLayout == null) {
            mSavedLayout = (BoringLayout) mLayout;
        }
        if (mHintLayout instanceof BoringLayout && mSavedHintLayout == null) {
            mSavedHintLayout = (BoringLayout) mHintLayout;
        }

        mLayout = mHintLayout = null;

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
        // Bug fixed by Modern UI
        final boolean testDirChange = !bringIntoView && mSingleLine && mLayout != null;
        bringIntoView |= testDirChange && mLayout.getAlignment() != alignment;
        int oldDir = 0;
        if (testDirChange && !bringIntoView)
            oldDir = mLayout.getParagraphDirection(0);
        boolean shouldEllipsize = mEllipsize != null && mBufferType != BufferType.EDITABLE;
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
    @Nonnull
    private Layout makeSingleLayout(int wantWidth, BoringLayout.Metrics boring, int ellipsisWidth,
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
                    .setEllipsize(mBufferType != BufferType.EDITABLE ? effectiveEllipsize : null)
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

    /**
     * Set whether the TextView includes extra top and bottom padding to make
     * room for accents that go above the normal ascent and descent.
     * The default is true.
     *
     * @see #getIncludeFontPadding()
     */
    public void setIncludeFontPadding(boolean includePad) {
        if (mIncludePad != includePad) {
            mIncludePad = includePad;

            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    /**
     * Gets whether the TextView includes extra top and bottom padding to make
     * room for accents that go above the normal ascent and descent.
     *
     * @see #setIncludeFontPadding(boolean)
     */
    public boolean getIncludeFontPadding() {
        return mIncludePad;
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
                    || (fromexisting && des <= want));

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
     * Check whether a change to the existing text layout requires a
     * new view layout.
     */
    private void checkForResize() {
        boolean sizeChanged = false;

        if (mLayout != null) {
            // Check if our width changed
            ViewGroup.LayoutParams params = getLayoutParams();
            if (params.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                sizeChanged = true;
                invalidate();
            }

            // Check if our height changed
            if (params.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                int desiredHeight = getDesiredHeight();

                if (desiredHeight != this.getHeight()) {
                    sizeChanged = true;
                }
            } else if (params.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                if (mDesiredHeightAtMeasure >= 0) {
                    int desiredHeight = getDesiredHeight();

                    if (desiredHeight != mDesiredHeightAtMeasure) {
                        sizeChanged = true;
                    }
                }
            }
        }

        if (sizeChanged) {
            requestLayout();
            // caller will have already invalidated
        }
    }

    /**
     * Check whether entirely new text requires a new view layout
     * or merely a new text layout.
     */
    private void checkForRelayout() {
        // If we have a fixed width, we can just swap in a new text layout
        // if the text height stays the same or if the view height is fixed.
        final ViewGroup.LayoutParams params = getLayoutParams();
        if ((params.width != ViewGroup.LayoutParams.WRAP_CONTENT
                || (mMaxWidth == mMinWidth))
                && (mHint == null || mHintLayout != null)
                && (getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight() > 0)) {
            // Static width, so try making a new text layout.

            int oldHeight = mLayout.getHeight();
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

            // In a fixed-height view, so use our new text layout.
            if (params.height != ViewGroup.LayoutParams.WRAP_CONTENT
                    && params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                invalidate();
                return;
            }

            // Dynamic height, but height has stayed the same,
            // so use our new text layout.
            if (mLayout.getHeight() == oldHeight
                    && (mHintLayout == null || mHintLayout.getHeight() == oldHeight)) {
                invalidate();
                return;
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
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mDeferScroll >= 0) {
            int curs = mDeferScroll;
            mDeferScroll = -1;
            bringPointIntoView(Math.min(curs, mText.length()));
        }
    }

    private boolean isShowingHint() {
        return TextUtils.isEmpty(mText) && !TextUtils.isEmpty(mHint);
    }

    /**
     * Returns true if anything changed.
     */
    private void bringTextIntoView() {
        Layout layout = isShowingHint() ? mHintLayout : mLayout;
        int line = 0;
        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
            line = layout.getLineCount() - 1;
        }

        Layout.Alignment a = layout.getParagraphAlignment(line);
        int dir = layout.getParagraphDirection(line);
        int hspace = getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();
        int vspace = getHeight() - getExtendedPaddingTop() - getExtendedPaddingBottom();
        int ht = layout.getHeight();

        int scrollx, scrolly;

        // Convert to left, center, or right alignment.
        if (a == Layout.Alignment.ALIGN_NORMAL) {
            a = dir == Layout.DIR_LEFT_TO_RIGHT
                    ? Layout.Alignment.ALIGN_LEFT : Layout.Alignment.ALIGN_RIGHT;
        } else if (a == Layout.Alignment.ALIGN_OPPOSITE) {
            a = dir == Layout.DIR_LEFT_TO_RIGHT
                    ? Layout.Alignment.ALIGN_RIGHT : Layout.Alignment.ALIGN_LEFT;
        }

        if (a == Layout.Alignment.ALIGN_CENTER) {
            /*
             * Keep centered if possible, or, if it is too wide to fit,
             * keep leading edge in view.
             */

            int left = (int) Math.floor(layout.getLineLeft(line));
            int right = (int) Math.ceil(layout.getLineRight(line));

            if (right - left < hspace) {
                scrollx = (right + left) / 2 - hspace / 2;
            } else {
                if (dir < 0) {
                    scrollx = right - hspace;
                } else {
                    scrollx = left;
                }
            }
        } else if (a == Layout.Alignment.ALIGN_RIGHT) {
            int right = (int) Math.ceil(layout.getLineRight(line));
            scrollx = right - hspace;
        } else { // a == Layout.Alignment.ALIGN_LEFT (will also be the default)
            scrollx = (int) Math.floor(layout.getLineLeft(line));
        }

        if (ht < vspace) {
            scrolly = 0;
        } else {
            if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
                scrolly = ht - vspace;
            } else {
                scrolly = 0;
            }
        }

        if (scrollx != mScrollX || scrolly != mScrollY) {
            scrollTo(scrollx, scrolly);
        }
    }

    /**
     * Move the point, specified by the offset, into the view if it is needed.
     * This has to be called after layout. Returns true if anything changed.
     */
    public boolean bringPointIntoView(int offset) {
        if (isLayoutRequested()) {
            mDeferScroll = offset;
            return false;
        }

        Layout layout = isShowingHint() ? mHintLayout : mLayout;

        if (layout == null) {
            return false;
        }

        boolean changed = false;

        int line = layout.getLineForOffset(offset);

        int grav = switch (layout.getParagraphAlignment(line)) {
            case ALIGN_LEFT -> 1;
            case ALIGN_RIGHT -> -1;
            case ALIGN_NORMAL -> layout.getParagraphDirection(line);
            case ALIGN_OPPOSITE -> -layout.getParagraphDirection(line);
            default -> 0;
        };

        // We only want to clamp the cursor to fit within the layout width
        // in left-to-right modes, because in a right to left alignment,
        // we want to scroll to keep the line-right on the screen, as other
        // lines are likely to have text flush with the right margin, which
        // we want to keep visible.
        // A better long-term solution would probably be to measure both
        // the full line and a blank-trimmed version, and, for example, use
        // the latter measurement for centering and right alignment, but for
        // the time being we only implement the cursor clamping in left to
        // right where it is most likely to be annoying.
        final boolean clamped = grav > 0;
        // FIXME: Is it okay to truncate this, or should we round?
        final int x = (int) layout.getPrimaryHorizontal(offset, clamped);
        final int top = layout.getLineTop(line);
        final int bottom = layout.getLineTop(line + 1);

        int left = (int) Math.floor(layout.getLineLeft(line));
        int right = (int) Math.ceil(layout.getLineRight(line));
        int ht = layout.getHeight();

        int hspace = getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();
        int vspace = getHeight() - getExtendedPaddingTop() - getExtendedPaddingBottom();
        if (!mHorizontallyScrolling && right - left > hspace && right > x) {
            // If cursor has been clamped, make sure we don't scroll.
            right = Math.max(x, left + hspace);
        }

        int hslack = (bottom - top) / 2;
        int vslack = hslack;

        if (vslack > vspace / 4) {
            vslack = vspace / 4;
        }
        if (hslack > hspace / 4) {
            hslack = hspace / 4;
        }

        int hs = mScrollX;
        int vs = mScrollY;

        if (top - vs < vslack) {
            vs = top - vslack;
        }
        if (bottom - vs > vspace - vslack) {
            vs = bottom - (vspace - vslack);
        }
        if (ht - vs < vspace) {
            vs = ht - vspace;
        }
        if (vs < 0) {
            vs = 0;
        }

        if (grav != 0) {
            if (x - hs < hslack) {
                hs = x - hslack;
            }
            if (x - hs > hspace - hslack) {
                hs = x - (hspace - hslack);
            }
        }

        if (grav < 0) {
            if (left - hs > 0) {
                hs = left;
            }
            if (right - hs < hspace) {
                hs = right - hspace;
            }
        } else if (grav > 0) {
            if (right - hs < hspace) {
                hs = right - hspace;
            }
            if (left - hs > 0) {
                hs = left;
            }
        } else /* grav == 0 */ {
            if (right - left <= hspace) {
                /*
                 * If the entire text fits, center it exactly.
                 */
                hs = left - (hspace - (right - left)) / 2;
            } else if (x > right - hslack) {
                /*
                 * If we are near the right edge, keep the right edge
                 * at the edge of the view.
                 */
                hs = right - hspace;
            } else if (x < left + hslack) {
                /*
                 * If we are near the left edge, keep the left edge
                 * at the edge of the view.
                 */
                hs = left;
            } else if (left > hs) {
                /*
                 * Is there whitespace visible at the left?  Fix it if so.
                 */
                hs = left;
            } else if (right < hs + hspace) {
                /*
                 * Is there whitespace visible at the right?  Fix it if so.
                 */
                hs = right - hspace;
            } else {
                /*
                 * Otherwise, float as needed.
                 */
                if (x - hs < hslack) {
                    hs = x - hslack;
                }
                if (x - hs > hspace - hslack) {
                    hs = x - (hspace - hslack);
                }
            }
        }

        if (hs != mScrollX || vs != mScrollY) {
            scrollTo(hs, vs);
            changed = true;
        }

        if (isFocused()) {
            // This offsets because getInterestingRect() is in terms of viewport coordinates, but
            // requestRectangleOnScreen() is in terms of content coordinates.

            // The offsets here are to ensure the rectangle we are using is
            // within our view bounds, in case the cursor is on the far left
            // or right.  If it isn't withing the bounds, then this request
            // will be ignored.
            if (mTempRect == null) mTempRect = new Rect();
            mTempRect.set(x - 2, top, x + 2, bottom);
            getInterestingRect(mTempRect, line);
            mTempRect.offset(mScrollX, mScrollY);

            if (requestRectangleOnScreen(mTempRect)) {
                changed = true;
            }
        }

        return changed;
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

    private void getInterestingRect(@Nonnull Rect r, int line) {
        convertFromViewportToContentCoordinates(r);

        // Rectangle can can be expanded on first and last line to take
        // padding into account.
        if (line == 0) r.top -= getExtendedPaddingTop();
        if (line == mLayout.getLineCount() - 1) r.bottom += getExtendedPaddingBottom();
    }

    private void convertFromViewportToContentCoordinates(@Nonnull Rect r) {
        final int horizontalOffset = viewportToContentHorizontalOffset();
        r.left += horizontalOffset;
        r.right += horizontalOffset;

        final int verticalOffset = viewportToContentVerticalOffset();
        r.top += verticalOffset;
        r.bottom += verticalOffset;
    }

    int viewportToContentHorizontalOffset() {
        return getCompoundPaddingLeft() - mScrollX;
    }

    int viewportToContentVerticalOffset() {
        int offset = getExtendedPaddingTop() - mScrollY;
        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
            offset += getVerticalOffset(false);
        }
        return offset;
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
     * Sets the properties of this field (lines, horizontally scrolling,
     * transformation method) to be for a single-line input.
     */
    public void setSingleLine() {
        setSingleLine(true);
    }

    /**
     * If true, sets the properties of this field (number of lines, horizontally scrolling,
     * transformation method) to be for a single-line input; if false, restores these to the default
     * conditions.
     * <p>
     * Note that the default conditions are not necessarily those that were in effect prior this
     * method, and you may want to reset these properties to your custom values.
     * <p>
     * Note that due to performance reasons, by setting single line for the EditText, the maximum
     * text length is set to 5000 if no other character limitation are applied.
     */
    public void setSingleLine(boolean singleLine) {
        if (mSingleLine == singleLine) return;
        applySingleLine(singleLine, true, true, true);
    }

    private void applySingleLine(boolean singleLine, boolean applyTransformation,
                                 boolean changeMaxLines, boolean changeMaxLength) {
        mSingleLine = singleLine;

        if (singleLine) {
            setLines(1);
            setHorizontallyScrolling(true);
            if (applyTransformation) {
                setTransformationMethod(SingleLineTransformationMethod.getInstance());
            }

            if (!changeMaxLength) return;

            // Single line length filter is only applicable editable text.
            if (mBufferType != BufferType.EDITABLE) return;

            final InputFilter[] prevFilters = getFilters();
            for (InputFilter filter : getFilters()) {
                // We don't add LengthFilter if already there.
                if (filter instanceof InputFilter.LengthFilter) return;
            }

            if (mSingleLineLengthFilter == null) {
                mSingleLineLengthFilter = new InputFilter.LengthFilter(
                        MAX_LENGTH_FOR_SINGLE_LINE_EDIT_TEXT);
            }

            final InputFilter[] newFilters = new InputFilter[prevFilters.length + 1];
            System.arraycopy(prevFilters, 0, newFilters, 0, prevFilters.length);
            newFilters[prevFilters.length] = mSingleLineLengthFilter;

            setFilters(newFilters);

            // Since filter doesn't apply to existing text, trigger filter by setting text.
            setText(getText());
        } else {
            if (changeMaxLines) {
                setMaxLines(Integer.MAX_VALUE);
            }
            setHorizontallyScrolling(false);
            if (applyTransformation) {
                setTransformationMethod(null);
            }

            if (!changeMaxLength) return;

            // Single line length filter is only applicable editable text.
            if (mBufferType != BufferType.EDITABLE) return;

            final InputFilter[] prevFilters = getFilters();
            if (prevFilters.length == 0) return;

            // Short Circuit: if mSingleLineLengthFilter is not allocated, nobody sets automated
            // single line char limit filter.
            if (mSingleLineLengthFilter == null) return;

            // If we need to remove mSingleLineLengthFilter, we need to allocate another array.
            // Since filter list is expected to be small and want to avoid unnecessary array
            // allocation, check if there is mSingleLengthFilter first.
            int targetIndex = -1;
            for (int i = 0; i < prevFilters.length; ++i) {
                if (prevFilters[i] == mSingleLineLengthFilter) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex == -1) return;  // not found. Do nothing.

            if (prevFilters.length == 1) {
                setFilters(NO_FILTERS);
                return;
            }

            // Create new array which doesn't include mSingleLengthFilter.
            final InputFilter[] newFilters = new InputFilter[prevFilters.length - 1];
            System.arraycopy(prevFilters, 0, newFilters, 0, targetIndex);
            System.arraycopy(
                    prevFilters,
                    targetIndex + 1,
                    newFilters,
                    targetIndex,
                    prevFilters.length - targetIndex - 1);
            setFilters(newFilters);
            mSingleLineLengthFilter = null;
        }
    }

    /**
     * Causes words in the text that are longer than the view's width
     * to be ellipsized instead of broken in the middle.  You may also
     * want to {@link #setSingleLine} or {@link #setHorizontallyScrolling}
     * to constrain the text to a single line.  Use <code>null</code>
     * to turn off ellipsizing.
     * <p>
     * If {@link #setMaxLines} has been used to set two or more lines,
     * only {@link TextUtils.TruncateAt#END} is supported
     * (other ellipsizing types will not do anything).
     */
    public void setEllipsize(@Nullable TextUtils.TruncateAt where) {
        // TruncateAt is an enum. != comparison is ok between these singleton objects.
        if (mEllipsize != where) {
            mEllipsize = where;

            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    /**
     * Returns where, if anywhere, words that are longer than the view
     * is wide should be ellipsized.
     */
    @Nullable
    public TextUtils.TruncateAt getEllipsize() {
        return mEllipsize;
    }

    /**
     * Set the TextView so that when it takes focus, all the text is
     * selected.
     */
    public void setSelectAllOnFocus(boolean selectAllOnFocus) {
        createEditorIfNeeded();
        mEditor.mSelectAllOnFocus = selectAllOnFocus;

        if (selectAllOnFocus && !(mText instanceof Spannable)) {
            setText(mText, BufferType.SPANNABLE);
        }
    }

    /**
     * Set whether the cursor is visible. The default is true. Note that this property only
     * makes sense for editable TextView. If IME is consuming the input, the cursor will always be
     * invisible, visibility will be updated as the last state when IME does not consume
     * the input anymore.
     *
     * @see #isCursorVisible()
     */
    public void setCursorVisible(boolean visible) {
        if (visible && mEditor == null) return; // visible is the default value with no edit data
        createEditorIfNeeded();
        if (mEditor.mCursorVisible != visible) {
            mEditor.mCursorVisible = visible;
            invalidate();

            mEditor.makeBlink();
        }
    }

    /**
     * @return whether the cursor is visible (assuming this TextView is editable). This
     * method may return {@code false} when the IME is consuming the input even if the
     * {@code mEditor.mCursorVisible} attribute is {@code true} or {@code #setCursorVisible(true)}
     * is called.
     * @see #setCursorVisible(boolean)
     */
    public boolean isCursorVisible() {
        // true is the default value
        return mEditor == null || mEditor.mCursorVisible;
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
     * Adds a TextWatcher to the list of those whose methods are called
     * whenever this TextView's text changes.
     * <p>
     * In 1.0, the {@link TextWatcher#afterTextChanged} method was erroneously
     * not called after {@link #setText} calls.  Now, doing {@link #setText}
     * if there are any text changed listeners forces the buffer type to
     * Editable if it would not otherwise be and does call this method.
     *
     * @param watcher the watcher to add
     */
    public void addTextChangedListener(@Nonnull TextWatcher watcher) {
        if (mListeners == null) {
            mListeners = new ArrayList<>();
        }
        mListeners.add(watcher);
    }

    /**
     * Removes the specified TextWatcher from the list of those whose
     * methods are called whenever this TextView's text changes.
     *
     * @param watcher the watcher to remove
     */
    public void removeTextChangedListener(@Nonnull TextWatcher watcher) {
        if (mListeners != null) {
            mListeners.remove(watcher);
        }
    }

    private void sendBeforeTextChanged(CharSequence text, int start, int before, int after) {
        if (mListeners != null && mListeners.size() > 0) {
            for (TextWatcher textWatcher : mListeners) {
                textWatcher.beforeTextChanged(text, start, before, after);
            }
        }
    }

    /**
     * Not private so it can be called from an inner class without going
     * through a thunk.
     */
    private void sendOnTextChanged(CharSequence text, int start, int before, int after) {
        if (mListeners != null && mListeners.size() > 0) {
            for (TextWatcher textWatcher : mListeners) {
                textWatcher.onTextChanged(text, start, before, after);
            }
        }

        if (mEditor != null) {
            mEditor.sendOnTextChanged(start, before, after);
        }
    }

    /**
     * Not private so it can be called from an inner class without going
     * through a thunk.
     */
    private void sendAfterTextChanged(Editable text) {
        if (mListeners != null && mListeners.size() > 0) {
            for (TextWatcher textWatcher : mListeners) {
                textWatcher.afterTextChanged(text);
            }
        }
    }

    /**
     * Not private so it can be called from an inner class without going
     * through a thunk.
     */
    void handleTextChanged(CharSequence buffer, int start, int before, int after) {
        invalidate();
        int curs = getSelectionStart();

        if (curs >= 0 || (mGravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
            registerForPreDraw();
        }

        checkForResize();

        if (curs >= 0) {
            mHighlightPathBogus = true;
            if (mEditor != null) mEditor.makeBlink();
            bringPointIntoView(curs);
        }

        sendOnTextChanged(buffer, start, before, after);
        onTextChanged(buffer, start, before, after);
    }

    /**
     * Not private so it can be called from an inner class without going
     * through a thunk.
     */
    private void spanChange(Spanned buf, Object what, int oldStart, int newStart, int oldEnd, int newEnd) {
        // XXX Make the start and end move together if this ends up
        // spending too much time invalidating.

        boolean selChanged = false;

        if (what == Selection.SELECTION_END) {
            selChanged = true;

            if (oldStart >= 0 || newStart >= 0) {
                invalidate();
                checkForResize();
                registerForPreDraw();
                if (mEditor != null) {
                    mEditor.makeBlink();
                }
            }
        }

        if (what == Selection.SELECTION_START) {
            selChanged = true;

            if (oldStart >= 0 || newStart >= 0) {
                invalidate();
            }
        }

        if (selChanged) {
            mHighlightPathBogus = true;
            if (mEditor != null && !isFocused()) {
                mEditor.mSelectionMoved = true;
            }
        }

        if (what instanceof UpdateAppearance || what instanceof ParagraphStyle
                || what instanceof CharacterStyle) {
            invalidate();
            mHighlightPathBogus = true;
            checkForResize();
        }

        if (TextKeyListener.isMetaTracker(what)) {
            mHighlightPathBogus = true;

            if (Selection.getSelectionStart(buf) >= 0 && getSelectionEnd() >= 0) {
                invalidate();
            }
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction) {
        if (mEditor != null) {
            mEditor.onFocusChanged(focused, direction);
        }

        if (focused) {
            if (mSpannable != null) {
                TextKeyListener.resetMetaState(mSpannable);
            }
        }

        if (mTransformation != null) {
            mTransformation.onFocusChanged(this, mText, focused, direction);
        }

        super.onFocusChanged(focused, direction);
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
    public boolean onGenericMotionEvent(@Nonnull MotionEvent event) {
        if (mMovement != null && mSpannable != null && mLayout != null) {
            if (mMovement.onGenericMotionEvent(this, mSpannable, event)) {
                return true;
            }
        }
        return super.onGenericMotionEvent(event);
    }

    /**
     * @return True if this TextView contains a text that can be edited, or if this is
     * a selectable TextView.
     */
    boolean isTextEditable() {
        return mText instanceof Editable && mEditor != null && isEnabled();
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
    public void cancelLongPress() {
        super.cancelLongPress();
        if (mEditor != null) mEditor.mIgnoreActionUpEvent = true;
    }

    @Override
    protected int computeHorizontalScrollRange() {
        if (mLayout != null) {
            return mSingleLine && (mGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.LEFT
                    ? (int) mLayout.getLineWidth(0) : mLayout.getWidth();
        }

        return super.computeHorizontalScrollRange();
    }

    @Override
    protected int computeVerticalScrollRange() {
        if (mLayout != null) {
            return mLayout.getHeight();
        }
        return super.computeVerticalScrollRange();
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return getHeight() - getCompoundPaddingTop() - getCompoundPaddingBottom();
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
                if (mSingleLine && mBufferType == BufferType.EDITABLE && event.hasNoModifiers()) {
                    if (hasOnClickListeners()) {
                        return super.onKeyDown(keyCode, event);
                    }
                    return true;
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
            // allow users to type line feed characters
            if ((keyCode == KeyEvent.KEY_ENTER || keyCode == KeyEvent.KEY_KP_ENTER)
                    && !mSingleLine && (event.hasNoModifiers() || event.isShiftPressed())) {
                final int selStart = getSelectionStart();
                final int selEnd = getSelectionEnd();
                ((Editable) mText).replace(Math.min(selStart, selEnd), Math.max(selStart, selEnd), "\n");
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
     * Test based on the <i>intrinsic</i> characteristics of the TextView.
     * The text must be spannable and the movement method must allow for arbitrary selection.
     */
    boolean textCanBeSelected() {
        if (mMovement == null || !mMovement.canSelectArbitrarily()) return false;
        return isTextEditable()
                || (isTextSelectable() && mSpannable != null && isEnabled());
    }

    /**
     * This method is used by the ArrowKeyMovementMethod to jump from one word to the other.
     * Made available to achieve a consistent behavior.
     *
     * @hide
     */
    public WordIterator getWordIterator() {
        if (mEditor != null) {
            return mEditor.getWordIterator();
        } else {
            return null;
        }
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
                ArchCore.recordMainCall(() -> Clipboard.setText(cut));
                getEditableText().delete(min, max);
                return true;
            }
            case ID_COPY -> {
                CharSequence copy = mTransformed.subSequence(min, max);
                ArchCore.recordMainCall(() -> Clipboard.setText(copy));
                return true;
            }
            case ID_PASTE -> {
                int aMax = max;
                int aMin = min;
                ArchCore.recordMainCall(() -> {
                    String replacement = Clipboard.getText();
                    if (replacement != null) {
                        post(() -> {
                            Editable editable = getEditableText();
                            Selection.setSelection(editable, aMax);
                            editable.replace(aMin, aMax, replacement);
                        });
                    }
                });
                return true;
            }
            case ID_SELECT_ALL -> {
                selectAllText();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        final TextDirectionHeuristic newTextDir = getTextDirectionHeuristic();
        if (mTextDir != newTextDir) {
            mTextDir = newTextDir;
            if (mLayout != null) {
                checkForRelayout();
            }
        }
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
    @Override
    public void onResolveDrawables(int layoutDirection) {
        // No need to resolve twice
        if (mLastLayoutDirection == layoutDirection) {
            return;
        }
        mLastLayoutDirection = layoutDirection;

        // Resolve drawables
        if (mDrawables != null) {
            if (mDrawables.resolveWithLayoutDirection(layoutDirection)) {
                prepareDrawableForDisplay(mDrawables.mShowing[Drawables.LEFT]);
                prepareDrawableForDisplay(mDrawables.mShowing[Drawables.RIGHT]);
                //applyCompoundDrawableTint();
            }
        }
    }

    /**
     * Prepares a drawable for display by propagating layout direction and
     * drawable state.
     *
     * @param dr the drawable to prepare
     */
    private void prepareDrawableForDisplay(@Nullable Drawable dr) {
        if (dr == null) {
            return;
        }

        dr.setLayoutDirection(getLayoutDirection());

        /*if (dr.isStateful()) {
            dr.setState(getDrawableState());
            dr.jumpToCurrentState();
        }*/
    }

    @Override
    public void onHoverChanged(boolean hovered) {
        if (hovered && (isTextSelectable() || isTextEditable())) {
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
            // start or stop the cursor blinking as appropriate
            mEditor.makeBlink();
        }
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
                && mLayout != null
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

        Drawable mDrawableStart, mDrawableEnd, mDrawableTemp;
        Drawable mDrawableLeftInitial, mDrawableRightInitial;

        boolean mOverride;

        int mDrawableSizeTop, mDrawableSizeBottom, mDrawableSizeLeft, mDrawableSizeRight,
                mDrawableSizeStart, mDrawableSizeEnd, mDrawableSizeTemp;

        int mDrawableWidthTop, mDrawableWidthBottom, mDrawableHeightLeft, mDrawableHeightRight,
                mDrawableHeightStart, mDrawableHeightEnd, mDrawableHeightTemp;

        int mDrawablePadding;

        int mDrawableSaved = DRAWABLE_NONE;

        public Drawables() {
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

            if (!ModernUI.getInstance().hasRtlSupport()) {
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

        private void applyErrorDrawableIfNeeded(int layoutDirection) {
            // first restore the initial state if needed
            switch (mDrawableSaved) {
                case DRAWABLE_LEFT -> {
                    mShowing[Drawables.LEFT] = mDrawableTemp;
                    mDrawableSizeLeft = mDrawableSizeTemp;
                    mDrawableHeightLeft = mDrawableHeightTemp;
                }
                case DRAWABLE_RIGHT -> {
                    mShowing[Drawables.RIGHT] = mDrawableTemp;
                    mDrawableSizeRight = mDrawableSizeTemp;
                    mDrawableHeightRight = mDrawableHeightTemp;
                }
            }
        }
    }

    private class ChangeWatcher implements TextWatcher, SpanWatcher {

        @Override
        public void onSpanAdded(Spannable text, Object what, int start, int end) {
            spanChange(text, what, -1, start, -1, end);
        }

        @Override
        public void onSpanRemoved(Spannable text, Object what, int start, int end) {
            spanChange(text, what, start, -1, end, -1);
        }

        @Override
        public void onSpanChanged(Spannable text, Object what, int ost, int oen, int nst, int nen) {
            spanChange(text, what, ost, nst, oen, nen);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            sendBeforeTextChanged(s, start, count, after);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            handleTextChanged(s, start, before, count);
        }

        @Override
        public void afterTextChanged(Editable s) {
            sendAfterTextChanged(s);
        }
    }
}
