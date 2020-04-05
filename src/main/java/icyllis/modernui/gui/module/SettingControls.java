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

import icyllis.modernui.gui.master.Module;
import icyllis.modernui.gui.option.OptionCategory;
import icyllis.modernui.gui.option.OptionEntry;
import icyllis.modernui.gui.option.KeyBindingEntry;
import icyllis.modernui.gui.scroll.SettingScrollWindow;
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
        List<OptionCategory> groups = new ArrayList<>();
        groups.add(getMouseCategory());
        groups.addAll(getAllKeyBindings());
        window.addGroups(groups);
        addWidget(window);
    }

    private OptionCategory getMouseCategory() {
        List<OptionEntry> list = new ArrayList<>();

        list.add(SettingsManager.SENSITIVITY.apply(window));
        list.add(SettingsManager.MOUSE_WHEEL_SENSITIVITY.apply(window));
        list.add(SettingsManager.INVERT_MOUSE.apply(window));
        list.add(SettingsManager.DISCRETE_MOUSE_WHEEL.apply(window));
        list.add(SettingsManager.TOUCHSCREEN.apply(window));
        if (InputMappings.func_224790_a()) {
            list.add(SettingsManager.RAW_MOUSE_INPUT.apply(window));
        }

        return new OptionCategory(window, I18n.format("gui.modernui.settings.category.mouse"), list);
    }

    @SuppressWarnings("DanglingJavadoc")
    private List<OptionCategory> getAllKeyBindings() {
        List<OptionCategory> groups = new ArrayList<>();
        KeyBinding[] keyBindings = ArrayUtils.clone(minecraft.gameSettings.keyBindings);
        /**
         * Sort by category and key desc {@link KeyBinding#compareTo(KeyBinding)}
         **/
        Arrays.sort(keyBindings);

        String categoryKey = null;
        List<OptionEntry> list = null;

        for (KeyBinding keybinding : keyBindings) {
            String ck = keybinding.getKeyCategory();
            if (!ck.equals(categoryKey)) {
                if (list != null) {
                    OptionCategory category = new OptionCategory(window, I18n.format(categoryKey), list);
                    groups.add(category);
                }
                categoryKey = ck;
                list = new ArrayList<>();
            }
            KeyBindingEntry entry = new KeyBindingEntry(window, keybinding, this::checkAllConflicts);
            list.add(entry);
            allKeyBinding.add(entry);
            if (allKeyBinding.size() >= 1000) {
                ModernUI.LOGGER.warn("Too much key bindings, please report this issue");
                // maybe we want more optimization?
                break;
            }
        }
        // add last category
        if (categoryKey != null) {
            OptionCategory category = new OptionCategory(window, I18n.format(categoryKey), list);
            groups.add(category);
        }
        checkAllConflicts();
        return groups;
    }

    /**
     * If a key conflicts with another key, they both are conflicted
     * Yes, double for loop, but only called when a key binding changed
     * However, vanilla does this every frame...
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
