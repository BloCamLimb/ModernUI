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

package icyllis.arc3d.opengl;

import icyllis.modernui.graphics.RefCnt;
import icyllis.modernui.graphics.SharedPtr;
import icyllis.arc3d.engine.*;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static icyllis.arc3d.opengl.GLCore.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class GLBuffer extends Buffer {

    private final int mType;
    private int mBuffer;

    private boolean mLocked;

    private long mMappedBuffer;
    @SharedPtr
    private CpuBuffer mStagingBuffer;

    private GLBuffer(GLEngine engine,
                     int size,
                     int usage,
                     int type,
                     int buffer) {
        super(engine, size, usage);
        mType = type;
        mBuffer = buffer;

        registerWithCache(true);

        // OpenGL 3.3 uses mutable allocation
        if (!engine.getCaps().hasDSASupport()) {

            int target = engine.bindBuffer(this);
            int flag = getUsageFlag();

            if (engine.getCaps().skipErrorChecks()) {
                glBufferData(target, size, flag);
            } else {
                glClearErrors();
                glBufferData(target, size, flag);
                if (glGetError() != GL_NO_ERROR) {
                    glDeleteBuffers(mBuffer);
                    mBuffer = 0;
                    // OOM
                    removeScratchKey();
                }
            }
        }
    }

    @Nullable
    @SharedPtr
    public static GLBuffer make(GLEngine engine,
                                int size,
                                int usage) {
        assert (size > 0);

        int type;
        if ((usage & Engine.BufferUsageFlags.kVertex) != 0) {
            type = GLEngine.BUFFER_TYPE_VERTEX;
        } else if ((usage & Engine.BufferUsageFlags.kIndex) != 0) {
            type = GLEngine.BUFFER_TYPE_INDEX;
        } else if ((usage & Engine.BufferUsageFlags.kTransferSrc) != 0) {
            type = GLEngine.BUFFER_TYPE_XFER_SRC;
        } else if ((usage & Engine.BufferUsageFlags.kTransferDst) != 0) {
            type = GLEngine.BUFFER_TYPE_XFER_DST;
        } else {
            return null;
        }

        if (engine.getCaps().hasDSASupport()) {
            int flags = 0;
            if ((usage & (Engine.BufferUsageFlags.kVertex | Engine.BufferUsageFlags.kIndex)) != 0) {
                flags |= GL_DYNAMIC_STORAGE_BIT;
            }
            if ((usage & Engine.BufferUsageFlags.kTransferSrc) != 0) {
                flags |= GL_MAP_WRITE_BIT;
            }
            if ((usage & Engine.BufferUsageFlags.kTransferDst) != 0) {
                flags |= GL_MAP_READ_BIT;
            }

            int buffer = glCreateBuffers();
            if (buffer == 0) {
                return null;
            }
            if (engine.getCaps().skipErrorChecks()) {
                glNamedBufferStorage(buffer, size, flags);
            } else {
                glClearErrors();
                glNamedBufferStorage(buffer, size, flags);
                if (glGetError() != GL_NO_ERROR) {
                    glDeleteBuffers(buffer);
                    return null;
                }
            }

            return new GLBuffer(engine, size, usage, type, buffer);
        } else {
            int buffer = glGenBuffers();
            if (buffer == 0) {
                return null;
            }

            GLBuffer res = new GLBuffer(engine, size, usage, type, buffer);
            if (res.mBuffer == 0) {
                // OOM
                res.unref();
                return null;
            }

            return res;
        }
    }

    /**
     * For OpenGL 3.3.
     */
    public int getTypeEnum() {
        return mType;
    }

    /**
     * For OpenGL 3.3.
     */
    public int getUsageFlag() {
        if ((mUsage & Engine.BufferUsageFlags.kTransferDst) != 0) {
            return GL_DYNAMIC_READ;
        } else if ((mUsage & Engine.BufferUsageFlags.kStream) != 0) {
            return GL_STREAM_DRAW;
        } else {
            return GL_DYNAMIC_DRAW;
        }
    }

    public int getBufferID() {
        return mBuffer;
    }

    @Override
    protected void onSetLabel(@Nonnull String label) {
        if (getEngine().getCaps().hasDebugSupport()) {
            if (label.isEmpty()) {
                nglObjectLabel(GL_BUFFER, mBuffer, 0, MemoryUtil.NULL);
            } else {
                label = label.substring(0, Math.min(label.length(),
                        getEngine().getCaps().maxLabelLength()));
                glObjectLabel(GL_BUFFER, mBuffer, label);
            }
        }
    }

    @Override
    protected void onRelease() {
        if (mBuffer != 0) {
            glDeleteBuffers(mBuffer);
        }
        onDiscard();
    }

    @Override
    protected void onDiscard() {
        mBuffer = 0;
        mLocked = false;
        mMappedBuffer = NULL;
        mStagingBuffer = RefCnt.move(mStagingBuffer);
    }

    @Override
    protected GLEngine getEngine() {
        return (GLEngine) super.getEngine();
    }

    @Override
    protected long onLock(int mode, int offset, int size) {
        assert (getEngine().getContext().isOwnerThread());
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
            mStagingBuffer = getEngine().getCpuBufferCache().makeBuffer(size);
            assert (mStagingBuffer != null);
            return mStagingBuffer.data();
        }
    }

    @Override
    protected void onUnlock(int mode, int offset, int size) {
        assert (getEngine().getContext().isOwnerThread());
        assert (mLocked);
        assert (mBuffer != 0);

        if (mode == kRead_LockMode) {
            assert (mMappedBuffer != NULL);
            glUnmapNamedBuffer(mBuffer);
            mMappedBuffer = NULL;
        } else {
            assert (mode == kWriteDiscard_LockMode);
            if ((mUsage & Engine.BufferUsageFlags.kStatic) == 0) {
                // non-static needs triple buffering, but GPU drivers did
                doInvalidateBuffer(offset, size);
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
        assert (getEngine().getContext().isOwnerThread());
        assert (mBuffer != 0);
        if ((mUsage & Engine.BufferUsageFlags.kStatic) == 0) {
            // non-static needs triple buffering, but GPU drivers did
            doInvalidateBuffer(offset, size);
        }
        doUploadData(data, offset, size);
        return true;
    }

    // restricted to 256KB per update
    private static final int MAX_TRANSFER_UNIT = 1 << 18;

    private void doUploadData(long data, int offset, int totalSize) {
        if (getEngine().getCaps().hasDSASupport()) {
            while (totalSize > 0) {
                int size = Math.min(MAX_TRANSFER_UNIT, totalSize);
                nglNamedBufferSubData(mBuffer, offset, size, data);
                data += size;
                offset += size;
                totalSize -= size;
            }
        } else {
            int target = getEngine().bindBuffer(this);
            while (totalSize > 0) {
                int size = Math.min(MAX_TRANSFER_UNIT, totalSize);
                nglBufferSubData(target, offset, size, data);
                data += size;
                offset += size;
                totalSize -= size;
            }
        }
    }

    private void doInvalidateBuffer(int offset, int size) {
        if (getEngine().getCaps().getInvalidateBufferType() == GLCaps.INVALIDATE_BUFFER_TYPE_INVALIDATE) {
            glInvalidateBufferSubData(mBuffer, offset, size);
        }
        // to be honest, invalidation doesn't help performance in most cases
        // because GPU drivers did optimizations
    }
}
