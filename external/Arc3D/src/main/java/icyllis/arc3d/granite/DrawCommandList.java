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

import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.core.Rect2ic;
import icyllis.arc3d.engine.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.PrintWriter;

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
    public static final int CMD_BIND_UNIFORM_BUFFER = 8;
    // length-prefixed command
    public static final int CMD_BIND_TEXTURES = 9;

    // the bind buffer 'offset' and 'size' always fit in 32 bits
    // we cheat here and in DrawPass

    /**
     * Holds all primitive data.
     */
    public final IntArrayList mPrimitives = new IntArrayList(128);
    /**
     * Holds all reference data, raw pointers.
     */
    public final ObjectArrayList<Object> mPointers = new ObjectArrayList<>();

    public void bindGraphicsPipeline(int pipelineIndex) {
        mPrimitives.add(CMD_BIND_GRAPHICS_PIPELINE);
        mPrimitives.add(pipelineIndex);
    }

    public void draw(int vertexCount, int baseVertex) {
        mPrimitives.add(CMD_DRAW);
        mPrimitives.add(vertexCount);
        mPrimitives.add(baseVertex);
    }

    public final void drawIndexed(int indexCount, int baseIndex,
                                  int baseVertex) {
        mPrimitives.add(CMD_DRAW_INDEXED);
        mPrimitives.add(indexCount);
        mPrimitives.add(baseIndex);
        mPrimitives.add(baseVertex);
    }

    public final void drawInstanced(int instanceCount, int baseInstance,
                                    int vertexCount, int baseVertex) {
        mPrimitives.add(CMD_DRAW_INSTANCED);
        mPrimitives.add(instanceCount);
        mPrimitives.add(baseInstance);
        mPrimitives.add(vertexCount);
        mPrimitives.add(baseVertex);
    }

    public final void drawIndexedInstanced(int indexCount, int baseIndex,
                                           int instanceCount, int baseInstance,
                                           int baseVertex) {
        mPrimitives.add(CMD_DRAW_INDEXED_INSTANCED);
        mPrimitives.add(indexCount);
        mPrimitives.add(baseIndex);
        mPrimitives.add(instanceCount);
        mPrimitives.add(baseInstance);
        mPrimitives.add(baseVertex);
    }

    public final void bindIndexBuffer(int indexType,
                                      BufferViewInfo bufferInfo) {
        mPrimitives.add(CMD_BIND_INDEX_BUFFER);
        mPrimitives.add(indexType);
        mPrimitives.add((int) bufferInfo.mOffset);
        mPointers.add(bufferInfo.mBuffer);
    }

    public final void bindVertexBuffer(int binding,
                                       BufferViewInfo bufferInfo) {
        mPrimitives.add(CMD_BIND_VERTEX_BUFFER);
        mPrimitives.add(binding);
        mPrimitives.add((int) bufferInfo.mOffset);
        mPointers.add(bufferInfo.mBuffer);
    }

    /**
     * Flush scissor.
     *
     * @param surfaceHeight the effective height of color attachment
     * @param origin        the surface origin
     * @see Engine.SurfaceOrigin
     */
    public final void setScissor(Rect2ic scissor, int surfaceHeight, int origin) {
        int y;
        int height = scissor.height();
        if (origin == Engine.SurfaceOrigin.kLowerLeft) {
            y = surfaceHeight - scissor.bottom();
        } else {
            assert (origin == Engine.SurfaceOrigin.kUpperLeft);
            y = scissor.y();
        }
        assert (y >= 0);
        mPrimitives.add(CMD_SET_SCISSOR);
        mPrimitives.add(scissor.x());
        mPrimitives.add(y);
        mPrimitives.add(scissor.width());
        mPrimitives.add(height);
    }

    public final void bindUniformBuffer(int binding,
                                        @RawPtr Buffer buffer,
                                        long offset,
                                        long size) {
        mPrimitives.add(CMD_BIND_UNIFORM_BUFFER);
        mPrimitives.add(binding);
        mPrimitives.add((int) offset);
        mPrimitives.add((int) size);
        mPointers.add(buffer);
    }

    /**
     * @param textures pairs of texture view index and sampler index
     */
    public final void bindTextures(int[] textures) {
        int n = textures.length >> 1;
        assert n > 0;
        mPrimitives.add(CMD_BIND_TEXTURES);
        mPrimitives.add(n);
        mPrimitives.addElements(mPrimitives.size(), textures);
    }

    public final void finish() {
    }

    public void debug(PrintWriter pw) {
        var p = mPrimitives.elements();
        int i = 0;
        var oa = mPointers.elements();
        int oi = 0;
        int lim = mPrimitives.size();
        while (i < lim) {
            switch (p[i++]) {
                case DrawCommandList.CMD_BIND_GRAPHICS_PIPELINE -> {
                    int pipelineIndex = p[i++];
                    pw.printf("[BindGraphicsPipeline pipelineIndex:%d]%n",
                            pipelineIndex);
                }
                case DrawCommandList.CMD_DRAW -> {
                    int vertexCount = p[i++];
                    int baseVertex = p[i++];
                    pw.printf("[Draw vertexCount:%d baseVertex:%d]%n",
                            vertexCount, baseVertex);
                }
                case DrawCommandList.CMD_DRAW_INDEXED -> {
                    int indexCount = p[i++];
                    int baseIndex = p[i++];
                    int baseVertex = p[i++];
                    pw.printf("[DrawIndexed indexCount:%d baseIndex:%d baseVertex:%d]%n",
                            indexCount, baseIndex, baseVertex);
                }
                case DrawCommandList.CMD_DRAW_INSTANCED -> {
                    int instanceCount = p[i++];
                    int baseInstance = p[i++];
                    int vertexCount = p[i++];
                    int baseVertex = p[i++];
                    pw.printf("[DrawInstanced instanceCount:%d baseInstance:%d vertexCount:%d baseVertex:%d]%n",
                            instanceCount, baseInstance, vertexCount, baseVertex);
                }
                case DrawCommandList.CMD_DRAW_INDEXED_INSTANCED -> {
                    int indexCount = p[i++];
                    int baseIndex = p[i++];
                    int instanceCount = p[i++];
                    int baseInstance = p[i++];
                    int baseVertex = p[i++];
                    pw.printf("[DrawIndexedInstanced indexCount:%d baseIndex:%d instanceCount:%d baseInstance:%d " +
                                    "baseVertex:%d]%n",
                            indexCount, baseIndex, instanceCount, baseInstance, baseVertex);
                }
                case DrawCommandList.CMD_BIND_INDEX_BUFFER -> {
                    int indexType = p[i++];
                    long offset = p[i++];
                    pw.printf("[BindIndexBuffer indexType:%d buffer:%s offset:%d]%n",
                            indexType, oa[oi++], offset);
                }
                case DrawCommandList.CMD_BIND_VERTEX_BUFFER -> {
                    int binding = p[i++];
                    long offset = p[i++];
                    pw.printf("[BindVertexBuffer binding:%d buffer:%s offset:%d]%n",
                            binding, oa[oi++], offset);
                }
            }
        }
    }
}
