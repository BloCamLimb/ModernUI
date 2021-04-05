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

package icyllis.modernui.platform;

/**
 * Enumerates available window modes.
 */
public enum WindowMode {
    /**
     * The window is movable and takes up a subsection of the screen.
     * This is the default mode.
     */
    WINDOWED,

    /**
     * The window is running in exclusive fullscreen and is potentially using a
     * different resolution to the desktop.
     */
    FULLSCREEN,

    /**
     * The window is running in non-exclusive fullscreen, where it expands to
     * fill the screen at the native desktop resolution.
     */
    FULLSCREEN_BORDERLESS,

    /**
     * The window is running in maximized mode, usually triggered by clicking
     * the operating system's maximize button.
     */
    MAXIMIZED,

    /**
     * The window is running in minimized mode, usually triggered by clicking
     * the operating system's minimize button.
     */
    MINIMIZED
}
