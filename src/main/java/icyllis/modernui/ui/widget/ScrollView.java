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
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.ui.drawable.ScrollThumbDrawable;
import icyllis.modernui.ui.layout.FrameLayout;
import icyllis.modernui.ui.master.IViewParent;
import icyllis.modernui.ui.master.UIManager;
import icyllis.modernui.ui.master.ViewGroup;

import javax.annotation.Nonnull;

/**
 * Vertical scroll view with relatively good performance
 */
public class ScrollView extends FrameLayout implements Scroller.ICallback, ScrollBar.ICallback {

    private float scrollAmount;

    private final Scroller  scroller  = new Scroller(this);
    private final ScrollBar scrollBar = new ScrollBar(this);

    private int scrollRange;

    public ScrollView() {
        scrollBar.setTrack(new Drawable() {
            @Override
            public void draw(@Nonnull Canvas canvas) {
                canvas.moveTo(this);
                canvas.setColor(16, 16, 16, 40);
                canvas.drawRect(0, 0, getWidth(), getHeight());
            }
        });
        scrollBar.setThumb(new ScrollThumbDrawable());
    }

    @Override
    public void assignParent(@Nonnull IViewParent parent) {
        super.assignParent(parent);
        scrollBar.assignParent(parent);
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
        scrollBar.draw(canvas);
        canvas.clipEnd();
    }

    @Override
    protected void onLayout(boolean changed) {
        super.onLayout(changed);
        scrollBar.layout(getRight() - 6, getTop() + 1, getRight() - 1, getBottom() - 1);
        if (getChildCount() > 0) {
            scrollRange = getChildAt(0).getHeight();
        } else {
            scrollRange = 0;
        }
        float maxScroll = scrollRange - getHeight();
        scroller.setMaxValue(Math.max(maxScroll, 0));
        scrollBar.setParameters(scrollRange, scrollAmount, getHeight(), true);
    }

    @Override
    protected boolean dispatchUpdateMouseHover(double mouseX, double mouseY) {
        if (scrollBar.updateMouseHover(mouseX, mouseY)) {
            return true;
        }
        return super.dispatchUpdateMouseHover(mouseX, mouseY);
    }

    @Override
    protected boolean onMouseScrolled(double mouseX, double mouseY, double amount) {
        scroller.scrollBy((int) Math.round(amount * -20.0f));
        return true;
    }

    @Override
    public void onClickScrollBar(ScrollBar scrollBar, float scrollDelta) {
        scroller.scrollBy(scrollDelta);
    }

    @Override
    public void onDragScrollBar(ScrollBar scrollBar, float scrollDelta) {
        scroller.scrollBy(scrollDelta);
        scroller.abortAnimation();
    }

    @Override
    public void applyScrollAmount(float scrollAmount) {
        this.scrollAmount = scrollAmount;
        scrollBar.setParameters(scrollRange, scrollAmount, getHeight(), true);
    }
}
