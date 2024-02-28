/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

import javax.annotation.Nonnull;

/**
 * An identifier referring to a function name. This is an intermediate value: FunctionReferences are
 * always eventually replaced by FunctionCalls in valid programs.
 */
public final class FunctionReference extends Expression {

    private final FunctionDecl mOverloadChain;

    private FunctionReference(int position, FunctionDecl overloadChain, Type type) {
        super(position, type);
        mOverloadChain = overloadChain;
    }

    @Nonnull
    public static Expression make(@Nonnull Context context,
                                  int position, FunctionDecl overloadChain) {
        return new FunctionReference(position, overloadChain, context.getTypes().mInvalid);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.FUNCTION_REFERENCE;
    }

    @Override
    public boolean accept(@Nonnull TreeVisitor visitor) {
        return visitor.visitFunctionReference(this);
    }

    public FunctionDecl getOverloadChain() {
        return mOverloadChain;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new FunctionReference(position, mOverloadChain, getType());
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        return "<function>";
    }
}
