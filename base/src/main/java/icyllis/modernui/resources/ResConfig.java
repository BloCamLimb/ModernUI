/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
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

package icyllis.modernui.resources;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

import java.util.Locale;
import java.util.Objects;

/**
 * Used for resource config matching.
 *
 * @hidden
 */
@ApiStatus.Internal
public class ResConfig {

    public Locale locale = Locale.ROOT; // only LSR, for LocaleMatcher

    public String variant = "";
    public String numberingSystem;

    public int density;

    // compare things other than LSR
    public boolean isBetterThan(@NonNull ResConfig o,
                                @NonNull Locale desiredLocale,
                                int desiredDensity) {

        boolean thisVariantMatches = variant.equals(desiredLocale.getVariant());
        boolean otherVariantMatches = o.variant.equals(desiredLocale.getVariant());
        if (thisVariantMatches != otherVariantMatches) {
            return thisVariantMatches;
        }

        var desiredNumsys = desiredLocale.getUnicodeLocaleType("nu");
        boolean thisNumsysMatches = Objects.equals(numberingSystem, desiredNumsys);
        boolean otherNumsysMatches = Objects.equals(o.numberingSystem, desiredNumsys);
        if (thisNumsysMatches != otherNumsysMatches) {
            return thisNumsysMatches;
        }

        if (density != o.density) {
            int h = density;
            int l = o.density;
            boolean thisHigher = true;
            if (l > h) {
                int t = l;
                l = h;
                h = t;
                thisHigher = false;
            }

            if (h == desiredDensity) {
                return thisHigher;
            } else if (l >= desiredDensity) {
                // requested value lower than both l and h, give l
                return !thisHigher;
            } else {
                // otherwise give h
                return thisHigher;
            }
        }

        return false;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof ResConfig resConfig)) return false;

        return density == resConfig.density &&
                locale.equals(resConfig.locale) &&
                variant.equals(resConfig.variant) &&
                Objects.equals(numberingSystem, resConfig.numberingSystem);
    }

    @Override
    public int hashCode() {
        int result = locale.hashCode();
        result = 31 * result + variant.hashCode();
        result = 31 * result + Objects.hashCode(numberingSystem);
        result = 31 * result + density;
        return result;
    }
}
