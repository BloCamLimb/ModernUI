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

package icyllis.modernui.math;

import javax.annotation.Nullable;

public enum Color3i {
    BLACK(0, 0, 0),
    DARK_BLUE(0, 0, 170),
    DARK_GREEN(0, 170, 0),
    DARK_AQUA(0, 170, 170),
    DARK_RED(170, 0, 0),
    DARK_PURPLE(170, 0, 170),
    GOLD(255, 170, 0),
    GRAY(170, 170, 170),
    DARK_GRAY(85, 85, 85),
    BLUE(85, 85, 255),
    GREEN(85, 255, 85),
    AQUA(85, 255, 255),
    RED(255, 85, 85),
    LIGHT_PURPLE(255, 85, 255),
    YELLOW(255, 255, 85),
    WHILE(255, 255, 255),
    BLUE_C(170, 220, 240),
    GRAY_224(224, 224, 224);

    private int red, green, blue;

    private float redF, greenF, blueF;

    Color3i(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.redF = red / 255.0f;
        this.greenF = green / 255.0f;
        this.blueF = blue / 255.0f;
        //this.RGB = red << 16 | green << 8 | blue;
    }

    public int getIntRed() {
        return red;
    }

    public int getIntGreen() {
        return green;
    }

    public int getIntBlue() {
        return blue;
    }

    public float getFloatRed() {
        return redF;
    }

    public float getFloatGreen() {
        return greenF;
    }

    public float getFloatBlue() {
        return blueF;
    }

    @Nullable
    public static Color3i getFormattingColor(int code) {
        if (code >= 0 && code <= 15) {
            return values()[code];
        }
        return null;
    }
}
