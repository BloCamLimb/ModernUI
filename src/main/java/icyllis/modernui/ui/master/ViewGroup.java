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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public abstract class ViewGroup extends View implements IViewParent {

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
    protected void dispatchDraw(@Nonnull Canvas canvas, float time) {
        super.dispatchDraw(canvas, time);
        boolean s = (getScrollX() != 0 || getScrollY() != 0);
        if (s) {
            canvas.save();
            canvas.translate(-getScrollX(), -getScrollY());
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
        double tmx = mouseX + getScrollX();
        double tmy = mouseY + getScrollY();
        for (int i = activeViews.size() - 1; i >= 0; i--) {
            if (activeViews.get(i).updateMouseHover(tmx, tmy)) {
                return true;
            }
        }
        return super.dispatchMouseHover(mouseX, mouseY);
    }

    @Override
    public float getScrollX() {
        return 0;
    }

    @Override
    public float getScrollY() {
        return 0;
    }

    /**
     * Create a safe layout params base on the given params to fit to this view group
     * <p>
     * Also see {@link #createDefaultLayoutParams()}
     * Also see {@link #checkLayoutParams(LayoutParams)}
     *
     * @param params layout params to convert
     * @return safe layout params
     */
    @Nonnull
    protected LayoutParams convertLayoutParams(@Nonnull ViewGroup.LayoutParams params) {
        return params;
    }

    /**
     * Create default layout params if child params is null
     * <p>
     * Also see {@link #convertLayoutParams(LayoutParams)}
     * Also see {@link #checkLayoutParams(LayoutParams)}
     *
     * @return layout params
     */
    @Nonnull
    protected LayoutParams createDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    /**
     * Check whether given params fit to this view group
     * <p>
     * Also see {@link #convertLayoutParams(LayoutParams)}
     * Also see {@link #createDefaultLayoutParams()}
     *
     * @param params layout params to check
     * @return params matched
     */
    protected boolean checkLayoutParams(@Nullable LayoutParams params) {
        return params != null;
    }

    /**
     * LayoutParams are used by views to tell their parents how they want to
     * be laid out.
     * <p>
     * The base LayoutParams class just describes how big the view wants to be
     * for both width and height.
     * <p>
     * There are subclasses of LayoutParams for different subclasses of
     * ViewGroup
     */
    public static class LayoutParams {

        /**
         * Special value for width or height, which means
         * views want to be as big as parent view,
         * but not greater than parent
         */
        public static final int MATCH_PARENT = -1;

        /**
         * Special value for width or height, which means
         * views want to be just large enough to fit
         * its own content
         */
        public static final int WRAP_CONTENT = -2;

        /**
         * The width that views want to be.
         * Can be one of MATCH_PARENT or WARP_CONTENT, or exact value
         */
        public int width;

        /**
         * The height that views want to be.
         * Can be one of MATCH_PARENT or WARP_CONTENT, or exact value
         */
        public int height;

        /**
         * Creates a new set of layout parameters with the specified width
         * and height.
         *
         * @param width  either {@link #WRAP_CONTENT},
         *               {@link #MATCH_PARENT}, or a fixed value
         * @param height either {@link #WRAP_CONTENT},
         *               {@link #MATCH_PARENT}, or a fixed value
         */
        public LayoutParams(int width, int height) {
            this.width = width;
            this.height = height;
        }

        /**
         * Copy constructor. Clones the width and height values of the source.
         *
         * @param source the layout params to copy from.
         */
        public LayoutParams(@Nonnull LayoutParams source) {
            width = source.width;
            height = source.height;
        }

        /**
         * Create a new object copied from this params
         *
         * @return copied layout params
         */
        @Nonnull
        public LayoutParams copy() {
            return new LayoutParams(this);
        }

    }

    public static class MarginLayoutParams extends LayoutParams {

        /**
         * The left margin
         */
        public int leftMargin;

        /**
         * The top margin
         */
        public int topMargin;

        /**
         * The right margin
         */
        public int rightMargin;

        /**
         * The bottom margin
         */
        public int bottomMargin;

        /**
         * Creates a new set of layout parameters with the specified width
         * and height.
         *
         * @param width  either {@link #WRAP_CONTENT},
         *               {@link #MATCH_PARENT}, or a fixed value
         * @param height either {@link #WRAP_CONTENT},
         *               {@link #MATCH_PARENT}, or a fixed value
         */
        public MarginLayoutParams(int width, int height) {
            super(width, height);
        }

        /**
         * Sets the margins, and its values should be positive.
         *
         * @param left   the left margin size
         * @param top    the top margin size
         * @param right  the right margin size
         * @param bottom the bottom margin size
         */
        public void setMargins(int left, int top, int right, int bottom) {
            leftMargin = left;
            topMargin = top;
            rightMargin = right;
            bottomMargin = bottom;
        }

        /**
         * Copy constructor. Clones the width, height and margin values of the source.
         *
         * @param source the layout params to copy from.
         */
        public MarginLayoutParams(@Nonnull MarginLayoutParams source) {
            super(source.width, source.height);

            leftMargin = source.leftMargin;
            topMargin = source.topMargin;
            rightMargin = source.rightMargin;
            bottomMargin = source.bottomMargin;
        }

        public MarginLayoutParams(@Nonnull LayoutParams source) {
            super(source);
        }

        @Nonnull
        @Override
        public MarginLayoutParams copy() {
            return new MarginLayoutParams(this);
        }
    }
}
