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
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.TextView;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A movement method that interprets movement keys by scrolling the text buffer.
 */
@ParametersAreNonnullByDefault
public class ScrollingMovementMethod extends BaseMovementMethod {

    private static final ScrollingMovementMethod sInstance = new ScrollingMovementMethod();

    private ScrollingMovementMethod() {
    }

    public static MovementMethod getInstance() {
        return sInstance;
    }

    @Override
    protected boolean left(TextView widget, Spannable buffer) {
        return scrollLeft(widget, 1);
    }

    @Override
    protected boolean right(TextView widget, Spannable buffer) {
        return scrollRight(widget, 1);
    }

    @Override
    protected boolean up(TextView widget, Spannable buffer) {
        return scrollUp(widget, 1);
    }

    @Override
    protected boolean down(TextView widget, Spannable buffer) {
        return scrollDown(widget, 1);
    }

    @Override
    protected boolean pageUp(TextView widget, Spannable buffer) {
        return scrollPageUp(widget);
    }

    @Override
    protected boolean pageDown(TextView widget, Spannable buffer) {
        return scrollPageDown(widget);
    }

    @Override
    protected boolean top(TextView widget, Spannable buffer) {
        return scrollTop(widget);
    }

    @Override
    protected boolean bottom(TextView widget, Spannable buffer) {
        return scrollBottom(widget);
    }

    @Override
    protected boolean lineStart(TextView widget, Spannable buffer) {
        return scrollLineStart(widget);
    }

    @Override
    protected boolean lineEnd(TextView widget, Spannable buffer) {
        return scrollLineEnd(widget);
    }

    @Override
    protected boolean home(TextView widget, Spannable buffer) {
        return top(widget, buffer);
    }

    @Override
    protected boolean end(TextView widget, Spannable buffer) {
        return bottom(widget, buffer);
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        return Touch.onTouchEvent(widget, buffer, event);
    }

    @Override
    public void onTakeFocus(TextView widget, Spannable text, int dir) {
        Layout layout = widget.getLayout();

        if (layout != null && (dir & View.FOCUS_FORWARD) != 0) {
            widget.scrollTo(widget.getScrollX(),
                    layout.getLineTop(0));
        }
        if (layout != null && (dir & View.FOCUS_BACKWARD) != 0) {
            int padding = widget.getTotalPaddingTop() +
                    widget.getTotalPaddingBottom();
            int line = layout.getLineCount() - 1;

            widget.scrollTo(widget.getScrollX(),
                    layout.getLineTop(line + 1) -
                            (widget.getHeight() - padding));
        }
    }
}
