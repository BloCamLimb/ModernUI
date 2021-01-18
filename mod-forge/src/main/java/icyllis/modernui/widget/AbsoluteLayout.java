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

import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A layout that lets you specify exact locations (x/y coordinates) of its
 * children. Absolute layouts are less flexible and harder to maintain than
 * other types of layouts without absolute positioning.
 *
 * @since 2.0.1
 */
@SuppressWarnings("unused")
public class AbsoluteLayout extends ViewGroup {

    public AbsoluteLayout() {

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        int maxWidth = 0;
        int maxHeight = 0;

        measureChildren(widthMeasureSpec, heightMeasureSpec);

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();

                int childRight = lp.x + child.getMeasuredWidth();
                int childBottom = lp.y + child.getMeasuredHeight();

                maxWidth = Math.max(maxWidth, childRight);
                maxHeight = Math.max(maxHeight, childBottom);
            }
        }

        maxWidth = Math.max(maxWidth, getMinWidth());
        maxHeight = Math.max(maxHeight, getMinHeight());

        setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec),
                resolveSize(maxHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed) {
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();

                child.layout(lp.x, lp.y,
                        lp.x + child.getMeasuredWidth(),
                        lp.y + child.getMeasuredHeight());
            }
        }
    }

    @Nonnull
    @Override
    protected ViewGroup.LayoutParams convertLayoutParams(@Nonnull ViewGroup.LayoutParams params) {
        if (params instanceof LayoutParams) {
            return params;
        }
        return new LayoutParams(params);
    }

    @Nonnull
    @Override
    protected ViewGroup.LayoutParams createDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0, 0);
    }

    @Override
    protected boolean checkLayoutParams(@Nullable ViewGroup.LayoutParams params) {
        return params instanceof LayoutParams;
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        /**
         * The horizontal, or X, location of the child within the view group.
         */
        public int x;
        /**
         * The vertical, or Y, location of the child within the view group.
         */
        public int y;

        /**
         * Creates a new set of layout parameters with the specified width,
         * height and location.
         *
         * @param width  the width, either {@link #MATCH_PARENT},
         *               {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param height the height, either {@link #MATCH_PARENT},
         *               {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param x      the X location of the child
         * @param y      the Y location of the child
         */
        public LayoutParams(int width, int height, int x, int y) {
            super(width, height);
            this.x = x;
            this.y = y;
        }

        public LayoutParams(@Nonnull ViewGroup.LayoutParams source) {
            super(source);
        }

        /**
         * Copy constructor. Clones the width, height, x and y of the source.
         *
         * @param source The layout params to copy from.
         */
        public LayoutParams(LayoutParams source) {
            super(source);
            this.x = source.x;
            this.y = source.y;
        }

        @Nonnull
        @Override
        public LayoutParams copy() {
            return new LayoutParams(this);
        }
    }
}
