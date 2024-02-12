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
       return switch (type.getTypeKind()) {
           case Type.kScalar_TypeKind -> {
               int width = type.getWidth();
               if (width == 32) {
                   yield 4;
               }
               throw new UnsupportedOperationException();
           }
           case Type.kVector_TypeKind -> {
               int align = alignment(type.getElementType());
               // two-component returns 2N
               // three- or four-component returns 4N
               yield align << (type.getRows() > 2 ? 2 : 1);
           }
           case Type.kMatrix_TypeKind -> {
               int align = alignment(type.getElementType());
               if (this == Std140) {
                   align = MathUtil.alignTo(align, 16);
               }
               yield align;
           }
           case Type.kArray_TypeKind -> {
               int align = alignment(type.getElementType());
               if (this == Std140 || this == Std140_Extended) {
                   align = MathUtil.alignTo(align, 16);
               }
               yield align;
           }
           case Type.kStruct_TypeKind -> {
               int align = 0;
               for (var field : type.getFields()) {
                   align = Math.max(align, alignment(field.type()));
               }
               if (this == Std140 || this == Std140_Extended) {
                   align = MathUtil.alignTo(align, 16);
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
                int stride = size(type.getElementType());
                yield MathUtil.alignTo(stride, alignment(type));
            }
            default -> throw new UnsupportedOperationException();
        };
    }

    /**
     * Returns the total size in bytes.
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
                    int align = alignment(field.type());
                    size = MathUtil.alignTo(size, align);
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
     */
    public boolean isSupported(@Nonnull Type type) {
        return switch (type.getTypeKind()) {
            case Type.kScalar_TypeKind ->
                // boolean type is not supported
                // it must be converted into int32 or uint32
                    !type.isBoolean();
            case Type.kVector_TypeKind, Type.kMatrix_TypeKind -> isSupported(type.getComponentType());
            case Type.kArray_TypeKind -> isSupported(type.getElementType());
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
