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
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.math.Rect;
import icyllis.modernui.text.*;
import icyllis.modernui.text.method.KeyListener;
import icyllis.modernui.text.method.MovementMethod;
import icyllis.modernui.text.method.TransformationMethod;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.MeasureSpec;
import icyllis.modernui.view.View;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

/**
 * A user interface element that displays text to the user and provides user-editable text.
 */
//TODO
public class TextView extends View {

    static final Marker MARKER = MarkerManager.getMarker("TextView");
    static final boolean DEBUG_EXTRACT = false;
    static final boolean DEBUG_CURSOR = false;

    @VisibleForTesting
    public static final BoringLayout.Metrics UNKNOWN_BORING = new BoringLayout.Metrics();

    // Width modes.
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

    /**
     * Return code of {@link #doKeyDown}.
     */
    private static final int KEY_EVENT_NOT_HANDLED = 0;
    private static final int KEY_EVENT_HANDLED = -1;
    private static final int KEY_DOWN_HANDLED_BY_KEY_LISTENER = 1;
    private static final int KEY_DOWN_HANDLED_BY_MOVEMENT_METHOD = 2;

    private static final int FLOATING_TOOLBAR_SELECT_ALL_REFRESH_DELAY = 500;

    // System-wide time for last cut, copy or text changed action.
    static long sLastCutCopyOrTextChangedTime;

    /**
     * {@link #setTextColor(int)} or {@link #getCurrentTextColor()} should be used instead.
     */
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

    Drawables mDrawables;

    // Do not update following mText/mSpannable/mPrecomputed except for setTextInternal()
    private CharSequence mText = "";
    @Nullable
    private Spannable mSpannable;
    @Nullable
    private PrecomputedText mPrecomputed;

    private CharSequence mTransformed;
    private BufferType mBufferType = BufferType.NORMAL;

    /**
     * Type of the text buffer that defines the characteristics of the text such as static,
     * styleable, or editable.
     */
    public enum BufferType {
        NORMAL, SPANNABLE, EDITABLE
    }

    @Nullable
    private CharSequence mHint;
    @Nullable
    private Layout mHintLayout;

    //TODO give it a default movement method
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
    private boolean mLocalesChanged = false;
    private int mTextSizeUnit = -1;

    private Typeface mOriginalTypeface;

    // True if setKeyListener() has been explicitly called
    private boolean mListenerChanged = false;
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
    private int mMaxWidthMode = PIXELS;
    private int mMinWidth = 0;
    private int mMinWidthMode = PIXELS;

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

    private InputFilter[] mFilters = NO_FILTERS;

    private boolean mHighlightPathBogus = true;

    /**
     * Created on demand when one of the Editor fields is used.
     * See {@link #createEditorIfNeeded()}.
     */
    private Editor mEditor;

    public TextView() {
    }

    // Update member variables
    private void setTextInternal(@Nonnull CharSequence text) {
        mText = text;
        mSpannable = (text instanceof Spannable) ? (Spannable) text : null;
        mPrecomputed = (text instanceof PrecomputedText) ? (PrecomputedText) text : null;
    }

    public void setText(@Nonnull CharSequence text) {
        setTextInternal(text);
        if (mTransformation == null) {
            mTransformed = text;
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
     * However, this is like this for legacy reasons and we cannot break existing apps. This method
     * is useful since it matches what the user can see (obfuscated text or not).
     *
     * @return true if the current transformation method is of the password type.
     */
    boolean hasPasswordTransformationMethod() {
        //TODO
        return false;
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
        layout.draw(canvas);
        canvas.restore();
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
     * Return the text that TextView is displaying. If {@link #setText(CharSequence)} was called
     * with an argument of {@link TextView.BufferType#SPANNABLE BufferType.SPANNABLE}
     * or {@link TextView.BufferType#EDITABLE BufferType.EDITABLE}, you can cast
     * the return value from this method to Spannable or Editable, respectively.
     *
     * <p>The content of the return value should not be modified. If you want a modifiable one, you
     * should make your own copy first.</p>
     *
     * @return The text displayed by the text view.
     */
    public CharSequence getText() {
        return mText;
    }

    /**
     * Returns the length, in characters, of the text managed by this TextView
     *
     * @return The length of the text managed by the TextView in characters.
     */
    public int length() {
        return mText.length();
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
     * @hide
     */
    public CharSequence getTransformed() {
        return mTransformed;
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

    private void registerForPreDraw() {
        if (!mPreDrawRegistered) {
            //getViewTreeObserver().addOnPreDrawListener(this);
            mPreDrawRegistered = true;
        }
    }

    /**
     * Returns the state of the {@code textIsSelectable} flag (See
     * {@link #setTextIsSelectable setTextIsSelectable()}). Although you have to set this flag
     * to allow users to select and copy text in a non-editable TextView, the content of an
     * {@link EditText} can always be selected, independently of the value of this flag.
     * <p>
     *
     * @return True if the text displayed in this TextView can be selected by the user.
     */
    public boolean isTextSelectable() {
        return mEditor != null && mEditor.mTextIsSelectable;
    }

    /**
     * Gets the current {@link KeyListener} for the TextView.
     * This will frequently be null for non-EditText TextViews.
     *
     * @return the current key listener for this TextView.
     */
    @Nullable
    public final KeyListener getKeyListener() {
        return mEditor == null ? null : mEditor.mKeyListener;
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
        boolean shouldEllipsize = mEllipsize != null && getKeyListener() == null;
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

        // CursorControllers need a non-null mLayout
        if (mEditor != null)
            mEditor.prepareCursorControllers();
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
            assert mText != null;
            final DynamicLayout.Builder builder = DynamicLayout.builder(mText, mTextPaint,
                            wantWidth)
                    .setDisplayText(mTransformed)
                    .setAlignment(alignment)
                    .setTextDirection(mTextDir)
                    .setIncludePad(mIncludePad)
                    .setFallbackLineSpacing(mUseFallbackLineSpacing)
                    .setEllipsize(getKeyListener() == null ? effectiveEllipsize : null)
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
