/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.vulkan;

import icyllis.akashigi.engine.GBuffer;

//TODO
public final class VkBuffer extends GBuffer {

    public VkBuffer(VkServer server) {
        super(server, 0, 0, 0);
    }

    @Override
    protected void onRelease() {

    }

    @Override
    protected void onDiscard() {

    }

    @Override
    protected void onMap() {

    }

    @Override
    protected void onUnmap() {

    }

    @Override
    protected boolean onUpdateData(long data, int offset, int size) {
        return false;
    }
}
