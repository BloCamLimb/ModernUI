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

package icyllis.modernui.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.reflect.*;

@OnlyIn(Dist.CLIENT)
public final class OptiFineIntegration {

    private static Field of_fast_render;

    static {
        try {
            of_fast_render = Options.class.getDeclaredField("ofFastRender");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private OptiFineIntegration() {
    }

    public static void openShadersGui() {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            Class<?> clazz = Class.forName("net.optifine.shaders.gui.GuiShaders");
            Constructor<?> constructor = clazz.getConstructor(Screen.class, Options.class);
            minecraft.setScreen((Screen) constructor.newInstance(minecraft.screen, minecraft.options));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * Incompatible with TextMC. Because we break the Vanilla rendering order.
     * See TextRenderNode#drawText()  endBatch(Sheets.signSheet()).
     * Modern UI glyph texture is translucent, so ending sign rendering earlier
     * stops sign texture being discarded by depth test.
     */
    public static void setFastRender(boolean fastRender) {
        Minecraft minecraft = Minecraft.getInstance();
        if (of_fast_render != null) {
            try {
                of_fast_render.setBoolean(minecraft.options, fastRender);
                minecraft.options.save();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
