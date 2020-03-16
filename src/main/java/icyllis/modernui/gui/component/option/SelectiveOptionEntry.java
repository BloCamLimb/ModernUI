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

package icyllis.modernui.gui.component.option;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.window.SettingScrollWindow;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.system.ReferenceLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class SelectiveOptionEntry extends OptionEntry<String> {

    private TextureManager textureManager = Minecraft.getInstance().textureManager;

    private String text;

    public SelectiveOptionEntry(SettingScrollWindow window, String optionName, List<String> options, int originalIndex) {
        super(window, optionName, options, originalIndex);
        text = options.get(originalIndex);
    }

    @Override
    public void drawExtra(float centerX, float y, float currentTime) {
        fontRenderer.drawString(text, centerX + 150, y + 6, 0.75f, 0.75f, 0.75f, 1, 0.5f);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.pushMatrix();
        RenderSystem.scalef(0.25f, 0.25f, 1);
        textureManager.bindTexture(ReferenceLibrary.ICONS);
        DrawTools.blit(centerX * 4 + 606, y * 4 + 28, 64, 32, 32, 32);
        RenderSystem.popMatrix();
    }
}
