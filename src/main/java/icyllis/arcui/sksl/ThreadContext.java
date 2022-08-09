/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.sksl;

import icyllis.arcui.sksl.ir.SymbolTable;

import javax.annotation.Nullable;

/**
 * Thread-safe class that tracks per-thread state associated with SkSL output.
 */
public class ThreadContext {

    private static final ThreadLocal<ThreadContext> TLS = new ThreadLocal<>();

    private Compiler mCompiler;

    private void kill() {
    }

    /**
     * Returns true if the DSL has been started.
     */
    public static boolean isActive() {
        return TLS.get() != null;
    }

    /**
     * Returns the Compiler used by DSL operations in the current thread.
     */
    public static Compiler getCompiler() {
        return getInstance().mCompiler;
    }

    /**
     * Returns the Context used by DSL operations in the current thread.
     */
    public static Context getContext() {
        return getCompiler().getContext();
    }

    /**
     * Returns the current SymbolTable.
     */
    public static SymbolTable getSymbolTable() {
        return getCompiler().getSymbolTable();
    }

    public static ThreadContext getInstance() {
        var instance = TLS.get();
        assert instance != null;
        return instance;
    }

    public static void setInstance(@Nullable ThreadContext newInstance) {
        var instance = TLS.get();
        assert (instance == null) != (newInstance == null);
        if (instance != null) {
            instance.kill();
        }
        TLS.set(newInstance);
    }
}
