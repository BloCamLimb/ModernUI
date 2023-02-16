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
import icyllis.modernui.text.Layout.Alignment;
import icyllis.modernui.text.NoCopySpan;
import icyllis.modernui.text.Spannable;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.ViewConfiguration;
import icyllis.modernui.widget.TextView;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class Touch {

    private Touch() {
    }

    /**
     * Scrolls the specified widget to the specified coordinates, except
     * constrains the X scrolling position to the horizontal regions of
     * the text that will be visible after scrolling to the specified
     * Y position.
     */
    public static void scrollTo(TextView widget, Layout layout, int x, int y) {
        final int horizontalPadding = widget.getTotalPaddingLeft() + widget.getTotalPaddingRight();
        final int availableWidth = widget.getWidth() - horizontalPadding;

        final int top = layout.getLineForVertical(y);
        Layout.Alignment a = layout.getParagraphAlignment(top);
        boolean ltr = layout.getParagraphDirection(top) > 0;

        int left, right;
        if (widget.isHorizontallyScrollable()) {
            final int verticalPadding = widget.getTotalPaddingTop() + widget.getTotalPaddingBottom();
            final int bottom = layout.getLineForVertical(y + widget.getHeight() - verticalPadding);

            left = Integer.MAX_VALUE;
            right = 0;

            for (int i = top; i <= bottom; i++) {
                left = (int) Math.min(left, layout.getLineLeft(i));
                right = (int) Math.max(right, layout.getLineRight(i));
            }
        } else {
            left = 0;
            right = availableWidth;
        }

        final int actualWidth = right - left;

        if (actualWidth < availableWidth) {
            if (a == Alignment.ALIGN_CENTER) {
                x = left - ((availableWidth - actualWidth) / 2);
            } else if ((ltr && (a == Alignment.ALIGN_OPPOSITE)) ||
                    (!ltr && (a == Alignment.ALIGN_NORMAL)) ||
                    (a == Alignment.ALIGN_RIGHT)) {
                // align_opposite does NOT mean align_right, we need the paragraph
                // direction to resolve it to left or right
                x = left - (availableWidth - actualWidth);
            } else {
                x = left;
            }
        } else {
            x = Math.min(x, right - availableWidth);
            x = Math.max(x, left);
        }

        widget.scrollTo(x, y);
    }

    /**
     * Handles touch events for dragging.  You may want to do other actions
     * like moving the cursor on touch as well.
     */
    public static boolean onTouchEvent(TextView widget, Spannable buffer,
                                       MotionEvent event) {
        List<DragState> ds;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN -> {
                ds = buffer.getSpans(0, buffer.length(), DragState.class);
                for (DragState d : ds) {
                    buffer.removeSpan(d);
                }
                buffer.setSpan(new DragState(event.getX(), event.getY(),
                                widget.getScrollX(), widget.getScrollY()),
                        0, 0, Spannable.SPAN_MARK_MARK);
                return true;
            }
            case MotionEvent.ACTION_UP -> {
                ds = buffer.getSpans(0, buffer.length(), DragState.class);
                for (DragState d : ds) {
                    buffer.removeSpan(d);
                }
                return !ds.isEmpty() && ds.get(0).mUsed;
            }
            case MotionEvent.ACTION_MOVE -> {
                ds = buffer.getSpans(0, buffer.length(), DragState.class);
                if (!ds.isEmpty()) {
                    if (!ds.get(0).mFarEnough) {
                        int slop = ViewConfiguration.get().getScaledTouchSlop();

                        if (Math.abs(event.getX() - ds.get(0).mX) >= slop ||
                                Math.abs(event.getY() - ds.get(0).mY) >= slop) {
                            ds.get(0).mFarEnough = true;
                        }
                    }

                    if (ds.get(0).mFarEnough) {
                        ds.get(0).mUsed = true;
                        boolean cap = event.isShiftPressed() ||
                                event.isButtonPressed(MotionEvent.BUTTON_PRIMARY) ||
                                TextKeyListener.getMetaState(buffer, KeyEvent.META_SHIFT_ON) == 1;

                        float dx;
                        float dy;
                        if (cap) {
                            // if we're selecting, we want the scroll to go in
                            // the direction of the drag
                            dx = event.getX() - ds.get(0).mX;
                            dy = event.getY() - ds.get(0).mY;
                        } else {
                            dx = ds.get(0).mX - event.getX();
                            dy = ds.get(0).mY - event.getY();
                        }
                        ds.get(0).mX = event.getX();
                        ds.get(0).mY = event.getY();

                        int nx = widget.getScrollX() + (int) dx;
                        int ny = widget.getScrollY() + (int) dy;

                        int padding = widget.getTotalPaddingTop() + widget.getTotalPaddingBottom();
                        Layout layout = widget.getLayout();

                        ny = Math.min(ny, layout.getHeight() - (widget.getHeight() - padding));
                        ny = Math.max(ny, 0);

                        int oldX = widget.getScrollX();
                        int oldY = widget.getScrollY();

                        scrollTo(widget, layout, nx, ny);

                        // If we actually scrolled, then cancel the up action.
                        if (oldX != widget.getScrollX() || oldY != widget.getScrollY()) {
                            widget.cancelLongPress();
                        }

                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * @param buffer The text buffer.
     */
    public static int getInitialScrollX(Spannable buffer) {
        List<DragState> ds = buffer.getSpans(0, buffer.length(), DragState.class);
        return !ds.isEmpty() ? ds.get(0).mScrollX : -1;
    }

    /**
     * @param buffer The text buffer.
     */
    public static int getInitialScrollY(Spannable buffer) {
        List<DragState> ds = buffer.getSpans(0, buffer.length(), DragState.class);
        return !ds.isEmpty() ? ds.get(0).mScrollY : -1;
    }

    private static class DragState implements NoCopySpan {

        private float mX;
        private float mY;
        private final int mScrollX;
        private final int mScrollY;
        private boolean mFarEnough;
        private boolean mUsed;

        private DragState(float x, float y, int scrollX, int scrollY) {
            mX = x;
            mY = y;
            mScrollX = scrollX;
            mScrollY = scrollY;
        }
    }
}
