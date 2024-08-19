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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An expression that accesses an element of an array, vector, or matrix,
 * as in 'base [ index ]'.
 */
public final class IndexExpression extends Expression {

    private Expression mBase;
    private Expression mIndex;

    private IndexExpression(int position, Expression base, Expression index) {
        super(position, base.getType().getElementType());
        mBase = base;
        mIndex = index;
    }

    private IndexExpression(int position, Type type, Expression base, Expression index) {
        super(position, type);
        mBase = base;
        mIndex = index;
    }

    private static boolean indexOutOfBounds(@Nonnull Context context,
                                            int pos, long index,
                                            @Nonnull Expression base) {
        Type baseType = base.getType();
        if (index >= 0) {
            if (baseType.isArray()) {
                if (baseType.isUnsizedArray()) {
                    return false;
                }
                if (index < baseType.getArraySize()) {
                    return false;
                }
            } else if (baseType.isMatrix()) {
                if (index < baseType.getCols()) {
                    return false;
                }
            } else {
                assert baseType.isVector();
                if (index < baseType.getRows()) {
                    return false;
                }
            }
        }
        context.error(pos, "index " + index + " out of range for '" +
                baseType + "'");
        return true;
    }

    @Nullable
    public static Expression convert(@Nonnull Context context,
                                     int pos,
                                     @Nonnull Expression base,
                                     @Nullable Expression index) {
        // Convert an array type reference.
        if (base instanceof TypeReference) {
            Type baseType = ((TypeReference) base).getValue();
            final int arraySize;
            if (index != null) {
                arraySize = baseType.convertArraySize(context, pos, index);
            } else {
                arraySize = Type.kUnsizedArray;
            }
            if (arraySize == 0) {
                return null;
            }
            return TypeReference.make(context,
                    pos, context.getSymbolTable().getArrayType(baseType, arraySize));
        }
        if (index == null) {
            context.error(pos, "missing index in '[]'");
            return null;
        }
        Type baseType = base.getType();
        if (!baseType.isArray() && !baseType.isMatrix() && !baseType.isVector()) {
            context.error(base.mPosition,
                    "expected array, matrix or vector, but found '" + baseType + "'");
            return null;
        }
        if (!index.getType().isInteger()) {
            index = context.getTypes().mInt.coerceExpression(context, index);
            if (index == null) {
                return null;
            }
        }
        base = ConstantFolder.getConstantValueForVariable(base);
        index = ConstantFolder.getConstantValueForVariable(index);
        if (index.isIntLiteral()) {
            long indexValue = ((Literal) index).getIntegerValue();
            if (indexOutOfBounds(context, index.mPosition, indexValue, base)) {
                return null;
            }
            //TODO constant folding
        }
        return IndexExpression.make(context, pos, base, index);
    }

    public static Expression make(@Nonnull Context context,
                                  int pos,
                                  @Nonnull Expression base,
                                  @Nonnull Expression index) {
        return new IndexExpression(pos, base, index);
    }

    public Expression getBase() {
        return mBase;
    }

    public void setBase(Expression base) {
        mBase = base;
    }

    public Expression getIndex() {
        return mIndex;
    }

    public void setIndex(Expression index) {
        mIndex = index;
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.INDEX;
    }

    @Override
    public boolean accept(@Nonnull TreeVisitor visitor) {
        if (visitor.visitIndex(this)) {
            return true;
        }
        return mBase.accept(visitor) || mIndex.accept(visitor);
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new IndexExpression(position,
                getType(),
                mBase.clone(),
                mIndex.clone()
        );
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        return mBase.toString(Operator.PRECEDENCE_POSTFIX) + "[" +
                mIndex.toString(Operator.PRECEDENCE_EXPRESSION) + "]";
    }
}
