/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.*;
import org.lwjgl.vulkan.VkImageCreateInfo;
import icyllis.arc3d.engine.Engine.ImageType;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Represents Vulkan images, can be used as textures or attachments.
 */
public final class VulkanImage extends Image {

    private final long mImage;
    private final VulkanAllocation mMemoryAlloc;

    public VulkanImage(Context context,
                       VulkanImageDesc desc,
                       VulkanImageMutableState mutableState,
                       long image, VulkanAllocation memoryAlloc,
                       boolean wrapped) {
        super(context, wrapped, desc, mutableState);
        mImage = image;
        mMemoryAlloc = memoryAlloc;
    }

    public record CreatedImageInfo(long image,
                                   VulkanAllocation memoryAlloc,
                                   VulkanImageMutableState mutableState) {
    }

    /**
     * Create Vulkan image and allocate its device memory.
     *
     * @return created image or null if failed
     */
    @Nullable
    public static CreatedImageInfo create(VulkanDevice device,
                                          VulkanImageDesc desc) {

        boolean isLinear = desc.mImageTiling == VK_IMAGE_TILING_LINEAR;
        int initialLayout = isLinear
                ? VK_IMAGE_LAYOUT_PREINITIALIZED
                : VK_IMAGE_LAYOUT_UNDEFINED;

        int vkSamples = VKUtil.toVkSampleCount(desc.getSampleCount());
        if (vkSamples == 0) {
            device.getLogger().error("Failed to create VulkanImage: unsupported number of samples {}",
                    desc.getSampleCount());
            return null;
        }

        assert (!isLinear || vkSamples == VK_SAMPLE_COUNT_1_BIT);

        try (var stack = MemoryStack.stackPush()) {
            var pCreateInfo = VkImageCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(MemoryUtil.NULL)
                    .flags(desc.mVkFlags)
                    .imageType(desc.mVkImageType)
                    .format(desc.mFormat);
            pCreateInfo.extent().set(desc.getWidth(), desc.getHeight(), desc.getDepth());
            int layers = (desc.getImageType() == ImageType.kCube || desc.getImageType() == ImageType.kCubeArray)
                    ? 6
                    : 1;
            pCreateInfo
                    .mipLevels(desc.getMipLevelCount())
                    .arrayLayers(desc.getArraySize() * layers)
                    .samples(vkSamples)
                    .tiling(desc.mImageTiling)
                    .usage(desc.mImageUsageFlags)
                    .sharingMode(desc.mSharingMode)
                    .queueFamilyIndexCount(0)
                    .pQueueFamilyIndices(null)
                    .initialLayout(initialLayout);
            var pImage = stack.mallocLong(1);
            var result = vkCreateImage(
                    device.vkDevice(),
                    pCreateInfo,
                    null,
                    pImage
            );
            device.checkResult(result);
            if (result != VK_SUCCESS) {
                device.getLogger().error("Failed to create VulkanImage: {}",
                        VKUtil.getResultMessage(result));
                return null;
            }

            var allocator = device.getMemoryAllocator();
            var allocInfo = new VulkanAllocation();

            boolean useLazyAllocation =
                    (desc.mImageUsageFlags & VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT) != 0;

            int allocFlags = 0;
            long size = DataUtils.computeSize(desc);
            if (!useLazyAllocation &&
                    (desc.isRenderable() || size >= 12*1024*1024)) {
                // prefer dedicated allocation for render target or >= 12MB
                allocFlags |= VulkanMemoryAllocator.kDedicatedAllocation_AllocFlag;
            }
            if (useLazyAllocation) {
                allocFlags |= VulkanMemoryAllocator.kLazyAllocation_AllocFlag;
            }

            if (!allocator.allocateImageMemory(
                device, pImage.get(0), allocFlags, allocInfo
            )) {
                vkDestroyImage(device.vkDevice(), pImage.get(0), null);
                device.getLogger().error("Failed to create VulkanImage: cannot allocate {} bytes from device",
                        size);
                return null;
            }

            if (useLazyAllocation &&
                    (allocInfo.mMemoryFlags & VulkanAllocation.kLazilyAllocated_Flag) == 0) {
                allocator.freeMemory(allocInfo);
                vkDestroyImage(device.vkDevice(), pImage.get(0), null);
                device.getLogger().error("Failed to create VulkanImage: cannot allocate lazy memory when requested");
                return null;
            }

            result = vkBindImageMemory(
                    device.vkDevice(),
                    pImage.get(0),
                    allocInfo.mMemory,
                    allocInfo.mOffset
            );
            device.checkResult(result);
            if (result != VK_SUCCESS) {
                allocator.freeMemory(allocInfo);
                vkDestroyImage(device.vkDevice(), pImage.get(0), null);
                device.getLogger().error("Failed to bind image memory: {}",
                        VKUtil.getResultMessage(result));
                return null;
            }

            return new CreatedImageInfo(
                    pImage.get(0),
                    allocInfo,
                    new VulkanImageMutableState(initialLayout, VK_QUEUE_FAMILY_IGNORED)
            );
        }
    }

    @Nullable
    @SharedPtr
    public static VulkanImage make(@NonNull Context context,
                                   @NonNull VulkanImageDesc desc) {
        VulkanDevice device = (VulkanDevice) context.getDevice();
        CreatedImageInfo imageInfo = create(device, desc);
        if (imageInfo == null) {
            return null;
        }

        return new VulkanImage(context, desc,
                imageInfo.mutableState,
                imageInfo.image,
                imageInfo.memoryAlloc,
                false);
    }

    @NativeType("VkImage")
    public long vkImage() {
        return mImage;
    }

    @Override
    protected void onRelease() {
        if (!isWrapped()) {
            VulkanDevice device = (VulkanDevice) getDevice();
            device.getMemoryAllocator().freeMemory(mMemoryAlloc);
            vkDestroyImage(device.vkDevice(), mImage, null);
        }
    }

    @Override
    public String toString() {
        return "VulkanImage{" +
                "mDesc=" + getDesc() +
                ", mImage=0x" + Long.toHexString(mImage) +
                ", mMemoryAlloc=" + mMemoryAlloc +
                ", mDestroyed=" + isDestroyed() +
                ", mLabel=" + getLabel() +
                ", mMemorySize=" + getMemorySize() +
                '}';
    }

    public static final class ResourceKey implements IResourceKey {

        private final VulkanImageDesc mDesc;

        public ResourceKey(VulkanImageDesc desc) {
            mDesc = desc;
        }

        @Override
        public IResourceKey copy() {
            return new ResourceKey(mDesc);
        }

        @Override
        public int hashCode() {
            return mDesc.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ResourceKey that = (ResourceKey) o;

            return mDesc.equals(that.mDesc);
        }
    }
}
