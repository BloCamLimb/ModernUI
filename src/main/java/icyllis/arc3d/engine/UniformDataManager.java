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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.shading.UniformHandler.UniformHandle;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * Manages the resources used by a shader program. The resources are objects the program uses
 * to communicate with the application code.
 * <p>
 * The {@link UniformDataManager} is used to store uniforms for a program in a CPU buffer that
 * can be uploaded to a UBO. This currently assumes uniform layouts that are compatible with
 * OpenGL, Vulkan, and D3D12. It could be used more broadly if this aspect was made configurable.
 * <p>
 * The default implementation assumes the block uses std140 layout. For Vulkan, if push-constants
 * is used, subclasses should be configured to std430 layout. Uniforms here means that they can
 * be uploaded to UBO, and does not include opaque types such as samplers, which are updated
 * separately from this class.
 *
 * @see icyllis.arc3d.engine.shading.UniformHandler
 */
public abstract class UniformDataManager extends RefCnt {

    // lower 24 bits: offset in bytes
    // higher 8 bits: SLType (assert)
    protected final int[] mUniforms;

    protected final int mUniformSize;
    protected final long mUniformData;

    protected boolean mUniformsDirty;

    /**
     * Constructor.
     *
     * @param uniformCount the number of uniforms
     * @param uniformSize  the uniform block size in bytes
     */
    public UniformDataManager(int uniformCount, int uniformSize) {
        assert (uniformCount >= 1 && uniformSize >= 4);
        mUniforms = new int[uniformCount];
        mUniformSize = uniformSize;
        mUniformData = nmemAllocChecked(uniformSize);
        mUniformsDirty = false;
        assert (MathUtil.isAlign4(uniformSize));
        assert (MathUtil.isAlign4(mUniformData));
        // subclasses fill in the uniforms in their constructor
    }

    @Override
    protected void deallocate() {
        nmemFree(mUniformData);
    }

    /**
     * Specifies the value of an int, uint or bool uniform variable for the current program object.
     */
    public void set1i(@UniformHandle int u, int v0) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kInt ||
                (uni >> 24) == SLType.kUInt ||
                (uni >> 24) == SLType.kBool);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memPutInt(buffer, v0);
    }

    /**
     * Specifies the value of a single int, uint or bool uniform variable or an int, uint or bool
     * uniform variable array for the current program object.
     *
     * @param count the number of elements that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array, and 1 or more if it is an array
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    public void set1iv(@UniformHandle int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kInt ||
                (uni >> 24) == SLType.kUInt ||
                (uni >> 24) == SLType.kBool);
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

    /**
     * Array version of {@link #set1iv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    public void set1iv(@UniformHandle int u, int offset, int count, int[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kInt ||
                (uni >> 24) == SLType.kUInt ||
                (uni >> 24) == SLType.kBool);
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

    /**
     * Specifies the value of a float uniform variable for the current program object.
     */
    public void set1f(@UniformHandle int u, float v0) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memPutFloat(buffer, v0);
    }

    /**
     * Specifies the value of a single float uniform variable or a float uniform variable array
     * for the current program object.
     *
     * @param count the number of elements that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array, and 1 or more if it is an array
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    public void set1fv(@UniformHandle int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat);
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

    /**
     * Array version of {@link #set1fv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    public void set1fv(@UniformHandle int u, int offset, int count, float[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat);
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

    /**
     * Specifies the value of an ivec2, uvec2 or bvec2 uniform variable for the current program object.
     */
    public void set2i(@UniformHandle int u, int v0, int v1) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kInt2 ||
                (uni >> 24) == SLType.kUInt2 ||
                (uni >> 24) == SLType.kBool2);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memPutInt(buffer, v0);
        memPutInt(buffer + Float.BYTES, v1);
    }

    /**
     * Specifies the value of a single ivec2, uvec2 or bvec2  uniform variable or an ivec2, uvec2 or bvec2
     * uniform variable array for the current program object.
     *
     * @param count the number of elements that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array, and 1 or more if it is an array
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    public void set2iv(@UniformHandle int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kInt2 ||
                (uni >> 24) == SLType.kUInt2 ||
                (uni >> 24) == SLType.kBool2);
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

    /**
     * Array version of {@link #set2iv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    public void set2iv(@UniformHandle int u, int offset, int count, int[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kInt2 ||
                (uni >> 24) == SLType.kUInt2 ||
                (uni >> 24) == SLType.kBool2);
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

    /**
     * Specifies the value of a vec2 uniform variable for the current program object.
     */
    public void set2f(@UniformHandle int u, float v0, float v1) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat2);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memPutFloat(buffer, v0);
        memPutFloat(buffer + Float.BYTES, v1);
    }

    /**
     * Specifies the value of a single vec2 uniform variable or a vec2 uniform variable array
     * for the current program object.
     *
     * @param count the number of elements that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array, and 1 or more if it is an array
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    public void set2fv(@UniformHandle int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat2);
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

    /**
     * Array version of {@link #set2fv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    public void set2fv(@UniformHandle int u, int offset, int count, float[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat2);
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

    /**
     * Specifies the value of an ivec3, uvec3 or bvec3 uniform variable for the current program object.
     */
    public void set3i(@UniformHandle int u, int v0, int v1, int v2) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kInt3 ||
                (uni >> 24) == SLType.kUInt3 ||
                (uni >> 24) == SLType.kBool3);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memPutInt(buffer, v0);
        memPutInt(buffer + Float.BYTES, v1);
        memPutInt(buffer + 2 * Float.BYTES, v2);
    }

    /**
     * Specifies the value of a single ivec3, uvec3 or bvec3 uniform variable or an ivec3, uvec3 or bvec3
     * uniform variable array for the current program object.
     *
     * @param count the number of elements that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array, and 1 or more if it is an array
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    public void set3iv(@UniformHandle int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kInt3 ||
                (uni >> 24) == SLType.kUInt3 ||
                (uni >> 24) == SLType.kBool3);
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

    /**
     * Array version of {@link #set3iv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    public void set3iv(@UniformHandle int u, int offset, int count, int[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kInt3 ||
                (uni >> 24) == SLType.kUInt3 ||
                (uni >> 24) == SLType.kBool3);
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

    /**
     * Specifies the value of a vec3 uniform variable for the current program object.
     */
    public void set3f(@UniformHandle int u, float v0, float v1, float v2) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat3);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memPutFloat(buffer, v0);
        memPutFloat(buffer + Float.BYTES, v1);
        memPutFloat(buffer + 2 * Float.BYTES, v2);
    }

    /**
     * Specifies the value of a single vec3 uniform variable or a vec3 uniform variable array
     * for the current program object.
     *
     * @param count the number of elements that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array, and 1 or more if it is an array
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    public void set3fv(@UniformHandle int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat3);
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

    /**
     * Array version of {@link #set3fv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    public void set3fv(@UniformHandle int u, int offset, int count, float[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat3);
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

    /**
     * Specifies the value of an ivec4, uvec4 or bvec4 uniform variable for the current program object.
     */
    public void set4i(@UniformHandle int u, int v0, int v1, int v2, int v3) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kInt4 ||
                (uni >> 24) == SLType.kUInt4 ||
                (uni >> 24) == SLType.kBool4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memPutInt(buffer, v0);
        memPutInt(buffer + Float.BYTES, v1);
        memPutInt(buffer + 2 * Float.BYTES, v2);
        memPutInt(buffer + 3 * Float.BYTES, v3);
    }

    /**
     * Specifies the value of a single ivec4, uvec4 or bvec4 uniform variable or an ivec4, uvec4 or bvec4
     * uniform variable array for the current program object.
     *
     * @param count the number of elements that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array, and 1 or more if it is an array
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    public void set4iv(@UniformHandle int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kInt4 ||
                (uni >> 24) == SLType.kUInt4 ||
                (uni >> 24) == SLType.kBool4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memCopy(value, buffer, count * 4L * Float.BYTES);
    }

    /**
     * Array version of {@link #set4iv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    public void set4iv(@UniformHandle int u, int offset, int count, int[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kInt4 ||
                (uni >> 24) == SLType.kUInt4 ||
                (uni >> 24) == SLType.kBool4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0, e = count * 4; i < e; i++) {
            memPutInt(buffer, value[offset++]);
            buffer += Float.BYTES;
        }
    }

    /**
     * Specifies the value of a vec4 uniform variable for the current program object.
     */
    public void set4f(@UniformHandle int u, float v0, float v1, float v2, float v3) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memPutFloat(buffer, v0);
        memPutFloat(buffer + Float.BYTES, v1);
        memPutFloat(buffer + 2 * Float.BYTES, v2);
        memPutFloat(buffer + 3 * Float.BYTES, v3);
    }

    /**
     * Specifies the value of a single vec4 uniform variable or a vec4 uniform variable array
     * for the current program object.
     *
     * @param count the number of elements that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array, and 1 or more if it is an array
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    public void set4fv(@UniformHandle int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memCopy(value, buffer, count * 4L * Float.BYTES);
    }

    /**
     * Array version of {@link #set4fv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    public void set4fv(@UniformHandle int u, int offset, int count, float[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0, e = count * 4; i < e; i++) {
            memPutFloat(buffer, value[offset++]);
            buffer += Float.BYTES;
        }
    }

    /**
     * Specifies the value of a single mat2 uniform variable or a mat2 uniform variable array
     * for the current program object. Matrices are column-major.
     *
     * @param count the number of matrices that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array of matrices, and 1 or more if it is an array of matrices.
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    public void setMatrix2fv(@UniformHandle int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat2x2);
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

    /**
     * Array version of {@link #setMatrix2fv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    public void setMatrix2fv(@UniformHandle int u, int offset, int count, float[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat2x2);
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

    /**
     * Specifies the value of a single mat3 uniform variable or a mat3 uniform variable array
     * for the current program object. Matrices are column-major.
     *
     * @param count the number of matrices that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array of matrices, and 1 or more if it is an array of matrices.
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    public void setMatrix3fv(@UniformHandle int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat3x3);
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

    /**
     * Array version of {@link #setMatrix3fv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    public void setMatrix3fv(@UniformHandle int u, int offset, int count, float[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat3x3);
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

    /**
     * Specifies the value of a single mat4 uniform variable or a mat4 uniform variable array
     * for the current program object. Matrices are column-major.
     *
     * @param count the number of matrices that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array of matrices, and 1 or more if it is an array of matrices.
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    public void setMatrix4fv(@UniformHandle int u, int count, long value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat4x4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        memCopy(value, buffer, count * 4L * 4L * Float.BYTES);
    }

    /**
     * Array version of {@link #setMatrix4fv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    public void setMatrix4fv(@UniformHandle int u, int offset, int count, float[] value) {
        assert (count > 0);
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat4x4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        for (int i = 0, e = count * 4 * 4; i < e; i++) {
            memPutFloat(buffer, value[offset++]);
            buffer += Float.BYTES;
        }
    }

    /**
     * Convenience method for uploading a Matrix3 to a 3x3 matrix uniform.
     */
    public void setMatrix3f(@UniformHandle int u, Matrix3 matrix) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat3x3);
        long buffer = getBufferPtrAndMarkDirty(uni);
        matrix.storeAligned(buffer);
    }

    /**
     * Convenience method for uploading a Matrix4 to a 4x4 matrix uniform.
     */
    public void setMatrix4f(@UniformHandle int u, Matrix4 matrix) {
        int uni = mUniforms[u];
        assert ((uni >> 24) == SLType.kFloat4x4);
        long buffer = getBufferPtrAndMarkDirty(uni);
        matrix.store(buffer);
    }

    protected long getBufferPtrAndMarkDirty(int uni) {
        mUniformsDirty = true;
        // lower 24 bits: offset
        return mUniformData + (uni & 0xFFFFFF);
    }
}
