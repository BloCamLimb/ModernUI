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

import icyllis.modernui.annotation.MainThread;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * A helper class to get/set clipboard text, methods are only expected
 * to be called on the main thread.
 */
@MainThread
public final class Clipboard {

    /**
     * Get text from clipboard, or {@code null} if it is empty or cannot be converted to string.
     *
     * @return the clipboard text
     */
    @Nullable
    public static String getText() {
        GLFWErrorCallback callback = GLFW.glfwSetErrorCallback(null);
        final String text = GLFW.glfwGetClipboardString(NULL);
        GLFW.glfwSetErrorCallback(callback);
        if (text == null) {
            return null;
        }
        // fix surrogate pairs
        final StringBuilder builder = new StringBuilder();
        for (int i = 0, l = text.length(); i < l; i++) {
            final char _c1 = text.charAt(i);
            final char _c2;
            if (Character.isHighSurrogate(_c1) && i + 1 < l) {
                _c2 = text.charAt(i + 1);
                if (Character.isLowSurrogate(_c2)) {
                    builder.append(_c1).append(_c2);
                    ++i;
                } else if (Character.isSurrogate(_c1))
                    builder.append('\uFFFD');
                else
                    builder.append(_c1);
            } else if (Character.isSurrogate(_c1))
                builder.append('\uFFFD');
            else
                builder.append(_c1);
        }
        return builder.toString();
    }

    /**
     * Set clipboard text.
     *
     * @param text the text to set, must be not null
     */
    public static void setText(@Nonnull CharSequence text) {
        GLFW.glfwSetClipboardString(NULL, text);
    }
}
