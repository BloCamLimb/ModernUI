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
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.element.TextElement;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.IKeyboardListener;
import icyllis.modernui.gui.master.Icon;
import icyllis.modernui.gui.master.Module;
import icyllis.modernui.gui.widget.*;
import icyllis.modernui.impl.setting.SettingCategoryGroup;
import icyllis.modernui.impl.setting.SettingEntry;
import icyllis.modernui.impl.setting.KeyBindingEntry;
import icyllis.modernui.gui.scroll.SettingScrollWindow;
import icyllis.modernui.system.ConstantsLibrary;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.system.SettingsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.util.*;

public class SettingControls extends Module {

    private Minecraft minecraft;

    private SettingScrollWindow window;
    private SettingCategoryGroup landmarkGroup;

    private List<KeyBindingEntry> allKeyBinding = new ArrayList<>();

    private SearchBox searchBox;
    private TriangleButton nextButton;
    private TriangleButton previousButton;

    private KeyBindingEntry currentResult;
    private List<KeyBindingEntry> searchResults = new ArrayList<>();

    private TextIconButton filterConflictButton;
    private StaticFrameButton resetAllButton;

    private DropDownWidget searchModeButton;

    private TextElement resultCounter = new TextElement(TextAlign.CENTER);

    public SettingControls() {
        this.minecraft = Minecraft.getInstance();
        this.window = new SettingScrollWindow(this);

        List<SettingCategoryGroup> groups = new ArrayList<>();

        addDrawable(resultCounter);
        addWidget(window);

        filterConflictButton = new TextIconButton(this, I18n.format("gui.modernui.button.filterConflicts"), 12, 12,
                new Icon(ConstantsLibrary.ICONS, 0.5f, 0.25f, 0.625f, 0.375f, true), this::filterConflicts, TextIconButton.Direction.UP);
        addWidget(filterConflictButton);

        resetAllButton = new StaticFrameButton(this, 64, I18n.format("controls.resetAll"), this::resetAllKey, true);
        addWidget(resetAllButton);

        searchBox = new SearchBox(this, 100);
        searchBox.setListener(this::searchBoxCallback);
        searchBox.setEnterOperation(this::locateNextResult);
        addWidget(searchBox);

        nextButton = new TriangleButton(this, TriangleButton.Direction.DOWN, 12, this::locateNextResult, false);
        previousButton = new TriangleButton(this, TriangleButton.Direction.UP, 12, this::locatePreviousResult, false);
        addWidget(nextButton);
        addWidget(previousButton);

        searchModeButton = new DropDownWidget(this, Lists.newArrayList("Name", "Key"), 0,
                i -> searchBox.setText(""), DropDownMenu.Align.RIGHT);
        addWidget(searchModeButton);

        addMouseCategory(groups);
        addAllKeyBindings(groups);
        window.addGroups(groups);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        // layout widgets
        searchModeButton.setPos(width / 2f - 122, height - 34);
        searchBox.setPos(width / 2f - 120, height - 32);

        previousButton.setPos(width / 2f - 16, height - 32);
        nextButton.setPos(width / 2f - 2, height - 32);

        filterConflictButton.setPos(width / 2f + 20, height - 32);
        resultCounter.setPos(width / 2f + 62, height - 30);

        resetAllButton.setPos(width / 2f + 92, height - 32);
    }

    private void filterConflicts() {
        searchModeButton.updateValue(0);
        searchBox.setText("#conflicting");
    }

    private boolean searchBoxCallback(@Nonnull String t) {
        searchResults.clear();
        if (!t.isEmpty()) {
            String ct = t.toLowerCase();
            if (searchModeButton.getIndex() == 0) {
                if (ct.equals("#conflicting")) {
                    allKeyBinding.stream().filter(f -> f.getTier() > 0).forEach(searchResults::add);
                } else {
                    allKeyBinding.stream().filter(f -> f.title.toLowerCase().contains(ct)).forEach(searchResults::add);
                }
            } else {
                allKeyBinding.stream().filter(f ->
                        Objects.equals(TextFormatting.getTextWithoutFormattingCodes(
                                f.getInputBox().getKeyText().toLowerCase()), ct)).forEach(searchResults::add);
            }
            if (!searchResults.isEmpty()) {
                KeyBindingEntry prev = currentResult;
                if (searchResults.size() > 1) {
                    nextButton.setClickable(true);
                    previousButton.setClickable(true);
                    // get the closest result
                    if (prev == null) {
                        searchResults.stream().min(Comparator.comparing(e ->
                                Math.abs(e.getTop() - window.getTop() - window.getVisibleOffset()))).ifPresent(e -> currentResult = e);
                    }
                } else {
                    nextButton.setClickable(false);
                    previousButton.setClickable(false);
                    if (prev == null) {
                        currentResult = searchResults.get(0);
                    }
                }
                if (prev != currentResult && currentResult != null) {
                    currentResult.lightUp();
                    landmarkGroup.followEntry(currentResult);
                }
                updateResultCounter();
                return true;
            } else {
                nextButton.setClickable(false);
                previousButton.setClickable(false);
                currentResult = null;
                updateResultCounter();
                return false;
            }
        } else {
            nextButton.setClickable(false);
            previousButton.setClickable(false);
            currentResult = null;
            updateResultCounter();
            return true;
        }
    }

    private void resetAllKey() {
        for(KeyBinding keybinding : minecraft.gameSettings.keyBindings) {
            keybinding.setToDefault();
        }
        KeyBinding.resetKeyBindingArrayAndHash();
        allKeyBinding.forEach(KeyBindingEntry::updateKeyText);
        checkAllConflicts();
    }

    private void locateNextResult() {
        int i = searchResults.indexOf(currentResult) + 1;
        if (i >= searchResults.size()) {
            currentResult = searchResults.get(0);
        } else {
            currentResult = searchResults.get(i);
        }
        currentResult.lightUp();
        landmarkGroup.followEntry(currentResult);
        updateResultCounter();
    }

    private void locatePreviousResult() {
        int i = searchResults.indexOf(currentResult) - 1;
        if (i < 0) {
            currentResult = searchResults.get(searchResults.size() - 1);
        } else {
            currentResult = searchResults.get(i);
        }
        currentResult.lightUp();
        landmarkGroup.followEntry(currentResult);
        updateResultCounter();
    }

    private void updateResultCounter() {
        if (searchResults.isEmpty()) {
            resultCounter.setText("");
        } else {
            int i = searchResults.indexOf(currentResult) + 1;
            resultCounter.setText(I18n.format("gui.modernui.text.oneOfAll", i, searchResults.size()));
        }
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
            landmarkGroup = new SettingCategoryGroup(window, I18n.format(categoryKey), list);
            groups.add(landmarkGroup);
        }

        checkAllConflicts();
    }

    /**
     * Mark mark
     * This is a general logic for keyboard listeners, copy me!
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        IKeyboardListener k = getKeyboardListener();
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) {
            if (k != null && getKeyboardListener() != k) {
                setKeyboardListener(null);
            }
            return true;
        }
        if (getKeyboardListener() != null) {
            setKeyboardListener(null);
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
        searchBox.onTextChanged();
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
