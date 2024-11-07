/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.vulkan;

import icyllis.arc3d.engine.Engine.ImageType;
import icyllis.arc3d.engine.ManagedResource;
import icyllis.arc3d.engine.Swizzle;
import org.lwjgl.system.*;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Represents Vulkan image views, managed by {@link VulkanImage}.
 */
public final class VulkanImageView extends ManagedResource {

    private final long mImageView;
    private final int mBaseMipLevel;
    private final int mLevelCount;
    private final int mBaseArrayLayer;
    private final int mLayerCount;

    VulkanImageView(VulkanDevice device, long imageView,
                    int baseMipLevel, int levelCount,
                    int baseArrayLayer, int layerCount) {
        super(device);
        mImageView = imageView;
        mBaseMipLevel = baseMipLevel;
        mLevelCount = levelCount;
        mBaseArrayLayer = baseArrayLayer;
        mLayerCount = layerCount;
    }

    private static int get_aspect_mask(int format) {
        return switch (format) {
            case VK_FORMAT_S8_UINT -> VK_IMAGE_ASPECT_STENCIL_BIT;
            case VK_FORMAT_D16_UNORM,
                 VK_FORMAT_X8_D24_UNORM_PACK32,
                 VK_FORMAT_D32_SFLOAT -> VK_IMAGE_ASPECT_DEPTH_BIT;
            case VK_FORMAT_D16_UNORM_S8_UINT,
                 VK_FORMAT_D24_UNORM_S8_UINT,
                 VK_FORMAT_D32_SFLOAT_S8_UINT -> VK_IMAGE_ASPECT_STENCIL_BIT | VK_IMAGE_ASPECT_DEPTH_BIT;
            default -> VK_IMAGE_ASPECT_COLOR_BIT;
        };
    }

    private static int get_view_type(int imageType) {
        return switch (imageType) {
            case ImageType.k2D -> VK_IMAGE_VIEW_TYPE_2D;
            case ImageType.k2DArray -> VK_IMAGE_VIEW_TYPE_2D_ARRAY;
            case ImageType.kCube -> VK_IMAGE_VIEW_TYPE_CUBE;
            case ImageType.kCubeArray -> VK_IMAGE_VIEW_TYPE_CUBE_ARRAY;
            case ImageType.k3D -> VK_IMAGE_VIEW_TYPE_3D;
            default -> {
                assert false : imageType;
                yield VK_IMAGE_VIEW_TYPE_1D;
            }
        };
    }

    private static int get_swizzle(int index) {
        return switch (index) {
            case Swizzle.COMPONENT_R    -> VK_COMPONENT_SWIZZLE_R;
            case Swizzle.COMPONENT_G    -> VK_COMPONENT_SWIZZLE_G;
            case Swizzle.COMPONENT_B    -> VK_COMPONENT_SWIZZLE_B;
            case Swizzle.COMPONENT_A    -> VK_COMPONENT_SWIZZLE_A;
            case Swizzle.COMPONENT_ZERO -> VK_COMPONENT_SWIZZLE_ZERO;
            case Swizzle.COMPONENT_ONE  -> VK_COMPONENT_SWIZZLE_ONE;
            default -> {
                assert false : index;
                yield VK_COMPONENT_SWIZZLE_IDENTITY;
            }
        };
    }

    /**
     * Create a shader resource view as shader input, for texture sampling.
     *
     * @param imageType see {@link ImageType}
     * @param swizzle   see {@link Swizzle}
     */
    @Nullable
    public static VulkanImageView makeTexture(@Nonnull VulkanDevice device,
                                              long image,
                                              int imageType,
                                              @NativeType("VkFormat") int format,
                                              short swizzle,
                                              int mipLevelCount,
                                              int layerCount) {
        try (var stack = MemoryStack.stackPush()) {
            var pCreateInfo = VkImageViewCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(MemoryUtil.NULL)
                    .flags(0)
                    .image(image)
                    .viewType(get_view_type(imageType))
                    .format(format);
            if (swizzle != Swizzle.RGBA) {
                pCreateInfo.components().set(
                        get_swizzle(Swizzle.getR(swizzle)),
                        get_swizzle(Swizzle.getG(swizzle)),
                        get_swizzle(Swizzle.getB(swizzle)),
                        get_swizzle(Swizzle.getA(swizzle))
                );
            } else {
                pCreateInfo.components().set(
                        VK_COMPONENT_SWIZZLE_IDENTITY,
                        VK_COMPONENT_SWIZZLE_IDENTITY,
                        VK_COMPONENT_SWIZZLE_IDENTITY,
                        VK_COMPONENT_SWIZZLE_IDENTITY
                );
            }
            pCreateInfo.subresourceRange()
                    .aspectMask(get_aspect_mask(format))
                    .baseMipLevel(0)
                    .levelCount(mipLevelCount)
                    .baseArrayLayer(0)
                    .layerCount(layerCount);
            var pView = stack.mallocLong(1);
            var result = vkCreateImageView(
                    device.vkDevice(),
                    pCreateInfo,
                    null,
                    pView
            );
            device.checkResult(result);
            if (result != VK_SUCCESS) {
                device.getLogger().error("Failed to create shader resource view: {}",
                        VKUtil.getResultMessage(result));
                return null;
            }
            return new VulkanImageView(device, pView.get(0),
                    0, mipLevelCount, 0, layerCount);
        }
    }

    /**
     * Create a render target view as attachment.
     */
    // currently there's no multi view render target
    @Nullable
    public static VulkanImageView makeAttachment(@Nonnull VulkanDevice device,
                                                 long image,
                                                 int imageType,
                                                 @NativeType("VkFormat") int format,
                                                 int mipLevel,
                                                 int arraySlice) {
        try (var stack = MemoryStack.stackPush()) {
            var pCreateInfo = VkImageViewCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(MemoryUtil.NULL)
                    .flags(0)
                    .image(image)
                    .viewType(get_view_type(imageType))
                    .format(format);
            pCreateInfo.components().set(
                    VK_COMPONENT_SWIZZLE_IDENTITY,
                    VK_COMPONENT_SWIZZLE_IDENTITY,
                    VK_COMPONENT_SWIZZLE_IDENTITY,
                    VK_COMPONENT_SWIZZLE_IDENTITY
            );
            pCreateInfo.subresourceRange()
                    .aspectMask(get_aspect_mask(format))
                    .baseMipLevel(mipLevel)
                    .levelCount(1)
                    .baseArrayLayer(arraySlice)
                    .layerCount(1);
            var pView = stack.mallocLong(1);
            var result = vkCreateImageView(
                    device.vkDevice(),
                    pCreateInfo,
                    null,
                    pView
            );
            device.checkResult(result);
            if (result != VK_SUCCESS) {
                device.getLogger().error("Failed to create render target view: {}",
                        VKUtil.getResultMessage(result));
                return null;
            }
            return new VulkanImageView(device, pView.get(0),
                    mipLevel, 1, arraySlice, 1);
        }
    }

    @NativeType("VkImageView")
    public long vkImageView() {
        return mImageView;
    }

    public int getBaseMipLevel() {
        return mBaseMipLevel;
    }

    public int getLevelCount() {
        return mLevelCount;
    }

    public int getBaseArrayLayer() {
        return mBaseArrayLayer;
    }

    public int getLayerCount() {
        return mLayerCount;
    }

    @Override
    protected void deallocate() {
        VulkanDevice device = (VulkanDevice) getDevice();
        vkDestroyImageView(device.vkDevice(), mImageView, null);
    }
}
