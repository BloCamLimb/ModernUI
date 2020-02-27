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

package icyllis.modernui.gui.element;

/**
 * Universal
 */
public class Widget<T> implements IBase {

    public EventListener<T> listener;

    public Widget() {

    }

    protected void setListener(T t) {
        listener = new EventListener<>(t);
    }

    @Override
    public void draw() {

    }

    @Override
    public void resize(int width, int height) {
        listener.resize(width, height);
    }

}
