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

import icyllis.arc3d.compiler.Context;
import icyllis.arc3d.compiler.Operator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * An expression which selects a field from a struct/block, as in 'foo.bar'.
 */
public final class FieldAccess extends Expression {

    private final Expression mBase;
    private final int mFieldIndex;
    private final boolean mAnonymousBlock;

    private FieldAccess(int position, Expression base, int fieldIndex, boolean anonymousBlock) {
        super(position, base.getType().getFields().get(fieldIndex).type());
        mBase = base;
        mFieldIndex = fieldIndex;
        mAnonymousBlock = anonymousBlock;
    }

    /**
     * Returns a field-access expression.
     */
    @Nullable
    public static Expression convert(@NonNull Context context,
                                     int position,
                                     @NonNull Expression base,
                                     int namePosition,
                                     @NonNull String name) {
        Type baseType = base.getType();
        if (baseType.isVector() || baseType.isScalar()) {
            return Swizzle.convert(context, position, base, namePosition, name);
        }
        //TODO length() method for vector, matrix and array
        if (baseType.isStruct()) {
            final var fields = baseType.getFields();
            for (int i = 0; i < fields.size(); i++) {
                if (fields.get(i).name().equals(name)) {
                    return FieldAccess.make(position, base, i, false);
                }
            }
        }
        context.error(position, "type '" + baseType.getName() +
                "' does not have a member named '" + name + "'");
        return null;
    }

    /**
     * Returns a field-access expression.
     */
    @NonNull
    public static Expression make(int position,
                                  Expression base,
                                  int fieldIndex,
                                  boolean anonymousBlock) {
        Type baseType = base.getType();
        if (!baseType.isStruct()) {
            throw new AssertionError();
        }
        Objects.checkIndex(fieldIndex, baseType.getFields().size());

        return new FieldAccess(position, base, fieldIndex, anonymousBlock);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.FIELD_ACCESS;
    }

    public Expression getBase() {
        return mBase;
    }

    public int getFieldIndex() {
        return mFieldIndex;
    }

    public boolean isAnonymousBlock() {
        return mAnonymousBlock;
    }

    @NonNull
    @Override
    public Expression copy(int position) {
        return new FieldAccess(position,
                mBase.copy(),
                mFieldIndex,
                mAnonymousBlock);
    }

    @NonNull
    @Override
    public String toString(int parentPrecedence) {
        String s = mBase.toString(Operator.PRECEDENCE_POSTFIX);
        if (!s.isEmpty()) {
            s += ".";
        } // else anonymous block
        return s + mBase.getType().getFields().get(mFieldIndex).name();
    }
}
