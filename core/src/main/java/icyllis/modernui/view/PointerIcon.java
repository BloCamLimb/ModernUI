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

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;

/**
 * Represents an icon that can be used as a mouse pointer.
 * <p>
 * Pointer icons can be provided either by the system using system types,
 * or using image resources.
 * </p>
 */
public final class PointerIcon {

    public static final int TYPE_DEFAULT = 0;

    public static final int TYPE_HAND = 1002;

    public static final int TYPE_TEXT = 1008;

    private static final PointerIcon DEFAULT_CURSOR = new PointerIcon(MemoryUtil.NULL);
    private static final PointerIcon TEXT_CURSOR;
    private static final PointerIcon HAND_CURSOR;

    static {
        TEXT_CURSOR = new PointerIcon(GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR));
        HAND_CURSOR = new PointerIcon(GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR));
    }

    private final long mHandle;

    private PointerIcon(long handle) {
        mHandle = handle;
    }

    public long getHandle() {
        return mHandle;
    }

    @Nonnull
    public static PointerIcon getSystemIcon(int type) {
        return switch (type) {
            case TYPE_HAND -> HAND_CURSOR;
            case TYPE_TEXT -> TEXT_CURSOR;
            default -> DEFAULT_CURSOR;
        };
    }
}
