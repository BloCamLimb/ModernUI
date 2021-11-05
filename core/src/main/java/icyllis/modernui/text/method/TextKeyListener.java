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

import icyllis.modernui.text.NoCopySpan;
import icyllis.modernui.text.Spannable;
import icyllis.modernui.text.Spanned;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.widget.TextView;

/**
 * Provides methods for handling text additions and deletions in a {@link TextView}.
 */
public final class TextKeyListener {

    /**
     * The meta key has been pressed but has not yet been used.
     */
    private static final int PRESSED =
            Spannable.SPAN_MARK_MARK | (1 << Spannable.SPAN_USER_SHIFT);

    /**
     * The meta key has been pressed and released but has still
     * not yet been used.
     */
    private static final int RELEASED =
            Spannable.SPAN_MARK_MARK | (2 << Spannable.SPAN_USER_SHIFT);

    /**
     * The meta key has been pressed and used but has not yet been released.
     */
    private static final int USED =
            Spannable.SPAN_MARK_MARK | (3 << Spannable.SPAN_USER_SHIFT);

    /**
     * The meta key has been pressed and released without use, and then
     * pressed again; it may also have been released again.
     */
    private static final int LOCKED =
            Spannable.SPAN_MARK_MARK | (4 << Spannable.SPAN_USER_SHIFT);

    private static final Object CAP = new NoCopySpan.Concrete();
    private static final Object ALT = new NoCopySpan.Concrete();

    static final int PRESSED_RETURN_VALUE = 1;
    static final int LOCKED_RETURN_VALUE = 2;

    // As META_SELECTING is @hide we should not mention it in public comments, hence the
    // omission in @param meta
    /**
     * Gets the state of a particular meta key.
     *
     * @param meta META_SHIFT_ON, META_ALT_ON, META_SYM_ON
     * @param text the buffer in which the meta key would have been pressed.
     *
     * @return 0 if inactive, 1 if active, 2 if locked.
     */
    public static int getMetaState(CharSequence text, int meta) {
        return switch (meta) {
            case KeyEvent.MOD_SHIFT -> getActive(text, CAP, PRESSED_RETURN_VALUE, LOCKED_RETURN_VALUE);
            case KeyEvent.MOD_ALT -> getActive(text, ALT, PRESSED_RETURN_VALUE, LOCKED_RETURN_VALUE);
            default -> 0;
        };
    }

    private static int getActive(CharSequence text, Object meta,
                                 int on, int lock) {
        if (!(text instanceof Spanned sp)) {
            return 0;
        }

        int flag = sp.getSpanFlags(meta);

        if (flag == LOCKED) {
            return lock;
        } else if (flag != 0) {
            return on;
        } else {
            return 0;
        }
    }

    /**
     * Call this method after you handle a keypress so that the meta
     * state will be reset to unshifted (if it is not still down)
     * or primed to be reset to unshifted (once it is released).
     */
    public static void adjustMetaAfterKeypress(Spannable content) {
        adjust(content, CAP);
        adjust(content, ALT);
    }

    private static void adjust(Spannable content, Object what) {
        int current = content.getSpanFlags(what);

        if (current == PRESSED)
            content.setSpan(what, 0, 0, USED);
        else if (current == RELEASED)
            content.removeSpan(what);
    }

    /**
     * Call this if you are a method that ignores the locked meta state
     * (arrow keys, for example) and you handle a key.
     */
    public static void resetLockedMeta(Spannable content) {
        resetLock(content, CAP);
        resetLock(content, ALT);
    }

    private static void resetLock(Spannable content, Object what) {
        int current = content.getSpanFlags(what);

        if (current == LOCKED)
            content.removeSpan(what);
    }
}
