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
import icyllis.modernui.view.InputEventListener;
//import icyllis.modernui.view.MotionEvent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.system.Callback;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.stream.Stream;

//TODO
@MainThread
public class InputHandler {

    private static final InputHandler sInstance = new InputHandler();

    private InputHandler() {
    }

    public static InputHandler getInstance() {
        return sInstance;
    }

    public static void register(@Nonnull Window window, @Nonnull InputEventListener listener) {
        final long handle = window.getHandle();
        Stream.of(GLFW.glfwSetCursorPosCallback(handle, new CursorPos(window, listener)))
                .filter(Objects::nonNull)
                .forEach(Callback::free);
    }

    public static void onCursorPos(@Nonnull Window window, @Nonnull InputEventListener listener,
                                   double xPos, double yPos) {
        /*final long now = RenderCore.timeNanos();
        float eventX = (float) (xPos * window.screenToPixelX());
        float eventY = (float) (yPos * window.screenToPixelY());
        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_HOVER_MOVE,
                eventX, eventY, 0);
        listener.onInputEvent(event);*/
    }

    private static class CursorPos extends GLFWCursorPosCallback {

        private final Window window;
        private final InputEventListener listener;

        public CursorPos(Window window, InputEventListener listener) {
            this.window = window;
            this.listener = listener;
        }

        @Override
        public void invoke(long handle, double xPos, double yPos) {
            assert window.getHandle() == handle;
            onCursorPos(window, listener, xPos, yPos);
        }
    }
}
