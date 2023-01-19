/*
 * Akashi GI.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
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

package icyllis.akashigi.slang.dsl;

import icyllis.akashigi.slang.tree.*;

import javax.annotation.Nonnull;

public class DSLCore {

    @Nonnull
    public static Expression Conditional(int position, Expression condition,
                                         Expression trueExpr, Expression falseExpr) {
        Expression result = ConditionalExpression.convert(position, condition, trueExpr, falseExpr);
        if (result != null) {
            return result;
        }
        return Poison.make(position);
    }
}
