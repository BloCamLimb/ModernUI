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

import org.jspecify.annotations.NonNull;

/**
 * Represents the construction of a struct object, such as "Color(red, green, blue, 1)".
 */
//TODO
public final class ConstructorStruct extends ConstructorCall {

    private ConstructorStruct(int position, Type type, Expression[] arguments) {
        super(position, type, arguments);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.CONSTRUCTOR_STRUCT;
    }

    @NonNull
    @Override
    public Expression clone(int position) {
        return new ConstructorStruct(position, getType(), cloneArguments());
    }
}
