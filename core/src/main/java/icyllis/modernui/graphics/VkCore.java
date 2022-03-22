/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.vulkan.VK11;

import static org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_NATIVE_WINDOW_IN_USE_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_SURFACE_LOST_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;

public final class VkCore extends VK11 {

    public static final Marker MARKER = MarkerManager.getMarker("Vulkan");

    /**
     * Runtime assertion against a {@code VkResult} value, throws an exception
     * with a human-readable error message if failed.
     *
     * @param result the {@code VkResult} value
     * @throws AssertionError the VkResult is not VK_SUCCESS
     */
    public static void CHECK(int result) {
        if (result != VK_SUCCESS) throw new AssertionError(getResultMessage(result));
    }

    /**
     * Runtime assertion against a {@code VkResult} value, throws an exception
     * with a human-readable error message if failed.
     *
     * @param result the {@code VkResult} value
     * @throws AssertionError the VkResult is negative
     */
    public static void CHECK_ERROR(int result) {
        if (result < VK_SUCCESS) throw new AssertionError(getResultMessage(result));
    }

    /**
     * Translates a Vulkan {@code VkResult} value to a String describing the result.
     *
     * @param result the {@code VkResult} value
     * @return the result description
     */
    public static String getResultMessage(int result) {
        return switch (result) {
            // Success codes
            case VK_SUCCESS -> "Command successfully completed.";
            case VK_NOT_READY -> "A fence or query has not yet completed.";
            case VK_TIMEOUT -> "A wait operation has not completed in the specified time.";
            case VK_EVENT_SET -> "An event is signaled.";
            case VK_EVENT_RESET -> "An event is unsignaled.";
            case VK_INCOMPLETE -> "A return array was too small for the result.";
            case VK_SUBOPTIMAL_KHR -> "A swap-chain no longer matches the surface properties exactly, but can still " +
                    "be used to present to the surface successfully.";

            // Error codes
            case VK_ERROR_OUT_OF_HOST_MEMORY -> "A host memory allocation has failed.";
            case VK_ERROR_OUT_OF_DEVICE_MEMORY -> "A device memory allocation has failed.";
            case VK_ERROR_INITIALIZATION_FAILED -> "Initialization of an object could not be completed for " +
                    "implementation-specific reasons.";
            case VK_ERROR_DEVICE_LOST -> "The logical or physical device has been lost.";
            case VK_ERROR_MEMORY_MAP_FAILED -> "Mapping of a memory object has failed.";
            case VK_ERROR_LAYER_NOT_PRESENT -> "A requested layer is not present or could not be loaded.";
            case VK_ERROR_EXTENSION_NOT_PRESENT -> "A requested extension is not supported.";
            case VK_ERROR_FEATURE_NOT_PRESENT -> "A requested feature is not supported.";
            case VK_ERROR_INCOMPATIBLE_DRIVER -> "The requested version of Vulkan is not supported by the driver or " +
                    "is otherwise incompatible for implementation-specific reasons.";
            case VK_ERROR_TOO_MANY_OBJECTS -> "Too many objects of the type have already been created.";
            case VK_ERROR_FORMAT_NOT_SUPPORTED -> "A requested format is not supported on this device.";
            case VK_ERROR_SURFACE_LOST_KHR -> "A surface is no longer available.";
            case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR -> "The requested window is already connected to a VkSurfaceKHR, " +
                    "or to some other non-Vulkan API.";
            case VK_ERROR_OUT_OF_DATE_KHR -> "A surface has changed in such a way that it is no longer compatible " +
                    "with the swap-chain, and further presentation requests using the swap-chain will fail. " +
                    "Applications must query the new surface properties and recreate their swap-chain if they wish" +
                    "to continue presenting to the surface.";
            case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR -> "The display used by a swap-chain does not use the same " +
                    "presentable image layout, or is incompatible in a way that prevents sharing an image.";
            case VK_ERROR_VALIDATION_FAILED_EXT -> "A validation layer found an error.";
            default -> String.format("%s [%d]", "Unknown", result);
        };
    }

    public static String getPhysicalDeviceTypeName(int type) {
        return switch (type) {
            case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU -> "Integrated GPU";
            case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU -> "Discrete GPU";
            case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU -> "Virtual GPU";
            case VK_PHYSICAL_DEVICE_TYPE_CPU -> "CPU";
            default -> "Other";
        };
    }
}
