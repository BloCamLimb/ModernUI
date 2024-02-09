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

package icyllis.arc3d.compiler.tree;

import icyllis.arc3d.compiler.Context;
import icyllis.arc3d.compiler.analysis.TreeVisitor;

import javax.annotation.Nonnull;

/**
 * Represents an identifier referring to a type. This is an intermediate value: TypeReferences are
 * always eventually replaced by Constructors in valid programs.
 */
public final class TypeReference extends Expression {

    private final Type mValue;

    private TypeReference(int position, Type value, Type type) {
        super(position, type);
        mValue = value;
    }

    @Nonnull
    public static TypeReference make(@Nonnull Context context,
                                     int position, @Nonnull Type value) {
        return new TypeReference(position, value, context.getTypes().mInvalid);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.TYPE_REFERENCE;
    }

    @Override
    public boolean accept(@Nonnull TreeVisitor visitor) {
        return visitor.visitTypeReference(this);
    }

    public Type getValue() {
        return mValue;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new TypeReference(position, mValue, getType());
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        return mValue.getName();
    }
}
