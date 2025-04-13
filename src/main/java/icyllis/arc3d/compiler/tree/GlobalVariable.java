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
 * A variable declaration appearing at global scope. A global declaration like 'int x, y;' produces
 * two GlobalVariableDecl elements, each containing the declaration of one variable.
 */
public final class GlobalVariable extends TopLevelElement {

    private VariableDeclaration mDeclaration;

    public GlobalVariable(@NonNull VariableDeclaration decl) {
        super(decl.mPosition);
        mDeclaration = decl;
    }

    public VariableDeclaration getDeclaration() {
        return mDeclaration;
    }

    public void setDeclaration(Statement decl) {
        mDeclaration = (VariableDeclaration) decl;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.GLOBAL_VARIABLE;
    }

    @NonNull
    @Override
    public String toString() {
        return mDeclaration.toString();
    }
}
