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

import icyllis.arc3d.compiler.ThreadContext;
import icyllis.arc3d.compiler.analysis.NodeVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Variable declaration.
 */
public final class VariableDecl extends Statement {

    private Variable mVar;
    private final Type mBaseType;
    private final int mArraySize;
    private Expression mInit;

    public VariableDecl(Variable var, Type baseType, int arraySize, Expression init) {
        super(var.mPosition);
        mVar = var;
        mBaseType = baseType;
        mArraySize = arraySize;
        mInit = init;
    }

    // Checks the modifiers, baseType, and storage for compatibility with one another and reports
    // errors if needed. This method is implicitly called during Convert(), but is also explicitly
    // called while processing interface block fields.
    public static void checkError(int pos, Modifiers modifiers,
                                  Type type, Type baseType, byte storage) {
        assert type.isArray()
                ? baseType.matches(type.getElementType())
                : baseType.matches(type);
    }

    public static VariableDecl convert(int pos,
                                       Modifiers modifiers,
                                       Type type,
                                       String name,
                                       byte storage,
                                       @Nullable Expression init) {
        Variable var = Variable.convert(pos, modifiers, type, name, storage);

        if (var == null) {
            return null;
        }
        return convert(var, init);
    }

    @Nullable
    public static VariableDecl convert(@Nonnull Variable var,
                                       @Nullable Expression init) {
        Type baseType = var.getType();
        int arraySize = 0;
        if (baseType.isArray()) {
            arraySize = baseType.getArraySize();
            baseType = baseType.getElementType();
        }

        ThreadContext context = ThreadContext.getInstance();
        if (baseType.matches(context.getTypes().mInvalid)) {
            context.error(var.mPosition, "invalid type");
            return null;
        }
        if (baseType.isVoid()) {
            context.error(var.mPosition, "variables of type 'void' are not allowed");
            return null;
        }

        checkError(var.mPosition, var.getModifiers(), var.getType(), baseType, var.getStorage());

        if (init != null) {
            init = var.getType().coerceExpression(init);
            if (init == null) {
                return null;
            }
        }

        if ((var.getModifiers().flags() & Modifiers.kConst_Flag) != 0) {
            if (init == null) {
                context.error(var.mPosition, "'const' variables must be initialized");
                return null;
            }
        }

        VariableDecl varDecl = make(var, baseType, arraySize, init);

        if (varDecl == null) {
            return null;
        }

        context.getSymbolTable().insert(var);
        return varDecl;
    }

    public static VariableDecl make(Variable var,
                                    Type baseType,
                                    int arraySize,
                                    Expression init) {
        var result = new VariableDecl(var, baseType, arraySize, init);
        var.setVarDecl(result);
        return result;
    }

    public Variable getVar() {
        return mVar;
    }

    public void setVar(Variable var) {
        mVar = var;
    }

    public Type getBaseType() {
        return mBaseType;
    }

    public int getArraySize() {
        return mArraySize;
    }

    public Expression getInit() {
        return mInit;
    }

    public void setInit(Expression init) {
        mInit = init;
    }

    @Override
    public StatementKind getKind() {
        return StatementKind.VARIABLE_DECL;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        return false;
    }

    @Nonnull
    @Override
    public String toString() {
        return null;
    }
}
