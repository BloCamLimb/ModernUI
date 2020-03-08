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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

/**
 * A.K.A Module
 */
public class ElementPool implements IntPredicate, Consumer<IElement> {

    private IntPredicate availability;

    private Consumer<Consumer<IElement>> builder;

    private List<IElement> elements = new ArrayList<>();

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
            builder.accept(this);
            elements.sort(Comparator.comparing(IElement::priority));
            built = true;
        }
    }

    public void clear() {
        elements.clear();
        built = false;
    }

    @Override
    public void accept(IElement iBase) {
        elements.add(iBase);
    }

    @Override
    public boolean test(int value) {
        return availability.test(value);
    }
}
