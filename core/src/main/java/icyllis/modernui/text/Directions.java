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

package icyllis.modernui.text;

/**
 * Stores information about bidirectional (left-to-right or right-to-left)
 * text within the layout of a line.
 */
public class Directions {

    // 26 bits
    static final int RUN_LENGTH_MASK = 0x03ffffff;
    static final int RUN_LEVEL_SHIFT = 26;
    static final int RUN_LEVEL_MASK = 0x3f;
    static final int RUN_RTL_FLAG = 1 << RUN_LEVEL_SHIFT;

    public static final Directions ALL_LEFT_TO_RIGHT =
            new Directions(new int[]{0, RUN_LENGTH_MASK});

    public static final Directions ALL_RIGHT_TO_LEFT =
            new Directions(new int[]{0, RUN_LENGTH_MASK | RUN_RTL_FLAG});

    /**
     * Directions represents directional runs within a line of text. Runs are pairs of ints
     * listed in visual order, starting from the leading margin.  The first int of each pair is
     * the offset from the first character of the line to the start of the run.  The second int
     * represents both the length and level of the run. The length is in the lower bits,
     * accessed by masking with RUN_LENGTH_MASK.  The level is in the higher bits, accessed by
     * shifting by RUN_LEVEL_SHIFT and masking by RUN_LEVEL_MASK. To simply test for an RTL
     * direction, test the bit using RUN_RTL_FLAG, if set then the direction is rtl.
     */
    public int[] mDirections;

    /**
     * @see MeasuredParagraph#getDirections(int, int)
     */
    public Directions(int[] dirs) {
        mDirections = dirs;
    }

    /**
     * Returns number of BiDi runs.
     */
    public int getRunCount() {
        return mDirections.length >> 1;
    }

    /**
     * Returns the start offset of the BiDi run.
     *
     * @param runIndex the index of the BiDi run
     * @return the start offset of the BiDi run.
     */
    public int getRunStart(int runIndex) {
        return mDirections[runIndex << 1];
    }

    /**
     * Returns the length of the BiDi run.
     * <p>
     * Note that this method may return too large number due to reducing the number of object
     * allocations. The too large number means the remaining part is assigned to this run. The
     * caller must clamp the returned value.
     *
     * @param runIndex the index of the BiDi run
     * @return the length of the BiDi run.
     */
    public int getRunLength(int runIndex) {
        return mDirections[(runIndex << 1) + 1] & RUN_LENGTH_MASK;
    }

    /**
     * Returns true if the BiDi run is RTL.
     *
     * @param runIndex the index of the BiDi run
     * @return true if the BiDi run is RTL.
     */
    public boolean isRunRtl(int runIndex) {
        return (mDirections[(runIndex << 1) + 1] & RUN_RTL_FLAG) != 0;
    }
}
