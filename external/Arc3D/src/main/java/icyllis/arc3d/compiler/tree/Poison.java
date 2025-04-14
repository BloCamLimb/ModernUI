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
import icyllis.arc3d.compiler.ShaderCompiler;
import org.jspecify.annotations.NonNull;

/**
 * Represents an ill-formed expression. This is needed so that parser can go further.
 */
public final class Poison extends Expression {

    private Poison(int position, Type type) {
        super(position, type);
    }

    @NonNull
    public static Expression make(@NonNull Context context, int position) {
        return new Poison(position, context.getTypes().mPoison);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.POISON;
    }

    @NonNull
    @Override
    public Expression copy(int position) {
        return new Poison(position, getType());
    }

    @NonNull
    @Override
    public String toString(int parentPrecedence) {
        return ShaderCompiler.POISON_TAG;
    }
}
