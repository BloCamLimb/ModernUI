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

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static icyllis.arc3d.opengl.GLCore.*;

/**
 * This class manages the lifetime of the vertex array object and is used to track the state of the
 * vertex array to avoid redundant GL calls. May be shared by multiple {@link GLGraphicsPipelineState}.
 * <p>
 * Supports OpenGL 3.3 and OpenGL 4.5.
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

    private final boolean mDSAElementBuffer;

    // OpenGL 3 only
    // lower 24 bits: offset
    // high 8 bits: VertexAttribType
    // index: location (attrib index)
    private final int[] mAttributes;

    // this is the binding state stored in VAO, not context
    // (we don't care about context's binding state)
    private GLBuffer.UniqueID mIndexBuffer;
    private GLBuffer.UniqueID mVertexBuffer;
    private GLBuffer.UniqueID mInstanceBuffer;

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

        mDSAElementBuffer = attributes == null &&
                !device.getCaps().dsaElementBufferIsBroken();
    }

    @Nullable
    @SharedPtr
    public static GLVertexArray make(@Nonnull GLDevice device,
                                     @Nonnull GeometryProcessor geomProc) {
        final boolean dsa = device.getCaps().hasDSASupport();
        final int vertexArray;

        if (dsa) {
            vertexArray = glCreateVertexArrays();
        } else {
            vertexArray = glGenVertexArrays();
        }
        if (vertexArray == 0) {
            return null;
        }

        int oldVertexArray = 0;
        if (!dsa) {
            oldVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING);
            glBindVertexArray(vertexArray);
        }

        // index is location, they are the same
        int index = 0;
        int bindingIndex = 0;

        int vertexBinding = INVALID_BINDING;
        int instanceBinding = INVALID_BINDING;

        int numVertexLocations = 0;
        int numInstanceLocations = 0;

        if (geomProc.hasVertexAttributes()) {
            if (dsa) {
                int prevIndex = index;
                index = set_vertex_format_4(geomProc.getVertexAttributes(),
                        vertexArray,
                        index,
                        bindingIndex,
                        0); // per-vertex
                numVertexLocations = index - prevIndex;
                vertexBinding = bindingIndex++;
            } else {
                numVertexLocations = geomProc.numVertexLocations();
                index += numVertexLocations;
            }
        }

        if (geomProc.hasInstanceAttributes()) {
            if (dsa) {
                int prevIndex = index;
                index = set_vertex_format_4(geomProc.getInstanceAttributes(),
                        vertexArray,
                        index,
                        bindingIndex,
                        1); // per-instance
                numInstanceLocations = index - prevIndex;
                instanceBinding = bindingIndex;
            } else {
                numInstanceLocations = geomProc.numInstanceLocations();
                index += numInstanceLocations;
            }
        }

        assert index == numVertexLocations + numInstanceLocations;

        if (index > device.getCaps().maxVertexAttributes()) {
            glDeleteVertexArrays(vertexArray);
            if (!dsa) {
                glBindVertexArray(oldVertexArray);
            }
            return null;
        }

        int[] attributes = null;

        if (!dsa) {
            attributes = new int[index];
            index = 0;
            if (numVertexLocations > 0) {
                index = set_vertex_format_3(geomProc.getVertexAttributes(),
                        index,
                        0,  // per-vertex
                        attributes);
            }
            if (numInstanceLocations > 0) {
                index = set_vertex_format_3(geomProc.getInstanceAttributes(),
                        index,
                        1,  // per-instance
                        attributes);
            }
            assert index == numVertexLocations + numInstanceLocations;

            glBindVertexArray(oldVertexArray);
        }

        if (device.getCaps().hasDebugSupport()) {
            String label = geomProc.name();
            if (!label.isEmpty()) {
                label = label.substring(0, Math.min(label.length(),
                        device.getCaps().maxLabelLength()));
                glObjectLabel(GL_VERTEX_ARRAY, vertexArray, label);
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

    private static int set_vertex_format_3(@Nonnull Iterable<GeometryProcessor.Attribute> attribs,
                                           int index, int divisor, int[] attributes) {
        for (var attrib : attribs) {
            int locations = attrib.locationSize();
            int offset = attrib.offset();
            while (locations-- != 0) {
                glEnableVertexAttribArray(index);
                glVertexAttribDivisor(index, divisor);
                assert offset >= 0 && offset <= 0xFFFFFF;
                attributes[index] = (offset & 0xFFFFFF) | (attrib.srcType() << 24);
                index++;
                offset += attrib.stepSize();
            }
        }
        return index;
    }

    // @formatter:off
    private static void set_attrib_format_3(int type, int index, int stride, long offset) {
        switch (type) {
            case Engine.VertexAttribType.kFloat ->
                    glVertexAttribPointer(index, 1, GL_FLOAT, /*normalized*/false, stride, offset);
            case Engine.VertexAttribType.kFloat2 ->
                    glVertexAttribPointer(index, 2, GL_FLOAT, /*normalized*/false, stride, offset);
            case Engine.VertexAttribType.kFloat3 ->
                    glVertexAttribPointer(index, 3, GL_FLOAT, /*normalized*/false, stride, offset);
            case Engine.VertexAttribType.kFloat4 ->
                    glVertexAttribPointer(index, 4, GL_FLOAT, /*normalized*/false, stride, offset);
            case Engine.VertexAttribType.kHalf ->
                    glVertexAttribPointer(index, 1, GL_HALF_FLOAT, /*normalized*/false, stride, offset);
            case Engine.VertexAttribType.kHalf2 ->
                    glVertexAttribPointer(index, 2, GL_HALF_FLOAT, /*normalized*/false, stride, offset);
            case Engine.VertexAttribType.kHalf4 ->
                    glVertexAttribPointer(index, 4, GL_HALF_FLOAT, /*normalized*/false, stride, offset);
            case Engine.VertexAttribType.kInt2 ->
                    glVertexAttribIPointer(index, 2, GL_INT, stride, offset);
            case Engine.VertexAttribType.kInt3 ->
                    glVertexAttribIPointer(index, 3, GL_INT, stride, offset);
            case Engine.VertexAttribType.kInt4 ->
                    glVertexAttribIPointer(index, 4, GL_INT, stride, offset);
            case Engine.VertexAttribType.kByte ->
                    glVertexAttribIPointer(index, 1, GL_BYTE, stride, offset);
            case Engine.VertexAttribType.kByte2 ->
                    glVertexAttribIPointer(index, 2, GL_BYTE, stride, offset);
            case Engine.VertexAttribType.kByte4 ->
                    glVertexAttribIPointer(index, 4, GL_BYTE, stride, offset);
            case Engine.VertexAttribType.kUByte ->
                    glVertexAttribIPointer(index, 1, GL_UNSIGNED_BYTE, stride, offset);
            case Engine.VertexAttribType.kUByte2 ->
                    glVertexAttribIPointer(index, 2, GL_UNSIGNED_BYTE, stride, offset);
            case Engine.VertexAttribType.kUByte4 ->
                    glVertexAttribIPointer(index, 4, GL_UNSIGNED_BYTE, stride, offset);
            case Engine.VertexAttribType.kUByte_norm ->
                    glVertexAttribPointer(index, 1, GL_UNSIGNED_BYTE, /*normalized*/true, stride, offset);
            case Engine.VertexAttribType.kUByte4_norm ->
                    glVertexAttribPointer(index, 4, GL_UNSIGNED_BYTE, /*normalized*/true, stride, offset);
            case Engine.VertexAttribType.kShort2 ->
                    glVertexAttribIPointer(index, 2, GL_SHORT, stride, offset);
            case Engine.VertexAttribType.kShort4 ->
                    glVertexAttribIPointer(index, 4, GL_SHORT, stride, offset);
            case Engine.VertexAttribType.kUShort2 ->
                    glVertexAttribIPointer(index, 2, GL_UNSIGNED_SHORT, stride, offset);
            case Engine.VertexAttribType.kUShort2_norm ->
                    glVertexAttribPointer(index, 2, GL_UNSIGNED_SHORT, /*normalized*/true, stride, offset);
            case Engine.VertexAttribType.kInt ->
                    glVertexAttribIPointer(index, 1, GL_INT, stride, offset);
            case Engine.VertexAttribType.kUInt ->
                    glVertexAttribIPointer(index, 1, GL_UNSIGNED_INT, stride, offset);
            case Engine.VertexAttribType.kUShort_norm ->
                    glVertexAttribPointer(index, 1, GL_UNSIGNED_SHORT, /*normalized*/true, stride, offset);
            case Engine.VertexAttribType.kUShort4_norm ->
                    glVertexAttribPointer(index, 4, GL_UNSIGNED_SHORT, /*normalized*/true, stride, offset);
            default -> throw new AssertionError(type);
        }
    }
    // @formatter:on

    /**
     * See {@link icyllis.arc3d.engine.shading.VertexShaderBuilder} to see how we bind these on GPU side.
     */
    private static int set_vertex_format_4(@Nonnull Iterable<GeometryProcessor.Attribute> attribs,
                                           int array,
                                           int index,
                                           int binding,
                                           int divisor) {
        for (var attrib : attribs) {
            // a matrix can take up multiple locations
            int locations = attrib.locationSize();
            int offset = attrib.offset();
            while (locations-- != 0) {
                glEnableVertexArrayAttrib(array, index);
                glVertexArrayAttribBinding(array, index, binding);
                set_attrib_format_4(attrib.srcType(), array, index, offset);
                index++;
                offset += attrib.stepSize();
            }
        }
        glVertexArrayBindingDivisor(array,
                binding,
                divisor);
        return index;
    }

    // @formatter:off
    private static void set_attrib_format_4(int type, int array, int index, int offset) {
        switch (type) {
            case Engine.VertexAttribType.kFloat ->
                    glVertexArrayAttribFormat(array, index, 1, GL_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kFloat2 ->
                    glVertexArrayAttribFormat(array, index, 2, GL_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kFloat3 ->
                    glVertexArrayAttribFormat(array, index, 3, GL_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kFloat4 ->
                    glVertexArrayAttribFormat(array, index, 4, GL_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kHalf ->
                    glVertexArrayAttribFormat(array, index, 1, GL_HALF_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kHalf2 ->
                    glVertexArrayAttribFormat(array, index, 2, GL_HALF_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kHalf4 ->
                    glVertexArrayAttribFormat(array, index, 4, GL_HALF_FLOAT, /*normalized*/false, offset);
            case Engine.VertexAttribType.kInt2 ->
                    glVertexArrayAttribIFormat(array, index, 2, GL_INT, offset);
            case Engine.VertexAttribType.kInt3 ->
                    glVertexArrayAttribIFormat(array, index, 3, GL_INT, offset);
            case Engine.VertexAttribType.kInt4 ->
                    glVertexArrayAttribIFormat(array, index, 4, GL_INT, offset);
            case Engine.VertexAttribType.kByte ->
                    glVertexArrayAttribIFormat(array, index, 1, GL_BYTE, offset);
            case Engine.VertexAttribType.kByte2 ->
                    glVertexArrayAttribIFormat(array, index, 2, GL_BYTE, offset);
            case Engine.VertexAttribType.kByte4 ->
                    glVertexArrayAttribIFormat(array, index, 4, GL_BYTE, offset);
            case Engine.VertexAttribType.kUByte ->
                    glVertexArrayAttribIFormat(array, index, 1, GL_UNSIGNED_BYTE, offset);
            case Engine.VertexAttribType.kUByte2 ->
                    glVertexArrayAttribIFormat(array, index, 2, GL_UNSIGNED_BYTE, offset);
            case Engine.VertexAttribType.kUByte4 ->
                    glVertexArrayAttribIFormat(array, index, 4, GL_UNSIGNED_BYTE, offset);
            case Engine.VertexAttribType.kUByte_norm ->
                    glVertexArrayAttribFormat(array, index, 1, GL_UNSIGNED_BYTE, /*normalized*/true, offset);
            case Engine.VertexAttribType.kUByte4_norm ->
                    glVertexArrayAttribFormat(array, index, 4, GL_UNSIGNED_BYTE, /*normalized*/true, offset);
            case Engine.VertexAttribType.kShort2 ->
                    glVertexArrayAttribIFormat(array, index, 2, GL_SHORT, offset);
            case Engine.VertexAttribType.kShort4 ->
                    glVertexArrayAttribIFormat(array, index, 4, GL_SHORT, offset);
            case Engine.VertexAttribType.kUShort2 ->
                    glVertexArrayAttribIFormat(array, index, 2, GL_UNSIGNED_SHORT, offset);
            case Engine.VertexAttribType.kUShort2_norm ->
                    glVertexArrayAttribFormat(array, index, 2, GL_UNSIGNED_SHORT, /*normalized*/true, offset);
            case Engine.VertexAttribType.kInt ->
                    glVertexArrayAttribIFormat(array, index, 1, GL_INT, offset);
            case Engine.VertexAttribType.kUInt ->
                    glVertexArrayAttribIFormat(array, index, 1, GL_UNSIGNED_INT, offset);
            case Engine.VertexAttribType.kUShort_norm ->
                    glVertexArrayAttribFormat(array, index, 1, GL_UNSIGNED_SHORT, /*normalized*/true, offset);
            case Engine.VertexAttribType.kUShort4_norm ->
                    glVertexArrayAttribFormat(array, index, 4, GL_UNSIGNED_SHORT, /*normalized*/true, offset);
            default -> throw new AssertionError(type);
        }
    }
    // @formatter:on

    @Override
    protected void deallocate() {
        if (mVertexArray != 0) {
            glDeleteVertexArrays(mVertexArray);
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
     * Set element buffer (index buffer).
     * <p>
     * In OpenGL 3.3, bind pipeline first.
     *
     * @param buffer the element buffer object, raw ptr
     */
    public void bindIndexBuffer(@Nonnull GLBuffer buffer) {
        if (mVertexArray == 0) {
            return;
        }
        if (mIndexBuffer != buffer.getUniqueID()) {
            if (mDSAElementBuffer) {
                // OpenGL 4.5
                glVertexArrayElementBuffer(mVertexArray, buffer.getHandle());
            } else {
                // OpenGL 3.3
                // this binding state is associated with current VAO
                getDevice().bindIndexBufferInPipe(buffer);
            }
            mIndexBuffer = buffer.getUniqueID();
        }
    }

    // see https://registry.khronos.org/OpenGL/extensions/ARB/ARB_vertex_attrib_binding.txt

    /**
     * Set the buffer that stores the attribute data.
     * <p>
     * The stride, the distance to the next vertex data, in bytes, is determined in constructor.
     * <p>
     * In OpenGL 3.3, bind pipeline first.
     *
     * @param buffer the vertex buffer object, raw ptr
     * @param offset first vertex data to the base of the buffer, in bytes
     */
    public void bindVertexBuffer(@Nonnull GLBuffer buffer, long offset) {
        if (mVertexArray == 0) {
            return;
        }
        assert mVertexStride > 0;
        if (mVertexBuffer != buffer.getUniqueID() ||
                mVertexOffset != offset) {
            if (mVertexBinding != INVALID_BINDING) {
                // OpenGL 4.5
                glVertexArrayVertexBuffer(mVertexArray,
                        mVertexBinding,
                        buffer.getHandle(),
                        offset,
                        mVertexStride);
            } else if (mAttributes != null) {
                // OpenGL 3.3, you must bind pipeline before
                // 'offset' should translate into 'baseVertex'
                int target = getDevice().bindBuffer(buffer);
                assert target == GL_ARRAY_BUFFER;
                for (int index = 0;
                     index < mNumVertexLocations;
                     index++) {
                    int attr = mAttributes[index];
                    set_attrib_format_3(/*type*/attr >>> 24,
                            index,
                            mVertexStride,
                            /*base_offset*/offset + /*relative_offset*/(attr & 0xFFFFFF));
                }
            } else assert false;
            mVertexBuffer = buffer.getUniqueID();
            mVertexOffset = offset;
        }
    }

    /**
     * Set the buffer that stores the attribute data.
     * <p>
     * The stride, the distance to the next instance data, in bytes, is determined in constructor.
     * <p>
     * In OpenGL 3.3, bind pipeline first.
     *
     * @param buffer the vertex buffer object, raw ptr
     * @param offset first instance data to the base of the buffer, in bytes
     */
    public void bindInstanceBuffer(@Nonnull GLBuffer buffer, long offset) {
        if (mVertexArray == 0) {
            return;
        }
        assert mInstanceStride > 0;
        if (mInstanceBuffer != buffer.getUniqueID() ||
                mInstanceOffset != offset) {
            if (mInstanceBinding != INVALID_BINDING) {
                // OpenGL 4.5
                glVertexArrayVertexBuffer(mVertexArray,
                        mInstanceBinding,
                        buffer.getHandle(),
                        offset,
                        mInstanceStride);
            } else if (mAttributes != null) {
                // OpenGL 3.3, you must bind pipeline before
                // 'offset' should translate into 'baseInstance'
                int target = getDevice().bindBuffer(buffer);
                assert target == GL_ARRAY_BUFFER;
                for (int index = mNumVertexLocations;
                     index < mNumVertexLocations + mNumInstanceLocations;
                     index++) {
                    int attr = mAttributes[index];
                    set_attrib_format_3(/*type*/attr >>> 24,
                            index,
                            mInstanceStride,
                            /*base_offset*/offset + /*relative_offset*/(attr & 0xFFFFFF));
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
