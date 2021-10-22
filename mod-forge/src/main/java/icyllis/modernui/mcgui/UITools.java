/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.mcgui;

import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.regex.Pattern;

//TODO
@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public class UITools {

    //public static final ResourceLocation BUTTON = new ResourceLocation(ModernUI.MODID, "textures/gui/button.png");
    //@Deprecated
    //public static final ResourceLocation ICONS = new ResourceLocation(ModernUI.MODID, "textures/gui/gui_icon.png");

    //private static final TrueTypeRenderer FONT_RENDERER;

    private static final long IBEAM_CURSOR;
    private static final long HAND_CURSOR;

    static {
        //FONT_RENDERER = TrueTypeRenderer.getInstance();

        IBEAM_CURSOR = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
        HAND_CURSOR = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
    }

    public static final char CHECK_MARK = '\u2714';
    public static final char BLACK_CIRCLE = '\u25cf';

    /**
     * Return the width of a string in pixels.
     *
     * @param string compute the width of this string
     * @return the width in pixels
     */
    /*public static float getTextWidth(String string) {
        return FONT_RENDERER.getStringWidth(string);
    }*/

    /**
     * Trim a string so that it fits in the specified width when rendered, optionally reversing the string
     *
     * @param str     the String to trim
     * @param width   the desired string width (in GUI coordinate system)
     * @param reverse if true, the returned string will also be reversed
     * @return the trimmed and optionally reversed string
     */
    /*public static String trimTextToWidth(String str, float width, boolean reverse) {
        return FONT_RENDERER.trimStringToWidth(str, width, reverse);
    }*/

    /*@Nonnull
    public static String[] splitTextToWidth(@Nonnull String string, float width) {
        List<String> list = new ArrayList<>();
        String str;
        int currentIndex = 0;
        int size;
        do {
            str = string.substring(currentIndex);
            size = FONT_RENDERER.sizeStringToWidth(str, width);
            list.add(str.substring(0, size));
            currentIndex += size;
        } while (currentIndex < string.length());
        return list.toArray(new String[0]);
    }*/

    /**
     * The default arrow cursor.
     */
    public static void useDefaultCursor() {
        GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), MemoryUtil.NULL);
    }

    /**
     * The text input I-beam cursor.
     */
    public static void useIBeamCursor() {
        GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), IBEAM_CURSOR);
    }

    /**
     * The hand cursor on a link or a web-page button.
     */
    public static void useHandCursor() {
        GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), HAND_CURSOR);
    }

    /**
     * Run a view's method for all views of given view recursively
     *
     * @param view     root view
     * @param consumer method to run
     */
    public static void runViewTraversal(@Nonnull View view, @Nonnull Consumer<View> consumer) {
        consumer.accept(view);
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            final int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                runViewTraversal(group.getChildAt(i), consumer);
            }
        }
    }

    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]+");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?[1-9]\\d*$");
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)[0-9a-f]+");

    public static boolean matchDigit(String str) {
        return DIGIT_PATTERN.matcher(str).matches();
    }

    public static boolean matchInteger(String str) {
        return INTEGER_PATTERN.matcher(str).matches();
    }

    public static boolean matchHex(String str) {
        return HEX_PATTERN.matcher(str).matches();
    }

    @Nonnull
    public static String[] splitByCaps(@Nonnull String str) {
        return str.split("(?<!^)(?=[A-Z])");
    }

    @Nonnull
    public static String percentageToString(double p) {
        return (int) (p * 100) + "%";
    }
}
