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

package icyllis.modernui.text.method;

import com.ibm.icu.lang.UCharacter;
import icyllis.modernui.text.*;
import icyllis.modernui.text.style.ReplacementSpan;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.widget.TextView;

import javax.annotation.ParametersAreNonnullByDefault;
import java.text.BreakIterator;
import java.util.Locale;

/**
 * Provides methods for handling text additions and deletions in a {@link TextView}.
 */
@ParametersAreNonnullByDefault
public final class TextKeyListener {

    private static final TextKeyListener sInstance = new TextKeyListener();

    /**
     * The meta key has been pressed but has not yet been used.
     */
    private static final int PRESSED =
            Spannable.SPAN_MARK_MARK | (1 << Spannable.SPAN_USER_SHIFT);

    private static final Object CAP = new NoCopySpan.Concrete();
    private static final Object ALT = new NoCopySpan.Concrete();

    private static final int PRESSED_RETURN_VALUE = 1;

    // Initial state
    private static final int STATE_START = 0;

    // The offset is immediately before line feed.
    private static final int STATE_LF = 1;

    // The offset is immediately before a KEYCAP.
    private static final int STATE_BEFORE_KEYCAP = 2;
    // The offset is immediately before a variation selector and a KEYCAP.
    private static final int STATE_BEFORE_VS_AND_KEYCAP = 3;

    // The offset is immediately before an emoji modifier.
    private static final int STATE_BEFORE_EMOJI_MODIFIER = 4;
    // The offset is immediately before a variation selector and an emoji modifier.
    private static final int STATE_BEFORE_VS_AND_EMOJI_MODIFIER = 5;

    // The offset is immediately before a variation selector.
    private static final int STATE_BEFORE_VS = 6;

    // The offset is immediately before an emoji.
    private static final int STATE_BEFORE_EMOJI = 7;
    // The offset is immediately before a ZWJ that were seen before a ZWJ emoji.
    private static final int STATE_BEFORE_ZWJ = 8;
    // The offset is immediately before a variation selector and a ZWJ that were seen before a
    // ZWJ emoji.
    private static final int STATE_BEFORE_VS_AND_ZWJ = 9;

    // The number of following RIS code points is odd.
    private static final int STATE_ODD_NUMBERED_RIS = 10;
    // The number of following RIS code points is even.
    private static final int STATE_EVEN_NUMBERED_RIS = 11;

    // The offset is in emoji tag sequence.
    private static final int STATE_IN_TAG_SEQUENCE = 12;

    // The state machine has been stopped.
    private static final int STATE_FINISHED = 13;

    private TextKeyListener() {
    }

    public static TextKeyListener getInstance() {
        return sInstance;
    }

    /**
     * Resets all meta state to inactive.
     */
    public static void resetMetaState(Spannable text) {
        text.removeSpan(CAP);
        text.removeSpan(ALT);
    }

    /**
     * Gets the state of a particular meta key.
     *
     * @param meta META_SHIFT_ON, META_ALT_ON, META_SYM_ON
     * @param text the buffer in which the meta key would have been pressed.
     * @return 0 if inactive, 1 if active, 2 if locked.
     */
    public static int getMetaState(CharSequence text, int meta) {
        return switch (meta) {
            case KeyEvent.META_SHIFT_ON -> getActive(text, CAP);
            case KeyEvent.META_ALT_ON -> getActive(text, ALT);
            default -> 0;
        };
    }

    private static int getActive(CharSequence text, Object meta) {
        if (!(text instanceof Spanned sp)) {
            return 0;
        }

        int flag = sp.getSpanFlags(meta);

        if (flag != 0) {
            return PRESSED_RETURN_VALUE;
        } else {
            return 0;
        }
    }

    /**
     * Returns true if this object is one that this class would use to
     * keep track of any meta state in the specified text.
     */
    public static boolean isMetaTracker(Object what) {
        return what == CAP || what == ALT;
    }

    /**
     * If the key listener wants to handle this key, return true,
     * otherwise return false and the caller (i.e.&nbsp;the widget host)
     * will handle the key.
     */
    public boolean onKeyDown(TextView view, Editable content,
                             int keyCode, KeyEvent event) {
        boolean handled = switch (keyCode) {
            case KeyEvent.KEY_BACKSPACE -> backspace(view, content, event);
            case KeyEvent.KEY_DELETE -> forwardDelete(view, content, event);
            default -> false;
        };

        if (handled) {
            return true;
        }

        if (keyCode == KeyEvent.KEY_LEFT_SHIFT || keyCode == KeyEvent.KEY_RIGHT_SHIFT) {
            press(content, CAP);
            return true;
        }

        if (keyCode == KeyEvent.KEY_LEFT_ALT || keyCode == KeyEvent.KEY_RIGHT_ALT) {
            press(content, ALT);
            return true;
        }

        return false;
    }

    private void press(Editable content, Object what) {
        content.setSpan(what, 0, 0, PRESSED);
    }

    /**
     * If the key listener wants to handle this key release, return true,
     * otherwise return false and the caller (i.e.&nbsp;the widget host)
     * will handle the key.
     */
    public boolean onKeyUp(TextView view, Editable content,
                           int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEY_LEFT_SHIFT || keyCode == KeyEvent.KEY_RIGHT_SHIFT) {
            release(content, CAP);
            return true;
        }

        if (keyCode == KeyEvent.KEY_LEFT_ALT || keyCode == KeyEvent.KEY_RIGHT_ALT) {
            release(content, ALT);
            return true;
        }

        return false;
    }

    private void release(Editable content, Object what) {
        content.removeSpan(what);
    }

    /**
     * Performs the action that happens when you press the {@link KeyEvent#KEY_BACKSPACE} key in
     * a {@link TextView}.  If there is a selection, deletes the selection; otherwise,
     * deletes the character before the cursor, if any; ALT+DEL deletes everything on
     * the line the cursor is on.
     *
     * @return true if anything was deleted; false otherwise.
     */
    public boolean backspace(TextView view, Editable content, KeyEvent event) {
        return backspaceOrForwardDelete(view, content, event, false);
    }

    /**
     * Performs the action that happens when you press the {@link KeyEvent#KEY_DELETE}
     * key in a {@link TextView}.  If there is a selection, deletes the selection; otherwise,
     * deletes the character before the cursor, if any; ALT+FORWARD_DEL deletes everything on
     * the line the cursor is on.
     *
     * @return true if anything was deleted; false otherwise.
     */
    public boolean forwardDelete(TextView view, Editable content, KeyEvent event) {
        return backspaceOrForwardDelete(view, content, event, true);
    }

    private boolean backspaceOrForwardDelete(TextView view, Editable content,
                                             KeyEvent event, boolean isForwardDelete) {
        // Ensure the key event does not have modifiers except ALT or SHIFT or CTRL.
        if ((event.getModifiers() & ~(KeyEvent.META_SHIFT_ON | KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON)) != 0) {
            return false;
        }

        // If there is a current selection, delete it.
        if (deleteSelection(content)) {
            return true;
        }

        // MetaKeyKeyListener doesn't track control key state. Need to check the KeyEvent instead.
        boolean isCtrlActive = event.isCtrlPressed();
        boolean isShiftActive = event.isShiftPressed();
        boolean isAltActive = event.isAltPressed();

        if (isCtrlActive) {
            if (isAltActive || isShiftActive) {
                // Ctrl+Alt, Ctrl+Shift, Ctrl+Alt+Shift should not delete any characters.
                return false;
            }
            return deleteUntilWordBoundary(view, content, isForwardDelete);
        }

        // Alt+Backspace or Alt+ForwardDelete deletes the current line, if possible.
        if (isAltActive && deleteLine(view, content)) {
            return true;
        }

        // Delete a character.
        final int start = Selection.getSelectionEnd(content);
        final int end;
        if (isForwardDelete) {
            Locale locale = view.getPaint().getTextLocale();
            end = getOffsetForDeleteKey(content, start, locale);
        } else {
            end = getOffsetForBackspaceKey(content, start);
        }
        if (start != end) {
            content.delete(Math.min(start, end), Math.max(start, end));
            return true;
        }
        return false;
    }

    private boolean deleteSelection(Editable content) {
        int selectionStart = Selection.getSelectionStart(content);
        int selectionEnd = Selection.getSelectionEnd(content);
        if (selectionEnd < selectionStart) {
            int temp = selectionEnd;
            selectionEnd = selectionStart;
            selectionStart = temp;
        }
        if (selectionStart != selectionEnd) {
            content.delete(selectionStart, selectionEnd);
            return true;
        }
        return false;
    }

    private boolean deleteUntilWordBoundary(TextView view, Editable content, boolean isForwardDelete) {
        int currentCursorOffset = Selection.getSelectionStart(content);

        // If there is a selection, do nothing.
        if (currentCursorOffset != Selection.getSelectionEnd(content)) {
            return false;
        }

        // Early exit if there is no contents to delete.
        if ((!isForwardDelete && currentCursorOffset == 0) ||
                (isForwardDelete && currentCursorOffset == content.length())) {
            return false;
        }

        WordIterator wordIterator = view.getWordIterator();

        if (wordIterator == null) {
            // Default locale is used for WordIterator since the appropriate locale is not clear
            // here.
            wordIterator = new WordIterator();
        }

        int deleteFrom;
        int deleteTo;

        if (isForwardDelete) {
            deleteFrom = currentCursorOffset;
            wordIterator.setCharSequence(content, deleteFrom, content.length());
            deleteTo = wordIterator.following(currentCursorOffset);
            if (deleteTo == java.text.BreakIterator.DONE) {
                deleteTo = content.length();
            }
        } else {
            deleteTo = currentCursorOffset;
            wordIterator.setCharSequence(content, 0, deleteTo);
            deleteFrom = wordIterator.preceding(currentCursorOffset);
            if (deleteFrom == BreakIterator.DONE) {
                deleteFrom = 0;
            }
        }
        content.delete(deleteFrom, deleteTo);
        return true;
    }

    private boolean deleteLine(TextView view, Editable content) {
        final Layout layout = view.getLayout();
        if (layout != null) {
            final int line = layout.getLineForOffset(Selection.getSelectionStart(content));
            final int start = layout.getLineStart(line);
            final int end = layout.getLineEnd(line);
            if (end != start) {
                content.delete(start, end);
                return true;
            }
        }
        return false;
    }

    // Returns the end offset to be deleted by a forward delete key from the given offset.
    private static int getOffsetForDeleteKey(Editable text, int offset, Locale locale) {
        final int len = text.length();

        if (offset >= len - 1) {
            return len;
        }

        offset = GraphemeBreak.getTextRunCursor(text, locale, offset, len, offset, GraphemeBreak.AFTER);

        return adjustReplacementSpan(text, offset, false /* move to the end */);
    }

    // Returns the start offset to be deleted by a backspace key from the given offset.
    private static int getOffsetForBackspaceKey(Editable text, int offset) {
        if (offset <= 1) {
            return 0;
        }

        int deleteCharCount = 0;  // Char count to be deleted by backspace.
        int lastSeenVSCharCount = 0;  // Char count of previous variation selector.

        int state = STATE_START;

        int tmpOffset = offset;
        do {
            final int codePoint = Character.codePointBefore(text, tmpOffset);
            tmpOffset -= Character.charCount(codePoint);

            switch (state) {
                case STATE_START:
                    deleteCharCount = Character.charCount(codePoint);
                    if (codePoint == 0x0A) { // LF
                        state = STATE_LF;
                    } else if (Typeface.isVariationSelector(codePoint)) {
                        state = STATE_BEFORE_VS;
                    } else if (Emoji.isRegionalIndicatorSymbol(codePoint)) {
                        state = STATE_ODD_NUMBERED_RIS;
                    } else if (Emoji.isEmojiModifier(codePoint)) {
                        state = STATE_BEFORE_EMOJI_MODIFIER;
                    } else if (codePoint == Emoji.COMBINING_ENCLOSING_KEYCAP) {
                        state = STATE_BEFORE_KEYCAP;
                    } else if (Emoji.isEmoji(codePoint)) {
                        state = STATE_BEFORE_EMOJI;
                    } else if (codePoint == Emoji.CANCEL_TAG) {
                        state = STATE_IN_TAG_SEQUENCE;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_LF:
                    if (codePoint == 0x0D) { // CR
                        ++deleteCharCount;
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_ODD_NUMBERED_RIS:
                    if (Emoji.isRegionalIndicatorSymbol(codePoint)) {
                        deleteCharCount += 2; /* Char count of RIS */
                        state = STATE_EVEN_NUMBERED_RIS;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_EVEN_NUMBERED_RIS:
                    if (Emoji.isRegionalIndicatorSymbol(codePoint)) {
                        deleteCharCount -= 2; /* Char count of RIS */
                        state = STATE_ODD_NUMBERED_RIS;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_BEFORE_KEYCAP:
                    if (Typeface.isVariationSelector(codePoint)) {
                        lastSeenVSCharCount = Character.charCount(codePoint);
                        state = STATE_BEFORE_VS_AND_KEYCAP;
                        break;
                    }

                    if (Emoji.isKeycapBase(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint);
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_BEFORE_VS_AND_KEYCAP:
                    if (Emoji.isKeycapBase(codePoint)) {
                        deleteCharCount += lastSeenVSCharCount + Character.charCount(codePoint);
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_BEFORE_EMOJI_MODIFIER:
                    if (Typeface.isVariationSelector(codePoint)) {
                        lastSeenVSCharCount = Character.charCount(codePoint);
                        state = STATE_BEFORE_VS_AND_EMOJI_MODIFIER;
                        break;
                    } else if (Emoji.isEmojiModifierBase(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint);
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_BEFORE_VS_AND_EMOJI_MODIFIER:
                    if (Emoji.isEmojiModifierBase(codePoint)) {
                        deleteCharCount += lastSeenVSCharCount + Character.charCount(codePoint);
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_BEFORE_VS:
                    if (Emoji.isEmoji(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint);
                        state = STATE_BEFORE_EMOJI;
                        break;
                    }

                    if (!Typeface.isVariationSelector(codePoint) &&
                            UCharacter.getCombiningClass(codePoint) == 0) {
                        deleteCharCount += Character.charCount(codePoint);
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_BEFORE_EMOJI:
                    if (codePoint == Emoji.ZERO_WIDTH_JOINER) {
                        state = STATE_BEFORE_ZWJ;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_BEFORE_ZWJ:
                    if (Emoji.isEmoji(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint) + 1;  // +1 for ZWJ.
                        state = Emoji.isEmojiModifier(codePoint) ?
                                STATE_BEFORE_EMOJI_MODIFIER : STATE_BEFORE_EMOJI;
                    } else if (Typeface.isVariationSelector(codePoint)) {
                        lastSeenVSCharCount = Character.charCount(codePoint);
                        state = STATE_BEFORE_VS_AND_ZWJ;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_BEFORE_VS_AND_ZWJ:
                    if (Emoji.isEmoji(codePoint)) {
                        // +1 for ZWJ.
                        deleteCharCount += lastSeenVSCharCount + 1 + Character.charCount(codePoint);
                        lastSeenVSCharCount = 0;
                        state = STATE_BEFORE_EMOJI;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_IN_TAG_SEQUENCE:
                    if (Emoji.isTagSpecChar(codePoint)) {
                        deleteCharCount += 2; /* Char count of emoji tag spec character. */
                        // Keep the same state.
                    } else if (Emoji.isEmoji(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint);
                        state = STATE_FINISHED;
                    } else {
                        // Couldn't find tag_base character. Delete the last tag_term character.
                        deleteCharCount = 2;  // for U+E007F
                        state = STATE_FINISHED;
                    }
                    // TODO: Need handle emoji variation selectors. Issue 35224297
                    break;
                default:
                    throw new IllegalArgumentException("state " + state + " is unknown");
            }
        } while (tmpOffset > 0 && state != STATE_FINISHED);

        return adjustReplacementSpan(text, offset - deleteCharCount, true /* move to the start */);
    }

    // Returns the offset of the replacement span edge if the offset is inside the replacement
    // span.  Otherwise, does nothing and returns the input offset value.
    private static int adjustReplacementSpan(Editable text, int offset, boolean moveToStart) {
        ReplacementSpan[] spans = text.getSpans(offset, offset, ReplacementSpan.class);
        if (spans != null) {
            for (ReplacementSpan span : spans) {
                final int start = text.getSpanStart(span);
                final int end = text.getSpanEnd(span);

                if (start < offset && end > offset) {
                    offset = moveToStart ? start : end;
                }
            }
        }
        return offset;
    }
}
