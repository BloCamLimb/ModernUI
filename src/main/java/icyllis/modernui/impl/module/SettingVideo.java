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

package icyllis.modernui.impl.module;

import icyllis.modernui.gui.master.Module;
import icyllis.modernui.impl.setting.GuiScaleSettingEntry;
import icyllis.modernui.impl.setting.SettingCategoryGroup;
import icyllis.modernui.impl.setting.SettingEntry;
import icyllis.modernui.impl.setting.SettingScrollWindow;
import icyllis.modernui.system.ModIntegration;
import icyllis.modernui.system.SettingsManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.AbstractOption;
import net.optifine.config.IteratableOptionOF;
import net.optifine.gui.GuiAnimationSettingsOF;
import net.optifine.gui.GuiDetailSettingsOF;
import net.optifine.gui.GuiPerformanceSettingsOF;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SettingVideo extends Module {

    private SettingScrollWindow window;

    private GuiScaleSettingEntry guiScale;

    public SettingVideo() {
        this.window = new SettingScrollWindow(this);

        List<SettingCategoryGroup> groups = new ArrayList<>();

        addVideoCategory(groups);
        addQualityCategory(groups);
        addDetailsCategory(groups);
        addAnimationsCategory(groups);

        if (ModIntegration.optifineLoaded) {
            addPerformanceCategory(groups);
            addOtherCategory(groups);
        }

        window.addGroups(groups);

        addWidget(window);
    }

    private void addVideoCategory(List<SettingCategoryGroup> groups) {
        List<SettingEntry> list = new ArrayList<>();

        list.add(SettingsManager.GRAPHICS.apply(window));

        list.add(SettingsManager.RENDER_DISTANCE.apply(window));

        list.add(SettingsManager.FRAMERATE_LIMIT.apply(window));

        list.add(SettingsManager.FOV.apply(window));

        guiScale = new GuiScaleSettingEntry(window);
        list.add(guiScale);

        list.add(SettingsManager.GAMMA.apply(window));

        list.add(SettingsManager.ATTACK_INDICATOR.apply(window));

        list.add(SettingsManager.VIEW_BOBBING.apply(window));

        list.add(SettingsManager.VSYNC.apply(window));

        if (ModIntegration.optifineLoaded) {

            list.add(SettingsManager.DYNAMIC_FOV.apply(window));

            list.add(SettingsManager.DYNAMIC_LIGHTS.apply(window));

            list.add(SettingsManager.USE_VBO.apply(window));
        }

        SettingCategoryGroup categoryGroup = new SettingCategoryGroup(window, I18n.format("gui.modernui.settings.category.video"), list);
        groups.add(categoryGroup);
    }

    private void addQualityCategory(List<SettingCategoryGroup> groups) {
        List<SettingEntry> list = new ArrayList<>();

        list.add(SettingsManager.AO.apply(window));

        if (ModIntegration.optifineLoaded) {
            list.add(SettingsManager.AO_LEVEL.apply(window));
        }

        list.add(SettingsManager.MIPMAP_LEVEL.apply(window));

        if (ModIntegration.optifineLoaded) {

            list.add(SettingsManager.MIPMAP_TYPE.apply(window));
            //TODO optifine (WIP) AF Level, AA Level
            list.add(SettingsManager.EMISSIVE_TEXTURES.apply(window));

            list.add(SettingsManager.RANDOM_ENTITIES.apply(window));

            list.add(SettingsManager.BETTER_GRASS.apply(window));

            list.add(SettingsManager.BETTER_SNOW.apply(window));

            list.add(SettingsManager.CUSTOM_FONTS.apply(window));

            list.add(SettingsManager.CUSTOM_COLORS.apply(window));

            list.add(SettingsManager.CONNECTED_TEXTURES.apply(window));

            list.add(SettingsManager.NATURAL_TEXTURES.apply(window));

            list.add(SettingsManager.CUSTOM_SKY.apply(window));

            list.add(SettingsManager.CUSTOM_ITEMS.apply(window));

            list.add(SettingsManager.CUSTOM_ENTITY_MODELS.apply(window));

            list.add(SettingsManager.CUSTOM_GUIS.apply(window));
        }

        SettingCategoryGroup categoryGroup = new SettingCategoryGroup(window, I18n.format("gui.modernui.settings.category.quality"), list);
        groups.add(categoryGroup);
    }

    private void addDetailsCategory(List<SettingCategoryGroup> groups) {
        List<SettingEntry> list = new ArrayList<>();

        list.add(SettingsManager.ENTITY_SHADOWS.apply(window));
        list.add(SettingsManager.BIOME_BLEND_RADIUS.apply(window));

        if (ModIntegration.optifineLoaded) {
            try {
                Field field = GuiDetailSettingsOF.class.getDeclaredField("enumOptions");
                field.setAccessible(true);
                for (AbstractOption option : (AbstractOption[]) field.get(null)) {
                    if (option instanceof IteratableOptionOF) {
                        list.add(SettingsManager.INSTANCE.transformToOptiFine(window, (IteratableOptionOF) option));
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
            //  Clouds, Cloud Height, Trees, Rain, Sky, Stars, Sun Moon, Show Capes, Fog Fancy, Fog Start,
            //  Translucent Blocks, Held Item Tooltips, Dropped Items, Vignette, Alternate Blocks, Swamp Colors
        }

        SettingCategoryGroup categoryGroup = new SettingCategoryGroup(window, I18n.format("gui.modernui.settings.category.details"), list);
        groups.add(categoryGroup);
    }

    private void addAnimationsCategory(List<SettingCategoryGroup> groups) {
        List<SettingEntry> list = new ArrayList<>();

        list.add(SettingsManager.PARTICLES.apply(window));

        if (ModIntegration.optifineLoaded) {
            try {
                Field field = GuiAnimationSettingsOF.class.getDeclaredField("enumOptions");
                field.setAccessible(true);
                for (AbstractOption option : (AbstractOption[]) field.get(null)) {
                    if (option instanceof IteratableOptionOF) {
                        list.add(SettingsManager.INSTANCE.transformToOptiFine(window, (IteratableOptionOF) option));
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
            //  Animated Water, Animated Lava, Animated Fire, Animated Portal, Animated Redstone, Animated Explosion, Animated Flame, Animated Smoke, Void Particles, Water Particles,
            //  Rain Splash, Portal Particles, Potion Particles, Dripping Water Lava, Animated Terrain, Animated Textures, Firework Particles
        }

        SettingCategoryGroup categoryGroup = new SettingCategoryGroup(window, I18n.format("gui.modernui.settings.category.animations"), list);
        groups.add(categoryGroup);
    }

    private void addPerformanceCategory(List<SettingCategoryGroup> groups) {
        List<SettingEntry> list = new ArrayList<>();

        try {
            Field field = GuiPerformanceSettingsOF.class.getDeclaredField("enumOptions");
            field.setAccessible(true);
            for (AbstractOption option : (AbstractOption[]) field.get(null)) {
                if (option instanceof IteratableOptionOF) {
                    list.add(SettingsManager.INSTANCE.transformToOptiFine(window, (IteratableOptionOF) option));
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        //  Smooth Fps, Smooth World, Fast Render, Fast Math, Chunk Updates, Chunk Updates Dynamic, Render Regions, Lazy Chunk Loading, Smart Animations

        SettingCategoryGroup categoryGroup = new SettingCategoryGroup(window, I18n.format("gui.modernui.settings.category.performance"), list);
        groups.add(categoryGroup);
    }

    private void addOtherCategory(@Nonnull List<SettingCategoryGroup> groups) {
        List<SettingEntry> list = new ArrayList<>();

        SettingCategoryGroup categoryGroup = new SettingCategoryGroup(window, I18n.format("gui.modernui.settings.category.other"), list);
        groups.add(categoryGroup);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        guiScale.onResized();
    }

}
