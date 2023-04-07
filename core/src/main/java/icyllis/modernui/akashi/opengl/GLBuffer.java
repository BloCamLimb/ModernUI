/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.akashi.opengl;

import icyllis.modernui.core.RefCnt;
import icyllis.modernui.annotation.SharedPtr;
import icyllis.modernui.akashi.Buffer;
import icyllis.modernui.akashi.CpuBuffer;

import javax.annotation.Nullable;

import static icyllis.modernui.akashi.Engine.BufferUsageFlags;
import static icyllis.modernui.akashi.opengl.GLCore.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class GLBuffer extends Buffer {

    private int mBuffer;

    private boolean mLocked;

    private long mMappedBuffer;
    @SharedPtr
    private CpuBuffer mStagingBuffer;

    private GLBuffer(GLServer server,
                     int size,
                     int usage,
                     int buffer) {
        super(server, size, usage);
        mBuffer = buffer;

        registerWithCache(true);
    }

    @Nullable
    @SharedPtr
    public static GLBuffer make(GLServer server,
                                int size,
                                int usage) {
        assert (size > 0);

        int allocFlags = 0;
        if ((usage & (BufferUsageFlags.kVertex | BufferUsageFlags.kIndex | BufferUsageFlags.kUniform)) != 0) {
            allocFlags |= GL_DYNAMIC_STORAGE_BIT;
        }
        if ((usage & BufferUsageFlags.kTransferSrc) != 0) {
            allocFlags |= GL_MAP_WRITE_BIT;
        }
        if ((usage & BufferUsageFlags.kTransferDst) != 0) {
            allocFlags |= GL_MAP_READ_BIT;
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

        return new GLBuffer(server, size, usage, buffer);
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
        assert (getServer().getContext().isOwnerThread());
        assert (!mLocked);
        assert (mBuffer != 0);

        mLocked = true;

        if (mode == kRead_LockMode) {
            // prefer mapping, such as pixel buffer object
            mMappedBuffer = nglMapNamedBufferRange(mBuffer, offset, size, GL_MAP_READ_BIT);
            if (mMappedBuffer == NULL) {
                throw new IllegalStateException();
            }
            return mMappedBuffer;
        } else {
            // prefer CPU staging buffer
            assert (mode == kWriteDiscard_LockMode);
            mStagingBuffer = getServer().getCpuBufferCache().makeBuffer(size);
            assert (mStagingBuffer != null);
            return mStagingBuffer.data();
        }
    }

    @Override
    protected void onUnlock(int mode, int offset, int size) {
        assert (getServer().getContext().isOwnerThread());
        assert (mLocked);
        assert (mBuffer != 0);

        if (mode == kRead_LockMode) {
            assert (mMappedBuffer != NULL);
            glUnmapNamedBuffer(mBuffer);
            mMappedBuffer = NULL;
        } else {
            assert (mode == kWriteDiscard_LockMode);
            if ((mUsage & BufferUsageFlags.kStatic) == 0) {
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
        assert (getServer().getContext().isOwnerThread());
        assert (mBuffer != 0);
        if ((mUsage & BufferUsageFlags.kStatic) == 0) {
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
