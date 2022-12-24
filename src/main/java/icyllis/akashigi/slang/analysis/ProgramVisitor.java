/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
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

package icyllis.akashigi.slang.analysis;

import icyllis.akashigi.slang.ir.*;
import icyllis.akashigi.slang.ir.Node.ExpressionKind;
import icyllis.akashigi.slang.ir.Node.StatementKind;

/**
 * Utility class to visit every element, statement, and expression in a program IR.
 * This is intended for simple analysis and accumulation, where custom visitation behavior is only
 * needed for a limited set of expression kinds.
 * <p>
 * Subclasses should override visitExpression/visitStatement/visitProgramElement as needed and
 * intercept elements of interest. They can then invoke the base class's function to visit all
 * sub expressions. They can also choose not to call the base function to arrest recursion, or
 * implement custom recursion.
 * <p>
 * The visit functions return a bool that determines how the default implementation recurses. Once
 * any visit call returns true, the default behavior stops recursing and propagates true up the
 * stack.
 */
public class ProgramVisitor {

    public final boolean visit(Program program) {
        for (ProgramElement pe : program) {
            if (visitProgramElement(pe)) {
                return true;
            }
        }
        return false;
    }

    public boolean visitExpression(Expression expr) {
        return switch (expr.kind()) {
            case ExpressionKind.kFunctionReference,
                    ExpressionKind.kVariableReference,
                    ExpressionKind.kTypeReference,
                    ExpressionKind.kLiteral,
                    ExpressionKind.kPoison -> false; // Leaf expressions return false
            case ExpressionKind.kFieldAccess -> {
                var f = (FieldExpression) expr;
                yield visitExpressionChild(f.getBase());
            }
            case ExpressionKind.kPostfix -> {
                var u = (PostfixExpression) expr;
                yield visitExpressionChild(u.getOperand());
            }
            case ExpressionKind.kPrefix -> {
                var u = (PrefixExpression) expr;
                yield visitExpressionChild(u.getOperand());
            }
            case ExpressionKind.kBinary -> {
                var b = (BinaryExpression) expr;
                yield (b.getLeft() != null && visitExpressionChild(b.getLeft())) ||
                        (b.getRight() != null && visitExpressionChild(b.getRight()));
            }
            case ExpressionKind.kConditional -> {
                var t = (ConditionalExpression) expr;
                yield visitExpressionChild(t.getCondition()) ||
                        (t.getTrueExpr() != null && visitExpressionChild(t.getTrueExpr())) ||
                        (t.getFalseExpr() != null && visitExpressionChild(t.getFalseExpr()));
            }
            default -> true;
        };
    }

    public boolean visitStatement(Statement stmt) {
        return switch (stmt.kind()) {
            case StatementKind.kBreak,
                    StatementKind.kContinue,
                    StatementKind.kDiscard,
                    StatementKind.kNop -> false; // Leaf expressions return false
            default -> true;
        };
    }

    public boolean visitProgramElement(ProgramElement pe) {
        return false;
    }

    public boolean visitExpressionChild(Expression expr) {
        return visitExpression(expr);
    }

    public boolean visitStatementChild(Statement stmt) {
        return visitStatement(stmt);
    }
}
