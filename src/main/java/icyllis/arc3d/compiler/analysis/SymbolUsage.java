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
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;

/**
 * Counts the number of usages of a symbol.
 */
public final class SymbolUsage extends TreeVisitor {

    public static class Count {
        int use;

        @Override
        public String toString() {
            return Integer.toString(use);
        }
    }

    public static class VariableCounts {
        public int decl;
        public int read;
        public int write;

        @Override
        public String toString() {
            return "Counts{" +
                    "decl=" + decl +
                    ", read=" + read +
                    ", write=" + write +
                    '}';
        }
    }

    public final IdentityHashMap<Type, Count> mStructCounts = new IdentityHashMap<>();
    public final IdentityHashMap<FunctionDecl, Count> mFunctionCounts = new IdentityHashMap<>();
    public final IdentityHashMap<Variable, VariableCounts> mVariableCounts = new IdentityHashMap<>();

    private int mDelta;

    @NonNull
    public Count computeStructCount(Type typeSymbol) {
        return mStructCounts.computeIfAbsent(typeSymbol, __ -> new Count());
    }

    @Nullable
    public Count findStructCount(Type typeSymbol) {
        return mStructCounts.get(typeSymbol);
    }

    public int getStructCount(Type typeSymbol) {
        Count count = findStructCount(typeSymbol);
        if (count != null) {
            return count.use;
        }
        return 0;
    }

    @NonNull
    public Count computeFunctionCount(FunctionDecl functionSymbol) {
        return mFunctionCounts.computeIfAbsent(functionSymbol, __ -> new Count());
    }

    @Nullable
    public Count findFunctionCount(FunctionDecl functionSymbol) {
        return mFunctionCounts.get(functionSymbol);
    }

    public int getFunctionCount(FunctionDecl functionSymbol) {
        Count count = findFunctionCount(functionSymbol);
        if (count != null) {
            return count.use;
        }
        return 0;
    }

    @NonNull
    public VariableCounts computeVariableCounts(Variable varSymbol) {
        return mVariableCounts.computeIfAbsent(varSymbol, __ -> new VariableCounts());
    }

    @Nullable
    public VariableCounts findVariableCounts(Variable varSymbol) {
        return mVariableCounts.get(varSymbol);
    }

    public void add(@NonNull Node node) {
        mDelta = 1;
        node.accept(this);
    }

    public void remove(@NonNull Node node) {
        mDelta = -1;
        node.accept(this);
    }

    @Override
    public boolean visitFunctionDefinition(FunctionDefinition definition) {
        for (var param : definition.getFunctionDecl().getParameters()) {
            VariableCounts counts = computeVariableCounts(param);
            counts.decl += mDelta;
            visitType(param.getType());
        }
        return super.visitFunctionDefinition(definition);
    }

    @Override
    public boolean visitInterfaceBlock(InterfaceBlock interfaceBlock) {
        computeVariableCounts(interfaceBlock.getVariable());
        visitType(interfaceBlock.getVariable().getType());
        return super.visitInterfaceBlock(interfaceBlock);
    }

    @Override
    public boolean visitFunctionCall(FunctionCall expr) {
        Count count = computeFunctionCount(expr.getFunction());
        count.use += mDelta;
        assert count.use >= 0;
        return super.visitFunctionCall(expr);
    }

    @Override
    public boolean visitVariableReference(VariableReference expr) {
        VariableCounts counts = computeVariableCounts(expr.getVariable());
        switch (expr.getReferenceKind()) {
            case VariableReference.kRead_ReferenceKind -> counts.read += mDelta;
            case VariableReference.kWrite_ReferenceKind -> counts.write += mDelta;
            case VariableReference.kReadWrite_ReferenceKind, VariableReference.kPointer_ReferenceKind -> {
                counts.read += mDelta;
                counts.write += mDelta;
            }
        }
        assert counts.read >= 0 && counts.write >= 0;
        return super.visitVariableReference(expr);
    }

    @Override
    protected boolean visitExpression(Expression expr) {
        visitType(expr.getType());
        return super.visitExpression(expr);
    }

    @Override
    public boolean visitVariableDecl(VariableDecl variableDecl) {
        VariableCounts counts = computeVariableCounts(variableDecl.getVariable());
        counts.decl += mDelta;
        assert counts.decl == 0 || counts.decl == 1;
        if (variableDecl.getInit() != null) {
            counts.write += mDelta;
        }
        visitType(variableDecl.getVariable().getType());
        return super.visitVariableDecl(variableDecl);
    }

    private void visitType(Type t) {
        if (t.isArray()) {
            visitType(t.getElementType());
        } else if (t.isStruct()) {
            Count count = computeStructCount(t);
            count.use += mDelta;
            assert count.use >= 0;
            for (var field : t.getFields()) {
                visitType(field.type());
            }
        }
    }

    @Override
    public String toString() {
        return "ModuleUsage{" +
                "mStructCounts=" + mStructCounts +
                ", mFunctionCounts=" + mFunctionCounts +
                ", mVariableCounts=" + mVariableCounts +
                '}';
    }
}
