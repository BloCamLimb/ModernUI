/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.compiler.dsl;

import icyllis.arc3d.compiler.Operator;
import icyllis.arc3d.compiler.tree.*;

import javax.annotation.Nonnull;

public class DSLExpression {

    @Nonnull
    public static Expression Prefix(Expression base, Operator op, int position) {
        Expression result = PrefixExpression.convert(position, op, base);
        if (result != null) {
            return result;
        }
        return Poison.make(position);
    }
}
