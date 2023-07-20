/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.vulkan;

public final class VulkanSharedImageInfo {

    private volatile int mLayout;
    private volatile int mQueueFamilyIndex;

    public VulkanSharedImageInfo(VulkanImageInfo info) {
        this(info.mImageLayout, info.mCurrentQueueFamily);
    }

    public VulkanSharedImageInfo(int layout, int queueFamilyIndex) {
        mLayout = layout;
        mQueueFamilyIndex = queueFamilyIndex;
    }

    public void setImageLayout(int layout) {
        mLayout = layout;
    }

    public int getImageLayout() {
        return mLayout;
    }

    public void setQueueFamilyIndex(int queueFamilyIndex) {
        mQueueFamilyIndex = queueFamilyIndex;
    }

    public int getQueueFamilyIndex() {
        return mQueueFamilyIndex;
    }
}
