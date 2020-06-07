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

package icyllis.modernui.ui.master;

import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.ui.test.IViewRect;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ViewGroup extends View implements IViewParent {

    @Nonnull
    private List<View> activeViews = new ArrayList<>();

    @Nonnull
    private List<View> allViews = new ArrayList<>();

    public ViewGroup() {

    }

    public void addActiveViewToPool(@Nonnull View view) {
        if (!activeViews.contains(view)) {
            activeViews.add(view);
            allViews.add(view);
            view.assignParent(this);
        }
    }

    protected void removeView(@Nonnull View view) {
        activeViews.remove(view);
        allViews.remove(view);
    }

    protected void removeAllViews() {
        activeViews.clear();
        allViews.clear();
    }

    protected void deactivateAll() {
        activeViews.clear();
    }

    protected void activateView(@Nonnull View view) {
        if (allViews.contains(view) && !activeViews.contains(view)) {
            activeViews.add(view);
        }
    }

    @Nonnull
    protected List<View> getActiveViews() {
        return activeViews;
    }

    @Nonnull
    protected List<View> getAllViews() {
        return allViews;
    }

    /*protected void deactivateOnlyAdd(@Nonnull View view) {
        if (inactiveViews == null) {
            inactiveViews = new ArrayList<>();
        }
        inactiveViews.addAll(activeViews);
        inactiveViews.clear();
        activeViews.add(view);
    }

    protected void activateView(int id) {
        if (inactiveViews == null) {
            return;
        }
        Optional<View> o = inactiveViews.stream().filter(v -> v.getId() == id).findFirst();
        if (o.isPresent()) {
            View v = o.get();
            activeViews.add(v);
            inactiveViews.remove(v);
        }
    }*/

    @Override
    protected void dispatchLayout() {
        super.dispatchLayout();
        IViewRect prev = this;
        for (View view : activeViews) {
            view.layout(prev);
            prev = view;
        }
    }

    @Override
    protected void dispatchDraw(@Nonnull Canvas canvas, float time) {
        super.dispatchDraw(canvas, time);
        boolean s = (getTranslationX() != 0 || getTranslationY() != 0);
        if (s) {
            canvas.save();
            canvas.translate(-getTranslationX(), -getTranslationY());
        }
        for (View view : activeViews) {
            view.draw(canvas, time);
        }
        if (s) {
            canvas.restore();
        }
    }

    @Override
    protected boolean dispatchMouseHover(double mouseX, double mouseY) {
        double tmx = mouseX + getTranslationX();
        double tmy = mouseY + getTranslationY();
        for (int i = activeViews.size() - 1; i >= 0; i--) {
            if (activeViews.get(i).updateMouseHover(tmx, tmy)) {
                return true;
            }
        }
        return super.dispatchMouseHover(mouseX, mouseY);
    }

    @Override
    public float getTranslationX() {
        return 0;
    }

    @Override
    public float getTranslationY() {
        return 0;
    }

    @Override
    public void relayoutChild(@Nonnull View view) {
        int i = activeViews.indexOf(view);
        if (i > 0) {
            view.layout(activeViews.get(i - 1));
        } else {
            view.layout(this);
        }
    }
}
