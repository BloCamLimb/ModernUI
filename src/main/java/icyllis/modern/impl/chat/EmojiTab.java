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

package icyllis.modern.impl.chat;

import com.mojang.blaze3d.platform.GlStateManager;
import icyllis.modern.ui.font.EmojiStringRenderer;
import icyllis.modern.ui.master.DrawTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class EmojiTab {

    private List<Integer> cachedEmoji = new ArrayList<>();
    private final TextureManager TEX = Minecraft.getInstance().textureManager;

    public void open() {
        cachedEmoji = EmojiFinder.findEmoji("");
    }

    public void draw() {
        GlStateManager.color3f(0.867f, 0.867f, 0.867f);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        TEX.bindTexture(EmojiStringRenderer.EMOJI);
        for(int x = 0; x < 5; x++) {
            for(int y = 0; y < 5; y++) {
                int index = x * 5 + y;
                if(index >= cachedEmoji.size()) {
                    break;
                }
                int code = cachedEmoji.get(index);
                int code3 = code >> 8;
                int code4 = code & 0xff;
                DrawTools.blit(20 + x * 12, 20 + y * 12, code3 * 11.5f, code4 * 11.5f, 11.5f, 11.5f);
            }
        }
    }
}
