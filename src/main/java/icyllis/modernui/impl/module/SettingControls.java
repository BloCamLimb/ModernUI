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

package icyllis.modernui.impl.module;

import com.google.common.collect.Lists;
import icyllis.modernui.graphics.renderer.Icon;
import icyllis.modernui.view.UIManager;
import icyllis.modernui.font.text.TextAlign;
import icyllis.modernui.view.UITools;
import icyllis.modernui.widget.TextDrawable;
import icyllis.modernui.ui.test.Align9D;
import icyllis.modernui.ui.test.Direction4D;
import icyllis.modernui.ui.test.Module;
import icyllis.modernui.ui.test.WidgetStatus;
import icyllis.modernui.widget.*;
import icyllis.modernui.impl.setting.SettingCategoryGroup;
import icyllis.modernui.impl.setting.SettingEntry;
import icyllis.modernui.impl.setting.KeyBindingEntry;
import icyllis.modernui.impl.setting.SettingScrollWindow;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.system.SettingsManager;
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

    private SettingScrollWindow window;
    private SettingCategoryGroup landmarkGroup;

    private List<KeyBindingEntry> allKeyBinding = new ArrayList<>();

    private SearchBox searchBox;
    private TriangleButton nextButton;
    private TriangleButton previousButton;

    private KeyBindingEntry currentResult;
    private List<KeyBindingEntry> searchResults = new ArrayList<>();

    private TextIconButton filterConflictButton;
    private TextIconButton resetAllButton;

    private DropDownWidget searchModeButton;

    private TextDrawable resultCounter = new TextDrawable(TextAlign.CENTER);

    public SettingControls() {
        this.window = new SettingScrollWindow(this);

        List<SettingCategoryGroup> groups = new ArrayList<>();

        addDrawable(resultCounter);
        addWidget(window);

        filterConflictButton = new TextIconButton.Builder(
                new Icon(UITools.ICONS, 0.5f, 0.25f, 0.625f, 0.375f, true),
                I18n.format("gui.modernui.button.filterConflicts"))
                .setWidth(12)
                .setHeight(12)
                .setTextDirection(Direction4D.DOWN)
                .build(this)
                .buildCallback(false, this::filterConflicts);
        /*filterConflictButton = new TextIconButton(this, I18n.format("gui.modernui.button.filterConflicts"), 12, 12,
                new Icon(ConstantsLibrary.ICONS, 0.5f, 0.25f, 0.625f, 0.375f, true), this::filterConflicts, TextIconButton.Direction.DOWN);*/
        addWidget(filterConflictButton);

        resetAllButton = new TextIconButton.Builder(
                new Icon(UITools.ICONS, 0.625f, 0.25f, 0.75f, 0.375f, true),
                I18n.format("controls.resetAll"))
                .setWidth(12)
                .setHeight(12)
                .setTextDirection(Direction4D.DOWN)
                .build(this)
                .buildCallback(false, this::resetAllKey);
        /*resetAllButton = new TextIconButton(this, I18n.format("controls.resetAll"), 12, 12,
                new Icon(ConstantsLibrary.ICONS, 0.625f, 0.25f, 0.75f, 0.375f, true), this::resetAllKey, TextIconButton.Direction.DOWN);*/
        addWidget(resetAllButton);

        searchBox = new SearchBox(this, 100);
        searchBox.setListener(this::searchBoxCallback);
        searchBox.setEnterOperation(this::locateNextResult);
        addWidget(searchBox);

        nextButton = new TriangleButton.Builder(Direction4D.DOWN, 12)
                .build(this)
                .buildCallback(this::locateNextResult);
        nextButton.setStatus(WidgetStatus.INACTIVE, false);
        previousButton = new TriangleButton.Builder(Direction4D.UP, 12)
                .build(this)
                .buildCallback(this::locatePreviousResult);
        previousButton.setStatus(WidgetStatus.INACTIVE, false);
        //nextButton = new TriangleButton(this, TriangleButton.Direction.DOWN, 12, this::locateNextResult, false);
        //previousButton = new TriangleButton(this, TriangleButton.Direction.UP, 12, this::locatePreviousResult, false);
        addWidget(nextButton);
        addWidget(previousButton);

        /*searchModeButton = new DropDownWidget(this, Lists.newArrayList(
                I18n.format("gui.modernui.settings.entry.name"),
                I18n.format("gui.modernui.settings.entry.key")), 0,
                i -> searchBox.setText(""), DropDownMenu.Align.RIGHT);*/
        searchModeButton = new DropDownWidget.Builder(Lists.newArrayList(
                I18n.format("gui.modernui.settings.entry.name"),
                I18n.format("gui.modernui.settings.entry.key")), 0)
                .setAlign(Align9D.TOP_RIGHT)
                .build(this)
                .buildCallback(i -> searchBox.setText(""));
        addWidget(searchModeButton);

        addMouseCategory(groups);
        addAllKeyBindings(groups);
        window.addGroups(groups);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        // layout widgets
        searchModeButton.locate(width / 2f - 122, height - 34);
        searchBox.locate(width / 2f - 120, height - 32);

        previousButton.locate(width / 2f - 16, height - 32);
        nextButton.locate(width / 2f - 2, height - 32);

        filterConflictButton.locate(width / 2f + 112, height - 32);
        resultCounter.setPos(width / 2f + 56, height - 30);

        resetAllButton.locate(width / 2f + 132, height - 32);
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
                    nextButton.setStatus(WidgetStatus.ACTIVE, true);
                    previousButton.setStatus(WidgetStatus.ACTIVE, true);
                    // get the closest result
                    if (prev == null) {
                        searchResults.stream().min(Comparator.comparing(e ->
                                Math.abs(e.getTop() - window.getTop() - window.getVisibleOffset()))).ifPresent(e -> currentResult = e);
                    }
                } else {
                    nextButton.setStatus(WidgetStatus.INACTIVE, true);
                    previousButton.setStatus(WidgetStatus.INACTIVE, true);
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
                nextButton.setStatus(WidgetStatus.INACTIVE, true);
                previousButton.setStatus(WidgetStatus.INACTIVE, true);
                currentResult = null;
                updateResultCounter();
                return false;
            }
        } else {
            nextButton.setStatus(WidgetStatus.INACTIVE, true);
            previousButton.setStatus(WidgetStatus.INACTIVE, true);
            currentResult = null;
            updateResultCounter();
            return true;
        }
    }

    private void resetAllKey() {
        for (KeyBinding keybinding : minecraft.gameSettings.keyBindings) {
            keybinding.setToDefault();
        }
        KeyBinding.resetKeyBindingArrayAndHash();
        allKeyBinding.forEach(KeyBindingEntry::updateKeyText);
        checkAllConflicts();
    }

    private void locateNextResult() {
        if (searchResults.isEmpty()) {
            return;
        }
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
        if (searchResults.isEmpty()) {
            return;
        }
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
                ModernUI.LOGGER.warn(UIManager.MARKER, "Too much key bindings, please report this issue");
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
