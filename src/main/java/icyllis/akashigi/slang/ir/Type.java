/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.slang.ir;

import icyllis.akashigi.slang.Modifier;
import icyllis.akashigi.slang.ThreadContext;
import org.lwjgl.util.spvc.Spv;

import javax.annotation.Nonnull;

/**
 * Represents a type symbol, such as int or float4.
 */
public class Type extends Symbol {

    public static final int UNSIZED_ARRAY = -1;

    /**
     * Block member.
     *
     * @param position see {@link Node#makeRange(int, int)}
     */
    public record Field(int position, Modifier qualifiers, String name, Type type) {

        @Nonnull
        @Override
        public String toString() {
            return type.displayName() + " " + name + ";";
        }
    }

    /**
     * Kinds of Type.
     */
    public static final byte
            TYPE_KIND_ARRAY = 0,
            TYPE_KIND_GENERIC = 1,
            TYPE_KIND_MATRIX = 2,
            TYPE_KIND_OPAQUE = 3,
            TYPE_KIND_OTHER = 4,
            TYPE_KIND_SCALAR = 5,
            TYPE_KIND_STRUCT = 6,
            TYPE_KIND_VECTOR = 7,
            TYPE_KIND_VOID = 8;

    /**
     * Kinds of ScalarType.
     */
    public static final byte
            SCALAR_KIND_FLOAT = 0,
            SCALAR_KIND_SIGNED = 1,
            SCALAR_KIND_UNSIGNED = 2,
            SCALAR_KIND_BOOLEAN = 3,
            SCALAR_KIND_NON_SCALAR = 4;

    /**
     * For compute API. (to be removed)
     */
    public static final byte
            TEXTURE_ACCESS_SAMPLE = 0,      // texture2D (GLSL), Texture2D (HLSL), to be combined with sampler
            TEXTURE_ACCESS_READ = 1,        // readonly image2D (GLSL), texture2d<access::read> (Metal)
            TEXTURE_ACCESS_WRITE = 2,       // writeonly image2D (GLSL), texture2d<access::write> (Metal)
            TEXTURE_ACCESS_READ_WRITE = 3;  // image2D (GLSL), RWTexture2D (HLSL), texture2d<access::read_write> (Metal)

    private final String mDesc;
    private final byte mTypeKind;

    Type(String name, String desc, byte kind) {
        this(name, desc, kind, -1);
    }

    Type(String name, String desc, byte kind, int position) {
        super(position, SymbolKind.kType, name);
        mDesc = desc;
        mTypeKind = kind;
    }

    /**
     * Creates an alias which maps to another type.
     */
    @Nonnull
    public static Type makeAliasType(String name, Type underlyingType) {
        assert (underlyingType == underlyingType.resolve());
        return new AliasType(name, underlyingType);
    }

    /**
     * Create a generic type which maps to the listed types--e.g. __genFType is a generic type which
     * can match float, float2, float3 or float4.
     */
    @Nonnull
    public static Type makeGenericType(String name, Type... coercibleTypes) {
        return new GenericType(name, coercibleTypes);
    }

    /**
     * Create a scalar type.
     */
    @Nonnull
    public static Type makeScalarType(String name, String desc, byte scalarKind,
                                      int priority, int bitWidth) {
        return new ScalarType(name, desc, scalarKind, priority, bitWidth);
    }

    /**
     * Create a vector type.
     */
    @Nonnull
    public static Type makeVectorType(String name, String desc, Type componentType,
                                      int length) {
        return new VectorType(name, desc, componentType, length);
    }

    /**
     * Create a matrix type.
     */
    @Nonnull
    public static Type makeMatrixType(String name, String desc, Type componentType,
                                      int cols, int rows) {
        return new MatrixType(name, desc, componentType, cols, rows);
    }

    /**
     * Create a texture/image/sampler type. Includes images, textures without sampler,
     * textures with sampler and pure samplers.
     * <ul>
     * <li>isSampled=true,isSampler=true: combined texture sampler (e.g. sampler2D)</li>
     * <li>isSampled=true,isSampler=false: pure texture (e.g. texture2D)</li>
     * <li>isSampled=false,isSampler=true: pure sampler (e.g. sampler)</li>
     * <li>isSampled=false,isSampler=false: image or subpass (e.g. image2D)</li>
     * </ul>
     * isShadow: True for samplers that sample a depth texture with comparison (e.g.
     * samplerShadow, sampler2DShadow, HLSL's SamplerComparisonState).
     *
     * @param componentType e.g. texture2D has a type of float
     * @param dimensions    SpvDim (e.g. {@link Spv#SpvDim1D})
     */
    @Nonnull
    public static Type makeOpaqueType(String name, String desc, Type componentType, int dimensions,
                                      boolean isShadow, boolean isArrayed, boolean isMultisampled,
                                      boolean isSampled, boolean isSampler) {
        return new OpaqueType(name, desc, componentType, dimensions, isShadow, isArrayed,
                isMultisampled, isSampled, isSampler);
    }

    /**
     * Create an image or subpass type.
     */
    @Nonnull
    public static Type makeImageType(String name, String desc, Type componentType, int dimensions,
                                     boolean isArrayed, boolean isMultisampled) {
        assert (componentType.isScalar());
        return makeOpaqueType(name, desc, componentType, dimensions, /*isShadow=*/false, isArrayed,
                isMultisampled, /*isSampled=*/false, /*isSampler*/false);
    }

    /**
     * Create a pure texture type.
     */
    @Nonnull
    public static Type makeTextureType(String name, String desc, Type componentType, int dimensions,
                                       boolean isArrayed, boolean isMultisampled) {
        assert (componentType.isScalar());
        return makeOpaqueType(name, desc, componentType, dimensions, /*isShadow=*/false, isArrayed,
                isMultisampled, /*isSampled=*/true, /*isSampler*/false);
    }

    /**
     * Create a pure sampler type.
     */
    @Nonnull
    public static Type makeSamplerType(String name, String desc, Type componentType, boolean isShadow) {
        assert (componentType.isVoid());
        return makeOpaqueType(name, desc, componentType, /*dimensions*/-1, isShadow, /*isArrayed*/false,
                /*isMultisampled*/false, /*isSampled=*/false, /*isSampler*/true);
    }

    /**
     * Create a combined texture sampler type.
     */
    @Nonnull
    public static Type makeCombinedType(String name, String desc, Type componentType, int dimensions,
                                        boolean isShadow, boolean isArrayed, boolean isMultisampled) {
        assert (componentType.isScalar());
        return makeOpaqueType(name, desc, componentType, dimensions, isShadow, isArrayed,
                isMultisampled, /*isSampled=*/true, /*isSampler*/true);
    }

    /**
     * Creates an array type.
     */
    @Nonnull
    public static Type makeArrayType(Type elementType, int length) {
        return new ArrayType(elementType, length);
    }

    /**
     * Create a "special" type with the given name.
     */
    @Nonnull
    public static Type makeSpecialType(String name, String desc, byte typeKind) {
        return new Type(name, desc, typeKind);
    }

    @Nonnull
    @Override
    public Type getType() {
        return this;
    }

    /**
     * If this is an alias, returns the underlying type, otherwise returns this.
     */
    @Nonnull
    public Type resolve() {
        return this;
    }

    /**
     * For arrays, returns the base type. For all other types, returns the type itself.
     */
    @Nonnull
    public Type getElementType() {
        return this;
    }

    /**
     * For matrices and vectors, returns the type of individual cells (e.g. mat2 has a component
     * type of Float). For textures, returns the sampled type (e.g. texture2D has a component type
     * of Float). For all other types, returns the type itself.
     */
    @Nonnull
    public Type getComponentType() {
        return this;
    }

    /**
     * Returns true if these types are equal after alias resolution.
     */
    public final boolean matches(@Nonnull Type other) {
        return resolve().getName().equals(other.resolve().getName());
    }

    /**
     * Returns a descriptor of the type, meant for name-mangling. (e.g. float4x4 -> f44)
     */
    @Nonnull
    public final String getDescriptor() {
        return mDesc;
    }

    /**
     * Returns the category (scalar, vector, matrix, etc.) of this type.
     */
    public final byte typeKind() {
        return mTypeKind;
    }

    /**
     * Returns the ScalarKind of this type (always NonScalar for non-scalar values).
     */
    public byte scalarKind() {
        return SCALAR_KIND_NON_SCALAR;
    }

    @Nonnull
    public final String displayName() {
        return getName();
    }

    @Nonnull
    @Override
    public final String toString() {
        return displayName();
    }

    /**
     * Returns true if this type is known to come from BuiltinTypes. If this returns true, the Type
     * will always be available in the root SymbolTable and never needs to be copied to migrate an
     * Expression from one location to another. If it returns false, the Type might not exist in a
     * separate SymbolTable, and you'll need to consider copying it.
     */
    public final boolean isInBuiltinTypes() {
        return !(isArray() || isStruct());
    }

    /**
     * Returns true if this type is a bool.
     */
    public final boolean isBoolean() {
        return scalarKind() == SCALAR_KIND_BOOLEAN;
    }

    /**
     * Returns true if this is a numeric scalar type.
     */
    public final boolean isNumeric() {
        return switch (scalarKind()) {
            case SCALAR_KIND_FLOAT, SCALAR_KIND_SIGNED, SCALAR_KIND_UNSIGNED -> true;
            default -> false;
        };
    }

    /**
     * Returns true if this is a floating-point scalar type (float or half).
     */
    public final boolean isFloat() {
        return scalarKind() == SCALAR_KIND_FLOAT;
    }

    /**
     * Returns true if this is a signed scalar type (int or short).
     */
    public final boolean isSigned() {
        return scalarKind() == SCALAR_KIND_SIGNED;
    }

    /**
     * Returns true if this is an unsigned scalar type (uint or ushort).
     */
    public final boolean isUnsigned() {
        return scalarKind() == SCALAR_KIND_UNSIGNED;
    }

    /**
     * Returns true if this is a signed or unsigned integer.
     */
    public final boolean isInteger() {
        return switch (scalarKind()) {
            case SCALAR_KIND_SIGNED, SCALAR_KIND_UNSIGNED -> true;
            default -> false;
        };
    }

    /**
     * Returns true if this is an "opaque type" (an external object which the shader references in
     * some fashion). <a href="https://www.khronos.org/opengl/wiki/Data_Type_(GLSL)#Opaque_types">Link</a>
     */
    public final boolean isOpaque() {
        return mTypeKind == TYPE_KIND_OPAQUE;
    }

    /**
     * Returns the "priority" of a number type, in order of float > half > int > short.
     * When operating on two number types, the result is the higher-priority type.
     */
    public int priority() {
        throw new AssertionError();
    }

    /**
     * Returns true if an instance of this type can be freely coerced (implicitly converted) to
     * another type.
     */
    public final boolean canCoerceTo(Type other) {
        return CoercionCost.isPossible(coercionCost(other));
    }

    /**
     * Determines the "cost" of coercing (implicitly converting) this type to another type. The cost
     * is a number with no particular meaning other than that lower costs are preferable to higher
     * costs. Returns IMPOSSIBLE if the coercion is not possible.
     *
     * @see CoercionCost
     */
    public final int coercionCost(Type other) {
        if (matches(other)) {
            return CoercionCost.free();
        }
        if (typeKind() == other.typeKind() &&
                (isVector() || isMatrix() || isArray())) {
            // Vectors/matrices/arrays of the same size can be coerced if their component type can be.
            if (isMatrix() && (rows() != other.rows() || cols() != other.cols())) {
                return CoercionCost.impossible();
            }
            if (length() != other.length()) {
                return CoercionCost.impossible();
            }
            return getComponentType().coercionCost(other.getComponentType());
        }
        if (isNumeric() && other.isNumeric()) {
            if (scalarKind() != other.scalarKind()) {
                return CoercionCost.impossible();
            } else if (other.priority() >= priority()) {
                return CoercionCost.normal(other.priority() - priority());
            } else {
                return CoercionCost.impossible();
            }
        }
        if (mTypeKind == TYPE_KIND_GENERIC) {
            final Type[] types = getCoercibleTypes();
            for (int i = 0; i < types.length; i++) {
                if (types[i].matches(other)) {
                    return CoercionCost.normal(i + 1);
                }
            }
        }
        return CoercionCost.impossible();
    }

    /**
     * For generic types, returns the types that this generic type can substitute for.
     */
    @Nonnull
    public Type[] getCoercibleTypes() {
        throw new AssertionError();
    }

    /**
     * Coerces the passed-in expression to this type. If the types are incompatible, reports an
     * error and returns null.
     */
    public Expression coerceExpression(Expression expr) {
        if (expr == null || expr.isIncomplete()) {
            return null;
        }
        if (expr.getType().matches(this)) {
            return expr;
        }

        if (!CoercionCost.isPossible(expr.coercionCost(this))) {
            ThreadContext.getInstance().error(expr.mPosition,
                    "expected '" + displayName() + "', but found '" +
                            expr.getType().displayName() + "'");
            return null;
        }

        ThreadContext.getInstance().error(expr.mPosition, "cannot construct '" + displayName() + "'");
        return null;
    }

    /**
     * Returns the minimum value that can fit in the type.
     */
    public double getMinValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the maximum value that can fit in the type.
     */
    public double getMaxValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * For matrices, returns the number of columns (e.g. mat3x4 returns 3).
     * For scalars, returns 1. For all other types, causes an assertion failure.
     */
    public int cols() {
        throw new AssertionError();
    }

    /**
     * For matrices, returns the number of rows (e.g. mat3x4 return 4).
     * For scalars, returns 1. For all other types, causes an assertion failure.
     */
    public int rows() {
        throw new AssertionError();
    }

    /**
     * For arrays, returns either the size of the array (if known) or -1 (unsized).
     * For vectors, returns the number of components (e.g. float3 return 3).
     * For all other types, causes an assertion failure.
     */
    public int length() {
        throw new AssertionError();
    }

    @Nonnull
    public Field[] fields() {
        throw new AssertionError();
    }

    public int dimensions() {
        throw new AssertionError();
    }

    /**
     * True for samplers that sample a depth texture with comparison (e.g., samplerShadow,
     * sampler2DShadow, HLSL SamplerComparisonState).
     */
    public boolean isShadow() {
        throw new AssertionError();
    }

    public boolean isArrayed() {
        throw new AssertionError();
    }

    public final boolean isVoid() {
        return mTypeKind == TYPE_KIND_VOID;
    }

    public boolean isScalar() {
        return false;
    }

    public boolean isVector() {
        return false;
    }

    public boolean isMatrix() {
        return false;
    }

    public boolean isArray() {
        return false;
    }

    public boolean isUnsizedArray() {
        return false;
    }

    public boolean isStruct() {
        return false;
    }

    public boolean isInterfaceBlock() {
        return false;
    }

    public boolean isMultisampled() {
        assert false;
        return false;
    }

    public boolean isSampled() {
        assert false;
        return false;
    }

    public boolean isCombinedSampler() {
        assert false;
        return false;
    }

    public boolean isPureSampler() {
        assert false;
        return false;
    }

    public final boolean hasPrecision() {
        return getComponentType().isNumeric() || isCombinedSampler();
    }

    public final boolean highPrecision() {
        return getBitWidth() >= 32;
    }

    /**
     * Returns the minimum size in bits of the type.
     */
    public int getBitWidth() {
        return 0;
    }

    /**
     * Returns the corresponding vector or matrix type with the specified number of columns and
     * rows.
     */
    public final Type toCompound(ThreadContext context, int cols, int rows) {
        if (!isScalar()) {
            throw new IllegalArgumentException();
        }
        if (cols == 1 && rows == 1) {
            return this;
        }
        if (matches(context.getTypes().mFloat)) {
            return switch (cols) {
                case 1 -> switch (rows) {
                    case 2 -> context.getTypes().mFloat2;
                    case 3 -> context.getTypes().mFloat3;
                    case 4 -> context.getTypes().mFloat4;
                    default -> throw new IllegalArgumentException();
                };
                case 2 -> switch (rows) {
                    case 2 -> context.getTypes().mFloat2x2;
                    case 3 -> context.getTypes().mFloat2x3;
                    case 4 -> context.getTypes().mFloat2x4;
                    default -> throw new IllegalArgumentException();
                };
                case 3 -> switch (rows) {
                    case 2 -> context.getTypes().mFloat3x2;
                    case 3 -> context.getTypes().mFloat3x3;
                    case 4 -> context.getTypes().mFloat3x4;
                    default -> throw new IllegalArgumentException();
                };
                case 4 -> switch (rows) {
                    case 2 -> context.getTypes().mFloat4x2;
                    case 3 -> context.getTypes().mFloat4x3;
                    case 4 -> context.getTypes().mFloat4x4;
                    default -> throw new IllegalArgumentException();
                };
                default -> throw new IllegalArgumentException();
            };
        } else if (matches(context.getTypes().mHalf)) {
            return switch (cols) {
                case 1 -> switch (rows) {
                    case 2 -> context.getTypes().mHalf2;
                    case 3 -> context.getTypes().mHalf3;
                    case 4 -> context.getTypes().mHalf4;
                    default -> throw new IllegalArgumentException();
                };
                case 2 -> switch (rows) {
                    case 2 -> context.getTypes().mHalf2x2;
                    case 3 -> context.getTypes().mHalf2x3;
                    case 4 -> context.getTypes().mHalf2x4;
                    default -> throw new IllegalArgumentException();
                };
                case 3 -> switch (rows) {
                    case 2 -> context.getTypes().mHalf3x2;
                    case 3 -> context.getTypes().mHalf3x3;
                    case 4 -> context.getTypes().mHalf3x4;
                    default -> throw new IllegalArgumentException();
                };
                case 4 -> switch (rows) {
                    case 2 -> context.getTypes().mHalf4x2;
                    case 3 -> context.getTypes().mHalf4x3;
                    case 4 -> context.getTypes().mHalf4x4;
                    default -> throw new IllegalArgumentException();
                };
                default -> throw new IllegalArgumentException();
            };
        } else if (matches(context.getTypes().mDouble)) {
            return switch (cols) {
                case 1 -> switch (rows) {
                    case 2 -> context.getTypes().mDouble2;
                    case 3 -> context.getTypes().mDouble3;
                    case 4 -> context.getTypes().mDouble4;
                    default -> throw new IllegalArgumentException();
                };
                case 2 -> switch (rows) {
                    case 2 -> context.getTypes().mDouble2x2;
                    case 3 -> context.getTypes().mDouble2x3;
                    case 4 -> context.getTypes().mDouble2x4;
                    default -> throw new IllegalArgumentException();
                };
                case 3 -> switch (rows) {
                    case 2 -> context.getTypes().mDouble3x2;
                    case 3 -> context.getTypes().mDouble3x3;
                    case 4 -> context.getTypes().mDouble3x4;
                    default -> throw new IllegalArgumentException();
                };
                case 4 -> switch (rows) {
                    case 2 -> context.getTypes().mDouble4x2;
                    case 3 -> context.getTypes().mDouble4x3;
                    case 4 -> context.getTypes().mDouble4x4;
                    default -> throw new IllegalArgumentException();
                };
                default -> throw new IllegalArgumentException();
            };
        } else if (matches(context.getTypes().mInt)) {
            if (cols == 1) {
                return switch (rows) {
                    case 2 -> context.getTypes().mInt2;
                    case 3 -> context.getTypes().mInt3;
                    case 4 -> context.getTypes().mInt4;
                    default -> throw new IllegalArgumentException();
                };
            }
        } else if (matches(context.getTypes().mShort)) {
            if (cols == 1) {
                return switch (rows) {
                    case 2 -> context.getTypes().mShort2;
                    case 3 -> context.getTypes().mShort3;
                    case 4 -> context.getTypes().mShort4;
                    default -> throw new IllegalArgumentException();
                };
            }
        } else if (matches(context.getTypes().mUInt)) {
            if (cols == 1) {
                return switch (rows) {
                    case 2 -> context.getTypes().mUInt2;
                    case 3 -> context.getTypes().mUInt3;
                    case 4 -> context.getTypes().mUInt4;
                    default -> throw new IllegalArgumentException();
                };
            }
        } else if (matches(context.getTypes().mUShort)) {
            if (cols == 1) {
                return switch (rows) {
                    case 2 -> context.getTypes().mUShort2;
                    case 3 -> context.getTypes().mUShort3;
                    case 4 -> context.getTypes().mUShort4;
                    default -> throw new IllegalArgumentException();
                };
            }
        } else if (matches(context.getTypes().mBool)) {
            if (cols == 1) {
                return switch (rows) {
                    case 2 -> context.getTypes().mBool2;
                    case 3 -> context.getTypes().mBool3;
                    case 4 -> context.getTypes().mBool4;
                    default -> throw new IllegalArgumentException();
                };
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * CoercionCost. The values are packed into a long value.
     *
     * @see #coercionCost(Type)
     */
    public static final class CoercionCost {

        ///// Pack

        public static int free() {
            return 0;
        }

        public static int normal(int cost) {
            assert cost >= 0;
            return cost;
        }

        public static int impossible() {
            // any negative value means impossible
            return Integer.MIN_VALUE;
        }

        ///// METHODS

        public static boolean isPossible(int cost) {
            return cost >= 0;
        }

        ///// OPERATORS

        // Addition of two costs. Saturates at IMPOSSIBLE.
        public static long concat(int lhs, int rhs) {
            // any negative value means impossible
            if (lhs < 0 || rhs < 0) {
                return impossible();
            }
            // overflow becomes impossible (should not happen)
            return normal(lhs + rhs);
        }

        public static int compare(int lhs, int rhs) {
            // any negative value means impossible
            int res = Boolean.compare(lhs < 0, rhs < 0);
            if (res != 0) {
                return res;
            }
            return Integer.compare(lhs, rhs);
        }
    }

    public static final class AliasType extends Type {

        private final Type mUnderlyingType;

        AliasType(String name, Type underlyingType) {
            super(name, underlyingType.getDescriptor(), underlyingType.typeKind());
            mUnderlyingType = underlyingType;
        }

        @Nonnull
        @Override
        public Type resolve() {
            return mUnderlyingType;
        }

        @Nonnull
        @Override
        public Type getComponentType() {
            return mUnderlyingType.getComponentType();
        }

        @Override
        public byte scalarKind() {
            return mUnderlyingType.scalarKind();
        }

        @Override
        public int priority() {
            return mUnderlyingType.priority();
        }

        @Override
        public int cols() {
            return mUnderlyingType.cols();
        }

        @Override
        public int rows() {
            return mUnderlyingType.rows();
        }

        @Override
        public int getBitWidth() {
            return mUnderlyingType.getBitWidth();
        }

        @Override
        public boolean isShadow() {
            return mUnderlyingType.isShadow();
        }

        @Override
        public boolean isArrayed() {
            return mUnderlyingType.isArrayed();
        }

        @Override
        public boolean isScalar() {
            return mUnderlyingType.isScalar();
        }

        @Override
        public boolean isVector() {
            return mUnderlyingType.isVector();
        }

        @Override
        public boolean isMatrix() {
            return mUnderlyingType.isMatrix();
        }

        @Override
        public boolean isArray() {
            return mUnderlyingType.isArray();
        }

        @Override
        public boolean isStruct() {
            return mUnderlyingType.isStruct();
        }

        @Override
        public boolean isInterfaceBlock() {
            return mUnderlyingType.isInterfaceBlock();
        }

        @Nonnull
        @Override
        public Type[] getCoercibleTypes() {
            return mUnderlyingType.getCoercibleTypes();
        }
    }

    public static final class ArrayType extends Type {

        private final Type mElementType;
        private final int mLength;

        ArrayType(Type elementType, int length) {
            super(convert(elementType.getName(), length),
                    convert(elementType.getDescriptor(), length),
                    TYPE_KIND_ARRAY);
            assert (length == UNSIZED_ARRAY || length > 0);
            // Vulkan: disallow multi-dimensional arrays
            if (elementType instanceof ArrayType) {
                throw new IllegalArgumentException("multi-dimensional arrays");
            }
            mElementType = elementType;
            mLength = length;
        }

        @Nonnull
        public static String convert(String s, int length) {
            if (length == UNSIZED_ARRAY) {
                return s + "[]";
            }
            assert (length > 0);
            return s + "[" + length + "]";
        }

        @Override
        public boolean isArray() {
            return true;
        }

        @Override
        public boolean isUnsizedArray() {
            return mLength == UNSIZED_ARRAY;
        }

        @Override
        public int length() {
            return mLength;
        }

        @Nonnull
        @Override
        public Type getElementType() {
            return mElementType;
        }

        @Nonnull
        @Override
        public Type getComponentType() {
            return mElementType.getComponentType();
        }

        @Override
        public int getBitWidth() {
            return mElementType.getBitWidth();
        }
    }
}
