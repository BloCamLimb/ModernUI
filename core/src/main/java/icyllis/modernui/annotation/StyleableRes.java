/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes that a string array or integer parameter, field or method return value is expected
 * to be a styleable resource declaration.
 * <p>
 * When a string array is denoted, then the array contains (namespace, attribute) pairs of
 * declared attributes, and must be grouped by namespace, sorted by namespace, and then by
 * attribute name in lexicographic order.<br>
 * When an integer is denoted, then it represents the index of attribute pair in the string
 * array, ranged from 0 (inclusive) to <code>array.length / 2</code> (exclusive).
 * <p>
 * Example:
 * <pre>{@code
 *  // 5 attributes
 *  @StyleableRes
 *  static final String[] STYLEABLE = {
 *    "framework", "color", // index 0
 *    "framework", "size",  // index 1
 *    "framework", "text",  // ...
 *    "library", "color",
 *    "library", "shape",   // index 4
 *  };
 *  {
 *      TypedArray a = theme.obtainStyledAttributes(R.style.MyStyle, STYLEABLE);
 *      int value = a.getDimensionPixelSize(1, -1); // get "framework:attr/size"
 *      a.recycle();
 *  }
 * }</pre>
 */
@Documented
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.LOCAL_VARIABLE, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
public @interface StyleableRes {
}
