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
import icyllis.modernui.gui.component.option.*;
import icyllis.modernui.gui.element.IElement;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.IGuiModule;
import icyllis.modernui.gui.window.SettingScrollWindow;
import icyllis.modernui.system.SettingsManager;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.NarratorStatus;
import net.minecraft.entity.player.ChatVisibility;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.network.play.client.CLockDifficultyPacket;
import net.minecraft.network.play.client.CSetDifficultyPacket;
import net.minecraft.util.HandSide;
import net.minecraft.world.Difficulty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SettingGeneral implements IGuiModule {

    private static Supplier<List<String>> DIFFICULTY_OPTIONS = () -> Lists.newArrayList(Difficulty.values()).stream().map(d -> d.getDisplayName().getFormattedText()).collect(Collectors.toCollection(ArrayList::new));

    private static Supplier<List<String>> MAIN_HANDS = () -> Lists.newArrayList(HandSide.values()).stream().map(HandSide::toString).collect(Collectors.toCollection(ArrayList::new));

    private static Supplier<List<String>> CHAT_VISIBILITIES = () -> Lists.newArrayList(ChatVisibility.values()).stream().map(c -> I18n.format(c.getResourceKey())).collect(Collectors.toCollection(ArrayList::new));

    private Minecraft minecraft;

    private List<IElement> elements = new ArrayList<>();

    private List<IGuiEventListener> listeners = new ArrayList<>();

    private SettingScrollWindow window;

    private MenuOptionEntry difficultyEntry;

    private BooleanOptionEntry lockEntry;

    public SettingGeneral() {
        this.minecraft = Minecraft.getInstance();
        this.window = new SettingScrollWindow();
        addGameCategory();
        addSkinCategory();
        addChatCategory();
        addAccessibilityCategory();
        elements.add(window);
        listeners.add(window);
    }

    private void addGameCategory() {
        List<OptionEntry> list = new ArrayList<>();

        if (minecraft.world != null) {
            difficultyEntry = new MenuOptionEntry(window, I18n.format("options.difficulty"), DIFFICULTY_OPTIONS.get(),
                    minecraft.world.getDifficulty().getId(), i -> {
                Difficulty difficulty = Difficulty.values()[i];
                minecraft.getConnection().sendPacket(new CSetDifficultyPacket(difficulty));
            });
            list.add(difficultyEntry);
            if (minecraft.isSingleplayer() && !minecraft.world.getWorldInfo().isHardcore()) {
                boolean locked = minecraft.world.getWorldInfo().isDifficultyLocked();
                lockEntry = new BooleanOptionEntry(window, I18n.format("difficulty.lock.title"), locked, b -> {
                    if (b) {
                        GlobalModuleManager.INSTANCE.openPopup(new PopupLockDifficulty(this::lockDifficulty));
                    }
                }, true);
                difficultyEntry.setClickable(!locked);
                lockEntry.setClickable(!locked);
            } else {
                difficultyEntry.setClickable(false);
            }
            list.add(lockEntry);
        }

        list.add(SettingsManager.FOV.apply(window));
        list.add(SettingsManager.REALMS_NOTIFICATIONS.apply(window));

        OptionCategory category = new OptionCategory("Game", list);
        window.addGroup(category);
    }

    private void addSkinCategory() {
        List<OptionEntry> list = new ArrayList<>();
        GameSettings gameSettings = minecraft.gameSettings;

        for (PlayerModelPart part : PlayerModelPart.values()) {
            BooleanOptionEntry entry = new BooleanOptionEntry(window, part.getName().getFormattedText(),
                    gameSettings.getModelParts().contains(part), b -> gameSettings.setModelPartEnabled(part, b));
            list.add(entry);
        }
        OptionEntry mainHand = new MenuOptionEntry(window, I18n.format("options.mainHand"), MAIN_HANDS.get(),
                gameSettings.mainHand.ordinal(), i -> {
           gameSettings.mainHand = HandSide.values()[i];
           gameSettings.saveOptions();
           gameSettings.sendSettingsToServer();
        });
        list.add(mainHand);

        OptionCategory category = new OptionCategory("Skin", list);
        window.addGroup(category);
    }

    private void addChatCategory() {
        List<OptionEntry> list = new ArrayList<>();
        GameSettings gameSettings = minecraft.gameSettings;

        list.add(new MenuOptionEntry(window, "Visibility", CHAT_VISIBILITIES.get(),
                gameSettings.chatVisibility.ordinal(), i -> gameSettings.chatVisibility = ChatVisibility.values()[i]));
        list.add(SettingsManager.CHAT_COLOR.apply(window));
        list.add(SettingsManager.CHAT_LINKS.apply(window));
        list.add(SettingsManager.CHAT_LINKS_PROMPT.apply(window));
        list.add(SettingsManager.CHAT_OPACITY.apply(window));
        list.add(SettingsManager.CHAT_SCALE.apply(window));
        list.add(SettingsManager.CHAT_WIDTH.apply(window));
        list.add(SettingsManager.CHAT_HEIGHT_FOCUSED.apply(window));
        list.add(SettingsManager.CHAT_HEIGHT_UNFOCUSED.apply(window));
        list.add(SettingsManager.REDUCED_DEBUG_INFO.apply(window));
        list.add(SettingsManager.AUTO_SUGGEST_COMMANDS.apply(window));

        OptionCategory category = new OptionCategory("Chat", list);
        window.addGroup(category);
    }

    private void addAccessibilityCategory() {
        List<OptionEntry> list = new ArrayList<>();
        GameSettings gameSettings = minecraft.gameSettings;

        boolean active = NarratorChatListener.INSTANCE.isActive();
        List<String> narratorStatus = active ?
                Lists.newArrayList(NarratorStatus.values()).stream().map(n -> I18n.format(n.getResourceKey())).collect(Collectors.toCollection(ArrayList::new)) :
                Lists.newArrayList(I18n.format("options.narrator.notavailable"));
        MenuOptionEntry narrator = new MenuOptionEntry(window, I18n.format("options.narrator"), narratorStatus,
                active ? gameSettings.narrator.ordinal() : 0, i -> {
            gameSettings.narrator = NarratorStatus.values()[i];
            NarratorChatListener.INSTANCE.announceMode(gameSettings.narrator);
        });
        list.add(narrator);
        list.add(SettingsManager.SHOW_SUBTITLES.apply(window));
        list.add(SettingsManager.TEXT_BACKGROUND_OPACITY.apply(window));
        List<String> textBackgrounds = Lists.newArrayList(I18n.format("options.accessibility.text_background.chat"),
                I18n.format("options.accessibility.text_background.everywhere"));
        list.add(new MenuOptionEntry(window, I18n.format("options.accessibility.text_background"), textBackgrounds,
                gameSettings.accessibilityTextBackground ? 0 : 1, i -> gameSettings.accessibilityTextBackground = i == 0));

        OptionCategory category = new OptionCategory("Accessibility", list);
        window.addGroup(category);
    }

    private void lockDifficulty(boolean lock) {
        GlobalModuleManager.INSTANCE.closePopup();
        if (lock) {
            if (this.minecraft.world != null) {
                this.minecraft.getConnection().sendPacket(new CLockDifficultyPacket(true));
                difficultyEntry.setClickable(false);
                lockEntry.setClickable(false);
            }
        } else {
            lockEntry.onValueChanged(1);
        }
    }

    @Override
    public List<? extends IElement> getElements() {
        return elements;
    }

    @Override
    public List<? extends IGuiEventListener> getEventListeners() {
        return listeners;
    }
}
