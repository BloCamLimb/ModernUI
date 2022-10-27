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

package icyllis.akashigi.aksl;

import icyllis.akashigi.aksl.ir.ProgramElement;

import java.util.*;

/**
 * Thread-safe class that tracks per-thread state associated with {@link Parser}.
 */
public final class ThreadContext {

    private static final ThreadLocal<ThreadContext> TLS = new ThreadLocal<>();

    private final Compiler mCompiler;

    // The Context holds a pointer to the configuration of the program being compiled.
    private final ModuleKind mKind;
    private final ModuleSettings mSettings;
    private final boolean mIsModule;

    // This is the current symbol table of the code we are processing, and therefore changes during
    // compilation
    private final SymbolTable mSymbolTable;
    // The element map from the base module
    private final ElementMap mBaseElements;

    private final List<ProgramElement> mUniqueElements = new ArrayList<>();
    private final List<ProgramElement> mSharedElements = new ArrayList<>();

    // The Context holds a pointer to our error handler.
    private ErrorHandler mErrors;

    /**
     * @param isModule true if we are processing include files
     */
    ThreadContext(Compiler compiler,
                  ModuleKind kind,
                  ModuleSettings settings,
                  ParsedModule baseModule,
                  boolean isModule) {
        Objects.requireNonNull(compiler);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(settings);
        Objects.requireNonNull(baseModule);
        mCompiler = compiler;
        mKind = kind;
        mSettings = settings;
        mIsModule = isModule;

        mSymbolTable = SymbolTable.push(baseModule.mSymbols, isModule);
        mBaseElements = baseModule.mElements;

        mErrors = compiler.getErrorHandler();

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

    /**
     * Returns the Compiler used by DSL operations in the current thread.
     */
    public Compiler getCompiler() {
        return mCompiler;
    }

    public ModuleKind getKind() {
        return mKind;
    }

    public ModuleSettings getSettings() {
        return mSettings;
    }

    public boolean isModule() {
        return mIsModule;
    }

    /**
     * Returns the BuiltInTypes used by DSL operations in the current thread.
     */
    public BuiltInTypes getTypes() {
        return mCompiler.getTypes();
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
    public ElementMap getBaseElements() {
        return mBaseElements;
    }

    /**
     * Returns a list for adding owned elements in the target module.
     */
    public List<ProgramElement> getUniqueElements() {
        return mUniqueElements;
    }

    /**
     * Returns a list for adding used elements in the target module shared from {@link #getBaseElements()}.
     */
    public List<ProgramElement> getSharedElements() {
        return mSharedElements;
    }

    public void error(int position, String msg) {
        mErrors.error(position, msg);
    }

    public void warning(int position, String msg) {
        mErrors.warning(position, msg);
    }

    public ErrorHandler getErrorHandler() {
        return mErrors;
    }

    public void setErrorHandler(ErrorHandler errors) {
        mErrors = Objects.requireNonNull(errors);
    }
}
