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

import icyllis.akashigi.slang.ThreadContext;

import javax.annotation.Nonnull;

/**
 * An identifier referring to a function name. This is an intermediate value: FunctionReferences are
 * always eventually replaced by FunctionCalls in valid programs.
 */
public final class FunctionReference extends Expression {

    private final Function mOverloadChain;

    private FunctionReference(int position, Function overloadChain, Type type) {
        super(position, ExpressionKind.kFunctionReference, type);
        mOverloadChain = overloadChain;
    }

    @Nonnull
    public static Expression make(int position, Function overloadChain) {
        ThreadContext context = ThreadContext.getInstance();
        return new FunctionReference(position, overloadChain, context.getTypes().mInvalid);
    }

    public Function getOverloadChain() {
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
