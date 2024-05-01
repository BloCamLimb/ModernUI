/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.engine;

import java.nio.ByteBuffer;

/**
 * Manage streaming vertex buffers, for vertex/instance data are updated per draw call.
 * <p>
 * This prefers to create a large ring buffer that is host visible and device visible.
 * If not available, creates a large staging buffer and a device local buffer.
 */
//TODO
public class StreamBufferManager {

    public ByteBuffer getVertexWriter(int requiredBytes, BufferViewInfo info) {
        return null;
    }

    public void putBackVertexBytes(int unusedBytes) {

    }
}
