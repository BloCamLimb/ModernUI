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

package icyllis.modernui.test;

import icyllis.modernui.ModernUI;
import icyllis.modernui.R;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.resources.SystemTheme;
import icyllis.modernui.resources.ResourcesBuilder;
import icyllis.modernui.resources.TypedArray;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.util.Log;

public class TestThemeBuilder {

    public static void main(String[] args) {
        Log.setLevel(Log.DEBUG);
        ResourcesBuilder builder = new ResourcesBuilder();

        SystemTheme.addToResources(builder);

        Resources resources = builder.build();
        Resources.Theme theme = resources.newTheme();
        theme.applyStyle(R.style.Theme_Material3_Dark, true);
        String[] styleable = {
                R.ns, R.attr.colorError,
                R.ns, R.attr.textColor,
                R.ns, R.attr.textSize,
        };
        TypedArray a = theme.obtainStyledAttributes(
                R.style.TextAppearance_Material3_DisplayMedium,
                styleable);

        ModernUI.LOGGER.info(a.getDimensionPixelSize(2, -1));

        TypedValue value = new TypedValue();
        boolean result = theme.resolveAttribute(R.attr.textAppearanceDisplayMedium,
                value, true);
        ModernUI.LOGGER.info(result);
        ModernUI.LOGGER.info(value);
        ModernUI.LOGGER.info(value.getResourceId());
        ModernUI.LOGGER.info(R.style.TextAppearance_Material3_DisplayMedium.equals(value.getResourceId()));
    }
}
