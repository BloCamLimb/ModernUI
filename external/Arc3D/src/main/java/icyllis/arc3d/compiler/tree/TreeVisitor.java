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

package icyllis.arc3d.compiler.tree;

import icyllis.arc3d.compiler.tree.*;
import org.jetbrains.annotations.ApiStatus;

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

    public boolean visitFunctionPrototype(FunctionPrototype prototype) {
        return visitTopLevelElement(prototype);
    }

    public boolean visitFunctionDefinition(FunctionDefinition definition) {
        return visitTopLevelElement(definition);
    }

    public boolean visitGlobalVariableDecl(GlobalVariableDecl variableDecl) {
        return visitTopLevelElement(variableDecl);
    }

    public boolean visitInterfaceBlock(InterfaceBlock interfaceBlock) {
        return visitTopLevelElement(interfaceBlock);
    }

    /**
     * Fallback method for any element kind that has not been overridden.
     */
    @ApiStatus.OverrideOnly
    protected boolean visitTopLevelElement(TopLevelElement e) {
        return false;
    }

    /// Expressions

    public boolean visitFunctionReference(FunctionReference expr) {
        return visitExpression(expr);
    }

    public boolean visitVariableReference(VariableReference expr) {
        return visitExpression(expr);
    }

    public boolean visitTypeReference(TypeReference expr) {
        return visitExpression(expr);
    }

    public boolean visitLiteral(Literal expr) {
        return visitExpression(expr);
    }

    public boolean visitFieldAccess(FieldAccess expr) {
        return visitExpression(expr);
    }

    public boolean visitIndex(IndexExpression expr) {
        return visitExpression(expr);
    }

    public boolean visitPostfix(PostfixExpression expr) {
        return visitExpression(expr);
    }

    public boolean visitPrefix(PrefixExpression expr) {
        return visitExpression(expr);
    }

    public boolean visitBinary(BinaryExpression expr) {
        return visitExpression(expr);
    }

    public boolean visitConditional(ConditionalExpression expr) {
        return visitExpression(expr);
    }

    public boolean visitSwizzle(Swizzle expr) {
        return visitExpression(expr);
    }

    public boolean visitFunctionCall(FunctionCall expr) {
        return visitExpression(expr);
    }

    public boolean visitConstructorCall(ConstructorCall expr) {
        return visitExpression(expr);
    }

    /**
     * Fallback method for any expression kind that has not been overridden.
     */
    @ApiStatus.OverrideOnly
    protected boolean visitExpression(Expression expr) {
        return false;
    }

    /// Statements

    public boolean visitBlock(BlockStatement stmt) {
        return visitStatement(stmt);
    }

    public boolean visitBreak(BreakStatement stmt) {
        return visitStatement(stmt);
    }

    public boolean visitContinue(ContinueStatement stmt) {
        return visitStatement(stmt);
    }

    public boolean visitDiscard(DiscardStatement stmt) {
        return visitStatement(stmt);
    }

    public boolean visitEmpty(EmptyStatement stmt) {
        return visitStatement(stmt);
    }

    public boolean visitExpression(ExpressionStatement stmt) {
        return visitStatement(stmt);
    }

    public boolean visitForLoop(ForLoop stmt) {
        return visitStatement(stmt);
    }

    public boolean visitIf(IfStatement stmt) {
        return visitStatement(stmt);
    }

    public boolean visitReturn(ReturnStatement stmt) {
        return visitStatement(stmt);
    }

    public boolean visitVariableDecl(VariableDecl variableDecl) {
        return visitStatement(variableDecl);
    }

    /**
     * Fallback method for any statement kind that has not been overridden.
     */
    @ApiStatus.OverrideOnly
    protected boolean visitStatement(Statement stmt) {
        return false;
    }
}
