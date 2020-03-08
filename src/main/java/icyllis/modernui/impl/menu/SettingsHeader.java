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

package icyllis.modernui.impl.menu;

import icyllis.modernui.api.element.IElement;
import icyllis.modernui.gui.element.MenuSettingsBG;
import icyllis.modernui.gui.widget.LineTextButton;

import java.util.function.Consumer;

public class SettingsHeader {

    public SettingsHeader(Consumer<IElement> pool) {
        pool.accept(new MenuSettingsBG());
        pool.accept(new LineTextButton(w -> 64f, h -> 20f, "General", 48f, 31));
        pool.accept(new LineTextButton(w -> 128f, h -> 20f, "Video", 48f, 32));
        pool.accept(new LineTextButton(w -> 192f, h -> 20f, "Audio", 48f, 33));
        pool.accept(new LineTextButton(w -> 256f, h -> 20f, "Controls", 48f, 34));
        pool.accept(new LineTextButton(w -> 320f, h -> 20f, "Assets", 48f, 35));
        pool.accept(new LineTextButton(w -> 384f, h -> 20f, "Configs", 48f, 36));

    }
}
