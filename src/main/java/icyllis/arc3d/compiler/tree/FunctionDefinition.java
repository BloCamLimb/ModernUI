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

import icyllis.arc3d.compiler.analysis.NodeVisitor;

import javax.annotation.Nonnull;

/**
 * A function definition (a function declaration plus an associated block of code).
 */
public final class FunctionDefinition extends Element {

    private final Function mDecl;
    private final Statement mBody;

    private FunctionDefinition(int position, Function decl, Statement body) {
        super(position);
        mDecl = decl;
        mBody = body;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.FUNCTION_DEFINITION;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        return false;
    }

    public Function getDecl() {
        return mDecl;
    }

    public Statement getBody() {
        return mBody;
    }

    @Nonnull
    @Override
    public String toString() {
        return mDecl.toString() + mBody.toString();
    }
}
