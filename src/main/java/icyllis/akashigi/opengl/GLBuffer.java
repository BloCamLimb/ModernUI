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

package icyllis.akashigi.opengl;

import icyllis.akashigi.core.RefCnt;
import icyllis.akashigi.core.SharedPtr;
import icyllis.akashigi.engine.Buffer;
import icyllis.akashigi.engine.CpuBuffer;

import javax.annotation.Nullable;

import static icyllis.akashigi.engine.Engine.*;
import static icyllis.akashigi.opengl.GLCore.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class GLBuffer extends Buffer {

    private int mBuffer;

    private boolean mLocked;

    private long mMappedBuffer;
    @SharedPtr
    private CpuBuffer mStagingBuffer;

    private GLBuffer(GLServer server,
                     int size,
                     int bufferType,
                     int accessPattern,
                     int buffer) {
        super(server, size, bufferType, accessPattern);
        mBuffer = buffer;

        registerWithCache(true);
    }

    @Nullable
    @SharedPtr
    public static GLBuffer make(GLServer server,
                                int size,
                                int bufferType,
                                int accessPattern) {
        assert (size > 0);

        int allocFlags = 0;
        switch (bufferType) {
            case BufferType_Vertex, BufferType_Index -> {
                assert (accessPattern == AccessPattern_Static
                        || accessPattern == AccessPattern_Dynamic);
                allocFlags |= GL_DYNAMIC_STORAGE_BIT;
            }
            case BufferType_Uniform -> {
                assert (accessPattern == AccessPattern_Dynamic);
                allocFlags |= GL_DYNAMIC_STORAGE_BIT;
            }
            case BufferType_XferSrcToDst -> {
                assert (accessPattern == AccessPattern_Dynamic);
                allocFlags |= GL_MAP_WRITE_BIT;
            }
            case BufferType_XferDstToSrc -> {
                assert (accessPattern == AccessPattern_Dynamic);
                allocFlags |= GL_MAP_READ_BIT;
            }
        }

        int buffer = glCreateBuffers();
        if (buffer == 0) {
            return null;
        }
        if (server.getCaps().skipErrorChecks()) {
            glNamedBufferStorage(buffer, size, allocFlags);
        } else {
            glClearErrors();
            glNamedBufferStorage(buffer, size, allocFlags);
            if (glGetError() != GL_NO_ERROR) {
                glDeleteBuffers(buffer);
                return null;
            }
        }

        return new GLBuffer(server, size, bufferType, accessPattern, buffer);
    }

    public int getBufferID() {
        return mBuffer;
    }

    @Override
    protected void onRelease() {
        if (mBuffer != 0) {
            glDeleteBuffers(mBuffer);
            mBuffer = 0;
        }
        mLocked = false;
        mMappedBuffer = NULL;
        mStagingBuffer = RefCnt.move(mStagingBuffer);
    }

    @Override
    protected void onDiscard() {
        mBuffer = 0;
        mLocked = false;
        mMappedBuffer = NULL;
        mStagingBuffer = RefCnt.move(mStagingBuffer);
    }

    @Override
    protected GLServer getServer() {
        return (GLServer) super.getServer();
    }

    @Override
    protected long onLock(int mode, int offset, int size) {
        assert (getServer().getContext().isOnOwnerThread());
        assert (!mLocked);
        assert (mBuffer != 0);

        mLocked = true;

        if (mode == LockMode_Read) {
            // prefer mapping, such as pixel buffer object
            mMappedBuffer = nglMapNamedBufferRange(mBuffer, offset, size, GL_MAP_READ_BIT);
            if (mMappedBuffer == NULL) {
                throw new IllegalStateException();
            }
            return mMappedBuffer;
        } else {
            // prefer CPU staging buffer
            assert (mode == LockMode_WriteDiscard);
            mStagingBuffer = getServer().getCpuBufferCache().makeBuffer(size);
            assert (mStagingBuffer != null);
            return mStagingBuffer.data();
        }
    }

    @Override
    protected void onUnlock(int mode, int offset, int size) {
        assert (getServer().getContext().isOnOwnerThread());
        assert (mLocked);
        assert (mBuffer != 0);

        if (mode == LockMode_Read) {
            assert (mMappedBuffer != NULL);
            glUnmapNamedBuffer(mBuffer);
            mMappedBuffer = NULL;
        } else {
            assert (mode == LockMode_WriteDiscard);
            if (accessPattern() != AccessPattern_Static) {
                glInvalidateBufferSubData(mBuffer, offset, size);
            }
            assert (mStagingBuffer != null);
            doUploadData(mStagingBuffer.data(), offset, size);
            mStagingBuffer = RefCnt.move(mStagingBuffer);
        }
        mLocked = false;
    }

    @Override
    public boolean isLocked() {
        return mLocked;
    }

    @Override
    public long getLockedBuffer() {
        assert (mLocked);
        return mMappedBuffer != NULL ? mMappedBuffer : mStagingBuffer.data();
    }

    @Override
    protected boolean onUpdateData(long data, int offset, int size) {
        assert (getServer().getContext().isOnOwnerThread());
        assert (mBuffer != 0);
        if (accessPattern() != AccessPattern_Static) {
            glInvalidateBufferSubData(mBuffer, offset, size);
        }
        doUploadData(data, offset, size);
        return true;
    }

    private void doUploadData(long data, int offset, int totalSize) {
        while (totalSize > 0) {
            // restricted to 256KB per update
            int size = Math.min(1 << 18, totalSize);
            nglNamedBufferSubData(mBuffer, offset, size, data);
            data += size;
            offset += size;
            totalSize -= size;
        }
    }
}
