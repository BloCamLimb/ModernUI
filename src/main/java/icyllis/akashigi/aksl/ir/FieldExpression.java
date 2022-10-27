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

package icyllis.akashigi.aksl.ir;

import icyllis.akashigi.aksl.ThreadContext;
import icyllis.akashigi.aksl.Operator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * An expression which selects a field from a block, as in 'foo.bar'.
 */
public final class FieldExpression extends Expression {

    private final Expression mBase;
    private final int mFieldIndex;

    private FieldExpression(int position, Expression base, int fieldIndex) {
        super(position, KIND_FIELD, base.type().fields()[fieldIndex].type());
        mBase = base;
        mFieldIndex = fieldIndex;
    }

    /**
     * Returns a field-access expression; reports errors via the ErrorHandler.
     */
    @Nullable
    public static Expression convert(ThreadContext context,
                                     int position,
                                     Expression base,
                                     String fieldName) {
        Type baseType = base.type();
        if (baseType.isStruct()) {
            final Type.Field[] fields = baseType.fields();
            for (int i = 0, e = fields.length; i < e; i++) {
                if (fields[i].name().equals(fieldName)) {
                    return make(context, position, base, i);
                }
            }
        }
        context.error(position, "type '" + baseType.displayName() +
                "' does not have a field named '" + fieldName + "'");
        return null;
    }

    /**
     * Returns a field-access expression; reports errors via RuntimeException.
     */
    @Nonnull
    public static Expression make(ThreadContext context,
                                  int position,
                                  Expression base,
                                  int fieldIndex) {
        Type baseType = base.type();
        if (!baseType.isStruct()) {
            throw new IllegalArgumentException();
        }
        Objects.checkIndex(fieldIndex, baseType.fields().length);

        return new FieldExpression(position, base, fieldIndex);
    }

    public Expression getBase() {
        return mBase;
    }

    public int getFieldIndex() {
        return mFieldIndex;
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        return mBase.toString(Operator.PRECEDENCE_POSTFIX) + "." +
                mBase.type().fields()[mFieldIndex].name();
    }
}
