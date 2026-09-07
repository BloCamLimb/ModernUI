/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.system;

import icyllis.modernui.core.Handler;
import icyllis.modernui.core.Looper;

import static org.lwjgl.sdl.SDLTimer.*;

/**
 * Core timer facilities.
 *
 * @since 3.14.0
 */
public final class SystemClock {

    private SystemClock() {
    }

    /**
     * Returns the current value of system's highest-resolution monotonic time source,
     * in milliseconds. The resolution of the timer is system dependent, but is usually
     * on the order of a few micro- or nanoseconds. The timer measures time elapsed
     * since framework was initialized.
     * <p>
     * The {@link Handler} class can schedule asynchronous callbacks at an absolute or
     * relative time. Handler objects also use this clock, and require an {@link Looper}
     * (normally present in any GUI application).
     * <p>
     * This time base is used in all input events and frame events, but not in high-level API
     * (such as animations).
     * <p>
     * Calling this method should be equivalent to calling {@code uptimeNanos() / 1000000}.
     *
     * @return current time in milliseconds
     */
    public static long uptimeMillis() {
        return SDL_GetTicks();
    }

    /**
     * Returns the current value of system's highest-resolution monotonic time source,
     * in nanoseconds. The resolution of the timer is system dependent, but is usually
     * on the order of a few micro- or nanoseconds. The timer measures time elapsed
     * since framework was initialized.
     * <p>
     * Calling this method is faster than {@link System#nanoTime()}. This time base
     * is used in all input events and frame events, but not in high-level API
     * (such as animations).
     *
     * @return current time in nanoseconds
     */
    public static long uptimeNanos() {
        return SDL_GetTicksNS();
    }
}
