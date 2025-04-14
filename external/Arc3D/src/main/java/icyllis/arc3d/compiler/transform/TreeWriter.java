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

package icyllis.arc3d.compiler.transform;

import icyllis.arc3d.compiler.tree.*;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Similar to {@link icyllis.arc3d.compiler.analysis.TreeVisitor}, but this
 * allows statements or expressions to be replaced during a visit.
 */
public abstract class TreeWriter {

    /// Top level elements

    public boolean visitTopLevelElement(@NonNull TopLevelElement e) {
        return switch (e.getKind()) {
            case EXTENSION,
                 MODIFIERS,
                 INTERFACE_BLOCK,
                 STRUCT_DEFINITION,
                 FUNCTION_PROTOTYPE ->
                // Leaf program elements just return false by default
                    false;
            case FUNCTION_DEFINITION -> {
                var f = (FunctionDefinition) e;
                yield visitStatementPtr(f::getBody, f::setBody);
            }
            case GLOBAL_VARIABLE -> {
                var g = (GlobalVariable) e;
                yield visitStatementPtr(g::getDeclaration, g::setDeclaration);
            }
        };
    }

    /// Expressions

    public boolean visitExpression(@NonNull Expression expr) {
        switch (expr.getKind()) {
            case LITERAL,
                 POISON,
                 TYPE_REFERENCE,
                 VARIABLE_REFERENCE,
                 FUNCTION_REFERENCE -> {
                // Leaf expressions return false
                return false;
            }
            case BINARY -> {
                var b = (BinaryExpression) expr;
                return (b.getLeft() != null && visitExpressionPtr(b::getLeft, b::setLeft)) ||
                        (b.getRight() != null && visitExpressionPtr(b::getRight, b::setRight));
            }
            case CONDITIONAL -> {
                var c = (ConditionalExpression) expr;
                return visitExpressionPtr(c::getCondition, c::setCondition) ||
                        (c.getWhenTrue() != null && visitExpressionPtr(c::getWhenTrue, c::setWhenTrue)) ||
                        (c.getWhenFalse() != null && visitExpressionPtr(c::getWhenFalse, c::setWhenFalse));
            }
            case CONSTRUCTOR_ARRAY,
                 CONSTRUCTOR_ARRAY_CAST,
                 CONSTRUCTOR_COMPOUND,
                 CONSTRUCTOR_COMPOUND_CAST,
                 CONSTRUCTOR_DIAGONAL,
                 CONSTRUCTOR_MATRIX_RESIZE,
                 CONSTRUCTOR_SCALAR_CAST,
                 CONSTRUCTOR_STRUCT,
                 CONSTRUCTOR_SPLAT -> {
                var args = ((ConstructorCall) expr).getArguments();
                for (int i = 0; i < args.length; i++) {
                    int that = i;
                    if (visitExpressionPtr(() -> args[that], newE -> args[that] = newE)) {
                        return true;
                    }
                }
                return false;
            }
            case FIELD_ACCESS -> {
                var f = (FieldAccess) expr;
                return visitExpressionPtr(f::getBase, f::setBase);
            }
            case FUNCTION_CALL -> {
                var args = ((FunctionCall) expr).getArguments();
                for (int i = 0; i < args.length; i++) {
                    int that = i;
                    if (visitExpressionPtr(() -> args[that], newE -> args[that] = newE)) {
                        return true;
                    }
                }
                return false;
            }
            case INDEX -> {
                var i = (IndexExpression) expr;
                return visitExpressionPtr(i::getBase, i::setBase) ||
                        visitExpressionPtr(i::getIndex, i::setIndex);
            }
            case PREFIX -> {
                var p = (PrefixExpression) expr;
                return visitExpressionPtr(p::getOperand, p::setOperand);
            }
            case POSTFIX -> {
                var p = (PostfixExpression) expr;
                return visitExpressionPtr(p::getOperand, p::setOperand);
            }
            case SWIZZLE -> {
                var s = (Swizzle) expr;
                return s.getBase() != null && visitExpressionPtr(s::getBase, s::setBase);
            }
            default -> throw new AssertionError();
        }
    }

    public boolean visitExpressionPtr(@NonNull Supplier<Expression> getter,
                                      @NonNull Consumer<Expression> setter) {
        return visitExpression(getter.get());
    }

    /// Statements

    public boolean visitStatement(@NonNull Statement stmt) {
        switch (stmt.getKind()) {
            case BREAK,
                 CONTINUE,
                 DISCARD,
                 EMPTY -> {
                // Leaf statements just return false
                return false;
            }
            case BLOCK -> {
                var statements = ((Block) stmt).getStatements();
                for (int i = 0; i < statements.size(); i++) {
                    int that = i;
                    if (visitStatementPtr(() -> statements.get(that), newS -> statements.set(that, newS))) {
                        return true;
                    }
                }
                return false;
            }
            case EXPRESSION -> {
                var e = (ExpressionStatement) stmt;
                return visitExpressionPtr(e::getExpression, e::setExpression);
            }
            case FOR -> {
                var f = (ForStatement) stmt;
                return (f.getInit() != null && visitStatementPtr(f::getInit, f::setInit)) ||
                        (f.getCondition() != null && visitExpressionPtr(f::getCondition, f::setCondition)) ||
                        (f.getStep() != null && visitExpressionPtr(f::getStep, f::setStep)) ||
                        visitStatementPtr(f::getStatement, f::setStatement);
            }
            case IF -> {
                var i = (IfStatement) stmt;
                return (i.getCondition() != null && visitExpressionPtr(i::getCondition, i::setCondition)) ||
                        (i.getWhenTrue() != null && visitStatementPtr(i::getWhenTrue, i::setWhenTrue)) ||
                        (i.getWhenFalse() != null && visitStatementPtr(i::getWhenFalse, i::setWhenFalse));
            }
            case RETURN -> {
                var r = (ReturnStatement) stmt;
                return r.getExpression() != null && visitExpressionPtr(r::getExpression, r::setExpression);
            }
            case SWITCH -> {
                var s = (SwitchStatement) stmt;
                return visitExpressionPtr(s::getInit, s::setInit) ||
                        visitStatementPtr(s::getCaseBlock, s::setCaseBlock);
            }
            case SWITCH_CASE -> {
                var s = (SwitchCase) stmt;
                return visitStatementPtr(s::getStatement, s::setStatement);
            }
            case VARIABLE_DECLARATION -> {
                var v = (VariableDeclaration) stmt;
                return v.getInit() != null && visitExpressionPtr(v::getInit, v::setInit);
            }
            default -> throw new AssertionError();
        }
    }

    public boolean visitStatementPtr(@NonNull Supplier<Statement> getter,
                                     @NonNull Consumer<Statement> setter) {
        return visitStatement(getter.get());
    }
}
