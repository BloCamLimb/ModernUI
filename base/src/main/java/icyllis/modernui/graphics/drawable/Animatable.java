/*
 * ModernUI.
 * Copyright (C) 2024-2026 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.drawable;

/**
 * Interface that drawables supporting animations should implement.
 */
public interface Animatable {
    /**
     * Starts the drawable's animation.
     */
    void start();

    /**
     * Stops the drawable's animation.
     */
    void stop();

    /**
     * Indicates whether the animation is running.
     *
     * @return True if the animation is running, false otherwise.
     */
    boolean isRunning();
}
