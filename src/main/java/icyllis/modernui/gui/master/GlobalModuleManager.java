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
import icyllis.modernui.gui.element.Background;
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

public enum GlobalModuleManager implements IModuleFactory, IModuleManager {
    INSTANCE;

    private final Marker MARKER = MarkerManager.getMarker("MODULE");

    @Nullable
    public IMasterScreen master;

    private PacketBuffer extraData;

    private List<ElementPool> pools = new ArrayList<>();

    private List<ElementPool> popups = new ArrayList<>();

    private List<IAnimation> animations = new ArrayList<>();

    public ElementPool popup;

    /**
     * The current module which is building, used to add event listener
     */
    private ElementPool pool;

    private int ticks = 0;

    private float floatingPointTicks = 0;

    private int width, height;

    public void build(IMasterScreen master, int width, int height) {
        this.master = master;
        this.width = width;
        this.height = height;
        this.pools.forEach(master::addEventListener);
        this.switchTo(0);
    }
    @Override
    public void switchTo(int id) {
        pools.stream().filter(m -> m.test(id)).forEach(ElementPool::build);
        pools.forEach(e -> e.runModuleEvents(id));
        pools.stream().filter(m -> !m.test(id)).forEach(ElementPool::clear);
        pools.forEach(e -> e.resize(width, height));
    }

    @Override
    public void openPopup(float fadeInTime, int id) {
        if (popup != null) {
            ModernUI.LOGGER.warn(MARKER, "Failed to open popup, there's already a popup open");
            return;
        }
        popups.stream().filter(m -> m.test(id)).findFirst()
                .ifPresent(popupPool -> {
                    popupPool.addElement(new Background(fadeInTime));
                    popupPool.build();
                    popupPool.resize(width, height);
                    this.popup = popupPool;
                });
        if (master != null) {
            master.setHasPopup(true);
        } else {
            ModernUI.LOGGER.fatal(MARKER, "Current screen shouldn't have been null when calling #openPopup(FI)V");
        }
    }

    @Override
    public void closePopup() {
        if (popup != null) {
            popup.clear();
            popup = null;
        }
        if (master != null) {
            master.setHasPopup(false);
        } else {
            ModernUI.LOGGER.fatal(MARKER, "Current screen shouldn't have been null when calling #closePopup()V");
        }
    }

    @Override
    public void addElement(IElement element) {
        if (pool != null) {
            pool.addElement(element);
        } else {
            ModernUI.LOGGER.warn(MARKER, "Failed to add event for {}, there's no module under building", element);
        }
    }

    @Override
    public void addModuleEvent(IntConsumer event) {
        if (pool != null) {
            pool.addModuleEvent(event);
        } else {
            ModernUI.LOGGER.warn(MARKER, "Failed to add module event for {}, there's no module under building", event);
        }
    }

    @Override
    public void addEventListener(IGuiEventListener listener) {
        if (pool != null) {
            pool.addEventListener(listener);
        } else {
            ModernUI.LOGGER.warn(MARKER, "Failed to add event listener for {}, there's no module under building", listener);
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
        pools.forEach(elementPool -> elementPool.draw(floatingPointTicks));
        if (popup != null) {
            popup.draw(floatingPointTicks);
        }
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        this.pools.forEach(e -> e.resize(width, height));
        if (popup != null)
            popup.resize(width, height);
    }

    public void clear() {
        pools.clear();
        animations.clear();
        popups.clear();
        pool = null;
        popup = null;
        master = null;
        extraData = null;
    }

    @Override
    public void addModule(IntPredicate availability, Consumer<IModuleManager> manager) {
        ElementPool newPool = new ElementPool(availability, manager);
        pools.add(0, newPool);
    }

    @Override
    public void addPopupModule(int id, Consumer<IModuleManager> manager) {
        ElementPool newPool = new ElementPool(i -> i == id, manager);
        popups.add(newPool);
    }

    public void clientTick() {
        ticks++;
        pools.forEach(e -> e.tick(ticks));
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

    public void setPool(ElementPool pool) {
        this.pool = pool;
    }

    @Override
    public PacketBuffer getExtraData() {
        return extraData;
    }
}
