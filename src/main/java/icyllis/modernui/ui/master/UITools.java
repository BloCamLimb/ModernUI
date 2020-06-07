/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.master;

import icyllis.modernui.graphics.font.TrueTypeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class UITools {

    private static final TrueTypeRenderer FONT_RENDERER;

    private static final long IBEAM_CURSOR;
    private static final long HAND_CURSOR;

    static {
        FONT_RENDERER = TrueTypeRenderer.INSTANCE;

        IBEAM_CURSOR = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
        HAND_CURSOR = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
    }

    public static float getStringWidth(String string) {
        return FONT_RENDERER.getStringWidth(string);
    }

    public static String trimStringToWidth(String str, float width, boolean reverse) {
        return FONT_RENDERER.trimStringToWidth(str, width, reverse);
    }

    @Nonnull
    public static String[] splitStringToWidth(@Nonnull String string, float width) {
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
    }

    /**
     * The default arrow cursor.
     */
    public static void useDefaultCursor() {
        GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), 0);
    }

    /**
     * The text input I-beam cursor.
     */
    public static void useIBeamCursor() {
        GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), IBEAM_CURSOR);
    }

    public static void useHandCursor() {
        GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), HAND_CURSOR);
    }
}
