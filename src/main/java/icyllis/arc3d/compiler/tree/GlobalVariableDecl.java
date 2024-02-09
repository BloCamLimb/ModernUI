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

import icyllis.arc3d.compiler.analysis.TreeVisitor;

import javax.annotation.Nonnull;

/**
 * A variable declaration appearing at global scope. A global declaration like 'int x, y;' produces
 * two GlobalVariableDecl elements, each containing the declaration of one variable.
 */
public final class GlobalVariableDecl extends TopLevelElement {

    private final VariableDecl mVariableDecl;

    public GlobalVariableDecl(@Nonnull VariableDecl decl) {
        super(decl.mPosition);
        mVariableDecl = decl;
    }

    public VariableDecl getVariableDecl() {
        return mVariableDecl;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.GLOBAL_VARIABLE;
    }

    @Override
    public boolean accept(@Nonnull TreeVisitor visitor) {
        if (visitor.visitGlobalVariableDecl(this)) {
            return true;
        }
        return mVariableDecl.accept(visitor);
    }

    @Nonnull
    @Override
    public String toString() {
        return mVariableDecl.toString();
    }
}
