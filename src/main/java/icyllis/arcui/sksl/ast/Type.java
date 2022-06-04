/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.sksl.ast;

import icyllis.arcui.sksl.Qualifiers;
import org.lwjgl.util.spvc.Spv;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * Represents a type, such as int or float4.
 */
public class Type extends Symbol {

    public static final int MAX_ABBREV_LENGTH = 3;

    /**
     * Kinds of Type.
     */
    public static final byte
            KIND_ARRAY = 0,
            KIND_GENERIC = 1,
            KIND_LITERAL = 2,
            KIND_MATRIX = 3,
            KIND_OTHER = 4,
            KIND_SAMPLER = 5,
            KIND_SEPARATE_SAMPLER = 6,
            KIND_SCALAR = 7,
            KIND_STRUCT = 8,
            KIND_TEXTURE = 9,
            KIND_VECTOR = 10,
            KIND_VOID = 11,
            KIND_COLOR_FILTER = 12,
            KIND_SHADER = 13,
            KIND_BLENDER = 14;

    /**
     * Kinds of ScalarType.
     */
    public static final byte
            SCALAR_KIND_FLOAT = 0,
            SCALAR_KIND_SIGNED = 1,
            SCALAR_KIND_UNSIGNED = 2,
            SCALAR_KIND_BOOLEAN = 3,
            SCALAR_KIND_DEFAULT = 4;

    /**
     * CoercionCost.
     */
    public static final class CoercionCost {

        ///// CONSTANTS

        public static final long
                FREE = 0,
                IMPOSSIBLE = 1L << (31 + 31);

        ///// CONSTRUCTORS

        public static long makeNormalCost(int normalCost) {
            assert normalCost >= 0;
            return normalCost;
        }

        public static long makeNarrowingCost(int narrowingCost) {
            assert narrowingCost >= 0;
            return (long) narrowingCost << 31;
        }

        public static long makeCoercionCost(int normalCost, int narrowingCost, boolean impossible) {
            assert normalCost >= 0;
            assert narrowingCost >= 0;
            return makeNormalCost(normalCost) |
                    makeNarrowingCost(narrowingCost) |
                    (impossible ? IMPOSSIBLE : 0);
        }

        ///// GETTERS

        public static int getNormalCost(long coercionCost) {
            return (int) (coercionCost & 0x7FFFFFFF);
        }

        public static int getNarrowingCost(long coercionCost) {
            return (int) ((coercionCost >> 31) & 0x7FFFFFFF);
        }

        public static boolean getImpossible(long coercionCost) {
            return (coercionCost & IMPOSSIBLE) != 0;
        }

        ///// METHODS

        public static boolean isImpossible(long coercionCost, boolean allowNarrowing) {
            return !getImpossible(coercionCost) && (allowNarrowing || getNarrowingCost(coercionCost) == 0);
        }

        ///// OPERATORS

        // Addition of two costs. Saturates at IMPOSSIBLE.
        public static long add(long lhs, long rhs) {
            if (getImpossible(lhs) || getImpossible(rhs)) {
                return IMPOSSIBLE;
            }
            return makeCoercionCost(getNormalCost(lhs) + getNormalCost(rhs),
                    getNarrowingCost(lhs) + getNarrowingCost(rhs), false);
        }

        public static int compare(long lhs, long rhs) {
            int res = Boolean.compare(getImpossible(lhs), getImpossible(rhs));
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

    /**
     * Struct member.
     *
     * @param start the start offset
     * @param end   the end offset
     */
    public record Field(int start, int end, Qualifiers qualifiers, String name, Type type) {

        @Nonnull
        public String getDescription() {
            return type.getDisplayName() + " " + name + ";";
        }
    }

    private final String mAbbrev;
    private final byte mTypeKind;

    Type(String name, String abbrev, byte kind) {
        this(name, abbrev, kind, -1, -1);
    }

    Type(String name, String abbrev, byte kind, int start, int end) {
        super(start, end, name, null);
        assert abbrev.length() <= MAX_ABBREV_LENGTH;
        mAbbrev = abbrev;
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
        return new GenericType(name, List.of(types));
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
    public final Type getType() {
        return this;
    }

    /**
     * If this is an alias, returns the underlying type, otherwise returns this.
     */
    @Nonnull
    public Type getResolvedType() {
        return this;
    }

    /**
     * For matrices and vectors, returns the type of individual cells (e.g. mat2 has a component
     * type of Float). For arrays, returns the base type. For all other types, returns the type
     * itself.
     */
    @Nonnull
    public Type getComponentType() {
        return this;
    }

    /**
     * For texture samplers, returns the type of texture it samples (e.g., sampler2D has
     * a texture type of texture2D).
     */
    @Nonnull
    public Type getTextureType() {
        assert false;
        return this;
    }

    @Nonnull
    public Type getLiteralScalarType() {
        return this;
    }

    /**
     * Returns true if these types are equal after alias resolution.
     */
    public final boolean matches(@Nonnull Type other) {
        return getResolvedType().getName().equals(other.getResolvedType().getName());
    }

    /**
     * Returns an abbreviated name of the type, meant for name-mangling. (e.g. float4x4 -> f44)
     */
    @Nonnull
    public final String getAbbreviation() {
        return mAbbrev;
    }

    /**
     * Returns the category (scalar, vector, matrix, etc.) of this type.
     */
    public final byte getTypeKind() {
        return mTypeKind;
    }

    /**
     * Returns the ScalarKind of this type (always DEFAULT for non-scalar values).
     */
    public byte getScalarKind() {
        return SCALAR_KIND_DEFAULT;
    }

    /**
     * Converts a component type and a size (float, 10) into an array name ("float[10]").
     */
    public final String getArrayName(int arraySize) {
        return String.format("%s[%d]", getName(), arraySize);
    }

    @Nonnull
    public final String getDisplayName() {
        return getLiteralScalarType().getName();
    }

    @Nonnull
    @Override
    public final String getDescription() {
        return getDisplayName();
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
        return getName().startsWith("$");
    }

    /**
     * Returns true if this type is a bool.
     */
    public final boolean isBoolean() {
        return getScalarKind() == SCALAR_KIND_BOOLEAN;
    }

    /**
     * Returns true if this is a numeric scalar type.
     */
    public final boolean isNumber() {
        return switch (getScalarKind()) {
            case SCALAR_KIND_FLOAT, SCALAR_KIND_SIGNED, SCALAR_KIND_UNSIGNED -> true;
            default -> false;
        };
    }

    /**
     * Returns true if this is a floating-point scalar type (float or half).
     */
    public final boolean isFloat() {
        return getScalarKind() == SCALAR_KIND_FLOAT;
    }

    /**
     * Returns true if this is a signed scalar type (int or short).
     */
    public final boolean isSigned() {
        return getScalarKind() == SCALAR_KIND_SIGNED;
    }

    /**
     * Returns true if this is an unsigned scalar type (uint or ushort).
     */
    public final boolean isUnsigned() {
        return getScalarKind() == SCALAR_KIND_UNSIGNED;
    }

    /**
     * Returns true if this is a signed or unsigned integer.
     */
    public final boolean isInteger() {
        return switch (getScalarKind()) {
            case SCALAR_KIND_SIGNED, SCALAR_KIND_UNSIGNED -> true;
            default -> false;
        };
    }

    /**
     * Returns true if this is an "opaque type" (an external object which the shader references in
     * some fashion). https://www.khronos.org/opengl/wiki/Data_Type_(GLSL)#Opaque_types
     */
    public final boolean isOpaque() {
        return switch (mTypeKind) {
            case KIND_SAMPLER,
                    KIND_SEPARATE_SAMPLER,
                    KIND_TEXTURE,
                    KIND_COLOR_FILTER,
                    KIND_SHADER,
                    KIND_BLENDER -> true;
            default -> false;
        };
    }

    /**
     * Returns the "priority" of a number type, in order of float > half > int > short.
     * When operating on two number types, the result is the higher-priority type.
     */
    public int getPriority() {
        assert false;
        return -1;
    }

    /**
     * Returns true if an instance of this type can be freely coerced (implicitly converted) to
     * another type.
     */
    public final boolean canCoerceTo(Type other, boolean allowNarrowing) {
        return CoercionCost.isImpossible(getCoercionCost(other), allowNarrowing);
    }

    /**
     * Determines the "cost" of coercing (implicitly converting) this type to another type. The cost
     * is a number with no particular meaning other than that lower costs are preferable to higher
     * costs. Returns IMPOSSIBLE if the coercion is not possible.
     */
    public final long getCoercionCost(Type other) {
        if (matches(other)) {
            return CoercionCost.FREE;
        }
        if (mTypeKind == other.mTypeKind && (isVector() || isMatrix() || isArray())) {
            // Vectors/matrices/arrays of the same size can be coerced if their component type can be.
            if (isMatrix() && (getRows() != other.getRows())) {
                return CoercionCost.IMPOSSIBLE;
            }
            if (getColumns() != other.getColumns()) {
                return CoercionCost.IMPOSSIBLE;
            }
            return getComponentType().getCoercionCost(other.getComponentType());
        }
        if (isNumber() && other.isNumber()) {
            if (isLiteral() && isInteger()) {
                return CoercionCost.FREE;
            } else if (getScalarKind() != other.getScalarKind()) {
                return CoercionCost.IMPOSSIBLE;
            } else if (other.getPriority() >= getPriority()) {
                return CoercionCost.makeNormalCost(other.getPriority() - getPriority());
            } else {
                return CoercionCost.makeNarrowingCost(getPriority() - other.getPriority());
            }
        }
        if (mTypeKind == KIND_GENERIC) {
            final List<Type> types = getCoercibleTypes();
            for (int i = 0; i < types.size(); i++) {
                if (types.get(i).matches(other)) {
                    return CoercionCost.makeNormalCost(i + 1);
                }
            }
        }
        return CoercionCost.IMPOSSIBLE;
    }

    /**
     * For generic types, returns the types that this generic type can substitute for.
     */
    @Nonnull
    public List<Type> getCoercibleTypes() {
        assert false;
        return Collections.emptyList();
    }

    /**
     * For integer types, returns the minimum value that can fit in the type.
     */
    public final long getMinimumValue() {
        assert isInteger();
        return isUnsigned() ? 0 : -(1L << (getBitWidth() - 1));
    }

    /**
     * For integer types, returns the maximum value that can fit in the type.
     */
    public final long getMaximumValue() {
        assert isInteger();
        return (isUnsigned() ? (1L << getBitWidth())
                : (1L << (getBitWidth() - 1))) - 1;
    }

    /**
     * For matrices and vectors, returns the number of columns (e.g. both mat3 and float3 return 3).
     * For scalars, returns 1. For arrays, returns either the size of the array (if known) or -1.
     * For all other types, causes an assertion failure.
     */
    public int getColumns() {
        assert false;
        return -1;
    }

    /**
     * For matrices, returns the number of rows (e.g. mat2x4 returns 4). For vectors and scalars,
     * returns 1. For all other types, causes an assertion failure.
     */
    public int getRows() {
        assert false;
        return -1;
    }

    /**
     * Returns the number of scalars needed to hold this type.
     */
    public int getSlots() {
        return 0;
    }

    public int getDimensions() {
        assert false;
        return Spv.SpvDim1D;
    }

    public boolean isDepth() {
        assert false;
        return false;
    }

    public boolean isLayered() {
        assert false;
        return false;
    }

    public final boolean isVoid() {
        return mTypeKind == KIND_VOID;
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
        return mTypeKind == KIND_COLOR_FILTER ||
                mTypeKind == KIND_SHADER ||
                mTypeKind == KIND_BLENDER;
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
        return getComponentType().isNumber() || mTypeKind == KIND_SAMPLER;
    }

    public final boolean highPrecision() {
        return getBitWidth() >= 32;
    }

    public int getBitWidth() {
        return 0;
    }
}
