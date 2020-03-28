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

import icyllis.modernui.gui.element.IElement;
import icyllis.modernui.gui.master.IGuiModule;
import icyllis.modernui.gui.scroll.option.KeyBindingEntry;
import icyllis.modernui.gui.scroll.option.OptionCategory;
import icyllis.modernui.gui.scroll.option.OptionEntry;
import icyllis.modernui.gui.window.SettingScrollWindow;
import icyllis.modernui.system.SettingsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingControls implements IGuiModule {

    private Minecraft minecraft;

    private List<IElement> elements = new ArrayList<>();

    private List<IGuiEventListener> listeners = new ArrayList<>();

    private SettingScrollWindow window;

    private List<KeyBindingEntry> allKeyBinding = new ArrayList<>();

    public SettingControls() {
        this.minecraft = Minecraft.getInstance();
        this.window = new SettingScrollWindow();
        addMouseCategory();
        addAllKeyBindings();
        elements.add(window);
        listeners.add(window);
    }

    private void addMouseCategory() {
        List<OptionEntry> list = new ArrayList<>();

        list.add(SettingsManager.SENSITIVITY.apply(window));
        list.add(SettingsManager.MOUSE_WHEEL_SENSITIVITY.apply(window));
        list.add(SettingsManager.INVERT_MOUSE.apply(window));
        list.add(SettingsManager.DISCRETE_MOUSE_WHEEL.apply(window));
        list.add(SettingsManager.TOUCHSCREEN.apply(window));
        if (InputMappings.func_224790_a()) {
            list.add(SettingsManager.RAW_MOUSE_INPUT.apply(window));
        }

        OptionCategory category = new OptionCategory(I18n.format("gui.modernui.settings.category.mouse"), list);
        window.addGroup(category);
    }

    @SuppressWarnings("DanglingJavadoc")
    private void addAllKeyBindings() {
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
                    OptionCategory category = new OptionCategory(I18n.format(categoryKey), list);
                    window.addGroup(category);
                }
                categoryKey = ck;
                list = new ArrayList<>();
            }
            KeyBindingEntry entry = new KeyBindingEntry(window, keybinding, this::checkAllConflicts);
            list.add(entry);
            allKeyBinding.add(entry);
        }
        // add last category
        if (categoryKey != null) {
            OptionCategory category = new OptionCategory(I18n.format(categoryKey), list);
            window.addGroup(category);
        }
        checkAllConflicts();
    }

    /**
     * If a key conflicts with another key, they both are conflicted
     * Yes, double for loop, but only called when a key binding changed
     * However, vanilla does this every frame...
     * so it's better than vanilla, I don't have to do more optimization
     */
    private void checkAllConflicts() {
        KeyBinding[] keyBindings = Minecraft.getInstance().gameSettings.keyBindings;
        KeyBinding keyBinding;
        for (KeyBindingEntry entry : allKeyBinding) {
            boolean conflict = false;
            boolean modifierConflict = true;
            keyBinding = entry.getKeyBinding();
            if (!keyBinding.isInvalid()) {
                for (KeyBinding other : keyBindings) {
                    if (keyBinding != other && conflicts(keyBinding, other)) {
                        conflict = true;
                        modifierConflict &= keyBinding.hasKeyCodeModifierConflict(other);
                    }
                }
            }
            if (conflict) {
                if (modifierConflict) {
                    entry.setConflictTier(1);
                } else {
                    entry.setConflictTier(2);
                }
            } else {
                entry.setConflictTier(0);
            }
        }
    }

    // different from forge, finally return false is correct
    private boolean conflicts(KeyBinding a, KeyBinding b) {
        if (a.getKeyConflictContext().conflicts(b.getKeyConflictContext()) || b.getKeyConflictContext().conflicts(a.getKeyConflictContext())) {
            KeyModifier keyModifier = a.getKeyModifier();
            KeyModifier otherKeyModifier = b.getKeyModifier();
            if (keyModifier.matches(b.getKey()) || otherKeyModifier.matches(a.getKey())) {
                return true;
            } else if (a.getKey().equals(b.getKey())) {
                return keyModifier == otherKeyModifier ||
                        (a.getKeyConflictContext().conflicts(KeyConflictContext.IN_GAME) &&
                                (keyModifier == KeyModifier.NONE || otherKeyModifier == KeyModifier.NONE));
            }
        }
        return false;
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
