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

package icyllis.modernui.gui.option;

import icyllis.modernui.font.FontTools;
import icyllis.modernui.font.IFontRenderer;
import icyllis.modernui.gui.master.IMouseListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.ClientResourcePackInfo;

public class ResourcePackEntry implements IMouseListener {

    private final IFontRenderer fontRenderer = FontTools.FONT_RENDERER;

    private final TextureManager textureManager = Minecraft.getInstance().getTextureManager();

    private final ClientResourcePackInfo info;

    private float x1, y1;

    private float x2, y2;

    private float width;

    private final float height;

    private boolean mouseHovered = false;

    private String title = "";

    private String[] desc = new String[0];

    public ResourcePackEntry(ClientResourcePackInfo info) {
        this.info = info;
        this.height = ResourcePackGroup.ENTRY_HEIGHT;
    }

    public final void setPos(float x1, float x2, float y) {
        this.x1 = x1;
        this.x2 = x2;
        this.width = x2 - x1;
        this.y1 = y;
        this.y2 = y + height;
        updateTitle();
        updateDescription();
    }

    public final void draw(float time) {
        fontRenderer.drawString(title, x1, y1);
    }

    public void updateTitle() {
        title = info.getTitle().getFormattedText();
        float w = fontRenderer.getStringWidth(title);
        float cw = width - 43;
        if (w > cw) {
            float kw = cw - fontRenderer.getStringWidth("...");
            title = fontRenderer.trimStringToWidth(title, kw, false) + "...";
        }

    }

    public void updateDescription() {
        //TODO split string is not perfect
        this.desc = FontTools.splitStringToWidth(info.getDescription().getFormattedText(), width - 43);
    }

    public void bindTexture() {
        info.func_195808_a(textureManager);
    }
}
