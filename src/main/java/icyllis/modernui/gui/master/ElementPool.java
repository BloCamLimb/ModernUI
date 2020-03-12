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

import icyllis.modernui.api.manager.IModuleManager;
import icyllis.modernui.gui.element.IElement;
import net.minecraft.client.gui.IGuiEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/**
 * A.K.A Module
 */
public class ElementPool implements IGuiEventListener {

    private final IntPredicate availability;

    private final Consumer<IModuleManager> builder;

    private List<IElement> elements = new ArrayList<>();

    private List<IntConsumer> events = new ArrayList<>();

    private List<IGuiEventListener> listeners = new ArrayList<>();

    private boolean built = false;

    public ElementPool(IntPredicate availability, Consumer<IModuleManager> builder) {
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
            GlobalModuleManager.INSTANCE.setPool(this);
            builder.accept(GlobalModuleManager.INSTANCE);
            GlobalModuleManager.INSTANCE.setPool(null);
            elements.sort(Comparator.comparing(IElement::priority));
            built = true;
        }
    }

    public void runModuleEvents(int id) {
        events.forEach(e -> e.accept(id));
    }

    public void clear() {
        elements.clear();
        events.clear();
        listeners.clear();
        built = false;
    }

    public boolean test(int value) {
        return availability.test(value);
    }

    public void addElement(IElement element) {
        elements.add(element);
    }

    public void addModuleEvent(IntConsumer event) {
        events.add(event);
    }

    public void addEventListener(IGuiEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void mouseMoved(double xPos, double yPos) {
        listeners.forEach(e -> e.mouseMoved(xPos, yPos));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        for (IGuiEventListener listener : listeners) {
            if (listener.mouseClicked(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        for (IGuiEventListener listener : listeners) {
            if (listener.mouseReleased(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double RDX, double RDY) {
        for (IGuiEventListener listener : listeners) {
            if (listener.mouseDragged(mouseX, mouseY, mouseButton, RDX, RDY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double shift) {
        for (IGuiEventListener listener : listeners) {
            if (listener.mouseScrolled(mouseX, mouseY, shift)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifier) {
        for (IGuiEventListener listener : listeners) {
            if (listener.keyPressed(key, scanCode, modifier)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifier) {
        for (IGuiEventListener listener : listeners) {
            if (listener.keyReleased(key, scanCode, modifier)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char charCode, int modifier) {
        for (IGuiEventListener listener : listeners) {
            if (listener.charTyped(charCode, modifier)) {
                return true;
            }
        }
        return false;
    }
}
