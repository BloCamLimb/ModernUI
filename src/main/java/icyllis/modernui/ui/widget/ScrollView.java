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

import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.ui.drawable.ScrollThumbDrawable;
import icyllis.modernui.ui.layout.FrameLayout;
import icyllis.modernui.ui.master.View;

import javax.annotation.Nonnull;

/**
 * Vertical scroll view
 */
public class ScrollView extends FrameLayout implements Scroller.IListener {

    private float scrollAmount;

    /*private final Scroller  scroller  = new Scroller(this);
    private final ScrollBar scrollBar = new ScrollBar(this);*/

    private int scrollRange;

    public ScrollView() {
        /*scrollBar.setTrackDrawable(new Drawable() {
            @Override
            public void draw(@Nonnull Canvas canvas) {
                canvas.moveTo(this);
                canvas.setColor(16, 16, 16, 40);
                canvas.drawRect(0, 0, getWidth(), getHeight());
            }
        });
        scrollBar.setThumbDrawable(new ScrollThumbDrawable());*/
    }

    @Override
    public float getScrollY() {
        return scrollAmount;
    }

    @Override
    protected void dispatchDraw(@Nonnull Canvas canvas) {
        //scroller.update(canvas.getDrawingTime());
        canvas.clipVertical(this);
        super.dispatchDraw(canvas);
        canvas.clipEnd();
        //scrollBar.draw(canvas);
    }

    @Override
    protected void onLayout(boolean changed) {
        super.onLayout(changed);
        //TODO use params
        //scrollBar.layout(getRight() - 6, getTop() + 1, getRight() - 1, getBottom() - 1);
        if (getChildCount() > 0) {
            scrollRange = getChildAt(0).getHeight();
        } else {
            scrollRange = 0;
        }
        float maxScroll = scrollRange - getHeight();
        // must specify max scroll amount
        //scroller.setMaxValue(Math.max(maxScroll, 0));
        // refresh gui scale factor
        //scroller.scrollBy(0);
        // scroller may not callback this method
        //scrollBar.setParameters(scrollRange, scrollAmount, getHeight(), true);
    }

    @Override
    protected boolean onMouseScrolled(double mouseX, double mouseY, double amount) {
        //scroller.scrollBy((int) Math.round(amount * -20.0f));
        return true;
    }

    /*@Override
    public void onScrollBarClicked(ScrollBar scrollBar, float scrollDelta) {
        scroller.scrollBy(scrollDelta);
    }

    @Override
    public void onScrollBarDragged(ScrollBar scrollBar, float scrollDelta) {
        scroller.scrollBy(scrollDelta);
        scroller.abortAnimation();
    }*/

    @Override
    public void onScrollAmountUpdated(Scroller scroller, float scrollAmount) {
        this.scrollAmount = scrollAmount;
        //scrollBar.setParameters(scrollRange, scrollAmount, getHeight(), true);
    }
}
