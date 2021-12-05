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

package icyllis.modernui.util;

import org.jetbrains.annotations.ApiStatus;

/**
 * A class for defining layout directions. A layout direction can be left-to-right (LTR)
 * or right-to-left (RTL). It can also be inherited (from a parent) or deduced from the default
 * language script of a locale.
 */
public final class LayoutDirection {

    private LayoutDirection() {
    }

    /**
     * An undefined layout direction.
     */
    @ApiStatus.Internal
    public static final int UNDEFINED = -1;

    /**
     * Horizontal layout direction is from Left to Right.
     */
    public static final int LTR = 0;

    /**
     * Horizontal layout direction is from Right to Left.
     */
    public static final int RTL = 1;

    /**
     * Horizontal layout direction is inherited.
     */
    public static final int INHERIT = 2;

    /**
     * Horizontal layout direction is deduced from the default language script for the locale.
     */
    public static final int LOCALE = 3;
}
