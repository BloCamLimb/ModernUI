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
    public static Statement convert(@NonNull Context context, Expression expr) {
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

    public Expression getExpression() {
        return mExpression;
    }

    public void setExpression(Expression expression) {
        mExpression = expression;
    }

    @NonNull
    @Override
    public String toString() {
        return mExpression.toString(Operator.PRECEDENCE_STATEMENT) + ";";
    }
}
