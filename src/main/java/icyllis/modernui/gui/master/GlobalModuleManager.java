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

package icyllis.modernui.gui.master;

import icyllis.modernui.gui.animation.IAnimation;
import icyllis.modernui.api.global.IModuleFactory;
import icyllis.modernui.api.manager.IModuleManager;
import icyllis.modernui.gui.element.IElement;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.network.PacketBuffer;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

public enum GlobalModuleManager implements IModuleFactory, IModuleManager {
    INSTANCE;

    private final Marker MARKER = MarkerManager.getMarker("MODULE");

    @Nullable
    public IMasterScreen master;

    private PacketBuffer extraData;

    private List<ModuleBuilder> builders = new ArrayList<>();

    private List<IAnimation> animations = new ArrayList<>();

    @Nullable
    public IGuiModule popup;

    private int ticks = 0;

    private float floatingPointTicks = 0;

    private int width, height;

    public void build(IMasterScreen master, int width, int height) {
        this.master = master;
        this.width = width;
        this.height = height;
        this.builders.forEach(master::addEventListener);
        this.switchModule(0);
    }
    @Override
    public void switchModule(int newID) {
        builders.stream().filter(m -> m.test(newID)).forEach(ModuleBuilder::build);
        builders.forEach(e -> e.onModuleChanged(newID));
        builders.stream().filter(m -> !m.test(newID)).forEach(ModuleBuilder::clear);
        builders.forEach(e -> e.resize(width, height));
    }

    @Override
    public void openPopup(IGuiModule popup) {
        if (this.popup != null) {
            ModernUI.LOGGER.warn(MARKER, "Failed to open popup, there's already a popup open");
            return;
        }
        popup.resize(width, height);
        this.popup = popup;
        if (master != null) {
            master.setHasPopup(true);
        } else {
            ModernUI.LOGGER.fatal(MARKER, "Current screen shouldn't have been null when calling #openPopup");
        }
    }

    @Override
    public void closePopup() {
        if (popup != null) {
            popup = null;
        }
        if (master != null) {
            master.setHasPopup(false);
        } else {
            ModernUI.LOGGER.fatal(MARKER, "Current screen shouldn't have been null when calling #closePopup()V");
        }
    }

    @Override
    public void addAnimation(IAnimation animation) {
        animations.add(animation);
    }

    @Override
    public void refreshCursor() {
        if (master != null) {
            master.refreshCursor();
        }  else {
            ModernUI.LOGGER.fatal(MARKER, "Current screen shouldn't have been null when calling #refreshCursor()V");
        }
    }

    public void draw() {
        animations.forEach(e -> e.update(floatingPointTicks));
        builders.forEach(e -> e.draw(floatingPointTicks));
        if (popup != null) {
            popup.draw(floatingPointTicks);
        }
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        builders.forEach(e -> e.resize(width, height));
        if (popup != null)
            popup.resize(width, height);
    }

    public void clear() {
        builders.clear();
        animations.clear();
        popup = null;
        master = null;
        extraData = null;
    }

    @Override
    public void addModule(IntPredicate availability, Supplier<IGuiModule> module) {
        ModuleBuilder builder = new ModuleBuilder(availability, module);
        builders.add(builder);
    }

    public void clientTick() {
        ticks++;
        builders.forEach(e -> e.tick(ticks));
        if (popup != null)
            popup.tick(ticks);
    }

    public void renderTick(float partialTick) {
        floatingPointTicks = ticks + partialTick;
        animations.removeIf(IAnimation::shouldRemove);
    }

    public void resetTicks() {
        ticks = 0;
        floatingPointTicks = 0;
    }

    @Override
    public float getAnimationTime() {
        return floatingPointTicks;
    }

    @Override
    public int getTicks() {
        return ticks;
    }

    public void setExtraData(PacketBuffer extraData) {
        this.extraData = extraData;
    }

    @Override
    public PacketBuffer getExtraData() {
        return extraData;
    }
}
