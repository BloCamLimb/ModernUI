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

import icyllis.arc3d.compiler.Context;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Variable declaration.
 */
public final class VariableDecl extends Statement {

    private Variable mVariable;
    private Expression mInit;

    public VariableDecl(Variable variable, Expression init) {
        super(variable.mPosition);
        mVariable = variable;
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
        //TODO more checks
    }

    // For use when no Variable yet exists. The newly-created variable will be added to the active
    // symbol table. Performs proper error checking and type coercion; reports errors via
    // ErrorReporter.
    @Nullable
    public static VariableDecl convert(@NonNull Context context,
                                       int pos,
                                       @NonNull Modifiers modifiers,
                                       @NonNull Type type,
                                       @NonNull String name,
                                       byte storage,
                                       @Nullable Expression init) {
        // Parameter declaration-statements do not exist in the grammar (unlike, say, K&R C).
        assert (storage != Variable.kParameter_Storage);

        if (init != null && type.isUnsizedArray() && init.getType().isArray()) {
            // implicitly sized array
            int arraySize = init.getType().getArraySize();
            if (arraySize > 0) {
                type = context.getSymbolTable().getArrayType(
                        type.getElementType(), arraySize);
            }
        }

        Variable variable = Variable.convert(context, pos, modifiers, type, name, storage);

        return VariableDecl.convert(context, variable, init);
    }

    @Nullable
    public static VariableDecl convert(@NonNull Context context,
                                       @NonNull Variable variable,
                                       @Nullable Expression init) {
        Type baseType = variable.getType();
        if (baseType.isArray()) {
            baseType = baseType.getElementType();
        }

        if (baseType.matches(context.getTypes().mInvalid)) {
            context.error(variable.mPosition, "invalid type");
            return null;
        }
        if (baseType.isVoid()) {
            context.error(variable.mPosition, "variables of type 'void' are not allowed");
            return null;
        }

        checkError(variable.mPosition, variable.getModifiers(), variable.getType(), baseType, variable.getStorage());

        if (init != null) {
            if ((variable.getModifiers().flags() & Modifiers.kIn_Flag) != 0) {
                context.error(init.mPosition,
                        "'in' variables cannot use initializer expressions");
                return null;
            }
            if (variable.getModifiers().isUniform()) {
                context.error(init.mPosition,
                        "'uniform' variables cannot use initializer expressions");
                return null;
            }
            init = variable.getType().coerceExpression(context, init);
            if (init == null) {
                return null;
            }
        }

        if (variable.getModifiers().isConst()) {
            if (init == null) {
                context.error(variable.mPosition, "'const' variables must be initialized");
                return null;
            }
            //TODO check const expression
        }

        VariableDecl variableDecl = make(variable, init);

        context.getSymbolTable().insert(context, variable);
        return variableDecl;
    }

    @NonNull
    public static VariableDecl make(Variable variable,
                                    Expression init) {
        var result = new VariableDecl(variable, init);
        variable.setVariableDecl(result);
        return result;
    }

    public Variable getVariable() {
        return mVariable;
    }

    public void setVariable(Variable variable) {
        mVariable = variable;
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

    @NonNull
    @Override
    public String toString() {
        String result = mVariable.toString();
        if (mInit != null) {
            result += " = " + mInit;
        }
        return result + ";";
    }
}
