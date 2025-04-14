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

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class VulkanRenderTarget extends GpuRenderTarget {

    // the color buffers, raw ptr
    // null for wrapped render targets
    @SharedPtr
    private VulkanImage[] mColorAttachments;
    // the resolve buffers, raw ptr
    // null for wrapped/single-sampled/non-resolvable render targets
    @SharedPtr
    private VulkanImage[] mResolveAttachments;

    @SharedPtr
    private VulkanImage mDepthStencilAttachment;

    protected VulkanRenderTarget(Context context, int width, int height, int sampleCount, int numColorAttachments) {
        super(context, width, height, sampleCount, numColorAttachments);
    }

    @NonNull
    @Override
    public BackendFormat getBackendFormat() {
        return null;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @NonNull
    @Override
    public BackendRenderTarget getBackendRenderTarget() {
        return null;
    }

    @Nullable
    @Override
    public Image getColorAttachment() {
        return null;
    }

    @Nullable
    @Override
    public Image getColorAttachment(int index) {
        return null;
    }


    @Override
    protected Image @Nullable[] getColorAttachments() {
        return new Image[0];
    }

    @Nullable
    @Override
    public Image getResolveAttachment() {
        return null;
    }

    @Nullable
    @Override
    public Image getResolveAttachment(int index) {
        return null;
    }


    @Override
    protected Image @Nullable[] getResolveAttachments() {
        return new Image[0];
    }

    @Nullable
    @Override
    public Image getDepthStencilAttachment() {
        return null;
    }

    @Override
    public int getDepthBits() {
        return 0;
    }

    @Override
    public int getStencilBits() {
        return 0;
    }

    @Override
    protected boolean canAttachStencil() {
        return false;
    }

    @Override
    protected void onRelease() {

    }
}
