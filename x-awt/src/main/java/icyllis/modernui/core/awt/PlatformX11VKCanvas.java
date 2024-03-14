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

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.jawt.JAWTX11DrawingSurfaceInfo;
import org.lwjgl.vulkan.*;

import java.awt.*;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRXlibSurface.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author Guenther
 * @author SWinxy
 */
class PlatformX11VKCanvas {

    public static final String EXTENSION_NAME = VK_KHR_XLIB_SURFACE_EXTENSION_NAME;

    static long create(Canvas canvas, VkInstance instance) throws AWTException {
        try (AWT awt = new AWT(canvas)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                JAWTX11DrawingSurfaceInfo dsiX11 = JAWTX11DrawingSurfaceInfo.create(awt.getPlatformInfo());

                VkXlibSurfaceCreateInfoKHR pCreateInfo = VkXlibSurfaceCreateInfoKHR
                        .calloc(stack)
                        .dpy(dsiX11.display())
                        .window(dsiX11.drawable());

                LongBuffer pSurface = stack.mallocLong(1);
                int result = vkCreateXlibSurfaceKHR(instance, pCreateInfo, null, pSurface);

                switch (result) {
                    case VK_SUCCESS:
                        return pSurface.get(0);

                    // Possible VkResult codes returned
                    case VK_ERROR_OUT_OF_HOST_MEMORY:
                        throw new AWTException("Failed to create a Vulkan surface: a host memory allocation has " +
                                "failed.");
                    case VK_ERROR_OUT_OF_DEVICE_MEMORY:
                        throw new AWTException("Failed to create a Vulkan surface: a device memory allocation has " +
                                "failed.");

                        // Error unknown to the implementation
                    case VK_ERROR_UNKNOWN:
                        throw new AWTException("An unknown error has occurred;" +
                                " either the application has provided invalid input, or an implementation failure has" +
                                " occurred.");

                        // Unknown error not included in this list
                    default:
                        throw new AWTException("Calling vkCreateXlibSurfaceKHR failed with unknown Vulkan error: " + result);
                }
            }
        }
    }

    static boolean checkSupport(VkPhysicalDevice physicalDevice, int queueFamilyIndex) {
        return vkGetPhysicalDeviceXlibPresentationSupportKHR(physicalDevice, queueFamilyIndex, 0, 0);
    }
}
