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

import icyllis.modernui.api.ModernUI_API;
import icyllis.modernui.gui.element.IElement;
import icyllis.modernui.api.manager.IModuleManager;
import icyllis.modernui.gui.font.FontRendererTools;
import icyllis.modernui.gui.font.IFontRenderer;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Single option line in settings interface
 */
public abstract class OptionEntryBase implements IElement {

    public static Function<Integer, Float> X = w -> w / 2f - 300f;

    protected IFontRenderer fontRenderer = FontRendererTools.CURRENT_RENDERER;

    protected IModuleManager moduleManager = ModernUI_API.INSTANCE.getModuleManager();

    public String optionName;

    public String[] desc = new String[0];

    public float x, absY;

    public float textBrightness = 0.7f;

    public OptionEntryBase(String optionName) {
        this(optionName, null);
    }

    public OptionEntryBase(String optionName, @Nullable String desc) {
        this.optionName = optionName;
        if (desc != null)
            this.desc = FontRendererTools.splitStringToWidth(desc, 150);
        //TODO
    }

    public void setY(float absY) {
        this.absY = absY;
    }

    public void mouseMoved(double mouseX, double mouseY) {

    }

    @Override
    public void draw(float currentTime) {
        fontRenderer.drawString(optionName, x, absY, textBrightness, textBrightness, textBrightness, 1, 0);
        if (desc.length > 0) {
            //TODO
        }
    }

    @Override
    public void resize(int width, int height) {
        x = X.apply(width);
    }
}
