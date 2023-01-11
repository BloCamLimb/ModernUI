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

package icyllis.akashigi.slang.analysis;

import icyllis.akashigi.slang.ir.Expression;
import icyllis.akashigi.slang.ir.Literal;

public final class Analysis {

    /**
     * Determines if `expr` is a compile-time constant (composed of just constructors and literals).
     */
    public static boolean isCompileTimeConstant(Expression expr) {
        class IsCompileTimeConstantVisitor extends NodeVisitor {
            @Override
            public boolean visitLiteral(Literal expr) {
                // Literals are compile-time constants.
                return false;
            }

            @Override
            protected boolean visitExpression(Expression expr) {
                return switch (expr.getKind()) {
                    case CONSTRUCTOR_ARRAY,
                            CONSTRUCTOR_COMPOUND,
                            CONSTRUCTOR_MATRIX_MATRIX,
                            CONSTRUCTOR_MATRIX_SCALAR,
                            CONSTRUCTOR_STRUCT,
                            CONSTRUCTOR_VECTOR_SCALAR ->
                        // Constructors might be compile-time constants.
                            super.visitExpression(expr);
                    // This expression isn't a compile-time constant.
                    default -> true;
                };
            }
        }
        IsCompileTimeConstantVisitor visitor = new IsCompileTimeConstantVisitor();
        return !expr.accept(visitor);
    }

    public static boolean updateVariableRefKind(Expression expr, int refKind) {
        return true;
    }
}
