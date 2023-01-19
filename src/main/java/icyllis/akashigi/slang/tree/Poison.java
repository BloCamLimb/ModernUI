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

package icyllis.akashigi.slang.tree;

import icyllis.akashigi.slang.Compiler;
import icyllis.akashigi.slang.ThreadContext;
import icyllis.akashigi.slang.analysis.NodeVisitor;

import javax.annotation.Nonnull;

/**
 * Represents an ill-formed expression. This is needed so that parser can go further.
 */
public final class Poison extends Expression {

    private Poison(int position, Type type) {
        super(position, type);
    }

    @Nonnull
    public static Expression make(int position) {
        return new Poison(position, ThreadContext.getInstance().getTypes().mPoison);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.POISON;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        return false;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new Poison(position, getType());
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        return Compiler.POISON_TAG;
    }
}
