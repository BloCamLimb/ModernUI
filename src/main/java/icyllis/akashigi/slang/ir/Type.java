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

import icyllis.akashigi.slang.*;
import org.lwjgl.util.spvc.Spv;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a type symbol, such as int or float4.
 */
public class Type extends Symbol {

    public static final int kUnsizedArray = -1; // an unsized array (declared with [])

    /**
     * @param position  see {@link Node#makeRange(int, int)}
     * @param modifiers see {@link Modifier}
     */
    public record Field(int position, Layout layout, int modifiers, String name, Type type) {

        @Nonnull
        @Override
        public String toString() {
            return type.getName() + " " + name + ";";
        }
    }

    /**
     * Kinds of Type.
     */
    public static final byte
            kArray_TypeKind = 0,
            kGeneric_TypeKind = 1,  // private
            kMatrix_TypeKind = 2,
            kOther_TypeKind = 3,
            kSampler_TypeKind = 4,  // sampler/image
            kScalar_TypeKind = 5,
            kStruct_TypeKind = 6,
            kVector_TypeKind = 7,
            kVoid_TypeKind = 8;

    /**
     * Kinds of ScalarType.
     */
    public static final byte
            kFloat_ScalarKind = 0,
            kSigned_ScalarKind = 1,
            kUnsigned_ScalarKind = 2,
            kBoolean_ScalarKind = 3,
            kNonScalar_ScalarKind = 4;

    private final String mAbbr;
    private final byte mTypeKind;

    Type(String name, String abbr, byte typeKind) {
        this(name, abbr, typeKind, -1);
    }

    Type(String name, String abbr, byte typeKind, int position) {
        super(position, SymbolKind.kType, name);
        mAbbr = abbr;
        mTypeKind = typeKind;
    }

    /**
     * Creates an alias which maps to another type.
     */
    @Nonnull
    public static Type makeAliasType(String name, Type type) {
        assert (type == type.resolve());
        return new AliasType(name, type);
    }

    /**
     * Create a generic type which maps to the listed types
     * (e.g. __genFType is a generic type which can match float, float2, float3 or float4).
     */
    @Nonnull
    public static Type makeGenericType(String name, Type... types) {
        for (Type type : types) {
            assert (type == type.resolve());
        }
        return new GenericType(name, types);
    }

    /**
     * Create a scalar type.
     */
    @Nonnull
    public static Type makeScalarType(String name, String abbr,
                                      byte kind, int rank, int width) {
        return new ScalarType(name, abbr, kind, rank, width);
    }

    /**
     * Create a vector type.
     *
     * @param type a scalar type
     */
    @Nonnull
    public static Type makeVectorType(String name, String abbr,
                                      Type type, int rows) {
        assert (type == type.resolve());
        return new VectorType(name, abbr, type, rows);
    }

    /**
     * Create a matrix type.
     *
     * @param type a scalar type
     */
    @Nonnull
    public static Type makeMatrixType(String name, String abbr,
                                      Type type, int cols, int rows) {
        assert (type == type.resolve());
        return new MatrixType(name, abbr, type, cols, rows);
    }

    /**
     * Create a sampler/image type. Includes images, textures without sampler,
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
     * @param type       e.g. texture2D has a type of half
     * @param dimensions SpvDim (e.g. {@link Spv#SpvDim1D})
     */
    @Nonnull
    public static Type makeSamplerType(String name, String abbr, Type type, int dimensions,
                                       boolean isShadow, boolean isArrayed, boolean isMultiSampled,
                                       boolean isSampled, boolean isSampler) {
        assert (type == type.resolve());
        return new SamplerType(name, abbr, type, dimensions, isShadow, isArrayed,
                isMultiSampled, isSampled, isSampler);
    }

    /**
     * Create an image or subpass type.
     */
    @Nonnull
    public static Type makeImageType(String name, String abbr, Type type, int dimensions,
                                     boolean isArrayed, boolean isMultiSampled) {
        assert (type.isScalar());
        return makeSamplerType(name, abbr, type, dimensions, /*isShadow=*/false, isArrayed,
                isMultiSampled, /*isSampled=*/false, /*isSampler*/false);
    }

    /**
     * Create a texture type.
     */
    @Nonnull
    public static Type makeTextureType(String name, String abbr, Type type, int dimensions,
                                       boolean isArrayed, boolean isMultiSampled) {
        assert (type.isScalar());
        return makeSamplerType(name, abbr, type, dimensions, /*isShadow=*/false, isArrayed,
                isMultiSampled, /*isSampled=*/true, /*isSampler*/false);
    }

    /**
     * Create a separate sampler type.
     */
    @Nonnull
    public static Type makeSeparateType(String name, String abbr, Type type, boolean isShadow) {
        assert (type.isVoid());
        return makeSamplerType(name, abbr, type, /*dimensions*/-1, isShadow, /*isArrayed*/false,
                /*isMultiSampled*/false, /*isSampled=*/false, /*isSampler*/true);
    }

    /**
     * Create a combined sampler type.
     */
    @Nonnull
    public static Type makeCombinedType(String name, String abbr, Type type, int dimensions,
                                        boolean isShadow, boolean isArrayed, boolean isMultiSampled) {
        assert (type.isScalar());
        return makeSamplerType(name, abbr, type, dimensions, isShadow, isArrayed,
                isMultiSampled, /*isSampled=*/true, /*isSampler*/true);
    }

    /**
     * Create a "special" type with the given name.
     */
    @Nonnull
    public static Type makeSpecialType(String name, String abbr, byte kind) {
        return new Type(name, abbr, kind);
    }

    /**
     * Creates an array type.
     */
    @Nonnull
    public static Type makeArrayType(Type type, int length) {
        return new ArrayType(type, length);
    }

    /**
     * Creates a struct type with the given fields. Reports an error if the struct is ill-formed.
     */
    @Nonnull
    public static Type makeStructType(int position, String name, Field[] fields, boolean interfaceBlock) {
        ThreadContext context = ThreadContext.getInstance();
        for (Field field : fields) {
            if (field.modifiers() != 0) {
                String desc = Modifier.describeFlags(field.modifiers());
                context.error(field.position(),
                        "modifier '" + desc + "' is not permitted on a struct field");
            }
            if ((field.layout().flags() & Layout.kBinding_Flag) != 0) {
                context.error(field.position(),
                        "layout qualifier 'binding' is not permitted on a struct field");
            }
            if ((field.layout().flags() & Layout.kSet_Flag) != 0) {
                context.error(field.position(),
                        "layout qualifier 'set' is not permitted on a struct field");
            }
            if (field.type().isVoid()) {
                context.error(field.position(), "type 'void' is not permitted in a struct");
            }
            if (field.type().isOpaque()) {
                context.error(field.position(), "opaque type '" + field.type().getName() +
                        "' is not permitted in a struct");
            }
        }
        for (Field field : fields) {
            if (isTooDeeplyNested(field.type(), 8)) {
                context.error(position, "struct '" + name + "' is too deeply nested");
                break;
            }
        }
        return new StructType(position, name, fields, interfaceBlock);
    }

    private static boolean isTooDeeplyNested(Type t, int limit) {
        if (limit <= 0) {
            return true;
        }

        if (t.isStruct()) {
            for (Field f : t.getFields()) {
                if (isTooDeeplyNested(f.type(), limit - 1)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nonnull
    @Override
    public final Type getType() {
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
     * For matrices and vectors, returns the scalar type of individual cells (e.g. mat2 has a component
     * type of Float). For textures, returns the sampled type (e.g. texture2D has a component type
     * of Half). For all other types, returns the type itself.
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
     * Returns an abbreviation of the type, meant for name-mangling. (e.g. float4x4 -> f44)
     */
    @Nonnull
    public final String getAbbr() {
        return mAbbr;
    }

    /**
     * Returns the category (scalar, vector, matrix, etc.) of this type.
     */
    public final byte getTypeKind() {
        return mTypeKind;
    }

    /**
     * Returns the ScalarKind of this type (always NonScalar for non-scalar values).
     */
    public byte getScalarKind() {
        return kNonScalar_ScalarKind;
    }

    @Nonnull
    @Override
    public final String toString() {
        return getName();
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
        return getScalarKind() == kBoolean_ScalarKind;
    }

    /**
     * Returns true if this is a numeric scalar type.
     */
    public final boolean isNumeric() {
        return switch (getScalarKind()) {
            case kFloat_ScalarKind, kSigned_ScalarKind, kUnsigned_ScalarKind -> true;
            default -> false;
        };
    }

    /**
     * Returns true if this is a floating-point scalar type (float or half).
     */
    public final boolean isFloat() {
        return getScalarKind() == kFloat_ScalarKind;
    }

    /**
     * Returns true if this is a signed scalar type (int or short).
     */
    public final boolean isSigned() {
        return getScalarKind() == kSigned_ScalarKind;
    }

    /**
     * Returns true if this is an unsigned scalar type (uint or ushort).
     */
    public final boolean isUnsigned() {
        return getScalarKind() == kUnsigned_ScalarKind;
    }

    /**
     * Returns true if this is a signed or unsigned integer.
     */
    public final boolean isInteger() {
        return switch (getScalarKind()) {
            case kSigned_ScalarKind, kUnsigned_ScalarKind -> true;
            default -> false;
        };
    }

    /**
     * Returns true if this is an "opaque type" (an external object which the shader references in
     * some fashion). <a href="https://www.khronos.org/opengl/wiki/Data_Type_(GLSL)#Opaque_types">Link</a>
     */
    public final boolean isOpaque() {
        return mTypeKind == kSampler_TypeKind;
    }

    public final boolean isGeneric() {
        return mTypeKind == kGeneric_TypeKind;
    }

    /**
     * Returns the "rank" of a numeric type, in order of float > half > int > short.
     * When operating on two numeric types, the result is the higher-rank type.
     */
    public int getRank() {
        throw new UnsupportedOperationException("non-scalar");
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
        if (getTypeKind() == other.getTypeKind() &&
                (isVector() || isMatrix() || isArray())) {
            // Vectors/matrices/arrays of the same size can be coerced if their component type can be.
            if (isMatrix() && (getRows() != other.getRows() || getCols() != other.getCols())) {
                return CoercionCost.impossible();
            }
            if (isArray() && getArrayLength() != other.getArrayLength()) {
                return CoercionCost.impossible();
            }
            if (getRows() != other.getRows()) {
                return CoercionCost.impossible();
            }
            return getComponentType().coercionCost(other.getComponentType());
        }
        if (isNumeric() && other.isNumeric()) {
            if (getScalarKind() != other.getScalarKind()) {
                return CoercionCost.impossible();
            } else if (other.getRank() >= getRank()) {
                return CoercionCost.normal(other.getRank() - getRank());
            } else {
                return CoercionCost.impossible();
            }
        }
        if (mTypeKind == kGeneric_TypeKind) {
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
    @Nullable
    public final Expression coerceExpression(Expression expr) {
        if (expr == null || expr.isIncomplete()) {
            return null;
        }
        if (expr.getType().matches(this)) {
            return expr;
        }

        int position = expr.mPosition;
        if (!CoercionCost.isPossible(expr.coercionCost(this))) {
            ThreadContext.getInstance().error(position, "expected '" + getName() + "', but found '" +
                    expr.getType().getName() + "'");
            return null;
        }

        if (isScalar()) {
            return ConstructorScalarCast.make(position, this, expr);
        }
        if (isVector() || isMatrix()) {
            return ConstructorCompoundCast.make(position, this, expr);
        }
        if (isArray()) {
            return ConstructorArrayCast.make(position, this, expr);
        }
        ThreadContext.getInstance().error(position, "cannot construct '" + getName() + "'");
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
     * For scalars and vectors, returns 1. For all other types, causes an assertion failure.
     */
    public int getCols() {
        throw new AssertionError();
    }

    /**
     * For matrices and vectors, returns the number of rows (e.g. mat3x4 return 4).
     * For scalars, returns 1. For all other types, causes an assertion failure.
     */
    public int getRows() {
        throw new AssertionError();
    }

    /**
     * For type that contains scalars, returns the number of scalars.
     * For all other types, causes an assertion failure.
     */
    public int getComponents() {
        return getCols() * getRows();
    }

    /**
     * For arrays, returns either the size of the array (if known) or -1 (unsized).
     * For all other types, causes an assertion failure.
     */
    public int getArrayLength() {
        throw new UnsupportedOperationException("non-array");
    }

    /**
     * For sampler/image types, returns the SpvDim.
     * For all other types, causes an assertion failure.
     */
    public int getDimensions() {
        throw new AssertionError();
    }

    @Nonnull
    public Field[] getFields() {
        throw new AssertionError();
    }

    /**
     * True for samplers that sample a depth texture with comparison (e.g., samplerShadow,
     * sampler2DShadow, HLSL SamplerComparisonState).
     */
    public boolean isShadow() {
        throw new AssertionError();
    }

    /**
     * True for arrayed texture.
     */
    public boolean isArrayed() {
        throw new AssertionError();
    }

    public final boolean isVoid() {
        return mTypeKind == kVoid_TypeKind;
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

    public boolean isStruct() {
        return false;
    }

    public boolean isInterfaceBlock() {
        return false;
    }

    public boolean isMultiSampled() {
        throw new AssertionError();
    }

    public boolean isSampled() {
        throw new AssertionError();
    }

    public boolean isCombinedSampler() {
        throw new AssertionError();
    }

    public boolean isSeparateSampler() {
        throw new AssertionError();
    }

    /**
     * Returns the minimum size in bits of the type.
     */
    public int getScalarWidth() {
        return 0;
    }

    /**
     * Returns the corresponding vector or matrix type with the specified number of columns and
     * rows.
     */
    public final Type toCompound(int cols, int rows) {
        if (!isScalar()) {
            throw new IllegalArgumentException("non-scalar");
        }
        if (cols == 1 && rows == 1) {
            return this;
        }
        BuiltinTypes types = ThreadContext.getInstance().getTypes();
        if (matches(types.mFloat)) {
            return switch (cols) {
                case 1 -> switch (rows) {
                    case 2 -> types.mFloat2;
                    case 3 -> types.mFloat3;
                    case 4 -> types.mFloat4;
                    default -> throw new AssertionError(rows);
                };
                case 2 -> switch (rows) {
                    case 2 -> types.mFloat2x2;
                    case 3 -> types.mFloat2x3;
                    case 4 -> types.mFloat2x4;
                    default -> throw new AssertionError(rows);
                };
                case 3 -> switch (rows) {
                    case 2 -> types.mFloat3x2;
                    case 3 -> types.mFloat3x3;
                    case 4 -> types.mFloat3x4;
                    default -> throw new AssertionError(rows);
                };
                case 4 -> switch (rows) {
                    case 2 -> types.mFloat4x2;
                    case 3 -> types.mFloat4x3;
                    case 4 -> types.mFloat4x4;
                    default -> throw new AssertionError(rows);
                };
                default -> throw new AssertionError(cols);
            };
        } else if (matches(types.mHalf)) {
            return switch (cols) {
                case 1 -> switch (rows) {
                    case 2 -> types.mHalf2;
                    case 3 -> types.mHalf3;
                    case 4 -> types.mHalf4;
                    default -> throw new AssertionError(rows);
                };
                case 2 -> switch (rows) {
                    case 2 -> types.mHalf2x2;
                    case 3 -> types.mHalf2x3;
                    case 4 -> types.mHalf2x4;
                    default -> throw new AssertionError(rows);
                };
                case 3 -> switch (rows) {
                    case 2 -> types.mHalf3x2;
                    case 3 -> types.mHalf3x3;
                    case 4 -> types.mHalf3x4;
                    default -> throw new AssertionError(rows);
                };
                case 4 -> switch (rows) {
                    case 2 -> types.mHalf4x2;
                    case 3 -> types.mHalf4x3;
                    case 4 -> types.mHalf4x4;
                    default -> throw new AssertionError(rows);
                };
                default -> throw new AssertionError(cols);
            };
        } else if (matches(types.mInt)) {
            if (cols == 1) {
                return switch (rows) {
                    case 2 -> types.mInt2;
                    case 3 -> types.mInt3;
                    case 4 -> types.mInt4;
                    default -> throw new AssertionError(rows);
                };
            }
        } else if (matches(types.mShort)) {
            if (cols == 1) {
                return switch (rows) {
                    case 2 -> types.mShort2;
                    case 3 -> types.mShort3;
                    case 4 -> types.mShort4;
                    default -> throw new AssertionError(rows);
                };
            }
        } else if (matches(types.mUInt)) {
            if (cols == 1) {
                return switch (rows) {
                    case 2 -> types.mUInt2;
                    case 3 -> types.mUInt3;
                    case 4 -> types.mUInt4;
                    default -> throw new AssertionError(rows);
                };
            }
        } else if (matches(types.mUShort)) {
            if (cols == 1) {
                return switch (rows) {
                    case 2 -> types.mUShort2;
                    case 3 -> types.mUShort3;
                    case 4 -> types.mUShort4;
                    default -> throw new AssertionError(rows);
                };
            }
        } else if (matches(types.mBool)) {
            if (cols == 1) {
                return switch (rows) {
                    case 2 -> types.mBool2;
                    case 3 -> types.mBool3;
                    case 4 -> types.mBool4;
                    default -> throw new AssertionError(rows);
                };
            }
        }
        throw new IllegalArgumentException("type mismatch");
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

        AliasType(String name, Type type) {
            super(name, type.getAbbr(), type.getTypeKind());
            mUnderlyingType = type;
        }

        @Nonnull
        @Override
        public Type resolve() {
            return mUnderlyingType;
        }

        @Nonnull
        @Override
        public Type getElementType() {
            return mUnderlyingType.getElementType();
        }

        @Nonnull
        @Override
        public Type getComponentType() {
            return mUnderlyingType.getComponentType();
        }

        @Override
        public byte getScalarKind() {
            return mUnderlyingType.getScalarKind();
        }

        @Override
        public int getRank() {
            return mUnderlyingType.getRank();
        }

        @Override
        public int getCols() {
            return mUnderlyingType.getCols();
        }

        @Override
        public int getRows() {
            return mUnderlyingType.getRows();
        }

        @Override
        public int getComponents() {
            return mUnderlyingType.getComponents();
        }

        @Override
        public int getArrayLength() {
            return mUnderlyingType.getArrayLength();
        }

        @Override
        public double getMinValue() {
            return mUnderlyingType.getMinValue();
        }

        @Override
        public double getMaxValue() {
            return mUnderlyingType.getMaxValue();
        }

        @Override
        public int getScalarWidth() {
            return mUnderlyingType.getScalarWidth();
        }

        @Override
        public int getDimensions() {
            return mUnderlyingType.getDimensions();
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

        @Override
        public boolean isMultiSampled() {
            return mUnderlyingType.isMultiSampled();
        }

        @Override
        public boolean isSampled() {
            return mUnderlyingType.isSampled();
        }

        @Override
        public boolean isCombinedSampler() {
            return mUnderlyingType.isCombinedSampler();
        }

        @Override
        public boolean isSeparateSampler() {
            return mUnderlyingType.isSeparateSampler();
        }

        @Nonnull
        @Override
        public Type[] getCoercibleTypes() {
            return mUnderlyingType.getCoercibleTypes();
        }

        @Nonnull
        @Override
        public Field[] getFields() {
            return mUnderlyingType.getFields();
        }
    }

    public static final class ArrayType extends Type {

        private final Type mElementType;
        private final int mArrayLength;

        ArrayType(Type type, int length) {
            super(mangle(type.getName(), length),
                    mangle(type.getAbbr(), length),
                    kArray_TypeKind);
            if (type instanceof ArrayType) {
                throw new IllegalArgumentException("Vulkan: disallow multi-dimensional arrays");
            }
            mElementType = type;
            mArrayLength = length;
        }

        @Nonnull
        public static String mangle(String base, int length) {
            if (length == kUnsizedArray) {
                return base + "[]";
            }
            assert (length > 0);
            return base + "[" + length + "]";
        }

        @Override
        public boolean isArray() {
            return true;
        }

        @Override
        public int getArrayLength() {
            return mArrayLength;
        }

        @Override
        public int getComponents() {
            assert (mArrayLength != kUnsizedArray);
            return super.getComponents() * mArrayLength;
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
        public int getScalarWidth() {
            return mElementType.getScalarWidth();
        }
    }

    public static final class ScalarType extends Type {

        private final byte mScalarKind;
        private final byte mRank;
        private final byte mWidth;

        ScalarType(String name, String desc, byte kind, int rank, int width) {
            super(name, desc, kScalar_TypeKind);
            assert (desc.length() == 1);
            mScalarKind = kind;
            mRank = (byte) rank;
            mWidth = (byte) width;
        }

        @Override
        public boolean isScalar() {
            return true;
        }

        @Override
        public byte getScalarKind() {
            return mScalarKind;
        }

        @Override
        public int getRank() {
            return mRank;
        }

        @Override
        public int getScalarWidth() {
            return mWidth;
        }

        @Override
        public int getCols() {
            return 1;
        }

        @Override
        public int getRows() {
            return 1;
        }

        @Override
        public int getComponents() {
            return 1;
        }

        @Override
        public double getMinValue() {
            return switch (mScalarKind) {
                case kSigned_ScalarKind -> mWidth == 32
                        ? 0x8000_0000
                        : 0xFFFF_8000;
                case kUnsigned_ScalarKind -> 0;
                default -> mWidth == 64
                        ? -Double.MAX_VALUE
                        : -Float.MAX_VALUE;
            };
        }

        @Override
        public double getMaxValue() {
            return switch (mScalarKind) {
                case kSigned_ScalarKind -> mWidth == 32
                        ? 0x7FFF_FFFF
                        : 0x7FFF;
                case kUnsigned_ScalarKind -> mWidth == 32
                        ? 0xFFFF_FFFFL
                        : 0xFFFFL;
                default -> mWidth == 64
                        ? Double.MAX_VALUE
                        : Float.MAX_VALUE;
            };
        }
    }

    public static final class VectorType extends Type {

        private final ScalarType mComponentType;
        private final byte mRows;

        VectorType(String name, String abbr, Type type, int rows) {
            super(name, abbr, kVector_TypeKind);
            assert (rows >= 2 && rows <= 4);
            assert (abbr.equals(type.getAbbr() + rows));
            assert (name.equals(type.getName() + rows));
            mComponentType = (ScalarType) type;
            mRows = (byte) rows;
        }

        @Override
        public boolean isVector() {
            return true;
        }

        @Nonnull
        @Override
        public ScalarType getComponentType() {
            return mComponentType;
        }

        @Override
        public int getCols() {
            return 1;
        }

        @Override
        public int getRows() {
            return mRows;
        }

        @Override
        public int getScalarWidth() {
            return mComponentType.getScalarWidth();
        }
    }

    public static final class MatrixType extends Type {

        private final ScalarType mComponentType;
        private final byte mCols;
        private final byte mRows;

        MatrixType(String name, String abbr, Type type, int cols, int rows) {
            super(name, abbr, kMatrix_TypeKind);
            assert (rows >= 2 && rows <= 4);
            assert (cols >= 2 && cols <= 4);
            assert (abbr.equals(type.getAbbr() + cols + "" + rows));
            assert (name.equals(type.getName() + cols + "x" + rows));
            mComponentType = (ScalarType) type;
            mCols = (byte) cols;
            mRows = (byte) rows;
        }

        @Override
        public boolean isMatrix() {
            return true;
        }

        @Nonnull
        @Override
        public ScalarType getComponentType() {
            return mComponentType;
        }

        @Override
        public int getCols() {
            return mCols;
        }

        @Override
        public int getRows() {
            return mRows;
        }

        @Override
        public int getScalarWidth() {
            return mComponentType.getScalarWidth();
        }
    }

    public static final class SamplerType extends Type {

        private final Type mComponentType;
        private final int mDimensions;
        private final boolean mIsShadow;
        private final boolean mIsArrayed;
        private final boolean mIsMultiSampled;
        private final boolean mIsSampled;
        private final boolean mIsSampler;

        SamplerType(String name, String desc, Type type, int dimensions,
                    boolean isShadow, boolean isArrayed, boolean isMultiSampled,
                    boolean isSampled, boolean isSampler) {
            super(name, desc, kSampler_TypeKind);
            mComponentType = type;
            mDimensions = dimensions;
            mIsArrayed = isArrayed;
            mIsMultiSampled = isMultiSampled;
            mIsSampled = isSampled;
            mIsSampler = isSampler;
            mIsShadow = isShadow;
        }

        @Nonnull
        @Override
        public Type getComponentType() {
            return mComponentType;
        }

        @Override
        public int getDimensions() {
            return mDimensions;
        }

        @Override
        public boolean isShadow() {
            return mIsShadow;
        }

        @Override
        public boolean isArrayed() {
            return mIsArrayed;
        }

        @Override
        public boolean isMultiSampled() {
            return mIsMultiSampled;
        }

        @Override
        public boolean isSampled() {
            return mIsSampled;
        }

        @Override
        public boolean isCombinedSampler() {
            return mIsSampled && mIsSampler;
        }

        @Override
        public boolean isSeparateSampler() {
            return !mIsSampled && mIsSampler;
        }
    }

    public static final class StructType extends Type {

        private final Field[] mFields;
        private final boolean mInterfaceBlock;

        StructType(int position, String name, Field[] fields, boolean interfaceBlock) {
            super(name, interfaceBlock ? "block" : "struct", kStruct_TypeKind, position);
            mFields = fields;
            mInterfaceBlock = interfaceBlock;
        }

        @Override
        public boolean isStruct() {
            return true;
        }

        @Override
        public boolean isInterfaceBlock() {
            return mInterfaceBlock;
        }

        @Nonnull
        @Override
        public Field[] getFields() {
            return mFields;
        }

        @Override
        public int getComponents() {
            int components = 0;
            for (Field field : mFields) {
                components += field.type().getComponents();
            }
            return components;
        }
    }

    public static final class GenericType extends Type {

        private final Type[] mCoercibleTypes;

        GenericType(String name, Type[] types) {
            super(name, "G", kGeneric_TypeKind);
            mCoercibleTypes = types;
        }

        @Nonnull
        @Override
        public Type[] getCoercibleTypes() {
            return mCoercibleTypes;
        }
    }
}
