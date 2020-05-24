/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package icyllis.modernui.graphics.font;

import icyllis.modernui.graphics.math.TextAlign;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import javax.annotation.Nonnull;

/**
 * Use vanilla's font renderer to replace modern ui renderer
 */
@Deprecated
public class VanillaFontRenderer implements IFontRenderer {

    private static final VanillaFontRenderer INSTANCE = new VanillaFontRenderer();

    private final FontRenderer FONT;

    {
        FONT = Minecraft.getInstance().fontRenderer;
        //FONT = Minecraft.getInstance().getFontResourceManager().getFontRenderer(new ResourceLocation(ModernUI.MODID, "unix"));
    }

    private VanillaFontRenderer() {

    }

    @Override
    public float drawString(String str, float startX, float startY, float r, float g, float b, float a, @Nonnull TextAlign align) {
        startX = startX - FONT.getStringWidth(str) * align.getOffsetFactor() * 2;
        return FONT.drawString(str, startX, startY, (int) (a * 0xff) << 24 | (int) (r * 0xff) << 16 | (int) (g * 0xff) << 8 | (int) (b * 0xff)) - startX;
    }

    @Override
    public float getStringWidth(String str) {
        return FONT.getStringWidth(str);
    }

    @Override
    public int sizeStringToWidth(String str, float width) {
        return FONT.sizeStringToWidth(str, (int) width);
    }

    @Override
    public String trimStringToWidth(String str, float width, boolean reverse) {
        return FONT.trimStringToWidth(str, (int) width, reverse);
    }
}
