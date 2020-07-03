/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.widget;

import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.ui.drawable.ScrollThumbDrawable;
import icyllis.modernui.ui.layout.FrameLayout;
import icyllis.modernui.ui.master.View;

import javax.annotation.Nonnull;

/**
 * Vertical scroll view
 */
public class ScrollView extends FrameLayout implements Scroller.IListener {

    private int   scrollRange;
    private float scrollAmount;

    private final Scroller scroller = new Scroller(this);

    public ScrollView() {
        setVerticalScrollBarEnabled(true);
        runVerticalScrollBar(bar -> bar.setThumbDrawable(new ScrollThumbDrawable()));
    }

    @Override
    public float getScrollY() {
        return scrollAmount;
    }

    @Override
    protected void dispatchDraw(@Nonnull Canvas canvas) {
        scroller.update(canvas.getDrawingTime());
        canvas.clipVertical(this);
        super.dispatchDraw(canvas);
        canvas.clipEnd();
    }

    @Override
    protected void onLayout(boolean changed) {
        super.onLayout(changed);
        scrollRange = getScrollRange();
        // we must specify max scroll amount
        scroller.setMaxScroll(scrollRange);
        // scroller may not callback this method
        runVerticalScrollBar(bar -> bar.setParameters(scrollRange, scrollAmount, getHeight()));
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
        scroller.scrollBy(Math.round(amount * -20.0f));
        return true;
    }

    @Override
    protected void onScrollBarClicked(boolean vertical, float scrollDelta) {
        scroller.scrollBy(scrollDelta);
    }

    @Override
    protected void onScrollBarDragged(boolean vertical, float scrollDelta) {
        scroller.scrollBy(scrollDelta);
        scroller.abortAnimation();
    }

    @Override
    public void onScrollAmountUpdated(Scroller scroller, float amount) {
        scrollAmount = amount;
        runVerticalScrollBar(bar -> bar.setParameters(scrollRange, scrollAmount, getHeight()));
    }
}
