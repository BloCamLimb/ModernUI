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

package icyllis.modernui.gui.component.option;

import com.google.common.collect.Lists;
import icyllis.modernui.gui.window.SettingScrollWindow;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.resources.I18n;

import java.util.List;
import java.util.function.Supplier;

public class BooleanOptionEntry extends MenuOptionEntry {

    private static Supplier<List<String>> YES_OR_NO = () -> Lists.newArrayList(I18n.format("gui.yes"), I18n.format("gui.no"));

    private static Supplier<List<String>> ON_OR_OFF = () -> Lists.newArrayList(I18n.format("options.on"), I18n.format("options.off"));

    private BooleanConsumer saveOption;

    public BooleanOptionEntry(SettingScrollWindow window, String optionTitle, boolean originalValue, BooleanConsumer saveOption) {
        this(window, optionTitle, originalValue, saveOption, false);
    }

    public BooleanOptionEntry(SettingScrollWindow window, String optionTitle, boolean originalValue, BooleanConsumer saveOption, boolean useYesOrNo) {
        super(window, optionTitle, useYesOrNo ? YES_OR_NO.get() : ON_OR_OFF.get(), originalValue ? 0 : 1, null);
        this.saveOption = saveOption;
    }

    @Override
    public void saveOption() {
        saveOption.accept(currentOptionIndex == 0);
    }
}
