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

package icyllis.arctic.sksl.ir;

import icyllis.arctic.sksl.Context;
import icyllis.arctic.sksl.Modifiers;
import org.lwjgl.util.spvc.Spv;

import javax.annotation.Nonnull;

/**
 * Represents a type, such as int or float4.
 */
public class Type extends Symbol {

    public static final int MAX_ABBREV_LENGTH = 3;

    /**
     * Struct member.
     *
     * @param start the start offset
     * @param end   the end offset
     */
    public record Field(int start, int end, Modifiers modifiers, String name, Type type) {

        @Nonnull
        public String description() {
            return type.displayName() + " " + name + ";";
        }
    }

    /**
     * Kinds of Type.
     */
    public static final byte
            TypeKind_Array = 0,
            TypeKind_Generic = 1,
            TypeKind_Literal = 2,
            TypeKind_Matrix = 3,
            TypeKind_Other = 4,
            TypeKind_Sampler = 5,
            TypeKind_SeparateSampler = 6,
            TypeKind_Scalar = 7,
            TypeKind_Struct = 8,
            TypeKind_Texture = 9,
            TypeKind_Vector = 10,
            TypeKind_Void = 11;
    public static final byte
            TypeKind_ColorFilter = 12,
            TypeKind_Shader = 13,
            TypeKind_Blender = 14;

    /**
     * Kinds of ScalarType.
     */
    public static final byte
            ScalarKind_Float = 0,
            ScalarKind_Signed = 1,
            ScalarKind_Unsigned = 2,
            ScalarKind_Boolean = 3,
            ScalarKind_NonScalar = 4;

    private final String mAbbreviatedName;
    private final byte mTypeKind;

    Type(String name, String abbrev, byte kind) {
        this(name, abbrev, kind, -1, -1);
    }

    Type(String name, String abbrev, byte kind, int start, int end) {
        super(start, end, Kind_Type, name, null);
        assert (abbrev.length() <= MAX_ABBREV_LENGTH);
        mAbbreviatedName = abbrev;
        mTypeKind = kind;
    }

    /**
     * Creates an alias which maps to another type.
     */
    @Nonnull
    public static Type makeAliasType(String name, Type targetType) {
        return new AliasType(name, targetType);
    }

    /**
     * Creates an array type.
     */
    @Nonnull
    public static Type makeArrayType(String name, Type componentType, int columns) {
        return new ArrayType(name, componentType, columns);
    }

    /**
     * Create a generic type which maps to the listed types--e.g. $genType is a generic type which
     * can match float, float2, float3 or float4.
     */
    @Nonnull
    public static Type makeGenericType(String name, Type... types) {
        return new GenericType(name, types);
    }

    /**
     * Create a type for literal scalars.
     */
    @Nonnull
    public static Type makeLiteralType(String name, Type scalarType, int priority) {
        return new LiteralType(name, scalarType, priority);
    }

    /**
     * Create a matrix type.
     */
    @Nonnull
    public static Type makeMatrixType(String name, String abbrev, Type componentType,
                                      int columns, int rows) {
        return new MatrixType(name, abbrev, componentType, columns, rows);
    }

    /**
     * Create a sampler type.
     */
    @Nonnull
    public static Type makeSamplerType(String name, Type textureType) {
        return new SamplerType(name, textureType);
    }

    /**
     * Create a scalar type.
     */
    @Nonnull
    public static Type makeScalarType(String name, String abbrev, byte scalarKind, int priority,
                                      int bitWidth) {
        return new ScalarType(name, abbrev, scalarKind, priority, bitWidth);
    }

    /**
     * Create a "special" type with the given name, abbreviation, and TypeKind.
     */
    @Nonnull
    public static Type makeSpecialType(String name, String abbrev, byte typeKind) {
        return new Type(name, abbrev, typeKind);
    }

    /**
     * Create a texture type.
     *
     * @param dimensions SpvDim, for example, {@link Spv#SpvDim1D}
     */
    @Nonnull
    public static Type makeTextureType(String name, int dimensions, boolean isDepth, boolean isLayered,
                                       boolean isMultisampled, boolean isSampled) {
        return new TextureType(name, dimensions, isDepth, isLayered, isMultisampled, isSampled);
    }

    /**
     * Create a vector type.
     */
    @Nonnull
    public static Type makeVectorType(String name, String abbrev, Type componentType, int columns) {
        return new VectorType(name, abbrev, componentType, columns);
    }

    /**
     * Returns this.
     */
    @Nonnull
    @Override
    public final Type type() {
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
     * For matrices and vectors, returns the type of individual cells (e.g. mat2 has a component
     * type of Float). For arrays, returns the base type. For all other types, returns the type
     * itself.
     */
    @Nonnull
    public Type componentType() {
        return this;
    }

    /**
     * For texture samplers, returns the type of texture it samples (e.g., sampler2D has
     * a texture type of texture2D).
     */
    @Nonnull
    public Type textureType() {
        throw new AssertionError();
    }

    @Nonnull
    public Type scalarTypeForLiteral() {
        return this;
    }

    /**
     * Returns true if these types are equal after alias resolution.
     */
    public final boolean matches(@Nonnull Type other) {
        return resolve().name().equals(other.resolve().name());
    }

    /**
     * Returns an abbreviated name of the type, meant for name-mangling. (e.g. float4x4 -> f44)
     */
    @Nonnull
    public final String abbreviatedName() {
        return mAbbreviatedName;
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
        return ScalarKind_NonScalar;
    }

    /**
     * Converts a component type and a size (float, 10) into an array name ("float[10]").
     */
    public final String getArrayName(int arraySize) {
        return String.format("%s[%d]", name(), arraySize);
    }

    @Nonnull
    public final String displayName() {
        return scalarTypeForLiteral().name();
    }

    @Nonnull
    @Override
    public final String description() {
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
     * Returns true if this type is either private, or contains a private field (recursively).
     */
    public boolean isPrivate() {
        return name().startsWith("$");
    }

    /**
     * Returns true if this type is a bool.
     */
    public final boolean isBoolean() {
        return scalarKind() == ScalarKind_Boolean;
    }

    /**
     * Returns true if this is a numeric scalar type.
     */
    public final boolean isNumber() {
        return switch (scalarKind()) {
            case ScalarKind_Float, ScalarKind_Signed, ScalarKind_Unsigned -> true;
            default -> false;
        };
    }

    /**
     * Returns true if this is a floating-point scalar type (float or half).
     */
    public final boolean isFloat() {
        return scalarKind() == ScalarKind_Float;
    }

    /**
     * Returns true if this is a signed scalar type (int or short).
     */
    public final boolean isSigned() {
        return scalarKind() == ScalarKind_Signed;
    }

    /**
     * Returns true if this is an unsigned scalar type (uint or ushort).
     */
    public final boolean isUnsigned() {
        return scalarKind() == ScalarKind_Unsigned;
    }

    /**
     * Returns true if this is a signed or unsigned integer.
     */
    public final boolean isInteger() {
        return switch (scalarKind()) {
            case ScalarKind_Signed, ScalarKind_Unsigned -> true;
            default -> false;
        };
    }

    /**
     * Returns true if this is an "opaque type" (an external object which the shader references in
     * some fashion). <a href="https://www.khronos.org/opengl/wiki/Data_Type_(GLSL)#Opaque_types">Link</a>
     */
    public final boolean isOpaque() {
        return switch (mTypeKind) {
            case TypeKind_Sampler,
                    TypeKind_SeparateSampler,
                    TypeKind_Texture,
                    TypeKind_ColorFilter,
                    TypeKind_Shader,
                    TypeKind_Blender -> true;
            default -> false;
        };
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
    public final boolean canCoerceTo(Type other, boolean allowNarrowing) {
        return CoercionCost.isPossible(coercionCost(other), allowNarrowing);
    }

    /**
     * Determines the "cost" of coercing (implicitly converting) this type to another type. The cost
     * is a number with no particular meaning other than that lower costs are preferable to higher
     * costs. Returns IMPOSSIBLE if the coercion is not possible.
     *
     * @see CoercionCost
     */
    public final long coercionCost(Type other) {
        if (matches(other)) {
            return CoercionCost.free();
        }
        if (typeKind() == other.typeKind() &&
                (isVector() || isMatrix() || isArray())) {
            // Vectors/matrices/arrays of the same size can be coerced if their component type can be.
            if (isMatrix() && (rows() != other.rows())) {
                return CoercionCost.impossible();
            }
            if (columns() != other.columns()) {
                return CoercionCost.impossible();
            }
            return componentType().coercionCost(other.componentType());
        }
        if (isNumber() && other.isNumber()) {
            if (isLiteral() && isInteger()) {
                return CoercionCost.free();
            } else if (scalarKind() != other.scalarKind()) {
                return CoercionCost.impossible();
            } else if (other.priority() >= priority()) {
                return CoercionCost.normal(other.priority() - priority());
            } else {
                return CoercionCost.narrowing(priority() - other.priority());
            }
        }
        if (mTypeKind == TypeKind_Generic) {
            final Type[] types = coercibleTypes();
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
    public Type[] coercibleTypes() {
        throw new AssertionError();
    }

    /**
     * For integer types, returns the minimum value that can fit in the type.
     */
    public long minimumValue() {
        assert isInteger();
        return isUnsigned() ? 0 : -(1L << (bitWidth() - 1));
    }

    /**
     * For integer types, returns the maximum value that can fit in the type.
     */
    public long maximumValue() {
        assert isInteger();
        return (isUnsigned() ? (1L << bitWidth())
                : (1L << (bitWidth() - 1))) - 1;
    }

    /**
     * For matrices and vectors, returns the number of columns (e.g. both mat3 and float3 return 3).
     * For scalars, returns 1. For arrays, returns either the size of the array (if known) or -1.
     * For all other types, causes an assertion failure.
     */
    public int columns() {
        throw new AssertionError();
    }

    /**
     * For matrices, returns the number of rows (e.g. mat2x4 returns 4). For vectors and scalars,
     * returns 1. For all other types, causes an assertion failure.
     */
    public int rows() {
        throw new AssertionError();
    }

    /**
     * Returns the number of scalars needed to hold this type.
     */
    public int slotCount() {
        return 0;
    }

    @Nonnull
    public Field[] fields() {
        throw new AssertionError();
    }

    public int dimensions() {
        throw new AssertionError();
    }

    public boolean isDepth() {
        throw new AssertionError();
    }

    public boolean isLayered() {
        throw new AssertionError();
    }

    public final boolean isVoid() {
        return mTypeKind == TypeKind_Void;
    }

    public boolean isScalar() {
        return false;
    }

    public boolean isLiteral() {
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

    public boolean isStruct() {
        return false;
    }

    public boolean isInterfaceBlock() {
        return false;
    }

    // Is this type something that can be bound & sampled from an RuntimeEffect?
    // Includes types that represent stages of the ENGINE (colorFilter, shader, blender).
    public final boolean isEffectChild() {
        return mTypeKind == TypeKind_ColorFilter ||
                mTypeKind == TypeKind_Shader ||
                mTypeKind == TypeKind_Blender;
    }

    public boolean isMultisampled() {
        assert false;
        return false;
    }

    public boolean isSampled() {
        assert false;
        return false;
    }

    public final boolean hasPrecision() {
        return componentType().isNumber() || mTypeKind == TypeKind_Sampler;
    }

    public final boolean highPrecision() {
        return bitWidth() >= 32;
    }

    public int bitWidth() {
        return 0;
    }

    /**
     * Returns the corresponding vector or matrix type with the specified number of columns and
     * rows.
     */
    public final Type toCompound(Context context, int columns, int rows) {
        assert (isScalar());
        if (columns == 1 && rows == 1) {
            return this;
        }
        if (matches(context.mTypes.mFloat) || matches(context.mTypes.mFloatLiteral)) {
            return switch (rows) {
                case 1 -> switch (columns) {
                    case 2 -> context.mTypes.mFloat2;
                    case 3 -> context.mTypes.mFloat3;
                    case 4 -> context.mTypes.mFloat4;
                    default -> throw new IllegalStateException();
                };
                case 2 -> switch (columns) {
                    case 2 -> context.mTypes.mFloat2x2;
                    case 3 -> context.mTypes.mFloat3x2;
                    case 4 -> context.mTypes.mFloat4x2;
                    default -> throw new IllegalStateException();
                };
                case 3 -> switch (columns) {
                    case 2 -> context.mTypes.mFloat2x3;
                    case 3 -> context.mTypes.mFloat3x3;
                    case 4 -> context.mTypes.mFloat4x3;
                    default -> throw new IllegalStateException();
                };
                case 4 -> switch (columns) {
                    case 2 -> context.mTypes.mFloat2x4;
                    case 3 -> context.mTypes.mFloat3x4;
                    case 4 -> context.mTypes.mFloat4x4;
                    default -> throw new IllegalStateException();
                };
                default -> throw new IllegalStateException();
            };
        } else if (matches(context.mTypes.mHalf)) {
            return switch (rows) {
                case 1 -> switch (columns) {
                    case 2 -> context.mTypes.mHalf2;
                    case 3 -> context.mTypes.mHalf3;
                    case 4 -> context.mTypes.mHalf4;
                    default -> throw new IllegalStateException();
                };
                case 2 -> switch (columns) {
                    case 2 -> context.mTypes.mHalf2x2;
                    case 3 -> context.mTypes.mHalf3x2;
                    case 4 -> context.mTypes.mHalf4x2;
                    default -> throw new IllegalStateException();
                };
                case 3 -> switch (columns) {
                    case 2 -> context.mTypes.mHalf2x3;
                    case 3 -> context.mTypes.mHalf3x3;
                    case 4 -> context.mTypes.mHalf4x3;
                    default -> throw new IllegalStateException();
                };
                case 4 -> switch (columns) {
                    case 2 -> context.mTypes.mHalf2x4;
                    case 3 -> context.mTypes.mHalf3x4;
                    case 4 -> context.mTypes.mHalf4x4;
                    default -> throw new IllegalStateException();
                };
                default -> throw new IllegalStateException();
            };
        } else if (matches(context.mTypes.mInt) || matches(context.mTypes.mIntLiteral)) {
            if (rows == 1) {
                return switch (columns) {
                    case 2 -> context.mTypes.mInt2;
                    case 3 -> context.mTypes.mInt3;
                    case 4 -> context.mTypes.mInt4;
                    default -> throw new IllegalStateException();
                };
            }
            throw new IllegalStateException();
        } else if (matches(context.mTypes.mShort)) {
            if (rows == 1) {
                return switch (columns) {
                    case 2 -> context.mTypes.mShort2;
                    case 3 -> context.mTypes.mShort3;
                    case 4 -> context.mTypes.mShort4;
                    default -> throw new IllegalStateException();
                };
            }
            throw new IllegalStateException();
        } else if (matches(context.mTypes.mUInt)) {
            if (rows == 1) {
                return switch (columns) {
                    case 2 -> context.mTypes.mUInt2;
                    case 3 -> context.mTypes.mUInt3;
                    case 4 -> context.mTypes.mUInt4;
                    default -> throw new IllegalStateException();
                };
            }
            throw new IllegalStateException();
        } else if (matches(context.mTypes.mUShort)) {
            if (rows == 1) {
                return switch (columns) {
                    case 2 -> context.mTypes.mUShort2;
                    case 3 -> context.mTypes.mUShort3;
                    case 4 -> context.mTypes.mUShort4;
                    default -> throw new IllegalStateException();
                };
            }
            throw new IllegalStateException();
        } else if (matches(context.mTypes.mBool)) {
            if (rows == 1) {
                return switch (columns) {
                    case 2 -> context.mTypes.mBool2;
                    case 3 -> context.mTypes.mBool3;
                    case 4 -> context.mTypes.mBool4;
                    default -> throw new IllegalStateException();
                };
            }
            throw new IllegalStateException();
        }
        throw new IllegalStateException();
    }

    /**
     * CoercionCost. The values are packed into a long value.
     *
     * @see #coercionCost(Type)
     */
    public static final class CoercionCost {

        ///// Pack

        public static long free() {
            return 0;
        }

        public static long normal(int cost) {
            assert cost >= 0;
            return cost;
        }

        public static long narrowing(int cost) {
            assert cost >= 0;
            return (long) cost << 32;
        }

        public static long impossible() {
            // any negative value means impossible
            return 0x80000000_80000000L;
        }

        private static long make(int normalCost, int narrowingCost) {
            return normal(normalCost) | narrowing(narrowingCost);
        }

        ///// Unpack

        public static int getNormalCost(long coercionCost) {
            return (int) coercionCost;
        }

        public static int getNarrowingCost(long coercionCost) {
            return (int) (coercionCost >> 32);
        }

        public static boolean isImpossible(long coercionCost) {
            // any negative value means impossible
            return (coercionCost & impossible()) != 0;
        }

        ///// METHODS

        public static boolean isPossible(long coercionCost, boolean allowNarrowing) {
            return !isImpossible(coercionCost) && (allowNarrowing || getNarrowingCost(coercionCost) == 0);
        }

        ///// OPERATORS

        // Addition of two costs. Saturates at IMPOSSIBLE.
        public static long concat(long lhs, long rhs) {
            if (isImpossible(lhs) || isImpossible(rhs)) {
                return impossible();
            }
            // overflow becomes impossible (should not happen)
            return make(getNormalCost(lhs) + getNormalCost(rhs),
                    getNarrowingCost(lhs) + getNarrowingCost(rhs));
        }

        public static int compare(long lhs, long rhs) {
            // order is, impossible, then narrowing, then normal
            int res = Boolean.compare(isImpossible(lhs), isImpossible(rhs));
            if (res != 0) {
                return res;
            }
            res = Integer.compare(getNarrowingCost(lhs), getNarrowingCost(rhs));
            if (res != 0) {
                return res;
            }
            return Integer.compare(getNormalCost(lhs), getNormalCost(rhs));
        }
    }
}
