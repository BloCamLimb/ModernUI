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

package icyllis.arctic.engine;

import icyllis.arctic.core.*;
import icyllis.arctic.engine.shading.ProgramDataManager;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * Subclass of {@link ProgramDataManager} used to store uniforms for a program in a CPU buffer that
 * can be uploaded to a UBO. This currently assumes uniform layouts that are compatible with
 * OpenGL, Vulkan, and D3D12. It could be used more broadly if this aspect was made configurable.
 * <p>
 * The default implementation assumes uniforms are std140 layout. For vulkan, if push constants is
 * used, subclasses should be configured to std430 layout.
 * <p>
 * Uniforms here means that they can be uploaded to UBO, and does not include uniforms such as
 * samplers, which are uploaded separately from this class.
 *
 * @see icyllis.arctic.engine.shading.UniformHandler
 */
public abstract class UniformDataManager implements ProgramDataManager, AutoCloseable {

    // lower 24 bits: offset, higher 8 bits: SLType (debug)
    protected final int[] mUniforms;
    protected final int mUniformSize;
    protected final long mUniformData;
    protected boolean mUniformsDirty;

    /**
     * @param uniformCount the number of uniforms
     * @param uniformSize  the uniform block size in bytes
     */
    public UniformDataManager(int uniformCount, int uniformSize) {
        assert (uniformCount >= 1 && uniformSize >= Float.BYTES);
        mUniforms = new int[uniformCount];
        mUniformSize = uniformSize;
        // it can be faster for transferring small values if dword aligned
        mUniformData = nmemAlignedAllocChecked(Long.BYTES, uniformSize);
        mUniformsDirty = false;
        // subclasses fill in the uniforms in their constructor
    }

    @Override
    public void set1i(int u, int v0) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Int ||
                (uni >> 24) == SLType.UInt ||
                (uni >> 24) == SLType.Bool);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memPutInt(buffer, v0);
    }

    @Override
    public void set1iv(int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Int ||
                (uni >> 24) == SLType.UInt ||
                (uni >> 24) == SLType.Bool);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0; ; ) {
            // for small values this is faster than memCopy
            memPutInt(buffer, memGetInt(value));
            if (++i < count) {
                buffer += 4 * Float.BYTES; // 4N
                value += Integer.BYTES;
            } else {
                return;
            }
        }
    }

    @Override
    public void set1iv(int u, int offset, int count, int[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Int ||
                (uni >> 24) == SLType.UInt ||
                (uni >> 24) == SLType.Bool);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0; ; ) {
            memPutInt(buffer, value[offset + i]);
            if (++i < count) {
                buffer += 4 * Float.BYTES; // 4N
            } else {
                return;
            }
        }
    }

    @Override
    public void set1f(int u, float v0) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Float);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memPutFloat(buffer, v0);
    }

    @Override
    public void set1fv(int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Float);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0; ; ) {
            // for small values this is faster than memCopy
            memPutInt(buffer, memGetInt(value));
            if (++i < count) {
                buffer += 4 * Float.BYTES; // 4N
                value += Integer.BYTES;
            } else {
                return;
            }
        }
    }

    @Override
    public void set1fv(int u, int offset, int count, float[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Float);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0; ; ) {
            memPutFloat(buffer, value[offset + i]);
            if (++i < count) {
                buffer += 4 * Float.BYTES; // 4N
            } else {
                return;
            }
        }
    }

    @Override
    public void set2i(int u, int v0, int v1) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.IVec2 ||
                (uni >> 24) == SLType.UVec2 ||
                (uni >> 24) == SLType.BVec2);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memPutInt(buffer, v0);
        memPutInt(buffer + Float.BYTES, v1);
    }

    @Override
    public void set2iv(int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.IVec2 ||
                (uni >> 24) == SLType.UVec2 ||
                (uni >> 24) == SLType.BVec2);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0; ; ) {
            // for small values this is faster than memCopy
            memPutLong(buffer, memGetLong(value));
            if (++i < count) {
                buffer += 4 * Float.BYTES; // 4N
                value += 2 * Integer.BYTES;
            } else {
                return;
            }
        }
    }

    @Override
    public void set2iv(int u, int offset, int count, int[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.IVec2 ||
                (uni >> 24) == SLType.UVec2 ||
                (uni >> 24) == SLType.BVec2);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0; ; ) {
            memPutInt(buffer, value[offset]);
            memPutInt(buffer + Float.BYTES, value[offset + 1]);
            if (++i < count) {
                buffer += 4 * Float.BYTES; // 4N
                offset += 2;
            } else {
                return;
            }
        }
    }

    @Override
    public void set2f(int u, float v0, float v1) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Vec2);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memPutFloat(buffer, v0);
        memPutFloat(buffer + Float.BYTES, v1);
    }

    @Override
    public void set2fv(int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Vec2);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0; ; ) {
            // for small values this is faster than memCopy
            memPutLong(buffer, memGetLong(value));
            if (++i < count) {
                buffer += 4 * Float.BYTES; // 4N
                value += 2 * Integer.BYTES;
            } else {
                return;
            }
        }
    }

    @Override
    public void set2fv(int u, int offset, int count, float[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Vec2);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0; ; ) {
            memPutFloat(buffer, value[offset]);
            memPutFloat(buffer + Float.BYTES, value[offset + 1]);
            if (++i < count) {
                buffer += 4 * Float.BYTES; // 4N
                offset += 2;
            } else {
                return;
            }
        }
    }

    @Override
    public void set3i(int u, int v0, int v1, int v2) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.IVec3 ||
                (uni >> 24) == SLType.UVec3 ||
                (uni >> 24) == SLType.BVec3);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memPutInt(buffer, v0);
        memPutInt(buffer + Float.BYTES, v1);
        memPutInt(buffer + 2 * Float.BYTES, v2);
    }

    @Override
    public void set3iv(int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.IVec3 ||
                (uni >> 24) == SLType.UVec3 ||
                (uni >> 24) == SLType.BVec3);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0; ; ) {
            // for small values this is faster than memCopy
            memPutLong(buffer, memGetLong(value));
            memPutInt(buffer + 2 * Float.BYTES, memGetInt(value + 2 * Integer.BYTES));
            if (++i < count) {
                buffer += 4 * Float.BYTES; // 4N
                value += 3 * Integer.BYTES;
            } else {
                return;
            }
        }
    }

    @Override
    public void set3iv(int u, int offset, int count, int[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.IVec3 ||
                (uni >> 24) == SLType.UVec3 ||
                (uni >> 24) == SLType.BVec3);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0; ; ) {
            memPutInt(buffer, value[offset]);
            memPutInt(buffer + Float.BYTES, value[offset + 1]);
            memPutInt(buffer + 2 * Float.BYTES, value[offset + 2]);
            if (++i < count) {
                buffer += 4 * Float.BYTES; // 4N
                offset += 3;
            } else {
                return;
            }
        }
    }

    @Override
    public void set3f(int u, float v0, float v1, float v2) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Vec3);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memPutFloat(buffer, v0);
        memPutFloat(buffer + Float.BYTES, v1);
        memPutFloat(buffer + 2 * Float.BYTES, v2);
    }

    @Override
    public void set3fv(int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Vec3);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0; ; ) {
            // for small values this is faster than memCopy
            memPutLong(buffer, memGetLong(value));
            memPutInt(buffer + 2 * Float.BYTES, memGetInt(value + 2 * Integer.BYTES));
            if (++i < count) {
                buffer += 4 * Float.BYTES; // 4N
                value += 3 * Integer.BYTES;
            } else {
                return;
            }
        }
    }

    @Override
    public void set3fv(int u, int offset, int count, float[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Vec3);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0; ; ) {
            memPutFloat(buffer, value[offset]);
            memPutFloat(buffer + Float.BYTES, value[offset + 1]);
            memPutFloat(buffer + 2 * Float.BYTES, value[offset + 2]);
            if (++i < count) {
                buffer += 4 * Float.BYTES; // 4N
                offset += 3;
            } else {
                return;
            }
        }
    }

    @Override
    public void set4i(int u, int v0, int v1, int v2, int v3) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.IVec4 ||
                (uni >> 24) == SLType.UVec4 ||
                (uni >> 24) == SLType.BVec4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memPutInt(buffer, v0);
        memPutInt(buffer + Float.BYTES, v1);
        memPutInt(buffer + 2 * Float.BYTES, v2);
        memPutInt(buffer + 3 * Float.BYTES, v3);
    }

    @Override
    public void set4iv(int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.IVec4 ||
                (uni >> 24) == SLType.UVec4 ||
                (uni >> 24) == SLType.BVec4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memCopy(value, buffer, count * 4L * Float.BYTES);
    }

    @Override
    public void set4iv(int u, int offset, int count, int[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.IVec4 ||
                (uni >> 24) == SLType.UVec4 ||
                (uni >> 24) == SLType.BVec4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0, e = count * 4; i < e; i++) {
            memPutInt(buffer, value[offset++]);
            buffer += Float.BYTES;
        }
    }

    @Override
    public void set4f(int u, float v0, float v1, float v2, float v3) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Vec4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memPutFloat(buffer, v0);
        memPutFloat(buffer + Float.BYTES, v1);
        memPutFloat(buffer + 2 * Float.BYTES, v2);
        memPutFloat(buffer + 3 * Float.BYTES, v3);
    }

    @Override
    public void set4fv(int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Vec4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memCopy(value, buffer, count * 4L * Float.BYTES);
    }

    @Override
    public void set4fv(int u, int offset, int count, float[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Vec4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0, e = count * 4; i < e; i++) {
            memPutFloat(buffer, value[offset++]);
            buffer += Float.BYTES;
        }
    }

    @Override
    public void setMatrix2fv(int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Mat2);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0; ; ) {
            memPutLong(buffer, memGetLong(value));
            memPutLong(buffer + 4 * Float.BYTES, memGetLong(value + 2 * Integer.BYTES));
            if (++i < count) {
                buffer += 2 * 4 * Float.BYTES; // 4N with std140
                value += 2 * 2 * Integer.BYTES;
            } else {
                return;
            }
        }
    }

    @Override
    public void setMatrix2fv(int u, int offset, int count, float[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Mat2);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0; ; ) {
            memPutFloat(buffer, value[offset]);
            memPutFloat(buffer + Float.BYTES, value[offset + 1]);
            memPutFloat(buffer + 4 * Float.BYTES, value[offset + 2]);
            memPutFloat(buffer + 5 * Float.BYTES, value[offset + 3]);
            if (++i < count) {
                buffer += 2 * 4 * Float.BYTES; // 4N with std140
                offset += 2 * 2;
            } else {
                return;
            }
        }
    }

    @Override
    public void setMatrix3fv(int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Mat3);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0; ; ) {
            memPutLong(buffer, memGetLong(value));
            memPutInt(buffer + 2 * Float.BYTES, memGetInt(value + 2 * Integer.BYTES));
            memPutLong(buffer + 4 * Float.BYTES, memGetLong(value + 3 * Integer.BYTES));
            memPutInt(buffer + 6 * Float.BYTES, memGetInt(value + 5 * Integer.BYTES));
            memPutLong(buffer + 8 * Float.BYTES, memGetLong(value + 6 * Integer.BYTES));
            memPutInt(buffer + 10 * Float.BYTES, memGetInt(value + 8 * Integer.BYTES));
            if (++i < count) {
                buffer += 3 * 4 * Float.BYTES; // 4N
                value += 3 * 3 * Integer.BYTES;
            } else {
                return;
            }
        }
    }

    @Override
    public void setMatrix3fv(int u, int offset, int count, float[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Mat3);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0; ; ) {
            memPutFloat(buffer, value[offset]);
            memPutFloat(buffer + Float.BYTES, value[offset + 1]);
            memPutFloat(buffer + 2 * Float.BYTES, value[offset + 2]);
            memPutFloat(buffer + 4 * Float.BYTES, value[offset + 3]);
            memPutFloat(buffer + 5 * Float.BYTES, value[offset + 4]);
            memPutFloat(buffer + 6 * Float.BYTES, value[offset + 5]);
            memPutFloat(buffer + 8 * Float.BYTES, value[offset + 6]);
            memPutFloat(buffer + 9 * Float.BYTES, value[offset + 7]);
            memPutFloat(buffer + 10 * Float.BYTES, value[offset + 8]);
            if (++i < count) {
                buffer += 3 * 4 * Float.BYTES; // 4N
                offset += 3 * 3;
            } else {
                return;
            }
        }
    }

    @Override
    public void setMatrix4fv(int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Mat4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memCopy(value, buffer, count * 4L * 4L * Float.BYTES);
    }

    @Override
    public void setMatrix4fv(int u, int offset, int count, float[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Mat4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0, e = count * 4 * 4; i < e; i++) {
            memPutFloat(buffer, value[offset++]);
            buffer += Float.BYTES;
        }
    }

    @Override
    public void setMatrix3f(int u, Matrix3 matrix) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Mat3);
        long buffer = getBufferPtrAndMarkDirty(uni);
        matrix.putAligned(buffer);
    }

    @Override
    public void setMatrix4f(int u, Matrix4 matrix) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.Mat4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        matrix.put(buffer);
    }

    @Override
    public void close() {
        nmemAlignedFree(mUniformData);
    }

    protected long getBufferPtrAndMarkDirty(int uni) {
        mUniformsDirty = true;
        // lower 24 bits: offset
        return mUniformData + (uni & 0xFFFFFF);
    }
}
