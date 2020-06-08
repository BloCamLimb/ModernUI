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

import javax.annotation.Nullable;

public class FrameLayout extends ViewGroup {

    private static final int DEFAULT_CHILD_GRAVITY = Gravity.TOP | Gravity.START;

    @Override
    protected boolean checkLayoutParams(@Nullable ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        /**
         * The gravity to apply with the View to which these layout parameters
         * are associated.
         */
        public int gravity = DEFAULT_CHILD_GRAVITY;

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
    }
}
