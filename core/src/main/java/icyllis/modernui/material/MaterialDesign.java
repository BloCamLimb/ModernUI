/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.material;

import icyllis.modernui.R;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.StateSet;

public final class MaterialDesign {

    public static final float disabled_alpha_material_dark = 0.3f;
    public static final int bright_foreground_dark = 0xff000000;
    public static final int bright_foreground_light = 0xffffffff;
    public static final int dim_foreground_light = 0xff323232;
    public static final int dim_foreground_light_disabled = 0x80323232;

    public static final ColorStateList secondary_text_light;

    static {
        int[][] states = {
                new int[]{R.attr.state_pressed, -R.attr.state_enabled}, // [0]
                new int[]{R.attr.state_selected, -R.attr.state_enabled}, // [1]
                new int[]{R.attr.state_pressed}, // [2]
                new int[]{R.attr.state_selected}, // [3]
                new int[]{R.attr.state_activated}, // [4]
                new int[]{-R.attr.state_enabled}, // [5]
                StateSet.WILD_CARD // [6]
        };
        int[] colors = {
                dim_foreground_light_disabled,
                dim_foreground_light_disabled,
                dim_foreground_light,
                dim_foreground_light,
                dim_foreground_light,
                dim_foreground_light,
                dim_foreground_light
        };
        secondary_text_light = new ColorStateList(states, colors);
    }
}
