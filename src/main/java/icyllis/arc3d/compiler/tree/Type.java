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

package icyllis.arc3d.compiler.tree;

import icyllis.arc3d.compiler.*;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.util.spvc.Spv;

import java.util.List;
import java.util.OptionalLong;

/**
 * Represents a type symbol, such as int or float4.
 */
public class Type extends Symbol {

    public static final int kUnsizedArray = -1; // an unsized array (declared with [])
    public static final int kMaxNestingDepth = 8;

    /**
     * Represents a single field in a struct/block type. Not associated with Variable.
     *
     * @param position see {@link Position}
     */
    public record Field(int position, @NonNull Modifiers modifiers, @NonNull Type type, @NonNull String name) {

        @NonNull
        @Override
        public String toString() {
            return modifiers.toString() + type.getName() + " " + name + ";";
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

    private final @NonNull String mDesc;
    private final byte mTypeKind;

    Type(@NonNull String name, @NonNull String desc, byte kind) {
        this(name, desc, kind, Position.NO_POS);
    }

    Type(@NonNull String name, @NonNull String desc, byte kind, int position) {
        super(position, name);
        mDesc = desc;
        mTypeKind = kind;
    }

    /**
     * Creates an alias which maps to another type.
     */
    public static @NonNull Type makeAliasType(@NonNull String name, @NonNull Type type) {
        assert (type == type.resolve());
        return new AliasType(type.mPosition, name, type);
    }

    /**
     * Creates an alias which maps to another type.
     */
    @NonNull
    public static Type makeAliasType(int position, String name, Type type) {
        assert (type == type.resolve());
        return new AliasType(position, name, type);
    }

    /**
     * Create a generic type which maps to the listed types
     * (e.g. __genFType is a generic type which can match float, float2, float3 or float4).
     */
    @NonNull
    public static Type makeGenericType(String name, Type... types) {
        for (Type type : types) {
            assert (type == type.resolve());
        }
        return new GenericType(name, types);
    }

    /**
     * Create a scalar type.
     *
     * @param kind a scalar kind
     */
    @NonNull
    public static Type makeScalarType(String name, String desc,
                                      byte kind, int rank, int width) {
        return makeScalarType(name, desc, kind, rank, width, width);
    }

    /**
     * Create a scalar type with minimum precision.
     *
     * @param kind a scalar kind
     */
    @NonNull
    public static Type makeScalarType(String name, String desc,
                                      byte kind, int rank, int minWidth, int width) {
        assert minWidth == width || (width == 32 && minWidth < width);
        return new ScalarType(name, desc, kind, rank, minWidth, width);
    }

    /**
     * Create a vector type.
     *
     * @param componentType a scalar type
     */
    @NonNull
    public static Type makeVectorType(String name, String desc,
                                      Type componentType, int rows) {
        assert (componentType == componentType.resolve());
        return new VectorType(name, desc, componentType, rows);
    }

    /**
     * Create a matrix type.
     *
     * @param columnType a vector type
     */
    @NonNull
    public static Type makeMatrixType(String name, String desc,
                                      Type columnType, int cols) {
        assert (columnType == columnType.resolve());
        return new MatrixType(name, desc, columnType, cols);
    }

    /**
     * Create a sampler/image type. Includes images, subpass inputs, textures without sampler,
     * textures with sampler, and pure samplers.
     * <ul>
     * <li>isSampled=true,isSampler=true: combined texture sampler (e.g. sampler2D)</li>
     * <li>isSampled=true,isSampler=false: pure texture (e.g. texture2D)</li>
     * <li>isSampled=false,isSampler=true: pure sampler (e.g. sampler)</li>
     * <li>isSampled=false,isSampler=false: image or subpass (e.g. image2D)</li>
     * </ul>
     * isShadow: True for samplers that sample a depth texture with comparison (e.g.
     * samplerShadow, sampler2DShadow, HLSL's SamplerComparisonState).
     *
     * @param component  e.g. texture2D has a type of half
     * @param dimensions SpvDim (e.g. {@link Spv#SpvDim1D})
     */
    @NonNull
    public static Type makeSamplerType(String name, String abbr, Type component, int dimensions,
                                       boolean isShadow, boolean isArrayed, boolean isMultiSampled,
                                       boolean isSampled, boolean isSampler) {
        assert (component == component.resolve());
        return new SamplerType(name, abbr, component, dimensions, isShadow, isArrayed,
                isMultiSampled, isSampled, isSampler);
    }

    /**
     * Create an image or subpass type.
     */
    @NonNull
    public static Type makeImageType(String name, String abbr, Type component, int dimensions,
                                     boolean isArrayed, boolean isMultiSampled) {
        assert (component.isScalar());
        return makeSamplerType(name, abbr, component, dimensions, /*isShadow=*/false, isArrayed,
                isMultiSampled, /*isSampled=*/false, /*isSampler*/false);
    }

    /**
     * Create a texture type.
     */
    @NonNull
    public static Type makeTextureType(String name, String abbr, Type component, int dimensions,
                                       boolean isArrayed, boolean isMultiSampled) {
        assert (component.isScalar());
        return makeSamplerType(name, abbr, component, dimensions, /*isShadow=*/false, isArrayed,
                isMultiSampled, /*isSampled=*/true, /*isSampler*/false);
    }

    /**
     * Create a separate sampler type.
     */
    @NonNull
    public static Type makeSeparateType(String name, String abbr, Type component, boolean isShadow) {
        assert (component.isVoid());
        return makeSamplerType(name, abbr, component, /*dimensions*/-1, isShadow, /*isArrayed*/false,
                /*isMultiSampled*/false, /*isSampled=*/false, /*isSampler*/true);
    }

    /**
     * Create a combined sampler type.
     */
    @NonNull
    public static Type makeCombinedType(String name, String abbr, Type component, int dimensions,
                                        boolean isShadow, boolean isArrayed, boolean isMultiSampled) {
        assert (component.isScalar());
        return makeSamplerType(name, abbr, component, dimensions, isShadow, isArrayed,
                isMultiSampled, /*isSampled=*/true, /*isSampler*/true);
    }

    /**
     * Create a "special" type with the given name.
     *
     * @param kind a type kind
     */
    @NonNull
    public static Type makeSpecialType(String name, String abbr, byte kind) {
        return new Type(name, abbr, kind);
    }

    /**
     * Creates an array type. Call {@link #isUsableInArray(Context, int)} first.
     *
     * @param type the element type
     */
    @NonNull
    public static Type makeArrayType(String name, Type type, int size) {
        assert !type.isArray();
        return new ArrayType(name, type, size);
    }

    /**
     * Creates a struct type with the given fields. Reports an error if the struct is ill-formed.
     */
    @NonNull
    public static Type makeStructType(@NonNull Context context, int position,
                                      @NonNull String name, @NonNull List<Field> fields,
                                      boolean interfaceBlock) {
        String structOrBlock = interfaceBlock ? "block" : "struct";
        if (fields.isEmpty()) {
            context.error(position, structOrBlock + " '" + name +
                    "' must contain at least one field");
        }
        for (Field field : fields) {
            if (field.type().isVoid()) {
                context.error(field.position(),
                        "type 'void' is not permitted in a " + structOrBlock);
            }
        }
        int nestingDepth = 0;
        for (Field field : fields) {
            nestingDepth = Math.max(nestingDepth, field.type().getNestingDepth());
        }
        if (nestingDepth >= kMaxNestingDepth) {
            context.error(position, structOrBlock + " '" + name +
                    "' is too deeply nested");
        }
        return new StructType(position, name, fields,
                nestingDepth + 1, interfaceBlock);
    }

    @Override
    public @NonNull SymbolKind getKind() {
        return SymbolKind.TYPE;
    }

    /**
     * Returns this.
     */
    @Override
    public final @NonNull Type getType() {
        return this;
    }

    /**
     * If this is an alias, returns the underlying type, otherwise returns this.
     */
    public @NonNull Type resolve() {
        return this;
    }

    /**
     * For arrays, returns the base type. For matrices, returns the column vector type.
     * For vectors, returns the scalar type. For all other types, returns the type itself.
     */
    public @NonNull Type getElementType() {
        return this;
    }

    /**
     * For matrices and vectors, returns the scalar type of individual cells (e.g. mat2 has a component
     * type of Float). For textures, returns the sampled type (e.g. texture2D has a component type
     * of Float). For arrays, returns the component type of their base type. For all other types,
     * returns the type itself.
     */
    public @NonNull Type getComponentType() {
        return this;
    }

    /**
     * Returns true if these types are equal after alias resolution.
     */
    public final boolean matches(@NonNull Type other) {
        return resolve().getName().equals(other.resolve().getName());
    }

    /**
     * Returns a descriptor of the type, meant for name-mangling. (e.g. float4x4 -> f44)
     */
    public final @NonNull String getDesc() {
        return mDesc;
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

    @Override
    public final @NonNull String toString() {
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
            case kFloat_ScalarKind,
                 kSigned_ScalarKind,
                 kUnsigned_ScalarKind -> true;
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
            case kSigned_ScalarKind,
                 kUnsigned_ScalarKind -> true;
            default -> false;
        };
    }

    /**
     * Returns true if this is a floating-point scalar type (float or half),
     * or its vector/matrix form.
     */
    public final boolean isFloatOrCompound() {
        return (isScalar() || isVector() || isMatrix()) &&
                getComponentType().isFloat();
    }

    /**
     * Returns true if this is a signed scalar type (int or short),
     * or its vector form.
     */
    public final boolean isSignedOrCompound() {
        return (isScalar() || isVector()) &&
                getComponentType().isSigned();
    }

    /**
     * Returns true if this is an unsigned scalar type (uint or ushort),
     * or its vector form.
     */
    public final boolean isUnsignedOrCompound() {
        return (isScalar() || isVector()) &&
                getComponentType().isUnsigned();
    }

    /**
     * Returns true if this type is a bool, or its vector form.
     */
    public final boolean isBooleanOrCompound() {
        return (isScalar() || isVector()) &&
                getComponentType().isBoolean();
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
    public final boolean canCoerceTo(Type other, boolean allowNarrowing) {
        return CoercionCost.accept(getCoercionCost(other), allowNarrowing);
    }

    /**
     * Determines the "cost" of coercing (implicitly converting) this type to another type. The cost
     * is a number with no particular meaning other than that lower costs are preferable to higher
     * costs.
     *
     * @see CoercionCost
     */
    public final long getCoercionCost(Type other) {
        return getCoercionCost(other, false);
    }

    private long getCoercionCost(Type other, boolean inArray) {
        if (matches(other)) {
            return CoercionCost.free();
        }
        if (getTypeKind() == other.getTypeKind()) {
            // Vectors/matrices/arrays of the same size can be coerced if their component type can be.
            if (isVector()) {
                if (getRows() != other.getRows()) {
                    return CoercionCost.saturate();
                }
                return getComponentType().getCoercionCost(other.getComponentType(), inArray);
            }
            if (isMatrix()) {
                if (getRows() != other.getRows() || getCols() != other.getCols()) {
                    return CoercionCost.saturate();
                }
                return getComponentType().getCoercionCost(other.getComponentType(), inArray);
            }
            if (isArray()) {
                if (getArraySize() != other.getArraySize()) {
                    return CoercionCost.saturate();
                }
                return getElementType().getCoercionCost(other.getElementType(), true);
            }
        }
        if (isNumeric() && other.isNumeric()) {
            if (inArray && getScalarKind() != other.getScalarKind()) {
                return CoercionCost.saturate();
            } else if (other.getRank() >= getRank()) {
                return CoercionCost.widening(other.getRank() - getRank());
            } else if (getWidth() == other.getWidth()) {
                return CoercionCost.narrowing(getRank() - other.getRank());
            }
        }
        if (isGeneric()) {
            final var types = getCoercibleTypes();
            for (int i = 0; i < types.size(); i++) {
                if (types.get(i).matches(other)) {
                    return CoercionCost.widening(i + 1);
                }
            }
        }
        return CoercionCost.saturate();
    }

    /**
     * For generic types, returns the types that this generic type can substitute for.
     */
    public @Unmodifiable List<Type> getCoercibleTypes() {
        throw new AssertionError();
    }

    /**
     * Coerces the passed-in expression to this type. If the types are incompatible, reports an
     * error and returns null.
     */
    @Nullable
    public final Expression coerceExpression(@NonNull Context context,
                                             @Nullable Expression expr) {
        if (expr == null || expr.isIncomplete(context)) {
            return null;
        }
        if (expr.getType().matches(this)) {
            return expr;
        }

        int pos = expr.mPosition;
        if (!CoercionCost.accept(expr.getCoercionCost(this), false)) {
            context.error(pos, "expected '" + getName() + "', but found '" +
                    expr.getType().getName() + "'");
            return null;
        }

        if (isScalar()) {
            return ConstructorScalarCast.make(context, pos, this, expr);
        }
        if (isVector() || isMatrix()) {
            return ConstructorCompoundCast.make(pos, this, expr);
        }
        if (isArray()) {
            return ConstructorArrayCast.make(context, pos, this, expr);
        }
        context.error(pos, "cannot construct '" + getName() + "'");
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
    public int getArraySize() {
        throw new UnsupportedOperationException("non-array");
    }

    /**
     * For sampler/image types, returns the SpvDim.
     * For all other types, causes an assertion failure.
     */
    public int getDimensions() {
        throw new AssertionError();
    }

    public @Unmodifiable List<Field> getFields() {
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

    public boolean isUnsizedArray() {
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

    /**
     * @return true for texture with sampler
     */
    public boolean isCombinedSampler() {
        throw new AssertionError();
    }

    /**
     * @return true for pure sampler
     */
    public boolean isSeparateSampler() {
        throw new AssertionError();
    }

    /**
     * @return true for image, except subpass input
     */
    public boolean isStorageImage() {
        throw new AssertionError();
    }

    /**
     * For scalars, returns the minimum size in bits of the type.
     */
    public int getMinWidth() {
        return 0;
    }

    /**
     * For scalars, returns the size in bits of the type.
     */
    public int getWidth() {
        return 0;
    }

    /**
     * Returns true if the component type has relaxed precision.
     */
    public boolean isRelaxedPrecision() {
        Type componentType = getComponentType();
        return componentType.getWidth() == 32 && componentType.getMinWidth() < 32;
    }

    /**
     * Returns the corresponding vector type with the specified number of rows.
     */
    public final Type toVector(@NonNull Context context, int rows) {
        return toCompound(context, 1, rows);
    }

    /**
     * Returns the corresponding vector or matrix type with the specified number of columns and
     * rows.
     */
    public final Type toCompound(@NonNull Context context, int cols, int rows) {
        if (!isScalar()) {
            throw new IllegalStateException("non-scalar");
        }
        if (cols == 1 && rows == 1) {
            return this;
        }
        BuiltinTypes types = context.getTypes();
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
        throw new IllegalStateException("type mismatch");
    }

    /**
     * Converts an element type and a size (float, 10) into an array name ("float[10]").
     */
    public @NonNull String getArrayName(int size) {
        return getArrayName(getName(), size);
    }

    /**
     * Converts an element type and a size (float, 10) into an array name ("float[10]").
     */
    public static @NonNull String getArrayName(@NonNull String baseName, int size) {
        if (size == kUnsizedArray) {
            return baseName + "[]";
        }
        assert (size > 0);
        return baseName + "[" + size + "]";
    }

    /**
     * Checks if `value` can fit in this type. The type must be scalar.
     */
    public boolean checkLiteralOutOfRange(Context context,
                                          int pos,
                                          double value) {
        assert isScalar();
        if (!isNumeric()) {
            return false;
        }
        if (value >= getMinValue() && value <= getMaxValue()) {
            return false;
        }
        // We found a value that can't fit in our type. Flag it as an error.
        context.error(pos, String.format("value is out of range for type '%s': %.0f",
                this, value));
        return true;
    }

    /**
     * Reports errors and returns false if this type cannot be used as the element type for an array.
     */
    public boolean isUsableInArray(@NonNull Context context, int position) {
        if (isArray()) {
            // Vulkan: disallow multi-dimensional arrays
            context.error(position, "multi-dimensional arrays are not allowed");
            return false;
        }
        if (isVoid()) {
            context.error(position, "type 'void' may not be used in an array");
            return false;
        }
        if (isOpaque()) {
            context.error(position, "opaque type '" + getName() +
                    "' may not be used in an array");
            return false;
        }
        return true;
    }

    /**
     * Verifies that the expression is a valid constant array size for this type. Returns the array
     * size, or reports errors and returns zero if the expression isn't a valid literal value.
     */
    public int convertArraySize(@NonNull Context context, int position, @Nullable Expression size) {
        size = context.getTypes().mInt.coerceExpression(context, size);
        if (size == null) {
            return 0;
        }
        OptionalLong value = ConstantFolder.getConstantInt(size);
        if (value.isEmpty()) {
            context.error(size.mPosition, "array size must be a constant integer expression");
            return 0;
        }
        return convertArraySize(context, position, size.mPosition, value.getAsLong());
    }

    public int convertArraySize(@NonNull Context context, int position, int sizePosition, long size) {
        if (!isUsableInArray(context, position)) {
            return 0;
        }
        if (size <= 0) {
            context.error(sizePosition, "array size must be positive");
            return 0;
        }
        if (size > Integer.MAX_VALUE) {
            context.error(sizePosition, "array size is too large");
            return 0;
        }
        return (int) size;
    }

    /**
     * If the type is a struct, returns the depth of the most deeply-nested field.
     */
    public int getNestingDepth() {
        return 0;
    }

    /**
     * CoercionCost. The values are packed into a long value.
     *
     * @see #getCoercionCost(Type)
     */
    public static final class CoercionCost {

        private static final long SATURATE = 0x80000000_80000000L;

        ///// Constructor

        /**
         * No cost.
         */
        public static long free() {
            return 0;
        }

        public static long widening(int cost) {
            assert cost >= 0;
            return cost;
        }

        public static long narrowing(int cost) {
            assert cost >= 0;
            return (long) cost << 32;
        }

        public static long saturate() {
            return SATURATE;
        }

        ///// METHODS

        public static boolean accept(long cost, boolean allowNarrowing) {
            return (cost & SATURATE) == 0 && (allowNarrowing || (cost >> 32) == 0);
        }

        ///// OPERATORS

        // Addition of two costs.
        public static long plus(long lhs, long rhs) {
            if (((lhs & SATURATE) | (rhs & SATURATE)) != 0) {
                return SATURATE;
            }
            // overflow becomes impossible (should not happen)
            long widening = (int) lhs + (int) rhs;
            long narrowing = (lhs >> 32) + (rhs >> 32);
            return widening | (narrowing << 32);
        }

        public static int compare(long lhs, long rhs) {
            int res = Boolean.compare((lhs & SATURATE) != 0, (rhs & SATURATE) != 0);
            if (res != 0) {
                return res;
            }
            // narrowing
            res = Integer.compare((int) (lhs >> 32), (int) (rhs >> 32));
            if (res != 0) {
                return res;
            }
            // widening
            return Integer.compare((int) lhs, (int) rhs);
        }
    }

    public static final class AliasType extends Type {

        private final Type mUnderlyingType;

        AliasType(int position, String name, @NonNull Type type) {
            super(name, type.getDesc(), type.getTypeKind(), position);
            mUnderlyingType = type;
        }

        @NonNull
        @Override
        public Type resolve() {
            return mUnderlyingType;
        }

        @Override
        public @NonNull Type getElementType() {
            return mUnderlyingType.getElementType();
        }

        @NonNull
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
        public int getArraySize() {
            return mUnderlyingType.getArraySize();
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
        public int getMinWidth() {
            return mUnderlyingType.getMinWidth();
        }

        @Override
        public int getWidth() {
            return mUnderlyingType.getWidth();
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
        public boolean isUnsizedArray() {
            return mUnderlyingType.isUnsizedArray();
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

        @Override
        public boolean isStorageImage() {
            return mUnderlyingType.isStorageImage();
        }

        @Override
        public @Unmodifiable List<Type> getCoercibleTypes() {
            return mUnderlyingType.getCoercibleTypes();
        }

        @Override
        public @Unmodifiable List<Field> getFields() {
            return mUnderlyingType.getFields();
        }
    }

    public static final class ArrayType extends Type {

        private final Type mElementType;
        private final int mArraySize;

        ArrayType(String name, Type type, int size) {
            super(name, type.getDesc() + (size != kUnsizedArray ? "_" + size : "_x"),
                    kArray_TypeKind);
            assert name.equals(type.getArrayName(size));
            mElementType = type;
            mArraySize = size;
        }

        @Override
        public boolean isArray() {
            return true;
        }

        @Override
        public boolean isUnsizedArray() {
            return mArraySize == kUnsizedArray;
        }

        @Override
        public int getArraySize() {
            return mArraySize;
        }

        @Override
        public int getComponents() {
            if (mArraySize == kUnsizedArray) {
                return 0;
            }
            return mElementType.getComponents() * mArraySize;
        }

        @NonNull
        @Override
        public Type getElementType() {
            return mElementType;
        }

        @NonNull
        @Override
        public Type getComponentType() {
            return mElementType.getComponentType();
        }
    }

    public static final class ScalarType extends Type {

        private final byte mScalarKind;
        private final byte mRank;
        private final byte mMinWidth;
        private final byte mWidth;

        ScalarType(String name, String desc, byte kind, int rank, int minWidth, int width) {
            super(name, desc, kScalar_TypeKind);
            mScalarKind = kind;
            mRank = (byte) rank;
            mMinWidth = (byte) minWidth;
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
        public int getMinWidth() {
            return mMinWidth;
        }

        @Override
        public int getWidth() {
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
                case kSigned_ScalarKind -> mMinWidth == 32
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
                case kSigned_ScalarKind -> mMinWidth == 32
                        ? 0x7FFF_FFFF
                        : 0x7FFF;
                case kUnsigned_ScalarKind -> mMinWidth == 32
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

        VectorType(String name, String abbr, Type componentType, int rows) {
            super(name, abbr, kVector_TypeKind);
            assert (rows >= 2 && rows <= 4);
            assert (abbr.equals(componentType.getDesc() + rows));
            assert (name.equals(componentType.getName() + rows));
            mComponentType = (ScalarType) componentType;
            mRows = (byte) rows;
        }

        @Override
        public boolean isVector() {
            return true;
        }

        @NonNull
        @Override
        public Type getElementType() {
            return mComponentType;
        }

        @NonNull
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
    }

    public static final class MatrixType extends Type {

        private final VectorType mColumnType;
        private final byte mCols;

        MatrixType(String name, String abbr, Type columnType, int cols) {
            super(name, abbr, kMatrix_TypeKind);
            assert (columnType.isVector());
            assert (cols >= 2 && cols <= 4);
            int rows = columnType.getRows();
            Type componentType = columnType.getComponentType();
            assert (abbr.equals(componentType.getDesc() + cols + rows));
            assert (name.equals(componentType.getName() + cols + "x" + rows));
            mColumnType = (VectorType) columnType;
            mCols = (byte) cols;
        }

        @Override
        public boolean isMatrix() {
            return true;
        }

        @NonNull
        @Override
        public Type getElementType() {
            return mColumnType;
        }

        @NonNull
        @Override
        public ScalarType getComponentType() {
            return mColumnType.getComponentType();
        }

        @Override
        public int getCols() {
            return mCols;
        }

        @Override
        public int getRows() {
            return mColumnType.getRows();
        }
    }

    public static final class ImageType extends Type {

        ImageType(String name, String desc, byte kind) {
            super(name, desc, kind);
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

        @NonNull
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

        @Override
        public boolean isStorageImage() {
            return !mIsSampled && !mIsSampler && mDimensions != Spv.SpvDimSubpassData;
        }
    }

    public static final class StructType extends Type {

        private final @Unmodifiable List<Field> mFields;
        private final int mNestingDepth;
        private final boolean mInterfaceBlock;
        private final int mComponents;

        // name - the type name, not instance name
        // (interface block can have no instance name, but there must be type name)
        StructType(int position, String name, List<Field> fields, int nestingDepth,
                   boolean interfaceBlock) {
            super(name, name, kStruct_TypeKind, position);
            mFields = List.copyOf(fields);
            mNestingDepth = nestingDepth;
            mInterfaceBlock = interfaceBlock;
            int components = 0;
            try {
                for (Field field : mFields) {
                    components += field.type().getComponents();
                }
            } catch (AssertionError e) {
                components = 0;
            }
            mComponents = components;
        }

        @Override
        public boolean isStruct() {
            return true;
        }

        @Override
        public boolean isInterfaceBlock() {
            return mInterfaceBlock;
        }

        @Override
        public @Unmodifiable List<Field> getFields() {
            return mFields;
        }

        @Override
        public int getComponents() {
            return mComponents;
        }

        @Override
        public int getNestingDepth() {
            return mNestingDepth;
        }
    }

    public static final class GenericType extends Type {

        private final @Unmodifiable List<Type> mCoercibleTypes;

        GenericType(String name, Type[] types) {
            super(name, "G", kGeneric_TypeKind);
            mCoercibleTypes = List.of(types);
        }

        @Override
        public @Unmodifiable List<Type> getCoercibleTypes() {
            return mCoercibleTypes;
        }
    }
}
