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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.*;
import icyllis.modernui.text.*;
import icyllis.modernui.text.method.MovementMethod;
import icyllis.modernui.text.method.WordIterator;
import icyllis.modernui.util.Parcel;
import icyllis.modernui.view.*;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.annotation.Nonnull;

/**
 * Helper class used by TextView to handle editable text views.
 */
public class Editor {

    private static final Marker MARKER = MarkerFactory.getMarker("Editor");

    static final int BLINK = 500;
    // Tag used when the Editor maintains its own separate UndoManager.
    private static final String UNDO_OWNER_TAG = "Editor";

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

    // Each Editor manages its own undo stack.
    private final UndoManager mUndoManager = new UndoManager();
    private UndoOwner mUndoOwner = mUndoManager.getOwner(UNDO_OWNER_TAG, this);
    final UndoInputFilter mUndoInputFilter = new UndoInputFilter(this);
    boolean mAllowUndo = true;

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

    /**
     * Forgets all undo and redo operations for this Editor.
     */
    void forgetUndoRedo() {
        UndoOwner[] owners = {mUndoOwner};
        mUndoManager.forgetUndos(owners, -1 /* all */);
        mUndoManager.forgetRedos(owners, -1 /* all */);
    }

    boolean canUndo() {
        UndoOwner[] owners = {mUndoOwner};
        return mAllowUndo && mUndoManager.countUndos(owners) > 0;
    }

    boolean canRedo() {
        UndoOwner[] owners = {mUndoOwner};
        return mAllowUndo && mUndoManager.countRedos(owners) > 0;
    }

    void undo() {
        if (!mAllowUndo) {
            return;
        }
        UndoOwner[] owners = {mUndoOwner};
        mUndoManager.undo(owners, 1);  // Undo 1 action.
    }

    void redo() {
        if (!mAllowUndo) {
            return;
        }
        UndoOwner[] owners = {mUndoOwner};
        mUndoManager.redo(owners, 1);  // Redo 1 action.
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
        if (mIsBeingLongClicked) {
            return;
        }
        // Added by Modern UI
        final int offset;
        if (Float.isNaN(mContextMenuAnchorX)
                || Float.isNaN(mContextMenuAnchorY)) {
            offset = mTextView.getSelectionEnd();
        } else {
            offset = mTextView.getOffsetForPosition(mContextMenuAnchorX, mContextMenuAnchorY);
        }
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

        //TODO localization

        menu.add(Menu.NONE, TextView.ID_UNDO, MENU_ITEM_ORDER_UNDO, "Undo")
                .setAlphabeticShortcut('z')
                .setOnMenuItemClickListener(mOnContextMenuItemClickListener)
                .setEnabled(mTextView.canUndo());
        menu.add(Menu.NONE, TextView.ID_REDO, MENU_ITEM_ORDER_REDO, "Redo")
                .setAlphabeticShortcut('y')
                .setOnMenuItemClickListener(mOnContextMenuItemClickListener)
                .setEnabled(mTextView.canRedo());

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

    /**
     * @return True iff (start, end) is a valid range within the text.
     */
    private static boolean isValidRange(CharSequence text, int start, int end) {
        return 0 <= start && start <= end && end <= text.length();
    }

    /**
     * An InputFilter that monitors text input to maintain undo history. It does not modify the
     * text being typed (and hence always returns null from the filter() method).
     * <p>
     * TODO: Make this span aware.
     */
    public static class UndoInputFilter implements InputFilter {
        private final Editor mEditor;

        // Whether the current filter pass is directly caused by an end-user text edit.
        private boolean mIsUserEdit;

        // Whether the previous edit operation was in the current batch edit.
        private boolean mPreviousOperationWasInSameBatchEdit;

        public UndoInputFilter(Editor editor) {
            mEditor = editor;
        }

        /**
         * Signals that a user-triggered edit is starting.
         */
        public void beginBatchEdit() {
            mIsUserEdit = true;
        }

        public void endBatchEdit() {
            mIsUserEdit = false;
            mPreviousOperationWasInSameBatchEdit = false;
        }

        @Override
        public CharSequence filter(@NonNull CharSequence source, int start, int end,
                                   @NonNull Spanned dest, int dstart, int dend) {
            // Check to see if this edit should be tracked for undo.
            if (!canUndoEdit(source, start, end, dest, dstart, dend)) {
                return null;
            }

            // Handle edit.
            handleEdit(source, start, end, dest, dstart, dend);
            return null;
        }

        void freezeLastEdit() {
            mEditor.mUndoManager.beginUpdate("Edit text");
            EditOperation lastEdit = getLastEdit();
            if (lastEdit != null) {
                lastEdit.mFrozen = true;
            }
            mEditor.mUndoManager.endUpdate();
        }

        private static final int MERGE_EDIT_MODE_FORCE_MERGE = 0;
        /**
         * Use {@link EditOperation#mergeWith} to merge
         */
        private static final int MERGE_EDIT_MODE_NORMAL = 2;

        private void handleEdit(CharSequence source, int start, int end,
                                Spanned dest, int dstart, int dend) {
            // An application may install a TextWatcher to provide additional modifications after
            // the initial input filters run (e.g. a credit card formatter that adds spaces to a
            // string). This results in multiple filter() calls for what the user considers to be
            // a single operation. Always undo the whole set of changes in one step.
            final int mergeMode;
            if (isInTextWatcher() || mPreviousOperationWasInSameBatchEdit) {
                mergeMode = MERGE_EDIT_MODE_FORCE_MERGE;
            } else {
                mergeMode = MERGE_EDIT_MODE_NORMAL;
            }
            // Build a new operation with all the information from this edit.
            String newText = TextUtils.substring(source, start, end);
            String oldText = TextUtils.substring(dest, dstart, dend);
            EditOperation edit = new EditOperation(mEditor, oldText, dstart, newText);
            recordEdit(edit, mergeMode);
        }

        private EditOperation getLastEdit() {
            return mEditor.mUndoManager.getLastOperation(
                    EditOperation.class, mEditor.mUndoOwner, UndoManager.MERGE_MODE_UNIQUE);
        }

        /**
         * Fetches the last undo operation and checks to see if a new edit should be merged into it.
         * If forceMerge is true then the new edit is always merged.
         */
        private void recordEdit(EditOperation edit, int mergeMode) {
            // Fetch the last edit operation and attempt to merge in the new edit.
            final UndoManager um = mEditor.mUndoManager;
            um.beginUpdate("Edit text");
            EditOperation lastEdit = getLastEdit();
            if (lastEdit == null) {
                // Add this as the first edit.
                um.addOperation(edit, UndoManager.MERGE_MODE_NONE);
            } else if (mergeMode == MERGE_EDIT_MODE_FORCE_MERGE) {
                // Forced merges take priority because they could be the result of a non-user-edit
                // change and this case should not create a new undo operation.
                lastEdit.forceMergeWith(edit);
            } else if (!mIsUserEdit) {
                // An application directly modified the Editable outside of a text edit. Treat this
                // as a new change and don't attempt to merge.
                um.commitState(mEditor.mUndoOwner);
                um.addOperation(edit, UndoManager.MERGE_MODE_NONE);
            } else if (mergeMode == MERGE_EDIT_MODE_NORMAL && lastEdit.mergeWith(edit)) {
                // Merge succeeded, nothing else to do.
            } else {
                // Could not merge with the last edit, so commit the last edit and add this edit.
                um.commitState(mEditor.mUndoOwner);
                um.addOperation(edit, UndoManager.MERGE_MODE_NONE);
            }
            mPreviousOperationWasInSameBatchEdit = mIsUserEdit;
            um.endUpdate();
        }

        private boolean canUndoEdit(CharSequence source, int start, int end,
                                    Spanned dest, int dstart, int dend) {
            if (!mEditor.mAllowUndo) {
                return false;
            }

            if (mEditor.mUndoManager.isInUndo()) {
                return false;
            }

            // Text filters run before input operations are applied. However, some input operations
            // are invalid and will throw exceptions when applied. This is common in tests. Don't
            // attempt to undo invalid operations.
            if (!isValidRange(source, start, end) || !isValidRange(dest, dstart, dend)) {
                return false;
            }

            // Earlier filters can rewrite input to be a no-op, for example due to a length limit
            // on an input field. Skip no-op changes.
            if (start == end && dstart == dend) {
                return false;
            }

            return true;
        }

        private boolean isInTextWatcher() {
            CharSequence text = mEditor.mTextView.getText();
            return (text instanceof SpannableStringBuilder)
                    && ((SpannableStringBuilder) text).getTextWatcherDepth() > 0;
        }
    }

    /**
     * An operation to undo a single "edit" to a text view.
     */
    public static class EditOperation extends UndoOperation<Editor> {

        @SuppressWarnings("unused")
        public static final ClassLoaderCreator<EditOperation> CREATOR = EditOperation::new;

        private static final int TYPE_INSERT = 0;
        private static final int TYPE_DELETE = 1;
        private static final int TYPE_REPLACE = 2;

        private int mType;
        private String mOldText;
        private String mNewText;
        private int mStart;

        private int mOldCursorPos;
        private int mNewCursorPos;
        private boolean mFrozen;
        private boolean mIsComposition;

        /**
         * Constructs an edit operation from a text input operation on editor that replaces the
         * oldText starting at dstart with newText.
         */
        public EditOperation(Editor editor, String oldText, int dstart, String newText) {
            super(editor.mUndoOwner);
            mOldText = oldText;
            mNewText = newText;

            // Determine the type of the edit.
            if (!mNewText.isEmpty() && mOldText.isEmpty()) {
                mType = TYPE_INSERT;
            } else if (mNewText.isEmpty() && !mOldText.isEmpty()) {
                mType = TYPE_DELETE;
            } else {
                mType = TYPE_REPLACE;
            }

            mStart = dstart;
            // Store cursor data.
            mOldCursorPos = editor.mTextView.getSelectionStart();
            mNewCursorPos = dstart + mNewText.length();
        }

        public EditOperation(Parcel src, ClassLoader loader) {
            super(src, loader);
            mType = src.readInt();
            mOldText = src.readString();
            mNewText = src.readString();
            mStart = src.readInt();
            mOldCursorPos = src.readInt();
            mNewCursorPos = src.readInt();
            mFrozen = src.readInt() == 1;
            mIsComposition = src.readInt() == 1;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeInt(mType);
            dest.writeString(mOldText);
            dest.writeString(mNewText);
            dest.writeInt(mStart);
            dest.writeInt(mOldCursorPos);
            dest.writeInt(mNewCursorPos);
            dest.writeInt(mFrozen ? 1 : 0);
            dest.writeInt(mIsComposition ? 1 : 0);
        }

        private int getNewTextEnd() {
            return mStart + mNewText.length();
        }

        private int getOldTextEnd() {
            return mStart + mOldText.length();
        }

        @Override
        public void commit() {
        }

        @Override
        public void undo() {
            // Remove the new text and insert the old.
            Editor editor = getOwnerData();
            Editable text = (Editable) editor.mTextView.getText();
            modifyText(text, mStart, getNewTextEnd(), mOldText, mStart, mOldCursorPos);
        }

        @Override
        public void redo() {
            // Remove the old text and insert the new.
            Editor editor = getOwnerData();
            Editable text = (Editable) editor.mTextView.getText();
            modifyText(text, mStart, getOldTextEnd(), mNewText, mStart, mNewCursorPos);
        }

        /**
         * Attempts to merge this existing operation with a new edit.
         *
         * @param edit The new edit operation.
         * @return If the merge succeeded, returns true. Otherwise returns false and leaves this
         * object unchanged.
         */
        private boolean mergeWith(EditOperation edit) {
            if (mFrozen) {
                return false;
            }

            switch (mType) {
                case TYPE_INSERT:
                    return mergeInsertWith(edit);
                case TYPE_DELETE:
                    return mergeDeleteWith(edit);
                case TYPE_REPLACE:
                    return mergeReplaceWith(edit);
                default:
                    return false;
            }
        }

        private boolean mergeInsertWith(EditOperation edit) {
            if (edit.mType == TYPE_INSERT) {
                // Merge insertions that are contiguous even when it's frozen.
                if (getNewTextEnd() != edit.mStart) {
                    return false;
                }
                mNewText += edit.mNewText;
                mNewCursorPos = edit.mNewCursorPos;
                mFrozen = edit.mFrozen;
                mIsComposition = edit.mIsComposition;
                return true;
            }
            if (mIsComposition && edit.mType == TYPE_REPLACE
                    && mStart <= edit.mStart && getNewTextEnd() >= edit.getOldTextEnd()) {
                // Merge insertion with replace as they can be single insertion.
                mNewText = mNewText.substring(0, edit.mStart - mStart) + edit.mNewText
                        + mNewText.substring(edit.getOldTextEnd() - mStart);
                mNewCursorPos = edit.mNewCursorPos;
                mIsComposition = edit.mIsComposition;
                return true;
            }
            return false;
        }

        // TODO: Support forward delete.
        private boolean mergeDeleteWith(EditOperation edit) {
            // Only merge continuous deletes.
            if (edit.mType != TYPE_DELETE) {
                return false;
            }
            // Only merge deletions that are contiguous.
            if (mStart != edit.getOldTextEnd()) {
                return false;
            }
            mStart = edit.mStart;
            mOldText = edit.mOldText + mOldText;
            mNewCursorPos = edit.mNewCursorPos;
            mIsComposition = edit.mIsComposition;
            return true;
        }

        private boolean mergeReplaceWith(EditOperation edit) {
            if (edit.mType == TYPE_INSERT && getNewTextEnd() == edit.mStart) {
                // Merge with adjacent insert.
                mNewText += edit.mNewText;
                mNewCursorPos = edit.mNewCursorPos;
                return true;
            }
            if (!mIsComposition) {
                return false;
            }
            if (edit.mType == TYPE_DELETE && mStart <= edit.mStart
                    && getNewTextEnd() >= edit.getOldTextEnd()) {
                // Merge with delete as they can be single operation.
                mNewText = mNewText.substring(0, edit.mStart - mStart)
                        + mNewText.substring(edit.getOldTextEnd() - mStart);
                if (mNewText.isEmpty()) {
                    mType = TYPE_DELETE;
                }
                mNewCursorPos = edit.mNewCursorPos;
                mIsComposition = edit.mIsComposition;
                return true;
            }
            if (edit.mType == TYPE_REPLACE && mStart == edit.mStart
                    && TextUtils.contentEquals(mNewText, edit.mOldText)) {
                // Merge with the replace that replaces the same region.
                mNewText = edit.mNewText;
                mNewCursorPos = edit.mNewCursorPos;
                mIsComposition = edit.mIsComposition;
                return true;
            }
            return false;
        }

        /**
         * Forcibly creates a single merged edit operation by simulating the entire text
         * contents being replaced.
         */
        public void forceMergeWith(EditOperation edit) {
            if (mergeWith(edit)) {
                return;
            }
            Editor editor = getOwnerData();

            // Copy the text of the current field.
            // NOTE: Using StringBuilder instead of SpannableStringBuilder would be somewhat faster,
            // but would require two parallel implementations of modifyText() because Editable and
            // StringBuilder do not share an interface for replace/delete/insert.
            Editable editable = (Editable) editor.mTextView.getText();
            Editable originalText = new SpannableStringBuilder(editable.toString());

            // Roll back the last operation.
            modifyText(originalText, mStart, getNewTextEnd(), mOldText, mStart, mOldCursorPos);

            // Clone the text again and apply the new operation.
            Editable finalText = new SpannableStringBuilder(editable.toString());
            modifyText(finalText, edit.mStart, edit.getOldTextEnd(),
                    edit.mNewText, edit.mStart, edit.mNewCursorPos);

            // Convert this operation into a replace operation.
            mType = TYPE_REPLACE;
            mNewText = finalText.toString();
            mOldText = originalText.toString();
            mStart = 0;
            mNewCursorPos = edit.mNewCursorPos;
            mIsComposition = edit.mIsComposition;
            // mOldCursorPos is unchanged.
        }

        private static void modifyText(Editable text, int deleteFrom, int deleteTo,
                                       CharSequence newText, int newTextInsertAt, int newCursorPos) {
            // Apply the edit if it is still valid.
            if (isValidRange(text, deleteFrom, deleteTo)
                    && newTextInsertAt <= text.length() - (deleteTo - deleteFrom)) {
                if (deleteFrom != deleteTo) {
                    text.delete(deleteFrom, deleteTo);
                }
                if (!newText.isEmpty()) {
                    text.insert(newTextInsertAt, newText);
                }
            }
            // Restore the cursor position. If there wasn't an old cursor (newCursorPos == -1) then
            // don't explicitly set it and rely on SpannableStringBuilder to position it.
            // TODO: Select all the text that was undone.
            if (0 <= newCursorPos && newCursorPos <= text.length()) {
                Selection.setSelection(text, newCursorPos);
            }
        }

        private String getTypeString() {
            switch (mType) {
                case TYPE_INSERT:
                    return "insert";
                case TYPE_DELETE:
                    return "delete";
                case TYPE_REPLACE:
                    return "replace";
                default:
                    return "";
            }
        }

        @Override
        public String toString() {
            return "[mType=" + getTypeString() + ", "
                    + "mOldText=" + mOldText + ", "
                    + "mNewText=" + mNewText + ", "
                    + "mStart=" + mStart + ", "
                    + "mOldCursorPos=" + mOldCursorPos + ", "
                    + "mNewCursorPos=" + mNewCursorPos + ", "
                    + "mFrozen=" + mFrozen + ", "
                    + "mIsComposition=" + mIsComposition + "]";
        }
    }
}
