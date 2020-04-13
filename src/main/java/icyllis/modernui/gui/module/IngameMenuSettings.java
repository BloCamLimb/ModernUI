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
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.background.MenuSettingsBG;
import icyllis.modernui.gui.layout.WidgetLayout;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.ModuleGroup;
import icyllis.modernui.gui.popup.PopupMenu;
import icyllis.modernui.gui.widget.DropDownMenu;
import icyllis.modernui.gui.widget.LineTextButton;
import icyllis.modernui.system.ModIntegration;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class IngameMenuSettings extends ModuleGroup {

    private List<LineTextButton> buttons = new ArrayList<>();

    private WidgetLayout buttonLayout;

    private float xOffset;

    private final IngameMenuHome home;

    public IngameMenuSettings(IngameMenuHome home) {
        this.home = home;
        addElements(new MenuSettingsBG());
        buttonLayout = new WidgetLayout(buttons, WidgetLayout.Direction.HORIZONTAL_CENTER, 16);
        Consumer<LineTextButton> consumer = s -> {
            addElements(s);
            addMouseListener(s);
            buttons.add(s);
        };
        consumer.accept(new LineTextButton(I18n.format("gui.modernui.settings.tab.general"), 48f,
                () -> switchChildModule(1), i -> i == 1));
        consumer.accept(new LineTextButton(I18n.format("gui.modernui.settings.tab.video"), 48f,
                () -> switchChildModule(2), i -> i == 2));
        consumer.accept(new LineTextButton(I18n.format("gui.modernui.settings.tab.audio"), 48f,
                () -> switchChildModule(3), i -> i == 3));
        consumer.accept(new LineTextButton(I18n.format("gui.modernui.settings.tab.controls"), 48f,
                () -> switchChildModule(4), i -> i == 4));
        consumer.accept(new LineTextButton(I18n.format("gui.modernui.settings.tab.assets"), 48f,
                this::openAssetsMenu, i -> i >= 5 && i <= 8));
        int i = 0;
        addChildModule(++i, SettingGeneral::new);
        addChildModule(++i, SettingVideo::new);
        addChildModule(++i, SettingAudio::new);
        addChildModule(++i, SettingControls::new);
        addChildModule(++i, SettingResourcePack::new);
        addChildModule(++i, SettingLanguage::new);

        int c = GlobalModuleManager.INSTANCE.getWindowWidth();
        if (home.getTransitionDirection(true)) {
            GlobalModuleManager.INSTANCE.addAnimation(new Animation(4, true)
                    .applyTo(new Applier(-c, 0, v -> xOffset = v)));
        } else {
            GlobalModuleManager.INSTANCE.addAnimation(new Animation(4, true)
                    .applyTo(new Applier(c, 0, v -> xOffset = v)));
        }

        switchChildModule(1);
    }

    @Override
    public void draw(float time) {
        RenderSystem.pushMatrix();
        RenderSystem.translatef(xOffset, 0, 0);
        super.draw(time);
        RenderSystem.popMatrix();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        buttonLayout.layout(width / 2f, 20);
    }

    @Override
    public int[] changingModule() {
        int c = GlobalModuleManager.INSTANCE.getWindowWidth();
        if (home.getTransitionDirection(false)) {
            GlobalModuleManager.INSTANCE.addAnimation(new Animation(4, true)
                    .applyTo(new Applier(0, c, v -> xOffset = v)));
        } else {
            GlobalModuleManager.INSTANCE.addAnimation(new Animation(4, true)
                    .applyTo(new Applier(0, -c, v -> xOffset = v)));
        }
        return new int[]{1, 4};
    }

    @SuppressWarnings("NoTranslation")
    private void openAssetsMenu() {
        List<String> tabs = Lists.newArrayList(I18n.format("gui.modernui.settings.tab.resourcePacks"),
                I18n.format("gui.modernui.settings.tab.language"));
        //TODO optifine shaders
        if (ModIntegration.optifineLoaded) {
            tabs.add(I18n.format("of.options.shadersTitle") + " (WIP)");
        }
        DropDownMenu menu = new DropDownMenu(tabs, getCid() - 5, 12, this::assetsButtonMenuActions, DropDownMenu.Align.RIGHT);
        LineTextButton t = buttons.get(4);
        menu.setPos(t.getRight() - 8, t.getBottom() + 1);
        GlobalModuleManager.INSTANCE.openPopup(new PopupMenu(menu), false);
    }

    private void assetsButtonMenuActions(int index) {
        if (index >= 0 && index <= 2) {
            if (ModIntegration.optifineLoaded && index == 2) {
                try {
                    Class<?> clazz = Class.forName("net.optifine.shaders.gui.GuiShaders");
                    Constructor<?> constructor = clazz.getConstructor(Screen.class, GameSettings.class);
                    Screen screen = (Screen) constructor.newInstance(null, Minecraft.getInstance().gameSettings);
                    Minecraft.getInstance().displayGuiScreen(screen);
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException | ClassCastException e) {
                    e.printStackTrace();
                }
                return;
            }
            switchChildModule(5 + index);
        }
    }

    @Override
    public void moduleChanged(int id) {
        super.moduleChanged(id);
        buttons.forEach(e -> e.onModuleChanged(id));
    }

}
