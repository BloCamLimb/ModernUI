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
import icyllis.modernui.core.windows.WindowsNativeWindowBorder;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.resources.AssetManager;
import icyllis.modernui.resources.DirectoryAssetsProvider;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.ResourcesBuilder;
import icyllis.modernui.resources.ResourcesImpl;
import icyllis.modernui.resources.ResourcesLoader;
import icyllis.modernui.resources.ResourcesProvider;
import icyllis.modernui.util.DisplayMetrics;
import icyllis.modernui.util.Log;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.Platform;

import java.nio.file.Path;

public class MainLaunch {

    private static Runnable cleanup;

    public static final String ns = "root_app";

    public static class image {

        public static final ResourceId test_path_src = new ResourceId(ns, "image", "test_path_src");
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
        try (ModernUI app = new ModernUI()) {

            ResourcesLoader loader = loadExtraResources();

            AssetManager newAssets = new AssetManager.Builder()
                    .addLoader(loader)
                    .build();
            app.getResources().setImpl(new ResourcesImpl(newAssets, null, null));

            Fragment fragment = null;
            if (args.length > 0) {
                try {
                    fragment = (Fragment) Class.forName(args[0]).getConstructor()
                            .newInstance();
                } catch (Exception e) {
                    Log.LOGGER.warn("Cannot instantiate main fragment", e);
                }
                if (fragment == null) {
                    try {
                        fragment = (Fragment) Class.forName(MainLaunch.class.getPackageName() + "." + args[0]).getConstructor()
                                .newInstance();
                    } catch (Exception e) {
                        Log.LOGGER.warn("Cannot instantiate main fragment", e);
                    }
                }
            }
            if (fragment == null) {
                fragment = new TestFragment();
            }
            if (Platform.get() == Platform.WINDOWS) {
                app.run(fragment, win -> {
                    long hwnd = GLFWNativeWin32.glfwGetWin32Window(win);
                    @SuppressWarnings("resource") var newProc = new WindowsNativeWindowBorder.WndProc(hwnd);
                    cleanup = newProc::destroy;
                });
                //cleanup.run();
            } else {
                app.run(fragment);
            }
        }
        //AudioManager.getInstance().close();
        System.gc();
    }

    public static ResourcesLoader loadExtraResources() {

        ResourcesBuilder rb = new ResourcesBuilder(ns);
        rb.addString(image.test_path_src, "res/test_path_src.png");

        Path currentDir = Path.of("").toAbsolutePath();
        ResourcesProvider provider = rb.build(new DirectoryAssetsProvider(currentDir));

        ResourcesLoader loader = new ResourcesLoader();
        loader.addProvider(provider);
        return loader;
    }
}
