/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.graphics;

import icyllis.modernui.math.MathUtil;
import icyllis.modernui.math.Matrix4;
import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static icyllis.modernui.graphics.GLWrapper.*;

// internal use
public final class RenderNode {

    private static final AtomicInteger sFormatGen = new AtomicInteger();

    public static final int RECT = nextFormatId();

    public static int nextFormatId() {
        return sFormatGen.getAndIncrement();
    }

    {
        /*GLWrapper.glVertexAttribFormat();
        GLWrapper.glBindVertexBuffer();
        GLWrapper.glVertexAttribDivisor();*/
    }

    private int mVertexBuffer = INVALID_ID;

    // instanced array
    private int mMatrixBuffer = INVALID_ID;

    private Int2ObjectMap<List<Consumer<ByteBuffer>>> mVertexUploads;

    // format to count/startOffset
    private final Int2LongMap mDrawInfo = new Int2LongArrayMap();

    private ByteBuffer mVertexData;
    private FloatBuffer mMatrixData;

    private boolean mNeedUpdate = false;

    /**
     * Create a new RenderNode from any thread.
     */
    public RenderNode() {
    }

    public Int2LongMap update() {
        if (mVertexUploads != null) {
            throw new IllegalStateException("Not ending");
        }
        if (mNeedUpdate) {
            if (mVertexBuffer == INVALID_ID) {
                mVertexBuffer = glCreateBuffers();
                mMatrixBuffer = glCreateBuffers();
            }
            glNamedBufferSubData(mVertexBuffer, 0, mVertexData);
            glNamedBufferSubData(mMatrixBuffer, 0, mMatrixData);
            MemoryUtil.memFree(mVertexData);
            MemoryUtil.memFree(mMatrixData);
            mVertexData = null;
            mMatrixData = null;
            mNeedUpdate = false;
        }
        return mDrawInfo;
    }

    public void beginRecording() {
        if (!mNeedUpdate) {
            mVertexUploads = new Int2ObjectArrayMap<>();
            mMatrixData = MemoryUtil.memAllocFloat(16);
            mNeedUpdate = true;
        } else {
            throw new IllegalStateException("Already recorded or began");
        }
    }

    public void endRecording() {
        mDrawInfo.clear();
        ByteBuffer data = MemoryUtil.memAlloc(256);
        for (var entry : mVertexUploads.int2ObjectEntrySet()) {
            List<Consumer<ByteBuffer>> list = entry.getValue();
            int st = data.position();
            list.get(0).accept(data);
            // test vertex size
            int size = data.position() - st;
            int newCap = data.position() + size * (list.size() - 1) + 256;
            newCap = MathUtil.roundUp(newCap, data.capacity());
            if (newCap != data.capacity()) {
                data = MemoryUtil.memRealloc(data, newCap);
            }
            for (int i = 1; i < list.size(); i++) {
                list.get(i).accept(data);
            }
            mDrawInfo.put(entry.getIntKey(), (long) st + list.size() << 32);
        }
        data.flip();
        mVertexData = data;
        mMatrixData.flip();
        mVertexUploads = null;
    }

    public void addVertex(int format, Consumer<ByteBuffer> consumer) {
        mVertexUploads.computeIfAbsent(format, i -> new ArrayList<>()).add(consumer);
    }

    public void addMatrix(Matrix4 mat) {
        if (mMatrixData.remaining() == 0)
            mMatrixData = MemoryUtil.memRealloc(mMatrixData, mMatrixData.capacity() << 1);
        mat.get(mMatrixData);
    }
}
