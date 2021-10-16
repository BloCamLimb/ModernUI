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
import icyllis.modernui.text.*;
import icyllis.modernui.text.method.MovementMethod;
import icyllis.modernui.text.method.TransformationMethod;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.MeasureSpec;
import icyllis.modernui.view.View;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

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

    private static final BoringLayout.Metrics UNKNOWN_BORING = new BoringLayout.Metrics();

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

    // System wide time for last cut, copy or text changed action.
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

    // Do not update following mText/mSpannable/mPrecomputed except for setTextInternal()
    @Nullable
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

    /**
     * Created on demand when one of the Editor fields is used.
     * See {@link #createEditorIfNeeded()}.
     */
    private Editor mEditor;

    public TextView() {
    }

    // Update member variables
    private void setTextInternal(@Nullable CharSequence text) {
        mText = text;
        mSpannable = (text instanceof Spannable) ? (Spannable) text : null;
        mPrecomputed = (text instanceof PrecomputedText) ? (PrecomputedText) text : null;
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
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

        // Always need to resolve layout direction first
        final boolean defaultIsRtl = isLayoutRtl();

        // Now, we can select the heuristic
        return switch (getTextDirection()) {
            case TEXT_DIRECTION_ANY_RTL -> TextDirectionHeuristics.ANYRTL_LTR;
            case TEXT_DIRECTION_LTR -> TextDirectionHeuristics.LTR;
            case TEXT_DIRECTION_RTL -> TextDirectionHeuristics.RTL;
            case TEXT_DIRECTION_LOCALE -> TextDirectionHeuristics.LOCALE;
            case TEXT_DIRECTION_FIRST_STRONG_LTR -> TextDirectionHeuristics.FIRSTSTRONG_LTR;
            case TEXT_DIRECTION_FIRST_STRONG_RTL -> TextDirectionHeuristics.FIRSTSTRONG_RTL;
            default -> (defaultIsRtl ? TextDirectionHeuristics.FIRSTSTRONG_RTL :
                    TextDirectionHeuristics.FIRSTSTRONG_LTR);
        };
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
