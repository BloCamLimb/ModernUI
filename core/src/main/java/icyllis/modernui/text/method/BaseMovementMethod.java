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

import icyllis.modernui.text.Layout;
import icyllis.modernui.text.Spannable;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.widget.TextView;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Base classes for movement methods.
 */
@ParametersAreNonnullByDefault
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
        final int movementMods = event.getModifiers() & ~KeyEvent.META_SHIFT_ON;
        return handleMovementKey(widget, text, keyCode, movementMods);
    }

    @Override
    public boolean onKeyUp(TextView widget, Spannable text, int keyCode, KeyEvent event) {
        return false;
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
        if (event.getAction() == MotionEvent.ACTION_SCROLL) {
            final float vscroll;
            final float hscroll;
            if (event.isShiftPressed()) {
                vscroll = 0;
                hscroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            } else {
                vscroll = -event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                hscroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
            }

            boolean handled = false;
            if (hscroll < 0) {
                handled = scrollLeft(widget, (int) Math.ceil(-hscroll));
            } else if (hscroll > 0) {
                handled = scrollRight(widget, (int) Math.ceil(hscroll));
            }
            if (vscroll < 0) {
                handled |= scrollUp(widget, (int) Math.ceil(-vscroll));
            } else if (vscroll > 0) {
                handled |= scrollDown(widget, (int) Math.ceil(vscroll));
            }
            return handled;
        }
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
     * @param keyCode      The key code.
     * @param movementMods The keyboard meta states used for movement.
     * @return True if the event was handled.
     */
    protected boolean handleMovementKey(TextView widget, Spannable buffer,
                                        int keyCode, int movementMods) {
        switch (keyCode) {
            case KeyEvent.KEY_LEFT:
                if (movementMods == 0) {
                    return left(widget, buffer);
                } else if ((movementMods & KeyEvent.META_CTRL_ON) != 0) {
                    return leftWord(widget, buffer);
                } else if ((movementMods & KeyEvent.META_ALT_ON) != 0) {
                    return lineStart(widget, buffer);
                }
                break;

            case KeyEvent.KEY_RIGHT:
                if (movementMods == 0) {
                    return right(widget, buffer);
                } else if ((movementMods & KeyEvent.META_CTRL_ON) != 0) {
                    return rightWord(widget, buffer);
                } else if ((movementMods & KeyEvent.META_ALT_ON) != 0) {
                    return lineEnd(widget, buffer);
                }
                break;

            case KeyEvent.KEY_UP:
                if (movementMods == 0) {
                    return up(widget, buffer);
                } else if ((movementMods & KeyEvent.META_ALT_ON) != 0) {
                    return top(widget, buffer);
                }
                break;

            case KeyEvent.KEY_DOWN:
                if (movementMods == 0) {
                    return down(widget, buffer);
                } else if ((movementMods & KeyEvent.META_ALT_ON) != 0) {
                    return bottom(widget, buffer);
                }
                break;

            case KeyEvent.KEY_PAGE_UP:
                if (movementMods == 0) {
                    return pageUp(widget, buffer);
                } else if ((movementMods & KeyEvent.META_ALT_ON) != 0) {
                    return top(widget, buffer);
                }
                break;

            case KeyEvent.KEY_PAGE_DOWN:
                if (movementMods == 0) {
                    return pageDown(widget, buffer);
                } else if ((movementMods & KeyEvent.META_ALT_ON) != 0) {
                    return bottom(widget, buffer);
                }
                break;

            case KeyEvent.KEY_HOME:
                if (movementMods == 0) {
                    return home(widget, buffer);
                } else if ((movementMods & KeyEvent.META_CTRL_ON) != 0) {
                    return top(widget, buffer);
                }
                break;

            case KeyEvent.KEY_END:
                if (movementMods == 0) {
                    return end(widget, buffer);
                } else if ((movementMods & KeyEvent.META_CTRL_ON) != 0) {
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

    private int getTopLine(TextView widget) {
        return widget.getLayout().getLineForVertical(widget.getScrollY());
    }

    private int getBottomLine(TextView widget) {
        return widget.getLayout().getLineForVertical(widget.getScrollY() + getInnerHeight(widget));
    }

    private int getInnerWidth(TextView widget) {
        return widget.getWidth() - widget.getTotalPaddingLeft() - widget.getTotalPaddingRight();
    }

    private int getInnerHeight(TextView widget) {
        return widget.getHeight() - widget.getTotalPaddingTop() - widget.getTotalPaddingBottom();
    }

    private int getCharacterWidth(TextView widget) {
        return (int) Math.ceil(widget.getPaint().getFontMetricsInt(null));
    }

    private int getScrollBoundsLeft(TextView widget) {
        final Layout layout = widget.getLayout();
        final int topLine = getTopLine(widget);
        final int bottomLine = getBottomLine(widget);
        if (topLine > bottomLine) {
            return 0;
        }
        int left = Integer.MAX_VALUE;
        for (int line = topLine; line <= bottomLine; line++) {
            final int lineLeft = (int) Math.floor(layout.getLineLeft(line));
            if (lineLeft < left) {
                left = lineLeft;
            }
        }
        return left;
    }

    private int getScrollBoundsRight(TextView widget) {
        final Layout layout = widget.getLayout();
        final int topLine = getTopLine(widget);
        final int bottomLine = getBottomLine(widget);
        if (topLine > bottomLine) {
            return 0;
        }
        int right = Integer.MIN_VALUE;
        for (int line = topLine; line <= bottomLine; line++) {
            final int lineRight = (int) Math.ceil(layout.getLineRight(line));
            if (lineRight > right) {
                right = lineRight;
            }
        }
        return right;
    }

    /**
     * Performs a scroll left action.
     * Scrolls left by the specified number of characters.
     *
     * @param widget The text view.
     * @param amount The number of characters to scroll by.  Must be at least 1.
     * @return True if the event was handled.
     * @hide
     */
    protected boolean scrollLeft(TextView widget, int amount) {
        final int minScrollX = getScrollBoundsLeft(widget);
        int scrollX = widget.getScrollX();
        if (scrollX > minScrollX) {
            scrollX = Math.max(scrollX - getCharacterWidth(widget) * amount, minScrollX);
            widget.scrollTo(scrollX, widget.getScrollY());
            return true;
        }
        return false;
    }

    /**
     * Performs a scroll right action.
     * Scrolls right by the specified number of characters.
     *
     * @param widget The text view.
     * @param amount The number of characters to scroll by.  Must be at least 1.
     * @return True if the event was handled.
     * @hide
     */
    protected boolean scrollRight(TextView widget, int amount) {
        final int maxScrollX = getScrollBoundsRight(widget) - getInnerWidth(widget);
        int scrollX = widget.getScrollX();
        if (scrollX < maxScrollX) {
            scrollX = Math.min(scrollX + getCharacterWidth(widget) * amount, maxScrollX);
            widget.scrollTo(scrollX, widget.getScrollY());
            return true;
        }
        return false;
    }

    /**
     * Performs a scroll up action.
     * Scrolls up by the specified number of lines.
     *
     * @param widget The text view.
     * @param amount The number of lines to scroll by.  Must be at least 1.
     * @return True if the event was handled.
     * @hide
     */
    protected boolean scrollUp(TextView widget, int amount) {
        final Layout layout = widget.getLayout();
        final int top = widget.getScrollY();
        int topLine = layout.getLineForVertical(top);
        if (layout.getLineTop(topLine) == top) {
            // If the top line is partially visible, bring it all the way
            // into view; otherwise, bring the previous line into view.
            topLine -= 1;
        }
        if (topLine >= 0) {
            topLine = Math.max(topLine - amount + 1, 0);
            Touch.scrollTo(widget, layout, widget.getScrollX(), layout.getLineTop(topLine));
            return true;
        }
        return false;
    }

    /**
     * Performs a scroll down action.
     * Scrolls down by the specified number of lines.
     *
     * @param widget The text view.
     * @param amount The number of lines to scroll by.  Must be at least 1.
     * @return True if the event was handled.
     * @hide
     */
    protected boolean scrollDown(TextView widget, int amount) {
        final Layout layout = widget.getLayout();
        final int innerHeight = getInnerHeight(widget);
        final int bottom = widget.getScrollY() + innerHeight;
        int bottomLine = layout.getLineForVertical(bottom);
        if (layout.getLineTop(bottomLine + 1) < bottom + 1) {
            // Less than a pixel of this line is out of view,
            // so we must have tried to make it entirely in view
            // and now want the next line to be in view instead.
            bottomLine += 1;
        }
        final int limit = layout.getLineCount() - 1;
        if (bottomLine <= limit) {
            bottomLine = Math.min(bottomLine + amount - 1, limit);
            Touch.scrollTo(widget, layout, widget.getScrollX(),
                    layout.getLineTop(bottomLine + 1) - innerHeight);
            return true;
        }
        return false;
    }

    /**
     * Performs a scroll page up action.
     * Scrolls up by one page.
     *
     * @param widget The text view.
     * @return True if the event was handled.
     * @hide
     */
    protected boolean scrollPageUp(TextView widget) {
        final Layout layout = widget.getLayout();
        final int top = widget.getScrollY() - getInnerHeight(widget);
        int topLine = layout.getLineForVertical(top);
        if (topLine >= 0) {
            Touch.scrollTo(widget, layout, widget.getScrollX(), layout.getLineTop(topLine));
            return true;
        }
        return false;
    }

    /**
     * Performs a scroll page up action.
     * Scrolls down by one page.
     *
     * @param widget The text view.
     * @return True if the event was handled.
     * @hide
     */
    protected boolean scrollPageDown(TextView widget) {
        final Layout layout = widget.getLayout();
        final int innerHeight = getInnerHeight(widget);
        final int bottom = widget.getScrollY() + innerHeight + innerHeight;
        int bottomLine = layout.getLineForVertical(bottom);
        if (bottomLine <= layout.getLineCount() - 1) {
            Touch.scrollTo(widget, layout, widget.getScrollX(),
                    layout.getLineTop(bottomLine + 1) - innerHeight);
            return true;
        }
        return false;
    }

    /**
     * Performs a scroll to top action.
     * Scrolls to the top of the document.
     *
     * @param widget The text view.
     * @return True if the event was handled.
     * @hide
     */
    protected boolean scrollTop(TextView widget) {
        final Layout layout = widget.getLayout();
        if (getTopLine(widget) >= 0) {
            Touch.scrollTo(widget, layout, widget.getScrollX(), layout.getLineTop(0));
            return true;
        }
        return false;
    }

    /**
     * Performs a scroll to bottom action.
     * Scrolls to the bottom of the document.
     *
     * @param widget The text view.
     * @return True if the event was handled.
     * @hide
     */
    protected boolean scrollBottom(TextView widget) {
        final Layout layout = widget.getLayout();
        final int lineCount = layout.getLineCount();
        if (getBottomLine(widget) <= lineCount - 1) {
            Touch.scrollTo(widget, layout, widget.getScrollX(),
                    layout.getLineTop(lineCount) - getInnerHeight(widget));
            return true;
        }
        return false;
    }

    /**
     * Performs a scroll to line start action.
     * Scrolls to the start of the line.
     *
     * @param widget The text view.
     * @return True if the event was handled.
     * @hide
     */
    protected boolean scrollLineStart(TextView widget) {
        final int minScrollX = getScrollBoundsLeft(widget);
        int scrollX = widget.getScrollX();
        if (scrollX > minScrollX) {
            widget.scrollTo(minScrollX, widget.getScrollY());
            return true;
        }
        return false;
    }

    /**
     * Performs a scroll to line end action.
     * Scrolls to the end of the line.
     *
     * @param widget The text view.
     * @return True if the event was handled.
     * @hide
     */
    protected boolean scrollLineEnd(TextView widget) {
        final int maxScrollX = getScrollBoundsRight(widget) - getInnerWidth(widget);
        int scrollX = widget.getScrollX();
        if (scrollX < maxScrollX) {
            widget.scrollTo(maxScrollX, widget.getScrollY());
            return true;
        }
        return false;
    }
}
