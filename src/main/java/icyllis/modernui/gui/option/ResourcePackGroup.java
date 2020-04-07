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
import icyllis.modernui.gui.module.SettingResourcePack;
import icyllis.modernui.gui.scroll.ScrollGroup;
import icyllis.modernui.gui.scroll.ScrollWindow;
import net.minecraft.client.resources.ClientResourcePackInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
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

    private final SettingResourcePack module;

    public ResourcePackGroup(SettingResourcePack module, ScrollWindow<?> window, ResourcePackList<ClientResourcePackInfo> list, Type type) {
        super(window);
        this.module = module;
        this.type = type;
        if (type == Type.AVAILABLE) {
            this.title = TextFormatting.UNDERLINE + I18n.format("resourcePack.available.title");

            List<ClientResourcePackInfo> infoList = Lists.newArrayList(list.getAllPacks());
            infoList.removeAll(list.getEnabledPacks());
            infoList.removeIf(ClientResourcePackInfo::isHidden);

            infoList.forEach(t -> entries.add(new ResourcePackEntry(module, t)));
        } else {
            this.title = TextFormatting.UNDERLINE + I18n.format("resourcePack.selected.title");

            List<ClientResourcePackInfo> enabledList = Lists.newArrayList(list.getEnabledPacks());
            enabledList.removeIf(ClientResourcePackInfo::isHidden);
            Collections.reverse(enabledList);

            enabledList.forEach(t -> entries.add(new ResourcePackEntry(module, t)));
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
        x2 -= window.borderThickness + 1;
        for (ResourcePackEntry entry : entries) {
            float cy = y + i * ENTRY_HEIGHT;
            entry.setPos(x1, x2, cy);
            i++;
        }

        if (module.getHighlight() != null) {
            followEntry(module.getHighlight());
        }
    }

    @Override
    public void updateVisible(float top, float bottom) {
        //TODO maybe?
    }

    @Override
    public void draw(float time) {
        fontRenderer.drawString(title, titleCenterX, y1 + 2, TextAlign.CENTER);

        for (ResourcePackEntry entry : entries) {
            entry.draw();
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
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        for (ResourcePackEntry entry :entries) {
            if (entry.isMouseHovered() && entry.mouseClicked(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        for (ResourcePackEntry entry : entries) {
            if (entry.isMouseHovered() && entry.mouseReleased(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double amount) {
        for (ResourcePackEntry entry : entries) {
            if (entry.isMouseHovered() && entry.mouseScrolled(amount)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        entries.forEach(IMouseListener::setMouseHoverExit);
    }

    public List<ResourcePackEntry> getEntries() {
        return entries;
    }

    public void layoutGroup() {
        height = 18 + entries.size() * ENTRY_HEIGHT;
        window.layoutList();
    }

    /**
     * Follow and focus an entry to make sure it's visible on scroll window
     * @param entry entry to follow
     */
    public void followEntry(@Nonnull ResourcePackEntry entry) {
        float c = entry.getTop() - window.getActualScrollAmount() - ENTRY_HEIGHT;
        if (c < 0) {
            if (c < -120) {
                window.scrollDirect(c);
            } else {
                window.scrollSmooth(c);
            }
            return;
        }
        float d = entry.getBottom() - window.getActualScrollAmount() - window.getVisibleHeight() - ENTRY_HEIGHT;
        if (d > 0) {
            if (d > 120) {
                window.scrollDirect(d);
            } else {
                window.scrollSmooth(d);
            }
        }
    }

    public enum Type {
        AVAILABLE,
        SELECTED
    }
}
