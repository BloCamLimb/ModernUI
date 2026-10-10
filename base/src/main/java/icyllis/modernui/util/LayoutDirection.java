/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.util;

import com.ibm.icu.util.ULocale;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

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

    /**
     * Return the layout direction for a given Locale.
     * <p>
     * Be careful: this code will need to be updated when vertical scripts will be supported
     *
     * @param locale the Locale for which we want the layout direction. Can be null.
     * @return the layout direction. This may be one of: {@link #LTR} or {@link #RTL}.
     */
    public static int forLocale(@Nullable Locale locale) {
        return (locale != null && !locale.equals(Locale.ROOT)
                && ULocale.forLocale(locale).isRightToLeft())
                ? RTL
                : LTR;
    }
}
