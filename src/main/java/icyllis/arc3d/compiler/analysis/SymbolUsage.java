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
public final class SymbolUsage {

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

    private final TreeVisitor mVisitor = new TreeVisitor() {
        @Override
        public boolean visitTopLevelElement(@NonNull TopLevelElement e) {
            if (e instanceof FunctionDefinition definition) {
                for (var param : definition.getFunctionDecl().getParameters()) {
                    // Ensure function-parameter variables exist in the variable usage map. They aren't
                    // otherwise declared, but ProgramUsage::get() should be able to find them, even if
                    // they are unread and unwritten.
                    VariableCounts counts = computeVariableCounts(param);
                    counts.decl += mDelta;

                    visitType(param.getType());
                }
            } else if (e instanceof InterfaceBlock interfaceBlock) {
                // Ensure interface-block variables exist in the variable usage map.
                computeVariableCounts(interfaceBlock.getVariable());

                visitType(interfaceBlock.getVariable().getType());
            } else if (e instanceof StructDefinition definition) {
                // Ensure that structs referenced as nested types in other structs are counted as used.
                for (var field : definition.getType().getFields()) {
                    visitType(field.type());
                }
            }
            return super.visitTopLevelElement(e);
        }

        @Override
        public boolean visitExpression(@NonNull Expression expr) {
            visitType(expr.getType());
            if (expr instanceof FunctionCall call) {
                Count count = computeFunctionCount(call.getFunction());
                count.use += mDelta;
                assert count.use >= 0;
            } else if (expr instanceof VariableReference ref) {
                VariableCounts counts = computeVariableCounts(ref.getVariable());
                switch (ref.getReferenceKind()) {
                    case VariableReference.kRead_ReferenceKind -> counts.read += mDelta;
                    case VariableReference.kWrite_ReferenceKind -> counts.write += mDelta;
                    case VariableReference.kReadWrite_ReferenceKind, VariableReference.kPointer_ReferenceKind -> {
                        counts.read += mDelta;
                        counts.write += mDelta;
                    }
                }
                assert counts.read >= 0 && counts.write >= 0;
            }
            return super.visitExpression(expr);
        }

        @Override
        public boolean visitStatement(@NonNull Statement stmt) {
            if (stmt instanceof VariableDecl decl) {
                // Add all declared variables to the usage map (even if never otherwise accessed).
                VariableCounts counts = computeVariableCounts(decl.getVariable());
                counts.decl += mDelta;
                assert counts.decl == 0 || counts.decl == 1;
                if (decl.getInit() != null) {
                    // The initial-value expression, when present, counts as a write.
                    counts.write += mDelta;
                }
                visitType(decl.getVariable().getType());
            }
            return super.visitStatement(stmt);
        }
    };

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
        node.accept(mVisitor);
    }

    public void remove(@NonNull Node node) {
        mDelta = -1;
        node.accept(mVisitor);
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
        return "SymbolUsage{" +
                "mStructCounts=" + mStructCounts +
                ", mFunctionCounts=" + mFunctionCounts +
                ", mVariableCounts=" + mVariableCounts +
                '}';
    }
}
