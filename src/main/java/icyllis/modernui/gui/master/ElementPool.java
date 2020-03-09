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

import icyllis.modernui.api.element.IElement;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.INestedGuiEventHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

/**
 * A.K.A Module
 */
public class ElementPool implements IntPredicate, Consumer<IElement>, INestedGuiEventHandler {

    private IntPredicate availability;

    private Consumer<Consumer<IElement>> builder;

    private List<IElement> elements = new ArrayList<>();

    private List<IGuiEventListener> subEventListeners = new ArrayList<>();

    private boolean built = false;

    public ElementPool(IntPredicate availability, Consumer<Consumer<IElement>> builder) {
        this.availability = availability;
        this.builder = builder;
    }

    public void draw(float currentTime) {
        elements.forEach(iElement -> iElement.draw(currentTime));
    }

    public void resize(int width, int height) {
        elements.forEach(i -> i.resize(width, height));
    }

    public void tick(int ticks) {
        elements.forEach(iElement -> iElement.tick(ticks));
    }

    public void build() {
        if (!built) {
            GlobalModuleManager.INSTANCE.setCurrentPool(this);
            builder.accept(this);
            GlobalModuleManager.INSTANCE.setCurrentPool(null);
            elements.sort(Comparator.comparing(IElement::priority));
            built = true;
        }
    }

    public void clear() {
        elements.clear();
        subEventListeners.clear();
        built = false;
    }

    public void addEventListener(IGuiEventListener listener) {
        subEventListeners.add(listener);
    }

    @Override
    public void accept(IElement iBase) {
        elements.add(iBase);
    }

    @Override
    public boolean test(int value) {
        return availability.test(value);
    }

    @Override
    public void mouseMoved(double xPos, double yPos) {
        subEventListeners.forEach(e -> e.mouseMoved(xPos, yPos));
    }

    @Nonnull
    @Override
    public List<? extends IGuiEventListener> children() {
        return subEventListeners;
    }

    @Override
    public boolean isDragging() {
        return false;
    }

    @Override
    public void setDragging(boolean p_setDragging_1_) {

    }

    @Nullable
    @Override
    public IGuiEventListener getFocused() {
        return null;
    }

    @Override
    public void setFocused(@Nullable IGuiEventListener p_setFocused_1_) {

    }
}
