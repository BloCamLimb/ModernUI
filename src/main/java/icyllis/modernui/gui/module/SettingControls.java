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

import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.Module;
import icyllis.modernui.gui.setting.SettingCategoryGroup;
import icyllis.modernui.gui.setting.SettingEntry;
import icyllis.modernui.gui.setting.KeyBindingEntry;
import icyllis.modernui.gui.scroll.SettingScrollWindow;
import icyllis.modernui.gui.widget.KeyInputBox;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.system.SettingsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingControls extends Module {

    private Minecraft minecraft;

    private SettingScrollWindow window;

    private List<KeyBindingEntry> allKeyBinding = new ArrayList<>();

    public SettingControls() {
        this.minecraft = Minecraft.getInstance();
        this.window = new SettingScrollWindow(this);

        List<SettingCategoryGroup> groups = new ArrayList<>();

        addMouseCategory(groups);
        addAllKeyBindings(groups);

        window.addGroups(groups);

        addElements(window);
        addMouseListener(window);
    }

    private void addMouseCategory(List<SettingCategoryGroup> groups) {
        List<SettingEntry> list = new ArrayList<>();

        list.add(SettingsManager.SENSITIVITY.apply(window));
        list.add(SettingsManager.MOUSE_WHEEL_SENSITIVITY.apply(window));
        list.add(SettingsManager.INVERT_MOUSE.apply(window));
        list.add(SettingsManager.DISCRETE_MOUSE_WHEEL.apply(window));
        list.add(SettingsManager.TOUCHSCREEN.apply(window));
        if (InputMappings.func_224790_a()) {
            list.add(SettingsManager.RAW_MOUSE_INPUT.apply(window));
        }

        SettingCategoryGroup categoryGroup = new SettingCategoryGroup(window, I18n.format("gui.modernui.settings.category.mouse"), list);
        groups.add(categoryGroup);
    }

    private void addAllKeyBindings(List<SettingCategoryGroup> groups) {
        KeyBinding[] keyBindings = ArrayUtils.clone(minecraft.gameSettings.keyBindings);

        //Sort by category and key desc {@link KeyBinding#compareTo(KeyBinding)}
        Arrays.sort(keyBindings);

        String categoryKey = null;
        List<SettingEntry> list = null;

        for (KeyBinding keybinding : keyBindings) {
            String ck = keybinding.getKeyCategory();
            if (!ck.equals(categoryKey)) {
                if (list != null) {
                    SettingCategoryGroup category = new SettingCategoryGroup(window, I18n.format(categoryKey), list);
                    groups.add(category);
                }
                categoryKey = ck;
                list = new ArrayList<>();
            }
            KeyBindingEntry entry = new KeyBindingEntry(window, keybinding, this::checkAllConflicts);
            list.add(entry);
            allKeyBinding.add(entry);
            if (allKeyBinding.size() >= 1000) {
                ModernUI.LOGGER.warn(GlobalModuleManager.MARKER, "Too much key bindings, please report this issue");
                // maybe we want more optimization?
                break;
            }
        }
        // add last category
        if (categoryKey != null) {
            SettingCategoryGroup category = new SettingCategoryGroup(window, I18n.format(categoryKey), list);
            groups.add(category);
        }

        checkAllConflicts();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        if (getKeyboardListener() instanceof KeyInputBox) {
            ((KeyInputBox) getKeyboardListener()).stopEditing();
            return true;
        }
        return false;
    }

    /**
     * If a key conflicts with another key, they both are conflicted
     * Called when a key binding changed, but vanilla does this every frame
     * so it's much better than vanilla, I don't have to do more optimization
     */
    private void checkAllConflicts() {
        KeyBinding[] keyBindings = minecraft.gameSettings.keyBindings;
        KeyBinding keyBinding;
        for (KeyBindingEntry entry : allKeyBinding) {
            int conflict = 0;
            keyBinding = entry.getKeyBinding();
            if (!keyBinding.isInvalid()) {
                for (KeyBinding other : keyBindings) {
                    if (keyBinding != other) { // there's a same key binding
                        conflict = Math.max(conflict, conflicts(keyBinding, other));
                    }
                }
            }
            entry.setConflictTier(conflict);
        }
    }

    /**
     * different from forge, finally return false, and in-game conflict should be mutual
     * this is quicker than forge's, because we reduced checks, return 1 instead
     */
    private int conflicts(KeyBinding a, KeyBinding t) {
        IKeyConflictContext conflictContext = a.getKeyConflictContext();
        IKeyConflictContext otherConflictContext = t.getKeyConflictContext();
        if (conflictContext.conflicts(otherConflictContext) || otherConflictContext.conflicts(conflictContext)) {
            KeyModifier keyModifier = a.getKeyModifier();
            KeyModifier otherKeyModifier = t.getKeyModifier();
            if (keyModifier.matches(t.getKey()) || otherKeyModifier.matches(a.getKey())) {
                return 1;
            } else if (a.getKey().equals(t.getKey())) {
                if (keyModifier == otherKeyModifier ||
                        ((conflictContext.conflicts(KeyConflictContext.IN_GAME) ||
                                otherConflictContext.conflicts(KeyConflictContext.IN_GAME)) &&
                                (keyModifier == KeyModifier.NONE || otherKeyModifier == KeyModifier.NONE)))
                return 2;
            }
        }
        return 0;
    }

}
