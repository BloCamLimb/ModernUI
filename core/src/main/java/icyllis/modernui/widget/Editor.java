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

import icyllis.modernui.core.Core;
import icyllis.modernui.text.Selection;
import icyllis.modernui.text.Spannable;
import icyllis.modernui.text.method.MovementMethod;
import icyllis.modernui.text.method.WordIterator;
import icyllis.modernui.view.ContextMenu;
import icyllis.modernui.view.Menu;
import icyllis.modernui.view.MenuItem;
import icyllis.modernui.view.MotionEvent;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nonnull;

/**
 * Helper class used by TextView to handle editable text views.
 */
public class Editor {

    private static final Marker MARKER = MarkerManager.getMarker("Editor");

    static final int BLINK = 500;

    // Ordering constants used to place the Action Mode or context menu items in their menu.
    private static final int MENU_ITEM_ORDER_ASSIST = 0;
    private static final int MENU_ITEM_ORDER_UNDO = 2;
    private static final int MENU_ITEM_ORDER_REDO = 3;
    private static final int MENU_ITEM_ORDER_CUT = 4;
    private static final int MENU_ITEM_ORDER_COPY = 5;
    private static final int MENU_ITEM_ORDER_PASTE = 6;
    private static final int MENU_ITEM_ORDER_SHARE = 7;
    private static final int MENU_ITEM_ORDER_SELECT_ALL = 8;
    private static final int MENU_ITEM_ORDER_REPLACE = 9;
    private static final int MENU_ITEM_ORDER_AUTOFILL = 10;
    private static final int MENU_ITEM_ORDER_PASTE_AS_PLAIN_TEXT = 11;
    private static final int MENU_ITEM_ORDER_SECONDARY_ASSIST_ACTIONS_START = 50;
    private static final int MENU_ITEM_ORDER_PROCESS_TEXT_INTENT_ACTIONS_START = 100;

    private final TextView mTextView;

    boolean mSelectionMoved;
    boolean mTouchFocusSelected;

    boolean mDiscardNextActionUp;
    boolean mIgnoreActionUpEvent;

    private long mShowCursor;
    private Blink mBlink;

    boolean mCursorVisible = true;
    boolean mSelectAllOnFocus;
    boolean mTextIsSelectable;

    /*boolean mInBatchEditControllers;
    private boolean mPreserveSelection;
    private boolean mRestartActionModeOnNextRefresh;
    private boolean mRequestingLinkActionMode;*/

    boolean mIsBeingLongClicked;

    private float mContextMenuAnchorX, mContextMenuAnchorY;

    // The button state as of the last time #onTouchEvent is called.
    private int mLastButtonState;

    private WordIterator mWordIterator;

    private final MenuItem.OnMenuItemClickListener mOnContextMenuItemClickListener;
    private int mLastTapPosition = -1;

    Editor(TextView textView) {
        mTextView = textView;
        mOnContextMenuItemClickListener = item -> mTextView.onTextContextMenuItem(item.getItemId());
    }

    void onAttachedToWindow() {
        resumeBlink();
    }

    void onDetachedFromWindow() {
        suspendBlink();
    }

    private boolean isCursorVisible() {
        // The default value is true, even when there is no associated Editor
        return mCursorVisible && mTextView.isTextEditable();
    }

    boolean shouldRenderCursor() {
        if (isCursorVisible()) {
            final long showCursorDelta = Core.timeMillis() - mShowCursor;
            return showCursorDelta % (2 * BLINK) < BLINK;
        }
        return false;
    }

    private void suspendBlink() {
        if (mBlink != null) {
            mBlink.cancel();
        }
    }

    private void resumeBlink() {
        if (mBlink != null) {
            mBlink.reset();
            makeBlink();
        }
    }

    WordIterator getWordIterator() {
        if (mWordIterator == null) {
            mWordIterator = new WordIterator(mTextView.getTextLocale());
        }
        return mWordIterator;
    }

    void onFocusChanged(boolean focused, int direction) {
        mShowCursor = Core.timeMillis();

        if (focused) {
            int selStart = mTextView.getSelectionStart();
            int selEnd = mTextView.getSelectionEnd();

            // SelectAllOnFocus fields are highlighted and not selected. Do not start text selection
            // mode for these, unless there was a specific selection already started.
            final boolean isFocusHighlighted = mSelectAllOnFocus && selStart == 0
                    && selEnd == mTextView.getText().length();

            if (mLastTapPosition >= 0) {
                Selection.setSelection((Spannable) mTextView.getText(), mLastTapPosition);
            } else {
                // Note this may have to be moved out of the Editor class
                MovementMethod movement = mTextView.getMovementMethod();
                if (movement != null) {
                    movement.onTakeFocus(mTextView, (Spannable) mTextView.getText(), direction);
                }

                // The DecorView does not have focus when the 'Done' ExtractEditText button is
                // pressed. Since it is the ViewAncestor's mView, it requests focus before
                // ExtractEditText clears focus, which gives focus to the ExtractEditText.
                // This special case ensure that we keep current selection in that case.
                // It would be better to know why the DecorView does not have focus at that time.
                if (mSelectionMoved && selStart >= 0 && selEnd >= 0) {
                    /*
                     * Someone intentionally set the selection, so let them
                     * do whatever it is that they wanted to do instead of
                     * the default on-focus behavior.  We reset the selection
                     * here instead of just skipping the onTakeFocus() call
                     * because some movement methods do something other than
                     * just setting the selection in theirs and we still
                     * need to go through that path.
                     */
                    Selection.setSelection((Spannable) mTextView.getText(), selStart, selEnd);
                }
            }

            if (mSelectAllOnFocus) {
                mTextView.selectAllText();
            }

            //mTouchFocusSelected = true;

            makeBlink();
        } else {
            mLastTapPosition = -1;
        }
    }

    /**
     * Handles touch events on an editable text view, implementing cursor movement, selection, etc.
     */
    void onTouchEvent(@Nonnull MotionEvent event) {
        final int action = event.getAction();
        final boolean filterOutEvent;
        if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP)
                && ((mLastButtonState ^ event.getButtonState()) & MotionEvent.BUTTON_PRIMARY) == 0) {
            filterOutEvent = true;
        } else {
            filterOutEvent = action == MotionEvent.ACTION_MOVE
                    && !event.isButtonPressed(MotionEvent.BUTTON_PRIMARY);
        }
        mLastButtonState = event.getButtonState();
        if (filterOutEvent) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                mDiscardNextActionUp = true;
            }
            return;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mLastTapPosition = mTextView.getOffsetForPosition(event.getX(), event.getY());
            // Reset this state; it will be re-set if super.onTouchEvent
            // causes focus to move to the view.
            mTouchFocusSelected = false;
            mIgnoreActionUpEvent = false;
        }
    }

    void onTouchUpEvent(@Nonnull MotionEvent event) {
        boolean selectAllGotFocus = mSelectAllOnFocus && mTextView.didTouchFocusSelect();
        CharSequence text = mTextView.getText();
        if (!selectAllGotFocus) {
            // Move cursor
            final int offset = mTextView.getOffsetForPosition(event.getX(), event.getY());
            Selection.setSelection((Spannable) text, offset);
        }
    }

    void sendOnTextChanged(int start, int before, int after) {
    }

    void addSpanWatchers(Spannable sp) {
    }

    /**
     * @return True when the TextView isFocused and has a valid zero-length selection (cursor).
     */
    private boolean shouldBlink() {
        if (!isCursorVisible() || !mTextView.isFocused()) return false;

        final int start = mTextView.getSelectionStart();
        if (start < 0) return false;

        final int end = mTextView.getSelectionEnd();
        if (end < 0) return false;

        return start == end;
    }

    void makeBlink() {
        if (shouldBlink()) {
            mShowCursor = Core.timeMillis();
            if (mBlink == null) {
                mBlink = new Blink();
            }
            mTextView.removeCallbacks(mBlink);
            mTextView.postDelayed(mBlink, BLINK);
        } else {
            if (mBlink != null) {
                mTextView.removeCallbacks(mBlink);
            }
        }
    }

    void setContextMenuAnchor(float x, float y) {
        mContextMenuAnchorX = x;
        mContextMenuAnchorY = y;
    }

    void onCreateContextMenu(ContextMenu menu) {
        if (mIsBeingLongClicked || Float.isNaN(mContextMenuAnchorX)
                || Float.isNaN(mContextMenuAnchorY)) {
            return;
        }
        final int offset = mTextView.getOffsetForPosition(mContextMenuAnchorX, mContextMenuAnchorY);
        if (offset == -1) {
            return;
        }
        // Fixed by Modern UI
        final int min = Math.min(mTextView.getSelectionStart(), mTextView.getSelectionEnd());
        final int max = Math.max(mTextView.getSelectionStart(), mTextView.getSelectionEnd());
        final boolean isOnSelection = mTextView.hasSelection()
                && offset >= min
                && offset <= max;
        if (!isOnSelection) {
            // Right clicked position is not on the selection. Remove the selection and move the
            // cursor to the right clicked position.
            Selection.setSelection((Spannable) mTextView.getText(), offset);
        }

        /*menu.add(Menu.NONE, TextView.ID_UNDO, MENU_ITEM_ORDER_UNDO,
                        com.android.internal.R.string.undo)
                .setAlphabeticShortcut('z')
                .setOnMenuItemClickListener(mOnContextMenuItemClickListener)
                .setEnabled(mTextView.canUndo());
        menu.add(Menu.NONE, TextView.ID_REDO, MENU_ITEM_ORDER_REDO,
                        com.android.internal.R.string.redo)
                .setOnMenuItemClickListener(mOnContextMenuItemClickListener)
                .setEnabled(mTextView.canRedo());*/

        menu.add(Menu.NONE, TextView.ID_CUT, MENU_ITEM_ORDER_CUT, "Cut")
                .setAlphabeticShortcut('x')
                .setOnMenuItemClickListener(mOnContextMenuItemClickListener)
                .setEnabled(mTextView.canCut());
        menu.add(Menu.NONE, TextView.ID_COPY, MENU_ITEM_ORDER_COPY, "Copy")
                .setAlphabeticShortcut('c')
                .setOnMenuItemClickListener(mOnContextMenuItemClickListener)
                .setEnabled(mTextView.canCopy());
        menu.add(Menu.NONE, TextView.ID_PASTE, MENU_ITEM_ORDER_PASTE, "Paste")
                .setAlphabeticShortcut('v')
                .setEnabled(mTextView.canPaste())
                .setOnMenuItemClickListener(mOnContextMenuItemClickListener);
        /*menu.add(Menu.NONE, TextView.ID_PASTE_AS_PLAIN_TEXT, MENU_ITEM_ORDER_PASTE_AS_PLAIN_TEXT,
                        com.android.internal.R.string.paste_as_plain_text)
                .setEnabled(mTextView.canPasteAsPlainText())
                .setOnMenuItemClickListener(mOnContextMenuItemClickListener);
        menu.add(Menu.NONE, TextView.ID_SHARE, MENU_ITEM_ORDER_SHARE,
                        com.android.internal.R.string.share)
                .setEnabled(mTextView.canShare())
                .setOnMenuItemClickListener(mOnContextMenuItemClickListener);*/
        menu.add(Menu.NONE, TextView.ID_SELECT_ALL, MENU_ITEM_ORDER_SELECT_ALL, "Select all")
                .setAlphabeticShortcut('a')
                .setEnabled(mTextView.canSelectAllText())
                .setOnMenuItemClickListener(mOnContextMenuItemClickListener);
        /*menu.add(Menu.NONE, TextView.ID_AUTOFILL, MENU_ITEM_ORDER_AUTOFILL,
                        android.R.string.autofill)
                .setEnabled(mTextView.canRequestAutofill())
                .setOnMenuItemClickListener(mOnContextMenuItemClickListener);*/
        menu.setQwertyMode(true);
    }

    private class Blink implements Runnable {

        private boolean mCancelled;

        @Override
        public void run() {
            if (mCancelled) {
                return;
            }

            if (shouldBlink()) {
                if (mTextView.getLayout() != null) {
                    mTextView.invalidateCursorPath();
                }

                mTextView.postDelayed(this, BLINK);
            }
        }

        void cancel() {
            if (!mCancelled) {
                mTextView.removeCallbacks(this);
                mCancelled = true;
            }
        }

        void reset() {
            mCancelled = false;
        }
    }


    /**
     * A CursorController instance can be used to control a cursor in the text.
     */
    private interface CursorController {

        /**
         * Makes the cursor controller visible on screen.
         * See also {@link #hide()}.
         */
        void show();

        /**
         * Hide the cursor controller from screen.
         * See also {@link #show()}.
         */
        void hide();

        /**
         * Called when the view is detached from window. Perform housekeeping task, such as
         * stopping Runnable thread that would otherwise keep a reference on the context, thus
         * preventing the activity from being recycled.
         */
        void onDetached();

        boolean isCursorBeingModified();

        boolean isActive();
    }
}
