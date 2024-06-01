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
import icyllis.arc3d.granite.shading.VertexShaderBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;

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

    private int mVertexArray;

    // per-binding vertex stride
    private final int[] mStrides;

    // OpenGL 3 or OpenGL ES only, simulate ARB_base_instance
    private final int[] mInputRates;

    // OpenGL 3 only, simulate ARB_vertex_attrib_binding
    // lower 24 bits: offset
    // high 8 bits: VertexAttribType
    // index: location (attrib index)
    private final int[][] mAttributes;

    // this is the binding state stored in VAO, not context
    // (we don't care about context's binding state)
    private UniqueID mBoundIndexBuffer;

    // per-binding binding state
    private final UniqueID[] mBoundBuffers;
    private final long[] mBoundOffsets;

    private GLVertexArray(GLDevice device,
                          int vertexArray,
                          int[] strides,
                          int[] inputRates,
                          int[][] attributes) {
        super(device);
        assert (vertexArray != 0);
        assert (inputRates == null || inputRates.length == strides.length);
        assert (attributes == null || attributes.length == strides.length);
        mVertexArray = vertexArray;
        mStrides = strides;
        mInputRates = inputRates;
        mAttributes = attributes;
        mBoundBuffers = new UniqueID[strides.length];
        mBoundOffsets = new long[strides.length];
    }

    @Nullable
    @SharedPtr
    public static GLVertexArray make(@Nonnull GLDevice device,
                                     @Nonnull VertexInputLayout inputLayout,
                                     String label) {
        var gl = device.getGL();
        final boolean dsa = device.getCaps().hasDSASupport();
        final boolean vertexAttribBindingSupport = device.getCaps().hasVertexAttribBindingSupport();

        int bindings = inputLayout.getBindingCount();
        if (dsa || vertexAttribBindingSupport) {
            if (bindings > device.getCaps().maxVertexBindings()) {
                return null;
            }
        }

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

        int[] strides = new int[bindings];

        int[] inputRates;
        if (device.getCaps().hasBaseInstanceSupport()) {
            inputRates = null;
        } else {
            inputRates = new int[bindings];
        }

        int[][] attributes;
        if (dsa || vertexAttribBindingSupport) {
            attributes = null;
        } else {
            attributes = new int[bindings][];
        }

        for (int binding = 0; binding < bindings; binding++) {
            int inputRate = inputLayout.getInputRate(binding);
            if (dsa) {
                index = set_vertex_format_binding_group_dsa(gl,
                        inputLayout.getAttributes(binding),
                        vertexArray,
                        index,
                        binding,
                        inputRate);
            } else if (vertexAttribBindingSupport) {
                index = set_vertex_format_binding_group(gl,
                        inputLayout.getAttributes(binding),
                        index,
                        binding,
                        inputRate);
            } else {
                int prevIndex = index;
                int[] attrs = new int[inputLayout.getLocationCount(binding)];
                index = set_vertex_format_legacy(gl,
                        inputLayout.getAttributes(binding),
                        index,
                        inputRate,
                        attrs);
                attributes[binding] = attrs;
                assert prevIndex + attrs.length == index;
            }
            strides[binding] = inputLayout.getStride(binding);
            if (inputRates != null) {
                inputRates[binding] = inputRate;
            }
        }

        if (!dsa) {
            gl.glBindVertexArray(oldVertexArray);
        }

        if (index > device.getCaps().maxVertexAttributes()) {
            gl.glDeleteVertexArrays(vertexArray);
            return null;
        }

        if (device.getCaps().hasDebugSupport()) {
            if (label != null && !label.isEmpty()) {
                label = label.substring(0, Math.min(label.length(),
                        device.getCaps().maxLabelLength()));
                gl.glObjectLabel(GL_VERTEX_ARRAY, vertexArray, label);
            }
        }

        return new GLVertexArray(device,
                vertexArray,
                strides,
                inputRates,
                attributes);
    }

    private static int set_vertex_format_legacy(GLInterface gl,
                                                @Nonnull Iterator<VertexInputLayout.Attribute> attribs,
                                                int index, int divisor, int[] attributes) {
        while (attribs.hasNext()) {
            var attrib = attribs.next();
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
     * See {@link VertexShaderBuilder}.
     */
    private static int set_vertex_format_binding_group(GLInterface gl,
                                                       @Nonnull Iterator<VertexInputLayout.Attribute> attribs,
                                                       int index,
                                                       int binding,
                                                       int divisor) {
        while (attribs.hasNext()) {
            var attrib = attribs.next();
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
     * See {@link VertexShaderBuilder}.
     */
    private static int set_vertex_format_binding_group_dsa(GLInterface gl,
                                                           @Nonnull Iterator<VertexInputLayout.Attribute> attribs,
                                                           int array,
                                                           int index,
                                                           int binding,
                                                           int divisor) {
        while (attribs.hasNext()) {
            var attrib = attribs.next();
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

    public int getBindingCount() {
        return mStrides.length;
    }

    public int getStride(int binding) {
        return mStrides[binding];
    }

    public int getInputRate(int binding) {
        return mInputRates[binding];
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
        if (mBoundIndexBuffer != buffer.getUniqueID()) {
            // Sometimes glVertexArrayElementBuffer will cause segfault on glDrawElementsBaseVertex.
            // So we just use normal glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer)
            // NOTE: this binding state is associated with current VAO
            getDevice().bindIndexBufferInPipe(buffer);
            mBoundIndexBuffer = buffer.getUniqueID();
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
    public void bindVertexBuffer(int binding, @Nonnull @RawPtr GLBuffer buffer, long offset) {
        if (mVertexArray == 0) {
            return;
        }
        if (mBoundBuffers[binding] != buffer.getUniqueID() ||
                mBoundOffsets[binding] != offset) {
            int stride = mStrides[binding];
            assert stride > 0;
            if (mAttributes == null) {
                // OpenGL 4.3
                getDevice().getGL().glBindVertexBuffer(
                        binding,
                        buffer.getHandle(),
                        offset,
                        stride);
            } else {
                // 'offset' may translate into 'baseVertex'
                int target = getDevice().bindBuffer(buffer);
                assert target == GL_ARRAY_BUFFER;
                int index = 0;
                for (int i = 0; i < binding; i++) {
                    index += mAttributes[i].length;
                }
                for (int info : mAttributes[binding]) {
                    set_attrib_format_legacy(getDevice().getGL(),
                            /*type*/info >> 24,
                            index,
                            stride,
                            /*base_offset*/offset + /*relative_offset*/(info & 0xFFFFFF));
                    index++;
                }
            }
            mBoundBuffers[binding] = buffer.getUniqueID();
            mBoundOffsets[binding] = offset;
        }
    }

    @Override
    protected GLDevice getDevice() {
        return (GLDevice) super.getDevice();
    }
}
