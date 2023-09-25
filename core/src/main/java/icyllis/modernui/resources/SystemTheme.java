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

package icyllis.modernui.resources;

import icyllis.modernui.R;
import icyllis.modernui.graphics.Color;
import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.StateSet;

/**
 * Temp use.
 */
public class SystemTheme {

    public static final int COLOR_FOREGROUND = 0xFFFFFFFF;
    public static final int COLOR_FOREGROUND_NORMAL = 0xFFB0B0B0;
    public static final int COLOR_FOREGROUND_DISABLED = 0xFF3F3F3F;

    public static final float DISABLED_ALPHA = 0.3f;
    public static final float PRIMARY_CONTENT_ALPHA = 1;
    public static final float SECONDARY_CONTENT_ALPHA = 0.7f;

    public static final int COLOR_CONTROL_ACTIVATED = 0xffcda398;

    public static final ColorStateList TEXT_COLOR_SECONDARY;

    static {
        int[][] stateSet = {
                new int[]{-R.attr.state_enabled},
                new int[]{R.attr.state_hovered},
                StateSet.WILD_CARD
        };
        int[] colors = {
                COLOR_FOREGROUND_DISABLED,
                COLOR_FOREGROUND,
                COLOR_FOREGROUND_NORMAL
        };
        TEXT_COLOR_SECONDARY = new ColorStateList(stateSet, colors);
    }

    public static final ColorStateList COLOR_CONTROL_NORMAL = TEXT_COLOR_SECONDARY;

    public static int modulateColor(int baseColor, float alphaMod) {
        if (alphaMod == 1.0f) {
            return baseColor;
        }

        final int baseAlpha = Color.alpha(baseColor);
        final int alpha = MathUtil.clamp((int) (baseAlpha * alphaMod + 0.5f), 0, 255);

        return (baseColor & 0xFFFFFF) | (alpha << 24);
    }
}
