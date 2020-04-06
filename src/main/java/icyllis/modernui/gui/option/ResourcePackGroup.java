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

import com.google.common.collect.Lists;
import icyllis.modernui.font.FontTools;
import icyllis.modernui.font.IFontRenderer;
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.master.IMouseListener;
import icyllis.modernui.gui.scroll.ScrollGroup;
import icyllis.modernui.gui.scroll.ScrollWindow;
import net.minecraft.client.resources.ClientResourcePackInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Let resource packs roll!
 */
public class ResourcePackGroup extends ScrollGroup {

    public static int ENTRY_HEIGHT = 36;

    private final IFontRenderer fontRenderer = FontTools.FONT_RENDERER;

    private final String title;

    private float titleCenterX;

    private List<ResourcePackEntry> entries = new ArrayList<>();

    private final Type type;

    public ResourcePackGroup(ScrollWindow<?> window, ResourcePackList<ClientResourcePackInfo> list, Type type) {
        super(window);
        this.type = type;
        if (type == Type.AVAILABLE) {
            this.title = TextFormatting.UNDERLINE + I18n.format("resourcePack.available.title");

            List<ClientResourcePackInfo> infoList = Lists.newArrayList(list.getAllPacks());
            infoList.removeAll(list.getEnabledPacks());
            infoList.removeIf(ClientResourcePackInfo::isHidden);

            infoList.forEach(t -> entries.add(new ResourcePackEntry(t)));
        } else {
            this.title = TextFormatting.UNDERLINE + I18n.format("resourcePack.selected.title");

            List<ClientResourcePackInfo> enabledList = Lists.newArrayList(list.getEnabledPacks());
            enabledList.removeIf(ClientResourcePackInfo::isHidden);
            Collections.reverse(enabledList);

            enabledList.forEach(t -> entries.add(new ResourcePackEntry(t)));
        }
        // 14 for title, 4 for end space
        height = 18 + entries.size() * ENTRY_HEIGHT;
    }

    /**
     * Layout entries and group
     */
    @Override
    public void setPos(float x1, float x2, float y) {
        super.setPos(x1, x2, y);
        float pw = (int) Math.floor(x2 - x1);
        pw = Math.min(pw, 240);
        y += 14;
        if (type == Type.AVAILABLE) {
            x1 = x2 - pw;
        } else {
            x2 = x1 + pw;
        }
        titleCenterX = (int) ((x1 + x2) / 2f);
        int i = 0;
        for (ResourcePackEntry entry : entries) {
            float cy = y + i * ENTRY_HEIGHT;
            entry.setPos(x1, x2, cy);
            i++;
        }
    }

    @Override
    public void updateVisible(float top, float bottom) {

    }

    @Override
    public void draw(float time) {
        fontRenderer.drawString(title, titleCenterX, y1 + 2, TextAlign.CENTER);

        for (ResourcePackEntry entry : entries) {
            entry.draw(time);
        }
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (super.updateMouseHover(mouseX, mouseY)) {
            boolean result = false;
            for (ResourcePackEntry entry : entries) {
                if (!result && entry.updateMouseHover(mouseX, mouseY)) {
                    result = true;
                } else {
                    entry.setMouseHoverExit();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        entries.forEach(IMouseListener::setMouseHoverExit);
    }

    public enum Type {
        AVAILABLE,
        SELECTED
    }
}
