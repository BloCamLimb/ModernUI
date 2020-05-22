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

package icyllis.modernui.gui.math;

import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;

public class Color3i {

    public static final Color3i BLACK = new Color3i(0, 0, 0);
    public static final Color3i DARK_BLUE = new Color3i(0, 0, 170);
    public static final Color3i DARK_GREEN = new Color3i(0, 170, 0);
    public static final Color3i DARK_AQUA = new Color3i(0, 170, 170);
    public static final Color3i DARK_RED = new Color3i(170, 0, 0);
    public static final Color3i DARK_PURPLE = new Color3i(170, 0, 170);
    public static final Color3i GOLD = new Color3i(255, 170, 0);
    public static final Color3i GRAY = new Color3i(170, 170, 170);
    public static final Color3i DARK_GRAY = new Color3i(85, 85, 85);
    public static final Color3i BLUE = new Color3i(85, 85, 255);
    public static final Color3i GREEN = new Color3i(85, 255, 85);
    public static final Color3i AQUA = new Color3i(85, 255, 255);
    public static final Color3i RED = new Color3i(255, 85, 85);
    public static final Color3i LIGHT_PURPLE = new Color3i(255, 85, 255);
    public static final Color3i YELLOW = new Color3i(255, 255, 85);
    public static final Color3i WHITE = new Color3i(255, 255, 255);
    public static final Color3i BLUE_C = new Color3i(170, 220, 240);
    public static final Color3i GRAY_224 = new Color3i(224, 224, 224);

    private static final Color3i[] VANILLA_COLORS = new Color3i[]{BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE, GOLD, GRAY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE, YELLOW, WHITE};

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
            return VANILLA_COLORS[code];
        }
        return null;
    }

    public static float getAlphaFrom(int color) {
        return (color >> 24 & 0xff) / 255.0f;
    }

    public static float getRedFrom(int color) {
        return (color >> 16 & 0xff) / 255.0f;
    }

    public static float getGreenFrom(int color) {
        return (color >> 8 & 0xff) / 255.0f;
    }

    public static float getBlueFrom(int color) {
        return (color & 0xff) / 255.0f;
    }
}
