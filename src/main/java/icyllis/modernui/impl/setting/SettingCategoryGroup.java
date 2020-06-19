/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
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

import icyllis.modernui.graphics.font.TextAlign;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.ui.widget.ScrollWindow;
import icyllis.modernui.ui.widget.UniformScrollGroup;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import java.util.List;

public class SettingCategoryGroup extends UniformScrollGroup<SettingEntry> {

    public static int ENTRY_HEIGHT = 21;

    private String title;

    public SettingCategoryGroup(ScrollWindow<?> window, String title, @Nonnull List<SettingEntry> entries) {
        super(window, ENTRY_HEIGHT);
        this.title = TextFormatting.BOLD + title;
        this.entries = entries;
        height = entries.size() * entryHeight + 36;
    }

    @Override
    public void locate(float px, float py) {
        super.locate(px, py);
        // 30 for title, 6 for end space.
        height += 36;
        y2 += 36;

        py += 30;
        int i = 0;
        for (SettingEntry entry : entries) {
            float cy = py + i * entryHeight;
            entry.locate(px, cy);
            i++;
        }
    }

    /*@Override
    public void onLayout(float left, float right, float y) {
        super.onLayout(left, right, y);
        left = centerX - 160;
        right = centerX + 160;
        y += 30;
        int i = 0;
        for (SettingEntry entry : entries) {
            float cy = y + i * entryHeight;
            entry.onLayout(left, right, cy);
            i++;
        }
    }*/

    /*@Override
    public void draw(float time) {
        fontRenderer.drawString(title, centerX - 160, y1 + 14, Color3f.WHILE, 1.0f, TextAlign.LEFT);
        super.draw(time);
    }*/

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        canvas.resetColor();
        canvas.setTextAlign(TextAlign.LEFT);
        canvas.drawText(title, centerX - 160, y1 + 14);
        super.draw(canvas, time);
    }

    /*@Override
    public void mouseMoved(double mouseX, double mouseY) {
        //ModernUI.LOGGER.info("Category Mouse Move {} {}", deltaCenterX, deltaY);
        double deltaCenterX = mouseX - centerX;
        if (deltaCenterX >= -160 && deltaCenterX <= 160) {
            double rY = mouseY - y - 30;
            for (int i = 0; i < entries.size(); i++) {
                entries.get(i).mouseMoved(deltaCenterX, rY - i * ENTRY_HEIGHT, mouseX, mouseY);
            }
            if (rY >= 0) {
                int pIndex = (int) (rY / ENTRY_HEIGHT);
                if (pIndex < entries.size()) {
                    for (int i = 0; i < entries.size(); i++) {
                        OptionEntry entry = entries.get(i);
                        if (i == pIndex) {
                            entry.setMouseHovered(true);
                        } else {
                            entry.setMouseHovered(false);
                        }
                    }
                } else {
                    entries.forEach(e -> e.setMouseHovered(false));
                }
            } else {
                entries.forEach(e -> e.setMouseHovered(false));
            }
        } else {
            entries.forEach(e -> e.setMouseHovered(false));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        double ry = mouseY - y - 30;
        double deltaCenterX = mouseX - centerX;
        *//*if (ry >= 0) {
            int pIndex = (int) (ry / ENTRY_HEIGHT);
            if (pIndex >= 0 && pIndex < entries.size()) {
                if (entries.get(pIndex).mouseClicked(deltaCenterX, ry - pIndex * ENTRY_HEIGHT, mouseX, mouseY, mouseButton)) {
                    return true;
                }
            }
        }*//*
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).mouseClicked(deltaCenterX, ry - i * ENTRY_HEIGHT, mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        double ry = mouseY - y - 30;
        double deltaCenterX = mouseX - centerX;
        *//*if (ry >= 0) {
            int pIndex = (int) (ry / ENTRY_HEIGHT);
            if (pIndex >= 0 && pIndex < entries.size()) {
                if (entries.get(pIndex).mouseReleased(deltaCenterX, ry - pIndex * ENTRY_HEIGHT, mouseX, mouseY, mouseButton)) {
                    return true;
                }
            }
        }*//*
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).mouseReleased(deltaCenterX, ry - i * ENTRY_HEIGHT, mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }*/
}
