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

import icyllis.modernui.text.Spannable;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.widget.TextView;

/**
 * Base classes for movement methods.
 */
public abstract class BaseMovementMethod implements MovementMethod {

    protected BaseMovementMethod() {
    }

    @Override
    public boolean canSelectArbitrarily() {
        return false;
    }

    @Override
    public void initialize(TextView widget, Spannable text) {
    }

    @Override
    public boolean onKeyDown(TextView widget, Spannable text, int keyCode, KeyEvent event) {
        final int movementMods = event.getModifiers() & ~KeyEvent.MOD_SHIFT;
        boolean handled = handleMovementKey(widget, text, keyCode, movementMods, event);
        if (handled) {
            TextKeyListener.adjustMetaAfterKeypress(text);
            TextKeyListener.resetLockedMeta(text);
        }
        return handled;
    }

    @Override
    public boolean onKeyUp(TextView widget, Spannable text, int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyOther(TextView widget, Spannable text, KeyEvent event) {
        final int movementMods = event.getModifiers() & ~KeyEvent.MOD_SHIFT;
        final int keyCode = event.getKeyCode();
        final int repeat = event.getRepeatCount();
        boolean handled = false;
        for (int i = 0; i < repeat; i++) {
            if (!handleMovementKey(widget, text, keyCode, movementMods, event)) {
                break;
            }
            handled = true;
        }
        if (handled) {
            TextKeyListener.adjustMetaAfterKeypress(text);
            TextKeyListener.resetLockedMeta(text);
        }
        return handled;
    }

    @Override
    public void onTakeFocus(TextView widget, Spannable text, int direction) {
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable text, MotionEvent event) {
        return false;
    }

    @Override
    public boolean onGenericMotionEvent(TextView widget, Spannable text, MotionEvent event) {
        //TODO scrolling
        return false;
    }

    /**
     * Performs a movement key action.
     * <p>
     * The default implementation decodes the key down and invokes movement actions
     * such as {@link #down} and {@link #up}.
     *
     * @param widget       The text view.
     * @param buffer       The text buffer.
     * @param event        The key event.
     * @param keyCode      The key code.
     * @param movementMods The keyboard meta states used for movement.
     * @return True if the event was handled.
     */
    protected boolean handleMovementKey(TextView widget, Spannable buffer,
                                        int keyCode, int movementMods, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEY_LEFT:
                if (movementMods == 0) {
                    return left(widget, buffer);
                } else if ((movementMods & KeyEvent.MOD_CTRL) != 0) {
                    return leftWord(widget, buffer);
                } else if ((movementMods & KeyEvent.MOD_ALT) != 0) {
                    return lineStart(widget, buffer);
                }
                break;

            case KeyEvent.KEY_RIGHT:
                if (movementMods == 0) {
                    return right(widget, buffer);
                } else if ((movementMods & KeyEvent.MOD_CTRL) != 0) {
                    return rightWord(widget, buffer);
                } else if ((movementMods & KeyEvent.MOD_ALT) != 0) {
                    return lineEnd(widget, buffer);
                }
                break;

            case KeyEvent.KEY_UP:
                if (movementMods == 0) {
                    return up(widget, buffer);
                } else if ((movementMods & KeyEvent.MOD_ALT) != 0) {
                    return top(widget, buffer);
                }
                break;

            case KeyEvent.KEY_DOWN:
                if (movementMods == 0) {
                    return down(widget, buffer);
                } else if ((movementMods & KeyEvent.MOD_ALT) != 0) {
                    return bottom(widget, buffer);
                }
                break;

            case KeyEvent.KEY_PAGE_UP:
                if (movementMods == 0) {
                    return pageUp(widget, buffer);
                } else if ((movementMods & KeyEvent.MOD_ALT) != 0) {
                    return top(widget, buffer);
                }
                break;

            case KeyEvent.KEY_PAGE_DOWN:
                if (movementMods == 0) {
                    return pageDown(widget, buffer);
                } else if ((movementMods & KeyEvent.MOD_ALT) != 0) {
                    return bottom(widget, buffer);
                }
                break;

            case KeyEvent.KEY_HOME:
                if (movementMods == 0) {
                    return home(widget, buffer);
                } else if ((movementMods & KeyEvent.MOD_CTRL) != 0) {
                    return top(widget, buffer);
                }
                break;

            case KeyEvent.KEY_END:
                if (movementMods == 0) {
                    return end(widget, buffer);
                } else if ((movementMods & KeyEvent.MOD_CTRL) != 0) {
                    return bottom(widget, buffer);
                }
                break;
        }
        return false;
    }

    /**
     * Performs a left movement action.
     * Moves the cursor or scrolls left by one character.
     *
     * @param widget The text view.
     * @param buffer The text buffer.
     * @return True if the event was handled.
     */
    protected boolean left(TextView widget, Spannable buffer) {
        return false;
    }

    /**
     * Performs a right movement action.
     * Moves the cursor or scrolls right by one character.
     *
     * @param widget The text view.
     * @param buffer The text buffer.
     * @return True if the event was handled.
     */
    protected boolean right(TextView widget, Spannable buffer) {
        return false;
    }

    /**
     * Performs an up movement action.
     * Moves the cursor or scrolls up by one line.
     *
     * @param widget The text view.
     * @param buffer The text buffer.
     * @return True if the event was handled.
     */
    protected boolean up(TextView widget, Spannable buffer) {
        return false;
    }

    /**
     * Performs a down movement action.
     * Moves the cursor or scrolls down by one line.
     *
     * @param widget The text view.
     * @param buffer The text buffer.
     * @return True if the event was handled.
     */
    protected boolean down(TextView widget, Spannable buffer) {
        return false;
    }

    /**
     * Performs a page-up movement action.
     * Moves the cursor or scrolls up by one page.
     *
     * @param widget The text view.
     * @param buffer The text buffer.
     * @return True if the event was handled.
     */
    protected boolean pageUp(TextView widget, Spannable buffer) {
        return false;
    }

    /**
     * Performs a page-down movement action.
     * Moves the cursor or scrolls down by one page.
     *
     * @param widget The text view.
     * @param buffer The text buffer.
     * @return True if the event was handled.
     */
    protected boolean pageDown(TextView widget, Spannable buffer) {
        return false;
    }

    /**
     * Performs a top movement action.
     * Moves the cursor or scrolls to the top of the buffer.
     *
     * @param widget The text view.
     * @param buffer The text buffer.
     * @return True if the event was handled.
     */
    protected boolean top(TextView widget, Spannable buffer) {
        return false;
    }

    /**
     * Performs a bottom movement action.
     * Moves the cursor or scrolls to the bottom of the buffer.
     *
     * @param widget The text view.
     * @param buffer The text buffer.
     * @return True if the event was handled.
     */
    protected boolean bottom(TextView widget, Spannable buffer) {
        return false;
    }

    /**
     * Performs a line-start movement action.
     * Moves the cursor or scrolls to the start of the line.
     *
     * @param widget The text view.
     * @param buffer The text buffer.
     * @return True if the event was handled.
     */
    protected boolean lineStart(TextView widget, Spannable buffer) {
        return false;
    }

    /**
     * Performs a line-end movement action.
     * Moves the cursor or scrolls to the end of the line.
     *
     * @param widget The text view.
     * @param buffer The text buffer.
     * @return True if the event was handled.
     */
    protected boolean lineEnd(TextView widget, Spannable buffer) {
        return false;
    }

    /**
     * {@hide}
     */
    protected boolean leftWord(TextView widget, Spannable buffer) {
        return false;
    }

    /**
     * {@hide}
     */
    protected boolean rightWord(TextView widget, Spannable buffer) {
        return false;
    }

    /**
     * Performs a home movement action.
     * Moves the cursor or scrolls to the start of the line or to the top of the
     * document depending on whether the insertion point is being moved or
     * the document is being scrolled.
     *
     * @param widget The text view.
     * @param buffer The text buffer.
     * @return True if the event was handled.
     */
    protected boolean home(TextView widget, Spannable buffer) {
        return false;
    }

    /**
     * Performs an end movement action.
     * Moves the cursor or scrolls to the start of the line or to the top of the
     * document depending on whether the insertion point is being moved or
     * the document is being scrolled.
     *
     * @param widget The text view.
     * @param buffer The text buffer.
     * @return True if the event was handled.
     */
    protected boolean end(TextView widget, Spannable buffer) {
        return false;
    }
}
