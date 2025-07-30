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

package icyllis.modernui.test;

import icyllis.modernui.ModernUI;
import icyllis.modernui.TestFragment;
import icyllis.modernui.audio.AudioManager;
import icyllis.modernui.core.windows.WindowsNativeWindowBorder;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.Platform;

public class TestWindowsNativeBorder {

    private static Runnable cleanup;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
        try (ModernUI app = new ModernUI()) {
            if (Platform.get() == Platform.WINDOWS) {
                app.run(new TestFragment(), win -> {
                    long hwnd = GLFWNativeWin32.glfwGetWin32Window(win);
                    @SuppressWarnings("resource") var newProc = new WindowsNativeWindowBorder.WndProc(hwnd);
                    cleanup = newProc::destroy;
                });
                cleanup.run();
            } else {
                app.run(new TestFragment());
            }
        }
        AudioManager.getInstance().close();
        System.gc();
    }
}
