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

package icyllis.modernui.impl.setting;

import com.google.common.collect.Lists;
import icyllis.modernui.gui.math.Align3H;
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.impl.module.SettingResourcePack;
import icyllis.modernui.gui.scroll.ScrollWindow;
import icyllis.modernui.gui.scroll.UniformScrollGroup;
import net.minecraft.client.resources.ClientResourcePackInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * Let resource packs roll!
 */
public class ResourcePackGroup extends UniformScrollGroup<ResourcePackEntry> {

    public static int ENTRY_HEIGHT = 36;

    private final String title;

    private float titleCenterX;

    private final Type type;

    private final SettingResourcePack module;

    public ResourcePackGroup(SettingResourcePack module, ScrollWindow<?> window, ResourcePackList<ClientResourcePackInfo> list, Type type) {
        super(window, ENTRY_HEIGHT);
        this.module = module;
        this.type = type;
        if (type == Type.AVAILABLE) {
            this.title = TextFormatting.BOLD + I18n.format("resourcePack.available.title");

            List<ClientResourcePackInfo> infoList = Lists.newArrayList(list.getAllPacks());
            infoList.removeAll(list.getEnabledPacks());
            infoList.removeIf(ClientResourcePackInfo::isHidden);

            infoList.forEach(t -> entries.add(new ResourcePackEntry(module, window, t, Align9D.TOP_RIGHT)));
        } else {
            this.title = TextFormatting.BOLD + I18n.format("resourcePack.selected.title");

            List<ClientResourcePackInfo> enabledList = Lists.newArrayList(list.getEnabledPacks());
            enabledList.removeIf(ClientResourcePackInfo::isHidden);
            Collections.reverse(enabledList);

            enabledList.forEach(t -> entries.add(new ResourcePackEntry(module, window, t, Align9D.TOP_LEFT)));
        }
    }

    @Override
    public void locate(float px, float py) {
        super.locate(px, py);
        // 14 for title, 4 for end space
        height += 18;
        y2 += 18;
        float pw = Math.min(window.getWidth(), 240);
        float cw = pw - window.getMargin();
        entries.forEach(e -> e.setWidth(cw));
        py += 14;
        float left = window.getLeft(), right = window.getRight();
        if (type == Type.AVAILABLE) {
            left = right - pw;
        } else {
            right = left + pw;
        }
        titleCenterX = (int) ((left + right) / 2f);
        int i = 0;
        right -= window.getMargin();
        if (type == Type.AVAILABLE) {
            for (ResourcePackEntry entry : entries) {
                float cy = py + i * entryHeight;
                entry.locate(right, cy);
                i++;
            }
        } else {
            for (ResourcePackEntry entry : entries) {
                float cy = py + i * entryHeight;
                entry.locate(left, cy);
                i++;
            }
        }

        if (module.getHighlightEntry() != null && entries.contains(module.getHighlightEntry())) {
            followEntry(module.getHighlightEntry());
        }
    }

    /*@Override
    public void onLayout(float left, float right, float y) {
        super.onLayout(left, right, y);
        float pw = (int) Math.floor(right - left);
        pw = Math.min(pw, 240);
        y += 14;
        if (type == Type.AVAILABLE) {
            left = right - pw;
        } else {
            right = left + pw;
        }
        titleCenterX = (int) ((left + right) / 2f);
        int i = 0;
        right -= window.borderThickness;
        for (ResourcePackEntry entry : entries) {
            float cy = y + i * entryHeight;
            entry.onLayout(left, right, cy);
            i++;
        }

        if (module.getHighlightEntry() != null && entries.contains(module.getHighlightEntry())) {
            followEntry(module.getHighlightEntry());
        }
    }*/

    /*@Override
    public void draw(float time) {
        fontRenderer.drawString(title, titleCenterX, y1 + 2, TextAlign.CENTER);
        super.draw(time);
    }*/

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        canvas.resetColor();
        canvas.setTextAlign(Align3H.CENTER);
        canvas.drawText(title, titleCenterX, y1 + 2);
        super.draw(canvas, time);
    }

    public void layoutGroup() {
        height = 18 + entries.size() * entryHeight;
        window.layoutList();
    }

    public enum Type {
        AVAILABLE,
        SELECTED
    }
}
