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

package icyllis.modern.ui.button;

import icyllis.modern.api.element.IBaseGetter;

import java.util.function.Consumer;

public class InternalEvent<T extends IBaseGetter> {

    public static int MOUSE_HOVER_ON = 1;
    public static int MOUSE_HOVER_OFF = 2;

    private final int id;

    private final Consumer<T> consumer;

    public InternalEvent(int id, Consumer<T> consumer) {
        this.id = id;
        this.consumer = consumer;
    }

    public int getId() {
        return id;
    }

    public void run(T t) {
        consumer.accept(t);
    }
}
