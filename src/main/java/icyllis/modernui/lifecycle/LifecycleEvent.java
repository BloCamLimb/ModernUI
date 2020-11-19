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

package icyllis.modernui.lifecycle;

import net.minecraftforge.eventbus.api.Event;

public class LifecycleEvent extends Event {

    private final ILifecycleOwner source;
    private final Type type;

    private LifecycleEvent(ILifecycleOwner source, Type type) {
        this.source = source;
        this.type = type;
    }

    /**
     * Get the source of the event
     *
     * @return the source
     */
    public ILifecycleOwner getSource() {
        return source;
    }

    /**
     * Get the type of the event
     *
     * @return the type
     */
    public Type getType() {
        return type;
    }

    public enum Type {
        ON_CREATE,
        ON_START,
        ON_RESUME,
        ON_PAUSE,
        ON_STOP,
        ON_DESTROY
    }

    public static class Create extends LifecycleEvent {

        public Create(ILifecycleOwner owner) {
            super(owner, Type.ON_CREATE);
        }
    }

    public static class Start extends LifecycleEvent {

        public Start(ILifecycleOwner owner) {
            super(owner, Type.ON_START);
        }
    }

    public static class Resume extends LifecycleEvent {

        public Resume(ILifecycleOwner owner) {
            super(owner, Type.ON_RESUME);
        }
    }

    public static class Pause extends LifecycleEvent {

        public Pause(ILifecycleOwner owner) {
            super(owner, Type.ON_PAUSE);
        }
    }

    public static class Stop extends LifecycleEvent {

        public Stop(ILifecycleOwner owner) {
            super(owner, Type.ON_STOP);
        }
    }

    public static class Destroy extends LifecycleEvent {

        public Destroy(ILifecycleOwner owner) {
            super(owner, Type.ON_DESTROY);
        }
    }
}
