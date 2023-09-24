/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

import java.lang.annotation.*;

/**
 * Denotes that the annotated element should have a given size or length.
 * Note that "-1" means "unset". Typically used with a parameter or
 * return value of type array or collection.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD,
        ElementType.PARAMETER,
        ElementType.FIELD,
        ElementType.LOCAL_VARIABLE,
        ElementType.ANNOTATION_TYPE})
public @interface Size {
    /** An exact size (or -1 if not specified) */
    long value() default -1;
    /** A minimum size, inclusive */
    long min() default Long.MIN_VALUE;
    /** A maximum size, inclusive */
    long max() default Long.MAX_VALUE;
    /** The size must be a multiple of this factor */
    long multiple() default 1;
}
