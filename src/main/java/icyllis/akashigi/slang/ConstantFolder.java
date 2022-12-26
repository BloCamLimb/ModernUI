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

package icyllis.akashigi.slang;

import icyllis.akashigi.slang.analysis.Analysis;
import icyllis.akashigi.slang.ir.*;

import javax.annotation.Nullable;

public class ConstantFolder {

    public static Expression makeConstantValueForVariable(int position, Expression expr) {
        Expression constexpr = getConstantValueOrNullForVariable(expr);
        return constexpr != null ? constexpr.clone(position) : expr;
    }

    /**
     * If the expression is a const variable with a known compile-time-constant value, returns that
     * value. If not, returns the original expression as-is.
     */
    public static Expression getConstantValueForVariable(Expression expr) {
        Expression constexpr = getConstantValueOrNullForVariable(expr);
        return constexpr != null ? constexpr : expr;
    }

    /**
     * If the expression is a const variable with a known compile-time-constant value, returns that
     * value. If not, returns null.
     */
    @Nullable
    public static Expression getConstantValueOrNullForVariable(Expression expr) {
        for (;;) {
            if (!(expr instanceof VariableReference ref)) {
                break;
            }
            if (ref.getReferenceKind() != VariableReference.kRead_ReferenceKind) {
                break;
            }
            Variable var = ref.getVariable();
            if ((var.getModifiers() & Modifier.kConst_Flag) == 0) {
                break;
            }
            expr = var.initialValue();
            if (expr == null) {
                // Function parameters can be const but won't have an initial value.
                break;
            }
            if (Analysis.isCompileTimeConstant(expr)) {
                return expr;
            }
        }
        // We didn't find a compile-time constant at the end.
        return null;
    }
}
