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

import icyllis.modernui.api.animation.IAnimation;
import icyllis.modernui.api.element.IElement;
import icyllis.modernui.api.global.IModuleFactory;
import icyllis.modernui.api.manager.IModuleManager;
import icyllis.modernui.gui.element.Background;
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

    private List<IntConsumer> moduleEvents = new ArrayList<>();

    private List<IAnimation> animations = new ArrayList<>();

    private ElementPool popupPool;

    public List<IGuiEventListener> popupListener = new ArrayList<>(1);

    /**
     * The current module which is building, used to add event listener
     */
    private ElementPool currentPool;

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
        pools.stream().filter(m -> !m.test(id)).forEach(ElementPool::clear);
        pools.forEach(e -> e.resize(width, height));
        moduleEvents.forEach(e -> e.accept(id));
    }

    @Override
    public void openPopup(float fadeInTime, int id) {
        if (popupPool != null || !popupListener.isEmpty()) {
            ModernUI.LOGGER.warn(MARKER, "Failed to open popup, there's already a popup open");
            return;
        }
        popups.stream().filter(m -> m.test(id)).findFirst()
                .ifPresent(popupPool -> {
                    this.popupPool = popupPool;
                    this.popupListener.add(popupPool);
                    popupPool.accept(new Background(fadeInTime));
                    popupPool.build();
                    popupPool.resize(width, height);
                });
    }

    @Override
    public void closePopup() {
        popupListener.clear();
        if (popupPool != null) {
            popupPool.clear();
            popupPool = null;
        }
    }

    @Override
    public void addModuleEvent(IntConsumer event) {
        moduleEvents.add(event);
    }

    @Override
    public void addEventListener(IGuiEventListener listener) {
        if (currentPool != null) {
            currentPool.addEventListener(listener);
        } else {
            ModernUI.LOGGER.warn(MARKER, "Failed to add event listener for {}, there's no module under building", listener);
        }
    }

    @Override
    public void addAnimation(IAnimation animation) {
        animations.add(animation);
    }

    public void draw() {
        animations.forEach(e -> e.update(floatingPointTicks));
        pools.forEach(elementPool -> elementPool.draw(floatingPointTicks));
        if (popupPool != null) {
            popupPool.draw(floatingPointTicks);
        }
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        pools.forEach(e -> e.resize(width, height));
        if (popupPool != null)
            popupPool.resize(width, height);
    }

    public void clear() {
        pools.clear();
        animations.clear();
        closePopup();
        master = null;
    }

    @Override
    public void addModule(IntPredicate availability, Consumer<Consumer<IElement>> pool) {
        ElementPool newPool = new ElementPool(availability, pool);
        pools.add(0, newPool);
    }

    @Override
    public void addPopupModule(int id, Consumer<Consumer<IElement>> pool) {
        ElementPool newPool = new ElementPool(i -> i == id, pool);
        popups.add(newPool);
    }

    public void clientTick() {
        ticks++;
        pools.forEach(e -> e.tick(ticks));
    }

    // called before draw
    public void renderTick(float partialTick) {
        floatingPointTicks = ticks + partialTick;
        animations.removeIf(IAnimation::shouldRemove);
    }

    public void resetTicks() {
        ticks = 0;
        floatingPointTicks = 0;
    }

    public float getAnimationTime() {
        return floatingPointTicks;
    }

    public void setExtraData(PacketBuffer extraData) {
        this.extraData = extraData;
    }

    public void setCurrentPool(ElementPool pool) {
        this.currentPool = pool;
    }

    @Override
    public PacketBuffer getExtraData() {
        return extraData;
    }
}
