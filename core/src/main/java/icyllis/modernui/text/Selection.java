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

/**
 * Utility class for manipulating cursors and selections in CharSequences.
 * A cursor is a selection where the start and end are at the same offset.
 */
public class Selection {

    /*
     * Public constants
     */

    public static final Object SELECTION_START = new Start();
    public static final Object SELECTION_END = new End();

    private static final Object SELECTION_MEMORY = new Memory();

    private static final class Start implements NoCopySpan {
    }

    private static final class End implements NoCopySpan {
    }

    private static final class Memory implements NoCopySpan {
    }

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
}
