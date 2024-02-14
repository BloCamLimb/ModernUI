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
     */
    Std140,
    /**
     * Extended alignment, for uniform blocks.
     * (SPIR-V only, OpenGL or Vulkan)
     */
    Std140_Extended,
    /**
     * Vulkan standard layout, for push constants and shader storage blocks.
     * Can be used on uniform blocks for Vulkan, if supported.
     * (GLSL or SPIR-V, OpenGL or Vulkan)
     */
    Std430;

    /**
     * Returns the base alignment in bytes.
     */
    public int alignment(@Nonnull Type type) {
       return alignment(type, null);
    }

    /**
     * Returns the base alignment in bytes, also computes the size and stride simultaneously.
     * out[0] holds {@link #size}, out[1] holds {@link #stride} for array and matrix.
     *
     * @param out size and stride, respectively, can be null
     * @return base alignment
     */
    public int alignment(@Nonnull Type type, @Nullable int[] out) {
        return switch (type.getTypeKind()) {
            case Type.kScalar_TypeKind -> {
                int width = type.getWidth();
                if (width == 32) {
                    if (out != null) {
                        out[0] = 4;
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
                yield scalarAlign << (type.getRows() > 2 ? 2 : 1);
            }
            case Type.kMatrix_TypeKind -> {
                int align = alignment(type.getElementType(), null);
                if (this == Std140) {
                    // alignment > 16 is already a multiple of 16
                    align = Math.max(align, 16);
                    assert (align & 15) == 0;
                }
                if (out != null) {
                    out[1] = align; // simplified
                    out[0] = align * type.getCols();
                }
                yield align;
            }
            case Type.kArray_TypeKind -> {
                int align = alignment(type.getElementType(), out);
                if (this == Std140 || this == Std140_Extended) {
                    align = Math.max(align, 16);
                    assert (align & 15) == 0;
                }
                if (out != null) {
                    out[1] = MathUtil.alignTo(out[0], align);
                    out[0] = type.isUnsizedArray()
                            ? out[1] // count 1 for runtime sized array
                            : out[1] * type.getArraySize();
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
                if (this == Std140 || this == Std140_Extended) {
                    align = Math.max(align, 16);
                    assert (align & 15) == 0;
                }
                if (out != null) {
                    out[0] = MathUtil.alignTo(size, align);
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
            case Type.kMatrix_TypeKind -> alignment(type); // simplified
            case Type.kArray_TypeKind -> {
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
                int align = alignment(type);
                yield MathUtil.alignTo(size, align);
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
