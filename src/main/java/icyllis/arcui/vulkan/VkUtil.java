/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.vulkan;

import icyllis.arcui.core.Color;
import org.lwjgl.system.NativeType;

import static org.lwjgl.vulkan.VK11.*;

public final class VkUtil {

    /**
     * @param vkFormat see VK11
     * @return see Color
     */
    public static int getVkFormatChannels(@NativeType("VkFormat") int vkFormat) {
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
                    VK_FORMAT_B8G8R8A8_UNORM -> Color.RGBA_CHANNEL_FLAGS;
            case VK_FORMAT_R8_UNORM,
                    VK_FORMAT_R16_UNORM,
                    VK_FORMAT_R16_SFLOAT -> Color.RED_CHANNEL_FLAG;
            case VK_FORMAT_R5G6B5_UNORM_PACK16,
                    VK_FORMAT_BC1_RGB_UNORM_BLOCK,
                    VK_FORMAT_ETC2_R8G8B8_UNORM_BLOCK,
                    VK_FORMAT_R8G8B8_UNORM -> Color.RGB_CHANNEL_FLAGS;
            case VK_FORMAT_R8G8_UNORM,
                    VK_FORMAT_R16G16_SFLOAT,
                    VK_FORMAT_R16G16_UNORM -> Color.RG_CHANNEL_FLAGS;
            // either depth/stencil format or unsupported yet
            default -> 0;
        };
    }

    private VkUtil() {
    }
}
