/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.annotation;

import java.lang.annotation.*;

/**
 * Denotes that the annotated element should be a float or double in the given range
 * <p>
 * Example:
 * <pre>{@code
 *  @FloatRange(from=0.0,to=1.0)
 *  public float getAlpha() {
 *      ...
 *  }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD,
        ElementType.PARAMETER,
        ElementType.FIELD,
        ElementType.LOCAL_VARIABLE,
        ElementType.ANNOTATION_TYPE})
public @interface FloatRange {
    /** Smallest value. Whether it is inclusive or not is determined
     * by {@link #fromInclusive} */
    double from() default Double.NEGATIVE_INFINITY;
    /** Largest value. Whether it is inclusive or not is determined
     * by {@link #toInclusive} */
    double to() default Double.POSITIVE_INFINITY;

    /** Whether the from value is included in the range */
    boolean fromInclusive() default true;

    /** Whether the to value is included in the range */
    boolean toInclusive() default true;
}
