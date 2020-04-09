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

import com.google.common.collect.Lists;
import icyllis.modernui.gui.option.BooleanOptionEntry;
import icyllis.modernui.gui.option.DSliderOptionEntry;
import icyllis.modernui.gui.option.OptionEntry;
import icyllis.modernui.gui.option.SSliderOptionEntry;
import icyllis.modernui.gui.scroll.SettingScrollWindow;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.NewChatGui;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.AbstractOption;
import net.minecraft.client.settings.BooleanOption;
import net.minecraft.client.settings.IteratableOption;
import net.minecraft.client.settings.SliderPercentageOption;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public enum SettingsManager {
    INSTANCE;

    public static Function<SettingScrollWindow, SSliderOptionEntry> FOV;

    /**
     * Different (from vanilla):
     * Same effect, but less computation. [0.1, 1.0]
     */
    public static Function<SettingScrollWindow, SSliderOptionEntry> CHAT_OPACITY;

    /**
     * Different (from vanilla):
     * Set minimum value to 10% rather than 0% (OFF), because we have visibility. [0.1, 1.0]
     */
    public static Function<SettingScrollWindow, SSliderOptionEntry> CHAT_SCALE;

    /**
     * Different (from vanilla):
     * Use Optifine setting, so now width in [40, 1176] rather than [40, 320]
     */
    public static Function<SettingScrollWindow, SSliderOptionEntry> CHAT_WIDTH;

    public static Function<SettingScrollWindow, SSliderOptionEntry> CHAT_HEIGHT_FOCUSED;

    public static Function<SettingScrollWindow, SSliderOptionEntry> CHAT_HEIGHT_UNFOCUSED;

    public static Function<SettingScrollWindow, SSliderOptionEntry> TEXT_BACKGROUND_OPACITY;

    public static Function<SettingScrollWindow, SSliderOptionEntry> GAMMA;

    /**
     * Optifine setting
     */
    public static LazyOptional<Function<SettingScrollWindow, SSliderOptionEntry>> AO_LEVEL = LazyOptional.empty();

    public static Function<SettingScrollWindow, SSliderOptionEntry> SENSITIVITY;

    public static Function<SettingScrollWindow, SSliderOptionEntry> MOUSE_WHEEL_SENSITIVITY;



    public static Function<SettingScrollWindow, DSliderOptionEntry> RENDER_DISTANCE;

    public static Function<SettingScrollWindow, DSliderOptionEntry> BIOME_BLEND_RADIUS;



    public static Function<SettingScrollWindow, BooleanOptionEntry> REALMS_NOTIFICATIONS;

    public static Function<SettingScrollWindow, BooleanOptionEntry> CHAT_COLOR;
    public static Function<SettingScrollWindow, BooleanOptionEntry> CHAT_LINKS;
    public static Function<SettingScrollWindow, BooleanOptionEntry> CHAT_LINKS_PROMPT;
    public static Function<SettingScrollWindow, BooleanOptionEntry> REDUCED_DEBUG_INFO;
    public static Function<SettingScrollWindow, BooleanOptionEntry> AUTO_SUGGEST_COMMANDS;

    public static Function<SettingScrollWindow, BooleanOptionEntry> SHOW_SUBTITLES;
    public static Function<SettingScrollWindow, BooleanOptionEntry> AUTO_JUMP;

    public static Function<SettingScrollWindow, BooleanOptionEntry> VSYNC;
    public static Function<SettingScrollWindow, BooleanOptionEntry> VIEW_BOBBING;

    public static Function<SettingScrollWindow, BooleanOptionEntry> ENTITY_SHADOWS;

    public static Function<SettingScrollWindow, BooleanOptionEntry> INVERT_MOUSE;
    public static Function<SettingScrollWindow, BooleanOptionEntry> DISCRETE_MOUSE_WHEEL;
    public static Function<SettingScrollWindow, BooleanOptionEntry> TOUCHSCREEN;
    public static Function<SettingScrollWindow, BooleanOptionEntry> RAW_MOUSE_INPUT;



    static {
        FOV = INSTANCE
                .transformToSmooth(AbstractOption.FOV);
        CHAT_OPACITY = INSTANCE
                .transformToSmooth(AbstractOption.CHAT_OPACITY, p -> (int) (p * 90 + 10) + "%");
        CHAT_SCALE = INSTANCE
                .transformToSmooth(AbstractOption.CHAT_SCALE, Triple.of(0.1, null, null), ConstantsLibrary.PERCENTAGE_STRING_FUNC);
        if (ModIntegration.optifineLoaded) {
            CHAT_WIDTH = INSTANCE
                    .transformToSmooth(AbstractOption.CHAT_WIDTH, Triple.of(null, null, 1.0f / 1136.0f),
                            d -> NewChatGui.calculateChatboxWidth(d * 4.0571431d) + "px");
        } else {
            CHAT_WIDTH = INSTANCE
                    .transformToSmooth(AbstractOption.CHAT_WIDTH, Triple.of(null, 4.0571431d, 1.0f / 1136.0f),
                            d -> NewChatGui.calculateChatboxWidth(d) + "px");
        }
        CHAT_HEIGHT_FOCUSED = INSTANCE
                .transformToSmooth(AbstractOption.CHAT_HEIGHT_FOCUSED, Triple.of(null, null, 1.0f / 160.0f),
                        d -> NewChatGui.calculateChatboxHeight(d) + "px");
        CHAT_HEIGHT_UNFOCUSED = INSTANCE
                .transformToSmooth(AbstractOption.CHAT_HEIGHT_UNFOCUSED, Triple.of(null, null, 1.0f / 160.0f),
                        d -> NewChatGui.calculateChatboxHeight(d) + "px");
        TEXT_BACKGROUND_OPACITY = INSTANCE
                .transformToSmooth(AbstractOption.ACCESSIBILITY_TEXT_BACKGROUND_OPACITY, ConstantsLibrary.PERCENTAGE_STRING_FUNC);
        GAMMA = INSTANCE
                .transformToSmooth(AbstractOption.GAMMA, ConstantsLibrary.PERCENTAGE_STRING_FUNC);
        SENSITIVITY = INSTANCE
                .transformToSmooth(AbstractOption.SENSITIVITY, Triple.of(null, null, 0.005f), p -> (int) (p * 200) + "%");
        MOUSE_WHEEL_SENSITIVITY = INSTANCE
                .transformToSmooth(AbstractOption.MOUSE_WHEEL_SENSITIVITY, ConstantsLibrary.PERCENTAGE_STRING_FUNC);
        RENDER_DISTANCE = INSTANCE
                .transformToDiscrete(AbstractOption.RENDER_DISTANCE, false);
        BIOME_BLEND_RADIUS = INSTANCE
                .transformToDiscrete(AbstractOption.BIOME_BLEND_RADIUS, i -> {
                    if (i == 0) {
                        return I18n.format("options.off");
                    } else {
                        int c = i * 2 + 1;
                        return c + "x" + c;
                    }
                }, false);
        REALMS_NOTIFICATIONS = INSTANCE
                .transformToBoolean(AbstractOption.REALMS_NOTIFICATIONS);
        CHAT_COLOR = INSTANCE
                .transformToBoolean(AbstractOption.CHAT_COLOR);
        CHAT_LINKS = INSTANCE
                .transformToBoolean(AbstractOption.CHAT_LINKS);
        CHAT_LINKS_PROMPT = INSTANCE
                .transformToBoolean(AbstractOption.CHAT_LINKS_PROMPT);
        REDUCED_DEBUG_INFO = INSTANCE
                .transformToBoolean(AbstractOption.REDUCED_DEBUG_INFO);
        AUTO_SUGGEST_COMMANDS = INSTANCE
                .transformToBoolean(AbstractOption.AUTO_SUGGEST_COMMANDS);
        SHOW_SUBTITLES = INSTANCE
                .transformToBoolean(AbstractOption.SHOW_SUBTITLES);
        AUTO_JUMP = INSTANCE
                .transformToBoolean(AbstractOption.AUTO_JUMP);
        VSYNC = INSTANCE
                .transformToBoolean(AbstractOption.VSYNC);
        VIEW_BOBBING = INSTANCE
                .transformToBoolean(AbstractOption.VIEW_BOBBING);
        ENTITY_SHADOWS = INSTANCE
                .transformToBoolean(AbstractOption.ENTITY_SHADOWS);
        INVERT_MOUSE = INSTANCE
                .transformToBoolean(AbstractOption.INVERT_MOUSE);
        DISCRETE_MOUSE_WHEEL = INSTANCE
                .transformToBoolean(AbstractOption.DISCRETE_MOUSE_SCROLL);
        TOUCHSCREEN = INSTANCE
                .transformToBoolean(AbstractOption.TOUCHSCREEN);
        RAW_MOUSE_INPUT = INSTANCE
                .transformToBoolean(AbstractOption.RAW_MOUSE_INPUT);

        if (ModIntegration.optifineLoaded) {
            try {
                Field field;
                field = AbstractOption.class.getDeclaredField("AO_LEVEL");
                SliderPercentageOption ao_level = (SliderPercentageOption) field.get(AbstractOption.class);
                AO_LEVEL = LazyOptional.of(() ->
                        INSTANCE.transformToSmooth(ao_level, ConstantsLibrary.PERCENTAGE_STRING_FUNC));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }


    private GameSettings gameSettings;

    private Field option_translateKey;

    private Field slider_minValue;
    private Field slider_maxValue;
    private Field slider_stepSize;
    private Field slider_getter;
    private Field slider_setter;

    private Field boolean_getter;
    private Field boolean_setter;

    /**
     * Optifine
     */
    private Field of_dynamic_fov;

    private Field of_chat_background;

    private Field of_chat_shadow;

    {
        gameSettings = Minecraft.getInstance().gameSettings;

        option_translateKey = ObfuscationReflectionHelper.findField(AbstractOption.class, "field_216693_Q");

        slider_minValue = ObfuscationReflectionHelper.findField(SliderPercentageOption.class, "field_216735_R");
        slider_maxValue = ObfuscationReflectionHelper.findField(SliderPercentageOption.class, "field_216736_S");
        slider_stepSize = ObfuscationReflectionHelper.findField(SliderPercentageOption.class, "field_216734_Q");
        slider_getter = ObfuscationReflectionHelper.findField(SliderPercentageOption.class, "field_216737_T");
        slider_setter = ObfuscationReflectionHelper.findField(SliderPercentageOption.class, "field_216738_U");

        boolean_getter = ObfuscationReflectionHelper.findField(BooleanOption.class, "field_216746_Q");
        boolean_setter = ObfuscationReflectionHelper.findField(BooleanOption.class, "field_216747_R");

        if (ModIntegration.optifineLoaded) {
            try {
                of_dynamic_fov = GameSettings.class.getDeclaredField("ofDynamicFov");
                of_chat_background = GameSettings.class.getDeclaredField("ofChatBackground");
                of_chat_shadow = GameSettings.class.getDeclaredField("ofChatShadow");
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        /*try {
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
        }*/
    }

    public Function<SettingScrollWindow, SSliderOptionEntry> transformToSmooth(SliderPercentageOption instance) {
        return transformToSmooth(instance, null, null);
    }

    public Function<SettingScrollWindow, SSliderOptionEntry> transformToSmooth(SliderPercentageOption instance, Function<Double, String> stringFunction) {
        return transformToSmooth(instance, null, stringFunction);
    }

    public Function<SettingScrollWindow, SSliderOptionEntry> transformToSmooth(SliderPercentageOption instance, Triple<Double, Double, Float> customize) {
        return transformToSmooth(instance, customize, null);
    }

    public Function<SettingScrollWindow, SSliderOptionEntry> transformToSmooth(SliderPercentageOption instance, @Nullable Triple<Double, Double, Float> customize, @Nullable Function<Double, String> stringFunction) {
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
                return window -> new SSliderOptionEntry(window, I18n.format(translationKey), minValue, maxValue,
                        stepSize, getter.apply(gameSettings), v -> setter.accept(gameSettings, v));
            } else {
                return window -> new SSliderOptionEntry(window, I18n.format(translationKey), minValue, maxValue,
                        stepSize, getter.apply(gameSettings), v -> setter.accept(gameSettings, v), stringFunction);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException();
    }

    public Function<SettingScrollWindow, DSliderOptionEntry> transformToDiscrete(SliderPercentageOption instance, boolean dynamicModify) {
        return this.transformToDiscrete(instance, String::valueOf, dynamicModify);
    }

    public Function<SettingScrollWindow, DSliderOptionEntry> transformToDiscrete(SliderPercentageOption instance, Function<Integer, String> displayStringFunc, boolean dynamicModify) {
        GameSettings gameSettings = Minecraft.getInstance().gameSettings;
        try {
            String translationKey = (String) option_translateKey.get(instance);
            int minValue;
            int maxValue;
            minValue = (int) slider_minValue.getDouble(instance);
            maxValue = (int) slider_maxValue.getDouble(instance);
            Function<GameSettings, Double> getter = (Function<GameSettings, Double>) slider_getter.get(instance);
            BiConsumer<GameSettings, Double> setter = (BiConsumer<GameSettings, Double>) slider_setter.get(instance);
            if (dynamicModify) {
                return window -> new DSliderOptionEntry(window, I18n.format(translationKey), minValue, maxValue,
                        getter.apply(gameSettings).intValue(), v -> setter.accept(gameSettings, (double) v), displayStringFunc);
            } else {
                return window -> new DSliderOptionEntry(window, I18n.format(translationKey), minValue, maxValue,
                        getter.apply(gameSettings).intValue(), i -> {}, displayStringFunc).setApplyChange(v -> setter.accept(gameSettings, (double) v));
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
                // There's no way to do this at present, we should get all selective options name before iterate or a new way. awa
                //ModernUI.LOGGER.fatal("Iterable option found, {} with name {}", abstractOption, translationKey);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException();
    }


    /** Optifine Soft Compatibility **/

    public boolean getDynamicFov() {
        try {
            return of_dynamic_fov.getBoolean(gameSettings);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setDynamicFov(boolean b) {
        try {
            of_dynamic_fov.setBoolean(gameSettings, b);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("NoTranslation")
    public List<String> getChatBackgroundTexts() {
        return Lists.newArrayList(I18n.format("generator.default"), I18n.format("of.general.compact"), I18n.format("options.off"));
    }

    public int getChatBackgroundIndex() {
        try {
            int c = of_chat_background.getInt(gameSettings);
            if (c == 0) {
                return 0;
            } else if (c == 5) {
                return 1;
            } else {
                return 2;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void setChatBackgroundIndex(int index) {
        try {
            of_chat_background.setInt(gameSettings, index);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public boolean getChatShadow() {
        try {
            return of_chat_shadow.getBoolean(gameSettings);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setChatShadow(boolean b) {
        try {
            of_chat_shadow.setBoolean(gameSettings, b);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
