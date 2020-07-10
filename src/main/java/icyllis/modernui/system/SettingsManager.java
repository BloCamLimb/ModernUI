/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.system;

import com.google.common.collect.Lists;
import icyllis.modernui.impl.setting.*;
import icyllis.modernui.ui.master.UITools;
import net.minecraft.client.AbstractOption;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.NewChatGui;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.*;
import net.minecraft.entity.player.ChatVisibility;
import net.minecraft.util.HandSide;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.optifine.*;
import net.optifine.config.IteratableOptionOF;
import net.optifine.util.FontUtils;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Manage almost ALL Overwritten vanilla settings (options) and OptiFine
 * settings to set or get their values or generate all option texts, and
 * cache the methods to create their entries for Modern UI settings interface
 * <p>
 * Settings are only available on client side
 *
 * @since 1.1
 * @deprecated BOMB!
 */
@Deprecated
@SuppressWarnings({"unchecked", "NoTranslation"})
@OnlyIn(Dist.CLIENT)
public enum SettingsManager {
    INSTANCE;

    public static Function<SettingScrollWindow, SSliderSettingEntry> FOV;

    /**
     * Different (from vanilla):
     * Same effect, but less computation. [0.1, 1.0]
     */
    public static Function<SettingScrollWindow, SSliderSettingEntry> CHAT_OPACITY;

    /**
     * Different (from vanilla):
     * Set minimum value to 10% rather than 0% (OFF), because we have visibility. [0.1, 1.0]
     */
    public static Function<SettingScrollWindow, SSliderSettingEntry> CHAT_SCALE;

    /**
     * Different (from vanilla):
     * Use Optifine setting, so now width in [40, 1176] rather than [40, 320]
     */
    public static Function<SettingScrollWindow, SSliderSettingEntry> CHAT_WIDTH;

    public static Function<SettingScrollWindow, SSliderSettingEntry> CHAT_HEIGHT_FOCUSED;

    public static Function<SettingScrollWindow, SSliderSettingEntry> CHAT_HEIGHT_UNFOCUSED;

    public static Function<SettingScrollWindow, SSliderSettingEntry> TEXT_BACKGROUND_OPACITY;

    public static Function<SettingScrollWindow, SSliderSettingEntry> GAMMA;


    public static Function<SettingScrollWindow, SSliderSettingEntry> SENSITIVITY;

    public static Function<SettingScrollWindow, SSliderSettingEntry> MOUSE_WHEEL_SENSITIVITY;


    public static Function<SettingScrollWindow, DSliderSettingEntry> FRAMERATE_LIMIT;

    public static Function<SettingScrollWindow, DSliderSettingEntry> RENDER_DISTANCE;

    public static Function<SettingScrollWindow, DSliderSettingEntry> BIOME_BLEND_RADIUS;

    public static Function<SettingScrollWindow, DSliderSettingEntry> MIPMAP_LEVEL;


    public static Function<SettingScrollWindow, BooleanSettingEntry> REALMS_NOTIFICATIONS;

    public static Function<SettingScrollWindow, BooleanSettingEntry> CHAT_COLOR;
    public static Function<SettingScrollWindow, BooleanSettingEntry> CHAT_LINKS;
    public static Function<SettingScrollWindow, BooleanSettingEntry> CHAT_LINKS_PROMPT;
    public static Function<SettingScrollWindow, BooleanSettingEntry> REDUCED_DEBUG_INFO;
    public static Function<SettingScrollWindow, BooleanSettingEntry> AUTO_SUGGEST_COMMANDS;

    public static Function<SettingScrollWindow, BooleanSettingEntry> SHOW_SUBTITLES;
    public static Function<SettingScrollWindow, BooleanSettingEntry> AUTO_JUMP;

    public static Function<SettingScrollWindow, BooleanSettingEntry> VSYNC;
    public static Function<SettingScrollWindow, BooleanSettingEntry> VIEW_BOBBING;

    public static Function<SettingScrollWindow, BooleanSettingEntry> ENTITY_SHADOWS;

    public static Function<SettingScrollWindow, BooleanSettingEntry> INVERT_MOUSE;
    public static Function<SettingScrollWindow, BooleanSettingEntry> DISCRETE_MOUSE_WHEEL;
    public static Function<SettingScrollWindow, BooleanSettingEntry> TOUCHSCREEN;
    public static Function<SettingScrollWindow, BooleanSettingEntry> RAW_MOUSE_INPUT;


    public static Function<SettingScrollWindow, DropdownSettingEntry> GRAPHICS;

    public static Function<SettingScrollWindow, DropdownSettingEntry> ATTACK_INDICATOR;

    public static Function<SettingScrollWindow, DropdownSettingEntry> AO;

    public static Function<SettingScrollWindow, DropdownSettingEntry> PARTICLES;

    public static Function<SettingScrollWindow, DropdownSettingEntry> CHAT_VISIBILITY;

    public static Function<SettingScrollWindow, DropdownSettingEntry> NARRATOR;

    public static Function<SettingScrollWindow, DropdownSettingEntry> MAIN_HAND;


    /**
     * OptiFine Settings
     **/

    public static Function<SettingScrollWindow, BooleanSettingEntry> DYNAMIC_FOV;

    public static Function<SettingScrollWindow, BooleanSettingEntry> CHAT_SHADOW;

    public static Function<SettingScrollWindow, BooleanSettingEntry> EMISSIVE_TEXTURES;

    public static Function<SettingScrollWindow, BooleanSettingEntry> RANDOM_ENTITIES;

    public static Function<SettingScrollWindow, BooleanSettingEntry> CUSTOM_FONTS;

    public static Function<SettingScrollWindow, BooleanSettingEntry> CUSTOM_COLORS;

    public static Function<SettingScrollWindow, BooleanSettingEntry> BETTER_SNOW;

    public static Function<SettingScrollWindow, BooleanSettingEntry> NATURAL_TEXTURES;

    public static Function<SettingScrollWindow, BooleanSettingEntry> CUSTOM_SKY;

    public static Function<SettingScrollWindow, BooleanSettingEntry> CUSTOM_ITEMS;

    public static Function<SettingScrollWindow, BooleanSettingEntry> CUSTOM_ENTITY_MODELS;

    public static Function<SettingScrollWindow, BooleanSettingEntry> CUSTOM_GUIS;

    public static Function<SettingScrollWindow, BooleanSettingEntry> USE_VBO;


    public static Function<SettingScrollWindow, SSliderSettingEntry> AO_LEVEL;


    public static Function<SettingScrollWindow, DropdownSettingEntry> CHAT_BACKGROUND;

    public static Function<SettingScrollWindow, DropdownSettingEntry> DYNAMIC_LIGHTS;

    public static Function<SettingScrollWindow, DropdownSettingEntry> MIPMAP_TYPE;

    public static Function<SettingScrollWindow, DropdownSettingEntry> BETTER_GRASS;

    public static Function<SettingScrollWindow, DropdownSettingEntry> CONNECTED_TEXTURES;


    static {
        /*if (ModIntegration.optifineLoaded) {
            try {
                Field field;
                field = AbstractOption.class.getDeclaredField("AO_LEVEL");
                SliderPercentageOption ao_level = (SliderPercentageOption) field.get(AbstractOption.class);
                AO_LEVEL = LazyOptional.of(() ->
                        INSTANCE.transformToSmooth(ao_level, ConstantsLibrary.PERCENTAGE_STRING_FUNC));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }*/
    }


    private final Minecraft minecraft;

    /**
     * The instance of game settings
     */
    private final GameSettings gameSettings;

    private final Field option_translateKey;

    private final Field slider_minValue;
    private final Field slider_maxValue;
    private final Field slider_stepSize;
    private final Field slider_getter;
    private final Field slider_setter;

    private final Field boolean_getter;
    private final Field boolean_setter;

    /**
     * Belows are OptiFine's
     */
    private Field of_dynamic_fov;

    private Field of_chat_background;

    private Field of_chat_shadow;

    private Field of_ao_level;

    private Field of_dynamic_lights;

    private Field of_use_vbo;

    private Field of_mipmap_type;

    private Field of_emissive_textures;

    private Field of_random_entities;

    private Field of_better_grass;

    private Field of_better_snow;

    private Field of_custom_fonts;

    private Field of_custom_colors;

    private Field of_connected_textures;

    private Field of_natural_textures;

    private Field of_custom_sky;

    private Field of_custom_items;

    private Field of_custom_entity_models;

    private Field of_custom_guis;

    {
        minecraft = Minecraft.getInstance();
        gameSettings = minecraft.gameSettings;

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
                of_ao_level = GameSettings.class.getDeclaredField("ofAoLevel");
                of_dynamic_lights = GameSettings.class.getDeclaredField("ofDynamicLights");
                of_use_vbo = GameSettings.class.getDeclaredField("ofUseVbo");
                of_mipmap_type = GameSettings.class.getDeclaredField("ofMipmapType");
                of_emissive_textures = GameSettings.class.getDeclaredField("ofEmissiveTextures");
                of_random_entities = GameSettings.class.getDeclaredField("ofRandomEntities");
                of_better_grass = GameSettings.class.getDeclaredField("ofBetterGrass");
                of_better_snow = GameSettings.class.getDeclaredField("ofBetterSnow");
                of_custom_fonts = GameSettings.class.getDeclaredField("ofCustomFonts");
                of_custom_colors = GameSettings.class.getDeclaredField("ofCustomColors");
                of_connected_textures = GameSettings.class.getDeclaredField("ofConnectedTextures");
                of_natural_textures = GameSettings.class.getDeclaredField("ofNaturalTextures");
                of_custom_sky = GameSettings.class.getDeclaredField("ofCustomSky");
                of_custom_items = GameSettings.class.getDeclaredField("ofCustomItems");
                of_custom_entity_models = GameSettings.class.getDeclaredField("ofCustomEntityModels");
                of_custom_guis = GameSettings.class.getDeclaredField("ofCustomGuis");
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                throw new RuntimeException();
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

    public void buildAllSettings() {
        FOV = INSTANCE
                .transformToSmooth(AbstractOption.FOV);
        CHAT_OPACITY = INSTANCE
                .transformToSmooth(AbstractOption.CHAT_OPACITY, p -> (int) (p * 90 + 10) + "%");
        CHAT_SCALE = INSTANCE
                .transformToSmooth(AbstractOption.CHAT_SCALE, Triple.of(0.1, null, null), UITools::percentageToString);
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
                .transformToSmooth(AbstractOption.ACCESSIBILITY_TEXT_BACKGROUND_OPACITY, UITools::percentageToString);
        GAMMA = INSTANCE
                .transformToSmooth(AbstractOption.GAMMA, UITools::percentageToString);
        SENSITIVITY = INSTANCE
                .transformToSmooth(AbstractOption.SENSITIVITY, Triple.of(null, null, 0.005f), p -> (int) (p * 200) + "%");
        MOUSE_WHEEL_SENSITIVITY = INSTANCE
                .transformToSmooth(AbstractOption.MOUSE_WHEEL_SENSITIVITY, UITools::percentageToString);
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

        FRAMERATE_LIMIT = window -> new DSliderSettingEntry(window, I18n.format("options.framerateLimit"),
                1, 52, gameSettings.framerateLimit / 5, i -> {
            gameSettings.framerateLimit = i * 5;
            minecraft.getMainWindow().setFramerateLimit(gameSettings.framerateLimit);
        }, i -> i > 51 ? "260+" : Integer.toString(i * 5), true);

        MIPMAP_LEVEL = window -> new DSliderSettingEntry(window, I18n.format("options.mipmapLevels"),
                0, 4, gameSettings.mipmapLevels, i -> {
            gameSettings.mipmapLevels = i;
            this.minecraft.setMipmapLevels(i);
            this.minecraft.scheduleResourcesRefresh(); // Forge?
        }, String::valueOf, false);

        //FIXME
        /*GRAPHICS = window -> new DropdownSettingEntry(window, I18n.format("options.graphics"),
                getGraphicTexts(),
                gameSettings. ? 0 : 1, i -> {
            gameSettings.fancyGraphics = i == 0;
            minecraft.worldRenderer.loadRenderers();
        });*/

        ATTACK_INDICATOR = window -> new DropdownSettingEntry(window, I18n.format("options.attackIndicator"),
                getAttackIndicatorTexts(),
                gameSettings.attackIndicator.ordinal(), i -> gameSettings.attackIndicator = AttackIndicatorStatus.values()[i]);

        AO = window -> new DropdownSettingEntry(window, I18n.format("options.ao"),
                getAoTexts(),
                gameSettings.ambientOcclusionStatus.ordinal(), i -> {
            gameSettings.ambientOcclusionStatus = AmbientOcclusionStatus.values()[i];
            minecraft.worldRenderer.loadRenderers();
        });

        PARTICLES = window -> new DropdownSettingEntry(window, I18n.format("options.particles"),
                getParticleTexts(),
                gameSettings.particles.ordinal(), i -> gameSettings.particles = ParticleStatus.values()[i]);

        CHAT_VISIBILITY = window -> new DropdownSettingEntry(window, I18n.format("options.chat.visibility"),
                getChatVisibilityTexts(),
                gameSettings.chatVisibility.ordinal(), i -> gameSettings.chatVisibility = ChatVisibility.values()[i]);

        NARRATOR = window -> new DropdownSettingEntry(window, I18n.format("options.narrator"),
                getNarratorTexts(),
                NarratorChatListener.INSTANCE.isActive() ? gameSettings.narrator.ordinal() : 0, i -> {
            gameSettings.narrator = NarratorStatus.values()[i];
            NarratorChatListener.INSTANCE.announceMode(gameSettings.narrator);
        });

        MAIN_HAND = window -> new DropdownSettingEntry(window, I18n.format("options.mainHand"),
                getMainHandTexts(),
                gameSettings.mainHand.ordinal(), i -> {
            gameSettings.mainHand = HandSide.values()[i];
            gameSettings.saveOptions();
            gameSettings.sendSettingsToServer();
        });

        if (ModIntegration.optifineLoaded) {

            DYNAMIC_FOV = window -> new BooleanSettingEntry(window, I18n.format("of.options.DYNAMIC_FOV"),
                    SettingsManager.INSTANCE.getDynamicFov(), SettingsManager.INSTANCE::setDynamicFov);

            AO_LEVEL = window -> new SSliderSettingEntry(window, I18n.format("of.options.AO_LEVEL"),
                    0, 1, 0,
                    SettingsManager.INSTANCE.getAoLevel(), SettingsManager.INSTANCE::setAoLevel,
                    UITools::percentageToString, false);

            CHAT_SHADOW = window -> new BooleanSettingEntry(window, I18n.format("of.options.CHAT_SHADOW"),
                    SettingsManager.INSTANCE.getChatShadow(), SettingsManager.INSTANCE::setChatShadow);

            CHAT_BACKGROUND = window -> new DropdownSettingEntry(window, I18n.format("of.options.CHAT_BACKGROUND"),
                    SettingsManager.INSTANCE.getChatBackgroundTexts(),
                    SettingsManager.INSTANCE.getChatBackgroundIndex(), SettingsManager.INSTANCE::setChatBackgroundIndex);

            DYNAMIC_LIGHTS = window -> new DropdownSettingEntry(window, I18n.format("of.options.DYNAMIC_LIGHTS"),
                    SettingsManager.INSTANCE.getOffFastFancy(),
                    SettingsManager.INSTANCE.getDynamicLightsIndex(), SettingsManager.INSTANCE::setDynamicLightsIndex);

            USE_VBO = window -> new BooleanSettingEntry(window, I18n.format("options.vbo"),
                    SettingsManager.INSTANCE.getUseVbo(), SettingsManager.INSTANCE::setUseVbo);

            MIPMAP_TYPE = window -> new DropdownSettingEntry(window, I18n.format("of.options.MIPMAP_TYPE"),
                    getMipmapTypeTexts(),
                    getMipmapTypeIndex(), this::setMipmapTypeIndex);

            EMISSIVE_TEXTURES = window -> new BooleanSettingEntry(window, I18n.format("of.options.EMISSIVE_TEXTURES"),
                    getEmissiveTextures(), this::setEmissiveTextures);

            RANDOM_ENTITIES = window -> new BooleanSettingEntry(window, I18n.format("of.options.RANDOM_ENTITIES"),
                    getRandomEntities(), this::setRandomEntities);

            BETTER_GRASS = window -> new DropdownSettingEntry(window, I18n.format("of.options.BETTER_GRASS"),
                    getOffFastFancy(),
                    getBetterGrassIndex(), this::setBetterGrass);

            BETTER_SNOW = window -> new BooleanSettingEntry(window, I18n.format("of.options.BETTER_SNOW"),
                    getBetterSnow(), this::setBetterSnow);

            CUSTOM_FONTS = window -> new BooleanSettingEntry(window, I18n.format("of.options.CUSTOM_FONTS"),
                    getCustomFonts(), this::setCustomFonts);

            CUSTOM_COLORS = window -> new BooleanSettingEntry(window, I18n.format("of.options.CUSTOM_COLORS"),
                    getCustomColors(), this::setCustomColors);

            CONNECTED_TEXTURES = window -> new DropdownSettingEntry(window, I18n.format("of.options.CONNECTED_TEXTURES"),
                    getOffFastFancy(),
                    getConnectedTexturesIndex(), this::setConnectedTexturesIndex);

            NATURAL_TEXTURES = window -> new BooleanSettingEntry(window, I18n.format("of.options.NATURAL_TEXTURES"),
                    getNaturalTextures(), this::setNaturalTextures);

            CUSTOM_SKY = window -> new BooleanSettingEntry(window, I18n.format("of.options.CUSTOM_SKY"),
                    getCustomSky(), this::setCustomSky);

            CUSTOM_ITEMS = window -> new BooleanSettingEntry(window, I18n.format("of.options.CUSTOM_ITEMS"),
                    getCustomItems(), this::setCustomItems);

            CUSTOM_ENTITY_MODELS = window -> new BooleanSettingEntry(window, I18n.format("of.options.CUSTOM_ENTITY_MODELS"),
                    getCustomEntityModels(), this::setCustomEntityModels);

            CUSTOM_GUIS = window -> new BooleanSettingEntry(window, I18n.format("of.options.CUSTOM_GUIS"),
                    getCustomGuis(), this::setCustomGuis);

        }
    }

    @Nonnull
    public Function<SettingScrollWindow, SSliderSettingEntry> transformToSmooth(SliderPercentageOption instance) {
        return transformToSmooth(instance, null, null);
    }

    @Nonnull
    public Function<SettingScrollWindow, SSliderSettingEntry> transformToSmooth(SliderPercentageOption instance, Function<Double, String> stringFunction) {
        return transformToSmooth(instance, null, stringFunction);
    }

    @Nonnull
    public Function<SettingScrollWindow, SSliderSettingEntry> transformToSmooth(SliderPercentageOption instance, Triple<Double, Double, Float> customize) {
        return transformToSmooth(instance, customize, null);
    }

    @Nonnull
    public Function<SettingScrollWindow, SSliderSettingEntry> transformToSmooth(SliderPercentageOption instance, @Nullable Triple<Double, Double, Float> customize, @Nullable Function<Double, String> stringFunction) {
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
                return window -> new SSliderSettingEntry(window, I18n.format(translationKey), minValue, maxValue,
                        stepSize, getter.apply(gameSettings), v -> setter.accept(gameSettings, v), String::valueOf, true);
            } else {
                return window -> new SSliderSettingEntry(window, I18n.format(translationKey), minValue, maxValue,
                        stepSize, getter.apply(gameSettings), v -> setter.accept(gameSettings, v), stringFunction, true);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException();
    }

    @Nonnull
    public OptiFineSettingEntry transformToOptiFine(SettingScrollWindow window, IteratableOptionOF instance) {
        try {
            return new OptiFineSettingEntry(window, I18n.format((String) option_translateKey.get(instance)), instance);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException();
    }

    @Nonnull
    public Function<SettingScrollWindow, DSliderSettingEntry> transformToDiscrete(SliderPercentageOption instance, boolean dynamicModify) {
        return this.transformToDiscrete(instance, String::valueOf, dynamicModify);
    }

    @Nonnull
    public Function<SettingScrollWindow, DSliderSettingEntry> transformToDiscrete(SliderPercentageOption instance, Function<Integer, String> displayStringFunc, boolean dynamicModify) {
        GameSettings gameSettings = Minecraft.getInstance().gameSettings;
        try {
            String translationKey = (String) option_translateKey.get(instance);
            int minValue;
            int maxValue;
            minValue = (int) slider_minValue.getDouble(instance);
            maxValue = (int) slider_maxValue.getDouble(instance);
            Function<GameSettings, Double> getter = (Function<GameSettings, Double>) slider_getter.get(instance);
            BiConsumer<GameSettings, Double> setter = (BiConsumer<GameSettings, Double>) slider_setter.get(instance);
            return window -> new DSliderSettingEntry(window, I18n.format(translationKey), minValue, maxValue,
                    getter.apply(gameSettings).intValue(), v -> setter.accept(gameSettings, (double) v), displayStringFunc, dynamicModify);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException();
    }

    @Nonnull
    public Function<SettingScrollWindow, BooleanSettingEntry> transformToBoolean(BooleanOption instance) {
        GameSettings gameSettings = Minecraft.getInstance().gameSettings;
        try {
            String translationKey = (String) option_translateKey.get(instance);
            Predicate<GameSettings> getter = (Predicate<GameSettings>) boolean_getter.get(instance);
            BiConsumer<GameSettings, Boolean> setter = (BiConsumer<GameSettings, Boolean>) boolean_setter.get(instance);
            return window -> new BooleanSettingEntry(window, I18n.format(translationKey), getter.test(gameSettings), b -> setter.accept(gameSettings, b));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException();
    }

    @Deprecated
    public <T extends AbstractOption> Function<SettingScrollWindow, SettingEntry> transformVanillaOption(T abstractOption) {
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
            } else if (abstractOption instanceof IteratableOption) {
                // There's no way to do this at present, we should get all selective options name before iterate or a new way. awa
                ModernUI.LOGGER.fatal("Iterable option found, {} with name {}", abstractOption, translationKey);
            }*/
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException();
    }

    @Nonnull
    public List<String> getGraphicTexts() {
        return Lists.newArrayList(
                I18n.format("options.graphics.fancy"),
                I18n.format("options.graphics.fast")
        );
    }

    public List<String> getAttackIndicatorTexts() {
        return Lists.newArrayList(AttackIndicatorStatus.values()).stream()
                .map(m -> I18n.format(m.getResourceKey())).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<String> getAoTexts() {
        return Lists.newArrayList(AmbientOcclusionStatus.values()).stream()
                .map(m -> I18n.format(m.getResourceKey())).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<String> getParticleTexts() {
        return Lists.newArrayList(ParticleStatus.values()).stream()
                .map(m -> I18n.format(m.getResourceKey())).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<String> getMainHandTexts() {
        return Lists.newArrayList(HandSide.values()).stream()
                .map(HandSide::toString).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<String> getChatVisibilityTexts() {
        return Lists.newArrayList(ChatVisibility.values()).stream()
                .map(c -> I18n.format(c.getResourceKey())).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<String> getNarratorTexts() {
        return NarratorChatListener.INSTANCE.isActive() ?
                Lists.newArrayList(NarratorStatus.values()).stream()
                        .map(n -> n.func_238233_b_().getString()).collect(Collectors.toCollection(ArrayList::new)) :
                Lists.newArrayList(I18n.format("options.narrator.notavailable"));
    }


    /* All belows are OptiFine soft compatibility */

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

    public boolean getUseVbo() {
        try {
            return of_use_vbo.getBoolean(gameSettings);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setUseVbo(boolean b) {
        try {
            of_use_vbo.setBoolean(gameSettings, b);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        minecraft.worldRenderer.loadRenderers();
    }

    @Nonnull
    @SuppressWarnings("NoTranslation")
    public List<String> getChatBackgroundTexts() {
        return Lists.newArrayList(
                I18n.format("generator.default"),
                I18n.format("of.general.compact"),
                I18n.format("options.off")
        );
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

    public void setChatBackgroundIndex(int i) {
        try {
            if (i == 1) {
                of_chat_background.setInt(gameSettings, 5);
            } else if (i == 2) {
                of_chat_background.setInt(gameSettings, 3);
            } else {
                of_chat_background.setInt(gameSettings, 0);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Nonnull
    public List<String> getOffFastFancy() {
        return Lists.newArrayList(
                I18n.format("options.off"),
                I18n.format("options.graphics.fast"),
                I18n.format("options.graphics.fancy")
        );
    }

    public int getDynamicLightsIndex() {
        try {
            int c = of_dynamic_lights.getInt(gameSettings);
            if (c == 3) {
                return 0;
            } else if (c == 1) {
                return 1;
            } else {
                return 2;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void setDynamicLightsIndex(int i) {
        try {
            if (i == 0) {
                of_dynamic_lights.setInt(gameSettings, 3);
            } else if (i == 1) {
                of_dynamic_lights.setInt(gameSettings, 1);
            } else {
                of_dynamic_lights.setInt(gameSettings, 2);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        try {
            Class<?> clazz = Class.forName("net.optifine.DynamicLights");
            Method method = clazz.getDeclaredMethod("removeLights", WorldRenderer.class);
            method.invoke(null, minecraft.worldRenderer);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
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

    public double getAoLevel() {
        try {
            return of_ao_level.getDouble(gameSettings);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void setAoLevel(double d) {
        try {
            of_ao_level.setDouble(gameSettings, d);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        minecraft.worldRenderer.loadRenderers();
    }

    @Nonnull
    public List<String> getMipmapTypeTexts() {
        return Lists.newArrayList(
                I18n.format("of.options.mipmap.nearest"),
                I18n.format("of.options.mipmap.linear"),
                I18n.format("of.options.mipmap.bilinear"),
                I18n.format("of.options.mipmap.trilinear")
        );
    }

    public int getMipmapTypeIndex() {
        try {
            return of_mipmap_type.getInt(gameSettings);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void setMipmapTypeIndex(int i) {
        try {
            of_mipmap_type.setInt(gameSettings, i);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        try {
            GameSettings.class.getDeclaredMethod("updateMipmaps").invoke(gameSettings);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public boolean getEmissiveTextures() {
        try {
            return of_emissive_textures.getBoolean(gameSettings);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setEmissiveTextures(boolean b) {
        try {
            of_emissive_textures.setBoolean(gameSettings, b);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        minecraft.scheduleResourcesRefresh();
    }

    public boolean getRandomEntities() {
        try {
            return of_random_entities.getBoolean(gameSettings);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setRandomEntities(boolean b) {
        try {
            of_random_entities.setBoolean(gameSettings, b);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        RandomEntities.update();
        /*try {
            // FaQ
            Class.forName("net.optifine.RandomEntities").getDeclaredMethod("update").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }*/
    }

    public int getBetterGrassIndex() {
        try {
            return of_better_grass.getInt(gameSettings) - 1;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void setBetterGrass(int i) {
        try {
            of_better_grass.setInt(gameSettings, i + 1);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        minecraft.worldRenderer.loadRenderers();
    }

    public boolean getBetterSnow() {
        try {
            return of_better_snow.getBoolean(gameSettings);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setBetterSnow(boolean b) {
        try {
            of_better_snow.setBoolean(gameSettings, b);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        minecraft.worldRenderer.loadRenderers();
    }

    public boolean getCustomFonts() {
        try {
            return of_custom_fonts.getBoolean(gameSettings);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setCustomFonts(boolean b) {
        try {
            of_custom_fonts.setBoolean(gameSettings, b);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        FontUtils.reloadFonts();
        /*try {
            Class.forName("net.optifine.util.FontUtils").getDeclaredMethod("reloadFonts").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }*/
    }

    public boolean getCustomColors() {
        try {
            return of_custom_colors.getBoolean(gameSettings);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setCustomColors(boolean b) {
        try {
            of_custom_colors.setBoolean(gameSettings, b);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        CustomColors.update();
        /*try {
            Class.forName("net.optifine.CustomColors").getDeclaredMethod("update").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }*/
        minecraft.worldRenderer.loadRenderers();
    }

    public int getConnectedTexturesIndex() {
        try {
            int i = of_connected_textures.getInt(gameSettings);
            if (i == 3) {
                return 0;
            } else {
                return i;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void setConnectedTexturesIndex(int i) {
        try {
            if (i == 0) {
                i = 3;
            }
            of_connected_textures.set(gameSettings, i);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        if ((getConnectedTexturesIndex() == 1 || getConnectedTexturesIndex() == 2) && (i == 1 || i == 2)) {
            minecraft.worldRenderer.loadRenderers();
        } else {
            minecraft.scheduleResourcesRefresh();
        }
    }

    public boolean getNaturalTextures() {
        try {
            return of_natural_textures.getBoolean(gameSettings);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setNaturalTextures(boolean b) {
        try {
            of_natural_textures.setBoolean(gameSettings, b);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        NaturalTextures.update();
        /*try {
            Class.forName("net.optifine.NaturalTextures").getDeclaredMethod("update").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }*/
        minecraft.worldRenderer.loadRenderers();
    }

    public boolean getCustomSky() {
        try {
            return of_custom_sky.getBoolean(gameSettings);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setCustomSky(boolean b) {
        try {
            of_custom_sky.setBoolean(gameSettings, b);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        CustomSky.update();
        /*try {
            Class.forName("net.optifine.CustomSky").getDeclaredMethod("update").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }*/
    }

    public boolean getCustomItems() {
        try {
            return of_custom_items.getBoolean(gameSettings);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setCustomItems(boolean b) {
        try {
            of_custom_items.setBoolean(gameSettings, b);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        minecraft.scheduleResourcesRefresh();
    }

    public boolean getCustomEntityModels() {
        try {
            return of_custom_entity_models.getBoolean(gameSettings);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setCustomEntityModels(boolean b) {
        try {
            of_custom_entity_models.setBoolean(gameSettings, b);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        minecraft.scheduleResourcesRefresh();
    }

    public boolean getCustomGuis() {
        try {
            return of_custom_guis.getBoolean(gameSettings);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setCustomGuis(boolean b) {
        try {
            of_custom_guis.setBoolean(gameSettings, b);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        CustomGuis.update();
        /*try {
            Class.forName("net.optifine.CustomGuis").getDeclaredMethod("update").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }*/
    }

}
