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

package icyllis.akashigi.slang.ir;

import javax.annotation.Nonnull;

/**
 * A function definition (a function declaration plus an associated block of code).
 */
public final class FunctionDefinition extends Element {

    private final Function mDecl;
    private final Statement mBody;

    private FunctionDefinition(int position, Function decl, Statement body) {
        super(position, ElementKind.kFunctionDefinition);
        mDecl = decl;
        mBody = body;
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
