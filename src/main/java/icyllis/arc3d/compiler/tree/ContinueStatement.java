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

/**
 * A continue statement.
 */
public final class ContinueStatement extends Statement {

    private ContinueStatement(int position) {
        super(position);
    }

    public static Statement make(int pos) {
        return new ContinueStatement(pos);
    }

    @Override
    public StatementKind getKind() {
        return StatementKind.CONTINUE;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        return visitor.visitContinue(this);
    }

    @Nonnull
    @Override
    public String toString() {
        return "continue;";
    }
}
