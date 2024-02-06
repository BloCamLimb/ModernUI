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
import icyllis.arc3d.compiler.analysis.NodeVisitor;

import javax.annotation.Nonnull;

/**
 * A function definition (a function declaration plus an associated block of code).
 */
public final class FunctionDefinition extends TopLevelElement {

    private final FunctionDecl mFunctionDecl;
    private final boolean mBuiltin;
    private Statement mBody;

    private FunctionDefinition(int position, FunctionDecl functionDecl, boolean builtin, Statement body) {
        super(position);
        mFunctionDecl = functionDecl;
        mBuiltin = builtin;
        mBody = body;
    }

    public static FunctionDefinition convert(@Nonnull Context context,
                                             int pos,
                                             FunctionDecl functionDecl,
                                             boolean builtin,
                                             Statement body) {
        if (functionDecl.isIntrinsic()) {
            context.error(pos, "Intrinsic function '" +
                    functionDecl.getName() +
                    "' should not have a definition");
            return null;
        }

        if (body == null || !(body instanceof BlockStatement block) || !block.isScoped()) {
            context.error(pos, "function body '" + functionDecl +
                    "' must be a braced block");
            return null;
        }

        if (functionDecl.getDefinition() != null) {
            context.error(pos, "function '" + functionDecl +
                    "' was already defined");
            return null;
        }

        return make(pos, functionDecl, builtin, body);
    }

    public static FunctionDefinition make(int pos,
                                          FunctionDecl functionDecl,
                                          boolean builtin,
                                          Statement body) {
        return new FunctionDefinition(pos, functionDecl, builtin, body);
    }

    public FunctionDecl getFunctionDecl() {
        return mFunctionDecl;
    }

    public boolean isBuiltin() {
        return mBuiltin;
    }

    public Statement getBody() {
        return mBody;
    }

    public void setBody(Statement body) {
        mBody = body;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.FUNCTION_DEFINITION;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        if (visitor.visitFunctionDefinition(this)) {
            return true;
        }
        return mBody.accept(visitor);
    }

    @Nonnull
    @Override
    public String toString() {
        return mFunctionDecl.toString() + " " + mBody.toString();
    }
}
