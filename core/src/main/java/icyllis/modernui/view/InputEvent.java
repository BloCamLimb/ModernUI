/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.view;

/**
 * The base class of input events.
 */
public abstract class InputEvent {

    InputEvent() {
    }

    /**
     * Copies the event.
     *
     * @return A deep copy of the event.
     */
    public abstract InputEvent copy();

    /**
     * Recycles the event.
     * <p>
     * This method should only be called by system.
     */
    public abstract void recycle();

    /**
     * Get the time in milliseconds that this event object
     * is created in the GLFW time base
     *
     * @return the time this event occurred
     */
    public abstract long getEventTime();

    /**
     * Get the precise time in nanoseconds that this event
     * object is created in the GLFW time base
     *
     * @return the time this event occurred
     */
    public abstract long getEventTimeNano();

    /**
     * Marks the input event as being canceled.
     */
    public abstract void cancel();
}
