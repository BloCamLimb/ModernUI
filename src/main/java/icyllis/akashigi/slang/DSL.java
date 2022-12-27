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

public final class DSL {

    /**
     * Starts the DSL on the current thread for compiling programs.
     */
    public static void start(ModuleKind kind, ModuleOptions options, icyllis.akashigi.slang.Module parent) {
        if (ThreadContext.isActive()) {
            throw new IllegalStateException("DSL is already started");
        }
        new ThreadContext(kind, options, parent, false);
    }

    /**
     * Starts the DSL on the current thread for compiling modules (include files).
     */
    public static void startModule(ModuleKind kind, ModuleOptions options, Module parent) {
        if (ThreadContext.isActive()) {
            throw new IllegalStateException("DSL is already started");
        }
        new ThreadContext(kind, options, parent, true);
    }

    /**
     * Ends the DSL on the current thread.
     */
    public static void end() {
        ThreadContext.getInstance().kill();
    }

    /**
     * Returns the ErrorHandler which will be notified of any errors that occur during DSL calls.
     * The default error handler throws RuntimeException on any error.
     */
    public static ErrorHandler getErrorHandler() {
        return ThreadContext.getInstance().getErrorHandler();
    }

    /**
     * Installs an ErrorHandler which will be notified of any errors that occur during DSL calls.
     */
    public static void setErrorHandler(ErrorHandler errorHandler) {
        ThreadContext.getInstance().setErrorHandler(errorHandler);
    }
}
