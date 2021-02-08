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

/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package icyllis.modernui.text;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LineBreaker {

    /**
     * Line breaking constraints for single paragraph.
     */
    public static class ParagraphConstraints {

    }

    /**
     * The methods in this interface may be called several times. The implementation
     * must return the same value for the same input.
     */
    public interface LineWidth {

        /**
         * Find out the width for the line. This must not return negative values.
         *
         * @param line the line number
         * @return the line width
         */
        default float getAt(int line) {
            return 0;
        }

        /**
         * Find out the minimum line width. This mut not return negative values.
         *
         * @return the minimum line width
         */
        default float getMin() {
            return 0;
        }
    }

    public static class Result {

    }

    private static final LineBreaker sDefInstance = new LineBreaker(null);

    @Nullable
    private final int[] mIndents;

    private LineBreaker(@Nullable int[] indents) {
        mIndents = indents;
    }

    /**
     * Obtain a LineBreaker
     *
     * @see #obtain(int[])
     */
    public static LineBreaker obtain() {
        return sDefInstance;
    }

    /**
     * Obtain a LineBreaker
     *
     * @param indents The supplied array provides the total amount of indentation per
     *                line, in pixel. This amount is the sum of both left and right
     *                indentations. For lines past the last element in the array, the
     *                indentation amount of the last element is used.
     */
    public static LineBreaker obtain(@Nullable int[] indents) {
        return indents == null ? sDefInstance : new LineBreaker(indents);
    }

    /**
     * Break paragraph into lines.
     * <p>
     * The result is filled to out param.
     *
     * @param measuredPara a result of the text measurement
     * @param constraints  constraints for a single paragraph
     * @param lineNumber   a line number (offset) of this paragraph
     * @return the result of line break
     */
    @Nonnull
    public Result computeLineBreaks(@Nonnull MeasuredText measuredPara, @Nonnull ParagraphConstraints constraints,
                                    int lineNumber) {

    }
}
