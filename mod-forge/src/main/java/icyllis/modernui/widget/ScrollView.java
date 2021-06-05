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

import icyllis.modernui.graphics.CanvasForge;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.test.drawable.ScrollThumbDrawable;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewConfig;

import javax.annotation.Nonnull;

/**
 * Vertical scroll view
 */
public class ScrollView extends FrameLayout implements ScrollController.IListener {

    private int scrollRange;
    private int scrollAmount;

    private final ScrollController scrollController = new ScrollController(this);

    public ScrollView() {
        setVerticalScrollBarEnabled(true);
        ScrollBar bar = new ScrollBar();
        bar.setThumbDrawable(new ScrollThumbDrawable());
        bar.setTrackDrawable(new Drawable() {
            @Override
            public void draw(@Nonnull CanvasForge canvas) {
                Paint paint = Paint.take();
                paint.reset();
                paint.setFeatherRadius(0);
                paint.setRGBA(16, 16, 16, 40);
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            }
        });
        setVerticalScrollBar(bar);
    }

    @Override
    public float getScrollY() {
        return scrollAmount;
    }

    @Override
    protected void dispatchDraw(@Nonnull CanvasForge canvas) {
        scrollController.update(canvas.getDrawingTime());
        canvas.clipVertical(this);
        super.dispatchDraw(canvas);
        canvas.clipEnd();
    }

    @Override
    protected void onLayout(boolean changed) {
        super.onLayout(changed);
        scrollRange = getScrollRange();
        // we must specify max scroll amount
        scrollController.setMaxScroll(scrollRange);
        // scroller may not callback this method
        if (getVerticalScrollBar() != null) {
            getVerticalScrollBar().setParameters(scrollRange, scrollAmount, getHeight());
        }
    }

    private int getScrollRange() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            scrollRange = Math.max(0, child.getHeight() - getHeight());
        }
        return scrollRange;
    }

    @Override
    protected boolean onMouseScrolled(double mouseX, double mouseY, double amount) {
        scrollController.scrollBy(Math.round(amount * -20.0f));
        return true;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_SCROLL) {
            float delta = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            return scrollController.scrollBy(Math.round(delta * -60.0f * ViewConfig.sViewScale));
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
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
        scrollAmount = (int) amount;
        if (getVerticalScrollBar() != null) {
            getVerticalScrollBar().setParameters(scrollRange, scrollAmount, getHeight());
        }
    }
}
