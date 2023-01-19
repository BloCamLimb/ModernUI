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

package icyllis.akashigi.slang.tree;

import icyllis.akashigi.slang.Operator;
import icyllis.akashigi.slang.ThreadContext;
import icyllis.akashigi.slang.analysis.NodeVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * An expression which selects a field from a struct/block, as in 'foo.bar'.
 */
public final class FieldExpression extends Expression {

    private final Expression mBase;
    private final int mFieldIndex;
    private final boolean mAnonymousBlock;

    private FieldExpression(int position, Expression base, int fieldIndex, boolean anonymousBlock) {
        super(position, base.getType().getFields()[fieldIndex].type());
        mBase = base;
        mFieldIndex = fieldIndex;
        mAnonymousBlock = anonymousBlock;
    }

    /**
     * Returns a field-access expression.
     */
    @Nullable
    public static Expression convert(int position,
                                     Expression base,
                                     String fieldName) {
        Type baseType = base.getType();
        if (baseType.isStruct()) {
            final Type.Field[] fields = baseType.getFields();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].name().equals(fieldName)) {
                    return make(position, base, i, false);
                }
            }
        }
        ThreadContext.getInstance().error(position, "type '" + baseType.getName() +
                "' does not have a field named '" + fieldName + "'");
        return null;
    }

    /**
     * Returns a field-access expression.
     */
    @Nonnull
    public static Expression make(int position,
                                  Expression base,
                                  int fieldIndex,
                                  boolean anonymousBlock) {
        Type baseType = base.getType();
        if (!baseType.isStruct()) {
            throw new AssertionError();
        }
        Objects.checkIndex(fieldIndex, baseType.getFields().length);

        return new FieldExpression(position, base, fieldIndex, anonymousBlock);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.FIELD_ACCESS;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        if (visitor.visitFieldAccess(this)) {
            return true;
        }
        return mBase.accept(visitor);
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

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new FieldExpression(position,
                mBase.clone(),
                mFieldIndex,
                mAnonymousBlock);
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        String s = mBase.toString(Operator.PRECEDENCE_POSTFIX);
        if (!s.isEmpty()) {
            s += ".";
        } // else anonymous block
        return s + mBase.getType().getFields()[mFieldIndex].name();
    }
}
