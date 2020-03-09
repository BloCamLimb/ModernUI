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

package icyllis.modernui.gui.component;

import icyllis.modernui.api.element.Element;
import icyllis.modernui.api.widget.EventListener;
import icyllis.modernui.api.widget.Shape;
import icyllis.modernui.gui.font.FontRendererTools;
import icyllis.modernui.gui.master.DrawTools;

public class ConfirmWindow extends Element {

    private EventListener listener;

    private String titleText;

    private String[] infoText;

    private float textYOffset;

    public ConfirmWindow(String title, String info) {
        super(width -> width / 2f - 90f, height -> height / 2f - 40f);
        listener = new EventListener(width -> width / 2f - 105f, height -> height / 2f - 45f, new Shape.RectShape(210, 90));
        listener.addLeftClick(() -> moduleManager.closePopup());
        moduleManager.addEventListener(listener);
        this.titleText = "Confirm " + title;
        infoText = FontRendererTools.splitStringToWidth(info, 164);
        if (infoText.length > 1) {
            textYOffset = 16;
        } else {
            textYOffset = 24;
        }
    }

    @Override
    public void draw(float currentTime) {
        DrawTools.fillRectWithFrame(x, y, x + 180, y + 80, 0.51f, 0x101010, 0.4f, 0x404040, 0.8f);
        DrawTools.fillRectWithColor(x, y, x + 180, y + 14, 0x101010, 0.4f);
        fontRenderer.drawString(titleText, x + 90, y + 3, 1, 1, 1, 1, 0.25f);
        int i = 0;
        for (String t : infoText) {
            fontRenderer.drawString(t, x + 8, y + textYOffset + i++ * 12, 1, 1, 1, 1, 0);
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        listener.resize(width, height);
    }
}
