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
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.master.IMouseListener;
import icyllis.modernui.gui.module.SettingLanguage;
import icyllis.modernui.gui.scroll.ScrollGroup;
import icyllis.modernui.gui.scroll.ScrollWindow;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;

public class LanguageGroup extends ScrollGroup {

    public static int ENTRY_HEIGHT = 18;

    private final IFontRenderer fontRenderer = FontTools.FONT_RENDERER;

    private final String title;

    private List<LanguageEntry> entries = new ArrayList<>();

    private final SettingLanguage module;

    public LanguageGroup(SettingLanguage module, ScrollWindow<?> window, LanguageManager manager) {
        super(window);
        this.module = module;
        title = TextFormatting.UNDERLINE + I18n.format("gui.modernui.settings.tab.language");
        manager.getLanguages().forEach(l -> {
            LanguageEntry entry = new LanguageEntry(module, l);
            entries.add(entry);
            if (l.getCode().equals(manager.getCurrentLanguage().getCode())) {
                module.setHighlight(entry);
            }
        });

        height = 18 + entries.size() * ENTRY_HEIGHT;
    }

    @Override
    public void setPos(float x1, float x2, float y) {
        super.setPos(x1, x2, y);
        y += 14;
        x1 = centerX - 120;
        x2 = centerX + 120;
        int i = 0;
        for (LanguageEntry entry : entries) {
            float cy = y + i * ENTRY_HEIGHT;
            entry.setPos(x1, x2, cy);
            i++;
        }

        float d = module.getHighlight().getBottom() - window.getActualScrollAmount() - window.getVisibleHeight() - ENTRY_HEIGHT;
        if (d > 0) {
            if (d > 120) {
                window.scrollDirect(d);
            } else {
                window.scrollSmooth(d);
            }
        }
    }

    @Override
    public void updateVisible(float top, float bottom) {

    }

    @Override
    public void draw(float time) {
        fontRenderer.drawString(title, centerX, y1 + 2, TextAlign.CENTER);

        for (LanguageEntry entry : entries) {
            entry.draw();
        }
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (super.updateMouseHover(mouseX, mouseY)) {
            boolean result = false;
            for (LanguageEntry entry : entries) {
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
        for (LanguageEntry entry :entries) {
            if (entry.isMouseHovered() && entry.mouseClicked(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        for (LanguageEntry entry : entries) {
            if (entry.isMouseHovered() && entry.mouseReleased(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double amount) {
        for (LanguageEntry entry : entries) {
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
}
