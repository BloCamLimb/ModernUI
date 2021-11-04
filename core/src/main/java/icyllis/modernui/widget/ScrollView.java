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

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.math.Rect;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewConfiguration;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;

/**
 * A view group that allows the view hierarchy placed within it to be scrolled.
 * Scroll view may have only one direct child placed within it.
 * To add multiple views within the scroll view, make
 * the direct child you add a view group, for example {@link LinearLayout}, and
 * place additional views within that LinearLayout.
 */
public class ScrollView extends FrameLayout {

    private final Scroller mScroller = new Scroller();

    public ScrollView() {
        setVerticalScrollBarEnabled(true);
        setVerticalScrollbarThumbDrawable(new Drawable() {
            private int alpha = 255;

            @Override
            public void draw(@Nonnull Canvas canvas) {
                Paint paint = Paint.take();
                paint.setRGBA(84, 190, 196, (int) (alpha * 0.5));
                Rect bounds = getBounds();
                canvas.drawRoundRect(bounds.left, bounds.top + 1, bounds.right - 1, bounds.bottom - 1, bounds.width() / 2f - 0.5f, paint);
            }

            @Override
            public void setAlpha(int alpha) {
                this.alpha = alpha;
            }
        });
        setVerticalScrollbarTrackDrawable(new Drawable() {
            private int alpha = 255;

            @Override
            public void draw(@Nonnull Canvas canvas) {
                Paint paint = Paint.take();
                paint.setRGBA(128, 128, 128, (int) (alpha * 0.75));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3);
                Rect bounds = getBounds();
                canvas.drawRoundRect(bounds.left, bounds.top + 1, bounds.right - 1, bounds.bottom - 1, bounds.width() / 2f - 0.5f, paint);
            }

            @Override
            public void setAlpha(int alpha) {
                this.alpha = alpha;
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // we must specify max scroll amount
        // scroller may not callback this method
    }

    @Override
    public void addView(@Nonnull View child) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child);
    }

    @Override
    public void addView(@Nonnull View child, int index) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child, index);
    }

    @Override
    public void addView(@Nonnull View child, @Nonnull ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child, params);
    }

    @Override
    public void addView(@Nonnull View child, int index, @Nonnull ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child, index, params);
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mScrollX = mScroller.getCurrX();
            mScrollY = mScroller.getCurrY();
            awakenScrollBars();
        }
    }

    /**
     * <p>The scroll range of a scroll view is the overall height of all of its
     * children.</p>
     */
    @Override
    protected int computeVerticalScrollRange() {
        final int count = getChildCount();
        final int contentHeight = getHeight();
        if (count == 0) {
            return contentHeight;
        }

        int scrollRange = getChildAt(0).getBottom();
        final int scrollY = mScrollY;
        final int overscrollBottom = Math.max(0, scrollRange - contentHeight);
        if (scrollY < 0) {
            scrollRange -= scrollY;
        } else if (scrollY > overscrollBottom) {
            scrollRange += scrollY - overscrollBottom;
        }

        return scrollRange;
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_SCROLL) {
            float delta = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            final int height = getHeight();
            final int bottom = getChildAt(0).getHeight();
            final int maxY = Math.max(0, bottom - height);
            final int scrollY = mScrollY;
            int dy = Math.round(delta * -60.0f * ViewConfiguration.get().getViewScale());
            dy = Math.max(0, Math.min(mScroller.getFinalY() + dy, maxY)) - scrollY;
            mScroller.startScroll(mScrollX, scrollY, 0, dy);
            invalidate();
            return dy > 0;
        }
        return super.onGenericMotionEvent(event);
    }

    /*@Override
    protected void onScrollBarClicked(boolean vertical, float scrollDelta) {
        scrollController.scrollBy(scrollDelta);
    }

    @Override
    protected void onScrollBarDragged(boolean vertical, float scrollDelta) {
        scrollController.scrollBy(scrollDelta);
        scrollController.abortAnimation();
    }

    @Override
    public void onScrollAmountUpdated(ScrollController controller, float amount) {
        mScrollY = (int) amount;
        if (getVerticalScrollBar() != null) {
            getVerticalScrollBar().setParameters(scrollRange, mScrollY, getHeight());
        }
        invalidate();
    }*/
}
