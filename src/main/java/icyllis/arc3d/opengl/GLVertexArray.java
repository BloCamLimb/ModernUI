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

import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL30C.*;

/**
 * This class manages the lifetime of the vertex array object and is used to track the state of the
 * vertex array to avoid redundant GL calls.
 * <p>
 * The implementation attempts to utilize ARB_vertex_attrib_binding that is available in OpenGL 4.3
 * and OpenGL ES 3.1. Except for buffer binding, all other states are immutable after creation. See <a
 * href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_vertex_attrib_binding.txt">ARB_vertex_attrib_binding</a>
 */
public final class GLVertexArray extends ManagedResource {

    private static final int INVALID_BINDING = -1;

    private int mVertexArray;

    // OpenGL 4 only
    private final int mVertexBinding;
    private final int mInstanceBinding;

    private final int mVertexStride;
    private final int mInstanceStride;

    // OpenGL 3 only
    private final int mNumVertexLocations;
    private final int mNumInstanceLocations;

    // OpenGL 3 only
    // lower 24 bits: offset
    // high 8 bits: VertexAttribType
    // index: location (attrib index)
    private final int[] mAttributes;

    // this is the binding state stored in VAO, not context
    // (we don't care about context's binding state)
    private UniqueID mIndexBuffer;
    private UniqueID mVertexBuffer;
    private UniqueID mInstanceBuffer;

    private long mVertexOffset;
    private long mInstanceOffset;

    private GLVertexArray(GLDevice device,
                          int vertexArray,
                          int vertexBinding,
                          int instanceBinding,
                          int vertexStride,
                          int instanceStride,
                          int numVertexLocations,
                          int numInstanceLocations,
                          int[] attributes) {
        super(device);
        assert (vertexArray != 0);
        assert (vertexBinding == INVALID_BINDING || vertexStride > 0);
        assert (instanceBinding == INVALID_BINDING || instanceStride > 0);
        mVertexArray = vertexArray;
        mVertexBinding = vertexBinding;
        mInstanceBinding = instanceBinding;
        mVertexStride = vertexStride;
        mInstanceStride = instanceStride;
        mNumVertexLocations = numVertexLocations;
        mNumInstanceLocations = numInstanceLocations;
        mAttributes = attributes;
    }

    @Nullable
    @SharedPtr
    public static GLVertexArray make(@Nonnull GLDevice device,
                                     @Nonnull GeometryStep geomProc) {
        var gl = device.getGL();
        final boolean dsa = device.getCaps().hasDSASupport();
        final boolean vertexAttribBindingSupport = device.getCaps().hasVertexAttribBindingSupport();
        final int vertexArray;

        if (dsa) {
            vertexArray = gl.glCreateVertexArrays();
        } else {
            vertexArray = gl.glGenVertexArrays();
        }
        if (vertexArray == 0) {
            return null;
        }

        int oldVertexArray = 0;
        if (!dsa) {
            oldVertexArray = gl.glGetInteger(GL_VERTEX_ARRAY_BINDING);
            gl.glBindVertexArray(vertexArray);
        }

        // index is location, they are the same
        int index = 0;
        int bindingIndex = 0;

        int vertexBinding = INVALID_BINDING;
        int instanceBinding = INVALID_BINDING;

        int numVertexLocations = 0;
        int numInstanceLocations = 0;

        if (geomProc.hasVertexAttributes()) {
            if (dsa || vertexAttribBindingSupport) {
                int prevIndex = index;
                if (dsa) {
                    index = set_vertex_format_binding_group_dsa(gl,
                            geomProc.vertexAttributes(),
                            vertexArray,
                            index,
                            bindingIndex,
                            0); // per-vertex
                } else {
                    index = set_vertex_format_binding_group(gl,
                            geomProc.vertexAttributes(),
                            index,
                            bindingIndex,
                            0); // per-vertex
                }
                numVertexLocations = index - prevIndex;
                vertexBinding = bindingIndex++;
            } else {
                numVertexLocations = geomProc.numVertexLocations();
                index += numVertexLocations;
            }
        }

        if (geomProc.hasInstanceAttributes()) {
            if (dsa || vertexAttribBindingSupport) {
                int prevIndex = index;
                if (dsa) {
                    index = set_vertex_format_binding_group_dsa(gl,
                            geomProc.instanceAttributes(),
                            vertexArray,
                            index,
                            bindingIndex,
                            1); // per-instance
                } else {
                    index = set_vertex_format_binding_group(gl,
                            geomProc.instanceAttributes(),
                            index,
                            bindingIndex,
                            1); // per-instance
                }
                numInstanceLocations = index - prevIndex;
                instanceBinding = bindingIndex;
            } else {
                numInstanceLocations = geomProc.numInstanceLocations();
                index += numInstanceLocations;
            }
        }

        assert index == numVertexLocations + numInstanceLocations;

        if (index > device.getCaps().maxVertexAttributes()) {
            gl.glDeleteVertexArrays(vertexArray);
            if (!dsa) {
                gl.glBindVertexArray(oldVertexArray);
            }
            return null;
        }

        int[] attributes = null;

        if (!dsa && !vertexAttribBindingSupport) {
            attributes = new int[index];
            index = 0;
            if (numVertexLocations > 0) {
                index = set_vertex_format_legacy(gl,
                        geomProc.vertexAttributes(),
                        index,
                        0,  // per-vertex
                        attributes);
            }
            if (numInstanceLocations > 0) {
                index = set_vertex_format_legacy(gl,
                        geomProc.instanceAttributes(),
                        index,
                        1,  // per-instance
                        attributes);
            }
            assert index == numVertexLocations + numInstanceLocations;
        }
        if (!dsa) {
            gl.glBindVertexArray(oldVertexArray);
        }

        if (device.getCaps().hasDebugSupport()) {
            String label = geomProc.name();
            if (!label.isEmpty()) {
                label = label.substring(0, Math.min(label.length(),
                        device.getCaps().maxLabelLength()));
                gl.glObjectLabel(GL_VERTEX_ARRAY, vertexArray, label);
            }
        }

        return new GLVertexArray(device,
                vertexArray,
                vertexBinding,
                instanceBinding,
                geomProc.vertexStride(),
                geomProc.instanceStride(),
                numVertexLocations,
                numInstanceLocations,
                attributes);
    }

    private static int set_vertex_format_legacy(GLInterface gl,
                                                @Nonnull Iterable<VertexInputLayout.Attribute> attribs,
                                                int index, int divisor, int[] attributes) {
        for (var attrib : attribs) {
            int locations = attrib.locations();
            int offset = attrib.offset();
            while (locations-- != 0) {
                gl.glEnableVertexAttribArray(index);
                gl.glVertexAttribDivisor(index, divisor);
                assert offset >= 0 && offset <= 0xFFFFFF;
                attributes[index] = (offset & 0xFFFFFF) | ((attrib.srcType() & 0xFF) << 24);
                index++;
                offset += attrib.size();
            }
        }
        return index;
    }

    // @formatter:off
    private static void set_attrib_format_legacy(GLInterface gl, int type, int index, int stride, long offset) {
        switch (type) {
            case Engine.VertexAttribType.kFloat ->
                    gl.glVertexAttribPointer(index, 1, GL_FLOAT, /*normalized*/false, stride, offset);
            case Engine.VertexAttribType.kFloat2 ->
                    gl.glVertexAttribPointer(index, 2, GL_FLOAT, /*normalized*/false, stride, offset);
            case Engine.VertexAttribType.kFloat3 ->
                    gl.glVertexAttribPointer(index, 3, GL_FLOAT, /*normalized*/false, stride, offset);
            case Engine.VertexAttribType.kFloat4 ->
                    gl.glVertexAttribPointer(index, 4, GL_FLOAT, /*normalized*/false, stride, offset);
            case Engine.VertexAttribType.kHalf ->
                    gl.glVertexAttribPointer(index, 1, GL_HALF_FLOAT, /*normalized*/false, stride, offset);
            case Engine.VertexAttribType.kHalf2 ->
                    gl.glVertexAttribPointer(index, 2, GL_HALF_FLOAT, /*normalized*/false, stride, offset);
            case Engine.VertexAttribType.kHalf4 ->
                    gl.glVertexAttribPointer(index, 4, GL_HALF_FLOAT, /*normalized*/false, stride, offset);
            case Engine.VertexAttribType.kInt2 ->
                    gl.glVertexAttribIPointer(index, 2, GL_INT, stride, offset);
            case Engine.VertexAttribType.kInt3 ->
                    gl.glVertexAttribIPointer(index, 3, GL_INT, stride, offset);
            case Engine.VertexAttribType.kInt4 ->
                    gl.glVertexAttribIPointer(index, 4, GL_INT, stride, offset);
            case Engine.VertexAttribType.kByte ->
                    gl.glVertexAttribIPointer(index, 1, GL_BYTE, stride, offset);
            case Engine.VertexAttribType.kByte2 ->
                    gl.glVertexAttribIPointer(index, 2, GL_BYTE, stride, offset);
            case Engine.VertexAttribType.kByte4 ->
                    gl.glVertexAttribIPointer(index, 4, GL_BYTE, stride, offset);
            case Engine.VertexAttribType.kUByte ->
                    gl.glVertexAttribIPointer(index, 1, GL_UNSIGNED_BYTE, stride, offset);
            case Engine.VertexAttribType.kUByte2 ->
                    gl.glVertexAttribIPointer(index, 2, GL_UNSIGNED_BYTE, stride, offset);
            case Engine.VertexAttribType.kUByte4 ->
                    gl.glVertexAttribIPointer(index, 4, GL_UNSIGNED_BYTE, stride, offset);
            case Engine.VertexAttribType.kUByte_norm ->
                    gl.glVertexAttribPointer(index, 1, GL_UNSIGNED_BYTE, /*normalized*/true, stride, offset);
            case Engine.VertexAttribType.kUByte4_norm ->
                    gl.glVertexAttribPointer(index, 4, GL_UNSIGNED_BYTE, /*normalized*/true, stride, offset);
            case Engine.VertexAttribType.kShort2 ->
                    gl.glVertexAttribIPointer(index, 2, GL_SHORT, stride, offset);
            case Engine.VertexAttribType.kShort4 ->
                    gl.glVertexAttribIPointer(index, 4, GL_SHORT, stride, offset);
            case Engine.VertexAttribType.kUShort2 ->
                    gl.glVertexAttribIPointer(index, 2, GL_UNSIGNED_SHORT, stride, offset);
            case Engine.VertexAttribType.kUShort2_norm ->
                    gl.glVertexAttribPointer(index, 2, GL_UNSIGNED_SHORT, /*normalized*/true, stride, offset);
            case Engine.VertexAttribType.kInt ->
                    gl.glVertexAttribIPointer(index, 1, GL_INT, stride, offset);
            case Engine.VertexAttribType.kUInt ->
                    gl.glVertexAttribIPointer(index, 1, GL_UNSIGNED_INT, stride, offset);
            case Engine.VertexAttribType.kUShort_norm ->
                    gl.glVertexAttribPointer(index, 1, GL_UNSIGNED_SHORT, /*normalized*/true, stride, offset);
            case Engine.VertexAttribType.kUShort4_norm ->
                    gl.glVertexAttribPointer(index, 4, GL_UNSIGNED_SHORT, /*normalized*/true, stride, offset);
            default -> throw new AssertionError(type);
        }
    }
    // @formatter:on

    /**
     * See {@link icyllis.arc3d.engine.shading.VertexShaderBuilder}.
     */
    private static int set_vertex_format_binding_group(GLInterface gl,
                                                       @Nonnull Iterable<VertexInputLayout.Attribute> attribs,
                                                       int index,
                                                       int binding,
                                                       int divisor) {
        for (var attrib : attribs) {
            // a matrix can take up multiple locations
            int locations = attrib.locations();
            int offset = attrib.offset();
            while (locations-- != 0) {
                gl.glEnableVertexAttribArray(index);
                gl.glVertexAttribBinding(index, binding);
                set_attrib_format_binding_group(gl, attrib.srcType(), index, offset);
                index++;
                offset += attrib.size();
            }
        }
        gl.glVertexBindingDivisor(binding,
                divisor);
        return index;
    }

    // @formatter:off
    private static void set_attrib_format_binding_group(GLInterface gl, int type, int index, int offset) {
        switch (type) {
            case Engine.VertexAttribType.kFloat ->
                    gl.glVertexAttribFormat(index, 1, GL_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kFloat2 ->
                    gl.glVertexAttribFormat(index, 2, GL_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kFloat3 ->
                    gl.glVertexAttribFormat(index, 3, GL_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kFloat4 ->
                    gl.glVertexAttribFormat(index, 4, GL_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kHalf ->
                    gl.glVertexAttribFormat(index, 1, GL_HALF_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kHalf2 ->
                    gl.glVertexAttribFormat(index, 2, GL_HALF_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kHalf4 ->
                    gl.glVertexAttribFormat(index, 4, GL_HALF_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kInt2 ->
                    gl.glVertexAttribIFormat(index, 2, GL_INT, offset);
            case Engine.VertexAttribType.kInt3 ->
                    gl.glVertexAttribIFormat(index, 3, GL_INT, offset);
            case Engine.VertexAttribType.kInt4 ->
                    gl.glVertexAttribIFormat(index, 4, GL_INT, offset);
            case Engine.VertexAttribType.kByte ->
                    gl.glVertexAttribIFormat(index, 1, GL_BYTE, offset);
            case Engine.VertexAttribType.kByte2 ->
                    gl.glVertexAttribIFormat(index, 2, GL_BYTE, offset);
            case Engine.VertexAttribType.kByte4 ->
                    gl.glVertexAttribIFormat(index, 4, GL_BYTE, offset);
            case Engine.VertexAttribType.kUByte ->
                    gl.glVertexAttribIFormat(index, 1, GL_UNSIGNED_BYTE, offset);
            case Engine.VertexAttribType.kUByte2 ->
                    gl.glVertexAttribIFormat(index, 2, GL_UNSIGNED_BYTE, offset);
            case Engine.VertexAttribType.kUByte4 ->
                    gl.glVertexAttribIFormat(index, 4, GL_UNSIGNED_BYTE, offset);
            case Engine.VertexAttribType.kUByte_norm ->
                    gl.glVertexAttribFormat(index, 1, GL_UNSIGNED_BYTE, /*normalized*/true, offset);
            case Engine.VertexAttribType.kUByte4_norm ->
                    gl.glVertexAttribFormat(index, 4, GL_UNSIGNED_BYTE, /*normalized*/true, offset);
            case Engine.VertexAttribType.kShort2 ->
                    gl.glVertexAttribIFormat(index, 2, GL_SHORT, offset);
            case Engine.VertexAttribType.kShort4 ->
                    gl.glVertexAttribIFormat(index, 4, GL_SHORT, offset);
            case Engine.VertexAttribType.kUShort2 ->
                    gl.glVertexAttribIFormat(index, 2, GL_UNSIGNED_SHORT, offset);
            case Engine.VertexAttribType.kUShort2_norm ->
                    gl.glVertexAttribFormat(index, 2, GL_UNSIGNED_SHORT, /*normalized*/true, offset);
            case Engine.VertexAttribType.kInt ->
                    gl.glVertexAttribIFormat(index, 1, GL_INT, offset);
            case Engine.VertexAttribType.kUInt ->
                    gl.glVertexAttribIFormat(index, 1, GL_UNSIGNED_INT, offset);
            case Engine.VertexAttribType.kUShort_norm ->
                    gl.glVertexAttribFormat(index, 1, GL_UNSIGNED_SHORT, /*normalized*/true, offset);
            case Engine.VertexAttribType.kUShort4_norm ->
                    gl.glVertexAttribFormat(index, 4, GL_UNSIGNED_SHORT, /*normalized*/true, offset);
            default -> throw new AssertionError(type);
        }
    }
    // @formatter:on

    /**
     * See {@link icyllis.arc3d.engine.shading.VertexShaderBuilder}.
     */
    private static int set_vertex_format_binding_group_dsa(GLInterface gl,
                                                           @Nonnull Iterable<VertexInputLayout.Attribute> attribs,
                                                           int array,
                                                           int index,
                                                           int binding,
                                                           int divisor) {
        for (var attrib : attribs) {
            // a matrix can take up multiple locations
            int locations = attrib.locations();
            int offset = attrib.offset();
            while (locations-- != 0) {
                gl.glEnableVertexArrayAttrib(array, index);
                gl.glVertexArrayAttribBinding(array, index, binding);
                set_attrib_format_binding_group_dsa(gl, attrib.srcType(), array, index, offset);
                index++;
                offset += attrib.size();
            }
        }
        gl.glVertexArrayBindingDivisor(array,
                binding,
                divisor);
        return index;
    }

    // @formatter:off
    private static void set_attrib_format_binding_group_dsa(GLInterface gl, int type, int array, int index, int offset) {
        switch (type) {
            case Engine.VertexAttribType.kFloat ->
                    gl.glVertexArrayAttribFormat(array, index, 1, GL_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kFloat2 ->
                    gl.glVertexArrayAttribFormat(array, index, 2, GL_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kFloat3 ->
                    gl.glVertexArrayAttribFormat(array, index, 3, GL_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kFloat4 ->
                    gl.glVertexArrayAttribFormat(array, index, 4, GL_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kHalf ->
                    gl.glVertexArrayAttribFormat(array, index, 1, GL_HALF_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kHalf2 ->
                    gl.glVertexArrayAttribFormat(array, index, 2, GL_HALF_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kHalf4 ->
                    gl.glVertexArrayAttribFormat(array, index, 4, GL_HALF_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kInt2 ->
                    gl.glVertexArrayAttribIFormat(array, index, 2, GL_INT, offset);
            case Engine.VertexAttribType.kInt3 ->
                    gl.glVertexArrayAttribIFormat(array, index, 3, GL_INT, offset);
            case Engine.VertexAttribType.kInt4 ->
                    gl.glVertexArrayAttribIFormat(array, index, 4, GL_INT, offset);
            case Engine.VertexAttribType.kByte ->
                    gl.glVertexArrayAttribIFormat(array, index, 1, GL_BYTE, offset);
            case Engine.VertexAttribType.kByte2 ->
                    gl.glVertexArrayAttribIFormat(array, index, 2, GL_BYTE, offset);
            case Engine.VertexAttribType.kByte4 ->
                    gl.glVertexArrayAttribIFormat(array, index, 4, GL_BYTE, offset);
            case Engine.VertexAttribType.kUByte ->
                    gl.glVertexArrayAttribIFormat(array, index, 1, GL_UNSIGNED_BYTE, offset);
            case Engine.VertexAttribType.kUByte2 ->
                    gl.glVertexArrayAttribIFormat(array, index, 2, GL_UNSIGNED_BYTE, offset);
            case Engine.VertexAttribType.kUByte4 ->
                    gl.glVertexArrayAttribIFormat(array, index, 4, GL_UNSIGNED_BYTE, offset);
            case Engine.VertexAttribType.kUByte_norm ->
                    gl.glVertexArrayAttribFormat(array, index, 1, GL_UNSIGNED_BYTE, /*normalized*/true, offset);
            case Engine.VertexAttribType.kUByte4_norm ->
                    gl.glVertexArrayAttribFormat(array, index, 4, GL_UNSIGNED_BYTE, /*normalized*/true, offset);
            case Engine.VertexAttribType.kShort2 ->
                    gl.glVertexArrayAttribIFormat(array, index, 2, GL_SHORT, offset);
            case Engine.VertexAttribType.kShort4 ->
                    gl.glVertexArrayAttribIFormat(array, index, 4, GL_SHORT, offset);
            case Engine.VertexAttribType.kUShort2 ->
                    gl.glVertexArrayAttribIFormat(array, index, 2, GL_UNSIGNED_SHORT, offset);
            case Engine.VertexAttribType.kUShort2_norm ->
                    gl.glVertexArrayAttribFormat(array, index, 2, GL_UNSIGNED_SHORT, /*normalized*/true, offset);
            case Engine.VertexAttribType.kInt ->
                    gl.glVertexArrayAttribIFormat(array, index, 1, GL_INT, offset);
            case Engine.VertexAttribType.kUInt ->
                    gl.glVertexArrayAttribIFormat(array, index, 1, GL_UNSIGNED_INT, offset);
            case Engine.VertexAttribType.kUShort_norm ->
                    gl.glVertexArrayAttribFormat(array, index, 1, GL_UNSIGNED_SHORT, /*normalized*/true, offset);
            case Engine.VertexAttribType.kUShort4_norm ->
                    gl.glVertexArrayAttribFormat(array, index, 4, GL_UNSIGNED_SHORT, /*normalized*/true, offset);
            default -> throw new AssertionError(type);
        }
    }
    // @formatter:on

    @Override
    protected void deallocate() {
        if (mVertexArray != 0) {
            getDevice().getGL().glDeleteVertexArrays(mVertexArray);
        }
        discard();
    }

    public void discard() {
        mVertexArray = 0;
    }

    public int getHandle() {
        return mVertexArray;
    }

    public int getVertexStride() {
        return mVertexStride;
    }

    public int getInstanceStride() {
        return mInstanceStride;
    }

    /**
     * Set element buffer (index buffer), bind pipeline first.
     *
     * @param buffer the element buffer object, raw ptr
     */
    public void bindIndexBuffer(@Nonnull @RawPtr GLBuffer buffer) {
        if (mVertexArray == 0) {
            return;
        }
        if (mIndexBuffer != buffer.getUniqueID()) {
            // Sometimes glVertexArrayElementBuffer will cause segfault on glDrawElementsBaseVertex.
            // So we just use normal glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer)
            // NOTE: this binding state is associated with current VAO
            getDevice().bindIndexBufferInPipe(buffer);
            mIndexBuffer = buffer.getUniqueID();
        }
    }

    /**
     * Set the buffer that stores the attribute data, bind pipeline first.
     * <p>
     * The stride, the distance to the next vertex data, in bytes, is determined in constructor.
     *
     * @param buffer the vertex buffer object, raw ptr
     * @param offset first vertex data to the base of the buffer, in bytes
     */
    public void bindVertexBuffer(@Nonnull @RawPtr GLBuffer buffer, long offset) {
        if (mVertexArray == 0) {
            return;
        }
        assert mVertexStride > 0;
        if (mVertexBuffer != buffer.getUniqueID() ||
                mVertexOffset != offset) {
            if (mVertexBinding != INVALID_BINDING) {
                // OpenGL 4.5
                getDevice().getGL().glBindVertexBuffer(
                        mVertexBinding,
                        buffer.getHandle(),
                        offset,
                        mVertexStride);
            } else if (mAttributes != null) {
                // 'offset' may translate into 'baseVertex'
                int target = getDevice().bindBuffer(buffer);
                assert target == GL_ARRAY_BUFFER;
                for (int index = 0;
                     index < mNumVertexLocations;
                     index++) {
                    int info = mAttributes[index];
                    set_attrib_format_legacy(getDevice().getGL(),
                            /*type*/info >> 24,
                            index,
                            mVertexStride,
                            /*base_offset*/offset + /*relative_offset*/(info & 0xFFFFFF));
                }
            } else assert false;
            mVertexBuffer = buffer.getUniqueID();
            mVertexOffset = offset;
        }
    }

    /**
     * Set the buffer that stores the attribute data, bind pipeline first.
     * <p>
     * The stride, the distance to the next instance data, in bytes, is determined in constructor.
     *
     * @param buffer the vertex buffer object, raw ptr
     * @param offset first instance data to the base of the buffer, in bytes
     */
    public void bindInstanceBuffer(@Nonnull @RawPtr GLBuffer buffer, long offset) {
        if (mVertexArray == 0) {
            return;
        }
        assert mInstanceStride > 0;
        if (mInstanceBuffer != buffer.getUniqueID() ||
                mInstanceOffset != offset) {
            if (mInstanceBinding != INVALID_BINDING) {
                // OpenGL 4.3
                getDevice().getGL().glBindVertexBuffer(
                        mInstanceBinding,
                        buffer.getHandle(),
                        offset,
                        mInstanceStride);
            } else if (mAttributes != null) {
                // 'offset' may translate into 'baseInstance'
                int target = getDevice().bindBuffer(buffer);
                assert target == GL_ARRAY_BUFFER;
                for (int index = mNumVertexLocations;
                     index < mNumVertexLocations + mNumInstanceLocations;
                     index++) {
                    int info = mAttributes[index];
                    set_attrib_format_legacy(getDevice().getGL(),
                            /*type*/info >> 24,
                            index,
                            mInstanceStride,
                            /*base_offset*/offset + /*relative_offset*/(info & 0xFFFFFF));
                }
            } else assert false;
            mInstanceBuffer = buffer.getUniqueID();
            mInstanceOffset = offset;
        }
    }

    @Override
    protected GLDevice getDevice() {
        return (GLDevice) super.getDevice();
    }
}
