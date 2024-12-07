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

package icyllis.arc3d.compiler;

import icyllis.arc3d.compiler.tree.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

/**
 * Contains objects and state associated with {@link ShaderCompiler}
 * (i.e. {@link Parser} or CodeGenerator).
 */
public final class Context {

    // The Context holds a pointer to the configuration of the program being compiled.
    private @Nullable ShaderKind mKind;
    private @Nullable CompileOptions mOptions;
    private boolean mIsBuiltin;
    private boolean mIsModule;

    // The Context holds all of the built-in types.
    private final BuiltinTypes mTypes;

    // This is the current symbol table of the code we are processing, and therefore changes during
    // compilation
    private SymbolTable mSymbolTable;

    // The Context holds a pointer to our error handler.
    ErrorHandler mErrorHandler;

    private boolean mActive;

    Context(ErrorHandler errorHandler) {
        mTypes = ModuleLoader.getInstance().getBuiltinTypes();
        mErrorHandler = errorHandler;
    }

    /**
     * Starts the DSL on the current thread for compiling modules.
     */
    void start(ShaderKind kind, CompileOptions options,
               ModuleUnit parent, boolean isBuiltin, boolean isModule) {
        if (isActive()) {
            throw new IllegalStateException("DSL is already started");
        }
        mKind = Objects.requireNonNull(kind);
        mOptions = Objects.requireNonNull(options);
        mIsBuiltin = isBuiltin;
        mIsModule = isModule;

        if (parent != null) {
            mSymbolTable = parent.mSymbols.enterModule(isBuiltin);
        }

        mActive = true;
    }

    /**
     * Ends the DSL on the current thread.
     */
    void end() {
        mKind = null;
        mOptions = null;
        mSymbolTable = null;

        mActive = false;
    }

    /**
     * Returns true if the DSL has been started.
     */
    public boolean isActive() {
        return mActive;
    }

    public ShaderKind getKind() {
        assert mKind != null;
        return mKind;
    }

    @NonNull
    public CompileOptions getOptions() {
        assert mOptions != null;
        return mOptions;
    }

    /**
     * @return true if we are at built-in level
     */
    public boolean isBuiltin() {
        return mIsBuiltin;
    }

    /**
     * @return true if we are processing include files
     */
    public boolean isModule() {
        return mIsModule;
    }

    /**
     * Returns the BuiltinTypes used by DSL operations in the current thread.
     */
    public BuiltinTypes getTypes() {
        return mTypes;
    }

    /**
     * Returns the current SymbolTable.
     */
    public SymbolTable getSymbolTable() {
        return mSymbolTable;
    }

    /**
     * Enters a scope level.
     */
    public void enterScope() {
        mSymbolTable = mSymbolTable.enterScope();
    }

    /**
     * Leaves a scope level.
     */
    public void leaveScope() {
        mSymbolTable = mSymbolTable.leaveScope();
    }

    public void error(int position, String msg) {
        mErrorHandler.error(position, msg);
    }

    public void error(int start, int end, String msg) {
        mErrorHandler.error(start, end, msg);
    }

    public void warning(int position, String msg) {
        mErrorHandler.warning(position, msg);
    }

    public void warning(int start, int end, String msg) {
        mErrorHandler.warning(start, end, msg);
    }

    /**
     * Returns the ErrorHandler which will be notified of any errors that occur during DSL calls.
     * The default error handler throws RuntimeException on any error.
     */
    public ErrorHandler getErrorHandler() {
        return mErrorHandler;
    }

    /**
     * Installs an ErrorHandler which will be notified of any errors that occur during DSL calls.
     */
    public void setErrorHandler(ErrorHandler errorHandler) {
        mErrorHandler = Objects.requireNonNull(errorHandler);
    }

    /**
     * Create expressions with the given identifier name and current symbol table.
     * Report errors via {@link ErrorHandler}; return null on error.
     */
    @Nullable
    public Expression convertIdentifier(int position, String name) {
        Symbol result = mSymbolTable.find(name);
        if (result == null) {
            error(position, "identifier '" + name + "' is undefined");
            return null;
        }
        return switch (result.getKind()) {
            case FUNCTION_DECL -> {
                var overloadChain = (FunctionDecl) result;
                yield FunctionReference.make(this, position, overloadChain);
            }
            case VARIABLE -> {
                var variable = (Variable) result;
                yield VariableReference.make(position, variable,
                        VariableReference.kRead_ReferenceKind);
            }
            case ANONYMOUS_FIELD -> {
                var field = (AnonymousField) result;
                Expression base = VariableReference.make(position, field.getContainer(),
                        VariableReference.kRead_ReferenceKind);
                yield FieldAccess.make(position, base, field.getFieldIndex(),
                        /*anonymousBlock*/true);
            }
            case TYPE -> {
                var type = (Type) result;
                if (!mIsBuiltin && type.isGeneric()) {
                    error(position, "type '" + type.getName() + "' is generic");
                    type = getTypes().mPoison;
                }
                yield TypeReference.make(this, position, type);
            }
        };
    }
}
