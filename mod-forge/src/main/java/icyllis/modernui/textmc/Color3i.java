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

package icyllis.modernui.textmc;

/**
 * No use, actually deprecated.
 */
@Deprecated
public class Color3i {

    /**
     * Classic 16 formatting colors {@link net.minecraft.ChatFormatting}
     */
    public static final Color3i BLACK        = new Color3i(0, 0, 0);
    public static final Color3i DARK_BLUE    = new Color3i(0, 0, 170);
    public static final Color3i DARK_GREEN   = new Color3i(0, 170, 0);
    public static final Color3i DARK_AQUA    = new Color3i(0, 170, 170);
    public static final Color3i DARK_RED     = new Color3i(170, 0, 0);
    public static final Color3i DARK_PURPLE  = new Color3i(170, 0, 170);
    public static final Color3i GOLD         = new Color3i(255, 170, 0);
    public static final Color3i GRAY         = new Color3i(170, 170, 170);
    public static final Color3i DARK_GRAY    = new Color3i(85, 85, 85);
    public static final Color3i BLUE         = new Color3i(85, 85, 255);
    public static final Color3i GREEN        = new Color3i(85, 255, 85);
    public static final Color3i AQUA         = new Color3i(85, 255, 255);
    public static final Color3i RED          = new Color3i(255, 85, 85);
    public static final Color3i LIGHT_PURPLE = new Color3i(255, 85, 255);
    public static final Color3i YELLOW       = new Color3i(255, 255, 85);
    public static final Color3i WHITE        = new Color3i(255, 255, 255);

    public static final Color3i BLUE_C   = new Color3i(170, 220, 240);
    public static final Color3i GRAY_224 = new Color3i(224, 224, 224);

    public static final Color3i[] FORMATTING_COLORS = new Color3i[]{
            BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE, GOLD,
            GRAY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE, YELLOW, WHITE};

    private final int red;
    private final int green;
    private final int blue;

    private final int color;

    /**
     * Constructor in code
     *
     * @param red   red value [0, 255]
     * @param green green value [0, 255]
     * @param blue  blue value [0, 255]
     */
    Color3i(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        color = red << 16 | green << 8 | blue;
    }

    /**
     * Constructor in stored json file
     *
     * @param color 0xRRGGBB
     */
    Color3i(int color) {
        red = color >> 16 & 0xff;
        green = color >> 8 & 0xff;
        blue = color & 0xff;
        this.color = color;
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    /**
     * Return in RGB form
     *
     * @return RGB color
     */
    public int getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "Color3i{" +
                "color=" + color +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Color3i color3i = (Color3i) o;

        return color == color3i.color;
    }

    @Override
    public int hashCode() {
        return color;
    }

    public static Color3i fromFormattingCode(int code) {
        if (code >= 0 && code <= 15) {
            return FORMATTING_COLORS[code];
        }
        return WHITE;
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
