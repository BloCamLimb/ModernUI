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

package icyllis.modernui.text;

import com.ibm.icu.text.BreakIterator;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Utility class for manipulating cursors and selections in CharSequences.
 * A cursor is a selection where the start and end are at the same offset.
 */
@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
public final class Selection {

    public static final Object SELECTION_START = new NoCopySpan.Concrete();
    public static final Object SELECTION_END = new NoCopySpan.Concrete();

    private static final Object SELECTION_MEMORY = new NoCopySpan.Concrete();

    private Selection() {
    }

    /*
     * Retrieving the selection
     */

    /**
     * Return the offset of the selection anchor or cursor, or -1 if
     * there is no selection or cursor.
     */
    public static int getSelectionStart(CharSequence text) {
        if (text instanceof Spanned) {
            return ((Spanned) text).getSpanStart(SELECTION_START);
        }
        return -1;
    }

    /**
     * Return the offset of the selection edge or cursor, or -1 if
     * there is no selection or cursor.
     */
    public static int getSelectionEnd(CharSequence text) {
        if (text instanceof Spanned) {
            return ((Spanned) text).getSpanStart(SELECTION_END);
        }
        return -1;
    }

    private static int getSelectionMemory(CharSequence text) {
        if (text instanceof Spanned) {
            return ((Spanned) text).getSpanStart(SELECTION_MEMORY);
        }
        return -1;
    }

    /*
     * Setting the selection
     */

    /**
     * Set the selection anchor to <code>start</code> and the selection edge
     * to <code>stop</code>.
     */
    public static void setSelection(Spannable text, int start, int stop) {
        setSelection(text, start, stop, -1);
    }

    /**
     * Set the selection anchor to <code>start</code>, the selection edge
     * to <code>stop</code> and the memory horizontal to <code>memory</code>.
     */
    private static void setSelection(Spannable text, int start, int stop, int memory) {
        // int len = text.length();
        // start = pin(start, 0, len);  XXX remove unless we really need it
        // stop = pin(stop, 0, len);

        int ostart = getSelectionStart(text);
        int oend = getSelectionEnd(text);

        if (ostart != start || oend != stop) {
            text.setSpan(SELECTION_START, start, start,
                    Spanned.SPAN_POINT_POINT | Spanned.SPAN_INTERMEDIATE);
            text.setSpan(SELECTION_END, stop, stop, Spanned.SPAN_POINT_POINT);
            updateMemory(text, memory);
        }
    }

    /**
     * Update the memory position for text. This is used to ensure vertical navigation of lines
     * with different lengths behaves as expected and remembers the longest horizontal position
     * seen during a vertical traversal.
     */
    private static void updateMemory(Spannable text, int memory) {
        if (memory > -1) {
            int currentMemory = getSelectionMemory(text);
            if (memory != currentMemory) {
                text.setSpan(SELECTION_MEMORY, memory, memory, Spanned.SPAN_POINT_POINT);
                if (currentMemory == -1) {
                    // This is the first value, create a watcher.
                    final TextWatcher watcher = new MemoryTextWatcher();
                    text.setSpan(watcher, 0, text.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                }
            }
        } else {
            removeMemory(text);
        }
    }

    private static void removeMemory(Spannable text) {
        text.removeSpan(SELECTION_MEMORY);
        MemoryTextWatcher[] watchers = text.getSpans(0, text.length(), MemoryTextWatcher.class);
        if (watchers != null) {
            for (MemoryTextWatcher watcher : watchers) {
                text.removeSpan(watcher);
            }
        }
    }

    /**
     * @hide
     */
    private static final class MemoryTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            s.removeSpan(SELECTION_MEMORY);
            s.removeSpan(this);
        }
    }

    /**
     * Move the cursor to offset <code>index</code>.
     */
    public static void setSelection(Spannable text, int index) {
        setSelection(text, index, index);
    }

    /**
     * Select the entire text.
     */
    public static void selectAll(Spannable text) {
        setSelection(text, 0, text.length());
    }

    /**
     * Move the selection edge to offset <code>index</code>.
     */
    public static void extendSelection(Spannable text, int index) {
        extendSelection(text, index, -1);
    }

    /**
     * Move the selection edge to offset <code>index</code> and update the memory horizontal.
     */
    private static void extendSelection(Spannable text, int index, int memory) {
        if (text.getSpanStart(SELECTION_END) != index) {
            text.setSpan(SELECTION_END, index, index, Spanned.SPAN_POINT_POINT);
        }
        updateMemory(text, memory);
    }

    /**
     * Remove the selection or cursor, if any, from the text.
     */
    public static void removeSelection(Spannable text) {
        text.removeSpan(SELECTION_START, Spanned.SPAN_INTERMEDIATE);
        text.removeSpan(SELECTION_END);
        removeMemory(text);
    }

    /*
     * Moving the selection within the layout
     */

    /**
     * Move the cursor to the buffer offset physically above the current
     * offset, to the beginning if it is on the top line but not at the
     * start, or return false if the cursor is already on the top line.
     */
    public static boolean moveUp(Spannable text, Layout layout) {
        int start = getSelectionStart(text);
        int end = getSelectionEnd(text);

        if (start != end) {
            int min = Math.min(start, end);
            int max = Math.max(start, end);

            setSelection(text, min);

            return min != 0 || max != text.length();
        } else {
            int line = layout.getLineForOffset(end);

            if (line > 0) {
                setSelectionAndMemory(
                        text, layout, line, end, -1 /* direction */, false /* extend */);
                return true;
            } else if (end != 0) {
                setSelection(text, 0);
                return true;
            }
        }

        return false;
    }

    /**
     * Calculate the movement and memory positions needed, and set or extend the selection.
     */
    private static void setSelectionAndMemory(Spannable text, Layout layout, int line, int end,
                                              int direction, boolean extend) {
        int move;
        int newMemory;

        if (layout.getParagraphDirection(line)
                == layout.getParagraphDirection(line + direction)) {
            int memory = getSelectionMemory(text);
            if (memory > -1) {
                // We have a memory position
                float h = layout.getPrimaryHorizontal(memory);
                move = layout.getOffsetForHorizontal(line + direction, h);
                newMemory = memory;
            } else {
                // Create a new memory position
                float h = layout.getPrimaryHorizontal(end);
                move = layout.getOffsetForHorizontal(line + direction, h);
                newMemory = end;
            }
        } else {
            move = layout.getLineStart(line + direction);
            newMemory = -1;
        }

        if (extend) {
            extendSelection(text, move, newMemory);
        } else {
            setSelection(text, move, move, newMemory);
        }
    }

    /**
     * Move the cursor to the buffer offset physically below the current
     * offset, to the end of the buffer if it is on the bottom line but
     * not at the end, or return false if the cursor is already at the
     * end of the buffer.
     */
    public static boolean moveDown(Spannable text, Layout layout) {
        int start = getSelectionStart(text);
        int end = getSelectionEnd(text);

        if (start != end) {
            int min = Math.min(start, end);
            int max = Math.max(start, end);

            setSelection(text, max);

            return min != 0 || max != text.length();
        } else {
            int line = layout.getLineForOffset(end);

            if (line < layout.getLineCount() - 1) {
                setSelectionAndMemory(
                        text, layout, line, end, 1 /* direction */, false /* extend */);
                return true;
            } else if (end != text.length()) {
                setSelection(text, text.length());
                return true;
            }
        }

        return false;
    }

    /**
     * Move the cursor to the buffer offset physically to the left of
     * the current offset, or return false if the cursor is already
     * at the left edge of the line and there is not another line to move it to.
     */
    public static boolean moveLeft(Spannable text, Layout layout) {
        int start = getSelectionStart(text);
        int end = getSelectionEnd(text);

        if (start != end) {
            setSelection(text, chooseHorizontal(layout, -1, start, end));
            return true;
        } else {
            int to = layout.getOffsetToLeftOf(end);

            if (to != end) {
                setSelection(text, to);
                return true;
            }
        }

        return false;
    }

    /**
     * Move the cursor to the buffer offset physically to the right of
     * the current offset, or return false if the cursor is already at
     * at the right edge of the line and there is not another line
     * to move it to.
     */
    public static boolean moveRight(Spannable text, Layout layout) {
        int start = getSelectionStart(text);
        int end = getSelectionEnd(text);

        if (start != end) {
            setSelection(text, chooseHorizontal(layout, 1, start, end));
            return true;
        } else {
            int to = layout.getOffsetToRightOf(end);

            if (to != end) {
                setSelection(text, to);
                return true;
            }
        }

        return false;
    }

    /**
     * Move the selection end to the buffer offset physically above
     * the current selection end.
     */
    public static boolean extendUp(Spannable text, Layout layout) {
        int end = getSelectionEnd(text);
        int line = layout.getLineForOffset(end);

        if (line > 0) {
            setSelectionAndMemory(text, layout, line, end, -1 /* direction */, true /* extend */);
            return true;
        } else if (end != 0) {
            extendSelection(text, 0);
            return true;
        }

        return true;
    }

    /**
     * Move the selection end to the buffer offset physically below
     * the current selection end.
     */
    public static boolean extendDown(Spannable text, Layout layout) {
        int end = getSelectionEnd(text);
        int line = layout.getLineForOffset(end);

        if (line < layout.getLineCount() - 1) {
            setSelectionAndMemory(text, layout, line, end, 1 /* direction */, true /* extend */);
            return true;
        } else if (end != text.length()) {
            extendSelection(text, text.length(), -1);
            return true;
        }

        return true;
    }

    /**
     * Move the selection end to the buffer offset physically to the left of
     * the current selection end.
     */
    public static boolean extendLeft(Spannable text, Layout layout) {
        int end = getSelectionEnd(text);
        int to = layout.getOffsetToLeftOf(end);

        if (to != end) {
            extendSelection(text, to);
            return true;
        }

        return true;
    }

    /**
     * Move the selection end to the buffer offset physically to the right of
     * the current selection end.
     */
    public static boolean extendRight(Spannable text, Layout layout) {
        int end = getSelectionEnd(text);
        int to = layout.getOffsetToRightOf(end);

        if (to != end) {
            extendSelection(text, to);
            return true;
        }

        return true;
    }

    public static boolean extendToLeftEdge(Spannable text, Layout layout) {
        int where = findEdge(text, layout, -1);
        extendSelection(text, where);
        return true;
    }

    public static boolean extendToRightEdge(Spannable text, Layout layout) {
        int where = findEdge(text, layout, 1);
        extendSelection(text, where);
        return true;
    }

    public static boolean moveToLeftEdge(Spannable text, Layout layout) {
        int where = findEdge(text, layout, -1);
        setSelection(text, where);
        return true;
    }

    public static boolean moveToRightEdge(Spannable text, Layout layout) {
        int where = findEdge(text, layout, 1);
        setSelection(text, where);
        return true;
    }

    /**
     * {@hide}
     */
    public static boolean moveToPreceding(
            Spannable text, BreakIterator iter, boolean extendSelection) {
        final int offset = iter.preceding(getSelectionEnd(text));
        if (offset != BreakIterator.DONE) {
            if (extendSelection) {
                extendSelection(text, offset);
            } else {
                setSelection(text, offset);
            }
        }
        return true;
    }

    /**
     * {@hide}
     */
    public static boolean moveToFollowing(
            Spannable text, BreakIterator iter, boolean extendSelection) {
        final int offset = iter.following(getSelectionEnd(text));
        if (offset != BreakIterator.DONE) {
            if (extendSelection) {
                extendSelection(text, offset);
            } else {
                setSelection(text, offset);
            }
        }
        return true;
    }

    private static int findEdge(Spannable text, Layout layout, int dir) {
        int pt = getSelectionEnd(text);
        int line = layout.getLineForOffset(pt);
        int pdir = layout.getParagraphDirection(line);

        if (dir * pdir < 0) {
            return layout.getLineStart(line);
        } else {
            int end = layout.getLineEnd(line);

            if (line == layout.getLineCount() - 1)
                return end;
            else
                return end - 1;
        }
    }

    private static int chooseHorizontal(Layout layout, int direction,
                                        int off1, int off2) {
        int line1 = layout.getLineForOffset(off1);
        int line2 = layout.getLineForOffset(off2);

        if (line1 == line2) {
            // same line, so it goes by pure physical direction

            float h1 = layout.getPrimaryHorizontal(off1);
            float h2 = layout.getPrimaryHorizontal(off2);

            if (direction < 0) {
                // to left

                if (h1 < h2)
                    return off1;
                else
                    return off2;
            } else {
                // to right

                if (h1 > h2)
                    return off1;
                else
                    return off2;
            }
        } else {
            // different line, so which line is "left" and which is "right"
            // depends upon the directionality of the text

            // This only checks at one end, but it's not clear what the
            // right thing to do is if the ends don't agree.  Even if it
            // is wrong it should still not be too bad.
            int line = layout.getLineForOffset(off1);
            int textdir = layout.getParagraphDirection(line);

            if (textdir == direction)
                return Math.max(off1, off2);
            else
                return Math.min(off1, off2);
        }
    }
}
