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

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.resources.I18n;

import java.util.List;
import java.util.function.Supplier;

public class BooleanSettingEntry extends DropdownSettingEntry {

    private static Supplier<List<String>> YES_OR_NO = () -> Lists.newArrayList(I18n.format("gui.yes"), I18n.format("gui.no"));

    private static Supplier<List<String>> ON_OR_OFF = () -> Lists.newArrayList(I18n.format("options.on"), I18n.format("options.off"));

    private BooleanConsumer saveOption;

    public BooleanSettingEntry(SettingScrollWindow window, String optionTitle, boolean originalValue, BooleanConsumer saveOption) {
        this(window, optionTitle, originalValue, saveOption, false);
    }

    public BooleanSettingEntry(SettingScrollWindow window, String optionTitle, boolean originalValue, BooleanConsumer saveOption, boolean useYesOrNo) {
        super(window, optionTitle, useYesOrNo ? YES_OR_NO.get() : ON_OR_OFF.get(), originalValue ? 0 : 1, null);
        this.saveOption = saveOption;
    }

    @Override
    public void saveOption() {
        saveOption.accept(currentOptionIndex == 0);
    }
}
