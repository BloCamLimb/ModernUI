/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
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
import icyllis.modernui.system.ModIntegration;
import icyllis.modernui.ui.animation.Animation;
import icyllis.modernui.ui.animation.Applier;
import icyllis.modernui.ui.animation.ITimeInterpolator;
import icyllis.modernui.ui.test.WidgetLayout;
import icyllis.modernui.ui.master.UIManager;
import icyllis.modernui.ui.test.ModuleGroup;
import icyllis.modernui.ui.test.Align9D;
import icyllis.modernui.ui.view.DropDownMenu;
import icyllis.modernui.ui.view.PopupMenu;
import icyllis.modernui.ui.widget.LineTextButton;
import icyllis.modernui.impl.background.MenuSettingsBG;
import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class IngameMenuSettings extends ModuleGroup {

    private List<LineTextButton> buttons = new ArrayList<>();

    private WidgetLayout buttonLayout;

    private final IngameMenuHome home;

    private final MenuSettingsBG bg;

    public IngameMenuSettings(IngameMenuHome home) {
        this.home = home;
        addDrawable(bg = new MenuSettingsBG(home));

        buttonLayout = new WidgetLayout(buttons, WidgetLayout.Direction.HORIZONTAL_CENTER, 16);
        Consumer<LineTextButton> consumer = s -> {
            addWidget(s);
            buttons.add(s);
        };

        consumer.accept(
                new LineTextButton.Builder(I18n.format("gui.modernui.settings.tab.general"))
                        .setWidth(48f)
                        .build(this)
                        .buildCallback(() -> switchChildModule(1), i -> i == 1)
        );
        consumer.accept(
                new LineTextButton.Builder(I18n.format("gui.modernui.settings.tab.video"))
                        .setWidth(48f)
                        .build(this)
                        .buildCallback(() -> switchChildModule(2), i -> i == 2)
        );
        consumer.accept(
                new LineTextButton.Builder(I18n.format("gui.modernui.settings.tab.audio"))
                        .setWidth(48f)
                        .build(this)
                        .buildCallback(() -> switchChildModule(3), i -> i == 3)
        );
        consumer.accept(
                new LineTextButton.Builder(I18n.format("gui.modernui.settings.tab.controls"))
                        .setWidth(48f)
                        .build(this)
                        .buildCallback(() -> switchChildModule(4), i -> i == 4)
        );
        consumer.accept(new LineTextButton.Builder(I18n.format("gui.modernui.settings.tab.assets"))
                .setWidth(48f)
                .build(this)
                .buildCallback(this::openAssetsMenu, i -> i >= 5 && i <= 8)
        );
        int i = 0;
        addChildModule(++i, SettingGeneral::new);
        addChildModule(++i, SettingVideo::new);
        addChildModule(++i, SettingAudio::new);
        addChildModule(++i, SettingControls::new);
        addChildModule(++i, SettingResourcePack::new);
        addChildModule(++i, SettingLanguage::new);



        switchChildModule(1);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        buttonLayout.layout(width / 2f, 20);
    }

    @Override
    public int[] onChangingModule() {
        int c = home.getGameWidth();
        if (home.getTransitionDirection(false)) {
            new Animation(200)
                    .applyTo(
                            new Applier(0, c, bg::getXOffset, bg::setXOffset)
                                    .setInterpolator(ITimeInterpolator.SINE))
                    .start();
        } else {
            new Animation(200)
                    .applyTo(
                            new Applier(0, -c, bg::getXOffset, bg::setXOffset)
                                    .setInterpolator(ITimeInterpolator.SINE))
                    .start();
        }
        return new int[]{1, 4};
    }

    @SuppressWarnings("NoTranslation")
    private void openAssetsMenu() {
        List<String> tabs = Lists.newArrayList(I18n.format("gui.modernui.settings.tab.resourcePacks"),
                I18n.format("gui.modernui.settings.tab.language"));
        if (ModIntegration.optifineLoaded) {
            tabs.add(I18n.format("of.options.shadersTitle"));
        }
        DropDownMenu menu = new DropDownMenu.Builder(
                tabs, getCid() - 5)
                .setAlign(Align9D.TOP_RIGHT).build(this).buildCallback(this::assetsButtonMenuActions);
        LineTextButton t = buttons.get(4);
        menu.locate(t.getRight() - 8, t.getBottom() + 1);
        UIManager.INSTANCE.openPopup(new PopupMenu(menu), false);
    }

    private void assetsButtonMenuActions(int index) {
        if (index >= 0 && index <= 2) {
            if (index == 2) {
                /*try {
                    Class<?> clazz = Class.forName("net.optifine.shaders.gui.GuiShaders");
                    Constructor<?> constructor = clazz.getConstructor(Screen.class, GameSettings.class);
                    Screen screen = (Screen) constructor.newInstance(GlobalModuleManager.INSTANCE.getModernScreen(), Minecraft.getInstance().gameSettings);
                    Minecraft.getInstance().displayGuiScreen(screen);
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException | ClassCastException e) {
                    e.printStackTrace();
                }*/
                ModIntegration.OptiFine.openShadersGui();
                return;
            }
            switchChildModule(5 + index);
        }
    }

    @Override
    public void onChildModuleChanged(int id) {
        super.onChildModuleChanged(id);
        buttons.forEach(e -> e.onModuleChanged(id));
    }

}
