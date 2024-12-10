/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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
 * A single case of a 'switch' statement.
 */
public final class SwitchCase extends Statement {

    private final boolean mIsDefault;
    private final long mValue;
    private Statement mStatement;

    private SwitchCase(int position, boolean isDefault, long value, Statement statement) {
        super(position);
        mIsDefault = isDefault;
        mValue = value;
        mStatement = statement;
    }

    @NonNull
    public static SwitchCase make(int position,
                                  long value,
                                  Statement statement) {
        return new SwitchCase(position, false, value, statement);
    }

    @NonNull
    public static SwitchCase makeDefault(int position,
                                         Statement statement) {
        return new SwitchCase(position, true, -1, statement);
    }

    public boolean isDefault() {
        return mIsDefault;
    }

    public long getValue() {
        return mValue;
    }

    public Statement getStatement() {
        return mStatement;
    }

    public void setStatement(Statement statement) {
        mStatement = statement;
    }

    @Override
    public StatementKind getKind() {
        return StatementKind.SWITCH_CASE;
    }

    @Override
    public boolean accept(@NonNull TreeVisitor visitor) {
        if (visitor.visitSwitchCase(this)) {
            return true;
        }
        return mStatement.accept(visitor);
    }

    @NonNull
    @Override
    public String toString() {
        return mIsDefault
                ? "default: \n" + mStatement
                : "case " + mValue + ": \n" + mStatement;
    }
}
