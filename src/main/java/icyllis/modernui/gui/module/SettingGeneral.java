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
import icyllis.modernui.gui.scroll.option.*;
import icyllis.modernui.gui.element.IElement;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.IGuiModule;
import icyllis.modernui.gui.window.SettingScrollWindow;
import icyllis.modernui.system.ModIntegration;
import icyllis.modernui.system.SettingsManager;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.OptionsScreen;
import net.minecraft.client.renderer.GameRenderer;
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

    private static Supplier<List<String>> NARRATOR = () -> Lists.newArrayList(NarratorStatus.values()).stream().map(n -> I18n.format(n.getResourceKey())).collect(Collectors.toCollection(ArrayList::new));

    private Minecraft minecraft;

    private List<IElement> elements = new ArrayList<>();

    private List<IGuiEventListener> listeners = new ArrayList<>();

    private SettingScrollWindow window;

    private DropdownOptionEntry difficultyEntry;

    private BooleanOptionEntry lockEntry;

    public SettingGeneral() {
        this.minecraft = Minecraft.getInstance();
        this.window = new SettingScrollWindow();
        addGameCategory();
        addChatCategory();
        addAccessibilityCategory();
        addSkinCategory();
        elements.add(window);
        listeners.add(window);
    }

    private void addGameCategory() {
        List<OptionEntry> list = new ArrayList<>();

        if (minecraft.world != null) {
            difficultyEntry = new DropdownOptionEntry(window, I18n.format("options.difficulty"), DIFFICULTY_OPTIONS.get(),
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
                difficultyEntry.setAvailable(!locked);
                lockEntry.setAvailable(!locked);
                list.add(lockEntry);
            } else {
                difficultyEntry.setAvailable(false);
            }
        }

        list.add(SettingsManager.REALMS_NOTIFICATIONS.apply(window));

        OptionCategory category = new OptionCategory(I18n.format("gui.modernui.settings.category.game"), list);
        window.addGroup(category);
    }

    @SuppressWarnings("NoTranslation")
    private void addChatCategory() {
        List<OptionEntry> list = new ArrayList<>();
        GameSettings gameSettings = minecraft.gameSettings;

        list.add(new DropdownOptionEntry(window, I18n.format("options.chat.visibility"), CHAT_VISIBILITIES.get(),
                gameSettings.chatVisibility.ordinal(), i -> gameSettings.chatVisibility = ChatVisibility.values()[i]));
        list.add(SettingsManager.CHAT_COLOR.apply(window));
        list.add(SettingsManager.CHAT_LINKS.apply(window));
        list.add(SettingsManager.CHAT_LINKS_PROMPT.apply(window));
        list.add(SettingsManager.CHAT_OPACITY.apply(window));
        list.add(SettingsManager.CHAT_SCALE.apply(window));
        list.add(SettingsManager.CHAT_WIDTH.apply(window));
        list.add(SettingsManager.CHAT_HEIGHT_FOCUSED.apply(window));
        list.add(SettingsManager.CHAT_HEIGHT_UNFOCUSED.apply(window));

        if (ModIntegration.optifineLoaded) {
            DropdownOptionEntry chatBackground = new DropdownOptionEntry(window, I18n.format("of.options.CHAT_BACKGROUND"),
                    SettingsManager.INSTANCE.getChatBackgroundTexts(), SettingsManager.INSTANCE.getChatBackgroundIndex(), SettingsManager.INSTANCE::setChatBackgroundIndex);
            list.add(chatBackground);
            BooleanOptionEntry chatShadow = new BooleanOptionEntry(window, I18n.format("of.options.CHAT_SHADOW"),
                    SettingsManager.INSTANCE.getChatShadow(), SettingsManager.INSTANCE::setChatShadow);
            list.add(chatShadow);
        }

        list.add(SettingsManager.REDUCED_DEBUG_INFO.apply(window));
        list.add(SettingsManager.AUTO_SUGGEST_COMMANDS.apply(window));

        OptionCategory category = new OptionCategory(I18n.format("gui.modernui.settings.category.chat"), list);
        window.addGroup(category);
    }

    private void addAccessibilityCategory() {
        List<OptionEntry> list = new ArrayList<>();
        GameSettings gameSettings = minecraft.gameSettings;

        boolean active = NarratorChatListener.INSTANCE.isActive();
        List<String> narratorStatus = active ?
                NARRATOR.get() :
                Lists.newArrayList(I18n.format("options.narrator.notavailable"));
        DropdownOptionEntry narrator = new DropdownOptionEntry(window, I18n.format("options.narrator"), narratorStatus,
                active ? gameSettings.narrator.ordinal() : 0, i -> {
            gameSettings.narrator = NarratorStatus.values()[i];
            NarratorChatListener.INSTANCE.announceMode(gameSettings.narrator);
        });
        list.add(narrator);
        list.add(SettingsManager.SHOW_SUBTITLES.apply(window));

        List<String> textBackgrounds = Lists.newArrayList(I18n.format("options.accessibility.text_background.chat"),
                I18n.format("options.accessibility.text_background.everywhere"));
        list.add(new DropdownOptionEntry(window, I18n.format("options.accessibility.text_background"), textBackgrounds,
                gameSettings.accessibilityTextBackground ? 0 : 1, i -> gameSettings.accessibilityTextBackground = i == 0));
        list.add(SettingsManager.TEXT_BACKGROUND_OPACITY.apply(window));

        list.add(SettingsManager.AUTO_JUMP.apply(window));

        List<String> toggle = Lists.newArrayList(I18n.format("options.key.toggle"), I18n.format("options.key.hold"));
        DropdownOptionEntry sneak = new DropdownOptionEntry(window, I18n.format("key.sneak"), toggle,
                gameSettings.toggleCrouch ? 0 : 1, i -> gameSettings.toggleCrouch = i == 0);
        list.add(sneak);
        DropdownOptionEntry sprint = new DropdownOptionEntry(window, I18n.format("key.sprint"), toggle,
                gameSettings.toggleSprint ? 0 : 1, i -> gameSettings.toggleSprint = i == 0);
        list.add(sprint);

        OptionCategory category = new OptionCategory(I18n.format("gui.modernui.settings.category.accessibility"), list);
        window.addGroup(category);
    }

    private void addSkinCategory() {
        List<OptionEntry> list = new ArrayList<>();
        GameSettings gameSettings = minecraft.gameSettings;

        OptionEntry mainHand = new DropdownOptionEntry(window, I18n.format("options.mainHand"), MAIN_HANDS.get(),
                gameSettings.mainHand.ordinal(), i -> {
            gameSettings.mainHand = HandSide.values()[i];
            gameSettings.saveOptions();
            gameSettings.sendSettingsToServer();
        });
        list.add(mainHand);

        for (PlayerModelPart part : PlayerModelPart.values()) {
            BooleanOptionEntry entry = new BooleanOptionEntry(window, part.getName().getFormattedText(),
                    gameSettings.getModelParts().contains(part), b -> gameSettings.setModelPartEnabled(part, b));
            list.add(entry);
        }

        OptionCategory category = new OptionCategory(I18n.format("gui.modernui.settings.category.skin"), list);
        window.addGroup(category);
    }

    private void lockDifficulty(boolean lock) {
        GlobalModuleManager.INSTANCE.closePopup();
        if (lock) {
            if (this.minecraft.world != null) {
                this.minecraft.getConnection().sendPacket(new CLockDifficultyPacket(true));
                difficultyEntry.setAvailable(false);
                lockEntry.setAvailable(false);
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
