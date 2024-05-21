/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.Buffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static icyllis.arc3d.engine.Engine.BufferUsageFlags;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL43C.GL_BUFFER;
import static org.lwjgl.opengl.GL44C.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class GLBuffer extends Buffer {

    private int mBuffer;

    private boolean mLocked;

    private long mMappedBuffer;
    @SharedPtr
    private CpuBuffer mStagingBuffer;

    private GLBuffer(GLDevice device,
                     long size,
                     int usage,
                     int buffer) {
        super(device, size, usage);
        mBuffer = buffer;

        registerWithCache(true);
    }

    @Nullable
    @SharedPtr
    public static GLBuffer make(GLDevice device,
                                long size,
                                int usage) {
        assert (size > 0);

        int typeFlags = usage & (BufferUsageFlags.kVertex |
                BufferUsageFlags.kIndex |
                BufferUsageFlags.kTransferSrc |
                BufferUsageFlags.kTransferDst |
                BufferUsageFlags.kUniform |
                BufferUsageFlags.kDrawIndirect);
        assert typeFlags != 0;

        if (Integer.bitCount(typeFlags) != 1) {
            device.getContext().getLogger().error(
                    "Failed to create GLBuffer: only one type bit is allowed, given 0x{}",
                    Integer.toHexString(typeFlags));
            return null;
        }

        if (device.getCaps().hasDSASupport()) {
            int buffer = device.getGL().glCreateBuffers();
            if (buffer == 0) {
                return null;
            }

            /*long persistentlyMappedBuffer = NULL;

            if (preferBufferStorage) {
                int allocFlags = getBufferStorageFlags(usage);

                if (device.getCaps().skipErrorChecks()) {
                    glNamedBufferStorage(buffer, size, allocFlags);
                } else {
                    glClearErrors();
                    glNamedBufferStorage(buffer, size, allocFlags);
                    if (glGetError() != GL_NO_ERROR) {
                        glDeleteBuffers(buffer);
                        new Throwable("RHICreateBuffer, failed to allocate " + size + " bytes from device")
                                .printStackTrace(device.getContext().getErrorWriter());
                        return null;
                    }
                }
                if ((usage & BufferUsageFlags.kStreaming) != 0) {
                    persistentlyMappedBuffer = nglMapNamedBufferRange(buffer, 0, size,
                            GL_MAP_WRITE_BIT |
                                    GL_MAP_PERSISTENT_BIT |
                                    GL_MAP_COHERENT_BIT);
                    if (persistentlyMappedBuffer == NULL) {
                        glDeleteBuffers(buffer);
                        new Throwable("RHICreateBuffer, failed to map buffer range persistently")
                                .printStackTrace(device.getContext().getErrorWriter());
                        return null;
                    }
                }
            } else {*/
            // OpenGL 3.3 uses mutable allocation
            int allocUsage = getBufferUsageM(usage);

            if (device.getCaps().skipErrorChecks()) {
                device.getGL().glNamedBufferData(buffer, size, NULL, allocUsage);
            } else {
                device.clearErrors();
                device.getGL().glNamedBufferData(buffer, size, NULL, allocUsage);
                if (device.getError() != GL_NO_ERROR) {
                    device.getGL().glDeleteBuffers(buffer);
                    device.getContext().getLogger().error(
                            "Failed to create GLBuffer: failed to allocate {} bytes from device",
                            size);
                    return null;
                }
            }
            //}

            return new GLBuffer(device, size, usage, buffer);
        } else {
            int buffer = device.getGL().glGenBuffers();
            if (buffer == 0) {
                return null;
            }

            int target = device.bindBufferForSetup(usage, buffer);
            /*long persistentlyMappedBuffer = NULL;

            if (preferBufferStorage && device.getCaps().hasBufferStorageSupport()) {
                int allocFlags = getBufferStorageFlags(usage);

                if (device.getCaps().skipErrorChecks()) {
                    glBufferStorage(target, size, allocFlags);
                } else {
                    glClearErrors();
                    glBufferStorage(target, size, allocFlags);
                    if (glGetError() != GL_NO_ERROR) {
                        glDeleteBuffers(buffer);
                        new Throwable("RHICreateBuffer, failed to allocate " + size + " bytes from device")
                                .printStackTrace(device.getContext().getErrorWriter());
                        return null;
                    }
                }
                if ((usage & BufferUsageFlags.kStreaming) != 0) {
                    persistentlyMappedBuffer = nglMapBufferRange(target, 0, size,
                            GL_MAP_WRITE_BIT |
                                    GL_MAP_PERSISTENT_BIT |
                                    GL_MAP_COHERENT_BIT);
                    if (persistentlyMappedBuffer == NULL) {
                        glDeleteBuffers(buffer);
                        new Throwable("RHICreateBuffer, failed to map buffer range persistently")
                                .printStackTrace(device.getContext().getErrorWriter());
                        return null;
                    }
                }
            } else {*/
            // OpenGL 3.3 uses mutable allocation
            int allocUsage = getBufferUsageM(usage);

            if (device.getCaps().skipErrorChecks()) {
                device.getGL().glBufferData(target, size, NULL, allocUsage);
            } else {
                device.clearErrors();
                device.getGL().glBufferData(target, size, NULL, allocUsage);
                if (device.getError() != GL_NO_ERROR) {
                    device.getGL().glDeleteBuffers(buffer);
                    device.getContext().getLogger().error(
                            "Failed to create GLBuffer: failed to allocate {} bytes from device",
                            size);
                    return null;
                }
            }
            //}

            return new GLBuffer(device, size, usage, buffer);
        }
    }

    public static int getBufferUsageM(int usage) {
        int allocUsage;
        if ((usage & BufferUsageFlags.kTransferDst) != 0) {
            allocUsage = GL_DYNAMIC_READ;
        } else if ((usage & BufferUsageFlags.kHostVisible) != 0) {
            if ((usage & BufferUsageFlags.kUniform) != 0) {
                allocUsage = GL_DYNAMIC_DRAW;
            } else {
                allocUsage = GL_STREAM_DRAW;
            }
        } else if ((usage & BufferUsageFlags.kDeviceLocal) != 0) {
            allocUsage = GL_STATIC_DRAW;
        } else {
            allocUsage = GL_DYNAMIC_DRAW;
        }
        return allocUsage;
    }

    //TODO figure this out and make use of OpenGL 4.4 buffer storage
    public static int getBufferStorageFlags(int usage) {
        int allocFlags = 0;
        if ((usage & BufferUsageFlags.kTransferSrc) != 0) {
            allocFlags |= GL_MAP_WRITE_BIT;
        }
        if ((usage & BufferUsageFlags.kTransferDst) != 0) {
            allocFlags |= GL_MAP_READ_BIT;
        }
        /*if ((usage & BufferUsageFlags.kStreaming) != 0) {
            // no staging buffer, use pinned memory
            allocFlags |= GL_MAP_WRITE_BIT |
                    GL_MAP_PERSISTENT_BIT |
                    GL_MAP_COHERENT_BIT;
        } else if ((usage & (BufferUsageFlags.kVertex |
                BufferUsageFlags.kIndex)) != 0) {
            allocFlags |= GL_DYNAMIC_STORAGE_BIT;
        }*/
        return allocFlags;
    }

    public int getHandle() {
        return mBuffer;
    }

    @Override
    protected void onSetLabel(@Nonnull String label) {
        if (getDevice().getCaps().hasDebugSupport()) {
            if (label.isEmpty()) {
                getDevice().getGL().glObjectLabel(GL_BUFFER, mBuffer, 0, NULL);
            } else {
                label = label.substring(0, Math.min(label.length(),
                        getDevice().getCaps().maxLabelLength()));
                getDevice().getGL().glObjectLabel(GL_BUFFER, mBuffer, label);
            }
        }
    }

    @Override
    protected void onRelease() {
        if (mBuffer != 0) {
            getDevice().getGL().glDeleteBuffers(mBuffer);
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
    protected GLDevice getDevice() {
        return (GLDevice) super.getDevice();
    }

    @Override
    protected long onLock(int mode, long offset, long size) {
        var device = getDevice();
        assert (device.getContext().isOwnerThread());
        assert (!mLocked);
        assert (mBuffer != 0);

        mLocked = true;

        if (mode == kRead_LockMode) {
            // prefer mapping, such as pixel buffer object
            if (device.getCaps().hasDSASupport()) {
                mMappedBuffer = device.getGL().glMapNamedBufferRange(mBuffer, offset, size, GL_MAP_READ_BIT);
            } else {
                int target = device.bindBuffer(this);
                mMappedBuffer = device.getGL().glMapBufferRange(target, offset, size, GL_MAP_READ_BIT);
            }
            if (mMappedBuffer == NULL) {
                getDevice().getContext().getLogger().error("Failed to map buffer {}", mBuffer);
            }
            return mMappedBuffer;
        } else {
            // prefer CPU staging buffer
            assert (mode == kWriteDiscard_LockMode);
            mStagingBuffer = device.getCpuBufferPool().makeBuffer(size);
            assert (mStagingBuffer != null);
            return mStagingBuffer.data();
        }
    }

    @Override
    protected void onUnlock(int mode, long offset, long size) {
        var device = getDevice();
        assert (device.getContext().isOwnerThread());
        assert (mLocked);
        assert (mBuffer != 0);

        int target = device.getCaps().hasDSASupport()
                ? 0
                : device.bindBuffer(this);
        if (mode == kRead_LockMode) {
            assert (mMappedBuffer != NULL);
            if (target == 0) {
                device.getGL().glUnmapNamedBuffer(mBuffer);
            } else {
                device.getGL().glUnmapBuffer(target);
            }
            mMappedBuffer = NULL;
        } else {
            assert (mode == kWriteDiscard_LockMode);
            if ((mUsage & BufferUsageFlags.kDeviceLocal) == 0) {
                // non-static needs triple buffering, though most GPU drivers did it internally
                doInvalidateBuffer(target, offset, size);
            }
            assert (mStagingBuffer != null);
            doUploadData(target, mStagingBuffer.data(), offset, size);
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
        if (mLocked) {
            return mMappedBuffer != NULL ? mMappedBuffer : mStagingBuffer.data();
        }
        return NULL;
    }

    @Override
    protected boolean onUpdateData(int offset, int size, long data) {
        assert (getDevice().getContext().isOwnerThread());
        assert (mBuffer != 0);
        int target = getDevice().getCaps().hasDSASupport()
                ? 0
                : getDevice().bindBuffer(this);
        if ((mUsage & BufferUsageFlags.kDeviceLocal) == 0) {
            // non-static needs triple buffering, though most GPU drivers did it internally
            if (!doInvalidateBuffer(target, offset, size)) {
                return false;
            }
        }
        doUploadData(target, data, offset, size);
        return true;
    }

    // restricted to 256KB per update
    private static final long MAX_BYTES_PER_UPDATE = 1 << 18;

    private void doUploadData(int target, long data, long offset, long totalSize) {
        var gl = getDevice().getGL();
        if (target == 0) {
            while (totalSize > 0) {
                long size = Math.min(MAX_BYTES_PER_UPDATE, totalSize);
                gl.glNamedBufferSubData(mBuffer, offset, size, data);
                data += size;
                offset += size;
                totalSize -= size;
            }
        } else {
            while (totalSize > 0) {
                long size = Math.min(MAX_BYTES_PER_UPDATE, totalSize);
                gl.glBufferSubData(target, offset, size, data);
                data += size;
                offset += size;
                totalSize -= size;
            }
        }
    }

    private boolean doInvalidateBuffer(int target, long offset, long size) {
        // to be honest, invalidation doesn't help performance in most cases
        // because GPU drivers did optimizations
        var device = getDevice();
        switch (device.getCaps().getInvalidateBufferType()) {
            case GLCaps.INVALIDATE_BUFFER_TYPE_NULL_DATA -> {
                assert target != 0; // DSA is not support
                int allocUsage = getBufferUsageM(getUsage());
                // orphan full size
                // null data will allocate new space, so check errors
                if (device.getCaps().skipErrorChecks()) {
                    device.getGL().glBufferData(target, mSize, NULL, allocUsage);
                } else {
                    device.clearErrors();
                    device.getGL().glBufferData(target, mSize, NULL, allocUsage);
                    if (device.getError() != GL_NO_ERROR) {
                        return false;
                    }
                }
            }
            case GLCaps.INVALIDATE_BUFFER_TYPE_INVALIDATE -> {
                device.getGL().glInvalidateBufferSubData(mBuffer, offset, size);
            }
        }
        return true;
    }
}
