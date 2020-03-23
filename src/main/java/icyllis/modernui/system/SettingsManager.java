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
import net.minecraft.client.gui.NewChatGui;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.AbstractOption;
import net.minecraft.client.settings.BooleanOption;
import net.minecraft.client.settings.IteratableOption;
import net.minecraft.client.settings.SliderPercentageOption;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public enum SettingsManager {
    INSTANCE;

    public static Function<SettingScrollWindow, SliderOptionEntry> FOV = INSTANCE
            .transformToPercentage(AbstractOption.FOV);

    /**
     * Different (from vanilla):
     * Same effect, but less computation. [0.1, 1.0]
     */
    public static Function<SettingScrollWindow, SliderOptionEntry> CHAT_OPACITY = INSTANCE
            .transformToPercentage(AbstractOption.CHAT_OPACITY, Triple.of(0.1, null, null), ConstantsLibrary.PERCENTAGE_STRING_FUNC);

    /**
     * Different (from vanilla):
     * Set minimum value to 10% rather than 0% (OFF), because we have visibility. [0.1, 1.0]
     */
    public static Function<SettingScrollWindow, SliderOptionEntry> CHAT_SCALE = INSTANCE
            .transformToPercentage(AbstractOption.CHAT_SCALE, Triple.of(0.1, null, null), ConstantsLibrary.PERCENTAGE_STRING_FUNC);

    /**
     * Different (from vanilla):
     * Use Optifine setting, so now width in [40, 1176] rather than [40, 320]
     */
    public static Function<SettingScrollWindow, SliderOptionEntry> CHAT_WIDTH = INSTANCE
            .transformToPercentage(AbstractOption.CHAT_WIDTH, Triple.of(null, 4.0571431d, 1.0f / 1136.0f), d -> NewChatGui.calculateChatboxWidth(d) + "px");

    public static Function<SettingScrollWindow, SliderOptionEntry> CHAT_HEIGHT_FOCUSED = INSTANCE
            .transformToPercentage(AbstractOption.CHAT_HEIGHT_FOCUSED, Triple.of(null, null, 1.0f / 160.0f), d -> NewChatGui.calculateChatboxHeight(d) + "px");

    public static Function<SettingScrollWindow, SliderOptionEntry> CHAT_HEIGHT_UNFOCUSED = INSTANCE
            .transformToPercentage(AbstractOption.CHAT_HEIGHT_UNFOCUSED, Triple.of(null, null, 1.0f / 160.0f), d -> NewChatGui.calculateChatboxHeight(d) + "px");

    public static Function<SettingScrollWindow, SliderOptionEntry> TEXT_BACKGROUND_OPACITY = INSTANCE
            .transformToPercentage(AbstractOption.ACCESSIBILITY_TEXT_BACKGROUND_OPACITY, ConstantsLibrary.PERCENTAGE_STRING_FUNC);



    public static Function<SettingScrollWindow, BooleanOptionEntry> REALMS_NOTIFICATIONS = INSTANCE.transformToBoolean(AbstractOption.REALMS_NOTIFICATIONS);

    public static Function<SettingScrollWindow, BooleanOptionEntry> CHAT_COLOR = INSTANCE.transformToBoolean(AbstractOption.CHAT_COLOR);
    public static Function<SettingScrollWindow, BooleanOptionEntry> CHAT_LINKS = INSTANCE.transformToBoolean(AbstractOption.CHAT_LINKS);
    public static Function<SettingScrollWindow, BooleanOptionEntry> CHAT_LINKS_PROMPT = INSTANCE.transformToBoolean(AbstractOption.CHAT_LINKS_PROMPT);
    public static Function<SettingScrollWindow, BooleanOptionEntry> REDUCED_DEBUG_INFO = INSTANCE.transformToBoolean(AbstractOption.REDUCED_DEBUG_INFO);
    public static Function<SettingScrollWindow, BooleanOptionEntry> AUTO_SUGGEST_COMMANDS = INSTANCE.transformToBoolean(AbstractOption.AUTO_SUGGEST_COMMANDS);

    public static Function<SettingScrollWindow, BooleanOptionEntry> SHOW_SUBTITLES = INSTANCE.transformToBoolean(AbstractOption.SHOW_SUBTITLES);



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

        try {
            Field[] fields = AbstractOption.class.getFields();
            ModernUI.LOGGER.debug("Searching Abstract Options...");
            for (Field f : fields) {
                if (Modifier.isStatic(f.getModifiers())) {
                    AbstractOption instance = (AbstractOption) f.get(AbstractOption.class);
                    String translateKey = (String) option_translateKey.get(instance);
                    ModernUI.LOGGER.debug("Name: {{}}, ClassName: {{}}, TranslateKey: {{}}", f.getName(), instance.getClass().getSimpleName(), translateKey);
                }
            }
            ModernUI.LOGGER.debug("Searching Abstract Options finished");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (ModIntegration.optifineLoaded) {
            try {
                Class<?> clazz = Class.forName("net.optifine.gui.GuiQualitySettingsOF");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public Function<SettingScrollWindow, SliderOptionEntry> transformToPercentage(SliderPercentageOption instance) {
        return transformToPercentage(instance, null, null);
    }

    public Function<SettingScrollWindow, SliderOptionEntry> transformToPercentage(SliderPercentageOption instance, Function<Double, String> stringFunction) {
        return transformToPercentage(instance, null, stringFunction);
    }

    public Function<SettingScrollWindow, SliderOptionEntry> transformToPercentage(SliderPercentageOption instance, Triple<Double, Double, Float> customize) {
        return transformToPercentage(instance, customize, null);
    }

    public Function<SettingScrollWindow, SliderOptionEntry> transformToPercentage(SliderPercentageOption instance, @Nullable Triple<Double, Double, Float> customize, @Nullable Function<Double, String> stringFunction) {
        GameSettings gameSettings = Minecraft.getInstance().gameSettings;
        try {
            String translationKey = (String) option_translateKey.get(instance);
            double minValue;
            double maxValue;
            float stepSize;
            if (customize == null) {
                minValue = slider_minValue.getDouble(instance);
                maxValue = slider_maxValue.getDouble(instance);
                stepSize = slider_stepSize.getFloat(instance);
            } else {
                minValue = customize.getLeft() == null ? slider_minValue.getDouble(instance) : customize.getLeft();
                maxValue = customize.getMiddle() == null ? slider_maxValue.getDouble(instance) : customize.getMiddle();
                stepSize = customize.getRight() == null ? slider_stepSize.getFloat(instance) : customize.getRight();
            }
            Function<GameSettings, Double> getter = (Function<GameSettings, Double>) slider_getter.get(instance);
            BiConsumer<GameSettings, Double> setter = (BiConsumer<GameSettings, Double>) slider_setter.get(instance);
            if (stringFunction == null) {
                return window -> new SliderOptionEntry(window, I18n.format(translationKey), minValue, maxValue,
                        stepSize, getter.apply(gameSettings), v -> setter.accept(gameSettings, v));
            } else {
                return window -> new SliderOptionEntry(window, I18n.format(translationKey), minValue, maxValue,
                        stepSize, getter.apply(gameSettings), v -> setter.accept(gameSettings, v), stringFunction);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException();
    }

    public Function<SettingScrollWindow, BooleanOptionEntry> transformToBoolean(BooleanOption instance) {
        GameSettings gameSettings = Minecraft.getInstance().gameSettings;
        try {
            String translationKey = (String) option_translateKey.get(instance);
            Predicate<GameSettings> getter = (Predicate<GameSettings>) boolean_getter.get(instance);
            BiConsumer<GameSettings, Boolean> setter = (BiConsumer<GameSettings, Boolean>) boolean_setter.get(instance);
            return window -> new BooleanOptionEntry(window, I18n.format(translationKey), getter.test(gameSettings), b -> setter.accept(gameSettings, b));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException();
    }

    @Deprecated
    public <T extends AbstractOption> Function<SettingScrollWindow, OptionEntry> transformVanillaOption(T abstractOption) {
        GameSettings gameSettings = Minecraft.getInstance().gameSettings;
        try {
            String translationKey = (String) option_translateKey.get(abstractOption);
            /*if (abstractOption instanceof SliderPercentageOption) {
                SliderPercentageOption instance = (SliderPercentageOption) abstractOption;
                double minValue = slider_minValue.getDouble(instance);
                double maxValue = slider_maxValue.getDouble(instance);
                float stepSize = slider_stepSize.getFloat(instance);
                Function<GameSettings, Double> getter = (Function<GameSettings, Double>) slider_getter.get(instance);
                BiConsumer<GameSettings, Double> setter = (BiConsumer<GameSettings, Double>) slider_setter.get(instance);
                return window -> new SliderOptionEntry(window, I18n.format(translationKey), minValue, maxValue,
                        stepSize, getter.apply(gameSettings), v -> setter.accept(gameSettings, v));
            } else if (abstractOption instanceof BooleanOption) {
                BooleanOption instance = (BooleanOption) abstractOption;
                Predicate<GameSettings> getter = (Predicate<GameSettings>) boolean_getter.get(instance);
                BiConsumer<GameSettings, Boolean> setter = (BiConsumer<GameSettings, Boolean>) boolean_setter.get(instance);
                return window -> new BooleanOptionEntry(window, I18n.format(translationKey), getter.test(gameSettings), b -> setter.accept(gameSettings, b));
            } else */if (abstractOption instanceof IteratableOption) {
                //TODO There's no way to do this at present, we should get all selective options name before iterate or a new way. awa
                ModernUI.LOGGER.fatal("Iterable option found, {} with name {}", abstractOption, translationKey);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException();
    }
}
