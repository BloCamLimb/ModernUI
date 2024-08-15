/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.MathUtil;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.Buffer;
import icyllis.arc3d.engine.Context;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nullable;

import static icyllis.arc3d.engine.Engine.BufferUsageFlags;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL43C.*;
import static org.lwjgl.opengl.GL44C.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class GLBuffer extends Buffer {

    private volatile int mBuffer;

    private volatile long mPersistentlyMappedBuffer;

    private long mCachedBuffer;
    private long mCachedBufferSize;

    // No persistent mapping support. Most write operations are done on non OpenGL threads
    // then we just use client array as staging buffer.
    private final boolean mClientUploadBuffer;

    private GLBuffer(Context context,
                     long size,
                     int usage,
                     boolean clientUploadBuffer) {
        super(context, size, usage);
        mClientUploadBuffer = clientUploadBuffer;
    }

    @Nullable
    @SharedPtr
    public static GLBuffer make(Context context,
                                long size,
                                int usage) {
        assert (size > 0);

        GLDevice device = (GLDevice) context.getDevice();

        boolean clientUploadBuffer;
        boolean bufferStorage = device.getCaps().hasBufferStorageSupport();
        // No persistent mapping support. Most write operations are done on non OpenGL threads
        // then we just use client array as staging buffer.
        clientUploadBuffer = !bufferStorage && (usage & BufferUsageFlags.kUpload) != 0;

        if (clientUploadBuffer) {
            long clientBuffer = MemoryUtil.nmemAlloc(size);
            if (clientBuffer == NULL) {
                return null;
            }
            // je_malloc is 16-byte aligned on 64-bit system,
            // it's safe to use Unsafe to transfer primitive data
            assert MathUtil.isAlign8(clientBuffer);
            GLBuffer buffer = new GLBuffer(context, size, usage, true);
            buffer.mCachedBuffer = clientBuffer;
            buffer.mCachedBufferSize = size;
            return buffer;
        } else {
            GLBuffer buffer = new GLBuffer(context, size, usage, false);
            if (device.isOnExecutingThread()) {
                if (!buffer.initialize()) {
                    buffer.unref();
                    return null;
                }
            } else {
                device.executeRenderCall(dev -> {
                    if (!buffer.isDestroyed() && !buffer.initialize()) {
                        buffer.setNonCacheable();
                    }
                });
            }
            return buffer;
        }
    }

    // OpenGL thread
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean initialize() {
        if (mClientUploadBuffer) {
            return true;
        }
        GLDevice device = getDevice();
        boolean bufferStorage = device.getCaps().hasBufferStorageSupport();
        boolean dsa = device.getCaps().hasDSASupport();
        int buffer;
        if (dsa) {
            buffer = device.getGL().glCreateBuffers();
        } else {
            buffer = device.getGL().glGenBuffers();
        }
        if (buffer == 0) {
            return false;
        }
        int target = 0;
        if (!dsa) {
            target = getTarget();
            device.getGL().glBindBuffer(target, buffer);
        }
        boolean success;
        if (bufferStorage) {
            success = allocate(device, buffer, target, dsa);
        } else {
            success = allocateMutable(device, buffer, target, dsa);
        }
        if (success) {
            if (!dsa) {
                device.getGL().glBindBuffer(target, 0);
            }
            mBuffer = buffer;
        } else {
            device.getGL().glDeleteBuffers(buffer);
        }
        return success;
    }

    public static int getBufferStorageFlags(int usage) {
        int allocFlags;
        if ((usage & BufferUsageFlags.kReadback) != 0) {
            allocFlags = GL_MAP_READ_BIT |
                    GL_MAP_PERSISTENT_BIT |
                    GL_MAP_COHERENT_BIT |
                    GL_CLIENT_STORAGE_BIT;
        } else if ((usage & BufferUsageFlags.kHostVisible) != 0) {
            allocFlags = GL_MAP_WRITE_BIT |
                    GL_MAP_PERSISTENT_BIT |
                    GL_MAP_COHERENT_BIT;
            if ((usage & BufferUsageFlags.kUpload) != 0) {
                allocFlags |= GL_CLIENT_STORAGE_BIT;
            }
        } else if ((usage & BufferUsageFlags.kDeviceLocal) != 0) {
            // GPU only buffers, no flags
            allocFlags = 0;
        } else {
            assert false;
            allocFlags = GL_DYNAMIC_STORAGE_BIT;
        }
        return allocFlags;
    }

    // OpenGL thread
    private boolean allocate(GLDevice device, int buffer, int target, boolean dsa) {
        int allocFlags = getBufferStorageFlags(mUsage);

        boolean checkError = !device.getCaps().skipErrorChecks();
        if (checkError) {
            device.clearErrors();
        }
        if (dsa) {
            device.getGL().glNamedBufferStorage(buffer, mSize, NULL, allocFlags);
        } else {
            device.getGL().glBufferStorage(target, mSize, NULL, allocFlags);
        }
        if (checkError) {
            if (device.getError() != GL_NO_ERROR) {
                device.getLogger().error("Failed to create GLBuffer: cannot allocate {} bytes from device",
                        mSize);
                return false;
            }
        }

        if ((allocFlags & GL_MAP_PERSISTENT_BIT) != 0) {
            int mapFlags = allocFlags & (GL_MAP_READ_BIT |
                    GL_MAP_WRITE_BIT |
                    GL_MAP_PERSISTENT_BIT |
                    GL_MAP_COHERENT_BIT);
            long persistentlyMappedBuffer;
            if (dsa) {
                persistentlyMappedBuffer = device.getGL().glMapNamedBufferRange(
                        buffer, 0, mSize, mapFlags);
            } else {
                persistentlyMappedBuffer = device.getGL().glMapBufferRange(
                        target, 0, mSize, mapFlags);
            }
            if (persistentlyMappedBuffer == NULL) {
                device.getLogger().error("Failed to create GLBuffer: cannot create persistent mapping");
                return false;
            }
            mPersistentlyMappedBuffer = persistentlyMappedBuffer;
        }

        return true;
    }

    public static int getMutableBufferUsage(int usage) {
        int allocUsage;
        if ((usage & BufferUsageFlags.kReadback) != 0) {
            allocUsage = GL_DYNAMIC_READ;
        } else if ((usage & BufferUsageFlags.kHostVisible) != 0) {
            if ((usage & BufferUsageFlags.kDeviceLocal) != 0) {
                allocUsage = GL_DYNAMIC_DRAW;
            } else {
                allocUsage = GL_STREAM_DRAW;
            }
        } else if ((usage & BufferUsageFlags.kDeviceLocal) != 0) {
            allocUsage = GL_STATIC_DRAW;
        } else {
            assert false;
            allocUsage = GL_DYNAMIC_DRAW;
        }
        return allocUsage;
    }

    // OpenGL thread, legacy buffer allocation
    private boolean allocateMutable(GLDevice device, int buffer, int target, boolean dsa) {
        int allocUsage = getMutableBufferUsage(mUsage);

        boolean checkError = !device.getCaps().skipErrorChecks();
        if (checkError) {
            device.clearErrors();
        }
        if (dsa) {
            device.getGL().glNamedBufferData(buffer, mSize, NULL, allocUsage);
        } else {
            device.getGL().glBufferData(target, mSize, NULL, allocUsage);
        }
        if (checkError) {
            if (device.getError() != GL_NO_ERROR) {
                device.getLogger().error("Failed to create GLBuffer: cannot allocate {} bytes from device",
                        mSize);
                return false;
            }
        }

        return true;
    }

    public int getHandle() {
        return mBuffer;
    }

    // No persistent mapping support. Most write operations are done on non OpenGL threads
    // then we just use client array as staging buffer.
    public long getClientUploadBuffer() {
        return mClientUploadBuffer ? mCachedBuffer : NULL;
    }

    /**
     * Returns a default target for this buffer.
     */
    public int getTarget() {
        if ((mUsage & BufferUsageFlags.kStorage) != 0) {
            return GL_SHADER_STORAGE_BUFFER;
        } else if ((mUsage & BufferUsageFlags.kUniform) != 0) {
            return GL_UNIFORM_BUFFER;
        } else if ((mUsage & BufferUsageFlags.kDrawIndirect) != 0) {
            return GL_DRAW_INDIRECT_BUFFER;
        } else if ((mUsage & BufferUsageFlags.kReadback) != 0) {
            return GL_PIXEL_PACK_BUFFER;
        } else {
            // use ARRAY_BUFFER for kVertex, kIndex, kUpload
            // this is because GL_ELEMENT_ARRAY_BUFFER will be associated with the current VAO
            return GL_ARRAY_BUFFER;
        }
    }

    public int getBindingTarget() {
        if ((mUsage & BufferUsageFlags.kStorage) != 0) {
            return GL_SHADER_STORAGE_BUFFER_BINDING;
        } else if ((mUsage & BufferUsageFlags.kUniform) != 0) {
            return GL_UNIFORM_BUFFER_BINDING;
        } else if ((mUsage & BufferUsageFlags.kDrawIndirect) != 0) {
            return GL_DRAW_INDIRECT_BUFFER_BINDING;
        } else if ((mUsage & BufferUsageFlags.kReadback) != 0) {
            return GL_PIXEL_PACK_BUFFER_BINDING;
        } else {
            // use ARRAY_BUFFER for kVertex, kIndex, kUpload
            return GL_ARRAY_BUFFER_BINDING;
        }
    }

    @Override
    protected void onSetLabel(@Nullable String label) {
        getDevice().executeRenderCall(dev -> {
            if (dev.getCaps().hasDebugSupport() && !mClientUploadBuffer) {
                if (label == null) {
                    dev.getGL().glObjectLabel(GL_BUFFER, mBuffer, 0, NULL);
                } else {
                    String subLabel = label.substring(0, Math.min(label.length(),
                            dev.getCaps().maxLabelLength()));
                    dev.getGL().glObjectLabel(GL_BUFFER, mBuffer, subLabel);
                }
            }
        });
    }

    @Override
    protected void onRelease() {
        getDevice().executeRenderCall(dev -> {
            if (mBuffer != 0) {
                dev.getGL().glDeleteBuffers(mBuffer);
            }
            mBuffer = 0;
        });
        if (mCachedBuffer != NULL) {
            MemoryUtil.nmemFree(mCachedBuffer);
        }
        mCachedBuffer = NULL;
    }

    @Override
    protected GLDevice getDevice() {
        return (GLDevice) super.getDevice();
    }

    @Override
    protected long onMap(int mode, long offset, long size) {
        GLDevice device = getDevice();

        if (mode == kRead_MapMode) {
            if (mBuffer == 0) {
                return NULL;
            }
            assert (mBuffer != 0);
            assert (device.isOnExecutingThread());
            // prefer mapping, such as pixel buffer object
            long mappedBuffer;
            if (device.getCaps().hasDSASupport()) {
                mappedBuffer = device.getGL().glMapNamedBufferRange(mBuffer, offset, size, GL_MAP_READ_BIT);
            } else {
                int target = getTarget();
                device.getGL().glBindBuffer(target, mBuffer);
                mappedBuffer = device.getGL().glMapBufferRange(target, offset, size, GL_MAP_READ_BIT);
                device.getGL().glBindBuffer(target, 0);
            }
            if (mappedBuffer == NULL) {
                device.getLogger().error("Failed to map buffer {}", mBuffer);
            }
            return mappedBuffer;
        } else {
            assert (mode == kWriteDiscard_MapMode);
            if (mPersistentlyMappedBuffer != NULL) {
                return mPersistentlyMappedBuffer + offset;
            }
            // there are two cases: if persistent mapping is supported,
            // but we create a Buffer from other threads, then we write to a CPU buffer
            // for the first time, and free the CPU buffer as soon as possible
            // so future writes directly go to the 'mPersistentlyMappedBuffer'.
            // otherwise, we use legacy mutable buffer and prefer CPU staging buffer
            if (mCachedBuffer == NULL || mCachedBufferSize < size) {
                if (mCachedBuffer == NULL) {
                    mCachedBuffer = MemoryUtil.nmemAlloc(size);
                } else {
                    mCachedBuffer = MemoryUtil.nmemRealloc(mCachedBuffer, size);
                }
                mCachedBufferSize = size;
                if (mCachedBuffer == NULL) {
                    device.getLogger().error("Failed to map buffer {}", this);
                }
            }
            return mCachedBuffer;
        }
    }

    @Override
    protected void onUnmap(int mode, long offset, long size) {
        GLDevice device = getDevice();

        if (mode == kRead_MapMode) {
            if (mBuffer == 0) {
                return;
            }
            assert (mBuffer != 0);
            assert (device.isOnExecutingThread());
            if (device.getCaps().hasDSASupport()) {
                device.getGL().glUnmapNamedBuffer(mBuffer);
            } else {
                int target = getTarget();
                device.getGL().glBindBuffer(target, mBuffer);
                device.getGL().glUnmapBuffer(target);
                device.getGL().glBindBuffer(target, 0);
            }
        } else {
            assert (mode == kWriteDiscard_MapMode);
            if (mPersistentlyMappedBuffer != NULL || mClientUploadBuffer) {
                // always coherent, noop
                return;
            }
            if (size == 0) {
                return;
            }
            device.executeRenderCall(dev -> {
                if (mBuffer == 0) {
                    return;
                }
                if (mPersistentlyMappedBuffer != NULL) {
                    if (mCachedBuffer != NULL) {
                        MemoryUtil.memCopy(/*src*/mCachedBuffer,
                                /*dst*/mPersistentlyMappedBuffer + offset, size);
                        MemoryUtil.nmemFree(mCachedBuffer);
                        mCachedBuffer = NULL;
                        // later we write to persistently mapped buffer, free CPU buffer now
                    }
                } else if (mCachedBuffer != NULL) {
                    int target = dev.getCaps().hasDSASupport()
                            ? 0
                            : getTarget();
                    // we already do synchronization, so no buffer orphaning or invalidation
                    doUploadData(dev, mBuffer, target, mCachedBuffer, offset, size);
                }
                // else some allocation failed
            });
        }
    }

    // restricted to 256KB per update
    private static final long MAX_BYTES_PER_UPDATE = 1 << 18;

    public static void doUploadData(GLDevice device, int buffer, int target,
                                    long data, long offset, long totalSize) {
        GLInterface gl = device.getGL();
        if (target == 0) {
            while (totalSize > 0) {
                long size = Math.min(MAX_BYTES_PER_UPDATE, totalSize);
                gl.glNamedBufferSubData(buffer, offset, size, data);
                data += size;
                offset += size;
                totalSize -= size;
            }
        } else {
            gl.glBindBuffer(target, buffer);
            while (totalSize > 0) {
                long size = Math.min(MAX_BYTES_PER_UPDATE, totalSize);
                gl.glBufferSubData(target, offset, size, data);
                data += size;
                offset += size;
                totalSize -= size;
            }
        }
    }
}
