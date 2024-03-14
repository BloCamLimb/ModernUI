/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.core.awt;

import java.awt.*;

/**
 * Interface for platform-specific implementations of {@link AWTGLCanvas}.
 *
 * @author Kai Burjack
 */
public interface PlatformGLCanvas {
    long create(Canvas canvas, GLData data, GLData effective) throws AWTException;

    boolean deleteContext(long context);

    boolean makeCurrent(long context);

    boolean isCurrent(long context);

    boolean swapBuffers();

    boolean delayBeforeSwapNV(float seconds);

    void lock() throws AWTException;

    void unlock() throws AWTException;

    void dispose();
}
