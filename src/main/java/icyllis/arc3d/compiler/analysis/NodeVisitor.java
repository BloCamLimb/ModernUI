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

package icyllis.arc3d.compiler.analysis;

import icyllis.arc3d.compiler.tree.*;

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
public abstract class NodeVisitor {

    public final boolean visit(TranslationUnit translationUnit) {
        for (TopLevelElement e : translationUnit) {
            if (e.accept(this)) {
                return true;
            }
        }
        return false;
    }

    public boolean visitFunctionPrototype(FunctionPrototype prototype) {
        return visitAnyTopLevelElement(prototype);
    }

    public boolean visitFunctionDefinition(FunctionDefinition definition) {
        return visitAnyTopLevelElement(definition);
    }

    public boolean visitAnyTopLevelElement(TopLevelElement e) {
        return false;
    }

    public boolean visitFunctionReference(FunctionReference expr) {
        return visitAnyExpression(expr);
    }

    public boolean visitVariableReference(VariableReference expr) {
        return visitAnyExpression(expr);
    }

    public boolean visitTypeReference(TypeReference expr) {
        return visitAnyExpression(expr);
    }

    public boolean visitLiteral(Literal expr) {
        return visitAnyExpression(expr);
    }

    public boolean visitFieldAccess(FieldAccess expr) {
        return visitAnyExpression(expr);
    }

    public boolean visitIndex(IndexExpression expr) {
        return visitAnyExpression(expr);
    }

    public boolean visitPostfix(PostfixExpression expr) {
        return visitAnyExpression(expr);
    }

    public boolean visitPrefix(PrefixExpression expr) {
        return visitAnyExpression(expr);
    }

    public boolean visitBinary(BinaryExpression expr) {
        return visitAnyExpression(expr);
    }

    public boolean visitConditional(ConditionalExpression expr) {
        return visitAnyExpression(expr);
    }

    public boolean visitSwizzle(Swizzle expr) {
        return visitAnyExpression(expr);
    }

    public boolean visitFunctionCall(FunctionCall expr) {
        return visitAnyExpression(expr);
    }

    public boolean visitConstructorCall(ConstructorCall expr) {
        return visitAnyExpression(expr);
    }

    /**
     * Fallback method for any expression kind that has not been overridden.
     */
    protected boolean visitAnyExpression(Expression expr) {
        return false;
    }

    public boolean visitBlock(BlockStatement stmt) {
        return visitAnyStatement(stmt);
    }

    public boolean visitBreak(BreakStatement stmt) {
        return visitAnyStatement(stmt);
    }

    public boolean visitContinue(ContinueStatement stmt) {
        return visitAnyStatement(stmt);
    }

    public boolean visitDiscard(DiscardStatement stmt) {
        return visitAnyStatement(stmt);
    }

    public boolean visitEmpty(EmptyStatement stmt) {
        return visitAnyStatement(stmt);
    }

    public boolean visitExpression(ExpressionStatement stmt) {
        return visitAnyStatement(stmt);
    }

    public boolean visitForLoop(ForLoop stmt) {
        return visitAnyStatement(stmt);
    }

    public boolean visitIf(IfStatement stmt) {
        return visitAnyStatement(stmt);
    }

    public boolean visitReturn(ReturnStatement stmt) {
        return visitAnyStatement(stmt);
    }

    protected boolean visitAnyStatement(Statement stmt) {
        return switch (stmt.getKind()) {
            case BREAK,
                    CONTINUE,
                    DISCARD,
                    EMPTY -> false; // Leaf expressions return false
            default -> true;
        };
    }
}
