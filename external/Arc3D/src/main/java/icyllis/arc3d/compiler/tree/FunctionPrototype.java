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
 * A function prototype (a function declaration as a top-level element)
 */
public final class FunctionPrototype extends TopLevelElement {

    private final FunctionDeclaration mDeclaration;
    private final boolean mBuiltin;

    public FunctionPrototype(int position, FunctionDeclaration declaration, boolean builtin) {
        super(position);
        mDeclaration = declaration;
        mBuiltin = builtin;
    }

    public FunctionDeclaration getDeclaration() {
        return mDeclaration;
    }

    public boolean isBuiltin() {
        return mBuiltin;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.FUNCTION_PROTOTYPE;
    }

    @NonNull
    @Override
    public String toString() {
        return mDeclaration.toString() + ";";
    }
}
