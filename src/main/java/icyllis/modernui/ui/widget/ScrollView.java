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
import icyllis.modernui.ui.master.*;

import javax.annotation.Nonnull;

/**
 * Vertical scroll view with relatively good performance
 */
public class ScrollView extends ViewGroup {

    private float scrollAmount = 0.0f;

    private float maxHeight = 0.0f;

    private final ScrollBar scrollBar = new ScrollBar(this);

    public ScrollView() {

    }

    @Override
    public float getScrollX() {
        return super.getScrollX();
    }

    @Override
    public final float getScrollY() {
        return super.getScrollY() + scrollAmount;
    }

    @Override
    protected void dispatchDraw(@Nonnull Canvas canvas, float time) {
        canvas.clipStart(getLeft(), getTop(), getWidth(), getHeight());
        super.dispatchDraw(canvas, time);
        scrollBar.draw(canvas, time);
        canvas.clipEnd();
    }

    @Override
    protected void onLayout(boolean changed) {

    }

    @Override
    protected boolean dispatchMouseHover(double mouseX, double mouseY) {
        /*if (scrollBar.updateMouseHover(mouseX, mouseY)) {
            return true;
        }*/
        return super.dispatchMouseHover(mouseX, mouseY);
    }

    /*@Override
    protected void onLayout() {
        super.onLayout();

        Optional<View> o = getActiveViews().stream().max(Comparator.comparing(View::getBottom));
        maxHeight = o.map(v -> v.getBottom() - getTop()).orElse(0);

        float v = getHeight();
        float t = maxHeight;
        boolean renderBar = t > v;
        scrollBar.setVisible(renderBar);
        if (renderBar) {
            float p = v / t;
            scrollBar.setBarLength(p);
        }
        updateScrollAmount(scrollAmount);
    }*/

    final void updateScrollAmount(float scrollAmount) {
        this.scrollAmount = scrollAmount;
        float tTop = getTop() + getScrollY();
        float tBottom = getBottom() + getScrollY();
        /*for (View view : getActiveViews()) {
            if (view.getBottom() > tTop && view.getTop() < tBottom) {
                view.setVisibility(Visibility.VISIBLE);
            } else {
                view.setVisibility(Visibility.INVISIBLE);
            }
        }*/
        updateScrollBarOffset();
        UIManager.INSTANCE.refreshMouse();
    }

    public float getScrollPercentage() {
        float max = getMaxScrollAmount();
        if (max == 0) {
            return 0;
        }
        return scrollAmount / max;
    }

    private void updateScrollBarOffset() {
        scrollBar.setBarOffset(getScrollPercentage());
    }

    public float getMaxScrollAmount() {
        return Math.max(0, maxHeight - getHeight());
    }
}
