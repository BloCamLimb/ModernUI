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

package icyllis.akashigi.slang;

import icyllis.akashigi.slang.ir.*;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Thread-safe class that tracks per-thread state associated with {@link Parser}.
 */
public final class ThreadContext {

    private static final ThreadLocal<ThreadContext> TLS = new ThreadLocal<>();

    // The Context holds a pointer to the configuration of the program being compiled.
    private final ModuleKind mKind;
    private final ModuleOptions mOptions;
    private final boolean mIsBuiltin;

    // The Context holds all of the built-in types.
    private final BuiltinTypes mTypes;

    // This is the current symbol table of the code we are processing, and therefore changes during
    // compilation
    private final SymbolTable mSymbolTable;
    // The element map from the base module
    private final List<Element> mParentElements;

    private final List<Element> mUniqueElements = new ArrayList<>();
    private final List<Element> mSharedElements = new ArrayList<>();

    // The Context holds a pointer to our error handler.
    private ErrorHandler mErrorHandler = new DefaultErrorHandler();

    /**
     * @param isBuiltin true if we are processing include files
     */
    ThreadContext(ModuleKind kind, ModuleOptions options,
                  Module parent, boolean isBuiltin) {
        mKind = Objects.requireNonNull(kind);
        mOptions = Objects.requireNonNull(options);
        Objects.requireNonNull(parent);
        mIsBuiltin = isBuiltin;

        mTypes = ModuleLoader.getInstance().getBuiltinTypes();

        mSymbolTable = SymbolTable.push(parent.mSymbols, isBuiltin);
        mParentElements = parent.mElements;

        TLS.set(this);
    }

    void kill() {
        TLS.remove();
    }

    /**
     * Returns true if the DSL has been started.
     */
    public static boolean isActive() {
        return TLS.get() != null;
    }

    /**
     * Returns the Context on the current thread.
     *
     * @throws NullPointerException DSL is not started
     */
    public static ThreadContext getInstance() {
        return Objects.requireNonNull(TLS.get(), "DSL is not started");
    }

    public ModuleKind getKind() {
        return mKind;
    }

    public ModuleOptions getOptions() {
        return mOptions;
    }

    public boolean isBuiltin() {
        return mIsBuiltin;
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
     * Returns the elements of the base module.
     */
    public List<Element> getParentElements() {
        return mParentElements;
    }

    /**
     * Returns a list for adding owned elements in the target module.
     */
    public List<Element> getUniqueElements() {
        return mUniqueElements;
    }

    /**
     * Returns a list for adding used elements in the target module shared from {@link #getParentElements()}.
     */
    public List<Element> getSharedElements() {
        return mSharedElements;
    }

    public void error(int position, String msg) {
        mErrorHandler.error(position, msg);
    }

    public void warning(int position, String msg) {
        mErrorHandler.warning(position, msg);
    }

    ErrorHandler getErrorHandler() {
        return mErrorHandler;
    }

    void setErrorHandler(ErrorHandler errorHandler) {
        mErrorHandler = Objects.requireNonNull(errorHandler);
    }

    @Nullable
    public Expression convertIdentifier(int position, String name) {
        Symbol result = mSymbolTable.find(name);
        if (result == null) {
            error(position, "unknown identifier '" + name + "'");
            return null;
        }
        return switch (result.kind()) {
            case Node.SymbolKind.kFunctionDeclaration -> {
                FunctionDeclaration overloadChain = (FunctionDeclaration) result;
                yield FunctionReference.make(position, overloadChain);
            }
            case Node.SymbolKind.kVariable -> {
                Variable variable = (Variable) result;
                yield VariableReference.make(position, variable,
                        VariableReference.kRead_ReferenceKind);
            }
            case Node.SymbolKind.kAnonymousField -> {
                AnonymousField field = (AnonymousField) result;
                Expression base = VariableReference.make(position, field.getContainer(),
                        VariableReference.kRead_ReferenceKind);
                yield FieldExpression.make(position, base, field.getFieldIndex(),
                        FieldExpression.kAnonymousInterfaceBlock_ContainerKind);
            }
            case Node.SymbolKind.kType -> {
                Type type = (Type) result;
                if (!mIsBuiltin && type.isGeneric()) {
                    error(position, "type '" + type.getName() + "' is generic");
                    type = getTypes().mPoison;
                }
                yield TypeReference.make(position, type);
            }
            default -> throw new AssertionError(result.kind());
        };
    }

    private static class DefaultErrorHandler extends ErrorHandler {

        @Override
        protected void handleError(int start, int end, String msg) {
            throw new RuntimeException("error: " + msg);
        }

        @Override
        protected void handleWarning(int start, int end, String msg) {
            // noop
        }
    }
}
