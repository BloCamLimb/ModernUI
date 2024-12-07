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

import icyllis.arc3d.compiler.ConstantFolder;
import icyllis.arc3d.compiler.Context;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;

public final class SwitchStatement extends Statement {

    private Expression mInit;
    // must be a BlockStatement containing only SwitchCase statements
    private Statement mCaseBlock;

    public SwitchStatement(int position,
                           Expression init,
                           Statement caseBlock) {
        super(position);
        mInit = init;
        mCaseBlock = caseBlock;
    }

    public static Statement convert(Context context,
                                    int position,
                                    Expression init,
                                    List<Expression> caseValues,
                                    List<Statement> caseStatements) {
        assert caseValues.size() == caseStatements.size();

        init = context.getTypes().mInt.coerceExpression(context, init);
        if (init == null) {
            return null;
        }

        ArrayList<Statement> cases = new ArrayList<>();
        for (int i = 0; i < caseValues.size(); i++) {
            if (caseValues.get(i) != null) {
                int casePos = caseValues.get(i).mPosition;
                // Case values must be constant integers of the same type as the switch value
                Expression caseValue = init.getType().coerceExpression(
                        context, caseValues.get(i));
                if (caseValue == null) {
                    return null;
                }
                OptionalLong intValue = ConstantFolder.getConstantInt(caseValue);
                if (intValue.isEmpty()) {
                    context.error(casePos, "case value must be a constant integer");
                    return null;
                }
                cases.add(SwitchCase.make(casePos, intValue.getAsLong(), caseStatements.get(i)));
            } else {
                cases.add(SwitchCase.makeDefault(position, caseStatements.get(i)));
            }
        }

        List<SwitchCase> duplicateCases = find_duplicate_cases(cases);
        if (!duplicateCases.isEmpty()) {
            for (SwitchCase sc : duplicateCases) {
                if (sc.isDefault()) {
                    context.error(sc.mPosition, "duplicate default case");
                } else {
                    context.error(sc.mPosition, "duplicate case value '" +
                            sc.getValue() + "'");
                }
            }
            return null;
        }

        Statement switchStmt = SwitchStatement.make(context,
                position, init,
                BlockStatement.makeBlock(position, cases));
        return switchStmt;
    }

    public static Statement make(Context context,
                                 int position,
                                 Expression init,
                                 Statement caseBlock) {
        return new SwitchStatement(position, init, caseBlock);
    }

    private static List<SwitchCase> find_duplicate_cases(List<Statement> cases) {
        ArrayList<SwitchCase> duplicateCases = new ArrayList<>();
        LongOpenHashSet intValues = new LongOpenHashSet();
        boolean foundDefault = false;

        for (Statement stmt : cases) {
            SwitchCase sc = (SwitchCase) stmt;
            if (sc.isDefault()) {
                if (foundDefault) {
                    duplicateCases.add(sc);
                    continue;
                }
                foundDefault = true;
            } else if (!intValues.add(sc.getValue())) {
                duplicateCases.add(sc);
            }
        }

        return duplicateCases;
    }

    public Expression getInit() {
        return mInit;
    }

    public void setInit(Expression init) {
        mInit = init;
    }

    public Statement getCaseBlock() {
        return mCaseBlock;
    }

    public void setCaseBlock(Statement caseBlock) {
        mCaseBlock = caseBlock;
    }

    public List<Statement> getCases() {
        return ((BlockStatement) mCaseBlock).getStatements();
    }

    @Override
    public StatementKind getKind() {
        return StatementKind.SWITCH;
    }

    @Override
    public boolean accept(@NonNull TreeVisitor visitor) {
        if (visitor.visitSwitch(this)) {
            return true;
        }
        return mInit.accept(visitor) || mCaseBlock.accept(visitor);
    }

    @NonNull
    @Override
    public String toString() {
        return "switch (" + mInit + ") " + mCaseBlock;
    }
}
