/*
 * Modern UI.
 * Copyright (C) 2021-2025 BloCamLimb. All rights reserved.
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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   Copyright (C) 2006 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package icyllis.modernui.widget;

import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.util.AttributeSet;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import org.jetbrains.annotations.ApiStatus;

/**
 * A layout that lets you specify exact locations (x/y coordinates) of its
 * children. Absolute layouts are less flexible and harder to maintain than
 * other types of layouts without absolute positioning.
 *
 * @since 2.0
 */
@ApiStatus.Obsolete(since = "3.12")
@SuppressWarnings("unused")
public class AbsoluteLayout extends ViewGroup {

    public AbsoluteLayout(Context context) {
        super(context);
    }

    public AbsoluteLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, null);
    }

    public AbsoluteLayout(Context context, @Nullable AttributeSet attrs,
                          @Nullable @AttrRes ResourceId defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    public AbsoluteLayout(Context context, @Nullable AttributeSet attrs,
                          @Nullable @AttrRes ResourceId defStyleAttr,
                          @Nullable @StyleRes ResourceId defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        int maxHeight = 0;
        int maxWidth = 0;

        // Find out how big everyone wants to be
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        // Find rightmost and bottom-most child
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

        // Account for padding too
        maxWidth += mPaddingLeft + mPaddingRight;
        maxHeight += mPaddingTop + mPaddingBottom;

        // Check against minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                resolveSizeAndState(maxHeight, heightMeasureSpec, 0));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();

                int childLeft = mPaddingLeft + lp.x;
                int childTop = mPaddingTop + lp.y;
                child.layout(childLeft, childTop,
                        childLeft + child.getMeasuredWidth(),
                        childTop + child.getMeasuredHeight());
            }
        }
    }

    @NonNull
    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(@NonNull ViewGroup.LayoutParams params) {
        if (params instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) params);
        }
        return new LayoutParams(params);
    }

    /**
     * Returns a set of layout parameters with a width of
     * {@link ViewGroup.LayoutParams#WRAP_CONTENT},
     * a height of {@link ViewGroup.LayoutParams#WRAP_CONTENT}
     * and with the coordinates (0, 0).
     */
    @NonNull
    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0, 0);
    }

    @Override
    protected boolean checkLayoutParams(@Nullable ViewGroup.LayoutParams params) {
        return params instanceof LayoutParams;
    }

    /**
     * Per-child layout information associated with AbsoluteLayout.
     */
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

        public LayoutParams(@NonNull ViewGroup.LayoutParams source) {
            super(source);
        }

        /**
         * Copy constructor. Clones the width, height, x and y of the source.
         *
         * @param source The layout params to copy from.
         */
        public LayoutParams(@NonNull LayoutParams source) {
            super(source);
            this.x = source.x;
            this.y = source.y;
        }
    }
}
