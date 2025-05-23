/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.engine.*;
import org.jspecify.annotations.Nullable;

public class VulkanResourceProvider extends ResourceProvider {

    private final VulkanDevice mDevice;

    protected VulkanResourceProvider(VulkanDevice device, Context context,
                                     long maxResourceBudget) {
        super(device, context, maxResourceBudget);
        mDevice = device;
    }

    @Nullable
    @Override
    protected Image onCreateNewImage(ImageDesc desc) {
        if (!(desc instanceof VulkanImageDesc vulkanImageDesc)) {
            return null;
        }
        return VulkanImage.make(mContext, vulkanImageDesc);
    }

    @Nullable
    @Override
    protected Sampler createSampler(SamplerDesc desc) {
        return null;
    }

    @Nullable
    @Override
    protected Buffer onCreateNewBuffer(long size, int usage) {
        return null;
    }
}
