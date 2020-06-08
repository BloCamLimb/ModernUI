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

import javax.annotation.Nonnull;

/**
 * Measure specification, encapsulates the layout requirements passed from parent to child
 */
public class MeasureSpec {

    private static final int MODE_SHIFT = 30;
    private static final int MODE_MASK = 0x3 << MODE_SHIFT;

    /**
     * Make measure specification based on the size and mode
     *
     * @param size measure size
     * @param mode measure mode
     * @return measure specification
     */
    public static int makeMeasureSpec(int size, @Nonnull Mode mode) {
        if (size < 0 || size > (1 << MODE_SHIFT - 1)) {
            throw new IllegalArgumentException("out of range?");
        }
        return (size & ~MODE_MASK) | (mode.ordinal() << MODE_SHIFT & MODE_MASK);
    }

    /**
     * Extracts the size from the supplied measure specification.
     *
     * @param measureSpec measure specification
     * @return measure size
     */
    public static int getSize(int measureSpec) {
        return (measureSpec & ~MODE_MASK);
    }

    /**
     * Extracts the mode from the supplied measure specification.
     *
     * @param measureSpec measure specification
     * @return measure mode
     */
    public static Mode getMode(int measureSpec) {
        switch (measureSpec & MODE_MASK) {
            case 0:
                return Mode.UNSPECIFIED;
            case 1 << MODE_SHIFT:
                return Mode.EXACTLY;
            case 2 << MODE_SHIFT:
                return Mode.AT_MOST;
        }
        throw new IllegalStateException("unknown mode?");
    }

    /**
     * Measure specification mode
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
        AT_MOST

    }
}
