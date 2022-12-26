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

package icyllis.akashigi.slang.ir;

import javax.annotation.Nonnull;

/**
 * Represents the construction of a struct object, such as "Color(red, green, blue, 1)".
 */
//TODO
public final class ConstructorStruct extends AnyConstructor {

    private ConstructorStruct(int position, Type type, Expression[] arguments) {
        super(position, ExpressionKind.kConstructorStruct, type, arguments);
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new ConstructorStruct(position, getType(), cloneArguments());
    }
}
