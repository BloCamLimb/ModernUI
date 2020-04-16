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
import icyllis.modernui.gui.layout.WidgetLayout;
import icyllis.modernui.gui.master.Module;
import icyllis.modernui.gui.master.Widget;
import icyllis.modernui.gui.scroll.ScrollWindow;
import icyllis.modernui.gui.widget.StaticFrameButton;
import icyllis.modernui.gui.widget.TriangleButton;
import icyllis.modernui.impl.background.ResourcePackBG;
import icyllis.modernui.impl.setting.ResourcePackEntry;
import icyllis.modernui.impl.setting.ResourcePackGroup;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ClientResourcePackInfo;
import net.minecraft.client.resources.I18n;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class SettingResourcePack extends Module {

    private final Minecraft minecraft;

    private ResourcePackGroup availableGroup;
    private ResourcePackGroup selectedGroup;

    private TriangleButton leftArrow;
    private TriangleButton rightArrow;
    private TriangleButton upArrow;
    private TriangleButton downArrow;

    private WidgetLayout arrowsLayout;

    @Nullable
    private ResourcePackEntry highlightEntry;

    private StaticFrameButton apply;

    public SettingResourcePack() {
        minecraft = Minecraft.getInstance();
        addDrawable(new ResourcePackBG());

        Function<Integer, Float> widthFunc = w -> Math.min((w - 80) / 2f - 8f, 240);
        Function<Integer, Float> leftXFunc = w -> w / 2f - widthFunc.apply(w) - 8f;

        ScrollWindow<ResourcePackGroup> aWindow = new ScrollWindow<>(this, leftXFunc, h -> 36f, widthFunc, h -> h - 72f);
        ScrollWindow<ResourcePackGroup> sWindow = new ScrollWindow<>(this, w -> w / 2f + 8, h -> 36f, widthFunc, h -> h - 72f);

        minecraft.getResourcePackList().reloadPacksFromFinders();

        availableGroup = new ResourcePackGroup(this, aWindow, minecraft.getResourcePackList(), ResourcePackGroup.Type.AVAILABLE);
        selectedGroup = new ResourcePackGroup(this, sWindow, minecraft.getResourcePackList(), ResourcePackGroup.Type.SELECTED);

        aWindow.addGroups(Lists.newArrayList(availableGroup));
        sWindow.addGroups(Lists.newArrayList(selectedGroup));

        List<Widget> list = new ArrayList<>();

        leftArrow = new TriangleButton(this, TriangleButton.Direction.LEFT, 12, this::intoAvailable, false);
        rightArrow = new TriangleButton(this, TriangleButton.Direction.RIGHT, 12, this::intoSelected, false);
        upArrow = new TriangleButton(this, TriangleButton.Direction.UP, 12, this::goUp, false);
        downArrow = new TriangleButton(this, TriangleButton.Direction.DOWN, 12, this::goDown, false);

        list.add(leftArrow);
        list.add(rightArrow);
        list.add(upArrow);
        list.add(downArrow);

        apply = new StaticFrameButton(this, 48, I18n.format("gui.modernui.button.apply"), this::applyResourcePacks, false);

        //list.add(new SlidingToggleButton(this, 4, b -> {}, 0xb020a0d0, 0x40808080, false)); // test

        arrowsLayout = new WidgetLayout(list, WidgetLayout.Direction.VERTICAL_POSITIVE, 4);

        addWidget(apply);
        addWidget(aWindow);
        addWidget(sWindow);
        list.forEach(this::addWidget);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        arrowsLayout.layout(width / 2f - 6, height * 0.25f);
        apply.setPos(width / 2f - 24, height - 32);
    }

    private void applyResourcePacks() {
        List<ClientResourcePackInfo> list = Lists.newArrayList();
        GameSettings gameSettings = minecraft.gameSettings;

        for (ResourcePackEntry c2 : selectedGroup.getEntries()) {
            list.add(c2.getResourcePack());
        }

        Collections.reverse(list);
        minecraft.getResourcePackList().setEnabledPacks(list);
        gameSettings.resourcePacks.clear();
        gameSettings.incompatibleResourcePacks.clear();

        for (ClientResourcePackInfo c3 : list) {
            if (!c3.isOrderLocked()) {
                gameSettings.resourcePacks.add(c3.getName());
                if (!c3.getCompatibility().isCompatible()) {
                    gameSettings.incompatibleResourcePacks.add(c3.getName());
                }
            }
        }

        gameSettings.saveOptions();
        minecraft.reloadResources();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        if (highlightEntry != null) {
            setHighlightEntry(null);
            return true;
        }
        return false;
    }

    private void intoSelected() {
        if (highlightEntry != null && highlightEntry.canIntoSelected()) {
            availableGroup.getEntries().remove(highlightEntry);
            availableGroup.layoutGroup();
            highlightEntry.intoSelected(selectedGroup);
            selectedGroup.layoutGroup();
            selectedGroup.followEntry(highlightEntry);
            highlightEntry.setMouseHoverExit();
            refocusCursor();
            setHighlightEntry(highlightEntry);
            apply.setListening(true);
        }
    }

    private void intoAvailable() {
        if (highlightEntry != null && highlightEntry.canIntoAvailable()) {
            selectedGroup.getEntries().remove(highlightEntry);
            selectedGroup.layoutGroup();
            availableGroup.getEntries().add(highlightEntry);
            availableGroup.layoutGroup();
            availableGroup.followEntry(highlightEntry);
            highlightEntry.setMouseHoverExit();
            refocusCursor();
            setHighlightEntry(highlightEntry);
            apply.setListening(true);
        }
    }

    private void goUp() {
        if (highlightEntry != null && highlightEntry.canGoUp()) {
            highlightEntry.goUp();
            selectedGroup.layoutGroup();
            selectedGroup.followEntry(highlightEntry);
            setHighlightEntry(highlightEntry);
            refocusCursor();
            apply.setListening(true);
        }
    }

    private void goDown() {
        if (highlightEntry != null && highlightEntry.canGoDown()) {
            highlightEntry.goDown();
            selectedGroup.layoutGroup();
            selectedGroup.followEntry(highlightEntry);
            setHighlightEntry(highlightEntry);
            refocusCursor();
            apply.setListening(true);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_W) {
            goUp();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_S) {
            goDown();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_A) {
            intoAvailable();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_D) {
            intoSelected();
            return true;
        }
        return false;
    }

    @Nullable
    public ResourcePackEntry getHighlightEntry() {
        return highlightEntry;
    }

    public void setHighlightEntry(@Nullable ResourcePackEntry highlightEntry) {
        this.highlightEntry = highlightEntry;
        if (highlightEntry != null) {
            leftArrow.setClickable(highlightEntry.canIntoAvailable());
            rightArrow.setClickable(highlightEntry.canIntoSelected());
            upArrow.setClickable(highlightEntry.canGoUp());
            downArrow.setClickable(highlightEntry.canGoDown());
        } else {
            leftArrow.setClickable(false);
            rightArrow.setClickable(false);
            upArrow.setClickable(false);
            downArrow.setClickable(false);
        }
    }

    public ResourcePackGroup getSelectedGroup() {
        return selectedGroup;
    }
}
