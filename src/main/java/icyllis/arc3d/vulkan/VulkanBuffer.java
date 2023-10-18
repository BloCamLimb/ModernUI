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

import icyllis.arc3d.engine.GpuBuffer;

//TODO
public final class VulkanBuffer extends GpuBuffer {

    public VulkanBuffer(VulkanDevice device) {
        super(device, 0, 0);
    }

    @Override
    protected void onRelease() {

    }

    @Override
    protected void onDiscard() {

    }

    @Override
    protected long onLock(int mode, int offset, int size) {
        return 0;
    }

    @Override
    protected void onUnlock(int mode, int offset, int size) {

    }

    @Override
    public boolean isLocked() {
        return false;
    }

    @Override
    public long getLockedBuffer() {
        return 0;
    }

    @Override
    protected boolean onUpdateData(int offset, int size, long data) {
        return false;
    }
}
