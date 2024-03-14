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

import org.lwjgl.system.Platform;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.awt.*;

/**
 * Vulkan API. To use the surface,
 * {@link org.lwjgl.vulkan.KHRSurface#VK_KHR_SURFACE_EXTENSION_NAME VK_KHR_SURFACE_EXTENSION_NAME}
 * and {@link #getSurfaceExtensionName()} must be enabled extensions.
 */
public class AWTVK {

    /**
     * Gets the required surface extension for the platform.
     * Also enable {@link org.lwjgl.vulkan.KHRSurface#VK_KHR_SURFACE_EXTENSION_NAME VK_KHR_SURFACE_EXTENSION_NAME}.
     */
    public static String getSurfaceExtensionName() {
        switch (Platform.get()) {
            case WINDOWS:
                return PlatformWin32VKCanvas.EXTENSION_NAME;
            case MACOSX:
                return PlatformMacOSXVKCanvas.EXTENSION_NAME;
            case LINUX:
                return PlatformX11VKCanvas.EXTENSION_NAME;

            default:
                throw new RuntimeException("Platform " + Platform.get() + " not supported.");
        }
    }

    /**
     * Checks if the physical device supports the queue family index.
     *
     * @param physicalDevice   the physical device to check
     * @param queueFamilyIndex the index of the queue family to test
     * @return true if the physical device supports the queue family index
     */
    public static boolean checkSupport(VkPhysicalDevice physicalDevice, int queueFamilyIndex) {
        switch (Platform.get()) {
            case WINDOWS:
                return PlatformWin32VKCanvas.checkSupport(physicalDevice, queueFamilyIndex);
            case MACOSX:
                return PlatformMacOSXVKCanvas.checkSupport(physicalDevice, queueFamilyIndex);
            case LINUX:
                return PlatformX11VKCanvas.checkSupport(physicalDevice, queueFamilyIndex);

            default:
                throw new RuntimeException("Platform " + Platform.get() + " not supported.");
        }
    }

    /**
     * Uses the provided canvas to create a Vulkan surface to draw on.
     *
     * @param canvas   canvas to render onto
     * @param instance vulkan instance
     * @return handle of the surface
     * @throws AWTException if the surface creation fails
     */
    public static long create(Canvas canvas, VkInstance instance) throws AWTException {
        switch (Platform.get()) {
            case WINDOWS:
                return PlatformWin32VKCanvas.create(canvas, instance);
            case MACOSX:
                return PlatformMacOSXVKCanvas.create(canvas, instance);
            case LINUX:
                return PlatformX11VKCanvas.create(canvas, instance);

            default:
                throw new RuntimeException("Platform " + Platform.get() + " not supported.");
        }
    }
}
