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

package icyllis.modernui.view;

import icyllis.modernui.math.MathUtil;

import javax.annotation.Nonnull;

/**
 * Measure specification encapsulates the layout requirements passed from parent to child.
 * Each MeasureSpec represents a requirement for either the width or the height.
 * A MeasureSpec is comprised of a size and a mode.
 */
@SuppressWarnings("unused")
public class MeasureSpec {

    private static final int MODE_SHIFT = 30;
    private static final int MODE_MASK  = 0x3 << MODE_SHIFT;

    /**
     * Creates a measure specification based on the given size and mode.
     *
     * @param size the size of the measure specification
     * @param mode the mode of the measure specification
     * @return the measure specification based on size and mode
     */
    public static int makeMeasureSpec(int size, @Nonnull Mode mode) {
        size = MathUtil.clamp(size, 0, (1 << MODE_SHIFT) - 1);
        return (size & ~MODE_MASK) | (mode.ordinal() << MODE_SHIFT);
    }

    /**
     * Extracts the size from the supplied measure specification.
     *
     * @param measureSpec the measure specification to extract from
     * @return the size in pixels defined in the supplied measure specification
     */
    public static int getSize(int measureSpec) {
        return (measureSpec & ~MODE_MASK);
    }

    /**
     * Extracts the mode from the supplied measure specification.
     *
     * @param measureSpec the measure specification to extract from
     * @return the measure mode, see {@link Mode}
     */
    @Nonnull
    public static Mode getMode(int measureSpec) {
        return Mode.values()[measureSpec >>> MODE_SHIFT];
    }

    /**
     * Measure specification modes.
     */
    public enum Mode {
        /**
         * The parent has not imposed any constraint on the child.
         * It can be whatever size it wants.
         */
        UNSPECIFIED,

        /**
         * The parent has determined an exact size for the child.
         * The child is going to be given those bounds regardless
         * of how big it wants to be.
         */
        EXACTLY,

        /**
         * The child can be as large as it wants up to the specified size.
         */
        AT_MOST;

        public boolean isUnspecified() {
            return this == UNSPECIFIED;
        }

        public boolean notUnspecified() {
            return this != UNSPECIFIED;
        }

        public boolean isExactly() {
            return this == EXACTLY;
        }

        public boolean notExactly() {
            return this != EXACTLY;
        }

        public boolean isAtMost() {
            return this == AT_MOST;
        }

        public boolean notAtMost() {
            return this != AT_MOST;
        }
    }
}
