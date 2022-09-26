/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.opengl;

import icyllis.arcticgi.core.SharedPtr;
import icyllis.arcticgi.engine.Buffer;

import javax.annotation.Nullable;

import static icyllis.arcticgi.engine.Engine.*;
import static icyllis.arcticgi.opengl.GLCore.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class GLBuffer extends Buffer {

    private int mBuffer;

    public GLBuffer(GLServer server,
                    int size,
                    int bufferType,
                    int accessPattern,
                    int buffer,
                    long mapPtr) {
        super(server, size, bufferType, accessPattern);
        mBuffer = buffer;
        mMapPtr = mapPtr;

        registerWithCache(true);
    }

    @Nullable
    @SharedPtr
    public static GLBuffer make(GLServer server,
                                int size,
                                int bufferType,
                                int accessPattern) {
        assert (size > 0);
        assert (checkGpuBufferType(bufferType));
        assert (checkAccessPattern(accessPattern));

        int flags = 0;
        switch (bufferType) {
            case GpuBufferType_Vertex, GpuBufferType_Index -> {
                assert (accessPattern == AccessPattern_Static
                        || accessPattern == AccessPattern_Stream);
                flags |= GL_DYNAMIC_STORAGE_BIT;
            }
            case GpuBufferType_Uniform -> {
                assert (accessPattern == AccessPattern_Dynamic);
                flags |= GL_DYNAMIC_STORAGE_BIT;
            }
            case GpuBufferType_XferSrcToDst -> {
                assert (accessPattern == AccessPattern_Dynamic);
                flags |= GL_MAP_WRITE_BIT
                        | GL_MAP_PERSISTENT_BIT
                        | GL_MAP_COHERENT_BIT;
            }
            case GpuBufferType_XferDstToSrc -> {
                assert (accessPattern == AccessPattern_Dynamic
                        || accessPattern == AccessPattern_Stream);
                flags |= GL_MAP_READ_BIT;
            }
        }

        int buffer = glCreateBuffers();
        if (buffer == 0) {
            return null;
        }
        if (server.mCaps.skipErrorChecks()) {
            glNamedBufferStorage(buffer, size, flags);
        } else {
            glClearErrors();
            glNamedBufferStorage(buffer, size, flags);
            if (glGetError() != GL_NO_ERROR) {
                glDeleteBuffers(buffer);
                return null;
            }
        }
        long mapPtr = NULL;
        if ((flags & GL_MAP_PERSISTENT_BIT) != 0) {
            mapPtr = nglMapNamedBufferRange(buffer, 0, size, GL_MAP_WRITE_BIT
                    | GL_MAP_PERSISTENT_BIT
                    | GL_MAP_COHERENT_BIT);
            if (mapPtr == NULL) {
                glDeleteBuffers(buffer);
                return null;
            }
        }

        return new GLBuffer(server, size, bufferType, accessPattern, buffer, mapPtr);
    }

    public int getBuffer() {
        return mBuffer;
    }

    @Override
    protected void onFree() {
        if (mBuffer != 0) {
            glDeleteBuffers(mBuffer);
            mBuffer = 0;
        }
        mMapPtr = NULL;
    }

    @Override
    protected void onDrop() {
        mBuffer = 0;
        mMapPtr = NULL;
    }

    @Override
    protected void onMap() {
        assert (mBuffer != 0);
        assert (!wasDestroyed());
        assert (!isMapped());

        assert (bufferType() == GpuBufferType_XferDstToSrc);
        mMapPtr = nglMapNamedBufferRange(mBuffer, 0, size(), GL_MAP_READ_BIT);
    }

    @Override
    protected void onUnmap() {
        assert (bufferType() == GpuBufferType_XferDstToSrc);
        glUnmapNamedBuffer(mBuffer);
        // read only, no flush
    }

    @Override
    protected boolean onUpdateData(long data, int offset, int size) {
        final int buffer = mBuffer;
        assert (buffer != 0);
        if (accessPattern() != AccessPattern_Static) {
            glInvalidateBufferSubData(buffer, offset, size);
        }
        while (size > 0) {
            int bufferSize = Math.min(1 << 18, size);

            nglNamedBufferSubData(buffer, offset, bufferSize, data);

            data += bufferSize;
            offset += bufferSize;
            size -= bufferSize;
        }
        return true;
    }
}
