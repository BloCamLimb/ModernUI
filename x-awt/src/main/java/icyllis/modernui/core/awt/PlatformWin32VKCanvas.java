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
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.jawt.JAWTWin32DrawingSurfaceInfo;
import org.lwjgl.system.windows.WinBase;
import org.lwjgl.vulkan.*;

import java.awt.*;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRWin32Surface.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author Kai Burjack
 * @author SWinxy
 */
class PlatformWin32VKCanvas {

    public static final String EXTENSION_NAME = VK_KHR_WIN32_SURFACE_EXTENSION_NAME;

    static long create(Canvas canvas, VkInstance instance) throws AWTException {
        try (AWT awt = new AWT(canvas)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {

                // Get ptr to win32 struct
                JAWTWin32DrawingSurfaceInfo dsiWin = JAWTWin32DrawingSurfaceInfo.create(awt.getPlatformInfo());

                // Gets a handle to the file used to create the calling process (.exe file)
                long handle = WinBase.nGetModuleHandle(MemoryUtil.NULL);

                VkWin32SurfaceCreateInfoKHR sci = VkWin32SurfaceCreateInfoKHR
                        .calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_WIN32_SURFACE_CREATE_INFO_KHR)
                        .hinstance(handle)
                        .hwnd(dsiWin.hwnd());

                LongBuffer pSurface = stack.mallocLong(1);
                int result = vkCreateWin32SurfaceKHR(instance, sci, null, pSurface);

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
                        throw new AWTException("Calling vkCreateWin32SurfaceKHR failed with unknown Vulkan error: " + result);
                }
            }
        }
    }

    static boolean checkSupport(VkPhysicalDevice physicalDevice, int queueFamilyIndex) {
        return vkGetPhysicalDeviceWin32PresentationSupportKHR(physicalDevice, queueFamilyIndex);
    }
}
