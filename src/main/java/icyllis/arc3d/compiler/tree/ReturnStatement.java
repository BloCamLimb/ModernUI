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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A return statement.
 */
public final class ReturnStatement extends Statement {

    @Nullable
    private Expression mExpression;

    private ReturnStatement(int position, @Nullable Expression expression) {
        super(position);
        mExpression = expression;
    }

    public static Statement make(int pos, @Nullable Expression expression) {
        return new ReturnStatement(pos, expression);
    }

    @Nullable
    public Expression getExpression() {
        return mExpression;
    }

    public void setExpression(@Nullable Expression expression) {
        mExpression = expression;
    }

    @Override
    public StatementKind getKind() {
        return StatementKind.RETURN;
    }

    @Override
    public boolean accept(@Nonnull TreeVisitor visitor) {
        if (visitor.visitReturn(this)) {
            return true;
        }
        return mExpression != null && mExpression.accept(visitor);
    }

    @Nonnull
    @Override
    public String toString() {
        if (mExpression != null) {
            return "return " + mExpression + ";";
        } else {
            return "return;";
        }
    }
}
