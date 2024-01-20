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

import icyllis.arc3d.compiler.Position;
import icyllis.arc3d.compiler.analysis.NodeVisitor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * A block of multiple statements functioning as a single statement.
 */
public final class BlockStatement extends Statement {

    private List<Statement> mStatements;
    private boolean mScoped;

    public BlockStatement(int position, List<Statement> statements, boolean scoped) {
        super(position);
        mStatements = statements;
        mScoped = scoped;
    }

    public static Statement make(int pos, List<Statement> statements, boolean scoped) {
        if (scoped) {
            return new BlockStatement(pos, statements, true);
        }

        if (statements.isEmpty()) {
            return new EmptyStatement(pos);
        }

        if (statements.size() > 1) {
            Statement foundStatement = null;
            for (Statement stmt : statements) {
                if (!stmt.isEmpty()) {
                    if (foundStatement == null) {
                        foundStatement = stmt;
                        continue;
                    }
                    return new BlockStatement(pos, statements, scoped);
                }
            }

            if (foundStatement != null) {
                return foundStatement;
            }
        }

        return statements.get(0);
    }

    public static Statement makeCompound(Statement before, Statement after) {
        if (before == null || before.isEmpty()) {
            return after;
        }
        if (after == null || after.isEmpty()) {
            return before;
        }

        if (before instanceof BlockStatement block && !block.isScoped()) {
            block.getStatements().add(after);
            return before;
        }

        int pos = Position.range(before.getStartOffset(), after.getEndOffset());
        List<Statement> statements = new ArrayList<>(2);
        statements.add(before);
        statements.add(after);
        return make(pos, statements, false);
    }

    @Override
    public StatementKind getKind() {
        return StatementKind.BLOCK;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        if (visitor.visitBlock(this)) {
            return true;
        }
        for (Statement stmt : mStatements) {
            if (stmt.accept(visitor)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        for (Statement stmt : mStatements) {
            if (!stmt.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public List<Statement> getStatements() {
        return mStatements;
    }

    public void setStatements(List<Statement> statements) {
        mStatements = statements;
    }

    public boolean isScoped() {
        return mScoped;
    }

    public void setScoped(boolean scoped) {
        mScoped = scoped;
    }

    @Nonnull
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        boolean isScoped = isScoped() || isEmpty();
        if (isScoped) {
            result.append("{");
        }
        for (Statement stmt : mStatements) {
            result.append("\n");
            result.append(stmt.toString());
        }
        result.append(isScoped ? "\n}\n" : "\n");
        return result.toString();
    }
}
