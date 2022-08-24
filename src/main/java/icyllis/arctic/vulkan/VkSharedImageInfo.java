/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arctic.vulkan;

public final class VkSharedImageInfo {

    // Java's volatile is compatible with C++ atomic load/store with memory_order_seq_cst
    // std::atomic<VkImageLayout> mLayout;
    // std::atomic<uint32_t> mQueueFamilyIndex;
    private volatile int mLayout;
    private volatile int mQueueFamilyIndex;

    public VkSharedImageInfo(VkImageInfo info) {
        this(info.mImageLayout, info.mCurrentQueueFamily);
    }

    public VkSharedImageInfo(int layout, int queueFamilyIndex) {
        mLayout = layout;
        mQueueFamilyIndex = queueFamilyIndex;
    }

    public void setImageLayout(int layout) {
        // Defaulting to use std::memory_order_seq_cst
        // mLayout.store(layout);
        mLayout = layout;
    }

    public int getImageLayout() {
        // Defaulting to use std::memory_order_seq_cst
        // return mLayout.load();
        return mLayout;
    }

    public void setQueueFamilyIndex(int queueFamilyIndex) {
        // Defaulting to use std::memory_order_seq_cst
        // mQueueFamilyIndex.store(queueFamilyIndex);
        mQueueFamilyIndex = queueFamilyIndex;
    }

    public int getQueueFamilyIndex() {
        // Defaulting to use std::memory_order_seq_cst
        // return mQueueFamilyIndex.load();
        return mQueueFamilyIndex;
    }
}
