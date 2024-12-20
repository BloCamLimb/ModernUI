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

package icyllis.arc3d.compiler.analysis;

import icyllis.arc3d.compiler.tree.*;
import org.jspecify.annotations.NonNull;

/**
 * Utility class to visit every element, statement, and expression in a program IR.
 * This is intended for simple analysis and accumulation, where custom visitation behavior is only
 * needed for a limited set of expression kinds.
 * <p>
 * Subclasses should override visitExpression/visitStatement/visitElement as needed and
 * intercept elements of interest. They can then invoke the base class's function to visit all
 * sub expressions. They can also choose not to call the base function to arrest recursion, or
 * implement custom recursion.
 * <p>
 * The visit functions return a bool that determines how the default implementation recursions. Once
 * any visit call returns true, the default behavior stops recursion and propagates true up the
 * stack.
 */
public abstract class TreeVisitor {

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
                yield visitStatement(f.getBody());
            }
            case GLOBAL_VARIABLE -> {
                var g = (GlobalVariableDecl) e;
                yield visitStatement(g.getVariableDecl());
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
                return (b.getLeft() != null && visitExpression(b.getLeft())) ||
                        (b.getRight() != null && visitExpression(b.getRight()));
            }
            case CONDITIONAL -> {
                var c = (ConditionalExpression) expr;
                return visitExpression(c.getCondition()) ||
                        (c.getWhenTrue() != null && visitExpression(c.getWhenTrue())) ||
                        (c.getWhenFalse() != null && visitExpression(c.getWhenFalse()));
            }
            case CONSTRUCTOR_ARRAY,
                 CONSTRUCTOR_ARRAY_CAST,
                 CONSTRUCTOR_COMPOUND,
                 CONSTRUCTOR_COMPOUND_CAST,
                 CONSTRUCTOR_DIAGONAL_MATRIX,
                 CONSTRUCTOR_MATRIX_RESIZE,
                 CONSTRUCTOR_SCALAR_CAST,
                 CONSTRUCTOR_STRUCT,
                 CONSTRUCTOR_VECTOR_SPLAT -> {
                var c = (ConstructorCall) expr;
                for (var arg : c.getArguments()) {
                    if (visitExpression(arg)) {
                        return true;
                    }
                }
                return false;
            }
            case FIELD_ACCESS -> {
                var f = (FieldAccess) expr;
                return visitExpression(f.getBase());
            }
            case FUNCTION_CALL -> {
                var c = (FunctionCall) expr;
                for (var arg : c.getArguments()) {
                    if (visitExpression(arg)) {
                        return true;
                    }
                }
                return false;
            }
            case INDEX -> {
                var i = (IndexExpression) expr;
                return visitExpression(i.getBase()) || visitExpression(i.getIndex());
            }
            case PREFIX -> {
                var p = (PrefixExpression) expr;
                return visitExpression(p.getOperand());
            }
            case POSTFIX -> {
                var p = (PostfixExpression) expr;
                return visitExpression(p.getOperand());
            }
            case SWIZZLE -> {
                var s = (Swizzle) expr;
                return s.getBase() != null && visitExpression(s.getBase());
            }
            default -> throw new AssertionError();
        }
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
                var b = (BlockStatement) stmt;
                for (var s : b.getStatements()) {
                    if (visitStatement(s)) {
                        return true;
                    }
                }
                return false;
            }
            case EXPRESSION -> {
                var e = (ExpressionStatement) stmt;
                return visitExpression(e.getExpression());
            }
            case FOR_LOOP -> {
                var f = (ForLoop) stmt;
                return (f.getInit() != null && visitStatement(f.getInit())) ||
                        (f.getCondition() != null && visitExpression(f.getCondition())) ||
                        (f.getStep() != null && visitExpression(f.getStep())) ||
                        visitStatement(f.getStatement());
            }
            case IF -> {
                var i = (IfStatement) stmt;
                return (i.getCondition() != null && visitExpression(i.getCondition())) ||
                        (i.getWhenTrue() != null && visitStatement(i.getWhenTrue())) ||
                        (i.getWhenFalse() != null && visitStatement(i.getWhenFalse()));
            }
            case RETURN -> {
                var r = (ReturnStatement) stmt;
                return r.getExpression() != null && visitExpression(r.getExpression());
            }
            case SWITCH -> {
                var s = (SwitchStatement) stmt;
                return visitExpression(s.getInit()) || visitStatement(s.getCaseBlock());
            }
            case SWITCH_CASE -> {
                var s = (SwitchCase) stmt;
                return visitStatement(s.getStatement());
            }
            case VARIABLE_DECL -> {
                var v = (VariableDecl) stmt;
                return v.getInit() != null && visitExpression(v.getInit());
            }
            default -> throw new AssertionError();
        }
    }
}
