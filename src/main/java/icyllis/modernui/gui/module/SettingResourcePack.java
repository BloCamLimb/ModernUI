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
import icyllis.modernui.gui.background.ResourcePackBG;
import icyllis.modernui.gui.layout.WidgetLayout;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.IModule;
import icyllis.modernui.gui.master.IWidget;
import icyllis.modernui.gui.master.Module;
import icyllis.modernui.gui.option.ResourcePackEntry;
import icyllis.modernui.gui.option.ResourcePackGroup;
import icyllis.modernui.gui.popup.ConfirmCallback;
import icyllis.modernui.gui.popup.PopupConfirm;
import icyllis.modernui.gui.scroll.ScrollWindow;
import icyllis.modernui.gui.widget.ArrowButton;
import icyllis.modernui.gui.widget.TextFrameButton;
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

    private ResourcePackGroup aGroup;

    private ResourcePackGroup sGroup;

    private ArrowButton leftArrow;

    private ArrowButton rightArrow;

    private ArrowButton upArrow;

    private ArrowButton downArrow;

    private WidgetLayout arrowLayout;

    @Nullable
    private ResourcePackEntry highlight;

    private boolean changed = false;

    public SettingResourcePack() {
        minecraft = Minecraft.getInstance();
        addBackground(new ResourcePackBG());

        Function<Integer, Float> widthFunc = w -> Math.min((w - 80) / 2f - 8f, 240);
        Function<Integer, Float> leftXFunc = w -> w / 2f - widthFunc.apply(w) - 8f;

        ScrollWindow<ResourcePackGroup> aWindow = new ScrollWindow<>(this, leftXFunc, h -> 36f, widthFunc, h -> h - 72f);
        ScrollWindow<ResourcePackGroup> sWindow = new ScrollWindow<>(this, w -> w / 2f + 8, h -> 36f, widthFunc, h -> h - 72f);

        aGroup = new ResourcePackGroup(this, aWindow, minecraft.getResourcePackList(), ResourcePackGroup.Type.AVAILABLE);
        sGroup = new ResourcePackGroup(this, sWindow, minecraft.getResourcePackList(), ResourcePackGroup.Type.SELECTED);

        aWindow.addGroups(Lists.newArrayList(aGroup));
        sWindow.addGroups(Lists.newArrayList(sGroup));

        List<ArrowButton> list = new ArrayList<>();

        leftArrow = new ArrowButton(ArrowButton.Direction.LEFT, this::intoAvailable, false);
        rightArrow = new ArrowButton(ArrowButton.Direction.RIGHT, this::intoSelected, false);
        upArrow = new ArrowButton(ArrowButton.Direction.UP, this::goUp, false);
        downArrow = new ArrowButton(ArrowButton.Direction.DOWN, this::goDown, false);

        list.add(leftArrow);
        list.add(rightArrow);
        list.add(upArrow);
        list.add(downArrow);

        arrowLayout = new WidgetLayout(list, WidgetLayout.Direction.VERTICAL_POSITIVE, 4);

        addWidget(aWindow);
        addWidget(sWindow);
        list.forEach(this::addWidget);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        arrowLayout.layout(width / 2f - 6, height * 0.25f);
    }

    @Override
    public boolean onBack() {
        if (changed) {
            applyResourcePacks(0);
        }
        return false;
    }

    @Override
    public void upperModuleExit() {
        super.upperModuleExit();
        if (changed) {
            applyResourcePacks(0);
        }
    }

    //TODO buttons
    private void applyResourcePacks(int callback) {
        if (callback == ConfirmCallback.CANCEL) {
            return;
        }
        List<ClientResourcePackInfo> list = Lists.newArrayList();
        GameSettings gameSettings = minecraft.gameSettings;

        for (ResourcePackEntry c2 : sGroup.getEntries()) {
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
        if (callback == ConfirmCallback.CONFIRM) {
            minecraft.reloadResources();
        }
        changed = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        setHighlight(null);
        return false;
    }

    private void intoSelected() {
        if (highlight != null && highlight.canIntoSelected()) {
            aGroup.getEntries().remove(highlight);
            aGroup.layoutGroup();
            highlight.intoSelected(sGroup);
            sGroup.layoutGroup();
            sGroup.followEntry(highlight);
            highlight.setMouseHoverExit();
            setHighlight(highlight);
            changed = true;
        }
    }

    private void intoAvailable() {
        if (highlight != null && highlight.canIntoAvailable()) {
            sGroup.getEntries().remove(highlight);
            sGroup.layoutGroup();
            aGroup.getEntries().add(highlight);
            aGroup.layoutGroup();
            highlight.setMouseHoverExit();
            setHighlight(highlight);
            changed = true;
        }
    }

    private void goUp() {
        if (highlight != null && highlight.canGoUp()) {
            highlight.goUp();
            sGroup.layoutGroup();
            sGroup.followEntry(highlight);
            setHighlight(highlight);
            GlobalModuleManager.INSTANCE.refreshMouse();
            changed = true;
        }
    }

    private void goDown() {
        if (highlight != null && highlight.canGoDown()) {
            highlight.goDown();
            sGroup.layoutGroup();
            sGroup.followEntry(highlight);
            setHighlight(highlight);
            GlobalModuleManager.INSTANCE.refreshMouse();
            changed = true;
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
    public ResourcePackEntry getHighlight() {
        return highlight;
    }

    public void setHighlight(@Nullable ResourcePackEntry highlight) {
        this.highlight = highlight;
        if (highlight != null) {
            leftArrow.setAvailable(highlight.canIntoAvailable());
            rightArrow.setAvailable(highlight.canIntoSelected());
            upArrow.setAvailable(highlight.canGoUp());
            downArrow.setAvailable(highlight.canGoDown());
        } else {
            leftArrow.setAvailable(false);
            rightArrow.setAvailable(false);
            upArrow.setAvailable(false);
            downArrow.setAvailable(false);
        }
    }

    public ResourcePackGroup getSelectedGroup() {
        return sGroup;
    }
}
