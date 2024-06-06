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

import icyllis.arc3d.engine.BufferViewInfo;
import icyllis.arc3d.engine.DynamicBufferManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.function.Function;

public class UniformTracker {

    // ideally this is per-size cache rather than per-pipeline cache
    // but we will use push constants for small UBOs

    static class UniformCache {
        // uniform data is already de-duplicated, use reference identity, raw pointer
        final IdentityHashMap<ByteBuffer, Integer> mDataToIndex = new IdentityHashMap<>();
        // raw pointers
        final ObjectArrayList<ByteBuffer> mIndexToData = new ObjectArrayList<>();
        final Function<ByteBuffer, Integer> mAccumulator = data -> {
            int index = mIndexToData.size();
            mIndexToData.add(data);
            return index;
        };
        final BufferViewInfo mBufferInfo = new BufferViewInfo();

        int insert(ByteBuffer data) {
            return mDataToIndex.computeIfAbsent(data, mAccumulator);
        }
    }

    // Access first by pipeline index. The final UniformCache::Index is either used to select the
    // BindBufferInfo for a draw using UBOs, or it's the real index into a packed array of uniforms
    // in a storage buffer object (whose binding is stored in index 0).
    ObjectArrayList<UniformCache> mPerPipelineCaches = new ObjectArrayList<>();

    int mLastPipelineIndex = DrawPass.INVALID_INDEX;
    int mLastUniformIndex = DrawPass.INVALID_INDEX;

    /**
     * Maps a given {pipeline index, uniform data cache index} pair to a buffer index within the
     * pipeline's accumulated array of uniforms.
     *
     * @param data a stable pointer to uniform data
     * @return uniform index
     */
    public int trackUniforms(int pipelineIndex,
                             ByteBuffer data) {
        if (data == null) {
            return DrawPass.INVALID_INDEX;
        }

        while (pipelineIndex >= mPerPipelineCaches.size()) {
            mPerPipelineCaches.add(new UniformCache());
        }

        var cache = mPerPipelineCaches.get(pipelineIndex);
        return cache.insert(data);
    }

    public boolean writeUniforms(DynamicBufferManager bufferManager) {
        for (var cache : mPerPipelineCaches) {
            int numBlocks = cache.mIndexToData.size();
            if (numBlocks == 0) {
                continue;
            }
            var blocks = cache.mIndexToData.elements();
            // All data blocks for the same pipeline have the same size, so peek the first
            // to determine the total buffer size
            int dataSize = blocks[0].remaining();
            int blockSize = bufferManager.alignUniformBlockSize(dataSize);

            var writer = bufferManager.getUniformWriter(
                    blockSize * numBlocks, cache.mBufferInfo);
            if (writer == null) {
                return false;
            }
            int padding = blockSize - dataSize;
            cache.mBufferInfo.mSize = blockSize;

            for (int i = 0; i < numBlocks; i++) {
                ByteBuffer src = blocks[i];
                assert src.remaining() == dataSize;
                writer.put(src)
                        .position(writer.position() + padding);
            }
        }

        return true;
    }

    // Updates the current tracked pipeline and uniform index and returns whether or not
    // bindBuffers() needs to be called, depending on if 'fUseStorageBuffers' is true or not.
    public boolean setCurrentUniforms(int pipelineIndex,
                                      int uniformIndex) {
        if (uniformIndex == DrawPass.INVALID_INDEX) {
            return false;
        }
        assert pipelineIndex != DrawPass.INVALID_INDEX;
        assert pipelineIndex < mPerPipelineCaches.size() &&
                uniformIndex < mPerPipelineCaches.get(pipelineIndex).mIndexToData.size();

        if (pipelineIndex != mLastPipelineIndex ||
                uniformIndex != mLastUniformIndex) {
            mLastPipelineIndex = pipelineIndex;
            mLastUniformIndex = uniformIndex;
            return true;
        } else {
            return false;
        }
    }

    public void bindUniforms(int binding, DrawCommandList commandList) {
        assert mLastUniformIndex != DrawPass.INVALID_INDEX;
        var cache = mPerPipelineCaches.get(mLastPipelineIndex);
        var bufferInfo = cache.mBufferInfo;
        commandList.bindUniformBuffer(
                binding,
                bufferInfo.mBuffer,
                bufferInfo.mOffset + mLastUniformIndex * bufferInfo.mSize,
                bufferInfo.mSize
        );
    }
}
