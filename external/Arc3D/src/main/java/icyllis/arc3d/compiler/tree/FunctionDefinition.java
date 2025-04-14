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
import org.jspecify.annotations.NonNull;

/**
 * A function definition (a function declaration plus an associated block of code).
 */
public final class FunctionDefinition extends TopLevelElement {

    private final FunctionDeclaration mDeclaration;
    private final boolean mBuiltin;
    private Block mBody;

    private FunctionDefinition(int position, FunctionDeclaration declaration, boolean builtin, Block body) {
        super(position);
        mDeclaration = declaration;
        mBuiltin = builtin;
        mBody = body;
    }

    public static FunctionDefinition convert(@NonNull Context context,
                                             int pos,
                                             FunctionDeclaration functionDeclaration,
                                             boolean builtin,
                                             Statement body) {
        if (functionDeclaration.isIntrinsic()) {
            context.error(pos, "Intrinsic function '" +
                    functionDeclaration.getName() +
                    "' should not have a definition");
            return null;
        }

        if (body == null || !(body instanceof Block block) || !block.isScoped()) {
            context.error(pos, "function body '" + functionDeclaration +
                    "' must be a braced block");
            return null;
        }

        if (functionDeclaration.getDefinition() != null) {
            context.error(pos, "function '" + functionDeclaration +
                    "' was already defined");
            return null;
        }

        return make(pos, functionDeclaration, builtin, block);
    }

    public static FunctionDefinition make(int pos,
                                          FunctionDeclaration functionDeclaration,
                                          boolean builtin,
                                          Block body) {
        return new FunctionDefinition(pos, functionDeclaration, builtin, body);
    }

    public FunctionDeclaration getDeclaration() {
        return mDeclaration;
    }

    public boolean isBuiltin() {
        return mBuiltin;
    }

    public Block getBody() {
        return mBody;
    }

    public void setBody(Statement body) {
        mBody = (Block) body;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.FUNCTION_DEFINITION;
    }

    @NonNull
    @Override
    public String toString() {
        return mDeclaration.toString() + " " + mBody.toString();
    }
}
