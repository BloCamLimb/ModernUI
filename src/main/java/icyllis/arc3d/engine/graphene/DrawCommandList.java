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

package icyllis.arc3d.engine.graphene;

import icyllis.arc3d.engine.BufferViewInfo;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.nio.ByteBuffer;

/**
 * The list that holds commands of a draw pass, and can be played on
 * {@link icyllis.arc3d.engine.CommandBuffer}.
 */
public class DrawCommandList {

    /**
     * <pre><code>
     * struct BindGraphicsPipeline {
     *     int pipelineIndex;
     * }</code></pre>
     */
    public static final int CMD_BIND_GRAPHICS_PIPELINE = 0;
    public static final int CMD_DRAW = 1;
    public static final int CMD_DRAW_INDEXED = 2;
    public static final int CMD_DRAW_INSTANCED = 3;
    public static final int CMD_DRAW_INDEXED_INSTANCED = 4;
    public static final int CMD_BIND_BUFFERS = 5;

    /**
     * The heap buffer that holds all primitive data.
     */
    public ByteBuffer mPrimitives = ByteBuffer.allocate(512);
    public final ObjectArrayList<Object> mPointers = new ObjectArrayList<>();

    private void grow(int minCapacity) {
        if (minCapacity > mPrimitives.capacity()) {
            int oldCapacity = mPrimitives.capacity();
            int newCapacity = Math.max(minCapacity, oldCapacity + (oldCapacity >> 1));
            mPrimitives = ByteBuffer.allocate(newCapacity)
                    .put(mPrimitives.flip());
        }
    }

    public void bindGraphicsPipeline(int pipelineIndex) {
        grow(mPrimitives.position() + 8);
        mPrimitives.putInt(CMD_BIND_GRAPHICS_PIPELINE)
                .putInt(pipelineIndex);
    }

    public void draw(int vertexCount, int baseVertex) {
        grow(mPrimitives.position() + 12);
        mPrimitives.putInt(CMD_DRAW)
                .putInt(vertexCount)
                .putInt(baseVertex);
    }

    public final void drawIndexed(int indexCount, int baseIndex,
                                  int baseVertex) {
        grow(mPrimitives.position() + 16);
        mPrimitives.putInt(CMD_DRAW_INDEXED)
                .putInt(indexCount)
                .putInt(baseIndex)
                .putInt(baseVertex);
    }

    public final void drawInstanced(int instanceCount, int baseInstance,
                                    int vertexCount, int baseVertex) {
        grow(mPrimitives.position() + 20);
        mPrimitives.putInt(CMD_DRAW_INSTANCED)
                .putInt(instanceCount)
                .putInt(baseInstance)
                .putInt(vertexCount)
                .putInt(baseVertex);
    }

    public final void drawIndexedInstanced(int indexCount, int baseIndex,
                                           int instanceCount, int baseInstance,
                                           int baseVertex) {
        grow(mPrimitives.position() + 24);
        mPrimitives.putInt(CMD_DRAW_INDEXED_INSTANCED)
                .putInt(indexCount)
                .putInt(baseIndex)
                .putInt(instanceCount)
                .putInt(baseInstance)
                .putInt(baseVertex);
    }

    public final void bindBuffers(BufferViewInfo vertexBufferInfo,
                                  BufferViewInfo instanceBufferInfo,
                                  BufferViewInfo indexBufferInfo,
                                  int indexType) {
        grow(mPrimitives.position() + 32);
        mPrimitives.putInt(CMD_BIND_BUFFERS)
                .putLong(vertexBufferInfo.mOffset)
                .putLong(instanceBufferInfo.mOffset)
                .putLong(indexBufferInfo.mOffset)
                .putInt(indexType);
        mPointers.add(vertexBufferInfo.mBuffer);
        mPointers.add(instanceBufferInfo.mBuffer);
        mPointers.add(indexBufferInfo.mBuffer);
    }
}
