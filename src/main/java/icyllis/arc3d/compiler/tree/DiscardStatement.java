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

import icyllis.arc3d.compiler.ThreadContext;
import icyllis.arc3d.compiler.analysis.NodeVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A discard statement, fragment shader only.
 */
public final class DiscardStatement extends Statement {

    private DiscardStatement(int position) {
        super(position);
    }

    @Nullable
    public static Statement convert(int pos) {
        ThreadContext context = ThreadContext.getInstance();
        if (!context.getModel().isFragment()) {
            context.error(pos, "discard statement is only permitted in fragment shaders");
            return null;
        }
        return make(pos);
    }

    @Nonnull
    public static Statement make(int pos) {
        return new DiscardStatement(pos);
    }

    @Override
    public StatementKind getKind() {
        return StatementKind.DISCARD;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        return visitor.visitDiscard(this);
    }

    @Nonnull
    @Override
    public String toString() {
        return "discard;";
    }
}
