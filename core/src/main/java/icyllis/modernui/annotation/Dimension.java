/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

import org.intellij.lang.annotations.MagicConstant;

import java.lang.annotation.*;

/**
 * Denotes that a numeric parameter, field or method return value is expected
 * to represent a dimension.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD,
        ElementType.PARAMETER,
        ElementType.FIELD,
        ElementType.LOCAL_VARIABLE,
        ElementType.ANNOTATION_TYPE})
public @interface Dimension {
    @Unit
    int unit() default PX;

    int DP = 0;
    int PX = 1;
    int SP = 2;

    @MagicConstant(intValues = {PX, DP, SP})
    @Retention(RetentionPolicy.SOURCE)
    @interface Unit {
    }
}
