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

package icyllis.modernui.gui.module;

import com.google.common.collect.Lists;
import icyllis.modernui.gui.master.Module;
import icyllis.modernui.gui.option.*;
import icyllis.modernui.gui.scroll.SettingScrollWindow;
import icyllis.modernui.system.ConstantsLibrary;
import icyllis.modernui.system.ModIntegration;
import icyllis.modernui.system.SettingsManager;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.AmbientOcclusionStatus;
import net.minecraft.client.settings.AttackIndicatorStatus;
import net.minecraft.client.settings.ParticleStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SettingVideo extends Module {

    private static Supplier<List<String>> AO = () -> Lists.newArrayList(AmbientOcclusionStatus.values()).stream().map(m -> I18n.format(m.getResourceKey())).collect(Collectors.toCollection(ArrayList::new));

    private static Supplier<List<String>> PARTICLES = () -> Lists.newArrayList(ParticleStatus.values()).stream().map(m -> I18n.format(m.getResourceKey())).collect(Collectors.toCollection(ArrayList::new));

    private Minecraft minecraft;

    private SettingScrollWindow window;

    private GuiScaleOptionEntry guiScale;

    public SettingVideo() {
        this.minecraft = Minecraft.getInstance();
        this.window = new SettingScrollWindow(this);

        List<OptionCategoryGroup> groups = new ArrayList<>();

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

    private void addVideoCategory(List<OptionCategoryGroup> groups) {
        List<OptionEntry> list = new ArrayList<>();

        list.add(SettingsManager.GRAPHICS.apply(window));

        list.add(SettingsManager.RENDER_DISTANCE.apply(window));

        list.add(SettingsManager.FRAMERATE_LIMIT.apply(window));

        list.add(SettingsManager.FOV.apply(window));

        guiScale = new GuiScaleOptionEntry(window);
        list.add(guiScale);

        list.add(SettingsManager.GAMMA.apply(window));

        list.add(SettingsManager.ATTACK_INDICATOR.apply(window));

        list.add(SettingsManager.VIEW_BOBBING.apply(window));

        list.add(SettingsManager.VSYNC.apply(window));

        if (ModIntegration.optifineLoaded) {
            list.add(SettingsManager.DYNAMIC_FOV.apply(window));
            //TODO optifine Dynamic Lights and Use VBOs
        }

        OptionCategoryGroup categoryGroup = new OptionCategoryGroup(window, I18n.format("gui.modernui.settings.category.video"), list);
        groups.add(categoryGroup);
    }

    @SuppressWarnings("NoTranslation")
    private void addQualityCategory(List<OptionCategoryGroup> groups) {
        List<OptionEntry> list = new ArrayList<>();
        GameSettings gameSettings = minecraft.gameSettings;

        DropdownOptionEntry ao = new DropdownOptionEntry(window, I18n.format("options.ao"), AO.get(),
                gameSettings.ambientOcclusionStatus.ordinal(), i -> {
            gameSettings.ambientOcclusionStatus = AmbientOcclusionStatus.values()[i];
            minecraft.worldRenderer.loadRenderers();
        });
        list.add(ao);

        SSliderOptionEntry aoLevel = new SSliderOptionEntry(window, I18n.format("of.options.AO_LEVEL"), 0, 1, 0,
                SettingsManager.INSTANCE.getAoLevel(), SettingsManager.INSTANCE::setAoLevel, ConstantsLibrary.PERCENTAGE_STRING_FUNC)
                .setApplyChange(d -> minecraft.worldRenderer.loadRenderers());

        list.add(aoLevel);

        DSliderOptionEntry mipmapLevel = new DSliderOptionEntry(window, I18n.format("options.mipmapLevels"),
                0, 4, gameSettings.mipmapLevels, i -> gameSettings.mipmapLevels = i, String::valueOf).
                setApplyChange(i -> {
                    this.minecraft.setMipmapLevels(i);
                    this.minecraft.scheduleResourcesRefresh(); // Forge?
                });
        list.add(mipmapLevel);

        if (ModIntegration.optifineLoaded) {
            //TODO optifine Mipmap Type, AF Level, AA Level, Emissive Textures, Random Entities, Better Grass, Better Snow,
            // Custom Fonts, Custom Colors, Connected Textures, Natural Textures, Custom Sky, Custom Items, Custom Entity Models, Custom Guis
        }

        OptionCategoryGroup categoryGroup = new OptionCategoryGroup(window, I18n.format("gui.modernui.settings.category.quality"), list);
        groups.add(categoryGroup);
    }

    private void addDetailsCategory(List<OptionCategoryGroup> groups) {
        List<OptionEntry> list = new ArrayList<>();
        GameSettings gameSettings = minecraft.gameSettings;

        list.add(SettingsManager.ENTITY_SHADOWS.apply(window));
        list.add(SettingsManager.BIOME_BLEND_RADIUS.apply(window));

        if (ModIntegration.optifineLoaded) {
            //TODO optifine Clouds, Cloud Height, Trees, Rain, Sky, Stars, Sun Moon, Show Capes, Fog Fancy, Fog Start,
            //      Translucent Blocks, Held Item Tooltips, Dropped Items, Vignette, Alternate Blocks, Swamp Colors
        }

        OptionCategoryGroup categoryGroup = new OptionCategoryGroup(window, I18n.format("gui.modernui.settings.category.details"), list);
        groups.add(categoryGroup);
    }

    private void addAnimationsCategory(List<OptionCategoryGroup> groups) {
        List<OptionEntry> list = new ArrayList<>();
        GameSettings gameSettings = minecraft.gameSettings;

        DropdownOptionEntry particles = new DropdownOptionEntry(window, I18n.format("options.particles"), PARTICLES.get(),
                gameSettings.particles.ordinal(), i -> gameSettings.particles = ParticleStatus.values()[i]);
        list.add(particles);

        if (ModIntegration.optifineLoaded) {
            //TODO optifine Animated Water, Animated Lava, Animated Fire, Animated Portal, Animated Redstone, Animated Explosion, Animated Flame, Animated Smoke, Void Particles, Water Particles,
            //      Rain Splash, Portal Particles, Potion Particles, Dripping Water Lava, Animated Terrain, Animated Textures, Firework Particles
        }

        OptionCategoryGroup categoryGroup = new OptionCategoryGroup(window, I18n.format("gui.modernui.settings.category.animations"), list);
        groups.add(categoryGroup);
    }

    private void addPerformanceCategory(List<OptionCategoryGroup> groups) {
        List<OptionEntry> list = new ArrayList<>();
        GameSettings gameSettings = minecraft.gameSettings;

        if (ModIntegration.optifineLoaded) {
            //TODO optifine Smooth Fps, Smooth World, Fast Render, Fast Math, Chunk Updates, Chunk Updates Dynamic, Render Regions, Lazy Chunk Loading, Smart Animations
        }

        OptionCategoryGroup categoryGroup = new OptionCategoryGroup(window, I18n.format("gui.modernui.settings.category.performance"), list);
        groups.add(categoryGroup);
    }

    private void addOtherCategory(List<OptionCategoryGroup> groups) {
        List<OptionEntry> list = new ArrayList<>();
        GameSettings gameSettings = minecraft.gameSettings;


        OptionCategoryGroup categoryGroup = new OptionCategoryGroup(window, I18n.format("gui.modernui.settings.category.other"), list);
        groups.add(categoryGroup);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        guiScale.onResized();
    }

}
