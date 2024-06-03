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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.Rect2ic;
import icyllis.arc3d.engine.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.PrintWriter;
import java.nio.ByteBuffer;

/**
 * The list that holds commands of a draw pass, and can be replayed on
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
    /**
     * <pre><code>
     * struct Draw {
     *     int vertexCount;
     *     int baseVertex;
     * }</code></pre>
     */
    public static final int CMD_DRAW = 1;
    public static final int CMD_DRAW_INDEXED = 2;
    public static final int CMD_DRAW_INSTANCED = 3;
    public static final int CMD_DRAW_INDEXED_INSTANCED = 4;
    public static final int CMD_BIND_INDEX_BUFFER = 5;
    public static final int CMD_BIND_VERTEX_BUFFER = 6;
    public static final int CMD_SET_SCISSOR = 7;

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

    public final void bindIndexBuffer(int indexType,
                                      BufferViewInfo indexBufferInfo) {
        grow(mPrimitives.position() + 16);
        mPrimitives.putInt(CMD_BIND_INDEX_BUFFER)
                .putInt(indexType)
                .putLong(indexBufferInfo.mOffset);
        mPointers.add(indexBufferInfo.mBuffer);
    }

    public final void bindVertexBuffer(int binding,
                                       BufferViewInfo vertexBufferInfo) {
        grow(mPrimitives.position() + 16);
        mPrimitives.putInt(CMD_BIND_VERTEX_BUFFER)
                .putInt(binding)
                .putLong(vertexBufferInfo.mOffset);
        mPointers.add(vertexBufferInfo.mBuffer);
    }

    public final void setScissor(Rect2ic scissor) {
        grow(mPrimitives.position() + 20);
        mPrimitives.putInt(CMD_SET_SCISSOR)
                .putInt(scissor.left())
                .putInt(scissor.top())
                .putInt(scissor.right())
                .putInt(scissor.bottom());
    }

    public final void finish() {
        mPrimitives.flip();
    }

    public boolean execute(CommandBuffer commandBuffer) {
        var p = mPrimitives;
        var oa = mPointers.elements();
        int oi = 0;
        while (p.hasRemaining()) {
            switch (p.getInt()) {
                case DrawCommandList.CMD_BIND_GRAPHICS_PIPELINE -> {
                    p.getInt();
                }
                case DrawCommandList.CMD_DRAW -> {
                    int vertexCount = p.getInt();
                    int baseVertex = p.getInt();
                    commandBuffer.draw(vertexCount, baseVertex);
                }
                case DrawCommandList.CMD_DRAW_INDEXED -> {
                    int indexCount = p.getInt();
                    int baseIndex = p.getInt();
                    int baseVertex = p.getInt();
                    commandBuffer.drawIndexed(indexCount, baseIndex, baseVertex);
                }
                case DrawCommandList.CMD_DRAW_INSTANCED -> {
                    int instanceCount = p.getInt();
                    int baseInstance = p.getInt();
                    int vertexCount = p.getInt();
                    int baseVertex = p.getInt();
                    commandBuffer.drawInstanced(instanceCount, baseInstance, vertexCount, baseVertex);
                }
                case DrawCommandList.CMD_DRAW_INDEXED_INSTANCED -> {
                    int indexCount = p.getInt();
                    int baseIndex = p.getInt();
                    int instanceCount = p.getInt();
                    int baseInstance = p.getInt();
                    int baseVertex = p.getInt();
                    commandBuffer.drawIndexedInstanced(indexCount, baseIndex, instanceCount, baseInstance, baseVertex);
                }
                case DrawCommandList.CMD_BIND_INDEX_BUFFER -> {
                    int indexType = p.getInt();
                    long offset = p.getLong();
                    commandBuffer.bindIndexBuffer(indexType, (Buffer) oa[oi++], offset);
                }
                case DrawCommandList.CMD_BIND_VERTEX_BUFFER -> {
                    int binding = p.getInt();
                    long offset = p.getLong();
                    commandBuffer.bindVertexBuffer(binding, (Buffer) oa[oi++], offset);
                }
            }
        }
        p.rewind();
        //TODO track resources
        return true;
    }

    public void debug(PrintWriter pw) {
        var p = mPrimitives;
        var oa = mPointers.elements();
        int oi = 0;
        while (p.hasRemaining()) {
            switch (p.getInt()) {
                case DrawCommandList.CMD_BIND_GRAPHICS_PIPELINE -> {
                    int pipelineIndex = p.getInt();
                    pw.printf("[BindGraphicsPipeline pipelineIndex:%d]%n",
                            pipelineIndex);
                }
                case DrawCommandList.CMD_DRAW -> {
                    int vertexCount = p.getInt();
                    int baseVertex = p.getInt();
                    pw.printf("[Draw vertexCount:%d baseVertex:%d]%n",
                            vertexCount, baseVertex);
                }
                case DrawCommandList.CMD_DRAW_INDEXED -> {
                    int indexCount = p.getInt();
                    int baseIndex = p.getInt();
                    int baseVertex = p.getInt();
                    pw.printf("[DrawIndexed indexCount:%d baseIndex:%d baseVertex:%d]%n",
                            indexCount, baseIndex, baseVertex);
                }
                case DrawCommandList.CMD_DRAW_INSTANCED -> {
                    int instanceCount = p.getInt();
                    int baseInstance = p.getInt();
                    int vertexCount = p.getInt();
                    int baseVertex = p.getInt();
                    pw.printf("[DrawInstanced instanceCount:%d baseInstance:%d vertexCount:%d baseVertex:%d]%n",
                            instanceCount, baseInstance, vertexCount, baseVertex);
                }
                case DrawCommandList.CMD_DRAW_INDEXED_INSTANCED -> {
                    int indexCount = p.getInt();
                    int baseIndex = p.getInt();
                    int instanceCount = p.getInt();
                    int baseInstance = p.getInt();
                    int baseVertex = p.getInt();
                    pw.printf("[DrawIndexedInstanced indexCount:%d baseIndex:%d instanceCount:%d baseInstance:%d baseVertex:%d]%n",
                            indexCount, baseIndex, instanceCount, baseInstance, baseVertex);
                }
                case DrawCommandList.CMD_BIND_INDEX_BUFFER -> {
                    int indexType = p.getInt();
                    long offset = p.getLong();
                    pw.printf("[BindIndexBuffer indexType:%d buffer:%s offset:%d]%n",
                            indexType, oa[oi++], offset);
                }
                case DrawCommandList.CMD_BIND_VERTEX_BUFFER -> {
                    int binding = p.getInt();
                    long offset = p.getLong();
                    pw.printf("[BindVertexBuffer binding:%d buffer:%s offset:%d]%n",
                            binding, oa[oi++], offset);
                }
            }
        }
        p.rewind();
    }
}
