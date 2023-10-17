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

import icyllis.arc3d.engine.GPUManagedResource;
import icyllis.arc3d.core.SharedPtr;

import javax.annotation.Nullable;

import static icyllis.arc3d.opengl.GLCore.*;

public class GLUniformBuffer extends GPUManagedResource {

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

            device.currentCommandBuffer().bindUniformBuffer(this);

            if (device.getCaps().skipErrorChecks()) {
                glBufferData(GL_UNIFORM_BUFFER, size, GL_DYNAMIC_DRAW);
            } else {
                glClearErrors();
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
                glClearErrors();
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
