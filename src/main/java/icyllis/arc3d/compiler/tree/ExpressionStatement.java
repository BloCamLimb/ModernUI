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

package icyllis.arc3d.compiler.tree;

import icyllis.arc3d.compiler.Context;
import icyllis.arc3d.compiler.Operator;
import icyllis.arc3d.compiler.analysis.TreeVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A lone expression being used as a statement.
 */
public final class ExpressionStatement extends Statement {

    private Expression mExpression;

    public ExpressionStatement(Expression expression) {
        super(expression.mPosition);
        mExpression = expression;
    }

    @Nullable
    public static Statement convert(@Nonnull Context context, Expression expr) {
        if (expr.isIncomplete(context)) {
            return null;
        }
        return ExpressionStatement.make(expr);
    }

    public static Statement make(Expression expr) {
        return new ExpressionStatement(expr);
    }

    @Override
    public StatementKind getKind() {
        return StatementKind.EXPRESSION;
    }

    @Override
    public boolean accept(@Nonnull TreeVisitor visitor) {
        if (visitor.visitExpression(this)) {
            return true;
        }
        return mExpression.accept(visitor);
    }

    public Expression getExpression() {
        return mExpression;
    }

    public void setExpression(Expression expression) {
        mExpression = expression;
    }

    @Nonnull
    @Override
    public String toString() {
        return mExpression.toString(Operator.PRECEDENCE_STATEMENT) + ";";
    }
}
