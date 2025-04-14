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

import icyllis.arc3d.engine.BackendFormat;
import icyllis.arc3d.engine.BackendImage;
import icyllis.arc3d.engine.Engine.BackendApi;
import org.jspecify.annotations.NonNull;

import static org.lwjgl.vulkan.VK11.*;

/**
 * When importing external memory,
 * {@link #mMemoryHandle} is POSIX file descriptor or Win32 NT handle (though <code>HANDLE</code> is defined
 * as <code>void*</code>, we can safely truncate it because Win32 handles are 32-bit significant).
 * If it is an NT handle, it must be released manually by the memory exporter (e.g. Vulkan).
 */
//TODO
public final class VulkanBackendImage extends BackendImage {

    // We don't know if the backend texture is made renderable or not, so we default the usage flags
    // to include color attachment as well.
    private static final int DEFAULT_USAGE_FLAGS = VK_IMAGE_USAGE_TRANSFER_DST_BIT |
            VK_IMAGE_USAGE_TRANSFER_SRC_BIT |
            VK_IMAGE_USAGE_SAMPLED_BIT |
            VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

    private long mImage;
    public int mLevelCount = 0;
    public long mMemoryHandle = -1;
    private VulkanAllocation mAlloc;
    private final VulkanImageDesc mInfo;
    final VulkanImageMutableState mState;

    private final BackendFormat mBackendFormat;

    // The VkImageInfo can NOT be modified anymore.
    public VulkanBackendImage(int width, int height, VulkanImageDesc desc) {
        //TODO disallow VK_IMAGE_TILING_DRM_FORMAT_MODIFIER_EXT
        this(width, height, desc, null, VkBackendFormat.make(desc.mFormat));
    }

    VulkanBackendImage(int width, int height, VulkanImageDesc desc,
                       VulkanImageMutableState state, BackendFormat backendFormat) {
        super(desc, state);
        mInfo = desc;
        mState = state;
        mBackendFormat = backendFormat;
    }

    @Override
    public int getBackend() {
        return BackendApi.kVulkan;
    }

    @Override
    public boolean isExternal() {
        return mBackendFormat.isExternal();
    }

    /*
     * Copies a snapshot of the {@link VulkanImageDesc} struct into the passed in pointer.
     * This snapshot will set the {@link VulkanImageDesc#mImageLayout} to the current layout
     * state.
     */
    /*public void getVulkanImageInfo(VulkanImageDesc desc) {
        desc.set(mInfo);
        desc.mImageLayout = mState.getImageLayout();
        desc.mCurrentQueueFamily = mState.getQueueFamilyIndex();
    }*/

    @Override
    public void setVkImageLayout(int layout) {
        mState.setImageLayout(layout);
    }

    @Override
    public void setVkQueueFamilyIndex(int queueFamilyIndex) {
        mState.setQueueFamilyIndex(queueFamilyIndex);
    }

    @NonNull
    @Override
    public BackendFormat getBackendFormat() {
        return mBackendFormat;
    }

    @Override
    public boolean isProtected() {
        return mInfo.isProtected();
    }

    @Override
    public boolean isSameImage(BackendImage image) {
        if (image instanceof VulkanBackendImage t) {
            return mImage == t.mImage;
        }
        return false;
    }
}
