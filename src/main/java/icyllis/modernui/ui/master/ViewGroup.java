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
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.ui.layout.MeasureSpec;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public abstract class ViewGroup extends View implements IViewParent {

    private static final int ARRAY_CAPACITY_INCREMENT = 12;

    // child views
    private View[] children = new View[ARRAY_CAPACITY_INCREMENT];

    // number of valid children in the children array, the rest
    // should be null or not considered as children
    private int childrenCount = 0;

    public ViewGroup() {

    }

    @Override
    protected void dispatchDraw(@Nonnull Canvas canvas) {
        final boolean doTranslate = (getScrollX() != 0 || getScrollY() != 0);
        if (doTranslate) {
            canvas.save();
            canvas.translate(-getScrollX(), -getScrollY());
        }
        final View[] views = children;
        final int count = childrenCount;
        for (int i = 0; i < count; i++) {
            views[i].draw(canvas);
        }
        if (doTranslate) {
            canvas.restore();
        }
    }

    @Override
    public final void layout(int left, int top, int right, int bottom) {
        super.layout(left, top, right, bottom);
    }

    @Override
    protected abstract void onLayout(boolean changed);

    @Override
    final boolean dispatchUpdateMouseHover(double mouseX, double mouseY) {
        final double mx = mouseX + getScrollX();
        final double my = mouseY + getScrollY();
        final View[] views = children;
        boolean anyHovered = false;
        View child;
        for (int i = childrenCount - 1; i >= 0; i--) {
            child = views[i];
            if (!anyHovered && child.updateMouseHover(mx, my)) {
                anyHovered = true;
            } else {
                child.ensureMouseHoverExit();
            }
        }
        return anyHovered;
    }

    @Override
    final void ensureMouseHoverExit() {
        super.ensureMouseHoverExit();
        final View[] views = children;
        final int count = childrenCount;
        for (int i = 0; i < count; i++) {
            views[i].ensureMouseHoverExit();
        }
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
     * Add a child view to the end of array with default layout params
     *
     * @param child child view to add
     */
    public void addView(@Nonnull View child) {
        addView(child, -1);
    }

    /**
     * Add a child view to specified index of array with default layout params
     *
     * @param child child view to add
     * @param index target index
     */
    public void addView(@Nonnull View child, int index) {
        LayoutParams params = child.getLayoutParams();
        if (params == null) {
            params = createDefaultLayoutParams();
        }
        addView(child, index, params);
    }

    /**
     * Add a child view to end of array with specified width and height
     *
     * @param child  child view to add
     * @param width  view layout width
     * @param height view layout height
     */
    public void addView(@Nonnull View child, int width, int height) {
        LayoutParams params = createDefaultLayoutParams();
        params.width = width;
        params.height = height;
        addView(child, -1, params);
    }

    /**
     * Add a child view to the end of array with specified layout params
     *
     * @param child  child view to add
     * @param params layout params of the view
     */
    public void addView(@Nonnull View child, @Nonnull LayoutParams params) {
        addView(child, -1, params);
    }

    /**
     * Add a child view to specified index of array with specified layout params
     *
     * @param child  child view to add
     * @param index  target index
     * @param params layout params of the view
     */
    public void addView(@Nonnull View child, int index, @Nonnull LayoutParams params) {
        addView0(child, index, params);
    }

    private void addView0(@Nonnull final View child, int index, @Nonnull LayoutParams params) {
        if (child.getParent() != null) {
            ModernUI.LOGGER.fatal(UIManager.MARKER,
                    "Failed to add child view {} to {}. The child has already a parent.", child, this);
            return;
        }

        requestLayout();

        if (!checkLayoutParams(params)) {
            params = convertLayoutParams(params);
        }

        child.setLayoutParams(params);

        if (index < 0) {
            index = childrenCount;
        }

        addInArray(child, index);

        child.assignParent(this);
    }

    public View getChildAt(int index) {
        if (index < 0 || index >= childrenCount) {
            return null;
        }
        return children[index];
    }

    public int getChildCount() {
        return childrenCount;
    }

    private void addInArray(@Nonnull View child, int index) {
        View[] views = children;
        final int count = childrenCount;
        final int size = views.length;
        if (index == count) {
            if (size == count) {
                children = new View[size + ARRAY_CAPACITY_INCREMENT];
                System.arraycopy(views, 0, children, 0, size);
                views = children;
            }
            views[childrenCount++] = child;
        } else if (index < count) {
            if (size == count) {
                children = new View[size + ARRAY_CAPACITY_INCREMENT];
                System.arraycopy(views, 0, children, 0, index);
                System.arraycopy(views, index, children, index + 1, count - index);
                views = children;
            } else {
                System.arraycopy(views, index, views, index + 1, count - index);
            }
            views[index] = child;
            childrenCount++;
        } else {
            throw new IndexOutOfBoundsException("index=" + index + " count=" + count);
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    final <T extends View> T findViewTraversal(int id) {
        if (id == getId()) {
            return (T) this;
        }

        final View[] views = children;
        final int count = childrenCount;

        View view;
        for (int i = 0; i < count; i++) {
            view = views[i];

            view = view.findViewTraversal(id);

            if (view != null) {
                return (T) view;
            }
        }

        return null;
    }

    /**
     * Ask one of the children of this view to measure itself, taking into
     * account both the MeasureSpec requirements for this view and its padding
     * and margins. The child must have MarginLayoutParams The heavy lifting is
     * done in getChildMeasureSpec.
     *
     * @param child            The child to measure
     * @param parentWidthSpec  The width requirements for this view
     * @param widthUsed        Extra space that has been used up by the parent
     *                         horizontally (possibly by other children of the parent)
     * @param parentHeightSpec The height requirements for this view
     * @param heightUsed       Extra space that has been used up by the parent
     *                         vertically (possibly by other children of the parent)
     */
    protected void measureChildWithMargins(@Nonnull View child,
                                           int parentWidthSpec, int widthUsed,
                                           int parentHeightSpec, int heightUsed) {
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        int childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec,
                lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width);
        int childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec,
                lp.topMargin + lp.bottomMargin
                        + heightUsed, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    /**
     * Does the hard part of measureChildren: figuring out the MeasureSpec to
     * pass to a particular child. This method figures out the right MeasureSpec
     * for one dimension (height or width) of one child view.
     * <p>
     * The goal is to combine information from our MeasureSpec with the
     * LayoutParams of the child to get the best possible results. For example,
     * if the this view knows its size (because its MeasureSpec has a mode of
     * EXACTLY), and the child has indicated in its LayoutParams that it wants
     * to be the same size as the parent, the parent should ask the child to
     * layout given an exact size.
     *
     * @param spec           The requirements for this view
     * @param padding        The padding of this view for the current dimension and
     *                       margins, if applicable
     * @param childDimension How big the child wants to be in the current
     *                       dimension
     * @return a MeasureSpec integer for the child
     */
    public static int getChildMeasureSpec(int spec, int padding, int childDimension) {
        int specSize = MeasureSpec.getSize(spec);

        int size = Math.max(0, specSize - padding);

        int resultSize = 0;
        MeasureSpec.Mode resultMode = MeasureSpec.Mode.UNSPECIFIED;

        switch (MeasureSpec.getMode(spec)) {
            // Parent has imposed an exact size on us
            case EXACTLY:
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.Mode.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    // Child wants to be our size. So be it.
                    resultSize = size;
                    resultMode = MeasureSpec.Mode.EXACTLY;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    // Child wants to determine its own size. It can't be
                    // bigger than us.
                    resultSize = size;
                    resultMode = MeasureSpec.Mode.AT_MOST;
                }
                break;

            // Parent has imposed a maximum size on us
            case AT_MOST:
                if (childDimension >= 0) {
                    // Child wants a specific size... so be it
                    resultSize = childDimension;
                    resultMode = MeasureSpec.Mode.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    // Child wants to be our size, but our size is not fixed.
                    // Constrain child to not be bigger than us.
                    resultSize = size;
                    resultMode = MeasureSpec.Mode.AT_MOST;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    // Child wants to determine its own size. It can't be
                    // bigger than us.
                    resultSize = size;
                    resultMode = MeasureSpec.Mode.AT_MOST;
                }
                break;

            // Parent asked to see how big we want to be
            case UNSPECIFIED:
                if (childDimension >= 0) {
                    // Child wants a specific size... let him have it
                    resultSize = childDimension;
                    resultMode = MeasureSpec.Mode.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    // Child wants to be our size... find out how big it should
                    // be
                    resultSize = size;
                    resultMode = MeasureSpec.Mode.UNSPECIFIED;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    // Child wants to determine its own size.... find out how
                    // big it should be
                    resultSize = size;
                    resultMode = MeasureSpec.Mode.UNSPECIFIED;
                }
                break;
        }

        return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
    }

    @Override
    protected void tick(int ticks) {
        final View[] views = children;
        final int count = childrenCount;
        for (int i = 0; i < count; i++) {
            views[i].tick(ticks);
        }
    }

    /**
     * Create a safe layout params base on the given params to fit to this view group
     * <p>
     * See also {@link #createDefaultLayoutParams()}
     * See also {@link #checkLayoutParams(LayoutParams)}
     *
     * @param params the layout params to convert
     * @return safe layout params
     */
    @Nonnull
    protected LayoutParams convertLayoutParams(@Nonnull ViewGroup.LayoutParams params) {
        return params;
    }

    /**
     * Create default layout params if child params is null
     * <p>
     * See also {@link #convertLayoutParams(LayoutParams)}
     * See also {@link #checkLayoutParams(LayoutParams)}
     *
     * @return default layout params
     */
    @Nonnull
    protected LayoutParams createDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    /**
     * Check whether given params fit to this view group
     * <p>
     * See also {@link #convertLayoutParams(LayoutParams)}
     * See also {@link #createDefaultLayoutParams()}
     *
     * @param params layout params to check
     * @return if params matched to this view group
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
