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

package icyllis.modernui.gui.master;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class MouseTools {

    private final static long IBEAM_CURSOR;

    private final static long HAND_CURSOR;

    static {
        IBEAM_CURSOR = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
        HAND_CURSOR = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
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
