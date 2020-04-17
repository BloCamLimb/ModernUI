/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.system;

import net.minecraft.util.ResourceLocation;

import java.util.function.Function;
import java.util.function.Predicate;

public class ConstantsLibrary {

    public static final ResourceLocation BUTTON = new ResourceLocation(ModernUI.MODID, "textures/gui/button.png");
    public static final ResourceLocation ICONS = new ResourceLocation(ModernUI.MODID, "textures/gui/gui_icon.png");

    public static final Function<Double, String> PERCENTAGE_STRING_FUNC = p -> (int) (p * 100) + "%";

    public static final char CHECK_MARK = '\u2714';
    public static final char BLACK_CIRCLE = '\u25cf';

    public static final String CHECK_MARK_STRING = String.valueOf(CHECK_MARK);
    public static final String BLACK_CIRCLE_STRING = String.valueOf(BLACK_CIRCLE);

    public static final Predicate<String> DIGIT_FILTER = s -> s.matches("[0-9]+");
    public static final Predicate<String> HEX_FILTER = s -> s.matches("(?i)[0-9a-f]+");
}
