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

package icyllis.modernui.system;

import icyllis.modernui.gui.component.option.BooleanOptionEntry;
import icyllis.modernui.gui.component.option.OptionEntry;
import icyllis.modernui.gui.component.option.SliderOptionEntry;
import icyllis.modernui.gui.window.SettingScrollWindow;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.AbstractOption;
import net.minecraft.client.settings.BooleanOption;
import net.minecraft.client.settings.IteratableOption;
import net.minecraft.client.settings.SliderPercentageOption;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public enum SettingsManager {
    INSTANCE;

    public static Function<SettingScrollWindow, OptionEntry> FOV = INSTANCE.transformVanillaOption(AbstractOption.FOV);

    public static Function<SettingScrollWindow, OptionEntry> REALMS_NOTIFICATIONS = INSTANCE.transformVanillaOption(AbstractOption.REALMS_NOTIFICATIONS);

    private Field option_translateKey;

    private Field slider_minValue;

    private Field slider_maxValue;

    private Field slider_stepSize;

    private Field slider_getter;

    private Field slider_setter;

    private Field boolean_getter;

    private Field boolean_setter;

    {
        option_translateKey = ObfuscationReflectionHelper.findField(AbstractOption.class, "field_216693_Q");
        slider_minValue = ObfuscationReflectionHelper.findField(SliderPercentageOption.class, "field_216735_R");
        slider_maxValue = ObfuscationReflectionHelper.findField(SliderPercentageOption.class, "field_216736_S");
        slider_stepSize = ObfuscationReflectionHelper.findField(SliderPercentageOption.class, "field_216734_Q");
        slider_getter = ObfuscationReflectionHelper.findField(SliderPercentageOption.class, "field_216737_T");
        slider_setter = ObfuscationReflectionHelper.findField(SliderPercentageOption.class, "field_216738_U");
        boolean_getter = ObfuscationReflectionHelper.findField(BooleanOption.class, "field_216746_Q");
        boolean_setter = ObfuscationReflectionHelper.findField(BooleanOption.class, "field_216747_R");
    }

    public <T extends AbstractOption> Function<SettingScrollWindow, OptionEntry> transformVanillaOption(T abstractOption) {
        GameSettings gameSettings = Minecraft.getInstance().gameSettings;
        try {
            String translationKey = (String) option_translateKey.get(abstractOption);
            if (abstractOption instanceof SliderPercentageOption) {
                SliderPercentageOption instance = (SliderPercentageOption) abstractOption;
                double minValue = slider_minValue.getDouble(instance);
                double maxValue = slider_maxValue.getDouble(instance);
                float stepSize = slider_stepSize.getFloat(instance);
                Function<GameSettings, Double> getter = (Function<GameSettings, Double>) slider_getter.get(instance);
                BiConsumer<GameSettings, Double> setter = (BiConsumer<GameSettings, Double>) slider_setter.get(instance);
                return window -> new SliderOptionEntry(window, I18n.format(translationKey), (float) minValue, (float) maxValue,
                        stepSize, getter.apply(gameSettings).floatValue(), v -> setter.accept(gameSettings, (double) v));
            } else if (abstractOption instanceof BooleanOption) {
                BooleanOption instance = (BooleanOption) abstractOption;
                Predicate<GameSettings> getter = (Predicate<GameSettings>) boolean_getter.get(instance);
                BiConsumer<GameSettings, Boolean> setter = (BiConsumer<GameSettings, Boolean>) boolean_setter.get(instance);
                return window -> new BooleanOptionEntry(window, I18n.format(translationKey), getter.test(gameSettings), b -> setter.accept(gameSettings, b));
            } else if (abstractOption instanceof IteratableOption) {
                // There's no way to do this except manually, bcz we should get all selective options name before iterate. awa
                ModernUI.LOGGER.fatal("Iterable option found, {} with name {}", abstractOption, translationKey);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException();
    }
}
