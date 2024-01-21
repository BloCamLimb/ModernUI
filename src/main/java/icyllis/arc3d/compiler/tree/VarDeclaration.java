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

import icyllis.arc3d.compiler.analysis.NodeVisitor;

import javax.annotation.Nonnull;

public class VarDeclaration extends Statement {

    protected VarDeclaration(int position) {
        super(position);
    }

    // Checks the modifiers, baseType, and storage for compatibility with one another and reports
    // errors if needed. This method is implicitly called during Convert(), but is also explicitly
    // called while processing interface block fields.
    public static void checkError(int pos, Modifiers modifiers,
                                  Type type, Type baseType, byte storage) {
        assert type.isArray()
                ? baseType.matches(type.getElementType())
                : baseType.matches(type);
    }

    @Override
    public StatementKind getKind() {
        return StatementKind.VAR_DECLARATION;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        return false;
    }

    @Nonnull
    @Override
    public String toString() {
        return null;
    }
}
