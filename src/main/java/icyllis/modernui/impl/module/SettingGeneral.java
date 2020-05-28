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

import com.google.common.collect.Lists;
import icyllis.modernui.gui.master.IModule;
import icyllis.modernui.gui.master.Module;
import icyllis.modernui.impl.setting.*;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.popup.ConfirmCallback;
import icyllis.modernui.gui.popup.PopupConfirm;
import icyllis.modernui.impl.setting.SettingScrollWindow;
import icyllis.modernui.system.ModIntegration;
import icyllis.modernui.system.SettingsManager;
import net.minecraft.client.GameSettings;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.network.play.client.CLockDifficultyPacket;
import net.minecraft.network.play.client.CSetDifficultyPacket;
import net.minecraft.world.Difficulty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SettingGeneral extends Module {

    private SettingScrollWindow window;

    private DropdownSettingEntry difficultyEntry;

    private BooleanSettingEntry lockEntry;

    public SettingGeneral() {
        this.window = new SettingScrollWindow(this);

        List<SettingCategoryGroup> groups = new ArrayList<>();

        addGameCategory(groups);
        addChatCategory(groups);
        addAccessibilityCategory(groups);
        addSkinCategory(groups);

        window.addGroups(groups);

        addWidget(window);
    }

    private void addGameCategory(List<SettingCategoryGroup> groups) {
        List<SettingEntry> list = new ArrayList<>();

        List<String> difficulties = Lists.newArrayList(Difficulty.values()).stream().
                map(d -> d.getDisplayName().getFormattedText()).collect(Collectors.toCollection(ArrayList::new));
        if (minecraft.world != null) {
            difficultyEntry = new DropdownSettingEntry(window, I18n.format("options.difficulty"), difficulties,
                    minecraft.world.getDifficulty().getId(), i -> {
                Difficulty difficulty = Difficulty.values()[i];
                Objects.requireNonNull(minecraft.getConnection()).sendPacket(new CSetDifficultyPacket(difficulty));
            });
            list.add(difficultyEntry);
            if (minecraft.isSingleplayer() && !minecraft.world.getWorldInfo().isHardcore()) {
                boolean locked = minecraft.world.getWorldInfo().isDifficultyLocked();
                lockEntry = new BooleanSettingEntry(window, I18n.format("difficulty.lock.title"), locked, yes -> {
                    if (yes) {
                        IModule popup = new PopupConfirm(this::lockDifficulty, 3)
                                .setConfirmTitle(I18n.format("gui.modernui.button.Lock"))
                                .setDescription(I18n.format("gui.modernui.popup.lockDifficulty"));
                        GlobalModuleManager.INSTANCE.openPopup(popup, true);
                    }
                }, true);
                difficultyEntry.setAvailable(!locked);
                lockEntry.setAvailable(!locked);
                list.add(lockEntry);
            } else {
                difficultyEntry.setAvailable(false);
            }
        }

        list.add(SettingsManager.REALMS_NOTIFICATIONS.apply(window));

        SettingCategoryGroup categoryGroup = new SettingCategoryGroup(window, I18n.format("gui.modernui.settings.category.game"), list);
        groups.add(categoryGroup);
    }

    private void addChatCategory(List<SettingCategoryGroup> groups) {
        List<SettingEntry> list = new ArrayList<>();

        list.add(SettingsManager.CHAT_VISIBILITY.apply(window));

        list.add(SettingsManager.CHAT_COLOR.apply(window));

        list.add(SettingsManager.CHAT_LINKS.apply(window));

        list.add(SettingsManager.CHAT_LINKS_PROMPT.apply(window));

        list.add(SettingsManager.CHAT_OPACITY.apply(window));

        list.add(SettingsManager.CHAT_SCALE.apply(window));

        list.add(SettingsManager.CHAT_WIDTH.apply(window));

        list.add(SettingsManager.CHAT_HEIGHT_FOCUSED.apply(window));

        list.add(SettingsManager.CHAT_HEIGHT_UNFOCUSED.apply(window));

        if (ModIntegration.optifineLoaded) {

            list.add(SettingsManager.CHAT_BACKGROUND.apply(window));

            list.add(SettingsManager.CHAT_SHADOW.apply(window));
        }

        list.add(SettingsManager.REDUCED_DEBUG_INFO.apply(window));
        list.add(SettingsManager.AUTO_SUGGEST_COMMANDS.apply(window));

        SettingCategoryGroup categoryGroup = new SettingCategoryGroup(window, I18n.format("gui.modernui.settings.category.chat"), list);
        groups.add(categoryGroup);
    }

    private void addAccessibilityCategory(List<SettingCategoryGroup> groups) {
        List<SettingEntry> list = new ArrayList<>();
        GameSettings gameSettings = minecraft.gameSettings;

        list.add(SettingsManager.NARRATOR.apply(window));

        list.add(SettingsManager.SHOW_SUBTITLES.apply(window));

        List<String> textBackgrounds = Lists.newArrayList(I18n.format("options.accessibility.text_background.chat"),
                I18n.format("options.accessibility.text_background.everywhere"));
        list.add(new DropdownSettingEntry(window, I18n.format("options.accessibility.text_background"), textBackgrounds,
                gameSettings.accessibilityTextBackground ? 0 : 1, i -> gameSettings.accessibilityTextBackground = i == 0));
        list.add(SettingsManager.TEXT_BACKGROUND_OPACITY.apply(window));

        list.add(SettingsManager.AUTO_JUMP.apply(window));

        List<String> toggle = Lists.newArrayList(I18n.format("options.key.toggle"), I18n.format("options.key.hold"));
        DropdownSettingEntry sneak = new DropdownSettingEntry(window, I18n.format("key.sneak"), toggle,
                gameSettings.toggleCrouch ? 0 : 1, i -> gameSettings.toggleCrouch = i == 0);
        list.add(sneak);
        DropdownSettingEntry sprint = new DropdownSettingEntry(window, I18n.format("key.sprint"), toggle,
                gameSettings.toggleSprint ? 0 : 1, i -> gameSettings.toggleSprint = i == 0);
        list.add(sprint);

        SettingCategoryGroup categoryGroup = new SettingCategoryGroup(window, I18n.format("gui.modernui.settings.category.accessibility"), list);
        groups.add(categoryGroup);
    }

    private void addSkinCategory(List<SettingCategoryGroup> groups) {
        List<SettingEntry> list = new ArrayList<>();
        GameSettings gameSettings = minecraft.gameSettings;

        list.add(SettingsManager.MAIN_HAND.apply(window));

        for (PlayerModelPart part : PlayerModelPart.values()) {
            BooleanSettingEntry entry = new BooleanSettingEntry(window, part.getName().getFormattedText(),
                    gameSettings.getModelParts().contains(part), b -> gameSettings.setModelPartEnabled(part, b));
            list.add(entry);
        }

        SettingCategoryGroup categoryGroup = new SettingCategoryGroup(window, I18n.format("gui.modernui.settings.category.skin"), list);
        groups.add(categoryGroup);
    }

    private void lockDifficulty(int callback) {
        GlobalModuleManager.INSTANCE.closePopup();
        if (callback == ConfirmCallback.CONFIRM) {
            if (this.minecraft.world != null) {
                Objects.requireNonNull(this.minecraft.getConnection()).sendPacket(new CLockDifficultyPacket(true));
                difficultyEntry.setAvailable(false);
                lockEntry.setAvailable(false);
            }
        } else {
            lockEntry.onValueChanged(1);
        }
    }
}
