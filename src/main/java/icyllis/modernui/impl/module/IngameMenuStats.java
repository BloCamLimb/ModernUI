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

import icyllis.modernui.animation.Animation;
import icyllis.modernui.animation.Applier;
import icyllis.modernui.animation.ITimeInterpolator;
import icyllis.modernui.impl.background.MenuSettingsBG;
import icyllis.modernui.test.discard.WidgetLayout;
import icyllis.modernui.test.discard.ModuleGroup;
import icyllis.modernui.widget.LineTextButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.resources.I18n;
import net.minecraft.network.play.client.CClientStatusPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class IngameMenuStats extends ModuleGroup {

    private List<LineTextButton> buttons = new ArrayList<>();

    private WidgetLayout buttonLayout;

    private final ClientPlayNetHandler netHandler;

    private final IngameMenuHome home;

    private final MenuSettingsBG bg;

    public IngameMenuStats(IngameMenuHome home) {
        this.home = home;
        netHandler = Objects.requireNonNull(Minecraft.getInstance().getConnection());
        netHandler.sendPacket(new CClientStatusPacket(CClientStatusPacket.State.REQUEST_STATS));

        bg = new MenuSettingsBG(home);
        addDrawable(bg);

        buttonLayout = new WidgetLayout(buttons, WidgetLayout.Direction.HORIZONTAL_CENTER, 16);

        Consumer<LineTextButton> consumer = s -> {
            addWidget(s);
            buttons.add(s);
        };
        /*consumer.accept(new LineTextButton(this, I18n.format("stat.generalButton"), 48f,
                () -> switchChildModule(1), i -> i == 1));
        consumer.accept(new LineTextButton(this, I18n.format("stat.blocksButton"), 48f,
                () -> switchChildModule(2), i -> i == 2));
        consumer.accept(new LineTextButton(this, I18n.format("stat.itemsButton"), 48f,
                () -> switchChildModule(3), i -> i == 3));
        consumer.accept(new LineTextButton(this, I18n.format("stat.mobsButton"), 48f,
                () -> switchChildModule(4), i -> i == 4));*/

        consumer.accept(
                new LineTextButton.Builder(I18n.format("stat.generalButton"))
                        .setWidth(48f)
                        .build(this)
                        .buildCallback(() -> switchChildModule(1), i -> i == 1)
        );
        consumer.accept(
                new LineTextButton.Builder(I18n.format("stat.blocksButton"))
                        .setWidth(48f)
                        .build(this)
                        .buildCallback(() -> switchChildModule(2), i -> i == 2)
        );
        consumer.accept(
                new LineTextButton.Builder(I18n.format("stat.itemsButton"))
                        .setWidth(48f)
                        .build(this)
                        .buildCallback(() -> switchChildModule(3), i -> i == 3)
        );
        consumer.accept(
                new LineTextButton.Builder(I18n.format("stat.mobsButton"))
                        .setWidth(48f)
                        .build(this)
                        .buildCallback(() -> switchChildModule(4), i -> i == 4)
        );

        int i = 0;
        addChildModule(++i, StatsGeneral::new);
        addChildModule(++i, StatsBlocks::new);
        addChildModule(++i, StatsItems::new);
        addChildModule(++i, StatsMobs::new);



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

    @Override
    public void tick(int ticks) {
        super.tick(ticks);
        if ((ticks & 127) == 0) {
            netHandler.sendPacket(new CClientStatusPacket(CClientStatusPacket.State.REQUEST_STATS));
        }
    }

    @Override
    public void onChildModuleChanged(int id) {
        super.onChildModuleChanged(id);
        buttons.forEach(e -> e.onModuleChanged(id));
    }
}
