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

package icyllis.modernui.gui.component.scroll;

import icyllis.modernui.font.FontRendererTools;
import icyllis.modernui.font.IFontRenderer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Single option line in settings interface
 */
public class OptionEntry {

    protected IFontRenderer fontRenderer = FontRendererTools.CURRENT_RENDERER;

    public String optionName;

    public String[] desc = new String[0];

    public float textBrightness = 0.7f;

    public OptionEntry(String optionName) {
        this(optionName, null);
    }

    public OptionEntry(String optionName, @Nullable String desc) {
        this.optionName = optionName;
        if (desc != null)
            this.desc = FontRendererTools.splitStringToWidth(desc, 150);

    }

    public void draw(float centerX, float y, float maxY, float currentTime) {
        fontRenderer.drawString(optionName, centerX - 160, y + 8, textBrightness, textBrightness, textBrightness, 1, 0);
        if (desc.length > 0) {
            //TODO
        }
    }

}
