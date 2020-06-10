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

package icyllis.modernui.ui.layout;

import icyllis.modernui.ui.master.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FrameLayout extends ViewGroup {

    @Nonnull
    @Override
    protected ViewGroup.LayoutParams convertLayoutParams(@Nonnull ViewGroup.LayoutParams params) {
        if (params instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) params);
        } else if (params instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) params);
        }
        return new LayoutParams(params);
    }

    @Nonnull
    @Override
    protected ViewGroup.LayoutParams createDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected boolean checkLayoutParams(@Nullable ViewGroup.LayoutParams params) {
        return params instanceof LayoutParams;
    }

    public static class LayoutParams extends MarginLayoutParams {

        /**
         * The gravity to apply with the View to which these layout parameters
         * are associated. Default value is TOP_LEFT
         */
        public int gravity = Gravity.TOP | Gravity.LEFT;

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
            super(width, height);
        }

        /**
         * Creates a new set of layout parameters with the specified width
         * and height.
         *
         * @param width   either {@link #WRAP_CONTENT},
         *                {@link #MATCH_PARENT}, or a fixed value
         * @param height  either {@link #WRAP_CONTENT},
         *                {@link #MATCH_PARENT}, or a fixed value
         * @param gravity the gravity
         */
        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(@Nonnull ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(@Nonnull ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        /**
         * Copy constructor. Clones the width, height, margin values, and
         * gravity of the source.
         *
         * @param source the layout params to copy from.
         */
        public LayoutParams(@Nonnull LayoutParams source) {
            super(source);

            gravity = source.gravity;
        }

        @Nonnull
        @Override
        public LayoutParams copy() {
            return new LayoutParams(this);
        }
    }
}
