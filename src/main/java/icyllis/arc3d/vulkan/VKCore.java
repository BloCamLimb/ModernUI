/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.vulkan;

import icyllis.arc3d.core.*;
import org.lwjgl.system.NativeType;
import org.lwjgl.vulkan.VK11;

import static org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;

/**
 * Provides native interfaces of Vulkan 1.1 core and user-defined utilities.
 */
public final class VKCore extends VK11 {

    /**
     * Runtime assertion against a {@code VkResult} value, throws an exception
     * with a human-readable error message if failed.
     *
     * @param vkResult the {@code VkResult} value
     * @throws AssertionError the VkResult is not VK_SUCCESS
     */
    public static void _CHECK_(@NativeType("VkResult") int vkResult) {
        if (vkResult != VK_SUCCESS) throw new AssertionError(getResultMessage(vkResult));
    }

    /**
     * Runtime assertion against a {@code VkResult} value, throws an exception
     * with a human-readable error message if failed.
     *
     * @param vkResult the {@code VkResult} value
     * @throws AssertionError the VkResult is negative
     */
    public static void _CHECK_ERROR_(@NativeType("VkResult") int vkResult) {
        if (vkResult < VK_SUCCESS) throw new AssertionError(getResultMessage(vkResult));
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

    public static String getPhysicalDeviceTypeName(@NativeType("VkPhysicalDeviceType") int vkPhysicalDeviceType) {
        return switch (vkPhysicalDeviceType) {
            case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU -> "Integrated GPU";
            case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU -> "Discrete GPU";
            case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU -> "Virtual GPU";
            case VK_PHYSICAL_DEVICE_TYPE_CPU -> "CPU";
            default -> "Other";
        };
    }

    /**
     * @return see Color
     */
    public static int vkFormatChannels(@NativeType("VkFormat") int vkFormat) {
        return switch (vkFormat) {
            case VK_FORMAT_R8G8B8A8_UNORM,
                    VK_FORMAT_R16G16B16A16_UNORM,
                    VK_FORMAT_BC1_RGBA_UNORM_BLOCK,
                    VK_FORMAT_R8G8B8A8_SRGB,
                    VK_FORMAT_R4G4B4A4_UNORM_PACK16,
                    VK_FORMAT_B4G4R4A4_UNORM_PACK16,
                    VK_FORMAT_A2R10G10B10_UNORM_PACK32,
                    VK_FORMAT_A2B10G10R10_UNORM_PACK32,
                    VK_FORMAT_R16G16B16A16_SFLOAT,
                    VK_FORMAT_B8G8R8A8_UNORM -> Color.COLOR_CHANNEL_FLAGS_RGBA;
            case VK_FORMAT_R8_UNORM,
                    VK_FORMAT_R16_UNORM,
                    VK_FORMAT_R16_SFLOAT -> Color.COLOR_CHANNEL_FLAG_RED;
            case VK_FORMAT_R5G6B5_UNORM_PACK16,
                    VK_FORMAT_BC1_RGB_UNORM_BLOCK,
                    VK_FORMAT_ETC2_R8G8B8_UNORM_BLOCK,
                    VK_FORMAT_R8G8B8_UNORM -> Color.COLOR_CHANNEL_FLAGS_RGB;
            case VK_FORMAT_R8G8_UNORM,
                    VK_FORMAT_R16G16_SFLOAT,
                    VK_FORMAT_R16G16_UNORM -> Color.COLOR_CHANNEL_FLAGS_RG;
            // either depth/stencil format or unsupported yet
            default -> 0;
        };
    }

    @ColorInfo.CompressionType
    public static int vkFormatCompressionType(@NativeType("VkFormat") int vkFormat) {
        return switch (vkFormat) {
            case VK_FORMAT_ETC2_R8G8B8_UNORM_BLOCK -> ColorInfo.COMPRESSION_ETC2_RGB8_UNORM;
            case VK_FORMAT_BC1_RGB_UNORM_BLOCK -> ColorInfo.COMPRESSION_BC1_RGB8_UNORM;
            case VK_FORMAT_BC1_RGBA_UNORM_BLOCK -> ColorInfo.COMPRESSION_BC1_RGBA8_UNORM;
            default -> ColorInfo.COMPRESSION_NONE;
        };
    }

    /**
     * Currently we are just over estimating this value to be used in gpu size calculations even
     * though the actually size is probably less. We should instead treat planar formats similar
     * to compressed textures that go through their own special query for calculating size.
     * <pre>{@code
     * case VK_FORMAT_G8_B8_R8_3PLANE_420_UNORM: return 3;
     * case VK_FORMAT_G8_B8R8_2PLANE_420_UNORM:  return 3;
     * case VK_FORMAT_S8_UINT:                   return 1;
     * case VK_FORMAT_D24_UNORM_S8_UINT:         return 4;
     * case VK_FORMAT_D32_SFLOAT_S8_UINT:        return 8;
     * }</pre>
     */
    public static int vkFormatBytesPerBlock(@NativeType("VkFormat") int vkFormat) {
        return switch (vkFormat) {
            case VK_FORMAT_R8G8B8A8_UNORM,
                    VK_FORMAT_D24_UNORM_S8_UINT,
                    VK_FORMAT_R16G16_SFLOAT,
                    VK_FORMAT_R16G16_UNORM,
                    VK_FORMAT_R8G8B8A8_SRGB,
                    VK_FORMAT_A2R10G10B10_UNORM_PACK32,
                    VK_FORMAT_A2B10G10R10_UNORM_PACK32,
                    VK_FORMAT_B8G8R8A8_UNORM -> 4;
            case VK_FORMAT_R8_UNORM,
                    VK_FORMAT_S8_UINT -> 1;
            case VK_FORMAT_R5G6B5_UNORM_PACK16,
                    VK_FORMAT_R16_UNORM,
                    VK_FORMAT_R4G4B4A4_UNORM_PACK16,
                    VK_FORMAT_B4G4R4A4_UNORM_PACK16,
                    VK_FORMAT_R8G8_UNORM,
                    VK_FORMAT_R16_SFLOAT -> 2;
            case VK_FORMAT_R16G16B16A16_SFLOAT,
                    VK_FORMAT_D32_SFLOAT_S8_UINT,
                    VK_FORMAT_R16G16B16A16_UNORM,
                    VK_FORMAT_BC1_RGBA_UNORM_BLOCK,
                    VK_FORMAT_BC1_RGB_UNORM_BLOCK,
                    VK_FORMAT_ETC2_R8G8B8_UNORM_BLOCK -> 8;
            case VK_FORMAT_R8G8B8_UNORM,
                    VK_FORMAT_G8_B8R8_2PLANE_420_UNORM,
                    VK_FORMAT_G8_B8_R8_3PLANE_420_UNORM -> 3;
            default -> 0;
        };
    }

    public static int vkFormatDepthBits(@NativeType("VkFormat") int vkFormat) {
        return switch (vkFormat) {
            case VK_FORMAT_D16_UNORM,
                    VK_FORMAT_D16_UNORM_S8_UINT -> 16;
            case VK_FORMAT_D24_UNORM_S8_UINT,
                    VK_FORMAT_X8_D24_UNORM_PACK32 -> 24;
            case VK_FORMAT_D32_SFLOAT,
                    VK_FORMAT_D32_SFLOAT_S8_UINT -> 32;
            default -> 0;
        };
    }

    public static int vkFormatStencilBits(@NativeType("VkFormat") int vkFormat) {
        return switch (vkFormat) {
            case VK_FORMAT_S8_UINT,
                    VK_FORMAT_D16_UNORM_S8_UINT,
                    VK_FORMAT_D24_UNORM_S8_UINT,
                    VK_FORMAT_D32_SFLOAT_S8_UINT -> 8;
            default -> 0;
        };
    }

    public static String vkFormatName(@NativeType("VkFormat") int vkFormat) {
        return switch (vkFormat) {
            case VK_FORMAT_R8G8B8A8_UNORM -> "R8G8B8A8_UNORM";
            case VK_FORMAT_R8_UNORM -> "R8_UNORM";
            case VK_FORMAT_B8G8R8A8_UNORM -> "B8G8R8A8_UNORM";
            case VK_FORMAT_R5G6B5_UNORM_PACK16 -> "R5G6B5_UNORM_PACK16";
            case VK_FORMAT_R16G16B16A16_SFLOAT -> "R16G16B16A16_SFLOAT";
            case VK_FORMAT_R16_SFLOAT -> "R16_SFLOAT";
            case VK_FORMAT_R8G8B8_UNORM -> "R8G8B8_UNORM";
            case VK_FORMAT_R8G8_UNORM -> "R8G8_UNORM";
            case VK_FORMAT_A2B10G10R10_UNORM_PACK32 -> "A2B10G10R10_UNORM_PACK32";
            case VK_FORMAT_A2R10G10B10_UNORM_PACK32 -> "A2R10G10B10_UNORM_PACK32";
            case VK_FORMAT_B4G4R4A4_UNORM_PACK16 -> "B4G4R4A4_UNORM_PACK16";
            case VK_FORMAT_R4G4B4A4_UNORM_PACK16 -> "R4G4B4A4_UNORM_PACK16";
            case VK_FORMAT_R32G32B32A32_SFLOAT -> "R32G32B32A32_SFLOAT";
            case VK_FORMAT_R8G8B8A8_SRGB -> "R8G8B8A8_SRGB";
            case VK_FORMAT_ETC2_R8G8B8_UNORM_BLOCK -> "ETC2_R8G8B8_UNORM_BLOCK";
            case VK_FORMAT_BC1_RGB_UNORM_BLOCK -> "BC1_RGB_UNORM_BLOCK";
            case VK_FORMAT_BC1_RGBA_UNORM_BLOCK -> "BC1_RGBA_UNORM_BLOCK";
            case VK_FORMAT_R16_UNORM -> "R16_UNORM";
            case VK_FORMAT_R16G16_UNORM -> "R16G16_UNORM";
            case VK_FORMAT_R16G16B16A16_UNORM -> "R16G16B16A16_UNORM";
            case VK_FORMAT_R16G16_SFLOAT -> "R16G16_SFLOAT";
            case VK_FORMAT_S8_UINT -> "S8_UINT";
            case VK_FORMAT_D24_UNORM_S8_UINT -> "D24_UNORM_S8_UINT";
            case VK_FORMAT_D32_SFLOAT_S8_UINT -> "D32_SFLOAT_S8_UINT";
            default -> "Unknown";
        };
    }
}
