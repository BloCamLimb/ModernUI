/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.compiler;

import icyllis.arc3d.compiler.tree.Type;
import icyllis.arc3d.core.MathUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Standard layout for interface blocks, according to OpenGL and Vulkan specification.
 */
public enum MemoryLayout {
    /**
     * OpenGL standard layout, for uniform blocks.
     * (GLSL only, OpenGL only)
     * <p>
     * The base alignment of an array, matrix, and structure needs to be a multiple of 16.
     */
    Std140,
    /**
     * SPIR-V extended alignment, for uniform blocks.
     * (WGSL or SPIR-V, WebGPU, OpenGL or Vulkan)
     * <p>
     * Similar to std140, the base alignment of an array and structure needs to be a multiple
     * of 16, but that of a matrix does not.
     */
    Extended,
    /**
     * OpenGL standard layout, for push constants and shader storage blocks.
     * Can be used on uniform blocks for Vulkan, if supported.
     * (GLSL, WGSL or SPIR-V, OpenGL, WebGPU or Vulkan)
     */
    Std430,
    /**
     * Scalar alignment, may be slower than std430, for Vulkan if supported.
     * <p>
     * Test only. We're unsure about if we can use the padding between the end of a structure or
     * an array and the next multiple of alignment of that structure or array. glslang doesn't
     * use it, but Vulkan spec allows it.
     */
    Scalar;

    /**
     * Returns the alignment in bytes.
     */
    public int alignment(@Nonnull Type type) {
        return alignment(type, null);
    }

    /**
     * Returns the alignment in bytes, also computes the size and stride simultaneously.
     * out[0] holds {@link #size}, out[1] holds matrix stride, out[2] holds array stride.
     * Matrix stride is non-zero only when the type is matrix or array-of-matrices.
     * Array stride is non-zero only when the type is array.
     *
     * @param out size, matrix stride, array stride, respectively, can be null
     * @return base alignment
     */
    public int alignment(@Nonnull Type type, @Nullable int[] out) {
        return switch (type.getTypeKind()) {
            case Type.kScalar_TypeKind -> {
                int width = type.getWidth();
                if (width == 32) {
                    if (out != null) {
                        out[0] = 4;
                        out[1] = out[2] = 0; // clear matrix and array stride
                    }
                    yield 4;
                }
                throw new UnsupportedOperationException();
            }
            case Type.kVector_TypeKind -> {
                int scalarAlign = alignment(type.getElementType(), out);
                if (out != null) {
                    out[0] *= type.getRows();
                }
                // two-component returns 2N
                // three- or four-component returns 4N
                yield this != Scalar
                        ? scalarAlign << (type.getRows() > 2 ? 2 : 1)
                        : scalarAlign;
            }
            case Type.kMatrix_TypeKind -> {
                int align = alignment(type.getElementType(), out);
                if (this == Std140) {
                    // alignment > 16 is already a multiple of 16
                    align = Math.max(align, 16);
                    assert (align & 15) == 0;
                }
                if (out != null) {
                    out[1] = MathUtil.alignTo(out[0], align); // matrix stride
                    out[0] = out[1] * type.getCols();
                }
                yield align;
            }
            case Type.kArray_TypeKind -> {
                int align = alignment(type.getElementType(), out);
                if (this == Std140 || this == Extended) {
                    align = Math.max(align, 16);
                    assert (align & 15) == 0;
                }
                if (out != null) {
                    out[2] = MathUtil.alignTo(out[0], align); // array stride
                    out[0] = type.isUnsizedArray()
                            ? out[2] // count 1 for runtime sized array
                            : out[2] * type.getArraySize();
                }
                yield align;
            }
            case Type.kStruct_TypeKind -> {
                int align = 0, size = 0;
                for (var field : type.getFields()) {
                    int memberAlign = alignment(field.type(), out);
                    align = Math.max(align, memberAlign);
                    if (out != null) {
                        size = MathUtil.alignTo(size, memberAlign);
                        size += out[0];
                    }
                }
                if (this == Std140 || this == Extended) {
                    align = Math.max(align, 16);
                    assert (align & 15) == 0;
                }
                if (out != null) {
                    if (this != Scalar) {
                        // add tail padding
                        size = MathUtil.alignTo(size, align);
                    }
                    out[0] = size;
                    out[1] = out[2] = 0; // clear matrix and array stride
                }
                yield align;
            }
            default -> throw new UnsupportedOperationException();
        };
    }

    /**
     * Returns the minimum stride for {@code SpvDecorationArrayStride} and
     * {@code SpvDecorationMatrixStride}.
     */
    public int stride(@Nonnull Type type) {
        return switch (type.getTypeKind()) {
            case Type.kMatrix_TypeKind, Type.kArray_TypeKind -> {
                // 15.5.4. Offset and Stride Assignment
                // Any ArrayStride or MatrixStride decoration must be a multiple of the alignment of the array or
                // matrix as defined above.
                int size = size(type.getElementType());
                yield MathUtil.alignTo(size, alignment(type));
            }
            default -> throw new UnsupportedOperationException();
        };
    }

    /**
     * Returns the total size in bytes, including padding at the end.
     */
    public int size(@Nonnull Type type) {
        return switch (type.getTypeKind()) {
            case Type.kScalar_TypeKind -> {
                int width = type.getWidth();
                if (width == 32) {
                    yield 4;
                }
                throw new UnsupportedOperationException();
            }
            case Type.kVector_TypeKind -> {
                int size = size(type.getElementType());
                yield size * type.getRows();
            }
            case Type.kMatrix_TypeKind -> {
                int stride = stride(type);
                yield stride * type.getCols();
            }
            case Type.kArray_TypeKind -> {
                int stride = stride(type);
                yield type.isUnsizedArray()
                        ? stride // count 1 for runtime sized array
                        : stride * type.getArraySize();
            }
            case Type.kStruct_TypeKind -> {
                int size = 0;
                for (var field : type.getFields()) {
                    int memberAlign = alignment(field.type());
                    size = MathUtil.alignTo(size, memberAlign);
                    size += size(field.type());
                }
                if (this != Scalar) {
                    int align = alignment(type);
                    // add tail padding
                    size = MathUtil.alignTo(size, align);
                }
                yield size;
            }
            default -> throw new UnsupportedOperationException();
        };
    }

    /**
     * Returns true if the type is host shareable, i.e., a composite type.
     * Boolean type is not supported, use uint32 instead.
     */
    public boolean isSupported(@Nonnull Type type) {
        return switch (type.getTypeKind()) {
            case Type.kScalar_TypeKind -> !type.isBoolean();
            case Type.kVector_TypeKind,
                    Type.kMatrix_TypeKind,
                    Type.kArray_TypeKind -> isSupported(type.getElementType());
            case Type.kStruct_TypeKind -> {
                for (var field : type.getFields()) {
                    if (!isSupported(field.type())) {
                        yield false;
                    }
                }
                yield true;
            }
            default -> false;
        };
    }
}
