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

import icyllis.arc3d.engine.ManagedResource;
import icyllis.arc3d.core.SharedPtr;

import javax.annotation.Nullable;

import static org.lwjgl.opengl.GL45C.*;

//TODO
// We will introduce 4 ways for streaming UBO (which is updated-per-draw or updated-per-frame).
// 1. Ring Buffer (OpenGL 4.4 Persistent Mapping + Manual Sync)
//    map with MAP_PERSISTENT_BIT | MAP_COHERENT_BIT
//    This is the technology used in D3D11 and Vulkan
//    Possibly create with CLIENT_STORAGE_BIT on Mesa driver (it should be PCIe pinned memory)
// 2. Legacy Ring Buffer (before OpenGL 4.4 non-persistent mapping)
//    map with MAP_INVALIDATE_RANGE_BIT | MAP_UNSYNCHRONIZED_BIT with small block size
//    Use UnmapBuffer or FlushMappedBufferRange to flush, and map next small block
//    Don't sync, use null data or invalidate buffer if whole buffer wraps
//    This should be good but may be slow on some drivers, because map bits are just hints
// 3. BufferSubData multiple times on the same buffer with similar size (alignment 128)
//    Create buffer with STREAM_DRAW or STATIC_DRAW, one buffer one binding
//    This should be good, but is implementation-dependent
// 4. BufferSubData with pool + triple buffering + implicit sync
//    Since BufferSubData may block for sync, use 3 or 4 buckets of uniform buffers
//    and assume they are signaled and free to use again after 3 or 4 frames (submits / SwapBuffers)
//    each bucket is a list of uniform buffers that have similar sizes
// For non-streaming UBO (updated occasionally), use method 3 or method 4
// just differ in STREAM_DRAW or STATIC_DRAW hint
// The first two methods should be avoided on OpenGL ES
// If method 3 is slow, use method 4 (best compatibility, lowest priority)
@Deprecated
public class GLUniformBuffer extends ManagedResource {

    private final int mBinding;
    private final int mSize;
    private int mBuffer;

    private GLUniformBuffer(GLDevice device,
                            int binding,
                            int size,
                            int buffer) {
        super(device);
        mSize = size;
        mBuffer = buffer;
        mBinding = binding;

        // OpenGL 3.3 uses mutable allocation
        if (!device.getCaps().hasDSASupport()) {

            //device.currentCommandBuffer().bindUniformBuffer(this);

            if (device.getCaps().skipErrorChecks()) {
                glBufferData(GL_UNIFORM_BUFFER, size, GL_DYNAMIC_DRAW);
            } else {
                //glClearErrors();
                glBufferData(GL_UNIFORM_BUFFER, size, GL_DYNAMIC_DRAW);
                if (glGetError() != GL_NO_ERROR) {
                    glDeleteBuffers(mBuffer);
                    mBuffer = 0;
                }
            }
        }
    }

    /**
     * @param binding the index of the uniform block binding point
     */
    @Nullable
    @SharedPtr
    public static GLUniformBuffer make(GLDevice device,
                                       int size,
                                       int binding) {
        assert (size > 0);

        if (device.getCaps().hasDSASupport()) {
            int buffer = glCreateBuffers();
            if (buffer == 0) {
                return null;
            }
            if (device.getCaps().skipErrorChecks()) {
                glNamedBufferStorage(buffer, size, GL_DYNAMIC_STORAGE_BIT);
            } else {
                //glClearErrors();
                glNamedBufferStorage(buffer, size, GL_DYNAMIC_STORAGE_BIT);
                if (glGetError() != GL_NO_ERROR) {
                    glDeleteBuffers(buffer);
                    return null;
                }
            }

            return new GLUniformBuffer(device, binding, size, buffer);
        } else {
            int buffer = glGenBuffers();
            if (buffer == 0) {
                return null;
            }

            GLUniformBuffer res = new GLUniformBuffer(device, binding, size, buffer);
            if (res.mBuffer == 0) {
                res.unref();
                return null;
            }

            return res;
        }
    }

    @Override
    protected void deallocate() {
        if (mBuffer != 0) {
            glDeleteBuffers(mBuffer);
        }
        mBuffer = 0;
    }

    public void discard() {
        mBuffer = 0;
    }

    public int getSize() {
        return mSize;
    }

    public int getHandle() {
        return mBuffer;
    }

    public int getBinding() {
        return mBinding;
    }
}
