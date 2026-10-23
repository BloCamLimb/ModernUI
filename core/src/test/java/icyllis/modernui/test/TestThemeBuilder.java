/*
 * ModernUI.
 * Copyright (C) 2025-2026 BloCamLimb. All rights reserved.
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

package icyllis.modernui.test;

import icyllis.modernui.R;
import icyllis.modernui.material.MaterialTheme;
import icyllis.modernui.resources.AssetManager;
import icyllis.modernui.resources.AttributeSlots;
import icyllis.modernui.resources.EmptyAssetProvider;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.resources.ResourcesBuilder;
import icyllis.modernui.resources.ThemeKey;
import icyllis.modernui.resources.TypedArray;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.util.DisplayMetrics;
import icyllis.modernui.util.Log;

public class TestThemeBuilder {

    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");

        Resources resources;{
            var rb = new ResourcesBuilder(R.ns);
            MaterialTheme.addToResources(rb);
            var m = new DisplayMetrics();
            m.setToDefaults();
            resources = new Resources(
                    new AssetManager.Builder()
                            .addAssetPack(rb.build(new EmptyAssetProvider()))
                            .build(),
                    m
            );
        }
        Resources.Theme theme = resources.newTheme(
                new ThemeKey.Builder()
                        .append(R.style.Theme_Material3_Dark, true)
                        .build()
        );
        String[] attrs = {
                R.attr.colorError,
                R.attr.textColor,
                R.attr.textSize,
        };
        AttributeSlots slots = new AttributeSlots(null, attrs);
        TypedArray a = theme.obtainStyledAttributes(
                R.style.TextAppearance_Material3_DisplayMedium,
                slots);

        Log.i(null, a.getDimensionPixelSize(2, -1));

        TypedValue value = new TypedValue();
        boolean result = theme.resolveAttribute(R.attr.textAppearanceDisplayMedium,
                value, true);
        Log.i(null, result);
        Log.i(null, value);
        Log.i(null, value.getReference());
        Log.i(null, R.style.TextAppearance_Material3_DisplayMedium.equals(value.getReference()));
    }
}
